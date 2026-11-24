import { useState, type FormEvent } from 'react';
import { Navigate, useNavigate, useSearchParams } from 'react-router-dom';
import { api, ApiError, ASSET, DEMO as PREVIEW } from '../api';
import { useAuth } from '../auth';
import { clearCache } from '../hooks';
import Leaves, { leavesWanted } from '../ui/Leaves';

const DEMO = [
  { label: 'Student', user: 'U25CS001', pass: 'student123' },
  { label: 'Faculty', user: 'F001', pass: 'faculty123' },
  { label: 'Admin', user: 'admin', pass: 'admin123' },
];

function message(reason?: string, left = 0) {
  if (reason === 'locked') return 'Too many wrong passwords, so this account is locked for 15 minutes. Wait, or ask the admin office to unlock it.';
  if (reason === 'disabled') return 'Sign-in is turned off for this account. Contact the admin office to turn it back on.';
  if (!reason) return null;
  const warn = left >= 1 && left <= 3
    ? (left === 1 ? ' One more wrong try locks the account.' : ` ${left} tries left before the account locks.`) : '';
  return 'That username and password don\'t match. Check them and try again.' + warn;
}

export default function Login() {
  const { me, refresh } = useAuth();
  const navigate = useNavigate();
  const [params] = useSearchParams();
  const [form, setForm] = useState({ username: '', password: '', remember: false });
  const [show, setShow] = useState(false);
  const [caps, setCaps] = useState(false);
  const [chip, setChip] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(message(params.get('error') ?? undefined));

  if (me) return <Navigate to="/" replace />;

  async function submit(e: FormEvent) {
    e.preventDefault();
    setBusy(true); setError(null);
    try {
      const body: Record<string, string> = { username: form.username, password: form.password };
      if (form.remember) body['remember-me'] = 'on';
      await api('/login', { method: 'POST', form: body });
      clearCache();
      await refresh();
      navigate('/', { replace: true });
    } catch (err) {
      const d = err instanceof ApiError ? err.data : null;
      setError(message(d?.reason ?? 'bad', d?.left ?? 0));
      setBusy(false);
    }
  }

  const capsCheck = (e: React.KeyboardEvent) => setCaps(e.getModifierState?.('CapsLock') ?? false);

  return (
    <div className="login-page">
      {leavesWanted() && <Leaves />}
      <div className="login-island" aria-hidden="true"><img className="logo" src={ASSET + 'img/gmu-logo.png'} alt="" width="40" height="40" /><span>GM University</span></div>
      <main className="login-box glass-window">
        <div className="window-body">
          <img className="login-logo" src={ASSET + 'img/gmu-logo.png'} alt="GM University, Innovating Minds" width="96" height="96" />
          <h1>Welcome back</h1>
          <p className="muted">Sign in to the GM University ERP with your USN or employee ID.</p>
          {error && <div className="flash error" role="alert">{error}</div>}
          {params.has('logout') && !error && <div className="flash">You're signed out.</div>}
          <form onSubmit={submit}>
            <div className="field">
              <label htmlFor="username">USN or employee ID</label>
              <input id="username" autoComplete="username" autoCapitalize="characters" spellCheck={false} required autoFocus
                value={form.username} onChange={e => setForm({ ...form, username: e.target.value })} />
            </div>
            <div className="field">
              <label htmlFor="password">Password</label>
              <div className="pw-wrap">
                <input id="password" type={show ? 'text' : 'password'} autoComplete="current-password" required
                  value={form.password} onChange={e => setForm({ ...form, password: e.target.value })}
                  onKeyDown={capsCheck} onKeyUp={capsCheck} />
                <button type="button" className="pw-toggle" aria-pressed={show} onClick={() => setShow(!show)}>
                  {show ? 'Hide' : 'Show'}
                </button>
              </div>
              {caps && <small className="caps">Caps Lock is on</small>}
            </div>
            <label className="switch" htmlFor="remember-me">
              <input id="remember-me" type="checkbox" role="switch" checked={form.remember}
                onChange={e => setForm({ ...form, remember: e.target.checked })} /><i /> Keep me signed in for a week
            </label>
            <button className="primary wide" type="submit" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</button>
          </form>
          <div className="demo">
            <p>Try a demo account</p>
            <div className="demo-chips">
              {DEMO.map(d => (
                <button key={d.label} type="button" className={'chip' + (chip === d.label ? ' on' : '')}
                  onClick={() => { setForm({ ...form, username: d.user, password: d.pass }); setChip(d.label); }}>
                  {d.label}
                </button>
              ))}
            </div>
            <p className="small muted forgot">{PREVIEW
              ? 'This is a static preview with sample data. Pick a demo account to look around; changes are not saved.'
              : 'Forgot your password? The admin office can reset it for you.'}</p>
          </div>
        </div>
      </main>
    </div>
  );
}
