package org.keemun

class HtmlRenderer(
    private val cosmographRenderer: CosmographHtmlRenderer = CosmographHtmlRenderer(),
) {
    fun render(
        graph: KeemunGraph,
        editable: Boolean = false,
        engine: RenderEngine = RenderEngine.SVG,
    ): String =
        when (engine) {
            RenderEngine.SVG -> renderSvg(graph, editable)
            RenderEngine.COSMOGRAPH -> cosmographRenderer.render(graph, editable)
        }

    private fun renderSvg(graph: KeemunGraph, editable: Boolean = false): String {
        val normalized = graph.requireValid()
        val graphJson = GraphJson.encode(normalized).scriptSafe()
        val editorEnabled = if (editable) "true" else "false"
        return """
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${normalized.metadata.title.escapeHtml()}</title>
  <style>
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
      grid-template-columns: minmax(0, 1fr) 380px;
      height: 100vh;
      overflow: hidden;
    }
    header {
      position: sticky;
      top: 0;
      height: 72px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 18px 24px;
      z-index: 4;
      background: rgba(246, 247, 249, 0.92);
      border-bottom: 1px solid rgba(216, 222, 228, 0.74);
      backdrop-filter: blur(10px);
    }
    h1 {
      margin: 0;
      font-size: 22px;
      line-height: 1.2;
      letter-spacing: 0;
    }
    .summary {
      margin: 4px 0 0;
      color: var(--muted);
      font-size: 14px;
      max-width: 760px;
    }
    .workspace {
      position: relative;
      height: 100vh;
      overflow: auto;
      overscroll-behavior: contain;
    }
    .toolbar {
      display: flex;
      gap: 8px;
      flex: 0 0 auto;
    }
    input, textarea, button, select {
      font: inherit;
    }
    .search {
      width: min(320px, 46vw);
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
    button.primary {
      background: var(--accent);
      color: #fff;
      border-color: var(--accent);
    }
    #graph-svg {
      display: block;
      width: 100%;
      height: calc(100vh - 72px);
      min-height: 520px;
    }
    .cluster-hull {
      fill: rgba(255, 255, 255, 0.58);
      stroke: rgba(136, 147, 157, 0.36);
      stroke-width: 1;
      stroke-dasharray: 6 5;
      pointer-events: none;
    }
    .cluster-label {
      fill: var(--muted);
      font-size: 12px;
      font-weight: 600;
      pointer-events: none;
      text-anchor: middle;
    }
    .edge {
      stroke: #88939d;
      stroke-width: 1.2;
      opacity: 0.42;
      marker-end: url(#arrow);
    }
    .edge.focused {
      stroke-width: 2.4;
      opacity: 0.92;
    }
    .edge.muted {
      opacity: 0.08;
      marker-end: none;
    }
    .edge.negative {
      stroke: var(--negative);
      stroke-dasharray: 6 5;
    }
    .edge-label {
      fill: var(--muted);
      font-size: 11px;
      pointer-events: none;
    }
    .node circle {
      stroke: #fff;
      stroke-width: 3;
      filter: drop-shadow(0 4px 10px rgba(23, 32, 38, 0.18));
    }
    .node.compact circle {
      stroke-width: 1.5;
      filter: none;
    }
    .node.inactive {
      opacity: 0.18;
    }
    .node.related {
      opacity: 1;
    }
    .node text {
      fill: var(--ink);
      font-size: 12px;
      text-anchor: middle;
      pointer-events: none;
    }
    .node.active circle {
      stroke: #111827;
      stroke-width: 4;
    }
    .node.compact.active circle {
      stroke-width: 3;
    }
    aside {
      height: 100vh;
      border-left: 1px solid var(--line);
      background: var(--panel);
      padding: 20px;
      overflow: auto;
      overscroll-behavior: contain;
    }
    .eyebrow {
      color: var(--muted);
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.08em;
      margin: 0 0 8px;
    }
    h2 {
      margin: 0 0 8px;
      font-size: 20px;
      line-height: 1.25;
      letter-spacing: 0;
    }
    .panel-section {
      border-top: 1px solid var(--line);
      padding-top: 16px;
      margin-top: 16px;
    }
    .muted {
      color: var(--muted);
    }
    .pill-row {
      display: flex;
      gap: 6px;
      flex-wrap: wrap;
      margin-top: 12px;
    }
    .pill {
      border: 1px solid var(--line);
      border-radius: 999px;
      padding: 4px 8px;
      font-size: 12px;
      color: var(--muted);
      background: var(--panel-strong);
    }
    .edge-list {
      display: grid;
      gap: 8px;
      margin-top: 10px;
    }
    .edge-button {
      width: 100%;
      text-align: left;
      background: #fff;
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 9px;
      line-height: 1.35;
    }
    .edge-button strong {
      display: block;
      color: var(--ink);
      margin-bottom: 2px;
    }
    pre {
      white-space: pre-wrap;
      word-break: break-word;
      background: #f2f4f7;
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 12px;
      line-height: 1.45;
      max-height: 360px;
      overflow: auto;
    }
    .editor {
      display: none;
    }
    .editor.enabled {
      display: block;
    }
    #json-editor {
      width: 100%;
      min-height: 360px;
      resize: vertical;
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 10px;
      font-family: ui-monospace, SFMono-Regular, Menlo, Consolas, monospace;
      font-size: 12px;
      line-height: 1.45;
    }
    .editor-actions {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 10px;
    }
    #save-status {
      color: var(--muted);
      font-size: 13px;
    }
    @media (max-width: 900px) {
      .app {
        grid-template-columns: 1fr;
        height: auto;
        overflow: visible;
      }
      header {
        height: auto;
        padding-right: 16px;
        align-items: stretch;
        flex-direction: column;
      }
      .workspace {
        height: 62vh;
        min-height: 560px;
      }
      .toolbar {
        width: 100%;
      }
      .search {
        width: 100%;
      }
      #graph-svg {
        height: 560px;
        min-height: 560px;
      }
      aside {
        height: auto;
        max-height: none;
        border-left: 0;
        border-top: 1px solid var(--line);
      }
    }
  </style>
</head>
<body>
  <div class="app">
    <main class="workspace">
      <header>
        <div>
          <h1>${normalized.metadata.title.escapeHtml()}</h1>
          <p class="summary">${normalized.metadata.summary.escapeHtml()}</p>
        </div>
        <div class="toolbar">
          <input id="search" class="search" type="search" placeholder="Search nodes">
          <button id="fit-button" type="button">Center</button>
        </div>
      </header>
      <svg id="graph-svg" role="img" aria-label="Keemun decision graph"></svg>
    </main>
    <aside>
      <section id="details"></section>
      <section class="panel-section">
        <p class="eyebrow">Generated rationale</p>
        <pre id="trace">Select a node to generate the path rationale.</pre>
      </section>
      <section id="editor-section" class="panel-section editor">
        <p class="eyebrow">Graph JSON</p>
        <textarea id="json-editor" spellcheck="false"></textarea>
        <div class="editor-actions">
          <button id="save-json" type="button" class="primary">Save</button>
          <button id="reload-json" type="button">Reload</button>
          <span id="save-status"></span>
        </div>
      </section>
    </aside>
  </div>
  <script type="application/json" id="graph-data">$graphJson</script>
  <script>
    const editorEnabled = $editorEnabled;
    const colors = {
      constraint: '#6554c0',
      decision: '#0f766e',
      option: '#b45309',
      question: '#2563eb',
      outcome: '#475569'
    };
    let graph = JSON.parse(document.getElementById('graph-data').textContent);
    const hashId = decodeURIComponent(window.location.hash.replace(/^#/, ''));
    let selectedId = graph.nodes.some(function(node) { return node.id === hashId; })
      ? hashId
      : (graph.nodes.length > 0 ? graph.nodes[0].id : null);

    function sortedEdges(edges) {
      return edges.slice().sort(function(a, b) {
        const orderA = a.order || 999999;
        const orderB = b.order || 999999;
        if (orderA !== orderB) return orderA - orderB;
        return a.id.localeCompare(b.id);
      });
    }

    function nodeById(id) {
      return graph.nodes.find(function(node) { return node.id === id; });
    }

    function incoming(id) {
      return sortedEdges(graph.edges.filter(function(edge) { return edge.target === id; }));
    }

    function outgoing(id) {
      return sortedEdges(graph.edges.filter(function(edge) { return edge.source === id; }));
    }

    function escapeHtml(value) {
      return String(value || '').replace(/[&<>"']/g, function(char) {
        return {'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[char];
      });
    }

    function short(value, max) {
      value = String(value || '');
      return value.length > max ? value.slice(0, max - 1) + '...' : value;
    }

    function clusterKey(node) {
      const tags = node.tags || [];
      const cluster = tags.find(function(tag) {
        return tag.startsWith('cluster-') || tag.startsWith('bucket-');
      });
      return cluster || ('type-' + node.type);
    }

    function clusterLabel(key) {
      return key.replace('cluster-', 'Cluster ').replace('bucket-', 'Bucket ').replace('type-', '');
    }

    function graphClusters() {
      const byKey = {};
      graph.nodes.forEach(function(node) {
        const key = clusterKey(node);
        if (!byKey[key]) byKey[key] = {key: key, label: clusterLabel(key), nodes: []};
        byKey[key].nodes.push(node);
      });
      return Object.values(byKey).sort(function(a, b) {
        return a.key.localeCompare(b.key);
      });
    }

    function nodeRadius(node, compact, selected, related) {
      if (selected) return compact ? 13 : 34;
      if (related) return compact ? 9 : 30;
      if (graph.nodes.length > 500) return 7;
      if (graph.nodes.length > 250) return 10;
      return node.type === 'constraint' ? 28 : 32;
    }

    function createBaseLayout(width) {
      const clusters = graphClusters();
      const compact = graph.nodes.length > 250;
      const columns = Math.min(compact ? 4 : 3, Math.max(1, Math.ceil(Math.sqrt(clusters.length))));
      const rows = Math.max(1, Math.ceil(clusters.length / columns));
      const cellWidth = Math.max(220, width / columns);
      const largestCluster = clusters.reduce(function(max, cluster) {
        return Math.max(max, cluster.nodes.length);
      }, 1);
      const cellHeight = compact
        ? Math.max(300, Math.min(460, Math.sqrt(largestCluster) * 34 + 120))
        : 340;
      const height = Math.max(window.innerHeight - 72, rows * cellHeight + 120);
      const positions = {};
      const hulls = [];

      clusters.forEach(function(cluster, clusterIndex) {
        const col = clusterIndex % columns;
        const row = Math.floor(clusterIndex / columns);
        const cx = col * cellWidth + cellWidth / 2;
        const cy = 84 + row * cellHeight + cellHeight / 2;
        const maxRadius = Math.min(cellWidth, cellHeight) * 0.38;
        hulls.push({cx: cx, cy: cy, r: maxRadius + 24, label: cluster.label + ' / ' + cluster.nodes.length});

        cluster.nodes.forEach(function(node, nodeIndex) {
          if (cluster.nodes.length === 1) {
            positions[node.id] = {x: cx, y: cy};
            return;
          }
          const angle = nodeIndex * 2.399963229728653 + clusterIndex * 0.57;
          const radius = Math.min(maxRadius, 12 + Math.sqrt(nodeIndex + 1) * (compact ? 13 : 36));
          positions[node.id] = {
            x: cx + Math.cos(angle) * radius,
            y: cy + Math.sin(angle) * radius
          };
        });
      });

      return {positions: positions, hulls: hulls, height: height};
    }

    function selectedContext() {
      const edgeList = selectedId ? sortedEdges(incoming(selectedId).concat(outgoing(selectedId))) : [];
      const relatedIds = new Set();
      edgeList.forEach(function(edge) {
        relatedIds.add(edge.source === selectedId ? edge.target : edge.source);
      });
      return {
        edgeIds: new Set(edgeList.map(function(edge) { return edge.id; })),
        nodeIds: relatedIds,
        nodes: Array.from(relatedIds).map(nodeById).filter(Boolean)
      };
    }

    function applyOrbitLayout(width, baseLayout, context) {
      if (!selectedId) return {positions: baseLayout.positions, height: baseLayout.height};
      const positions = Object.assign({}, baseLayout.positions);
      const centerX = width * 0.50;
      const centerY = 420;
      positions[selectedId] = {x: centerX, y: centerY};

      let maxY = baseLayout.height;
      let placed = 0;
      let ring = 0;
      while (placed < context.nodes.length) {
        const radius = 150 + ring * 82;
        const capacity = Math.max(8, Math.floor((Math.PI * 2 * radius) / 54));
        for (let slot = 0; slot < capacity && placed < context.nodes.length; slot++) {
          const node = context.nodes[placed];
          const angle = (Math.PI * 2 * slot / capacity) + ring * 0.31;
          positions[node.id] = {
            x: centerX + Math.cos(angle) * radius,
            y: centerY + Math.sin(angle) * radius
          };
          maxY = Math.max(maxY, positions[node.id].y + 180);
          placed += 1;
        }
        ring += 1;
      }

      return {positions: positions, height: Math.max(baseLayout.height, maxY)};
    }

    function draw() {
      const svg = document.getElementById('graph-svg');
      const width = Math.max(svg.clientWidth || 900, 680);
      const compact = graph.nodes.length > 250;
      const showEdgeLabels = graph.edges.length <= 500;
      const showNodeLabels = graph.nodes.length <= 250;
      const context = selectedContext();
      const baseLayout = createBaseLayout(width);
      const focusedLayout = applyOrbitLayout(width, baseLayout, context);
      const positions = focusedLayout.positions;
      const height = focusedLayout.height;
      svg.style.height = height + 'px';
      svg.setAttribute('viewBox', '0 0 ' + width + ' ' + height);
      svg.innerHTML = '<defs><marker id="arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M 0 0 L 10 5 L 0 10 z" fill="#88939d"></path></marker></defs>';

      baseLayout.hulls.forEach(function(hull) {
        const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        circle.setAttribute('cx', hull.cx);
        circle.setAttribute('cy', hull.cy);
        circle.setAttribute('r', hull.r);
        circle.setAttribute('class', 'cluster-hull');
        svg.appendChild(circle);

        const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
        label.setAttribute('x', hull.cx);
        label.setAttribute('y', hull.cy - hull.r - 10);
        label.setAttribute('class', 'cluster-label');
        label.textContent = hull.label;
        svg.appendChild(label);
      });

      function appendEdge(edge, className) {
        const source = positions[edge.source];
        const target = positions[edge.target];
        if (!source || !target) return;
        const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
        line.setAttribute('x1', source.x);
        line.setAttribute('y1', source.y);
        line.setAttribute('x2', target.x);
        line.setAttribute('y2', target.y);
        line.setAttribute('class', className + ' ' + (edge.polarity === 'negative' ? 'negative' : ''));
        svg.appendChild(line);

        if (showEdgeLabels) {
          const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
          label.setAttribute('x', (source.x + target.x) / 2);
          label.setAttribute('y', (source.y + target.y) / 2 - 6);
          label.setAttribute('class', 'edge-label');
          label.textContent = edge.type + ' ' + (edge.weight || 1);
          svg.appendChild(label);
        }
      }

      const sorted = sortedEdges(graph.edges);
      sorted.forEach(function(edge) {
        if (context.edgeIds.has(edge.id)) return;
        appendEdge(edge, selectedId ? 'edge muted' : 'edge');
      });
      sorted.forEach(function(edge) {
        if (!context.edgeIds.has(edge.id)) return;
        appendEdge(edge, 'edge focused');
      });

      graph.nodes.forEach(function(node) {
        const position = positions[node.id];
        if (!position) return;
        const isSelected = node.id === selectedId;
        const isRelated = context.nodeIds.has(node.id);
        const group = document.createElementNS('http://www.w3.org/2000/svg', 'g');
        group.setAttribute('class', 'node ' + (compact ? 'compact ' : '') + (isSelected ? 'active ' : '') + (isRelated ? 'related ' : '') + (selectedId && !isSelected && !isRelated ? 'inactive' : ''));
        group.setAttribute('tabindex', '0');
        group.setAttribute('role', 'button');
        group.setAttribute('aria-label', node.title);
        group.addEventListener('click', function() { selectNode(node.id); });
        group.addEventListener('keydown', function(event) {
          if (event.key === 'Enter' || event.key === ' ') selectNode(node.id);
        });

        const circle = document.createElementNS('http://www.w3.org/2000/svg', 'circle');
        circle.setAttribute('cx', position.x);
        circle.setAttribute('cy', position.y);
        circle.setAttribute('r', nodeRadius(node, compact, isSelected, isRelated));
        circle.setAttribute('fill', colors[node.type] || colors.decision);
        group.appendChild(circle);

        const title = document.createElementNS('http://www.w3.org/2000/svg', 'title');
        title.textContent = node.title;
        group.appendChild(title);

        if (showNodeLabels || isSelected || isRelated) {
          const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
          label.setAttribute('x', position.x);
          label.setAttribute('y', position.y + (isSelected ? 34 : 28));
          label.textContent = short(node.title, isSelected ? 34 : 24);
          group.appendChild(label);
        }
        svg.appendChild(group);
      });
    }

    function centerGraphPane() {
      const pane = document.querySelector('.workspace');
      pane.scrollTo({top: 0, left: 0, behavior: 'smooth'});
    }

    function edgeButton(edge, otherNode, direction) {
      const targetTitle = otherNode ? otherNode.title : direction;
      return '<button type="button" class="edge-button" data-node="' + escapeHtml(otherNode ? otherNode.id : '') + '">' +
        '<strong>' + escapeHtml(edge.type + ' ' + targetTitle) + '</strong>' +
        '<span class="muted">' + escapeHtml(short(edge.rationale, 140)) + '</span>' +
        '</button>';
    }

    function selectNode(id) {
      selectedId = id;
      const node = nodeById(id);
      if (!node) return;
      history.replaceState(null, '', '#' + encodeURIComponent(id));
      const tags = (node.tags || []).map(function(tag) { return '<span class="pill">' + escapeHtml(tag) + '</span>'; }).join('');
      const incomingHtml = incoming(id).map(function(edge) { return edgeButton(edge, nodeById(edge.source), 'source'); }).join('');
      const outgoingHtml = outgoing(id).map(function(edge) { return edgeButton(edge, nodeById(edge.target), 'target'); }).join('');
      document.getElementById('details').innerHTML =
        '<p class="eyebrow">' + escapeHtml(node.type + ' / ' + (node.status || 'accepted')) + '</p>' +
        '<h2>' + escapeHtml(node.title) + '</h2>' +
        '<p class="muted">' + escapeHtml(node.summary || 'No summary recorded.') + '</p>' +
        '<div class="pill-row">' + tags + '</div>' +
        '<div class="panel-section"><p class="eyebrow">Incoming</p><div class="edge-list">' + (incomingHtml || '<p class="muted">No incoming relationships.</p>') + '</div></div>' +
        '<div class="panel-section"><p class="eyebrow">Outgoing</p><div class="edge-list">' + (outgoingHtml || '<p class="muted">No outgoing relationships.</p>') + '</div></div>';
      document.querySelectorAll('.edge-button').forEach(function(button) {
        button.addEventListener('click', function() {
          const nextId = button.getAttribute('data-node');
          if (nextId) selectNode(nextId);
        });
      });
      document.getElementById('trace').textContent = explain(id);
      draw();
      centerGraphPane();
    }

    function explain(id) {
      const node = nodeById(id);
      const state = {lines: [], seen: {}, truncated: false, limit: graph.nodes.length > 250 ? 80 : 500};
      const lines = state.lines;
      lines.push(node.type + ': ' + node.title);
      if (node.summary) lines.push(node.summary);
      lines.push('');
      lines.push('How this node was reached:');
      appendIncoming(id, 0, state);
      if (state.truncated) {
        lines.push('');
        lines.push('Trace truncated for this large graph.');
      }
      return lines.join('\n');
    }

    function appendIncoming(id, depth, state) {
      if (state.lines.length >= state.limit) {
        state.truncated = true;
        return;
      }
      const edges = incoming(id);
      if (edges.length === 0 && depth === 0) {
        state.lines.push('- No incoming rationale recorded.');
        return;
      }
      edges.forEach(function(edge) {
        if (state.lines.length >= state.limit) {
          state.truncated = true;
          return;
        }
        const source = nodeById(edge.source);
        const target = nodeById(edge.target);
        if (state.seen[edge.id]) {
          state.lines.push('  '.repeat(depth) + '- ' + edge.id + ' closes a cycle already described.');
          return;
        }
        state.seen[edge.id] = true;
        state.lines.push('  '.repeat(depth) + '- ' + source.title + ' ' + edge.type + ' ' + target.title + ' [' + (edge.polarity || 'positive') + ', weight ' + (edge.weight || 1) + ']');
        state.lines.push('  '.repeat(depth) + '  Rationale: ' + edge.rationale);
        appendIncoming(source.id, depth + 1, state);
      });
    }

    function setupSearch() {
      const search = document.getElementById('search');
      search.addEventListener('input', function() {
        const query = search.value.trim().toLowerCase();
        if (!query) return;
        const node = graph.nodes.find(function(candidate) {
          return candidate.id.toLowerCase().includes(query) || candidate.title.toLowerCase().includes(query);
        });
        if (node) selectNode(node.id);
      });
    }

    function setupEditor() {
      const section = document.getElementById('editor-section');
      if (!editorEnabled) return;
      section.classList.add('enabled');
      const textarea = document.getElementById('json-editor');
      const status = document.getElementById('save-status');
      function syncEditor() {
        textarea.value = JSON.stringify(graph, null, 2) + '\n';
      }
      syncEditor();
      document.getElementById('reload-json').addEventListener('click', function() {
        fetch('/api/graph').then(function(response) { return response.json(); }).then(function(nextGraph) {
          graph = nextGraph;
          selectedId = graph.nodes.length > 0 ? graph.nodes[0].id : null;
          syncEditor();
          selectNode(selectedId);
          status.textContent = 'Reloaded';
        }).catch(function(error) {
          status.textContent = error.message;
        });
      });
      document.getElementById('save-json').addEventListener('click', function() {
        status.textContent = 'Saving...';
        fetch('/api/graph', {
          method: 'PUT',
          headers: {'content-type': 'application/json'},
          body: textarea.value
        }).then(function(response) {
          if (!response.ok) {
            return response.text().then(function(text) { throw new Error(text); });
          }
          return response.json();
        }).then(function(nextGraph) {
          graph = nextGraph;
          syncEditor();
          selectedId = selectedId && nodeById(selectedId) ? selectedId : (graph.nodes[0] ? graph.nodes[0].id : null);
          if (selectedId) selectNode(selectedId);
          status.textContent = 'Saved';
        }).catch(function(error) {
          status.textContent = error.message;
        });
      });
    }

    document.getElementById('fit-button').addEventListener('click', function() {
      draw();
      centerGraphPane();
    });
    window.addEventListener('resize', draw);
    setupSearch();
    setupEditor();
    draw();
    if (selectedId) selectNode(selectedId);
  </script>
</body>
</html>
""".trimIndent()
    }

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
}
