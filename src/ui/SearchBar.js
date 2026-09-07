/**
 * @module SearchBar
 * @description Provides a search input for Mars landmarks.
 * Loads landmark data from a local JSON file and shows
 * a filtered dropdown as the user types. Selecting a
 * result pans the map to that landmark.
 */
export class SearchBar {
    /**
     * Create a new SearchBar.
     * @param {L.Map} map - Leaflet map instance
     * @param {string} containerId - DOM id of the search bar container
     */
    constructor(map, containerId) {
        this.map = map;
        this.container = document.getElementById(containerId);
        /** @type {Array<{name: string, lat: number, lon: number}>} */
        this.landmarks = [];
        /** @type {HTMLDivElement|null} */
        this.resultsContainer = null;
        /** @type {number|null} Debounce timer id */
        this._debounceTimer = null;

        if (!this.container) {
            console.error(`SearchBar container '${containerId}' not found.`);
            return;
        }

        // Register the global click-outside listener ONCE in the constructor,
        // not inside render(), to avoid duplicate listeners on re-render.
        this._boundOutsideClick = (e) => {
            if (this.resultsContainer && !this.container.contains(e.target)) {
                this.resultsContainer.style.display = 'none';
            }
        };
        document.addEventListener('click', this._boundOutsideClick);

        this.init();
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
        input.placeholder = 'Search landmarks & POIs...';
        input.setAttribute('aria-label', 'Search planetary landmarks');
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
            input.focus();
        });

        // Debounced input handler (200ms)
        input.addEventListener('input', (e) => {
            const val = e.target.value;
            clearBtn.style.display = val ? 'flex' : 'none';
            clearTimeout(this._debounceTimer);
            this._debounceTimer = setTimeout(() => this.handleInput(val), 200);
        });
        input.addEventListener('focus', (e) => this.handleInput(e.target.value));

        // Keyboard navigation (Esc to close)
        input.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                if (this.resultsContainer) this.resultsContainer.style.display = 'none';
            }
        });

        box.appendChild(icon);
        box.appendChild(input);
        box.appendChild(clearBtn);

        // Results Dropdown
        this.resultsContainer = document.createElement('div');
        this.resultsContainer.className = 'modern-search-results custom-slim-scroll';

        this.container.appendChild(box);
        this.container.appendChild(this.resultsContainer);

        // Update placeholder on body change
        document.addEventListener('jmars:body-changed', (e) => {
            const body = e?.detail?.body || 'Mars';
            const capBody = body.charAt(0).toUpperCase() + body.slice(1);
            input.placeholder = `Search ${capBody} landmarks...`;
        });
    }

    /**
     * Filter landmarks by query and render matching results.
     * @param {string} query - Current input value
     */
    handleInput(query) {
        if (!query || query.trim() === '') {
            this.resultsContainer.style.display = 'none';
            return;
        }

        const lowerQuery = query.toLowerCase();
        const matches = this.landmarks.filter(l =>
            l.name.toLowerCase().includes(lowerQuery)
        );

        this.renderResults(matches);
    }

    /**
     * Render the filtered results dropdown.
     * @param {Array<{name: string, lat: number, lon: number}>} matches - Matching landmarks
     * @private
     */
    renderResults(matches) {
        this.resultsContainer.innerHTML = '';

        if (matches.length === 0) {
            const noRes = document.createElement('div');
            noRes.textContent = 'No matching landmarks';
            noRes.style.padding = '10px';
            noRes.style.color = '#64748b';
            noRes.style.fontSize = '11px';
            noRes.style.fontStyle = 'italic';
            noRes.style.textAlign = 'center';
            this.resultsContainer.appendChild(noRes);
        } else {
            matches.slice(0, 15).forEach(match => {
                const item = document.createElement('div');
                item.className = 'modern-search-item';

                const nameSpan = document.createElement('span');
                nameSpan.textContent = match.name;
                nameSpan.style.fontWeight = '500';

                const coordsSpan = document.createElement('span');
                coordsSpan.style.fontFamily = 'monospace';
                coordsSpan.style.fontSize = '9px';
                coordsSpan.style.color = '#64748b';
                coordsSpan.textContent = `${match.lat.toFixed(1)}°, ${match.lon.toFixed(1)}°`;

                item.appendChild(nameSpan);
                item.appendChild(coordsSpan);

                item.addEventListener('click', () => {
                    this.selectLandmark(match);
                });

                this.resultsContainer.appendChild(item);
            });
        }

        this.resultsContainer.style.display = 'block';
    }

    /**
     * Pan the map to the selected landmark.
     * Normalizes longitude from 0-360 to -180/180 if needed.
     * @param {{name: string, lat: number, lon: number}} landmark - Selected landmark
     */
    selectLandmark(landmark) {
        // Normalize longitude for Leaflet's -180/180 range
        let targetLon = landmark.lon;
        if (targetLon > 180) targetLon -= 360;

        this.map.setView([landmark.lat, targetLon], Math.max(this.map.getZoom(), 6));
        this.resultsContainer.style.display = 'none';
        if (this.input) this.input.value = landmark.name;
    }
}
