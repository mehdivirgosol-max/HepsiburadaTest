# Hepsiburada UI Test Projesi — Baştan Sona Teknik Analiz

## 1. Bu proje ne yapıyor?

Bu proje, gerçek Hepsiburada web sitesi üzerinde çalışan bir kullanıcı arayüzü
otomasyonudur. Test, gerçek bir kullanıcının tarayıcıda gerçekleştireceği işlemleri
Selenium ile otomatik olarak yapar.

Testin iş akışı özetle şöyledir:

1. Chrome tarayıcısını açar.
2. Hepsiburada ana sayfasına gider.
3. Çerez tercihleri görünüyorsa **Kabul Et** seçeneğine tıklar.
4. Giriş sayfasına gider.
5. Ortam değişkenlerinden alınan e-posta ve şifreyi yazar.
6. Bir saniye bekleyip **Giriş yap** butonuna tıklar.
7. Başarılı giriş yapıldığını doğrular.
8. Girişten sonra **Hesabım** alanına tekrar dokunmadan doğrudan arama alanına gider.
9. `bilgisayar` kelimesini arar.
10. Arama sonuçlarındaki görsel gridin ikinci satır, birinci sütun ürününü bulur.
11. Ürünü bir saniye ekranda gösterecek şekilde scroll yapar ve ürüne tıklar.
12. Açılan detay sayfasının seçilen ürünle aynı olduğunu doğrular.
13. Sepetteki başlangıç ürün sayısını kaydeder.
14. Ürünü sepete ekler.
15. Sepet sayısının tam olarak bir arttığını doğrular.
16. Sepeti açar ve aynı ürünün sepette bulunduğunu doğrular.
17. Testin eklediği tek adedi geri alır.
18. Ürün miktarı birden fazlaysa bir adet azaltır; miktar birse ürün satırını siler.
19. Sepet sayısının test öncesi değerine döndüğünü doğrular.
20. Tarayıcıyı kapatır.

Bu test bir **E2E (end-to-end / uçtan uca) UI testi**dir. Birim testi değildir.
Gerçek tarayıcıyı, gerçek ağı, gerçek kullanıcı hesabını ve gerçek sepeti kullanır.

---

## 2. Projenin genel mimarisi

Projede sorumluluklar birbirinden ayrılmıştır:

```text
Gauge specification (.spec)
          ↓
Gauge concept dosyası (.cpt)
          ↓
HepsiburadaSteps içindeki @Step metotları
          ↓
Page Object sınıfları
          ↓
BasePage ortak Selenium altyapısı
          ↓
WebDriver / ChromeDriver
          ↓
Gerçek Chrome tarayıcısı
          ↓
Hepsiburada
```

Bu ana zincirin çevresinde şu yardımcı yapılar bulunur:

```text
TestConfig
    → Ortam değişkenlerini ve test ayarlarını taşır.

DriverSession
    → ChromeDriver oturumunu açar, saklar ve kapatır.

SelectedProduct
    → Arama sonuçlarında seçilen ürünün adını, URL'sini ve ürün kodunu taşır.

ScenarioState
    → Seçilen ürünü ve sepet sayısını Gauge adımları arasında taşır.

SeleniumHooks
    → Senaryo yarım kalsa bile sepeti temizlemeyi dener ve tarayıcıyı kapatır.

SeleniumScreenshotWriter
    → Hata anında güvenli ekran görüntüsü üretir.

TestLog
    → Mesajları hem konsola hem Gauge HTML raporuna yazar.
```

Projede JUnit tarzı `@Test` metotları bulunmaz. Testin kendisi `specs` klasöründeki
Gauge specification dosyasında tanımlıdır. Java tarafında çalıştırılan metotlar
`HepsiburadaSteps` içindeki `@Step` metotlarıdır.

---

## 3. Temel test kavramları

### 3.1 E2E test nedir?

E2E test, sistemi kullanıcının göreceği biçimde başından sonuna kadar sınar.

Bu projede yalnızca bir Java metodunun doğru değer döndürmesi test edilmez. Gerçek
Chrome açılır, gerçek sayfaya gidilir, butonlara tıklanır, form doldurulur ve gerçek
sepet durumu değiştirilir.

Bu nedenle test:

- Ağ bağlantısına,
- Hepsiburada servislerinin çalışmasına,
- Sayfanın HTML/DOM yapısına,
- Chrome ve ChromeDriver uyumluluğuna,
- Kullanılan test hesabının durumuna

bağlıdır.

### 3.2 Selenium nedir?

Selenium, Java kodundan tarayıcıya komut göndermemizi sağlar.

Örneğin:

```java
element.click();
element.sendKeys("bilgisayar");
driver.get("https://www.hepsiburada.com");
```

komutları sırasıyla bir elemente tıklamak, alana metin yazmak ve sayfaya gitmek için
kullanılır.

### 3.3 WebDriver ve ChromeDriver nedir?

`WebDriver`, Selenium'un tarayıcılarla konuşmak için kullandığı ortak arayüzdür.

`ChromeDriver`, bu arayüzün Chrome tarayıcısına özel uygulamasıdır:

```text
Java test kodu
    → Selenium WebDriver
        → ChromeDriver
            → Chrome
```

### 3.4 Gauge nedir?

Gauge, test adımlarını insan tarafından okunabilir cümlelerle tanımlamaya yarar.

Örneğin:

```text
* "bilgisayar" metnini ara
```

adımı Java tarafında şuna bağlanır:

```java
@Step("<query> metnini ara")
public void searchFor(String query) {
    searchResultsPage().searchFor(query);
}
```

Gauge, `"bilgisayar"` değerini Java metodundaki `query` parametresine verir.

### 3.5 Page Object nedir?

Page Object Model, web sayfalarını Java sınıflarıyla temsil etme yaklaşımıdır.

Örneğin:

- `HomePage` ana sayfa/header davranışlarını,
- `LoginPage` giriş sayfasını,
- `SearchResultsPage` arama sonuçlarını,
- `ProductDetailPage` ürün detayını,
- `CartPage` sepet sayfasını

temsil eder.

Gauge step sınıfı selector bilmez. Örneğin `HepsiburadaSteps`, yalnızca:

```java
homePage().openSearch();
```

çağrısını yapar. Arama inputunun CSS selector'u `HomePage` içinde bulunur.

### 3.6 Locator/selector nedir?

Locator, sayfadaki bir elementi bulmak için kullanılan tariftir.

Örnekler:

```java
By.id("btnLogin")
By.cssSelector("input[data-test-id='search-bar-input']")
By.xpath("//button[normalize-space()='Sepete Ekle']")
```

- `By.id`: HTML `id` değerine göre arar.
- `By.cssSelector`: CSS selector kullanır.
- `By.xpath`: Elementin konumunu, niteliğini veya metnini XPath ile tarif eder.

### 3.7 Explicit wait nedir?

Explicit wait, bir koşul gerçekleşene kadar belirli aralıklarla kontrol yapar.

Bu projede temel polling aralığı 200 milisaniyedir:

```java
new FluentWait<>(driver)
    .withTimeout(duration)
    .pollingEvery(Duration.ofMillis(200))
    .until(condition);
```

`HB_TIMEOUT_SECONDS=25` olması her adımın 25 saniye beklediği anlamına gelmez.
Bu yalnızca üst sınırdır. Koşul 600 milisaniyede gerçekleşirse test hemen devam eder.

### 3.8 Stale element nedir?

React gibi dinamik arayüzlerde bir element sayfa tarafından silinip yeniden
oluşturulabilir. Selenium'un daha önce tuttuğu element referansı bu durumda geçersiz
olur ve `StaleElementReferenceException` oluşur.

Projede stale element çoğunlukla geçici durum kabul edilir:

- Explicit wait bir sonraki polling turunda elementi tekrar arar.
- Tıklama metodu locator verilmişse elementi yeniden bulur.
- Ürün seçiminde aynı ürün URL'si tekrar doğrulanır.

### 3.9 Shadow DOM nedir?

Bazı web component'ler iç elementlerini normal DOM'dan ayrı bir `shadowRoot` içinde
tutar. Normal Selenium selector'ları bu elementleri doğrudan göremeyebilir.

Hepsiburada çerez butonu bazı çalıştırmalarda Efilli bileşeninin açık Shadow DOM'u
içinde oluştuğu için `BasePage` içindeki özel JavaScript bütün erişilebilir shadow
root'ları dolaşır.

### 3.10 Record nedir?

Java record, değiştirilemez veri taşımak için kullanılan sınıf türüdür.

Bu projedeki örnekler:

- `TestConfig`
- `SelectedProduct`
- `SearchResultsPage.ProductCandidate`
- `CartPage.CartAdjustment`

---

## 4. Proje dosya yapısı

```text
HepsiburadaTest/
├── pom.xml
├── manifest.json
├── README.md
├── run-tests.cmd
├── env/
│   └── default/
│       ├── default.properties
│       └── java.properties
├── specs/
│   ├── hepsiburada-shopping.spec
│   └── concepts/
│       └── hepsiburada.cpt
├── src/
│   ├── main/java/com/virgosol/
│   │   ├── HepsiburadaTestLauncher.java
│   │   └── adımAdım.md
│   └── test/java/com/virgosol/hepsiburada/
│       ├── config/
│       │   └── TestConfig.java
│       ├── driver/
│       │   └── DriverSession.java
│       ├── hooks/
│       │   └── SeleniumHooks.java
│       ├── model/
│       │   └── SelectedProduct.java
│       ├── pages/
│       │   ├── BasePage.java
│       │   ├── HomePage.java
│       │   ├── LoginPage.java
│       │   ├── SearchResultsPage.java
│       │   ├── ProductDetailPage.java
│       │   └── CartPage.java
│       ├── steps/
│       │   └── HepsiburadaSteps.java
│       └── support/
│           ├── ScenarioState.java
│           ├── SeleniumScreenshotWriter.java
│           └── TestLog.java
├── target/
├── reports/
├── logs/
└── .gauge/
```

### Kaynak ve çıktı klasörlerinin farkı

- `src/main`: IntelliJ'den testi başlatan launcher sınıfı.
- `src/test`: Asıl test otomasyonu sınıfları.
- `specs`: Gauge test senaryosu ve concept'ler.
- `env`: Gauge çalışma ayarları.
- `target`: Maven tarafından derlenen sınıflar ve bağımlılıklar.
- `reports`: Gauge HTML raporları.
- `logs`: Gauge ve Java çalışma logları.
- `.gauge/screenshots`: Hata ekran görüntüleri.

`target`, `reports`, `logs`, `.gauge` gibi çalışma çıktıları `.gitignore` tarafından
kaynak kontrolünün dışında tutulur.

---

## 5. Gauge dosyaları

### 5.1 `specs/hepsiburada-shopping.spec`

Bu dosya asıl test senaryosudur:

```text
# Hepsiburada giriş, arama ve sepet akışı

Tags: e2e, ui, live, destructive, serial

## Bilgisayar aramasındaki ikinci satırın ilk ürününü sepete ekleme

* Güvenli şekilde Hepsiburada hesabına giriş yap
* "bilgisayar" ara ve ikinci satırdaki ilk ürünü aç
* Açılan ürünü sepete ekle ve onayı doğrula
* Kaydedilen ürünü sepette doğrula
* Testin eklediği ürünü kaldır ve sepeti önceki durumuna döndür
```

Etiketlerin anlamı:

- `e2e`: Uçtan uca test.
- `ui`: Kullanıcı arayüzü üzerinden çalışır.
- `live`: Gerçek siteyi kullanır.
- `destructive`: Gerçek dış durumu, burada sepeti değiştirir.
- `serial`: Paralel çalıştırılmamalıdır.

Specification'daki beş adım üst seviyeli concept adımlarıdır.

### 5.2 `specs/concepts/hepsiburada.cpt`

Concept dosyası, üst seviyeli adımları küçük adımlara böler.

Örneğin:

```text
# <aranan> ara ve ikinci satırdaki ilk ürünü aç

* Arama alanını aç
* <aranan> metnini ara
* <aranan> arama sonuçlarının geldiğini doğrula
* İkinci satırın birinci sütunundaki ürüne kaydır ve başlık bağlantısına tıkla
* Ürün detay sayfasını kaydedilen ürün kodu veya adıyla doğrula
```

Specification içindeki:

```text
* "bilgisayar" ara ve ikinci satırdaki ilk ürünü aç
```

çalıştığında `<aranan>` parametresi `bilgisayar` olur.

Concept kullanmanın faydaları:

- Specification kısa ve okunabilir kalır.
- Aynı iş akışı başka sorgularla tekrar kullanılabilir.
- Teknik alt adımlar tek bir yerde tutulur.

### 5.3 `manifest.json`

```json
{
  "Language": "java",
  "Plugins": [
    "html-report"
  ]
}
```

- Gauge step uygulamalarının Java olduğunu belirtir.
- Çalışma sonunda HTML rapor üretir.

### 5.4 `env/default/default.properties`

Önemli ayarlar:

```properties
gauge_reports_dir = reports
overwrite_reports = true
screenshot_on_failure = true
logs_directory = logs
gauge_specs_dir = specs
gauge_concepts_dir = specs/concepts
enable_multithreading = false
package_to_scan = com.virgosol.hepsiburada
```

`enable_multithreading=false` özellikle önemlidir. Test gerçek bir hesabın sepetini
değiştirdiği için iki test aynı anda çalışırsa sayaçlar ve cleanup işlemleri birbirine
karışabilir.

### 5.5 `env/default/java.properties`

```properties
gauge_custom_build_path = target/test-classes
gauge_additional_libs = target/gauge-libs/*, libs/*
gauge_clear_state_level = scenario
```

- Gauge derlenmiş sınıfları `target/test-classes` altında bulur.
- Maven bağımlılıklarını `target/gauge-libs` altından yükler.
- Scenario state her senaryo için temizlenir.

---

## 6. Maven yapısı (`pom.xml`)

### 6.1 Java sürümü

```xml
<maven.compiler.release>17</maven.compiler.release>
```

Kod Java 17 özelliklerine göre derlenir. Record ve text block gibi özellikler bu
sürümde desteklenir.

### 6.2 Gauge Java bağımlılığı

```xml
<dependency>
    <groupId>com.thoughtworks.gauge</groupId>
    <artifactId>gauge-java</artifactId>
</dependency>
```

Şunları sağlar:

- `@Step`
- `@AfterScenario`
- `ScenarioDataStore`
- `CustomScreenshotWriter`
- Gauge rapor mesajları

### 6.3 Selenium bağımlılıkları

```text
selenium-chrome-driver
selenium-support
selenium-devtools-v150
```

- ChromeDriver ve WebDriver sınıfları,
- FluentWait ve Actions,
- Chrome 150 DevTools uyumluluğu

için kullanılır.

Bağımlılıkların `scope=test` olması bunların üretim uygulaması için değil test için
kullanıldığını gösterir.

### 6.4 Maven dependency plugin

Test bağımlılıkları:

```text
target/gauge-libs
```

klasörüne kopyalanır. Gauge Java runner bu JAR dosyalarını classpath'e ekler.

### 6.5 `live-e2e` profili

```powershell
mvn -P live-e2e test
```

çalıştırılırsa:

1. Test kodu derlenir.
2. Gauge specification doğrulanır.
3. Gerçek Gauge E2E testi çalıştırılır.

Normal:

```powershell
mvn test
```

komutu canlı Gauge senaryosunu çalıştırmaz. Projede klasik JUnit `@Test` metodu
olmadığı için esas olarak derleme/doğrulama yapar. Böylece normal Maven testi
yanlışlıkla gerçek sepeti değiştirmez.

---

## 7. Testi başlatan yapılar

### 7.1 `run-tests.cmd`

Windows scripti şu sırayla çalışır:

1. Konsolu UTF-8 yapar.
2. Proje köküne geçer.
3. `HB_EMAIL` var mı kontrol eder.
4. `HB_PASSWORD` var mı kontrol eder.
5. Gauge `PATH` üzerinde mi kontrol eder.
6. `mvn -q clean test-compile` çalıştırır.
7. `gauge run --verbose specs\hepsiburada-shopping.spec` çalıştırır.
8. Gerçek çıkış kodunu çağıran sürece döndürür.

### 7.2 IntelliJ Run Configuration

`.run/Hepsiburada E2E.run.xml`, IntelliJ'e şu main sınıfını çalıştırmasını söyler:

```text
com.virgosol.HepsiburadaTestLauncher
```

Çalışma dizini `$PROJECT_DIR$` olur ve UTF-8 JVM parametreleri uygulanır.

---

## 8. Bütün Java sınıfları

Projede 15 üst seviye Java sınıfı/record vardır:

| Sınıf | Tür | Görevi |
|---|---|---|
| `HepsiburadaTestLauncher` | Main launcher | IntelliJ'den test scriptini başlatır. |
| `TestConfig` | Record | Ortam değişkenleri ve test ayarlarını taşır. |
| `DriverSession` | Driver yöneticisi | Chrome oturumunu açar, saklar ve kapatır. |
| `BasePage` | Soyut Page Object | Ortak wait, click, cookie, scroll ve metin işlemleri. |
| `HomePage` | Page Object | Ana sayfa, hesap, arama ve header sepet işlemleri. |
| `LoginPage` | Page Object | Kimlik bilgileri, giriş, hata ve yönlendirme kontrolü. |
| `SearchResultsPage` | Page Object | Arama ve ikinci satır/birinci sütun seçimi. |
| `ProductDetailPage` | Page Object | Ürün detayı ve sepete ekleme doğrulaması. |
| `CartPage` | Page Object | Sepet doğrulama ve cleanup. |
| `SelectedProduct` | Record | Seçilen ürünün adı, URL'si ve kodu. |
| `ScenarioState` | Senaryo hafızası | Ürün, sayaç ve cleanup bayraklarını taşır. |
| `HepsiburadaSteps` | Gauge step sınıfı | Gauge cümlelerini Java/Page Object metotlarına bağlar. |
| `SeleniumHooks` | Gauge hook | Hata sonrası cleanup ve tarayıcı kapatma. |
| `SeleniumScreenshotWriter` | Screenshot sağlayıcısı | Güvenli hata görüntüsü üretir. |
| `TestLog` | Log yardımcısı | Konsol ve Gauge rapor mesajları. |

İki private iç record da vardır:

- `SearchResultsPage.ProductCandidate`
- `CartPage.CartAdjustment`

---

## 9. `HepsiburadaTestLauncher`

Dosya:

```text
src/main/java/com/virgosol/HepsiburadaTestLauncher.java
```

Bu sınıf test mantığını çalıştırmaz. IntelliJ'den normal Java uygulaması gibi Gauge
testini başlatmak için kullanılır.

### `main()`

Önce:

```java
requireEnvironmentVariable("HB_EMAIL");
requireEnvironmentVariable("HB_PASSWORD");
```

çağrılır. Gizli değerlerin yalnızca mevcut ve boş olmayan değerler olduğu kontrol
edilir; gerçek değerler yazdırılmaz.

Ardından proje kökü bulunur ve:

```java
new ProcessBuilder(
    "cmd.exe",
    "/d",
    "/c",
    projectRoot.resolve("run-tests.cmd").toString()
);
```

ile Windows scripti başlatılır.

- `/d`: Kullanıcının `cmd` AutoRun ayarlarını kapatır.
- `/c`: Verilen komutu çalıştırıp `cmd` sürecini kapatır.
- `inheritIO()`: Alt süreç çıktısını IntelliJ konsoluna bağlar.
- `waitFor()`: Gauge çalışması bitene kadar launcher'ı bekletir.
- `System.exit(exitCode)`: Test sonucunu IntelliJ'e iletir.

### `findProjectRoot()`

Mevcut dizinden yukarı doğru çıkarak aynı klasörde:

- `pom.xml`
- `run-tests.cmd`
- `specs`

bulunduğu yeri proje kökü kabul eder.

---

## 10. `TestConfig`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/config/TestConfig.java
```

Değiştirilemez test ayarlarını taşır:

```java
URI baseUri
String email
String password
boolean headless
Duration timeout
Duration typingDelay
```

### Ortam değişkenleri

| Değişken | Zorunlu | Varsayılan |
|---|---:|---|
| `HB_EMAIL` | Evet | Yok |
| `HB_PASSWORD` | Evet | Yok |
| `HB_BASE_URL` | Hayır | `https://www.hepsiburada.com` |
| `HB_HEADLESS` | Hayır | `false` |
| `HB_TIMEOUT_SECONDS` | Hayır | `25` |
| `HB_TYPING_DELAY_MS` | Hayır | `60` |

Timeout 5–120 saniye, yazma gecikmesi 0–500 milisaniye arasında tutulur.

### Trusted URL kontrolü

`assertTrustedUrl()` URL'nin:

- HTTPS olmasını,
- tam olarak `hepsiburada.com` veya gerçek `.hepsiburada.com` alt alanı olmasını

zorunlu tutar.

Örnek:

```text
https://www.hepsiburada.com          → kabul
https://checkout.hepsiburada.com     → kabul
http://www.hepsiburada.com           → ret
https://evil-hepsiburada.com         → ret
```

Bu kontrol e-posta/şifre yazılmadan ve ürün/sepet bağlantıları kullanılmadan önce
yapılır.

### Gizli bilgi koruması

`toString()` gerçek e-posta veya şifreyi göstermez:

```text
email=[REDACTED], password=[REDACTED]
```

---

## 11. `DriverSession`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/driver/DriverSession.java
```

ChromeDriver'ın açılması, saklanması ve kapatılmasından sorumludur.

### ThreadLocal

```java
ThreadLocal<WebDriver> DRIVER
ThreadLocal<TestConfig> CONFIG
```

Her çalışma thread'i kendi driver/config değerini kullanır. Şu anda Gauge paralel
çalışmayı kapatsa da tasarım thread izolasyonuna uygundur.

### Chrome ayarları

```java
options.setPageLoadStrategy(PageLoadStrategy.EAGER);
```

`EAGER`, bütün resim ve reklamları beklemek yerine temel HTML hazır olduğunda
kontrolü Selenium'a verir. Dinamik elementler Page Object explicit wait'leriyle
ayrıca beklenir.

Kullanılan argümanlar:

```text
--disable-notifications
--disable-popup-blocking
--lang=tr-TR
--disable-blink-features=AutomationControlled
```

Deneysel seçenekler:

```text
excludeSwitches = ["enable-automation"]
useAutomationExtension = false
```

Chrome parola yöneticisi kapatılır. Bu sayede otomatik doldurma ve “şifreyi kaydet”
pencereleri teste müdahale etmez.

### Headless ve görünür mod

Headless mod:

```text
1920x1080
```

çözünürlük kullanır. Görsel grid hesabı için sabit çözünürlük önemlidir.

Görünür modda Chrome maksimize edilir.

### Timeout ayarları

```java
implicitlyWait(Duration.ZERO);
pageLoadTimeout(Duration.ofSeconds(60));
scriptTimeout(Duration.ofSeconds(20));
```

Implicit wait sıfırdır. Proje explicit wait kullandığı için iki bekleme türünün
birbirine eklenmesi engellenir.

### Güvenli kapanma

Driver açılırken hata oluşursa kısmen açılmış Chrome kapatılır. `close()` çağrısında:

```java
driver.quit();
DRIVER.remove();
CONFIG.remove();
```

çalışır.

---

## 12. `BasePage`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/pages/BasePage.java
```

Bütün Page Object sınıflarının ortak tabanıdır ve doğrudan oluşturulmaz.

### Alanlar

```java
protected final WebDriver driver;
protected final TestConfig config;
protected final JavascriptExecutor javascript;
```

Alt Page Object sınıfları aynı driver, config ve JavaScript executor'a erişir.

### `waitFor()`

Merkezi explicit wait metodudur:

```java
new FluentWait<>(driver)
    .withTimeout(duration)
    .pollingEvery(Duration.ofMillis(200))
    .ignoring(NoSuchElementException.class)
    .ignoring(StaleElementReferenceException.class)
    .until(condition);
```

Süre aşılırsa ham Selenium timeout yerine anlamlı `AssertionError` oluşturur. Hata
mesajına ilgili locatorlar ve query/fragment bölümü temizlenmiş URL eklenir.

### `waitUntilVisible()`

Verilen locator alternatiflerini sırayla tarar ve ilk görünür elementi döndürür.

### `waitUntilClickable()`

Elementin hem görünür hem enabled olmasını ister.

### Dayanıklı `click()`

Tıklama zinciri:

1. Element ekranın merkezine scroll edilir.
2. Normal `element.click()` denenir.
3. Overlay engellerse cookie kontrol edilir.
4. Element stale olduysa locator ile yeniden bulunur.
5. `Actions.moveToElement(...).click()` denenir.
6. Son çare JavaScript click uygulanır.

### Cookie ve Shadow DOM

Önce normal DOM locatorları denenir:

- `#hb-accept-all`
- OneTrust selector'u
- test-id
- aria-label
- “Kabul Et” metni

Bulunmazsa JavaScript:

1. `document` ile başlar.
2. `#hb-accept-all` arar.
3. Buton/link elementlerinin metnini kontrol eder.
4. Açık shadow root'ları kuyruğa ekler.
5. Shadow DOM'ları katman katman dolaşır.
6. Yalnız görünür kabul kontrolünü döndürür.

Buton tıklandıktan sonra iki saniye içinde gerçekten kaybolduğu doğrulanır.

### `typeControlled()`

1. Alanı tıklar.
2. `Ctrl+A` yapar.
3. `Backspace` ile temizler.
4. Metni karakter karakter yazar.
5. Karakterler arasında `typingDelay` kadar Selenium pause uygular.

### Scroll yardımcıları

- `scrollIntoView()`: Elementi ekranın merkezine getirir.
- `scrollToTop()`: Sayfanın en üstüne gider ve scroll değerini doğrular.
- `waitForDocumentReady()`: `interactive` veya `complete` bekler.

### Metin eşleştirme

`normalizeText()`:

- Büyük/küçük harfi eşitler.
- Diakritikleri azaltır.
- Noktalama işaretlerini kaldırır.
- Boşlukları tekilleştirir.

`meaningfullyMatches()` önce doğrudan içerme kontrolü yapar. Bu olmazsa üç veya daha
uzun ortak kelimeleri karşılaştırır.

---

## 13. `HomePage`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/pages/HomePage.java
```

Ana sayfa ve header işlemlerini yönetir.

### Önemli locatorlar

```text
#myAccount
#login
input[data-test-id='search-bar-input']
#shoppingCart
```

### `open()`

1. Base URL açılır.
2. Document ready beklenir.
3. Cookie en fazla iki saniye aranır.
4. Hesap veya arama bölgesinin görünmesi beklenir.

### `openAccountMenu()`

Bu metot yalnızca girişten önce login bağlantısına ulaşmak için kullanılır.

1. Cookie varsa kapatılır.
2. Hesabım tıklanır.
3. Menü açılmazsa hover fallback yapılır.
4. Login veya authenticated bağlantı beklenir.

Girişten sonra bu metot tekrar çağrılmaz.

### `openLoginPage()`

- Login bağlantısına tıklar.
- Document ready bekler.
- Login sayfasındaki cookie'yi tekrar kontrol eder.
- E-posta alanını bekler.

### `verifyLoggedIn()`

Başarı için:

- E-posta/şifre alanları görünmemeli,
- Arama inputu veya arama bölgesi görünmelidir.

Hesabım alanına tıklamaz veya hover yapmaz.

### `openSearch()` ve kategori hover düzeltmesi

Arama alanı önce clickable olarak bulunur. Ardından pointer, viewport'un içerik
bulunmayan sol üst noktasına taşınır ve arama inputu JavaScript ile focus edilir:

```java
movePointerToNeutralViewportPosition();
javascript.executeScript(
    "arguments[0].focus({preventScroll: true});",
    searchInput
);
```

Bu davranış iki nedenle önemlidir:

1. Arama başlamadan önce pointer başka bir header/kategori alanında bırakılmaz.
2. Focus için mouse click kullanılmadığından öneri veya kategori koordinatında
   kalıcı bir CSS `:hover` oluşmaz.

### `openCartFromHeader()`

1. Sayfanın en üstüne çıkar.
2. Cookie varsa kapatır.
3. Sepetim'e tıklar.
4. URL'nin `sepet` veya `cart` içerdiğini doğrular.

---

## 14. `LoginPage`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/pages/LoginPage.java
```

Giriş formu ve login sonucunu yönetir.

### Locatorlar

```java
EMAIL = By.id("txtUserName");
PASSWORD = By.id("txtPassword");
LOGIN_BUTTON = By.id("btnLogin");
```

CAPTCHA ve hata yüzeyleri için de alternatif locator listeleri vardır.

### `fillCredentials()`

İlk işlem trusted URL kontrolüdür:

```java
config.assertTrustedUrl(driver.getCurrentUrl(), "Kimlik bilgisi girişi");
```

Bu kontrol geçmeden e-posta veya şifre yazılmaz.

Ardından:

1. Cookie kontrol edilir.
2. E-posta alanı beklenir.
3. E-posta kontrollü yazılır.
4. Şifre alanı beklenir.
5. Şifre kontrollü yazılır.

### `submitAndWaitForLoginCompletion()`

1. URL güvenliği tekrar doğrulanır.
2. Cookie kontrol edilir.
3. Mevcut pencere handle'ları kaydedilir.
4. Login butonu clickable olarak bulunur.
5. Bir saniye Selenium pause uygulanır.
6. Aynı login butonuna tıklanır.

Sonrasında:

- Yeni sekme/pencere var mı?
- Orijinal pencere kapandı mı?
- Aktif pencere trusted Hepsiburada adresinde mi?
- Gecikmeli cookie çıktı mı?
- CAPTCHA var mı?
- Görünür login hatası var mı?
- Login formu kayboldu mu?
- URL artık login URL'si değil mi?
- Document ready mi?

kontrol edilir.

CAPTCHA bulunursa test onu aşmaya çalışmaz ve açık hata ile durur.

### Login hata güvenliği

Hata metni raporlanmadan önce:

```java
email    → [gizli e-posta]
password → [gizli]
```

olarak maskelenir.

---

## 15. `SearchResultsPage`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/pages/SearchResultsPage.java
```

Arama zamanlaması ve ikinci satır/birinci sütun seçiminin gerçekleştiği sınıftır.

### Zamanlama sabitleri

```java
SAME_ROW_TOLERANCE_PX = 20;
SAME_COLUMN_TOLERANCE_PX = 25;
GRID_STABILITY_DURATION = 3 saniye;
BEFORE_SCROLL_PAUSE = 1 saniye;
AFTER_SCROLL_PAUSE = 1 saniye;
```

### `searchFor()`

1. Görünür ve enabled arama inputu bulunur.
2. `Ctrl+A` ile mevcut metin seçilir.
3. Sorgu yazılır.
4. Aynı `sendKeys` çağrısında Enter gönderilir.

Arama önerisi beklenmez ve öneriye mouse ile tıklanmaz. Böylece eski akıştaki
iki saniyelik öneri bekleme süresi de kaldırılmıştır.

### Arama önerisi sonrası kategori hover düzeltmesi

Eski akışta arama önerisine gerçek mouse click gönderiliyordu. Öneri paneli
kapandığında pointer aynı ekran koordinatında kalıyor ve bu koordinatın altında:

```text
Oto, Bahçe, Yapı Market
```

gibi bir kategori bulunabilir.

Bu durumda kategoriye gerçek bir click yapılmaz; pointer aynı koordinatta kaldığı
için CSS `:hover` uygulanır ve kategori turuncu/seçili görünür. Fonksiyonel etkisi
olmamasının nedeni budur.

Kalıcı çözümde öneriye hiç mouse click gönderilmez. `HomePage.openSearch()` inputu
JavaScript ile focus eder; `searchFor()` aramayı Enter ile gönderir. Sonuçlar
gerçekten geldikten sonra pointer sabit, nötr viewport koordinatına taşınır:

```java
new Actions(driver)
    .moveToLocation(1, 1)
    .perform();
```

Burada click yoktur. Arama inputuna veya öneriye mouse click atılmadığı için
overlay kapanma yarışı ortadan kalkar; pointer hiçbir kategori alanında kalmaz.

### `verifyResultsFor()`

İki kanıt ister:

1. URL query veya H1 aranan metni içermelidir.
2. İkinci satır/birinci sütun adayı gerçekten oluşmalıdır.

### Grid JavaScript'i

Ana grid:

```javascript
document.querySelector("main ul[id='1']")
```

ile bulunur.

Önce doğrudan grid çocukları arasındaki görünür, kimlikli `li` ürün yuvaları
toplanır. Kimliksiz banner, sentinel ve diğer layout satırları görsel ürün
satırı sayılmaz. Bu ayrım önemlidir; Hepsiburada kartın yerini önce oluşturup
başlık linkini daha sonra asenkron olarak ekleyebilir.

Kesin hedef kart belirlendikten sonra yalnız o kartın içinde şu başlık linkleri
aranır:

```text
data-test-id='product-card-name'
h2 title linki
h2 ürün linki
h3 ürün linki
```

Şunlar elenir:

- Kimliği olmayan veya `li` olmayan grid çocukları,
- Boyutu sıfır olanlar,
- Grid genişliğinin büyük bölümünü kaplayan bannerlar,
- `display:none`,
- `visibility:hidden`.

Satır/sütun hesabında her kart yuvası için yalnız:

```text
card
x
y
```

bilgisi çıkarılır.

Algoritma:

1. Kartları önce Y, sonra X'e göre sıralar.
2. Y koordinatlarını 20 piksellik toleransla görsel satırlara ayırır.
3. İlk iki ürün satırının sütun sayıları eşit değilse hydration/reflow beklenir.
4. İlk satırdaki en küçük X'i birinci sütunun X koordinatı kabul eder.
5. İkinci satırda yalnız bu X ile en fazla 25 piksel farkı olan kartı hedef seçer.
6. İlk iki satırdaki bütün ürün linklerinin hydrate olmasını bekler.
7. Bu iki satırdaki bütün `URL@X:Y` değerlerinden tam yerleşim imzası üretir.
8. Birinci sütun hazır değilken ikinci sütuna kesinlikle fallback yapmaz.
9. Hedef hazır olduğunda link, ürün adı ve URL yalnız bu karttan çıkarılır.

Bu nedenle seçim:

- Sabit `nth-child` değerine,
- Kartın özel `id` formatına,
- DOM'daki beşinci elemana,
- O anda linki hazır olan en soldaki karta

bağlı değildir.

Sponsorlu veya yavaş hydrate olan kartın ürün yuvası satır/sütun hesabına katılır;
ancak ilk iki satırın bütün bağlantıları hazır olmadan yerleşim stabil sayılmaz.

### Stabil seçim

Yalnız hedef kartın iki kısa polling turunda aynı kalması yeterli değildir.
Sponsorlu bir kart gecikmeli olarak araya girdiğinde hedef kart aynı DOM elementi
olarak kalıp başka görsel slota taşınabilir.

Bu nedenle ilk iki satırdaki bütün kartların:

```text
URL@X:Y
```

değerlerinden üretilen tam yerleşim imzası kesintisiz üç saniye aynı kalmalıdır.
Scroll sonrasında stabilite sayacı sıfırdan başlatılır. Herhangi bir kartın URL'si
veya koordinatı değişirse süre yeniden başlar.

Bu kontrol, ilk yüklemede geçici olarak ikinci satır/birinci sütunda görünen bir
ürünün sponsorlu kart yerleşince birinci satır/dördüncü sütuna taşınması yarışını
engeller.

### Scroll ve tıklama

1. Mevcut sekmeler ve URL kaydedilir.
2. İlk iki satırın tam yerleşimi üç saniye stabil kalana kadar beklenir.
3. Scroll öncesi bir saniye beklenir.
4. Kesin hedef ekranın merkezine scroll edilir.
5. Ürün bir saniye görünür bırakılır.
6. Scroll sonrası tam yerleşim yeniden üç saniye stabil olana kadar beklenir.
7. Yenilenen hedef URL trusted-domain kontrolünden geçer.
8. İkinci satır/birinci sütun yeniden hesaplanır; beklenen URL değişmişse stabilite
   beklemesi sıfırdan başlar.
9. Hesaplama ve başlık linkine tıklama aynı JavaScript görevi içinde yapılır.
10. Yeni sekme varsa ona geçilir.
11. Son ürün detay URL'si tekrar trusted-domain kontrolünden geçirilir.
12. `SelectedProduct`, tıklanan kart adı ve gerçek detay URL'siyle oluşturulur.

Hesaplama ile click aynı tarayıcı JavaScript görevi içinde olduğu için React bu
iki işlem arasına yeni bir kart sıralaması sokamaz. Eski bir `WebElement`
referansı konumu değiştikten sonra tıklanmaz.

### `ProductCandidate`

Private record:

```java
WebElement link
String name
String url
int x
int y
String layoutSignature
```

taşır.

---

## 16. `ProductDetailPage`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/pages/ProductDetailPage.java
```

### `verifyProduct()`

1. Ürün H1 başlığı beklenir.
2. Detay URL'si alınır.
3. URL trusted-domain kontrolünden geçer.
4. Ürün kodu URL'de aranır.
5. Alternatif olarak ürün adları anlamlı biçimde karşılaştırılır.

Başarı için:

```text
ürün kodu eşleşmesi
VEYA
ürün adı eşleşmesi
```

yeterlidir.

### `addToCart()`

Sepete eklemeden hemen önce header sayaç değeri okunur:

```java
Integer visibleCount = visibleCartCount();
click(ADD_TO_CART, ADD_TO_CART_TEXT);
```

Bu başlangıç değeri:

- Eklemenin tam `+1` olduğunu,
- Cleanup'ın hangi değere dönmesi gerektiğini

belirler.

### `verifyAddToCartConfirmation()`

Önce görünür başarı toast/alert aranır. Metinde:

- “sepete eklendi”
- “sepetine eklendi”
- “ürün sepetinde”

gibi ifadeler beklenir.

Toast kaçırılırsa header sepet sayacının artması ikinci başarı kanıtıdır.

### `verifyCartCount()`

Sayaç yalnızca artmış olmamalıdır. Tam olarak:

```text
başlangıç + 1
```

olmalıdır.

---

## 17. `CartPage`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/pages/CartPage.java
```

### `waitUntilLoaded()`

1. Cookie tekrar görünürse kabul etmeyi dener.
2. `aria-busy=true` olduğu sürece bekler.
3. Boş sepet, görünür cart item veya pozitif sayaç kanıtlarından birini ister.

### `verifyContains()`

State'te saklanan aynı ürünün sepette bulunmasını bekler.

Eşleşme:

```text
ürün kodunun href içinde bulunması
VEYA
ürün adının anlamlı eşleşmesi
```

ile yapılır.

Ürün bağlantıları trusted URL kontrolünden geçirilir.

### Sepet toplamı

Normalize edilmiş body metnine:

```regex
\bsepetim\s+(\d+)\s+urun\b
```

uygulanır.

Örneğin:

```text
Sepetim 14 ürün
```

metninden `14` çıkarılır.

### `removeOneUnitAndRestoreCount()`

Cleanup'ın ana metodudur.

#### Sayaç zaten başlangıç değerindeyse

Metot işlem yapmadan döner. Cleanup idempotent olur.

#### Sayaç başlangıç + 1 değilse

Sepete hiçbir değişiklik yapılmaz. Bu güvenlik bariyeri, kullanıcının aynı anda
sepette başka değişiklik yapmış olabileceğini kabul eder.

Örneğin başlangıç `13` ise cleanup yalnızca mevcut sayı `14` olduğunda çalışır.

#### Ürün miktarı birden fazlaysa

Doğru ürün satırındaki:

```text
Ürünü Azalt
```

kontrolü kullanılır ve yalnızca bir adet azaltılır.

#### Ürün miktarı birse

Azaltma kontrolü yoksa:

```text
Sepetten Çıkar
```

veya trash butonu kullanılır.

Sayaç doğrudan güncellenmezse doğru **Sil** onayı aranır.

Her durumda sayaç gerçekten başlangıç değerine dönmelidir. Yalnızca butona tıklamak
başarı sayılmaz.

### Yalnız doğru ürün satırında işlem

Önce `SelectedProduct` ile eşleşen link bulunur. JavaScript `closest(...)` ile o
ürünün en yakın sepet satırına çıkılır. Azaltma/silme kontrolleri tüm sayfada değil
yalnızca bu satırın içinde aranır.

### `CartAdjustment`

Private record:

```java
WebElement control
boolean removesLine
```

taşır.

- `false`: Bir adet azalt.
- `true`: Satırı sil.

---

## 18. `SelectedProduct`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/model/SelectedProduct.java
```

Arama sonucunda seçilen ürünün değiştirilemez snapshot'ıdır:

```java
String name
String url
String code
```

Ürün kodu URL'den şu regex ile çıkarılır:

```regex
(?i)-p-([a-z0-9]+)(?:[/?#]|$)
```

Kod bulunursa büyük harfe çevrilir. Bulunmazsa boş bırakılır ve sonraki adımlar ad
eşleşmesiyle devam eder.

Veri zinciri:

```text
SearchResultsPage
    → SelectedProduct
        → ProductDetailPage
        → CartPage
        → Cleanup
```

---

## 19. `ScenarioState`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/support/ScenarioState.java
```

Gauge adımları farklı Java metotlarıdır. Bir adımın lokal değişkeni sonraki adımda
doğrudan bulunmaz. `ScenarioState`, Gauge `ScenarioDataStore` üzerinden senaryo içi
hafıza sağlar.

Saklanan değerler:

| Anahtar | Değer |
|---|---|
| `selected-product` | Seçilen ürün |
| `cart-count-before-add` | Sepete ekleme öncesi sayı |
| `add-attempted` | Ekleme gerçekleştirildi mi? |
| `cart-cleaned` | Cleanup tamamlandı mı? |

Değerler okunurken tip kontrolü yapılır. Ürün veya başlangıç sayısı kaydedilmemişse
varsayılan değerle devam edilmez.

Bu özellikle sepet güvenliği için önemlidir. Başlangıç sayısı bilinmeden cleanup
yapılmaz.

---

## 20. `HepsiburadaSteps`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/steps/HepsiburadaSteps.java
```

Gauge cümlelerini Page Object metotlarına bağlar.

Örnek:

```java
@Step("<query> metnini ara")
public void searchFor(String query) {
    searchResultsPage().searchFor(query);
}
```

Bu sınıfın görevleri:

- Doğru Page Object metodunu çağırmak,
- `ScenarioState`e veri yazmak/okumak,
- `TestLog` ile anlaşılır rapor üretmek.

Selector ve karmaşık DOM işlemleri bu sınıfta tutulmaz.

### Page Object üretimi

Örneğin:

```java
private HomePage homePage() {
    return new HomePage(driver(), config());
}
```

Her step yeni Page Object nesnesi oluşturabilir fakat yeni Chrome açılmaz. Hepsi
`DriverSession` içindeki aynı driver ve config'i kullanır.

### Veri aktarımı

Ürün seçildiğinde:

```java
ScenarioState.saveProduct(selectedProduct);
```

Sepete ekleme öncesi:

```java
ScenarioState.saveCartCountBeforeAdd(countBeforeAdding);
ScenarioState.markAddAttempted();
```

Cleanup tamamlandığında:

```java
ScenarioState.markCartCleaned();
```

---

## 21. `SeleniumHooks`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/hooks/SeleniumHooks.java
```

`@AfterScenario` işaretli metot her Gauge senaryosundan sonra çalışır.

### Normal başarı

Normal cleanup:

```text
cart-cleaned=true
```

yaptığı için hook sepete tekrar dokunmaz. Tarayıcıyı kapatır.

### Eklemeden önce hata

```text
add-attempted=false
```

olduğu için sepete müdahale edilmez.

### Eklemeden sonra hata

```text
add-attempted=true
cart-cleaned=false
```

ise hook:

1. Gerekirse sepeti açar.
2. Sepetin yüklenmesini bekler.
3. Aynı `SelectedProduct`u bulur.
4. Başlangıç sayısını okur.
5. Testin eklediği tek adedi geri almayı dener.

Cleanup hatası uyarı olarak raporlanır; asıl test hatası gizlenmez.

Tarayıcı dış `finally` bloğunda her durumda kapatılır.

---

## 22. `SeleniumScreenshotWriter`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/support/SeleniumScreenshotWriter.java
```

Gauge başarısızlık ekran görüntülerini üretir.

### Gizli alanların temizlenmesi

Gerçek screenshot alınmadan önce:

```text
#txtUserName
#txtPassword
```

alanları JavaScript ile temizlenir.

Hem canlı `value` property hem HTML `value` attribute boşaltılır. Gerçekten boş
oldukları doğrulanamazsa gerçek ekran görüntüsü alınmaz.

### Nötr PNG

Şu durumlarda 1x1 piksellik nötr PNG yazılır:

- Driver yok,
- Canlı pencere yok,
- Login alanları güvenle temizlenemiyor,
- Screenshot alınırken WebDriver hatası oluşuyor.

Bu davranış masaüstü veya gizli bilgilerin rapora girmesinden daha güvenlidir.

Dosya adı UUID içerir:

```text
selenium-<benzersiz-id>.png
```

---

## 23. `TestLog`

Dosya:

```text
src/test/java/com/virgosol/hepsiburada/support/TestLog.java
```

Mesajları hem:

- IntelliJ/terminal konsoluna,
- Gauge HTML raporuna

yazar.

Mesaj seviyeleri:

```text
[BAŞARILI]
[BİLGİ]
[UYARI]
```

`TestLog` otomatik secret maskelemez. E-posta ve şifre log metoduna hiçbir zaman
verilmemelidir. Mevcut kod bu bilgileri loglamaz.

---

## 24. Test sırasında taşınan veri

### Ürün verisi

```text
SearchResultsPage
    │
    ├── ürün adı
    ├── ürün URL'si
    └── ürün kodu
           ↓
     SelectedProduct
           ↓
     ScenarioState
        ↙       ↘
ProductDetail   CartPage
doğrulaması     doğrulama/cleanup
```

### Sepet sayısı

```text
Sepete Ekle öncesi sayaç
            ↓
ScenarioState.cartCountBeforeAdd
       ↙                    ↘
Ekleme +1 doğrulaması     Cleanup hedef sayısı
```

### Cleanup bayrakları

```text
markAddAttempted()
        ↓
Ekleme sonrası hata olursa hook cleanup gerektiğini bilir.

markCartCleaned()
        ↓
Normal cleanup tamamlandı; hook ikinci kez azaltmaz.
```

---

## 25. Testin tam çalışma sırası

Specification'daki beş concept toplam 18 gerçek Gauge adımına genişler.

### 1. Tarayıcıyı aç

```text
HepsiburadaSteps.openBrowser()
```

1. `TestConfig.fromEnvironment()` çağrılır.
2. Ortam değişkenleri doğrulanır.
3. `DriverSession.start()` çağrılır.
4. Chrome açılır.
5. Driver ve config mevcut thread'e kaydedilir.

### 2. Ana sayfaya git

```text
HomePage.open()
```

- Ana URL açılır.
- Document ready beklenir.
- Cookie kabul edilir.
- Header/arama alanı beklenir.

### 3. Hesabım menüsünü aç

```text
HomePage.openAccountMenu()
```

Bu giriş öncesindeki tek Hesabım etkileşimidir.

### 4. Login sayfasını aç

```text
HomePage.openLoginPage()
```

- Login bağlantısı tıklanır.
- Giriş sayfası ve cookie kontrol edilir.
- E-posta alanı beklenir.

### 5. E-posta ve şifreyi doldur

```text
LoginPage.fillCredentials()
```

- Trusted URL kontrol edilir.
- E-posta yazılır.
- Şifre yazılır.

### 6. Login butonuna tıkla

```text
LoginPage.submitAndWaitForLoginCompletion()
```

- Buton bulunur.
- Bir saniye beklenir.
- Butona tıklanır.
- Redirect, hata ve CAPTCHA kontrolleri yapılır.

### 7. Girişi doğrula

```text
HomePage.verifyLoggedIn()
```

- Login alanları kaybolmalıdır.
- Arama alanı görünmelidir.
- Hesabım alanına dokunulmaz.

### 8. Arama alanını aç

```text
HomePage.openSearch()
```

- Pointer viewport'un nötr sol üst noktasına taşınır.
- Input mouse click kullanılmadan JavaScript ile focus edilir.

### 9. `bilgisayar` yaz ve ara

```text
SearchResultsPage.searchFor("bilgisayar")
```

- `enterSearchQuery()` metni yazar ve Enter ile doğrudan gönderir.
- Arama önerisi beklenmez veya tıklanmaz.
- Sonuçlar doğrulandıktan sonra pointer nötr viewport noktasına taşınır.
- Kategori üzerinde kalan hover temizlenir.

### 10. Sonuçları doğrula

```text
SearchResultsPage.verifyResultsFor("bilgisayar")
```

- URL query veya H1 sorguyu içermelidir.
- İkinci satır/birinci sütun adayı oluşmalıdır.

### 11. İkinci satır/birinci ürünü aç

```text
SearchResultsPage.openFirstColumnProductInSecondVisualRow()
```

- Grid önce linklerden bağımsız kart yuvalarıyla hesaplanır.
- Yalnız kimlikli gerçek ürün `li` yuvaları satır hesabına katılır.
- İkinci satırda yalnız ilk sütun X koordinatındaki slot kabul edilir.
- İlk iki satırdaki linkler gecikirse beklenir; ikinci sütuna geçilmez.
- İlk iki satırın tam yerleşimi üç saniye stabil olmalıdır.
- Scroll öncesi bir saniye beklenir.
- Ürüne scroll yapılır.
- Ürün bir saniye görünür bırakılır.
- Scroll sonrası tam yerleşim yeniden stabilize edilir.
- Aynı kesin slot hesaplanır ve tek atomik JavaScript adımında tıklanır.
- `SelectedProduct` gerçek detay URL'siyle oluşturulur.
- Yeni sekme varsa geçilir.

### 12. Ürün detayını doğrula

```text
ProductDetailPage.verifyProduct()
```

- Ürün kodu veya ürün adı eşleşmelidir.

### 13. Sepete ekle

```text
ProductDetailPage.addToCart()
```

- Başlangıç sepet sayısı kaydedilir.
- Sepete Ekle tıklanır.
- `add-attempted=true` yapılır.

### 14. Sepete eklemeyi doğrula

```text
ProductDetailPage.verifyAddToCartConfirmation()
ProductDetailPage.verifyCartCount()
```

- Toast veya sayaç artışı doğrulanır.
- Sayaç tam başlangıç + 1 olmalıdır.

### 15. Sepeti aç

```text
HomePage.openCartFromHeader()
CartPage.waitUntilLoaded()
```

### 16. Ürünü sepette doğrula

```text
CartPage.verifyContains()
```

- Aynı ürün kod/ad üzerinden bulunmalıdır.

### 17. Sepeti üç saniye göster

```text
CartPage.observe(3)
```

Selenium Actions pause kullanılır.

### 18. Test ürününü geri al

```text
CartPage.removeOneUnitAndRestoreCount()
```

- Miktar >1 ise bir adet azaltılır.
- Miktar 1 ise ürün silinir.
- Sayaç başlangıç değerine dönmelidir.
- `cart-cleaned=true` yapılır.

### Senaryo sonu hook

```text
SeleniumHooks.closeBrowserAndCleanResidualCartData()
```

- Normal cleanup yapılmamışsa telafi cleanup'ı dener.
- Chrome her durumda kapatılır.

---

## 26. Hata durumları

### Ortam değişkeni eksik

Tarayıcı açılmadan test durur.

### Gauge kurulu değil

`run-tests.cmd` hata çıkış koduyla durur.

### Cookie görünmüyor

Cookie opsiyonel olduğu için test devam eder.

### Cookie Shadow DOM içinde

Recursive Shadow DOM JavaScript'i ile bulunur.

### Element hazır değil

Explicit wait 200 milisaniyede bir tekrar kontrol eder.

### Element stale

Polling veya yeniden bulma mekanizması devreye girer.

### Click overlay tarafından engelleniyor

Cookie kontrolü, Actions click ve JavaScript click fallback'leri uygulanır.

### Yabancı siteye yönlendirme

Trusted URL kontrolü testi durdurur.

### CAPTCHA

Test güvenlik kontrolünü aşmaya çalışmadan durur.

### Login hatası

Hata metni raporlanır; e-posta ve şifre maskelenir.

### Yanlış ürün detayına gidilmesi

Ürün kodu/ad eşleşmediği için test başarısız olur.

### Sepet sayacı +1 olmuyor

Sepete ekleme başarısız kabul edilir.

### Sepet beklenmeyen değerde

Cleanup hiçbir ürünü değiştirmez.

### Test eklemeden sonra yarım kalıyor

`SeleniumHooks` aynı üründen yalnız bir adet geri almayı dener.

### Screenshot güvenli alınamıyor

Gerçek görüntü yerine nötr PNG yazılır.

---

## 27. Tasarımın güçlü yönleri

1. Senaryo Gauge ile iş dilinde okunabiliyor.
2. Selenium detayları Page Object sınıflarına ayrılmış.
3. Normal DOM ve açık Shadow DOM cookie kontrolü var.
4. Kimlik bilgileri dosyalarda tutulmuyor.
5. Hassas işlemlerden önce trusted-domain kontrolü yapılıyor.
6. CAPTCHA aşılmaya çalışılmıyor.
7. Uzun kör `Thread.sleep` beklemeleri yerine explicit wait ve Actions pause var.
8. Arama, input veya öneriye mouse click göndermeden klavyeyle başlatılıyor.
9. Pointer nötr viewport noktasına taşınarak kategori hover görünümü engelleniyor.
10. İkinci satır/birinci sütun gerçek görsel koordinatlarla belirleniyor.
11. İlk iki satırın tam yerleşimi scroll öncesi ve sonrasında stabilize ediliyor.
12. Konum hesabı ile click atomik yapılarak eski karta tıklama yarışı kapatılıyor.
13. İlk sütunun linki gecikirse komşu sütuna geçmek yerine doğru kart bekleniyor.
14. Seçilen ürün detay ve sepette yeniden doğrulanıyor.
15. Sepete ekleme kesin sayaç artışıyla doğrulanıyor.
16. Cleanup yalnız testin eklediği tek adedi geri alıyor.
17. Beklenmeyen sayaç durumunda sepeti değiştirmeyi reddediyor.
18. Başarısız senaryo için telafi cleanup hook'u var.
19. Screenshot öncesi giriş alanları temizleniyor.
20. Tarayıcı `finally` bloğunda kapatılıyor.

---

## 28. Bilinmesi gereken sınırlar

- Hepsiburada DOM yapısı değişirse selectorlar güncellenmelidir.
- Canlı site veya ağ yavaşsa explicit wait üst sınırına ulaşılabilir.
- Aynı hesap/sepet eşzamanlı başka yerde değiştirilmemelidir.
- Closed Shadow DOM taranamaz.
- CAPTCHA görüldüğünde testin durması bilinçli güvenlik davranışıdır.
- Hook ağ veya kapanmış tarayıcı nedeniyle cleanup yapamazsa sepet manuel kontrol
  edilmelidir.
- Headless ve görünür modda kolon sayısı değişebilir; grid algoritması sabit kolon
  sayısı yerine gerçek X/Y koordinatlarını kullandığı için buna uyum sağlar.

---

## 29. Çalıştırma ve çıktılar

### IntelliJ

`Hepsiburada E2E` Run Configuration çalıştırılır.

### Windows scripti

```powershell
.\run-tests.cmd
```

### Gauge doğrudan

```powershell
mvn clean test-compile
gauge run --verbose specs\hepsiburada-shopping.spec
```

### Maven live profili

```powershell
mvn -P live-e2e test
```

### Çıktılar

```text
reports/html-report/index.html → HTML raporu
logs/gauge.log                 → Gauge çalışma logu
.gauge/screenshots             → Hata screenshotları
target/test-classes            → Derlenmiş test sınıfları
target/gauge-libs              → Gauge runtime bağımlılıkları
```

---

## 30. Sonuç

Bu projede:

```text
“Ne yapılacak?”
    → Gauge specification ve concept dosyalarında

“Web sayfasında nasıl yapılacak?”
    → Page Object sınıflarında

“Ortak teknik işlemler nasıl yapılacak?”
    → BasePage içinde

“Adımlar arasında veri nasıl taşınacak?”
    → SelectedProduct ve ScenarioState ile

“Test yarım kalırsa ne olacak?”
    → SeleniumHooks ile

“Hata nasıl raporlanacak?”
    → TestLog, ScreenshotWriter ve HTML report ile
```

tanımlanmıştır.

Tam veri ve işlem zinciri:

```text
Gauge senaryosu
    → HepsiburadaSteps
    → Page Object işlemi
    → SelectedProduct / ScenarioState
    → Ürün detay doğrulaması
    → Sepete ekleme ve sayaç doğrulaması
    → Sepette aynı ürünü doğrulama
    → Güvenli cleanup
    → AfterScenario hook
    → Tarayıcı kapatma ve raporlama
```
