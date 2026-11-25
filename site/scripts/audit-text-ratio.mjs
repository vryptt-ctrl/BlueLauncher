// One-off audit: text-to-code ratio, word count and inline-SVG weight per page.
import { readdirSync, readFileSync, statSync } from 'node:fs';
import { join } from 'node:path';

const root = new URL('../dist', import.meta.url).pathname.replace(/^\/([A-Za-z]:)/, '$1');

function walk(dir) {
  let out = [];
  for (const f of readdirSync(dir)) {
    const p = join(dir, f);
    if (statSync(p).isDirectory()) out = out.concat(walk(p));
    else if (f.endsWith('.html')) out.push(p);
  }
  return out;
}

const rows = [];
for (const p of walk(root)) {
  const html = readFileSync(p, 'utf8');
  const raw = html.length;
  const text = html
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<style[\s\S]*?<\/style>/gi, ' ')
    .replace(/<[^>]+>/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
  const svgBytes = [...html.matchAll(/<svg[\s\S]*?<\/svg>/g)].reduce((a, m) => a + m[0].length, 0);
  const name = p
    .slice(root.length + 1)
    .replace(/\\/g, '/')
    .replace(/index\.html$/, '');
  rows.push({
    page: name || '/',
    rawKB: +(raw / 1024).toFixed(1),
    words: text.split(' ').filter((w) => /[a-z]/i.test(w)).length,
    ratio: +((text.length / raw) * 100).toFixed(2),
    svgKB: +(svgBytes / 1024).toFixed(1),
  });
}
console.table(rows);
