package io.heapy.keemun

class HtmlRenderer {
    fun render(log: GraphLog, editable: Boolean = false): String {
        val view = log.view()
        val logJson = GraphJson.encodeLogView(view).scriptSafe()
        val title = log.current().metadata.title
        val editableFlag = if (editable) "true" else "false"
        return page(title, logJson, editableFlag)
    }

    private fun page(title: String, logJson: String, editableFlag: String): String =
        """
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${title.escapeHtml()}</title>
  <style>
$STYLE
  </style>
</head>
<body>
  <div class="app">
    <main class="workspace">
      <header>
        <div>
          <h1 id="graph-title">Keemun</h1>
          <p class="summary" id="graph-summary"></p>
        </div>
        <div class="toolbar">
          <input id="search" class="search" type="search" placeholder="Search nodes">
          <button id="fit-button" type="button">Fit</button>
        </div>
      </header>
      <svg id="graph-svg" role="img" aria-label="Keemun decision graph"></svg>
      <footer id="timeline" aria-label="Change history"></footer>
    </main>
    <aside>
      <section id="change-panel" class="panel-block"></section>
      <section id="author-panel" class="panel-block editor"></section>
      <section id="details" class="panel-block"></section>
      <section class="panel-block">
        <p class="eyebrow">Generated rationale</p>
        <pre id="trace">Select a node to generate the path rationale.</pre>
      </section>
    </aside>
  </div>
  <script type="application/json" id="log-data">$logJson</script>
  <script>
    const editorEnabled = $editableFlag;
$SCRIPT
  </script>
</body>
</html>
""".trimIndent()

    private fun String.scriptSafe(): String =
        replace("<", "\\u003c")

    private fun String.escapeHtml(): String =
        buildString(length) {
            this@escapeHtml.forEach { char ->
                append(
                    when (char) {
                        '&' -> "&amp;"
                        '<' -> "&lt;"
                        '>' -> "&gt;"
                        '"' -> "&quot;"
                        '\'' -> "&#39;"
                        else -> char
                    },
                )
            }
        }

    private companion object {
        private val STYLE = """
    :root {
      color-scheme: light;
      --bg: #f6f7f9;
      --panel: #ffffff;
      --panel-strong: #eef1f4;
      --ink: #172026;
      --muted: #5b6770;
      --line: #d8dee4;
      --accent: #0f766e;
      --constraint: #6554c0;
      --decision: #0f766e;
      --option: #b45309;
      --question: #2563eb;
      --outcome: #475569;
      --negative: #b42318;
      --proposed: #b45309;
      --accepted: #0f766e;
      --rejected: #94a3b8;
      font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
    }
    * { box-sizing: border-box; }
    body {
      margin: 0;
      background: var(--bg);
      color: var(--ink);
      height: 100vh;
      overflow: hidden;
    }
    .app {
      display: grid;
      grid-template-columns: minmax(0, 1fr) 400px;
      height: 100vh;
      overflow: hidden;
    }
    .workspace {
      position: relative;
      display: flex;
      flex-direction: column;
      height: 100vh;
      overflow: hidden;
      overscroll-behavior: contain;
    }
    header {
      flex: 0 0 auto;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 16px 24px;
      z-index: 4;
      background: rgba(246, 247, 249, 0.92);
      border-bottom: 1px solid rgba(216, 222, 228, 0.74);
      backdrop-filter: blur(10px);
    }
    h1 { margin: 0; font-size: 22px; line-height: 1.2; }
    .summary { margin: 4px 0 0; color: var(--muted); font-size: 14px; max-width: 620px; }
    .toolbar { display: flex; gap: 8px; flex: 0 0 auto; }
    input, textarea, button, select { font: inherit; }
    .search {
      width: min(280px, 40vw);
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 9px 10px;
      background: #fff;
      color: var(--ink);
    }
    button {
      border: 1px solid var(--line);
      background: #fff;
      color: var(--ink);
      border-radius: 6px;
      padding: 8px 10px;
      cursor: pointer;
    }
    button:hover { border-color: #aab4be; }
    button.primary { background: var(--accent); color: #fff; border-color: var(--accent); }
    button.danger { color: var(--negative); border-color: #e6b4ad; }
    button.small { padding: 4px 8px; font-size: 12px; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
    #graph-svg {
      flex: 1 1 auto;
      display: block;
      width: 100%;
      min-height: 0;
      cursor: grab;
      touch-action: none;
    }
    #graph-svg.panning { cursor: grabbing; }
    #timeline {
      flex: 0 0 auto;
      display: flex;
      align-items: center;
      gap: 6px;
      padding: 8px 12px;
      overflow-x: auto;
      border-top: 1px solid var(--line);
      background: var(--panel);
    }
    .tl-chip {
      flex: 0 0 auto;
      display: inline-flex;
      align-items: center;
      gap: 6px;
      border: 1px solid var(--line);
      border-radius: 999px;
      padding: 5px 11px;
      font-size: 12px;
      cursor: pointer;
      background: #fff;
      white-space: nowrap;
    }
    .tl-chip .dot {
      width: 9px; height: 9px; border-radius: 50%;
      background: var(--accepted); flex: 0 0 auto;
    }
    .tl-chip.proposed .dot { background: var(--proposed); }
    .tl-chip.rejected .dot { background: var(--rejected); }
    .tl-chip.rejected { color: var(--muted); text-decoration: line-through; }
    .tl-chip.viewing { border-color: var(--ink); box-shadow: 0 0 0 1px var(--ink) inset; }
    .tl-chip.dim { opacity: 0.55; }
    .tl-spacer { flex: 1 1 auto; }
    aside {
      height: 100vh;
      border-left: 1px solid var(--line);
      background: var(--panel);
      padding: 18px;
      overflow: auto;
      overscroll-behavior: contain;
    }
    .panel-block { border-top: 1px solid var(--line); padding-top: 14px; margin-top: 14px; }
    .panel-block:first-child { border-top: 0; margin-top: 0; padding-top: 0; }
    .eyebrow { color: var(--muted); font-size: 12px; text-transform: uppercase; letter-spacing: 0.08em; margin: 0 0 8px; }
    h2 { margin: 0 0 8px; font-size: 19px; line-height: 1.25; }
    h3 { margin: 0 0 6px; font-size: 14px; }
    .muted { color: var(--muted); }
    .row { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .row.spread { justify-content: space-between; }
    .badge {
      display: inline-flex; align-items: center; gap: 5px;
      border-radius: 999px; padding: 3px 9px; font-size: 12px;
      border: 1px solid var(--line); background: var(--panel-strong); color: var(--muted);
    }
    .badge .dot { width: 8px; height: 8px; border-radius: 50%; background: var(--accepted); }
    .badge.proposed .dot { background: var(--proposed); }
    .badge.rejected .dot { background: var(--rejected); }
    .pill-row { display: flex; gap: 6px; flex-wrap: wrap; margin-top: 12px; }
    .pill { border: 1px solid var(--line); border-radius: 999px; padding: 4px 8px; font-size: 12px; color: var(--muted); background: var(--panel-strong); }
    .edge-list { display: grid; gap: 8px; margin-top: 10px; }
    .edge-button { width: 100%; text-align: left; background: #fff; border: 1px solid var(--line); border-radius: 6px; padding: 9px; line-height: 1.35; }
    .edge-button strong { display: block; color: var(--ink); margin-bottom: 2px; }
    .diff { display: grid; gap: 4px; margin-top: 8px; }
    .diff-item { font-size: 13px; padding: 4px 8px; border-radius: 5px; border: 1px solid var(--line); }
    .diff-item.added { border-color: #b7e0c8; background: #ecfdf3; }
    .diff-item.removed { border-color: #e6b4ad; background: #fef3f2; }
    .diff-item.modified { border-color: #cdd6e6; background: #eef2fb; }
    .diff-item .k { font-weight: 600; text-transform: uppercase; font-size: 10px; letter-spacing: 0.05em; margin-right: 6px; color: var(--muted); }
    .diff-legend { display: flex; gap: 12px; margin: 6px 0 2px; font-size: 11px; color: var(--muted); }
    .diff-legend .lg::before { content: ''; display: inline-block; width: 9px; height: 9px; border-radius: 50%; margin-right: 4px; vertical-align: middle; }
    .diff-legend .lg.added::before { background: #16a34a; }
    .diff-legend .lg.modified::before { background: #d97706; }
    .diff-legend .lg.removed::before { background: #dc2626; }
    pre { white-space: pre-wrap; word-break: break-word; background: #f2f4f7; border: 1px solid var(--line); border-radius: 6px; padding: 12px; line-height: 1.45; max-height: 320px; overflow: auto; }
    .field { display: grid; gap: 4px; margin-top: 8px; }
    .field label { font-size: 12px; color: var(--muted); }
    .field input, .field textarea, .field select {
      border: 1px solid var(--line); border-radius: 6px; padding: 7px 8px; background: #fff; color: var(--ink); width: 100%;
    }
    .field textarea { min-height: 56px; resize: vertical; }
    .field.two { grid-template-columns: 1fr 1fr; }
    .form-actions { display: flex; gap: 8px; margin-top: 10px; align-items: center; flex-wrap: wrap; }
    .draft-list { display: grid; gap: 6px; margin-top: 8px; }
    .draft-item { display: flex; justify-content: space-between; align-items: center; gap: 8px; font-size: 13px; border: 1px solid var(--line); border-radius: 6px; padding: 6px 8px; }
    .status-text { font-size: 13px; color: var(--muted); }
    .editor { display: none; }
    .editor.enabled { display: block; }
    .cluster-hull { fill: rgba(255,255,255,0.58); stroke: rgba(136,147,157,0.36); stroke-width: 1; stroke-dasharray: 6 5; pointer-events: none; }
    .cluster-label { fill: var(--muted); font-size: 12px; font-weight: 600; pointer-events: none; text-anchor: middle; }
    .edge { fill: none; stroke: #88939d; stroke-width: 1.2; opacity: 0.42; }
    .edge.focused { stroke-width: 2.4; opacity: 0.92; }
    .edge.muted { opacity: 0.08; marker-end: none; }
    .edge.negative { stroke: var(--negative); stroke-dasharray: 6 5; }
    .edge.added { stroke: #16a34a; opacity: 0.95; }
    .edge.modified { stroke: #d97706; opacity: 0.95; }
    .edge.removed { stroke: #dc2626; opacity: 0.6; stroke-dasharray: 3 4; }
    .edge-label { fill: var(--muted); font-size: 11px; pointer-events: none; }
    .node circle { stroke: #fff; stroke-width: 3; vector-effect: non-scaling-stroke; filter: drop-shadow(0 4px 10px rgba(23,32,38,0.18)); }
    .node.compact circle { stroke-width: 1.5; filter: none; }
    .node.inactive { opacity: 0.18; }
    .node.related { opacity: 1; }
    .node text { fill: var(--ink); font-size: 12px; text-anchor: middle; pointer-events: none; }
    .node.active circle { stroke: #111827; stroke-width: 4; }
    .node.compact.active circle { stroke-width: 3; }
    .node.removed circle { fill-opacity: 0.3; }
    .node.removed text { fill: var(--muted); text-decoration: line-through; }
    .diff-ring { fill: none; stroke-width: 2.5; vector-effect: non-scaling-stroke; pointer-events: none; }
    .diff-ring.added { stroke: #16a34a; }
    .diff-ring.modified { stroke: #d97706; }
    .diff-ring.removed { stroke: #dc2626; stroke-dasharray: 3 3; }
    @media (max-width: 960px) {
      .app { grid-template-columns: 1fr; height: auto; overflow: visible; }
      .workspace { height: 70vh; }
      aside { height: auto; border-left: 0; border-top: 1px solid var(--line); }
    }
""".trimIndent()

        private val SCRIPT = """
    const colors = {
      constraint: '#6554c0',
      decision: '#0f766e',
      option: '#b45309',
      question: '#2563eb',
      outcome: '#475569'
    };
    const NODE_TYPES = ['decision', 'constraint', 'question', 'option', 'outcome'];
    const NODE_STATES = ['accepted', 'proposed', 'rejected', 'deprecated'];
    const EDGE_TYPES = ['enables', 'constrains', 'forbids', 'conflicts'];
    const EDGE_POLARITIES = ['positive', 'negative'];

    let logView = JSON.parse(document.getElementById('log-data').textContent);
    let graph = {metadata: {title: 'Keemun', summary: '', authors: []}, nodes: [], edges: []};
    const viewState = {asOfSeq: null, previewChangeId: null, selectedChangeId: null};
    const draft = {message: '', records: [], previewing: false};
    let selectedId = null;
    // Diff of the change-set currently being viewed (added/updated/removed), or null.
    let diffView = null;

    // --- Projection: fold change-sets into a graph (mirror of the Kotlin projection). ---
    function projectGraph(changes, opts) {
      opts = opts || {};
      const nodes = new Map();
      const edges = new Map();
      let metadata = {title: 'Keemun', summary: '', authors: []};
      changes.forEach(function(change) {
        const acceptedView = change.status === 'accepted' && (opts.asOfSeq == null || change.seq <= opts.asOfSeq);
        const previewed = opts.previewChangeId != null && change.change_id === opts.previewChangeId;
        if (!acceptedView && !previewed) return;
        (change.records || []).forEach(function(rec) {
          if (rec.kind === 'node') nodes.set(rec.id, rec);
          else if (rec.kind === 'edge') edges.set(rec.id, rec);
          else if (rec.kind === 'meta') metadata = {title: rec.title || 'Keemun', summary: rec.summary || '', authors: rec.authors || []};
          else if (rec.kind === 'delete') (rec.entity === 'node' ? nodes : edges).delete(rec.id);
        });
      });
      return {metadata: metadata, nodes: Array.from(nodes.values()), edges: Array.from(edges.values())};
    }

    function effectiveChanges() {
      if (!draft.previewing || draft.records.length === 0) return logView.changes;
      const synthetic = {
        change_id: '__draft__',
        status: 'proposed',
        message: draft.message || 'Draft',
        seq: logView.changes.length + 1,
        records: draft.records.map(function(rec) { return Object.assign({}, rec, {change_id: '__draft__'}); })
      };
      return logView.changes.concat([synthetic]);
    }

    function previewId() {
      return draft.previewing && draft.records.length ? '__draft__' : viewState.previewChangeId;
    }

    function rebuildGraph(refit) {
      graph = projectGraph(effectiveChanges(), {asOfSeq: viewState.asOfSeq, previewChangeId: previewId()});
      if (!graph.nodes.some(function(n) { return n.id === selectedId; })) {
        selectedId = graph.nodes.length ? graph.nodes[0].id : null;
      }
      document.getElementById('graph-title').textContent = graph.metadata.title || 'Keemun';
      document.getElementById('graph-summary').textContent = graph.metadata.summary || '';
      diffView = computeDiffView();
      layoutCache = null;
      draw();
      if (selectedId) {
        selectNode(selectedId, false);
      } else {
        document.getElementById('details').innerHTML = '<p class="muted">No nodes at this point in history.</p>';
        document.getElementById('trace').textContent = '';
      }
      if (refit) fitTo(getLayout().bbox, false);
    }

    // --- Graph helpers ---
    function sortedEdges(edges) {
      return edges.slice().sort(function(a, b) {
        const oa = a.order || 999999;
        const ob = b.order || 999999;
        if (oa !== ob) return oa - ob;
        return a.id.localeCompare(b.id);
      });
    }
    function nodeById(id) { return graph.nodes.find(function(n) { return n.id === id; }); }
    function incoming(id) { return sortedEdges(graph.edges.filter(function(e) { return e.target === id; })); }
    function outgoing(id) { return sortedEdges(graph.edges.filter(function(e) { return e.source === id; })); }
    function escapeHtml(value) {
      return String(value == null ? '' : value).replace(/[&<>"']/g, function(c) {
        return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c];
      });
    }
    function short(value, max) {
      value = String(value == null ? '' : value);
      return value.length > max ? value.slice(0, max - 1) + '...' : value;
    }
    function nodeRadius(node, compact, selected, related) {
      // Constant 10px radius for every node at every zoom level (the counter-scale
      // in rescale() keeps it 10px on screen). Selection is shown via stroke/opacity.
      return 10;
    }

    const svg = document.getElementById('graph-svg');
    const svgNS = 'http://www.w3.org/2000/svg';
    let viewport = null;
    let layoutCache = null;
    let nodeVisuals = [];
    let edgeVisuals = [];
    let lastScale = NaN;

    function marker(id, color) {
      return '<marker id="' + id + '" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">' +
        '<path d="M 0 0 L 10 5 L 0 10 z" fill="' + color + '"></path></marker>';
    }

    function connectivityOrder(nodes, edges) {
      const adj = {};
      nodes.forEach(function(n) { adj[n.id] = []; });
      edges.forEach(function(e) {
        if (adj[e.source] && adj[e.target]) { adj[e.source].push(e.target); adj[e.target].push(e.source); }
      });
      const degree = {};
      const byId = {};
      nodes.forEach(function(n) { degree[n.id] = adj[n.id].length; byId[n.id] = n; });
      const roots = nodes.slice().sort(function(a, b) {
        if (degree[b.id] !== degree[a.id]) return degree[b.id] - degree[a.id];
        return a.id.localeCompare(b.id);
      });
      const visited = {};
      const order = [];
      roots.forEach(function(root) {
        if (visited[root.id]) return;
        visited[root.id] = true;
        const queue = [root.id];
        let head = 0;
        while (head < queue.length) {
          const id = queue[head++];
          order.push(byId[id]);
          const neighbours = adj[id].slice().sort(function(a, b) {
            if (degree[b] !== degree[a]) return degree[b] - degree[a];
            return a.localeCompare(b);
          });
          neighbours.forEach(function(nid) { if (!visited[nid]) { visited[nid] = true; queue.push(nid); } });
        }
      });
      return order;
    }

    function computeForceLayout() {
      // Include removed entities from the viewed diff so their ghosts get positions.
      const ghostNodes = diffView ? diffView.delNodes : [];
      const ghostEdges = diffView ? diffView.delEdges : [];
      const nodes = ghostNodes.length ? graph.nodes.concat(ghostNodes) : graph.nodes;
      const edges = ghostEdges.length ? graph.edges.concat(ghostEdges) : graph.edges;
      const n = nodes.length;
      const compact = n > 250;
      const order = connectivityOrder(nodes, edges);
      const pos = {};
      order.forEach(function(node, idx) {
        const angle = idx * 2.399963229728653;
        const radius = 16 + Math.sqrt(idx + 1) * (compact ? 34 : 52);
        pos[node.id] = {x: Math.cos(angle) * radius, y: Math.sin(angle) * radius, vx: 0, vy: 0};
      });
      const iterations = n > 5000 ? 60 : (n > 2000 ? 100 : (n > 700 ? 150 : 220));
      const repulse = compact ? 2600 : 4200;
      const springLen = compact ? 70 : 110;
      const springK = 0.02;
      const gravity = 0.006;
      const repCut = 260;
      const repCut2 = repCut * repCut;
      const maxStep = 24;
      const P = nodes.map(function(node) { return pos[node.id]; });
      for (let it = 0; it < iterations; it++) {
        const cool = 1 - it / iterations;
        const grid = {};
        for (let i = 0; i < n; i++) {
          const p = P[i];
          const key = Math.floor(p.x / repCut) + ',' + Math.floor(p.y / repCut);
          (grid[key] || (grid[key] = [])).push(i);
        }
        for (let i = 0; i < n; i++) {
          const a = P[i];
          const gx = Math.floor(a.x / repCut);
          const gy = Math.floor(a.y / repCut);
          for (let cx = gx - 1; cx <= gx + 1; cx++) {
            for (let cy = gy - 1; cy <= gy + 1; cy++) {
              const bucket = grid[cx + ',' + cy];
              if (!bucket) continue;
              for (let k = 0; k < bucket.length; k++) {
                const j = bucket[k];
                if (j <= i) continue;
                const b = P[j];
                let dx = a.x - b.x;
                let dy = a.y - b.y;
                let d2 = dx * dx + dy * dy;
                if (d2 > repCut2) continue;
                if (d2 < 1) { d2 = 1; dx = (i % 7) - 3; dy = (j % 5) - 2; }
                const d = Math.sqrt(d2);
                const f = repulse / d2;
                const fx = (dx / d) * f;
                const fy = (dy / d) * f;
                a.vx += fx; a.vy += fy;
                b.vx -= fx; b.vy -= fy;
              }
            }
          }
        }
        edges.forEach(function(edge) {
          const a = pos[edge.source];
          const b = pos[edge.target];
          if (!a || !b) return;
          const dx = b.x - a.x;
          const dy = b.y - a.y;
          const d = Math.sqrt(dx * dx + dy * dy) || 1;
          const f = (d - springLen) * springK;
          const fx = (dx / d) * f;
          const fy = (dy / d) * f;
          a.vx += fx; a.vy += fy;
          b.vx -= fx; b.vy -= fy;
        });
        nodes.forEach(function(node) {
          const p = pos[node.id];
          p.vx += -p.x * gravity;
          p.vy += -p.y * gravity;
          p.x += Math.max(-maxStep, Math.min(maxStep, p.vx * 0.5 * cool));
          p.y += Math.max(-maxStep, Math.min(maxStep, p.vy * 0.5 * cool));
          p.vx *= 0.82;
          p.vy *= 0.82;
        });
      }
      const positions = {};
      let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
      nodes.forEach(function(node) {
        const p = pos[node.id];
        positions[node.id] = {x: p.x, y: p.y};
        minX = Math.min(minX, p.x); minY = Math.min(minY, p.y);
        maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y);
      });
      if (!isFinite(minX)) { minX = 0; minY = 0; maxX = 100; maxY = 100; }
      const pad = 90;
      const bbox = {x: minX - pad, y: minY - pad, w: (maxX - minX) + pad * 2, h: (maxY - minY) + pad * 2};
      return {positions: positions, bbox: bbox};
    }

    function getLayout() {
      if (!layoutCache) layoutCache = computeForceLayout();
      return layoutCache;
    }

    function selectedContext() {
      const edgeList = selectedId ? sortedEdges(incoming(selectedId).concat(outgoing(selectedId))) : [];
      const relatedIds = new Set();
      edgeList.forEach(function(edge) { relatedIds.add(edge.source === selectedId ? edge.target : edge.source); });
      return {
        edgeIds: new Set(edgeList.map(function(edge) { return edge.id; })),
        nodeIds: relatedIds,
        nodes: Array.from(relatedIds).map(nodeById).filter(Boolean)
      };
    }

    const view = {scale: 1, x: 0, y: 0};
    let tweenRAF = null;
    function clamp(v, lo, hi) { return Math.max(lo, Math.min(hi, v)); }

    function edgeGeometry(e, inv) {
      const dx = e.tx - e.sx;
      const dy = e.ty - e.sy;
      const dist = Math.hypot(dx, dy) || 1;
      const ux = dx / dist;
      const uy = dy / dist;
      const sr = e.srBase * inv;
      const tr = e.trBase * inv;
      const x1 = e.sx + ux * sr;
      const y1 = e.sy + uy * sr;
      const gap = e.muted ? 2 * inv : tr + 7 * inv;
      const x2 = e.tx - ux * gap;
      const y2 = e.ty - uy * gap;
      const mx = (x1 + x2) / 2;
      const my = (y1 + y2) / 2;
      const bend = 0.14;
      const cx = mx - uy * dist * bend;
      const cy = my + ux * dist * bend;
      return {d: 'M ' + x1 + ' ' + y1 + ' Q ' + cx + ' ' + cy + ' ' + x2 + ' ' + y2, cx: cx, cy: cy};
    }

    function rescale() {
      const inv = 1 / view.scale;
      for (let i = 0; i < nodeVisuals.length; i++) {
        const v = nodeVisuals[i];
        const r = v.base * inv;
        v.circle.setAttribute('r', r);
        if (v.label) v.label.setAttribute('y', v.y + r + 13 * inv);
      }
      for (let i = 0; i < edgeVisuals.length; i++) {
        const e = edgeVisuals[i];
        const geom = edgeGeometry(e, inv);
        e.path.setAttribute('d', geom.d);
        if (e.label) { e.label.setAttribute('x', geom.cx); e.label.setAttribute('y', geom.cy - 4); }
      }
    }

    function applyTransform() {
      if (viewport) viewport.setAttribute('transform', 'translate(' + view.x + ',' + view.y + ') scale(' + view.scale + ')');
      if (view.scale !== lastScale) { lastScale = view.scale; rescale(); }
    }

    function setView(scale, x, y, animate) {
      if (tweenRAF) { cancelAnimationFrame(tweenRAF); tweenRAF = null; }
      if (!animate) { view.scale = scale; view.x = x; view.y = y; applyTransform(); return; }
      const s0 = view.scale, x0 = view.x, y0 = view.y, t0 = performance.now(), dur = 420;
      function step(now) {
        const k = Math.min(1, (now - t0) / dur);
        const e = k < 0.5 ? 2 * k * k : 1 - Math.pow(-2 * k + 2, 2) / 2;
        view.scale = s0 + (scale - s0) * e;
        view.x = x0 + (x - x0) * e;
        view.y = y0 + (y - y0) * e;
        applyTransform();
        tweenRAF = k < 1 ? requestAnimationFrame(step) : null;
      }
      tweenRAF = requestAnimationFrame(step);
    }

    function fitTo(box, animate) {
      const vw = svg.clientWidth || 900;
      const vh = svg.clientHeight || 600;
      const margin = 60;
      const fitW = Math.max(vw - margin * 2, 50);
      const fitH = Math.max(vh - margin * 2, 50);
      const scale = clamp(Math.min(fitW / box.w, fitH / box.h), 0.12, 2.4);
      const x = vw / 2 - (box.x + box.w / 2) * scale;
      const y = vh / 2 - (box.y + box.h / 2) * scale;
      setView(scale, x, y, animate);
    }

    function neighborhoodBox(id, context) {
      const layout = getLayout();
      const ids = [id].concat(Array.from(context.nodeIds));
      let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
      ids.forEach(function(nid) {
        const p = layout.positions[nid];
        if (!p) return;
        minX = Math.min(minX, p.x); minY = Math.min(minY, p.y);
        maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y);
      });
      if (!isFinite(minX)) return layout.bbox;
      const cx = (minX + maxX) / 2;
      const cy = (minY + maxY) / 2;
      const w = Math.max(maxX - minX, 280) + 240;
      const h = Math.max(maxY - minY, 280) + 240;
      return {x: cx - w / 2, y: cy - h / 2, w: w, h: h};
    }

    function setupPanZoom() {
      let pan = null;
      svg.addEventListener('wheel', function(event) {
        event.preventDefault();
        const rect = svg.getBoundingClientRect();
        const mx = event.clientX - rect.left;
        const my = event.clientY - rect.top;
        const factor = Math.exp(-event.deltaY * 0.0015);
        const next = clamp(view.scale * factor, 0.12, 4);
        view.x = mx - (mx - view.x) * (next / view.scale);
        view.y = my - (my - view.y) * (next / view.scale);
        view.scale = next;
        applyTransform();
      }, {passive: false});
      svg.addEventListener('pointerdown', function(event) {
        if (event.target.closest('.node')) return;
        pan = {x: event.clientX, y: event.clientY, vx: view.x, vy: view.y, id: event.pointerId};
        svg.classList.add('panning');
        svg.setPointerCapture(event.pointerId);
      });
      svg.addEventListener('pointermove', function(event) {
        if (!pan) return;
        view.x = pan.vx + (event.clientX - pan.x);
        view.y = pan.vy + (event.clientY - pan.y);
        applyTransform();
      });
      function endPan() {
        if (!pan) return;
        try { svg.releasePointerCapture(pan.id); } catch (err) {}
        pan = null;
        svg.classList.remove('panning');
      }
      svg.addEventListener('pointerup', endPan);
      svg.addEventListener('pointercancel', endPan);
      svg.addEventListener('dblclick', function(event) {
        if (event.target.closest('.node')) return;
        fitTo(getLayout().bbox, true);
      });
    }

    // Diff of the change-set being viewed (selected or previewed), works for any
    // status: before = state just prior to the change, after = before + the change.
    function computeDiffView() {
      const id = previewId() || viewState.selectedChangeId;
      if (!id) return null;
      if (!effectiveChanges().some(function(c) { return c.change_id === id; })) return null;
      const d = diffForChange(id);
      return {
        changeId: id,
        addNodes: new Set(d.nodes.added.map(function(x) { return x.id; })),
        modNodes: new Set(d.nodes.modified.map(function(x) { return x.id; })),
        delNodes: d.nodes.removed,
        addEdges: new Set(d.edges.added.map(function(x) { return x.id; })),
        modEdges: new Set(d.edges.modified.map(function(x) { return x.id; })),
        delEdges: d.edges.removed
      };
    }

    function draw() {
      const layout = getLayout();
      const positions = layout.positions;
      const compact = graph.nodes.length > 250;
      const showEdgeLabels = graph.edges.length <= 200;
      const showNodeLabels = graph.nodes.length <= 250;
      const context = selectedContext();
      // While reviewing a change-set, the diff highlight takes over from the
      // single-node focus dimming so added/updated/removed are all visible.
      const reviewing = !!diffView;
      nodeVisuals = [];
      edgeVisuals = [];

      const radiusById = {};
      graph.nodes.forEach(function(node) {
        radiusById[node.id] = nodeRadius(node, compact, node.id === selectedId, context.nodeIds.has(node.id));
      });

      svg.innerHTML = '<defs>' + marker('arrow', '#88939d') + marker('arrow-strong', '#3a444d') + marker('arrow-negative', '#b42318') + '</defs>';
      viewport = document.createElementNS(svgNS, 'g');
      viewport.setAttribute('id', 'viewport');
      svg.appendChild(viewport);

      function appendEdge(edge, className) {
        const source = positions[edge.source];
        const target = positions[edge.target];
        if (!source || !target) return;
        const muted = className.indexOf('muted') >= 0;
        const focused = className.indexOf('focused') >= 0;
        const negative = edge.polarity === 'negative';
        const removed = className.indexOf('removed') >= 0;
        const diffClass = removed ? ' removed'
          : (diffView && diffView.addEdges.has(edge.id)) ? ' added'
          : (diffView && diffView.modEdges.has(edge.id)) ? ' modified' : '';
        const entry = {
          sx: source.x, sy: source.y, tx: target.x, ty: target.y,
          srBase: radiusById[edge.source] || 10, trBase: radiusById[edge.target] || 10, muted: muted
        };
        const geom = edgeGeometry(entry, 1 / view.scale);
        const path = document.createElementNS(svgNS, 'path');
        path.setAttribute('d', geom.d);
        path.setAttribute('class', className + (negative ? ' negative' : '') + diffClass);
        if (!muted) path.setAttribute('marker-end', 'url(#' + (negative ? 'arrow-negative' : (focused ? 'arrow-strong' : 'arrow')) + ')');
        viewport.appendChild(path);
        entry.path = path;
        if (showEdgeLabels && !muted) {
          const label = document.createElementNS(svgNS, 'text');
          label.setAttribute('x', geom.cx);
          label.setAttribute('y', geom.cy - 4);
          label.setAttribute('text-anchor', 'middle');
          label.setAttribute('class', 'edge-label');
          label.textContent = edge.type + ' ' + (edge.weight == null ? 1 : edge.weight);
          viewport.appendChild(label);
          entry.label = label;
        }
        edgeVisuals.push(entry);
      }

      const sorted = sortedEdges(graph.edges);
      if (reviewing) {
        sorted.forEach(function(edge) { appendEdge(edge, 'edge'); });
        diffView.delEdges.forEach(function(edge) { appendEdge(edge, 'edge removed'); });
      } else {
        sorted.forEach(function(edge) { if (!context.edgeIds.has(edge.id)) appendEdge(edge, selectedId ? 'edge muted' : 'edge'); });
        sorted.forEach(function(edge) { if (context.edgeIds.has(edge.id)) appendEdge(edge, 'edge focused'); });
      }

      const inv = 1 / view.scale;
      // A colored outline ring around a node marks its diff status, leaving the
      // node's own colour/stroke untouched (clearer than recolouring the border).
      function addDiffRing(position, base, kind) {
        const ring = document.createElementNS(svgNS, 'circle');
        ring.setAttribute('cx', position.x);
        ring.setAttribute('cy', position.y);
        ring.setAttribute('r', (base + 5) * inv);
        ring.setAttribute('class', 'diff-ring ' + kind);
        viewport.appendChild(ring);
        nodeVisuals.push({circle: ring, base: base + 5, y: position.y, label: null});
      }
      graph.nodes.forEach(function(node) {
        const position = positions[node.id];
        if (!position) return;
        const isSelected = node.id === selectedId;
        const isRelated = context.nodeIds.has(node.id);
        const diffKind = (diffView && diffView.addNodes.has(node.id)) ? 'added'
          : (diffView && diffView.modNodes.has(node.id)) ? 'modified' : '';
        const base = radiusById[node.id];
        const radius = base * inv;
        const group = document.createElementNS(svgNS, 'g');
        group.setAttribute('class', 'node ' + (compact ? 'compact ' : '') + (isSelected ? 'active ' : '') + (isRelated ? 'related ' : '') + (diffKind ? diffKind + ' ' : '') + (!reviewing && selectedId && !isSelected && !isRelated ? 'inactive' : ''));
        group.setAttribute('tabindex', '0');
        group.setAttribute('role', 'button');
        group.setAttribute('aria-label', node.title);
        group.addEventListener('click', function() { selectNode(node.id); });
        group.addEventListener('keydown', function(event) { if (event.key === 'Enter' || event.key === ' ') selectNode(node.id); });
        const circle = document.createElementNS(svgNS, 'circle');
        circle.setAttribute('cx', position.x);
        circle.setAttribute('cy', position.y);
        circle.setAttribute('r', radius);
        circle.setAttribute('fill', colors[node.type] || colors.decision);
        group.appendChild(circle);
        const title = document.createElementNS(svgNS, 'title');
        title.textContent = node.title;
        group.appendChild(title);
        const visual = {circle: circle, base: base, y: position.y, label: null};
        if (showNodeLabels || isSelected || isRelated) {
          const label = document.createElementNS(svgNS, 'text');
          label.setAttribute('x', position.x);
          label.setAttribute('y', position.y + radius + 13 * inv);
          label.textContent = short(node.title, isSelected ? 34 : 24);
          group.appendChild(label);
          visual.label = label;
        }
        nodeVisuals.push(visual);
        viewport.appendChild(group);
        if (diffKind) addDiffRing(position, base, diffKind);
      });

      // Ghosts: nodes this change-set removed are not in the projection, so draw
      // them as faint, struck-through tombstones at their laid-out positions.
      if (reviewing) {
        diffView.delNodes.forEach(function(node) {
          const position = positions[node.id];
          if (!position) return;
          const base = 10;
          const radius = base * inv;
          const group = document.createElementNS(svgNS, 'g');
          group.setAttribute('class', 'node removed ' + (compact ? 'compact' : ''));
          const circle = document.createElementNS(svgNS, 'circle');
          circle.setAttribute('cx', position.x);
          circle.setAttribute('cy', position.y);
          circle.setAttribute('r', radius);
          circle.setAttribute('fill', colors[node.type] || colors.decision);
          group.appendChild(circle);
          const title = document.createElementNS(svgNS, 'title');
          title.textContent = node.title + ' (removed)';
          group.appendChild(title);
          const label = document.createElementNS(svgNS, 'text');
          label.setAttribute('x', position.x);
          label.setAttribute('y', position.y + radius + 13 * inv);
          label.textContent = short(node.title, 24);
          group.appendChild(label);
          nodeVisuals.push({circle: circle, base: base, y: position.y, label: label});
          viewport.appendChild(group);
          addDiffRing(position, base, 'removed');
        });
      }

      lastScale = view.scale;
      applyTransform();
    }

    function edgeButton(edge, otherNode, direction) {
      const targetTitle = otherNode ? otherNode.title : direction;
      return '<button type="button" class="edge-button" data-node="' + escapeHtml(otherNode ? otherNode.id : '') + '">' +
        '<strong>' + escapeHtml(edge.type + ' ' + targetTitle) + '</strong>' +
        '<span class="muted">' + escapeHtml(short(edge.rationale, 140)) + '</span>' +
        '</button>';
    }

    function selectNode(id, animate) {
      selectedId = id;
      const node = nodeById(id);
      if (!node) return;
      const tags = (node.tags || []).map(function(t) { return '<span class="pill">' + escapeHtml(t) + '</span>'; }).join('');
      const incomingHtml = incoming(id).map(function(e) { return edgeButton(e, nodeById(e.source), 'source'); }).join('');
      const outgoingHtml = outgoing(id).map(function(e) { return edgeButton(e, nodeById(e.target), 'target'); }).join('');
      const editBtn = editorEnabled ? '<button type="button" class="small" id="edit-node-btn">Edit</button> <button type="button" class="small danger" id="delete-node-btn">Delete</button>' : '';
      document.getElementById('details').innerHTML =
        '<div class="row spread"><p class="eyebrow">' + escapeHtml(node.type + ' / ' + (node.status || 'accepted')) + '</p><span class="row">' + editBtn + '</span></div>' +
        '<h2>' + escapeHtml(node.title) + '</h2>' +
        '<p class="muted">' + escapeHtml(node.summary || 'No summary recorded.') + '</p>' +
        '<div class="pill-row">' + tags + '</div>' +
        '<div class="panel-block"><p class="eyebrow">Incoming</p><div class="edge-list">' + (incomingHtml || '<p class="muted">No incoming relationships.</p>') + '</div></div>' +
        '<div class="panel-block"><p class="eyebrow">Outgoing</p><div class="edge-list">' + (outgoingHtml || '<p class="muted">No outgoing relationships.</p>') + '</div></div>';
      document.querySelectorAll('#details .edge-button').forEach(function(button) {
        button.addEventListener('click', function() {
          const nextId = button.getAttribute('data-node');
          if (nextId) selectNode(nextId);
        });
      });
      if (editorEnabled) {
        const editBtnEl = document.getElementById('edit-node-btn');
        if (editBtnEl) editBtnEl.addEventListener('click', function() { openNodeForm(node); });
        const delBtnEl = document.getElementById('delete-node-btn');
        if (delBtnEl) delBtnEl.addEventListener('click', function() { stageDelete('node', node.id); });
      }
      document.getElementById('trace').textContent = explain(id);
      draw();
      fitTo(neighborhoodBox(id, selectedContext()), animate !== false);
    }

    function explain(id) {
      const node = nodeById(id);
      if (!node) return '';
      const state = {lines: [], seen: {}, truncated: false, limit: graph.nodes.length > 250 ? 80 : 500};
      const lines = state.lines;
      lines.push(node.type + ': ' + node.title);
      if (node.summary) lines.push(node.summary);
      lines.push('');
      lines.push('How this node was reached:');
      appendIncoming(id, 0, state);
      if (state.truncated) { lines.push(''); lines.push('Trace truncated for this large graph.'); }
      return lines.join('\n');
    }

    function appendIncoming(id, depth, state) {
      if (state.lines.length >= state.limit) { state.truncated = true; return; }
      const edges = incoming(id);
      if (edges.length === 0 && depth === 0) { state.lines.push('- No incoming rationale recorded.'); return; }
      edges.forEach(function(edge) {
        if (state.lines.length >= state.limit) { state.truncated = true; return; }
        const source = nodeById(edge.source);
        const target = nodeById(edge.target);
        if (!source || !target) return;
        if (state.seen[edge.id]) { state.lines.push('  '.repeat(depth) + '- ' + edge.id + ' closes a cycle already described.'); return; }
        state.seen[edge.id] = true;
        state.lines.push('  '.repeat(depth) + '- ' + source.title + ' ' + edge.type + ' ' + target.title + ' [' + (edge.polarity || 'positive') + ', weight ' + (edge.weight == null ? 1 : edge.weight) + ']');
        state.lines.push('  '.repeat(depth) + '  Rationale: ' + edge.rationale);
        appendIncoming(source.id, depth + 1, state);
      });
    }

    // --- History timeline + change review ---
    function headSeq() {
      let head = 0;
      logView.changes.forEach(function(c) { if (c.status === 'accepted' && c.seq > head) head = c.seq; });
      return head;
    }

    function renderTimeline() {
      const el = document.getElementById('timeline');
      let html = '';
      logView.changes.forEach(function(change) {
        const cls = ['tl-chip', change.status];
        const viewingHistory = viewState.asOfSeq != null && change.status === 'accepted' && change.seq === viewState.asOfSeq;
        const viewingProposed = viewState.previewChangeId === change.change_id;
        if (viewingHistory || viewingProposed) cls.push('viewing');
        if (viewState.asOfSeq != null && change.status === 'accepted' && change.seq > viewState.asOfSeq) cls.push('dim');
        html += '<button type="button" class="' + cls.join(' ') + '" data-change="' + escapeHtml(change.change_id) + '" data-seq="' + change.seq + '" data-status="' + change.status + '" title="' + escapeHtml(change.message || change.change_id) + '">' +
          '<span class="dot"></span>' + escapeHtml(short(change.message || change.change_id, 22)) + '</button>';
      });
      el.innerHTML = html + '<span class="tl-spacer"></span>' +
        '<button type="button" class="tl-chip" id="tl-latest"><span class="dot"></span>Latest</button>';
      el.querySelectorAll('.tl-chip[data-change]').forEach(function(chip) {
        chip.addEventListener('click', function() {
          const id = chip.getAttribute('data-change');
          const status = chip.getAttribute('data-status');
          if (status === 'accepted') viewAsOf(parseInt(chip.getAttribute('data-seq'), 10), id);
          else selectChange(id);
        });
      });
      document.getElementById('tl-latest').addEventListener('click', viewLatest);
    }

    function viewLatest() {
      viewState.asOfSeq = null;
      viewState.previewChangeId = null;
      viewState.selectedChangeId = null;
      draft.previewing = false;
      rebuildGraph(true);
      renderTimeline();
      renderChangePanel();
    }

    function viewAsOf(seq, changeId) {
      viewState.asOfSeq = seq === headSeq() ? null : seq;
      viewState.previewChangeId = null;
      viewState.selectedChangeId = changeId;
      draft.previewing = false;
      rebuildGraph(true);
      renderTimeline();
      renderChangePanel();
    }

    function selectChange(changeId) {
      const change = logView.changes.find(function(c) { return c.change_id === changeId; });
      if (!change) return;
      viewState.selectedChangeId = changeId;
      if (change.status === 'proposed') {
        viewState.previewChangeId = changeId;
        viewState.asOfSeq = null;
        draft.previewing = false;
      } else {
        viewState.previewChangeId = null;
      }
      rebuildGraph(true);
      renderTimeline();
      renderChangePanel();
    }

    function entityFingerprint(rec) {
      const copy = Object.assign({}, rec);
      delete copy.change_id;
      delete copy.kind;
      return JSON.stringify(copy);
    }

    // Diff one change-set against the state immediately before it, so it works for
    // accepted changes (compare as-of seq-1) as well as proposed/draft ones.
    function diffForChange(changeId) {
      const changes = effectiveChanges();
      const change = changes.find(function(c) { return c.change_id === changeId; });
      const beforeSeq = change ? change.seq - 1 : null;
      const before = projectGraph(changes, {asOfSeq: beforeSeq});
      const after = projectGraph(changes, {asOfSeq: beforeSeq, previewChangeId: changeId});
      return {nodes: diffEntities(before.nodes, after.nodes), edges: diffEntities(before.edges, after.edges)};
    }

    function diffEntities(beforeArr, afterArr) {
      const b = new Map(beforeArr.map(function(x) { return [x.id, x]; }));
      const a = new Map(afterArr.map(function(x) { return [x.id, x]; }));
      const added = [], removed = [], modified = [];
      a.forEach(function(x, id) {
        if (!b.has(id)) added.push(x);
        else if (entityFingerprint(b.get(id)) !== entityFingerprint(x)) modified.push(x);
      });
      b.forEach(function(x, id) { if (!a.has(id)) removed.push(x); });
      return {added: added, removed: removed, modified: modified};
    }

    function diffRow(kind, entity, klass) {
      const label = entity.title || (entity.source + ' -> ' + entity.target) || entity.id;
      return '<div class="diff-item ' + klass + '"><span class="k">' + klass + '</span>' + escapeHtml(kind + ': ' + label) + '</div>';
    }

    function renderChangePanel() {
      const el = document.getElementById('change-panel');
      const changeId = viewState.selectedChangeId;
      const change = changeId ? logView.changes.find(function(c) { return c.change_id === changeId; }) : null;
      if (!change) {
        const head = headSeq();
        const viewing = viewState.asOfSeq == null ? 'latest' : ('as of change ' + viewState.asOfSeq);
        el.innerHTML = '<p class="eyebrow">History</p><p class="status-text">Viewing ' + escapeHtml(viewing) + ' (' + head + ' accepted change-set' + (head === 1 ? '' : 's') + '). Pick a change in the timeline below.</p>';
        return;
      }
      let html = '<p class="eyebrow">Change</p>' +
        '<div class="row spread"><h2>' + escapeHtml(change.message || change.change_id) + '</h2>' +
        '<span class="badge ' + change.status + '"><span class="dot"></span>' + change.status + '</span></div>' +
        '<p class="status-text">' + escapeHtml(change.change_id + (change.author ? ' · ' + change.author : '') + (change.created_at ? ' · ' + change.created_at : '')) + '</p>';

      const diff = diffForChange(change.change_id);
      const rows = [];
      diff.nodes.added.forEach(function(x) { rows.push(diffRow('node', x, 'added')); });
      diff.nodes.modified.forEach(function(x) { rows.push(diffRow('node', x, 'modified')); });
      diff.nodes.removed.forEach(function(x) { rows.push(diffRow('node', x, 'removed')); });
      diff.edges.added.forEach(function(x) { rows.push(diffRow('edge', x, 'added')); });
      diff.edges.modified.forEach(function(x) { rows.push(diffRow('edge', x, 'modified')); });
      diff.edges.removed.forEach(function(x) { rows.push(diffRow('edge', x, 'removed')); });
      const diffLabel = change.status === 'proposed' ? 'Proposed changes' : 'What this change did';
      html += '<p class="eyebrow" style="margin-top:14px">' + diffLabel + '</p>' +
        '<div class="diff-legend"><span class="lg added">added</span><span class="lg modified">updated</span><span class="lg removed">removed</span></div>' +
        '<div class="diff">' + (rows.join('') || '<p class="muted">No structural changes.</p>') + '</div>';
      if (change.status === 'proposed') {
        if (editorEnabled) {
          html += '<div class="form-actions"><button type="button" class="primary" id="accept-btn">Accept</button>' +
            '<button type="button" class="danger" id="reject-btn">Reject</button>' +
            '<span class="status-text" id="review-status"></span></div>';
        } else {
          html += '<p class="status-text">Run keemun serve to accept or reject changes.</p>';
        }
      }
      el.innerHTML = html;
      if (change.status === 'proposed' && editorEnabled) {
        document.getElementById('accept-btn').addEventListener('click', function() { reviewChange(change.change_id, 'accept'); });
        document.getElementById('reject-btn').addEventListener('click', function() { reviewChange(change.change_id, 'reject'); });
      }
    }

    function reviewChange(changeId, action) {
      const status = document.getElementById('review-status');
      if (status) status.textContent = action === 'accept' ? 'Accepting...' : 'Rejecting...';
      postJson('/api/changes/' + encodeURIComponent(changeId) + '/' + action, null).then(function(next) {
        logView = next;
        viewState.selectedChangeId = changeId;
        viewState.previewChangeId = null;
        viewState.asOfSeq = null;
        rebuildGraph(true);
        renderTimeline();
        renderChangePanel();
      }).catch(function(err) { if (status) status.textContent = err.message; });
    }

    // --- Authoring (editable) ---
    function renderAuthorPanel() {
      if (!editorEnabled) return;
      const el = document.getElementById('author-panel');
      el.classList.add('enabled');
      const items = draft.records.map(function(rec, idx) {
        const label = rec.kind === 'delete'
          ? ('delete ' + rec.entity + ' ' + rec.id)
          : (rec.kind + ' ' + (rec.title || (rec.source + ' -> ' + rec.target) || rec.id));
        return '<div class="draft-item"><span>' + escapeHtml(short(label, 38)) + '</span>' +
          '<button type="button" class="small danger" data-drop="' + idx + '">remove</button></div>';
      }).join('');
      el.innerHTML =
        '<div class="row spread"><p class="eyebrow">Author a change</p>' +
        '<span class="row"><button type="button" class="small" id="add-node-btn">+ Node</button>' +
        '<button type="button" class="small" id="add-edge-btn">+ Edge</button></span></div>' +
        '<div id="form-host"></div>' +
        '<div class="draft-list">' + (items || '<p class="muted">No staged edits. Add nodes/edges, then submit for review.</p>') + '</div>' +
        '<div class="field"><label for="draft-message">Change message</label><input id="draft-message" type="text" value="' + escapeHtml(draft.message) + '" placeholder="What is this change about?"></div>' +
        '<div class="form-actions">' +
        '<button type="button" class="primary" id="submit-draft" ' + (draft.records.length ? '' : 'disabled') + '>Submit for review</button>' +
        '<button type="button" id="preview-draft" ' + (draft.records.length ? '' : 'disabled') + '>' + (draft.previewing ? 'Stop preview' : 'Preview') + '</button>' +
        '<button type="button" id="discard-draft" ' + (draft.records.length ? '' : 'disabled') + '>Discard</button>' +
        '<span class="status-text" id="draft-status"></span></div>';
      el.querySelectorAll('[data-drop]').forEach(function(btn) {
        btn.addEventListener('click', function() {
          draft.records.splice(parseInt(btn.getAttribute('data-drop'), 10), 1);
          if (!draft.records.length) draft.previewing = false;
          renderAuthorPanel();
          rebuildGraph(false);
        });
      });
      document.getElementById('add-node-btn').addEventListener('click', function() { openNodeForm(null); });
      document.getElementById('add-edge-btn').addEventListener('click', function() { openEdgeForm(); });
      document.getElementById('draft-message').addEventListener('input', function(e) { draft.message = e.target.value; });
      document.getElementById('submit-draft').addEventListener('click', submitDraft);
      document.getElementById('discard-draft').addEventListener('click', function() {
        draft.records = []; draft.message = ''; draft.previewing = false;
        renderAuthorPanel(); rebuildGraph(false);
      });
      document.getElementById('preview-draft').addEventListener('click', function() {
        draft.previewing = !draft.previewing;
        renderAuthorPanel(); rebuildGraph(true);
      });
    }

    function fieldText(id, label, value) {
      return '<div class="field"><label for="' + id + '">' + label + '</label><input id="' + id + '" type="text" value="' + escapeHtml(value || '') + '"></div>';
    }
    function fieldArea(id, label, value) {
      return '<div class="field"><label for="' + id + '">' + label + '</label><textarea id="' + id + '">' + escapeHtml(value || '') + '</textarea></div>';
    }
    function fieldSelect(id, label, options, value) {
      const opts = options.map(function(o) { return '<option value="' + o + '"' + (o === value ? ' selected' : '') + '>' + o + '</option>'; }).join('');
      return '<div class="field"><label for="' + id + '">' + label + '</label><select id="' + id + '">' + opts + '</select></div>';
    }

    function openNodeForm(node) {
      const host = document.getElementById('form-host');
      const editing = !!node;
      host.innerHTML = '<h3>' + (editing ? 'Edit node' : 'New node') + '</h3>' +
        fieldText('nf-id', 'id', node ? node.id : '') +
        fieldText('nf-title', 'title', node ? node.title : '') +
        '<div class="field two">' + fieldSelect('nf-type', 'type', NODE_TYPES, node ? node.type : 'decision') +
        fieldSelect('nf-status', 'status', NODE_STATES, node ? node.status : 'accepted') + '</div>' +
        fieldArea('nf-summary', 'summary', node ? node.summary : '') +
        fieldText('nf-tags', 'tags (comma separated)', node && node.tags ? node.tags.join(', ') : '') +
        '<div class="form-actions"><button type="button" class="primary small" id="nf-save">Stage node</button>' +
        '<button type="button" class="small" id="nf-cancel">Cancel</button></div>';
      if (editing) document.getElementById('nf-id').setAttribute('readonly', 'readonly');
      document.getElementById('nf-cancel').addEventListener('click', function() { host.innerHTML = ''; });
      document.getElementById('nf-save').addEventListener('click', function() {
        const id = document.getElementById('nf-id').value.trim();
        const title = document.getElementById('nf-title').value.trim();
        if (!id || !title) return;
        const tags = document.getElementById('nf-tags').value.split(',').map(function(t) { return t.trim(); }).filter(Boolean);
        const rec = {kind: 'node', id: id, type: document.getElementById('nf-type').value, title: title,
          summary: document.getElementById('nf-summary').value.trim(), status: document.getElementById('nf-status').value, tags: tags};
        stageRecord(rec, function(r) { return r.kind === 'node' && r.id === id; });
        host.innerHTML = '';
      });
    }

    function openEdgeForm() {
      const host = document.getElementById('form-host');
      const ids = graph.nodes.map(function(n) { return n.id; });
      host.innerHTML = '<h3>New edge</h3>' +
        fieldText('ef-id', 'id', '') +
        '<div class="field two">' + fieldSelect('ef-source', 'source', ids, ids[0]) + fieldSelect('ef-target', 'target', ids, ids[1] || ids[0]) + '</div>' +
        '<div class="field two">' + fieldSelect('ef-type', 'type', EDGE_TYPES, 'enables') + fieldSelect('ef-polarity', 'polarity', EDGE_POLARITIES, 'positive') + '</div>' +
        fieldArea('ef-rationale', 'rationale', '') +
        fieldText('ef-weight', 'weight (0.0-1.0)', '1.0') +
        '<div class="form-actions"><button type="button" class="primary small" id="ef-save">Stage edge</button>' +
        '<button type="button" class="small" id="ef-cancel">Cancel</button></div>';
      document.getElementById('ef-cancel').addEventListener('click', function() { host.innerHTML = ''; });
      document.getElementById('ef-save').addEventListener('click', function() {
        const id = document.getElementById('ef-id').value.trim();
        const rationale = document.getElementById('ef-rationale').value.trim();
        if (!id || !rationale) return;
        let weight = parseFloat(document.getElementById('ef-weight').value);
        if (isNaN(weight)) weight = 1.0;
        const rec = {kind: 'edge', id: id, source: document.getElementById('ef-source').value,
          target: document.getElementById('ef-target').value, type: document.getElementById('ef-type').value,
          rationale: rationale, polarity: document.getElementById('ef-polarity').value, weight: weight};
        stageRecord(rec, function(r) { return r.kind === 'edge' && r.id === id; });
        host.innerHTML = '';
      });
    }

    function stageRecord(rec, matches) {
      const idx = draft.records.findIndex(matches);
      if (idx >= 0) draft.records[idx] = rec; else draft.records.push(rec);
      renderAuthorPanel();
      rebuildGraph(false);
    }

    function stageDelete(entity, id) {
      stageRecord({kind: 'delete', entity: entity, id: id}, function(r) { return r.kind === 'delete' && r.entity === entity && r.id === id; });
    }

    function submitDraft() {
      const status = document.getElementById('draft-status');
      status.textContent = 'Submitting...';
      const payload = {status: 'proposed', message: draft.message || 'Proposed change', records: draft.records.map(function(r) { return Object.assign({}, r); })};
      postJson('/api/changes', payload).then(function(next) {
        logView = next;
        const created = next.changes[next.changes.length - 1];
        draft.records = []; draft.message = ''; draft.previewing = false;
        viewState.selectedChangeId = created ? created.change_id : null;
        viewState.previewChangeId = created ? created.change_id : null;
        viewState.asOfSeq = null;
        renderAuthorPanel();
        rebuildGraph(true);
        renderTimeline();
        renderChangePanel();
      }).catch(function(err) { status.textContent = err.message; });
    }

    function postJson(url, body) {
      return fetch(url, {
        method: 'POST',
        headers: {'content-type': 'application/json'},
        body: body == null ? '' : JSON.stringify(body)
      }).then(function(response) {
        if (!response.ok) return response.text().then(function(text) { throw new Error(text || ('HTTP ' + response.status)); });
        return response.json();
      });
    }

    function setupSearch() {
      const search = document.getElementById('search');
      search.addEventListener('input', function() {
        const query = search.value.trim().toLowerCase();
        if (!query) return;
        const node = graph.nodes.find(function(c) {
          return c.id.toLowerCase().includes(query) || c.title.toLowerCase().includes(query);
        });
        if (node) selectNode(node.id);
      });
    }

    document.getElementById('fit-button').addEventListener('click', function() { fitTo(getLayout().bbox, true); });
    window.addEventListener('resize', applyTransform);
    setupPanZoom();
    setupSearch();
    renderAuthorPanel();
    rebuildGraph(false);
    renderTimeline();
    renderChangePanel();
    if (selectedId) selectNode(selectedId, false);
    else fitTo(getLayout().bbox, false);
""".trimIndent()
    }
}
