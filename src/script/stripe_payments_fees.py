#!/usr/bin/env python3
"""
Extrait tous les paiements d'un compte Stripe et produit un tableau
avec le montant brut, les frais Stripe et le net reçu (utile pour une asso).

Usage:
  export STRIPE_API_KEY=sk_live_...   # ou sk_test_...
  pip install stripe
  python stripe_payments_fees.py
  python stripe_payments_fees.py --from 2025-01-01 --to 2025-12-31
  python stripe_payments_fees.py --csv paiements_stripe.csv
"""

from __future__ import annotations

import argparse
import csv
import os
import sys
from datetime import datetime, timezone
from typing import Any, Iterator

try:
    import stripe
except ImportError:
    print("Installez la lib: pip install stripe", file=sys.stderr)
    sys.exit(1)


def parse_date(value: str) -> int:
    """Parse YYYY-MM-DD en timestamp Unix (UTC, début de journée)."""
    dt = datetime.strptime(value, "%Y-%m-%d").replace(tzinfo=timezone.utc)
    return int(dt.timestamp())


def end_of_day(value: str) -> int:
    """Parse YYYY-MM-DD en timestamp Unix (UTC, fin de journée)."""
    dt = datetime.strptime(value, "%Y-%m-%d").replace(
        hour=23, minute=59, second=59, tzinfo=timezone.utc
    )
    return int(dt.timestamp())


def format_amount(cents: int | None, currency: str = "") -> str:
    if cents is None:
        return ""
    # Stripe stocke en centimes (sauf devises zero-decimal rares)
    # Format FR : virgule décimale, sans symbole de devise
    amount = cents / 100.0
    return f"{amount:.2f}".replace(".", ",")


def format_date(ts: int | None) -> str:
    if not ts:
        return ""
    return datetime.fromtimestamp(ts, tz=timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")


def iter_balance_transactions(
    created: dict[str, int] | None = None,
) -> Iterator[Any]:
    """Parcourt toutes les balance transactions (paiements + frais)."""
    params: dict[str, Any] = {"limit": 100, "expand": ["data.source"]}
    if created:
        params["created"] = created

    for txn in stripe.BalanceTransaction.list(**params).auto_paging_iter():
        yield txn


def fee_attr(obj: Any, key: str, default: Any = None) -> Any:
    """Lit un champ sur un StripeObject ou un dict."""
    if obj is None:
        return default
    if isinstance(obj, dict):
        return obj.get(key, default)
    return getattr(obj, key, default)


def fee_breakdown(txn: Any) -> tuple[int, str]:
    """Retourne (total frais en centimes, détail des frais)."""
    details = getattr(txn, "fee_details", None) or []
    parts = []
    for d in details:
        desc = fee_attr(d, "description") or fee_attr(d, "type") or "fee"
        amount = fee_attr(d, "amount", 0) or 0
        currency = fee_attr(d, "currency") or txn.currency
        parts.append(f"{desc}: {format_amount(amount, currency)}")
    return int(txn.fee or 0), " | ".join(parts)


def source_info(txn: Any) -> dict[str, str]:
    """Infos utiles depuis la source (Charge / PaymentIntent / Refund...)."""
    src = getattr(txn, "source", None)
    info = {
        "source_id": "",
        "description": getattr(txn, "description", None) or "",
        "customer_email": "",
        "payment_intent": "",
        "charge_id": "",
        "status": "",
    }
    if src is None:
        info["source_id"] = str(getattr(txn, "source", "") or "")
        return info

    if isinstance(src, str):
        info["source_id"] = src
        return info

    info["source_id"] = getattr(src, "id", "") or ""
    info["status"] = getattr(src, "status", "") or ""

    # Charge
    if getattr(src, "object", None) == "charge":
        info["charge_id"] = src.id
        info["payment_intent"] = getattr(src, "payment_intent", "") or ""
        billing = getattr(src, "billing_details", None)
        if billing and getattr(billing, "email", None):
            info["customer_email"] = billing.email
        elif getattr(src, "receipt_email", None):
            info["customer_email"] = src.receipt_email
        if not info["description"]:
            info["description"] = getattr(src, "description", None) or ""

    return info


def collect_rows(
    types: set[str] | None = None,
    created: dict[str, int] | None = None,
) -> list[dict[str, Any]]:
    """
    Collecte les lignes du tableau.
    Par défaut: charge, payment, refund, payout, adjustment.
    """
    if types is None:
        types = {"charge", "payment", "payment_refund", "refund", "payout", "adjustment"}

    rows: list[dict[str, Any]] = []
    for txn in iter_balance_transactions(created=created):
        if txn.type not in types:
            continue

        fee_cents, fee_detail = fee_breakdown(txn)
        info = source_info(txn)
        currency = (txn.currency or "eur").upper()

        rows.append(
            {
                "date": format_date(txn.created),
                "type": txn.type,
                "description": info["description"] or (txn.description or ""),
                "email": info["customer_email"],
                "brut_cents": int(txn.amount or 0),
                "frais_cents": fee_cents,
                "net_cents": int(txn.net or 0),
                "brut": format_amount(txn.amount, currency),
                "frais": format_amount(fee_cents, currency),
                "net": format_amount(txn.net, currency),
                "devise": currency,
                "frais_detail": fee_detail,
                "txn_id": txn.id,
                "charge_id": info["charge_id"],
                "payment_intent": info["payment_intent"],
                "status": info["status"],
                "disponible": format_date(getattr(txn, "available_on", None)),
            }
        )
    return rows


def print_table(rows: list[dict[str, Any]]) -> None:
    cols = ["date", "type", "description", "email", "brut", "frais", "net", "devise"]
    widths = {c: len(c) for c in cols}
    for r in rows:
        for c in cols:
            widths[c] = max(widths[c], len(str(r.get(c, ""))))

    def line(r: dict[str, Any] | None = None) -> str:
        vals = []
        for c in cols:
            val = c if r is None else str(r.get(c, ""))
            vals.append(val.ljust(widths[c]))
        return " | ".join(vals)

    print(line())
    print("-+-".join("-" * widths[c] for c in cols))
    for r in rows:
        print(line(r))


def print_summary(rows: list[dict[str, Any]]) -> None:
    by_currency: dict[str, dict[str, int]] = {}
    for r in rows:
        if r["type"] not in {"charge", "payment"}:
            continue
        cur = r["devise"]
        agg = by_currency.setdefault(cur, {"brut": 0, "frais": 0, "net": 0, "n": 0})
        agg["brut"] += r["brut_cents"]
        agg["frais"] += r["frais_cents"]
        agg["net"] += r["net_cents"]
        agg["n"] += 1

    print("\n=== Récapitulatif (paiements / charges uniquement) ===")
    if not by_currency:
        print("Aucun paiement trouvé.")
        return
    for cur, agg in sorted(by_currency.items()):
        print(
            f"  {cur}: {agg['n']} paiements | "
            f"brut {format_amount(agg['brut'], cur)} | "
            f"frais {format_amount(agg['frais'], cur)} | "
            f"net {format_amount(agg['net'], cur)}"
        )


CSV_FIELDS = [
    "date",
    "type",
    "description",
    "email",
    "brut",
    "frais",
    "net",
    "devise",
    "frais_detail",
    "disponible",
    "txn_id",
    "charge_id",
    "payment_intent",
    "status",
]


def write_csv(path: str, rows: list[dict[str, Any]]) -> None:
    with open(path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=CSV_FIELDS, extrasaction="ignore", delimiter=";")
        writer.writeheader()
        writer.writerows(rows)
    print(f"\nCSV écrit: {path} ({len(rows)} lignes, séparateur ';')")


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Extrait les paiements Stripe avec frais (tableau asso)."
    )
    parser.add_argument(
        "--key",
        default=os.environ.get("STRIPE_API_KEY") or os.environ.get("STRIPE_SECRET_KEY"),
        help="Clé secrète Stripe (sinon STRIPE_API_KEY / STRIPE_SECRET_KEY)",
    )
    parser.add_argument("--from", dest="date_from", help="Date début YYYY-MM-DD")
    parser.add_argument("--to", dest="date_to", help="Date fin YYYY-MM-DD")
    parser.add_argument(
        "--csv",
        default="paiements_stripe.csv",
        help="Fichier CSV de sortie (défaut: paiements_stripe.csv)",
    )
    parser.add_argument(
        "--no-csv",
        action="store_true",
        help="N'écrit pas de CSV, affiche seulement le tableau",
    )
    parser.add_argument(
        "--payments-only",
        action="store_true",
        help="Uniquement charges/paiements (pas de refunds/payouts)",
    )
    args = parser.parse_args()

    if not args.key:
        print(
            "Manque la clé API. Exportez STRIPE_API_KEY=sk_live_... "
            "ou passez --key sk_live_...",
            file=sys.stderr,
        )
        return 1

    stripe.api_key = args.key

    created: dict[str, int] = {}
    if args.date_from:
        created["gte"] = parse_date(args.date_from)
    if args.date_to:
        created["lte"] = end_of_day(args.date_to)

    types = {"charge", "payment"} if args.payments_only else None

    print("Récupération des transactions Stripe...")
    rows = collect_rows(types=types, created=created or None)
    rows.sort(key=lambda r: r["date"])

    if not rows:
        print("Aucune transaction trouvée.")
        return 0

    print_table(rows)
    print_summary(rows)

    if not args.no_csv:
        write_csv(args.csv, rows)

    return 0


if __name__ == "__main__":
    sys.exit(main())
