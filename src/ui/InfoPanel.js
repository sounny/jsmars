import { EVENTS } from '../constants.js';

/**
 * InfoPanel displays detailed information about a WMS layer.
 * Shows metadata from GetCapabilities: abstract, attribution,
 * bounding box, styles, dimensions.
 *
 * Can be attached to the layer settings modal or opened standalone.
 */
export class InfoPanel {
  constructor() {
    this.container = null;
    this._createModal();
  }

  /**
   * Create the modal DOM.
   */
  _createModal() {
    this.container = document.createElement('div');
    this.container.id = 'info-panel-modal';
    this.container.className = 'welcome-modal-backdrop';
    this.container.style.display = 'none';
    this.container.innerHTML = `
      <div class="welcome-modal" style="max-width:520px; text-align:left; max-height:80vh; overflow-y:auto">
        <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px">
          <h2 id="info-panel-title" style="margin:0">Layer Info</h2>
          <span id="info-panel-close" style="cursor:pointer; font-size:20px; line-height:1">&times;</span>
        </div>
        <div id="info-panel-content" style="font-size:13px; line-height:1.5; color:#ccc"></div>
      </div>
    `;

    document.body.appendChild(this.container);

    this.container.querySelector('#info-panel-close').addEventListener('click', () => this.close());
    this.container.addEventListener('click', (e) => {
      if (e.target === this.container) this.close();
    });
  }

  /**
   * Open the info panel with layer details.
   * @param {object} layerConfig - Layer configuration object
   * @param {object} [capabilities] - WMS capabilities metadata (optional)
   */
  open(layerConfig, capabilities = null) {
    const title = this.container.querySelector('#info-panel-title');
    const content = this.container.querySelector('#info-panel-content');

    title.textContent = layerConfig.name || layerConfig.id || 'Layer Info';

    let html = '';

    // Basic info
    html += `<div class="info-section">`;
    html += `<div class="info-row"><strong>ID:</strong> <code>${layerConfig.id || '-'}</code></div>`;
    html += `<div class="info-row"><strong>Type:</strong> ${layerConfig.type || '-'}</div>`;
    if (layerConfig.url) {
      html += `<div class="info-row"><strong>URL:</strong> <span style="word-break:break-all; font-size:11px">${layerConfig.url}</span></div>`;
    }
    html += `</div>`;

    // WMS options
    if (layerConfig.options) {
      html += `<div class="info-section">`;
      html += `<h3>WMS Parameters</h3>`;
      const opts = layerConfig.options;
      if (opts.layers) html += `<div class="info-row"><strong>Layers:</strong> ${opts.layers}</div>`;
      if (opts.format) html += `<div class="info-row"><strong>Format:</strong> ${opts.format}</div>`;
      if (opts.attribution) html += `<div class="info-row"><strong>Attribution:</strong> ${opts.attribution}</div>`;
      html += `</div>`;
    }

    // Capabilities metadata (if provided)
    if (capabilities) {
      if (capabilities.abstract) {
        html += `<div class="info-section">`;
        html += `<h3>Description</h3>`;
        html += `<p style="margin:4px 0; color:#bbb">${capabilities.abstract}</p>`;
        html += `</div>`;
      }
      if (capabilities.bbox) {
        html += `<div class="info-section">`;
        html += `<h3>Bounding Box</h3>`;
        html += `<div class="info-row">West: ${capabilities.bbox.west}, East: ${capabilities.bbox.east}</div>`;
        html += `<div class="info-row">South: ${capabilities.bbox.south}, North: ${capabilities.bbox.north}</div>`;
        html += `</div>`;
      }
      if (capabilities.styles && capabilities.styles.length > 0) {
        html += `<div class="info-section">`;
        html += `<h3>Available Styles</h3>`;
        capabilities.styles.forEach(s => {
          html += `<div class="info-row">${s.name || s} ${s.title ? `(${s.title})` : ''}</div>`;
        });
        html += `</div>`;
      }
    }

    // Action buttons
    html += `<div style="margin-top:16px; display:flex; gap:8px">`;
    if (layerConfig.url) {
      const capsUrl = layerConfig.url + (layerConfig.url.includes('?') ? '&' : '?') + 'SERVICE=WMS&REQUEST=GetCapabilities';
      html += `<a href="${capsUrl}" target="_blank" rel="noopener" style="color:#4dabf7; font-size:12px">View Capabilities XML</a>`;
    }
    html += `</div>`;

    content.innerHTML = html;
    this.container.style.display = 'flex';
  }

  /**
   * Close the info panel.
   */
  close() {
    this.container.style.display = 'none';
  }
}
