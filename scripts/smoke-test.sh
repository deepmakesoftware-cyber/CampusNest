#!/usr/bin/env bash
# CampusNest smoke test: checks that every page and the main API endpoints answer correctly through the
# Next.js proxy, for each role. Run it before a demo, with the database, backend and frontend running
# and DEMO_DATA=true in .env (it logs in as the sample accounts).
#
#   bash scripts/smoke-test.sh            (from the repository root; uses Git Bash on Windows)
set -u
B=${BASE_URL:-http://localhost:3000}
ENV_FILE=${ENV_FILE:-.env}
get_env() { grep "^$1=" "$ENV_FILE" | cut -d= -f2- | tr -d '\r'; }
PW=$(get_env DEMO_PASSWORD); AE=$(get_env ADMIN_EMAIL); AP=$(get_env ADMIN_PASSWORD)
D=$(mktemp -d); trap 'rm -rf "$D"' EXIT
xs() { awk '$6=="XSRF-TOKEN"{print $7}' "$1" | tail -1; }
login() {
  curl -s -o /dev/null -c "$1" -b "$1" "$B/api/v1/auth/csrf"
  curl -s -o /dev/null -w '%{http_code}' -c "$1" -b "$1" -X POST -H "X-XSRF-TOKEN: $(xs "$1")" \
    --data-urlencode "email=$2" --data-urlencode "password=$3" "$B/api/v1/auth/login"
}
fail=0
chk() { if [ "$2" = "$3" ]; then echo "ok   $1"; else echo "FAIL $1 (expected $2, got $3)"; fail=$((fail + 1)); fi; }
code() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

chk "student login" 200 "$(login "$D/s" student.asha@example.com "$PW")"
chk "owner login" 200 "$(login "$D/o" owner.ramesh@example.com "$PW")"
chk "admin login" 200 "$(login "$D/a" "$AE" "$AP")"

L1=$(curl -s "$B/api/v1/listings?q=Green%20Nest" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
L2=$(curl -s "$B/api/v1/listings?q=Chembur" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)

for u in "/api/v1/listings?collegeId=1" "/api/v1/listings/$L1" "/api/v1/listings/$L1/reviews" /api/v1/colleges /api/v1/amenities /api/v1/fare-rules; do
  chk "GET $u (public)" 200 "$(code "$B$u")"
done
for u in /api/v1/student/profile /api/v1/student/recommendations "/api/v1/student/compare?ids=$L1,$L2" /api/v1/student/saved \
         /api/v1/student/enquiries /api/v1/student/roommates/matches /api/v1/student/roommates/connections; do
  chk "GET $u (student)" 200 "$(code -b "$D/s" "$B$u")"
done
for u in /api/v1/owner/profile /api/v1/owner/listings /api/v1/owner/enquiries; do
  chk "GET $u (owner)" 200 "$(code -b "$D/o" "$B$u")"
done
for u in /api/v1/admin/overview /api/v1/admin/users "/api/v1/admin/reports?status=OPEN" "/api/v1/admin/owners?status=PENDING"; do
  chk "GET $u (admin)" 200 "$(code -b "$D/a" "$B$u")"
done
chk "student cannot use admin API" 403 "$(code -b "$D/s" "$B/api/v1/admin/users")"
chk "owner cannot use student API" 403 "$(code -b "$D/o" "$B/api/v1/student/recommendations")"
chk "anonymous cannot use owner API" 401 "$(code "$B/api/v1/owner/listings")"

for p in / /listings "/listings/$L1" /login /register /student /student/recommendations "/student/compare?ids=$L1,$L2" \
         /student/roommates /messages /owner /admin /admin/listings /admin/reports /admin/owners /admin/fares /status; do
  chk "page $p" 200 "$(code "$B$p")"
done

echo "=== $fail failure(s)"
exit "$fail"
