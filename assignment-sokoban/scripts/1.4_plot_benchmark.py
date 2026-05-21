import csv
import statistics
from collections import defaultdict
from pathlib import Path
import matplotlib.pyplot as plt

SCRIPT_DIR = Path(__file__).parent.resolve()
INPUT_CSV = SCRIPT_DIR / "../../benchmarks/1.4_benchmark.csv"
PLOTS_DIR = SCRIPT_DIR / "../../plots"


def read_runtimes(csv_path):
    runtimes_by_thread = defaultdict(list)
    with csv_path.open(newline="") as file:
        reader = csv.DictReader(file)
        for row in reader:
            threads = int(row["threads"])
            runtime_ms = int(row["runtime_ms"])
            runtimes_by_thread[threads].append(runtime_ms)
    return runtimes_by_thread


def summarize(runtimes_by_thread):
    threads = sorted(runtimes_by_thread)
    mean_runtimes = [
        statistics.mean(runtimes_by_thread[thread])
        for thread in threads
    ]
    baseline = mean_runtimes[0]
    speedups = [baseline / runtime for runtime in mean_runtimes]
    return threads, mean_runtimes, speedups


def plot_runtime(threads, mean_runtimes):
    runtimes_seconds = [runtime / 1000 for runtime in mean_runtimes]

    plt.figure(figsize=(8, 5), dpi=160)
    plt.plot(threads, runtimes_seconds, marker="o", linewidth=2)
    plt.title("Mean runtime - screen.615")
    plt.xlabel("Number of threads")
    plt.ylabel("Mean runtime (seconds)")
    plt.xticks(threads)
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(PLOTS_DIR / "runtime - screen.615.png")
    plt.close()


def plot_speedup(threads, speedups):
    plt.figure(figsize=(8, 5), dpi=160)
    plt.plot(threads, speedups, marker="o", linewidth=2, label="Measured speedup")
    plt.plot(threads, threads, linestyle="--", linewidth=1.5, label="Ideal speedup")
    plt.title("Speedup - screen.615")
    plt.xlabel("Number of threads")
    plt.ylabel("Speedup")
    plt.xticks(threads)
    plt.grid(True, alpha=0.3)
    plt.legend()
    plt.tight_layout()
    plt.savefig(PLOTS_DIR / "speedup - screen.615.png")
    plt.close()



runtimes_by_thread = read_runtimes(INPUT_CSV)
threads, mean_runtimes, speedups = summarize(runtimes_by_thread)

plot_runtime(threads, mean_runtimes)
plot_speedup(threads, speedups)
