#!/usr/bin/env bash
# Restart Node.js api.pastiumrah.com (admin server)
# Pemakaian: bash scripts/restart-node-admin.sh
#
# WAJIB: kill SEMUA proses (parent lsnode + child node app.js)
# karena filter tunggal "api.pastiumrah" TIDAK menangkap child node app.js.

set -e

SSH_PORT=65002
SSH_USER=u120369480
SSH_HOST=153.92.10.222

ssh -p "$SSH_PORT" "$SSH_USER@$SSH_HOST" 'bash -s' <<'REMOTE_EOF'
set -e
cd ~/domains/api.pastiumrah.com/hbuilds/versions/01a0c969-934d-723f-9aba-f2cf0517d7a8/nodejs/

echo "=== 1. Kill SEMUA proses (parent + child) ==="
PIDS=$(ps aux | grep -E "api\.pastiumrah|node app\.js" | grep -v grep | awk '{print $2}')
if [ -n "$PIDS" ]; then
  for pid in $PIDS; do
    kill -9 "$pid" 2>/dev/null && echo "  KILL -9 -> $pid"
  done
else
  echo "  Tidak ada proses aktif"
fi
sleep 5

echo
echo "=== 2. Pastikan bersih ==="
REMAINING=$(ps aux | grep -E "api\.pastiumrah|node app\.js" | grep -v grep || true)
if [ -n "$REMAINING" ]; then
  echo "  Masih ada, kill ulang:"
  echo "$REMAINING" | awk '{print $2}' | while read pid; do kill -9 "$pid" 2>/dev/null; done
  sleep 5
fi
ps aux | grep -E "api\.pastiumrah|node app\.js" | grep -v grep || echo "  BERSIH"

echo
echo "=== 3. Arsip log lama ==="
[ -f console.log ] && mv console.log console.log.old-$(date +%s) && echo "  console.log diarsip"
[ -f stderr.log ] && mv stderr.log stderr.log.old-$(date +%s) && echo "  stderr.log diarsip"

echo
echo "=== 4. Enable env + spawn baru ==="
source /opt/alt/alt-nodejs24/enable
set -a
source ~/domains/api.pastiumrah.com/hbuilds/config/.env
set +a

setsid nohup node app.js >> console.log 2>> stderr.log < /dev/null &
sleep 6

echo
echo "=== 5. Verifikasi PID ==="
ps aux | grep -E "api\.pastiumrah|node app\.js" | grep -v grep

echo
echo "=== 6. Log startup ==="
tail -5 console.log

echo
echo "=== 7. Cek stderr (HARUS KOSONG) ==="
if [ -s stderr.log ]; then
  echo "  GAGAL: stderr tidak kosong:"
  cat stderr.log
  exit 1
else
  echo "  OK: stderr kosong"
fi

echo
echo "=== DONE ==="
REMOTE_EOF
