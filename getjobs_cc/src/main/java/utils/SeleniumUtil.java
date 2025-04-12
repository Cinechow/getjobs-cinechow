package utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SeleniumUtil {
    public static void initDriver(){
        SeleniumUtil.getChromeDriver();
        SeleniumUtil.getActions();
        SeleniumUtil.getWait();
    }

    private static void getWait() {
    }

    private static void getActions() {
    }


}
