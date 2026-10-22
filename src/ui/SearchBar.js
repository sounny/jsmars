import { jmarsState } from '../jmars-state.js';
import { normalizeBodyKey, switchActiveBody } from '../util/body.js';
import { to180 } from '../util/geo.js';

/**
 * @module SearchBar
 * @description Provides a unified, body-aware search input for planetary landmarks
 * and direct coordinate navigation, with typo-tolerant fuzzy matching, keyboard
 * navigation, and automatic cross-body switching.
 */
export class SearchBar {
    /**
     * Create a new SearchBar.
     * @param {L.Map|Object} mapOrController - Leaflet map instance or JMARSMap controller
     * @param {string} containerId - DOM id of the search bar container
     */
    constructor(mapOrController, containerId) {
        this.jmarsMap = mapOrController?.map ? mapOrController : null;
        this.map = this.jmarsMap?.map || mapOrController;
        this.container = document.getElementById(containerId);
        /** @type {Array<{name: string, lat: number, lon: number, body?: string, type?: string, origin?: string}>} */
        this.landmarks = [];
        /** @type {HTMLDivElement|null} */
        this.resultsContainer = null;
        /** @type {number|null} Debounce timer id */
        this._debounceTimer = null;
        /** @type {string} Current active planetary body */
        this.currentBody = normalizeBodyKey(jmarsState.get('body') || 'mars');
        /** @type {Array<Object>} Currently displayed search result items */
        this.currentMatches = [];
        /** @type {number} Active keyboard selection index */
        this.activeIndex = -1;

        if (!this.container) {
            console.error(`SearchBar container '${containerId}' not found.`);
            return;
        }

        // Register the global click-outside listener ONCE in the constructor,
        // not inside render(), to avoid duplicate listeners on re-render.
        this._boundOutsideClick = (e) => {
            if (this.resultsContainer && !this.container.contains(e.target)) {
                this.resultsContainer.style.display = 'none';
                this.activeIndex = -1;
            }
        };
        document.addEventListener('click', this._boundOutsideClick);

        this.init();
    }

    /**
     * Parse a direct coordinate query such as "18.65, 226.2" or "-89.54 197.2" or "18.65N, 133.8W".
     * @param {string} query
     * @returns {{name: string, lat: number, lon: number, isCoordinate: boolean}|null}
     */
    static parseCoordinateQuery(query) {
        if (!query || typeof query !== 'string') return null;
        const m = query.trim().match(/^(-?\d+(?:\.\d+)?)\s*°?\s*([NS])?[\s,;]+(-?\d+(?:\.\d+)?)\s*°?\s*([EW])?$/i);
        if (!m) return null;
        let lat = parseFloat(m[1]);
        let lon = parseFloat(m[3]);
        if (!Number.isFinite(lat) || !Number.isFinite(lon)) return null;
        if (m[2] && m[2].toUpperCase() === 'S') lat = -Math.abs(lat);
        if (m[2] && m[2].toUpperCase() === 'N') lat = Math.abs(lat);
        if (m[4] && m[4].toUpperCase() === 'W') lon = -Math.abs(lon);
        if (m[4] && m[4].toUpperCase() === 'E') lon = Math.abs(lon);
        if (lat < -90 || lat > 90 || lon < -180 || lon > 360) return null;
        if (lon > 180) lon -= 360;
        return {
            name: `Go to ${lat.toFixed(4)}°, ${lon.toFixed(4)}°`,
            lat,
            lon,
            isCoordinate: true
        };
    }

    /**
     * Bounded Levenshtein edit distance between two lowercase strings.
     * @param {string} a
     * @param {string} b
     * @returns {number}
     */
    static levenshteinDistance(a, b) {
        if (a === b) return 0;
        const m = a.length, n = b.length;
        if (!m) return n;
        if (!n) return m;
        if (Math.abs(m - n) > 3) return Math.abs(m - n);
        const prev = new Array(n + 1);
        const curr = new Array(n + 1);
        for (let j = 0; j <= n; j++) prev[j] = j;
        for (let i = 1; i <= m; i++) {
            curr[0] = i;
            for (let j = 1; j <= n; j++) {
                const cost = a[i - 1] === b[j - 1] ? 0 : 1;
                curr[j] = Math.min(prev[j] + 1, curr[j - 1] + 1, prev[j - 1] + cost);
            }
            for (let j = 0; j <= n; j++) prev[j] = curr[j];
        }
        return prev[n];
    }

    /**
     * Rank and filter landmarks against a search query, prioritizing current-body features
     * and supporting typo-tolerant fuzzy matching.
     * @param {Array<Object>} landmarks
     * @param {string} query
     * @param {string} [currentBody='mars']
     * @returns {Array<Object>}
     */
    static rankLandmarks(landmarks, query, currentBody = 'mars') {
        if (!query || !query.trim() || !Array.isArray(landmarks)) return [];
        const q = query.trim().toLowerCase();
        const qTokens = q.split(/\s+/).filter(Boolean);
        const activeBody = normalizeBodyKey(currentBody || 'mars');
        const scored = [];

        for (const l of landmarks) {
            const name = (l.name || '').toLowerCase();
            const type = (l.type || '').toLowerCase();
            const origin = (l.origin || '').toLowerCase();
            const body = normalizeBodyKey(l.body || 'mars');
            const nameWords = name.split(/[\s\-()/]+/).filter(Boolean);

            let score = 0;
            if (name === q) {
                score = 100;
            } else if (name.startsWith(q)) {
                score = 85;
            } else if (name.includes(q)) {
                score = 70;
            } else if (qTokens.length > 1 && qTokens.every(t => name.includes(t) || type.includes(t) || origin.includes(t))) {
                score = 60;
            } else if (type.includes(q) || origin.includes(q)) {
                score = 45;
            } else if (q.length >= 3) {
                // Typo-tolerant fuzzy match against individual words in the landmark name
                const maxDist = q.length >= 6 ? 2 : 1;
                const fuzzyHit = qTokens.every(qt => {
                    if (qt.length < 3) return name.includes(qt);
                    return nameWords.some(w => {
                        if (w.startsWith(qt) || qt.startsWith(w)) return true;
                        const prefix = w.slice(0, Math.min(w.length, qt.length + 1));
                        return SearchBar.levenshteinDistance(qt, w) <= maxDist ||
                               SearchBar.levenshteinDistance(qt, prefix) <= 1;
                    });
                });
                if (fuzzyHit) score = 35;
            }

            if (score > 0) {
                if (body === activeBody) score += 20;
                scored.push({ landmark: l, score });
            }
        }

        scored.sort((a, b) => b.score - a.score || (a.landmark.name || '').localeCompare(b.landmark.name || ''));
        return scored.map(s => s.landmark);
    }

    /**
     * Initialize the search bar: load data then render.
     * @private
     */
    async init() {
        await this.loadLandmarks();
        this.render();
    }

    /**
     * Fetch landmark data from the local JSON file.
     * @private
     */
    async loadLandmarks() {
        try {
            const response = await fetch('./src/data/landmarks.json');
            if (!response.ok) throw new Error('Failed to load landmarks');
            this.landmarks = await response.json();
        } catch (error) {
            console.error('Error loading landmarks:', error);
            this.landmarks = [];
        }
    }

    /**
     * Build the search input and results dropdown DOM.
     * Does NOT register a global click listener (that is done once in the constructor).
     * @private
     */
    render() {
        this.container.innerHTML = '';
        this.container.className = 'modern-search-wrapper';

        // Search Input Box
        const box = document.createElement('div');
        box.className = 'modern-search-input-box';

        // Search Icon (Monoline SVG)
        const icon = document.createElement('span');
        icon.className = 'modern-search-icon';
        icon.innerHTML = `
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
        `;

        // Input
        const input = document.createElement('input');
        input.type = 'text';
        input.className = 'modern-search-input';
        const capInitial = this.currentBody.charAt(0).toUpperCase() + this.currentBody.slice(1);
        input.placeholder = `Search ${capInitial} landmarks or lat, lon...`;
        input.setAttribute('aria-label', 'Search planetary landmarks or coordinates');
        input.setAttribute('role', 'combobox');
        input.setAttribute('aria-autocomplete', 'list');
        input.setAttribute('aria-expanded', 'false');
        this.input = input;

        // Clear Button
        const clearBtn = document.createElement('button');
        clearBtn.type = 'button';
        clearBtn.className = 'modern-search-clear-btn';
        clearBtn.setAttribute('aria-label', 'Clear search');
        clearBtn.innerHTML = `
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
        `;
        clearBtn.addEventListener('click', () => {
            input.value = '';
            clearBtn.style.display = 'none';
            if (this.resultsContainer) this.resultsContainer.style.display = 'none';
            input.setAttribute('aria-expanded', 'false');
            this.activeIndex = -1;
            input.focus();
        });

        // Debounced input handler (150ms)
        input.addEventListener('input', (e) => {
            const val = e.target.value;
            clearBtn.style.display = val ? 'flex' : 'none';
            clearTimeout(this._debounceTimer);
            this._debounceTimer = setTimeout(() => this.handleInput(val), 150);
        });
        input.addEventListener('focus', (e) => this.handleInput(e.target.value));

        // Keyboard navigation (ArrowDown, ArrowUp, Enter, Escape)
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                if (this.resultsContainer) this.resultsContainer.style.display = 'none';
                input.setAttribute('aria-expanded', 'false');
                this.activeIndex = -1;
                return;
            }
            if (!this.resultsContainer || this.resultsContainer.style.display === 'none' || !this.currentMatches.length) {
                if (e.key === 'Enter' && input.value.trim()) {
                    this.handleInput(input.value);
                    if (this.currentMatches.length > 0) {
                        e.preventDefault();
                        this.selectLandmark(this.currentMatches[0]);
                    }
                }
                return;
            }
            if (e.key === 'ArrowDown') {
                e.preventDefault();
                this.activeIndex = (this.activeIndex + 1) % this.currentMatches.length;
                this._updateActiveOption();
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                this.activeIndex = (this.activeIndex - 1 + this.currentMatches.length) % this.currentMatches.length;
                this._updateActiveOption();
            } else if (e.key === 'Enter') {
                e.preventDefault();
                const target = this.currentMatches[this.activeIndex >= 0 ? this.activeIndex : 0];
                if (target) this.selectLandmark(target);
            }
        });

        box.appendChild(icon);
        box.appendChild(input);
        box.appendChild(clearBtn);

        // Results Dropdown
        this.resultsContainer = document.createElement('div');
        this.resultsContainer.className = 'modern-search-results custom-slim-scroll';
        this.resultsContainer.setAttribute('role', 'listbox');

        this.container.appendChild(box);
        this.container.appendChild(this.resultsContainer);

        // Update placeholder on body change
        document.addEventListener('jmars:body-changed', (e) => {
            const body = normalizeBodyKey(e?.detail?.body || 'mars');
            this.currentBody = body;
            const capBody = body.charAt(0).toUpperCase() + body.slice(1);
            input.placeholder = `Search ${capBody} landmarks or lat, lon...`;
        });
    }

    /**
     * Highlight the currently focused option in the dropdown.
     * @private
     */
    _updateActiveOption() {
        if (!this.resultsContainer) return;
        const items = this.resultsContainer.querySelectorAll('.modern-search-item');
        items.forEach((el, idx) => {
            const active = idx === this.activeIndex;
            el.setAttribute('aria-selected', active ? 'true' : 'false');
            el.style.background = active ? 'rgba(56, 189, 248, 0.18)' : '';
            if (active && el.scrollIntoView) el.scrollIntoView({ block: 'nearest' });
        });
    }

    /**
     * Filter landmarks by query and render matching results.
     * @param {string} query - Current input value
     */
    handleInput(query) {
        if (!query || query.trim() === '') {
            this.resultsContainer.style.display = 'none';
            this.input?.setAttribute('aria-expanded', 'false');
            this.currentMatches = [];
            this.activeIndex = -1;
            return;
        }

        const coordHit = SearchBar.parseCoordinateQuery(query);
        const ranked = SearchBar.rankLandmarks(this.landmarks, query, this.currentBody);
        const matches = coordHit ? [coordHit, ...ranked] : ranked;

        this.renderResults(matches);
    }

    /**
     * Render the filtered results dropdown.
     * @param {Array<{name: string, lat: number, lon: number, body?: string, isCoordinate?: boolean}>} matches - Matching landmarks
     * @private
     */
    renderResults(matches) {
        this.resultsContainer.innerHTML = '';
        this.currentMatches = matches.slice(0, 15);
        this.activeIndex = -1;

        if (this.currentMatches.length === 0) {
            const noRes = document.createElement('div');
            noRes.textContent = 'No matching landmarks';
            noRes.style.padding = '10px';
            noRes.style.color = '#64748b';
            noRes.style.fontSize = '11px';
            noRes.style.fontStyle = 'italic';
            noRes.style.textAlign = 'center';
            this.resultsContainer.appendChild(noRes);
        } else {
            this.currentMatches.forEach((match, idx) => {
                const item = document.createElement('div');
                item.className = 'modern-search-item';
                item.setAttribute('role', 'option');
                item.setAttribute('aria-selected', 'false');

                const nameSpan = document.createElement('span');
                nameSpan.textContent = match.name;
                nameSpan.style.fontWeight = '500';

                const rightGroup = document.createElement('span');
                rightGroup.style.display = 'inline-flex';
                rightGroup.style.alignItems = 'center';
                rightGroup.style.gap = '6px';

                if (!match.isCoordinate && match.body) {
                    const badge = document.createElement('span');
                    const bKey = normalizeBodyKey(match.body);
                    badge.textContent = bKey.toUpperCase();
                    badge.style.fontSize = '9px';
                    badge.style.fontFamily = 'monospace';
                    badge.style.padding = '1px 4px';
                    badge.style.borderRadius = '3px';
                    badge.style.background = bKey === this.currentBody ? 'rgba(56, 189, 248, 0.16)' : 'rgba(148, 163, 184, 0.15)';
                    badge.style.color = bKey === this.currentBody ? '#38bdf8' : '#94a3b8';
                    rightGroup.appendChild(badge);
                }

                const coordsSpan = document.createElement('span');
                coordsSpan.style.fontFamily = 'monospace';
                coordsSpan.style.fontSize = '9px';
                coordsSpan.style.color = '#94a3b8';
                coordsSpan.textContent = `${match.lat.toFixed(1)}°, ${match.lon.toFixed(1)}°`;
                rightGroup.appendChild(coordsSpan);

                item.appendChild(nameSpan);
                item.appendChild(rightGroup);

                item.addEventListener('click', () => {
                    this.selectLandmark(match);
                });
                item.addEventListener('mouseenter', () => {
                    this.activeIndex = idx;
                    this._updateActiveOption();
                });

                this.resultsContainer.appendChild(item);
            });
        }

        this.resultsContainer.style.display = 'block';
        this.input?.setAttribute('aria-expanded', 'true');
    }

    /**
     * Pan the map to the selected landmark (switching planetary body first if needed).
     * Normalizes longitude from 0-360 to -180/180 if needed.
     * @param {{name: string, lat: number, lon: number, body?: string, isCoordinate?: boolean}} landmark - Selected landmark
     */
    async selectLandmark(landmark) {
        if (!landmark || !this.map) return;

        if (!landmark.isCoordinate && landmark.body) {
            const targetBody = normalizeBodyKey(landmark.body);
            if (targetBody !== this.currentBody) {
                if (this.jmarsMap) {
                    await Promise.resolve(switchActiveBody(this.jmarsMap, targetBody));
                } else {
                    this.currentBody = targetBody;
                    jmarsState.set('body', targetBody);
                }
            }
        }

        const targetLon = to180(landmark.lon);
        this.map.setView([landmark.lat, targetLon], Math.max(this.map.getZoom(), 6));
        this.resultsContainer.style.display = 'none';
        this.input?.setAttribute('aria-expanded', 'false');
        this.activeIndex = -1;
        if (this.input) this.input.value = landmark.isCoordinate ? `${landmark.lat}, ${landmark.lon}` : landmark.name;
    }
}
