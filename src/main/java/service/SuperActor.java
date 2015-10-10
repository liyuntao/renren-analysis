package service;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.dispatch.OnFailure;
import akka.dispatch.OnSuccess;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.japi.Creator;
import model.FriendInfo;
import scala.concurrent.Future;
import utils.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import static akka.dispatch.Futures.future;


/**
 * 顶级Actor， 只有一个实例，用于分发子任务给工作者Actor
 */
public class SuperActor extends UntypedActor {
    LoggingAdapter log = Logging.getLogger(getContext().system(), this);

    private List<FriendInfo> friends;
    private boolean running = false;
    private int subTaskLeft;
    private int errorCount = 0;

    private Map<FriendInfo, List<FriendInfo>> dataMap = new HashMap<>();

    private SuperActor(List<FriendInfo> friendList) {
        this.friends = friendList;

        // FIXME 测试使用，减小数据量
//        List<FriendInfo> fake = new ArrayList<>();
//        for(int i = 0; i < 10; i++) {
//            fake.add(friends.get(i));
//        }
//        friends = fake;

        this.subTaskLeft = friends.size();
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
            log.warning("superActor未处理的消息");
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
                Future<Pair<FriendInfo, List<FriendInfo>>> f = future(new Callable<Pair<FriendInfo, List<FriendInfo>>>() {
                    public Pair<FriendInfo, List<FriendInfo>> call() throws Exception {
                        List<FriendInfo> commonFriends = new ArrayList<>();

                        RenrenHttpClient httpClient = RenrenHttpClient.getInstance();
                        int friendCount = httpClient.getFriendCount(friendInfo.getUid());
                        if (friendCount > 1000) { // 忽略粉丝众多的运营大号和交际花 TODO 后期应将这个过滤参数扩展至外部
                            log.warning("[{}]好友数量超过{}, 已忽略抓取", friendInfo.getName(), 1000);
                            return new Pair<>(friendInfo, commonFriends);
                        }
                        log.info("正在获取[{}]的好友列表... 共计{}个", friendInfo.getName(), friendCount);
                        List<FriendInfo> friendList = httpClient.getFriendList(friendInfo.getUid(), friendInfo.getName());
                        for (FriendInfo info : friendList) {
                            if(httpClient.isMyFriend(info)) commonFriends.add(info);
                        }
                        return new Pair<>(friendInfo, commonFriends);
                    }
                }, getContext().system().dispatcher());

                ActorRef sender = getSender();
                ActorRef self = getSelf();

                f.onSuccess(new OnSuccess<Pair<FriendInfo, List<FriendInfo>>>() {
                    @Override
                    public void onSuccess(Pair<FriendInfo, List<FriendInfo>> result) throws Throwable {
                        sender.tell(result, ActorRef.noSender());
                        context().stop(self);
                    }
                }, getContext().system().dispatcher());

                f.onFailure(new OnFailure() {
                    @Override
                    public void onFailure(Throwable failure) throws Throwable {
                        log.error(failure.getMessage());
                        sender.tell("errorOccur", getSelf());
                    }
                }, getContext().system().dispatcher());
                break;
            default:
                log.warning("receive an unrecognized message!");
                break;
        }
    }
}

