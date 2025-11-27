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
    // If container is empty, we add structure.
    // If we are appending to a specific container meant for this, good.

    const header = document.createElement('div');
    header.className = 'control-header';
    header.style.borderTop = '1px solid #444'; // visual separator
    header.innerHTML = '<span>Fixed Overlays</span>';

    const content = document.createElement('div');
    content.style.padding = '10px';

    // Graticule
    this.checkGraticule = this.createToggle('Lat/Lon Grid', 'graticule');
    content.appendChild(this.checkGraticule.container);

    // Panner
    this.checkPanner = this.createToggle('Panner View', 'panner');
    content.appendChild(this.checkPanner.container);



    this.container.appendChild(header);
    this.container.appendChild(content);
  }

  createToggle(label, id) {
    const div = document.createElement('div');
    div.style.marginBottom = '5px';
    div.style.display = 'flex';
    div.style.alignItems = 'center';

    const input = document.createElement('input');
    input.type = 'checkbox';
    input.id = `toggle-${id}`;
    input.style.marginRight = '8px';
    input.style.cursor = 'pointer';
    input.onchange = (e) => {
      jmarsState.toggleOverlay(id, e.target.checked);
    };

    const lbl = document.createElement('label');
    lbl.htmlFor = `toggle-${id}`;
    lbl.textContent = label;
    lbl.style.cursor = 'pointer';
    lbl.style.fontSize = '14px';

    div.appendChild(input);
    div.appendChild(lbl);

    return { container: div, input };
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
