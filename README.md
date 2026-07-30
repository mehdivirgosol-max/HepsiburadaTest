# Hepsiburada UI otomasyonu

Bu proje, Gauge ve Selenium ile aşağıdaki canlı kullanıcı akışını doğrular:
giriş, `bilgisayar` araması, ikinci görsel satırın birinci sütunundaki ürünü açma, ürünü
detay sayfasından sepete ekleme, açılan penceredeki **Sepete git** düğmesiyle sepete
geçme, ürünü sepette doğrulama ve testin eklediği ürünü kaldırarak sepeti önceki
durumuna döndürme. Assertion kapsamı başarılı giriş, arama sonuçlarının gelmesi ve
kaydedilen ürünün sepette görünmesiyle sınırlıdır.

## Ön koşullar

- JDK 17 veya üzeri
- Maven 3.9.11 veya üzeri
- Gauge ve Gauge Java eklentisi
- Google Chrome
- Yalnızca bu test için ayrılmış bir Hepsiburada hesabı

Kimlik bilgileri hiçbir proje dosyasına yazılmaz. Çalıştırmadan önce
`HB_EMAIL` ve `HB_PASSWORD` ortam değişkenlerini işletim sistemi, IDE Run
Configuration veya CI secret yönetimi üzerinden tanımlayın.

İsteğe bağlı ayarlar:

| Değişken | Varsayılan | Açıklama |
|---|---:|---|
| `HB_BASE_URL` | `https://www.hepsiburada.com` | Test edilen ana adres |
| `HB_HEADLESS` | `false` | Chrome'u headless çalıştırır |
| `HB_TIMEOUT_SECONDS` | `25` | Açık beklemelerin üst sınırı |
| `HB_TYPING_DELAY_MS` | `60` | Karakterler arasındaki kontrollü gecikme |

## Çalıştırma

`HB_EMAIL` ve `HB_PASSWORD` işletim sistemi ortam değişkenleri IntelliJ
başlatılmadan önce tanımlanmış olmalıdır. Değişkenleri IntelliJ açıkken
tanımladıysanız IDE'yi yeniden başlatın. Gizli değerler paylaşılan Run
Configuration dosyasına yazılmaz.

`ScenarioState.java` çalıştırılacak sınıf değildir; yalnızca senaryo sırasında
seçilen ürün ve sepet bilgisini saklar. Gauge adımları
`HepsiburadaSteps.java` içinden otomatik çağırır.

## Test mimarisi

`specs/hepsiburada-shopping.spec` iş akışını ve arayüzde görülen alanları,
`specs/concepts/hepsiburada.cpt` ise bu akışı oluşturan tekrar kullanılabilir
adımları anlatır. `HepsiburadaSteps.java`, Gauge cümlelerini ilgili iş akışına
bağlayan ince katmandır.

Page Object sınıfları yalnızca sayfaya özgü iş akışlarını içerir. CSS, XPath ve
ID locator'ları `HepsiburadaLocators.java` içinde arayüz bölgesine göre merkezi
olarak yönetilir. Bekleme, kontrollü yazma ve tıklama gibi bütün ortak Selenium
eylemleri `BasePage.java` içindedir. Standart tıklama öğeyi görünür alana
ortalayabilir; giriş formunun gönderiminde kullanılan kaydırmasız tıklama ise
mevcut scroll konumunu korur.

## Güvenli hata davranışı

Hepsiburada `N1E2` gibi bir giriş hatası veya CAPTCHA/güvenlik doğrulaması
döndürürse test otomatik tekrar ya da aşma girişiminde bulunmadan durur. Böyle
bir durumda aynı test hesabıyla standart Chrome üzerinden manuel girişi
doğrulayın; hesap veya servis sorunu çözülmeden sepet adımlarını çalıştırmayın.
Başarısızlık ekran görüntülerinde giriş alanları temizlenir. Güvenli tarayıcı
görüntüsü alınamazsa masaüstü yerine nötr bir hata görseli rapora eklenir.

Ana sayfa açıldığında `#hb-accept-all` kimlikli **Kabul Et** kontrolü kısa explicit
wait ile aranır ve diğer işlemlerden önce tıklanır. Efilli bileşeni shadow DOM
içinde oluşturduğunda kontrol shadow root içinde de aranır. Giriş/e-posta
sayfasına geçildiğinde aynı kontrol yeniden yapılır; görünürse kimlik bilgileri
yazılmadan önce kabul edilir. E-posta ve şifre yazıldıktan sonra **Giriş yap**
düğmesine tıklamadan önce tam 1 saniye beklenir ve sayfa kaydırılmadan o anda
görünür olan düğmeye tıklanır. Giriş doğrulamasından sonra
**Hesabım** alanına tekrar tıklanmaz veya odaklanılmaz; doğrudan arama alanına
tıklanır. Arama alanına tıklandıktan sonra `bilgisayar` yazılmadan önce tam 1 saniye
beklenir. Sonuç kartı hazır olduğunda scroll işleminden önce 1 saniye, hedef ürün
ekranda göründükten sonra da 1 saniye beklenerek ürün bağlantısına tıklanır.
Ürün detayında **Sepete ekle** tıklandıktan sonra açılan penceredeki
**Sepete git** düğmesi kullanılır; header sepet bağlantısına dönmek için yukarı
scroll yapılmaz. Ürün detay sayfasının tamamen yüklenmesi ve butonun scroll
sonrasında 2 saniye stabil kalması beklenir. Sepete ekleme penceresi tembel
yüklendiği için **Sepete git** düğmesi görünmeden ilgili adım tamamlanmaz; bu
pencere için en az 45 saniyelik üst bekleme sınırı uygulanır.

Proje, Chrome'un standart otomasyon bilgi çubuğunu ve WebDriver otomasyon
işaretlerini azaltan başlangıç seçeneklerini kullanır. Sahte user-agent kullanmaz
ve CAPTCHA/anti-bot kontrollerini aşmaya çalışmaz. Bu tür kontroller Hepsiburada
tarafından gösterilirse senaryo güvenli biçimde durur. Yanlış pozitif devam ederse
ayrılmış test hesabını normal Chrome'da manuel olarak doğrulayın ve yetkili bir
test ortamı ya da destek kanalı kullanın.
