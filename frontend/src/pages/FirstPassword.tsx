import { useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { useFeedback } from '../ui/feedback';
import PasswordForm from '../ui/PasswordForm';

/** First sign-in (or after a reset): only the password window, nothing else to do yet. */
export default function FirstPassword() {
  const { refresh, logout } = useAuth();
  const { notify } = useFeedback();
  const navigate = useNavigate();
  return (
    <main className="container first-run">
      <div className="glass-window">
        <div className="window-body">
          <h1>Choose your own password</h1>
          <p className="muted">You're signed in with the default password. Pick a new one to open the rest of the ERP.</p>
          <PasswordForm first onDone={async msg => { await refresh(); notify(msg); navigate('/', { replace: true }); }} />
          <button type="button" className="link-btn later" onClick={logout}>Sign out instead</button>
        </div>
      </div>
    </main>
  );
}
