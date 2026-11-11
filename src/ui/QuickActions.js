import { JMARS_CONFIG } from '../jmars-config.js';
import { jmarsState } from '../jmars-state.js';
import { normalizeBodyKey, switchActiveBody } from '../util/body.js';
import { to180 } from '../util/geo.js';
import { SearchBar } from './SearchBar.js';

/**
 * @module QuickActions
 * @description Unified JMARS Quick Command & Navigation Palette (`Ctrl+K`).
 * Supports fuzzy command search, direct coordinate fly-to (`lat, lon`),
 * cross-body planetary switching, saved user bookmarks, and IAU landmarks.
 * Built 100% with XSS-safe DOM APIs (`createElement` + `textContent`).
 */
export class QuickActions {
    /**
     * Create a new QuickActions command palette.
     * @param {string|null} containerId - Optional DOM id of an inline container element
     * @param {object} [options={}] - Handlers and data providers
     */
    constructor(containerId, options = {}) {
        this.container = containerId ? document.getElementById(containerId) : null;
        this.options = options;
        /** @type {HTMLInputElement|null} */
        this.commandInput = null;
        /** @type {HTMLDivElement|null} */
        this.backdrop = null;
        /** @type {HTMLInputElement|null} */
        this.modalInput = null;
        /** @type {HTMLDivElement|null} */
        this.resultsList = null;
        /** @type {boolean} */
        this.isOpen = false;
        /** @type {Array<object>} */
        this.currentItems = [];
        /** @type {number} */
        this.activeIndex = 0;

        if (this.container) {
            this.render();
            this.bindEvents();
        }
        this._buildModal();
    }

    /**
     * Build optional inline input if a container element was provided.
     * @private
     */
    render() {
        if (!this.container) return;
        this.container.textContent = '';

        const wrapper = document.createElement('div');
        wrapper.className = 'quick-actions-inline';
        wrapper.style.cssText = 'display:flex; gap:6px; align-items:center;';

        const input = document.createElement('input');
        input.type = 'text';
        input.id = 'quick-action-input';
        input.placeholder = 'Type a command or lat, lon...';
        input.setAttribute('aria-label', 'Quick command input');
        input.style.cssText = 'flex:1; padding:6px 8px; border-radius:6px; border:1px solid #334155; background:#0f172a; color:#f1f5f9; font-size:12px;';

        const runBtn = document.createElement('button');
        runBtn.type = 'button';
        runBtn.id = 'quick-action-run';
        runBtn.textContent = 'Run';
        runBtn.style.cssText = 'padding:6px 10px; border-radius:6px; border:1px solid #334155; background:#1e293b; color:#f1f5f9; font-size:12px; cursor:pointer;';

        wrapper.appendChild(input);
        wrapper.appendChild(runBtn);
        this.container.appendChild(wrapper);
        this.commandInput = input;
    }

    /**
     * Bind inline input events if present.
     * @private
     */
    bindEvents() {
        if (!this.container || !this.commandInput) return;
        const runBtn = this.container.querySelector('#quick-action-run');
        if (!runBtn) return;

        const runCommand = () => {
            const command = this.commandInput.value.trim();
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
     * Build the accessible modal Command Palette mounted to `document.body`.
     * @private
     */
    _buildModal() {
        if (typeof document === 'undefined' || !document.body) return;

        const existing = document.getElementById('quick-command-backdrop');
        if (existing) existing.remove();

        const backdrop = document.createElement('div');
        backdrop.id = 'quick-command-backdrop';
        backdrop.className = 'quick-cmd-backdrop';
        backdrop.hidden = true;

        const modal = document.createElement('div');
        modal.className = 'quick-cmd-modal';
        modal.setAttribute('role', 'dialog');
        modal.setAttribute('aria-modal', 'true');
        modal.setAttribute('aria-label', 'Quick Command and Navigation Palette');

        const header = document.createElement('div');
        header.className = 'quick-cmd-header';

        const searchIcon = document.createElement('span');
        searchIcon.className = 'quick-cmd-icon';
        searchIcon.setAttribute('aria-hidden', 'true');
        searchIcon.textContent = '\u2315';

        const input = document.createElement('input');
        input.type = 'text';
        input.id = 'quick-cmd-input';
        input.className = 'quick-cmd-input';
        input.placeholder = 'Jump to landmark, lat/lon, bookmark, body, or tool...';
        input.setAttribute('role', 'combobox');
        input.setAttribute('aria-autocomplete', 'list');
        input.setAttribute('aria-expanded', 'true');
        input.setAttribute('aria-controls', 'quick-cmd-results');
        input.autocomplete = 'off';

        const escBadge = document.createElement('button');
        escBadge.type = 'button';
        escBadge.className = 'quick-cmd-esc';
        escBadge.setAttribute('aria-label', 'Close command palette');
        escBadge.textContent = 'Esc';
        escBadge.addEventListener('click', () => this.close());

        header.appendChild(searchIcon);
        header.appendChild(input);
        header.appendChild(escBadge);

        const list = document.createElement('div');
        list.id = 'quick-cmd-results';
        list.className = 'quick-cmd-results custom-slim-scroll';
        list.setAttribute('role', 'listbox');

        const footer = document.createElement('div');
        footer.className = 'quick-cmd-footer';
        footer.textContent = '\u2191\u2193 Navigate   \u21b5 Select   Esc Close   Ctrl+K Toggle';

        modal.appendChild(header);
        modal.appendChild(list);
        modal.appendChild(footer);
        backdrop.appendChild(modal);
        document.body.appendChild(backdrop);

        backdrop.addEventListener('mousedown', (e) => {
            if (e.target === backdrop) {
                this.close();
            }
        });

        input.addEventListener('input', () => {
            this.activeIndex = 0;
            this._renderModalItems(input.value);
        });

        input.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                e.preventDefault();
                e.stopPropagation();
                this.close();
                return;
            }
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                if (this.currentItems.length > 0) {
                    this.activeIndex = (this.activeIndex + 1) % this.currentItems.length;
                    this._syncActiveSelection();
                }
                return;
            }
            if (e.key === 'ArrowUp') {
                e.preventDefault();
                if (this.currentItems.length > 0) {
                    this.activeIndex = (this.activeIndex - 1 + this.currentItems.length) % this.currentItems.length;
                    this._syncActiveSelection();
                }
                return;
            }
            if (e.key === 'Enter') {
                e.preventDefault();
                const chosen = this.currentItems[this.activeIndex] || this.currentItems[0];
                if (chosen) {
                    this._runItem(chosen);
                } else if (input.value.trim()) {
                    this.executeCommand(input.value.trim());
                }
            }
        });

        this.backdrop = backdrop;
        this.modalInput = input;
        this.resultsList = list;
    }

    /**
     * Open the modal Command Palette and focus the search input.
     * @param {string} [initialQuery=''] - Optional initial query text
     */
    open(initialQuery = '') {
        if (!this.backdrop || !this.modalInput) {
            this._buildModal();
        }
        if (!this.backdrop || !this.modalInput) return;

        this.isOpen = true;
        this.backdrop.hidden = false;
        this.modalInput.value = typeof initialQuery === 'string' ? initialQuery : '';
        this.activeIndex = 0;
        this._renderModalItems(this.modalInput.value);
        setTimeout(() => {
            this.modalInput?.focus();
            this.modalInput?.select();
        }, 0);
    }

    /**
     * Close the modal Command Palette.
     */
    close() {
        if (!this.backdrop) return;
        this.isOpen = false;
        this.backdrop.hidden = true;
    }

    /**
     * Toggle the modal Command Palette open or closed.
     */
    toggle() {
        if (this.isOpen) {
            this.close();
        } else {
            this.open('');
        }
    }

    /**
     * Build the catalog of built-in commands wired via `this.options`.
     * @returns {Array<{id: string, label: string, category: string, keywords: string, shortcut?: string, run: Function}>}
     */
    getCommands() {
        const o = this.options || {};
        const cmds = [];

        if (o.onSwitchBody || o.jmarsMap) {
            const switchTo = (bodyKey, label, group = '') => ({
                id: `body-${bodyKey}`,
                label: `Switch Body: ${label}`,
                category: 'Body',
                meta: group || undefined,
                keywords: `planet moon asteroid body switch ${bodyKey} ${label.toLowerCase()} ${group.toLowerCase()}`,
                run: async () => {
                    if (o.onSwitchBody) {
                        await o.onSwitchBody(bodyKey);
                    } else if (o.jmarsMap) {
                        await Promise.resolve(switchActiveBody(o.jmarsMap, bodyKey));
                    }
                }
            });
            const cfgBodies = JMARS_CONFIG?.bodies || {};
            Object.entries(cfgBodies).forEach(([key, bodyCfg]) => {
                if (!bodyCfg) return;
                const displayLabel = key === 'moon' ? 'Moon (Luna)' : (bodyCfg.name || key);
                cmds.push(switchTo(key, displayLabel, bodyCfg.group || ''));
            });
        }

        if (o.onSetViewMode) {
            cmds.push(
                {
                    id: 'view-2d',
                    label: 'View Mode: 2D Equirectangular Map',
                    category: 'View',
                    keywords: '2d flat map leaflet mode',
                    run: () => o.onSetViewMode('2d')
                },
                {
                    id: 'view-3d',
                    label: 'View Mode: 3D Planetary Globe (Cesium)',
                    category: 'View',
                    keywords: '3d globe sphere cesium ellipsoid',
                    run: () => o.onSetViewMode('3d')
                }
            );
        }

        if (o.onRunTool) {
            const addTool = (id, label, keywords) => cmds.push({
                id: `tool-${id}`,
                label,
                category: 'Tool',
                keywords,
                run: () => o.onRunTool(id)
            });
            addTool('measure-distance', 'Measure Geodesic Distance & Azimuth', 'measure distance length azimuth bearing line ruler');
            addTool('measure-area', 'Measure Geodesic Polygon Area & Perimeter', 'measure area polygon perimeter surface');
            addTool('profile-linear', 'Draw Topographic Elevation Profile', 'elevation profile cross section topography mola lola');
            addTool('profile-radial', 'Radial Topographic Profile', 'radial profile crater rays elevation');
            addTool('investigate', 'Investigate Point (Layer Pixel / Value Query)', 'investigate point inspect pixel value wms');
            addTool('crater', 'Crater Counting & CSFD Isochron Tool', 'crater count csfd hartmann neukum age');
            addTool('shapes', 'Shapes, Geologic Patterns & Shapefile/WKT I/O', 'shapes draw polygon geologic pattern wkt shapefile geojson buffer');
            addTool('stamps', 'Spacecraft Stamp Footprints (ODE)', 'stamps footprints hirise ctx themis lroc');
            addTool('krc', 'KRC 1D Thermal Conduction Model', 'krc thermal temperature inertia diurnal model');
            addTool('mcd', 'Mars Climate Database (MCD) Atmospheric Profiler', 'mcd atmosphere pressure density wind mars climate');
            addTool('radar', 'Subsurface Radar Sounder Model (SHARAD / REASON)', 'radar sounder sharad marsis reason subsurface ice');
            addTool('lunar-site', 'Lunar Base Site Planner (South Pole 20m LOLA)', 'lunar base site planner artemis shackleton south pole cad');
        }

        if (o.onOpenLayers) {
            cmds.push({
                id: 'open-layers',
                label: 'Open Layer Manager',
                category: 'Workspace',
                keywords: 'layers basemap overlay opacity visibility order',
                run: () => o.onOpenLayers()
            });
        }

        if (o.onOpenTools) {
            cmds.push({
                id: 'open-tools',
                label: 'Open Science & Cartography Tools',
                category: 'Workspace',
                keywords: 'tools panel sidebar',
                run: () => o.onOpenTools()
            });
        }

        if (o.onSaveSession) {
            cmds.push({
                id: 'save-session',
                label: 'Save Session (.json)',
                category: 'Session',
                keywords: 'save export session workspace json',
                shortcut: 'Ctrl+S',
                run: () => o.onSaveSession()
            });
        }

        if (o.onLoadSession) {
            cmds.push({
                id: 'load-session',
                label: 'Load Session (.json)',
                category: 'Session',
                keywords: 'load open import restore session json',
                shortcut: 'Ctrl+O',
                run: () => o.onLoadSession()
            });
        }

        if (o.onShareLink) {
            cmds.push({
                id: 'share-link',
                label: 'Copy Permalink / Share Current View URL',
                category: 'Session',
                keywords: 'share link url copy permalink',
                run: () => o.onShareLink()
            });
        }

        if (o.onResetView) {
            cmds.push({
                id: 'reset-view',
                label: 'Reset Map View to Planetary Global Extent',
                category: 'Navigation',
                keywords: 'reset home global view extent zoom',
                shortcut: 'R',
                run: () => o.onResetView()
            });
        }

        if (o.onToggleSidebar) {
            cmds.push({
                id: 'toggle-sidebar',
                label: 'Toggle Sidebar Panel',
                category: 'Workspace',
                keywords: 'sidebar collapse expand toggle panel',
                run: () => o.onToggleSidebar()
            });
        }

        if (Array.isArray(o.customCommands)) {
            cmds.push(...o.customCommands);
        }

        return cmds;
    }

    /**
     * Filter and rank palette items for the given query string.
     * @param {string} query
     * @returns {Array<object>}
     */
    buildMatches(query = '') {
        const q = (query || '').trim();
        const qLower = q.toLowerCase();
        const currentBody = normalizeBodyKey(jmarsState.get('body') || 'mars');
        const items = [];

        // 1. Direct Coordinate Navigation
        const coordHit = SearchBar.parseCoordinateQuery(q);
        if (coordHit) {
            items.push({
                id: `coord-${coordHit.lat}-${coordHit.lon}`,
                label: coordHit.name,
                category: 'Coordinates',
                meta: `${currentBody.toUpperCase()}`,
                run: () => this._flyToLatLon(coordHit.lat, coordHit.lon)
            });
        }

        // 2. Commands
        const commands = this.getCommands();
        if (!qLower) {
            const bodyCmds = commands.filter((c) => c.category === 'Body').slice(0, 5);
            const otherCmds = commands.filter((c) => c.category !== 'Body').slice(0, 9);
            items.push(...bodyCmds, ...otherCmds);
        } else {
            const qTokens = qLower.split(/\s+/).filter(Boolean);
            commands.forEach((cmd) => {
                const hay = `${cmd.label} ${cmd.category} ${cmd.keywords || ''}`.toLowerCase();
                if (qTokens.every((t) => hay.includes(t))) {
                    items.push(cmd);
                }
            });
        }

        // 3. Saved User Bookmarks
        const bookmarks = typeof this.options.getBookmarks === 'function'
            ? (this.options.getBookmarks() || [])
            : [];
        if (Array.isArray(bookmarks) && bookmarks.length > 0) {
            const matchedBookmarks = !qLower
                ? bookmarks.slice(0, 4)
                : bookmarks.filter((bm) => {
                    const hay = `${bm.name || ''} ${bm.body || ''}`.toLowerCase();
                    return hay.includes(qLower);
                }).slice(0, 6);

            matchedBookmarks.forEach((bm) => {
                const bmBody = normalizeBodyKey(bm.body || currentBody);
                items.push({
                    id: `bm-${bm.id || bm.name}`,
                    label: bm.name || 'Unnamed Bookmark',
                    category: 'Bookmark',
                    meta: `${bmBody.toUpperCase()} · ${Number(bm.lat || 0).toFixed(1)}°, ${Number(bm.lng ?? bm.lon ?? 0).toFixed(1)}°`,
                    run: () => this.options.onSelectBookmark?.(bm)
                });
            });
        }

        // 4. Planetary Landmarks (IAU Gazetteer)
        const landmarks = typeof this.options.getLandmarks === 'function'
            ? (this.options.getLandmarks() || [])
            : [];
        if (qLower.length >= 2 && Array.isArray(landmarks) && landmarks.length > 0) {
            const ranked = SearchBar.rankLandmarks(landmarks, q, currentBody).slice(0, 8);
            ranked.forEach((lm, idx) => {
                const lmBody = normalizeBodyKey(lm.body || 'mars');
                items.push({
                    id: `lm-${lmBody}-${idx}-${lm.name}`,
                    label: lm.name,
                    category: lm.type || 'Landmark',
                    meta: `${lmBody.toUpperCase()} · ${Number(lm.lat).toFixed(1)}°, ${Number(lm.lon).toFixed(1)}°`,
                    run: () => {
                        if (this.options.onSelectLandmark) {
                            return this.options.onSelectLandmark(lm);
                        }
                        return this._flyToLatLon(lm.lat, lm.lon, lmBody);
                    }
                });
            });
        }

        return items.slice(0, 18);
    }

    /**
     * Render filtered items into the modal listbox using safe DOM APIs.
     * @param {string} query
     * @private
     */
    _renderModalItems(query) {
        if (!this.resultsList) return;
        this.resultsList.textContent = '';

        this.currentItems = this.buildMatches(query);
        if (this.activeIndex >= this.currentItems.length) {
            this.activeIndex = 0;
        }

        if (this.currentItems.length === 0) {
            const empty = document.createElement('div');
            empty.className = 'quick-cmd-empty';
            empty.textContent = 'No matching commands, landmarks, or coordinates (try "Olympus Mons", "18.65, -133.8", or "Moon").';
            this.resultsList.appendChild(empty);
            return;
        }

        this.currentItems.forEach((item, idx) => {
            const row = document.createElement('div');
            row.className = 'quick-cmd-item' + (idx === this.activeIndex ? ' active' : '');
            row.setAttribute('role', 'option');
            row.setAttribute('aria-selected', idx === this.activeIndex ? 'true' : 'false');

            const left = document.createElement('div');
            left.className = 'quick-cmd-item-left';

            const catBadge = document.createElement('span');
            catBadge.className = 'quick-cmd-badge';
            catBadge.textContent = item.category || 'Action';

            const title = document.createElement('span');
            title.className = 'quick-cmd-label';
            title.textContent = item.label || '';

            left.appendChild(catBadge);
            left.appendChild(title);

            const right = document.createElement('div');
            right.className = 'quick-cmd-item-right';

            if (item.meta) {
                const meta = document.createElement('span');
                meta.className = 'quick-cmd-meta';
                meta.textContent = item.meta;
                right.appendChild(meta);
            }
            if (item.shortcut) {
                const kbd = document.createElement('kbd');
                kbd.className = 'quick-cmd-kbd';
                kbd.textContent = item.shortcut;
                right.appendChild(kbd);
            }

            row.appendChild(left);
            row.appendChild(right);

            row.addEventListener('mouseenter', () => {
                this.activeIndex = idx;
                this._syncActiveSelection();
            });
            row.addEventListener('click', () => {
                this._runItem(item);
            });

            this.resultsList.appendChild(row);
        });
    }

    /**
     * Synchronize visual highlight and ARIA state on listbox rows.
     * @private
     */
    _syncActiveSelection() {
        if (!this.resultsList) return;
        const rows = this.resultsList.querySelectorAll('.quick-cmd-item');
        rows.forEach((row, idx) => {
            const active = idx === this.activeIndex;
            row.classList.toggle('active', active);
            row.setAttribute('aria-selected', active ? 'true' : 'false');
            if (active && typeof row.scrollIntoView === 'function') {
                row.scrollIntoView({ block: 'nearest' });
            }
        });
    }

    /**
     * Execute a selected palette item and close the modal.
     * @param {object} item
     * @private
     */
    async _runItem(item) {
        this.close();
        try {
            await item.run?.();
            this.announce(`${item.label}`);
        } catch (err) {
            console.error('QuickActions command error:', err);
        }
    }

    /**
     * Navigate the map to a coordinate (switching body first if requested).
     * @private
     */
    async _flyToLatLon(lat, lon, bodyKey = null) {
        const jmarsMap = this.options.jmarsMap;
        const map = jmarsMap?.map || this.options.map;
        if (bodyKey && jmarsMap) {
            const current = normalizeBodyKey(jmarsState.get('body') || 'mars');
            const target = normalizeBodyKey(bodyKey);
            if (target !== current) {
                await Promise.resolve(switchActiveBody(jmarsMap, target));
            }
        }
        if (map && typeof map.setView === 'function') {
            const currentZoom = typeof map.getZoom === 'function' ? map.getZoom() : 4;
            map.setView([lat, to180(lon)], Math.max(currentZoom, 6));
        }
    }

    /**
     * Execute a text command by matching against coordinates, known keywords, or fuzzy items.
     * Preserves backward-compatible string command execution.
     * @param {string} command - Command string
     */
    executeCommand(command) {
        if (!command || typeof command !== 'string') return;
        const trimmed = command.trim();
        const normalized = trimmed.toLowerCase().replace(/\s+/g, ' ');

        const coordHit = SearchBar.parseCoordinateQuery(trimmed);
        if (coordHit) {
            this._flyToLatLon(coordHit.lat, coordHit.lon);
            this.announce(coordHit.name);
            return;
        }

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

        const matches = this.buildMatches(trimmed);
        if (matches.length > 0) {
            this._runItem(matches[0]);
            return;
        }

        this.announce(`Unknown command: ${command}`);
    }

    /**
     * Provide feedback for a command result (safe when `commandInput` is null).
     * @param {string} message - Feedback message
     */
    announce(message) {
        if (this.commandInput) {
            this.commandInput.setAttribute('aria-invalid', message.startsWith('Unknown') ? 'true' : 'false');
        }
        const statusEmitter = this.options.onStatus;
        if (typeof statusEmitter === 'function') {
            statusEmitter(message);
        }
    }
}
