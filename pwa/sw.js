const CACHE='guhflix-shell-20261107-1';
const ASSETS=['/pwa/','/pwa/style.css?v=20261107.1','/pwa/app.js?v=20261107.1','/pwa/manifest.webmanifest','/pwa/icon-192.png','/pwa/icon-512.png','/pwa/apple-touch-icon.png'];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS))));
self.addEventListener('message',event=>{if(event.data==='ACTIVATE')self.skipWaiting()});
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('guhflix-shell-')&&k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{const url=new URL(event.request.url);if(event.request.method!=='GET'||url.origin!==self.location.origin||!url.pathname.startsWith('/pwa/'))return;
 if(event.request.mode==='navigate'){event.respondWith(fetch(event.request).then(response=>response.ok?response:caches.match('/pwa/').then(hit=>hit||response)).catch(()=>caches.match('/pwa/')));return}
 if(ASSETS.includes(url.pathname+url.search)||ASSETS.includes(url.pathname)){event.respondWith(caches.match(event.request).then(hit=>hit||fetch(event.request)));}
});
