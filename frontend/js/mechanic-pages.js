async function mechanicPage() {
  if (state.page === 'detail') return detailView();
  let d;
  if (state.page === 'overview') {
    d = await api('/dashboard');
    return `${heading('Műhely áttekintés','A csapatod munkái és az újonnan beérkezett szervizigények.') }<div class="stats">${stat('Beérkező ticketek',d.incoming,'↘','blue','incoming')}${stat('Aktív munkák',d.active,'◷','orange','active')}${stat('Visszaigazolásra vár',d.waiting,'✓','green','waiting')}</div>${card('Legújabb beérkező ticketek',ticketRows(d.recent,true),'<button class="btn ghost small" data-page="incoming">Összes ticket →</button>')}`
  }
  if (state.page === 'calendar') {
    let ts = await api('/tickets');
    return `${heading('Időpontok','A hozzád rendelt munkák időrendi áttekintése.')}${card('Saját időpontjaim',ticketRows(ts.filter(t=>t.mechanic?.id===state.user.id),true))}`
  }
  const map = {
    incoming: ['PENDING', 'Beérkező ticketek',
      'Új foglalások, amelyek szerelői elfogadásra várnak.'
    ],
    active: ['ACTIVE', 'Aktív munkák', 'A hozzád rendelt, folyamatban lévő munkák.'],
    waiting: ['AWAITING_CUSTOMER', 'Visszaigazolásra vár',
      'Lezárt munkák, amelyek ügyfél visszajelzésére várnak.'
    ],
    resolved: ['RESOLVED', 'Lezárt ticketek',
      'Az ügyfél által véglegesített munkák archívuma.'
    ]
  };
  const [key, title, sub] = map[state.page] || map.incoming;
  state.tickets = await api('/tickets');
  let list = state.tickets.filter(t => key === 'ACTIVE' ? ['ACCEPTED', 'IN_PROGRESS',
      'WAITING_FOR_PARTS', 'COMPLAINT'
    ].includes(t.status) : t.status === key)
    .filter(t => !state.filter || [t.id, t.customer.name, t.customer.email, t.vehicle.brand, t
        .vehicle.model, t.service.name, t.description
      ].join(' ')
      .toLocaleLowerCase('hu-HU')
      .includes(state.filter.toLocaleLowerCase('hu-HU')));
  return `${heading(title,sub)}${card(`${title} <span class="count">${list.length}</span>`,`<div class="search-wrap">
    <span>⌕</span>
    <input id="ticket-search" placeholder="Keresés ügyfélre, autóra, ticketre…" value="${esc(state.filter)}">
    </div>
    <div id="ticket-list">${ticketRows(list,true)}</div>`)}`
}
async function detailPage(id) {
  state.lastPage = state.page;
  state.detail = await api(`/tickets/${id}`);
  state.page = 'detail';
  render()
}

function detailView() {
  let t = state.detail;
  if (!t) return '';
  let own = t.mechanic?.id === state.user.id,
    actions = '';
  if (t.status === 'PENDING') actions =
    `<button class="btn primary" data-status="ACCEPTED" data-id="${t.id}">Ticket elfogadása</button>`;
  else if (own && t.status === 'ACCEPTED') actions =
    `<button class="btn ghost" data-status="PENDING" data-id="${t.id}">Munka leadása</button>
    <button class="btn primary" data-status="IN_PROGRESS" data-id="${t.id}">Munka indítása</button>`;
  else if (own && t.status === 'IN_PROGRESS') actions =
    `<button class="btn ghost" data-status="WAITING_FOR_PARTS" data-id="${t.id}">Alkatrészre vár</button>
    <button class="btn primary" data-status="AWAITING_CUSTOMER" data-id="${t.id}">Munka lezárása</button>`;
  else if (own && t.status === 'WAITING_FOR_PARTS') actions =
    `<button class="btn primary" data-status="IN_PROGRESS" data-id="${t.id}">Munka folytatása</button>`;
  else if (own && t.status === 'COMPLAINT') actions =
    `<button class="btn primary" data-status="IN_PROGRESS" data-id="${t.id}">Munka újranyitása</button>`;
  return `<button class="back-link" id="back">← Vissza a listához</button>${heading(`Ticket #${t.id}`,'A foglalás és a javítás részletei.')}${card('Munkalap',`<div class="detail-title">
    <h2>${esc(vehicleLabel(t.vehicle))}</h2>${badge(t.status)}</div>
    <div class="detail-grid">${[['Ügyfél',t.customer.name],['E-mail',t.customer.email],['Jármű származása',t.vehicle.origin],['Rendszám',t.vehicle.licensePlate||'—'],['Szerviz típusa',t.service.name],['Becsült időtartam',`${t.service.durationMinutes} perc`],['Foglalási időpont',dt(t.startAt)],['Hozzárendelt szerelő',t.mechanic?.name||'Nincs hozzárendelve']].map(([a,b])=>`<div class="detail-cell">
    <small>${a}</small>
    <b>${esc(b)}</b>
    </div>`).join('')}<div class="detail-cell span-2">
    <small>Hibaleírás</small>
    <b>${esc(t.description||'Nincs megadott leírás.')}</b>
    </div>
    </div>
    <div class="detail-actions">${actions}</div>`)}`
}
// Delegate actions so they continue working after each page render.
