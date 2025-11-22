export class JMARSState {
  constructor() {
    this.state = {
      body: 'Mars',
      activeLayers: [], // List of { id, opacity, visible } in display order (bottom to top)
      overlays: {
        graticule: false,
        panner: false,
        scalebar: true,
        northArrow: true
      },
      view: {
        lat: 0,
        lng: 0,
        zoom: 2
      }
    };

    this.listeners = {};
  }

  get(key) {
    return this.state[key];
  }

  set(key, value) {
    this.state[key] = value;
    this.emit('change', { key, value, state: this.state });
    this.emit(`change:${key}`, value);
  }

  // Layers Management
  addLayer(layerId) {
    if (this.state.activeLayers.find(l => l.id === layerId)) return;

    // Add to top
    this.state.activeLayers.push({ id: layerId, opacity: 1, visible: true });
    this.emit('layers-changed', this.state.activeLayers);
  }

  removeLayer(layerId) {
    this.state.activeLayers = this.state.activeLayers.filter(l => l.id !== layerId);
    this.emit('layers-changed', this.state.activeLayers);
  }

  updateLayer(layerId, updates) {
    const layer = this.state.activeLayers.find(l => l.id === layerId);
    if (layer) {
      Object.assign(layer, updates);
      this.emit('layers-changed', this.state.activeLayers);
    }
  }

  reorderLayers(newOrderIds) {
    // innovative sorting based on newOrderIds
    const currentLayers = [...this.state.activeLayers];
    this.state.activeLayers = newOrderIds
      .map(id => currentLayers.find(l => l.id === id))
      .filter(Boolean);

    this.emit('layers-changed', this.state.activeLayers);
  }

  // Overlays
  toggleOverlay(overlayId, isActive) {
    this.state.overlays[overlayId] = isActive;
    this.emit('overlays-changed', this.state.overlays);
  }

  // Events
  on(event, callback) {
    if (!this.listeners[event]) this.listeners[event] = [];
    this.listeners[event].push(callback);
  }

  emit(event, data) {
    if (this.listeners[event]) {
      this.listeners[event].forEach(cb => cb(data));
    }
    // Also dispatch global custom event for loose coupling
    const customEvent = new CustomEvent(`jmars:${event}`, { detail: data });
    document.dispatchEvent(customEvent);
  }
}

// Singleton instance
export const jmarsState = new JMARSState();
