// Service Worker simples para suporte PWA no Android
const CACHE_NAME = 'compra-coletiva-v1';

self.addEventListener('install', (event) => {
  self.skipWaiting();
});

self.addEventListener('activate', (event) => {
  event.waitUntil(self.clients.claim());
});

self.addEventListener('fetch', (event) => {
  // Pass-through padrão para garantir comunicação dinâmica com o backend Spring Boot
  event.respondWith(
    fetch(event.request).catch(() => caches.match(event.request))
  );
});
