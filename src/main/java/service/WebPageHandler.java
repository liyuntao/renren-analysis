package service;


import model.FriendInfo;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class WebPageHandler {
    private static final Logger log = LoggerFactory.getLogger(WebPageHandler.class);

    public static String getUid(String content) {
        Document document = Jsoup.parse(content);
        Elements elements = document.select("a[href~=renren.com/[0-9]+/profile]");
        String[] hrefs = elements.get(0).attr("href").split("/");
        return hrefs[3];
    }

    public static int getFriendCounts(String content) {
        Document document = Jsoup.parse(content);
        Elements elements = document.select("span[class=count]");
        String num = elements.get(0).html();
        return Integer.parseInt(num);
    }

    public static List<Element> getFriendsInOnePage(String content) {
        Document document = Jsoup.parse(content);
        Elements elements = document.select("ol[id=friendListCon]");
        if (!elements.isEmpty()) {
            LinkedList<Element> list = new LinkedList<>();
            for (Element element : elements.get(0).children()) {
                list.addFirst(element);
            }
            return list;
        } else {
            log.error("未爬取到friends页面内容!");
            throw new RuntimeException("未爬取到friends页面内容!");
        }
    }

    public static List<FriendInfo> transformElementsToFriendInfos(List<Element> elements) {
        if (elements == null || elements.size() == 0) return Collections.emptyList();
        List<FriendInfo> result = new ArrayList<>(elements.size());
        for (Element element : elements) {
            result.add(transformElementToFriendInfo(element));
        }
        return result;
    }

    public static FriendInfo transformElementToFriendInfo(Element input) {
        Elements infos = input.select("div[class=info]");
        Iterator<Element> infosIter = infos.iterator();
        Element info = infosIter.next();
        Iterator<Element> valueIter = info.select("dd").iterator();
        valueIter.next();
        String name = info.select("a").iterator().next().html();
        String school = valueIter.next().html();
        String link = info.select("a").iterator().next().attr("href");
        String uid = link.split("=")[1];
        return new FriendInfo(uid, school, name, link);
    }

    public static void genSchoolRankFile() {
        // TODO
    }

}
