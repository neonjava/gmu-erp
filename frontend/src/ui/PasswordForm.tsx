import { useState, type FormEvent } from 'react';
import { api, ApiError } from '../api';
import { useFeedback } from './feedback';

const WORDS = ['At least 6 characters', 'Weak, add more characters', 'Okay', 'Strong', 'Very strong'];
function strength(v: string) {
  if (v.length < 6) return 0;
  let s = 1;
  if (v.length >= 10) s++;
  if (/[A-Z]/.test(v) && /[a-z]/.test(v)) s++;
  if (/\d/.test(v) && /[^A-Za-z0-9]/.test(v)) s++;
  return s;
}

/** Change-password form with a strength meter. Used on the profile page and on first sign-in. */
export default function PasswordForm({ first, onDone }: { first?: boolean; onDone: (msg: string) => void }) {
  const { notify } = useFeedback();
  const [f, setF] = useState({ current: '', next: '', confirm: '' });
  const [busy, setBusy] = useState(false);
  const level = strength(f.next);
  const set = (k: keyof typeof f) => (e: React.ChangeEvent<HTMLInputElement>) => setF({ ...f, [k]: e.target.value });

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (f.next !== f.confirm) { notify('The two new passwords don\'t match', true); return; }
    setBusy(true);
    try {
      const r = await api<{ message: string }>('/api/me/password', { method: 'POST', body: f });
      setF({ current: '', next: '', confirm: '' });
      onDone(r.message);
    } catch (err) {
      notify(err instanceof ApiError ? err.message : 'Could not change the password', true);
    } finally { setBusy(false); }
  }

  return (
    <form className="pw-form" onSubmit={submit}>
      <div className="field">
        <label htmlFor="current">Current password</label>
        <input id="current" type="password" autoComplete="current-password" required autoFocus={first}
          value={f.current} onChange={set('current')} />
        {first && <small className="muted">This is the one the admin gave you, like student123.</small>}
      </div>
      <div className="field">
        <label htmlFor="next">New password</label>
        <input id="next" type="password" minLength={6} autoComplete="new-password" required value={f.next} onChange={set('next')} />
        <div className="strength" data-level={level} aria-live="polite"
          style={{ '--s': f.next.length < 6 ? Math.min(f.next.length / 6, .9) : level } as React.CSSProperties}>
          <i /><span>{WORDS[level]}</span>
        </div>
      </div>
      <div className="field">
        <label htmlFor="confirm">Type it again</label>
        <input id="confirm" type="password" minLength={6} autoComplete="new-password" required value={f.confirm} onChange={set('confirm')} />
      </div>
      {first
        ? <button className="primary wide" type="submit" disabled={busy}>Save password and continue</button>
        : <div className="form-actions"><button className="primary" type="submit" disabled={busy}>Change password</button></div>}
    </form>
  );
}
