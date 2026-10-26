/**
 * JSMARS - Service Worker (Application Shell & Offline Cache)
 * Version: jsmars-shell-v1.5.2
 * 
 * Rules:
 * 1. Cache-first strategy for versioned owned local application shell assets.
 * 2. Navigation fallback to app shell (index.html) for any client route with query parameters.
 * 3. Stale-while-revalidate for small CDN dependencies (Leaflet, GeoTIFF).
 * 4. Network-only for CesiumJS. Do not precache or runtime-cache the multi-MB bundle, workers, or assets.
 * 5. Network-first with graceful transparent tile / offline fallback for remote WMS tiles & APIs.
 * 6. Never precache unconstrained scientific rasters or gigabytes of tiles.
 * 7. User-prompted update flow via SKIP_WAITING to avoid interrupting active workflows.
 */

const CACHE_NAME = 'jsmars-shell-v1.5.2';

const PRECACHE_ASSETS = [
  './',
  './about.html',
  './assets/icons/apple-touch-icon.png',
  './assets/icons/icon-192.png',
  './assets/icons/icon-512.png',
  './assets/icons/icon-maskable-192.png',
  './assets/icons/icon-maskable-512.png',
  './assets/icons/icon.svg',
  './assets/lunar-south-pole/manifest.json',
  './index.html',
  './jsmars_logo.svg',
  './manifest.webmanifest',
  './og-image.png',
  './services.html',
  './src/constants.js',
  './src/core/EventBus.js',
  './src/core/PluginRegistry.js',
  './src/core/ToolManager.js',
  './src/data/landing-sites.json',
  './src/data/landmarks.json',
  './src/features/bands/BandMathEngine.js',
  './src/features/bands/BandMathLayer.js',
  './src/features/bands/BandMathPanel.js',
  './src/features/bookmarks/BookmarksTool.js',
  './src/features/contour/ContourLayer.js',
  './src/features/contour/ContourPanel.js',
  './src/features/crater-counting/CSFDChart.js',
  './src/features/crater-counting/CSFDEngine.js',
  './src/features/crater-counting/CraterLayer.js',
  './src/features/crater-counting/CraterTable.js',
  './src/features/custom-map/CustomMapManager.js',
  './src/features/export/ExportTool.js',
  './src/features/globe/CesiumGlobe.js',
  './src/features/globe/ViewModeToggle.js',
  './src/features/globe/cesiumLoader.js',
  './src/features/globe/globeViewModel.js',
  './src/features/grid/GridLayer.js',
  './src/features/grid/GridPanel.js',
  './src/features/groundtrack/GroundTrackLayer.js',
  './src/features/groundtrack/GroundTrackPanel.js',
  './src/features/hillshade/HillshadeLayer.js',
  './src/features/hillshade/HillshadePanel.js',
  './src/features/investigate/InvestigateTool.js',
  './src/features/krc/KRCChart.js',
  './src/features/krc/KRCEngine.js',
  './src/features/krc/KRCPanel.js',
  './src/features/landing/LandingSitesLayer.js',
  './src/features/lunar-site/CadImport.js',
  './src/features/lunar-site/Exporters.js',
  './src/features/lunar-site/LunarSitePlanner.js',
  './src/features/lunar-site/PlannerCad.js',
  './src/features/lunar-site/PlannerCadExport.js',
  './src/features/lunar-site/PlannerCharts.js',
  './src/features/lunar-site/PlannerExport.js',
  './src/features/lunar-site/PlannerRenderers.js',
  './src/features/lunar-site/PlannerTools.js',
  './src/features/lunar-site/PolarGrid.js',
  './src/features/lunar-site/TerrainAnalysis.js',
  './src/features/lunar-site/lunar-site.css',
  './src/features/mcd/MCDChart.js',
  './src/features/mcd/MCDEngine.js',
  './src/features/mcd/MCDPanel.js',
  './src/features/measure/MeasureTool.js',
  './src/features/measure/MeasurementTable.js',
  './src/features/nomenclature/NomenclatureLayer.js',
  './src/features/nomenclature/NomenclatureTool.js',
  './src/features/orbit/TrajectoryEngine.js',
  './src/features/orbit/TrajectoryPanel.js',
  './src/features/places/PlacesManager.js',
  './src/features/profile/EnhancedProfileTool.js',
  './src/features/profile/ProfileChart.js',
  './src/features/profile/RadialProfileTool.js',
  './src/features/projections/ProjectionManager.js',
  './src/features/radar/RadarChart.js',
  './src/features/radar/RadarPanel.js',
  './src/features/radar/RadarSounderEngine.js',
  './src/features/sampling/SampleTable.js',
  './src/features/sampling/SamplingTool.js',
  './src/features/shapes/ShapeIO.js',
  './src/features/shapes/ShapeLayer.js',
  './src/features/shapes/ShapeTable.js',
  './src/features/shapes/StyleEditor.js',
  './src/features/slider/MarsTime.js',
  './src/features/slider/TimeSlider.js',
  './src/features/stamp/StampLayer.js',
  './src/features/stamp/StampQueryPanel.js',
  './src/features/threed/ThreeDEngine.js',
  './src/jmars-config.js',
  './src/jmars-map.js',
  './src/jmars-state.js',
  './src/jmars-vectors.js',
  './src/jmars-wms.js',
  './src/layers/GraticuleLayer.js',
  './src/layers/index.js',
  './src/pwa/PWAManager.js',
  './src/ui/Accordion.js',
  './src/ui/BodySelector.js',
  './src/ui/CollapsibleGroup.js',
  './src/ui/ColorStretchControl.js',
  './src/ui/FixedOverlays.js',
  './src/ui/InfoPanel.js',
  './src/ui/KeyboardShortcuts.js',
  './src/ui/MiniMap.js',
  './src/ui/MobileSheet.js',
  './src/ui/MosaicCatalog.js',
  './src/ui/NorthArrow.js',
  './src/ui/Panner.js',
  './src/ui/PlanetaryScaleBar.js',
  './src/ui/QuickActions.js',
  './src/ui/ResetViewControl.js',
  './src/ui/SearchBar.js',
  './src/ui/SessionManager.js',
  './src/ui/ScienceProvenance.js',
  './src/ui/Sidebar.js',
  './src/ui/StatusBar.js',
  './src/ui/layer-manager.js',
  './src/util/ColorRampEngine.js',
  './src/util/InteractionLogger.js',
  './src/util/URLStateEngine.js',
  './src/util/body.js',
  './src/util/geo.js',
  './src/util/mola-dem.js',
  './src/util/sounny-forms.js',
  './style.css',
  './vendor/leaflet-draw/images/spritesheet-2x.png',
  './vendor/leaflet-draw/images/spritesheet.png',
  './vendor/leaflet-draw/images/spritesheet.svg',
  './vendor/leaflet-draw/leaflet.draw.css',
  './vendor/leaflet-draw/leaflet.draw.js',
  './vendor/leaflet/images/layers-2x.png',
  './vendor/leaflet/images/layers.png',
  './vendor/leaflet/images/marker-icon-2x.png',
  './vendor/leaflet/images/marker-icon.png',
  './vendor/leaflet/images/marker-shadow.png',
  './vendor/leaflet/leaflet.css',
  './vendor/leaflet/leaflet.js'
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
      const pathname = url.pathname;
      const isRoot = pathname === '/' || pathname.endsWith('/index.html') || pathname.endsWith('index.html');
      const isAbout = pathname.endsWith('/about.html') || pathname.endsWith('about.html');
      const isServices = pathname.endsWith('/services.html') || pathname.endsWith('services.html');

      let targetShell = null;
      if (isRoot) targetShell = './index.html';
      else if (isAbout) targetShell = './about.html';
      else if (isServices) targetShell = './services.html';

      if (targetShell) {
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
              return caches.match('./index.html').then((fallback) => {
                if (fallback) return fallback;
                return caches.match('./');
              });
            });
          })
        );
        return;
      }

      // For all other HTML documents (docs, tests, guides): Network-first with isolated caching
      event.respondWith(
        fetch(request).then((networkResponse) => {
          if (networkResponse && networkResponse.status === 200) {
            const clone = networkResponse.clone();
            caches.open(CACHE_NAME).then((cache) => cache.put(request, clone));
          }
          return networkResponse;
        }).catch(() => {
          return caches.match(request).then((cached) => {
            if (cached) return cached;
            return caches.match('./index.html');
          });
        })
      );
      return;
    }

    // Large scientific rasters (any .tif / .tiff): network only, never cached in app shell.
    if (/\.tiff?$/i.test(url.pathname)) {
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

  // Remote Cloud-Optimized GeoTIFF range reads (e.g. USGS LOLA 5 m COG): bypass entirely.
  if (/\.tiff?$/i.test(url.pathname)) {
    return;
  }

  // CesiumJS is opt-in and multi-megabyte (script, workers, skybox assets).
  // Network only — never write it into the app-shell cache.
  if (/cesium@/i.test(url.pathname) || /\/cesium\//i.test(url.pathname) || url.hostname.includes('cesium.com')) {
    event.respondWith(fetch(request));
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
