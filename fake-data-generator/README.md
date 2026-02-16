# Fake Data Generator

Bu proje hem PostgreSQL hem Redis servislerine test verisi üretir.

## Kurulum
```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Çalıştırma
```bash
python main.py
```

Varsayılanlar:
- PostgreSQL API: `http://localhost:18080`
- Redis API: `http://localhost:18081`
- 5.000 user, 10.000 product, 20.000 order

Örnek özelleştirme:
```bash
python main.py --users 200 --products 500 --orders 2000
```
