# renren-analysis
**a project which is used for crawler and data visualization on renren.com**

<figure>
	<img src="http://gaocegege.com/scala-renren/example.png" height="450">
</figure>

## Requirements

In order to run this projectyou will need to have installed:

* JDK 1.8 or later
* Maven (3.0+ recommended)

## How to start

1. 将工程导入 IDE
2. 添加工程根目录下的 lib/gephi-toolkit.jar 至 dependencies
3. 首次使用需填写账号信息至配置文件， 位于 resources/userinfo.properties
4. 执行 Main.java 即可

## Structure and Usage tips

1. 工程主要分为两大块，绝大部分的类实现了一个爬虫。
2. 并发方面使用 [Akka](http://akka.io) 的 actor 模型; 爬虫部分使用 Apache HttpClient 配合 [jsoup](http://jsoup.org/) 进行页面抓取
3. 执行爬虫task的API会进行爬取，生成一个 network.txt 文件在项目目录下
4. Grapher类使用 [gephi](https://gephi.github.io/) 实现了对数据的可视化展示
5. 爬虫只需执行一次，后续的可视化可以由工具类读取本地已经保存的数据文件

## Output

### 好友学校统计

暂时未实现

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

