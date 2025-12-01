import { JMARS_CONFIG } from '../jmars-config.js';
import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';

/**
 * Simple Mosaic Catalog browser that lists configured mosaics for the active body.
 * Selecting a mosaic adds it to available layers and activates it via jmarsState.
 */
export class MosaicCatalog {
  constructor(jmarsMap, containerId) {
    this.jmarsMap = jmarsMap;
    this.container = typeof containerId === 'string' ? document.getElementById(containerId) : containerId;
    if (!this.container) return;

    this.render();

    document.addEventListener(EVENTS.BODY_CHANGED, () => this.render());
  }

  getBodyKey() {
    const body = jmarsState.get('body') || JMARS_CONFIG.body || 'mars';
    return body.toLowerCase();
  }

  getMosaics() {
    const key = this.getBodyKey();
    return (JMARS_CONFIG.mosaics && JMARS_CONFIG.mosaics[key]) ? JMARS_CONFIG.mosaics[key] : [];
  }

  render() {
    const mosaics = this.getMosaics();
    this.container.innerHTML = '';

    const header = document.createElement('div');
    header.className = 'mosaic-header';
    header.textContent = 'Mosaic Catalog';
    this.container.appendChild(header);

    if (mosaics.length === 0) {
      const empty = document.createElement('div');
      empty.className = 'mosaic-empty';
      empty.textContent = 'No mosaics available for this body.';
      this.container.appendChild(empty);
      return;
    }

    const list = document.createElement('div');
    list.className = 'mosaic-list';
    mosaics.forEach(m => list.appendChild(this.createCard(m)));
    this.container.appendChild(list);
  }

  createCard(mosaic) {
    const card = document.createElement('div');
    card.className = 'mosaic-card';

    const img = document.createElement('div');
    img.className = 'mosaic-thumb';
    img.style.backgroundImage = `url('${mosaic.thumbnail || ''}')`;
    card.appendChild(img);

    const info = document.createElement('div');
    info.className = 'mosaic-info';

    const title = document.createElement('div');
    title.className = 'mosaic-title';
    title.textContent = mosaic.name;
    info.appendChild(title);

    if (mosaic.description) {
      const desc = document.createElement('div');
      desc.className = 'mosaic-desc';
      desc.textContent = mosaic.description;
      info.appendChild(desc);
    }

    const action = document.createElement('button');
    action.className = 'tool-btn mosaic-activate-btn';
    action.textContent = 'Activate';
    action.addEventListener('click', () => this.activateMosaic(mosaic));
    info.appendChild(action);

    card.appendChild(info);
    return card;
  }

  activateMosaic(mosaic) {
    // Add to availableLayers if needed
    if (!this.jmarsMap.availableLayers.find(l => l.id === mosaic.id)) {
      this.jmarsMap.availableLayers.push({ ...mosaic });
      document.dispatchEvent(new CustomEvent(EVENTS.LAYERS_UPDATED, { detail: this.jmarsMap.availableLayers }));
    }

    // Activate via state (LayerManager/map will handle creation)
    jmarsState.addLayer(mosaic.id);
  }
}
