import argparse
import random
import sys
from typing import List

import requests
from faker import Faker


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Generate fake data for both APIs.")
    parser.add_argument("--pg-base-url", default="http://localhost:8080")
    parser.add_argument("--redis-base-url", default="http://localhost:8081")
    parser.add_argument("--users", type=int, default=5000)
    parser.add_argument("--products", type=int, default=10000)
    parser.add_argument("--orders", type=int, default=20000)
    parser.add_argument("--min-items", type=int, default=1)
    parser.add_argument("--max-items", type=int, default=3)
    parser.add_argument("--seed", type=int, default=42)
    return parser.parse_args()


def post_json(session: requests.Session, url: str, payload: dict) -> dict:
    resp = session.post(url, json=payload, timeout=10)
    if not resp.ok:
        raise RuntimeError(f"POST {url} failed: {resp.status_code} {resp.text}")
    data = resp.json()
    return data.get("data", data)


def generate_users(session: requests.Session, base_url: str, faker: Faker, count: int) -> List[str]:
    user_ids = []
    for _ in range(count):
        payload = {
            "name": faker.name(),
            "email": faker.unique.email(),
        }
        data = post_json(session, f"{base_url}/api/users", payload)
        user_ids.append(str(data["id"]))
    return user_ids


def generate_products(session: requests.Session, base_url: str, faker: Faker, count: int) -> List[str]:
    product_ids = []
    for _ in range(count):
        payload = {
            "name": faker.word().title(),
            "price": round(random.uniform(5, 500), 2),
            "stockQuantity": random.randint(50, 500),
        }
        data = post_json(session, f"{base_url}/api/products", payload)
        product_ids.append(str(data["id"]))
    return product_ids


def generate_orders(
    session: requests.Session,
    base_url: str,
    user_ids: List[str],
    product_ids: List[str],
    count: int,
    min_items: int,
    max_items: int,
) -> None:
    for _ in range(count):
        items_count = random.randint(min_items, max_items)
        items = []
        for product_id in random.sample(product_ids, k=min(items_count, len(product_ids))):
            items.append(
                {
                    "productId": product_id,
                    "quantity": random.randint(1, 5),
                }
            )
        payload = {"userId": random.choice(user_ids), "items": items}
        post_json(session, f"{base_url}/api/orders", payload)


def generate_all(base_url: str, args: argparse.Namespace, label: str) -> None:
    faker = Faker()
    Faker.seed(args.seed)
    random.seed(args.seed)

    session = requests.Session()
    session.headers.update({"Content-Type": "application/json"})

    print(f"[{label}] users: {args.users}, products: {args.products}, orders: {args.orders}")
    user_ids = generate_users(session, base_url, faker, args.users)
    product_ids = generate_products(session, base_url, faker, args.products)
    generate_orders(session, base_url, user_ids, product_ids, args.orders, args.min_items, args.max_items)
    print(f"[{label}] done")


def main() -> None:
    args = parse_args()
    try:
        generate_all(args.pg_base_url, args, "PostgreSQL")
        generate_all(args.redis_base_url, args, "Redis")
    except Exception as exc:
        print(f"Error: {exc}")
        sys.exit(1)


if __name__ == "__main__":
    main()
