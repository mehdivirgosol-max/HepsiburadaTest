package com.virgosol.hepsiburada.driver;

import com.virgosol.hepsiburada.config.TestConfig;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.UnexpectedAlertBehaviour;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DriverSession {
    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<TestConfig> CONFIG = new ThreadLocal<>();

    private DriverSession() {
    }

    public static void start(TestConfig config) {
        if (DRIVER.get() != null) {
            throw new IllegalStateException("Bu senaryo için tarayıcı zaten açık.");
        }

        ChromeOptions options = new ChromeOptions();
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setUnhandledPromptBehaviour(UnexpectedAlertBehaviour.DISMISS_AND_NOTIFY);
        options.addArguments(
                "--disable-notifications",
                "--disable-popup-blocking",
                "--lang=tr-TR"
        );
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.setExperimentalOption(
                "excludeSwitches",
                Collections.singletonList("enable-automation")
        );
        options.setExperimentalOption("useAutomationExtension", false);

        Map<String, Object> preferences = new HashMap<>();
        preferences.put("credentials_enable_service", false);
        preferences.put("profile.password_manager_enabled", false);
        preferences.put("intl.accept_languages", "tr-TR,tr");
        options.setExperimentalOption("prefs", preferences);

        if (config.headless()) {
            options.addArguments("--headless=new", "--window-size=1920,1080");
        } else {
            options.addArguments("--start-maximized");
        }

        WebDriver driver = null;
        try {
            driver = new ChromeDriver(options);

            if (config.headless()) {
                driver.manage().window().setSize(new Dimension(1920, 1080));
            } else {
                // Görünür Chrome açılır açılmaz pencereyi ekranın kullanılabilir alanına büyütür.
                driver.manage().window().maximize();
            }

            CONFIG.set(config);
            DRIVER.set(driver);

            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
            driver.manage().timeouts().scriptTimeout(Duration.ofSeconds(20));
        } catch (RuntimeException startupFailure) {
            try {
                if (driver != null) {
                    driver.quit();
                }
            } catch (RuntimeException ignored) {
                startupFailure.addSuppressed(ignored);
            } finally {
                DRIVER.remove();
                CONFIG.remove();
            }
            throw startupFailure;
        }
    }

    public static WebDriver driver() {
        WebDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("Tarayıcı oturumu henüz başlatılmadı.");
        }
        return driver;
    }

    public static WebDriver currentOrNull() {
        return DRIVER.get();
    }

    public static TestConfig config() {
        TestConfig config = CONFIG.get();
        if (config == null) {
            throw new IllegalStateException("Test yapılandırması henüz yüklenmedi.");
        }
        return config;
    }

    public static void close() {
        WebDriver driver = DRIVER.get();
        try {
            if (driver != null) {
                driver.quit();
            }
        } finally {
            DRIVER.remove();
            CONFIG.remove();
        }
    }
}
