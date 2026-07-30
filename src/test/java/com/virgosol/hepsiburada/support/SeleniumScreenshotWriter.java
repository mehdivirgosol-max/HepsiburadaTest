package com.virgosol.hepsiburada.support;

import com.thoughtworks.gauge.screenshot.CustomScreenshotWriter;
import com.virgosol.hepsiburada.driver.DriverSession;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Base64;

import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Authentication.EMAIL_INPUT;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Authentication.PASSWORD_INPUT;
import java.util.UUID;

public final class SeleniumScreenshotWriter implements CustomScreenshotWriter {
    private static final byte[] NEUTRAL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk"
                    + "+A8AAQUBAScY42YAAAAASUVORK5CYII="
    );

    @Override
    public String takeScreenshot() {
        WebDriver driver = DriverSession.currentOrNull();
        String screenshotDirectory = System.getenv("gauge_screenshots_dir");
        if (screenshotDirectory == null || screenshotDirectory.isBlank()) {
            screenshotDirectory = Path.of(".gauge", "screenshots").toString();
        }

        String fileName = "selenium-" + UUID.randomUUID() + ".png";
        Path output = Path.of(screenshotDirectory).resolve(fileName);
        byte[] screenshotBytes = NEUTRAL_PNG;

        if (driver instanceof TakesScreenshot screenshotDriver) {
            try {
                if (switchToLiveWindow(driver) && redactLoginFields(driver)) {
                    screenshotBytes = screenshotDriver.getScreenshotAs(OutputType.BYTES);
                } else {
                    warnNeutralScreenshot();
                }
            } catch (RuntimeException screenshotFailure) {
                warnNeutralScreenshot();
            }
        } else {
            warnNeutralScreenshot();
        }

        try {
            Files.createDirectories(output.getParent());
            Files.write(
                    output,
                    screenshotBytes,
                    StandardOpenOption.CREATE_NEW
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Selenium ekran görüntüsü kaydedilemedi.", exception);
        }
        return fileName;
    }

    private boolean redactLoginFields(WebDriver driver) {
        if (!(driver instanceof JavascriptExecutor javascript)) {
            return false;
        }

        try {
            for (By locator : new By[]{EMAIL_INPUT, PASSWORD_INPUT}) {
                for (WebElement field : driver.findElements(locator)) {
                    javascript.executeScript(
                            "arguments[0].value = ''; arguments[0].setAttribute('value', '');",
                            field
                    );
                    String remainingValue = field.getDomProperty("value");
                    if (remainingValue != null && !remainingValue.isBlank()) {
                        return false;
                    }
                }
            }
            return true;
        } catch (RuntimeException redactionFailure) {
            return false;
        }
    }

    private boolean switchToLiveWindow(WebDriver driver) {
        try {
            driver.getCurrentUrl();
            return true;
        } catch (RuntimeException currentWindowFailure) {
            try {
                for (String handle : driver.getWindowHandles()) {
                    try {
                        driver.switchTo().window(handle);
                        driver.getCurrentUrl();
                        return true;
                    } catch (RuntimeException ignored) {
                        // Kapanma yarışındaki diğer pencere denenir.
                    }
                }
            } catch (RuntimeException ignored) {
                // Tarayıcı oturumu yoksa nötr görüntü kullanılacaktır.
            }
            return false;
        }
    }

    private void warnNeutralScreenshot() {
        System.err.println(
                "[UYARI] Tarayıcı görüntüsü güvenli biçimde alınamadı; nötr hata görseli kaydedildi."
        );
    }
}
