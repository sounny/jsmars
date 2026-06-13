/**
 * Registry for jsMars plugins/features.
 * Plugins register themselves with metadata and the registry
 * handles initialization order and sidebar section creation.
 */
export class PluginRegistry {
  constructor() {
    /** @type {Map<string, {name: string, icon: string, init: Function, section: string}>} */
    this.plugins = new Map();
    this._initialized = false;
  }

  /**
   * Register a plugin.
   * @param {string} id - Unique plugin identifier
   * @param {object} config - Plugin configuration
   * @param {string} config.name - Display name
   * @param {string} [config.icon] - Icon character or emoji
   * @param {string} [config.section] - Sidebar section ('tools', 'layers', 'data')
   * @param {Function} config.init - Initialization function(map, toolManager)
   * @param {number} [config.order] - Sort order (lower = first)
   */
  register(id, config) {
    this.plugins.set(id, { order: 100, section: 'tools', ...config });
  }

  /**
   * Initialize all registered plugins.
   * @param {object} map - Leaflet map instance
   * @param {ToolManager} toolManager - Tool manager instance
   * @returns {Map<string, *>} Map of plugin ID to init result
   */
  initAll(map, toolManager) {
    const results = new Map();
    const sorted = [...this.plugins.entries()].sort((a, b) => a[1].order - b[1].order);
    
    for (const [id, config] of sorted) {
      try {
        const result = config.init(map, toolManager);
        results.set(id, result);
        console.debug(`Plugin initialized: ${id}`);
      } catch (err) {
        console.error(`Failed to initialize plugin "${id}":`, err);
      }
    }
    
    this._initialized = true;
    return results;
  }

  /**
   * Get a plugin by ID.
   * @param {string} id
   * @returns {object|undefined}
   */
  get(id) {
    return this.plugins.get(id);
  }

  /**
   * List all plugins in a section.
   * @param {string} section
   * @returns {Array}
   */
  getBySection(section) {
    return [...this.plugins.entries()]
      .filter(([, config]) => config.section === section)
      .sort((a, b) => a[1].order - b[1].order);
  }
}
