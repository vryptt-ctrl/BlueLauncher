/**
 * Generates public/icons.svg from src/data/icons.ts, so the site ships one
 * cached, first-party sprite file instead of inlining every icon's path into
 * every HTML document. The sprite is fetched once and reused across pages,
 * which cuts document weight and lifts the text-to-code ratio.
 *
 * Run automatically before every build via the prebuild npm hook.
 */
import { mkdirSync, readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const source = readFileSync(join(here, '../src/data/icons.ts'), 'utf8');

/** Extract `name: '<path>'` pairs from the ICON_PATHS object literal. */
function extractPaths(source) {
  const start = source.indexOf('ICON_PATHS');
  const open = source.indexOf('{', start);
  let depth = 0;
  let end = open;
  for (let i = open; i < source.length; i++) {
    if (source[i] === '{') depth++;
    if (source[i] === '}') {
      depth--;
      if (depth === 0) {
        end = i;
        break;
      }
    }
  }
  const body = source.slice(open + 1, end);
  const icons = [];
  const re = /(\w+):\s*'((?:[^'\\]|\\.)*)'/g;
  let m;
  while ((m = re.exec(body))) icons.push([m[1], m[2]]);
  return icons;
}

const FILLED = ['star', 'bolt', 'pin'];

const icons = extractPaths(source);
if (icons.length < 10) throw new Error('icon extraction failed: ' + icons.length + ' icons found');

const symbols = icons
  .map(([name, paths]) => {
    const filled = FILLED.includes(name);
    const attrs = filled
      ? 'fill="currentColor" stroke="none"'
      : 'fill="none" stroke="currentColor" stroke-width="1.75" stroke-linecap="round" stroke-linejoin="round"';
    return (
      '  <symbol id="i-' +
      name +
      '" viewBox="0 0 24 24"><g ' +
      attrs +
      '>' +
      paths +
      '</g></symbol>'
    );
  })
  .join('\n');

const svg =
  '<svg xmlns="http://www.w3.org/2000/svg" style="display:none">\n' + symbols + '\n</svg>\n';

const target = join(here, '../public/icons.svg');
mkdirSync(dirname(target), { recursive: true });
writeFileSync(target, svg);
console.log('icons.svg written (' + icons.length + ' symbols)');
