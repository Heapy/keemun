# Keemun

An ADR-like decision graph that lets coding agents propose architecture changes during the plan stage.

_Authors: heapy_

_18 nodes · 19 edges · 4 change-sets — this view reflects accepted change-sets only._

## Change history

| # | Change | Status | Author | Contents | Summary |
| --- | --- | --- | --- | --- | --- |
| 1 | `change-0001` | accepted | — | 6 nodes, 4 edges | Foundations: agent planning, Kotlin/Multiplatform, native CLI, property graph |
| 2 | `change-0002` | accepted | codex | 8 nodes, 7 edges | Append-only JSONL change-set log, review workflow, HTTP API, HTML review; drop Cosmograph |
| 3 | `change-0003` | accepted | claude | 3 nodes, 6 edges | Markdown render engine, force-directed SVG layout, and agent skills |
| 4 | `change-0004` | accepted | agent | 1 node, 2 edges | Install agent skill command |

## Nodes

### Decisions

- **Ship keemun skills for Claude Code and Codex** (`agent-skills`) — A keemun usage skill plus a run-keemun build skill teach agents the review/propose/apply loop.  `agent` `skills`
- **Reviewable change-sets (proposed / accepted / rejected)** (`change-sets`) — Records sharing a change_id form one unit of design; the current graph is the projection of accepted change-sets.  `workflow`
- **Use Clikt for the command surface** (`clikt-cli`) — A scriptable subcommand CLI is the primary interface for agents and humans.  `cli`
- **Keep one SVG renderer; drop the WebGL engine** (`drop-cosmograph`) — Remove the Cosmograph engine to avoid maintaining two render paths.  `renderer`
- **Interactive HTML with change timeline and authoring** (`html-review`) — A self-contained page renders the graph plus a timeline of change-sets for human review.  `human` `renderer`
- **Import legacy single-file JSON graphs** (`import-legacy`) — A migration path folds an old single-file graph into a new JSONL log.  `migration`
- **Install agent skills from the CLI** (`install-skill-command`) — Expose `keemun install skill` to place the bundled keemun usage skill into Codex or Claude Code project/global skill directories.  `agent` `cli` `skills`
- **Append-only JSONL log as source of truth** (`jsonl-log`) — Every line is a record; the graph is never edited in place, only appended.  `model` `storage`
- **Target Kotlin/Multiplatform (JVM + native)** (`kmp`) — One codebase compiled for the JVM and for native macOS/Linux targets.  `architecture`
- **Markdown render format for agents** (`markdown-render`) — Render any projection to agent-readable Markdown so an agent reads the architecture before proposing.  `agent` `renderer`
- **Model architecture as a property graph** (`property-graph`) — Decisions, constraints, questions, options, and outcomes are nodes; edges carry first-class rationale.  `model`
- **Serve an editable graph and review API over HTTP** (`serve-api`) — Ktor CIO exposes /api/graph, /api/log, and accept/reject endpoints.  `server`
- **Ship as a self-contained native CLI** (`single-binary`) — Users run a single native executable with no JVM runtime required.  `distribution`
- **Force-directed SVG layout** (`svg-force-layout`) — Constant-size nodes, connectivity-ordered seed, and grid-accelerated repulsion so connected nodes sit close and the field stays coarse.  `layout` `renderer`

### Constraints

- **Agents propose architecture during planning** (`agent-planning`) — The tool must let an agent read, propose, and revise architecture decisions as part of the plan stage.  `product`
- **Kotlin is the implementation language** (`kotlin-language`) _(external)_ — The project is written in Kotlin, so tooling and serialization should be Kotlin-native.  `external`

### Options

- **WebGL Cosmograph renderer** (`cosmograph-engine`) _(rejected)_ — A second, GPU-based rendering engine for very large graphs.  `alternative`
- **Single mutable JSON file** (`single-json-file`) _(rejected)_ — Store the whole graph in one file edited in place.  `alternative`

## Rationale (edges)

- **Reviewable change-sets (proposed / accepted / rejected)** —enables→ **Interactive HTML with change timeline and authoring**  ·  `change-sets` → `html-review`  ·  weight 0.85
  The timeline visualizes proposed versus accepted change-sets for human review.
- **Reviewable change-sets (proposed / accepted / rejected)** —enables→ **Markdown render format for agents**  ·  `change-sets` → `markdown-render`  ·  weight 0.80
  An agent reads a proposed change-set as Markdown before deciding what to do.
- **Reviewable change-sets (proposed / accepted / rejected)** —enables→ **Serve an editable graph and review API over HTTP**  ·  `change-sets` → `serve-api`  ·  weight 0.70
  Reviewers need a live surface to accept or reject proposals.
- **WebGL Cosmograph renderer** —conflicts→ **Keep one SVG renderer; drop the WebGL engine**  ·  `cosmograph-engine` → `drop-cosmograph`  ·  weight 0.80, negative
  A second WebGL engine duplicated the renderer with little added value.
- **Model architecture as a property graph** —enables→ **Interactive HTML with change timeline and authoring**  ·  `property-graph` → `html-review`  ·  weight 0.80
  The property graph is exactly what the HTML view renders and navigates.
- **Model architecture as a property graph** —enables→ **Markdown render format for agents**  ·  `property-graph` → `markdown-render`  ·  weight 0.85
  Markdown is a projection of the same property graph, shaped for agent reading.
- **Interactive HTML with change timeline and authoring** —enables→ **Ship keemun skills for Claude Code and Codex**  ·  `html-review` → `agent-skills`  ·  weight 0.70
  The skill routes the HTML view to the human for approval.
- **Append-only JSONL log as source of truth** —enables→ **Reviewable change-sets (proposed / accepted / rejected)**  ·  `jsonl-log` → `change-sets`  ·  weight 0.90
  An append-only history makes every proposal an auditable, revertible unit.
- **Target Kotlin/Multiplatform (JVM + native)** —enables→ **Ship as a self-contained native CLI**  ·  `kmp` → `single-binary`  ·  weight 0.90
  Kotlin/Native compiles to a standalone executable with no JVM runtime for users.
- **Kotlin is the implementation language** —enables→ **Target Kotlin/Multiplatform (JVM + native)**  ·  `kotlin-language` → `kmp`  ·  weight 0.85
  A fixed Kotlin codebase makes Kotlin/Multiplatform the natural way to reach every target.
- **Markdown render format for agents** —enables→ **Ship keemun skills for Claude Code and Codex**  ·  `markdown-render` → `agent-skills`  ·  weight 0.70
  The skill tells agents to read the Markdown view first.
- **Agents propose architecture during planning** —enables→ **Reviewable change-sets (proposed / accepted / rejected)**  ·  `agent-planning` → `change-sets`  ·  weight 0.90
  Reviewable units let an agent propose changes a human can approve or reject.
- **Agents propose architecture during planning** —enables→ **Use Clikt for the command surface**  ·  `agent-planning` → `clikt-cli`  ·  weight 0.70
  Agents drive the tool programmatically, so a scriptable command surface is required.
- **Agents propose architecture during planning** —enables→ **Model architecture as a property graph**  ·  `agent-planning` → `property-graph`  ·  weight 0.90
  A queryable graph lets an agent reason about architecture the way it reasons about code.
- **Agents propose architecture during planning** —enables→ **Ship keemun skills for Claude Code and Codex**  ·  `agent-planning` → `agent-skills`  ·  weight 0.80
  Skills encode the plan-stage workflow agents follow.
- **Single mutable JSON file** —conflicts→ **Append-only JSONL log as source of truth**  ·  `single-json-file` → `jsonl-log`  ·  weight 0.90, negative
  A single mutable file cannot represent reviewable, append-only history.
- **Force-directed SVG layout** —enables→ **Interactive HTML with change timeline and authoring**  ·  `svg-force-layout` → `html-review`  ·  weight 0.80
  The force layout is the SVG the human-review HTML draws.
- **Ship keemun skills for Claude Code and Codex** —enables→ **Install agent skills from the CLI**  ·  `agent-skills` → `install-skill-command`  ·  weight 0.85
  Bundled agent skills need a first-class installer so the native binary can place them where Codex and Claude discover project or global skills.
- **Use Clikt for the command surface** —enables→ **Install agent skills from the CLI**  ·  `clikt-cli` → `install-skill-command`  ·  weight 0.80
  The existing scriptable Clikt command surface lets agents and humans install skills without copying files by hand.
