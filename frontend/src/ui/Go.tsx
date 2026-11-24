import { forwardRef, type AnchorHTMLAttributes } from 'react';
import { Link } from 'react-router-dom';
import { DEMO } from '../api';
import { useFeedback } from './feedback';

// Pages already in the React app open instantly; the rest are still server pages and load normally.
const SPA = [/^\/$/, /^\/profile$/, /^\/notices$/, /^\/student\//, /^\/admin\/students$/];
export const inApp = (href: string) => SPA.some(r => r.test(href));

type Props = AnchorHTMLAttributes<HTMLAnchorElement> & { href: string };
const Go = forwardRef<HTMLAnchorElement, Props>(({ href, onClick, ...rest }, ref) => {
  const { notify } = useFeedback();
  if (inApp(href)) return <Link ref={ref} to={href} onClick={onClick} {...rest} />;
  if (DEMO) {
    // The static preview has only the React pages; the others need the Java app.
    return <a ref={ref} href="#" {...rest} onClick={e => { e.preventDefault(); notify('This page is in the full app. Run it locally to see it.'); onClick?.(e); }} />;
  }
  return <a ref={ref} href={href} onClick={onClick} {...rest} />;
});
export default Go;
