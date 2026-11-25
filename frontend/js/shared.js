// API connection and shared session state.
const API = '/api';
const state = {
  user: null,
  page: document.body.dataset.page || 'overview',
  catalog: {
    brands: [],
    models: [],
    services: []
  },
  vehicles: [],
  tickets: [],
  filter: '',
  detail: null,
  mode: document.body.dataset.page === 'register' ? 'register' : 'login',
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
    if (state.page === 'login' || state.page === 'register') {
      window.location.replace(state.user.role === 'CUSTOMER' ? '/dashboard.html' : '/incoming-tickets.html');
      return;
    }
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
