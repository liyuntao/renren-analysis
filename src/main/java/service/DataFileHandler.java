package service;

import config.AppConfig;
import model.FriendInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataFileHandler {

    private static final Logger log = LoggerFactory.getLogger(DataFileHandler.class);

    private static FileWriter infoFile;

    private static FileWriter listFile;

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
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(new File(friendDataFilePath)), StandardCharsets.UTF_8))) {
            FriendInfo parent = null;
            for (String line; (line = br.readLine()) != null; ) {
                if (isParentLine(line)) {
                    parent = ModelFactory.parseFriendFromStr(line);
                    if (parent == null) continue;
                    map.put(parent, new ArrayList<>());
                } else {
                    FriendInfo child = ModelFactory.parseFriendFromStr(line);
                    if (parent == null) continue;
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
     * 将一个好友的信息以及其好友列表持久化
     *
     * @param pair
     */
    public static void writeFriendInfo(Pair<FriendInfo, List<FriendInfo>> pair) throws IOException {
        FriendInfo friendInfo = pair.getObject1();
        List<FriendInfo> friendInfoList = pair.getObject2();
        infoFile.write("-");
        infoFile.write(friendInfo.toString());
        for (FriendInfo ele : friendInfoList) {
            infoFile.write("--");
            infoFile.write(ele.toString());
            infoFile.write("\r\n");
        }
        infoFile.flush();
    }

    public static void writeFriendList(List<FriendInfo> friendInfoList) throws IOException {
        for (FriendInfo friendInfo : friendInfoList) {
            listFile.write("-");
            listFile.write(friendInfo.toString());
            listFile.write("\r\n");
            listFile.flush();
        }
    }

    public static List<FriendInfo> recoverUnDoTask() throws IOException {
        List<FriendInfo> list = new ArrayList<>();
        File checkListFile = new File(AppConfig.FriendList_OUT_PATH);
        if (checkListFile.exists()) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(checkListFile), StandardCharsets.UTF_8))) {
                FriendInfo friendInfo;
                for (String line; (line = br.readLine()) != null; ) {
                    if (isParentLine(line)) {
                        friendInfo = ModelFactory.parseFriendFromStr(line);
                        list.add(friendInfo);
                    }
                }
            }

            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    new FileInputStream(new File(AppConfig.FriendRelationship_OUT_PATH)), StandardCharsets.UTF_8))) {
                FriendInfo parent;
                for (String line; (line = br.readLine()) != null; ) {
                    if (isParentLine(line)) {
                        parent = ModelFactory.parseFriendFromStr(line);
                        if (list.contains(parent)) {
                            list.remove(parent);
                        }
                    }
                }
            }

            infoFile = new FileWriter(AppConfig.FriendRelationship_OUT_PATH, true);
        } else {
            infoFile = new FileWriter(AppConfig.FriendRelationship_OUT_PATH);
        }
        listFile = new FileWriter(AppConfig.FriendList_OUT_PATH);
        return list;
    }

    public static void finishFile() {
        new File(AppConfig.FriendList_OUT_PATH).delete();
    }

    /**
     * 由map数据结构，输出一份文本文件至指定路径
     *
     * @param map                源数据结构
     * @param targetDataFilePath 输出文件路径
     * @throws IOException
     */
    public static void dumpDataFile(Map<FriendInfo, List<FriendInfo>> map, String targetDataFilePath) throws IOException {
        try (PrintWriter pw = new PrintWriter(new File(targetDataFilePath), "UTF-8")) {
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

    public static void dumpSchoolRank(List<Pair<String, Integer>> list, String schoolRankFilePath) throws IOException {
        try (PrintWriter pw = new PrintWriter(new File(schoolRankFilePath), "UTF-8")) {
            pw.println("---Friend School Rank---");
            list.forEach(pair -> pw.println("School: " + pair.getObject1() + "   Count: " + pair.getObject2()));
        }
    }

}
