#!/usr/bin/env python3
"""Download daily OHLCV from Yahoo Finance into tools/backtest/data/<SYMBOL>.csv.

Usage: python3 tools/backtest/fetch_history.py [SYMBOL ...]
With no arguments, fetches the frozen universe in universe.txt plus TDEX
(SET50 ETF, the benchmark; Yahoo serves no daily history for ^SET.BK).
Output columns: date,open,high,low,close,volume, dividend-adjusted (total return); rows with missing close are skipped.
"""
import csv, datetime, json, os, re, sys, time, urllib.parse, urllib.request

ROOT = os.path.dirname(os.path.abspath(__file__))
# BACKTEST_OUT / BACKTEST_START override the folder and first year (the holdout uses data_holdout from 2008).
DATA = os.path.join(ROOT, os.environ.get("BACKTEST_OUT", "data"))
UNIVERSE = os.path.join(ROOT, "universe.txt")
START = int(datetime.datetime(int(os.environ.get("BACKTEST_START", "2014")), 1, 1, tzinfo=datetime.timezone.utc).timestamp())
END = int(datetime.datetime(2026, 1, 1, tzinfo=datetime.timezone.utc).timestamp())


def set50_symbols():
    return [l.strip() for l in open(UNIVERSE) if l.strip() and not l.startswith("#")]


def fetch(yahoo_symbol):
    url = (f"https://query1.finance.yahoo.com/v8/finance/chart/{urllib.parse.quote(yahoo_symbol)}"
           f"?period1={START}&period2={END}&interval=1d&events=history")
    req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
    with urllib.request.urlopen(req, timeout=30) as r:
        result = json.load(r)["chart"]["result"][0]
    q = result["indicators"]["quote"][0]
    adj = (result["indicators"].get("adjclose") or [{}])[0].get("adjclose")
    rows = []
    for i, ts in enumerate(result.get("timestamp") or []):
        c = q["close"][i]
        if c is None:
            continue
        # Dividend-adjust OHLC so strategy and buy-and-hold are both total-return series.
        f = (adj[i] / c) if adj and adj[i] else 1.0
        d = datetime.datetime.fromtimestamp(ts, datetime.timezone(datetime.timedelta(hours=7))).date()
        rows.append((d.isoformat(), round((q["open"][i] or c) * f, 4), round((q["high"][i] or c) * f, 4),
                     round((q["low"][i] or c) * f, 4), round(c * f, 4), q["volume"][i] or 0))
    return rows


def main():
    os.makedirs(DATA, exist_ok=True)
    targets = sys.argv[1:] or (set50_symbols() + ["TDEX"])
    for sym in targets:
        ysym = f"{sym}.BK"
        try:
            rows = fetch(ysym)
        except Exception as e:
            print(f"{sym}: FAILED {e}")
            continue
        with open(os.path.join(DATA, f"{sym}.csv"), "w", newline="") as f:
            w = csv.writer(f)
            w.writerow(["date", "open", "high", "low", "close", "volume"])
            w.writerows(rows)
        first = rows[0][0] if rows else "-"
        print(f"{sym}: {len(rows)} rows from {first}")
        time.sleep(0.4)


if __name__ == "__main__":
    main()
