#!/usr/bin/env bash
# Section 1.3 benchmark harness. Run from the assignment-sokoban project root.
set -u

LEVELS=("screen.2017" "screen.372" "screen.380" "screen.401" "screen.455")
THREADS=(1 2 4 8 16 32)
RUNS=3
RESULT_DIR="results-sokoban-large"

JAR="target/assignment-sokoban-1.0-jar-with-dependencies.jar"

if [[ ! -f "$JAR" ]]; then
  echo "Error: JAR not found at ${JAR}" >&2
  echo "Build it with:" >&2
  echo "  mvn clean compile assembly:single" >&2
  exit 1
fi

mkdir -p "${RESULT_DIR}/raw"

{
  echo "=== date ==="
  date
  echo
  echo "=== uname -a ==="
  uname -a
  echo
  echo "=== sysctl machdep.cpu.brand_string (if available) ==="
  if sysctl -n machdep.cpu.brand_string 2>/dev/null; then
    :
  else
    echo "(not available)"
  fi
  echo
  echo "=== sysctl hw.physicalcpu hw.logicalcpu (if available) ==="
  if sysctl hw.physicalcpu hw.logicalcpu 2>/dev/null; then
    :
  else
    echo "(not available)"
  fi
  echo
  echo "=== sysctl hw.memsize (if available) ==="
  if sysctl hw.memsize 2>/dev/null; then
    :
  else
    echo "(not available)"
  fi
  echo
  echo "=== java -version ==="
  java -version 2>&1
  echo
  echo "=== mvn -version ==="
  mvn -version 2>&1
} > "${RESULT_DIR}/metadata.txt"

for level in "${LEVELS[@]}"; do
  for mode in normal async; do
    for threads in "${THREADS[@]}"; do
      for ((run = 1; run <= RUNS; run++)); do
        echo "Running level=${level} mode=${mode} threads=${threads} run=${run}"
        out="${RESULT_DIR}/raw/${level}_${mode}_t${threads}_r${run}.out"
        err="${RESULT_DIR}/raw/${level}_${mode}_t${threads}_r${run}.err"
        if [[ "${mode}" == "async" ]]; then
          java -jar "$JAR" -p -t "${threads}" --async-pool "levels/${level}" > "${out}" 2> "${err}"
        else
          java -jar "$JAR" -p -t "${threads}" "levels/${level}" > "${out}" 2> "${err}"
        fi
        rc=$?
        if [[ "${rc}" -ne 0 ]]; then
          echo "Warning: run failed with exit code ${rc} for level=${level} mode=${mode} threads=${threads} run=${run}" >&2
        fi
      done
    done
  done
done
