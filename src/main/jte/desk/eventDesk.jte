@param String contextPath
@param com.github.gcolin.platform.Messages msg
@param com.github.gcolin.notification.Notifications notifications
@param String page
@param com.github.gcolin.platform.Page pageConfig
@param com.github.gcolin.platform.StateService state
@param String success
@param String title
@param com.github.gcolin.auth.LoggedUser user
@param Object amount
@param Object club
@param String currentStatus
@param com.github.gcolin.event.Event event
@param java.util.List<com.github.gcolin.event.Event> events
@param Object filter
@param boolean hasCollection
@param com.github.gcolin.membership.Membership m
@param com.github.gcolin.membership.MembershipOption option
@param java.util.List<com.github.gcolin.membership.MembershipOption> options
@param Object p
@param com.github.gcolin.player.DisplayPlayer player
@param java.util.List<com.github.gcolin.player.Player> players
@param Object search
@param String status
@param Object type
@param String eventsJson
@param String deskTicket
!{title = msg.get("event.desk.title") + " — " + event.getName();}
@template.platform.head(title=title, contextPath=contextPath, pageConfig=pageConfig)
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">

<html>


<body class="d-flex flex-column min-vh-100 bg-chess-overlay">
!{page = "event";}
@template.platform.menubar(contextPath=contextPath, user=user, state=state, notifications=notifications, pageConfig=pageConfig, page=page, msg=msg)
<main class="container flex-fill">
  <div class="bg-body p-3 rounded mb-3">
    <div class="d-flex flex-wrap justify-content-between align-items-start gap-2 mb-3">
      <div>
        <h2 class="mb-1"><i class="bi bi-clipboard-check me-2"></i>${msg.get("event.desk.title")}</h2>
        <div id="desk-event-name" class="text-body-secondary">${event.getName()}</div>
      </div>
      <div class="d-flex flex-wrap gap-2 align-items-center">
        <span id="desk-status" class="badge text-bg-secondary">${msg.get("event.desk.connecting")}</span>
        <span id="desk-pending" class="badge text-bg-warning d-none">0</span>
        <a id="desk-back" class="btn btn-outline-info btn-sm" href="${contextPath}/event/${String.valueOf(event.getId())}">${msg.get("action.back")}</a>
      </div>
    </div>

    @if(hasCollection)
    <div class="mb-3">
      <label class="form-label" for="desk-event-select">${msg.get("event.desk.switchEvent")}</label>
      <select id="desk-event-select" class="form-select"></select>
    </div>
    @endif

    <ul class="nav nav-tabs" id="desk-tabs" role="tablist">
      <li class="nav-item" role="presentation">
        <button class="nav-link active" id="desk-players-tab" data-bs-toggle="tab"
          data-bs-target="#desk-players-pane" type="button" role="tab"
          aria-controls="desk-players-pane" aria-selected="true">
          ${msg.get("event.desk.tab.players")}
        </button>
      </li>
      <li class="nav-item" role="presentation">
        <button class="nav-link" id="desk-history-tab" data-bs-toggle="tab"
          data-bs-target="#desk-history-pane" type="button" role="tab"
          aria-controls="desk-history-pane" aria-selected="false">
          ${msg.get("event.desk.tab.history")}
          <span id="desk-history-count" class="badge text-bg-secondary ms-1 d-none">0</span>
        </button>
      </li>
    </ul>
    <div class="tab-content" id="desk-tab-content">
      <div class="tab-pane fade show active" id="desk-players-pane" role="tabpanel" aria-labelledby="desk-players-tab">
        <div class="row g-2 mb-3 mt-3 align-items-end">
          <div class="col-md">
            <label class="form-label" for="desk-search">${msg.get("search.label")}</label>
            <input type="search" id="desk-search" class="form-control" placeholder="${msg.get("search.label")}" autocomplete="off">
          </div>
          <div class="col-md-auto">
            <label class="form-label" for="desk-filter">${msg.get("label.filters")}</label>
            <select id="desk-filter" class="form-select">
              <option value="all" selected>${msg.get("event.desk.filter.all")}</option>
              <option value="notPresent">${msg.get("event.desk.filter.notPresent")}</option>
              <option value="notPaid" id="desk-filter-not-paid">${msg.get("event.desk.filter.notPaid")}</option>
            </select>
          </div>
        </div>

        <div class="table-responsive rounded" style="overflow: hidden;">
          <table class="table table-striped mb-0" id="desk-table">
            <thead>
              <tr>
                <th scope="col">${msg.get("player.name")}</th>
                @if(hasCollection)
                <th id="desk-event-col" scope="col" class="d-none">${msg.get("event.desk.history.event")}</th>
                @endif
                <th scope="col" class="d-none d-md-table-cell">${msg.get("player.licence")}</th>
                <th scope="col" class="d-none d-md-table-cell">${msg.get("player.rating")}</th>
                <th scope="col" class="text-center">${msg.get("player.attendance")}</th>
                <th id="desk-paid-header" scope="col" class="text-center">${msg.get("event.status")}</th>
              </tr>
            </thead>
            <tbody id="desk-tbody"></tbody>
          </table>
        </div>
      </div>
      <div class="tab-pane fade" id="desk-history-pane" role="tabpanel" aria-labelledby="desk-history-tab">
        <div class="table-responsive rounded mt-3" style="overflow: hidden;">
          <table class="table table-striped mb-0" id="desk-history-table">
            <thead>
              <tr>
                <th scope="col">${msg.get("event.desk.history.time")}</th>
                @if(hasCollection)
                <th scope="col" class="d-none d-md-table-cell">${msg.get("event.desk.history.event")}</th>
                @endif
                <th scope="col">${msg.get("event.desk.history.player")}</th>
                <th scope="col">${msg.get("event.desk.history.action")}</th>
                <th scope="col" class="text-end"></th>
              </tr>
            </thead>
            <tbody id="desk-history-tbody"></tbody>
          </table>
        </div>
        <p id="desk-history-empty" class="text-body-secondary mt-3 mb-0 d-none">${msg.get("event.desk.history.empty")}</p>
      </div>
    </div>
  </div>
</main>
@template.platform.footer(contextPath=contextPath, pageConfig=pageConfig, msg=msg)

<script type="application/json" id="desk-bootstrap">$unsafe{eventsJson != null ? eventsJson.replace("</", "<\\/") : "[]"}</script>
<script>
(function () {
  const INITIAL_EVENT_ID = ${event.getId() != null ? event.getId().intValue() : 0};
  const CONTEXT_PATH = '${contextPath}';
  const DESK_TICKET = "${deskTicket != null ? deskTicket : ""}";
  window.DESK_TICKET = DESK_TICKET;
  const MSG = {
    connecting: "${msg.get("event.desk.connecting")}",
    online: "${msg.get("event.desk.online")}",
    offline: "${msg.get("event.desk.offline")}",
    pending: "${msg.get("event.desk.pending")}",
    paid: "${msg.get("playerStatus.PAID")}",
    notPaid: "${msg.get("playerStatus.NOT_PAID")}",
    markPresent: "${msg.get("player.attendance.mark")}",
    clearPresent: "${msg.get("player.attendance.clear")}",
    historyRevert: "${msg.get("event.desk.history.revert")}",
    historyReverted: "${msg.get("event.desk.history.reverted")}",
    historyPresentMark: "${msg.get("event.desk.history.present.mark")}",
    historyPresentClear: "${msg.get("event.desk.history.present.clear")}",
    historyPaid: "${msg.get("event.desk.history.paid")}",
    historyNotPaid: "${msg.get("event.desk.history.notPaid")}",
    allEvents: "${msg.get("event.desk.allEvents")}",
    fideTitres: {
      m: "${msg.get("fidetitre.m")}",
      mf: "${msg.get("fidetitre.mf")}",
      f: "${msg.get("fidetitre.f")}",
      ff: "${msg.get("fidetitre.ff")}",
      gf: "${msg.get("fidetitre.gf")}",
      g: "${msg.get("fidetitre.g")}"
    }
  };
  const HAS_COLLECTION = ${hasCollection};
  const HISTORY_MAX = 200;
  const ALL_EVENTS = 'all';

  let eventsById = {};
  let eventOrder = [];
  try {
    const boot = JSON.parse(document.getElementById('desk-bootstrap').textContent || '[]');
    (boot || []).forEach(function (evt) {
      eventsById[evt.id] = evt;
      eventOrder.push(evt.id);
    });
  } catch (e) {}

  const CACHE_KEY = 'desk-events-' + eventOrder.join('-');
  try {
    const cached = JSON.parse(localStorage.getItem(CACHE_KEY) || 'null');
    if (cached && Array.isArray(cached.events)) {
      cached.events.forEach(function (evt) {
        if (evt && evt.id != null) {
          eventsById[evt.id] = evt;
          if (eventOrder.indexOf(evt.id) < 0) {
            eventOrder.push(evt.id);
          }
        }
      });
    }
  } catch (e) {}

  let currentEventId = INITIAL_EVENT_ID;
  let viewingAll = false;
  try {
    const savedRaw = localStorage.getItem(CACHE_KEY + '-current');
    if (savedRaw === ALL_EVENTS && HAS_COLLECTION) {
      viewingAll = true;
    } else {
      const saved = parseInt(savedRaw, 10);
      if (saved && eventsById[saved]) {
        currentEventId = saved;
      }
    }
  } catch (e) {}

  function aggregatePlayers() {
    const rows = [];
    eventOrder.forEach(function (id) {
      const evt = eventsById[id];
      if (!evt || !Array.isArray(evt.players)) {
        return;
      }
      evt.players.forEach(function (p) {
        p._eventId = id;
        p._eventName = evt.name || '';
        rows.push(p);
      });
    });
    const groups = [];
    let current = null;
    rows.forEach(function (p) {
      if (p.rowType === 'OPTION' && current) {
        current.rows.push(p);
        return;
      }
      current = { rows: [p] };
      groups.push(current);
    });
    groups.sort(function (a, b) {
      const pa = a.rows[0] || {};
      const pb = b.rows[0] || {};
      const nameCmp = String(pa.name || '').localeCompare(String(pb.name || ''), undefined, { sensitivity: 'base' });
      if (nameCmp !== 0) {
        return nameCmp;
      }
      const firstCmp = String(pa.firstname || '').localeCompare(String(pb.firstname || ''), undefined, { sensitivity: 'base' });
      if (firstCmp !== 0) {
        return firstCmp;
      }
      const eventCmp = String(pa._eventName || '').localeCompare(String(pb._eventName || ''), undefined, { sensitivity: 'base' });
      if (eventCmp !== 0) {
        return eventCmp;
      }
      return (pa.subId || 0) - (pb.subId || 0);
    });
    const sorted = [];
    groups.forEach(function (g) {
      g.rows.forEach(function (p) {
        sorted.push(p);
      });
    });
    return sorted;
  }

  let players = viewingAll
    ? aggregatePlayers()
    : ((eventsById[currentEventId] && eventsById[currentEventId].players) || []);
  let eventFree = !!(eventsById[currentEventId] && eventsById[currentEventId].free);
  let pendingById = {};
  let historyEntries = [];

  let socket = null;
  let reconnectTimer = null;
  let pingTimer = null;
  let intentionalClose = false;
  let currentStatus = 'connecting';
  let reconnectPaused = false;
  const PING_INTERVAL_MS = 90000;

  const statusEl = document.getElementById('desk-status');
  const pendingEl = document.getElementById('desk-pending');
  const tbody = document.getElementById('desk-tbody');
  const searchEl = document.getElementById('desk-search');
  const filterEl = document.getElementById('desk-filter');
  const filterNotPaidEl = document.getElementById('desk-filter-not-paid');
  const eventNameEl = document.getElementById('desk-event-name');
  const backEl = document.getElementById('desk-back');
  const paidHeaderEl = document.getElementById('desk-paid-header');
  const eventColEl = document.getElementById('desk-event-col');
  const eventSelectEl = document.getElementById('desk-event-select');
  const historyTbody = document.getElementById('desk-history-tbody');
  const historyEmptyEl = document.getElementById('desk-history-empty');
  const historyCountEl = document.getElementById('desk-history-count');
  const historyTableEl = document.getElementById('desk-history-table');

  function storageKey(eventId) {
    return 'desk-pending-' + eventId;
  }

  function historyStorageKey() {
    return 'desk-history-' + CACHE_KEY;
  }

  function loadHistory() {
    try {
      const raw = localStorage.getItem(historyStorageKey());
      const list = raw ? JSON.parse(raw) : [];
      return Array.isArray(list) ? list : [];
    } catch (e) {
      return [];
    }
  }

  function saveHistory() {
    try {
      localStorage.setItem(historyStorageKey(), JSON.stringify(historyEntries));
    } catch (e) {}
  }

  function formatHistoryTime(ts) {
    try {
      return new Date(ts).toLocaleString();
    } catch (e) {
      return String(ts || '');
    }
  }

  function describeHistoryEntry(entry) {
    if (typeof entry.present === 'boolean') {
      return entry.present ? MSG.historyPresentMark : MSG.historyPresentClear;
    }
    if (entry.status === 'PAID') {
      return MSG.historyPaid;
    }
    if (entry.status === 'NOT_PAID') {
      return MSG.historyNotPaid;
    }
    return '';
  }

  function renderHistory() {
    if (!historyTbody) {
      return;
    }
    historyTbody.innerHTML = '';
    const activeCount = historyEntries.filter(function (e) { return e && !e.reverted; }).length;
    if (historyCountEl) {
      if (activeCount === 0) {
        historyCountEl.classList.add('d-none');
      } else {
        historyCountEl.classList.remove('d-none');
        historyCountEl.textContent = String(activeCount);
      }
    }
    if (historyEntries.length === 0) {
      if (historyTableEl) {
        historyTableEl.classList.add('d-none');
      }
      if (historyEmptyEl) {
        historyEmptyEl.classList.remove('d-none');
      }
      return;
    }
    if (historyTableEl) {
      historyTableEl.classList.remove('d-none');
    }
    if (historyEmptyEl) {
      historyEmptyEl.classList.add('d-none');
    }
    historyEntries.forEach(function (entry) {
      const tr = document.createElement('tr');
      if (entry.reverted) {
        tr.classList.add('text-body-secondary');
      }

      const timeTd = document.createElement('td');
      timeTd.className = 'text-nowrap small';
      timeTd.textContent = formatHistoryTime(entry.clientTs);
      tr.appendChild(timeTd);

      if (HAS_COLLECTION) {
        const eventTd = document.createElement('td');
        eventTd.className = 'd-none d-md-table-cell';
        eventTd.textContent = entry.eventName || '';
        tr.appendChild(eventTd);
      }

      const playerTd = document.createElement('td');
      playerTd.textContent = entry.playerName || '';
      tr.appendChild(playerTd);

      const actionTd = document.createElement('td');
      actionTd.textContent = describeHistoryEntry(entry);
      if (entry.reverted) {
        const badge = document.createElement('span');
        badge.className = 'badge text-bg-secondary ms-2';
        badge.textContent = MSG.historyReverted;
        actionTd.appendChild(badge);
      }
      tr.appendChild(actionTd);

      const btnTd = document.createElement('td');
      btnTd.className = 'text-end';
      if (!entry.reverted) {
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'btn btn-outline-info btn-sm';
        btn.textContent = MSG.historyRevert;
        btn.addEventListener('click', function () {
          revertHistoryEntry(entry.id);
        });
        btnTd.appendChild(btn);
      }
      tr.appendChild(btnTd);

      historyTbody.appendChild(tr);
    });
  }

  function pushHistory(entry) {
    historyEntries.unshift(entry);
    if (historyEntries.length > HISTORY_MAX) {
      historyEntries = historyEntries.slice(0, HISTORY_MAX);
    }
    saveHistory();
    renderHistory();
  }

  function revertHistoryEntry(historyId) {
    let entry = null;
    for (let i = 0; i < historyEntries.length; i++) {
      if (historyEntries[i].id === historyId) {
        entry = historyEntries[i];
        break;
      }
    }
    if (!entry || entry.reverted) {
      return;
    }
    if (!viewingAll && entry.eventId && entry.eventId !== currentEventId) {
      switchEvent(entry.eventId);
    }
    const fields = {};
    if (typeof entry.present === 'boolean') {
      fields.present = !!entry.prevPresent;
    }
    if (entry.status) {
      fields.status = entry.prevStatus || 'NOT_PAID';
    }
    entry.reverted = true;
    saveHistory();
    queueOp(fields, entry.subId, {
      skipHistory: true,
      optionId: entry.optionId,
      eventId: entry.eventId != null ? entry.eventId : currentEventId
    });
    renderHistory();
  }

  function loadPending(eventId) {
    try {
      const raw = localStorage.getItem(storageKey(eventId));
      const list = raw ? JSON.parse(raw) : [];
      const map = {};
      (list || []).forEach(function (op) {
        if (op && op.id) {
          map[op.id] = op;
        }
      });
      return map;
    } catch (e) {
      return {};
    }
  }

  function savePending() {
    localStorage.setItem(storageKey(currentEventId), JSON.stringify(Object.values(pendingById)));
    updatePendingBadge();
  }

  function saveEventsCache() {
    try {
      const events = eventOrder.map(function (id) { return eventsById[id]; }).filter(Boolean);
      localStorage.setItem(CACHE_KEY, JSON.stringify({ events: events }));
      localStorage.setItem(CACHE_KEY + '-current', viewingAll ? ALL_EVENTS : String(currentEventId));
    } catch (e) {}
  }

  function updatePendingBadge() {
    const count = Object.values(pendingById).length;
    let total = count;
    eventOrder.forEach(function (id) {
      if (id === currentEventId) {
        return;
      }
      total += Object.keys(loadPending(id)).length;
    });
    if (total === 0) {
      pendingEl.classList.add('d-none');
    } else {
      pendingEl.classList.remove('d-none');
      pendingEl.textContent = MSG.pending + ': ' + total;
    }
  }

  function setStatus(kind) {
    if (kind === currentStatus) {
      return;
    }
    if (kind === 'connecting' && currentStatus === 'offline') {
      return;
    }
    currentStatus = kind;
    statusEl.classList.remove('text-bg-secondary', 'text-bg-success', 'text-bg-danger', 'text-bg-warning');
    if (kind === 'online') {
      statusEl.classList.add('text-bg-success');
      statusEl.textContent = MSG.online;
    } else if (kind === 'offline') {
      statusEl.classList.add('text-bg-danger');
      statusEl.textContent = MSG.offline;
    } else {
      statusEl.classList.add('text-bg-secondary');
      statusEl.textContent = MSG.connecting;
    }
  }

  function clearReconnect() {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
  }

  function clearPing() {
    if (pingTimer) {
      clearInterval(pingTimer);
      pingTimer = null;
    }
  }

  function startPing() {
    clearPing();
    pingTimer = setInterval(function () {
      if (socket && socket.readyState === WebSocket.OPEN) {
        try {
          socket.send(JSON.stringify({ type: 'ping' }));
        } catch (e) {}
      }
    }, PING_INTERVAL_MS);
  }

  function scheduleReconnect() {
    clearReconnect();
    if (intentionalClose || reconnectPaused || !navigator.onLine) {
      return;
    }
    reconnectTimer = setTimeout(connect, 2000);
  }

  function currentEvent() {
    return eventsById[currentEventId] || { id: currentEventId, name: '', free: true, players: [] };
  }

  function hidePaidColumn() {
    if (viewingAll) {
      return eventOrder.every(function (id) {
        return !eventsById[id] || eventsById[id].free;
      });
    }
    return !!(eventsById[currentEventId] && eventsById[currentEventId].free);
  }

  function updateChrome() {
    eventFree = hidePaidColumn();
    if (viewingAll) {
      eventNameEl.textContent = MSG.allEvents;
      backEl.href = CONTEXT_PATH + '/event/' + currentEventId;
      document.title = "${msg.get("event.desk.title")} — " + MSG.allEvents;
      if (eventSelectEl) {
        eventSelectEl.value = ALL_EVENTS;
      }
    } else {
      const evt = currentEvent();
      eventNameEl.textContent = evt.name || '';
      backEl.href = CONTEXT_PATH + '/event/' + currentEventId;
      document.title = "${msg.get("event.desk.title")} — " + (evt.name || '');
      if (eventSelectEl) {
        eventSelectEl.value = String(currentEventId);
      }
    }
    if (paidHeaderEl) {
      paidHeaderEl.classList.toggle('d-none', eventFree);
    }
    if (filterNotPaidEl) {
      filterNotPaidEl.hidden = eventFree;
      if (eventFree && filterEl && filterEl.value === 'notPaid') {
        filterEl.value = 'all';
      }
    }
    if (eventColEl) {
      eventColEl.classList.toggle('d-none', !viewingAll);
    }
  }

  function fillEventSelect() {
    if (!eventSelectEl) {
      return;
    }
    eventSelectEl.innerHTML = '';
    if (HAS_COLLECTION && eventOrder.length > 1) {
      const allOpt = document.createElement('option');
      allOpt.value = ALL_EVENTS;
      allOpt.textContent = MSG.allEvents;
      if (viewingAll) {
        allOpt.selected = true;
      }
      eventSelectEl.appendChild(allOpt);
    }
    eventOrder.forEach(function (id) {
      const evt = eventsById[id];
      if (!evt) {
        return;
      }
      const opt = document.createElement('option');
      opt.value = String(evt.id);
      opt.textContent = evt.name;
      if (!viewingAll && evt.id === currentEventId) {
        opt.selected = true;
      }
      eventSelectEl.appendChild(opt);
    });
  }

  function playerLabel(p) {
    if (p && p.rowType === 'OPTION') {
      return p.description || p.name || '';
    }
    return ((p.name || '') + ' ' + (p.firstname || '')).trim();
  }

  function fideTitreLabel(code) {
    if (!code) {
      return '';
    }
    return MSG.fideTitres[code] || code;
  }

  function formatAmount(cents) {
    if (cents == null || cents === '') {
      return '';
    }
    const n = Number(cents);
    if (!isFinite(n)) {
      return '';
    }
    return (n / 100).toFixed(2) + ' \u20ac';
  }

  function unpaidLabel(p) {
    const amount = formatAmount(p.amountCents);
    return amount || MSG.notPaid;
  }

  function matchesSearch(p, q) {
    if (!q) {
      return true;
    }
    const hay = [playerLabel(p), fideTitreLabel(p.fideTitre), p.licence || '', p.club || '', p.category || '', p.rating || '', p.description || '', p._eventName || '']
      .join(' ')
      .toLowerCase();
    return hay.indexOf(q) >= 0;
  }

  function rowGroupKey(p) {
    return String(p._eventId != null ? p._eventId : currentEventId) + ':' + p.subId;
  }

  function matchesStatusFilter(p, statusFilter) {
    if (!statusFilter || statusFilter === 'all') {
      return true;
    }
    if (statusFilter === 'notPresent') {
      return p.rowType !== 'OPTION' && !p.present;
    }
    if (statusFilter === 'notPaid') {
      return p.status !== 'PAID';
    }
    return true;
  }

  function filterRows(q, statusFilter) {
    if (!q && (!statusFilter || statusFilter === 'all')) {
      return players.slice();
    }
    const searchKeys = {};
    const statusKeys = {};
    players.forEach(function (p) {
      const key = rowGroupKey(p);
      if (matchesSearch(p, q)) {
        searchKeys[key] = true;
      }
      if (matchesStatusFilter(p, statusFilter)) {
        statusKeys[key] = true;
      }
    });
    return players.filter(function (p) {
      const key = rowGroupKey(p);
      return !!searchKeys[key] && !!statusKeys[key];
    });
  }

  function render() {
    const q = (searchEl.value || '').trim().toLowerCase();
    const statusFilter = filterEl ? filterEl.value : 'all';
    const rows = filterRows(q, statusFilter);
    tbody.innerHTML = '';
    rows.forEach(function (p) {
      const isOption = p.rowType === 'OPTION';
      const rowEventId = p._eventId != null ? p._eventId : currentEventId;
      const rowFree = viewingAll
        ? !!(eventsById[rowEventId] && eventsById[rowEventId].free)
        : eventFree;
      const tr = document.createElement('tr');
      tr.dataset.subId = String(p.subId);
      if (rowEventId != null) {
        tr.dataset.eventId = String(rowEventId);
      }
      if (isOption && p.optionId != null) {
        tr.dataset.optionId = String(p.optionId);
        tr.classList.add('table-light');
      }

      const nameTd = document.createElement('td');
      if (isOption) {
        nameTd.className = 'ps-4 text-body-secondary';
        nameTd.appendChild(document.createTextNode(playerLabel(p)));
      } else {
        nameTd.appendChild(document.createTextNode(playerLabel(p)));
        if (p.fideTitre) {
          nameTd.appendChild(document.createTextNode(' '));
          const badge = document.createElement('span');
          badge.className = 'badge bg-success';
          badge.textContent = fideTitreLabel(p.fideTitre);
          nameTd.appendChild(badge);
        }
      }
      tr.appendChild(nameTd);

      if (HAS_COLLECTION) {
        const eventTd = document.createElement('td');
        if (!viewingAll) {
          eventTd.className = 'd-none';
        }
        eventTd.textContent = viewingAll ? (p._eventName || '') : '';
        tr.appendChild(eventTd);
      }

      const licenceTd = document.createElement('td');
      licenceTd.className = 'd-none d-md-table-cell';
      licenceTd.textContent = isOption ? '' : (p.licence || '');
      tr.appendChild(licenceTd);

      const ratingTd = document.createElement('td');
      ratingTd.className = 'd-none d-md-table-cell';
      ratingTd.textContent = isOption ? '' : (p.rating || '');
      tr.appendChild(ratingTd);

      const attendanceTd = document.createElement('td');
      attendanceTd.className = 'text-center align-middle';
      if (!isOption) {
        const attendance = document.createElement('input');
        attendance.type = 'checkbox';
        attendance.className = 'form-check-input js-desk-attendance';
        attendance.checked = !!p.present;
        attendance.title = p.present ? MSG.clearPresent : MSG.markPresent;
        attendance.addEventListener('change', function () {
          queueOp({ present: attendance.checked }, p.subId, { eventId: rowEventId });
        });
        attendanceTd.appendChild(attendance);
      }
      tr.appendChild(attendanceTd);

      if (!eventFree) {
        const statusTd = document.createElement('td');
        statusTd.className = 'text-center align-middle';
        if (!rowFree) {
          const paid = document.createElement('input');
          paid.type = 'checkbox';
          paid.className = 'form-check-input js-desk-paid';
          paid.checked = p.status === 'PAID';
          const statusText = paid.checked ? MSG.paid : unpaidLabel(p);
          paid.title = statusText;
          paid.addEventListener('change', function () {
            queueOp(
              { status: paid.checked ? "PAID" : "NOT_PAID" },
              p.subId,
              isOption ? { optionId: p.optionId, eventId: rowEventId } : { eventId: rowEventId }
            );
          });
          const label = document.createElement('span');
          label.className = 'ms-2 small' + (paid.checked ? "" : " text-warning-emphasis fw-semibold");
          label.textContent = statusText;
          statusTd.appendChild(paid);
          statusTd.appendChild(label);
        }
        tr.appendChild(statusTd);
      }

      tbody.appendChild(tr);
    });
  }

  function findPlayer(subId, optionId, eventId) {
    for (let i = 0; i < players.length; i++) {
      const p = players[i];
      if (eventId != null && p._eventId != null && p._eventId !== eventId) {
        continue;
      }
      if (optionId != null) {
        if (p.optionId === optionId) {
          return p;
        }
      } else if (p.subId === subId && p.rowType !== 'OPTION') {
        return p;
      }
    }
    return null;
  }

  function applyLocalOp(op, eventId) {
    const player = findPlayer(op.subId, op.optionId, eventId);
    if (!player) {
      return;
    }
    if (op.optionId == null && typeof op.present === 'boolean') {
      player.present = op.present;
      player.attendanceAt = op.present ? new Date().toISOString() : null;
    }
    if (op.status) {
      player.status = op.status;
    }
  }

  function queueOp(fields, subId, options) {
    options = options || {};
    const optionId = options.optionId != null ? options.optionId : null;
    const targetEventId = options.eventId != null ? options.eventId : currentEventId;
    const player = findPlayer(subId, optionId, targetEventId);
    const prevPresent = player ? !!player.present : false;
    const prevStatus = player ? (player.status || 'NOT_PAID') : 'NOT_PAID';
    const op = {
      id: 'op-' + Date.now() + '-' + Math.random().toString(16).slice(2),
      subId: subId,
      clientTs: Date.now()
    };
    if (optionId != null) {
      op.optionId = optionId;
    }
    if (optionId == null && typeof fields.present === 'boolean') {
      op.present = fields.present;
    }
    if (fields.status) {
      op.status = fields.status;
    }
    applyLocalOp(op, targetEventId);
    if (targetEventId === currentEventId) {
      pendingById[op.id] = op;
      if (!viewingAll && eventsById[currentEventId]) {
        eventsById[currentEventId].players = players;
      }
      savePending();
    } else {
      const pending = loadPending(targetEventId);
      pending[op.id] = op;
      localStorage.setItem(storageKey(targetEventId), JSON.stringify(Object.values(pending)));
      updatePendingBadge();
    }
    saveEventsCache();
    if (!options.skipHistory) {
      const evt = eventsById[targetEventId] || currentEvent();
      const hist = {
        id: 'hist-' + Date.now() + '-' + Math.random().toString(16).slice(2),
        eventId: targetEventId,
        eventName: evt.name || '',
        subId: subId,
        optionId: optionId,
        playerName: player ? playerLabel(player) : String(optionId != null ? optionId : subId),
        clientTs: op.clientTs,
        reverted: false
      };
      if (typeof op.present === 'boolean') {
        hist.present = op.present;
        hist.prevPresent = prevPresent;
      }
      if (op.status) {
        hist.status = op.status;
        hist.prevStatus = prevStatus;
      }
      pushHistory(hist);
    }
    render();
    flushPendingForEvent(targetEventId);
  }

  function flushPendingForEvent(eventId) {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return;
    }
    const pending = (eventId === currentEventId) ? pendingById : loadPending(eventId);
    const ops = Object.values(pending);
    if (ops.length === 0) {
      return;
    }
    socket.send(JSON.stringify({ type: 'sync', eventId: eventId, ops: ops }));
  }

  function flushPending() {
    flushPendingForEvent(currentEventId);
  }

  function flushAllPendingAndReload() {
    if (!socket || socket.readyState !== WebSocket.OPEN) {
      return;
    }
    const batches = [];
    eventOrder.forEach(function (id) {
      const pending = (id === currentEventId) ? pendingById : loadPending(id);
      const ops = Object.values(pending);
      if (ops.length > 0) {
        batches.push({ eventId: id, ops: ops });
      }
    });
    if (batches.length > 0) {
      socket.send(JSON.stringify({ type: 'syncAll', batches: batches }));
    }
    socket.send(JSON.stringify({ type: 'reloadAll', eventIds: eventOrder.slice() }));
  }

  function applyAckedForEvent(eventId, acked) {
    if (!Array.isArray(acked) || acked.length === 0) {
      return;
    }
    if (eventId === currentEventId) {
      acked.forEach(function (id) {
        delete pendingById[id];
      });
      savePending();
      return;
    }
    const pending = loadPending(eventId);
    acked.forEach(function (id) {
      delete pending[id];
    });
    localStorage.setItem(storageKey(eventId), JSON.stringify(Object.values(pending)));
  }

  function applyPendingToEventPlayers(eventId) {
    const evt = eventsById[eventId];
    if (!evt || !Array.isArray(evt.players)) {
      return;
    }
    const pending = (eventId === currentEventId) ? pendingById : loadPending(eventId);
    Object.values(pending).forEach(function (op) {
      for (let i = 0; i < evt.players.length; i++) {
        const p = evt.players[i];
        if (op.optionId != null) {
          if (p.optionId === op.optionId) {
            if (op.status) {
              p.status = op.status;
            }
            return;
          }
        } else if (p.subId === op.subId && p.rowType !== 'OPTION') {
          if (typeof op.present === 'boolean') {
            p.present = op.present;
            p.attendanceAt = op.present ? new Date().toISOString() : null;
          }
          if (op.status) {
            p.status = op.status;
          }
          return;
        }
      }
    });
  }

  function refreshPlayersView() {
    if (viewingAll) {
      players = aggregatePlayers();
    } else {
      players = (eventsById[currentEventId] && eventsById[currentEventId].players) || [];
      Object.values(pendingById).forEach(function (op) {
        applyLocalOp(op, currentEventId);
      });
      if (eventsById[currentEventId]) {
        eventsById[currentEventId].players = players;
      }
    }
    render();
  }

  function applySnapshot(payload) {
    const snapshotEventId = payload.eventId == null ? currentEventId : payload.eventId;
    applyAckedForEvent(snapshotEventId, payload.acked);
    if (!Array.isArray(payload.players)) {
      updatePendingBadge();
      return;
    }
    if (eventsById[snapshotEventId]) {
      eventsById[snapshotEventId].players = payload.players;
      applyPendingToEventPlayers(snapshotEventId);
    }
    if (viewingAll || snapshotEventId === currentEventId) {
      refreshPlayersView();
    }
    saveEventsCache();
    updatePendingBadge();
  }

  function applySnapshots(payload) {
    if (!Array.isArray(payload.snapshots)) {
      return;
    }
    payload.snapshots.forEach(function (snap) {
      applySnapshot(snap);
    });
  }

  function switchToAll() {
    if (viewingAll) {
      return;
    }
    if (eventsById[currentEventId]) {
      eventsById[currentEventId].players = players;
    }
    savePending();
    saveEventsCache();
    viewingAll = true;
    players = aggregatePlayers();
    updateChrome();
    updatePendingBadge();
    render();
    saveEventsCache();
    if (socket && socket.readyState === WebSocket.OPEN) {
      socket.send(JSON.stringify({ type: 'reloadAll', eventIds: eventOrder.slice() }));
    }
  }

  function switchEvent(nextEventId) {
    if (nextEventId === ALL_EVENTS || nextEventId === 'all') {
      switchToAll();
      return;
    }
    nextEventId = parseInt(nextEventId, 10);
    if (!nextEventId || !eventsById[nextEventId]) {
      return;
    }
    if (!viewingAll && nextEventId === currentEventId) {
      return;
    }
    if (!viewingAll && eventsById[currentEventId]) {
      eventsById[currentEventId].players = players;
    }
    savePending();
    saveEventsCache();

    const sameWs = nextEventId === currentEventId;
    viewingAll = false;
    currentEventId = nextEventId;
    players = (eventsById[currentEventId].players || []).slice();
    pendingById = loadPending(currentEventId);
    Object.values(pendingById).forEach(function (op) {
      applyLocalOp(op, currentEventId);
    });
    updateChrome();
    updatePendingBadge();
    render();
    saveEventsCache();
    try {
      history.replaceState(null, '', CONTEXT_PATH + '/event/' + currentEventId + '/desk');
    } catch (e) {}

    if (sameWs) {
      return;
    }
    intentionalClose = true;
    clearReconnect();
    clearPing();
    if (socket) {
      try { socket.close(); } catch (e) {}
      socket = null;
    }
    intentionalClose = false;
    if (!reconnectPaused && navigator.onLine) {
      connect();
    }
  }

  function wsUrl() {
    const proto = location.protocol === 'https:' ? "wss:" : "ws:";
    return proto + '//' + location.host + CONTEXT_PATH + '/ws/event/' + currentEventId + '/desk?ticket=' + encodeURIComponent(DESK_TICKET);
  }

  function connect() {
    clearReconnect();
    if (intentionalClose || reconnectPaused) {
      return;
    }
    if (!navigator.onLine) {
      setStatus('offline');
      return;
    }
    if (currentStatus !== 'offline') {
      setStatus('connecting');
    }
    try {
      socket = new WebSocket(wsUrl());
    } catch (e) {
      setStatus('offline');
      scheduleReconnect();
      return;
    }
    socket.onopen = function () {
      setStatus('online');
      startPing();
      flushAllPendingAndReload();
    };
    socket.onmessage = function (ev) {
      let payload;
      try {
        payload = JSON.parse(ev.data);
      } catch (e) {
        return;
      }
      if (!payload || !payload.type) {
        return;
      }
      if (payload.type === 'snapshot') {
        applySnapshot(payload);
      } else if (payload.type === 'snapshots') {
        applySnapshots(payload);
      }
    };
    socket.onclose = function () {
      clearPing();
      socket = null;
      setStatus('offline');
      scheduleReconnect();
    };
    socket.onerror = function () {
      try { socket.close(); } catch (e) {}
    };
  }

  window.addEventListener('online', function () {
    reconnectPaused = false;
    if (!socket || socket.readyState === WebSocket.CLOSED || socket.readyState === WebSocket.CLOSING) {
      connect();
    }
  });
  window.addEventListener('offline', function () {
    reconnectPaused = true;
    clearReconnect();
    clearPing();
    intentionalClose = true;
    if (socket) {
      try { socket.close(); } catch (e) {}
      socket = null;
    }
    intentionalClose = false;
    setStatus('offline');
  });
  window.addEventListener('beforeunload', function () {
    intentionalClose = true;
    clearReconnect();
    clearPing();
    savePending();
    saveEventsCache();
    if (socket) {
      try { socket.close(); } catch (e) {}
    }
  });

  if (eventSelectEl) {
    eventSelectEl.addEventListener('change', function () {
      switchEvent(eventSelectEl.value);
    });
  }

  pendingById = loadPending(currentEventId);
  if (viewingAll) {
    eventOrder.forEach(applyPendingToEventPlayers);
    players = aggregatePlayers();
  } else {
    Object.values(pendingById).forEach(function (op) {
      applyLocalOp(op, currentEventId);
    });
  }
  historyEntries = loadHistory();
  fillEventSelect();
  updateChrome();
  searchEl.addEventListener('input', render);
  if (filterEl) {
    filterEl.addEventListener('change', render);
  }
  updatePendingBadge();
  render();
  renderHistory();
  saveEventsCache();
  connect();
})();
</script>
</body>
</html>