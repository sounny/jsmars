/**
 * @module PluginRegistry
 * @description Registry for jsMars plugins and features.
 *
 * Plugins register themselves with metadata (name, icon, section, init function)
 * and the registry handles initialization ordering and section grouping.
 *
 * Currently used for sidebar section generation and plugin lifecycle management.
 * To add a new plugin, call `register()` with a unique ID and config object,
 * then call `initAll()` after all plugins are registered.
 */

/**
 * @class PluginRegistry
 * @description Manages plugin registration, initialization, and lookup.
 */
export class PluginRegistry {
  constructor() {
    /**
     * Registered plugins keyed by ID.
     * @type {Map<string, {name: string, icon: string, init: Function, section: string, order: number}>}
     */
    this.plugins = new Map();
  }

  /**
   * Register a plugin with the registry.
   * If a plugin with the same ID already exists, it will be overwritten
   * (a warning is logged to aid debugging).
   *
   * @param {string} id - Unique plugin identifier (e.g., 'crater-counter', 'shapes').
   * @param {Object} config - Plugin configuration.
   * @param {string} config.name - Display name for the UI.
   * @param {string} [config.icon] - Icon character or emoji for sidebar display.
   * @param {string} [config.section='tools'] - Sidebar section ('tools', 'layers', 'data').
   * @param {Function} config.init - Initialization function receiving (map, toolManager).
   * @param {number} [config.order=100] - Sort order within section (lower values appear first).
   */
  register(id, config) {
    if (this.plugins.has(id)) {
      console.warn(`PluginRegistry: Overwriting existing plugin "${id}".`);
    }
    this.plugins.set(id, { order: 100, section: 'tools', ...config });
  }

  /**
   * Initialize all registered plugins in order.
   *
   * Plugins are sorted by their `order` property (ascending) before
   * initialization. Each plugin's `init()` is called within a try/catch
   * so one failing plugin does not prevent others from initializing.
   *
   * @param {L.Map} map - The Leaflet map instance.
   * @param {ToolManager} toolManager - The tool manager instance.
   * @returns {Map<string, *>} Map of plugin ID to the return value of its init().
   */
  async initAll(map, toolManager) {
    const results = new Map();
    const sorted = [...this.plugins.entries()].sort((a, b) => a[1].order - b[1].order);

    for (const [id, config] of sorted) {
      try {
        // Await in case init() returns a Promise (async plugin initialization)
        const result = await config.init(map, toolManager);
        results.set(id, result);
        console.debug(`Plugin initialized: ${id}`);
      } catch (err) {
        console.error(`Failed to initialize plugin "${id}":`, err);
      }
    }

    return results;
  }

  /**
   * Get a plugin configuration by ID.
   * @param {string} id - Plugin identifier.
   * @returns {Object|undefined} The plugin config, or undefined if not found.
   */
  get(id) {
    return this.plugins.get(id);
  }

  /**
   * List all plugins belonging to a specific sidebar section.
   * @param {string} section - Section name (e.g., 'tools', 'layers', 'data').
   * @returns {Array<[string, Object]>} Array of [id, config] pairs, sorted by order.
   */
  getBySection(section) {
    return [...this.plugins.entries()]
      .filter(([, config]) => config.section === section)
      .sort((a, b) => a[1].order - b[1].order);
  }
}
