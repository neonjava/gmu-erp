import { useAuth } from '../auth';
import { fmtDate } from '../hooks';
import { useFeedback } from '../ui/feedback';
import PasswordForm from '../ui/PasswordForm';

const ROLE = { ADMIN: 'Admin', FACULTY: 'Faculty', STUDENT: 'Student' } as const;

export default function Profile() {
  const { me } = useAuth();
  const { notify } = useFeedback();
  if (!me) return null;
  const s = me.student, f = me.faculty;
  const rows: [string, string | undefined][] = s
    ? [['USN', s.usn], ['Department', s.departmentName], ['Semester and section', `${s.semester} ${s.section}`],
       ['Email', s.email], ['Phone', s.phone]]
    : f ? [['Employee ID', f.empId], ['Designation', f.designation], ['Department', f.departmentName],
       ['Email', f.email], ['Phone', f.phone]]
    : [['Role', 'Administrator']];

  return (
    <main className="container">
      <div className="profile-head">
        <span className="avatar big">{me.displayName.charAt(0)}</span>
        <div>
          <h1>{me.displayName}</h1>
          <div className="muted"><span>{me.username}</span><span className="tag solid">{ROLE[me.role]}</span></div>
          {me.lastLogin && <div className="muted small">
            Signed in {fmtDate(me.lastLogin, { day: 'numeric', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' })}
          </div>}
        </div>
      </div>
      <div className="profile-grid">
        <section className="glass pad">
          <h2 className="card-title">Details</h2>
          <dl className="details">
            {rows.map(([k, v]) => <div key={k} className="contents"><dt>{k}</dt><dd>{v || '—'}</dd></div>)}
          </dl>
        </section>
        <section className="glass pad">
          <h2 className="card-title">Change password</h2>
          <PasswordForm onDone={msg => notify(msg)} />
        </section>
      </div>
    </main>
  );
}
