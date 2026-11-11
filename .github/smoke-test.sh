#!/usr/bin/env bash
# Logs in as each demo user and opens every page. Fails if any page is not HTTP 200.
set -u
BASE=http://localhost:8080
FAIL=0

login() {
  local user=$1 pass=$2 jar=$3
  rm -f "$jar"
  local token
  token=$(curl -s -c "$jar" -b "$jar" "$BASE/login" | grep -o 'name="_csrf" value="[^"]*"' | sed 's/.*value="//;s/"$//')
  curl -s -o /dev/null -c "$jar" -b "$jar" -X POST "$BASE/login" \
    --data-urlencode "username=$user" --data-urlencode "password=$pass" --data-urlencode "_csrf=$token"
}

check() {
  local jar=$1 path=$2
  local code
  code=$(curl -s -o /tmp/page.html -w '%{http_code}' -b "$jar" "$BASE$path")
  if [ "$code" != "200" ] || grep -q "Whitelabel\|Login · GMU ERP" /tmp/page.html; then
    echo "FAIL $code $path"; FAIL=1
  else
    echo "ok   $path"
  fi
}

login admin admin123 /tmp/a.jar
for p in / /notices /profile /admin/students "/admin/students?q=hem" /admin/students/new /admin/students/1/edit \
         /admin/faculty /admin/faculty/new /admin/faculty/1/edit /admin/courses /admin/courses/new /admin/courses/1/edit \
         /admin/departments /admin/notices /admin/timetable; do check /tmp/a.jar "$p"; done

login F001 faculty123 /tmp/f.jar
for p in / /faculty/courses /faculty/timetable /faculty/courses/1/attendance /faculty/courses/1/marks; do check /tmp/f.jar "$p"; done

login U25CS001 student123 /tmp/s.jar
for p in / /student/attendance /student/timetable /student/results /student/registration /profile; do check /tmp/s.jar "$p"; done

exit $FAIL
