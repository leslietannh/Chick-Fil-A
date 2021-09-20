package com.kobiton.scriptlessautomation;


import io.appium.java_client.MobileBy;
import io.appium.java_client.MobileElement;
import io.appium.java_client.remote.MobileCapabilityType;
import org.testng.Reporter;
import org.openqa.selenium.By;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.ScreenOrientation;
import org.openqa.selenium.remote.DesiredCapabilities;


public class TestApp extends TestBase {
    public void runTest() throws Exception {
        try {
            updateSettings();
            switchToNativeContext();
            setImplicitWaitInSecond(Config.IMPLICIT_WAIT_IN_SECOND);
            
            setCurrentCommandId(761792094);
            driver.findElement(MobileBy.id("com.chickfila.cfaflagship:id/splash_view_start_button")).click();

            setCurrentCommandId(761792145);
            driver.findElement(MobileBy.id("com.chickfila.cfaflagship:id/alert_content_right_btn")).click();
            
            setCurrentCommandId(761792177);
            driver.findElement(MobileBy.id("com.android.packageinstaller:id/permission_allow_button")).click();
            
            setCurrentCommandId(761792276);
            driver.findElement(By.xpath("/hierarchy/android.widget.FrameLayout/android.view.ViewGroup/androidx.viewpager.widget.ViewPager/android.view.ViewGroup[2]/android.view.ViewGroup/android.widget.ScrollView/android.view.ViewGroup/android.widget.Button")).click();
            
            setCurrentCommandId(761792362);
            driver.findElement(By.xpath("//android.widget.TextView[@text='Drive-Thru']")).click();
            
            setCurrentCommandId(761792401);
            driver.findElement(MobileBy.id("com.chickfila.cfaflagship:id/cta_btn")).click();
            
            setCurrentCommandId(761792465);
            By locatorLinearLayout2 = MobileBy.xpath("/hierarchy/android.widget.FrameLayout/android.view.ViewGroup[2]/androidx.viewpager.widget.ViewPager/androidx.recyclerview.widget.RecyclerView/android.widget.LinearLayout[2]");
            touchOnScrollableElement(locatorLinearLayout2, "761792465");
            
            setCurrentCommandId(761793581);
            By locatorLinearLayout21 = MobileBy.xpath("/hierarchy/android.widget.FrameLayout/android.view.ViewGroup[2]/android.widget.ScrollView/androidx.recyclerview.widget.RecyclerView/android.widget.LinearLayout[2]");
            touchOnScrollableElement(locatorLinearLayout21, "761793581");
            
            setCurrentCommandId(761793652);
            driver.findElement(MobileBy.id("com.chickfila.cfaflagship:id/add_meal_combo_item_button")).click();
            
            setCurrentCommandId(761793705);
            driver.findElement(By.xpath("//android.widget.TextView[@text='Chick-fil-A Waffle Potato Fries®']")).click();
                      
            setCurrentCommandId(761793767);
            driver.findElement(MobileBy.id("com.chickfila.cfaflagship:id/add_meal_combo_item_button")).click();
            
            setCurrentCommandId(761793830);
            By locatorViewGroup2 = MobileBy.xpath("/hierarchy/android.widget.FrameLayout/android.view.ViewGroup/android.view.ViewGroup/androidx.viewpager.widget.ViewPager/androidx.recyclerview.widget.RecyclerView/android.view.ViewGroup[2]");
            touchOnScrollableElement(locatorViewGroup2, "761793830");
            
            setCurrentCommandId(761793902);
            driver.findElement(MobileBy.id("com.chickfila.cfaflagship:id/add_meal_combo_item_button")).click();
                        
            setCurrentCommandId(761793967);
            driver.findElement(MobileBy.id("com.chickfila.cfaflagship:id/view_order_item_btn")).click();
            
            setCurrentCommandId(761794045);
            By locatorMyOrderTab5Of5 = MobileBy.AccessibilityId("My Order tab 5 of 5");
            MobileElement elementMyOrderTab5Of5 = findElementBy(locatorMyOrderTab5Of5);
            hideKeyboard();
            touchAtRelativePointOfElement(elementMyOrderTab5Of5, 0.56019, 0.41071);
            
            setCurrentCommandId(761794101);
            sleep(4979);
            press(PRESS_TYPES.HOME);

        } catch (Exception e) {
            saveDebugResource();
            e.printStackTrace();
            throw e;
        } finally {
            cleanup();
        }
    }

    @Override
    public void setup(DesiredCapabilities desiredCaps, double retinaScale) throws Exception {
        super.setup(desiredCaps, retinaScale);
        Reporter.log(String.format("View session at: https://portal.kobiton.com/sessions/%s", getKobitonSessionId()));
    }
}
