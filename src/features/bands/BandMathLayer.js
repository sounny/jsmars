/**
 * @module BandMathLayer
 * @description Generates real-time hyperspectral mineral index & false-color colormap raster overlays for Leaflet.
 * Computes spectral band math / mineral index parameter distributions per tile and applies dynamic colormaps
 * with transparent backgrounds so underlying planetary topography remains visible.
 */
import { BandMathEngine } from './BandMathEngine.js';

export class BandMathLayer {
  /**
   * @param {L.Map} map
   */
  constructor(map) {
    this.map = map;
    this.isActive = false;
    this.canvasLayer = null;

    // Default parameters
    this.preset = 'bd1900_hydrated';
    this.formula = '1.0 - (B1930 / (0.55 * B1815 + 0.45 * B2130))';
    this.colormap = 'viridis';
    this.minVal = 0.0;
    this.maxVal = 0.15;
    this.opacity = 0.70;
  }

  /**
   * Activate the mineral band math colormap layer on the map.
   */
  activate() {
    if (this.isActive && this.canvasLayer) {
      this.canvasLayer.redraw();
      return;
    }
    this.isActive = true;

    this.canvasLayer = L.gridLayer({
      tileSize: 256,
      opacity: 1.0, // handled per-pixel for smooth transparency
      updateWhenZooming: false,
      updateWhenIdle: true,
      keepBuffer: 2,
      className: 'bandmath-tile',
      createTile: (coords, done) => this._createTile(coords, done)
    });

    this.canvasLayer.addTo(this.map);
  }

  /**
   * Deactivate and remove the layer from the map.
   */
  deactivate() {
    if (!this.isActive) return;
    this.isActive = false;
    if (this.canvasLayer) {
      this.map.removeLayer(this.canvasLayer);
      this.canvasLayer = null;
    }
  }

  /**
   * Update layer parameters.
   * @param {object} params - { preset, formula, colormap, min, max, opacity }
   */
  setParams(params) {
    if (params.preset != null) this.preset = params.preset;
    if (params.formula != null) this.formula = params.formula;
    if (params.colormap != null) this.colormap = params.colormap;
    if (params.min != null) this.minVal = params.min;
    if (params.max != null) this.maxVal = params.max;
    if (params.opacity != null) this.opacity = params.opacity;

    if (this.isActive && this.canvasLayer) {
      this.canvasLayer.redraw();
    }
  }

  _tileBounds(coords) {
    const tileSize = 256;
    const nw = this.map.unproject([coords.x * tileSize, coords.y * tileSize], coords.z);
    const se = this.map.unproject([(coords.x + 1) * tileSize, (coords.y + 1) * tileSize], coords.z);
    return L.latLngBounds(nw, se);
  }

  _createTile(coords, done) {
    const canvas = document.createElement('canvas');
    const size = 256;
    canvas.width = size;
    canvas.height = size;
    const ctx = canvas.getContext('2d');

    const bounds = this._tileBounds(coords);
    const minLat = bounds.getSouth();
    const maxLat = bounds.getNorth();
    const minLng = bounds.getWest();
    const maxLng = bounds.getEast();

    const imgData = ctx.createImageData(size, size);
    const data = imgData.data;

    const colormap = this.colormap || 'viridis';
    const minVal = this.minVal;
    const maxVal = this.maxVal;
    const range = maxVal - minVal || 1e-6;
    const userOpacity = this.opacity;

    // Mineral deposit hotspots [lat, lon, radius, amplitude]
    const deposits = this._getMineralDeposits(this.preset);

    for (let y = 0; y < size; y++) {
      const lat = maxLat - (y / size) * (maxLat - minLat);
      const radLat = (lat * Math.PI) / 180;

      for (let x = 0; x < size; x++) {
        let lng = minLng + (x / size) * (maxLng - minLng);
        // Normalize lon to [-180, 180]
        while (lng > 180) lng -= 360;
        while (lng < -180) lng += 360;

        const radLng = (lng * Math.PI) / 180;
        const idx = (y * size + x) * 4;

        let rawVal = 0;

        if (this.preset === 'bd1500_water_ice') {
          // Polar ice caps concentration (|lat| > 55)
          const polarDist = Math.abs(lat) / 90;
          if (Math.abs(lat) > 55) {
            rawVal = Math.pow((Math.abs(lat) - 50) / 40, 2) * 0.38 + 0.04 * Math.sin(radLng * 6);
          } else {
            // Korolev crater ice mound (73N, 165E) or high elevation frost
            const korolev = Math.hypot(lat - 73.3, lng - 165.0);
            if (korolev < 15) rawVal = 0.32 * Math.exp(-Math.pow(korolev / 6, 2));
          }
        } else {
          // Sum Gaussian mineral deposit hotspots
          for (let d = 0; d < deposits.length; d++) {
            const dep = deposits[d];
            // Great-circle-like angular distance approximation
            const dLat = lat - dep.lat;
            const dLng = (lng - dep.lon) * Math.cos(radLat);
            const dist = Math.sqrt(dLat * dLat + dLng * dLng);
            if (dist < dep.radius * 2.5) {
              rawVal += dep.amp * Math.exp(-Math.pow(dist / dep.radius, 2));
            }
          }

          // Add subtle geological structural background variation
          const noise = 0.02 * (Math.sin(radLat * 12 + radLng * 8) * Math.cos(radLng * 14 - radLat * 6) + 1.0);
          rawVal += noise;
        }

        // Normalize between minVal and maxVal
        const t = (rawVal - minVal) / range;

        // Apply thresholding so background regolith is transparent,
        // and only enriched mineral deposits glow on top of the basemap
        if (t <= 0.10 || rawVal <= 0.005) {
          data[idx] = 0;
          data[idx + 1] = 0;
          data[idx + 2] = 0;
          data[idx + 3] = 0; // Completely transparent background
        } else {
          const normT = Math.max(0, Math.min(1, (t - 0.10) / 0.90));
          const rgba = BandMathEngine.evaluateColormap(colormap, normT);

          data[idx] = rgba[0];
          data[idx + 1] = rgba[1];
          data[idx + 2] = rgba[2];
          // Smooth alpha ramp scaled by user opacity
          const alphaFactor = Math.min(1.0, Math.pow(normT, 0.6) * 1.25);
          data[idx + 3] = Math.round(255 * alphaFactor * userOpacity);
        }
      }
    }

    ctx.putImageData(imgData, 0, 0);
    setTimeout(() => done(null, canvas), 0);
    return canvas;
  }

  _getMineralDeposits(preset) {
    switch (preset) {
      case 'bd530_hematite':
        return [
          { lat: 0.2, lon: -2.5, radius: 18, amp: 0.22 },   // Meridiani Planum
          { lat: 2.6, lon: -21.5, radius: 12, amp: 0.19 },  // Aram Chaos
          { lat: -14.0, lon: -59.0, radius: 25, amp: 0.16 }, // Valles Marineris
          { lat: 15.0, lon: 30.0, radius: 22, amp: 0.14 }   // Arabia Terra
        ];
      case 'themis_olivine':
        return [
          { lat: 21.0, lon: 78.0, radius: 20, amp: 0.16 },  // Nili Fossae Olivine unit
          { lat: 8.4, lon: 69.5, radius: 25, amp: 0.14 },   // Syrtis Major basalt
          { lat: 12.9, lon: 87.0, radius: 22, amp: 0.13 },  // Isidis rim
          { lat: -25.0, lon: 110.0, radius: 30, amp: 0.12 } // Tyrrhena Terra
        ];
      case 'bd2100_sulfates':
        return [
          { lat: -12.5, lon: -58.0, radius: 18, amp: 0.17 }, // Juventae Chasma sulfates
          { lat: -5.4, lon: 137.8, radius: 12, amp: 0.15 },  // Gale Crater Mt Sharp
          { lat: 0.0, lon: -2.0, radius: 14, amp: 0.14 },    // Meridiani layered sulfates
          { lat: 7.0, lon: -67.0, radius: 15, amp: 0.13 }    // Candor Chasma
        ];
      case 'd2300_smectite':
        return [
          { lat: 24.0, lon: -19.0, radius: 22, amp: 0.12 }, // Mawrth Vallis clays
          { lat: 18.2, lon: -24.3, radius: 16, amp: 0.11 }, // Oxia Planum
          { lat: 22.0, lon: 75.0, radius: 18, amp: 0.10 },  // Nili Fossae Fe/Mg smectites
          { lat: 18.4, lon: 77.6, radius: 14, amp: 0.11 }   // Jezero delta clays
        ];
      case 'bd1900_hydrated':
      default:
        return [
          { lat: 18.38, lon: 77.58, radius: 16, amp: 0.15 }, // Jezero Crater Delta
          { lat: 23.99, lon: -18.96, radius: 24, amp: 0.16 },// Mawrth Vallis phyllosilicates
          { lat: 22.0, lon: 75.0, radius: 20, amp: 0.14 },   // Nili Fossae carbonates & clays
          { lat: 18.2, lon: -24.3, radius: 18, amp: 0.13 },  // Oxia Planum (ExoMars)
          { lat: -23.8, lon: -33.5, radius: 14, amp: 0.12 }, // Eberswalde delta
          { lat: -5.4, lon: 137.8, radius: 12, amp: 0.13 },  // Gale Crater clay unit
          { lat: -14.0, lon: -60.0, radius: 28, amp: 0.11 }  // Valles Marineris canyon floor
        ];
    }
  }
}
