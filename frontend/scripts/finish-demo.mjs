// After `vite build --mode demo`: add the shared stylesheet, images and saved sample data to dist-demo/.
import { cpSync, writeFileSync } from 'node:fs';

const out = new URL('../dist-demo/', import.meta.url).pathname;
const stat = new URL('../../src/main/resources/static/', import.meta.url).pathname;
cpSync(stat + 'css', out + 'css', { recursive: true });
cpSync(stat + 'img', out + 'img', { recursive: true });
cpSync(new URL('../demo-data/', import.meta.url).pathname, out + 'demo', { recursive: true });
writeFileSync(out + '.nojekyll', '');
console.log('preview ready in dist-demo/');
