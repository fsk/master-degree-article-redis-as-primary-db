#!/usr/bin/env python3
"""
Bulk istek ve saniyede yüksek sayıda istek (RPS) için basit yük testi.
Kullanım:
  python scripts/load-test.py --base-url http://localhost:18080 --rps 200 --duration 30
  python scripts/load-test.py --base-url http://localhost:18081 --bulk-products 500
"""
from __future__ import annotations

import argparse
import random
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests


def parse_args() -> argparse.Namespace:
    p = argparse.ArgumentParser(description="API yük testi: bulk veya sabit RPS")
    p.add_argument("--base-url", default="http://localhost:18080", help="PG: 18080, Redis: 18081")
    p.add_argument("--rps", type=int, default=0, help="Hedef istek/saniye (0 = sadece bulk)")
    p.add_argument("--duration", type=int, default=60, help="RPS modunda süre (saniye)")
    p.add_argument("--bulk-products", type=int, default=0, help="Ardışık ürün oluşturma sayısı")
    p.add_argument("--bulk-orders", type=int, default=0, help="Sipariş sayısı (en az 1 user+product gerekir)")
    p.add_argument("--threads", type=int, default=10, help="RPS modunda eşzamanlı thread sayısı")
    p.add_argument("--timeout", type=int, default=30)
    return p.parse_args()


def create_product(session: requests.Session, base_url: str, i: int, timeout: int) -> tuple[bool, float]:
    payload = {"name": f"Load Product {i}", "price": round(random.uniform(5, 200), 2), "stockQuantity": 500}
    start = time.perf_counter()
    try:
        r = session.post(f"{base_url}/api/products", json=payload, timeout=timeout)
        elapsed = time.perf_counter() - start
        return r.ok, elapsed
    except Exception:
        return False, time.perf_counter() - start


def get_product(session: requests.Session, base_url: str, product_id: str, timeout: int) -> tuple[bool, float]:
    start = time.perf_counter()
    try:
        r = session.get(f"{base_url}/api/products/{product_id}", timeout=timeout)
        return r.ok, time.perf_counter() - start
    except Exception:
        return False, time.perf_counter() - start


def run_bulk_products(base_url: str, count: int, timeout: int) -> None:
    session = requests.Session()
    session.headers["Content-Type"] = "application/json"
    ok, fail, times = 0, 0, []
    print(f"Bulk: {count} ürün oluşturuluyor -> {base_url}")
    start = time.perf_counter()
    for i in range(1, count + 1):
        success, elapsed = create_product(session, base_url, i, timeout)
        if success:
            ok += 1
            times.append(elapsed)
        else:
            fail += 1
        if i % 100 == 0:
            print(f"  {i}/{count}")
    total = time.perf_counter() - start
    print(f"  Tamamlandı: {ok} ok, {fail} fail, toplam {total:.1f}s, ortalama {total/count*1000:.0f} ms/istek")


def run_rps(base_url: str, target_rps: int, duration_sec: int, threads: int, timeout: int) -> None:
    session = requests.Session()
    session.headers["Content-Type"] = "application/json"
    r = session.post(f"{base_url}/api/users", json={"name": "Load User", "email": "load@test.com"}, timeout=timeout)
    if not r.ok:
        print("User oluşturulamadı:", r.status_code, r.text[:200])
        sys.exit(1)
    user_id = r.json().get("data", {}).get("id")
    r = session.post(
        f"{base_url}/api/products",
        json={"name": "Load Product", "price": 100, "stockQuantity": 10000},
        timeout=timeout,
    )
    if not r.ok:
        print("Product oluşturulamadı:", r.status_code, r.text[:200])
        sys.exit(1)
    product_id = r.json().get("data", {}).get("id")

    ok, fail, latencies = 0, 0, []
    print(f"RPS: hedef {target_rps}/s, süre {duration_sec}s, thread {threads} -> {base_url}")

    def one_request() -> tuple[bool, float]:
        if random.random() < 0.7:
            return get_product(session, base_url, product_id, timeout)
        payload = {"userId": user_id, "items": [{"productId": product_id, "quantity": 1}]}
        start = time.perf_counter()
        try:
            resp = session.post(f"{base_url}/api/orders", json=payload, timeout=timeout)
            return resp.ok, time.perf_counter() - start
        except Exception:
            return False, time.perf_counter() - start

    with ThreadPoolExecutor(max_workers=threads) as ex:
        for sec in range(duration_sec):
            t0 = time.perf_counter()
            futures = [ex.submit(one_request) for _ in range(target_rps)]
            for f in as_completed(futures):
                try:
                    success, lat = f.result()
                    if success:
                        ok += 1
                        latencies.append(lat)
                    else:
                        fail += 1
                except Exception:
                    fail += 1
            remain = 1.0 - (time.perf_counter() - t0)
            if remain > 0:
                time.sleep(remain)
            if (sec + 1) % 10 == 0:
                print(f"  {sec + 1}s geçti, toplam ok: {ok}, fail: {fail}")

    total = ok + fail
    if latencies:
        latencies.sort()
        p95 = latencies[int(len(latencies) * 0.95)] if len(latencies) > 20 else latencies[-1]
        print(f"  Toplam istek: {total}, ok: {ok}, fail: {fail}, p95 gecikme: {p95*1000:.0f} ms")
    else:
        print(f"  ok: {ok}, fail: {fail}")


def main() -> None:
    args = parse_args()
    if args.bulk_products > 0:
        run_bulk_products(args.base_url, args.bulk_products, args.timeout)
    elif args.rps > 0:
        run_rps(args.base_url, args.rps, args.duration, args.threads, args.timeout)
    else:
        print("--rps N veya --bulk-products N belirtin. Örnek:")
        print("  python scripts/load-test.py --base-url http://localhost:18081 --rps 200 --duration 30")
        print("  python scripts/load-test.py --base-url http://localhost:18080 --bulk-products 500")
        sys.exit(1)


if __name__ == "__main__":
    main()
