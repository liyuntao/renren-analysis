package service;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import model.FriendInfo;
import utils.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 顶级Actor， 只有一个实例，用于分发子任务给工作者Actor
 */
public class SuperActor extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private List<FriendInfo> friends;
    private int retryTimes = -1;
    private boolean running = false;
    private int subTaskLeft;
    private int errorCount = 0;

    private Map<FriendInfo, List<FriendInfo>> dataMap = new HashMap<>();

    private SuperActor(List<FriendInfo> friendList) {
        this.friends = friendList;
        this.subTaskLeft = friendList.size();
    }

    public static Props props(final List<FriendInfo> friends) {
        return Props.create(new Creator<SuperActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public SuperActor create() throws Exception {
                return new SuperActor(friends);
            }
        });
    }

    public void onReceive(Object message) throws Exception {
        if (message instanceof String) {
            switch ((String) message) {
                case "begin":
                    if (running) {
                        log.warning("Warning: duplicate start message received!");
                    } else {
                        running = true;

                        // 若没有好友o(╯□╰)o，结束任务
                        if (friends.isEmpty()) {
                            log.info("no friends found! terminate the job");
                            context().system().shutdown();
                            return;
                        }

                        log.info("SuperActor begins to work");
                        // 为每一个Friend分发Actor，爬取共同好友
                        for (FriendInfo friendInfo : friends) {
                            ActorRef actorRef = context().actorOf(FriendActor.props(friendInfo), friendInfo.getUid() + "_actor");
                            actorRef.tell("start_crawling", getSelf());
                        }
                    }
                    break;
                case "errorOccur":
                    subTaskLeft--;
                    errorCount++;
                    break;
                case "ignore":
                    subTaskLeft--;
                    log.warning("忽略抓取某好友");
                    break;
                default:
                    log.warning("receive an unrecognized message!");
                    break;
            }
        } else if (message instanceof Pair) {
            // child actor返回它所负责查询的Friend的所有共同好友 List<FriendInfo>
            // 由super actor汇总之
            @SuppressWarnings("unchecked")
            Pair<FriendInfo, List<FriendInfo>> p = (Pair<FriendInfo, List<FriendInfo>>) message;
            dataMap.put(p.getObject1(), p.getObject2());
            log.info("SuperActor已收到并汇总[{}]的好友数据，任务进度:{}/{}", p.getObject1().getName(), dataMap.size(), friends.size());
            subTaskLeft--;

            if (subTaskLeft <= 0) {
                // 任务结束，unwatch并stop所有child actors
                stopAll();
                // 将结果dump至文件
                DataFileHandler.dumpDataFile(dataMap);
                log.info("抓取任务结束!! 共抓取{}个好友，出现{}个错误", friends.size(), errorCount);
                context().system().shutdown();
            }
        } else {
            unhandled(message);
        }
    }

    private void stopAll() {
        for (ActorRef each : getContext().getChildren()) {
            getContext().unwatch(each);
            getContext().stop(each);
        }
    }

}

/**
 * 工作者Actor，SuperActor为每个Friend分配一个FriendActor实例，爬取信息
 */
class FriendActor extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    FriendInfo friendInfo;

    private FriendActor(FriendInfo friendInfo) {
        this.friendInfo = friendInfo;
    }

    public static Props props(final FriendInfo friendInfo) {
        return Props.create(new Creator<FriendActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public FriendActor create() throws Exception {
                return new FriendActor(friendInfo);
            }
        });
    }

    public void onReceive(Object message) throws Exception {
        switch ((String) message) {
            case "start_crawling":
                try {
                    RenrenHttpClient httpClient = RenrenHttpClient.getInstance();
                    int friendCount = httpClient.getFriendCount(this.friendInfo.getUid());
                    if (friendCount > 1000) { // 忽略粉丝众多的运营大号和交际花 TODO 后期应将这个过滤参数扩展至外部
                        getSender().tell("ignore", getSelf());
                        return;
                    }
                    log.info("正在获取[{}]的好友列表... 共计{}个", friendInfo.getName(), friendCount);
                    List<FriendInfo> friendList = httpClient.getFriendList(this.friendInfo.getUid());
                    List<FriendInfo> commonFriends = new ArrayList<>();
                    for (FriendInfo info : friendList) {
                        if(httpClient.isMyFriend(info)) commonFriends.add(info);
                    }
                    getSender().tell(new Pair<>(this.friendInfo, commonFriends), ActorRef.noSender());
                } catch (Exception e) {
                    log.error(e.getMessage());
                    getSender().tell("errorOccur", getSelf());
                }
                break;
            default:
                log.warning("receive an unrecognized message!");
                break;
        }
    }
}

