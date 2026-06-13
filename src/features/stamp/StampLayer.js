import { EVENTS } from '../../constants.js';

/**
 * StampLayer queries the USGS ODE REST API for instrument footprints
 * (THEMIS, CTX, HiRISE, etc.) and renders them on the map.
 *
 * ODE REST API docs: https://oderest.rsl.wustl.edu/
 */
export class StampLayer {
  constructor(map) {
    this.map = map;
    this.footprintGroup = L.layerGroup();
    this.isActive = false;
    this.results = [];
    this.selectedStamp = null;
    this.imageOverlay = null;

    // Query defaults
    this.instrument = 'THEMIS';
    this.productType = '';
    this.maxResults = 500;

    // ODE REST API base URL (CORS-friendly)
    this.baseUrl = 'https://oderest.rsl.wustl.edu/live2';

    // Instrument configurations for ODE queries
    this.instruments = {
      'THEMIS': { target: 'mars', host: 'ODY', instrument: 'THEMIS', productType: 'THMIR_IR_GEO' },
      'CTX': { target: 'mars', host: 'MRO', instrument: 'CTX', productType: 'CTX_EDR' },
      'HiRISE': { target: 'mars', host: 'MRO', instrument: 'HIRISE', productType: 'HIRISE_RDRV11' },
      'MOC-NA': { target: 'mars', host: 'MGS', instrument: 'MOC', productType: 'MOC_AB_NA_EDR' },
      'CRISM': { target: 'mars', host: 'MRO', instrument: 'CRISM', productType: 'CRISM_MRDR_TER' }
    };
  }

  /**
   * Set the active instrument for queries.
   * @param {string} name - Instrument key from this.instruments
   */
  setInstrument(name) {
    if (this.instruments[name]) {
      this.instrument = name;
    }
  }

  /**
   * Query the ODE REST API for products in the current map extent.
   * @param {object} [options] - Optional overrides
   * @param {string} [options.instrument] - Instrument name override
   * @param {number} [options.limit] - Max results override
   * @returns {Promise<Array>} - Array of product results
   */
  async query(options = {}) {
    const inst = options.instrument || this.instrument;
    const config = this.instruments[inst];
    if (!config) {
      console.warn(`Unknown instrument: ${inst}`);
      return [];
    }

    const bounds = this.map.getBounds();
    const limit = options.limit || this.maxResults;

    // Build ODE query URL
    const params = new URLSearchParams({
      query: 'product',
      results: 'fmpc',  // footprint coordinates
      output: 'JSON',
      target: config.target,
      ihid: config.host,
      iid: config.instrument,
      pt: config.productType,
      westernlon: this._normLon(bounds.getWest()),
      easternlon: this._normLon(bounds.getEast()),
      minlat: bounds.getSouth().toFixed(4),
      maxlat: bounds.getNorth().toFixed(4),
      limit: limit
    });

    const url = `${this.baseUrl}?${params.toString()}`;
    document.dispatchEvent(new CustomEvent(EVENTS.STAMP_QUERY_START, { detail: { instrument: inst } }));

    try {
      const response = await fetch(url);
      if (!response.ok) throw new Error(`ODE API error: ${response.status}`);
      const data = await response.json();

      // Parse ODE response
      this.results = this._parseODEResponse(data);

      // Render footprints
      this._renderFootprints();

      document.dispatchEvent(new CustomEvent(EVENTS.STAMP_QUERY_COMPLETE, {
        detail: { instrument: inst, count: this.results.length, results: this.results }
      }));

      return this.results;
    } catch (err) {
      console.error('Stamp query failed:', err);
      document.dispatchEvent(new CustomEvent(EVENTS.STAMP_QUERY_COMPLETE, {
        detail: { instrument: inst, count: 0, error: err.message }
      }));
      return [];
    }
  }

  /**
   * Parse ODE REST API JSON response into a flat array of products.
   * @param {object} data - Raw ODE JSON
   * @returns {Array<object>} - Parsed products
   */
  _parseODEResponse(data) {
    const products = [];
    try {
      const odeResponse = data?.ODEResults;
      if (!odeResponse) return [];

      const count = parseInt(odeResponse.Count, 10) || 0;
      if (count === 0) return [];

      let items = odeResponse.Products?.Product;
      if (!items) return [];
      if (!Array.isArray(items)) items = [items];

      items.forEach(product => {
        const p = {
          id: product.ode_id || product.pdsid || 'unknown',
          pdsId: product.pdsid || '',
          instrument: product.iid || '',
          productType: product.pt || '',
          centerLat: parseFloat(product.Center_latitude) || 0,
          centerLon: parseFloat(product.Center_longitude) || 0,
          westLon: parseFloat(product.Westernmost_longitude) || 0,
          eastLon: parseFloat(product.Easternmost_longitude) || 0,
          minLat: parseFloat(product.Minimum_latitude) || 0,
          maxLat: parseFloat(product.Maximum_latitude) || 0,
          solarLon: parseFloat(product.Solar_longitude) || null,
          emissionAngle: parseFloat(product.Emission_angle) || null,
          incidenceAngle: parseFloat(product.Incidence_angle) || null,
          phaseAngle: parseFloat(product.Phase_angle) || null,
          utcStart: product.UTC_start_time || '',
          labelUrl: product.LabelURL || '',
          footprint: null
        };

        // Parse footprint polygon
        const fp = product.Footprint_geometry || product.Footprints_cross_meridian;
        if (fp) {
          p.footprint = this._parseFootprintGeometry(fp);
        } else {
          // Fallback: create bounding box polygon
          p.footprint = [
            [p.minLat, this._to180(p.westLon)],
            [p.maxLat, this._to180(p.westLon)],
            [p.maxLat, this._to180(p.eastLon)],
            [p.minLat, this._to180(p.eastLon)]
          ];
        }

        products.push(p);
      });
    } catch (err) {
      console.error('Error parsing ODE response:', err);
    }
    return products;
  }

  /**
   * Parse ODE footprint geometry (typically GML or WKT).
   * @param {string|object} geom - Geometry data
   * @returns {Array<Array<number>>} - Array of [lat, lon] pairs
   */
  _parseFootprintGeometry(geom) {
    if (typeof geom === 'string') {
      // WKT POLYGON format: POLYGON((lon lat, lon lat, ...))
      const match = geom.match(/POLYGON\s*\(\(([^)]+)\)\)/i);
      if (match) {
        return match[1].split(',').map(pair => {
          const [lon, lat] = pair.trim().split(/\s+/).map(Number);
          return [lat, this._to180(lon)];
        });
      }

      // Try MULTIPOLYGON
      const multiMatch = geom.match(/MULTIPOLYGON\s*\(\(\(([^)]+)\)\)\)/i);
      if (multiMatch) {
        return multiMatch[1].split(',').map(pair => {
          const [lon, lat] = pair.trim().split(/\s+/).map(Number);
          return [lat, this._to180(lon)];
        });
      }
    }

    // GML format from ODE
    if (typeof geom === 'object' && geom['FOOTPRINT_GEOMETRY_TYPE'] === 'POLYGON') {
      const coords = geom['FOOTPRINT_C0_GEOMETRY'];
      if (typeof coords === 'string') {
        return coords.split(',').map(pair => {
          const [lon, lat] = pair.trim().split(/\s+/).map(Number);
          return [lat, this._to180(lon)];
        });
      }
    }

    return null;
  }

  /**
   * Render footprint polygons on the map.
   */
  _renderFootprints() {
    this.footprintGroup.clearLayers();

    const instColors = {
      'THEMIS': '#ff6b6b',
      'CTX': '#4dabf7',
      'HIRISE': '#51cf66',
      'MOC': '#ffd43b',
      'CRISM': '#da77f2'
    };

    const color = instColors[this.instrument] || instColors[this.results[0]?.instrument] || '#4dabf7';

    this.results.forEach((product, idx) => {
      if (!product.footprint || product.footprint.length < 3) return;

      const polygon = L.polygon(product.footprint, {
        color: color,
        weight: 1.5,
        opacity: 0.8,
        fillColor: color,
        fillOpacity: 0.1,
        className: 'stamp-footprint'
      });

      polygon.on('click', () => {
        this.selectStamp(idx);
      });

      polygon.on('mouseover', () => {
        polygon.setStyle({ fillOpacity: 0.35, weight: 2.5 });
      });

      polygon.on('mouseout', () => {
        if (this.selectedStamp !== idx) {
          polygon.setStyle({ fillOpacity: 0.1, weight: 1.5 });
        }
      });

      polygon.bindTooltip(product.pdsId || product.id, {
        sticky: true,
        className: 'stamp-tooltip'
      });

      this.footprintGroup.addLayer(polygon);
    });
  }

  /**
   * Select a stamp by index and highlight it.
   * @param {number} idx - Index in results array
   */
  selectStamp(idx) {
    this.selectedStamp = idx;
    const product = this.results[idx];
    if (!product) return;

    // Reset all styles
    this.footprintGroup.eachLayer((layer, i) => {
      layer.setStyle({ fillOpacity: 0.1, weight: 1.5 });
    });

    // Highlight selected
    const layers = this.footprintGroup.getLayers();
    if (layers[idx]) {
      layers[idx].setStyle({ fillOpacity: 0.4, weight: 3 });
    }

    document.dispatchEvent(new CustomEvent(EVENTS.STAMP_SELECTED, { detail: product }));
  }

  /**
   * Activate the stamp layer.
   */
  activate() {
    if (this.isActive) return;
    this.isActive = true;
    this.footprintGroup.addTo(this.map);
  }

  /**
   * Deactivate the stamp layer.
   */
  deactivate() {
    if (!this.isActive) return;
    this.isActive = false;
    this.map.removeLayer(this.footprintGroup);
    if (this.imageOverlay) {
      this.map.removeLayer(this.imageOverlay);
      this.imageOverlay = null;
    }
  }

  /**
   * Clear all results and footprints.
   */
  clear() {
    this.results = [];
    this.selectedStamp = null;
    this.footprintGroup.clearLayers();
    if (this.imageOverlay) {
      this.map.removeLayer(this.imageOverlay);
      this.imageOverlay = null;
    }
  }

  /**
   * Export results as CSV.
   */
  exportCSV() {
    if (this.results.length === 0) return;

    const headers = ['Product ID', 'Instrument', 'Center Lat', 'Center Lon', 'Solar Lon', 'Emission Angle', 'Incidence Angle', 'UTC Start'];
    const rows = this.results.map(p => [
      p.pdsId, p.instrument, p.centerLat, p.centerLon,
      p.solarLon || '', p.emissionAngle || '', p.incidenceAngle || '', p.utcStart
    ]);

    const csv = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `stamps_${this.instrument}_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  /**
   * Get list of available instruments.
   * @returns {Array<{id: string, name: string}>}
   */
  getInstruments() {
    return Object.keys(this.instruments).map(key => ({
      id: key,
      name: key
    }));
  }

  // Utility: convert longitude to 0-360 for ODE queries
  _normLon(lon) {
    let n = lon % 360;
    if (n < 0) n += 360;
    return n.toFixed(4);
  }

  // Utility: convert longitude to -180..180 for Leaflet
  _to180(lon) {
    let n = lon % 360;
    if (n < 0) n += 360;
    return n > 180 ? n - 360 : n;
  }
}
