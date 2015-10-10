package service;

import model.AccountInfo;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BaseHttpClient {
    private static final Logger log = LoggerFactory.getLogger(BaseHttpClient.class);

    private AccountInfo accountInfo;
    private CloseableHttpClient client;

    public BaseHttpClient(AccountInfo accountInfo) {
        this.accountInfo = accountInfo;

        CookieStore cookieStore = new BasicCookieStore();

        RequestConfig renrenRequestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.DEFAULT)
                .setExpectContinueEnabled(true)
                .build();

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        // Increase max total connection to 200
        cm.setMaxTotal(20);

        client = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultCookieStore(cookieStore)
                .setDefaultRequestConfig(renrenRequestConfig)
                .build();

        login();
    }

    public void login() {
        String url = "http://www.renren.com/ajaxLogin/login";
        HttpPost httpPost = new HttpPost(url);
        List<NameValuePair> nvps = new ArrayList<>();
        nvps.add(new BasicNameValuePair("domain", "renren.com"));
        nvps.add(new BasicNameValuePair("isplogin", "true"));
        nvps.add(new BasicNameValuePair("formName", ""));
        nvps.add(new BasicNameValuePair("method", ""));
        nvps.add(new BasicNameValuePair("submit", "登录"));
        nvps.add(new BasicNameValuePair("email", accountInfo.getUsername()));
        nvps.add(new BasicNameValuePair("password", accountInfo.getPwd()));
        try {
            httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        } catch (UnsupportedEncodingException e) {
            log.error(e.getMessage());
        }
        try (CloseableHttpResponse res = client.execute(httpPost)) {
        } catch (IOException e) {
            log.error("登陆请求过程中出错", e);
        }
    }

    public String getContentByUrlSync(String url) throws IOException, InterruptedException {
        String[] useragents = {
                "Mozilla/5.0 (X11; Linux i686) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.57 Safari/537.17",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.11 (KHTML, like Gecko) Ubuntu Chromium/23.0.1271.97 Chrome/23.0.1271.97 Safari/537.11",
                "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.17 (KHTML, like Gecko) Chrome/24.0.1312.56 Safari/537.17",
                "Mozilla/5.0 (X11; Linux i686) AppleWebKit/534.34 (KHTML, like Gecko) rekonq/1.1 Safari/534.34",
                "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/536.26.17 (KHTML, like Gecko) Version/6.0.2 Safari/536.26.17"
        };

        Random random = new Random();
        HttpGet httpGet = new HttpGet(url);
        // 设置UserAgent
        httpGet.setHeader(HttpHeaders.USER_AGENT, useragents[random.nextInt(useragents.length)]);

        // 设置请求和传输超时时间
        RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(10000).setConnectTimeout(10000).build();
        httpGet.setConfig(requestConfig);

        try {
            try (CloseableHttpResponse res_new = client.execute(httpGet)) {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(res_new.getEntity().getContent()));
                StringBuilder content = new StringBuilder();
                String line = bufferedReader.readLine();
                while (line != null) {
                    content.append(line);
                    line = bufferedReader.readLine();
                }
                return content.toString();
            }
        } catch (Exception e) {
            log.debug(e.getMessage());
            throw e;
        }
    }

}


