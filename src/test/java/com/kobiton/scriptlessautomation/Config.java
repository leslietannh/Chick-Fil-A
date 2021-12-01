package com.kobiton.scriptlessautomation;

import io.appium.java_client.remote.MobileCapabilityType;
import org.apache.commons.codec.binary.Base64;
import org.openqa.selenium.remote.DesiredCapabilities;

public class Config {
    enum DEVICE_SOURCE_ENUMS {KOBITON, SAUCE_LABS}

    public static final String KOBITON_USERNAME = "LeslieTanNH";
    public static final String KOBITON_API_KEY = "1e2f89f9-dbfb-4c49-b431-6d6d8b46a111";
    public static final String APPIUM_SERVER_URL = "https://" + KOBITON_USERNAME + ":" + KOBITON_API_KEY + "@api.kobiton.com/wd/hub";
    public static final DEVICE_SOURCE_ENUMS DEVICE_SOURCE = DEVICE_SOURCE_ENUMS.KOBITON;
    public static final int IMPLICIT_WAIT_IN_SECOND = 30;
    public static final int DEVICE_WAITING_MAX_TRY_TIMES = 5;
    public static final int DEVICE_WAITING_INTERVAL_IN_MS = 30000;
    public static final String KOBITON_API_URL = "https://api.kobiton.com";
    
    public static final String APP_VERSION = "280557";

    public static String getBasicAuthString() {
        String authString = KOBITON_USERNAME + ":" + KOBITON_API_KEY;
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authEncString = new String(authEncBytes);
        return "Basic " + authEncString;
    }
        
    
    public static DesiredCapabilities getGalaxyS8PayloadAndroid9DesiredCapabilities() {
        DesiredCapabilities capabilities = new DesiredCapabilities();
        capabilities.setCapability("sessionName", "Jenkins Automation (every 30 min)");
        capabilities.setCapability("sessionDescription", "Today is 12-1-2021");
        capabilities.setCapability("deviceOrientation", "portrait");
        capabilities.setCapability("noReset", false);
        capabilities.setCapability("fullReset", true);
        capabilities.setCapability("captureScreenshots", true);
        capabilities.setCapability("newCommandTimeout", 15 * 60);
        capabilities.setCapability("ensureWebviewsHavePages", true);
        capabilities.setCapability("kobiton:baselineSessionId", 3186190);
        capabilities.setCapability("kobiton:visualValidation", true);
        capabilities.setCapability("kobiton:textValidation", true);
        capabilities.setCapability(MobileCapabilityType.APP, "kobiton-store:v280557");
        capabilities.setCapability("deviceGroup", "ORGANIZATION");
        capabilities.setCapability(MobileCapabilityType.DEVICE_NAME, "*");
        capabilities.setCapability(MobileCapabilityType.PLATFORM_VERSION, "11");
        capabilities.setCapability(MobileCapabilityType.PLATFORM_NAME, "Android");
        return capabilities;
    }

}
