---
name: keemun
description: Review and evolve a project's architecture decisions with the keemun CLI. Use when planning a structural/architecture change, recording an ADR, proposing a design decision, reviewing the decision graph, or accepting/rejecting a proposed change-set. Triggers include "propose an architecture change", "record this decision", "review the architecture", "what decisions exist", "apply/reject the proposal".
---

# Using keemun

keemun records a project's **architecture decisions as a graph** and makes
changes to it reviewable. Use it during the **plan** stage: read the current
architecture, propose a change as a reviewable change-set, let the human see it,
then apply or drop it — all from the CLI, leaving a durable, versioned trail of
*why*.

This skill is for **using** the `keemun` binary in a project. (To build keemun
itself from source, see the separate `run-keemun` skill.)

## Model in one minute

- The graph lives in an **append-only JSONL log** (default `keemun.jsonl`). You
  never edit it by hand — every command appends.
- Every line carries a `change_id`; lines sharing one form a **change-set** — a
  single unit of design with a review status: **proposed → accepted / rejected**.
- The **current architecture is the projection of accepted change-sets** (last
  write wins; a `delete` record is a tombstone). Proposed/rejected sets don't
  count until accepted.
- **Nodes**: `decision`, `constraint`, `question`, `option`, `outcome`.
  **Edges** carry first-class rationale; type is `constrains`, `enables`,
  `forbids`, or `conflicts`, with a `weight` in `0.0–1.0`.

## Two render formats, two audiences

| Format | Command | Who reads it |
| --- | --- | --- |
| **Markdown** | `keemun render --format markdown --out keemun.md` | **you, the agent** — read it to understand the architecture before proposing |
| **HTML** | `keemun render --out keemun.html` | **the human** — an interactive graph to review a proposal visually |

## The workflow

Follow these five steps when the user asks to plan or record an architecture change.

**1. Read the current architecture (markdown).** Generate the agent-readable view
and read it, so your proposal builds on what already exists instead of duplicating it.

```bash
keemun render --file keemun.jsonl --format markdown --out keemun.md
# then read keemun.md; also: keemun log    (list change-sets and their status)
```

**2. Review and decide the change.** Identify the decisions/constraints/edges to
add, revise, or delete. Reuse existing node ids when revising (a new record for an
existing id is a new full revision). Keep ids stable and meaningful.

**3. Propose the change-set.** Write the change as a JSON payload and append it as
a *proposed* set (it does not affect the current graph until accepted):

```bash
keemun propose --file keemun.jsonl --input change.json \
  --message "Adopt event sourcing for the ledger" --author agent
# prints e.g. "Proposed change-0002 (3 records)"
```

`change.json` (see schema below):

```json
{
  "records": [
    {"kind":"node","id":"constraint.audit","type":"constraint","title":"Full financial audit trail","summary":"Regulators require every balance change be reconstructable."},
    {"kind":"node","id":"decision.event-sourcing","type":"decision","title":"Adopt event sourcing for the ledger","summary":"Persist ledger state as an append-only event log."},
    {"kind":"edge","id":"edge.audit-enables-es","source":"constraint.audit","target":"decision.event-sourcing","type":"enables","rationale":"An append-only event log reconstructs any historical balance, satisfying audit.","weight":0.9}
  ]
}
```

**4. Render HTML and hand it to the human.** Produce the visual view and ask the
user to review the proposed change-set (it shows up in the timeline as *proposed*).

```bash
keemun render --file keemun.jsonl --out keemun.html
# Tell the user: "Open keemun.html and review change-0002."
# Or run a live, reviewable server: keemun serve --file keemun.jsonl
```

**5. Apply the verdict.** When the user approves, accept it (it now folds into the
current architecture). If they decline, reject it (kept in history, never counted).

```bash
keemun accept change-0002 --author maintainer    # approve → becomes current
keemun reject change-0002 --author maintainer     # decline → archived
keemun validate                                    # confirm the log is well-formed
```

> Only **accept** after the human approves. The CLI is how you apply their verdict;
> the decision is theirs.

## Change-set payload schema

`propose` takes a `ChangeProposal`: optional `change_id` (auto-assigned
`change-NNNN` if omitted), optional `message` / `author`, and a `records` array.
Each record uses a `"kind"` discriminator:

- **node** — `{"kind":"node","id","type","title","summary?","status?","tags?":[],"external?":false}`
  - `type`: `decision｜constraint｜question｜option｜outcome`
  - `status`: `accepted｜proposed｜rejected｜deprecated` (default `accepted`)
- **edge** — `{"kind":"edge","id","source","target","type","rationale","weight?":1.0,"polarity?","order?","decided_at?","criteria?":[]}`
  - `type`: `constrains｜enables｜forbids｜conflicts`; `polarity`: `positive｜negative`
  - `rationale` is **required**; `weight` must be `0.0–1.0`; `decided_at` is `YYYY-MM-DD`
- **delete** — `{"kind":"delete","entity":"node"|"edge","id":"..."}` (tombstone)
- **meta** — `{"kind":"meta","title","summary?","authors?":[]}` (replaces graph metadata)

Rules enforced on propose (a failing proposal is rejected, not written):
- ids must match `^[a-z0-9][a-z0-9._:-]*$` (e.g. `decision.event-sourcing`).
- every edge `source`/`target` must resolve within the resulting projection.
- don't reuse an existing `change_id`; omit it to auto-number.

## Command reference

| Command | Purpose |
| --- | --- |
| `keemun init [--file f]` | Create a new log seeded with a sample graph |
| `keemun import --from old.json [--file f]` | Import a legacy single-file JSON graph into a log |
| `keemun render --format markdown --out f.md` | **Agent-readable** markdown of the current graph |
| `keemun render --out f.html` | **Human-reviewable** interactive HTML |
| `keemun serve [--file f] [--port 8080]` | Live editable graph + review API over HTTP |
| `keemun log [--file f]` | List change-sets: seq, id, status, message, counts |
| `keemun describe <node-id> [--from <id>]` | A node and its rationale trace |
| `keemun propose --input c.json [--id] [--message] [--author]` | Append a *proposed* change-set |
| `keemun accept <change-id> [--author]` | Approve a change-set → folds into current |
| `keemun reject <change-id> [--author]` | Decline a change-set → archived |
| `keemun validate [--file f]` | Check the log projects to a valid graph |

`--file` defaults to `keemun.jsonl`; `propose` also accepts `--json '<text>'`
instead of `--input <file>`. The HTTP API mirrors the CLI:
`GET /api/graph`, `GET /api/log`, `POST /api/changes`,
`POST /api/changes/{id}/accept`, `POST /api/changes/{id}/reject`.
