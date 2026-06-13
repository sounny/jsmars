import { EVENTS } from '../../constants.js';

/**
 * LandingSitesLayer displays markers for spacecraft landing sites
 * on the current planetary body (Mars, Moon).
 */
export class LandingSitesLayer {
  constructor(map) {
    this.map = map;
    this.markerGroup = L.layerGroup();
    this.isActive = false;
    this.currentBody = 'mars';
    this.sites = [];
    this._onBodyChanged = this._onBodyChanged.bind(this);
    this._loadData();
  }

  /**
   * Load landing site data from JSON.
   */
  async _loadData() {
    try {
      const response = await fetch('./src/data/landing-sites.json');
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      this.sites = await response.json();
      if (this.isActive) this._render();
    } catch (err) {
      console.error('Failed to load landing sites:', err);
    }
  }

  /**
   * Activate the landing sites layer and show markers.
   */
  activate() {
    if (this.isActive) return;
    this.isActive = true;
    this.markerGroup.addTo(this.map);
    document.addEventListener(EVENTS.BODY_CHANGED, this._onBodyChanged);
    this._render();
  }

  /**
   * Deactivate the landing sites layer and remove markers.
   */
  deactivate() {
    if (!this.isActive) return;
    this.isActive = false;
    this.map.removeLayer(this.markerGroup);
    document.removeEventListener(EVENTS.BODY_CHANGED, this._onBodyChanged);
  }

  /**
   * Toggle layer visibility.
   * @returns {boolean} New active state
   */
  toggle() {
    if (this.isActive) {
      this.deactivate();
    } else {
      this.activate();
    }
    return this.isActive;
  }

  /**
   * Handle body change events.
   * @param {CustomEvent} e
   */
  _onBodyChanged(e) {
    const body = e?.detail?.body;
    if (body) {
      this.currentBody = body;
      this._render();
    }
  }

  /**
   * Render markers for the current body.
   */
  _render() {
    this.markerGroup.clearLayers();

    const filtered = this.sites.filter(s => s.body === this.currentBody);
    if (filtered.length === 0) return;

    // Agency colors
    const agencyColors = {
      'NASA': '#4dabf7',
      'ESA': '#ffd43b',
      'ESA/UK': '#ffd43b',
      'CNSA': '#ff6b6b',
      'ISRO': '#ff922b',
      'JAXA': '#a9e34b',
      'Roscosmos': '#da77f2'
    };

    filtered.forEach(site => {
      const color = agencyColors[site.agency] || '#aaa';

      const icon = L.divIcon({
        className: 'landing-site-marker',
        html: `<div class="landing-marker-dot" style="background:${color}; box-shadow: 0 0 6px ${color}80"></div>
               <div class="landing-marker-label">${site.name}</div>`,
        iconSize: [120, 30],
        iconAnchor: [8, 8]
      });

      const marker = L.marker([site.lat, site.lon], { icon });
      marker.bindPopup(`
        <div class="landing-popup">
          <h3 style="margin:0 0 6px; color:${color}">${site.name}</h3>
          <div style="font-size:12px; color:#ccc; margin-bottom:6px">
            <strong>${site.agency}</strong> | ${site.year}
          </div>
          <p style="margin:0; font-size:12px; color:#bbb; line-height:1.4">${site.description}</p>
          <div style="margin-top:8px; font-size:11px; color:#888">
            ${site.lat.toFixed(3)}\u00b0, ${site.lon.toFixed(3)}\u00b0
          </div>
        </div>
      `, {
        className: 'landing-popup-container',
        maxWidth: 280
      });

      this.markerGroup.addLayer(marker);
    });
  }
}
