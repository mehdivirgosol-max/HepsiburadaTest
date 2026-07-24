package com.virgosol.hepsiburada.hooks;

import com.thoughtworks.gauge.AfterScenario;
import com.virgosol.hepsiburada.driver.DriverSession;
import com.virgosol.hepsiburada.pages.CartPage;
import com.virgosol.hepsiburada.pages.HomePage;
import com.virgosol.hepsiburada.support.ScenarioState;
import com.virgosol.hepsiburada.support.TestLog;
import org.openqa.selenium.WebDriver;

import java.util.Locale;

public final class SeleniumHooks {
    @AfterScenario
    public void closeBrowserAndCleanResidualCartData() {
        WebDriver driver = DriverSession.currentOrNull();
        if (driver == null) {
            return;
        }

        try {
            if (ScenarioState.wasAddAttempted()
                    && !ScenarioState.wasCartCleaned()) {
                try {
                    String currentUrl = driver.getCurrentUrl().toLowerCase(Locale.ROOT);
                    if (!currentUrl.contains("checkout.hepsiburada.com/sepetim")) {
                        new HomePage(driver, DriverSession.config()).openCartFromHeader();
                    }
                    CartPage cartPage = new CartPage(driver, DriverSession.config());
                    cartPage.waitUntilLoaded();
                    cartPage.removeOneUnitAndRestoreCount(
                            ScenarioState.product(),
                            ScenarioState.cartCountBeforeAdd()
                    );
                    ScenarioState.markCartCleaned();
                    TestLog.info(
                            "Başarısız/yarım kalan akıştan sonra eklenen bir adet geri alındı."
                    );
                } catch (RuntimeException | AssertionError cleanupFailure) {
                    TestLog.warning(
                            "Senaryo sonu sepet temizliği tamamlanamadı: %s",
                            cleanupFailure.getMessage()
                    );
                }
            }
        } finally {
            try {
                DriverSession.close();
                System.out.println("[BİLGİ] Tarayıcı kapatıldı.");
            } catch (RuntimeException closeFailure) {
                TestLog.warning(
                        "Tarayıcı kapatılırken hata oluştu: %s",
                        closeFailure.getMessage()
                );
                throw closeFailure;
            }
        }
    }
}
