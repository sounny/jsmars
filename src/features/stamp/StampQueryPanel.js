import { EVENTS } from '../../constants.js';

/**
 * StampQueryPanel provides a UI for querying the stamp layer.
 * Shows instrument selector, filter controls, and search actions.
 */
export class StampQueryPanel {
  /**
   * @param {HTMLElement} container - Parent container element
   * @param {import('./StampLayer.js').StampLayer} stampLayer - Stamp layer instance
   */
  constructor(container, stampLayer) {
    this.container = container;
    this.stampLayer = stampLayer;
    this.isLoading = false;
    this._build();
    this._bindEvents();
  }

  _build() {
    this.container.innerHTML = `
      <div class="stamp-panel">
        <div class="stamp-controls">
          <label class="stamp-label">Instrument</label>
          <select id="stamp-instrument-select" class="stamp-select">
            ${this.stampLayer.getInstruments().map(i =>
              `<option value="${i.id}" ${i.id === 'THEMIS' ? 'selected' : ''}>${i.name}</option>`
            ).join('')}
          </select>

          <label class="stamp-label">Max Results</label>
          <select id="stamp-limit-select" class="stamp-select">
            <option value="100">100</option>
            <option value="250">250</option>
            <option value="500" selected>500</option>
            <option value="1000">1000</option>
          </select>

          <div class="stamp-btn-row">
            <button id="stamp-search-btn" class="tool-btn stamp-search-btn">
              Search Current View
            </button>
          </div>

          <div id="stamp-status" class="stamp-status"></div>

          <div class="stamp-btn-row">
            <button id="stamp-clear-btn" class="crater-action-btn" style="background:#333">Clear</button>
            <button id="stamp-export-btn" class="crater-action-btn" style="background:#333">Export CSV</button>
          </div>
        </div>

        <div id="stamp-results-container" class="stamp-results-container"></div>
      </div>
    `;

    this.searchBtn = this.container.querySelector('#stamp-search-btn');
    this.clearBtn = this.container.querySelector('#stamp-clear-btn');
    this.exportBtn = this.container.querySelector('#stamp-export-btn');
    this.statusEl = this.container.querySelector('#stamp-status');
    this.instrumentSelect = this.container.querySelector('#stamp-instrument-select');
    this.limitSelect = this.container.querySelector('#stamp-limit-select');
    this.resultsContainer = this.container.querySelector('#stamp-results-container');
  }

  _bindEvents() {
    this.searchBtn.addEventListener('click', () => this._doSearch());
    this.clearBtn.addEventListener('click', () => this._doClear());
    this.exportBtn.addEventListener('click', () => this.stampLayer.exportCSV());

    this.instrumentSelect.addEventListener('change', (e) => {
      this.stampLayer.setInstrument(e.target.value);
    });

    // Listen for stamp events
    document.addEventListener(EVENTS.STAMP_QUERY_START, () => {
      this.isLoading = true;
      this.searchBtn.textContent = 'Searching...';
      this.searchBtn.disabled = true;
      this.statusEl.textContent = '';
    });

    document.addEventListener(EVENTS.STAMP_QUERY_COMPLETE, (e) => {
      this.isLoading = false;
      this.searchBtn.textContent = 'Search Current View';
      this.searchBtn.disabled = false;

      const detail = e.detail;
      if (detail.error) {
        this.statusEl.textContent = `Error: ${detail.error}`;
        this.statusEl.style.color = '#ff6b6b';
      } else {
        this.statusEl.textContent = `Found ${detail.count} products`;
        this.statusEl.style.color = '#51cf66';
        this._renderResultsTable(detail.results || []);
      }
    });

    document.addEventListener(EVENTS.STAMP_SELECTED, (e) => {
      this._highlightRow(e.detail);
    });
  }

  async _doSearch() {
    if (this.isLoading) return;
    const limit = parseInt(this.limitSelect.value, 10);
    this.stampLayer.activate();
    await this.stampLayer.query({ limit });
  }

  _doClear() {
    this.stampLayer.clear();
    this.resultsContainer.innerHTML = '';
    this.statusEl.textContent = '';
  }

  /**
   * Render a scrollable results table.
   * @param {Array} results
   */
  _renderResultsTable(results) {
    if (results.length === 0) {
      this.resultsContainer.innerHTML = '<div style="padding:8px;color:#888">No results found.</div>';
      return;
    }

    const rows = results.map((p, idx) => `
      <tr class="stamp-row" data-idx="${idx}">
        <td title="${p.pdsId}">${this._truncate(p.pdsId, 24)}</td>
        <td>${p.centerLat.toFixed(2)}</td>
        <td>${p.centerLon.toFixed(2)}</td>
        <td>${p.solarLon != null ? p.solarLon.toFixed(1) : '-'}</td>
      </tr>
    `).join('');

    this.resultsContainer.innerHTML = `
      <table class="stamp-table">
        <thead>
          <tr>
            <th>Product ID</th>
            <th>Lat</th>
            <th>Lon</th>
            <th>Ls</th>
          </tr>
        </thead>
        <tbody>${rows}</tbody>
      </table>
    `;

    // Click to select and zoom
    this.resultsContainer.querySelectorAll('.stamp-row').forEach(row => {
      row.addEventListener('click', () => {
        const idx = parseInt(row.dataset.idx, 10);
        this.stampLayer.selectStamp(idx);
        const product = this.stampLayer.results[idx];
        if (product) {
          this.stampLayer.map?.flyTo([product.centerLat, product.centerLon], 8);
        }
      });
    });
  }

  /**
   * Highlight a row matching the selected product.
   * @param {object} product
   */
  _highlightRow(product) {
    this.resultsContainer.querySelectorAll('.stamp-row').forEach(row => {
      row.classList.remove('stamp-row-selected');
    });
    const idx = this.stampLayer.results.indexOf(product);
    if (idx >= 0) {
      const row = this.resultsContainer.querySelector(`[data-idx="${idx}"]`);
      if (row) {
        row.classList.add('stamp-row-selected');
        row.scrollIntoView({ block: 'nearest' });
      }
    }
  }

  _truncate(str, len) {
    if (!str) return '';
    return str.length > len ? str.substring(0, len) + '...' : str;
  }
}
