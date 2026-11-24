import { StrictMode, useEffect } from 'react';
import { createRoot } from 'react-dom/client';
import { BrowserRouter, HashRouter, Navigate, Route, Routes, useLocation, useOutlet } from 'react-router-dom';
import { AnimatePresence, motion } from 'framer-motion';
import { AuthProvider, useAuth } from './auth';
import { FeedbackProvider } from './ui/feedback';
import Island from './ui/Island';
import { BriefProvider } from './ui/SubjectBrief';
import Leaves, { leavesWanted } from './ui/Leaves';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Profile from './pages/Profile';
import FirstPassword from './pages/FirstPassword';
import Notices from './pages/Notices';
import AdminStudents from './pages/AdminStudents';
import { Attendance, Registration, Results, Timetable } from './pages/Student';
import { DEMO } from './api';
import './app.css';

const Router = DEMO ? HashRouter : BrowserRouter;   // the static preview can't rewrite deep links, so it uses #/ routes

/** Pages fade and slide a few pixels as you move between them. The outlet is held while the old page leaves. */
function Page() {
  const outlet = useOutlet();
  const { pathname } = useLocation();
  useEffect(() => { window.scrollTo({ top: 0 }); }, [pathname]);
  return (
    <AnimatePresence mode="wait" initial={false}>
      <motion.div key={pathname} initial={{ opacity: 0, y: 10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -4, transition: { duration: .11 } }}
        transition={{ duration: .24, ease: [.2, .8, .2, 1] }}>
        {outlet}
      </motion.div>
    </AnimatePresence>
  );
}

/** Signed-in frame: the island on top, the page below. Leaves fall on the dashboard (toggle in the island). */
function Shell() {
  const { me } = useAuth();
  if (me === undefined) return <div className="loading" aria-busy="true" />;
  if (me === null) return <Navigate to="/login" replace />;
  if (me.mustChangePassword) return <FirstPassword />;

  return (
    <BriefProvider>
      {leavesWanted() && <Leaves />}
      <Island />
      <Page />
    </BriefProvider>
  );
}

function Only({ role, children }: { role: string; children: JSX.Element }) {
  const { me } = useAuth();
  return me?.role === role ? children : <Navigate to="/" replace />;
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Router {...(DEMO ? {} : { basename: '/app' })}>
      <AuthProvider>
        <FeedbackProvider>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route element={<Shell />}>
              <Route index element={<Dashboard />} />
              <Route path="profile" element={<Profile />} />
              <Route path="notices" element={<Notices />} />
              <Route path="student/attendance" element={<Only role="STUDENT"><Attendance /></Only>} />
              <Route path="student/results" element={<Only role="STUDENT"><Results /></Only>} />
              <Route path="student/timetable" element={<Only role="STUDENT"><Timetable /></Only>} />
              <Route path="student/registration" element={<Only role="STUDENT"><Registration /></Only>} />
              <Route path="admin/students" element={<Only role="ADMIN"><AdminStudents /></Only>} />
              <Route path="*" element={<Navigate to="/" replace />} />
            </Route>
          </Routes>
        </FeedbackProvider>
      </AuthProvider>
      {DEMO && <div className="demo-badge">Static preview with sample data. Changes are not saved.</div>}
    </Router>
  </StrictMode>,
);
