import config.AppConfig;
import model.FriendInfo;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.DataFileHandler;
import service.RenrenHttpClient;
import service.graph.GraphHandler;

import java.text.ParseException;
import java.util.List;
import java.util.Map;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static Options options;

    static {
        // create the Options
        options = new Options();
        options.addOption("h", "help", false, "show help.");
        options.addOption("c", "crawl", true, "crawl data from renren");
        options.addOption("d", "draw", true, "draw pic");
    }

    public static void main(String[] args) throws Exception {
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption("h")) {
                help();
            } else if (cmd.hasOption("c")) {
                String configFilePath = cmd.getOptionValue("c");
                if(configFilePath != null) {
                    AppConfig.ACCOUNT_CONFIG_FILEPATH = configFilePath;
                }
                // 爬取好友数据，会自动生成至文件
                RenrenHttpClient controller = RenrenHttpClient.create();
                controller.runFriendNetwork();
            } else if (cmd.hasOption("d")) {
                String dataFilePath = cmd.getOptionValue("d");
                AppConfig.FriendRelationship_OUT_PATH = dataFilePath;
                log.info("Using cli argument -d=" + dataFilePath);
                // 读取文件至内存并进行可视化展示
                Map<FriendInfo, List<FriendInfo>> dataMap = DataFileHandler.parseDataFile(AppConfig.FriendRelationship_OUT_PATH);
                GraphHandler grapher = new GraphHandler(dataMap);
                grapher.script();
            } else {
                help();
            }
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
        }
    }

    private static void help() {
        HelpFormatter formater = new HelpFormatter();
        formater.printHelp("Main", options);
        System.exit(0);
    }
}
