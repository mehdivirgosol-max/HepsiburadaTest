package com.virgosol.hepsiburada.pages;

import com.virgosol.hepsiburada.config.TestConfig;
import com.virgosol.hepsiburada.model.SelectedProduct;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SearchResultsPage extends BasePage {
    private static final int SAME_ROW_TOLERANCE_PX = 20;
    private static final int SAME_COLUMN_TOLERANCE_PX = 25;
    private static final Duration GRID_STABILITY_DURATION = Duration.ofSeconds(3);
    private static final Duration BEFORE_SCROLL_PAUSE = Duration.ofSeconds(1);
    private static final Duration AFTER_SCROLL_PAUSE = Duration.ofSeconds(1);
    private static final By SEARCH_INPUT = By.cssSelector(
            "input[data-test-id='search-bar-input']"
    );
    private static final By SEARCH_HEADER = By.cssSelector(
            "h1[data-test-id='header-h1']"
    );
    private static final By RESULT_GRID_CARDS = By.cssSelector(
            "main ul[id='1'] > li"
    );
    private static final String FIND_SECOND_ROW_FIRST_PRODUCT = """
            const grid = document.querySelector("main ul[id='1']");
            if (!grid) {
                return null;
            }

            const gridRect = grid.getBoundingClientRect();
            const titleSelector = [
                "a[data-test-id='product-card-name'][href]",
                "[data-test-id='product-card-name'] a[href]",
                "h2[data-test-id^='title-'] a[href]",
                "h2 a[href]",
                "h3 a[href]"
            ].join(",");
            const rowTolerance = Number(arguments[0]);
            const columnTolerance = Number(arguments[1]);
            const expectedUrl = String(arguments[2] || "");
            const clickRequested = Boolean(arguments[3]);
            const slots = [];

            for (const card of grid.children) {
                const rect = card.getBoundingClientRect();
                const cardStyle = window.getComputedStyle(card);
                if (card.tagName !== "LI"
                        || !card.id
                        || rect.width <= 0
                        || rect.height <= 0
                        || rect.width >= gridRect.width * 0.75
                        || cardStyle.display === "none"
                        || cardStyle.visibility === "hidden") {
                    continue;
                }

                slots.push({
                    card,
                    x: Math.round(rect.left + window.scrollX),
                    y: Math.round(rect.top + window.scrollY)
                });
            }

            if (slots.length < 2) {
                return null;
            }

            slots.sort((left, right) => left.y - right.y || left.x - right.x);
            const rows = [];
            for (const slot of slots) {
                const currentRow = rows.at(-1);
                if (!currentRow
                        || Math.abs(slot.y - currentRow.anchorY) > rowTolerance) {
                    rows.push({anchorY: slot.y, slots: [slot]});
                } else {
                    currentRow.slots.push(slot);
                }
            }
            if (rows.length < 2) {
                return null;
            }

            rows[0].slots.sort((left, right) => left.x - right.x);
            rows[1].slots.sort((left, right) => left.x - right.x);
            if (rows[0].slots.length !== rows[1].slots.length) {
                return null;
            }

            const firstColumnX = Math.min(
                ...rows[0].slots.map(slot => slot.x)
            );
            const targetSlot = rows[1].slots
                .filter(slot => Math.abs(slot.x - firstColumnX) <= columnTolerance)
                .sort((left, right) => left.x - right.x)[0];
            if (!targetSlot) {
                return null;
            }

            const firstTwoRows = rows
                .slice(0, 2)
                .flatMap(row => row.slots);
            const hydratedSlots = [];
            for (const slot of firstTwoRows) {
                const slotLink = slot.card.querySelector(titleSelector);
                const slotUrl = slotLink?.href || "";
                if (!slotLink || !slotUrl) {
                    return null;
                }
                hydratedSlots.push({
                    x: slot.x,
                    y: slot.y,
                    url: slotUrl
                });
            }
            const layoutSignature = hydratedSlots
                .map(slot => `${slot.url}@${slot.x}:${slot.y}`)
                .join("|");

            // İlk iki görsel satırın bütün ürün linkleri hydrate olmadan yerleşim
            // imzası kabul edilmez ve komşu bir karta fallback yapılmaz.
            const link = targetSlot.card.querySelector(titleSelector);
            if (!link) {
                return null;
            }

            const linkRect = link.getBoundingClientRect();
            const linkStyle = window.getComputedStyle(link);
            const heading = targetSlot.card.querySelector(
                "h2[data-test-id^='title-'],h2,h3"
            );
            const name = (
                link.innerText
                || link.getAttribute("title")
                || link.getAttribute("aria-label")
                || heading?.innerText
                || ""
            ).replace(/\\s+/g, " ").trim();
            const url = link.href || "";
            if (linkRect.width <= 0
                    || linkRect.height <= 0
                    || linkStyle.display === "none"
                    || linkStyle.visibility === "hidden"
                    || !url
                    || !name) {
                return null;
            }

            if (clickRequested) {
                if (url !== expectedUrl) {
                    return {
                        clickStatus: "candidate-changed",
                        name,
                        url,
                        x: targetSlot.x,
                        y: targetSlot.y,
                        layoutSignature
                    };
                }

                // Adayı yeniden hesaplama ve click aynı JavaScript görevi içinde
                // yapılır. React bu iki işlem arasına kart reflow'u sokamaz.
                link.click();
                return {
                    clickStatus: "clicked",
                    name,
                    url,
                    x: targetSlot.x,
                    y: targetSlot.y,
                    layoutSignature
                };
            }

            return {
                link,
                name,
                url,
                x: targetSlot.x,
                y: targetSlot.y,
                layoutSignature
            };
            """;

    public SearchResultsPage(WebDriver driver, TestConfig config) {
        super(driver, config);
    }

    public void searchFor(String query) {
        // Öneriye mouse ile tıklamak, öneri kapanınca alttaki kategoriye hover
        // bırakıyordu. Arama yalnızca input üzerinden klavyeyle gönderilir.
        enterSearchQuery(query);
    }

    private void enterSearchQuery(String query) {
        waitFor(config.timeout(), "Arama metni alana yazılamadı", webDriver -> {
            for (WebElement input : webDriver.findElements(SEARCH_INPUT)) {
                try {
                    if (!input.isDisplayed() || !input.isEnabled()) {
                        continue;
                    }
                    input.sendKeys(
                            Keys.chord(Keys.CONTROL, "a"),
                            query,
                            Keys.ENTER
                    );
                    return true;
                } catch (StaleElementReferenceException ignored) {
                    return null;
                }
            }
            return null;
        }, SEARCH_INPUT);
    }

    public void verifyResultsFor(String query) {
        waitFor(
                config.timeout(),
                "Arama sonuçları beklenen sorgu için yüklenmedi",
                webDriver -> {
                    if (!urlContainsSearchQuery(query) && !headerMatchesQuery(query)) {
                        return null;
                    }
                    return selectSecondRowFirstCandidate() != null ? true : null;
                },
                SEARCH_HEADER,
                RESULT_GRID_CARDS
        );
        movePointerToNeutralViewportPosition();
    }

    public SelectedProduct openFirstColumnProductInSecondVisualRow() {
        ProductCandidate candidate = waitForStableSecondRowFirstCandidate(
                "Arama sonuçlarında ikinci satırın birinci sütunundaki ürün "
                        + "stabil hale gelmedi"
        );

        Set<String> handlesBefore = new HashSet<>(driver.getWindowHandles());
        String urlBefore = driver.getCurrentUrl();
        new Actions(driver).pause(BEFORE_SCROLL_PAUSE).perform();
        try {
            scrollIntoView(candidate.link());
            new Actions(driver).pause(AFTER_SCROLL_PAUSE).perform();

            // Scroll sırasında sponsorlu kartlar yeniden sıralanabilir. İlk iki
            // satır yeniden stabil olmadan hiçbir eski WebElement tıklanmaz.
            candidate = waitForStableCandidateAndClick(
                    "Kaydırma sonrasında ikinci satırın birinci sütunundaki ürün "
                            + "stabil hale gelmedi"
            );
        } catch (StaleElementReferenceException staleElement) {
            candidate = waitForStableSecondRowFirstCandidate(
                    "İkinci satırın birinci sütunundaki ürün bağlantısı yenilenemedi"
            );
            scrollIntoView(candidate.link());
            new Actions(driver).pause(AFTER_SCROLL_PAUSE).perform();
            candidate = waitForStableCandidateAndClick(
                    "Yenilenen ikinci satır/birinci sütun ürünü stabil hale gelmedi"
            );
        }

        waitFor(config.timeout(), "Ürün başlık bağlantısı detay sayfasını açmadı", webDriver -> {
            Set<String> handlesAfter = webDriver.getWindowHandles();
            boolean newWindow = handlesAfter.stream().anyMatch(handle -> !handlesBefore.contains(handle));
            boolean urlChanged = !webDriver.getCurrentUrl().equals(urlBefore);
            return newWindow || urlChanged ? true : null;
        }, RESULT_GRID_CARDS);

        Set<String> handlesAfter = driver.getWindowHandles();
        handlesAfter.stream()
                .filter(handle -> !handlesBefore.contains(handle))
                .findFirst()
                .ifPresent(handle -> driver.switchTo().window(handle));
        waitForDocumentReady();
        String detailUrl = driver.getCurrentUrl();
        config.assertTrustedUrl(detailUrl, "Ürün detay bağlantısı");
        return SelectedProduct.from(candidate.name(), detailUrl);
    }

    private ProductCandidate waitForStableSecondRowFirstCandidate(String failureMessage) {
        String[] previousLayoutSignature = {null};
        long[] stableSinceNanos = {-1L};
        return waitFor(
                config.timeout(),
                failureMessage,
                webDriver -> observeStableCandidate(
                        previousLayoutSignature,
                        stableSinceNanos
                ),
                RESULT_GRID_CARDS
        );
    }

    private ProductCandidate waitForStableCandidateAndClick(String failureMessage) {
        String[] previousLayoutSignature = {null};
        long[] stableSinceNanos = {-1L};
        return waitFor(
                config.timeout(),
                failureMessage,
                webDriver -> {
                    ProductCandidate candidate = observeStableCandidate(
                            previousLayoutSignature,
                            stableSinceNanos
                    );
                    if (candidate == null) {
                        return null;
                    }

                    config.assertTrustedUrl(
                            candidate.url(),
                            "Arama sonucu ürün bağlantısı"
                    );
                    if (!clickCurrentCandidateIfUnchanged(candidate.url())) {
                        previousLayoutSignature[0] = null;
                        stableSinceNanos[0] = -1L;
                        return null;
                    }
                    return candidate;
                },
                RESULT_GRID_CARDS
        );
    }

    private ProductCandidate observeStableCandidate(
            String[] previousLayoutSignature,
            long[] stableSinceNanos
    ) {
        ProductCandidate current = selectSecondRowFirstCandidate();
        if (current == null) {
            previousLayoutSignature[0] = null;
            stableSinceNanos[0] = -1L;
            return null;
        }

        long now = System.nanoTime();
        if (!current.layoutSignature().equals(previousLayoutSignature[0])) {
            previousLayoutSignature[0] = current.layoutSignature();
            stableSinceNanos[0] = now;
            return null;
        }

        long stableNanos = now - stableSinceNanos[0];
        return stableNanos >= GRID_STABILITY_DURATION.toNanos()
                ? current
                : null;
    }

    private boolean clickCurrentCandidateIfUnchanged(String expectedUrl) {
        Object result = javascript.executeScript(
                FIND_SECOND_ROW_FIRST_PRODUCT,
                SAME_ROW_TOLERANCE_PX,
                SAME_COLUMN_TOLERANCE_PX,
                expectedUrl,
                true
        );
        if (!(result instanceof Map<?, ?> clickResult)) {
            return false;
        }

        Object statusValue = clickResult.get("clickStatus");
        Object urlValue = clickResult.get("url");
        if (!(statusValue instanceof String status)
                || !(urlValue instanceof String currentUrl)) {
            return false;
        }
        if ("candidate-changed".equals(status)) {
            return false;
        }
        if (!"clicked".equals(status) || !expectedUrl.equals(currentUrl)) {
            throw new AssertionError(
                    "İkinci satırın birinci sütunundaki ürün atomik olarak tıklanamadı."
            );
        }
        return true;
    }

    private boolean headerMatchesQuery(String query) {
        String normalizedQuery = normalizeText(query);
        for (WebElement header : displayedElements(SEARCH_HEADER)) {
            if (normalizeText(header.getText()).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }

    private boolean urlContainsSearchQuery(String query) {
        try {
            String rawQuery = URI.create(driver.getCurrentUrl()).getRawQuery();
            if (rawQuery == null || rawQuery.isBlank()) {
                return false;
            }
            String decodedQuery = URLDecoder.decode(rawQuery, StandardCharsets.UTF_8);
            return normalizeText(decodedQuery).contains(normalizeText(query));
        } catch (IllegalArgumentException invalidUrlOrEncoding) {
            return false;
        }
    }

    private ProductCandidate selectSecondRowFirstCandidate() {
        Object result = javascript.executeScript(
                FIND_SECOND_ROW_FIRST_PRODUCT,
                SAME_ROW_TOLERANCE_PX,
                SAME_COLUMN_TOLERANCE_PX
        );
        if (!(result instanceof Map<?, ?> candidate)) {
            return null;
        }

        Object linkValue = candidate.get("link");
        Object nameValue = candidate.get("name");
        Object urlValue = candidate.get("url");
        Object xValue = candidate.get("x");
        Object yValue = candidate.get("y");
        Object layoutSignatureValue = candidate.get("layoutSignature");
        if (!(linkValue instanceof WebElement link)
                || !(nameValue instanceof String name)
                || !(urlValue instanceof String url)
                || !(xValue instanceof Number x)
                || !(yValue instanceof Number y)
                || !(layoutSignatureValue instanceof String layoutSignature)
                || name.isBlank()
                || url.isBlank()
                || layoutSignature.isBlank()) {
            return null;
        }
        return new ProductCandidate(
                link,
                name,
                url,
                x.intValue(),
                y.intValue(),
                layoutSignature
        );
    }

    private record ProductCandidate(
            WebElement link,
            String name,
            String url,
            int x,
            int y,
            String layoutSignature
    ) {
    }
}
