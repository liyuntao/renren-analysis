package service;

import model.AccountInfo;
import model.FriendInfo;
import utils.CharUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class ModelFactory {

    public static AccountInfo readConfigFile(String filePath) throws Exception {
        if (!Files.exists(Paths.get(filePath))) {
            throw new FileNotFoundException("配置文件[" + filePath + "]不存在！");
        }
        Properties prop = new Properties();
        try {
            prop.load(new FileInputStream(filePath));
            String username = prop.getProperty("username");
            String pwd = prop.getProperty("password");
            return new AccountInfo(username, pwd);
        } catch (IOException e) {
            throw new Exception("读取配置文件出现异常", e);
        }
    }

    public static FriendInfo parseFriendFromStr(String str) {
        if(str == null || str.isEmpty()) return null;
        String content = str.substring(str.indexOf('(') + 1, str.length() - 1);
        String[] parameters = content.split(", ");
        String uid = parameters[0];
        String school = parameters[1];
        String name = CharUtil.removeIllegalChars(parameters[2]);
        String link = parameters[3];
        return new FriendInfo(uid, school, name, link);
    }
}
