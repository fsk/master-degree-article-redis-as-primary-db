# Data Tools

Tek bir Python projesi icinde:
- Fake data generator
- DB truncate
- DB drop
- DB counts

## Kurulum
```bash
python -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## Kullanim
```bash
python main.py generate
python main.py truncate
python main.py drop
python main.py counts
```

## Varsayilanlar
- PostgreSQL: localhost:15432, db=appdb, user=app, pass=app
- Redis: localhost:16379, user=redisuser, pass=redispass
- API: PG http://localhost:18080, Redis http://localhost:18081
- Fake data: 25.000 user, 25.000 product, 1.000.000 order

## Ornek
```bash
python main.py generate --timeout 120 --retries 10 --retry-wait 3 --resume
python main.py generate --only redis
python main.py counts
```

Resume davranisi:
- `--resume` ile önce DB'den mevcut ID listeleri okunur ve sadece eksik olanlar eklenir.
