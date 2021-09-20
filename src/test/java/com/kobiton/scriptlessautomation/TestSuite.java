package com.kobiton.scriptlessautomation;

import org.openqa.selenium.remote.DesiredCapabilities;
import org.testng.annotations.Test;

public class TestSuite {
    
    @Test
    public void testOnGalaxyS8PayloadAndroid9() throws Exception {
        TestApp testApp = new TestApp();
        DesiredCapabilities capabilities = Config.getGalaxyS8PayloadAndroid9DesiredCapabilities();
        testApp.findOnlineDevice(capabilities);
        testApp.setup(capabilities, 1);
        testApp.runTest();
    }

}
