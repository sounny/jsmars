/**
 * @module ToolManager
 * @description Manages tool activation/deactivation with mutual exclusion.
 *
 * Only one tool can be active at a time. When a tool is activated, any
 * currently active tool is deactivated first. Dispatches DOM events via
 * EventBus so other modules (e.g., index.html button handlers) can
 * respond to tool state changes.
 *
 * Note: If the same tool is activated twice, it toggles off. This is
 * the expected UI behavior for tool buttons.
 */
import { EventBus } from './EventBus.js';
import { EVENTS } from '../constants.js';

/**
 * @class ToolManager
 * @description Centralized tool lifecycle manager with mutual exclusion.
 */
export class ToolManager {
  constructor() {
    /** @type {Map<string, {tool: {activate: Function, deactivate: Function}, button?: HTMLElement}>} */
    this.tools = new Map();
    /** @type {string|null} The name of the currently active tool, or null. */
    this.activeTool = null;
  }

  /**
   * Register a tool with the manager.
   * @param {string} name - Unique tool identifier (e.g., 'measure', 'crater', 'profile').
   * @param {{activate: Function, deactivate: Function}} tool - Tool object with lifecycle methods.
   * @param {HTMLElement} [button] - Optional button element to toggle the .active CSS class.
   */
  register(name, tool, button) {
    this.tools.set(name, { tool, button });
  }

  /**
   * Activate a tool by name, deactivating any currently active tool first.
   * If the same tool is already active, it toggles off instead.
   * @param {string} name - Tool name to activate.
   * @param {*} [mode] - Optional mode parameter passed to tool.activate() (e.g., 'distance' or 'area').
   */
  activate(name, mode) {
    if (this.activeTool === name) {
      // Toggle off if clicking the same tool again
      this.deactivateCurrent();
      return;
    }

    // Deactivate any currently active tool
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

    EventBus.emit(EVENTS.TOOL_ACTIVATED, { tool: name, mode });
  }

  /**
   * Deactivate the currently active tool and dispatch a deactivation event.
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
    EventBus.emit(EVENTS.TOOL_DEACTIVATED, { tool: prevTool });
  }

  /**
   * Check if a specific tool is currently active.
   * @param {string} name - Tool name to check.
   * @returns {boolean} True if the tool is active.
   */
  isActive(name) {
    return this.activeTool === name;
  }

  /**
   * Get a registered tool's implementation object by name.
   * @param {string} name - Tool name.
   * @returns {{activate: Function, deactivate: Function}|undefined} The tool object, or undefined.
   */
  get(name) {
    const entry = this.tools.get(name);
    return entry ? entry.tool : undefined;
  }
}
