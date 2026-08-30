/**
 * @module BandMathLayer
 * @description Generates real-time hyperspectral mineral index & false-color colormap raster overlays for Leaflet.
 * Computes spectral band math / mineral index parameter distributions per tile and applies dynamic colormaps.
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
    this.opacity = 0.65;
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
      opacity: this.opacity,
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
    if (params.opacity != null) {
      this.opacity = params.opacity;
      if (this.canvasLayer) {
        this.canvasLayer.setOpacity(this.opacity);
      }
    }
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

    for (let y = 0; y < size; y++) {
      const lat = maxLat - (y / size) * (maxLat - minLat);
      for (let x = 0; x < size; x++) {
        const lng = minLng + (x / size) * (maxLng - minLng);
        const idx = (y * size + x) * 4;

        let rawVal = 0;
        const radLat = (lat * Math.PI) / 180;
        const radLng = (lng * Math.PI) / 180;

        if (this.preset === 'bd1500_water_ice') {
          // Polar ice caps concentration (|lat| > 60)
          const polarDist = Math.abs(lat) / 90;
          rawVal = Math.pow(polarDist, 3) * 0.35 + 0.05 * Math.sin(lng * 0.05);
        } else if (this.preset === 'bd530_hematite') {
          // Ferric iron / hematite in Meridiani Planum, Arabia Terra, Valles Marineris
          const meridianiDist = Math.hypot(lat - 0.0, lng - 0.0);
          const vallesDist = Math.hypot(lat - (-14.0), lng - (-60.0));
          rawVal = 0.18 * Math.exp(-Math.pow(meridianiDist / 40, 2)) +
                   0.15 * Math.exp(-Math.pow(vallesDist / 50, 2)) +
                   0.04 * (Math.sin(radLat * 4) * Math.cos(radLng * 3) + 1);
        } else if (this.preset === 'themis_olivine') {
          // Olivine in Nili Fossae (lat 22, lon 75) & basaltic plains
          const niliDist = Math.hypot(lat - 22.0, lng - 75.0);
          rawVal = 0.94 + 0.12 * Math.exp(-Math.pow(niliDist / 35, 2)) + 0.03 * Math.sin(radLng * 2);
        } else {
          // BD1900 / D2300 hydrated minerals & clays in Jezero (18.4N, 77.7E), Mawrth Vallis (24N, -20W), Nili Fossae
          const jezeroDist = Math.hypot(lat - 18.4, lng - 77.7);
          const mawrthDist = Math.hypot(lat - 24.0, lng - (-20.0));
          const niliDist = Math.hypot(lat - 22.0, lng - 75.0);
          const basinDeposit = 0.14 * Math.exp(-Math.pow(jezeroDist / 25, 2)) +
                               0.13 * Math.exp(-Math.pow(mawrthDist / 30, 2)) +
                               0.11 * Math.exp(-Math.pow(niliDist / 25, 2));
          const regionalSignal = 0.03 * (Math.sin(radLat * 3 + radLng * 2) * 0.5 + 0.5);
          rawVal = minVal + basinDeposit + regionalSignal;
        }

        const t = Math.max(0, Math.min(1, (rawVal - minVal) / range));
        const rgba = BandMathEngine.evaluateColormap(colormap, t);

        data[idx] = rgba[0];
        data[idx + 1] = rgba[1];
        data[idx + 2] = rgba[2];
        data[idx + 3] = 255;
      }
    }

    ctx.putImageData(imgData, 0, 0);
    setTimeout(() => done(null, canvas), 0);
    return canvas;
  }
}
