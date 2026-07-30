package com.virgosol.hepsiburada.hooks;

import com.thoughtworks.gauge.AfterScenario;
import com.virgosol.hepsiburada.driver.DriverSession;
import com.virgosol.hepsiburada.pages.CartPage;
import com.virgosol.hepsiburada.support.ScenarioState;
import org.openqa.selenium.WebDriver;

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
                    CartPage cartPage = new CartPage(driver, DriverSession.config());
                    cartPage.open();
                    cartPage.removeOneUnit(ScenarioState.product());
                    ScenarioState.markCartCleaned();
                } catch (RuntimeException | AssertionError cleanupFailure) {
                    System.err.println(
                            "[UYARI] Senaryo sonu sepet temizliği tamamlanamadı: "
                                    + cleanupFailure.getMessage()
                    );
                }
            }
        } finally {
            DriverSession.close();
        }
    }
}
