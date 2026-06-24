# Keemun graph format

A Keemun graph is stored as an **append-only JSONL log** (`keemun.jsonl`): one JSON
object per line, never rewritten, only appended. This keeps full history and makes
git merges trivial — independent work lands as separate blocks at the end of the file.

The machine-readable schema for a single line is [`keemun.schema.json`](keemun.schema.json)
(JSON Schema draft 2020-12).

## Records

Every line is one record, discriminated by `kind`. Every record except `header`
carries a `change_id`.

| `kind`   | Purpose | Key fields |
|----------|---------|-----------|
| `header` | First line; format version + project repo | `schema_version`, `repo` |
| `change` | Declares / transitions a change-set | `change_id`, `status`, `message`, `author`, `created_at` |
| `meta`   | Versioned graph metadata | `change_id`, `title`, `summary`, `authors` |
| `node`   | Full node revision | `change_id`, `id`, `type`, `title`, … |
| `edge`   | Full edge revision (first-class rationale) | `change_id`, `id`, `source`, `target`, `type`, `rationale`, … |
| `delete` | Tombstone a node/edge | `change_id`, `entity`, `id` |

A node's `type` is one of `constraint` / `decision` / `question` / `option` /
`outcome`; an edge's `type` is `constrains` / `enables` / `forbids` / `conflicts`
with a `positive` or `negative` `polarity` and a QOC-style `weight` in `0.0–1.0`.

## Change-sets

Records sharing a `change_id` form one **change-set** — "a single unit of design".
On disk they are written as a contiguous block: the `change` line first, then its
`meta` / `node` / `edge` / `delete` lines. A change-set has a review `status`:

- `proposed` — staged for review (e.g. an LLM- or human-authored edit);
- `accepted` — part of the canonical graph;
- `rejected` — recorded but ignored.

Because the log is append-only, a status transition is **just another `change`
line**: proposing then later accepting appends `{"kind":"change","change_id":"…","status":"accepted"}`.
`change` records merge field-by-field across lines (status: last wins; message /
author / created_at: last non-null wins). `node` / `edge` / `meta` records are
full-state replaces — a new row is a new full version of that entity.

## Projection

The **current graph** is the projection (fold) of all `accepted` change-sets, in
append order:

1. `node` / `edge` → upsert by `id` (last write wins);
2. `delete` → remove by `id`;
3. `meta` → replace metadata.

History views fold the accepted change-sets up to a chosen point; previewing a
proposed change folds the accepted graph plus that one change. The web UI and the
Kotlin core implement the same projection.

## Working with the log

```sh
keemun init                       # create keemun.jsonl with a sample graph
keemun render                     # write keemun.html (current graph + history UI)
keemun serve                      # editable graph + review/authoring over HTTP
keemun propose --json '{"message":"…","records":[{"kind":"node",…}]}'
keemun log                        # list change-sets and their status
keemun accept change-0002         # fold a proposed change-set into the graph
keemun reject change-0002
keemun import --from old-keemun.json   # migrate a legacy single-file graph
```

Proposals (CLI `propose`, `POST /api/changes`) may omit `change_id` on each record;
it is stamped from the change-set. The current graph is always validated when a
change is accepted, so the accepted projection stays consistent.
