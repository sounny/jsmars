export class LayerManager {
  constructor(jmarsMap, containerId) {
    this.jmarsMap = jmarsMap;
    this.container = document.getElementById(containerId);
    if (!this.container) {
        console.error(`LayerManager container '${containerId}' not found.`);
        return;
    }
    this.init();
  }

  init() {
    // Initial render
    this.render(this.jmarsMap.availableLayers);

    // Listen for updates
    document.addEventListener('jmars-layers-updated', (e) => {
      this.render(e.detail);
    });
  }

  render(layers) {
    this.container.innerHTML = '';
    if (!layers || layers.length === 0) {
        this.container.innerHTML = '<div class="error-msg">No layers found.</div>';
        return;
    }

    layers.forEach(layer => {
      const item = this.createLayerItem(layer);
      this.container.appendChild(item);
    });
  }

  createLayerItem(layer) {
    const div = document.createElement('div');
    div.className = 'layer-item-container';

    // Row 1: Checkbox and Name
    const labelRow = document.createElement('label');
    labelRow.className = 'layer-item';
    labelRow.title = layer.abstract || layer.name;

    const checkbox = document.createElement('input');
    checkbox.type = 'checkbox';
    checkbox.checked = !!this.jmarsMap.activeLayers[layer.id];

    // Checkbox event
    checkbox.addEventListener('change', (e) => {
      if (e.target.checked) {
        this.jmarsMap.addLayer(layer.id);
        this.updateSliderState(div, true);
      } else {
        this.jmarsMap.removeLayer(layer.id);
        this.updateSliderState(div, false);
      }
    });

    const textSpan = document.createElement('span');
    textSpan.textContent = layer.name;

    labelRow.appendChild(checkbox);
    labelRow.appendChild(textSpan);
    div.appendChild(labelRow);

    // Row 2: Opacity Slider
    const sliderContainer = document.createElement('div');
    sliderContainer.className = 'layer-slider';

    const slider = document.createElement('input');
    slider.type = 'range';
    slider.min = 0;
    slider.max = 1;
    slider.step = 0.01;
    slider.value = 1; // Default

    // Sync with existing layer state if present
    const activeLayer = this.jmarsMap.activeLayers[layer.id];
    if (activeLayer && activeLayer.options.opacity !== undefined) {
        slider.value = activeLayer.options.opacity;
    }

    // Initial state
    slider.disabled = !checkbox.checked;

    slider.addEventListener('input', (e) => {
        this.jmarsMap.setLayerOpacity(layer.id, parseFloat(e.target.value));
    });

    sliderContainer.appendChild(slider);
    div.appendChild(sliderContainer);

    return div;
  }

  updateSliderState(container, active) {
     const slider = container.querySelector('input[type="range"]');
     if (slider) slider.disabled = !active;
  }
}
