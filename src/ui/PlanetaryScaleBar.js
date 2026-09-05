import { BODIES } from '../util/geo.js';
import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';

/**
 * @class PlanetaryScaleBar
 * @description Accurate, planet-aware cartographic graphic scale bar control for Leaflet.
 * Uses exact planetary radii (Mars, Moon, Earth) and latitude cosine distortion
 * rather than hardcoded terrestrial WGS84 metrics.
 */
export class PlanetaryScaleBar {
  /**
   * @param {L.Map} map - Leaflet map instance
   * @param {object} [options]
   * @param {string} [options.position='bottomleft'] - Position on map
   * @param {number} [options.maxWidth=150] - Maximum width of scale bar in pixels
   */
  constructor(map, options = {}) {
    this.map = map;
    this.options = Object.assign({
      position: 'bottomleft',
      maxWidth: 160
    }, options);

    this.control = null;
    this.container = null;
    this.barEl = null;
    this.labelEl = null;
    this.currentBody = (jmarsState.get('body') || 'mars').toLowerCase();

    this._onMove = this._onMove.bind(this);
    this._onBodyChanged = this._onBodyChanged.bind(this);
  }

  /**
   * Add scale bar control to map.
   */
  add() {
    if (!this.map || this.control) return;

    const self = this;
    const ControlClass = L.Control.extend({
      onAdd: function() {
        const div = L.DomUtil.create('div', 'planetary-scale-control');
        div.style.background = 'rgba(15, 23, 42, 0.85)';
        div.style.border = '1px solid #334155';
        div.style.borderRadius = '4px';
        div.style.padding = '4px 8px';
        div.style.boxShadow = '0 2px 6px rgba(0,0,0,0.5)';
        div.style.fontFamily = 'monospace, sans-serif';
        div.style.pointerEvents = 'none';

        const label = document.createElement('div');
        label.style.fontSize = '10px';
        label.style.fontWeight = 'bold';
        label.style.color = '#38bdf8';
        label.style.textAlign = 'center';
        label.style.marginBottom = '2px';

        const bar = document.createElement('div');
        bar.style.height = '4px';
        bar.style.background = 'linear-gradient(to right, #38bdf8 0%, #38bdf8 50%, #f8fafc 50%, #f8fafc 100%)';
        bar.style.border = '1px solid #0f172a';
        bar.style.borderRadius = '2px';

        div.appendChild(label);
        div.appendChild(bar);

        self.container = div;
        self.labelEl = label;
        self.barEl = bar;

        return div;
      }
    });

    this.control = new ControlClass({ position: this.options.position });
    this.control.addTo(this.map);

    this.currentBody = (jmarsState.get('body') || 'mars').toLowerCase();
    this.map.on('moveend', this._onMove);
    this.map.on('zoomend', this._onMove);
    document.addEventListener(EVENTS.BODY_CHANGED, this._onBodyChanged);

    this.update();
  }

  /**
   * Remove scale bar control from map.
   */
  remove() {
    if (this.control) {
      this.map.removeControl(this.control);
      this.control = null;
      this.map.off('moveend', this._onMove);
      this.map.off('zoomend', this._onMove);
      document.removeEventListener(EVENTS.BODY_CHANGED, this._onBodyChanged);
    }
  }

  _onMove() {
    this.update();
  }

  _onBodyChanged(e) {
    const body = e?.detail?.body?.toLowerCase();
    if (body) {
      this.currentBody = body;
      this.update();
    }
  }

  /**
   * Compute exact physical ground distance per pixel on the current planetary body.
   * @param {number} centerLat - Latitude in degrees
   * @param {number} zoom - Current Leaflet zoom level
   * @param {string} bodyName - Planetary body key
   * @returns {number} - Meters per pixel
   */
  static getMetersPerPixel(centerLat, zoom, bodyName = 'mars') {
    const radiusMeters = ((BODIES[bodyName] || BODIES.mars).meanRadius) * 1000;
    const cosLat = Math.max(Math.cos(centerLat * Math.PI / 180), 0.001);
    const circumference = 2 * Math.PI * radiusMeters * cosLat;
    return circumference / (256 * Math.pow(2, zoom));
  }

  /**
   * Pick a human-friendly rounded round distance.
   * @param {number} rawMeters
   * @returns {{meters: number, text: string}}
   */
  static getFriendlyDistance(rawMeters) {
    const steps = [
      0.1, 0.2, 0.5, 1, 2, 5, 10, 20, 50, 100, 200, 500,
      1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000,
      1000000, 2000000, 5000000
    ];

    let chosen = steps[0];
    for (let i = 0; i < steps.length; i++) {
      if (steps[i] <= rawMeters) {
        chosen = steps[i];
      } else {
        break;
      }
    }

    if (chosen >= 1000) {
      return { meters: chosen, text: `${chosen / 1000} km` };
    }
    return { meters: chosen, text: `${chosen} m` };
  }

  /**
   * Update scale bar width and label.
   */
  update() {
    if (!this.map || !this.labelEl || !this.barEl) return;

    const centerLat = this.map.getCenter().lat;
    const zoom = this.map.getZoom();
    const metersPerPx = PlanetaryScaleBar.getMetersPerPixel(centerLat, zoom, this.currentBody);

    const maxMeters = metersPerPx * this.options.maxWidth;
    const friendly = PlanetaryScaleBar.getFriendlyDistance(maxMeters);

    const pixelWidth = Math.max(Math.round(friendly.meters / metersPerPx), 20);

    const bodyDisplay = this.currentBody.charAt(0).toUpperCase() + this.currentBody.slice(1);
    this.labelEl.textContent = `${friendly.text} (${bodyDisplay})`;
    this.barEl.style.width = `${pixelWidth}px`;
  }
}
