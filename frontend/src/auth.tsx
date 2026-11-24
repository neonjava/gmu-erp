import { createContext, useCallback, useContext, useEffect, useState, type ReactNode } from 'react';
import { api, ApiError, type Me } from './api';
import { clearCache } from './hooks';

type Auth = { me: Me | null | undefined; refresh: () => Promise<Me | null>; logout: () => Promise<void> };
const AuthContext = createContext<Auth>(null!);

/** me: undefined while loading, null when signed out. */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [me, setMe] = useState<Me | null | undefined>(undefined);

  const refresh = useCallback(async () => {
    try {
      const m = await api<Me>('/api/me');
      setMe(prev => { if (prev && prev.username !== m.username) clearCache(); return m; });
      return m;
    } catch (e) {
      if (!(e instanceof ApiError) || e.status !== 401) console.error(e);
      setMe(null);
      return null;
    }
  }, []);

  const logout = useCallback(async () => {
    try { await api('/logout', { method: 'POST' }); } finally { clearCache(); setMe(null); await refresh(); }
  }, [refresh]);

  useEffect(() => { refresh(); }, [refresh]);
  return <AuthContext.Provider value={{ me, refresh, logout }}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
