// API connection and shared session state.
const API = '/api';
const state = {
  user: null,
  page: 'overview',
  catalog: {
    brands: [],
    models: [],
    services: []
  },
  vehicles: [],
  tickets: [],
  filter: '',
  detail: null,
  mode: 'login',
  customerFilter: 'all'
};
const labels = {
  PENDING: 'Függőben',
  ACCEPTED: 'Elfogadva',
  IN_PROGRESS: 'Folyamatban',
  WAITING_FOR_PARTS: 'Alkatrészre vár',
  AWAITING_CUSTOMER: 'Visszaigazolásra vár',
  RESOLVED: 'Befejezve',
  CANCELLED: 'Lemondva',
  COMPLAINT: 'Ügyfél visszajelzése'
};
const esc = v => String(v ?? '')
  .replace(/[&<>"']/g, c => ({
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#39;'
  } [c]));
async function api(path, options = {}) {
  const res = await fetch(API + path, {
    credentials: 'same-origin',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {})
    },
    ...options
  });
  if (res.status === 204) return null;
  let data;
  try {
    data = await res.json()
  } catch {
    data = {}
  }
  if (!res.ok) throw Error(data.detail || data.message || 'A kérés nem sikerült.');
  return data
}

function toast(msg) {
  const t = document.querySelector('#toast');
  t.textContent = msg;
  t.classList.add('show');
  clearTimeout(window.toastTimer);
  window.toastTimer = setTimeout(() => t.classList.remove('show'), 3000)
}

function dt(v) {
  return new Date(v)
    .toLocaleString('hu-HU', {
      dateStyle: 'medium',
      timeStyle: 'short',
      timeZone: 'UTC'
    })
}

function badge(s) {
  return `<span class="badge ${s.toLowerCase()}">${esc(labels[s]||s)}</span>`
}
const navCustomer = [
  ['overview', '⌂', 'Áttekintés'],
  ['booking', '＋', 'Új időpont'],
  ['bookings', '▤', 'Foglalásaim'],
  ['cars', '◇', 'Autóim']
];
const navMechanic = [
  ['overview', '⌂', 'Áttekintés'],
  ['incoming', '↘', 'Beérkező ticketek'],
  ['active', '◷', 'Aktív munkák'],
  ['waiting', '✓', 'Visszaigazolásra vár'],
  ['resolved', '▣', 'Lezárt ticketek'],
  ['calendar', '▦', 'Időpontok']
];
async function boot() {
  try {
    state.user = await api('/auth/me');
    await loadCommon();
  } catch {}
  render()
}
async function loadCommon() {
  state.catalog = await api('/catalog');
  if (state.user?.role === 'CUSTOMER') state.vehicles = await api('/vehicles')
}
async function refresh() {
  if (!state.user) return;
  await loadCommon();
  render()
}
async function render() {
  if (!state.user) {
    document.querySelector('#app')
      .innerHTML = authView();
    return
  }
  document.querySelector('#app')
    .innerHTML = layout();
  const view = state.page === 'profile' ? profilePage() : state.user.role === 'CUSTOMER' ?
    customerPage() : state.page === 'detail' ? detailView() : mechanicPage();
  document.querySelector('.page-wrap')
    .innerHTML = await view
}

// Authentication screens and shared application layout.
function authView() {
  return `<main class="auth">
      <div class="auth-card">
      <div class="brand">
      <span class="brand-mark">A</span> Auto<span>Care</span>
      </div>
      <div class="eyebrow">SZERVIZ, EGYSZERŰEN</div>
      <h1>${state.mode==='login'?'Üdvözlünk újra!':'Hozz létre egy fiókot'}</h1>
      <p class="muted">${state.mode==='login'?'Jelentkezz be a szervizügyeid kezeléséhez.':'Foglalj időpontot néhány egyszerű lépésben.'}</p>
      <form id="auth-form" class="stack">${state.mode==='register'?'<label>Teljes név<input name="name" required autocomplete="name" placeholder="Kovács Anna"></label>':''}<label>E-mail cím<input type="${state.mode==='register'?'email':'text'}" name="email" required autocomplete="username" placeholder="${state.mode==='register'?'nev@email.hu':'E-mail vagy felhasználónév'}">
      </label>
      <label>Jelszó<input type="password" name="password" required minlength="${state.mode==='register'?'8':'1'}" autocomplete="current-password" placeholder="${state.mode==='register'?'Legalább 8 karakter':'Jelszó'}">
      </label>
      <button class="btn primary full">${state.mode==='login'?'Bejelentkezés':'Regisztráció'} <span>→</span>
      </button>
      </form>
      <p class="switch">${state.mode==='login'?'Még nincs fiókod?':'Már van fiókod?'} <button class="text-button" id="switch-auth">${state.mode==='login'?'Regisztráció':'Bejelentkezés'}</button>
      </p>
      <div class="demo-hint">Demó hozzáférés: <b>ugyfel@auto.hu</b> / <b>ugyfel</b>
      <br>
      <b>admin</b> / <b>admin</b>
      </div>
      </div>
      <div class="auth-aside">
      <div class="aside-orbit">
      </div>
      <div class="aside-copy">
      <span class="eyebrow">AUTÓSZERVIZ MENEDZSMENT</span>
      <h2>Minden szervizügy<br>egy helyen.</h2>
      <p>Átlátható foglalás. Követhető javítás. Nyugodt ügyintézés.</p>
      </div>
      <div class="aside-foot">AutoCare <span>·</span> Szerviz és ügyfél között</div>
      </div>
      </main>`
}

function layout() {
  let nav = state.user.role === 'CUSTOMER' ? navCustomer : navMechanic;
  return `<div class="shell">
      <aside class="sidebar">
      <div class="brand">
      <span class="brand-mark">A</span> Auto<span>Care</span>
      </div>
      <div class="side-label">MENÜ</div>
      <nav>${nav.map(([id,icon,name])=>`<button class="nav-item ${state.page===id?'active':''}" data-page="${id}">
      <span class="nav-icon">${icon}</span>${name}</button>`).join('')}</nav>
      <div class="side-bottom">
      <div class="side-user">
      <div class="avatar">${esc((state.user.name||'U')[0].toUpperCase())}</div>
      <div>
      <b>${esc(state.user.name)}</b>
      <small>${state.user.role==='CUSTOMER'?'Ügyfél':state.user.role==='ADMIN'?'Adminisztrátor':'Autószerelő'}</small>
      </div>
      </div>
      <button class="nav-item ${state.page==='profile'?'active':''}" data-page="profile">
      <span class="nav-icon">⚙</span>Profilom</button>
      <button class="nav-item logout" id="logout">
      <span class="nav-icon">↪</span>Kijelentkezés</button>
      </div>
      </aside>
      <main class="main">
      <header class="mobile-head">
      <div class="brand">
      <span class="brand-mark">A</span> Auto<span>Care</span>
      </div>
      <button id="mobile-menu" class="icon-button">☰</button>
      </header>
      <div class="page-wrap">
      </div>
      </main>
      </div>`
}

function heading(title, sub, action = '') {
  return `<div class="page-head">
      <div>
      <div class="eyebrow">${state.user.role==='CUSTOMER'?'ÜGYFÉLPORTÁL':'MŰHELYKEZELÉS'}</div>
      <h1>${title}</h1>
      <p>${sub}</p>
      </div>${action}</div>`
}

function card(title, inner, action = '') {
  return `<section class="panel">
      <div class="panel-head">
      <h2>${title}</h2>${action}</div>${inner}</section>`
}

function stat(label, value, icon, tint = 'blue', link = '') {
  return `<article class="stat-card">
      <div class="stat-top">
      <span>${label}</span>
      <span class="stat-icon ${tint}">${icon}</span>
      </div>
      <strong>${value}</strong>${link?`<button class="stat-link" data-page="${link}">Megtekintés <span>→</span>
      </button>`:''}</article>`
}

function vehicleLabel(v) {
  return `${v.brand} ${v.model} (${v.year})`
}

function ticketRows(items, mechanic = false) {
  if (!items.length)
  return `<div class="empty">
      <span>✧</span>
      <b>Nincs megjeleníthető foglalás</b>
      <small>Amint új munka érkezik, itt fogod látni.</small>
      </div>`;
  return `<div class="ticket-list">${items.map(t=>`<article class="ticket-row">
      <div class="ticket-car-icon">◇</div>
      <div class="ticket-info">
      <b>#${t.id} · ${esc(vehicleLabel(t.vehicle))}</b>
      <span>${esc(t.service.name)} <i>·</i> ${esc(dt(t.startAt))}</span>
      <small>${mechanic?`Ügyfél: ${esc(t.customer.name)}`:`${t.service.durationMinutes} perc · ${esc(t.description||'Szervizfoglalás')}`}</small>
      </div>
      <div class="ticket-end">${badge(t.status)}<button class="btn small ${mechanic?'':'ghost'}" data-detail="${t.id}">${mechanic?'Részletek':'Megtekintés'}</button>
      </div>
      </article>`).join('')}</div>`
}
// Customer screens: overview, cars, booking, and booking history.
async function customerPage() {
  if (state.page === 'overview') {
    let d = await api('/dashboard');
    return `<div>${heading(`Szia, ${esc(state.user.name.split(' ')[0])} 👋`,'Nézd meg, hogyan haladnak a szervizügyeid.','<button class="btn primary" data-page="booking">＋ Új időpont</button>')}<div class="stats">${stat('Elmentett autóim',d.vehicles,'◇','blue','cars')}${stat('Aktív foglalások',d.active,'◷','orange','bookings')}${stat('Lezárt munkák',d.closed,'✓','green','bookings')}</div>${card('Legutóbbi foglalások',ticketRows(d.recent),'<button class="btn ghost small" data-page="bookings">Összes foglalás →</button>')}</div>`
  }
  if (state.page === 'cars')
  return `${heading('Autóim','A mentett autókat a foglalásnál gyorsan kiválaszthatod.')}${carForm()}${card(`Mentett autók <span class="count">${state.vehicles.length}</span>`,state.vehicles.length?`<div class="saved-cars">${state.vehicles.map(v=>`<article class="car-row">
      <div class="car-symbol">◇</div>
      <div class="ticket-info">
      <b>${esc(vehicleLabel(v))}</b>
      <span>${esc(v.origin)} <i>·</i> ${esc(v.fuel||'')}</span>
      <small>${esc(v.licensePlate||'Rendszám nincs megadva')}</small>
      </div>
      <button class="btn danger small" data-delete-car="${v.id}">Eltávolítás</button>
      </article>`).join('')}</div>`:'<div class="empty"><span>◇</span><b>Még nincs elmentett autód</b><small>Adj hozzá járművet a gyorsabb foglaláshoz.</small></div>')}`;
  if (state.page === 'booking')
  return `${heading('Új időpont','Add meg az autó és a szerviz adatait, majd válassz szabad időpontot.')}<form id="booking-form">${state.vehicles.length?`<section class="panel">
      <div class="panel-head">
      <h2>1. Jármű kiválasztása</h2>
      </div>
      <label>Mentett autó<select name="savedVehicle">
      <option value="">Új autó adatainak megadása</option>${state.vehicles.map(v=>`<option value="${v.id}">${esc(vehicleLabel(v))}</option>`).join('')}</select>
      </label>
      </section>`:''}${carFields('book')}<section class="panel">
      <div class="panel-head">
      <h2>2. Szervizigény</h2>
      </div>
      <div class="form-grid">
      <label>Szerviz típusa<select name="serviceId" required>
      <option value="">Válassz szolgáltatást</option>${state.catalog.services.map(x=>`<option value="${x.id}">${esc(x.name)} · ${x.durationMinutes} perc</option>`).join('')}</select>
      </label>
      <label class="span-2">Hibaleírás<textarea name="description" rows="3" placeholder="Írd le röviden, mit tapasztalsz az autónál.">
      </textarea>
      </label>
      </div>
      </section>
      <section class="panel">
      <div class="panel-head">
      <h2>3. Időpont kiválasztása</h2>
      <span class="hint">Hétköznap, 08:00–17:00</span>
      </div>
      <div class="form-grid">
      <label>Dátum<input name="date" type="date" required min="${new Date(Date.now()+86400000).toISOString().slice(0,10)}">
      </label>
      <label>Elérhető időpontok<select name="time" required disabled>
      <option value="">Először válassz dátumot és szolgáltatást</option>
      </select>
      </label>
      </div>
      <p class="note">A foglalás függőben marad, amíg egy szerelő el nem fogadja.</p>
      <button class="btn primary">Időpontkérés elküldése <span>→</span>
      </button>
      </section>
      </form>`;
  if (state.page === 'bookings') {
    let all = await api('/tickets/mine');
    let views = state.customerFilter === 'all' ? all : all.filter(t => state.customerFilter ===
      'pending' ? t.status === 'PENDING' : state.customerFilter === 'active' ? !['PENDING',
        'RESOLVED', 'CANCELLED'
      ].includes(t.status) : ['RESOLVED', 'CANCELLED'].includes(t.status));
    return `${heading('Foglalásaim','Kövesd a foglalásaid és a javítások állapotát.')}${card('Szervizügyeid',`<div class="filters">${[['all','Összes'],['active','Aktív'],['closed','Lezárt']].map(([k,l])=>`<button class="filter ${state.customerFilter===k?'selected':''}" data-filter="${k}">${l}</button>`).join('')}</div>${ticketRows(views)}${views.map(t=>t.status==='AWAITING_CUSTOMER'?`<div class="confirm-card">
      <b>#${t.id} · Elkészült az autód?</b>
      <p>Kérjük, jelezd, hogy mindent rendben találtál-e.</p>
      <button class="btn primary small" data-confirm="${t.id}" data-approved="true">Minden rendben</button> <button class="btn ghost small" data-confirm="${t.id}" data-approved="false">Problémám van</button>
      </div>`:'').join('')}`)}`
  }
  return ''
}

function carFields(prefix) {
  const brandOptions = state.catalog.brands
    .map(brand => `<option value="${brand.id}" data-origin="${esc(brand.origin)}">${esc(brand.name)}</option>`)
    .join('');
  const yearOptions = Array.from({ length: 30 }, (_, index) => new Date().getFullYear() - index)
    .map(year => `<option>${year}</option>`)
    .join('');
  const fuelOptions = ['Benzin', 'Dízel', 'Hibrid', 'Plug-in hibrid', 'Elektromos', 'LPG / CNG']
    .map(fuel => `<option>${fuel}</option>`)
    .join('');
  const formAction = prefix === 'book'
    ? '<label class="check-row"><input type="checkbox" name="saveVehicle" value="true"> Az autó mentése a profilomba</label>'
    : '<button class="btn primary" style="margin-top:22px">＋ Autó hozzáadása</button>';

  return `
    <section class="panel">
      <div class="panel-head">
        <h2>${prefix === 'book' ? '1. Jármű adatai' : 'Új autó hozzáadása'}</h2>
      </div>
      <div class="form-grid">
        <label>
          Márka
          <select name="brandId" data-brand required>
            <option value="">Válassz márkát</option>
            ${brandOptions}
          </select>
        </label>
        <label>
          Származási régió
          <input data-origin readonly placeholder="Automatikusan kitöltjük">
        </label>
        <label>
          Modell
          <select name="modelId" data-model required disabled>
            <option value="">Válassz márkát</option>
          </select>
        </label>
        <label>
          Évjárat
          <select name="year" required>
            <option value="">Válassz</option>
            ${yearOptions}
          </select>
        </label>
        <label>
          Üzemanyag
          <select name="fuel" required>
            <option value="">Válassz</option>
            ${fuelOptions}
          </select>
        </label>
        <label>
          Motortípus
          <input name="engine" placeholder="Pl. 2.0 TDI">
        </label>
        <label>
          Rendszám (opcionális)
          <input name="licensePlate" placeholder="ABC-123">
        </label>
      </div>
      ${formAction}
    </section>
  `;
}

function carForm() {
  return `<form id="car-form">${carFields('car')}</form>`;
}

function profilePage() {
  return `${heading('Profilom','Személyes bejelentkezési adataid.')}${card('Fiókadatok',`<form id="profile-form" class="form-grid">
    <label>Teljes név<input name="name" value="${esc(state.user.name)}" required>
    </label>
    <label>E-mail cím<input value="${esc(state.user.email)}" disabled>
    <small>Az e-mail cím módosítása jelenleg nem támogatott.</small>
    </label>
    <div class="span-2 divider">
    </div>
    <label>Új jelszó<input type="password" name="password" minlength="8" placeholder="Hagyd üresen, ha nem módosítod">
    </label>
    <div class="span-2">
    <button class="btn primary">Adatok mentése</button>
    </div>
    </form>`)}`
}
// Mechanic screens: ticket queues, schedule, and ticket details.
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
document.addEventListener('click', async e => {
  const b = e.target.closest('button');
  if (!b) return;
  try {
    if (b.id === 'switch-auth') {
      state.mode = state.mode === 'login' ? 'register' : 'login';
      render()
    } else if (b.id === 'logout') {
      await api('/auth/logout', {
        method: 'POST'
      });
      state.user = null;
      state.page = 'overview';
      render()
    } else if (b.dataset.page) {
      state.page = b.dataset.page;
      state.filter = '';
      render()
    } else if (b.dataset.filter) {
      state.customerFilter = b.dataset.filter;
      render()
    } else if (b.dataset.detail) {
      await detailPage(b.dataset.detail)
    } else if (b.id === 'back') {
      state.page = state.lastPage || 'incoming';
      render()
    } else if (b.dataset.deleteCar) {
      await api(`/vehicles/${b.dataset.deleteCar}`, {
        method: 'DELETE'
      });
      toast('Az autót eltávolítottuk.');
      await refresh()
    } else if (b.dataset.status) {
      await api(`/tickets/${b.dataset.id}/status`, {
        method: 'PUT',
        body: JSON.stringify({
          status: b.dataset.status
        })
      });
      toast('A ticket állapotát frissítettük.');
      if (state.page === 'detail') {
        state.detail = await api(`/tickets/${b.dataset.id}`);
        render()
      } else render()
    } else if (b.dataset.confirm) {
      await api(`/tickets/${b.dataset.confirm}/confirm`, {
        method: 'PUT',
        body: JSON.stringify({
          approved: b.dataset.approved === 'true'
        })
      });
      toast(b.dataset.approved === 'true' ? 'Köszönjük a visszajelzést!' :
        'Jeleztük a szerelőnek, hogy további egyeztetés szükséges.');
      render()
    } else if (b.id === 'mobile-menu') document.querySelector('.sidebar')
      .classList.toggle('open')
  } catch (err) {
    toast(err.message)
  }
});
document.addEventListener('change', async e => {
  try {
    if (e.target.matches('[data-brand]')) {
      let opt = e.target.selectedOptions[0],
        form = e.target.closest('form');
      form.querySelector('[data-origin]')
        .value = opt.dataset.origin || '';
      let sel = form.querySelector('[data-model]'),
        brandId = e.target.value;
      let models = state.catalog.models.filter(m => String(m.brandId) === brandId);
      sel.innerHTML = '<option value="">Válassz modellt</option>' + models.map(m =>
          `<option value="${m.id}">${esc(m.name)}</option>`)
        .join('');
      sel.disabled = !brandId;
      if (models.length === 1) sel.value = models[0].id
    }
    if (e.target.name === 'savedVehicle') {
      let v = state.vehicles.find(x => String(x.id) === e.target.value);
      if (v) {
        let f = e.target.closest('form');
        f.querySelector('[data-brand]')
          .value = v.brandId;
        f.querySelector('[data-brand]')
          .dispatchEvent(new Event('change', {
            bubbles: true
          }));
        f.querySelector('[data-model]')
          .value = v.modelId;
        f.querySelector('[name=year]')
          .value = v.year;
        f.querySelector('[name=fuel]')
          .value = v.fuel;
        f.querySelector('[name=engine]')
          .value = v.engine;
        f.querySelector('[name=licensePlate]')
          .value = v.licensePlate
      }
    }
    if (e.target.name === 'date' || e.target.name === 'serviceId') {
      let f = e.target.closest('form'),
        date = f.elements.date?.value,
        service = f.elements.serviceId?.value,
        time = f.elements.time;
      if (date && service) {
        let slots = await api(`/availability?date=${date}&serviceId=${service}`);
        time.disabled = false;
        time.innerHTML = slots.length ? '<option value="">Válassz időpontot</option>' +
          slots.map(x => `<option value="${x}">${x}</option>`)
          .join('') : '<option value="">Nincs szabad időpont</option>'
      }
    }
  } catch (err) {
    toast(err.message)
  }
});
document.addEventListener('input', e => {
  if (e.target.id === 'ticket-search') {
    state.filter = e.target.value;
    const key = state.page;
    let items = state.tickets.filter(t => key === 'active' ? ['ACCEPTED', 'IN_PROGRESS',
          'WAITING_FOR_PARTS', 'COMPLAINT'
        ].includes(t.status) : key === 'incoming' ? t.status === 'PENDING' : key ===
        'waiting' ? t.status === 'AWAITING_CUSTOMER' : t.status === 'RESOLVED')
      .filter(t => [t.id, t.customer.name, t.customer.email, t.vehicle.brand, t.vehicle.model,
          t.service.name, t.description
        ].join(' ')
        .toLocaleLowerCase('hu-HU')
        .includes(state.filter.toLocaleLowerCase('hu-HU')));
    document.querySelector('#ticket-list')
      .innerHTML = ticketRows(items, true)
  }
});
document.addEventListener('submit', async e => {
  e.preventDefault();
  let f = e.target;
  try {
    if (f.id === 'auth-form') {
      let data = Object.fromEntries(new FormData(f));
      state.user = await api(`/auth/${state.mode==='login'?'login':'register'}`, {
        method: 'POST',
        body: JSON.stringify(data)
      });
      state.page = 'overview';
      await loadCommon();
      render()
    } else if (f.id === 'profile-form') {
      state.user = await api('/profile', {
        method: 'PUT',
        body: JSON.stringify(Object.fromEntries(new FormData(f)))
      });
      toast('A profiladatokat elmentettük.');
      render()
    } else if (f.id === 'car-form' || f.id === 'booking-form') {
      let d = Object.fromEntries(new FormData(f));
      let vehicleId = d.savedVehicle;
      if (!vehicleId) {
        let v = await api('/vehicles', {
          method: 'POST',
          body: JSON.stringify({
            brandId: d.brandId,
            modelId: d.modelId,
            year: d.year,
            fuel: d.fuel,
            engine: d.engine,
            licensePlate: d.licensePlate
          })
        });
        vehicleId = v.id;
        state.vehicles = await api('/vehicles')
      }
      if (f.id === 'car-form') {
        toast('Az autót elmentettük.');
        await refresh()
      } else {
        await api('/tickets', {
          method: 'POST',
          body: JSON.stringify({
            vehicleId,
            serviceId: d.serviceId,
            startAt: `${d.date}T${d.time}:00`,
            description: d.description
          })
        });
        if (!d.savedVehicle && !d.saveVehicle) await api(`/vehicles/${vehicleId}`, {
          method: 'DELETE'
        });
        state.page = 'bookings';
        toast('Időpontkérés elküldve, szerelői elfogadásra vár.');
        render()
      }
    }
  } catch (err) {
    toast(err.message)
  }
});
boot();
