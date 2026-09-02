/**
 * JSMARS - Service Worker (Application Shell & Offline Cache)
 * Version: jsmars-shell-v1.0.0
 * 
 * Rules:
 * 1. Cache-first strategy for versioned owned local application shell assets.
 * 2. Stale-while-revalidate for CDN dependencies (Leaflet, Three.js).
 * 3. Network-first with graceful offline fallback for remote WMS tiles & APIs.
 * 4. Never precache unconstrained scientific rasters or gigabytes of tiles.
 */

const CACHE_NAME = 'jsmars-shell-v1.4.2';

const PRECACHE_ASSETS = [
  './',
  './index.html',
  './about.html',
  './services.html',
  './manifest.webmanifest',
  './style.css',
  './jsmars_logo.svg',
  './og-image.png',
  './assets/og-image.png',
  './assets/icons/icon-192.png',
  './assets/icons/icon-512.png',
  './assets/icons/icon-maskable-192.png',
  './assets/icons/icon-maskable-512.png',
  './assets/icons/apple-touch-icon.png',
  './src/constants.js',
  './src/jmars-config.js',
  './src/jmars-map.js',
  './src/jmars-state.js',
  './src/jmars-vectors.js',
  './src/jmars-wms.js',
  './src/pwa/PWAManager.js',
  './src/ui/MobileSheet.js',
  './src/ui/layer-manager.js',
  './src/ui/BodySelector.js',
  './src/ui/SessionManager.js',
  './src/layers/index.js',
  './src/layers/GraticuleLayer.js',
  './src/util/geo.js',
  './src/util/URLStateEngine.js',
  './src/data/landing-sites.json',
  './src/data/landmarks.json'
];

// Install: Precache owned application shell
self.addEventListener('install', (event) => {
  self.skipWaiting();
  event.waitUntil(
    caches.open(CACHE_NAME).then(async (cache) => {
      console.log('[SW] Precaching JSMARS application shell...');
      // Use individual add requests so single optional asset failures do not abort installation
      await Promise.allSettled(
        PRECACHE_ASSETS.map((url) =>
          cache.add(url).catch((err) => {
            console.warn('[SW] Failed to precache:', url, err);
          })
        )
      );
    })
  );
});

// Activate: Clean up old cache versions
self.addEventListener('activate', (event) => {
  event.waitUntil(
    caches.keys().then((keys) =>
      Promise.all(
        keys.map((key) => {
          if (key !== CACHE_NAME) {
            console.log('[SW] Deleting legacy cache:', key);
            return caches.delete(key);
          }
        })
      )
    ).then(() => self.clients.claim())
  );
});

// Fetch: Apply distinct strategies depending on request destination
self.addEventListener('fetch', (event) => {
  const request = event.request;
  const url = new URL(request.url);

  // Ignore non-GET requests
  if (request.method !== 'GET') return;

  // Strategy A: Same-Origin local application shell (Cache-First, fallback to network)
  if (url.origin === location.origin) {
    event.respondWith(
      caches.match(request).then((cachedResponse) => {
        if (cachedResponse) {
          // Return cached response, fetch background update if online
          fetch(request).then((networkResponse) => {
            if (networkResponse && networkResponse.status === 200) {
              caches.open(CACHE_NAME).then((cache) => cache.put(request, networkResponse));
            }
          }).catch(() => {/* offline, silent */});
          return cachedResponse;
        }
        return fetch(request).then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const responseToCache = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, responseToCache));
          }
          return networkResponse;
        });
      })
    );
    return;
  }

  // Strategy B: Third-Party CDNs (unpkg, cdnjs) - Stale-While-Revalidate
  if (url.hostname.includes('unpkg.com') || url.hostname.includes('cdnjs.cloudflare.com')) {
    event.respondWith(
      caches.match(request).then((cached) => {
        const fetchPromise = fetch(request).then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const clone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, clone));
          }
          return networkResponse;
        }).catch(() => cached);
        return cached || fetchPromise;
      })
    );
    return;
  }

  // Strategy C: WMS tiles, planetary APIs, ODE search - Network-First (Never precache blindly)
  event.respondWith(
    fetch(request).catch(() => {
      // If offline and request is an image/tile, return transparent pixel or cached fallback
      return caches.match(request);
    })
  );
});

// Handle update messages from client
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});
