# Agent guide

This project ships **keemun**, a CLI for recording architecture decisions as a
reviewable graph. When you plan or record an architecture/design change, use the
keemun workflow rather than scattering decisions across prose.

## keemun skill (Claude Code & Codex)

The full command reference, change-set schema, and workflow live in
[`.claude/skills/keemun/SKILL.md`](.claude/skills/keemun/SKILL.md). Read it before
proposing changes. Claude Code auto-loads it as the `keemun` skill; for Codex,
treat that file as the canonical instructions.

### The five-step loop

1. **Read** the architecture (agent format): `keemun render --format markdown --out keemun.md`, then read it (and `keemun log` for change-set status).
2. **Review** and decide what to add/revise/delete, reusing stable node ids.
3. **Propose** a reviewable change-set: `keemun propose --input change.json --message "..." --author agent` (appends as *proposed*; does not affect the current graph).
4. **Render HTML for the human**: `keemun render --out keemun.html` and ask them to review the proposed change-set.
5. **Apply their verdict**: `keemun accept <change-id>` once approved, or `keemun reject <change-id>` — never accept before the human approves.

Render formats: **markdown** is for you (the agent) to read; **HTML** is for the
human to review. The current architecture is the projection of *accepted*
change-sets only.

> Building keemun from source (not using it) is covered by the separate
> `run-keemun` skill under `.claude/skills/run-keemun/`.
