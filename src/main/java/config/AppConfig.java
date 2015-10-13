package config;

public class AppConfig {

    // 配置文件：人人账户密码
    public static final String ACCOUNT_CONFIG_FILEPATH = "userinfo.properties";

    // School汇总排序的结果文件名
    public static final String SchoolRank_OUT_PATH = "school_out.txt";

    // 好友关系数据的结果文件名
    public static final String FriendRelationship_OUT_PATH = "network.txt";

    // 并发子任务执行数，建议根据网络状况调整
    public static final int MAX_CONCURRENT_TASK_RUNNING = 4;

    // 网络请求的超时重试次数
    public static final int MAX_NETWORK_REQUEST_TIMES = 20;

    // 排除：超过此数目的好友不会被抓取（用以忽略粉丝众多的运营大号，减轻爬虫压力）
    public static final int MAX_FRIENDS_EXCLUDE = 1500;

}
