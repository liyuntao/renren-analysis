import config.AppConfig;
import model.FriendInfo;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import service.DataFileHandler;
import service.RenrenHttpClient;
import service.graph.GraphHandler;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new DefaultParser();
        // create the Options
        Options options = new Options();
        options.addOption("h", "help", false, "help help help");
        options.addOption("c", "crawl", false, "crawl data from renren");
        try {
            CommandLine line = parser.parse(options, args);
            // validate that block-size has been set
            if (line.hasOption("c")) {
                // 爬取好友数据，会自动生成至文件
                RenrenHttpClient controller = RenrenHttpClient.create();
                controller.runFriendNetwork();
            } else {
                // 读取文件至内存并进行可视化展示
                Map<FriendInfo, List<FriendInfo>> dataMap = DataFileHandler.parseDataFile(AppConfig.FriendRelationship_OUT_PATH);
                GraphHandler grapher = new GraphHandler(dataMap);
                grapher.script();
            }
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }
    }
}
