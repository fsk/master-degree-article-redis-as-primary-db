# Projeyi Çalıştırma ve Bileşenler — Detaylı Rehber

Bu doküman, “Redis’in primary veritabanı olarak kullanımı” konulu yüksek lisans projesinin nasıl çalıştırılacağını, proje içindeki her bir bileşenin ne işe yaradığını ve kullanılan araçların (run-tool.sh, data-tools, Docker vb.) nasıl çalıştığını adım adım açıklar. Onay ve denetim için hazırlanmıştır.

---

## İçindekiler

1. [Projenin Amacı ve Genel Yapı](#1-projenin-amacı-ve-genel-yapı)  
2. [Dizin ve Dosya Yapısı](#2-dizin-ve-dosya-yapısı)  
3. [Docker Servisleri](#3-docker-servisleri-docker-compose)  
4. [run-tool.sh — Nasıl Çalışır?](#4-run-toolsh--nasıl-çalışır)  
5. [data-tools — Komutlar](#5-data-tools--komutlar-ve-ne-işe-yaradıkları)  
6. [Uygulama Sunucularının Çalıştırılması](#6-uygulama-sunucularının-çalıştırılması-spring-boot)  
7. [API İstekleri: api.http ve requests.http](#7-api-istekleri-apihttp-ve-requestshttp)  
8. [Yük Testi: scripts/load-test.py](#8-yük-testi-scriptsload-testpy)  
9. [İzleme: Prometheus ve Grafana](#9-izleme-prometheus-ve-grafana)  
10. [Dokümanlar: GOZLEM-PLANI.md ve TRX-GOZLEM.md](#10-dokümanlar-gozlem-planimd-ve-trx-gozlemmd)  
11. [Örnek Çalıştırma Akışı](#11-örnek-çalıştırma-akışı-baştan-sona)

---

## Gereksinimler

Projeyi yerel ortamda çalıştırmak için:

- **Docker ve Docker Compose** — PostgreSQL, Redis, Redis Insight, Prometheus ve Grafana konteynerleri için.
- **Java (JDK 17+)** ve **Maven** — Spring Boot uygulamaları için (projede Maven wrapper `./mvnw` kullanılır).
- **Python 3** — data-tools ve scripts için; `run-tool.sh` proje dizininde `.venv` oluşturup bağımlılıkları yükler.
- **Tarayıcı** — Grafana (13000), Redis Insight (15540) ve isteğe bağlı Prometheus (19090) arayüzleri için.

---

## 1. Projenin Amacı ve Genel Yapı

Proje, **aynı iş mantığına** (kullanıcı, ürün, sipariş) sahip iki ayrı uygulama üzerinden **PostgreSQL** ile **Redis**’i primary veritabanı olarak karşılaştırmayı hedefler. İki uygulama da aynı REST API’yi sunar; fark, verinin nerede ve nasıl saklandığıdır. Veri üretimi, ölçüm ve izleme araçlarıyla her iki sistem aynı iş yüküne tabi tutularak performans ve davranış gözlemlenir.

---

## 2. Dizin ve Dosya Yapısı

Aşağıdaki tablo, proje kökündeki önemli dizin ve dosyaların ne işe yaradığını özetler.

| Yol | Açıklama |
|-----|----------|
| `spring-boot-postgresql/` | PostgreSQL’i primary DB olarak kullanan Spring Boot uygulaması. Port **18080**. |
| `spring-boot-redis/` | Redis’i primary DB olarak kullanan Spring Boot uygulaması. Port **18081**. |
| `data-tools/` | Veri üretimi, silme, sayım ve benchmark için Python araçları. `main.py` ve alt komutlar (generate, benchmark, truncate, drop, counts). |
| `scripts/` | Yük testi script’i: saniyede yüksek istek (RPS) veya toplu (bulk) ürün oluşturma. |
| `docker/` | Redis ve (varsa) PostgreSQL başlangıç yapılandırmaları (redis.conf, postgres-init vb.). |
| `observability/` | Prometheus ve Grafana yapılandırmaları; metrik toplama ve dashboard tanımları. |
| `api.http` | Tüm CRUD ve transaction test isteklerinin sıralı ve toplu kullanımı için HTTP istek dosyası (REST Client / IDE). |
| `requests.http` | Eski/alternatif HTTP istek örnekleri (PG ve Redis ayrı bloklar). |
| `run-tool.sh` | Python tabanlı alt projeleri (şu an sadece data-tools) sanal ortam ile çalıştıran giriş script’i. |
| `docker-compose.yml` | PostgreSQL, Redis, Redis Insight, Prometheus, Grafana konteynerlerinin tanımı. |
| `GOZLEM-PLANI.md` | Makalede gözlem ve avantaj/dezavantaj çıkarımı için notlar ve izlenecek sıra. |
| `TRX-GOZLEM.md` | Transaction test senaryolarında neye bakılacağının özeti. |

---

## 3. Docker Servisleri (docker-compose)

`docker compose up -d` ile ayağa kalkan servisler aşağıdadır. Uygulama sunucuları (Spring Boot) Docker’da değil, makinede ayrıca çalıştırılır.

| Servis | Görüntü | Port (host) | İşlevi |
|--------|---------|-------------|--------|
| **postgres** | postgres:16-alpine | 15432 | PostgreSQL veritabanı. spring-boot-postgresql bu porta bağlanır. |
| **redis** | redis:7-alpine | 16379 | Redis sunucusu. spring-boot-redis bu porta bağlanır. AOF + RDB ile kalıcılık açık. |
| **redis-always** | redis:7-alpine | 16380 | Opsiyonel; `docker compose --profile aof-always up` ile çalışır. AOF always modunda Redis. |
| **redisinsight** | redis/redisinsight | 15540 | Redis verisine tarayıcıdan bakmak için arayüz (http://localhost:15540). |
| **prometheus** | prom/prometheus | 19090 | Metrik toplama. Her iki Spring Boot uygulamasının /actuator/prometheus endpoint’ini scrape eder. |
| **grafana** | grafana/grafana | 13000 | Görselleştirme. Prometheus’u veri kaynağı olarak kullanır; hazır dashboard’lar yüklenir (http://localhost:13000). |

**Not:** Spring Boot uygulaması çalışan makine, Prometheus’un scrape edebilmesi için `host.docker.internal` ile erişilebilir olmalıdır (Docker Desktop’ta varsayılan; Linux’ta ek ayar gerekebilir).

---

## 4. run-tool.sh — Nasıl Çalışır?

`run-tool.sh`, proje kökünden çağrılan tek giriş noktasıdır. Amacı: **Python ile yazılmış alt projeleri** (şu an yalnızca `data-tools`) kendi dizininde, kendi sanal ortamında (.venv) ve kendi bağımlılıklarıyla çalıştırmak. Böylece kullanıcının global Python ortamına dokunulmaz ve komut tek tip kalır: `./run-tool.sh <proje> <komut> [argümanlar...]`.

### 4.1 Çağrı Biçimi

```bash
./run-tool.sh <proje-adi> [komut ve argümanlar...]
```

- **proje-adi:** Şu an sadece `data-tools` geçerlidir. Script, `./data-tools` dizininin varlığını ve içinde `main.py` ile `requirements.txt` olup olmadığını kontrol eder.
- **komut ve argümanlar:** Olduğu gibi ilgili projenin `main.py`’sine iletilir. Örnek: `generate`, `benchmark`, `truncate`, `drop`, `counts` ve bunlara ait parametreler.

### 4.2 Script’in Adım Adım İşleyişi

| Adım | Kod / Davranış | Açıklama |
|------|----------------|----------|
| 1 | `set -euo pipefail` | Hata durumunda çıkış, tanımsız değişken kullanımında hata, pipe’ta ilk hata ile dur. |
| 2 | `PROJECT="${1:-}"` | İlk argüman proje adı olarak alınır. |
| 3 | `EXTRA_ARGS=("$@")` (2. argümandan itibaren) | Kalan tüm argümanlar (örn. `generate`, `--users`, `500`) ayrı dizide toplanır. |
| 4 | Proje adı boşsa | Kullanım mesajı yazdırılır, çıkış kodu 1 ile çıkılır. |
| 5 | `ROOT_DIR` / `PROJECT_DIR` | Script’in bulunduğu dizin kök kabul edilir; proje dizini `ROOT_DIR/<proje-adi>`. |
| 6 | Dizin kontrolü | `PROJECT_DIR` yoksa “Proje bulunamadı” hatası. |
| 7 | main.py kontrolü | Proje dizininde `main.py` yoksa hata. |
| 8 | requirements.txt kontrolü | Proje dizininde `requirements.txt` yoksa hata. |
| 9 | `cd "${PROJECT_DIR}"` | Çalışma dizini ilgili projeye geçirilir. |
| 10 | Sanal ortam | `.venv` yoksa `python -m venv .venv` ile oluşturulur. |
| 11 | Bağımlılık kurulumu | `.venv/bin/pip install -r requirements.txt` çalıştırılır. |
| 12 | Komut çalıştırma | `.venv/bin/python main.py "${EXTRA_ARGS[@]}"` ile asıl komut (ve tüm argümanlar) main.py’ye aktarılır. |

### 4.3 Örnek Çağrılar

```bash
./run-tool.sh data-tools generate --users 1000 --orders 5000 --only pg
./run-tool.sh data-tools benchmark --users 500 --products 500 --orders 2000
./run-tool.sh data-tools truncate
./run-tool.sh data-tools counts
./run-tool.sh data-tools drop
```

Bu örneklerde `data-tools` proje adı, `generate` / `benchmark` / `truncate` / `counts` / `drop` ise `main.py`’nin alt komutlarıdır.

---

## 5. data-tools — Komutlar ve Ne İşe Yaradıkları

data-tools, `main.py` üzerinden tek bir program gibi çalışır; ilk argüman **komut** (subcommand), sonrakiler o komuta özel parametrelerdir. Tüm komutlar `./run-tool.sh data-tools <komut> [parametreler]` ile çalıştırılır.

### 5.1 Komut Özet Tablosu

| Komut | Kısa açıklama | Temel işlev |
|-------|----------------|--------------|
| **generate** | Sahte veri üretir. | REST API ile belirtilen sayıda kullanıcı, ürün ve sipariş oluşturur. Hedef PG ve/veya Redis (--only pg | redis | both). |
| **benchmark** | Süre karşılaştırması yapar. | Aynı iş yükünü önce PostgreSQL uygulamasına, sonra Redis uygulamasına uygular; toplam süre ve yaklaşık istek/saniye çıktılar. |
| **truncate** | Tabloları temizler, Redis’i flush eder. | PostgreSQL’de ilgili tabloları TRUNCATE eder; Redis’te FLUSHDB. Şema durur, veri silinir. |
| **drop** | Tabloları ve Redis’i tamamen siler. | PostgreSQL’de tablolar DROP edilir; Redis FLUSHDB. Şema da kalkar (PG tarafında uygulama veya init script ile yeniden oluşturulmalı). |
| **counts** | Kayıt sayılarını listeler. | PostgreSQL ve Redis’teki kullanıcı, ürün, sipariş (ve PG’de order_items) sayılarını JSON çıktı olarak verir. |

### 5.2 generate — Parametreler (Özet)

| Parametre | Varsayılan | Açıklama |
|-----------|------------|----------|
| --pg-base-url | http://localhost:18080 | PostgreSQL uygulamasının base URL’i. |
| --redis-base-url | http://localhost:18081 | Redis uygulamasının base URL’i. |
| --users | 25000 | Oluşturulacak kullanıcı sayısı. |
| --products | 25000 | Oluşturulacak ürün sayısı. |
| --orders | 1000000 | Oluşturulacak sipariş sayısı. |
| --only | both | Hedef: pg, redis veya both. |
| --state-dir | .state | Üretilen ID’lerin ve ilerleme bilgisinin saklandığı dizin (resume için). |
| --resume / --no-resume | resume | Önceki çalıştırmadan kalan state ile devam edilir veya edilmez. |
| --seed | 42 | Faker ve rastgele sayı üreteci için tohum; tekrarlanabilir veri. |
| --timeout, --retries, --retry-wait | 60, 5, 2 | HTTP istekleri için zaman aşımı ve yeniden deneme ayarları. |

Generate, önce kullanıcıları, sonra ürünleri, sonra siparişleri oluşturur; siparişler rastgele kullanıcı ve ürün seçer, stok yetersizse stok güncellemesi yapıp tekrar dener. State sayesinde kesilse bile aynı state-dir ile devam edilebilir.

### 5.3 benchmark — Kısa Açıklama

Önce `generate` ile aynı mantıkta veri üretir; farkı, önce yalnızca PG’ye (`--only pg`), sonra yalnızca Redis’e (`--only redis`) aynı parametrelerle (users, products, orders) istek atması ve her iki çalıştırmanın süresini ölçmesidir. Sonunda toplam süre ve yaklaşık “istek/saniye” değerleri yazdırılır; makalede performans karşılaştırması için kullanılabilir.

### 5.4 truncate / drop — Veritabanı Erişimi

- **PostgreSQL:** Komut satırından `pg_host`, `pg_port`, `pg_db`, `pg_user`, `pg_pass` ile bağlanır (varsayılanlar docker-compose’daki postgres servisiyle uyumludur). truncate: `TRUNCATE TABLE order_items, orders, products, app_users RESTART IDENTITY CASCADE;` — drop: ilgili tablolar `DROP TABLE ... CASCADE` ile silinir.
- **Redis:** `redis_host`, `redis_port`, `redis_user`, `redis_pass` ile bağlanır; `FLUSHDB` çalıştırılır.

### 5.5 counts — Çıktı

Her iki sistemden sayılar alınır; JSON formatında ekrana basılır. Örnek: `{"postgresql": {"users": 1000, "products": 1000, "orders": 5000, "order_items": 12000}, "redis": {"users": 1000, "products": 1000, "orders": 5000, "order_items": "N/A (embedded in orders)"}}`. Redis’te order_items ayrı sayılmaz; sipariş içinde gömülü tutulduğu için “N/A” benzeri ifade kullanılabilir.

---

## 6. Uygulama Sunucularının Çalıştırılması (Spring Boot)

Docker’daki servisler veritabanı ve izleme içindir; REST API’yi sunan uygulamalar makinede ayrı çalıştırılır.

| Uygulama | Dizin | Port | Bağlandığı veritabanı |
|----------|--------|------|------------------------|
| PostgreSQL uygulaması | spring-boot-postgresql | 18080 | localhost:15432, appdb |
| Redis uygulaması | spring-boot-redis | 18081 | localhost:16379 (Redis) |

**Çalıştırma (örnek):**

```bash
# 1) Docker servislerinin ayakta olduğundan emin olun
docker compose up -d

# 2) PostgreSQL uygulaması (bir terminalde)
cd spring-boot-postgresql && ./mvnw spring-boot:run

# 3) Redis uygulaması (başka bir terminalde)
cd spring-boot-redis && ./mvnw spring-boot:run
```

Her iki uygulama da sağlık kontrolü için `/actuator/health` ve metrik için `/actuator/prometheus` sunar. Prometheus, bu iki uygulamayı `job` etiketiyle (spring-boot-postgresql, spring-boot-redis) scrape eder.

---

## 7. API İstekleri: api.http ve requests.http

- **api.http:** Proje kökünde; CRUD akışını sırayla (User → Product → Order), toplu (bulk) örnekleri ve transaction test endpoint’lerini tek dosyada toplar. Değişkenlerle `@pg` ve `@redis` seçilebilir; id’ler manuel veya önceki yanıttan alınarak yazılır. Saniyede çok yüksek istek için `scripts/load-test.py` kullanılması dosyada belirtilir.
- **requests.http:** Benzer isteklerin PG ve Redis için ayrı bloklar halinde durduğu alternatif dosya.

Bu dosyalar, REST Client (VS Code eklentisi) veya benzeri bir HTTP istemcisi ile çalıştırılır; sunucuların (18080, 18081) ayakta olması gerekir.

---

## 8. Yük Testi: scripts/load-test.py

Bulk istek veya saniyede sabit istek sayısı (RPS) ile yük üretmek için kullanılır. Bağımlılık: `requests` (pip ile kurulur).

| Mod | Örnek komut | Açıklama |
|-----|-------------|----------|
| Bulk ürün | `python scripts/load-test.py --base-url http://localhost:18081 --bulk-products 500` | Belirtilen sayıda ürünü ardışık POST /api/products ile oluşturur. |
| Sabit RPS | `python scripts/load-test.py --base-url http://localhost:18080 --rps 200 --duration 30` | Önce 1 user + 1 product oluşturur; sonra belirtilen süre boyunca hedef RPS’e yakın istek atar (GET product ve POST order karışımı). |

Parametreler: `--base-url` (18080 PG, 18081 Redis), `--rps`, `--duration`, `--threads`, `--timeout`. Çıktıda başarılı/başarısız sayısı ve isteğe bağlı gecikme istatistiği (ör. p95) yer alır.

---

## 9. İzleme: Prometheus ve Grafana

- **Prometheus (port 19090):** `observability/prometheus/prometheus.yml` ile yapılandırılır. İki scrape hedefi vardır:
  - `job_name: "spring-boot-postgresql"` → `host.docker.internal:18080`
  - `job_name: "spring-boot-redis"` → `host.docker.internal:18081`  
  Metrik yolu: `/actuator/prometheus`. Böylece her iki uygulamanın HTTP, JVM vb. metrikleri `job` etiketiyle ayrıştırılır.

- **Grafana (port 13000):** Prometheus’u veri kaynağı olarak kullanır. Provisioning ile iki dashboard yüklenir:
  - **Spring Boot PostgreSQL Metrics:** Tek uygulama (PG) metrikleri.
  - **PG vs Redis — Karşılaştırma:** Her iki uygulamanın RPS, gecikme (p95/p99), hata oranları, endpoint bazlı metrikler ve JVM/CPU karşılaştırması. Tüm panellerde `job` etiketiyle PG ve Redis ayrı gösterilir.

Giriş (varsayılan): http://localhost:13000 — kullanıcı: admin, şifre: admin.

---

## 10. Dokümanlar: GOZLEM-PLANI.md ve TRX-GOZLEM.md

- **GOZLEM-PLANI.md:** Makalede kullanılmak üzere gözlem planı: ortamı çalıştırma, veri üretme, benchmark, Redis’i inceleme, performans/sorgulama/transaction/kalıcılık/operasyon başlıklarında neye bakılacağı ve avantaj/dezavantaj taslak tablosu. İzlenebilecek sıra adım adım yazılıdır.
- **TRX-GOZLEM.md:** Transaction test endpoint’leri (stock-adjust, order-rollback, deadlock) çalıştırılırken ne gözlemleneceği: rollback davranışı, eşzamanlı güncelleme, deadlock farkı (PG vs Redis) kısaca açıklanır.

---

## 11. Örnek Çalıştırma Akışı (Baştan Sona)

Aşağıdaki sıra, projeyi denemek ve makale için veri/ölçüm toplamak için kullanılabilir.

1. **Docker servislerini başlatın:**  
   `docker compose up -d`
2. **PostgreSQL uygulamasını başlatın:**  
   `cd spring-boot-postgresql && ./mvnw spring-boot:run`
3. **Redis uygulamasını başlatın (ayrı terminal):**  
   `cd spring-boot-redis && ./mvnw spring-boot:run`
4. **İsteğe bağlı — Veritabanlarını sıfırlayın:**  
   `./run-tool.sh data-tools truncate` (veya tam silmek için `drop`)
5. **Veri üretin (örnek — sadece PG):**  
   `./run-tool.sh data-tools generate --users 500 --products 500 --orders 2000 --only pg`
6. **Aynı iş yükünü Redis’e uygulayın:**  
   `./run-tool.sh data-tools generate --users 500 --products 500 --orders 2000 --only redis`
7. **Kayıt sayılarını kontrol edin:**  
   `./run-tool.sh data-tools counts`
8. **Süre karşılaştırması (aynı iş yükü PG ve Redis):**  
   `./run-tool.sh data-tools benchmark --users 500 --products 500 --orders 2000`
9. **API isteklerini deneyin:**  
   `api.http` veya `requests.http` dosyasındaki istekleri REST Client ile gönderin; gerekli id’leri create yanıtlarından alın.
10. **Transaction senaryolarını çalıştırın:**  
    `api.http` içindeki “TRANSACTION TESTLERİ” bölümündeki istekleri hem PG hem Redis base URL ile deneyin; gözlemleri TRX-GOZLEM.md’ye göre not alın.
11. **Grafana’da metrikleri inceleyin:**  
    http://localhost:13000 → “PG vs Redis — Karşılaştırma” dashboard’unu açın; RPS ve yük testi sırasında grafikleri gözlemleyin.
12. **Redis verisini inceleyin:**  
    http://localhost:15540 (Redis Insight) veya `docker exec -it redis-as-primary-database-redis redis-cli -a redispass` ile key’lere bakın.

Bu rehber, projenin bileşenlerinin ne işe yaradığını, run-tool.sh’ın nasıl çalıştığını ve projeyi nasıl adım adım çalıştıracağınızı tek dokümanda toplar; onay ve denetim için uygundur.
