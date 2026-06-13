import { EVENTS } from '../constants.js';

/**
 * KeyboardShortcuts provides global keyboard shortcut handling for jsMars.
 * Matches common shortcuts from the JMARS desktop application.
 *
 * Shortcuts:
 *   Ctrl+S       Save session
 *   Ctrl+O       Load session
 *   Ctrl+K       Quick actions palette
 *   Ctrl+Shift+E Export map
 *   Ctrl+G       Go to coordinates (prompt)
 *   Ctrl+Z       Undo last shape action
 *   Escape       Cancel current tool
 *   ?            Show shortcuts help
 *   1-5          Quick zoom levels
 *   + / -        Zoom in / out
 *   R            Reset view
 */
export class KeyboardShortcuts {
  /**
   * @param {L.Map} map - Leaflet map instance
   * @param {object} handlers - Named handler functions
   */
  constructor(map, handlers = {}) {
    this.map = map;
    this.handlers = handlers;
    this.helpVisible = false;
    this._init();
  }

  _init() {
    document.addEventListener('keydown', (e) => this._onKeyDown(e));
    this._createHelpModal();
  }

  /**
   * @param {KeyboardEvent} e
   */
  _onKeyDown(e) {
    // Ignore if focus is in input/textarea/select
    const tag = e.target.tagName.toLowerCase();
    if (tag === 'input' || tag === 'textarea' || tag === 'select') return;
    if (e.target.isContentEditable) return;

    const ctrl = e.ctrlKey || e.metaKey;
    const shift = e.shiftKey;
    const key = e.key.toLowerCase();

    // Ctrl+S: Save session
    if (ctrl && key === 's') {
      e.preventDefault();
      this.handlers.saveSession?.();
      return;
    }

    // Ctrl+O: Load session
    if (ctrl && key === 'o') {
      e.preventDefault();
      this.handlers.loadSession?.();
      return;
    }

    // Ctrl+K: Quick actions
    if (ctrl && key === 'k') {
      e.preventDefault();
      this.handlers.quickActions?.();
      return;
    }

    // Ctrl+Shift+E: Export map
    if (ctrl && shift && key === 'e') {
      e.preventDefault();
      this.handlers.exportMap?.();
      return;
    }

    // Ctrl+G: Go to coordinates
    if (ctrl && key === 'g') {
      e.preventDefault();
      this._goToCoordinates();
      return;
    }

    // Escape: Cancel current tool
    if (key === 'escape') {
      this.handlers.cancelTool?.();
      if (this.helpVisible) this._hideHelp();
      return;
    }

    // ?: Show help
    if (key === '?' || (shift && key === '/')) {
      this._toggleHelp();
      return;
    }

    // R: Reset view
    if (key === 'r' && !ctrl) {
      this.handlers.resetView?.();
      return;
    }

    // +/= : Zoom in
    if (key === '+' || key === '=') {
      this.map.zoomIn();
      return;
    }

    // -: Zoom out
    if (key === '-') {
      this.map.zoomOut();
      return;
    }

    // 1-5: Quick zoom levels
    if (key >= '1' && key <= '5' && !ctrl) {
      const zoomLevels = { '1': 2, '2': 4, '3': 6, '4': 8, '5': 10 };
      this.map.setZoom(zoomLevels[key]);
      return;
    }

    // F: Fullscreen toggle
    if (key === 'f' && !ctrl) {
      this._toggleFullscreen();
      return;
    }
  }

  /**
   * Prompt user for lat/lon and navigate there.
   */
  _goToCoordinates() {
    const input = prompt('Go to coordinates (lat, lon):');
    if (!input) return;

    const cleaned = input.replace(/[°'"NSEW]/gi, ' ').trim();
    const match = cleaned.match(/^(-?\d+\.?\d*)[,\s]+(-?\d+\.?\d*)$/);
    if (match) {
      const lat = parseFloat(match[1]);
      const lon = parseFloat(match[2]);
      if (lat >= -90 && lat <= 90) {
        this.map.setView([lat, lon], Math.max(this.map.getZoom(), 6));
      }
    }
  }

  /**
   * Toggle fullscreen mode.
   */
  _toggleFullscreen() {
    if (document.fullscreenElement) {
      document.exitFullscreen?.();
    } else {
      document.documentElement.requestFullscreen?.();
    }
  }

  /**
   * Create the help modal DOM.
   */
  _createHelpModal() {
    this.helpModal = document.createElement('div');
    this.helpModal.id = 'keyboard-help-modal';
    this.helpModal.className = 'welcome-modal-backdrop';
    this.helpModal.style.display = 'none';
    this.helpModal.innerHTML = `
      <div class="welcome-modal" style="max-width:480px; text-align:left">
        <h2 style="margin-top:0; text-align:center">Keyboard Shortcuts</h2>
        <div class="shortcut-grid">
          <div class="shortcut-section">
            <h3>Navigation</h3>
            <div class="shortcut-row"><kbd>+</kbd> / <kbd>-</kbd> <span>Zoom in / out</span></div>
            <div class="shortcut-row"><kbd>1</kbd>-<kbd>5</kbd> <span>Quick zoom levels</span></div>
            <div class="shortcut-row"><kbd>R</kbd> <span>Reset view</span></div>
            <div class="shortcut-row"><kbd>Ctrl+G</kbd> <span>Go to coordinates</span></div>
            <div class="shortcut-row"><kbd>F</kbd> <span>Toggle fullscreen</span></div>
          </div>
          <div class="shortcut-section">
            <h3>Session</h3>
            <div class="shortcut-row"><kbd>Ctrl+S</kbd> <span>Save session</span></div>
            <div class="shortcut-row"><kbd>Ctrl+O</kbd> <span>Load session</span></div>
            <div class="shortcut-row"><kbd>Ctrl+K</kbd> <span>Quick actions</span></div>
          </div>
          <div class="shortcut-section">
            <h3>Tools</h3>
            <div class="shortcut-row"><kbd>Esc</kbd> <span>Cancel current tool</span></div>
            <div class="shortcut-row"><kbd>Ctrl+Shift+E</kbd> <span>Export map</span></div>
            <div class="shortcut-row"><kbd>?</kbd> <span>Show this help</span></div>
          </div>
        </div>
        <div style="text-align:center; margin-top:16px">
          <button id="keyboard-help-close" style="padding:6px 20px; cursor:pointer; background:#333; color:#fff; border:1px solid #555; border-radius:4px">Close</button>
        </div>
      </div>
    `;

    document.body.appendChild(this.helpModal);

    this.helpModal.querySelector('#keyboard-help-close').addEventListener('click', () => this._hideHelp());
    this.helpModal.addEventListener('click', (e) => {
      if (e.target === this.helpModal) this._hideHelp();
    });
  }

  _toggleHelp() {
    if (this.helpVisible) {
      this._hideHelp();
    } else {
      this._showHelp();
    }
  }

  _showHelp() {
    this.helpModal.style.display = 'flex';
    this.helpVisible = true;
  }

  _hideHelp() {
    this.helpModal.style.display = 'none';
    this.helpVisible = false;
  }
}
