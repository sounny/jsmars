/**
 * @module features/groundtrack/GroundTrackPanel
 * @description UI panel for selecting and displaying spacecraft ground tracks.
 * Renders a checkbox list of available spacecraft with color-coded indicators
 * and provides Show / Clear actions. Tracks are computed from approximate
 * Keplerian orbital elements.
 */

/**
 * @class GroundTrackPanel
 * @description Provides UI controls for the spacecraft ground track layer.
 */
export class GroundTrackPanel {
  /**
   * @param {HTMLElement} container
   * @param {import('./GroundTrackLayer.js').GroundTrackLayer} groundTrackLayer
   */
  constructor(container, groundTrackLayer) {
    this.container = container;
    this.layer = groundTrackLayer;
    this._build();
  }

  _build() {
    const scList = this.layer.getSpacecraftList();

    this.container.innerHTML = `
      <div class="groundtrack-panel" style="padding:10px">
        <div style="margin-bottom:8px; font-size:12px; color:#aaa">
          Select spacecraft to display ground tracks.
          Tracks are approximate (Keplerian elements).
        </div>
        <div class="groundtrack-sc-list">
          ${scList.map(sc => `
            <label class="groundtrack-sc-item" style="display:flex; align-items:center; gap:8px; padding:4px 0; cursor:pointer">
              <input type="checkbox" data-sc="${sc.id}" ${sc.active ? 'checked' : ''}>
              <span class="groundtrack-sc-dot" style="width:10px; height:10px; border-radius:50%; background:${sc.color}; flex-shrink:0"></span>
              <span style="font-size:13px">${sc.name}</span>
              <span style="font-size:11px; color:#888; margin-left:auto">${sc.id}</span>
            </label>
          `).join('')}
        </div>
        <div style="margin-top:10px; display:flex; gap:5px">
          <button id="groundtrack-show-btn" class="tool-btn" style="flex:1">Show Selected</button>
          <button id="groundtrack-clear-btn" class="crater-action-btn" style="background:#333; flex:0.5">Clear</button>
        </div>
      </div>
    `;

    // Show button
    this.container.querySelector('#groundtrack-show-btn').addEventListener('click', () => {
      this.layer.activate();
      this.layer.clearAll()