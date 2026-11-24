// Small fetch wrapper for the Spring Boot API: same-origin session cookie, CSRF header, JSON in and out.

export class ApiError extends Error {
  constructor(public status: number, public data: any) {
    super(data?.message ?? `Request failed (${status})`);
  }
}

function cookie(name: string) {
  const hit = document.cookie.split('; ').find(c => c.startsWith(name + '='));
  return hit ? decodeURIComponent(hit.slice(name.length + 1)) : undefined;
}

type Options = { method?: string; body?: unknown; form?: Record<string, string> };

// ---------- Static preview (GitHub Pages): the same screens, answered from saved sample data ----------
export const DEMO = import.meta.env.VITE_DEMO === '1';
/** Where shared files (logo, stylesheet) live: the server root normally, the site folder in the preview. */
export const ASSET: string = import.meta.env.VITE_ASSET_BASE ?? '/';

const DEMO_LOGINS: Record<string, [string, string]> = {
  u25cs001: ['student123', 'student'], f001: ['faculty123', 'faculty'], admin: ['admin123', 'admin'],
};
const demoRole = () => { try { return sessionStorage.getItem('gmu-demo-role'); } catch { return null; } };

async function demoApi<T>(path: string, opts: Options): Promise<T> {
  const method = opts.method ?? 'GET';
  if (path === '/login') {
    const hit = DEMO_LOGINS[(opts.form?.username ?? '').trim().toLowerCase()];
    if (!hit || hit[0] !== opts.form?.password) throw new ApiError(401, { reason: 'bad', left: 4 });
    try { sessionStorage.setItem('gmu-demo-role', hit[1]); } catch { /* private mode */ }
    return { ok: true } as T;
  }
  if (path === '/logout') { try { sessionStorage.removeItem('gmu-demo-role'); } catch { /* ignore */ } return null as T; }
  if (method !== 'GET') throw new ApiError(400, { message: 'This is a read-only preview, so changes are not saved. Run the Java app to try them.' });
  const role = demoRole();
  if (!role) throw new ApiError(401, null);
  const file = path.replace(/^\/api\//, '').replace(/[/?=&]/g, '_');
  const res = await fetch(`${ASSET}demo/${role}/${file}.json`);
  if (!res.ok) throw new ApiError(404, { message: 'Not in the preview' });
  return res.json();
}

export async function api<T = any>(path: string, opts: Options = {}): Promise<T> {
  if (DEMO) return demoApi<T>(path, opts);
  const method = opts.method ?? 'GET';
  const headers: Record<string, string> = { 'X-Requested-With': 'XMLHttpRequest', Accept: 'application/json' };
  if (method !== 'GET') {
    const token = cookie('XSRF-TOKEN');
    if (token) headers['X-XSRF-TOKEN'] = token;
  }
  let body: BodyInit | undefined;
  if (opts.form) {
    body = new URLSearchParams(opts.form);
    headers['Content-Type'] = 'application/x-www-form-urlencoded';
  } else if (opts.body !== undefined) {
    body = JSON.stringify(opts.body);
    headers['Content-Type'] = 'application/json';
  }
  const res = await fetch(path, { method, headers, body, credentials: 'same-origin' });
  const text = await res.text();
  let data: any = null;
  try { data = text ? JSON.parse(text) : null; } catch { data = null; }
  if (!res.ok) throw new ApiError(res.status, data);
  return data as T;
}

// ---------- Shapes returned by the API (see Dto.java) ----------
export type StudentInfo = { id: number; usn: string; name: string; department?: string; departmentName?: string;
  semester: number; section: string; email?: string; phone?: string };
export type FacultyInfo = { id: number; empId: string; name: string; designation?: string; department?: string;
  departmentName?: string; email?: string; phone?: string };
export type Me = { username: string; displayName: string; role: 'ADMIN' | 'FACULTY' | 'STUDENT';
  mustChangePassword: boolean; lastLogin?: string; student?: StudentInfo; faculty?: FacultyInfo };
export type TodayClass = { period: number; time: string; code: string; name: string; room: string; group: string };
export type Stat = { label: string; value: string; link: string; ring?: number | null };
export type Notice = { id: number; title: string; body: string; postedOn: string };
export type EventRow = { id: number; title: string; category: string; venue: string; date: string };
export type CourseRow = { id: number; code: string; name: string; credits: number; semester: number; faculty?: string };
export type Dashboard = { greeting: string; stats: Stat[]; today?: TodayClass[] | null; notices: Notice[];
  events: EventRow[]; myCourses?: CourseRow[] | null };
export type StudentAccount = { student: StudentInfo; status: 'Active' | 'New' | 'Locked' | 'Off' | 'None';
  lastLogin?: string; lockedUntil?: string; enabled: boolean; locked: boolean };
