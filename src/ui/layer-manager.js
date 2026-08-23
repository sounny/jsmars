/**
 * @module layer-manager
 * @description UI component for managing map layers.
 *
 * Displays active layers (with opacity, visibility, reorder controls) and
 * available layers (with search filter). Syncs bidirectionally between
 * jmarsState and the JMARSMap instance.
 */
import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';

/**
 * @class LayerManager
 * @description Interactive layer management panel for the sidebar.
 */
export class LayerManager {
  /**
   * @param {string|HTMLElement} containerOrId - Container element or its DOM ID.
   * @param {JMARSMap} jmarsMap - The map instance for layer operations.
   */
  constructor(containerOrId, jmarsMap) {
    this.jmarsMap = jmarsMap;
    if (typeof containerOrId === 'string') {
      this.container = document.getElementById(containerOrId);
    } else {
      this.container = containerOrId;
    }
    this.availableLayers = [];
    this.availableFilter = '';
    this.sectionsState = { active: true, available: true };
    this.settingsModal = null;
    this.settingsContent = null;
    this.settingsTitle = null;
    this.lastFocusedElement = null;

    if (!this.container) {
      console.error(`LayerManager container not found.`);
      return;
    }

    this.initSettingsModal();
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

  initSettingsModal() {
    const backdrop = document.createElement('div');
    backdrop.className = 'layer-settings-backdrop';
    backdrop.setAttribute('role', 'dialog');
    backdrop.setAttribute('aria-modal', 'true');
    backdrop.setAttribute('aria-hidden', 'true');
    backdrop.setAttribute('aria-labelledby', 'layer-settings-title');

    const modal = document.createElement('div');
    modal.className = 'layer-settings-modal';
    modal.setAttribute('role', 'document');

    const header = document.createElement('div');
    header.className = 'layer-settings-header';

    const title = document.createElement('h3');
    title.textContent = 'Layer Settings';
    title.id = 'layer-settings-title';

    const closeButton = document.createElement('button');
    closeButton.type = 'button';
    closeButton.className = 'layer-settings-close';
    closeButton.textContent = 'Close';
    closeButton.setAttribute('aria-label', 'Close layer settings');

    header.appendChild(title);
    header.appendChild(closeButton);

    const content = document.createElement('div');
    content.className = 'layer-settings-content';

    modal.appendChild(header);
    modal.appendChild(content);
    backdrop.appendChild(modal);

    closeButton.addEventListener('click', () => this.closeLayerSettings());
    backdrop.addEventListener('click', (event) => {
      if (event.target === backdrop) {
        this.closeLayerSettings();
      }
    });

    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape' && backdrop.style.display === 'flex') {
        this.closeLayerSettings();
      }
    });

    document.body.appendChild(backdrop);

    this.settingsModal = backdrop;
    this.settingsContent = content;
    this.settingsTitle = title;
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
      const leafletLayer = this.jmarsMap.activeLayers[id];
      if (leafletLayer?.getContainer) {
        const el = leafletLayer.getContainer();
        if (el) el.style.mixBlendMode = lState.blendMode || 'normal';
      }
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
    // Save filter input focus state before clearing DOM
    const hadFilterFocus = document.activeElement &&
      document.activeElement.classList.contains('layer-filter-input');
    const cursorPos = hadFilterFocus ? document.activeElement.selectionStart : 0;

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

    const filterWrap = document.createElement('div');
    filterWrap.className = 'layer-filter-wrap';

    const filterInput = document.createElement('input');
    filterInput.type = 'search';
    filterInput.className = 'layer-filter-input';
    filterInput.placeholder = 'Filter available layers…';
    filterInput.value = this.availableFilter;
    filterInput.setAttribute('aria-label', 'Filter available layers');
    filterInput.addEventListener('input', (event) => {
      this.availableFilter = event.target.value;
      this.render();
    });

    filterWrap.appendChild(filterInput);
    availableContent.appendChild(filterWrap);

    const activeIds = activeLayers.map(l => l.id);
    const available = this.availableLayers.filter(l => !activeIds.includes(l.id));
    const normalizedFilter = this.availableFilter.trim().toLowerCase();
    const filteredAvailable = normalizedFilter
      ? available.filter((layer) => {
          const haystack = `${layer.name || ''} ${layer.id || ''}`.toLowerCase();
          return haystack.includes(normalizedFilter);
        })
      : available;

    const availableCount = document.createElement('div');
    availableCount.className = 'layer-filter-count';
    availableCount.textContent = `${filteredAvailable.length} of ${available.length} layers`;
    availableContent.appendChild(availableCount);

    if (available.length === 0) {
      const msg = document.createElement('div');
      msg.textContent = 'No more layers available';
      msg.style.padding = '10px';
      msg.style.color = '#888';
      availableContent.appendChild(msg);
    } else if (filteredAvailable.length === 0) {
      const msg = document.createElement('div');
      msg.textContent = 'No layers match this filter';
      msg.style.padding = '10px';
      msg.style.color = '#888';
      availableContent.appendChild(msg);
    }

    filteredAvailable.forEach(layer => {
      const el = this.createAvailableLayerItem(layer);
      availableContent.appendChild(el);
    });
    availableSection.appendChild(availableContent);
    this.container.appendChild(availableSection);

    // Restore focus to the filter input if it had focus before render
    if (hadFilterFocus) {
      const newInput = this.container.querySelector('.layer-filter-input');
      if (newInput) {
        newInput.focus();
        // Restore cursor position so the user doesn't lose their place
        newInput.setSelectionRange(cursorPos, cursorPos);
      }
    }
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
    div.setAttribute('tabindex', '0');
    div.style.padding = '8px';
    div.style.background = '#222';
    div.style.marginBottom = '5px';
    div.style.borderRadius = '4px';
    div.title = 'Double-click to view layer settings';

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

    const btnSettings = document.createElement('button');
    btnSettings.innerHTML = '&#9881;';
    btnSettings.title = 'Layer Settings';
    btnSettings.setAttribute('aria-label', `Open settings for ${name}`);
    btnSettings.style.marginRight = '5px';
    btnSettings.onclick = () => this.openLayerSettings(layerState.id);

    actions.appendChild(btnSettings);
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

    div.addEventListener('dblclick', (event) => {
      if (event.target.closest('button')) return;
      this.openLayerSettings(layerState.id);
    });

    div.addEventListener('keydown', (event) => {
      const isActionKey = event.key === 'Enter' || event.key === ' ';
      const targetIsInteractive = event.target.closest('button, input, select, textarea, a');
      if (!isActionKey || targetIsInteractive) return;

      event.preventDefault();
      this.openLayerSettings(layerState.id);
    });
    
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

  openLayerSettings(layerId) {
    const layerState = jmarsState.get('activeLayers').find(layer => layer.id === layerId);
    if (!layerState || !this.settingsModal || !this.settingsContent) return;

    const config = this.availableLayers.find(layer => layer.id === layerId);
    const layerName = config?.name || layerId;
    this.lastFocusedElement = document.activeElement;

    this.settingsTitle.textContent = `${layerName} Settings`;
    this.settingsContent.innerHTML = '';

    const infoList = document.createElement('dl');
    infoList.className = 'layer-settings-list';

    const addRow = (label, value) => {
      const term = document.createElement('dt');
      term.textContent = label;
      const desc = document.createElement('dd');
      desc.textContent = value || '—';
      infoList.appendChild(term);
      infoList.appendChild(desc);
    };

    addRow('Layer ID', layerId);
    addRow('Type', config?.type || 'Unknown');
    addRow('Source URL', config?.url);
    addRow('Attribution', config?.options?.attribution);
    addRow('WMS Layers', config?.options?.layers);
    addRow('Max Zoom', config?.options?.maxZoom?.toString());

    const opacitySection = document.createElement('div');
    opacitySection.className = 'layer-settings-opacity';

    const opacityLabel = document.createElement('label');
    opacityLabel.textContent = 'Opacity';
    opacityLabel.setAttribute('for', 'layer-settings-opacity');

    const opacityValue = document.createElement('span');
    opacityValue.className = 'layer-settings-opacity-value';
    opacityValue.textContent = `${Math.round(layerState.opacity * 100)}%`;

    const opacityInput = document.createElement('input');
    opacityInput.type = 'range';
    opacityInput.min = 0;
    opacityInput.max = 1;
    opacityInput.step = 0.01;
    opacityInput.value = layerState.opacity;
    opacityInput.id = 'layer-settings-opacity';
    opacityInput.addEventListener('input', (event) => {
      const value = parseFloat(event.target.value);
      opacityValue.textContent = `${Math.round(value * 100)}%`;
      jmarsState.updateLayer(layerId, { opacity: value });
    });

    opacitySection.appendChild(opacityLabel);
    opacitySection.appendChild(opacityValue);
    opacitySection.appendChild(opacityInput);

    const blendSection = document.createElement('div');
    blendSection.className = 'layer-settings-blend';
    blendSection.style.marginTop = '10px';
    blendSection.style.marginBottom = '12px';

    const blendLabel = document.createElement('label');
    blendLabel.textContent = 'Composite Blend Mode';
    blendLabel.setAttribute('for', 'layer-settings-blend');
    blendLabel.style.display = 'block';
    blendLabel.style.fontSize = '12px';
    blendLabel.style.color = '#aaa';
    blendLabel.style.marginBottom = '4px';

    const blendSelect = document.createElement('select');
    blendSelect.id = 'layer-settings-blend';
    blendSelect.style.width = '100%';
    blendSelect.style.padding = '4px';
    blendSelect.style.background = '#222';
    blendSelect.style.color = '#fff';
    blendSelect.style.border = '1px solid #444';
    blendSelect.style.borderRadius = '4px';

    const blendModes = [
      { id: 'normal', name: 'Normal (Standard)' },
      { id: 'multiply', name: 'Multiply (Topographic shading)' },
      { id: 'screen', name: 'Screen (Bright highlights)' },
      { id: 'overlay', name: 'Overlay (Contrast composite)' },
      { id: 'darken', name: 'Darken' },
      { id: 'lighten', name: 'Lighten' },
      { id: 'color-dodge', name: 'Color Dodge' },
      { id: 'difference', name: 'Difference (Feature detection)' },
      { id: 'luminosity', name: 'Luminosity' }
    ];

    const currentBlend = layerState.blendMode || 'normal';
    blendModes.forEach(m => {
      const opt = document.createElement('option');
      opt.value = m.id;
      opt.textContent = m.name;
      if (m.id === currentBlend) opt.selected = true;
      blendSelect.appendChild(opt);
    });

    blendSelect.addEventListener('change', (e) => {
      const mode = e.target.value;
      jmarsState.updateLayer(layerId, { blendMode: mode });
      const leafletLayer = this.jmarsMap.activeLayers[layerId];
      if (leafletLayer?.getContainer) {
        const el = leafletLayer.getContainer();
        if (el) el.style.mixBlendMode = mode;
      }
    });

    blendSection.appendChild(blendLabel);
    blendSection.appendChild(blendSelect);

    const actions = document.createElement('div');
    actions.className = 'layer-settings-actions';
    actions.style.display = 'flex';
    actions.style.flexWrap = 'wrap';
    actions.style.gap = '5px';

    const infoBtn = document.createElement('button');
    infoBtn.type = 'button';
    infoBtn.textContent = 'Layer Info';
    infoBtn.className = 'layer-settings-btn';
    infoBtn.style.flex = '1';
    infoBtn.addEventListener('click', () => {
      this.closeLayerSettings();
      if (window.jsmarsInfoPanel) {
        // Fetch Capabilities (dummy capabilities for now, we'll just pass config)
        window.jsmarsInfoPanel.open(config);
      }
    });

    const stretchBtn = document.createElement('button');
    stretchBtn.type = 'button';
    stretchBtn.textContent = 'Color Stretch';
    stretchBtn.className = 'layer-settings-btn';
    stretchBtn.style.flex = '1';
    // Only show color stretch for WMS layers basically (local too works with css filter)
    stretchBtn.addEventListener('click', () => {
      this.closeLayerSettings();
      if (window.jsmarsColorStretch) {
        const leafletLayer = this.jmarsMap.activeLayers[layerId];
        window.jsmarsColorStretch.open(leafletLayer, layerName);
      }
    });

    const removeButton = document.createElement('button');
    removeButton.type = 'button';
    removeButton.textContent = 'Remove Layer';
    removeButton.className = 'layer-settings-remove';
    removeButton.style.flex = '1';
    removeButton.addEventListener('click', () => {
      jmarsState.removeLayer(layerId);
      this.closeLayerSettings();
    });

    actions.appendChild(infoBtn);
    actions.appendChild(stretchBtn);
    actions.appendChild(removeButton);

    this.settingsContent.appendChild(infoList);
    this.settingsContent.appendChild(opacitySection);
    this.settingsContent.appendChild(actions);

    this.settingsModal.style.display = 'flex';
    this.settingsModal.setAttribute('aria-hidden', 'false');

    const closeButton = this.settingsModal.querySelector('.layer-settings-close');
    closeButton?.focus();
  }

  closeLayerSettings() {
    if (!this.settingsModal) return;
    this.settingsModal.style.display = 'none';
    this.settingsModal.setAttribute('aria-hidden', 'true');
    this.lastFocusedElement?.focus?.();
  }
}
