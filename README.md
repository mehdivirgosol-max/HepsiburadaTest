# Hepsiburada UI otomasyonu

Bu proje, Gauge ve Selenium ile aşağıdaki canlı kullanıcı akışını doğrular:
giriş, `bilgisayar` araması, ikinci görsel satırın birinci sütunundaki ürünü açma, ürünü
detay sayfasından sepete ekleme, sepette doğrulama ve testin eklediği ürünü
kaldırarak sepeti önceki durumuna döndürme.

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
düğmesine tıklamadan önce tam 1 saniye beklenir. Giriş doğrulamasından sonra
**Hesabım** alanına tekrar tıklanmaz veya odaklanılmaz; doğrudan arama alanına
tıklanır. Arama önerisi en fazla 2 saniye aranır; sonuç kartı hazır olduğunda
scroll işleminden önce 1 saniye, hedef ürün ekranda göründükten sonra da 1 saniye
beklenerek ürün bağlantısına tıklanır.

Proje, Chrome'un standart otomasyon bilgi çubuğunu ve WebDriver otomasyon
işaretlerini azaltan başlangıç seçeneklerini kullanır. Sahte user-agent kullanmaz
ve CAPTCHA/anti-bot kontrollerini aşmaya çalışmaz. Bu tür kontroller Hepsiburada
tarafından gösterilirse senaryo güvenli biçimde durur. Yanlış pozitif devam ederse
ayrılmış test hesabını normal Chrome'da manuel olarak doğrulayın ve yetkili bir
test ortamı ya da destek kanalı kullanın.
