import { EVENTS } from '../constants.js';

/**
 * @module InfoPanel
 * @description Displays detailed metadata about a WMS layer.
 * Shows information from GetCapabilities: abstract, attribution,
 * bounding box, styles, and dimensions.
 *
 * Can be attached to the layer settings modal or opened standalone.
 */
export class InfoPanel {
  /**
   * Create a new InfoPanel instance.
   * Builds the modal DOM and appends it to document.body.
   */
  constructor() {
    /** @type {HTMLDivElement|null} */
    this.container = null;
    this._createModal();
  }

  /**
   * Build the modal backdrop, dialog shell, and close button.
   * All static markup is safe; dynamic content is added in open().
   * @private
   */
  _createModal() {
    this.container = document.createElement('div');
    this.container.id = 'info-panel-modal';
    this.container.className = 'welcome-modal-backdrop';
    this.container.style.display = 'none';

    // Dialog wrapper
    const dialog = document.createElement('div');
    dialog.className = 'welcome-modal';
    dialog.style.cssText = 'max-width:520px; text-align:left; max-height:80vh; overflow-y:auto';
    dialog.setAttribute('role', 'dialog');
    dialog.setAttribute('aria-modal', 'true');
    dialog.setAttribute('aria-labelledby', 'info-panel-title');

    // Header row
    const headerRow = document.createElement('div');
    headerRow.style.cssText = 'display:flex; justify-content:space-between; align-items:center; margin-bottom:12px';

    const title = document.createElement('h2');
    title.id = 'info-panel-title';
    title.style.margin = '0';
    title.textContent = 'Layer Info';

    const closeBtn = document.createElement('button');
    closeBtn.id = 'info-panel-close';
    closeBtn.type = 'button';
    closeBtn.style.cssText = 'cursor:pointer; font-size:20px; line-height:1; background:none; border:none; color:inherit';
    closeBtn.setAttribute('aria-label', 'Close info panel');
    closeBtn.textContent = '\u00D7';

    headerRow.appendChild(title);
    headerRow.appendChild(closeBtn);

    // Content area
    const content = document.createElement('div');
    content.id = 'info-panel-content';
    content.style.cssText = 'font-size:13px; line-height:1.5; color:#ccc';

    dialog.appendChild(headerRow);
    dialog.appendChild(content);
    this.container.appendChild(dialog);
    document.body.appendChild(this.container);

    closeBtn.addEventListener('click', () => this.close());
    this.container.addEventListener('click', (e) => {
      if (e.target === this.container) this.close();
    });
  }

  /**
   * Open the info panel with layer details.
   * All user-supplied values are set via textContent to prevent XSS.
   * @param {object} layerConfig - Layer configuration object
   * @param {string} [layerConfig.id] - Layer identifier
   * @param {string} [layerConfig.name] - Display name
   * @param {string} [layerConfig.type] - Layer type (wms, xyz, etc.)
   * @param {string} [layerConfig.url] - Service URL
   * @param {object} [layerConfig.options] - WMS parameters
   * @param {object} [capabilities] - WMS capabilities metadata (optional)
   */
  open(layerConfig, capabilities = null) {
    const title = this.container.querySelector('#info-panel-title');
    const content = this.container.querySelector('#info-panel-content');

    title.textContent = layerConfig.name || layerConfig.id || 'Layer Info';

    // Clear previous content
    content.innerHTML = '';

    // --- Basic info section ---
    const basicSection = this._createSection();
    basicSection.appendChild(this._createRow('ID', layerConfig.id || '-', true));
    basicSection.appendChild(this._createRow('Type', layerConfig.type || '-'));
    if (layerConfig.url) {
      const urlRow = this._createRow('URL', '');
      const urlSpan = document.createElement('span');
      urlSpan.style.cssText = 'word-break:break-all; font-size:11px';
      urlSpan.textContent = layerConfig.url;
      urlRow.appendChild(urlSpan);
      basicSection.appendChild(urlRow);
    }
    content.appendChild(basicSection);

    // --- WMS parameters section ---
    if (layerConfig.options) {
      const opts = layerConfig.options;
      const wmsSection = this._createSection();
      const wmsTitle = document.createElement('h3');
      wmsTitle.textContent = 'WMS Parameters';
      wmsSection.appendChild(wmsTitle);

      if (opts.layers) wmsSection.appendChild(this._createRow('Layers', opts.layers));
      if (opts.format) wmsSection.appendChild(this._createRow('Format', opts.format));
      if (opts.attribution) wmsSection.appendChild(this._createRow('Attribution', opts.attribution));
      content.appendChild(wmsSection);
    }

    // --- Capabilities metadata (if provided) ---
    if (capabilities) {
      if (capabilities.abstract) {
        const descSection = this._createSection();
        const descTitle = document.createElement('h3');
        descTitle.textContent = 'Description';
        descSection.appendChild(descTitle);

        const p = document.createElement('p');
        p.style.cssText = 'margin:4px 0; color:#bbb';
        p.textContent = capabilities.abstract;
        descSection.appendChild(p);
        content.appendChild(descSection);
      }

      if (capabilities.bbox) {
        const bboxSection = this._createSection();
        const bboxTitle = document.createElement('h3');
        bboxTitle.textContent = 'Bounding Box';
        bboxSection.appendChild(bboxTitle);

        const ewRow = document.createElement('div');
        ewRow.className = 'info-row';
        ewRow.textContent = `West: ${capabilities.bbox.west}, East: ${capabilities.bbox.east}`;
        bboxSection.appendChild(ewRow);

        const nsRow = document.createElement('div');
        nsRow.className = 'info-row';
        nsRow.textContent = `South: ${capabilities.bbox.south}, North: ${capabilities.bbox.north}`;
        bboxSection.appendChild(nsRow);
        content.appendChild(bboxSection);
      }

      if (capabilities.styles && capabilities.styles.length > 0) {
        const stylesSection = this._createSection();
        const stylesTitle = document.createElement('h3');
        stylesTitle.textContent = 'Available Styles';
        stylesSection.appendChild(stylesTitle);

        capabilities.styles.forEach(s => {
          const row = document.createElement('div');
          row.className = 'info-row';
          const name = s.name || String(s);
          row.textContent = s.title ? `${name} (${s.title})` : name;
          stylesSection.appendChild(row);
        });
        content.appendChild(stylesSection);
      }
    }

    // --- Action buttons ---
    const actions = document.createElement('div');
    actions.style.cssText = 'margin-top:16px; display:flex; gap:8px';
    if (layerConfig.url) {
      const sep = layerConfig.url.includes('?') ? '&' : '?';
      const capsUrl = layerConfig.url + sep + 'SERVICE=WMS&REQUEST=GetCapabilities';
      const link = document.createElement('a');
      link.href = capsUrl;
      link.target = '_blank';
      link.rel = 'noopener';
      link.style.cssText = 'color:#4dabf7; font-size:12px';
      link.textContent = 'View Capabilities XML';
      actions.appendChild(link);
    }
    content.appendChild(actions);

    this.container.style.display = 'flex';
  }

  /**
   * Close the info panel.
   */
  close() {
    this.container.style.display = 'none';
  }

  // ── Private helpers ──────────────────────────────────────────

  /**
   * Create a styled section container div.
   * @returns {HTMLDivElement}
   * @private
   */
  _createSection() {
    const div = document.createElement('div');
    div.className = 'info-section';
    return div;
  }

  /**
   * Create an info row with a bold label and text value.
   * @param {string} label - Row label
   * @param {string} value - Row value (set via textContent)
   * @param {boolean} [asCode=false] - Wrap value in a code element
   * @returns {HTMLDivElement}
   * @private
   */
  _createRow(label, value, asCode = false) {
    const row = document.createElement('div');
    row.className = 'info-row';
    const strong = document.createElement('strong');
    strong.textContent = label + ':';
    row.appendChild(strong);
    row.appendChild(document.createTextNode(' '));
    if (asCode) {
      const code = document.createElement('code');
      code.textContent = value;
      row.appendChild(code);
    } else {
      row.appendChild(document.createTextNode(value));
    }
    return row;
  }
}
