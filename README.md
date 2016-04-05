# renren-analysis    [![Build Status](https://travis-ci.org/liyuntao/renren-analysis.svg?branch=master)](https://travis-ci.org/liyuntao/renren-analysis)
**a project which is used for crawler and data visualization on [renren.com](http://www.renren.com)**

<figure>
	<img src="http://i.imgur.com/QESM88q.png" height="450">
</figure>

## Requirements

In order to run this project you will need to have installed:

* JDK 1.8 or later
* Maven (3.0+ recommended)

## Build

    git clone https://github.com/liyuntao/renren-analysis
    cd renren-analysis
    mvn clean package

## Run jar file

    # 1. build the project first
    # 2. configuring the renren.com username and password in userinfo.properties
    # 3. run the crawler
    cd renren-analysis/target
    java -jar renren-analysis-1.0-SNAPSHOT-with-dependencies -c="./userinfo.properties"
    # or draw a pic by the output of crawler
    java -jar renren-analysis-1.0-SNAPSHOT-with-dependencies -d="./network.txt"

## Run source code

1. 将 Maven 工程导入 IDE
2. 首次使用需填写账号信息至配置文件， 位于 resources/userinfo.properties
3. 执行 Main.java 即可
4. 更多个性化调节可以修改AppConfig.java

## Structure and Usage tips

1. 工程主要分为两大块，绝大部分的代码实现了一个爬虫。
2. 并发方面使用 [Akka](http://akka.io) 的 actor 模型; 爬虫部分使用 Apache HttpClient 配合 [jsoup](http://jsoup.org/) 进行页面抓取
3. 执行 Main.java 中的示例 API01（方法体前两行代码）进行爬取，爬取完成后生成 network.txt 文件位于项目目录下。通常来讲爬虫API只需执行一次
4. 爬虫只需执行一次，后续的可视化展现可以由上述 txt 文档作为数据来源
5. Main.java 的示例 API02 (方法体后三行)使用 [gephi](https://gephi.github.io/) 实现数据的可视化展示

## Output

### 好友关系图

以JFrame的形式展示，并生成在`headless_simple.png`和`headless_simple.svg`中

### 文本格式的好友关系

在`network.txt`中，是语言无关的，可以基于该文本做很多事情。其格式为

	-Friend(uid, location, name, url)  // 1
	--Friend(uid, location, name, url) // 2
	--Friend(uid, location, name, url) // 3
	...
	-Friend(uid, location, name, url)  // 2
	--Friend(uid, location, name, url) // 5

其中-代表是节点，--代表由之前-节点指向该--节点的边。比如在此例中，行1为一个节点，行2代表一个由行1节点指向行2节点的一条边。

## Feature
1. 实现了请求的超时重试机制
2. 支持任务的断点续传
3. 支持多种Layout算法的扩展，可以方便的切换

<figure>
	<img src="http://i.imgur.com/K1rAIov.png" height="450" >
	<img src="http://i.imgur.com/tElpIgL.png" height="450" >
	<img src="http://i.imgur.com/Ey41iN5.png" height="450" >
</figure>

