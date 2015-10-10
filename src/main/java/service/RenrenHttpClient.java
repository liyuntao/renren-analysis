package service;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import config.AppConfig;
import model.AccountInfo;
import model.FriendInfo;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Pair;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RenrenHttpClient extends BaseHttpClient {
    private static final Logger log = LoggerFactory.getLogger(RenrenHttpClient.class);

    private Set<FriendInfo> cache;

    private static RenrenHttpClient INSTANCE;

    public static RenrenHttpClient getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("RenrenHttpClient对象尚未创建，需先调用create()方法");
        }
        return INSTANCE;
    }

    public static RenrenHttpClient create() throws Exception {
        if (INSTANCE == null) {
            INSTANCE = new RenrenHttpClient(ModelFactory.readConfigFile(AppConfig.ACCOUNT_CONFIG_FILEPATH));
        }
        return INSTANCE;
    }

    private RenrenHttpClient(AccountInfo accountInfo) throws Exception {
        super(accountInfo);
    }

    private String getMyUid() throws Exception {
        String redirectURL = "http://www.renren.com/profile.do";
        // visit the profile page
        String content = getContentByUrlSync(redirectURL);
        // parse profile to model
        return WebPageHandler.getUid(content);
    }

    public int getFriendCount(String _uid) throws Exception {
        if ("".equals(_uid)) {
            log.warn("uid is missing while calling getFriendCount()");
        }
        String url = "http://friend.renren.com/GetFriendList.do?curpage=0&id=" + _uid;
        String content = getContentByUrlSync(url);
        return WebPageHandler.getFriendCounts(content);
    }

    private List<Element> getFriendElementList(String uid, String name) throws Exception {
        if ("".equals(uid)) {
            log.warn("uid is missing while calling getFriendElementList()");
        }

        int elementsInOnePage = 20;
        int page = 0;

        String url = "http://friend.renren.com/GetFriendList.do?curpage=%s&id=" + uid;
        String page0_content = getContentByUrlSync(String.format(url, page));
        int count = WebPageHandler.getFriendCounts(page0_content);

        Pair<Integer, Integer> pair = divmod(count, elementsInOnePage);
        int pageNum = pair.getObject1();
        if (0 != pair.getObject2()) pageNum++;

        List<Element> result = new ArrayList<>();
        for (int i = 0; i <= pageNum; i++) {
            String page_url = String.format(url, i);
            String _content;
            long start = System.currentTimeMillis();
            if(i > 0) { // 超时重试机制
                int retryTimesLeft = AppConfig.MAX_NETWORK_REQUEST_TIMES;
                _content = null;
                while(retryTimesLeft > 0 && _content == null) {
                    try {
                        _content = getContentByUrlSync(page_url);
                    } catch(SocketTimeoutException e) {
                        retryTimesLeft--;
                    }
                }
                if(_content == null) {
                    log.warn("超时重试次数过多，忽略好友[{}]某个页面明细 debug_url: {}", name, page_url);
                    continue;
                }
            } else { // 重复利用前面的0页内容，减少一次网络IO
                _content = page0_content;
            }
            long end = System.currentTimeMillis();
            log.debug("已经获取好友[{}]页面明细, page={}, 耗时{}", name, i + 1, end - start);
            result.addAll(WebPageHandler.getFriendsInOnePage(_content));
        }
        return result;
    }

    private Pair<Integer, Integer> divmod(int x, int y) {
        return new Pair<>(x / y, x % y);
    }

    public List<FriendInfo> getFriendList(String uid, String name) throws Exception {
        List<Element> friendElementList;
        try {
            friendElementList = getFriendElementList(uid, name);
        } catch (IOException e) {
            log.error("获取好友[" + name + "]列表失败, uid=" + uid, e);
            throw e;
        }
        return WebPageHandler.transformElementsToFriendInfos(friendElementList);
    }

    private void getSchoolRank() {
        // TODO
    }

    public void runFriendNetwork() throws Exception {
        String myUid = getMyUid();
        log.info("正在获取账号的所有好友...");
        List<FriendInfo> friends = getFriendList(myUid, "me");
        // 缓存为map
        cache = new HashSet<>(friends);
        crawlFriendsData(friends);
    }

    public boolean isMyFriend(FriendInfo friendInfo) {
        if (cache == null) {
            throw new RuntimeException("内部错误，cache未初始化");
        }
        return cache.contains(friendInfo);
    }

    /**
     * 开启爬虫，收集数据至文件
     */
    private void crawlFriendsData(List<FriendInfo> friends) throws Exception {
        // 构建ActorSystem，开启并发作业
        ActorSystem system = ActorSystem.create("CrawlerSystem");
        ActorRef superActor = system.actorOf(SuperActor.props(friends), "super_actor");
        superActor.tell("begin", ActorRef.noSender());
    }

}
