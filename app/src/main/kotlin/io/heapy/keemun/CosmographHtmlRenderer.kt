package io.heapy.keemun

class CosmographHtmlRenderer {
    fun render(graph: KeemunGraph, editable: Boolean = false): String {
        val normalized = graph.requireValid()
        val graphJson = GraphJson.encode(normalized).scriptSafe()
        val editorEnabled = if (editable) "true" else "false"
        return """
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${normalized.metadata.title.escapeHtml()} / Cosmograph</title>
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
    main {
      min-width: 0;
      height: 100vh;
      display: grid;
      grid-template-rows: auto minmax(0, 1fr);
    }
    header {
      min-height: 72px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
      padding: 16px 24px;
      background: rgba(246, 247, 249, 0.94);
      border-bottom: 1px solid rgba(216, 222, 228, 0.74);
      backdrop-filter: blur(10px);
      z-index: 2;
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
    .toolbar {
      display: flex;
      align-items: center;
      gap: 8px;
      flex: 0 0 auto;
    }
    .search {
      width: min(320px, 38vw);
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 9px 10px;
      background: #fff;
      color: var(--ink);
      font: inherit;
    }
    button {
      border: 1px solid var(--line);
      background: #fff;
      color: var(--ink);
      border-radius: 6px;
      padding: 8px 10px;
      cursor: pointer;
      font: inherit;
    }
    button:hover { border-color: #aab4be; }
    button.primary {
      background: var(--accent);
      color: #fff;
      border-color: var(--accent);
    }
    .graph-stage {
      position: relative;
      min-height: 0;
      background:
        linear-gradient(rgba(216, 222, 228, 0.22) 1px, transparent 1px),
        linear-gradient(90deg, rgba(216, 222, 228, 0.22) 1px, transparent 1px);
      background-size: 32px 32px;
      overflow: hidden;
    }
    #cosmograph-container {
      position: absolute;
      inset: 0;
    }
    .status {
      position: absolute;
      left: 16px;
      bottom: 16px;
      max-width: min(520px, calc(100% - 32px));
      padding: 8px 10px;
      border: 1px solid var(--line);
      border-radius: 6px;
      color: var(--muted);
      background: rgba(255, 255, 255, 0.9);
      font-size: 13px;
      z-index: 3;
    }
    .load-error {
      max-width: 720px;
      margin: 80px auto;
      padding: 18px;
      border: 1px solid #f2b8b5;
      border-radius: 8px;
      background: #fff7f7;
      color: var(--negative);
      line-height: 1.45;
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
    .muted { color: var(--muted); }
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
    .editor { display: none; }
    .editor.enabled { display: block; }
    .node-form {
      display: grid;
      gap: 10px;
      margin-bottom: 16px;
    }
    .form-grid {
      display: grid;
      grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
      gap: 10px;
    }
    .field {
      display: grid;
      gap: 5px;
    }
    .field span {
      color: var(--muted);
      font-size: 12px;
    }
    .field input,
    .field select,
    .field textarea {
      width: 100%;
      min-width: 0;
      border: 1px solid var(--line);
      border-radius: 6px;
      padding: 8px 9px;
      color: var(--ink);
      background: #fff;
      font: inherit;
    }
    .field textarea {
      min-height: 86px;
      resize: vertical;
      line-height: 1.4;
    }
    .field input[readonly] {
      background: #f2f4f7;
      color: var(--muted);
    }
    .raw-json {
      border-top: 1px solid var(--line);
      padding-top: 14px;
      margin-top: 14px;
    }
    .raw-json summary {
      cursor: pointer;
      color: var(--muted);
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.08em;
    }
    .raw-json summary:hover { color: var(--ink); }
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
      body { overflow: auto; }
      .app {
        display: block;
        height: auto;
        overflow: visible;
      }
      main { height: 68vh; min-height: 560px; }
      header {
        align-items: stretch;
        flex-direction: column;
      }
      .toolbar, .search { width: 100%; }
      aside {
        height: auto;
        border-left: 0;
        border-top: 1px solid var(--line);
      }
      .form-grid { grid-template-columns: 1fr; }
    }
  </style>
</head>
<body>
  <div class="app">
    <main>
      <header>
        <div>
          <h1>${normalized.metadata.title.escapeHtml()}</h1>
          <p class="summary">${normalized.metadata.summary.escapeHtml()}</p>
        </div>
        <div class="toolbar">
          <input id="search" class="search" type="search" placeholder="Search nodes">
          <button id="fit-button" type="button">Fit</button>
        </div>
      </header>
      <section class="graph-stage">
        <div id="cosmograph-container"></div>
        <div id="status" class="status">Loading Cosmograph...</div>
      </section>
    </main>
    <aside>
      <section id="details"></section>
      <section class="panel-section">
        <p class="eyebrow">Generated rationale</p>
        <pre id="trace">Select a node to generate the path rationale.</pre>
      </section>
      <section id="editor-section" class="panel-section editor">
        <p id="node-editor-title" class="eyebrow">Node editor</p>
        <form id="node-form" class="node-form">
          <label class="field">
            <span>ID</span>
            <input id="node-id" name="id" autocomplete="off" required>
          </label>
          <div class="form-grid">
            <label class="field">
              <span>Type</span>
              <select id="node-type" name="type">
                <option value="decision">Decision</option>
                <option value="constraint">Constraint</option>
                <option value="question">Question</option>
                <option value="option">Option</option>
                <option value="outcome">Outcome</option>
              </select>
            </label>
            <label class="field">
              <span>Status</span>
              <select id="node-status" name="status">
                <option value="accepted">Accepted</option>
                <option value="proposed">Proposed</option>
                <option value="rejected">Rejected</option>
                <option value="deprecated">Deprecated</option>
              </select>
            </label>
          </div>
          <label class="field">
            <span>Title</span>
            <input id="node-title" name="title" autocomplete="off" required>
          </label>
          <label class="field">
            <span>Summary</span>
            <textarea id="node-summary" name="summary"></textarea>
          </label>
          <label class="field">
            <span>Tags</span>
            <input id="node-tags" name="tags" autocomplete="off" placeholder="comma separated">
          </label>
          <div class="editor-actions">
            <button id="save-node" type="submit" class="primary">Save node</button>
            <button id="new-node" type="button">Add node</button>
            <button id="discard-node" type="button">Reset</button>
          </div>
        </form>
        <details class="raw-json">
          <summary>Graph JSON</summary>
          <textarea id="json-editor" spellcheck="false"></textarea>
        </details>
        <div class="editor-actions">
          <button id="save-json" type="button" class="primary">Save</button>
          <button id="reload-json" type="button">Reload</button>
          <span id="save-status"></span>
        </div>
      </section>
    </aside>
  </div>
  <script type="application/json" id="graph-data">$graphJson</script>
  <script type="module">
    const editorEnabled = $editorEnabled;
    const moduleUrls = [
      'https://esm.sh/@cosmograph/cosmograph',
      'https://cdn.jsdelivr.net/npm/@cosmograph/cosmograph/+esm'
    ];
    const colors = {
      constraint: '#6554c0',
      decision: '#0f766e',
      option: '#b45309',
      question: '#2563eb',
      outcome: '#475569'
    };
    let graph = JSON.parse(document.getElementById('graph-data').textContent);
    let cosmograph = null;
    let preparedPoints = [];
    let selectedId = graph.nodes.length > 0 ? graph.nodes[0].id : null;
    let nodeEditorMode = 'edit';
    let nodeEditorOriginalId = selectedId;

    const status = document.getElementById('status');
    const container = document.getElementById('cosmograph-container');

    function syncEditor() {
      const textarea = document.getElementById('json-editor');
      if (textarea) {
        textarea.value = JSON.stringify(graph, null, 2) + '\n';
      }
    }

    function graphErrorMessage(responseText) {
      try {
        const payload = JSON.parse(responseText);
        const details = payload.details && payload.details.length ? ': ' + payload.details.join('; ') : '';
        return (payload.error || responseText) + details;
      } catch (error) {
        return responseText || 'Could not save graph';
      }
    }

    function applySavedGraph(nextGraph, nextSelectedId) {
      graph = nextGraph;
      syncEditor();
      const preferredId = nextSelectedId && nodeById(nextSelectedId)
        ? nextSelectedId
        : (selectedId && nodeById(selectedId) ? selectedId : (graph.nodes[0] ? graph.nodes[0].id : null));
      selectedId = preferredId;
      if (selectedId) {
        history.replaceState(null, '', '#' + encodeURIComponent(selectedId));
      }
      return initCosmograph();
    }

    function saveGraphText(text, nextSelectedId) {
      const saveStatus = document.getElementById('save-status');
      if (saveStatus) saveStatus.textContent = 'Saving...';
      return fetch('/api/graph', {
        method: 'PUT',
        headers: {'content-type': 'application/json'},
        body: text
      }).then(function(response) {
        if (!response.ok) {
          return response.text().then(function(responseText) {
            throw new Error(graphErrorMessage(responseText));
          });
        }
        return response.json();
      }).then(function(nextGraph) {
        return applySavedGraph(nextGraph, nextSelectedId).then(function() {
          if (saveStatus) saveStatus.textContent = 'Saved';
          return nextGraph;
        });
      });
    }

    function saveGraphObject(nextGraph, nextSelectedId) {
      return saveGraphText(JSON.stringify(nextGraph, null, 2) + '\n', nextSelectedId);
    }

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

    function connectedPointIndices(id) {
      const ids = new Set([id]);
      graph.edges.forEach(function(edge) {
        if (edge.source === id) ids.add(edge.target);
        if (edge.target === id) ids.add(edge.source);
      });
      return preparedPoints.filter(function(point) { return ids.has(point.id); }).map(function(point) { return point.index; });
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

    function nodeFormElements() {
      return {
        form: document.getElementById('node-form'),
        heading: document.getElementById('node-editor-title'),
        id: document.getElementById('node-id'),
        type: document.getElementById('node-type'),
        status: document.getElementById('node-status'),
        title: document.getElementById('node-title'),
        summary: document.getElementById('node-summary'),
        tags: document.getElementById('node-tags'),
        save: document.getElementById('save-node')
      };
    }

    function tagsFromText(value) {
      return String(value || '').split(',')
        .map(function(tag) { return tag.trim(); })
        .filter(function(tag, index, tags) { return tag && tags.indexOf(tag) === index; })
        .sort();
    }

    function tagsToText(tags) {
      return (tags || []).join(', ');
    }

    function nextNodeId() {
      let index = graph.nodes.length + 1;
      let id = '';
      do {
        id = 'node-' + String(index).padStart(4, '0');
        index += 1;
      } while (nodeById(id));
      return id;
    }

    function populateNodeEditor(node, mode) {
      const elements = nodeFormElements();
      if (!editorEnabled || !elements.form) return;
      const nextMode = mode || (node ? 'edit' : 'add');
      const draft = node || {
        id: nextNodeId(),
        type: 'decision',
        status: 'proposed',
        title: 'New node',
        summary: '',
        tags: []
      };
      nodeEditorMode = nextMode;
      nodeEditorOriginalId = nextMode === 'edit' ? draft.id : null;
      elements.heading.textContent = nextMode === 'add' ? 'Add node' : 'Edit node';
      elements.save.textContent = nextMode === 'add' ? 'Create node' : 'Save node';
      elements.id.value = draft.id;
      elements.id.readOnly = nextMode === 'edit';
      elements.type.value = draft.type || 'decision';
      elements.status.value = draft.status || 'accepted';
      elements.title.value = draft.title || '';
      elements.summary.value = draft.summary || '';
      elements.tags.value = tagsToText(draft.tags);
    }

    function nodeFromForm() {
      const elements = nodeFormElements();
      const node = {
        id: elements.id.value.trim(),
        type: elements.type.value,
        title: elements.title.value.trim(),
        summary: elements.summary.value.trim(),
        status: elements.status.value,
        tags: tagsFromText(elements.tags.value)
      };
      if (!/^[a-z0-9][a-z0-9._:-]*$/.test(node.id)) {
        throw new Error("Node ID must start with a lowercase letter or digit and only use lowercase letters, digits, '.', '_', ':', or '-'");
      }
      if (!node.title) {
        throw new Error('Node title is required');
      }
      return node;
    }

    function saveNodeFromForm() {
      const saveStatus = document.getElementById('save-status');
      let node = null;
      try {
        node = nodeFromForm();
      } catch (error) {
        if (saveStatus) saveStatus.textContent = error.message;
        return Promise.reject(error);
      }

      const nodes = graph.nodes.slice();
      if (nodeEditorMode === 'add') {
        if (nodeById(node.id)) {
          const error = new Error("Node '" + node.id + "' already exists");
          if (saveStatus) saveStatus.textContent = error.message;
          return Promise.reject(error);
        }
        nodes.push(node);
      } else {
        const index = nodes.findIndex(function(candidate) { return candidate.id === nodeEditorOriginalId; });
        if (index < 0) {
          const error = new Error('Selected node no longer exists');
          if (saveStatus) saveStatus.textContent = error.message;
          return Promise.reject(error);
        }
        nodes[index] = node;
      }

      return saveGraphObject(Object.assign({}, graph, {nodes: nodes}), node.id);
    }

    async function importCosmographModule() {
      let lastError = null;
      for (const url of moduleUrls) {
        try {
          status.textContent = 'Loading Cosmograph from ' + new URL(url).hostname + '...';
          return await import(url);
        } catch (error) {
          lastError = error;
        }
      }
      throw lastError || new Error('Could not import @cosmograph/cosmograph');
    }

    function buildCosmographData() {
      const degrees = {};
      graph.nodes.forEach(function(node) { degrees[node.id] = 0; });
      graph.edges.forEach(function(edge) {
        degrees[edge.source] = (degrees[edge.source] || 0) + 1;
        degrees[edge.target] = (degrees[edge.target] || 0) + 1;
      });

      const points = graph.nodes.map(function(node, index) {
        return {
          id: node.id,
          index: index,
          title: node.title,
          type: node.type,
          status: node.status || 'accepted',
          colorKey: node.type,
          degree: degrees[node.id] || 1,
          size: Math.max(3, Math.min(12, 3 + Math.sqrt(degrees[node.id] || 1)))
        };
      });
      const idSet = new Set(points.map(function(point) { return point.id; }));
      const pointIndexById = {};
      points.forEach(function(point) {
        pointIndexById[point.id] = point.index;
      });
      const links = graph.edges.filter(function(edge) {
        return idSet.has(edge.source) && idSet.has(edge.target);
      }).map(function(edge, index) {
        return {
          id: edge.id,
          index: index,
          source: edge.source,
          sourceIndex: pointIndexById[edge.source],
          target: edge.target,
          targetIndex: pointIndexById[edge.target],
          type: edge.type,
          polarity: edge.polarity || 'positive',
          color: edge.polarity === 'negative' ? '#b42318' : '#88939d',
          width: Math.max(0.45, Math.min(2.2, edge.weight || 1))
        };
      });
      return { points: points, links: links };
    }

    function edgeButton(edge, otherNode, direction) {
      const targetTitle = otherNode ? otherNode.title : direction;
      return '<button type="button" class="edge-button" data-node="' + escapeHtml(otherNode ? otherNode.id : '') + '">' +
        '<strong>' + escapeHtml(edge.type + ' ' + targetTitle) + '</strong>' +
        '<span class="muted">' + escapeHtml(short(edge.rationale, 140)) + '</span>' +
        '</button>';
    }

    function selectNode(id, focusGraph) {
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
          if (nextId) selectNode(nextId, true);
        });
      });
      document.getElementById('trace').textContent = explain(id);
      populateNodeEditor(node, 'edit');

      const point = preparedPoints.find(function(candidate) { return candidate.id === id; });
      if (cosmograph && point) {
        const indices = connectedPointIndices(id);
        if (focusGraph) {
          cosmograph.selectPoints(indices, false);
        } else {
          cosmograph.unselectAllPoints();
        }
        cosmograph.setFocusedPoint(point.index);
        if (focusGraph) {
          if (indices.length > 1 && cosmograph.fitViewByIndices) {
            cosmograph.fitViewByIndices(indices, 350, 0.28);
          } else if (cosmograph.zoomToPoint) {
            cosmograph.zoomToPoint(point.index, 350, 4, true);
          }
        }
      }
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
        if (node) selectNode(node.id, true);
      });
    }

    function setupEditor() {
      const section = document.getElementById('editor-section');
      if (!editorEnabled) return;
      section.classList.add('enabled');
      const textarea = document.getElementById('json-editor');
      const saveStatus = document.getElementById('save-status');
      syncEditor();
      const selectedNode = selectedId ? nodeById(selectedId) : null;
      populateNodeEditor(selectedNode, selectedNode ? 'edit' : 'add');
      document.getElementById('node-form').addEventListener('submit', function(event) {
        event.preventDefault();
        saveNodeFromForm().catch(function(error) {
          saveStatus.textContent = error.message;
        });
      });
      document.getElementById('new-node').addEventListener('click', function() {
        populateNodeEditor(null, 'add');
        document.getElementById('node-title').focus();
      });
      document.getElementById('discard-node').addEventListener('click', function() {
        if (nodeEditorMode === 'add') {
          populateNodeEditor(null, 'add');
        } else {
          const currentNode = selectedId ? nodeById(selectedId) : null;
          populateNodeEditor(currentNode, currentNode ? 'edit' : 'add');
        }
        saveStatus.textContent = '';
      });
      document.getElementById('reload-json').addEventListener('click', function() {
        fetch('/api/graph').then(function(response) { return response.json(); }).then(function(nextGraph) {
          return applySavedGraph(nextGraph, selectedId);
        }).then(function() {
          saveStatus.textContent = 'Reloaded';
        }).catch(function(error) {
          saveStatus.textContent = error.message;
        });
      });
      document.getElementById('save-json').addEventListener('click', function() {
        saveGraphText(textarea.value, selectedId).catch(function(error) {
          saveStatus.textContent = error.message;
        });
      });
    }

    async function initCosmograph() {
      status.textContent = 'Loading Cosmograph...';
      container.innerHTML = '';
      try {
        if (cosmograph && cosmograph.destroy) {
          cosmograph.destroy();
        }
        const module = await importCosmographModule();
        const Cosmograph = module.Cosmograph;
        const raw = buildCosmographData();
        preparedPoints = raw.points;
        const config = {
          points: raw.points,
          links: raw.links,
          pointIdBy: 'id',
          pointIndexBy: 'index',
          linkSourceBy: 'source',
          linkSourceIndexBy: 'sourceIndex',
          linkTargetBy: 'target',
          linkTargetIndexBy: 'targetIndex',
          pointColorBy: 'colorKey',
          pointColorByMap: colors,
          pointColorStrategy: 'map',
          pointSizeBy: 'size',
          pointSizeStrategy: 'direct',
          pointLabelBy: 'title',
          pointLabelWeightBy: 'degree',
          linkColorBy: 'color',
          linkColorStrategy: 'direct',
          linkWidthBy: 'width',
          linkWidthStrategy: 'direct',
          linkArrowBy: 'polarity',
          linkArrowByFn: function(value) { return value === 'negative' || value === 'positive'; },
          backgroundColor: '#f6f7f9',
          pointDefaultSize: 4,
          pointGreyoutOpacity: 0.12,
          linkOpacity: 0.24,
          linkGreyoutOpacity: 0.05,
          linkDefaultWidth: 0.75,
          curvedLinks: true,
          simulationRepulsion: 0.55,
          simulationLinkDistance: 24,
          simulationGravity: 0.12,
          showHoveredPointLabel: true,
          showFocusedPointLabel: true,
          showDynamicLabels: false,
          selectPointOnClick: true,
          focusPointOnClick: true,
          fitViewOnInit: true,
          fitViewDelay: 700,
          fitViewPadding: 0.16,
          randomSeed: 42,
          onPointClick: function(index) {
            const point = preparedPoints[index];
            if (point) selectNode(point.id, true);
          },
          onBackgroundClick: function() {
            if (cosmograph) {
              cosmograph.unselectAllPoints();
              cosmograph.setFocusedPoint(undefined);
              cosmograph.fitView(300, 0.16);
            }
          }
        };
        cosmograph = new Cosmograph(container, config);
        window.keemunCosmograph = cosmograph;
        window.keemunPoints = raw.points;
        window.keemunLinks = raw.links;
        cosmograph.start(0.35);
        status.textContent = 'Cosmograph: ' + raw.points.length + ' nodes, ' + raw.links.length + ' edges';
        const hashId = decodeURIComponent(window.location.hash.replace(/^#/, ''));
        selectedId = graph.nodes.some(function(node) { return node.id === hashId; }) ? hashId : (graph.nodes[0] ? graph.nodes[0].id : null);
        if (selectedId) {
          setTimeout(function() { selectNode(selectedId, Boolean(hashId)); }, 1400);
        }
      } catch (error) {
        console.error(error);
        status.textContent = 'Cosmograph failed to load';
        container.innerHTML = '<div class="load-error"><strong>Could not load Cosmograph.</strong><br>' +
          escapeHtml(error.message || String(error)) +
          '<br><br>This renderer imports @cosmograph/cosmograph from a CDN. Use <code>--engine svg</code> for the offline renderer.</div>';
      }
    }

    document.getElementById('fit-button').addEventListener('click', function() {
      if (cosmograph) cosmograph.fitView(350, 0.16);
    });
    setupSearch();
    setupEditor();
    initCosmograph();
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
