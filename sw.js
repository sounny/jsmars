/**
 * JSMARS - Service Worker (Application Shell & Offline Cache)
 * Version: jsmars-shell-v1.4.3
 * 
 * Rules:
 * 1. Cache-first strategy for versioned owned local application shell assets.
 * 2. Navigation fallback to app shell (index.html) for any client route with query parameters.
 * 3. Stale-while-revalidate for CDN dependencies (Leaflet, Three.js, GeoTIFF).
 * 4. Network-first with graceful transparent tile / offline fallback for remote WMS tiles & APIs.
 * 5. Never precache unconstrained scientific rasters or gigabytes of tiles.
 * 6. User-prompted update flow via SKIP_WAITING to avoid interrupting active workflows.
 */

const CACHE_NAME = 'jsmars-shell-v1.4.3';

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
  './src/ui/Accordion.js',
  './src/ui/BodySelector.js',
  './src/ui/CollapsibleGroup.js',
  './src/ui/ColorStretchControl.js',
  './src/ui/FixedOverlays.js',
  './src/ui/KeyboardShortcuts.js',
  './src/ui/MobileSheet.js',
  './src/ui/PlanetaryScaleBar.js',
  './src/ui/SearchBar.js',
  './src/ui/SessionManager.js',
  './src/ui/Sidebar.js',
  './src/ui/StatusBar.js',
  './src/ui/layer-manager.js',
  './src/features/bands/BandMathEngine.js',
  './src/features/bands/BandMathPanel.js',
  './src/features/bookmarks/BookmarksTool.js',
  './src/features/crater-counting/CSFDEngine.js',
  './src/features/crater-counting/CraterLayer.js',
  './src/features/crater-counting/CraterTable.js',
  './src/features/export/ExportTool.js',
  './src/features/grid/GridLayer.js',
  './src/features/grid/GridPanel.js',
  './src/features/investigate/InvestigateTool.js',
  './src/features/krc/KRCEngine.js',
  './src/features/krc/KRCPanel.js',
  './src/features/mcd/MCDEngine.js',
  './src/features/mcd/MCDPanel.js',
  './src/features/measure/MeasureTool.js',
  './src/features/orbit/TrajectoryEngine.js',
  './src/features/orbit/TrajectoryPanel.js',
  './src/features/profile/EnhancedProfileTool.js',
  './src/features/profile/RadialProfileTool.js',
  './src/features/projections/ProjectionManager.js',
  './src/features/radar/RadarPanel.js',
  './src/features/radar/RadarSounderEngine.js',
  './src/features/sampling/SamplingTool.js',
  './src/features/shapes/ShapeLayer.js',
  './src/features/slider/MarsTime.js',
  './src/features/slider/TimeSlider.js',
  './src/features/stamp/StampQueryPanel.js',
  './src/features/threed/ThreeDPanel.js',
  './src/util/InteractionLogger.js',
  './src/util/URLStateEngine.js',
  './src/util/geo.js',
  './src/data/landing-sites.json',
  './src/data/landmarks.json'
];

// 1x1 transparent PNG fallback for missing offline tiles
const TRANSPARENT_PIXEL = new Uint8Array([
  0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0x00, 0x00, 0x00, 0x0d,
  0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
  0x08, 0x06, 0x00, 0x00, 0x00, 0x1f, 0x15, 0xc4, 0x89, 0x00, 0x00, 0x00,
  0x0a, 0x49, 0x44, 0x41, 0x54, 0x78, 0x9c, 0x63, 0x00, 0x01, 0x00, 0x00,
  0x05, 0x00, 0x01, 0x0d, 0x0a, 0x2d, 0xb4, 0x00, 0x00, 0x00, 0x00, 0x49,
  0x45, 0x4e, 0x44, 0xae, 0x42, 0x60, 0x82
]);

// Install: Precache owned application shell
self.addEventListener('install', (event) => {
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

  // Strategy A: Same-Origin local application shell
  if (url.origin === location.origin) {
    // Navigation requests (HTML documents with or without query strings)
    if (request.mode === 'navigate' || request.destination === 'document') {
      let targetShell = './index.html';
      if (url.pathname.endsWith('/about.html') || url.pathname.endsWith('about.html')) {
        targetShell = './about.html';
      } else if (url.pathname.endsWith('/services.html') || url.pathname.endsWith('services.html')) {
        targetShell = './services.html';
      }

      event.respondWith(
        caches.match(targetShell).then((cached) => {
          if (cached) {
            // Return cached application shell immediately; revalidate in background if online
            fetch(request).then((networkResponse) => {
              if (networkResponse && networkResponse.status === 200) {
                caches.open(CACHE_NAME).then((cache) => cache.put(targetShell, networkResponse));
              }
            }).catch(() => {/* offline or cancelled, silent */});
            return cached;
          }

          // Not in cache: fetch from network
          return fetch(request).then((networkResponse) => {
            if (networkResponse && networkResponse.status === 200) {
              const clone = networkResponse.clone();
              caches.open(CACHE_NAME).then((cache) => cache.put(targetShell, clone));
            }
            return networkResponse;
          }).catch(() => {
            // Network failure fallback
            return caches.match('./index.html').then((fallback) => {
              if (fallback) return fallback;
              return caches.match('./');
            });
          });
        })
      );
      return;
    }

    // Static assets (CSS, JS, SVG, JSON, icons) - Cache First with ignoreSearch
    event.respondWith(
      caches.match(request, { ignoreSearch: true }).then((cachedResponse) => {
        if (cachedResponse) {
          // Return cached asset, fetch background update if online
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
        }).catch((err) => {
          return caches.match(request, { ignoreSearch: true }).then((fallback) => {
            if (fallback) return fallback;
            if (request.destination === 'image') {
              return new Response(TRANSPARENT_PIXEL, {
                status: 200,
                headers: { 'Content-Type': 'image/png' }
              });
            }
            throw err;
          });
        });
      })
    );
    return;
  }

  // Strategy B: Third-Party CDNs (unpkg, cdnjs, jsdelivr) - Stale-While-Revalidate
  if (url.hostname.includes('unpkg.com') || url.hostname.includes('cdnjs.cloudflare.com') || url.hostname.includes('cdn.jsdelivr.net')) {
    event.respondWith(
      caches.match(request).then((cached) => {
        const fetchPromise = fetch(request).then((networkResponse) => {
          if (networkResponse && (networkResponse.status === 200 || networkResponse.type === 'opaque')) {
            const clone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, clone));
          }
          return networkResponse;
        }).catch(() => cached);
        return cached || fetchPromise;
      }).catch(() => caches.match(request))
    );
    return;
  }

  // Strategy C: WMS tiles, planetary APIs, ODE search - Network-First (Never precache blindly)
  event.respondWith(
    fetch(request).catch(() => {
      // If offline, return cached tile or transparent pixel fallback
      return caches.match(request).then((cached) => {
        if (cached) return cached;
        if (request.destination === 'image' || request.headers.get('accept')?.includes('image')) {
          return new Response(TRANSPARENT_PIXEL, {
            status: 200,
            statusText: 'Offline Transparent Tile',
            headers: { 'Content-Type': 'image/png' }
          });
        }
        return new Response(JSON.stringify({ error: 'Offline', offline: true }), {
          status: 503,
          statusText: 'Service Unavailable (Offline)',
          headers: { 'Content-Type': 'application/json' }
        });
      });
    })
  );
});

// Handle update messages from client
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});
