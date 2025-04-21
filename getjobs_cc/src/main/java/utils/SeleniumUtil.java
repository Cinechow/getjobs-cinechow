package utils;

import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.File;
import java.util.Set;

import static utils.Constant.CHROME_DRIVER;
import static utils.Constant.WAIT_TIME;

@Slf4j
public class SeleniumUtil {
    public static void initDriver(){
        SeleniumUtil.getChromeDriver();
        SeleniumUtil.getActions();
        SeleniumUtil.getWait(WAIT_TIME);
    }

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
        }
    }

    private static void getWait(long time) {
    }

    private static void getActions() {
    }


}
