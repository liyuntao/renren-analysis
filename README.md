# renren-analysis
**a project which is used for crawler and data visualization on renren.com**

## Requirements

In order to run this projectyou will need to have installed:

* JDK 1.8 or later
* Maven (3.0+ recommended)

## How to start

1. 将工程导入 IDE
2. 添加工程根目录下的 lib/gephi-toolkit.jar 至 dependencies
3. 首次使用需填写账号信息至配置文件 位于 resources/userinfo.properties
4. 执行 Main.java 即可

## Structure

1. 工程主要分为两大块，绝大部分的类实现了一个爬虫。
2. 并发方面使用 [Akka](akka.io) 的 actor 并发模型; 爬虫部分使用Apache HttpClient配合 [jsoup](http://jsoup.org/) 进行页面抓取
3. 执行爬虫task的API会进行爬取，生成一个 network.txt 文件在项目目录下
4. Grapher类使用 [gephi](https://gephi.github.io/) 实现了对数据的可视化展示


