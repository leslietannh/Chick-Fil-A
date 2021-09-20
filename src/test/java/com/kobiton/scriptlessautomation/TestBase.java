package com.kobiton.scriptlessautomation;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import io.appium.java_client.*;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.ios.IOSDriver;
import io.appium.java_client.remote.MobileCapabilityType;
import io.appium.java_client.remote.MobilePlatform;
import io.appium.java_client.touch.TapOptions;
import io.appium.java_client.touch.WaitOptions;
import io.appium.java_client.touch.offset.ElementOption;
import io.appium.java_client.touch.offset.PointOption;
import okhttp3.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.http.HttpHeaders;
import org.apache.http.client.utils.URIBuilder;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.KeyInput;
import org.openqa.selenium.interactions.Pause;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import javax.annotation.Nullable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TestBase {
    public AppiumDriver<MobileElement> driver;
    public ProxyServer proxy;
    public OtpService otpService = new OtpService();
    public DesiredCapabilities desiredCaps;
    public boolean isIos;
    public Point screenSize;
    public double retinaScale;
    public String deviceName, platformVersion;

    public static String IOS_XPATH_REDUNDANT_PREFIX = "/AppiumAUT";
    public static String NATIVE_CONTEXT = "NATIVE_APP";

    enum PRESS_TYPES {HOME, BACK, POWER, APP_SWITCH, ENTER, DELETE}

    public Gson gson = new Gson();
    public final OkHttpClient httpClient = new OkHttpClient();

    public void setup(DesiredCapabilities desiredCaps, double retinaScale) throws Exception {
        this.desiredCaps = desiredCaps;
        this.retinaScale = retinaScale;
        this.isIos =  MobilePlatform.IOS.equalsIgnoreCase(
                (String) desiredCaps.getCapability(MobileCapabilityType.PLATFORM_NAME));
        this.deviceName = (String) desiredCaps.getCapability(MobileCapabilityType.DEVICE_NAME);
        this.platformVersion = (String) desiredCaps.getCapability(MobileCapabilityType.PLATFORM_VERSION);

        if (Config.DEVICE_SOURCE == Config.DEVICE_SOURCE_ENUMS.KOBITON) {
            this.proxy = new ProxyServer();
        }

        URL appiumServerUrl = getAppiumServerUrl();
        if (isIos) {
            driver = new IOSDriver<>(appiumServerUrl, desiredCaps);
        } else {
            driver = new AndroidDriver<>(appiumServerUrl, desiredCaps);
        }
    }

    public void cleanup() {
        if (driver != null) {
            driver.quit();
        }

        if (proxy != null && proxy.isAlive()) {
            proxy.stop();
        }

        if (otpService != null) {
            otpService.cleanup();
        }
    }

    public void switchContext(String context) {
        System.out.println(String.format("Switch to %s context", context));
        driver.context(context);
    }

    public void switchToNativeContext() {
        String currentContext = driver.getContext();
        if (NATIVE_CONTEXT.equals(currentContext)) {
            return;
        }

        switchContext(NATIVE_CONTEXT);
    }

    public String switchToWebContext() throws Exception {
        String currentContext = driver.getContext();
        if (!NATIVE_CONTEXT.equals(currentContext)) {
            return currentContext;
        }

        sleep(3000);
        String bestWebContext = null;
        int maxSourceLengthInWebContext = 0;
        for (int tryTime = 1; tryTime <= 3; tryTime++) {
            System.out.println(String.format("Find a web context, %s time", Utils.convertToOrdinal(tryTime)));
            Set<String> contexts = driver.getContextHandles();
            for (String context : contexts) {
                if (context.startsWith("WEBVIEW") || context.equals("CHROMIUM")) {
                    driver.context(context);
                    String source = driver.getPageSource();
                    if (source == null) {
                        continue;
                    }

                    if (source.length() > maxSourceLengthInWebContext) {
                        bestWebContext = context;
                        maxSourceLengthInWebContext = source.length();
                    }
                }
            }

            if (bestWebContext != null) {
                driver.context(bestWebContext);
                System.out.println(String.format("Switched to %s web context successfully", bestWebContext));
                return bestWebContext;
            }

            Thread.sleep(10000);
        }

        throw new Exception("Cannot find any web context");
    }

    public ImmutablePair<String, String> getLocatorInJson(By locator) throws Exception {
        String locatorValue, locatorType;
        Class locatorClass = locator.getClass();
        if (locatorClass.equals(By.ById.class)) {
            locatorType = "id";
            locatorValue = (String) FieldUtils.readField(locator, "id", true);
        } else if (locatorClass.equals(By.ByName.class)) {
            locatorType = "name";
            locatorValue = (String) FieldUtils.readField(locator, "name", true);
        } else if (locatorClass.equals(By.ByClassName.class)) {
            locatorType = "className";
            locatorValue = (String) FieldUtils.readField(locator, "className", true);
        } else if (locatorClass.equals(By.ByLinkText.class)) {
            locatorType = "linkText";
            locatorValue = (String) FieldUtils.readField(locator, "linkText", true);
        } else if (locatorClass.equals(By.ByXPath.class)) {
            locatorType = "xpath";
            locatorValue = (String) FieldUtils.readField(locator, "xpathExpression", true);
        } else if (locatorClass.equals(By.ByCssSelector.class)) {
            locatorType = "css";
            locatorValue = (String) FieldUtils.readField(locator, "cssSelector", true);
        } else {
            throw new Exception(String.format("Unsupported locator class: %s", locatorClass));
        }

        return new ImmutablePair<>(locatorType, locatorValue);
    }

    public Object executeScriptOnWebElement(By locator, String command) throws Exception {
        String currentContext = driver.getContext();
        switchToWebContext();
        ImmutablePair<String, String> locatorInJson = getLocatorInJson(locator);
        String script = IOUtils.toString(getResourceAsStream("execute-script-on-web-element.js"), StandardCharsets.UTF_8);
        Object result = driver.executeScript(script, locatorInJson.getKey(), locatorInJson.getValue(), command);
        if (result == null) {
            Thread.sleep(Config.IMPLICIT_WAIT_IN_SECOND * 1000);
            result = driver.executeScript(script, locatorInJson.getKey(), locatorInJson.getValue(), command);
            if (result == null) {
                throw new Exception(String.format("Cannot find element with locator: %s", locator));
            }
        }

        switchContext(currentContext);
        return result;
    }

    public void scrollToWebElement(By locator) throws Exception {
        executeScriptOnWebElement(locator, "scrollIntoView");
    }

    public Rectangle getWebElementRect(By locator) throws Exception {
        if (this.isIos) {
            String currentContext = driver.getContext();

            switchToWebContext();
            Rectangle elementRect = findElementBy(locator).getRect();

            switchToNativeContext();

            MobileElement urlBar = findElementBy(By.name("TopBrowserBar"), By.name("TopBrowserToolbar"));
            Rectangle urlBarRect = urlBar.getRect();

            elementRect.x += urlBarRect.x;
            elementRect.y += urlBarRect.y + urlBarRect.height;

            switchContext(currentContext);
            return elementRect;
        } else {
            String currentContext = driver.getContext();

            Map rectMap = (Map) executeScriptOnWebElement(locator, "getBoundingClientRect");
            Rectangle rect = new Rectangle(
                    Math.toIntExact((Long) rectMap.get("x")),
                    Math.toIntExact((Long) rectMap.get("y")),
                    Math.toIntExact((Long) rectMap.get("height")),
                    Math.toIntExact((Long) rectMap.get("width"))
            );

            switchToNativeContext();
            MobileElement webviewElement = findElementBy(MobileBy.className("android.webkit.WebView"));
            Rectangle webviewRect = webviewElement.getRect();

            switchContext(currentContext);

            return new Rectangle(webviewRect.x + rect.x, webviewRect.y + rect.y, rect.height, rect.width);
        }
    }

    public MobileElement findElementBy(By locator, long visibilityTimeoutInSeconds) throws Exception {
        System.out.println(String.format("Find element by: %s", locator));
        MobileElement foundElement = null;
        List<MobileElement> elements = driver.findElements(locator);
        if (elements.size() == 1) {
            foundElement = elements.get(0);
        } else {
            int smallestSize = Integer.MAX_VALUE;
            for (MobileElement element : elements) {
                Dimension size = element.getSize();
                int area = size.width * size.height;
                if (area < smallestSize) {
                    foundElement = element;
                    smallestSize = area;
                }
            }
        }

        if (foundElement == null) {
            throw new Exception(String.format("Cannot find element by: %s", locator));
        }

        if (!foundElement.isDisplayed()) {
            WebDriverWait wait = new WebDriverWait(driver, visibilityTimeoutInSeconds);
            wait.until(ExpectedConditions.presenceOfElementLocated(locator));
            foundElement = driver.findElement(locator);
        }

        return foundElement;
    }

    public MobileElement findElementBy(By locator) throws Exception {
        return findElementBy(locator, Config.IMPLICIT_WAIT_IN_SECOND);
    }

    @Nullable
    public MobileElement findElementWithoutWaiting(@Nullable MobileElement parentElement, By locator) {
        if (parentElement == null) {
            System.out.println(String.format("Find element without waiting by: %s", locator));
        } else {
            System.out.println(String.format("Find element from %s without waiting by: %s", parentElement.getTagName(), locator));
        }

        setImplicitWaitInSecond(0);
        MobileElement element = null;
        try {
            if (parentElement == null) {
                element = driver.findElement(locator);
            } else {
                element = parentElement.findElement(locator);
            }
        } catch (Exception ignored) {
        }

        setImplicitWaitInSecond(Config.IMPLICIT_WAIT_IN_SECOND);
        return element;
    }

    @Nullable
    public MobileElement findElementWithoutWaiting(By locator) {
        return findElementWithoutWaiting(null, locator);
    }

    public MobileElement findElementBy(By... locators) throws Exception {
        List<String> locatorStrings = new ArrayList<>();
        for (By locator : locators) {
            locatorStrings.add(locator.toString());
        }
        String locatorText = String.join(", ", locatorStrings);
        System.out.println(String.format("Find element by: %s", locatorText));

        setImplicitWaitInSecond(0);

        double waitInterval = 5d;
        MobileElement foundElement = null;

        outerLoop:
        for (int tryTime = 1; tryTime <= Math.ceil(Config.IMPLICIT_WAIT_IN_SECOND / waitInterval); tryTime++) {
            for (By locator : locators) {
                try {
                    foundElement = driver.findElement(locator);
                    if (foundElement != null) {
                        break outerLoop;
                    }
                } catch (Exception ignored) {
                }
            }
            sleep((int) (waitInterval * 1000));
        }

        setImplicitWaitInSecond(Config.IMPLICIT_WAIT_IN_SECOND);

        if (foundElement == null) {
            throw new Exception(String.format("Cannot find element by: %s", locatorText));
        }

        return foundElement;
    }

    /**
     * Touch at center of element (element need to be visible)
     */
    public TouchAction touchAtCenterOfElement(MobileElement element) {
        System.out.println(String.format("Touch at center of element %s", element.getTagName()));

        TouchAction action = new TouchAction(driver)
                .tap(TapOptions.tapOptions().withElement(ElementOption.element(element)));
        action.perform();

        return action;
    }

    /**
     * Touch at relative point of element (element need to be visible)
     */
    public TouchAction touchAtRelativePointOfElement(MobileElement element, double relativePointX, double relativePointY) {
        System.out.println(String.format("Touch on element %s at relative point (%s %s)", element.getTagName(), relativePointX, relativePointY));

        return touchAtPoint(getAbsolutePoint(relativePointX, relativePointY, element.getRect()));
    }

    /**
     * Touch at a relative position
     */
    public TouchAction touchAtPoint(double relativePointX, double relativePointY) throws IOException {
        System.out.println(String.format("Touch at relative point (%s, %s)", relativePointX, relativePointY));

        Point absolutePoint = getAbsolutePoint(relativePointX, relativePointY);
        return touchAtPoint(absolutePoint);
    }

    /**
     * Touch at a Point
     */
    public TouchAction touchAtPoint(Point point) {
        System.out.println(String.format("Touch at point (%s, %s)", point.x, point.y));

        TouchAction action = new TouchAction(driver)
                .tap(TapOptions.tapOptions().withPosition(PointOption.point(point)));
        action.perform();

        return action;
    }

    /**
     * Swipe from center of element (with accelerate)
     */
    public TouchAction swipeFromPoint(Point fromPoint, double relativeOffsetX, double relativeOffsetY, int durationInMs) throws IOException {
        double toX = fromPoint.x + relativeOffsetX * getScreenSize().x;
        double toY = fromPoint.y + relativeOffsetY * getScreenSize().y;
        toX = Math.max(toX, 0);
        toY = Math.max(toY, 0);
        Point toPoint = new Point((int) toX, (int) toY);

        System.out.println(String.format("Swipe from point (%s, %s) to point (%s, %s) with duration %s", fromPoint.x, fromPoint.y, toPoint.x, toPoint.y, durationInMs));

        return swipeByPoint(fromPoint, toPoint, durationInMs);
    }

    /**
     * Drag from center element (no accelerate)
     */
    public Sequence dragFromPoint(Point fromPoint, double relativeOffsetX, double relativeOffsetY) throws IOException {
        double toX = fromPoint.x + relativeOffsetX * getScreenSize().x;
        double toY = fromPoint.y + relativeOffsetY * getScreenSize().y;
        toX = Math.max(toX, 0);
        toY = Math.max(toY, 0);
        Point toPoint = new Point((int) toX, (int) toY);

        System.out.println(String.format("Drag from point (%s, %s) to point (%s, %s)", fromPoint.x, fromPoint.y, toPoint.x, toPoint.y));

        return dragByPoint(fromPoint, toPoint);
    }

    /**
     * Swipe from relative position to relative position (with accelerate)
     */
    public TouchAction swipeByPoint(double fromRelativePointX, double fromRelativePointY, double toRelativePointX, double toRelativePointY, int durationInMs) throws IOException {
        System.out.println(String.format("Swipe from relative point (%s, %s) to relative point (%s, %s) with duration %s", fromRelativePointX, fromRelativePointY, toRelativePointX, toRelativePointY, durationInMs));

        Point fromPoint = getAbsolutePoint(fromRelativePointX, fromRelativePointY);
        Point toPoint = getAbsolutePoint(toRelativePointX, toRelativePointY);

        return swipeByPoint(fromPoint, toPoint, durationInMs);
    }

    /**
     * Swipe from Point to Point (with accelerate)
     */
    public TouchAction swipeByPoint(Point fromPoint, Point toPoint, int durationInMs) {
        System.out.println(String.format("Swipe from point (%s, %s) to point (%s, %s) with duration %s", fromPoint.x, fromPoint.y, toPoint.x, toPoint.y, durationInMs));

        TouchAction action = new TouchAction(driver);

        action.press(PointOption.point(fromPoint));
        action.waitAction(WaitOptions.waitOptions(Duration.ofMillis(durationInMs)));
        action.moveTo(PointOption.point(toPoint));
        action.release();

        action.perform();

        return action;
    }

    /**
     * Quick swipe to top from a Point
     */
    public TouchAction swipeToTop(Point fromPoint) throws Exception {
        Point toPoint = new Point(fromPoint.x, getScreenSize().y - 10);
        System.out.println(String.format("Swipe to top from point (%s, %s) to point (%s, %s)", fromPoint.x, fromPoint.y, toPoint.x, toPoint.y));

        return swipeByPoint(fromPoint, toPoint, 100);
    }

    /**
     * Drag from Point to Point (no accelerate)
     */
    public Sequence dragByPoint(Point fromPoint, Point toPoint) {
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence sequence = new Sequence(finger, 0);

        if (isIos) {
            sequence.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), fromPoint.x, fromPoint.y));
            sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.MIDDLE.asArg()));
            sequence.addAction(new Pause(finger, Duration.ofMillis(2000)));
            sequence.addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), toPoint.x, toPoint.y));
            sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.MIDDLE.asArg()));
        } else {
            sequence.addAction(finger.createPointerMove(Duration.ofMillis(0), PointerInput.Origin.viewport(), fromPoint.x, fromPoint.y));
            sequence.addAction(finger.createPointerDown(PointerInput.MouseButton.MIDDLE.asArg()));
            sequence.addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.viewport(), toPoint.x, toPoint.y));
            sequence.addAction(new Pause(finger, Duration.ofMillis(300)));
            sequence.addAction(finger.createPointerMove(Duration.ofMillis(300), PointerInput.Origin.pointer(), 0, 0));
            sequence.addAction(finger.createPointerUp(PointerInput.MouseButton.MIDDLE.asArg()));
        }

        System.out.println(String.format("Drag from point (%s, %s) to point (%s, %s)", fromPoint.x, fromPoint.y, toPoint.x, toPoint.y));

        driver.perform(Arrays.asList(sequence));

        return sequence;
    }

    public void sendKeys(String keys) throws Exception {
        System.out.println(String.format("Send keys: %s", keys));

        if (this.isIos) {
            char[] chars = keys.toCharArray();
            JsonObject requestJson = new JsonObject();
            requestJson.add("value", gson.toJsonTree(chars).getAsJsonArray());
            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), requestJson.toString());

            Request.Builder sendKeysBuilder = new Request.Builder()
                    .post(requestBody)
                    .url(getAppiumServerUrl() + String.format("/session/%s/keys", driver.getSessionId()));

            try (Response response = httpClient.newCall(sendKeysBuilder.build()).execute()) {
                if (response.code() != 200) {
                    throw new Exception(response.body().string());
                }
            }
        } else {
            try {
                KeyInput keyInput = new KeyInput("keyboard");
                Sequence sequence = new Sequence(keyInput, 0);
                for (int index = 0; index < keys.length(); index++) {
                    int codePoint = Character.codePointAt(keys, index);
                    sequence.addAction(keyInput.createKeyDown(codePoint));
                    sequence.addAction(keyInput.createKeyUp(codePoint));
                }
    
                driver.perform(Arrays.asList(sequence));
            }
            catch (Exception ignored) {
                getAndroidDriver().getKeyboard().sendKeys(keys);
            }
        }
    }

    public void sendKeys(MobileElement element, String keys) {
        System.out.println(String.format("Send keys '%s' on element %s", keys, element.getTagName()));

        element.sendKeys(keys);
    }

    public void clearTextField(int maxChars) throws Exception {
        System.out.println(String.format("Clear text field, maximum %d characters", maxChars));
        for (int i = 0; i < maxChars; i++) {
            press(PRESS_TYPES.DELETE);
        }
    }

    public void press(PRESS_TYPES type) throws Exception {
        System.out.println(String.format("Press on %s key", type));

        switch (type) {
            case HOME:
                if (isIos) {
                    IOSDriver<MobileElement> iosDriver = getIosDriver();
                    if (iosDriver.isDeviceLocked()) {
                        iosDriver.unlockDevice();
                    } else {
                        driver.executeScript("mobile: pressButton", ImmutableMap.of("name", "home"));
                    }
                } else {
                    pressAndroidKey(AndroidKey.HOME);
                }
                break;

            case BACK:
                pressAndroidKey(AndroidKey.BACK);
                break;

            case POWER:
                if (isIos) {
                    IOSDriver<MobileElement> iosDriver = getIosDriver();
                    if (iosDriver.isDeviceLocked()) {
                        iosDriver.unlockDevice();
                    } else {
                        iosDriver.lockDevice();
                    }
                } else {
                    pressAndroidKey(AndroidKey.POWER);
                }
                break;

            case APP_SWITCH:
                pressAndroidKey(AndroidKey.APP_SWITCH);
                break;

            case ENTER:
                if (isIos) {
                    sendKeys("\n");
                } else {
                    pressAndroidKey(AndroidKey.ENTER);
                }
                break;

            case DELETE:
                if (isIos) {
                    sendKeys("\b");
                } else {
                    pressAndroidKey(AndroidKey.DEL);
                }
                break;

            default:
                throw new Exception(String.format("Don't support press %s key", type));

        }
    }

    public void pressAndroidKey(AndroidKey key) {
        getAndroidDriver().pressKey(new KeyEvent(key));
    }

    /**
     * Scroll to find best element to touch
     */
    public void touchOnScrollableElement(By locator, String commandId) throws Exception {
        long currentCommandId = getCurrentCommandId();
        // Temporary disable adding baseCommandId to Appium request to avoid asserting wrong command
        setCurrentCommandId(0);

        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        JsonReader reader = new JsonReader(new InputStreamReader(getResourceAsStream(commandId + ".json")));
        Map<String, String> infoMap = gson.fromJson(reader, type);

        Document sourceElementDoc = loadXMLFromString(infoMap.get("touchedElementSource"));
        Element sourceElement = sourceElementDoc.getRootElement();
        Point screenSize = getScreenSize();
        String sourceElementXpath = infoMap.get("touchedElementXpath");

        MobileElement touchableElement = Utils.retry(new Utils.Task<MobileElement>() {
            private MobileElement scrollableElement;
            private List<String> potentialXpathList = new ArrayList<>();
            private String targetElementXpath;

            @Override
            MobileElement exec(int attempt) throws Exception {
                final int VISIBILITY_TIMEOUT_IN_SECONDS = 5;

                try {
                    return findElementBy(locator, VISIBILITY_TIMEOUT_IN_SECONDS);
                } catch (Exception e) {
                    potentialXpathList.clear();
                    Document source = loadXMLFromString(driver.getPageSource());
                    List<Node> elementsByTagName = source.selectNodes("//" + sourceElement.getName());
                    for (Node itemNode : elementsByTagName) {
                        if (itemNode.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }

                        boolean isEqual = compareNodes(sourceElement, (Element) itemNode);
                        if (isEqual) {
                            potentialXpathList.add(itemNode.getUniquePath());
                        }
                    }

                    if (!potentialXpathList.isEmpty()) {
                        for (String xpath : potentialXpathList) {
                            if (sourceElementXpath.equals(xpath)) {
                                targetElementXpath = xpath;
                                break;
                            }
                        }

                        if (targetElementXpath == null) {
                            targetElementXpath = potentialXpathList.get(0);
                        }
                    }

                    if (targetElementXpath == null) {
                        throw new Exception();
                    }

                    return findElementBy(By.xpath(targetElementXpath.replace(IOS_XPATH_REDUNDANT_PREFIX, "")), VISIBILITY_TIMEOUT_IN_SECONDS);
                }
            }

            @Override
            public void handleException(Exception e, int attempt) throws Exception {
                System.out.println(String.format("Cannot find touchable element, %s attempt", Utils.convertToOrdinal(attempt)));

                if (scrollableElement == null) {
                    scrollableElement = findElementBy(By.xpath(infoMap.get("scrollableElementXpath")));
                    hideKeyboard();
                }

                if (attempt == 1) {
                    swipeToTop(scrollableElement.getCenter());
                } else {
                    Point center = scrollableElement.getCenter();
                    Rectangle rect = scrollableElement.getRect();
                    // Fix bug when scrollableElement is out of viewport
                    if (center.y > screenSize.y || rect.height < 0) {
                        center.y = screenSize.y / 2;
                    }

                    dragFromPoint(center, 0, -0.5);
                }
            }
        }, 10, 0);

        if (touchableElement == null) {
            throw new Exception("Cannot find any element to touch");
        }

        hideKeyboard();
        setCurrentCommandId(currentCommandId);
        touchAtRelativePointOfElement(touchableElement, Double.parseDouble(infoMap.get("touchedElementRelativeX")), Double.parseDouble(infoMap.get("touchedElementRelativeY")));
    }

    public void hideKeyboard() {
        if (this.isIos) {
            if (!getIosDriver().isKeyboardShown()) return;
            System.out.println("Keyboard is shown, hide it");

            String currentContext = driver.getContext();
            switchToNativeContext();

            MobileElement keyboardElement = findElementWithoutWaiting(By.className("XCUIElementTypeKeyboard"));
            if (keyboardElement == null) {
                switchContext(currentContext);
                return;
            }

            MobileElement hideKeyboardButton = findElementWithoutWaiting(keyboardElement, By.id("Hide keyboard"));
            if (hideKeyboardButton != null) {
                touchAtCenterOfElement(hideKeyboardButton);
                switchContext(currentContext);
                return;
            }

            MobileElement typePredictElement = findElementWithoutWaiting(By.name("Typing Predictions"));
            MobileElement anchorElement = typePredictElement != null ? typePredictElement : keyboardElement;

            Rectangle rect = anchorElement.getRect();
            Point center = anchorElement.getCenter();
            Point fromPoint = new Point(center.x, rect.y - 20);
            Point toPoint = new Point(center.x, rect.y + 20);
            swipeByPoint(fromPoint, toPoint, 300);
            switchContext(currentContext);
        } else {
            if (!getAndroidDriver().isKeyboardShown()) return;
            System.out.println("Keyboard is shown, hide it");
            driver.hideKeyboard();
        }
    }

    public void setImplicitWaitInSecond(int value) {
        driver.manage().timeouts().implicitlyWait(value, TimeUnit.SECONDS);
    }

    public void updateSettings() {
        if (this.isIos) {
            getIosDriver().setSetting(Setting.IGNORE_UNIMPORTANT_VIEWS, true);
        } else {
            getAndroidDriver().setSetting(Setting.IGNORE_UNIMPORTANT_VIEWS, true);
        }
    }

    public Point getScreenSize() throws IOException {
        if (screenSize == null) {
            byte[] screenshotBytes = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(screenshotBytes);
            BufferedImage image = ImageIO.read(inputStream);
            int width = image.getWidth();
            int height = image.getHeight();
            screenSize = new Point(width, height);
        }

        return screenSize;
    }

    public DoublePoint getAppScale() {
        if (!this.isIos) return new DoublePoint(1, 1);

        try {
            MobileElement rootElement = driver.findElement(By.xpath("/*"));
            Dimension rootElementSize = rootElement.getSize();
            Point screenSize = getScreenSize();
            double screenWidthScaled = screenSize.x / retinaScale;
            double screenHeightScaled = screenSize.y / retinaScale;
            return new DoublePoint(screenWidthScaled / rootElementSize.width,  screenHeightScaled / rootElementSize.height);
        }
        catch (Exception e) {
            e.printStackTrace();
            return new DoublePoint(1, 1);
        }
    }

    public Point getAbsolutePoint(double relativePointX, double relativePointY) throws IOException {
        Point screenSize = getScreenSize();

        if (retinaScale > 1) {
            return new Point((int) Math.round(relativePointX * screenSize.x / retinaScale), (int) Math.round(relativePointY * screenSize.y / retinaScale));
        } else {
            return new Point((int) Math.round(relativePointX * screenSize.x), (int) Math.round(relativePointY * screenSize.y));
        }
    }

    public Point getAbsolutePoint(double relativePointX, double relativePointY, Rectangle rect) {
        DoublePoint appScale = getAppScale();
        double x = rect.x * appScale.x + rect.width * appScale.x * relativePointX;
        double y = rect.y * appScale.y + rect.height * appScale.y * relativePointY;
        return new Point((int) x, (int) y);
    }

    public void sleep(int durationInMs) throws InterruptedException {
        Thread.sleep(durationInMs);
    }

    protected Document loadXMLFromString(String xml) throws Exception {
        return DocumentHelper.parseText(xml);
    }

    protected InputStream getResourceAsStream(String path) {
        return getClass().getClassLoader().getResourceAsStream(path);
    }

    protected boolean compareNodes(Element expected, Element actual) {
        if (!expected.getName().equals(actual.getName())) {
            return false;
        }

        String[] compareAttrs = new String[]{"label", "text", "visible", "class", "name", "type", "resource-id", "content-desc", "accessibility-id"};

        for (String attrName : compareAttrs) {
            String v1 = null;
            String v2 = null;
            try {
                v1 = expected.attributeValue(attrName);
                v2 = actual.attributeValue(attrName);
            } catch (Exception ignored) {
            }

            if (v1 != null && v2 != null && !v1.isEmpty() && !v2.isEmpty() && !v1.equals(v2)) {
                return false;
            }
        }

        if (expected.elements().size() != actual.elements().size()) {
            return false;
        }

        for (int i = 0; i < expected.elements().size(); i++) {
            Element expectedChild = expected.elements().get(i);
            Element actualChild = actual.elements().get(i);
            boolean isEqual = compareNodes(expectedChild, actualChild);
            if (!isEqual) {
                return false;
            }
        }

        return true;
    }

    public Point getCenterOfRect(Rectangle rect) {
        Point center = new Point(rect.x + rect.width / 2, rect.y + rect.height / 2);
        return center;
    }

    public IOSDriver<MobileElement> getIosDriver() {
        return (IOSDriver<MobileElement>) driver;
    }

    public AndroidDriver<MobileElement> getAndroidDriver() {
        return (AndroidDriver<MobileElement>) driver;
    }

    public URL getAppiumServerUrl() throws MalformedURLException {
        if (proxy != null) {
            return new URL(proxy.getServerUrl());
        } else {
            return new URL(Config.APPIUM_SERVER_URL);
        }
    }

    public Device getAvailableDevice(String deviceName, String platformVersion) throws Exception {
        URIBuilder deviceListUriBuilder = new URIBuilder(Config.KOBITON_API_URL + "/v1/devices");
        deviceListUriBuilder.addParameter("isOnline", "true");
        deviceListUriBuilder.addParameter("isBooked", "false");
        deviceListUriBuilder.addParameter("deviceName", deviceName);
        deviceListUriBuilder.addParameter("platformVersion", platformVersion);

        Request.Builder deviceListBuilder = new Request.Builder()
                .url(deviceListUriBuilder.build().toURL())
                .header(HttpHeaders.AUTHORIZATION, Config.getBasicAuthString())
                .get();



        try (Response response = httpClient.newCall(deviceListBuilder.build()).execute()) {
            if (response.code() != 200) {
                throw new Exception(response.body().string());
            }

            DeviceListResponse deviceListResponse = gson.fromJson(response.body().string(), DeviceListResponse.class);

            List<Device> deviceList = new ArrayList<>();
            deviceList.addAll(deviceListResponse.cloudDevices);
            deviceList.addAll(deviceListResponse.privateDevices);

            if (deviceList.isEmpty()) {
                return null;
            } else {
                return deviceList.get(0);
            }
        }
    }

    public Device findOnlineDevice(DesiredCapabilities capabilities) throws Exception {
        if (Config.DEVICE_SOURCE != Config.DEVICE_SOURCE_ENUMS.KOBITON) {
            return null;
        }

        int tryTime = 1;
        Device device = null;
        String deviceName = (String) capabilities.getCapability(MobileCapabilityType.DEVICE_NAME);
        String platformVersion = (String) capabilities.getCapability(MobileCapabilityType.PLATFORM_VERSION);
        while (tryTime <= Config.DEVICE_WAITING_MAX_TRY_TIMES) {
            System.out.println(String.format("Is device (name: %s, platform: %s) online, retrying at %s time?", deviceName, platformVersion, Utils.convertToOrdinal(tryTime)));
            device = getAvailableDevice(deviceName, platformVersion);
            if (device != null) {
                System.out.println(String.format("Device found: %s - %s", device.deviceName, device.platformVersion));
                break;
            }
            tryTime++;
            Thread.sleep(Config.DEVICE_WAITING_INTERVAL_IN_MS);
        }

        if (device == null) {
            throw new Exception(String.format("Cannot find an online device: %s - %s", deviceName, platformVersion));
        }
        return device;
    }
    
    public String getAppUrl() throws Exception {
        String appUrl = "";
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
            .url(String.format("%s/v1/app/versions/%s/downloadUrl", Config.KOBITON_API_URL, Config.APP_VERSION))
            .addHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .addHeader(HttpHeaders.AUTHORIZATION, Config.getBasicAuthString())
            .build();

        try (Response response = client.newCall(request).execute()) {
            String body = response.body().string();
            JsonObject object = gson.fromJson(body, JsonObject.class);
            appUrl = object.get("url").getAsString();
        }
        
        return appUrl;
    }

    public void saveDebugResource() {
        try {
            String resourcesDirPath = getClass().getClassLoader().getResource("").getFile();
            String debugDirName = String.format("%s %s", deviceName, platformVersion);
            debugDirName = debugDirName.replaceAll("[^a-zA-Z0-9]", "_");
            File debugDir = new File(resourcesDirPath, "debug/" + debugDirName);
            debugDir.mkdirs();

            String source = driver.getPageSource();
            FileUtils.writeStringToFile(new File(debugDir, "source.xml"), source, StandardCharsets.UTF_8);

            File screenshotFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(screenshotFile, new File(debugDir, "screenshot.png"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long getCurrentCommandId() {
        return this.proxy != null ? this.proxy.currentCommandId : 0;
    }

    public long getKobitonSessionId() {
        return this.proxy != null ? this.proxy.kobitonSessionId : 0;
    }

    public void setCurrentCommandId(long currentCommandId) {
        if (this.proxy != null) {
            this.proxy.currentCommandId = currentCommandId;
        }
    }

    public static class Device {
        public long id;
        public boolean isBooked, isOnline, isFavorite, isCloud;
        public String deviceName, platformName, platformVersion, udid;
    }

    public static class DeviceListResponse {
        public List<Device> privateDevices;
        public List<Device> favoriteDevices;
        public List<Device> cloudDevices;
    }

    public static class DoublePoint {
        public double x, y;

        public DoublePoint(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}
