import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';

export class LayerManager {
  constructor(containerOrId, jmarsMap) {
    this.jmarsMap = jmarsMap;
    if (typeof containerOrId === 'string') {
      this.container = document.getElementById(containerOrId);
    } else {
      this.container = containerOrId;
    }
    this.availableLayers = [];
    this.sectionsState = { active: true, available: true };

    if (!this.container) {
      console.error(`LayerManager container not found.`);
      return;
    }

    this.init();
  }

  init() {
    // 1. Listen for Discovery (available layers)
    document.addEventListener(EVENTS.LAYERS_UPDATED, (e) => {
      console.debug('LayerManager received layers update', e.detail);
      this.availableLayers = e.detail;
      this.render();
    });

    // 2. Listen for State Changes
    jmarsState.on(EVENTS.LAYERS_CHANGED, (activeLayers) => {
      console.debug('LayerManager received active layers change:', activeLayers);
      this.updateMapFromState(activeLayers);
      this.render();
    });

    // 3. Bootstrap:
    // Load available layers from map if already there
    this.availableLayers = this.jmarsMap.availableLayers || [];

    // Sync Map -> State (Initial population)
    // If Map has layers but State is empty, populate State.
    const mapActiveIds = Object.keys(this.jmarsMap.activeLayers);
    const stateActiveLayers = jmarsState.get('activeLayers');
    const stateActiveIds = stateActiveLayers.map(l => l.id);

    // If state is populated (e.g. by JMARSMap.init calling switchBody), sync Map to match State
    if (stateActiveLayers.length > 0) {
        this.updateMapFromState(stateActiveLayers);
    }

    // If map has layers but state is empty (fallback)
    mapActiveIds.forEach(id => {
      if (!stateActiveIds.includes(id)) {
        jmarsState.addLayer(id);
      }
    });

    this.render();
  }

  updateMapFromState(activeLayers) {
    // activeLayers: [Bottom, ..., Top]
    const activeIds = activeLayers.map(l => l.id);

    // Add missing
    activeIds.forEach(id => {
      if (!this.jmarsMap.activeLayers[id]) {
        this.jmarsMap.addLayer(id);
      }
      // Update opacity
      const lState = activeLayers.find(x => x.id === id);
      this.jmarsMap.setLayerOpacity(id, lState.opacity);
    });

    // Remove stale
    Object.keys(this.jmarsMap.activeLayers).forEach(id => {
      if (!activeIds.includes(id)) {
        this.jmarsMap.removeLayer(id);
      }
    });

    // Update Order
    // Map expects [Top, ..., Bottom]
    // activeLayers is [Bottom, ..., Top]
    const reversedIds = [...activeIds].reverse();
    this.jmarsMap.updateLayerOrder(reversedIds);
  }

  render() {
    this.container.innerHTML = '';

    // Helper to find config
    const getConfig = (id) => this.availableLayers.find(l => l.id === id);

    // --- Active Layers Section ---
    const activeSection = document.createElement('div');
    activeSection.className = 'layer-section';

    const activeHeader = this.createSectionHeader('Active Layers', 'active');
    activeSection.appendChild(activeHeader);

    const activeContent = document.createElement('div');
    activeContent.className = 'layer-section-content';
    if (!this.sectionsState.active) {
      activeContent.style.display = 'none';
    }

    const activeLayers = [...jmarsState.get('activeLayers')];
    // Render Top to Bottom (Reverse of array)
    activeLayers.reverse().forEach((layerState, index) => {
      const config = getConfig(layerState.id);
      const name = config ? config.name : layerState.id;

      const el = this.createActiveLayerItem(layerState, name, index, activeLayers.length);
      activeContent.appendChild(el);
    });

    if (activeLayers.length === 0) {
      const msg = document.createElement('div');
      msg.textContent = 'No active layers';
      msg.style.padding = '10px';
      msg.style.color = '#888';
      activeContent.appendChild(msg);
    }
    activeSection.appendChild(activeContent);
    this.container.appendChild(activeSection);

    // --- Available Layers Section ---
    const availableSection = document.createElement('div');
    availableSection.className = 'layer-section';
    availableSection.style.marginTop = '10px';
    availableSection.style.borderTop = '1px solid #555';

    const availableHeader = this.createSectionHeader('Available Layers', 'available');
    availableSection.appendChild(availableHeader);

    const availableContent = document.createElement('div');
    availableContent.className = 'layer-section-content';
    if (!this.sectionsState.available) {
      availableContent.style.display = 'none';
    }

    const activeIds = activeLayers.map(l => l.id);
    const available = this.availableLayers.filter(l => !activeIds.includes(l.id));

    if (available.length === 0) {
      const msg = document.createElement('div');
      msg.textContent = 'No more layers available';
      msg.style.padding = '10px';
      msg.style.color = '#888';
      availableContent.appendChild(msg);
    }

    available.forEach(layer => {
      const el = this.createAvailableLayerItem(layer);
      availableContent.appendChild(el);
    });
    availableSection.appendChild(availableContent);
    this.container.appendChild(availableSection);
  }

  createSectionHeader(title, stateKey) {
    const header = document.createElement('div');
    header.className = 'layer-section-header';
    header.style.background = '#333';
    header.style.padding = '5px';
    header.style.fontSize = '12px';
    header.style.fontWeight = 'bold';
    header.style.cursor = 'pointer';
    header.style.display = 'flex';
    header.style.justifyContent = 'space-between';
    header.style.alignItems = 'center';

    const titleSpan = document.createElement('span');
    titleSpan.textContent = title;

    const icon = document.createElement('span');
    icon.textContent = this.sectionsState[stateKey] ? '-' : '+';

    header.appendChild(titleSpan);
    header.appendChild(icon);

    header.onclick = () => {
      this.sectionsState[stateKey] = !this.sectionsState[stateKey];
      this.render();
    };

    return header;
  }

  createActiveLayerItem(layerState, name, visualIndex, total) {
    const div = document.createElement('div');
    div.className = 'layer-item-container';
    div.style.padding = '8px';
    div.style.background = '#222';
    div.style.marginBottom = '5px';
    div.style.borderRadius = '4px';

    // Header: Name + Actions
    const header = document.createElement('div');
    header.style.display = 'flex';
    header.style.justifyContent = 'space-between';
    header.style.alignItems = 'center';
    header.style.marginBottom = '5px';

    const title = document.createElement('span');
    title.textContent = name;
    title.style.fontWeight = 'bold';
    title.style.fontSize = '13px';

    const actions = document.createElement('div');

    // Reorder Buttons
    // Visual: Top (index 0). State: [Bottom...Top].
    // Moving "Up" visually means decreasing visualIndex, which means moving towards end of State array.

    const btnUp = document.createElement('button');
    btnUp.innerHTML = '&uarr;';
    btnUp.title = 'Move Up (Front)';
    btnUp.style.marginRight = '5px';
    btnUp.disabled = visualIndex === 0; // Already at top
    btnUp.onclick = () => this.moveLayer(layerState.id, 1); // +1 in state array (towards Top)

    const btnDown = document.createElement('button');
    btnDown.innerHTML = '&darr;';
    btnDown.title = 'Move Down (Back)';
    btnDown.style.marginRight = '5px';
    btnDown.disabled = visualIndex === total - 1; // Already at bottom
    btnDown.onclick = () => this.moveLayer(layerState.id, -1); // -1 in state array (towards Bottom)

    const btnRemove = document.createElement('button');
    btnRemove.innerHTML = '&times;';
    btnRemove.title = 'Remove Layer';
    btnRemove.style.background = '#d6336c';
    btnRemove.style.border = 'none';
    btnRemove.style.color = 'white';
    btnRemove.style.borderRadius = '3px';
    btnRemove.style.cursor = 'pointer';
    btnRemove.onclick = () => jmarsState.removeLayer(layerState.id);

    actions.appendChild(btnUp);
    actions.appendChild(btnDown);
    actions.appendChild(btnRemove);
    header.appendChild(title);
    header.appendChild(actions);
    div.appendChild(header);

    // Opacity Slider
    const sliderContainer = document.createElement('div');
    sliderContainer.style.display = 'flex';
    sliderContainer.style.alignItems = 'center';

    const sliderLabel = document.createElement('span');
    sliderLabel.textContent = 'Opacity: ';
    sliderLabel.style.fontSize = '11px';
    sliderLabel.style.color = '#aaa';
    sliderLabel.style.marginRight = '5px';

    const slider = document.createElement('input');
    slider.type = 'range';
    slider.min = 0;
    slider.max = 1;
    slider.step = 0.01;
    slider.value = layerState.opacity;
    slider.style.flex = 1;
    slider.addEventListener('input', (e) => {
      jmarsState.updateLayer(layerState.id, { opacity: parseFloat(e.target.value) });
    });

    sliderContainer.appendChild(sliderLabel);
    sliderContainer.appendChild(slider);
    div.appendChild(sliderContainer);

    // --- Drag and Drop Events ---
    div.draggable = true;
    div.dataset.layerId = layerState.id;
    
    div.addEventListener('dragstart', (e) => {
      e.dataTransfer.setData('text/plain', layerState.id);
      e.dataTransfer.effectAllowed = 'move';
      div.style.opacity = '0.4';
      // Store the element being dragged
      this.draggedElement = div;
    });

    div.addEventListener('dragend', (e) => {
      div.style.opacity = '1';
      this.draggedElement = null;
      this.container.querySelectorAll('.layer-item-container').forEach(el => {
        el.style.borderTop = '';
        el.style.borderBottom = '';
      });
    });

    div.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
      return false;
    });

    div.addEventListener('dragenter', (e) => {
      e.preventDefault();
      if (this.draggedElement === div) return;
      div.style.background = '#444';
    });

    div.addEventListener('dragleave', (e) => {
      if (this.draggedElement === div) return;
      div.style.background = '#222'; // Restore default
    });

    div.addEventListener('drop', (e) => {
      e.stopPropagation(); // stops the browser from redirecting.
      e.preventDefault();
      
      div.style.background = '#222'; // Restore default

      const draggedId = e.dataTransfer.getData('text/plain');
      const targetId = layerState.id;

      if (draggedId === targetId) return;

      this.handleReorder(draggedId, targetId);
      return false;
    });

    return div;
  }

  handleReorder(draggedId, targetId) {
    // State order is [Bottom, ..., Top]
    // DOM order is [Top, ..., Bottom]
    
    // We want to think in DOM order (Top to Bottom) because that's what the user sees.
    // Get current IDs in DOM order (which is State reversed)
    const stateLayers = [...jmarsState.get('activeLayers')];
    const domOrderIds = stateLayers.map(l => l.id).reverse();

    const oldIndex = domOrderIds.indexOf(draggedId);
    const newIndex = domOrderIds.indexOf(targetId);

    if (oldIndex < 0 || newIndex < 0) return;

    // Move draggedId to newIndex position in DOM order
    domOrderIds.splice(oldIndex, 1);
    domOrderIds.splice(newIndex, 0, draggedId);

    // Now domOrderIds is [NewTop, ..., NewBottom]
    // State expects [Bottom, ..., Top]
    // So reverse it back
    const newStateOrder = domOrderIds.reverse();

    jmarsState.reorderLayers(newStateOrder);
  }

  createAvailableLayerItem(layer) {
    const div = document.createElement('div');
    div.className = 'layer-item-container';
    div.style.display = 'flex';
    div.style.justifyContent = 'space-between';
    div.style.alignItems = 'center';
    div.style.padding = '5px 8px';

    const span = document.createElement('span');
    span.textContent = layer.name;
    span.style.fontSize = '13px';

    const btnAdd = document.createElement('button');
    btnAdd.textContent = '+ Add';
    btnAdd.style.background = '#339af0';
    btnAdd.style.border = 'none';
    btnAdd.style.color = 'white';
    btnAdd.style.padding = '2px 8px';
    btnAdd.style.borderRadius = '3px';
    btnAdd.style.cursor = 'pointer';
    btnAdd.onclick = () => jmarsState.addLayer(layer.id);

    div.appendChild(span);
    div.appendChild(btnAdd);
    return div;
  }

  moveLayer(layerId, direction) {
    // Direction: 1 (Move towards Top/End of array), -1 (Move towards Bottom/Start of array)
    const activeLayers = [...jmarsState.get('activeLayers')];
    const index = activeLayers.findIndex(l => l.id === layerId);
    if (index === -1) return;

    const newIndex = index + direction;
    if (newIndex < 0 || newIndex >= activeLayers.length) return;

    // Swap
    [activeLayers[index], activeLayers[newIndex]] = [activeLayers[newIndex], activeLayers[index]];

    // Extract IDs for State (State expects reordering via event, or we just direct update)
    // My jmarsState has `reorderLayers`
    const newOrderIds = activeLayers.map(l => l.id);
    jmarsState.reorderLayers(newOrderIds);
  }
}
