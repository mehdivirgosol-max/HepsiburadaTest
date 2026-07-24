package com.virgosol.hepsiburada.pages;

import com.virgosol.hepsiburada.config.TestConfig;
import com.virgosol.hepsiburada.model.SelectedProduct;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

public final class ProductDetailPage extends BasePage {
    private static final By PRODUCT_TITLE = By.cssSelector(
            "h1[data-test-id='title'],"
                    + "h1[data-test-id='product-name'],"
                    + "h1#product-name,"
                    + "main h1"
    );
    private static final By ADD_TO_CART = By.cssSelector(
            "button[data-test-id='addToCart'],"
                    + "[data-test-id='add-to-cart'] button,"
                    + "button#addToCart,"
                    + "button[data-test-id='add-to-cart']"
    );
    private static final By ADD_TO_CART_TEXT = By.xpath(
            "//button[normalize-space()='Sepete Ekle' or .//*[normalize-space()='Sepete Ekle']]"
    );
    private static final By CART_COUNT = By.cssSelector(
            "#cartItemCount, #basket-item-count"
    );
    private static final List<By> CONFIRMATION_SURFACES = List.of(
            By.cssSelector(".hb-toast-notifier-box.hb-toast-notifier-type-success"),
            By.cssSelector("[data-test-class='success']"),
            By.cssSelector("[role='alert']"),
            By.cssSelector("[data-test-id*='toast']"),
            By.cssSelector("[class*='toast']"),
            By.cssSelector("[data-test-id*='added-to-cart']"),
            By.cssSelector("[data-test-id*='success']")
    );

    private String confirmationMessage;

    public ProductDetailPage(WebDriver driver, TestConfig config) {
        super(driver, config);
    }

    public String verifyProduct(SelectedProduct selectedProduct) {
        WebElement title = waitUntilVisible(PRODUCT_TITLE);
        String detailName = title.getText().trim();
        String currentUrl = driver.getCurrentUrl();
        config.assertTrustedUrl(currentUrl, "Ürün detay doğrulaması");

        boolean codeMatches = !selectedProduct.code().isBlank()
                && currentUrl.toUpperCase(Locale.ROOT).contains(selectedProduct.code());
        boolean nameMatches = meaningfullyMatches(selectedProduct.name(), detailName);
        if (!codeMatches && !nameMatches) {
            throw new AssertionError(
                    "Ürün detay sayfası seçilen ürünle eşleşmiyor. "
                            + "Seçilen: " + selectedProduct.name()
                            + ", detay: " + detailName
            );
        }
        return detailName;
    }

    public int addToCart() {
        Integer visibleCount = visibleCartCount();
        click(ADD_TO_CART, ADD_TO_CART_TEXT);
        return visibleCount == null ? 0 : visibleCount;
    }

    public String verifyAddToCartConfirmation(int countBeforeAdding) {
        confirmationMessage = waitFor(
                config.timeout(),
                "Sepete ekleme bildirimi veya sayaç değişikliği doğrulanamadı",
                webDriver -> {
                    String message = visibleConfirmationMessage();
                    if (!message.isBlank()) {
                        return message;
                    }

                    Integer currentCount = visibleCartCount();
                    if (currentCount != null
                            && currentCount > countBeforeAdding) {
                        return "Header sepet sayacı " + currentCount + " olarak güncellendi.";
                    }
                    return null;
                },
                CART_COUNT
        );
        return confirmationMessage;
    }

    public int verifyCartCount(int expectedCount) {
        return waitFor(
                config.timeout(),
                "Header sepet sayacı " + expectedCount + " olarak güncellenmedi",
                webDriver -> {
                    Integer currentCount = visibleCartCount();
                    return currentCount != null && currentCount == expectedCount
                            ? currentCount
                            : null;
                },
                CART_COUNT
        );
    }

    public String confirmationMessage() {
        return confirmationMessage == null ? "" : confirmationMessage;
    }

    private String visibleConfirmationMessage() {
        for (By locator : CONFIRMATION_SURFACES) {
            for (WebElement element : driver.findElements(locator)) {
                try {
                    if (!element.isDisplayed()) {
                        continue;
                    }
                    String text = element.getText().trim();
                    String normalized = normalizeText(text);
                    if (normalized.contains("sepete eklendi")
                            || normalized.contains("sepetine eklendi")
                            || normalized.contains("urun sepetinde")
                            || normalized.contains("urun sepetinizde")) {
                        return text;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Kısa ömürlü toast için diğer yüzeyler kontrol edilir.
                }
            }
        }
        return "";
    }

    private Integer visibleCartCount() {
        for (WebElement element : driver.findElements(CART_COUNT)) {
            try {
                if (!element.isDisplayed()) {
                    continue;
                }
                String digits = element.getText().replaceAll("\\D+", "");
                if (!digits.isBlank()) {
                    return Integer.parseInt(digits);
                }
            } catch (RuntimeException ignored) {
                // Sayaç DOM güncellemesi sırasında yeniden kontrol edilir.
            }
        }
        return null;
    }
}
