package com.virgosol.hepsiburada.steps;

import com.thoughtworks.gauge.Step;
import com.virgosol.hepsiburada.config.TestConfig;
import com.virgosol.hepsiburada.driver.DriverSession;
import com.virgosol.hepsiburada.model.SelectedProduct;
import com.virgosol.hepsiburada.pages.CartPage;
import com.virgosol.hepsiburada.pages.HomePage;
import com.virgosol.hepsiburada.pages.LoginPage;
import com.virgosol.hepsiburada.pages.ProductDetailPage;
import com.virgosol.hepsiburada.pages.SearchResultsPage;
import com.virgosol.hepsiburada.support.ScenarioState;
import com.virgosol.hepsiburada.support.TestLog;
import org.openqa.selenium.WebDriver;

public final class HepsiburadaSteps {
    @Step("Tarayıcıyı aç")
    public void openBrowser() {
        TestConfig config = TestConfig.fromEnvironment();
        DriverSession.start(config);
        TestLog.info("Chrome tarayıcı penceresi açıldı ve maksimize edildi.");
    }

    @Step("Hepsiburada ana sayfasına git")
    public void openHomePage() {
        homePage().open();
    }

    @Step("Hesabım alanını aç")
    public void openAccountMenu() {
        homePage().openAccountMenu();
    }

    @Step("Giriş yap bağlantısına tıkla")
    public void openLoginPage() {
        homePage().openLoginPage();
    }

    @Step("Ortam değişkenlerindeki giriş bilgilerini kontrollü hızla doldur")
    public void fillCredentials() {
        loginPage().fillCredentials();
    }

    @Step("Giriş yap düğmesine tıkla")
    public void submitLogin() {
        loginPage().submitAndWaitForLoginCompletion();
    }

    @Step("Giriş işleminin başarılı olduğunu doğrula")
    public void verifyLogin() {
        homePage().verifyLoggedIn();
        TestLog.success("Giriş işlemi doğrulandı.");
    }

    @Step("Arama alanını aç")
    public void openSearch() {
        homePage().openSearch();
    }

    @Step("<query> metnini ara")
    public void searchFor(String query) {
        searchResultsPage().searchFor(query);
    }

    @Step("<query> arama sonuçlarının geldiğini doğrula")
    public void verifySearchResults(String query) {
        searchResultsPage().verifyResultsFor(query);
        TestLog.success("\"%s\" arama sonuçları doğrulandı.", query);
    }

    @Step("İkinci satırın birinci sütunundaki ürüne kaydır ve başlık bağlantısına tıkla")
    public void openSecondRowFirstProduct() {
        SelectedProduct selectedProduct = searchResultsPage()
                .openFirstColumnProductInSecondVisualRow();
        ScenarioState.saveProduct(selectedProduct);
        TestLog.info(
                "İkinci satırın birinci sütunundaki ürün seçildi: %s (%s)",
                selectedProduct.name(),
                selectedProduct.code().isBlank() ? "ürün kodu URL'de yok" : selectedProduct.code()
        );
    }

    @Step("Ürün detay sayfasını kaydedilen ürün kodu veya adıyla doğrula")
    public void verifyProductDetail() {
        SelectedProduct selectedProduct = ScenarioState.product();
        String detailName = productDetailPage().verifyProduct(selectedProduct);
        TestLog.success(
                "Ürün detay sayfası doğrulandı: %s (%s)",
                detailName,
                selectedProduct.code().isBlank() ? "ad ile eşleşti" : selectedProduct.code()
        );
    }

    @Step("Detay sayfasındaki Sepete Ekle düğmesine tıkla")
    public void addProductToCart() {
        int countBeforeAdding = productDetailPage().addToCart();
        ScenarioState.saveCartCountBeforeAdd(countBeforeAdding);
        ScenarioState.markAddAttempted();
    }

    @Step("Sepete ekleme bildirimini doğrula ve raporla")
    public void verifyAddToCart() {
        String confirmation = productDetailPage().verifyAddToCartConfirmation(
                ScenarioState.cartCountBeforeAdd()
        );
        int expectedCount = ScenarioState.cartCountBeforeAdd() + 1;
        int actualCount = productDetailPage().verifyCartCount(expectedCount);
        TestLog.success(
                "Sepete ekleme doğrulandı: %s Header sepet sayacı: %d.",
                confirmation,
                actualCount
        );
    }

    @Step("Sayfanın en üstüne çık ve header Sepetim alanına tıkla")
    public void openCart() {
        homePage().openCartFromHeader();
        cartPage().waitUntilLoaded();
    }

    @Step("Sepetin boş olmadığını ve kaydedilen ürünü içerdiğini doğrula")
    public void verifyCartProduct() {
        SelectedProduct selectedProduct = ScenarioState.product();
        cartPage().verifyContains(selectedProduct);
        TestLog.success(
                "Sepet boş değil ve seçilen ürün sepette bulundu: %s (%s)",
                selectedProduct.name(),
                selectedProduct.code().isBlank() ? "ürün adıyla" : selectedProduct.code()
        );
    }

    @Step("Sepeti gözlem için <seconds> saniye açık tut")
    public void observeCart(int seconds) {
        cartPage().observe(seconds);
    }

    @Step("Testin eklediği ürünü kaldır ve sepet sayısını başlangıç değerine döndür")
    public void removeAddedProductAndRestoreCartCount() {
        cartPage().removeOneUnitAndRestoreCount(
                ScenarioState.product(),
                ScenarioState.cartCountBeforeAdd()
        );
        ScenarioState.markCartCleaned();
        TestLog.success(
                "Testin eklediği ürün kaldırıldı; sepet sayısı yeniden %d.",
                ScenarioState.cartCountBeforeAdd()
        );
    }

    private WebDriver driver() {
        return DriverSession.driver();
    }

    private TestConfig config() {
        return DriverSession.config();
    }

    private HomePage homePage() {
        return new HomePage(driver(), config());
    }

    private LoginPage loginPage() {
        return new LoginPage(driver(), config());
    }

    private SearchResultsPage searchResultsPage() {
        return new SearchResultsPage(driver(), config());
    }

    private ProductDetailPage productDetailPage() {
        return new ProductDetailPage(driver(), config());
    }

    private CartPage cartPage() {
        return new CartPage(driver(), config());
    }
}
