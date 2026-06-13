/**
 * @module NorthArrow
 * @description North Arrow Leaflet control.
 * Displays a simple gray arrow icon pointing North.
 * Non-interactive; serves as a visual orientation indicator.
 */
export class NorthArrow {
  /**
   * Create a NorthArrow control.
   * @param {L.Map} map - Leaflet map instance
   * @param {object} [options] - Configuration options
   * @param {string} [options.position='topleft'] - Leaflet control position
   */
  constructor(map, options = {}) {
    this.map = map;
    this.options = Object.assign({ position: 'topleft' }, options);
    /** @type {L.Control|null} */
    this.control = null;
  }

  /**
   * Add the North Arrow control to the map.
   * No-op if already added or map is missing.
   */
  add() {
    if (!this.map || this.control) return;

    const control = L.control({ position: this.options.position });
    control.onAdd = () => {
      const container = L.DomUtil.create('div', 'leaflet-bar leaflet-control north-arrow-control');
      const button = L.DomUtil.create('a', 'north-arrow-btn', container);
      button.href = '#';
      button.title = 'North Arrow';
      button.role = 'button';
      button.setAttribute('aria-label', 'North Arrow');
      button.style.cursor = 'default'; // Not clickable

      // Gray Arrow Icon
      button.innerHTML = `
        <svg viewBox="0 0 24 24" width="18" height="18" fill="none" stroke="#555" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="12 2 19 21 12 17 5 21 12 2" fill="#888" stroke="none" />
          <text x="12" y="24" font-size="8" text-anchor="middle" fill="#555" font-weight="bold">N</text>
        </svg>
      `;

      return container;
    };

    control.addTo(this.map);
    this.control = control;
  }

  /**
   * Remove the North Arrow control from the map.
   */
  remove() {
    if (this.control) {
      this.control.remove();
      this.control = null;
    }
  }

  /**
   * Toggle the North Arrow on or off.
   * @param {boolean} isActive - Whether to show the control
   */
  toggle(isActive) {
    if (isActive) this.add();
    else this.remove();
  }
}
