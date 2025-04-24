package boss;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.SneakyThrows;
import org.json.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.Job;
import utils.SeleniumUtil;
import utils.TelegramNotificationBot;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static utils.Constant.*;

public class Boss {
    private static final Logger log  = LoggerFactory.getLogger(Boss.class);//创建日志记录器实例
    static boolean EnableNotifacations = false;//是否启用通知，默认关闭
    static Integer page = 1;//当前页码，默认第一页
    static String homeUrl = "https://www.zhipin.com";//网站url
    static String baseUrl = "https://www.zhipin.com/web/geek/job?query=%s&experience=%s&city=%s&page=%s";//职位搜索url
    static List<String > blackCompanies;//黑名单公司列表
    static List<String > blackRecruiters;//黑名单招聘者列表
    static List<String> blackJobs;//黑名单职位列表
    static List<Job> returnList = new ArrayList<>();//返回的职位列表
    static List<String> keywords = List.of("大模型工程师", "AIGC", "Java", "Python", "Golang");//关键词列表
    static String dataPath = "./src/main/java/boss/data.json";//数据文件路径
    static String cookiePath = "./src/main/java/boss/cookie.json";//cookie文件路径
    static final int noJobMaxPage = 5; //无岗位最大页数
    static int noJobPages;//当前无岗位页数
    static int lastSize;//上一次列表大小
    static Map<String, String> experience = new HashMap<>(){//经验映射表
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
    static Map<String, String> cityCode = new HashMap<>(){//城市编码映射表
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
        // 从指定路径加载数据（黑名单公司、招聘者、职位等）
        loadData(dataPath);
        // 初始化 Selenium WebDriver
        SeleniumUtil.initDriver();
        // 记录开始时间
        Date start = new Date();
        // 登录 Boss 直聘
        login();
        // 标签用于跳出多重循环
        endSubmission:
        // 遍历关键词列表
        for (String keyword : keywords){
            // 初始化页码、无岗位页数和上一次列表大小
            page = 1;
            noJobPages = 0;
            lastSize = -1;
            // 循环处理每一页的职位
            while (true) {
                // 记录当前处理的关键词和页码
                log.info("投递【{}】关键词第【{}】页", keyword, page);
                // 构造职位搜索 URL
                String url = String.format(baseUrl, keyword, setYear(List.of()),cityCode.get("厦门"),page);
                // 获取当前返回的职位列表大小
                int startSize = returnList.size();
                // 执行简历投递并获取结果
                Integer resultSize = resumeSubmission(url, keyword);
                // 如果返回 -1，表示今日沟通人数已达上限，跳出多重循环
                if (resultSize == -1){
                    log.info("今日沟通人数已达上限，请明天再试");
                    break endSubmission;
                }
                // 如果返回 -2，表示出现异常访问，跳出多重循环
                if (resultSize == -2) {
                    log.info("出现异常访问，请手动过验证后再继续投递...");
                    break endSubmission;
                }
                // 如果当前页无新岗位
                if (resultSize == startSize){
                    // 增加无岗位页数计数
                    noJobPages ++;
                    // 如果连续无岗位页数达到最大值，结束该关键词的投递
                    if (noJobPages >= noJobMaxPage){
                        log.info("【{}】关键词一连续【{}】页无岗位，结束该关键词的投递...", keyword, noJobMaxPage);
                        break ;
                    }else {
                        // 否则记录当前无岗位页数
                        log.info("【{}】关键词第【{}】页无岗位,目前已连续【{}】页无新岗位...", keyword, page, noJobPages);
                    }
                }else {
                    // 更新上一次列表大小和无岗位页数计数
                    lastSize = resultSize;
                    noJobPages = 0;
                }
                // 增加页码
                page ++;
            }
        }
        // 记录结束时间
        Date end = new Date();
        // 如果返回的职位列表为空，记录未发起新的聊天；否则记录新发起聊天的公司列表
        log.info(returnList.isEmpty() ? "未发起新的聊天..." : "新发起聊天公司如下:\n{}", returnList.stream().map(Object::toString).collect(Collectors.joining("\n")));
        // 计算总耗时
        long durationSeconds = (end.getTime() - start.getTime()) / 1000;
        long minutes = durationSeconds / 60;
        long seconds = durationSeconds % 60;
        // 构造消息内容
        String message = "共发起" + returnList.size() + "次聊天，用时" + minutes + "分" + seconds + "秒";
        // 如果启用了通知功能，发送包含列表的消息
        if (EnableNotifacations){
            new TelegramNotificationBot().sendMessageWithList(message, returnList.stream().map(Job::toString).toList(), "Boss直聘投递");

        }
        // 记录消息内容
        log.info(message);
        //保存黑名单数据
        saveData(dataPath);
        // 关闭 WebDriver
        CHROME_DRIVER.close();
        CHROME_DRIVER.quit();

    }

    // 定义 setYear 方法，用于将经验参数列表转换为对应的编码字符串
    private static String setYear(List<String> params){
        // 检查参数列表是否为空或为空列表
        if (params == null || params.isEmpty()){
            // 如果为空，返回空字符串
            return "";
        }
        // 使用流操作将参数列表中的每个经验值映射为对应的编码值，并用逗号连接成字符串
        return params.stream().map(experience::get).collect(Collectors.joining(","));
    }
    private  static void saveData(String path) {
        try {
            // 调用 updateListData 方法更新黑名单数据
            updateListData();
            // 创建一个 Map 对象，用于存储黑名单数据
            Map<String, List<String>> data = new HashMap<>();
            // 将黑名单公司列表添加到 Map 中
            data.put("blackCompanies", blackCompanies);
            // 将黑名单招聘者列表添加到 Map 中
            data.put("blackRecruiters", blackRecruiters);
            // 将黑名单职位列表添加到 Map 中
            data.put("blackJobs", blackJobs);
            // 调用 customJsonFormat 方法将 Map 转换为 JSON 格式的字符串
            String json = customJsonFormat(data);
            // 将 JSON 字符串写入指定路径的文件中
            Files.write(Paths.get(path), json.getBytes());
        } catch (IOException e) {
            // 捕获并记录保存数据失败的错误日志
            log.error("保存【{}】数据失败", path);
        }
    }


    private static void updateListData() {
        // 打开 Boss 直聘的聊天页面
        CHROME_DRIVER.get("https://www.zhipin.com/web/geek/chat");
        // 等待页面加载完成，直到找到指定的列表项元素
        WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("\"//li[@role='listitem']\"")));
        // 暂停 3 秒，确保页面完全加载
        SeleniumUtil.getWait(3);
        // 获取 JavaScript 执行器，用于滚动页面
        JavascriptExecutor js = CHROME_DRIVER;
        // 定义一个布尔变量，用于控制循环退出
        boolean shouldBreak = false;
        // 循环遍历聊天记录，直到找到“没有更多了”或加载完成
        while (!shouldBreak) {
            try {
                // 尝试找到“没有更多了”的提示元素
                WebElement bottom = CHROME_DRIVER.findElement(By.xpath("//div[@class='finished']"));
                // 如果找到提示文本为“没有更多了”，则退出循环
                if ("没有更多了".equals(bottom.getText())) {
                    shouldBreak = true;
                }
            } catch (Exception e) {
                // 如果未找到提示元素，继续加载更多数据
//            log.info("还未到底");
            }
            // 查找当前页面的所有聊天记录项
            List<WebElement> items = CHROME_DRIVER.findElements(By.xpath("//li[@role='listitem']"));
            // 遍历每个聊天记录项
            items.forEach(info -> {
                try {
                    // 获取公司名称元素并提取文本
                    WebElement companyElement = info.findElement(By.xpath(".//span[@class='name-box']//span[2]"));
                    String companyName = companyElement.getText();
                    // 获取最后一条消息元素并提取文本
                    WebElement messageElement = info.findElement(By.xpath(".//span[@class='last-msg-text']"));
                    String message = messageElement.getText();
                    // 判断消息是否包含特定关键词（如“不”、“感谢”等），但排除“不是”和“不生”
                    boolean match = message.contains("不") || message.contains("感谢") || message.contains("但") || message.contains("遗憾") || message.contains("需要本") || message.contains("对不");
                    boolean nomatch = message.contains("不是") || message.contains("不生");
                    if (match && !nomatch) {
                        // 如果匹配成功，记录公司名称和消息内容到日志
                        log.info("黑名单公司：【{}】, 信息：【{}】", companyName, message);
                        // 检查公司名称是否已存在于黑名单中，如果存在则跳过
                        if (blackCompanies.stream().anyMatch(companyName::contains)) {
                            return;
                        }
                        // 将公司名称添加到黑名单列表中
                        blackCompanies.add(companyName);
                    }
                } catch (Exception e) {
                    // 忽略解析数据时的异常
//                log.error("解析数据异常");
                }
            });
            // 尝试找到“滚动加载更多”的提示元素
            WebElement element = null;
            try {
                WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[contains(text(), '滚动加载更多')]")));
                element = CHROME_DRIVER.findElement(By.xpath("//div[contains(text(), '滚动加载更多')]"));
            } catch (Exception e) {
                // 如果未找到提示元素，记录日志
                log.info("没找到滚动条");
            }
            // 如果找到了“滚动加载更多”的提示元素
            if (element != null) {
                try {
                    // 使用 JavaScript 将滚动条滚动到该元素位置
                    js.executeScript("arguments[0].scrollIntoView();", element);
                } catch (Exception e) {
                    // 捕获并记录滚动到元素时的异常
                    log.error("滚到到元素出错", e);
                }
            } else {
                try {
                    // 如果未找到提示元素，滚动到页面底部
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight())");
                } catch (Exception e) {
                    // 捕获并记录滚动到页面底部时的异常
                    log.error("滚动到页面底部出错", e);
                }
            }
        }
        // 记录黑名单公司数量到日志
        log.info("黑名单公司数量：{}", blackCompanies.size());
    }
//    private static String customJsonFormat(Map<String, List<String>> data) {
//        // 创建一个 StringBuilder 对象，用于构建 JSON 格式的字符串
//        StringBuilder sb = new StringBuilder();
//        // 添加 JSON 对象的起始大括号
//        sb.append("{\n");
//        // 遍历传入的 Map，将键值对转换为 JSON 格式的字符串
//        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
//            // 添加键名，并用双引号包裹
//            sb.append("    \"").append(entry.getKey()).append("\":");
//            // 将 List 转换为 JSON 数组格式的字符串，每个元素用双引号包裹
//            sb.append(entry.getValue().stream().map(s -> "\"" + s + "\"").collect(Collectors.joining(",", "[", "]")));
//            // 添加逗号和换行符，准备下一行数据
//            sb.append(",\n");
//        }
//        // 删除最后一个逗号和换行符，避免 JSON 格式错误
//        sb.delete(sb.length() - 2, sb.length());
//        // 添加 JSON 对象的结束大括号
//        sb.append("\n}");
//        // 返回构建好的 JSON 字符串
//        return sb.toString();
//{
//    "blackCompanies":["公司A","公司B"],
//    "blackRecruiters":["招聘者A","招聘者B"],
//    "blackJobs":["职位A","职位B"]
//}

    //    }
    private static String customJsonFormat(Map<String, List<String>> data) {
        // 创建 Gson 对象，并启用 pretty printing 以美化 JSON 输出
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        // 使用 Gson 将 Map 转换为 JSON 字符串
        return gson.toJson(data);
        //{
        //  "blackCompanies": [
        //    "公司A",
        //    "公司B"
        //  ],
        //  "blackRecruiters": [
        //    "招聘者A",
        //    "招聘者B"
        //  ],
        //  "blackJobs": [
        //    "职位A",
        //    "职位B"
        //  ]
        //}
    }

    private static void loadData(String path) {
        try {
            // 从指定路径读取文件内容为字符串
            String json = new String(Files.readAllBytes(Paths.get(path)));
            // 调用 parseJson 方法解析 JSON 数据并更新黑名单数据
            parseJson(json);
        } catch (IOException e) {
            // 捕获并记录读取数据失败的错误日志
            log.error("读取【{}】数据失败！", path);
        }
    }

    private static void parseJson(String json) {
        // 将传入的 JSON 字符串解析为 JSONObject 对象
        JSONObject jsonObject = new JSONObject(json);
        // 从 JSONObject 中获取 "blackCompanies" 数组，并将其转换为 List<String>
        blackCompanies = jsonObject.getJSONArray("blackCompanies").toList()
                .stream().map(Object::toString).collect(Collectors.toList());
        // 从 JSONObject 中获取 "blackRecruiters" 数组，并将其转换为 List<String>
        blackRecruiters = jsonObject.getJSONArray("blackRecruiters").toList()
                .stream().map(Object::toString).collect(Collectors.toList());
        // 从 JSONObject 中获取 "blackJobs" 数组，并将其转换为 List<String>
        blackJobs = jsonObject.getJSONArray("blackJobs").toList()
                .stream().map(Object::toString).collect(Collectors.toList());
    }

    /**
     * 恢复简历投递的方法，根据给定的 URL 和关键词筛选岗位并进行投递操作
     * @param url 招聘页面的 URL
     * @param keyword 搜索岗位的关键词
     * @return 成功投递的岗位数量，若出现异常访问返回 -2，若达到投递限制返回 -1
     */
    private static Integer resumeSubmission(String url, String keyword) {
        // 打开指定的招聘页面
        CHROME_DRIVER.get(url);
        // 等待页面上符合条件的岗位标题元素加载完成
        WAIT.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='job-title clearfix']")));
        // 查找页面上所有的岗位卡片元素
        List<WebElement> jobCards = CHROME_DRIVER.findElements(By.cssSelector("li.job-card-wrapper"));
        // 用于存储筛选后的岗位信息
        List<Job> jobs = new ArrayList<>();
        // 遍历每个岗位卡片元素
        for (WebElement jobCard : jobCards) {
            // 查找岗位卡片中的招聘信息元素
            WebElement infoPublic = jobCard.findElement(By.cssSelector("div.info-public"));
            // 获取招聘信息的文本内容
            String recruiterText = infoPublic.getText();
            // 获取招聘者的姓名
            String recruiterName = infoPublic.findElement(By.cssSelector("em")).getText();
            // 检查招聘者是否在黑名单中，如果在则跳过该岗位
            if (blackRecruiters.stream().anyMatch(recruiterName::contains)) {
                continue;
            }
            // 获取岗位名称
            String jobName = jobCard.findElement(By.cssSelector("div.job-title span.job-name")).getText();
            // 判断是否为目标岗位，若关键词包含特定词汇且岗位名称不包含相关词汇则不是目标岗位
            boolean isNotTargetJob = (keyword.contains("大模型") || keyword.contains("AI")) && !jobName.contains("AI") && !jobName.contains("人工智能") && !jobName.contains("大模型") && !jobName.contains("生成");
            // 检查岗位名称是否在黑名单中或者是否不是目标岗位，如果是则跳过该岗位
            if (blackJobs.stream().anyMatch(jobName::contains) || isNotTargetJob) {
                continue;
            }
            // 获取公司名称
            String companyName = jobCard.findElement(By.cssSelector("div.company-info h3.company-name")).getText();
            // 检查公司是否在黑名单中，如果在则跳过该岗位
            if (blackCompanies.stream().anyMatch(companyName::contains)) {
                continue;
            }
            // 创建一个新的岗位对象
            Job job = new Job();
            // 设置招聘者信息，格式为招聘信息去除招聘者姓名后加上招聘者姓名
            job.setRecruiter(recruiterText.replace(recruiterName, "") + ":" + recruiterName);
            // 设置岗位的链接
            job.setHref(jobCard.findElement(By.cssSelector("a")).getAttribute("href"));
            // 设置岗位名称
            job.setJobName(jobName);
            // 设置岗位所在地区
            job.setJobArea(jobCard.findElement(By.cssSelector("div.job-title span.job-area")).getText());
            // 设置岗位薪资
            job.setSalary(jobCard.findElement(By.cssSelector("div.job-info span.salary")).getText());
            // 查找岗位标签元素列表
            List<WebElement> tagElements = jobCard.findElements(By.cssSelector("div.job-info ul.tag-list li"));
            // 用于拼接岗位标签
            StringBuilder tag = new StringBuilder();
            // 遍历每个岗位标签元素
            for (WebElement tagElement : tagElements) {
                // 将标签文本添加到 StringBuilder 中，并在后面添加分隔符
                tag.append(tagElement.getText()).append("·");
            }
            // 删除最后一个分隔符
            job.setCompanyTag(tag.substring(0, tag.length() - 1));
            // 将筛选后的岗位添加到岗位列表中
            jobs.add(job);
        }
        // 遍历筛选后的岗位列表
        for (Job job : jobs) {
            // 使用 JavaScript 在新标签页中打开岗位链接
            JavascriptExecutor jse = CHROME_DRIVER;
            jse.executeScript("window.open(arguments[0], '_blank')", job.getHref());
            // 获取所有打开的标签页的句柄
            ArrayList<String> tabs = new ArrayList<>(CHROME_DRIVER.getWindowHandles());
            // 切换到新打开的标签页
            CHROME_DRIVER.switchTo().window(tabs.get(tabs.size() - 1));
            try {
                // 等待立即沟通按钮元素加载完成
                WAIT.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("[class*='btn btn-startchat']")));
            } catch (Exception e) {
                // 查找异常访问提示元素
                WebElement element = SeleniumUtil.findElement(By.xpath("//div[@class='error-content']"));
                // 若存在异常访问提示，则返回 -2
                if (element != null && element.getText().contains("异常访问")) {
                    return -2;
                }
            }
            // 线程休眠 1 秒
            SeleniumUtil.sleep(1);
            // 查找立即沟通按钮元素
            WebElement btn = CHROME_DRIVER.findElement(By.cssSelector("[class*='btn btn-startchat']"));
            // 若按钮文本为立即沟通，则点击按钮
            if ("立即沟通".equals(btn.getText())) {
                btn.click();
                // 检查是否达到投递限制
                if (isLimit()) {
                    // 线程休眠 1 秒
                    SeleniumUtil.sleep(1);
                    // 若达到投递限制，则返回 -1
                    return -1;
                }
                try {
                    // 等待聊天输入框元素加载完成
                    WebElement input = WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@id='chat-input']")));
                    // 点击聊天输入框
                    input.click();
                    // 线程休眠 500 毫秒
                    SeleniumUtil.sleepByMilliSeconds(500);
                    try {
                        // 查找不匹配对话框元素
                        WebElement element = CHROME_DRIVER.findElement(By.xpath("//div[@class='dialog-container']"));
                        // 若对话框文本为不匹配，则关闭当前标签页并切换回第一个标签页
                        if ("不匹配".equals(element.getText())) {
                            CHROME_DRIVER.close();
                            CHROME_DRIVER.switchTo().window(tabs.get(0));
                            continue;
                        }
                    } catch (Exception ex) {
                        // 若未出现不匹配对话框，则记录日志表示岗位匹配，下一步发送消息
                        log.debug("岗位匹配，下一步发送消息");
                    }
                    // 向聊天输入框中输入打招呼的内容
                    input.sendKeys(SAY_HI);
                    // 等待发送按钮元素加载完成
                    WebElement send = WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//button[@type='send']")));
                    // 点击发送按钮
                    send.click();
                    // 查找招聘者姓名元素
                    WebElement recruiterNameElement = CHROME_DRIVER.findElement(By.xpath("//p[@class='base-info fl']/span[@class='name']"));
                    // 查找招聘者职位元素
                    WebElement recruiterTitleElement = CHROME_DRIVER.findElement(By.xpath("//p[@class='base-info fl']/span[@class='base-title']"));
                    // 拼接招聘者信息
                    String recruiter = recruiterNameElement.getText() + " " + recruiterTitleElement.getText();
                    WebElement companyElement;
                    try {
                        // 查找公司名称元素
                        companyElement = CHROME_DRIVER.findElement(By.xpath("//p[@class='base-info fl']/span[not(@class)]"));
                    } catch (Exception e) {
                        // 若查找失败，则等待元素加载完成后再次查找
                        WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//p[@class='base-info fl']/span[not(@class)]")));
                        companyElement = CHROME_DRIVER.findElement(By.xpath("//p[@class='base-info fl']/span[not(@class)]"));
                    }
                    String company = null;
                    // 若找到公司名称元素，则获取其文本内容并设置到岗位对象中
                    if (companyElement != null) {
                        company = companyElement.getText();
                        job.setCompanyName(company);
                    }
                    // 查找岗位名称元素
                    WebElement positionNameElement = CHROME_DRIVER.findElement(By.xpath("//a[@class='position-content']/span[@class='position-name']"));
                    // 查找岗位薪资元素
                    WebElement salaryElement = CHROME_DRIVER.findElement(By.xpath("//a[@class='position-content']/span[@class='salary']"));
                    // 查找岗位所在城市元素
                    WebElement cityElement = CHROME_DRIVER.findElement(By.xpath("//a[@class='position-content']/span[@class='city']"));
                    // 拼接岗位信息
                    String position = positionNameElement.getText() + " " + salaryElement.getText() + " " + cityElement.getText();
                    // 记录投递信息
                    log.info("投递【{}】公司，【{}】职位，招聘官【{}】", company == null ? "未知公司" + " " + job.getHref() : company, position, recruiter);
                    // 将成功投递的岗位添加到返回列表中
                    returnList.add(job);
                    // 重置无岗位页面计数器
                    noJobPages = 0;
                } catch (Exception ex) {
                    // 若发送消息失败，则记录错误日志
                    log.error("发送消息失败：{}", ex.getMessage());
                }
            }
            // 线程休眠 1 秒
            SeleniumUtil.sleep(1);
            // 关闭当前标签页
            CHROME_DRIVER.close();
            // 切换回第一个标签页
            CHROME_DRIVER.switchTo().window(tabs.get(0));
        }
        // 返回成功投递的岗位数量
        return returnList.size();
    }

    private static boolean isLimit() {
        try {
            // 暂停 1 秒，确保页面加载完成
            SeleniumUtil.sleep(1);
            // 尝试获取对话框容器中的文本内容
            String text = CHROME_DRIVER.findElement(By.cssSelector("dialog-con")).getText();
            // 判断文本中是否包含“您今天沟通的次数已达上限”的提示信息
            return text.contains("您今天沟通的次数已达上限");
        } catch (Exception e) {
            // 如果捕获到异常（例如元素未找到），返回 false 表示未达到沟通次数上限
            return false;
        }
    }

    /**
     * 登录方法，用于初始化浏览器并加载 Cookie 或进行扫码登录。
     */
    @SneakyThrows
    private static void login(){
        // 记录日志，表示正在打开 Boss 直聘网站
        log.info("打开Boss直聘网站中");
        // 打开 Boss 直聘的首页
        CHROME_DRIVER.get(homeUrl);
        // 检查 Cookie 是否有效
        if (SeleniumUtil.isCookeValid(cookiePath)){
            // 如果 Cookie 有效，加载 Cookie 并刷新页面
            SeleniumUtil.lodCookie(cookiePath);
            CHROME_DRIVER.navigate().refresh();
            SeleniumUtil.sleep(2);
        }
        // 检查是否需要登录
        if (isLoginRequired()){
            // 如果需要登录，记录日志并调用扫码登录方法
            log.error("cookie失效，尝试扫码登录");
            scanLogin();
        }
    }

    /**
     * 扫码登录方法，用于通过扫码完成登录操作。
     */
    @SneakyThrows
    private static void scanLogin() {
        // 打开 Boss 直聘的登录页面
        CHROME_DRIVER.get(homeUrl + "/web/user/?ka=header-login");
        // 记录日志，表示正在等待登录
        log.info("等待登录..");
        // 等待页面加载完成，直到找到指定的元素
        WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//a[@ka='header-home-logo']")));
        // 定义一个布尔变量，用于控制登录循环
        boolean login = false;
        while (!login){
            try {
                // 等待页面加载完成，直到找到指定的元素
                WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"header\"]/div[1]/div[1]/a")));
                WAIT.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//*[@id=\"wrap\"]/div[2]/div[1]/div/div[1]/a[2]")));
                // 如果找到元素，表示登录成功
                login = true;
                log.info("登陆成功！保存cookie...");
            }catch (Exception e){
                // 如果登录失败，记录日志并等待两秒后重试
                log.error("登陆失败，两秒后重试...");
            }finally {
                // 无论成功或失败，暂停两秒
                SeleniumUtil.sleep(2);
            }
        }
        // 登录成功后，保存 Cookie
        SeleniumUtil.saveCookie(cookiePath);
    }

    /**
     * 检查是否需要登录的方法。
     * @return 如果需要登录返回 true，否则返回 false。
     */
    public static boolean isLoginRequired() {
        try {
            // 尝试获取页面上的文本内容
            String text = CHROME_DRIVER.findElement(By.xpath("btns")).getText();
            // 如果文本包含“登录”，表示需要登录
            return text !=null && text.contains("登录");
        } catch (Exception e) {
            // 如果捕获到异常（例如元素未找到），记录日志并返回 false
            log.info("cook有效，已登录...");
            return false;
        }
    }

}
