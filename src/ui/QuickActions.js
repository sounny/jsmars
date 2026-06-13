/**
 * @module QuickActions
 * @description Provides a quick command palette for common actions.
 * Users type commands like "Save Session" or "Reset View" and the
 * palette dispatches to registered handler callbacks.
 *
 * NOTE: The Ctrl+K keyboard shortcut is handled globally by
 * KeyboardShortcuts.js; this module only provides the UI and
 * command execution.
 */
export class QuickActions {
    /**
     * Create a new QuickActions panel.
     * @param {string} containerId - DOM id of the container element
     * @param {object} [options] - Callback handlers for each command
     * @param {Function} [options.onSaveSession] - Handler for "Save Session"
     * @param {Function} [options.onLoadSession] - Handler for "Load Session"
     * @param {Function} [options.onResetView] - Handler for "Reset View"
     * @param {Function} [options.onOpenLayers] - Handler for "Open Layer Manager"
     * @param {Function} [options.onOpenTools] - Handler for "Open Tools"
     * @param {Function} [options.onToggleSidebar] - Handler for "Toggle Sidebar"
     * @param {Function} [options.onStatus] - Callback for status/feedback messages
     */
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        this.options = options;
        /** @type {HTMLInputElement|null} */
        this.commandInput = null;

        if (!this.container) return;
        this.render();
        this.bindEvents();
    }

    /**
     * Build the quick actions DOM (input, run button, datalist, hint).
     * @private
     */
    render() {
        this.container.innerHTML = `
            <div class="quick-actions" aria-label="Quick actions panel">
                <label for="quick-action-input" class="quick-actions-label">Quick Command</label>
                <div class="quick-actions-row">
                    <input
                        id="quick-action-input"
                        class="quick-actions-input"
                        type="text"
                        list="quick-action-suggestions"
                        placeholder="Try: Save Session"
                        aria-label="Quick command input"
                    />
                    <button id="quick-action-run" class="tool-btn quick-actions-run-btn" type="button">Run</button>
                </div>
                <datalist id="quick-action-suggestions">
                    <option value="Save Session"></option>
                    <option value="Load Session"></option>
                    <option value="Reset View"></option>
                    <option value="Open Layer Manager"></option>
                    <option value="Open Tools"></option>
                    <option value="Toggle Sidebar"></option>
                </datalist>
                <p class="quick-actions-hint">Shortcut: <kbd>Ctrl</kbd>/<kbd>⌘</kbd> + <kbd>K</kbd></p>
            </div>
        `;

        this.commandInput = this.container.querySelector('#quick-action-input');
    }

    /**
     * Bind click and keyboard events for the run button and input.
     * NOTE: Ctrl+K is NOT handled here; KeyboardShortcuts.js owns that binding.
     * @private
     */
    bindEvents() {
        const runBtn = this.container.querySelector('#quick-action-run');
        if (!runBtn || !this.commandInput) return;

        const runCommand = () => {
            const command = this.commandInput.value.trim().toLowerCase();
            if (!command) return;
            this.executeCommand(command);
        };

        runBtn.addEventListener('click', runCommand);
        this.commandInput.addEventListener('keydown', (event) => {
            if (event.key === 'Enter') {
                event.preventDefault();
                runCommand();
            }
        });
    }

    /**
     * Execute a text command by matching against known keywords.
     * @param {string} command - Normalized lowercase command string
     * @private
     */
    executeCommand(command) {
        const normalized = command.replace(/\s+/g, ' ');

        if (normalized.includes('save')) {
            this.options.onSaveSession?.();
            this.announce('Session saved.');
            return;
        }

        if (normalized.includes('load')) {
            this.options.onLoadSession?.();
            this.announce('Choose a session file to load.');
            return;
        }

        if (normalized.includes('reset')) {
            this.options.onResetView?.();
            this.announce('Map view reset.');
            return;
        }

        if (normalized.includes('layer')) {
            this.options.onOpenLayers?.();
            this.announce('Layer Manager opened.');
            return;
        }

        if (normalized.includes('tool')) {
            this.options.onOpenTools?.();
            this.announce('Tools panel opened.');
            return;
        }

        if (normalized.includes('sidebar') || normalized.includes('toggle')) {
            this.options.onToggleSidebar?.();
            this.announce('Sidebar toggled.');
            return;
        }

        this.announce(`Unknown command: ${command}`);
    }

    /**
     * Provide feedback for a command result.
     * Sets aria-invalid when the command was not recognized.
     * @param {string} message - Feedback message
     * @private
     */
    announce(message) {
        this.commandInput.setAttribute('aria-invalid', message.startsWith('Unknown') ? 'true' : 'false');
        const statusEmitter = this.options.onStatus;
        if (typeof statusEmitter === 'function') {
            statusEmitter(message);
        }
    }
}
