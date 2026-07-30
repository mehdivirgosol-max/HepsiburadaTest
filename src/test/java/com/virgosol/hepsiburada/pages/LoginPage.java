package com.virgosol.hepsiburada.pages;

import com.virgosol.hepsiburada.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.net.URI;
import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Authentication.EMAIL_INPUT;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Authentication.LOGIN_BUTTON;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Authentication.LOGIN_ERRORS;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Authentication.PASSWORD_INPUT;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Authentication.SECURITY_CHALLENGES;

public final class LoginPage extends BasePage {
    public LoginPage(WebDriver driver, TestConfig config) {
        super(driver, config);
    }

    public void fillCredentials() {
        config.assertTrustedUrl(driver.getCurrentUrl(), "Kimlik bilgisi girişi");
        acceptCookiePreferencesIfPresent();

        WebElement emailInput = waitUntilClickable(EMAIL_INPUT);
        typeCredential(emailInput, config.email(), "E-posta");

        WebElement passwordInput = waitUntilClickable(PASSWORD_INPUT);
        typeCredential(passwordInput, config.password(), "Şifre");
    }

    public void submitAndWaitForLoginCompletion() {
        config.assertTrustedUrl(driver.getCurrentUrl(), "Giriş formunu gönderme");
        acceptCookiePreferencesIfPresent();

        String originalHandle = driver.getWindowHandle();
        Set<String> handlesBeforeSubmit = new LinkedHashSet<>(driver.getWindowHandles());
        NoSuchWindowException submitWindowClosure = null;

        try {
            new Actions(driver).pause(Duration.ofSeconds(1)).perform();
            clickWithoutScrolling(LOGIN_BUTTON);
        } catch (NoSuchWindowException closedDuringSubmit) {
            submitWindowClosure = closedDuringSubmit;
        }

        try {
            waitFor(config.timeout(), "Giriş işlemi tamamlanmadı", webDriver -> {
                String currentUrl = switchToLiveTrustedWindow(
                        webDriver,
                        handlesBeforeSubmit,
                        originalHandle
                );
                if (currentUrl == null) {
                    return null;
                }

                // Giriş isteğinden sonra gecikmeli yüklenen consent banner'ını da kapatır.
                acceptCookiePreferencesIfPresent();

                if (hasVisibleSecurityChallenge()) {
                    throw new AssertionError(
                            "Hepsiburada ek güvenlik/CAPTCHA doğrulaması gösterdi; "
                                    + "otomasyon bu adımı aşmaya çalışmaz."
                    );
                }

                String error = visibleLoginError();
                if (!error.isBlank()) {
                    throw new AssertionError("Hepsiburada giriş isteğini reddetti: " + error);
                }

                boolean formVisible = anyDisplayed(EMAIL_INPUT, PASSWORD_INPUT, LOGIN_BUTTON);
                boolean loginUrl = isLoginUrl(currentUrl);
                Object readyState = javascript.executeScript("return document.readyState;");
                boolean documentReady = "interactive".equals(readyState)
                        || "complete".equals(readyState);
                return !formVisible && !loginUrl && documentReady ? true : null;
            }, EMAIL_INPUT, LOGIN_BUTTON);
        } catch (AssertionError loginFailure) {
            if (submitWindowClosure != null) {
                loginFailure.addSuppressed(submitWindowClosure);
            }
            throw loginFailure;
        }
        waitForDocumentReady();
    }

    private String switchToLiveTrustedWindow(
            WebDriver webDriver,
            Set<String> handlesBeforeSubmit,
            String originalHandle
    ) {
        Set<String> liveHandles = new LinkedHashSet<>(webDriver.getWindowHandles());
        if (liveHandles.isEmpty()) {
            return null;
        }

        Set<String> candidates = new LinkedHashSet<>();
        liveHandles.stream()
                .filter(handle -> !handlesBeforeSubmit.contains(handle))
                .forEach(candidates::add);
        if (liveHandles.contains(originalHandle)) {
            candidates.add(originalHandle);
        }
        candidates.addAll(liveHandles);

        SecurityException untrustedWindow = null;
        for (String handle : candidates) {
            try {
                webDriver.switchTo().window(handle);
                String url = webDriver.getCurrentUrl();
                if (url == null || url.isBlank() || "about:blank".equalsIgnoreCase(url)) {
                    continue;
                }
                try {
                    config.assertTrustedUrl(url, "Giriş sonrası yönlendirme");
                    return url;
                } catch (SecurityException securityFailure) {
                    untrustedWindow = securityFailure;
                }
            } catch (NoSuchWindowException ignored) {
                // Pencere geçiş sırasında kapandıysa güncel handle kümesi sonraki polling'de alınır.
            }
        }

        if (untrustedWindow != null) {
            throw untrustedWindow;
        }
        return null;
    }

    private boolean isLoginUrl(String url) {
        URI uri = URI.create(url);
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase(Locale.ROOT);
        return "giris.hepsiburada.com".equals(host)
                || path.contains("/uyelik/giris")
                || path.contains("/login");
    }

    private boolean hasVisibleSecurityChallenge() {
        return anyDisplayed(SECURITY_CHALLENGES.toArray(By[]::new));
    }

    private String visibleLoginError() {
        for (By locator : LOGIN_ERRORS) {
            for (WebElement element : driver.findElements(locator)) {
                try {
                    String text = element.getText().trim();
                    String normalized = normalizeText(text);
                    boolean looksLikeError = normalized.contains("hata")
                            || normalized.contains("yanlis")
                            || normalized.contains("gecersiz");
                    if (element.isDisplayed() && !text.isBlank() && looksLikeError) {
                        return redactSecrets(text);
                    }
                } catch (RuntimeException ignored) {
                    // DOM güncellenirken sonraki hata yüzeyi kontrol edilir.
                }
            }
        }
        return "";
    }

    private String redactSecrets(String value) {
        return value
                .replace(config.email(), "[gizli e-posta]")
                .replace(config.password(), "[gizli]");
    }

    private void typeCredential(WebElement field, String value, String fieldName) {
        try {
            typeControlled(field, value);
        } catch (RuntimeException sensitiveInputFailure) {
            throw new AssertionError(
                    fieldName + " alanı doldurulamadı; hassas içerik güvenlik nedeniyle raporlanmadı."
            );
        }
    }
}
