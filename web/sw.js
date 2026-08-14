const CACHE = 'arc-v5';
const PRECACHE = ['./','./index.html','./manifest.json','./icon.svg',
  './icon-192.png','./icon-512.png',
  './fonts/inter_regular.ttf','./fonts/inter_medium.ttf','./fonts/inter_semibold.ttf',
  './fonts/inter_bold.ttf','./fonts/fraunces_medium.ttf','./fonts/fraunces_semibold.ttf'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(PRECACHE).catch(()=>{})));
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(
    caches.keys()
      .then(ks => Promise.all(ks.filter(k => k !== CACHE).map(k => caches.delete(k))))
      .then(() => clients.matchAll({ type: 'window', includeUncontrolled: true }))
      .then(all => all.forEach(c => c.postMessage({ type: 'SW_UPDATED' })))
  );
  self.clients.claim();
});

self.addEventListener('fetch', e => {
  const url = e.request.url;
  // Always bypass cache for external requests
  if (url.includes('script.google.com') || url.includes('version.json') || url.includes('googleapis')) {
    e.respondWith(fetch(e.request).catch(() => new Response('', {status: 503})));
    return;
  }
  // index.html always network-first so new deployments take effect immediately
  if (url.endsWith('/') || url.includes('index.html')) {
    e.respondWith(
      fetch(e.request).then(res => {
        if (res && res.ok) caches.open(CACHE).then(c => c.put(e.request, res.clone()));
        return res;
      }).catch(() => caches.match(e.request))
    );
    return;
  }
  // Everything else: cache-first
  e.respondWith(caches.match(e.request).then(cached => {
    const network = fetch(e.request).then(res => {
      if (res && res.ok) caches.open(CACHE).then(c => c.put(e.request, res.clone()));
      return res;
    }).catch(()=>{});
    return cached || network;
  }));
});

// Tap on any task reminder notification → open the app
self.addEventListener('notificationclick', e => {
  e.notification.close();
  e.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(list => {
      const open = list.find(c => c.url.includes('/DAY-PLANNER/'));
      if (open) return open.focus();
      return clients.openWindow('/DAY-PLANNER/web/');
    })
  );
});
