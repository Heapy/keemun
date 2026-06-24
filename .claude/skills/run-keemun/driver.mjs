#!/usr/bin/env node
// Driver for the keemun decision-graph CLI.
//
// keemun is a Kotlin/Multiplatform CLI that turns a property-graph JSON into a
// self-contained interactive HTML page (SVG or Cosmograph engine) and can also
// `serve` an editable graph over HTTP. Most changes land in the renderers
// (app/src/main/kotlin/io/heapy/keemun/*Renderer.kt), so the interesting
// surface is the rendered HTML in a real browser.
//
// This driver uses ONLY Node built-ins — no npm install. It:
//   * runs the CLI through Gradle (`:app:run`) so it always compiles current
//     sources (the prebuilt dist/ binary may be stale),
//   * screenshots rendered HTML with the system's headless Chrome,
//   * smoke-tests the `serve` HTTP mode with the built-in fetch().
//
// Usage (run from anywhere; paths resolve against the repo root):
//   node .claude/skills/run-keemun/driver.mjs render [nodes]        # default 250
//   node .claude/skills/run-keemun/driver.mjs shot <html> [out.png] [#nodeId]
//   node .claude/skills/run-keemun/driver.mjs serve-smoke [json] [port]
//   node .claude/skills/run-keemun/driver.mjs all                   # render+shot 5/250/1000
//
// Override the browser with CHROME=/path/to/chrome.

import { execFileSync, spawn } from 'node:child_process'
import { existsSync, mkdirSync, readFileSync } from 'node:fs'
import { dirname, resolve, join, basename } from 'node:path'
import { fileURLToPath } from 'node:url'
import { setTimeout as sleep } from 'node:timers/promises'

const HERE = dirname(fileURLToPath(import.meta.url))
const ROOT = resolve(HERE, '../../..')        // <repo>/.claude/skills/run-keemun -> <repo>
const SHOTS = join(ROOT, 'build', 'run-shots') // build/ is gitignored
const GRADLEW = join(ROOT, process.platform === 'win32' ? 'gradlew.bat' : 'gradlew')

const CHROME =
  process.env.CHROME ||
  [
    '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
    '/Applications/Chromium.app/Contents/MacOS/Chromium',
    '/Applications/Microsoft Edge.app/Contents/MacOS/Microsoft Edge',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium',
    '/usr/bin/chromium-browser',
  ].find(existsSync)

// Run a keemun subcommand via Gradle and return its stdout (clikt `echo`).
// Gradle/JNA noise is filtered out so the caller sees just the CLI output.
function cli(argString) {
  console.error(`\n$ keemun ${argString}`)
  const out = execFileSync(GRADLEW, [':app:run', `--args=${argString}`, '-q', '--console=plain'], {
    cwd: ROOT,
    encoding: 'utf8',
    maxBuffer: 64 * 1024 * 1024,
    stdio: ['ignore', 'pipe', 'pipe'],
  })
  const clean = out
    .split('\n')
    .filter((l) => !/FSEvents|WARNING|native-access|restricted method|System::load/i.test(l))
    .join('\n')
    .trim()
  if (clean) console.error(clean)
  return clean
}

function shot(html, out, hash) {
  if (!CHROME) throw new Error('No Chrome found. Set CHROME=/path/to/chrome')
  const abs = resolve(ROOT, html)
  if (!existsSync(abs)) throw new Error(`No such HTML file: ${abs}`)
  mkdirSync(SHOTS, { recursive: true })
  const outPath = out
    ? resolve(ROOT, out)
    : join(SHOTS, basename(abs).replace(/\.html$/, '') + (hash ? `-${hash}` : '') + '.png')
  const url = 'file://' + abs + (hash ? '#' + encodeURIComponent(hash) : '')
  // --virtual-time-budget lets the on-load force layout + fit animation settle
  // before the frame is captured.
  execFileSync(
    CHROME,
    [
      '--headless=new',
      '--disable-gpu',
      '--hide-scrollbars',
      '--force-device-scale-factor=2',
      '--virtual-time-budget=3500',
      '--window-size=1600,1000',
      `--screenshot=${outPath}`,
      url,
    ],
    { stdio: ['ignore', 'ignore', 'inherit'] },
  )
  console.error(`  -> ${outPath}`)
  return outPath
}

function firstNodeId(jsonRelPath) {
  try {
    const g = JSON.parse(readFileSync(resolve(ROOT, jsonRelPath), 'utf8'))
    return g.nodes?.[0]?.id
  } catch {
    return undefined
  }
}

function render(nodes) {
  const json = `keemun-${nodes}.json`
  const html = `keemun-${nodes}.html`
  if (!existsSync(resolve(ROOT, json))) cli(`generate --file ${json} --nodes ${nodes} --force`)
  cli(`render --file ${json} --out ${html} --engine svg`)
  return { json, html }
}

async function serveSmoke(file = 'keemun-5.json', port = 8099) {
  if (!existsSync(resolve(ROOT, file))) render(file.match(/\d+/)?.[0] || 5)
  console.error(`\nStarting: keemun serve --file ${file} --port ${port}`)
  const proc = spawn(GRADLEW, [':app:run', `--args=serve --file ${file} --host 127.0.0.1 --port ${port}`, '-q', '--console=plain'], {
    cwd: ROOT,
    detached: true, // own process group, so we can kill the JVM child too
    stdio: 'ignore',
  })
  const base = `http://127.0.0.1:${port}`
  try {
    let up = false
    for (let i = 0; i < 60; i++) {
      try {
        const r = await fetch(`${base}/api/graph`)
        if (r.ok) { up = true; break }
      } catch {}
      await sleep(1000)
    }
    if (!up) throw new Error('server did not come up within 60s')
    console.error('  server up')

    const graphRes = await fetch(`${base}/api/graph`)
    const graph = await graphRes.json()
    console.error(`  GET /api/graph -> ${graphRes.status}, ${graph.nodes.length} nodes, ${graph.edges.length} edges`)

    const htmlRes = await fetch(`${base}/`)
    const htmlText = await htmlRes.text()
    const title = htmlText.match(/<title>([^<]*)<\/title>/)?.[1]
    console.error(`  GET / -> ${htmlRes.status}, <title>${title}</title>, ${htmlText.length} bytes`)

    // Round-trip the graph back through PUT to exercise the editor write path.
    const putRes = await fetch(`${base}/api/graph`, {
      method: 'PUT',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify(graph),
    })
    console.error(`  PUT /api/graph -> ${putRes.status}`)
    if (!graphRes.ok || !htmlRes.ok || !putRes.ok) throw new Error('a request failed')
    console.error('  serve-smoke OK')
  } finally {
    try { process.kill(-proc.pid, 'SIGKILL') } catch {}
    try { execFileSync('pkill', ['-f', 'io.heapy.keemun']) } catch {}
  }
}

async function main() {
  const [cmd, ...rest] = process.argv.slice(2)
  switch (cmd) {
    case 'render': {
      const { html } = render(rest[0] || 250)
      console.error(`rendered ${html}`)
      break
    }
    case 'shot':
      shot(rest[0], rest[1], rest[2]?.replace(/^#/, ''))
      break
    case 'serve-smoke':
      await serveSmoke(rest[0], rest[1] ? Number(rest[1]) : undefined)
      break
    case 'all': {
      for (const n of [5, 250, 1000]) {
        const { json, html } = render(n)
        shot(html) // default (auto-selected first node) view
        const id = firstNodeId(json)
        if (id) shot(html, undefined, id) // explicit selection via URL hash
      }
      console.error(`\nScreenshots in ${SHOTS}`)
      break
    }
    default:
      console.error(readFileSync(fileURLToPath(import.meta.url), 'utf8').split('\n').slice(15, 30).join('\n'))
      process.exit(1)
  }
}

main().catch((e) => {
  console.error('ERROR:', e.message)
  process.exit(1)
})
