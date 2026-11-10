import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';
import { normalizeBodyKey, switchActiveBody } from '../util/body.js';

/**
 * @module SessionManager
 * @description Manages saving and loading jsMars sessions to/from JSON files.
 * Serializes the application state (layers, view, body), crater data,
 * measurement data, bookmarks, and vector shapes (including geologic patterns
 * and custom attributes). On load, restores body first, then layers, view, and tools.
 */
export class SessionManager {
    /**
     * Create a new SessionManager.
     * @param {object|null} jmarsMap - JMARSMap controller instance (or null)
     * @param {object|null} craterLayer - CraterCounter instance (or null)
     * @param {object|null} measureTool - MeasureTool instance (or null)
     * @param {object|null} bookmarksTool - BookmarksTool instance (or null)
     * @param {object|null} [shapeLayer=null] - ShapeLayer instance (or null)
     */
    constructor(jmarsMap, craterLayer, measureTool, bookmarksTool, shapeLayer = null) {
        this.jmarsMap = jmarsMap;
        this.craterLayer = craterLayer;
        this.measureTool = measureTool;
        this.bookmarksTool = bookmarksTool;
        this.shapeLayer = shapeLayer;
    }

    /**
     * Build a serializable snapshot of the current session without triggering a download.
     * @returns {object} Session payload
     */
    buildSessionObject() {
        this.jmarsMap?.syncViewState?.({ updateUrl: true });
        const liveState = JSON.parse(JSON.stringify(jmarsState.state));
        liveState.body = normalizeBodyKey(liveState.body);

        return {
            version: '1.1',
            timestamp: new Date().toISOString(),
            state: liveState,
            craters: this.craterLayer ? this.craterLayer.getData() : [],
            measurements: this.measureTool ? this.measureTool.getData() : [],
            bookmarks: this.bookmarksTool ? this.bookmarksTool.getData() : [],
            shapes: this.shapeLayer ? this.shapeLayer.toGeoJSON() : { type: 'FeatureCollection', features: [] }
        };
    }

    /**
     * Save the current session to a downloadable JSON file.
     * Deep-clones state before serialization to avoid capturing live references.
     */
    saveSession() {
        const session = this.buildSessionObject();
        const content = JSON.stringify(session, null, 2);
        this.downloadFile(`jsmars_session_${Date.now()}.json`, content);
        this.notifyStatus('Session saved.');
    }

    /**
     * Apply a parsed session object to the application state, map, and tools.
     * Restores planetary body FIRST so body-switch does not overwrite restored layers/view.
     * @param {object} session - Parsed session object
     * @returns {Promise<void>}
     */
    async applySessionObject(session) {
        if (!session || typeof session !== 'object') {
            throw new Error('Invalid session payload.');
        }

        if (!session.version) {
            console.warn('Session file missing version. Trying best effort.');
        }

        // 1. Restore Planetary Body FIRST so switchBody does not overwrite restored layers
        if (session.state && session.state.body) {
            const targetBody = normalizeBodyKey(session.state.body);
            if (this.jmarsMap) {
                await Promise.resolve(switchActiveBody(this.jmarsMap, targetBody));
            } else {
                jmarsState.set('body', targetBody);
            }
        }

        // 2. Restore Active Layers & Layer Properties after body is established
        if (session.state && Array.isArray(session.state.activeLayers)) {
            jmarsState.setActiveLayers(session.state.activeLayers);
        }

        // 3. Restore View (Lat/Lon/Zoom)
        if (session.state && session.state.view) {
            jmarsState.set('view', session.state.view);
            if (typeof document !== 'undefined') {
                document.dispatchEvent(new CustomEvent(EVENTS.UPDATE_VIEW, {
                    detail: session.state.view
                }));
            }
        }

        // 4. Restore Tools & Vector Shapes
        if (session.craters && this.craterLayer) {
            this.craterLayer.loadData(session.craters);
        }

        if (session.measurements && this.measureTool) {
            this.measureTool.loadData(session.measurements);
        }

        if (session.bookmarks && this.bookmarksTool) {
            this.bookmarksTool.loadData(session.bookmarks);
        }

        if (session.shapes && this.shapeLayer) {
            if (typeof this.shapeLayer.clearAll === 'function') {
                this.shapeLayer.clearAll();
            }
            if (typeof this.shapeLayer.fromGeoJSON === 'function') {
                this.shapeLayer.fromGeoJSON(session.shapes);
            }
        }
    }

    /**
     * Load a session from a JSON File object.
     * @param {File} file - JSON session file chosen by the user
     * @returns {Promise<void>}
     */
    async loadSession(file) {
        try {
            const text = await file.text();
            const session = JSON.parse(text);
            await this.applySessionObject(session);
            this.notifyStatus('Session loaded successfully.');
        } catch (e) {
            console.error(e);
            this.notifyStatus('Error loading session: ' + (e?.message || 'Invalid JSON'), true);
        }
    }

    /**
     * Display a non-blocking, accessible toast notification for session events.
     * @param {string} message - Status message
     * @param {boolean} [isError=false] - Whether this represents an error
     */
    notifyStatus(message, isError = false) {
        if (typeof document === 'undefined' || !document.body) return;
        let toast = document.getElementById('jmars-session-toast');
        if (!toast) {
            toast = document.createElement('div');
            toast.id = 'jmars-session-toast';
            toast.setAttribute('role', 'status');
            toast.setAttribute('aria-live', 'polite');
            toast.style.cssText = [
                'position:fixed',
                'bottom:38px',
                'left:50%',
                'transform:translateX(-50%)',
                'z-index:5500',
                'padding:8px 16px',
                'border-radius:8px',
                'font-size:12px',
                'font-weight:500',
                'box-shadow:0 8px 24px rgba(0,0,0,0.45)',
                'pointer-events:none',
                'transition:opacity 0.25s ease'
            ].join(';');
            document.body.appendChild(toast);
        }
        toast.style.background = isError ? 'rgba(127, 29, 29, 0.96)' : 'rgba(15, 23, 42, 0.96)';
        toast.style.border = isError ? '1px solid #f87171' : '1px solid #38bdf8';
        toast.style.color = isError ? '#fecaca' : '#f1f5f9';
        toast.textContent = message;
        toast.style.opacity = '1';
        clearTimeout(this._toastTimer);
        this._toastTimer = setTimeout(() => {
            if (toast) toast.style.opacity = '0';
        }, 3200);
    }

    /**
     * Trigger a browser download for the given content.
     * @param {string} filename - Name for the downloaded file
     * @param {string} content - File content string
     * @private
     */
    downloadFile(filename, content) {
        const element = document.createElement('a');
        element.setAttribute('href', 'data:application/json;charset=utf-8,' + encodeURIComponent(content));
        element.setAttribute('download', filename);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    }
}
