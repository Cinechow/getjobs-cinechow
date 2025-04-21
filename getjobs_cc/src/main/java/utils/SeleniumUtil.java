package utils;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static utils.Constant.*;

@Slf4j
public class SeleniumUtil {
    public static void initDriver(){
        //初始化驱动程序
        SeleniumUtil.getChromeDriver();//获取ChromeDriver 实例
        SeleniumUtil.getActions();// 初始化Actions
        SeleniumUtil.getWait(WAIT_TIME);//初始化等待时间
    }
    //获取ChromeDriver实例
    private static void getChromeDriver() {
        ChromeOptions options = new ChromeOptions();
        // 设置 Chrome 浏览器的可执行文件路径，指定使用哪个 Chrome 浏览器实例。
        options.setBinary("C:/Program Files/Google/Chrome/Application/chrome.exe");
        // 为 Chrome 浏览器添加扩展插件。这里添加的是 "xpathHelper.crx" 扩展插件。
        options.addExtensions(new File("src/main/resources/xpathHelper.crx"));
        // 设置 ChromeDriver 的路径，Selenium 使用该驱动程序来控制 Chrome 浏览器
        System.setProperty("webdriver.chrome.driver", "src/main/resources/chromedriver.exe");
        options.addArguments("--window-position=2600,750");//将窗口移动到副屏的起始位置
        options.addArguments("--window-size=1600,1000");//设置窗口大小以适应副屏分辨率
        options.addArguments("--start-maximized");//最大化窗口
//        options.addArguments("--headless");//切换无头模式
        CHROME_DRIVER = new ChromeDriver(options);
    }
    public static void saveCookie(String path) {
        //获取所有cookies
        Set<Cookie> cookies = CHROME_DRIVER.manage().getCookies();
        //创建一个JSONArray来保存所有cookie信息
        JSONArray jsonArray = new JSONArray();
        //将每一个cookie转换为JSONObject，并添加到JSONArray中
        for (Cookie cookie : cookies){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("name", cookie.getName());
            jsonObject.put("value", cookie.getValue());
            jsonObject.put("domain", cookie.getDomain());
            jsonObject.put("path", cookie.getPath());
            if (cookie.getExpiry() != null){
                jsonObject.put("expriy", cookie.getExpiry().getTime());
            }
            jsonObject.put("isSecure", cookie.isSecure());
            jsonObject.put("isHttpOnly", cookie.isHttpOnly());
            jsonArray.put(jsonObject);
        }
        try (FileWriter file = new FileWriter(path)){
            //将JSONArray写入文件
            file.write(jsonArray.toString(4));//使用4个空格的缩进
            log.info("Cookie已保存到文件：{}",path);
        } catch (Exception e) {
            log.error("保存cookie异常！保存路径:{}", path);
        }
    }
    public static void lodCookie(String cookiePath){
        //首先清除由于浏览器打开已有的cookies
        CHROME_DRIVER.manage().deleteAllCookies();
        //从文件中读取JSONArray
        JSONArray jsonArray = null;
        try {
            String jsonText = new String(Files.readAllBytes(Paths.get(cookiePath)));
        } catch (IOException e){
            log.error("读取cookie异常");
        }
        //遍历JSONArray中每个JSONObeject，从中获取cookie的信息
        if (jsonArray != null){
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                String name = jsonObject.getString("name");
                String value = jsonObject.getString("value");
                String domain = jsonObject.getString("domain");
                String path = jsonObject.getString("path");
                Date expiry = null;
                if (!jsonObject.isNull("expriy")){
                    expiry = new Date(jsonObject.getLong("expiry"));
                }
                boolean isSecure = jsonObject.getBoolean("isSecure");
                boolean isHttpOnly = jsonObject.getBoolean("isHttpOnly");
                //使用这些信息来创建新的Cookie对象，并将它们添加到WebDriver中
                Cookie cookie = new Cookie.Builder(name, value)
                        .domain(domain)
                        .path(path)
                        .expiresOn(expiry)
                        .isSecure(isSecure)
                        .isHttpOnly(isHttpOnly)
                        .build();
                try {
                    CHROME_DRIVER.manage().addCookie(cookie);
                }catch (Exception e) {
                    log.error("【洒洒水拉，不要怕】cookie添加异常:【{}】",cookie);
                }
            }
        }
    }
    public static void getActions(){
        ACTIONS = new Actions(CHROME_DRIVER);
    }

    public static void getWait(long time) {
        WAIT = new WebDriverWait(CHROME_DRIVER,time);
    }
    public static void sleep(int seconds){
        try {
            TimeUnit.SECONDS.sleep(seconds);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            log.error("Sleep was interrupted", e);
        }
    }
    public static void sleepByMilliSeconds(int milliSeconds){
        try {
            TimeUnit.MICROSECONDS.sleep(milliSeconds);
        }catch (InterruptedException e){
            Thread.currentThread().interrupt();
            log.error("Sleep was interrupted", e);
        }
    }
    public static List<WebElement> findElements(By by){
        try {
            return CHROME_DRIVER.findElements(by);
        }catch (Exception e){
            log.error("Could not find elemnt:{}", by, e);
            return new ArrayList<>();
        }
    }
    public static WebElement findElement(By by){
        try {
            return CHROME_DRIVER.findElement(by);
        }catch (Exception e){
            log.error("Could not find elemnt:{}", by, e);
            return null;
        }
    }
    public static void click(By by){
        try {
            CHROME_DRIVER.findElement(by).click();
        }catch (Exception e){
            log.error("Could not find elemnt:{}", by, e);
        }
    }
    public static boolean isCookeValid(String cookiePath){
        return Files.exists(Paths.get(cookiePath));
    }




}
