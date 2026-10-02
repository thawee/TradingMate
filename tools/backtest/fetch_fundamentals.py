#!/usr/bin/env python3
"""Download quarterly fundamentals for the frozen backtest universe into tools/backtest/fundamentals/<SYMBOL>.csv.

Source: the thaifin library (https://github.com/ninyawee/thaifin), which reads Finnomena's public API.
Install it in a virtual environment first:  python3 -m venv .venv && .venv/bin/pip install thaifin
Then:  .venv/bin/python tools/backtest/fetch_fundamentals.py [SYMBOL ...]
The data is third-party and git-ignored; only this script is versioned.
Columns: quarter,net_profit,revenue,equity,npm,roe,debt_to_equity,dividend_yield,close,sector
"""
import csv, os, sys, time

from thaifin import Stock, Stocks

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, "fundamentals")
COLS = ["net_profit", "revenue", "equity", "npm", "roe", "debt_to_equity", "dividend_yield", "close"]


def universe():
    return [l.strip() for l in open(os.path.join(ROOT, "universe.txt")) if l.strip() and not l.startswith("#")]


def main():
    os.makedirs(OUT, exist_ok=True)
    symbols = sys.argv[1:] or universe()
    sectors = {}
    try:
        listing = Stocks.list_with_names()
        sectors = {r["symbol"]: (r.get("industry") or "") + "/" + (r.get("sector") or "") for _, r in listing.iterrows()}
    except Exception as e:  # sector is optional; financial detection then falls back to known names
        print("sector listing failed:", e)
    for sym in symbols:
        try:
            q = Stock(sym).quarter_dataframe
        except Exception as e:
            print(f"{sym}: failed ({e})")
            continue
        with open(os.path.join(OUT, f"{sym}.csv"), "w", newline="") as f:
            w = csv.writer(f)
            w.writerow(["quarter"] + COLS + ["sector"])
            for idx, row in q.iterrows():
                w.writerow([str(idx)] + ["" if row.get(c) is None or row.get(c) != row.get(c) else row.get(c) for c in COLS] + [sectors.get(sym, "")])
        print(f"{sym}: {len(q)} quarters")
        time.sleep(0.5)


if __name__ == "__main__":
    main()
