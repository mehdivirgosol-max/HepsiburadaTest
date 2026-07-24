package com.virgosol.hepsiburada.pages;

import com.virgosol.hepsiburada.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.FluentWait;

import java.text.Normalizer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class BasePage {
    private static final String FIND_SHADOW_COOKIE_ACCEPT_CONTROL = """
            const roots = [document];
            while (roots.length > 0) {
                const root = roots.shift();
                const exact = root.querySelector('#hb-accept-all');
                if (exact) {
                    const style = getComputedStyle(exact);
                    if (exact.getClientRects().length > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden') {
                        return exact;
                    }
                }

                const candidates = root.querySelectorAll(
                        '[data-name="Accept Button"], button, a, [role="button"]'
                );
                for (const candidate of candidates) {
                    const text = (candidate.textContent || '')
                            .replace(/\\s+/g, ' ')
                            .trim();
                    const style = getComputedStyle(candidate);
                    if ((text === 'Kabul Et' || text === 'Tümünü Kabul Et')
                            && candidate.getClientRects().length > 0
                            && style.display !== 'none'
                            && style.visibility !== 'hidden') {
                        return candidate;
                    }
                }

                for (const element of root.querySelectorAll('*')) {
                    if (element.shadowRoot) {
                        roots.push(element.shadowRoot);
                    }
                }
            }
            return null;
            """;
    private static final List<By> COOKIE_ACCEPT_CONTROLS = List.of(
            By.id("hb-accept-all"),
            By.id("onetrust-accept-btn-handler"),
            By.cssSelector("button[data-test-id='cookie-consent-accept']"),
            By.cssSelector("button[aria-label='Tüm çerezleri kabul et']"),
            By.cssSelector("input[type='button'][value='Kabul Et']"),
            By.xpath(
                    "//*[self::button or self::a or @role='button']["
                            + "normalize-space()='Tümünü kabul et' "
                            + "or normalize-space()='Tümünü Kabul Et' "
                            + "or normalize-space()='Kabul et' "
                            + "or normalize-space()='Kabul Et' "
                            + "or normalize-space()='Tüm Çerezleri Kabul Et']"
            )
    );

    protected final WebDriver driver;
    protected final TestConfig config;
    protected final JavascriptExecutor javascript;

    protected BasePage(WebDriver driver, TestConfig config) {
        this.driver = driver;
        this.config = config;
        this.javascript = (JavascriptExecutor) driver;
    }

    protected WebElement waitUntilVisible(By... locators) {
        return waitFor(config.timeout(), "Görünür öğe bulunamadı", webDriver -> {
            for (By locator : locators) {
                for (WebElement element : webDriver.findElements(locator)) {
                    try {
                        if (element.isDisplayed()) {
                            return element;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // DOM güncellenirken bir sonraki polling denemesinde tekrar aranır.
                    }
                }
            }
            return null;
        }, locators);
    }

    protected WebElement waitUntilClickable(By... locators) {
        return waitFor(config.timeout(), "Tıklanabilir öğe bulunamadı", webDriver -> {
            for (By locator : locators) {
                for (WebElement element : webDriver.findElements(locator)) {
                    try {
                        if (element.isDisplayed() && element.isEnabled()) {
                            return element;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // DOM güncellenirken bir sonraki polling denemesinde tekrar aranır.
                    }
                }
            }
            return null;
        }, locators);
    }

    protected <T> T waitFor(
            Duration duration,
            String failureMessage,
            Function<WebDriver, T> condition,
            By... relatedLocators
    ) {
        try {
            return new FluentWait<>(driver)
                    .withTimeout(duration)
                    .pollingEvery(Duration.ofMillis(200))
                    .ignoring(NoSuchElementException.class)
                    .ignoring(StaleElementReferenceException.class)
                    .until(condition);
        } catch (TimeoutException exception) {
            String suffix = relatedLocators.length == 0
                    ? ""
                    : " Locatorlar: " + Arrays.toString(relatedLocators);
            throw new AssertionError(
                    failureMessage + "." + suffix + " URL: " + sanitizedCurrentUrl(),
                    exception
            );
        }
    }

    protected void click(By... locators) {
        WebElement element = waitUntilClickable(locators);
        click(element, locators);
    }

    protected void click(WebElement element, By... refindLocators) {
        scrollIntoView(element);
        try {
            element.click();
            return;
        } catch (ElementClickInterceptedException exception) {
            acceptCookiePreferencesIfPresent();
        } catch (StaleElementReferenceException exception) {
            if (refindLocators.length == 0) {
                throw exception;
            }
            element = waitUntilClickable(refindLocators);
            scrollIntoView(element);
        }

        try {
            new Actions(driver)
                    .moveToElement(element)
                    .pause(Duration.ofMillis(100))
                    .click()
                    .perform();
        } catch (RuntimeException actionsFailure) {
            javascript.executeScript("arguments[0].click();", element);
        }
    }

    protected boolean tryClick(Duration duration, By... locators) {
        try {
            WebElement element = waitFor(duration, "İsteğe bağlı öğe bulunamadı", webDriver -> {
                for (By locator : locators) {
                    for (WebElement candidate : webDriver.findElements(locator)) {
                        if (candidate.isDisplayed() && candidate.isEnabled()) {
                            return candidate;
                        }
                    }
                }
                return null;
            }, locators);
            click(element, locators);
            return true;
        } catch (AssertionError | RuntimeException ignored) {
            return false;
        }
    }

    protected void scrollIntoView(WebElement element) {
        javascript.executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                element
        );
    }

    protected void movePointerToNeutralViewportPosition() {
        // Sayfa değiştiğinde eski pointer koordinatının altındaki kategoriye
        // CSS :hover taşınmaması için viewport'un boş sol üst köşesine gider.
        new Actions(driver)
                .moveToLocation(1, 1)
                .perform();
    }

    protected void scrollToTop() {
        javascript.executeScript("window.scrollTo({top: 0, behavior: 'instant'});");
        waitFor(Duration.ofSeconds(3), "Sayfanın en üstüne çıkılamadı", webDriver -> {
            Object value = javascript.executeScript(
                    "return Math.max(window.scrollY || 0, document.documentElement.scrollTop || 0);"
            );
            return value instanceof Number number && number.longValue() <= 5 ? true : null;
        });
    }

    protected void waitForDocumentReady() {
        waitFor(config.timeout(), "Sayfa yüklenmesi tamamlanmadı", webDriver -> {
            Object state = javascript.executeScript("return document.readyState;");
            return "interactive".equals(state) || "complete".equals(state) ? true : null;
        });
    }

    protected boolean acceptCookiePreferencesIfPresent() {
        return waitForAndAcceptCookiePreferences(Duration.ZERO);
    }

    protected boolean waitForAndAcceptCookiePreferences(Duration optionalWait) {
        if (optionalWait.isNegative()) {
            throw new IllegalArgumentException("Çerez bekleme süresi negatif olamaz.");
        }

        WebElement acceptControl;
        if (optionalWait.isZero()) {
            acceptControl = findCookieAcceptControl();
        } else {
            try {
                acceptControl = waitFor(
                        optionalWait,
                        "İsteğe bağlı çerez kabul kontrolü görünmedi",
                        webDriver -> findCookieAcceptControl(),
                        COOKIE_ACCEPT_CONTROLS.toArray(By[]::new)
                );
            } catch (AssertionError bannerNotShown) {
                return false;
            }
        }

        if (acceptControl == null) {
            return false;
        }

        try {
            acceptControl.click();
        } catch (StaleElementReferenceException staleControl) {
            acceptControl = findCookieAcceptControl();
            if (acceptControl == null) {
                return true;
            }
            acceptControl.click();
        } catch (ElementNotInteractableException clickFailure) {
            javascript.executeScript("arguments[0].click();", acceptControl);
        }

        waitFor(
                Duration.ofSeconds(2),
                "Çerez tercihleri kabul edildikten sonra bildirim kapanmadı",
                webDriver -> findCookieAcceptControl() == null ? true : null,
                COOKIE_ACCEPT_CONTROLS.toArray(By[]::new)
        );
        System.out.println("[BİLGİ] Çerez tercihleri kabul edildi.");
        return true;
    }

    private WebElement findCookieAcceptControl() {
        for (By locator : COOKIE_ACCEPT_CONTROLS) {
            for (WebElement element : driver.findElements(locator)) {
                try {
                    if (element.isDisplayed() && element.isEnabled()) {
                        return element;
                    }
                } catch (StaleElementReferenceException ignored) {
                    return null;
                }
            }
        }

        try {
            Object shadowControl = javascript.executeScript(
                    FIND_SHADOW_COOKIE_ACCEPT_CONTROL
            );
            if (shadowControl instanceof WebElement element) {
                return element;
            }
        } catch (RuntimeException ignored) {
            // Efilli bileşeni yeniden çizilirken sonraki polling turunda tekrar aranır.
        }
        return null;
    }

    protected void typeControlled(WebElement element, String value) {
        element.click();
        element.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        element.sendKeys(Keys.BACK_SPACE);

        Actions actions = new Actions(driver);
        for (int index = 0; index < value.length(); index++) {
            actions.sendKeys(String.valueOf(value.charAt(index)));
            if (!config.typingDelay().isZero()) {
                actions.pause(config.typingDelay());
            }
        }
        actions.perform();
    }

    protected boolean anyDisplayed(By... locators) {
        for (By locator : locators) {
            for (WebElement element : driver.findElements(locator)) {
                try {
                    if (element.isDisplayed()) {
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Başka bir eşleşme aranır.
                }
            }
        }
        return false;
    }

    protected List<WebElement> displayedElements(By... locators) {
        Set<WebElement> unique = new LinkedHashSet<>();
        for (By locator : locators) {
            for (WebElement element : driver.findElements(locator)) {
                try {
                    if (element.isDisplayed()) {
                        unique.add(element);
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Başka bir eşleşme aranır.
                }
            }
        }
        return new ArrayList<>(unique);
    }

    protected void observeForSeconds(int seconds) {
        if (seconds < 0 || seconds > 10) {
            throw new IllegalArgumentException("Gözlem süresi 0 ile 10 saniye arasında olmalıdır.");
        }
        new Actions(driver)
                .pause(Duration.ofSeconds(seconds))
                .perform();
    }

    protected String sanitizedCurrentUrl() {
        String url;
        try {
            url = driver.getCurrentUrl();
        } catch (RuntimeException unavailable) {
            return "[tarayıcı adresine ulaşılamıyor]";
        }
        int queryIndex = url.indexOf('?');
        int fragmentIndex = url.indexOf('#');
        int cutIndex = url.length();
        if (queryIndex >= 0) {
            cutIndex = Math.min(cutIndex, queryIndex);
        }
        if (fragmentIndex >= 0) {
            cutIndex = Math.min(cutIndex, fragmentIndex);
        }
        return url.substring(0, cutIndex);
    }

    protected static String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return decomposed
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    protected static boolean meaningfullyMatches(String first, String second) {
        String left = normalizeText(first);
        String right = normalizeText(second);
        if (left.isBlank() || right.isBlank()) {
            return false;
        }
        if (left.contains(right) || right.contains(left)) {
            return true;
        }

        Set<String> leftTokens = Arrays.stream(left.split(" "))
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
        Set<String> rightTokens = Arrays.stream(right.split(" "))
                .filter(token -> token.length() >= 3)
                .collect(Collectors.toSet());
        long overlap = leftTokens.stream().filter(rightTokens::contains).count();
        int required = Math.min(3, Math.min(leftTokens.size(), rightTokens.size()));
        return required > 0 && overlap >= required;
    }
}
