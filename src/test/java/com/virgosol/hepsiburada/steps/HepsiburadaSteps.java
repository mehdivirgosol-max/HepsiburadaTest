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
import org.openqa.selenium.WebDriver;

public final class HepsiburadaSteps {
    @Step("Test için Chrome tarayıcısını aç")
    public void openBrowser() {
        TestConfig config = TestConfig.fromEnvironment();
        DriverSession.start(config);
    }

    @Step("Hepsiburada ana sayfasını aç ve üst menünün yüklenmesini bekle")
    public void openHomePage() {
        homePage().open();
    }

    @Step("Üst menüdeki Hesabım alanını aç")
    public void openAccountMenu() {
        homePage().openAccountMenu();
    }

    @Step("Hesabım menüsündeki Giriş yap bağlantısına tıkla")
    public void openLoginPage() {
        homePage().openLoginPage();
    }

    @Step("Ortam değişkenlerindeki e-posta ve şifreyi giriş alanlarına kontrollü hızla yaz")
    public void fillCredentials() {
        loginPage().fillCredentials();
    }

    @Step("Sayfayı kaydırmadan görünür Giriş yap düğmesine tıkla")
    public void submitLogin() {
        loginPage().submitAndWaitForLoginCompletion();
    }

    @Step("Üst menüde oturum açan kullanıcı bilgisinin göründüğünü doğrula")
    public void verifyLogin() {
        homePage().verifyLoggedIn();
    }

    @Step("Üst menüdeki arama alanına tıkla ve yazmadan önce 1 saniye bekle")
    public void openSearch() {
        homePage().openSearch();
    }

    @Step("Arama alanına <query> yaz ve Enter tuşuyla aramayı başlat")
    public void searchFor(String query) {
        searchResultsPage().searchFor(query);
    }

    @Step("<query> başlığını ve ürün kartlarını gösteren arama sonuçlarının geldiğini doğrula")
    public void verifySearchResults(String query) {
        searchResultsPage().verifyResultsFor(query);
    }

    @Step("Görsel ürün ızgarasının ikinci satır birinci sütunundaki ürün başlık bağlantısına kaydırıp tıkla")
    public void openSecondRowFirstProduct() {
        SelectedProduct selectedProduct = searchResultsPage()
                .openFirstColumnProductInSecondVisualRow();
        ScenarioState.saveProduct(selectedProduct);
    }

    @Step("Ürün detayındaki Sepete ekle düğmesine tıkla")
    public void addProductToCart() {
        productDetailPage().addToCart();
        ScenarioState.markAddAttempted();
    }

    @Step("Açılan sepete ekleme penceresindeki Sepete git düğmesine tıkla")
    public void goToCart() {
        productDetailPage().goToCart();
        cartPage().waitUntilLoaded();
    }

    @Step("Seçilen ürünün sepet listesinde göründüğünü doğrula")
    public void verifyCartProduct() {
        cartPage().verifyContains(ScenarioState.product());
    }

    @Step("Testin eklediği tek ürün adedini kaldır ve sepet sayısını başlangıç değerine döndür")
    public void removeAddedProductAndRestoreCartCount() {
        cartPage().removeOneUnit(ScenarioState.product());
        ScenarioState.markCartCleaned();
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
