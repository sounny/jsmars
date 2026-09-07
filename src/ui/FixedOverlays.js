import { GraticuleLayer } from '../layers/GraticuleLayer.js';
import { Panner } from './Panner.js';
import { ResetViewControl } from './ResetViewControl.js';
import { jmarsState } from '../jmars-state.js';

export class FixedOverlays {
  constructor(jmarsMap, containerId) {
    this.jmarsMap = jmarsMap;
    this.container = document.getElementById(containerId);

    if (!this.container) {
      console.error(`FixedOverlays container '${containerId}' not found.`);
      return;
    }

    // Instantiate features
    this.graticule = new GraticuleLayer();
    this.panner = new Panner(jmarsMap);
    this.resetView = new ResetViewControl(jmarsMap.map);

    this.init();
  }

  init() {
    this.resetView.add();
    this.render();

    // Listen to state
    jmarsState.on('overlays-changed', (overlays) => {
      this.applyState(overlays);
      this.updateUI(overlays);
    });

    // Initial Apply
    this.applyState(jmarsState.get('overlays'));
    this.updateUI(jmarsState.get('overlays'));
  }

  render() {
    this.container.innerHTML = '';

    const card = document.createElement('div');
    card.className = 'titanium-card';

    const header = document.createElement('div');
    header.className = 'titanium-card-header';
    header.innerHTML = `
      <span class="titanium-card-title">
        <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
          <polygon points="12 2 2 7 12 12 22 7 12 2"></polygon>
          <polyline points="2 17 12 22 22 17"></polyline>
          <polyline points="2 12 12 17 22 12"></polyline>
        </svg>
        Display Overlays
      </span>
    `;

    card.appendChild(header);

    // Graticule
    this.checkGraticule = this.createToggle('Lat/Lon Grid', 'Coordinate graticule overlay', 'graticule');
    card.appendChild(this.checkGraticule.container);

    // Panner
    this.checkPanner = this.createToggle('Panner View', 'Minimap locator overview', 'panner');
    card.appendChild(this.checkPanner.container);

    this.container.appendChild(card);
  }

  createToggle(label, subLabel, id) {
    const row = document.createElement('div');
    row.className = 'switch-toggle-row';

    const labelGroup = document.createElement('div');
    labelGroup.className = 'switch-label-group';

    const mainLbl = document.createElement('label');
    mainLbl.className = 'switch-main-label';
    mainLbl.htmlFor = `toggle-${id}`;
    mainLbl.textContent = label;
    mainLbl.style.cursor = 'pointer';

    const subLbl = document.createElement('span');
    subLbl.className = 'switch-sub-label';
    subLbl.textContent = subLabel || '';

    labelGroup.appendChild(mainLbl);
    if (subLabel) labelGroup.appendChild(subLbl);

    const switchLabel = document.createElement('label');
    switchLabel.className = 'toggle-switch';

    const input = document.createElement('input');
    input.type = 'checkbox';
    input.id = `toggle-${id}`;
    input.onchange = (e) => {
      jmarsState.toggleOverlay(id, e.target.checked);
    };

    const slider = document.createElement('span');
    slider.className = 'toggle-slider';

    switchLabel.appendChild(input);
    switchLabel.appendChild(slider);

    row.appendChild(labelGroup);
    row.appendChild(switchLabel);

    return { container: row, input };
  }

  updateUI(overlays) {
    if (this.checkGraticule) this.checkGraticule.input.checked = !!overlays.graticule;
    if (this.checkPanner) this.checkPanner.input.checked = !!overlays.panner;
  }

  applyState(overlays) {
    // Graticule
    if (overlays.graticule) {
      if (!this.jmarsMap.map.hasLayer(this.graticule)) {
        this.graticule.addTo(this.jmarsMap.map);
      }
    } else {
      if (this.jmarsMap.map.hasLayer(this.graticule)) {
        this.jmarsMap.map.removeLayer(this.graticule);
      }
    }

    // Panner
    this.panner.toggle(!!overlays.panner);


  }
}
