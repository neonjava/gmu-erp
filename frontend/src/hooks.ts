import { useCallback, useEffect, useState } from 'react';
import { api, ApiError } from './api';

// Answers already fetched, by path. A page shows the last answer straight away and refreshes it in the
// background, so moving between pages never flashes empty. Hovering a tab warms it up with prefetch().
const cache = new Map<string, unknown>();
export function prefetch(path: string) {
  api(path).then(d => cache.set(path, d)).catch(() => { /* ignore: the page will report errors itself */ });
}
export function clearCache() { cache.clear(); }

/** Loads JSON from the API; `reload()` fetches again after a change. */
export function useData<T>(path: string | null) {
  const [data, setData] = useState<T | null>(() => (path && cache.has(path) ? (cache.get(path) as T) : null));
  const [error, setError] = useState<ApiError | null>(null);
  const [tick, setTick] = useState(0);
  useEffect(() => {
    if (!path) return;
    let alive = true;
    api<T>(path)
      .then(d => { cache.set(path, d); if (alive) { setData(d); setError(null); } })
      .catch(e => { if (alive) setError(e); });
    return () => { alive = false; };
  }, [path, tick]);
  const reload = useCallback(() => setTick(t => t + 1), []);
  return { data, error, reload };
}

/** The current time, refreshed every `ms` (default 30 s). */
export function useNow(ms = 30000) {
  const [now, setNow] = useState(() => new Date());
  useEffect(() => {
    const id = setInterval(() => setNow(new Date()), ms);
    return () => clearInterval(id);
  }, [ms]);
  return now;
}

export function useReducedMotion() {
  const q = '(prefers-reduced-motion: reduce)';
  const [reduce, setReduce] = useState(() => window.matchMedia(q).matches);
  useEffect(() => {
    const m = window.matchMedia(q);
    const on = () => setReduce(m.matches);
    m.addEventListener('change', on);
    return () => m.removeEventListener('change', on);
  }, []);
  return reduce;
}

export const fmtDate = (iso: string, opts: Intl.DateTimeFormatOptions) =>
  new Date(iso).toLocaleString('en-IN', opts);
