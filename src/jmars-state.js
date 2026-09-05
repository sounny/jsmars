/**
 * @module jmars-state
 * @description Central state management for jsMars.
 *
 * This module provides a singleton state store that tracks the active body,
 * layers, overlays, and view position. It uses a dual event system:
 *
 * 1. **Internal listeners** registered via `jmarsState.on(eventName, cb)`.
 *    These receive the raw event name as passed to `emit()`.
 *
 * 2. **DOM CustomEvents** dispatched on `document`. These always use the
 *    event name exactly as provided (all EVENTS constants now include
 *    the 'jmars:' prefix).
 *
 * WARNING: `reset()` clears ALL internal listeners. Any module that
 * registered via `on()` will silently stop receiving events.
 */
import { EVENTS } from './constants.js';

/**
 * @class JMARSState
 * @description Lightweight reactive state store for jsMars application state.
 */
export class JMARSState {
  constructor() {
    /** @type {{ body: string, activeLayers: Array<{id:string, opacity:number, visible:boolean}>, overlays: Object, view: {lat:number, lng:number, zoom:number} }} */
    this.state = {
      body: 'mars',
      activeLayers: [], // List of { id, opacity, visible } in display order (bottom to top)
      overlays: {
        graticule: false,
        panner: false,
        scalebar: true,
        northArrow: false
      },
      view: {
        lat: 0,
        lng: 0,
        zoom: 2
      }
    };

    /** @type {Object<string, Function[]>} Internal event listener map */
    this.listeners = {};
  }

  /**
   * Normalize a layer state object to the persisted app contract.
   * @param {object} layer
   * @returns {{id:string, opacity:number, visible:boolean}|null}
   */
  normalizeLayerState(layer) {
    if (!layer || !layer.id) return null;
    const opacity = typeof layer.opacity === 'number' && Number.isFinite(layer.opacity)
      ? Math.max(0, Math.min(1, layer.opacity))
      : 1;

    return {
      ...layer,
      id: layer.id,
      opacity,
      visible: layer.visible !== false
    };
  }

  /**
   * Normalize an array of layer state objects.
   * @param {Array<{id:string, opacity:number, visible:boolean}>} layers
   * @returns {Array<{id:string, opacity:number, visible:boolean}>}
   */
  normalizeActiveLayers(layers) {
    if (!Array.isArray(layers)) return [];
    return layers.map(layer => this.normalizeLayerState(layer)).filter(Boolean);
  }

  /**
   * Get a top-level state value by key.
   * @param {string} key - The state property name ('body', 'activeLayers', 'overlays', 'view').
   * @returns {*} The current value.
   */
  get(key) {
    return this.state[key];
  }

  /**
   * Set a top-level state value and emit change events.
   * Emits both a generic 'change' event and a specific 'change:<key>' event.
   * @param {string} key - The state property name.
   * @param {*} value - The new value.
   */
  set(key, value) {
    if (key === 'body' && typeof value === 'string') {
      value = value.toLowerCase();
    }
    this.state[key] = value;
    this.emit('change', { key, value, state: this.state });
    this.emit(`change:${key}`, value);
  }

  // ── Layer Management ──────────────────────────────────────

  /**
   * Add a layer to the top of the active stack.
   * No-op if the layer is already active.
   * @param {string} layerId - The layer ID to add.
   */
  addLayer(layerId) {
    if (this.state.activeLayers.find(l => l.id === layerId)) return;
    this.state.activeLayers.push(this.normalizeLayerState({ id: layerId, opacity: 1, visible: true }));
    this.emit(EVENTS.LAYERS_CHANGED, this.state.activeLayers);
  }

  /**
   * Remove a layer from the active stack.
   * @param {string} layerId - The layer ID to remove.
   */
  removeLayer(layerId) {
    this.state.activeLayers = this.state.activeLayers.filter(l => l.id !== layerId);
    this.emit(EVENTS.LAYERS_CHANGED, this.state.activeLayers);
  }

  /**
   * Update properties of an active layer (e.g., opacity, visibility).
   * @param {string} layerId - The layer ID to update.
   * @param {Object} updates - Properties to merge (e.g., { opacity: 0.5 }).
   */
  updateLayer(layerId, updates) {
    const layer = this.state.activeLayers.find(l => l.id === layerId);
    if (layer) {
      const normalized = this.normalizeLayerState({ ...layer, ...updates });
      Object.assign(layer, normalized);
      this.emit(EVENTS.LAYERS_CHANGED, this.state.activeLayers);
    }
  }

  /**
   * Replace the entire active layers array.
   * @param {Array<{id:string, opacity:number, visible:boolean}>} layers - New layer stack.
   */
  setActiveLayers(layers) {
    this.state.activeLayers = this.normalizeActiveLayers(layers);
    this.emit(EVENTS.LAYERS_CHANGED, this.state.activeLayers);
  }

  /**
   * Reorder active layers to match the given ID sequence.
   * Layers not in newOrderIds are silently dropped.
   * @param {string[]} newOrderIds - Layer IDs in desired order.
   */
  reorderLayers(newOrderIds) {
    const currentLayers = [...this.state.activeLayers];
    this.state.activeLayers = this.normalizeActiveLayers(newOrderIds
      .map(id => currentLayers.find(l => l.id === id))
      .filter(Boolean));
    this.emit(EVENTS.LAYERS_CHANGED, this.state.activeLayers);
  }

  // ── Overlays ──────────────────────────────────────────────

  /**
   * Toggle an overlay's visibility.
   * @param {string} overlayId - The overlay key (e.g., 'graticule', 'scalebar').
   * @param {boolean} isActive - Whether the overlay should be visible.
   */
  toggleOverlay(overlayId, isActive) {
    this.state.overlays[overlayId] = isActive;
    this.emit(EVENTS.OVERLAYS_CHANGED, this.state.overlays);
  }

  // ── Event System ──────────────────────────────────────────

  /**
   * Register an internal event listener.
   * @param {string} event - The event name to listen for.
   * @param {Function} callback - The callback function.
   */
  on(event, callback) {
    if (!this.listeners[event]) this.listeners[event] = [];
    this.listeners[event].push(callback);
  }

  /**
   * Emit an event to both internal listeners and the DOM.
   *
   * Internal listeners receive the exact event name passed here.
   * A DOM CustomEvent is also dispatched on `document` using the
   * same event name. Since all EVENTS constants now include the
   * 'jmars:' prefix, no auto-prefixing is needed for those.
   *
   * For internal events like 'change' and 'change:<key>', the
   * DOM event is prefixed with 'jmars:' if not already present.
   *
   * @param {string} event - The event name.
   * @param {*} data - The event payload (becomes CustomEvent.detail).
   */
  emit(event, data) {
    // Fire internal listeners
    if (this.listeners[event]) {
      this.listeners[event].forEach(cb => cb(data));
    }

    // Dispatch DOM CustomEvent for loose coupling.
    // Auto-prefix internal events (like 'change') that lack the 'jmars:' prefix.
    const eventName = event.startsWith('jmars:') ? event : `jmars:${event}`;
    const customEvent = new CustomEvent(eventName, { detail: data });
    document.dispatchEvent(customEvent);
  }

  /**
   * Reset state to defaults.
   * WARNING: This clears ALL internal listeners. Modules that registered
   * via on() will silently stop receiving events and must re-register.
   */
  reset() {
    this.state = {
      body: 'mars',
      activeLayers: [],
      overlays: {
        graticule: false,
        panner: false,
        scalebar: true,
        northArrow: false
      },
      view: {
        lat: 0,
        lng: 0,
        zoom: 2
      }
    };
    this.listeners = {};
  }
}

/** @type {JMARSState} Singleton state instance for the application. */
export const jmarsState = new JMARSState();
