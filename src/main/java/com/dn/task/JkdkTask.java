package com.dn.task;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.dn.web.MailService;
import lombok.Data;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
@Data
@RunWith(SpringRunner.class)
@SpringBootTest
@ConfigurationProperties(prefix = "jkdk")
public class JkdkTask {
    private RequestConfig config;
    private CloseableHttpClient client;
    private CookieStore cookieStore;
    private String username;
    private String password;
    private String currLocation = "%E5%B1%B1%E4%B8%9C%E7%9C%81%E8%8F%8F%E6%B3%BD%E5%B8%82%E7%89%A1%E4%B8%B9%E5%8C%BA%E5%92%8C%E5%B9%B3%E5%8D%97%E8%B7%AF";
    @Autowired
    private MailService mailService;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Scheduled(cron="0 0 12 * * ?")
    public void login() throws Exception {
        cookieStore = new BasicCookieStore();
        config = RequestConfig.custom().setRedirectsEnabled(false).setConnectTimeout(50000).setSocketTimeout(50000).setConnectionRequestTimeout(50000).build();
        client = HttpClients.custom().setDefaultRequestConfig(config).setDefaultCookieStore(cookieStore).build();
        cookieStore.addCookie(new BasicClientCookie("org.springframework.web.servlet.i18n.CookieLocaleResolver.LOCALE","zh_CN"));
        HttpGet loginGet = new HttpGet("https://authserver.nju.edu.cn:443/authserver/login?service=http%3A%2F%2Fehallapp.nju.edu.cn%2Fxgfw%2Fsys%2Fyqfxmrjkdkappnju%2Fapply%2FgetApplyInfoList.do");
        CloseableHttpResponse loginPageRes = client.execute(loginGet);

        XPath xPath = XPathFactory.newInstance().newXPath();
        String loginPage = EntityUtils.toString(loginPageRes.getEntity());
        Document doc = new DomSerializer(new CleanerProperties()).createDOM(new HtmlCleaner().clean(loginPage));
        String lt = xPath.evaluate("//*[@id=\"casLoginForm\"]/input[1]/@value", doc);
        String dllt = xPath.evaluate("//*[@id=\"casLoginForm\"]/input[2]/@value", doc);
        String execution = xPath.evaluate("//*[@id=\"casLoginForm\"]/input[3]/@value", doc);
        String _eventId = xPath.evaluate("//*[@id=\"casLoginForm\"]/input[4]/@value", doc);
        String rmShown = xPath.evaluate("//*[@id=\"casLoginForm\"]/input[5]/@value", doc);
        String salt = xPath.evaluate("//*[@id=\"pwdDefaultEncryptSalt\"]/@value", doc);

        HttpPost loginPost = new HttpPost("https://authserver.nju.edu.cn/authserver/login?service=http%3A%2F%2Fehallapp.nju.edu.cn%2Fxgfw%2Fsys%2Fyqfxmrjkdkappnju%2Fapply%2FgetApplyInfoList.do");
        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair("username",username));
        params.add(new BasicNameValuePair("password",encrypt(password,salt)));
        params.add(new BasicNameValuePair("lt",lt));
        params.add(new BasicNameValuePair("dllt",dllt));
        params.add(new BasicNameValuePair("execution",execution));
        params.add(new BasicNameValuePair("_eventId",_eventId));
        params.add(new BasicNameValuePair("rmShown",rmShown));
        loginPost.setEntity(new UrlEncodedFormEntity(params));
        CloseableHttpResponse loginResponse = client.execute(loginPost);
        String location = loginResponse.getFirstHeader("location").getValue();

        HttpGet httpGet = new HttpGet(location);
        CloseableHttpResponse locationRes = client.execute(httpGet);
        System.out.println(locationRes.getStatusLine().getStatusCode());
        String finalLocation = locationRes.getFirstHeader("location").getValue();

        sendRequest(finalLocation);
    }

    public void sendRequest(String finalLocation) throws Exception {
        HttpGet dataGet = new HttpGet(finalLocation);
        CloseableHttpResponse dataRes = client.execute(dataGet);

        String json = EntityUtils.toString(dataRes.getEntity());
        JSONObject resp = JSON.parseObject(json);
        if(resp.getInteger("code")==0){
            String wid = resp.getJSONArray("data").getJSONObject(0).getString("WID");
            HttpGet httpGet = new HttpGet("http://ehallapp.nju.edu.cn/xgfw/sys/yqfxmrjkdkappnju/apply/saveApplyInfos.do?WID="+wid+"&CURR_LOCATION="+currLocation+"&IS_TWZC=1&IS_HAS_JKQK=1&JRSKMYS=1&JZRJRSKMYS=1");
            CloseableHttpResponse httpResponse = client.execute(httpGet);

            String finalResJson = EntityUtils.toString(httpResponse.getEntity());
            JSONObject finalRes = JSON.parseObject(finalResJson);
            String code = finalRes.getString("code");
            String msg = finalRes.getString("msg");
            System.out.println("code"+code);
            System.out.println("msg:"+msg);

            mailService.sendEmail("每日自动健康打卡","响应码："+code+"\n返回信息："+msg);
        }else{
            throw new IOException("未查询到打卡记录，无法进行下一步操作");
        }
    }

    private String getRandomString(int length){
        String chs = "ABCDEFGHJKMNPQRSTWXYZabcdefhijkmnprstwxyz2345678";
        String result = "";
        for (int i = 0; i < length; i++) {
            result += chs.charAt((int)Math.floor(Math.random()*chs.length()));
        }
        return result;
    }

    private String AESEncrypt(String data,String key,String iv) throws Exception {
        String charset="UTF-8";
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(charset), "AES");
        AlgorithmParameterSpec ivParameterSpec = new IvParameterSpec(iv.getBytes(charset));
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE,secretKeySpec,ivParameterSpec);
        byte[] encryptedBytes = cipher.doFinal(data.getBytes(charset));
        return Base64.encodeBase64String(encryptedBytes);
    }

    private String encrypt(String password,String key) throws Exception {
        return AESEncrypt(getRandomString(64)+password,key,getRandomString(16));
    }


}
