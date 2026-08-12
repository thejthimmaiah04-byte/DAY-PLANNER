const CACHE = 'arc-v3';
const PRECACHE = ['./','./index.html','./manifest.json','./icon.svg',
  './icon-192.png','./icon-512.png',
  './fonts/inter_regular.ttf','./fonts/inter_medium.ttf','./fonts/inter_semibold.ttf',
  './fonts/inter_bold.ttf','./fonts/fraunces_medium.ttf','./fonts/fraunces_semibold.ttf'];

self.addEventListener('install', e => {
  e.waitUntil(caches.open(CACHE).then(c => c.addAll(PRECACHE).catch(()=>{})));
  self.skipWaiting();
});

self.addEventListener('activate', e => {
  e.waitUntil(caches.keys().then(ks => Promise.all(ks.filter(k=>k!==CACHE).map(k=>caches.delete(k)))));
  self.clients.claim();
});

self.addEventListener('fetch', e => {
  const url = e.request.url;
  // Always network-first for dynamic data
  if (url.includes('script.google.com') || url.includes('version.json') || url.includes('googleapis')) {
    e.respondWith(fetch(e.request).catch(() => new Response('', {status: 503})));
    return;
  }
  // Cache-first for static assets
  e.respondWith(caches.match(e.request).then(cached => {
    const network = fetch(e.request).then(res => {
      if (res && res.ok) caches.open(CACHE).then(c => c.put(e.request, res.clone()));
      return res;
    }).catch(()=>{});
    return cached || network;
  }));
});
