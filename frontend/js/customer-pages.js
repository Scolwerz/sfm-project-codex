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
