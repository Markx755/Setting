#!/system/bin/sh
PATTERN="${1:-roblox}"
COLS="${2:-0}"
INTERVAL="${3:-3}"
MARGIN="${4:-0}"

settings put global enable_freeform_support 1
settings put global force_resizable_activities 1

resize_task() {
  am task resize "$1" "$2" "$3" "$4" "$5" 2>/dev/null \
    || cmd activity task resize "$1" "$2" "$3" "$4" "$5" 2>/dev/null
}

get_ids() {
  dumpsys activity activities 2>/dev/null \
    | grep -iE "Task(Record)?\{" | grep -i "$PATTERN" \
    | sed -n 's/.*#\([0-9][0-9]*\).*/\1/p' | sort -un
}

LAST=""
while true; do
  IDS=$(get_ids | tr '\n' ' ')
  N=$(echo $IDS | wc -w)
  if [ "$N" -eq 0 ] && [ "$LAST" != "none" ]; then
    echo "no tasks matched '$PATTERN'"
    dumpsys activity activities 2>/dev/null | grep -iE "Task(Record)?\{" | head -5
    LAST="none"
  fi
  if [ "$N" -gt 0 ] && [ "$IDS" != "$LAST" ]; then
    SIZE=$(wm size | tail -1 | sed 's/.*: //')
    W=${SIZE%x*}; H=${SIZE#*x}
    ROT=$(dumpsys window displays 2>/dev/null | grep -o 'rotation=[0-9]' | head -1 | cut -d= -f2)
    if [ "$ROT" = "1" ] || [ "$ROT" = "3" ]; then T=$W; W=$H; H=$T; fi
    C=$COLS
    if [ "$C" -le 0 ]; then
      C=1
      while [ $((C * C)) -lt "$N" ]; do C=$((C + 1)); done
    fi
    R=$(( (N + C - 1) / C ))
    CW=$((W / C)); CH=$((H / R))
    i=0
    for id in $IDS; do
      r=$((i / C)); c=$((i % C))
      L=$((c * CW + MARGIN)); T=$((r * CH + MARGIN))
      resize_task "$id" "$L" "$T" $((L + CW - MARGIN)) $((T + CH - MARGIN))
      i=$((i + 1))
    done
    echo "arranged $N windows ($C x $R) cell ${CW}x${CH}"
    LAST="$IDS"
  fi
  sleep "$INTERVAL"
done
