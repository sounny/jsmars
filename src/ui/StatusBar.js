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
      <div class="status-item" id="status-scale"></div>
    `;

    this.coordsEl = this.container.querySelector('#status-coords');
    this.latValEl = this.container.querySelector('#status-lat-val');
    this.lonValEl = this.container.querySelector('#status-lon-val');
    this.elevValEl = this.container.querySelector('#status-elev-val');
    this.formatBtn = this.container.querySelector('#status-coord-format');
    this.zoomEl = this.container.querySelector('#status-zoom');
    this.scaleEl = this.container.querySelector('#status-scale');

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
}
