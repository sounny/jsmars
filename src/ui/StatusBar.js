import { EVENTS } from '../constants.js';
import { formatLatLon } from '../util/geo.js';

/**
 * StatusBar displays coordinates, zoom level, and scale.
 * Supports multiple coordinate formats matching JMARS desktop:
 * - East 180 (-180 to 180)
 * - East 360 (0 to 360)
 * - DMS (degrees, minutes, seconds)
 */
export class StatusBar {
  constructor(map, containerId) {
    this.map = map;
    this.container = document.getElementById(containerId);
    if (!this.container) return;

    // Coordinate format state
    this.formatIndex = 0;
    this.formats = [
      { label: 'E180', lonFormat: 'east180', notation: 'decimal', precision: 4 },
      { label: 'E360', lonFormat: 'east360', notation: 'decimal', precision: 4 },
      { label: 'DMS', lonFormat: 'east180', notation: 'dms', precision: 4 }
    ];
    this.currentBody = 'mars';

    this.initUI();
    this.bindEvents();
    this.update();
  }

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

  bindEvents() {
    this.map.on('mousemove', (e) => this.updateCoords(e.latlng));
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

  updateCoords(latlng) {
    const fmt = this.formats[this.formatIndex];
    const formatted = formatLatLon(latlng.lat, latlng.lng, {
      ...fmt,
      body: this.currentBody
    });
    // Add prefix labels for decimal formats
    if (fmt.notation === 'decimal') {
      const parts = formatted.split(', ');
      this.coordsEl.innerText = `Lat: ${parts[0]}, Lon: ${parts[1]}`;
    } else {
      this.coordsEl.innerText = formatted;
    }
  }

  updateZoom() {
    this.zoomEl.innerText = `Zoom: ${this.map.getZoom()}`;
  }

  update() {
    this.updateZoom();
  }
}
