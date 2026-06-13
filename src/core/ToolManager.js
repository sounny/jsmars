import { EventBus } from './EventBus.js';

/**
 * Manages tool activation/deactivation.
 * Only one tool can be active at a time.
 * Tools register themselves and the manager handles mutual exclusion.
 */
export class ToolManager {
  constructor() {
    /** @type {Map<string, {activate: Function, deactivate: Function, button?: HTMLElement}>} */
    this.tools = new Map();
    /** @type {string|null} */
    this.activeTool = null;
  }

  /**
   * Register a tool with the manager.
   * @param {string} name - Unique tool identifier
   * @param {object} tool - Tool object with activate() and deactivate() methods
   * @param {HTMLElement} [button] - Optional button element to toggle .active class
   */
  register(name, tool, button) {
    this.tools.set(name, { tool, button });
  }

  /**
   * Activate a tool by name, deactivating any currently active tool.
   * @param {string} name - Tool name
   * @param {*} [mode] - Optional mode parameter passed to tool.activate()
   */
  activate(name, mode) {
    if (this.activeTool === name) {
      // Toggle off
      this.deactivateCurrent();
      return;
    }
    
    // Deactivate current tool
    this.deactivateCurrent();
    
    const entry = this.tools.get(name);
    if (!entry) {
      console.warn(`ToolManager: Unknown tool "${name}"`);
      return;
    }
    
    this.activeTool = name;
    if (typeof entry.tool.activate === 'function') {
      entry.tool.activate(mode);
    }
    if (entry.button) {
      entry.button.classList.add('active');
    }
    
    EventBus.emit('jmars:tool-activated', { tool: name, mode });
  }

  /**
   * Deactivate the currently active tool.
   */
  deactivateCurrent() {
    if (!this.activeTool) return;
    
    const entry = this.tools.get(this.activeTool);
    if (entry) {
      if (typeof entry.tool.deactivate === 'function') {
        entry.tool.deactivate();
      }
      if (entry.button) {
        entry.button.classList.remove('active');
      }
    }
    
    const prevTool = this.activeTool;
    this.activeTool = null;
    EventBus.emit('jmars-tool-deactivated', { tool: prevTool });
  }

  /**
   * Check if a specific tool is active.
   * @param {string} name
   * @returns {boolean}
   */
  isActive(name) {
    return this.activeTool === name;
  }

  /**
   * Get a registered tool by name.
   * @param {string} name
   * @returns {object|undefined}
   */
  get(name) {
    const entry = this.tools.get(name);
    return entry ? entry.tool : undefined;
  }
}
