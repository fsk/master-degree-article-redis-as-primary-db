# Redis Primary DB — Gözlem ve Değerlendirme Notları

Makalede Redis’in primary veritabanı olarak kullanımının gözlemlenmesi ve avantaj/dezavantajların çıkarılması için notlar.

---

## Başlangıç

**Ortam:** Docker ile Redis, PostgreSQL; istenirse Redis Insight, Prometheus, Grafana. İki uygulama ayrı portlarda: PostgreSQL 18080, Redis 18081.

```bash
docker compose up -d
cd spring-boot-postgresql && ./mvnw spring-boot:run
# Ayrı terminalde:
cd spring-boot-redis && ./mvnw spring-boot:run
```

**İş yükü:** Önce küçük ölçekte denemek için aynı parametrelerle hem PG hem Redis’e veri üretmek mantıklı. Örnek: 500 user, 500 product, 2000 order. Süreleri kaydetmek iyi olur; makalede ölçek büyütülebilir (25k user/product, 100k order). `benchmark` komutu süreleri otomatik yazdırıyor.

```bash
./run-tool.sh data-tools generate --users 500 --products 500 --orders 2000 --only pg
./run-tool.sh data-tools generate --users 500 --products 500 --orders 2000 --only redis
```

**Tutarlılık:** `./run-tool.sh data-tools counts` ile iki taraftaki kayıt sayıları kontrol edilebilir.

**Redis’e doğrudan bakış:** Redis Insight http://localhost:15540 veya CLI: `docker exec -it redis-as-primary-database-redis redis-cli -a redispass`. Key’ler: users, products, orders (hash yapıları).

---

## Gözlem Başlıkları

**Performans.** Throughput için aynı user/product/order sayısında toplam süre (generate veya benchmark). Tekil istek gecikmesi için requests.http ile aynı endpoint’lere istek atıp yanıt sürelerine bakılabilir. Ölçeklenme: farklı order sayıları (1k, 10k, 100k) ile benchmark; süre–kayıt sayısı ilişkisi. Makalede somut sayılar kullanılabilir: “X siparişte Redis Y saniye, PostgreSQL Z saniye.”

**Sorgulama ve modelleme.** Projede Order–User ve Order–Product ilişkileri uygulama katmanında, id ile çekiliyor. Redis’te JOIN yok; ilişkiler key/id üzerinden. Kullanıcıya göre sipariş listesi gibi ihtiyaçlar için PostgreSQL’de index + SQL yeterli; Redis’te ek veri yapıları (set, sorted set) veya tüm key’ler üzerinde tarama gerekebilir. Agregasyon (toplam satış, stok özeti) PostgreSQL’de SQL ile; Redis’te uygulama tarafında toplama veya ek yapılar. Bu noktalar “primary DB olarak Redis kısıtları” başlığı altında örneklenebilir.

**İşlem (transaction) davranışı.** TransactionTestController/TransactionTestService: (1) Stock adjust + gecikme — eşzamanlı stok güncellemelerinde Redis tarafında WATCH/MULTI/EXEC veya uygulama seviyesi nasıl kullanılıyor? (2) Forced rollback — sipariş sonrası bilerek exception; PostgreSQL’de rollback, Redis’te kısmi yazma riski var mı, nasıl engellendi? (3) Deadlock — PostgreSQL’de gerçek deadlock senaryosu; Redis single-thread olduğu için deadlock yok, ancak rakip güncellemelerde retry/contention nasıl? requests.http’deki tx-tests endpoint’leri her iki sisteme gönderilip sonuç ve tutarlılık not edilebilir. Sonuç cümlesi: Redis’te tam ACID yok; gerekirse uygulama ile telafi edildiği vurgulanabilir.

**Kalıcılık.** redis.conf: AOF açık, appendfsync everysec; RDB snapshot da tanımlı. everysec ile çökme anında en fazla ~1 saniyelik veri kaybı riski. PostgreSQL WAL ile karşılaştırma; Redis’te AOF always / everysec trade-off’u. `docker compose restart redis` sonrası veri kalıyor mu kontrol edilebilir; makalede “primary DB olarak Redis’te kalıcılık ve riskler” kısa bir alt başlık olabilir.

**Operasyon.** Yedekleme: PostgreSQL pg_dump; Redis RDB veya AOF kopyası. Monitoring: Prometheus’ta Redis uygulaması da scrape ediliyor; Grafana’da JVM/HTTP metrikleri karşılaştırılabilir. Bellek: Redis veriyi RAM’de tutar; büyük veri setinde bellek planlaması. Bu maddeler avantaj/dezavantaj tablosunda operasyonel farklar olarak özetlenebilir.

---

## Avantaj / Dezavantaj (Taslak)

Gözlemlere göre doldurulacak özet:

| Boyut | Avantajlar | Dezavantajlar |
|--------|------------|----------------|
| Performans | Düşük gecikme, yüksek okuma/yazma (ölçeğe bağlı) | Büyük veri setinde bellek; karmaşık sorguda uygulama yükü |
| Sorgulama | Key/value ve hash erişimi hızlı | JOIN yok; agregasyon ve raporlama uygulama tarafında |
| İşlem | Lua/MULTI ile basit çoklu key işlemleri | Tam ACID yok; rollback ve tutarlılık uygulama ile |
| Kalıcılık | AOF/RDB ile yapılandırılabilir | Varsayılan cache odaklı; everysec ile 1 sn kayıp riski |
| Operasyon | Kurulum basit, tek binary | Yedekleme, bellek, scale-out (cluster) ayrıca planlanmalı |

---

## İzlenebilecek Sıra

1. Ortamı ayağa kaldır, küçük veri ile generate + counts, Redis Insight’ta key yapısına bak.
2. Benchmark ile süre karşılaştırması; throughput sonuçlarını tabloya işle.
3. Transaction test endpoint’lerini PG ve Redis’te dene; işlem semantiği notlarını al.
4. Redis restart sonrası veri kontrolü.
5. Tezde: deneysel düzenek (iki uygulama, aynı iş yükü) → gözlemler (süre, davranış) → avantaj ve dezavantajlar (yukarıdaki tabloya dayanarak).
