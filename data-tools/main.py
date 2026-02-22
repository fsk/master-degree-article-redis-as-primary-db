import argparse

from counts import run_counts
from drop import run_drop
from generate import run_generate
from truncate import run_truncate


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Data tools")
    subparsers = parser.add_subparsers(dest="command", required=True)

    generate_parser = subparsers.add_parser("generate", help="Generate fake data")
    generate_parser.add_argument("--pg-base-url", default="http://localhost:18080")
    generate_parser.add_argument("--redis-base-url", default="http://localhost:18081")
    generate_parser.add_argument("--users", type=int, default=25000)
    generate_parser.add_argument("--products", type=int, default=25000)
    generate_parser.add_argument("--orders", type=int, default=1000000)
    generate_parser.add_argument("--min-items", type=int, default=1)
    generate_parser.add_argument("--max-items", type=int, default=3)
    generate_parser.add_argument("--log-every", type=int, default=1000)
    generate_parser.add_argument("--timeout", type=int, default=60)
    generate_parser.add_argument("--retries", type=int, default=5)
    generate_parser.add_argument("--retry-wait", type=int, default=2)
    generate_parser.add_argument("--only", choices=["pg", "redis", "both"], default="both")
    generate_parser.add_argument("--no-continue-on-error", dest="continue_on_error", action="store_false")
    generate_parser.set_defaults(continue_on_error=True)
    generate_parser.add_argument("--state-dir", default=".state")
    generate_parser.add_argument("--resume", action="store_true", default=True)
    generate_parser.add_argument("--no-resume", dest="resume", action="store_false")
    generate_parser.add_argument("--seed", type=int, default=42)
    generate_parser.add_argument("--pg-host", default="localhost")
    generate_parser.add_argument("--pg-port", type=int, default=15432)
    generate_parser.add_argument("--pg-db", default="appdb")
    generate_parser.add_argument("--pg-user", default="app")
    generate_parser.add_argument("--pg-pass", default="app")
    generate_parser.add_argument("--redis-host", default="localhost")
    generate_parser.add_argument("--redis-port", type=int, default=16379)
    generate_parser.add_argument("--redis-user", default="redisuser")
    generate_parser.add_argument("--redis-pass", default="redispass")

    truncate_parser = subparsers.add_parser("truncate", help="Truncate Postgres tables and flush Redis")
    truncate_parser.add_argument("--pg-host", default="localhost")
    truncate_parser.add_argument("--pg-port", type=int, default=15432)
    truncate_parser.add_argument("--pg-db", default="appdb")
    truncate_parser.add_argument("--pg-user", default="app")
    truncate_parser.add_argument("--pg-pass", default="app")
    truncate_parser.add_argument("--redis-host", default="localhost")
    truncate_parser.add_argument("--redis-port", type=int, default=16379)
    truncate_parser.add_argument("--redis-user", default="redisuser")
    truncate_parser.add_argument("--redis-pass", default="redispass")

    drop_parser = subparsers.add_parser("drop", help="Drop Postgres tables and flush Redis")
    drop_parser.add_argument("--pg-host", default="localhost")
    drop_parser.add_argument("--pg-port", type=int, default=15432)
    drop_parser.add_argument("--pg-db", default="appdb")
    drop_parser.add_argument("--pg-user", default="app")
    drop_parser.add_argument("--pg-pass", default="app")
    drop_parser.add_argument("--redis-host", default="localhost")
    drop_parser.add_argument("--redis-port", type=int, default=16379)
    drop_parser.add_argument("--redis-user", default="redisuser")
    drop_parser.add_argument("--redis-pass", default="redispass")

    counts_parser = subparsers.add_parser("counts", help="Show record counts")
    counts_parser.add_argument("--pg-host", default="localhost")
    counts_parser.add_argument("--pg-port", type=int, default=15432)
    counts_parser.add_argument("--pg-db", default="appdb")
    counts_parser.add_argument("--pg-user", default="app")
    counts_parser.add_argument("--pg-pass", default="app")
    counts_parser.add_argument("--redis-host", default="localhost")
    counts_parser.add_argument("--redis-port", type=int, default=16379)
    counts_parser.add_argument("--redis-user", default="redisuser")
    counts_parser.add_argument("--redis-pass", default="redispass")

    return parser.parse_args()


def main() -> None:
    args = parse_args()
    if args.command == "generate":
        run_generate(args)
    elif args.command == "truncate":
        run_truncate(args)
    elif args.command == "drop":
        run_drop(args)
    elif args.command == "counts":
        run_counts(args)


if __name__ == "__main__":
    main()
