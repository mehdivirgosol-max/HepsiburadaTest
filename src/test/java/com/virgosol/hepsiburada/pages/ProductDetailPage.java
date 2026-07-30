package com.virgosol.hepsiburada.pages;

import com.virgosol.hepsiburada.config.TestConfig;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;

import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.ProductDetail.ADD_TO_CART;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.ProductDetail.ADD_TO_CART_TEXT;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.ProductDetail.GO_TO_CART_TEST_ID;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.ProductDetail.GO_TO_CART_TEXT;

public final class ProductDetailPage extends BasePage {
    private static final Duration ADD_TO_CART_BUTTON_SETTLE_TIME =
            Duration.ofSeconds(2);
    private static final Duration MINIMUM_CART_MODAL_TIMEOUT =
            Duration.ofSeconds(45);

    public ProductDetailPage(WebDriver driver, TestConfig config) {
        super(driver, config);
    }

    public void addToCart() {
        config.assertTrustedUrl(driver.getCurrentUrl(), "Sepete ekleme");
        waitForDocumentComplete();

        WebElement addToCartButton = waitUntilClickable(
                ADD_TO_CART,
                ADD_TO_CART_TEXT
        );
        scrollIntoView(addToCartButton);

        // Scroll, ürün detayındaki tembel yüklenen bileşenleri yeniden çizebilir.
        // Güncel butonu yeniden bulmadan önce DOM'un stabil hale gelmesini bekler.
        pause(ADD_TO_CART_BUTTON_SETTLE_TIME);
        clickWithoutScrolling(ADD_TO_CART, ADD_TO_CART_TEXT);

        waitUntilClickable(
                cartModalTimeout(),
                "Sepete ekleme tamamlandı ancak Sepete git penceresi açılmadı",
                GO_TO_CART_TEXT,
                GO_TO_CART_TEST_ID
        );
    }

    public void goToCart() {
        clickWithoutScrolling(GO_TO_CART_TEXT, GO_TO_CART_TEST_ID);
        waitFor(
                config.timeout(),
                "Sepete git düğmesi sepet sayfasını açmadı",
                webDriver -> isCartUrl(webDriver.getCurrentUrl()) ? true : null,
                GO_TO_CART_TEXT,
                GO_TO_CART_TEST_ID
        );
        config.assertTrustedUrl(driver.getCurrentUrl(), "Sepete git yönlendirmesi");
        waitForDocumentReady();
    }

    private Duration cartModalTimeout() {
        return config.timeout().compareTo(MINIMUM_CART_MODAL_TIMEOUT) >= 0
                ? config.timeout()
                : MINIMUM_CART_MODAL_TIMEOUT;
    }

    private boolean isCartUrl(String url) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null
                    ? ""
                    : uri.getHost().toLowerCase(Locale.ROOT);
            String path = uri.getPath() == null
                    ? ""
                    : uri.getPath().toLowerCase(Locale.ROOT);
            boolean checkoutCart = "checkout.hepsiburada.com".equals(host)
                    && (path.isBlank()
                    || "/".equals(path)
                    || path.startsWith("/sepetim"));
            return checkoutCart || path.startsWith("/sepetim");
        } catch (IllegalArgumentException invalidUrl) {
            return false;
        }
    }
}
