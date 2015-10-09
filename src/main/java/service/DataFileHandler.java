package service;

import config.AppConfig;
import model.FriendInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Pair;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFileHandler {

    private static final Logger log = LoggerFactory.getLogger(DataFileHandler.class);

    /**
     * 根据指定的datafile构建数据结构（用于绘图分析的map)
     *
     * @param friendDataFilePath frienddatafile文件的路径
     * @return
     * @throws Exception
     */
    public static Map<FriendInfo, List<FriendInfo>> parseDataFile(String friendDataFilePath) throws Exception {
        if (!Files.exists(Paths.get(friendDataFilePath))) {
            throw new FileNotFoundException("数据文件[" + friendDataFilePath + "]不存在！");
        }

        Map<FriendInfo, List<FriendInfo>> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(new File(friendDataFilePath)))) {
            FriendInfo parent = null;
            for (String line; (line = br.readLine()) != null; ) {
                if (isParentLine(line)) {
                    parent = ModelFactory.parseFriendFromStr(line);
                    if(parent == null) continue;
                    map.put(parent, new ArrayList<>());
                } else {
                    FriendInfo child = ModelFactory.parseFriendFromStr(line);
                    if(parent == null) continue;
                    map.get(parent).add(child);
                }
            }
        }
        return map;
    }

    /**
     * 判断DataFile中的某一行是父节点还是子节点
     * e.g.
     * -Friend(xx, xx, xx, xxx) 为父节点
     * --Friend(xx, xx, xx, xxx)为子节点
     *
     * @return ?
     */
    private static boolean isParentLine(String content) {
        return !content.startsWith("--");
    }

    /**
     * 由map数据结构，输出一份文本文件至指定路径
     *
     * @param map                源数据结构
     * @param targetDataFilePath 输出文件路径
     * @throws IOException
     */
    public static void dumpDataFile(Map<FriendInfo, List<FriendInfo>> map, String targetDataFilePath) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(targetDataFilePath))) {
            for (Map.Entry<FriendInfo, List<FriendInfo>> entry : map.entrySet()) {
                pw.print("-");
                pw.println(entry.getKey().toString());
                for (FriendInfo ele : entry.getValue()) {
                    pw.print("--");
                    pw.println(ele.toString());
                }
            }
        }
    }

    /**
     * 由map数据结构，输出一份文本文件至默认约定路径
     *
     * @param map 源数据结构
     * @throws IOException
     */
    public static void dumpDataFile(Map<FriendInfo, List<FriendInfo>> map) throws IOException {
        dumpDataFile(map, AppConfig.FriendRelationship_OUT_PATH);
    }

    public void dumpSchoolRank(List<Pair<String, Integer>> list, String schoolRankFilePath) throws IOException {
        try (PrintWriter pw = new PrintWriter(new FileWriter(schoolRankFilePath))) {
            pw.println("---Friend School Rank---");
            for (Pair<String, Integer> pair : list) {
                pw.printf("School: %s, Count: %d", pair.getObject1(), pair.getObject2());
            }
        }
    }

}
