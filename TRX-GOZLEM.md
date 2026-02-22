# Transaction testleri — Ne gözlemlenir?

Bu sayfa, `api.http` içindeki transaction endpoint’lerini çalıştırırken **neye bakılacağını** kısaca özetler.

---

## 1. Stock adjust + gecikme (`/api/tx-tests/stock-adjust`)

**İstek:** Bir ürünün stokuna `delta` eklenir, isteğe bağlı `delayMs` kadar beklenir.

**Gözlem:**

- **PostgreSQL:** Aynı ürüne eşzamanlı iki istek (farklı delta, ikisi de delay’li) gönderirseniz transaction izolasyonu ve kilit davranışı görülür. Gecikme varsa diğer işlem bekler veya deadlock oluşabilir.
- **Redis:** Single-thread; aynı key üzerinde işlemler sırayla işlenir. Deadlock yok; ancak uygulama tarafında “okuduğum stokla güncellediğim stok aynı mı?” (optimistic lock / WATCH) kontrolü yoksa yarış koşulu görülebilir.

**Ne not edilir:** İki uygulamada aynı senaryoyu (2 paralel stock-adjust) deneyip: son stok değeri tutarlı mı, hata/retry var mı?

---

## 2. Zorunlu rollback (`/api/tx-tests/order-rollback`)

**İstek:** Önce sipariş oluşturulur, hemen ardından bilerek exception fırlatılır.

**Gözlem:**

- **PostgreSQL:** Transaction rollback edilir; sipariş ve stok düşümü geri alınır. Veritabanında sipariş kaydı oluşmaz.
- **Redis:** Uygulama tek transaction’da değilse: sipariş yazıldıktan sonra exception’da “rollback” yok; kısmi yazma (sipariş var, stok düşmemiş veya tam tersi) mümkün. Projede bu endpoint’in nasıl implemente edildiği (transaction sınırı nerede?) kontrol edilip “kısmi yazma oldu mu?” not edilir.

**Ne not edilir:** Her iki tarafta istek sonrası: sipariş tablosu/key’i ve ilgili ürün stokları. PG’de sipariş yok, stok eski; Redis’te ne var?

---

## 3. Deadlock simülasyonu (`/api/tx-tests/deadlock`)

**İstek:** İki product id ve `delayMs`. Servis sadece delay yapıyor; gerçek kilit almıyor olabilir (mevcut projede `simulateDeadlock` sadece sleep).

**Gözlem:**

- **PostgreSQL:** Eğer iki farklı transaction A→B ve B→A sırasıyla kilit alıyorsa gerçek deadlock oluşur; biri deadlock exception alır.
- **Redis:** Tek thread; aynı anda tek komut işlendiği için deadlock oluşmaz. “Deadlock not applicable for Redis” benzeri yanıt beklenir.

**Ne not edilir:** PG’de deadlock testi için iki paralel istek (farklı sırada product id) gerekebilir. Redis’te aynı istekler deadlock üretmez; davranış farkı makalede vurgulanabilir.

---

## Paralel çalıştırma

Aynı anda birden fazla transaction isteği göndermek için:

- **REST Client (VS Code):** İlgili request bloklarını aynı anda “Send Request” ile (veya bir script ile) tetikleyebilirsiniz.
- **Script:** `scripts/load-test.py` benzeri bir script ile aynı anda N adet `stock-adjust` veya `order-rollback` gönderip yanıt ve veri tutarlılığını kontrol edebilirsiniz.

Özet: Transaction testleriyle **rollback davranışı**, **eşzamanlı güncelleme** ve **deadlock** farklarını gözlemleyip makalede “Redis primary DB’de işlem semantiği” başlığı altında kullanabilirsiniz.
