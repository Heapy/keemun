---
name: run-keemun
description: Build, run, and screenshot the keemun decision-graph CLI. Use when asked to run/start/build/test keemun, render or generate a graph, serve the editable graph over HTTP, or screenshot/verify the SVG (or Cosmograph) renderer output.
---

# Run keemun

keemun is a Kotlin/Multiplatform CLI (`io.heapy.keemun`) that turns a property-graph
JSON into a **self-contained interactive HTML page** (force-directed SVG graph, or the
Cosmograph engine) and can `serve` an editable graph over HTTP. Most changes land in the
renderers (`app/src/main/kotlin/io/heapy/keemun/HtmlRenderer.kt`,
`CosmographHtmlRenderer.kt`), so the surface that matters is the **rendered HTML in a real
browser**.

You drive it with the committed Node driver, which runs the CLI through Gradle (so it
compiles current sources), screenshots rendered HTML with the system's **headless
Chrome**, and smoke-tests `serve` with `fetch()`. No npm install — Node built-ins only.

All paths below are relative to the repo root (`<unit>/`). The driver lives at
`.claude/skills/run-keemun/driver.mjs`.

## Prerequisites

- **JDK 25** (the build pins `jvmToolchain(25)`). Verified with:
  ```bash
  java -version   # openjdk 25 (Corretto 25.0.1 here)
  ```
- **Node** (built-in `fetch`, ES modules — v18+; tested on v24).
- **Headless-capable Chromium browser.** On this macOS box the driver auto-finds
  `/Applications/Google Chrome.app`. Override with `CHROME=/path/to/chrome` if needed.
- No `apt-get` needed (this is macOS). On Linux, install a `chromium` package and set
  `CHROME`.

## Run (agent path) — the driver

First run compiles Kotlin via Gradle (~tens of seconds, cached after).

```bash
# Render + screenshot all three sample sizes (5 / 250 / 1000 nodes).
# Screenshots land in build/run-shots/ (gitignored).
node .claude/skills/run-keemun/driver.mjs all
```

```bash
# Render one size to keemun-<n>.html (generates keemun-<n>.json if missing).
node .claude/skills/run-keemun/driver.mjs render 250
```

```bash
# Screenshot any rendered HTML. Third arg selects a node via the URL hash
# (the page auto-selects from location.hash — no clicking needed).
node .claude/skills/run-keemun/driver.mjs shot keemun-5.html build/run-shots/sel.png "#node-0003"
```

```bash
# Smoke-test the HTTP server: starts `serve`, hits GET /api/graph, GET /,
# and PUT /api/graph, then kills the JVM. Args: [json] [port].
node .claude/skills/run-keemun/driver.mjs serve-smoke keemun-5.json 8099
```

**Always open the screenshot and look at it** — a blank or error frame means the layout
didn't settle (see Gotchas). Expected good frame: colored node dots, grey/red arrowed
edges, a selected node with a populated right-hand detail panel.

## Run (raw CLI / direct invocation)

The driver wraps these; use them directly when iterating on the Kotlin:

```bash
./gradlew :app:run --args="generate --file keemun-250.json --nodes 250 --force"
./gradlew :app:run --args="render --file keemun-250.json --out keemun-250.html --engine svg"
./gradlew :app:run --args="serve --file keemun-250.json --host 127.0.0.1 --port 8099"
```

Subcommands: `init`, `generate`, `render` (`--engine svg|cosmograph`), `serve`,
`describe <node-id>`, `validate`. Compile-check only:

```bash
./gradlew :app:compileKotlinJvm
```

## Run (human path)

`./gradlew :app:run --args="serve ..."` then open `http://127.0.0.1:8099/` in a browser
and Ctrl-C to stop. Or `render` and open the `.html` with `open keemun-250.html`. Useless
headless — use the driver instead.

## Gotchas

- **`./gradlew :app:compileKotlin` is ambiguous** — this is a multiplatform project. Use
  `:app:compileKotlinJvm` (or just `:app:run`, the JVM path). Native targets
  (`linuxX64`, `linuxArm64`, `macosArm64`) exist but aren't needed to run.
- **Use `:app:run` (JVM), not the `dist/` binary, to verify changes.** `dist/keemun…` is a
  prebuilt native macOS binary and may be stale; `:app:run` compiles current sources.
- **Screenshots need settle time.** The SVG runs a force simulation on page load *plus* a
  ~420ms fit animation. The driver passes `--virtual-time-budget=3500`; too low → a blank
  or mid-animation frame.
- **Select a node without clicking:** load `file://…/keemun-N.html#<nodeId>`. The page
  reads `location.hash` and selects that node (panel + focused neighborhood + labels). The
  default view already auto-selects `nodes[0]`, so screenshotting `#node-0001` looks
  identical to the default — pass a *different* id to see selection change.
- **Labels are reveal-on-zoom for graphs >250 nodes** — at the fit/overview they only show
  for the selected node's neighborhood. A 1000-node overview with most labels hidden is
  correct, not a bug.
- **`serve` via Gradle spawns a JVM child.** Killing the Gradle process alone leaves it
  running; the driver kills the whole process group and `pkill -f io.heapy.keemun`. If you
  start it by hand, stop it the same way.
- **Harmless Chrome stderr noise:** `task_policy_set TASK_SUPPRESSION_POLICY`,
  `Trying to load the allocator multiple times`, and a Google-Drive `installwebapp` install
  error all print to stderr and do not affect the screenshot.

## Troubleshooting

- `No Chrome found` → set `CHROME=/path/to/chrome`.
- Gradle "Ambiguous matches … compileKotlin" → use `:app:compileKotlinJvm`.
- `serve-smoke` "server did not come up" → first compile is slow; the driver already waits
  60s, but a cold Gradle cache plus download can exceed it — re-run once warm.
- Blank/partial screenshot → raise `--virtual-time-budget` in `driver.mjs` (`shot()`).
