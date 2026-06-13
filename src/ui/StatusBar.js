import { EVENTS } from '../constants.js';
import { formatLatLon } from '../util/geo.js';

/**
 * @module StatusBar
 * @description Displays coordinates, zoom level, and scale in the bottom bar.
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
    this.currentBody = 'mars';

    /** @type {boolean} Tracks pending rAF frame for mousemove throttling */
    this._pendingFrame = false;

    this.initUI();
    this.bindEvents();
    this.update();
  }

  /**
   * Build the status bar DOM elements and attach the Leaflet scale control.
   * @private
   */
  initUI() {
    this.container.innerHTML = `
      <div class="status-item" id="status-coords">Lat: 0.0000, Lon: 0.0000</div>
      <button class="coord-format-btn" id="status-coord-format" title="Click to cycle coordinate format">E180</button>
      <div class="status-item" id="status-zoom">Zoom: 0</div>
      <div class="status-item" id="status-scale"></div>
    `;

    this.coordsEl = this.container.querySelector('#status-coords');
    this.formatBtn = this.container.querySelector('#status-coord-format');
    this.zoomEl = this.container.querySelector('#status-zoom');
    this.scaleEl = this.container.querySelector('#status-scale');

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
   * Mousemove is throttled via requestAnimationFrame to reduce repaints.
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

    this.map.on('zoomend', () => this.updateZoom());

    // Format cycle button
    this.formatBtn.addEventListener('click', () => {
      this.formatIndex = (this.formatIndex + 1) % this.formats.length;
      const fmt = this.formats[this.formatIndex];
      this.formatBtn.textContent = fmt.label;

      document.dispatchEvent(new CustomEvent(EVENTS.COORD_FORMAT_CHANGED, {
        detail: fmt
      }));
    });

    // Listen for body changes
    document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
      this.currentBody = e?.detail?.body || 'mars';
    });
  }

  /**
   * Update the coordinate display for the given latlng.
   * @param {L.LatLng} latlng - Cursor position
   */
  updateCoords(latlng) {
    const fmt = this.formats[this.formatIndex];
    const formatted = formatLatLon(latlng.lat, latlng.lng, {
      ...fmt,
      body: this.currentBody
    });
    // Add prefix labels for decimal formats
    if (fmt.notation === 'decimal') {
      const parts = formatted.split(', ');
      this.coordsEl.textContent = `Lat: ${parts[0]}, Lon: ${parts[1]}`;
    } else {
      this.coordsEl.textContent = formatted;
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
  }
}
