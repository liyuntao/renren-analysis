package config;

public class AppConfig {

    // 配置文件：人人账户密码
    public static String ACCOUNT_CONFIG_FILEPATH = "userinfo.properties";

    // School汇总排序的结果文件名
    public static String SchoolRank_OUT_PATH = "school_out.txt";

    // 好友关系数据的结果文件名
    public static String FriendRelationship_OUT_PATH = "network.txt";

    // 并发子任务执行数，建议根据网络状况调整
    public static int MAX_CONCURRENT_TASK_RUNNING = 4;

}
