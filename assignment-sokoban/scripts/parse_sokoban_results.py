#!/usr/bin/env python3
# Parse section 1.3 Sokoban benchmark outputs under results-sokoban-large/raw/ and write CSVs + plots.
# Run from project root: python3 scripts/parse_sokoban_results.py

from __future__ import annotations

import csv
import re
import sys
from collections import defaultdict
from pathlib import Path
from statistics import mean, stdev

RAW_DIR = Path("results-sokoban-large/raw")
OUT_SUMMARY_RUNS = Path("results-sokoban-large/summary_runs.csv")
OUT_POOL_STATUS = Path("results-sokoban-large/pool_status.csv")
OUT_SUMMARY_AVG = Path("results-sokoban-large/summary_averages.csv")
PLOTS_DIR = Path("results-sokoban-large/plots")

FILENAME_OUT_RE = re.compile(
    r"^(?P<level>.+)_(?P<mode>normal|async)_t(?P<threads>\d+)_r(?P<run>\d+)\.out$"
)

OUT_LEVEL = re.compile(r"^level:\s*(.+)$", re.MULTILINE)
OUT_RUNTIME = re.compile(r"^runtime:\s*(\d+)\s*$", re.MULTILINE)
OUT_NUM_STATES = re.compile(r"^num_states:\s*(\d+)\s*$", re.MULTILINE)
OUT_LENGTH = re.compile(r"^\s*length:\s*(\d+)\s*$", re.MULTILINE)
OUT_SOLUTION_NULL = re.compile(r"^solution:\s*null\s*$", re.MULTILINE)

POOL_ASYNC_MODE = re.compile(r"^pool_async_mode:\s*(true|false)\s*$", re.MULTILINE)

POOL_STATUS_LINE = re.compile(
    r"^pool_status elapsed_s=(\d+) async=(true|false) threads=(\d+) parallelism=(\d+) "
    r"size=(\d+) active=(\d+) running=(\d+) steals=(\d+) queued_tasks=(\d+) "
    r"queued_submissions=(\d+)\s*$",
    re.MULTILINE,
)


def normalize_newlines(text: str) -> str:
    return text.replace("\r\n", "\n").replace("\r", "\n")


def parse_out_file(text: str) -> dict:
    """Extract fields from solver stdout."""
    m_level = OUT_LEVEL.search(text)
    m_rt = OUT_RUNTIME.search(text)
    m_ns = OUT_NUM_STATES.search(text)
    solution_null = bool(OUT_SOLUTION_NULL.search(text))
    m_len = OUT_LENGTH.search(text)

    return {
        "level_line": m_level.group(1).strip() if m_level else "",
        "runtime_ms": int(m_rt.group(1)) if m_rt else "",
        "num_states": int(m_ns.group(1)) if m_ns else "",
        "solution_length": int(m_len.group(1)) if m_len and not solution_null else "",
        "solution_null": solution_null,
    }


def parse_err_file(text: str) -> dict:
    """Parse stderr: timeout, final pool_async_mode, pool_status lines."""
    timed_out = "Hit timeout" in text
    async_lines = POOL_ASYNC_MODE.findall(text)
    final_pool_async_mode = async_lines[-1] if async_lines else ""

    pool_rows = []
    for m in POOL_STATUS_LINE.finditer(text):
        pool_rows.append(
            {
                "elapsed_s": int(m.group(1)),
                "async": m.group(2),
                "parallelism": int(m.group(4)),
                "size": int(m.group(5)),
                "active": int(m.group(6)),
                "running": int(m.group(7)),
                "steals": int(m.group(8)),
                "queued_tasks": int(m.group(9)),
                "queued_submissions": int(m.group(10)),
            }
        )

    final_steals = ""
    if pool_rows:
        final_steals = max(r["steals"] for r in pool_rows)

    return {
        "timed_out": timed_out,
        "final_pool_async_mode": final_pool_async_mode,
        "pool_status_rows": pool_rows,
        "final_steals": final_steals,
    }


def safe_mean(values: list) -> str:
    nums = [v for v in values if v != "" and v is not None]
    if not nums:
        return ""
    return f"{mean(nums):.6g}"


def safe_stdev(values: list) -> str:
    nums = [v for v in values if v != "" and v is not None]
    if len(nums) < 2:
        return "0" if len(nums) == 1 else ""
    return f"{stdev(nums):.6g}"


def main() -> int:
    if not RAW_DIR.is_dir():
        print(f"Error: directory not found: {RAW_DIR.resolve()}", file=sys.stderr)
        print("Create benchmark outputs first (e.g. run scripts/run_sokoban_bench.sh).", file=sys.stderr)
        return 1

    OUT_SUMMARY_RUNS.parent.mkdir(parents=True, exist_ok=True)
    PLOTS_DIR.mkdir(parents=True, exist_ok=True)

    run_rows: list[dict] = []
    pool_status_rows: list[dict] = []

    out_files = sorted(RAW_DIR.glob("*.out"))
    if not out_files:
        print(f"Warning: no .out files under {RAW_DIR}", file=sys.stderr)

    for out_path in out_files:
        m = FILENAME_OUT_RE.match(out_path.name)
        if not m:
            print(f"Skipping (filename pattern): {out_path.name}", file=sys.stderr)
            continue

        level = m.group("level")
        mode = m.group("mode")
        threads = int(m.group("threads"))
        run = int(m.group("run"))

        text = normalize_newlines(out_path.read_text(encoding="utf-8", errors="replace"))
        parsed = parse_out_file(text)

        err_path = out_path.with_suffix(".err")
        err_parsed: dict
        if err_path.is_file():
            err_text = normalize_newlines(err_path.read_text(encoding="utf-8", errors="replace"))
            err_parsed = parse_err_file(err_text)
        else:
            err_parsed = {
                "timed_out": False,
                "final_pool_async_mode": "",
                "pool_status_rows": [],
                "final_steals": "",
            }
            print(f"Warning: missing {err_path.name}", file=sys.stderr)

        expect_async = "true" if mode == "async" else "false"
        fpm = err_parsed.get("final_pool_async_mode", "")
        if fpm and fpm != expect_async:
            print(
                f"Warning: pool_async_mode={fpm} in {err_path.name} does not match "
                f"filename mode={mode}",
                file=sys.stderr,
            )

        run_rows.append(
            {
                "level": level,
                "mode": mode,
                "threads": threads,
                "run": run,
                "runtime_ms": parsed["runtime_ms"],
                "num_states": parsed["num_states"],
                "solution_length": parsed["solution_length"],
                "solution_null": parsed["solution_null"],
                "timed_out": err_parsed["timed_out"],
                "final_steals": err_parsed["final_steals"],
            }
        )

        for ps in err_parsed["pool_status_rows"]:
            pool_status_rows.append(
                {
                    "level": level,
                    "mode": mode,
                    "threads": threads,
                    "run": run,
                    **ps,
                }
            )

    # --- summary_runs.csv ---
    run_fieldnames = [
        "level",
        "mode",
        "threads",
        "run",
        "runtime_ms",
        "num_states",
        "solution_length",
        "solution_null",
        "timed_out",
        "final_steals",
    ]
    with OUT_SUMMARY_RUNS.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=run_fieldnames)
        w.writeheader()
        for r in run_rows:
            w.writerow(
                {
                    **{k: r[k] for k in run_fieldnames},
                    "solution_null": str(r["solution_null"]).lower(),
                    "timed_out": str(r["timed_out"]).lower(),
                }
            )

    # --- pool_status.csv ---
    ps_fieldnames = [
        "level",
        "mode",
        "threads",
        "run",
        "elapsed_s",
        "async",
        "parallelism",
        "size",
        "active",
        "running",
        "steals",
        "queued_tasks",
        "queued_submissions",
    ]
    with OUT_POOL_STATUS.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=ps_fieldnames)
        w.writeheader()
        for r in pool_status_rows:
            w.writerow({k: r[k] for k in ps_fieldnames})

    # --- Group means + summary_averages.csv ---
    groups: dict[tuple[str, str, int], list[dict]] = defaultdict(list)
    for r in run_rows:
        groups[(r["level"], r["mode"], r["threads"])].append(r)

    baseline_rt: dict[tuple[str, str], float] = {}
    for (lev, mod, thr), rows in groups.items():
        if thr != 1:
            continue
        rts = [row["runtime_ms"] for row in rows if row["runtime_ms"] != ""]
        if rts:
            baseline_rt[(lev, mod)] = mean(rts)

    avg_rows = []
    for (lev, mod, thr) in sorted(groups.keys()):
        xs = groups[(lev, mod, thr)]
        rts = [row["runtime_ms"] for row in xs if row["runtime_ms"] != ""]
        nss = [row["num_states"] for row in xs if row["num_states"] != ""]
        sts = [row["final_steals"] for row in xs if row["final_steals"] != ""]
        slens = [row["solution_length"] for row in xs if row["solution_length"] != ""]

        mean_rt = mean(rts) if rts else None
        speedup = ""
        b = baseline_rt.get((lev, mod))
        if b is not None and mean_rt is not None and mean_rt > 0:
            speedup = f"{b / mean_rt:.6g}"

        avg_rows.append(
            {
                "level": lev,
                "mode": mod,
                "threads": thr,
                "mean_runtime_ms": f"{mean_rt:.6g}" if mean_rt is not None else "",
                "std_runtime_ms": safe_stdev(rts),
                "mean_num_states": safe_mean(nss),
                "mean_final_steals": safe_mean(sts),
                "mean_solution_length": safe_mean(slens),
                "runs": len(xs),
                "speedup": speedup,
            }
        )

    avg_fieldnames = [
        "level",
        "mode",
        "threads",
        "mean_runtime_ms",
        "std_runtime_ms",
        "mean_num_states",
        "mean_final_steals",
        "mean_solution_length",
        "runs",
        "speedup",
    ]
    with OUT_SUMMARY_AVG.open("w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=avg_fieldnames)
        w.writeheader()
        for row in avg_rows:
            w.writerow(row)

    print(f"Wrote {OUT_SUMMARY_RUNS}")
    print(f"Wrote {OUT_POOL_STATUS}")
    print(f"Wrote {OUT_SUMMARY_AVG}")

    # --- Plots ---
    try:
        import matplotlib.pyplot as plt
    except ImportError:
        print(
            "matplotlib is not installed; CSV files were written successfully. "
            "Install matplotlib to generate plots (e.g. pip install matplotlib).",
            file=sys.stderr,
        )
        return 0

    # Build quick index: level -> mode -> threads -> mean_runtime, list of points for t32 steals
    rt_index: dict[str, dict[str, dict[int, float]]] = defaultdict(
        lambda: defaultdict(dict)
    )
    for row in avg_rows:
        if row["mean_runtime_ms"]:
            rt_index[row["level"]][row["mode"]][int(row["threads"])] = float(row["mean_runtime_ms"])

    steals_by_level_mode: dict[str, dict[str, list[tuple[int, int]]]] = defaultdict(
        lambda: defaultdict(list)
    )
    for r in pool_status_rows:
        if r["threads"] != 32:
            continue
        steals_by_level_mode[r["level"]][r["mode"]].append((r["elapsed_s"], r["steals"]))

    levels = sorted({r["level"] for r in run_rows}) if run_rows else sorted(rt_index.keys())

    for lev in levels:
        threads_sorted = sorted(
            {int(r["threads"]) for r in avg_rows if r["level"] == lev}
        )
        if not threads_sorted:
            continue

        # runtime_vs_threads
        fig, ax = plt.subplots(figsize=(8, 5))
        for mod, style in (("normal", "-"), ("async", "--")):
            ys = []
            xs = []
            for t in threads_sorted:
                mrt = rt_index.get(lev, {}).get(mod, {}).get(t)
                if mrt is not None:
                    xs.append(t)
                    ys.append(mrt)
            if xs:
                ax.plot(xs, ys, marker="o", linestyle=style, label=mod)
        ax.set_xlabel("threads")
        ax.set_ylabel("mean runtime (ms)")
        ax.set_title(f"Mean runtime vs threads — {lev}")
        ax.legend()
        ax.grid(True, alpha=0.3)
        fig.tight_layout()
        fig.savefig(PLOTS_DIR / f"runtime_vs_threads_{lev}.png", dpi=150)
        plt.close(fig)

        # speedup_vs_threads
        fig, ax = plt.subplots(figsize=(8, 5))
        ideal_x = threads_sorted
        ideal_y = threads_sorted
        ax.plot(ideal_x, ideal_y, color="gray", linestyle=":", label="ideal (linear)")
        for mod, style in (("normal", "-"), ("async", "--")):
            ys = []
            xs = []
            for t in threads_sorted:
                sp = None
                for ar in avg_rows:
                    if (
                        ar["level"] == lev
                        and ar["mode"] == mod
                        and int(ar["threads"]) == t
                    ):
                        sp = ar["speedup"]
                        break
                if sp != "" and sp is not None:
                    xs.append(t)
                    ys.append(float(sp))
            if xs:
                ax.plot(xs, ys, marker="o", linestyle=style, label=mod)
        ax.set_xlabel("threads")
        ax.set_ylabel("speedup (vs mean runtime at 1 thread)")
        ax.set_title(f"Speedup vs threads — {lev}")
        ax.legend()
        ax.grid(True, alpha=0.3)
        fig.tight_layout()
        fig.savefig(PLOTS_DIR / f"speedup_vs_threads_{lev}.png", dpi=150)
        plt.close(fig)

        # steals_over_time t32
        fig, ax = plt.subplots(figsize=(8, 5))
        for mod, style in (("normal", "-"), ("async", "--")):
            pts = steals_by_level_mode.get(lev, {}).get(mod, [])
            if not pts:
                continue
            by_elapsed: dict[int, list[int]] = defaultdict(list)
            for e, s in pts:
                by_elapsed[e].append(s)
            elapsed_sorted = sorted(by_elapsed.keys())
            avg_steals = [mean(by_elapsed[e]) for e in elapsed_sorted]
            ax.plot(
                elapsed_sorted,
                avg_steals,
                linestyle=style,
                label=f"{mod} (mean over runs)",
                marker="o",
                markersize=2,
            )
        ax.set_xlabel("elapsed (s)")
        ax.set_ylabel("steals (cumulative, ForkJoinPool)")
        ax.set_title(f"Steals over time — {lev} — 32 threads")
        ax.legend()
        ax.grid(True, alpha=0.3)
        fig.tight_layout()
        fig.savefig(PLOTS_DIR / f"steals_over_time_{lev}_t32.png", dpi=150)
        plt.close(fig)

        print(f"Wrote plots under {PLOTS_DIR}/ for level {lev}")

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
