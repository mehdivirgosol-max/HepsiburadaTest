package com.virgosol.hepsiburada.pages;

import com.virgosol.hepsiburada.config.TestConfig;
import com.virgosol.hepsiburada.model.SelectedProduct;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Cart.CART_COUNT;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Cart.CART_ITEMS;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Cart.CART_LIVE_REGION;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Cart.CONFIRM_REMOVE;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Cart.DECREASE_WITHIN_CART_ITEM;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Cart.EMPTY_CART;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Cart.PRODUCT_LINKS;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Cart.REMOVE_WITHIN_CART_ITEM;
import static com.virgosol.hepsiburada.locators.HepsiburadaLocators.Common.PAGE_BODY;

public final class CartPage extends BasePage {
    private static final String CART_URL = "https://checkout.hepsiburada.com/sepetim";
    private static final Pattern CART_ITEM_COUNT = Pattern.compile(
            "\\bsepetim\\s+(\\d+)\\s+urun\\b"
    );
    public CartPage(WebDriver driver, TestConfig config) {
        super(driver, config);
    }

    public void open() {
        config.assertTrustedUrl(CART_URL, "Sepet temizliği");
        driver.get(CART_URL);
        waitForDocumentReady();
        waitUntilLoaded();
    }

    public void waitUntilLoaded() {
        waitForAndAcceptCookiePreferences(Duration.ofSeconds(2));
        waitFor(config.timeout(), "Sepet içeriği yüklenmedi", webDriver -> {
            for (WebElement region : webDriver.findElements(CART_LIVE_REGION)) {
                String busy = region.getDomAttribute("aria-busy");
                if ("true".equalsIgnoreCase(busy)) {
                    return null;
                }
            }
            if (isEmptyStateVisible() || hasCartItems() || visibleCartCountAboveZero()) {
                return true;
            }
            return null;
        }, CART_ITEMS, EMPTY_CART);
    }

    public void verifyContains(SelectedProduct selectedProduct) {
        waitFor(config.timeout(), "Kaydedilen ürün sepette bulunamadı", webDriver -> {
            if (isEmptyStateVisible()) {
                return null;
            }
            return findMatchingProductLink(selectedProduct) != null ? true : null;
        }, PRODUCT_LINKS, CART_ITEMS);

        if (!hasCartItems() && !visibleCartCountAboveZero()) {
            WebElement matchingLink = findMatchingProductLink(selectedProduct);
            if (matchingLink == null) {
                throw new AssertionError("Sepetin boş olmadığı doğrulanamadı.");
            }
        }
    }

    public void removeOneUnit(SelectedProduct selectedProduct) {
        Integer currentCount = waitFor(
                config.timeout(),
                "Sepet ürün sayısı okunamadı",
                webDriver -> visibleCartItemCount()
        );
        if (currentCount < 1) {
            throw new AssertionError("Sepette azaltılabilecek ürün bulunamadı.");
        }
        int expectedCount = currentCount - 1;

        CartAdjustment adjustment = waitFor(
                config.timeout(),
                "Kaydedilen ürüne ait azaltma veya silme kontrolü bulunamadı",
                webDriver -> findCartAdjustmentFor(selectedProduct),
                PRODUCT_LINKS,
                CART_ITEMS
        );
        click(adjustment.control());

        if (adjustment.removesLine()) {
            boolean confirmationClicked = tryClick(
                    Duration.ofSeconds(3),
                    CONFIRM_REMOVE
            );
            if (!confirmationClicked
                    && !waitForItemCount(Duration.ofSeconds(1), expectedCount)) {
                throw new AssertionError(
                        "Miktarı 1 olan ürün için Sil onayı bulunamadı."
                );
            }
        }
        waitUntilItemCountEquals(expectedCount);
    }

    private void waitUntilItemCountEquals(int expectedCount) {
        waitFor(config.timeout(), "Sepet ürün sayısı başlangıç değerine dönmedi", webDriver -> {
            if (expectedCount == 0 && isEmptyStateVisible()) {
                return true;
            }
            Integer actualCount = visibleCartItemCount();
            return actualCount != null && actualCount == expectedCount ? true : null;
        });
    }

    private WebElement findMatchingProductLink(SelectedProduct selectedProduct) {
        for (WebElement link : driver.findElements(PRODUCT_LINKS)) {
            try {
                String href = link.getDomProperty("href");
                if (href == null || href.isBlank()) {
                    continue;
                }
                config.assertTrustedUrl(href, "Sepet ürün bağlantısı doğrulaması");
                String text = link.getText();
                boolean codeMatches = !selectedProduct.code().isBlank()
                        && href.toUpperCase(Locale.ROOT).contains(selectedProduct.code());
                boolean nameMatches = meaningfullyMatches(selectedProduct.name(), text);
                if (link.isDisplayed() && (codeMatches || nameMatches)) {
                    return link;
                }
            } catch (StaleElementReferenceException ignored) {
                // Güncellenen sepet DOM'u bir sonraki polling turunda tekrar aranır.
            }
        }
        return null;
    }

    private CartAdjustment findCartAdjustmentFor(SelectedProduct selectedProduct) {
        WebElement productLink = findMatchingProductLink(selectedProduct);
        if (productLink == null) {
            return null;
        }

        WebElement cartItem = nearestCartItem(productLink);
        for (WebElement control : cartItem.findElements(DECREASE_WITHIN_CART_ITEM)) {
            try {
                if (control.isDisplayed() && control.isEnabled()) {
                    return new CartAdjustment(control, false);
                }
            } catch (StaleElementReferenceException ignored) {
                return null;
            }
        }

        for (WebElement control : cartItem.findElements(REMOVE_WITHIN_CART_ITEM)) {
            try {
                if (control.isDisplayed() && control.isEnabled()) {
                    return new CartAdjustment(control, true);
                }
            } catch (StaleElementReferenceException ignored) {
                return null;
            }
        }
        return null;
    }

    private boolean waitForItemCount(Duration duration, int expectedCount) {
        try {
            waitFor(duration, "Sepet ürün sayısı henüz güncellenmedi", webDriver -> {
                if (expectedCount == 0 && isEmptyStateVisible()) {
                    return true;
                }
                Integer actualCount = visibleCartItemCount();
                return actualCount != null && actualCount == expectedCount ? true : null;
            });
            return true;
        } catch (AssertionError countNotUpdatedYet) {
            return false;
        }
    }

    private WebElement nearestCartItem(WebElement productLink) {
        Object cartItem = javascript.executeScript(
                "return arguments[0].closest("
                        + "'li[class*=\"basket_items_\"],[class*=\"basket_item_\"],"
                        + "[data-test-id=\"cart-item\"],"
                        + "[data-test-id=\"product-item\"],[data-test-id*=\"basket-item\"]'"
                        + ") || arguments[0];",
                productLink
        );
        return cartItem instanceof WebElement element ? element : productLink;
    }

    private boolean hasCartItems() {
        return anyDisplayed(CART_ITEMS);
    }

    private boolean visibleCartCountAboveZero() {
        for (WebElement element : driver.findElements(CART_COUNT)) {
            try {
                String digits = element.getText().replaceAll("\\D+", "");
                if (element.isDisplayed() && !digits.isBlank()) {
                    return Integer.parseInt(digits) > 0;
                }
            } catch (RuntimeException ignored) {
                // Sayaç güncellenirken sonraki eşleşme kontrol edilir.
            }
        }
        return false;
    }

    private Integer visibleCartItemCount() {
        try {
            String bodyText = normalizeText(driver.findElement(PAGE_BODY).getText());
            Matcher matcher = CART_ITEM_COUNT.matcher(bodyText);
            return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private boolean isEmptyStateVisible() {
        if (anyDisplayed(EMPTY_CART)) {
            return true;
        }

        String bodyText;
        try {
            bodyText = normalizeText(driver.findElement(PAGE_BODY).getText());
        } catch (RuntimeException exception) {
            return false;
        }
        return bodyText.contains("sepetin su an bos")
                || bodyText.contains("sepetiniz su an bos")
                || bodyText.contains("sepetiniz bos")
                || bodyText.contains("sepetinde urun bulunmuyor")
                || bodyText.contains("sepetinizde urun bulunmamaktadir");
    }

    private record CartAdjustment(WebElement control, boolean removesLine) {
    }
}
