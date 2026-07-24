package com.virgosol.hepsiburada.pages;

import com.virgosol.hepsiburada.config.TestConfig;
import com.virgosol.hepsiburada.model.SelectedProduct;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CartPage extends BasePage {
    private static final Pattern CART_ITEM_COUNT = Pattern.compile(
            "\\bsepetim\\s+(\\d+)\\s+urun\\b"
    );
    private static final By CART_COUNT = By.cssSelector(
            "#basket-item-count, #cartItemCount"
    );
    private static final By CART_LIVE_REGION = By.cssSelector(
            "[aria-live='polite'][aria-busy]"
    );
    private static final By PRODUCT_LINKS = By.cssSelector(
            "li[class*='basket_items_'] [class*='product_name_'] a[href],"
                    + "li[class*='basket_items_'] a[href*='-p-'],"
                    + "[class*='basket_item_'] [class*='product_name_'] a[href],"
                    + "[data-test-id='cart-item'] a[href*='-p-'],"
                    + "[data-test-id='product-item'] a[href*='-p-'],"
                    + "[data-test-id*='basket-item'] a[href*='-p-']"
    );
    private static final By CART_ITEMS = By.cssSelector(
            "li[class*='basket_items_'],"
                    + "[class*='basket_item_'],"
                    + "[data-test-id='cart-item'],"
                    + "[data-test-id='product-item'],"
                    + "[data-test-id*='basket-item'],"
                    + "main [class*='CartItem'],"
                    + "main [class*='cartItem'],"
                    + "main [class*='product_item']"
    );
    private static final By EMPTY_CART = By.cssSelector(
            "[data-test-id='empty-cart'],"
                    + "[data-test-id='empty-basket'],"
                    + "[class*='EmptyCart'],"
                    + "[class*='emptyCart']"
    );
    private static final By DECREASE_WITHIN_CART_ITEM = By.cssSelector(
            "a[aria-label='Ürünü Azalt'],"
                    + "button[aria-label='Ürünü Azalt'],"
                    + "a[aria-label*='Azalt'],"
                    + "button[aria-label*='Azalt']"
    );
    private static final By REMOVE_WITHIN_CART_ITEM = By.cssSelector(
            "a[aria-label='Sepetten Çıkar'],"
                    + "button[aria-label='Sepetten Çıkar'],"
                    + "a[class*='trash_button_'],"
                    + "button[class*='trash_button_']"
    );
    private static final By CONFIRM_REMOVE = By.xpath(
            "//button[contains(@class, 'favoritesButton_') "
                    + "and @kind='secondary' and normalize-space()='Sil']"
                    + " | //*[@role='dialog']//*[self::button or self::a]["
                    + "normalize-space()='Sil' or "
                    + "normalize-space()='Ürünü sil' or "
                    + "normalize-space()='Evet, sil' or "
                    + "normalize-space()='Evet']"
                    + " | //*[contains(@class, 'favoritesContainer_')]"
                    + "//*[self::button or self::a][normalize-space()='Sil']"
    );

    public CartPage(WebDriver driver, TestConfig config) {
        super(driver, config);
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

    public void removeOneUnitAndRestoreCount(
            SelectedProduct selectedProduct,
            int expectedCount
    ) {
        Integer currentCount = waitFor(
                config.timeout(),
                "Sepet ürün sayısı okunamadı",
                webDriver -> visibleCartItemCount()
        );
        if (currentCount == expectedCount) {
            return;
        }
        if (currentCount != expectedCount + 1) {
            throw new AssertionError(
                    "Sepet sayısı beklenmeyen değerde; ürün miktarı değiştirilmeyecek. "
                            + "Beklenen mevcut sayı: " + (expectedCount + 1)
                            + ", gerçek sayı: " + currentCount
            );
        }

        CartAdjustment adjustment = waitFor(
                config.timeout(),
                "Kaydedilen ürüne ait azaltma veya silme kontrolü bulunamadı",
                webDriver -> findCartAdjustmentFor(selectedProduct),
                PRODUCT_LINKS,
                CART_ITEMS
        );
        click(adjustment.control());

        if (adjustment.removesLine()) {
            if (!waitForItemCount(Duration.ofSeconds(1), expectedCount)
                    && !tryClick(Duration.ofSeconds(3), CONFIRM_REMOVE)) {
                throw new AssertionError(
                        "Miktarı 1 olan ürün için Sil onayı bulunamadı."
                );
            }
            System.out.println(
                    "[BİLGİ] Ürün miktarı 1 olduğu için ürün satırı sepetten silindi."
            );
        } else {
            System.out.println(
                    "[BİLGİ] Ürün miktarı 1'den fazla olduğu için yalnızca bir adet azaltıldı."
            );
        }
        verifyItemCount(expectedCount);
    }

    public void verifyItemCount(int expectedCount) {
        waitFor(config.timeout(), "Sepet ürün sayısı başlangıç değerine dönmedi", webDriver -> {
            if (expectedCount == 0 && isEmptyStateVisible()) {
                return true;
            }
            Integer actualCount = visibleCartItemCount();
            return actualCount != null && actualCount == expectedCount ? true : null;
        });
    }

    public void verifyEmpty() {
        waitFor(config.timeout(), "Sepetin boş olduğu doğrulanamadı", webDriver ->
                isEmptyStateVisible() ? true : null, EMPTY_CART, CART_COUNT);
    }

    public void observe(int seconds) {
        observeForSeconds(seconds);
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
            String bodyText = normalizeText(driver.findElement(By.tagName("body")).getText());
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
            bodyText = normalizeText(driver.findElement(By.tagName("body")).getText());
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
