import config.AppConfig;
import model.FriendInfo;
import service.DataFileHandler;
import service.Grapher;
import service.RenrenHttpClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {

        // 爬取好友数据，会自动生成至文件
        RenrenHttpClient controller = RenrenHttpClient.create();
        controller.runFriendNetwork();

        // 读取文件至内存并进行可视化展示
//        Map<FriendInfo, List<FriendInfo>> dataMap = DataFileHandler.parseDataFile(AppConfig.FriendRelationship_OUT_PATH);
//        Grapher grapher = new Grapher(dataMap);
//        grapher.script();
    }
}
