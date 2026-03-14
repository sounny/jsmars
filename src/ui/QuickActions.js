export class QuickActions {
    constructor(containerId, options = {}) {
        this.container = document.getElementById(containerId);
        this.options = options;
        this.commandInput = null;

        if (!this.container) return;
        this.render();
        this.bindEvents();
    }

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

        document.addEventListener('keydown', (event) => {
            const isShortcut = (event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k';
            if (!isShortcut) return;
            event.preventDefault();
            this.commandInput.focus();
            this.commandInput.select();
        });
    }

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

    announce(message) {
        this.commandInput.setAttribute('aria-invalid', message.startsWith('Unknown') ? 'true' : 'false');
        const statusEmitter = this.options.onStatus;
        if (typeof statusEmitter === 'function') {
            statusEmitter(message);
        }
    }
}
