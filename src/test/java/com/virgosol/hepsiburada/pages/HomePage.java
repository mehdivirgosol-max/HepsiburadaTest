package com.virgosol.hepsiburada.pages;

import com.virgosol.hepsiburada.config.TestConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.time.Duration;

import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Authentication.EMAIL_INPUT;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Authentication.PASSWORD_INPUT;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Header.ACCOUNT;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Header.ACCOUNT_CONTROL;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Header.AUTHENTICATED_ACCOUNT_LINK;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Header.LOGIN_LINK;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Search.SEARCH_INPUT;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Search.SEARCH_REGION;

public final class HomePage extends BasePage {
    public HomePage(WebDriver driver, TestConfig config) {
        super(driver, config);
    }

    public void open() {
        driver.get(config.baseUri().toString());
        waitForDocumentReady();
        waitForAndAcceptCookiePreferences(Duration.ofSeconds(2));
        waitUntilVisible(ACCOUNT, SEARCH_INPUT, SEARCH_REGION);
    }

    public void openAccountMenu() {
        acceptCookiePreferencesIfPresent();
        WebElement account = waitUntilClickable(ACCOUNT_CONTROL, ACCOUNT);
        click(account, ACCOUNT_CONTROL, ACCOUNT);

        if (!anyDisplayed(LOGIN_LINK, AUTHENTICATED_ACCOUNT_LINK)) {
            new Actions(driver)
                    .moveToElement(waitUntilVisible(ACCOUNT))
                    .pause(Duration.ofMillis(150))
                    .perform();
        }

        waitFor(
                Duration.ofSeconds(Math.min(8, config.timeout().toSeconds())),
                "Hesabım menüsü açılmadı",
                webDriver -> anyDisplayed(LOGIN_LINK, AUTHENTICATED_ACCOUNT_LINK) ? true : null,
                ACCOUNT,
                LOGIN_LINK
        );
    }

    public void openLoginPage() {
        click(LOGIN_LINK);
        waitForDocumentReady();
        waitForAndAcceptCookiePreferences(Duration.ofSeconds(2));
        waitUntilVisible(EMAIL_INPUT);
    }

    public void verifyLoggedIn() {
        waitFor(config.timeout(), "Oturum açılan kullanıcı bilgisi gösterilmedi", webDriver -> {
            if (anyDisplayed(EMAIL_INPUT, PASSWORD_INPUT)) {
                return null;
            }

            boolean authenticatedLinkPresent =
                    !webDriver.findElements(AUTHENTICATED_ACCOUNT_LINK).isEmpty();
            for (WebElement account : displayedElements(ACCOUNT)) {
                String accountText = normalizeText(account.getText());
                boolean signedOutPrompt = accountText.contains("giris yap")
                        || accountText.contains("uye ol");
                boolean userNameVisible = !accountText
                        .replace("hesabim", "")
                        .trim()
                        .isBlank();
                if (!signedOutPrompt
                        && anyDisplayed(SEARCH_INPUT, SEARCH_REGION)
                        && (userNameVisible || authenticatedLinkPresent)) {
                    return true;
                }
            }
            return null;
        }, ACCOUNT, AUTHENTICATED_ACCOUNT_LINK);
    }

    public void openSearch() {
        WebElement searchInput = waitUntilClickable(SEARCH_INPUT);
        movePointerToNeutralViewportPosition();
        click(searchInput, SEARCH_INPUT);
        new Actions(driver)
                .pause(Duration.ofSeconds(1))
                .perform();
    }

}
