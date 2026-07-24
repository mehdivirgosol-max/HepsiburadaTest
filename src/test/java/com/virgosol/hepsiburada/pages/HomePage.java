package com.virgosol.hepsiburada.pages;

import com.virgosol.hepsiburada.config.TestConfig;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;

public final class HomePage extends BasePage {
    private static final By ACCOUNT = By.id("myAccount");
    private static final By ACCOUNT_CONTROL = By.cssSelector(
            "#myAccount [data-test-id='account']"
    );
    private static final By LOGIN = By.id("login");
    private static final By SEARCH_REGION = By.cssSelector(
            "div[role='search'][aria-label='Site İçi Arama']"
    );
    private static final By SEARCH_INPUT = By.cssSelector(
            "input[data-test-id='search-bar-input']"
    );
    private static final By AUTHENTICATED_ACCOUNT_LINK = By.cssSelector(
            "#myAccount a[href*='hesabim'],"
                    + "#myAccount a[href*='siparis'],"
                    + "#myAccount a[href*='account'],"
                    + "#myAccount #logout"
    );
    private static final By CART = By.cssSelector(
            "#shoppingCart,"
                    + "a[href*='checkout.hepsiburada.com/sepetim'],"
                    + "a[href*='/sepetim'],"
                    + "[data-test-id='cart-icon']"
    );

    public HomePage(WebDriver driver, TestConfig config) {
        super(driver, config);
    }

    public void open() {
        driver.get(config.baseUri().toString());
        waitForDocumentReady();
        if (!waitForAndAcceptCookiePreferences(Duration.ofSeconds(2))) {
            System.out.println("[BİLGİ] Ana sayfada görünür çerez banner'ı bulunmadı.");
        }
        waitUntilVisible(ACCOUNT, SEARCH_INPUT, SEARCH_REGION);
    }

    public void openAccountMenu() {
        acceptCookiePreferencesIfPresent();
        WebElement account = waitUntilClickable(ACCOUNT_CONTROL, ACCOUNT);
        click(account, ACCOUNT_CONTROL, ACCOUNT);

        if (!anyDisplayed(LOGIN, AUTHENTICATED_ACCOUNT_LINK)) {
            new Actions(driver)
                    .moveToElement(waitUntilVisible(ACCOUNT))
                    .pause(Duration.ofMillis(150))
                    .perform();
        }

        waitFor(
                Duration.ofSeconds(Math.min(8, config.timeout().toSeconds())),
                "Hesabım menüsü açılmadı",
                webDriver -> anyDisplayed(LOGIN, AUTHENTICATED_ACCOUNT_LINK) ? true : null,
                ACCOUNT,
                LOGIN
        );
    }

    public void openLoginPage() {
        click(LOGIN);
        waitForDocumentReady();
        if (!waitForAndAcceptCookiePreferences(Duration.ofSeconds(2))) {
            System.out.println("[BİLGİ] Giriş sayfasında görünür çerez banner'ı bulunmadı.");
        }
        waitUntilVisible(By.id("txtUserName"));
    }

    public void verifyLoggedIn() {
        waitFor(config.timeout(), "Oturum açıldığı doğrulanamadı", webDriver -> {
            if (anyDisplayed(By.id("txtUserName"), By.id("txtPassword"))) {
                return null;
            }
            return anyDisplayed(SEARCH_INPUT, SEARCH_REGION) ? true : null;
        }, SEARCH_INPUT, SEARCH_REGION);
    }

    public void openSearch() {
        WebElement searchInput = waitUntilClickable(SEARCH_INPUT);
        movePointerToNeutralViewportPosition();
        javascript.executeScript(
                "arguments[0].focus({preventScroll: true});",
                searchInput
        );
    }

    public WebElement searchInput() {
        return waitUntilClickable(SEARCH_INPUT);
    }

    public void openCartFromHeader() {
        scrollToTop();
        acceptCookiePreferencesIfPresent();
        click(CART);
        waitForDocumentReady();
        waitFor(config.timeout(), "Sepet sayfası açılmadı", webDriver -> {
            String currentUrl = webDriver.getCurrentUrl().toLowerCase();
            return currentUrl.contains("sepet") || currentUrl.contains("cart") ? true : null;
        }, CART);
    }

}
