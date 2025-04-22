package boss;

import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Job;
import utils.SeleniumUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static utils.Constant.CHROME_DRIVER;
import static utils.Constant.WAIT;

public class Boss {
    private static final Logger log  = LoggerFactory.getLogger(Boss.class);
    static boolean EnableNotifacations = false;
    static Integer page = 1;
    static String homeUrl = "https://www.zhipin.com";
    static String baseUrl = "https://www.zhipin.com/web/geek/job?query=%s&experience=%s&city=%s&page=%s";
    static List<String > blackCompanies;
    static List<String > blackRecruiters;
    static List<String> blackJobs;
    static List<Job> returnList = new ArrayList<>();
    static List<String> keywords = List.of("大模型工程师", "AIGC", "Java", "Python", "Golang");
    static String dataPath = "./src/main/java/boss/data.json";
    static String cookiePath = "./src/main/java/boss/cookie.json";
    static final int noJobMaxPage = 5; //无岗位最大页数
    static int noJobPages;
    static int lastSize;
    static Map<String, String> experience = new HashMap<>(){
        {
            put("在校生", "108");
            put("应届生", "102");
            put("经验不限", "101");
            put("1年内", "103");
            put("1-3年", "104");
            put("3-5年", "105");
            put("5-10年", "106");
            put("10年以上", "107");
        }
    };
    static Map<String, String> cityCode = new HashMap<>(){
        {
            put("北京", "101010100");
            put("上海", "101020100");
            put("广州", "101280100");
            put("深圳", "101280600");
            put("杭州", "101210100");
            put("南京", "101190200");
            put("成都", "101270100");
            put("武汉", "101200100");
            put("西安", "101200700");
            put("厦门", "101230200");
        }
    };

    public static void main(String[] args)  {
        loadData(dataPath);
    }

    private static String setYear(List<String> params){
        if (params == null || params.isEmpty()){
            return "";
        }
        return params.stream().map(experience::get).collect(Collectors.joining(","));
    }
    private  static void saveData(String path) {
        try {
            updateListData();
            Map<String, List<String>> data = new HashMap<>();
            data.put("blackCompanies", blackCompanies);
            data.put("blackRecruiters", blackRecruiters);
            data.put("blackJobs", blackJobs);
            String json = customJsonFormat(data);
            Files.write(Paths.get(path), json.getBytes());
        } catch (IOException e) {
            log.error("保存【{}】数据失败", path);
        }
    }


    private static void updateListData() {
        CHROME_DRIVER.get("https://www.zhipin.com/web/geek/chat");
        WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("\"//li[@role='listitem']\"")));
        SeleniumUtil.getWait(3);
        JavascriptExecutor js = CHROME_DRIVER;
        boolean shouldBreak = false;
        while (!shouldBreak){
            try {
                WebElement bottom = CHROME_DRIVER.findElement(By.xpath("//div[@class='finished']"));
                if ("没有更多了".equals(bottom.getText())) {
                    shouldBreak = true;
                }
            } catch (Exception e) {
//                log.info("还未到底");
            }
            List<WebElement> items = CHROME_DRIVER.findElements(By.xpath("//li[@role='listitem']"));
            items.forEach(info -> {
                try {
                    WebElement companyElement = info.findElement(By.xpath(".//span[@class='name-box']//span[2]"));
                    String companyName = companyElement.getText();
                    WebElement messageElement = info.findElement(By.xpath(".//span[@class='last-msg-text']"));
                    String message = messageElement.getText();
                    boolean match = message.contains("不") || message.contains("感谢")|| message.contains("但") || message.contains("遗憾") || message.contains("需要本") || message.contains("对不") ;
                    boolean nomatch = message.contains("不是") || message.contains("不生");
                    if (match && != nomatch){
                        log.info("黑名单公司：【{}】, 信息：【{}】",companyName,message);
                        if (blackCompanies.stream().anyMatch(companyName::contains)){
                            return;
                        }
                        blackCompanies.add(companyName);
                    }
                }catch (Exception e){
//                    log.error("解析数据异常");
                }
            });
            WebElement element = null;
            try {
                WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(text(), '滚动加载更多')]")));
                element = CHROME_DRIVER.findElement(By.xpath("//div[contains(text(), '滚动加载更多')]"));
            } catch (Exception e) {
                log.info("没找到滚动条");
            }
            if (element != null){
                try {
                    js.executeScript("arguments[0].scrollIntoView();",element);
                } catch (Exception e) {
                    log.error("滚到到元素出错", e);
                }
            }else {
                try {
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight())");
                } catch (Exception e) {
                    log.error("滚动到页面底部出错", e);
                }

            }

        }
        log.info("黑名单公司数量：{}", blackCompanies.size());
    }
    private static String customJsonFormat(Map<String, List<String>> data){
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            sb.append("    \"").append(entry.getKey()).append("\":");
            sb.append(entry.getValue().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",", "[", "]")));
            sb.append(",\n");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("\n}");
        return sb.toString();
    }

//    private static String customJsonFormat(Map<String, List<String>> data) {
//        StringBuilder sb = new StringBuilder();
//        sb.append("{\n");
//        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
//            sb.append("    \"").append(entry.getKey()).append("\": ");
//            sb.append(entry.getValue().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(", ", "[", "]")));
//            sb.append(",\n");
//        }
//        sb.delete(sb.length() - 2, sb.length());
//        sb.append("\n}");
//        return sb.toString();
//    }

    private static void loadData(String path) {
        try {
            String json = new String(Files.readAllBytes(Paths.get(path)));
            parseJson(json);
        } catch (IOException e) {
            log.error("读取【{}】数据失败！", path);
        }
    }

    private static void parseJson(String json) {
        JSONObject jsonObject = new JSONObject(json);
        blackCompanies = jsonObject.getJSONArray("blackCompanies").toList()
                .stream().map(Object::toString).collect(Collectors.toList());
        blackRecruiters = jsonObject.getJSONArray("blackRecruiters").toList()
                .stream().map(Object::toString).collect(Collectors.toList());
        blackJobs = jsonObject.getJSONArray("blackJobs").toList()
                .stream().map(Object::toString).collect(Collectors.toList());
    }
    private static Integer resumeSubmission(String url, String keyword){
        CHROME_DRIVER.get(url);
        WAIT.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='job-title clearfix']")));
        List<WebElement> jobCards = CHROME_DRIVER.findElements(By.cssSelector("li.job-card-wrapper"));
        List<Job> jobs = new ArrayList<>();
        for (WebElement jobCard : jobCards) {
            WebElement infoPublic = jobCard.findElement(By.cssSelector("div.info-public"));
            String recruiterText = infoPublic.getText();
            String recruiterName = infoPublic.findElement(By.cssSelector("em")).getText();
            if (blackRecruiters.stream().anyMatch(recruiterName::contains)){
                continue;
            }
            String jobName = jobCard.findElement(By.cssSelector("div.job-title span.job-name")).getText();
            boolean isNotTargetJob = (keyword.contains("大模型") || keyword.contains("AI")) && !jobName.contains("AI") && !jobName.contains("人工智能") && !jobName.contains("大模型") && !jobName.contains("生成");
            if (blackJobs.stream().anyMatch(jobName::contains) || isNotTargetJob){
                continue;
            }
            String companyName = jobCard.findElement(By.cssSelector("div.company-info h3.company-name")).getText();
            if (blackCompanies.stream().anyMatch(companyName::contains)){
                continue;
            }
            Job job = new Job();
            job.setRecruiter(recruiterText.replace(recruiterName, "") + ":" + recruiterName);
            job.setHref(jobCard.findElement(By.cssSelector("a")).getAttribute("href"));
            job.setJobName(jobName);
            job.setJobArea(jobCard.findElement(By.cssSelector("div.job-title span.job-area")).getText());
            job.setSalary(jobCard.findElement(By.cssSelector("div.job-info span.salary")).getText());
            List<WebElement> tagElements = jobCard.findElements(By.cssSelector("div.job-info ul.tag-list li"));
            StringBuilder tag = new StringBuilder();
            for (WebElement tagElement : tagElements) {
                tag.append(tagElement.getText()).append("·");
            }job.setCompanyTag(tag.substring(0, tag.length() - 1));//删除最后一个"·"
            jobs.add(job);
        }
        for (Job job : jobs) {
            //打开新的标签也并打开链接
            JavascriptExecutor jse = CHROME_DRIVER;
            jse.executeScript("window.open(arguments[0], '_blank')", job.getHref());
            //切换到新的标签页
            ArrayList<String> tabs = new ArrayList<>(CHROME_DRIVER.getWindowHandles());
            CHROME_DRIVER.switchTo().window(tabs.get(tabs.size() - 1));
            try {
                WAIT.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='btn btn-startchat']")));
            } catch (Exception e) {
                WebElement element = SeleniumUtil.findElement(By.xpath("//div[@class='error-content']"));
                if (element != null && element.getText().contains("异常访问")) {
                    return -2;
                }
                SeleniumUtil.sleep(1);
                WebElement btn = CHROME_DRIVER.findElement(By.cssSelector("[class*='btn btn-startchat']"));
                if ("立即沟通".equals(btn.getText())){
                    btn.click();
                    if (isLimit()){

                    }
                }
            }

        }
    }

    private static boolean isLimit() {
        try {
            SeleniumUtil.sleep(1);
            String text = CHROME_DRIVER.findElement(By.cssSelector("dialog-con")).getText();
            return text.contains("您今天沟通的次数已达上限");
        } catch (Exception e) {
            return false;
        }
    }
}
