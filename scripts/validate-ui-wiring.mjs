#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';

const repoRoot = process.cwd();

function readFile(relPath) {
  return fs.readFileSync(path.join(repoRoot, relPath), 'utf8');
}

function walk(dir, acc = []) {
  const entries = fs.readdirSync(dir, { withFileTypes: true });
  for (const entry of entries) {
    const full = path.join(dir, entry.name);
    if (entry.isDirectory()) {
      walk(full, acc);
      continue;
    }
    if (entry.isFile() && (full.endsWith('.ts') || full.endsWith('.tsx'))) {
      acc.push(full);
    }
  }
  return acc;
}

function normalizePath(absPath) {
  return absPath.replace(`${repoRoot}/`, '');
}

function parseGlobalRoutes(globalText) {
  const map = new Map();
  const re = /export const (SCREEN_[A-Z0-9_]+)\s*=\s*"([^"]+)"/g;
  for (const match of globalText.matchAll(re)) {
    map.set(match[1], match[2]);
  }
  return map;
}

function parseRegisteredRoutes(navigationTexts, globalRoutes) {
  const routes = new Set();

  for (const navText of navigationTexts) {
    const screenRouteRe = /<(?:Stack|Tab)\.Screen[\s\S]*?name=(?:"([^"]+)"|\{Global\.(SCREEN_[A-Z0-9_]+)\})/g;
    for (const match of navText.matchAll(screenRouteRe)) {
      if (match[1]) {
        routes.add(match[1]);
      } else if (match[2]) {
        const resolved = globalRoutes.get(match[2]);
        if (resolved) {
          routes.add(resolved);
        }
      }
    }
  }

  return routes;
}

function parseTypedRootRoutes(typeText, globalRoutes) {
  const routes = new Set();
  const lines = typeText.split(/\r?\n/);

  let inBlock = false;
  let depth = 0;

  for (const line of lines) {
    if (!inBlock) {
      if (line.includes('export type RootStackParamList = {')) {
        inBlock = true;
        depth = 1;
      }
      continue;
    }

    const trimmed = line.trim();

    if (depth === 1) {
      const keyMatch = trimmed.match(/^(?:'([^']+)'|\[Global\.(SCREEN_[A-Z0-9_]+)\]|([A-Za-z][A-Za-z0-9_.]*))\s*:/);
      if (keyMatch) {
        if (keyMatch[1]) {
          routes.add(keyMatch[1]);
        } else if (keyMatch[2]) {
          const resolved = globalRoutes.get(keyMatch[2]);
          if (resolved) {
            routes.add(resolved);
          }
        } else if (keyMatch[3]) {
          routes.add(keyMatch[3]);
        }
      }
    }

    const opens = (line.match(/\{/g) || []).length;
    const closes = (line.match(/\}/g) || []).length;
    depth += opens - closes;

    if (depth <= 0) {
      break;
    }
  }

  return routes;
}

function parseNavigateTargets(files, globalRoutes) {
  const routeToFiles = new Map();
  const navRe = /\bnavigate\(\s*(?:"([^"]+)"|'([^']+)'|Global\.(SCREEN_[A-Z0-9_]+))/g;
  const configRouteRe = /\broute\s*:\s*"([A-Za-z][A-Za-z0-9.]+)"/g;

  for (const file of files) {
    const text = fs.readFileSync(file, 'utf8');
    for (const match of text.matchAll(navRe)) {
      const directRoute = match[1] || match[2];
      const globalKey = match[3];
      const route = directRoute || (globalKey ? globalRoutes.get(globalKey) : undefined);
      if (!route) {
        continue;
      }

      if (!routeToFiles.has(route)) {
        routeToFiles.set(route, new Set());
      }
      routeToFiles.get(route).add(normalizePath(file));
    }

    for (const match of text.matchAll(configRouteRe)) {
      const route = match[1];
      if (!route) {
        continue;
      }
      if (!routeToFiles.has(route)) {
        routeToFiles.set(route, new Set());
      }
      routeToFiles.get(route).add(normalizePath(file));
    }
  }

  return routeToFiles;
}

function auraRoute(routeName) {
  return (
    routeName.startsWith('Intake.') ||
    routeName.startsWith('Assessment.') ||
    routeName.startsWith('VideoDate.') ||
    routeName.startsWith('Matching.') ||
    routeName.startsWith('Growth.') ||
    routeName.startsWith('Values.') ||
    routeName.startsWith('Bridge.') ||
    routeName.startsWith('MatchWindow.') ||
    routeName.startsWith('Calendar.') ||
    routeName.startsWith('Report.') ||
    routeName.startsWith('Reputation.') ||
    routeName === 'VideoVerification' ||
    routeName === 'VideoIntro'
  );
}

function printList(label, items) {
  console.log(`\n${label} (${items.length})`);
  if (!items.length) {
    console.log('  - none');
    return;
  }
  for (const item of items) {
    console.log(`  - ${item}`);
  }
}

const globalText = readFile('alovoa-expo/Global.tsx');
const appText = readFile('alovoa-expo/App.tsx');
const mainText = readFile('alovoa-expo/screens/Main.tsx');
const typeText = readFile('alovoa-expo/myTypes.ts');
const expoFiles = walk(path.join(repoRoot, 'alovoa-expo'));

const globalRoutes = parseGlobalRoutes(globalText);
const registeredRoutes = parseRegisteredRoutes([appText, mainText], globalRoutes);
const typedRoutes = parseTypedRootRoutes(typeText, globalRoutes);
const navigateTargets = parseNavigateTargets(expoFiles, globalRoutes);
const navigateRouteSet = new Set(navigateTargets.keys());

const unknownNavigateTargets = [...navigateRouteSet]
  .filter((route) => !registeredRoutes.has(route))
  .sort();

const typedNotRegistered = [...typedRoutes]
  .filter((route) => !registeredRoutes.has(route))
  .sort();

const registeredNotTyped = [...registeredRoutes]
  .filter((route) => !typedRoutes.has(route))
  .sort();

const auraRegistered = [...registeredRoutes].filter(auraRoute).sort();
const auraWithoutNavigate = auraRegistered
  .filter((route) => !navigateRouteSet.has(route))
  .sort();

console.log('UI Wiring Validation');
console.log(`  Registered routes: ${registeredRoutes.size}`);
console.log(`  Typed routes: ${typedRoutes.size}`);
console.log(`  Navigate targets: ${navigateRouteSet.size}`);

printList('Unknown navigate targets (not registered)', unknownNavigateTargets);
printList('Typed routes missing registration', typedNotRegistered);
printList('Registered routes missing typing', registeredNotTyped);
printList('AURA routes with no navigation entrypoint', auraWithoutNavigate);

console.log('\nAURA route references');
for (const route of auraRegistered) {
  const files = [...(navigateTargets.get(route) || [])].sort();
  console.log(`  - ${route}: ${files.length} reference(s)`);
}

const hasErrors =
  unknownNavigateTargets.length > 0 ||
  typedNotRegistered.length > 0 ||
  auraWithoutNavigate.length > 0;

if (hasErrors) {
  process.exit(1);
}
