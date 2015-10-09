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

    private String getMyUid() throws IOException {
        String redirectURL = "http://www.renren.com/profile.do";
        // visit the profile page
        String content = getContentByUrlSync(redirectURL);
        // parse profile to model
        return WebPageHandler.getUid(content);
    }

    public int getFriendCount(String _uid) throws IOException {
        if ("".equals(_uid)) {
            log.warn("uid is missing while calling getFriendCount()");
        }
        String url = "http://friend.renren.com/GetFriendList.do?curpage=0&id=" + _uid;
        String content = getContentByUrlSync(url);
        return WebPageHandler.getFriendCounts(content);
    }

    private List<Element> getFriendElementList(String uid) throws IOException {
        if ("".equals(uid)) {
            log.warn("uid is missing while calling getFriendElementList()"); // TODO
        }

        int elementsInOnePage = 20;
        int page = 0;

        String url = "http://friend.renren.com/GetFriendList.do?curpage=%s&id=" + uid;
        String content = getContentByUrlSync(String.format(url, page));
        int count = WebPageHandler.getFriendCounts(content);

        Pair<Integer, Integer> pair = divmod(count, elementsInOnePage);
        int pageNum = pair.getObject1();
        if (0 != pair.getObject2()) pageNum++;

        List<Element> result = new ArrayList<>();
        for (int i = 0; i <= pageNum; i++) {
            log.debug("正在获取好友页面明细节, page={}", i + 1);
            String _content = getContentByUrlSync(String.format(url, i));
            result.addAll(WebPageHandler.getFriendsInOnePage(_content));
        }
        return result;
    }

    private Pair<Integer, Integer> divmod(int x, int y) {
        return new Pair<>(x / y, x % y);
    }

    public List<FriendInfo> getFriendList(String uid) throws Exception {
        List<Element> friendElementList;
        try {
            friendElementList = getFriendElementList(uid);
        } catch (IOException e) {
            log.error("获取好友列表失败, uid=" + uid, e);
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
        List<FriendInfo> friends = getFriendList(myUid);
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
