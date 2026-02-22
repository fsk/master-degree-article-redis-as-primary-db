import json

import psycopg
import redis


def count_postgres(args) -> dict:
    dsn = (
        f"host={args.pg_host} port={args.pg_port} dbname={args.pg_db} "
        f"user={args.pg_user} password={args.pg_pass}"
    )
    with psycopg.connect(dsn) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT COUNT(*) FROM app_users;")
            users = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM products;")
            products = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM orders;")
            orders = cur.fetchone()[0]
            cur.execute("SELECT COUNT(*) FROM order_items;")
            order_items = cur.fetchone()[0]
    return {
        "users": users,
        "products": products,
        "orders": orders,
        "order_items": order_items,
    }


def count_keys(client: redis.Redis, pattern: str) -> int:
    return sum(1 for _ in client.scan_iter(match=pattern, count=1000))


def count_redis(args) -> dict:
    client = redis.Redis(
        host=args.redis_host,
        port=args.redis_port,
        username=args.redis_user,
        password=args.redis_pass,
        decode_responses=True,
    )
    return {
        "users": count_keys(client, "users:*"),
        "products": count_keys(client, "products:*"),
        "orders": count_keys(client, "orders:*"),
        "order_items": "N/A (embedded in orders)",
    }


def run_counts(args) -> None:
    result = {
        "postgresql": count_postgres(args),
        "redis": count_redis(args),
    }
    print(json.dumps(result, indent=2))
