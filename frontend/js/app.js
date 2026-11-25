document.addEventListener('click', async e => {
  const b = e.target.closest('button');
  if (!b) return;
  try {
    if (b.id === 'switch-auth') {
      window.location.href = state.mode === 'login' ? '/register.html' : '/login.html'
    } else if (b.id === 'logout') {
      await api('/auth/logout', {
        method: 'POST'
      });
      state.user = null;
      window.location.href = '/login.html'
    } else if (b.dataset.page) {
      const pages = { overview: '/dashboard.html', booking: '/booking.html', bookings: '/bookings.html', cars: '/cars.html', incoming: '/incoming-tickets.html', active: '/active-jobs.html', waiting: '/waiting-confirmation.html', resolved: '/resolved-tickets.html', calendar: '/calendar.html', profile: '/profile.html' };
      if (b.dataset.page === 'detail') {
        state.lastPage = state.page;
        state.page = 'detail';
        state.filter = '';
        render();
      } else if (pages[b.dataset.page]) window.location.href = pages[b.dataset.page]
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
      window.location.href = state.user.role === 'CUSTOMER' ? '/dashboard.html' : '/incoming-tickets.html'
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
        window.location.href = '/bookings.html';
        toast('Időpontkérés elküldve, szerelői elfogadásra vár.');
        render()
      }
    }
  } catch (err) {
    toast(err.message)
  }
});
boot();
