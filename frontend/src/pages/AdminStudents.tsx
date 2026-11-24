import { useEffect, useMemo, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api, ApiError, type StudentAccount } from '../api';
import { fmtDate, useData } from '../hooks';
import { useFeedback } from '../ui/feedback';
import Go from '../ui/Go';

const FILTERS = [
  { key: 'all', label: 'All' },
  { key: 'Active', label: 'Active' },
  { key: 'New', label: 'Needs new password' },
  { key: 'Locked', label: 'Locked' },
  { key: 'Off', label: 'Turned off' },
] as const;

const STATUS: Record<string, string> = { Active: 'Active', New: 'Needs new password', Locked: 'Locked', Off: 'Turned off' };

/** Student accounts: search, filter by sign-in status, and unlock / reset / turn off / delete. */
export default function AdminStudents() {
  const { data, reload } = useData<StudentAccount[]>('/api/admin/students');
  const { notify, confirm } = useFeedback();
  const [params, setParams] = useSearchParams();
  const [q, setQ] = useState('');
  const [busy, setBusy] = useState<number | null>(null);
  const filter = params.get('status') ?? 'all';

  // A message handed over by the server-side add/edit form.
  useEffect(() => {
    const msg = params.get('msg');
    if (msg) { notify(msg); params.delete('msg'); setParams(params, { replace: true }); }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  const counts = useMemo(() => {
    const c: Record<string, number> = { Active: 0, New: 0, Locked: 0, Off: 0 };
    data?.forEach(a => { if (a.status in c) c[a.status]++; });
    return c;
  }, [data]);

  const rows = (data ?? []).filter(a => {
    const t = q.trim().toLowerCase();
    return (filter === 'all' || a.status === filter)
      && (!t || a.student.usn.toLowerCase().includes(t) || a.student.name.toLowerCase().includes(t));
  });

  async function act(a: StudentAccount, action: string, ask?: { text: string; ok: string; danger?: boolean }) {
    if (ask && !(await confirm(ask))) return;
    setBusy(a.student.id);
    try {
      const r = await api<{ message: string }>(`/api/admin/students/${a.student.id}` + (action === 'delete' ? '' : '/' + action),
        { method: action === 'delete' ? 'DELETE' : 'POST' });
      notify(r.message); reload();
    } catch (e) { notify(e instanceof ApiError ? e.message : 'Something went wrong', true); }
    finally { setBusy(null); }
  }

  const setFilter = (k: string) => { k === 'all' ? params.delete('status') : params.set('status', k); setParams(params, { replace: true }); };
  const usn = (a: StudentAccount) => a.student.usn;

  return (
    <main className="container">
      <div className="page-head">
        <div><h1>Students</h1><div className="muted">{data ? `${rows.length} of ${data.length} shown` : ' '}</div></div>
        <div className="toolbar">
          <input type="search" value={q} onChange={e => setQ(e.target.value)} placeholder="Search name or USN" aria-label="Search students" />
          <Go className="btn primary" href="/admin/students/new">Add student</Go>
        </div>
      </div>

      <div className="segmented" role="tablist" aria-label="Filter by sign-in status">
        {FILTERS.map(f => (
          <button key={f.key} type="button" role="tab" aria-selected={filter === f.key}
            className={filter === f.key ? 'on' : ''} onClick={() => setFilter(f.key)}>
            {f.key !== 'all' && <i className={'dot ' + f.key.toLowerCase()} />}
            <span>{f.label}</span>
            {f.key !== 'all' && <b>{counts[f.key]}</b>}
          </button>
        ))}
      </div>

      <div className="table-wrap"><table>
        <thead><tr><th>USN</th><th>Name</th><th>Dept</th><th className="num">Sem</th><th>Sec</th><th>Sign-in</th><th>Last signed in</th><th /></tr></thead>
        <tbody>
          {rows.map(a => (
            <tr key={a.student.id}>
              <td>{a.student.usn}</td>
              <td>{a.student.name}{a.student.email && <div className="muted small">{a.student.email}</div>}</td>
              <td>{a.student.department}</td>
              <td className="num">{a.student.semester}</td>
              <td>{a.student.section}</td>
              <td>{a.status === 'None' ? <span className="muted">No account</span>
                : <span className={'status ' + a.status.toLowerCase()}
                    title={a.locked && a.lockedUntil ? 'Unlocks at ' + fmtDate(a.lockedUntil, { hour: '2-digit', minute: '2-digit' }) : undefined}>
                    {STATUS[a.status]}</span>}</td>
              <td className="muted small">{a.lastLogin ? fmtDate(a.lastLogin, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' }) : 'Never'}</td>
              <td>
                <div className="row-actions" aria-busy={busy === a.student.id}>
                  <Go className="btn small" href={`/admin/students/${a.student.id}/edit`}>Edit</Go>
                  {a.status !== 'None' && <>
                    {a.locked && <button className="small" onClick={() => act(a, 'unlock')}>Unlock</button>}
                    <button className="small" onClick={() => act(a, 'reset-password', {
                      text: `Reset the password for ${usn(a)}? It goes back to student123 and they choose a new one at sign-in.`,
                      ok: 'Reset password' })}>Reset password</button>
                    {a.enabled
                      ? <button className="small" onClick={() => act(a, 'disable', {
                          text: `Turn off sign-in for ${usn(a)}? They are signed out right away. Their records stay.`, ok: 'Turn off' })}>Turn off</button>
                      : <button className="small" onClick={() => act(a, 'enable')}>Turn on</button>}
                  </>}
                  <button className="small quiet-danger" onClick={() => act(a, 'delete', {
                    text: `Delete ${usn(a)} and all their attendance, marks and bookings? This can't be undone.`, ok: 'Delete', danger: true })}>Delete</button>
                </div>
              </td>
            </tr>
          ))}
          {data && !rows.length && <tr><td colSpan={8} className="muted">No students match. Try another search or filter.</td></tr>}
        </tbody>
      </table></div>
    </main>
  );
}
