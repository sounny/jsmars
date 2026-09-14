import { EVENTS } from '../constants.js';
import { formatLatLon } from '../util/geo.js';
import { molaDem } from '../util/mola-dem.js';
import { jmarsState } from '../jmars-state.js';

/**
 * @module StatusBar
 * @description Displays prominent coordinates, elevation, zoom level, and scale in the bottom bar
 * and provides a floating on-map Live Coordinate HUD for instant visibility.
 * Supports multiple coordinate formats matching JMARS desktop:
 * - East 180 (-180 to 180)
 * - East 360 (0 to 360)
 * - DMS (degrees, minutes, seconds)
 */
export class StatusBar {
  /**
   * Create a new StatusBar.
   * @param {L.Map} map - Leaflet map instance
   * @param {string} containerId - DOM id of the status bar container
   */
  constructor(map, containerId) {
    this.map = map;
    this.container = document.getElementById(containerId);

    // Bail out early if container is missing
    if (!this.container) return;

    /** @type {number} Index into this.formats for the active format */
    this.formatIndex = 0;

    /**
     * Available coordinate display formats.
     * @type {Array<{label: string, lonFormat: string, notation: string, precision: number}>}
     */
    this.formats = [
      { label: 'E180', lonFormat: 'east180', notation: 'decimal', precision: 4 },
      { label: 'E360', lonFormat: 'east360', notation: 'decimal', precision: 4 },
      { label: 'DMS', lonFormat: 'east180', notation: 'dms', precision: 4 }
    ];

    /** @type {string} Current planetary body key */
    this.currentBody = (jmarsState.get('body') || 'mars').toLowerCase();

    /** @type {boolean} Tracks pending rAF frame for mousemove throttling */
    this._pendingFrame = false;

    /** @type {number|null} Current elevation debounce timer */
    this._elevTimer = null;

    this.initUI();
    this.initTileTracker();
    this.bindEvents();
    this.update();
  }

  /**
   * Build the status bar DOM elements, attach on-map HUD, and attach the Leaflet scale control.
   * @private
   */
  initUI() {
    // 1. Bottom Status Bar
    this.container.innerHTML = `
      <div class="status-item status-coords-pill" id="status-coords" title="Live Cursor Coordinates (Click to copy)" style="cursor: pointer;">
        <span style="color: #38bdf8; font-weight: 600;">📍 Lat:</span> <span id="status-lat-val" style="color: #e2e8f0; font-family: monospace; font-weight: 600;">0.0000°</span>
        <span style="color: #fbbf24; font-weight: 600; margin-left: 8px;">Lon:</span> <span id="status-lon-val" style="color: #e2e8f0; font-family: monospace; font-weight: 600;">0.0000°</span>
      </div>
      <div class="status-item status-elev-pill" id="status-elev" style="font-family: monospace; font-size: 11px; color: #4ade80;">
        <span style="color: #94a3b8;">Elev:</span> <span id="status-elev-val">0 m</span>
      </div>
      <button class="coord-format-btn" id="status-coord-format" title="Click to cycle coordinate format (E180 / E360 / DMS)">E180</button>
      <div class="status-item" id="status-zoom" style="font-family: monospace; color: #cbd5e1;">Zoom: 0</div>
      <div class="status-item status-load-pill ready" id="status-load" role="status" aria-live="polite" title="Map Data Load Status">
        <span class="status-load-icon" id="status-load-icon">
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
        </span>
        <span class="status-load-text" id="status-load-text">Loaded</span>
      </div>
      <div class="status-item" id="status-scale"></div>
    `;

    this.coordsEl = this.container.querySelector('#status-coords');
    this.latValEl = this.container.querySelector('#status-lat-val');
    this.lonValEl = this.container.querySelector('#status-lon-val');
    this.elevValEl = this.container.querySelector('#status-elev-val');
    this.formatBtn = this.container.querySelector('#status-coord-format');
    this.zoomEl = this.container.querySelector('#status-zoom');
    this.scaleEl = this.container.querySelector('#status-scale');
    this.loadPillEl = this.container.querySelector('#status-load');
    this.loadIconEl = this.container.querySelector('#status-load-icon');
    this.loadTextEl = this.container.querySelector('#status-load-text');

    // Remove any existing floating HUD badge if present
    const mapContainer = this.map.getContainer();
    const existingHud = mapContainer ? mapContainer.querySelector('#map-coord-hud') : null;
    if (existingHud) {
      existingHud.remove();
    }

    // Leaflet Scale Control
    this.scaleControl = L.control.scale({
      position: 'bottomleft',
      maxWidth: 200,
      metric: true,
      imperial: false
    });

    this.scaleControl.addTo(this.map);
    const scaleContainer = this.scaleControl.getContainer();
    this.scaleEl.appendChild(scaleContainer);

    scaleContainer.classList.remove('leaflet-bottom', 'leaflet-left', 'leaflet-control');
    scaleContainer.style.margin = '0';
  }

  /**
   * Bind map and UI event listeners.
   * @private
   */
  bindEvents() {
    // Throttle mousemove with requestAnimationFrame
    this.map.on('mousemove', (e) => {
      if (this._pendingFrame) return;
      this._pendingFrame = true;
      requestAnimationFrame(() => {
        this.updateCoords(e.latlng);
        this._pendingFrame = false;
      });
    });

    this.map.on('move', () => {
      // If mouse is not moving, show center coordinates
      if (!this._pendingFrame) {
        this.updateCoords(this.map.getCenter(), true);
      }
    });

    this.map.on('zoomend', () => this.updateZoom());

    // Click on status coordinates to copy
    this.coordsEl.addEventListener('click', () => {
      const text = `${this.latValEl.textContent}, ${this.lonValEl.textContent}`;
      navigator.clipboard.writeText(text).then(() => {
        const origLat = this.latValEl.textContent;
        this.latValEl.textContent = 'Copied!';
        setTimeout(() => {
          this.latValEl.textContent = origLat;
        }, 1200);
      }).catch(() => {});
    });

    // Format cycle button
    this.formatBtn.addEventListener('click', () => {
      this.formatIndex = (this.formatIndex + 1) % this.formats.length;
      const fmt = this.formats[this.formatIndex];
      this.formatBtn.textContent = fmt.label;

      document.dispatchEvent(new CustomEvent(EVENTS.COORD_FORMAT_CHANGED, {
        detail: fmt
      }));

      this.updateCoords(this.map.getCenter());
    });

    // Listen for body changes
    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      this.currentBody = e?.detail?.body || 'mars';
      this.updateCoords(this.map.getCenter());
    });

    // Listen for coordinate format changes from ProjectionManager
    document.addEventListener(EVENTS.COORD_FORMAT_CHANGED, (e) => {
      if (e.detail?.lonFormat) {
        const found = this.formats.findIndex(f => f.lonFormat === e.detail.lonFormat);
        if (found !== -1) {
          this.formatIndex = found;
          this.formatBtn.textContent = this.formats[found].label;
          this.updateCoords(this.map.getCenter());
        }
      }
    });
  }

  /**
   * Update the coordinate display for the given latlng.
   * @param {L.LatLng} latlng - Position
   * @param {boolean} [isCenter=false] - Whether this is the map center
   */
  updateCoords(latlng, isCenter = false) {
    if (!latlng) return;
    const fmt = this.formats[this.formatIndex];
    const formatted = formatLatLon(latlng.lat, latlng.lng, {
      ...fmt,
      body: this.currentBody
    });

    // Add prefix labels for decimal formats
    if (fmt.notation === 'decimal') {
      const parts = formatted.split(', ');
      this.latValEl.textContent = `${parts[0]}°`;
      this.lonValEl.textContent = `${parts[1]}°`;
    } else {
      this.latValEl.textContent = formatted;
      this.lonValEl.textContent = '';
    }

    // Debounced Elevation Sampling (Mars only)
    if (this.currentBody === 'mars') {
      clearTimeout(this._elevTimer);
      this._elevTimer = setTimeout(() => {
        molaDem.getElevation(latlng.lat, latlng.lng).then(elev => {
          if (Number.isFinite(elev)) {
            const elevStr = `${Math.round(elev)} m (${(elev / 1000).toFixed(2)} km)`;
            if (this.elevValEl) this.elevValEl.textContent = elevStr;
          } else {
            if (this.elevValEl) this.elevValEl.textContent = '--';
          }
        }).catch(() => {});
      }, 150);
    } else {
      if (this.elevValEl) this.elevValEl.textContent = 'N/A';
    }
  }

  /**
   * Update the zoom level display.
   */
  updateZoom() {
    this.zoomEl.textContent = `Zoom: ${this.map.getZoom()}`;
  }

  /**
   * Perform a full UI update (zoom, etc.).
   */
  update() {
    this.updateZoom();
    this.updateCoords(this.map.getCenter(), true);
  }

  /**
   * Attach tile lifecycle listeners to map layers to track active tile requests.
   * @private
   */
  initTileTracker() {
    this.pendingTiles = 0;
    this.failedTiles = 0;
    this._statusDebounce = null;
    this._trackedLayers = new Set();

    const trackLayer = (layer) => {
      if (!layer || typeof layer.on !== 'function' || this._trackedLayers.has(layer)) return;
      // Identify tile layers (standard XYZ and WMS tile layers)
      const isTileLayer = (window.L?.TileLayer && layer instanceof L.TileLayer) ||
                          typeof layer.getTileUrl === 'function' ||
                          layer._url;
      if (!isTileLayer) return;

      this._trackedLayers.add(layer);

      layer.on('tileloadstart', () => {
        this.pendingTiles++;
        this.renderLoadStatus();
      });

      layer.on('tileload', () => {
        this.pendingTiles = Math.max(0, this.pendingTiles - 1);
        this.renderLoadStatus();
      });

      layer.on('tileerror', () => {
        this.pendingTiles = Math.max(0, this.pendingTiles - 1);
        this.failedTiles++;
        this.renderLoadStatus();
      });

      layer.on('load', () => {
        // When layer fires 'load', clear pending count if layer has completed
        this.renderLoadStatus();
      });
    };

    // Track layers already present on map
    if (typeof this.map.eachLayer === 'function') {
      this.map.eachLayer((layer) => trackLayer(layer));
    }

    // Track layers dynamically added to map
    this.map.on('layeradd', (e) => trackLayer(e.layer));

    // Clean up when layer removed
    this.map.on('layerremove', (e) => {
      if (this._trackedLayers.has(e.layer)) {
        this._trackedLayers.delete(e.layer);
      }
      if (this._trackedLayers.size === 0) {
        this.pendingTiles = 0;
        this.renderLoadStatus();
      }
    });

    // When body changes, reset failed tiles and show body transition
    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      this.failedTiles = 0;
      const bName = e?.detail?.body || this.currentBody;
      this.renderLoadStatus(`Loading ${bName}...`);
    });

    // Reset error count if user clicks status pill
    if (this.loadPillEl) {
      this.loadPillEl.addEventListener('click', () => {
        if (this.failedTiles > 0) {
          this.failedTiles = 0;
          this.renderLoadStatus();
        }
      });
    }

    // Initial render
    this.renderLoadStatus();
  }

  /**
   * Render load status indicator (Loading / Ready / Warning) with smooth debouncing.
   * @param {string|null} [customMessage=null] - Optional temporary status text
   */
  renderLoadStatus(customMessage = null) {
    clearTimeout(this._statusDebounce);
    this._statusDebounce = setTimeout(() => {
      if (!this.loadPillEl || !this.loadIconEl || !this.loadTextEl) return;

      if (this.pendingTiles > 0 || customMessage) {
        this.loadPillEl.className = 'status-item status-load-pill loading';
        this.loadPillEl.title = `Loading planetary map tiles (${this.pendingTiles} in flight)...`;
        this.loadIconEl.innerHTML = `
          <svg class="status-spin" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <line x1="12" y1="2" x2="12" y2="6"></line>
            <line x1="12" y1="18" x2="12" y2="22"></line>
            <line x1="4.93" y1="4.93" x2="7.76" y2="7.76"></line>
            <line x1="16.24" y1="16.24" x2="19.07" y2="19.07"></line>
            <line x1="2" y1="12" x2="6" y2="12"></line>
            <line x1="18" y1="12" x2="22" y2="12"></line>
            <line x1="4.93" y1="19.07" x2="7.76" y2="16.24"></line>
            <line x1="16.24" y1="7.76" x2="19.07" y2="4.93"></line>
          </svg>
        `;
        this.loadTextEl.textContent = customMessage || (this.pendingTiles > 1 ? `Loading (${this.pendingTiles})` : 'Loading...');
      } else if (this.failedTiles > 0) {
        this.loadPillEl.className = 'status-item status-load-pill error';
        this.loadPillEl.title = `${this.failedTiles} tile(s) failed or timed out. Click to dismiss.`;
        this.loadIconEl.innerHTML = `
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
            <path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path>
            <line x1="12" y1="9" x2="12" y2="13"></line>
            <line x1="12" y1="17" x2="12.01" y2="17"></line>
          </svg>
        `;
        this.loadTextEl.textContent = 'Tile warning';
      } else {
        this.loadPillEl.className = 'status-item status-load-pill ready';
        this.loadPillEl.title = 'All planetary map layers loaded and ready.';
        this.loadIconEl.innerHTML = `
          <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="20 6 9 17 4 12"></polyline>
          </svg>
        `;
        this.loadTextEl.textContent = 'Loaded';
      }
    }, 40);
  }
}
