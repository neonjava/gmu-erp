import type { Notice } from '../api';
import { fmtDate, useData } from '../hooks';

export default function Notices() {
  const { data } = useData<Notice[]>('/api/notices');
  return (
    <main className="container">
      <div className="page-head"><div><h1>Latest news</h1><div className="muted">Notices from the college office</div></div></div>
      {data && !data.length && <p className="muted">No notices yet.</p>}
      {data?.map(n => (
        <div className="notice" key={n.id}>
          <b>{n.title}</b> <span className="muted small">{fmtDate(n.postedOn, { day: '2-digit', month: 'short', year: 'numeric' })}</span>
          <div>{n.body}</div>
        </div>
      ))}
    </main>
  );
}
