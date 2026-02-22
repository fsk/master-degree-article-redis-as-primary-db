import psycopg
import redis


def run_drop(args) -> None:
    dsn = (
        f"host={args.pg_host} port={args.pg_port} dbname={args.pg_db} "
        f"user={args.pg_user} password={args.pg_pass}"
    )
    with psycopg.connect(dsn) as conn:
        with conn.cursor() as cur:
            cur.execute("DROP TABLE IF EXISTS order_items CASCADE;")
            cur.execute("DROP TABLE IF EXISTS orders CASCADE;")
            cur.execute("DROP TABLE IF EXISTS products CASCADE;")
            cur.execute("DROP TABLE IF EXISTS app_users CASCADE;")
        conn.commit()

    client = redis.Redis(
        host=args.redis_host,
        port=args.redis_port,
        username=args.redis_user,
        password=args.redis_pass,
        decode_responses=True,
    )
    client.flushdb()
    print("Drop completed.")
