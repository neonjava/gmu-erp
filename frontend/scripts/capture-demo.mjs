// Saves the JSON the app shows for each role into demo-data/, so the static preview can run without a server.
// Run the Java app on a fresh database first (delete ./data), then:  node scripts/capture-demo.mjs
import { mkdirSync, writeFileSync, rmSync } from 'node:fs';

const BASE = process.env.BASE ?? 'http://localhost:8080';
const OUT = new URL('../demo-data/', import.meta.url).pathname;

const ROLES = {
  student: { user: 'U25CS001', pass: 'student123', paths: ['/api/student/attendance', '/api/student/results', '/api/student/timetable', '/api/student/registration', '/api/student/subjects'] },
  faculty: { user: 'F001', pass: 'faculty123', paths: [] },
  admin: { user: 'admin', pass: 'admin123', paths: ['/api/admin/students'] },
};

// A year of calendar months around today.
const months = [];
const now = new Date();
for (let i = -6; i <= 6; i++) {
  const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
  months.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`);
}

const key = p => p.replace(/^\/api\//, '').replace(/[/?=&]/g, '_');

async function session(user, pass) {
  const jar = new Map();
  const send = async (path, init = {}) => {
    const cookie = [...jar].map(([k, v]) => `${k}=${v}`).join('; ');
    const res = await fetch(BASE + path, { ...init, redirect: 'manual', headers: { 'X-Requested-With': 'XMLHttpRequest', cookie, ...(init.headers ?? {}) } });
    for (const c of res.headers.getSetCookie?.() ?? []) { const [kv] = c.split(';'); const i = kv.indexOf('='); jar.set(kv.slice(0, i), kv.slice(i + 1)); }
    return res;
  };
  await send('/api/me');
  const res = await send('/login', { method: 'POST', headers: { 'content-type': 'application/x-www-form-urlencoded', 'X-XSRF-TOKEN': jar.get('XSRF-TOKEN') ?? '' }, body: new URLSearchParams({ username: user, password: pass }) });
  if (res.status !== 200) throw new Error(`login failed for ${user}: ${res.status}`);
  return send;
}

rmSync(OUT, { recursive: true, force: true });
for (const [role, cfg] of Object.entries(ROLES)) {
  const send = await session(cfg.user, cfg.pass);
  mkdirSync(OUT + role, { recursive: true });
  const paths = ['/api/me', '/api/dashboard', '/api/notices', ...cfg.paths, ...months.map(m => `/api/calendar?month=${m}`)];
  for (const p of paths) {
    const res = await send(p);
    if (!res.ok) throw new Error(`${role} ${p} -> ${res.status}`);
    writeFileSync(`${OUT}${role}/${key(p)}.json`, await res.text());
  }
  console.log(role, paths.length, 'files');
}
