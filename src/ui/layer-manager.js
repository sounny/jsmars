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

    // Add / Update visible layers, detach hidden layers
    activeIds.forEach(id => {
      const lState = activeLayers.find(x => x.id === id);
      const isVisible = lState.visible !== false;

      if (isVisible) {
        if (!this.jmarsMap.activeLayers[id]) {
          this.jmarsMap.addLayer(id);
        }
        this.jmarsMap.setLayerOpacity(id, lState.opacity);
        const leafletLayer = this.jmarsMap.activeLayers[id];
        if (leafletLayer?.getContainer) {
          const el = leafletLayer.getContainer();
          if (el) el.style.mixBlendMode = lState.blendMode || 'normal';
        }
      } else {
        // Detach hidden layer from map display while keeping in user active stack
        if (this.jmarsMap.activeLayers[id]) {
          this.jmarsMap.removeLayer(id);
        }
      }
    });

    // Remove stale layers
    Object.keys(this.jmarsMap.activeLayers).forEach(id => {
      if (!activeIds.includes(id)) {
        this.jmarsMap.removeLayer(id);
      }
    });

    // Update Order for visible layers.
    // `activeLayers` state is already ordered [Bottom, ..., Top], which is
    // exactly what JMARSMap.updateLayerOrder expects — do not reverse it.
    const visibleIds = activeLayers.filter(l => l.visible !== false).map(l => l.id);
    this.jmarsMap.updateLayerOrder(visibleIds);
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
    activeContent.style.display = this.sectionsState.active ? 'block' : 'none';
    if (!this.sectionsState.active) {
      activeContent.classList.add('collapsed');
    }

    const activeLayers = [...jmarsState.get('activeLayers')];
    // Render Top to Bottom (Reverse of array)
    activeLayers.reverse().forEach((layerState, index) => {
      const config = getConfig(layerState.id);
      const rawName = config ? config.name : layerState.id;
      // Humanize raw ids like "MERRA2_2m_Air_Temperature_Monthly" by
      // dropping underscores; the full raw name/id is kept as a tooltip.
      const name = (rawName || '').replace(/_/g, ' ');

      const el = this.createActiveLayerItem(layerState, name, index, activeLayers.length, rawName);
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
    availableContent.style.display = this.sectionsState.available ? 'block' : 'none';
    if (!this.sectionsState.available) {
      availableContent.classList.add('collapsed');
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

  createActiveLayerItem(layerState, name, visualIndex, total, rawName) {
    const div = document.createElement('div');
    div.className = 'layer-item-container';
    div.setAttribute('tabindex', '0');
    div.title = `${name} (${layerState.id}) — Double-click to view layer settings`;

    const config = this.availableLayers.find(l => l.id === layerState.id);

    // Header: Drag Handle + Name + Actions
    const header = document.createElement('div');
    header.className = 'layer-item-header';

    // Drag Handle
    const dragHandle = document.createElement('span');
    dragHandle.className = 'layer-drag-handle';
    dragHandle.innerHTML = '&#8942;&#8942;'; // ⋮⋮ grip handle
    dragHandle.title = 'Drag to reorder layer';
    dragHandle.setAttribute('aria-hidden', 'true');

    // Layer Title
    const title = document.createElement('span');
    title.className = 'layer-item-title';
    title.textContent = name;
    title.title = `${rawName || name} (${layerState.id}) — click for metadata`;
    title.addEventListener('click', (event) => {
      event.stopPropagation();
      this.showLayerInfo(config, layerState.id);
    });

    // Action Buttons Container (single-row flexbox with nowrap)
    const actions = document.createElement('div');
    actions.className = 'layer-item-actions';

    // 1. Visibility Button
    const isVisible = layerState.visible !== false;
    const btnVisibility = document.createElement('button');
    btnVisibility.type = 'button';
    btnVisibility.className = `layer-action-btn visibility-btn ${isVisible ? 'active' : 'hidden'}`;
    btnVisibility.innerHTML = isVisible ? '&#128065;' : '&#128584;'; // 👁️ or 🙈
    btnVisibility.title = isVisible ? 'Hide Layer' : 'Show Layer';
    btnVisibility.setAttribute('aria-label', `${isVisible ? 'Hide' : 'Show'} layer ${name}`);
    btnVisibility.onclick = (e) => {
      e.stopPropagation();
      jmarsState.updateLayer(layerState.id, { visible: !isVisible });
    };

    // 2. Reorder Up (Towards Front / Top of stack)
    const btnUp = document.createElement('button');
    btnUp.type = 'button';
    btnUp.className = 'layer-action-btn move-up-btn';
    btnUp.innerHTML = '&uarr;';
    btnUp.title = 'Move Up (Front)';
    btnUp.setAttribute('aria-label', `Move layer ${name} up (to front)`);
    btnUp.disabled = visualIndex === 0; // Already at top
    btnUp.onclick = (e) => {
      e.stopPropagation();
      this.moveLayer(layerState.id, 1);
    };

    // 3. Reorder Down (Towards Back / Bottom of stack)
    const btnDown = document.createElement('button');
    btnDown.type = 'button';
    btnDown.className = 'layer-action-btn move-down-btn';
    btnDown.innerHTML = '&darr;';
    btnDown.title = 'Move Down (Back)';
    btnDown.setAttribute('aria-label', `Move layer ${name} down (to back)`);
    btnDown.disabled = visualIndex === total - 1; // Already at bottom
    btnDown.onclick = (e) => {
      e.stopPropagation();
      this.moveLayer(layerState.id, -1);
    };

    // 4. Layer Info (ⓘ metadata popup)
    const btnInfo = document.createElement('button');
    btnInfo.type = 'button';
    btnInfo.className = 'layer-action-btn info-btn';
    btnInfo.innerHTML = '&#9432;'; // ⓘ
    btnInfo.title = `View metadata for ${name}`;
    btnInfo.setAttribute('aria-label', `View metadata for ${name}`);
    btnInfo.onclick = (e) => {
      e.stopPropagation();
      this.showLayerInfo(config, layerState.id);
    };

    // 5. Layer Settings
    const btnSettings = document.createElement('button');
    btnSettings.type = 'button';
    btnSettings.className = 'layer-action-btn settings-btn';
    btnSettings.innerHTML = '&#9881;';
    btnSettings.title = 'Layer Settings';
    btnSettings.setAttribute('aria-label', `Open settings for ${name}`);
    btnSettings.onclick = (e) => {
      e.stopPropagation();
      this.openLayerSettings(layerState.id);
    };

    // 6. Remove Layer
    const btnRemove = document.createElement('button');
    btnRemove.type = 'button';
    btnRemove.className = 'layer-action-btn remove-btn';
    btnRemove.innerHTML = '&times;';
    btnRemove.title = 'Remove Layer';
    btnRemove.setAttribute('aria-label', `Remove layer ${name}`);
    btnRemove.onclick = (e) => {
      e.stopPropagation();
      jmarsState.removeLayer(layerState.id);
    };

    actions.appendChild(btnVisibility);
    actions.appendChild(btnUp);
    actions.appendChild(btnDown);
    actions.appendChild(btnInfo);
    actions.appendChild(btnSettings);
    actions.appendChild(btnRemove);

    header.appendChild(dragHandle);
    header.appendChild(title);
    header.appendChild(actions);
    div.appendChild(header);

    const visibilityContainer = document.createElement('label');
    visibilityContainer.style.display = 'flex';
    visibilityContainer.style.alignItems = 'center';
    visibilityContainer.style.gap = '6px';
    visibilityContainer.style.marginBottom = '6px';
    visibilityContainer.style.fontSize = '11px';
    visibilityContainer.style.color = '#cbd5e1';

    const visibilityCheckbox = document.createElement('input');
    visibilityCheckbox.type = 'checkbox';
    visibilityCheckbox.checked = isVisible;
    visibilityCheckbox.setAttribute('aria-label', `Toggle visibility for ${name}`);
    visibilityCheckbox.addEventListener('change', (event) => {
      jmarsState.updateLayer(layerState.id, { visible: event.target.checked });
    });

    const visibilityLabel = document.createElement('span');
    visibilityLabel.textContent = 'Visible on map';

    visibilityContainer.appendChild(visibilityCheckbox);
    visibilityContainer.appendChild(visibilityLabel);
    div.appendChild(visibilityContainer);

    // Opacity Slider
    const sliderContainer = document.createElement('div');
    sliderContainer.style.display = 'flex';
    sliderContainer.style.alignItems = 'center';
    sliderContainer.style.opacity = isVisible ? '1' : '0.55';

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
    slider.setAttribute('aria-label', `Opacity for ${name}`);
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
      if (event.target.closest('button, input')) return;
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
      // Prevent drag initiation from interactive controls (sliders, buttons)
      if (e.target.closest('input, button')) {
        e.preventDefault();
        return;
      }
      e.dataTransfer.setData('text/plain', layerState.id);
      e.dataTransfer.effectAllowed = 'move';
      div.classList.add('is-dragging');
      this.draggedElement = div;
    });

    div.addEventListener('dragend', () => {
      div.classList.remove('is-dragging');
      this.draggedElement = null;
      this.container.querySelectorAll('.layer-item-container').forEach(el => {
        el.classList.remove('drag-over-top', 'drag-over-bottom');
      });
    });

    div.addEventListener('dragover', (e) => {
      e.preventDefault();
      e.dataTransfer.dropEffect = 'move';
      if (this.draggedElement === div) return false;

      const rect = div.getBoundingClientRect();
      const midY = rect.top + rect.height / 2;
      if (e.clientY < midY) {
        div.classList.add('drag-over-top');
        div.classList.remove('drag-over-bottom');
      } else {
        div.classList.add('drag-over-bottom');
        div.classList.remove('drag-over-top');
      }
      return false;
    });

    div.addEventListener('dragleave', (e) => {
      // Only remove indicator when truly leaving the container element
      if (!div.contains(e.relatedTarget)) {
        div.classList.remove('drag-over-top', 'drag-over-bottom');
      }
    });

    div.addEventListener('drop', (e) => {
      e.stopPropagation();
      e.preventDefault();
      
      const insertBefore = div.classList.contains('drag-over-top');
      div.classList.remove('drag-over-top', 'drag-over-bottom');

      const draggedId = e.dataTransfer.getData('text/plain');
      const targetId = layerState.id;

      if (!draggedId || draggedId === targetId) return false;

      this.handleReorder(draggedId, targetId, insertBefore);
      return false;
    });

    return div;
  }

  handleReorder(draggedId, targetId, insertBefore = true) {
    // State order is [Bottom, ..., Top]
    // DOM order is [Top, ..., Bottom]
    
    const stateLayers = [...jmarsState.get('activeLayers')];
    const domOrderIds = stateLayers.map(l => l.id).reverse();

    const oldIndex = domOrderIds.indexOf(draggedId);
    if (oldIndex < 0) return;

    // Remove dragged item from DOM sequence
    domOrderIds.splice(oldIndex, 1);

    let newIndex = domOrderIds.indexOf(targetId);
    if (newIndex < 0) return;

    if (!insertBefore) {
      newIndex += 1;
    }

    // Insert at target position in DOM sequence
    domOrderIds.splice(newIndex, 0, draggedId);

    // Reverse back to state order [Bottom, ..., Top]
    const newStateOrder = domOrderIds.reverse();
    jmarsState.reorderLayers(newStateOrder);
  }

  createAvailableLayerItem(layer) {
    const div = document.createElement('div');
    div.className = 'layer-item-container';
    div.style.display = 'flex';
    div.style.justifyContent = 'space-between';
    div.style.alignItems = 'center';
    div.style.gap = '6px';
    div.style.padding = '5px 8px';

    // Many WMS layer IDs (e.g. GIBS "MERRA2_2m_Air_Temperature_Monthly") use
    // underscores as word separators. Render a human-readable label while
    // keeping the raw id/name available via the title tooltip.
    const displayName = (layer.name || layer.id || '').replace(/_/g, ' ');

    const span = document.createElement('span');
    span.textContent = displayName;
    span.title = `${layer.name || layer.id || ''} — click for layer info`;
    span.style.fontSize = '12px';
    span.style.fontWeight = '500';
    span.style.color = '#e2e8f0';
    span.style.cursor = 'pointer';
    // Let the label shrink/truncate instead of pushing the Add button
    // off the edge of the sidebar for very long layer names.
    span.style.flex = '1 1 auto';
    span.style.minWidth = '0';
    span.style.overflow = 'hidden';
    span.style.textOverflow = 'ellipsis';
    span.style.whiteSpace = 'nowrap';
    // Clicking the name previews metadata (abstract, source, attribution)
    // before the user decides to add the layer.
    span.addEventListener('click', () => this.showLayerInfo(layer));

    const btnInfo = document.createElement('button');
    btnInfo.innerHTML = '&#9432;'; // ⓘ
    btnInfo.title = `View metadata for ${displayName}`;
    btnInfo.setAttribute('aria-label', `View metadata for ${displayName}`);
    btnInfo.style.background = 'transparent';
    btnInfo.style.border = '1px solid #444';
    btnInfo.style.color = '#cbd5e1';
    btnInfo.style.borderRadius = '3px';
    btnInfo.style.cursor = 'pointer';
    btnInfo.style.flex = '0 0 auto';
    btnInfo.onclick = () => this.showLayerInfo(layer);

    const btnAdd = document.createElement('button');
    btnAdd.type = 'button';
    btnAdd.textContent = '+ Add';
    btnAdd.title = `Add ${displayName}`;
    btnAdd.setAttribute('aria-label', `Add ${displayName}`);
    btnAdd.style.background = '#0284c7';
    btnAdd.style.border = '1px solid #0369a1';
    btnAdd.style.color = 'white';
    btnAdd.style.padding = '3px 8px';
    btnAdd.style.borderRadius = '4px';
    btnAdd.style.fontSize = '11px';
    btnAdd.style.fontWeight = '600';
    btnAdd.style.cursor = 'pointer';
    btnAdd.style.flex = '0 0 auto';
    btnAdd.onclick = () => jmarsState.addLayer(layer.id);

    div.appendChild(span);
    div.appendChild(btnInfo);
    div.appendChild(btnAdd);
    return div;
  }

  /**
   * Show the metadata/info popup for a layer (active or not-yet-added).
   * Wires the layer config's `abstract` (parsed from WMS GetCapabilities,
   * see JMARSWMS.parseCapabilities) into the InfoPanel's "capabilities"
   * argument so the Description section renders when available.
   * @param {object} config - Layer config from availableLayers.
   * @param {string} [layerId] - Fallback layer id if config lookup failed.
   */
  showLayerInfo(config, layerId) {
    if (!window.jsmarsInfoPanel) return;
    const layerConfig = config || { id: layerId, name: layerId };
    window.jsmarsInfoPanel.open(layerConfig, { abstract: layerConfig.abstract });
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

    const visibilitySection = document.createElement('div');
    visibilitySection.className = 'layer-settings-visibility';
    visibilitySection.style.marginBottom = '12px';

    const visibilityLabel = document.createElement('label');
    visibilityLabel.style.display = 'flex';
    visibilityLabel.style.alignItems = 'center';
    visibilityLabel.style.gap = '8px';
    visibilityLabel.style.fontSize = '12px';
    visibilityLabel.style.color = '#e2e8f0';

    const visibilityInput = document.createElement('input');
    visibilityInput.type = 'checkbox';
    visibilityInput.checked = layerState.visible !== false;
    visibilityInput.addEventListener('change', (event) => {
      jmarsState.updateLayer(layerId, { visible: event.target.checked });
    });

    const visibilityText = document.createElement('span');
    visibilityText.textContent = 'Visible on map';

    visibilityLabel.appendChild(visibilityInput);
    visibilityLabel.appendChild(visibilityText);
    visibilitySection.appendChild(visibilityLabel);

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
      this.showLayerInfo(config, layerId);
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
    this.settingsContent.appendChild(visibilitySection);
    this.settingsContent.appendChild(opacitySection);
    this.settingsContent.appendChild(blendSection);
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
