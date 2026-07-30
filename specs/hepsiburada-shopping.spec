# Hepsiburada'da giriş, ürün arama ve sepete ekleme akışı

Tags: e2e, ui, live, destructive, serial

Bu canlı uçtan uca senaryo yalnızca test için ayrılmış bir Hepsiburada hesabıyla
ve aynı anda tek test çalışacak şekilde yürütülür. Kullanıcı bilgileri spec
dosyasında bulunmaz; `HB_EMAIL` ve `HB_PASSWORD` ortam değişkenlerinden alınır.

Arayüzü daha önce görmemiş bir kişi için akışın başlangıç noktası ana sayfanın üst
menüsüdür. Bu menüde hesap işlemleri için **Hesabım**, ürün bulmak için bir arama
alanı bulunur. **Hesabım** açıldığında görünen **Giriş yap** bağlantısı, e-posta
ve şifre alanlarının bulunduğu giriş ekranına götürür. Bilgiler yazıldıktan sonra
sayfa kaydırılmaz; o anda görünür olan **Giriş yap** düğmesi kullanılır.

Oturum açıldıktan sonra üst menüde kullanıcı bilgisinin görünmesi başarılı giriş
kanıtıdır. Arama alanına tıklandıktan sonra yazmaya başlamadan önce 1 saniye
beklenir, `bilgisayar` yazılır ve Enter ile arama başlatılır. Sonuç sayfasında
arama başlığı ile ürün kartlarının gelmesi doğrulanır. Kartlar görsel satır ve
sütun konumlarına göre değerlendirilir; ikinci görsel satırın ilk kartı açılır.

Ürün detayında **Sepete ekle** düğmesine basılınca bir sepete ekleme penceresi
açılır. Sepete geçiş, sayfanın üstündeki sepet alanına dönülerek değil, bu
pencerede beliren **Sepete git** düğmesine tıklanarak yapılır. Seçilen ürünün adı
ve bağlantı kodu akış boyunca saklanır; sepet doğrulaması aynı ürünü arar.
Senaryonun sonunda yalnızca testin eklediği bir adet ürün kaldırılır ve başlangıç
sepet sayısı korunur.

## Bilgisayar sonuçlarının ikinci görsel satırındaki ilk ürünü sepete ekleme

* Ayrılmış test hesabıyla giriş yap ve kullanıcı bilgisini doğrula
* "bilgisayar" ürününü ara, sonuçları doğrula ve ikinci görsel satırın ilk ürününü aç
* Açılan ürün detayından sepete ekle ve bildirim penceresinden sepete git
* Seçilen ürünün sepet listesinde bulunduğunu doğrula
* Testin eklediği tek ürün adedini kaldır ve sepeti başlangıç durumuna döndür
