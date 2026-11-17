const CACHE='guhflix-shell-20261117-fullscreen1';
const CINEMA=['/pwa/local-player.js','/pwa/cinema.css','/pwa/cinema-web.css','/pwa/cinema-ui.js','/pwa/cinema-web.js','/pwa/cinema-assets/neon.png','/pwa/cinema-assets/stadium.png','/pwa/cinema-assets/universes.png','/pwa/cinema-assets/ui-regular.ttf','/pwa/cinema-assets/ui-bold.ttf','/pwa/cinema-assets/ui-bolditalic.ttf'];
const ASSETS=[...CINEMA,'/pwa/index.html','/pwa/','/pwa/style.css?v=20261111.1','/pwa/app.js?v=20261111.1','/pwa/local-player.js','/pwa/vendor/hls.light.min.js?v=1.7.4','/pwa/manifest.webmanifest','/pwa/icon-192.png','/pwa/icon-512.png','/pwa/apple-touch-icon.png'];
self.addEventListener('install',event=>event.waitUntil(caches.open(CACHE).then(cache=>cache.addAll(ASSETS))));
self.addEventListener('message',event=>{if(event.data==='ACTIVATE')self.skipWaiting()});
self.addEventListener('activate',event=>event.waitUntil(caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('guhflix-shell-')&&k!==CACHE).map(k=>caches.delete(k)))).then(()=>self.clients.claim())));
self.addEventListener('fetch',event=>{const url=new URL(event.request.url);if(event.request.method!=='GET'||url.origin!==self.location.origin||!url.pathname.startsWith('/pwa/'))return;
 if(event.request.mode==='navigate'){event.respondWith(fetch(event.request).then(response=>response.ok?response:caches.match('/pwa/').then(hit=>hit||response)).catch(()=>caches.match('/pwa/')));return}
 if(CINEMA.includes(url.pathname)){event.respondWith(fetch(event.request).then(response=>{if(response.ok)caches.open(CACHE).then(cache=>cache.put(event.request,response.clone()));return response}).catch(()=>caches.match(event.request)));return}
 if(ASSETS.includes(url.pathname+url.search)||ASSETS.includes(url.pathname)){event.respondWith(caches.match(event.request).then(hit=>hit||fetch(event.request)));}
});
