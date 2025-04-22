package utils;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
//适用slf4j注解生成日志记录器
@Slf4j
public class TelegramNotificationBot{
    //主方法，程序入口点
    public static void main(String[] args){
        //打印空行
        System.out.println();
    }
    //定义Telegram API 令牌常量，需要替换为实际的api令牌
    private static final String TELEGRAM_API_TOKEN = "";
    //定义聊天ID常量，需要替换为实际的聊天Id
    private static final long CHAT_ID = 1L;
    //发送普通消息的方法
    public void sendMessage(String message, String title){
        //创建TelegramBOT实例，使用TELEGRAM_API_TOKEN 进行初始化
        TelegramBot bot = new TelegramBot(TELEGRAM_API_TOKEN);
        //构建消息文本含标题和消息内容
        String messageText = "*【" + title + "】*\n\n" + message;
        //创建SendMessage对象，设置消息内容和解析模式为Markdown
        SendMessage sendMessageRequest =
                new SendMessage(CHAT_ID, messageText).parseMode(ParseMode.Markdown);
        //使用bot实例执行发送消息的请求
        bot.execute(sendMessageRequest);
    }
    //发送包含列表消息
    public void sendMessageWithList(String message, List<String> list, String title){
        //创建StringBuilder 对象， 用于构建消息内容
        StringBuilder builder = new StringBuilder();
        //遍历列表中的每个字符串，并将其添加到StringBuilde中，每个字符串后跟一个换行符
        for (String s : list) {
            builder.append(s).append("\n");
        }
        //将StringBuilder转换wield字符串，去除前后空白字符，然后在末尾天机两个换行符
        String trim = builder.toString().trim() + "\n\n";
        //创建TelleGramBOt实例，适用TELEGRAM)API_TOKEN进行初始化
        TelegramBot bot = new TelegramBot(TELEGRAM_API_TOKEN);
        //构建消息文本，包含标题、列表内容、消息内容
        String messageText = "*【" + title + "】*\n\n" + trim + message;
        //创建SendMessage对象，设置消息内容和解析模式为MarkDOwn
        SendMessage sendMessageRequest =
                new SendMessage(CHAT_ID, messageText).parseMode(ParseMode.Markdown);
        //适用bot实例执行发送消息请求
        bot.execute(sendMessageRequest);
    }
    public void sendWarningMessage(String message, String title){
        TelegramBot bot = new TelegramBot(TELEGRAM_API_TOKEN);
        String messageText = "*⚠️WARNING:*\n*【" + title + "】*\n\n" + message;
        SendMessage sendMessageRequest = new SendMessage(CHAT_ID, messageText).parseMode(ParseMode.Markdown);
        bot.execute(sendMessageRequest);
    }
    public void sendWarningMessageWithList(String message, List<String> list, String title){
        StringBuilder builder = new StringBuilder();
        for (String s : list) {
            builder.append(s).append("\n");
        }
        String tirm = builder.toString().trim() + "\n\n";
        TelegramBot bot = new TelegramBot(TELEGRAM_API_TOKEN);
        String messageText = "*⚠️WARNING:*\\n*【\" + title + \"】*\\n\\n" + tirm + message;
        SendMessage sendMessageRequest =
                new SendMessage(CHAT_ID, messageText).parseMode(ParseMode.Markdown);
        bot.execute(sendMessageRequest);
    }
}
