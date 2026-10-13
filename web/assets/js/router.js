// router.js — Simple hash-based SPA router
'use strict';

const Router = (() => {
  const _routes  = {};
  let   _current = null;

  function _navigate(hash) {
    const id = hash.replace('#', '') || 'home';
    if (_current === id) return;

    // Hide all pages
    document.querySelectorAll('.page-content').forEach(p => p.classList.remove('active'));
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));

    // Show target page
    const page = document.getElementById(`page-${id}`);
    if (page) page.classList.add('active');

    // Activate nav item
    const navItem = document.querySelector(`[data-route="${id}"]`);
    if (navItem) navItem.classList.add('active');

    // Update header title
    const title = document.getElementById('header-title');
    const titles = { home: 'Dashboard Monitoring', control: 'Kalibrasi & Kontrol', camera: 'Kamera', data: 'Data Sensor' };
    if (title) title.textContent = titles[id] || 'SIPANDU TAMBAK';

    // Run route handler
    if (_routes[id]) _routes[id]();
    _current = id;
  }

  return {
    on(route, fn) { _routes[route] = fn; },

    init() {
      window.addEventListener('hashchange', () => _navigate(location.hash));
      _navigate(location.hash || '#home');

      // Nav item click handlers
      document.querySelectorAll('.nav-item[data-route]').forEach(item => {
        item.addEventListener('click', () => {
          location.hash = '#' + item.dataset.route;
          // Close mobile sidebar
          document.querySelector('.sidebar').classList.remove('open');
          document.querySelector('.sidebar-overlay').classList.remove('active');
        });
      });
    },

    go(route) { location.hash = '#' + route; },
    current()  { return _current; },
  };
})();

window.Router = Router;
