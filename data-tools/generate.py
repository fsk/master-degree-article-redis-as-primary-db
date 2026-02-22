import os
import random
import sys
import time
from typing import Dict, List, Tuple

import psycopg
import redis
import requests
from faker import Faker


def check_health(base_url: str, label: str) -> None:
    url = f"{base_url}/actuator/health"
    try:
        resp = requests.get(url, timeout=10)
        if not resp.ok:
            raise RuntimeError(f"{label} health check failed: {resp.status_code} {resp.text}")
        payload = resp.json()
        if payload.get("status") != "UP":
            raise RuntimeError(f"{label} health check not UP: {payload}")
    except Exception as exc:
        raise RuntimeError(f"{label} health check failed: {exc}") from exc


def get_json(session: requests.Session, url: str, timeout: int) -> requests.Response:
    return session.get(url, timeout=timeout)


def post_json(
    session: requests.Session,
    url: str,
    payload: dict,
    timeout: int,
    retries: int,
    retry_wait: int,
) -> dict:
    attempt = 0
    while True:
        attempt += 1
        try:
            resp = session.post(url, json=payload, timeout=timeout)
            if not resp.ok:
                raise RuntimeError(f"POST {url} failed: {resp.status_code} {resp.text}")
            data = resp.json()
            return data.get("data", data)
        except Exception as exc:
            if attempt >= retries:
                raise RuntimeError(f"POST {url} failed after {retries} attempts: {exc}") from exc
            time.sleep(retry_wait)


def load_ids(path: str) -> List[str]:
    if not os.path.exists(path):
        return []
    with open(path, "r", encoding="utf-8") as file:
        return [line.strip() for line in file if line.strip()]


def append_id(path: str, value: str) -> None:
    with open(path, "a", encoding="utf-8") as file:
        file.write(f"{value}\n")


def resolve_state_paths(state_dir: str, label: str) -> Tuple[str, str]:
    os.makedirs(state_dir, exist_ok=True)
    users_path = os.path.join(state_dir, f"{label.lower()}_users.txt")
    products_path = os.path.join(state_dir, f"{label.lower()}_products.txt")
    return users_path, products_path


def clear_state(state_dir: str, label: str) -> None:
    users_path, products_path = resolve_state_paths(state_dir, label)
    orders_path = os.path.join(state_dir, f"{label.lower()}_orders.count")
    for path in (users_path, products_path, orders_path):
        if os.path.exists(path):
            os.remove(path)


def validate_state(
    session: requests.Session,
    base_url: str,
    state_dir: str,
    label: str,
    timeout: int,
) -> bool:
    _, products_path = resolve_state_paths(state_dir, label)
    product_ids = load_ids(products_path)
    if not product_ids:
        return True

    test_id = product_ids[0]
    resp = get_json(session, f"{base_url}/api/products/{test_id}", timeout=timeout)
    if resp.status_code == 404:
        clear_state(state_dir, label)
        return False
    return True


def update_stock(
    session: requests.Session,
    base_url: str,
    product_id: str,
    delta: int,
    timeout: int,
    retries: int,
    retry_wait: int,
) -> None:
    post_json(
        session,
        f"{base_url}/api/products/{product_id}/stock",
        {"delta": delta},
        timeout,
        retries,
        retry_wait,
    )


def rebuild_state_from_postgres(args, state_dir: str, label: str) -> Tuple[List[str], List[str], int]:
    users_path, products_path = resolve_state_paths(state_dir, label)
    orders_path = os.path.join(state_dir, f"{label.lower()}_orders.count")
    dsn = (
        f"host={args.pg_host} port={args.pg_port} dbname={args.pg_db} "
        f"user={args.pg_user} password={args.pg_pass}"
    )
    with psycopg.connect(dsn) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT id::text FROM app_users;")
            users = [row[0] for row in cur.fetchall()]
            cur.execute("SELECT id::text FROM products;")
            products = [row[0] for row in cur.fetchall()]
            cur.execute("SELECT COUNT(*) FROM orders;")
            orders = cur.fetchone()[0]

    with open(users_path, "w", encoding="utf-8") as file:
        file.write("\n".join(users) + ("\n" if users else ""))
    with open(products_path, "w", encoding="utf-8") as file:
        file.write("\n".join(products) + ("\n" if products else ""))
    with open(orders_path, "w", encoding="utf-8") as file:
        file.write(str(orders))

    return users, products, orders


def rebuild_state_from_redis(args, state_dir: str, label: str) -> Tuple[List[str], List[str], int]:
    users_path, products_path = resolve_state_paths(state_dir, label)
    orders_path = os.path.join(state_dir, f"{label.lower()}_orders.count")
    client = redis.Redis(
        host=args.redis_host,
        port=args.redis_port,
        username=args.redis_user,
        password=args.redis_pass,
        decode_responses=True,
    )
    users = [key.split(":", 1)[1] for key in client.scan_iter(match="users:*", count=1000)]
    products = [key.split(":", 1)[1] for key in client.scan_iter(match="products:*", count=1000)]
    orders = sum(1 for _ in client.scan_iter(match="orders:*", count=1000))

    with open(users_path, "w", encoding="utf-8") as file:
        file.write("\n".join(users) + ("\n" if users else ""))
    with open(products_path, "w", encoding="utf-8") as file:
        file.write("\n".join(products) + ("\n" if products else ""))
    with open(orders_path, "w", encoding="utf-8") as file:
        file.write(str(orders))

    return users, products, orders


def generate_users(
    session: requests.Session,
    base_url: str,
    faker: Faker,
    count: int,
    log_every: int,
    label: str,
    timeout: int,
    retries: int,
    retry_wait: int,
    state_dir: str,
    resume: bool,
) -> List[str]:
    users_path, _ = resolve_state_paths(state_dir, label)
    user_ids = load_ids(users_path) if resume else []
    if len(user_ids) >= count:
        print(f"[{label}] users already at {len(user_ids)} >= {count}, skipping")
        return user_ids
    start_index = len(user_ids) + 1
    for idx in range(start_index, count + 1):
        payload = {
            "name": faker.name(),
            "email": f"user{idx}@example.com",
        }
        data = post_json(session, f"{base_url}/api/users", payload, timeout, retries, retry_wait)
        user_id = str(data["id"])
        user_ids.append(user_id)
        append_id(users_path, user_id)
        if log_every > 0 and idx % log_every == 0:
            print(f"[{label}] users created: {idx}/{count}")
    return user_ids


def generate_products(
    session: requests.Session,
    base_url: str,
    faker: Faker,
    count: int,
    log_every: int,
    label: str,
    timeout: int,
    retries: int,
    retry_wait: int,
    state_dir: str,
    resume: bool,
) -> List[str]:
    _, products_path = resolve_state_paths(state_dir, label)
    product_ids = load_ids(products_path) if resume else []
    product_stock: Dict[str, int] = {pid: 0 for pid in product_ids}
    if len(product_ids) >= count:
        print(f"[{label}] products already at {len(product_ids)} >= {count}, skipping")
        return product_ids, product_stock
    start_index = len(product_ids) + 1
    for idx in range(start_index, count + 1):
        initial_stock = random.randint(500, 2000)
        payload = {
            "name": f"Product {idx}",
            "price": round(random.uniform(5, 500), 2),
            "stockQuantity": initial_stock,
        }
        data = post_json(session, f"{base_url}/api/products", payload, timeout, retries, retry_wait)
        product_id = str(data["id"])
        product_ids.append(product_id)
        append_id(products_path, product_id)
        product_stock[product_id] = initial_stock
        if log_every > 0 and idx % log_every == 0:
            print(f"[{label}] products created: {idx}/{count}")
    return product_ids, product_stock


def generate_orders(
    session: requests.Session,
    base_url: str,
    user_ids: List[str],
    product_ids: List[str],
    product_stock: Dict[str, int],
    count: int,
    min_items: int,
    max_items: int,
    log_every: int,
    label: str,
    timeout: int,
    retries: int,
    retry_wait: int,
    state_dir: str,
    resume: bool,
) -> None:
    state_path = os.path.join(state_dir, f"{label.lower()}_orders.count")
    start_index = 1
    if resume and os.path.exists(state_path):
        with open(state_path, "r", encoding="utf-8") as file:
            value = file.read().strip()
            if value.isdigit():
                start_index = int(value) + 1
    if start_index > count:
        print(f"[{label}] orders already at {start_index - 1} >= {count}, skipping")
        return

    for idx in range(start_index, count + 1):
        items_count = random.randint(min_items, max_items)
        items = []
        for product_id in random.sample(product_ids, k=min(items_count, len(product_ids))):
            available = product_stock.get(product_id, 0)
            if available <= 0:
                replenish = random.randint(500, 1500)
                update_stock(session, base_url, product_id, replenish, timeout, retries, retry_wait)
                available += replenish
            qty = random.randint(1, 5)
            if available < qty:
                replenish = max(qty * 20, 100)
                update_stock(session, base_url, product_id, replenish, timeout, retries, retry_wait)
                available += replenish
            items.append(
                {
                    "productId": product_id,
                    "quantity": qty,
                }
            )
            product_stock[product_id] = available - qty
        payload = {"userId": random.choice(user_ids), "items": items}
        try:
            post_json(session, f"{base_url}/api/orders", payload, timeout, retries, retry_wait)
        except Exception as exc:
            message = str(exc)
            if "USER_NOT_FOUND" in message:
                raise RuntimeError(
                    f"{label}: user not found. Resume state might be stale. "
                    f"Clear {state_dir}/{label.lower()}_users.txt"
                ) from exc
            if "INSUFFICIENT_STOCK" in message:
                for item in items:
                    update_stock(
                        session,
                        base_url,
                        item["productId"],
                        1000,
                        timeout,
                        retries,
                        retry_wait,
                    )
                post_json(session, f"{base_url}/api/orders", payload, timeout, retries, retry_wait)
            else:
                raise
        with open(state_path, "w", encoding="utf-8") as file:
            file.write(str(idx))
        if log_every > 0 and idx % log_every == 0:
            print(f"[{label}] orders created: {idx}/{count}")


def generate_all(base_url: str, args, label: str) -> None:
    faker = Faker()
    Faker.seed(args.seed)
    random.seed(args.seed)

    session = requests.Session()
    session.headers.update({"Content-Type": "application/json"})

    print(f"[{label}] users: {args.users}, products: {args.products}, orders: {args.orders}")

    resume = args.resume
    if resume:
        if label == "PostgreSQL":
            rebuild_state_from_postgres(args, args.state_dir, label)
        else:
            rebuild_state_from_redis(args, args.state_dir, label)
    user_ids = generate_users(
        session,
        base_url,
        faker,
        args.users,
        args.log_every,
        label,
        args.timeout,
        args.retries,
        args.retry_wait,
        args.state_dir,
        resume,
    )
    product_ids, product_stock = generate_products(
        session,
        base_url,
        faker,
        args.products,
        args.log_every,
        label,
        args.timeout,
        args.retries,
        args.retry_wait,
        args.state_dir,
        resume,
    )
    generate_orders(
        session,
        base_url,
        user_ids,
        product_ids,
        product_stock,
        args.orders,
        args.min_items,
        args.max_items,
        args.log_every,
        label,
        args.timeout,
        args.retries,
        args.retry_wait,
        args.state_dir,
        resume,
    )
    print(f"[{label}] done")


def run_generate(args) -> int:
    if args.only in ("pg", "both"):
        check_health(args.pg_base_url, "PostgreSQL")
    if args.only in ("redis", "both"):
        check_health(args.redis_base_url, "Redis")

    exit_code = 0
    if args.only in ("pg", "both"):
        try:
            generate_all(args.pg_base_url, args, "PostgreSQL")
        except Exception as exc:
            print(f"[PostgreSQL] Error: {exc}")
            exit_code = 1
            if not args.continue_on_error:
                return exit_code

    if args.only in ("redis", "both"):
        try:
            generate_all(args.redis_base_url, args, "Redis")
        except Exception as exc:
            print(f"[Redis] Error: {exc}")
            exit_code = 1

    return exit_code


def run_benchmark(args) -> int:
    """Aynı iş yükünü önce PostgreSQL, sonra Redis'e uygulayıp süre karşılaştırması yapar."""
    import copy

    total_ops = args.users + args.products + args.orders
    print(f"Benchmark: users={args.users}, products={args.products}, orders={args.orders}")
    print(f"Yaklaşık toplam istek: {total_ops}")
    print()

    args_pg = copy.copy(args)
    args_pg.only = "pg"
    args_redis = copy.copy(args)
    args_redis.only = "redis"

    t0 = time.perf_counter()
    code_pg = run_generate(args_pg)
    t_pg = time.perf_counter() - t0

    print()
    t0 = time.perf_counter()
    code_redis = run_generate(args_redis)
    t_redis = time.perf_counter() - t0

    print()
    print("--- Süre karşılaştırması ---")
    print(f"  PostgreSQL: {t_pg:.1f} s  ({total_ops / t_pg:.0f} istek/s)")
    print(f"  Redis:     {t_redis:.1f} s  ({total_ops / t_redis:.0f} istek/s)")
    if t_pg > 0:
        print(f"  Oran (Redis/PG): {t_redis / t_pg:.2f}x")
    print()
    return code_pg if code_pg != 0 else code_redis
