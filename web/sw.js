const CACHE = 'arc-v4';
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
  if (url.includes('script.google.com') || url.includes('version.json') || url.includes('googleapis')) {
    e.respondWith(fetch(e.request).catch(() => new Response('', {status: 503})));
    return;
  }
  e.respondWith(caches.match(e.request).then(cached => {
    const network = fetch(e.request).then(res => {
      if (res && res.ok) caches.open(CACHE).then(c => c.put(e.request, res.clone()));
      return res;
    }).catch(()=>{});
    return cached || network;
  }));
});

/* ── Priority widget notification ─────────────────────────────────────────── */

self.addEventListener('message', e => {
  if (e.data && e.data.type === 'priorityWidget') showPriorityWidget(e.data);
});

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

async function showPriorityWidget(data) {
  const { today, tasks: all = [], nowMin = 0,
          nowTask, nextTask, doneCount = 0, totalCount = 0 } = data;

  const PRI   = { HIGH: 3, MEDIUM: 2, LOW: 1 };
  const EMOJI = { HIGH: '🔴', MEDIUM: '🟡', LOW: '⚪' };

  const pending = all
    .filter(t => !t.done && t.due === today)
    .sort((a, b) => (PRI[b.priority] || 0) - (PRI[a.priority] || 0));

  if (!pending.length && !doneCount) {
    const existing = await self.registration.getNotifications({ tag: 'arc-widget' });
    existing.forEach(n => n.close());
    return;
  }

  if (!pending.length && doneCount) {
    self.registration.showNotification('ARC — All done today 🎉', {
      body: `${doneCount} task${doneCount > 1 ? 's' : ''} completed. Great work!`,
      tag: 'arc-widget', icon: '/DAY-PLANNER/web/icon-192.png',
      badge: '/DAY-PLANNER/web/icon-192.png', silent: true, renotify: false
    });
    return;
  }

  function fmtMin(m) {
    const h = Math.floor(m / 60) % 12 || 12;
    const min = String(m % 60).padStart(2, '0');
    return `${h}:${min} ${Math.floor(m / 60) >= 12 ? 'PM' : 'AM'}`;
  }

  // NOW line
  const nowLine = nowTask ? `NOW  ${nowTask.title}` : 'NOW  Free';

  // NEXT line with countdown
  let nextLine = 'NEXT  —';
  if (nextTask) {
    const gap = nextTask.start - nowMin;
    const when = gap <= 0 ? 'now' : gap < 60 ? `in ${gap}m` : `at ${fmtMin(nextTask.start)}`;
    nextLine = `NEXT  ${nextTask.title}  (${when})`;
  }

  // Progress
  const pct = totalCount ? Math.round(doneCount / totalCount * 100) : 0;
  const progressLine = `${doneCount} of ${totalCount} done · ${pct}%`;

  // Priority task list (top 4)
  const top   = pending.slice(0, 4);
  const extra = pending.length > 4 ? `\n+${pending.length - 4} more` : '';
  const taskLines = top.map(t => `${EMOJI[t.priority] || '•'} ${t.title}`).join('\n');

  const title = `ARC · ${progressLine}`;
  const body  = `${nowLine}\n${nextLine}\n─────\n${taskLines}${extra}`;

  self.registration.showNotification(title, {
    body,
    tag:               'arc-widget',
    icon:              '/DAY-PLANNER/web/icon-192.png',
    badge:             '/DAY-PLANNER/web/icon-192.png',
    silent:            true,
    renotify:          false,
    requireInteraction: false
  });
}
