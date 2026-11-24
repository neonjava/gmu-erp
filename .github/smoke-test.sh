#!/usr/bin/env bash
# Logs in as each demo user (the same JSON call the React app makes), then opens every page and API.
# Server-rendered pages must return HTTP 200 and the React app's pages must be served, with data from /api.
set -u
BASE=http://localhost:8080
FAIL=0

login() {
  local user=$1 pass=$2 jar=$3 token code
  rm -f "$jar"
  curl -s -o /dev/null -c "$jar" -b "$jar" -H 'X-Requested-With: XMLHttpRequest' "$BASE/api/me"   # hands out the CSRF cookie
  token=$(awk '$6=="XSRF-TOKEN"{print $7}' "$jar")
  code=$(curl -s -o /dev/null -w '%{http_code}' -c "$jar" -b "$jar" -H 'X-Requested-With: XMLHttpRequest' \
    -H "X-XSRF-TOKEN: $token" --data-urlencode "username=$user" --data-urlencode "password=$pass" "$BASE/login")
  [ "$code" = "200" ] || { echo "FAIL login $user ($code)"; FAIL=1; }
}

check() {   # server-rendered page
  local jar=$1 path=$2 code
  code=$(curl -s -o /tmp/page.html -w '%{http_code}' -b "$jar" "$BASE$path")
  if [ "$code" != "200" ] || grep -q "Whitelabel\|Sign in · GMU ERP" /tmp/page.html; then
    echo "FAIL $code $path"; FAIL=1
  else
    echo "ok   $path"
  fi
}

api() {     # JSON endpoint
  local jar=$1 path=$2 code
  code=$(curl -s -o /tmp/api.json -w '%{http_code}' -b "$jar" -H 'X-Requested-With: XMLHttpRequest' "$BASE$path")
  if [ "$code" != "200" ] || [ ! -s /tmp/api.json ]; then echo "FAIL $code $path"; FAIL=1; else echo "ok   $path"; fi
}

app() {     # React app route: the shell is served for every client-side route
  local code
  code=$(curl -s -o /tmp/page.html -w '%{http_code}' "$BASE/app$1")
  if [ "$code" != "200" ] || ! grep -q 'id="root"' /tmp/page.html; then echo "FAIL $code /app$1"; FAIL=1; else echo "ok   /app$1"; fi
}

for r in "" /login /profile /notices /student/results /admin/students; do app "$r"; done

login admin admin123 /tmp/a.jar
for p in /api/me /api/dashboard /api/notices /api/admin/students; do api /tmp/a.jar "$p"; done
for p in /admin/students/new /admin/students/1/edit \
         /admin/faculty /admin/faculty/new /admin/faculty/1/edit /admin/courses /admin/courses/new /admin/courses/1/edit \
         /admin/departments /admin/notices /admin/timetable /admin/resources \
         /events /events/1 /events/new /resources /resources/1 /resources/4; do check /tmp/a.jar "$p"; done

login F001 faculty123 /tmp/f.jar
for p in /api/me /api/dashboard; do api /tmp/f.jar "$p"; done
for p in /faculty/courses /faculty/timetable /faculty/courses/1/attendance /faculty/courses/1/marks \
         /events /events/1 /events/new /resources /resources/1; do check /tmp/f.jar "$p"; done

login U25CS001 student123 /tmp/s.jar
for p in /api/me /api/dashboard /api/notices /api/student/attendance /api/student/results /api/student/timetable /api/student/registration /api/student/subjects; do api /tmp/s.jar "$p"; done
for p in /events /events/1 /events/3 /resources /resources/1 /resources/4; do check /tmp/s.jar "$p"; done

# Rules: a student can't use admin APIs, and signed-out API calls get 401
code=$(curl -s -o /dev/null -w '%{http_code}' -b /tmp/s.jar -H 'X-Requested-With: XMLHttpRequest' "$BASE/api/admin/students")
[ "$code" = "403" ] && echo "ok   student blocked from admin API" || { echo "FAIL student got $code from admin API"; FAIL=1; }
code=$(curl -s -o /dev/null -w '%{http_code}' -H 'X-Requested-With: XMLHttpRequest' "$BASE/api/dashboard")
[ "$code" = "401" ] && echo "ok   signed-out API gets 401" || { echo "FAIL signed-out API got $code"; FAIL=1; }

exit $FAIL
