import { EVENTS } from '../../constants.js';
import { to180, normalizeLon } from '../../util/geo.js';

/**
 * @class GridLayer
 * @description Renders a planetary latitude/longitude graticule grid on the Leaflet map.
 * Supports adaptive zoom spacing, major/minor grid lines, and edge coordinate annotations.
 */
export class GridLayer {
  constructor(map) {
    this.map = map;
    this.layerGroup = L.layerGroup();
    this.isActive = false;

    // Config options
    this.autoSpacing = true;
    this.majorInterval = 10; // degrees
    this.minorInterval = 2;  // degrees
    this.showMinor = true;
    this.showLabels = true;
    this.majorColor = '#38bdf8';
    this.minorColor = '#0284c7';
    this.majorOpacity = 0.6;
    this.minorOpacity = 0.25;
    this.lonFormat = 'east360'; // 'east360', '180', 'west360'

    this._onMoveEnd = this._onMoveEnd.bind(this);
  }

  /**
   * Activate the graticule grid layer.
   */
  activate() {
    if (this.isActive) return;
    this.isActive = true;
    this.layerGroup.addTo(this.map);
    this.map.on('moveend', this._onMoveEnd);
    this.map.on('zoomend', this._onMoveEnd);
    this.render();
  }

  /**
   * Deactivate the graticule grid layer.
   */
  deactivate() {
    if (!this.isActive) return;
    this.isActive = false;
    this.map.removeLayer(this.layerGroup);
    this.map.off('moveend', this._onMoveEnd);
    this.map.off('zoomend', this._onMoveEnd);
  }

  /**
   * Toggle grid visibility.
   * @returns {boolean}
   */
  toggle() {
    if (this.isActive) {
      this.deactivate();
    } else {
      this.activate();
    }
    return this.isActive;
  }

  _onMoveEnd() {
    if (this.isActive) {
      this.render();
    }
  }

  /**
   * Determine optimal grid spacing based on current map zoom level.
   * @returns {{major: number, minor: number}}
   */
  getAdaptiveSpacing() {
    const zoom = this.map.getZoom();
    if (zoom <= 2) return { major: 30, minor: 10 };
    if (zoom <= 4) return { major: 15, minor: 5 };
    if (zoom <= 6) return { major: 5, minor: 1 };
    if (zoom <= 8) return { major: 1, minor: 0.2 };
    if (zoom <= 10) return { major: 0.5, minor: 0.1 };
    if (zoom <= 12) return { major: 0.1, minor: 0.02 };
    return { major: 0.05, minor: 0.01 };
  }

  /**
   * Format longitude according to selected convention.
   * @param {number} lonDeg - Longitude in -180..180
   * @returns {string}
   */
  formatLon(lonDeg) {
    if (this.lonFormat === 'east360') {
      const e = normalizeLon(lonDeg);
      return `${e.toFixed(this._getPrecision())}\u00b0E`;
    } else if (this.lonFormat === 'west360') {
      let w = 360 - normalizeLon(lonDeg);
      if (w >= 360) w -= 360;
      return `${w.toFixed(this._getPrecision())}\u00b0W`;
    } else {
      const dir = lonDeg >= 0 ? 'E' : 'W';
      return `${Math.abs(lonDeg).toFixed(this._getPrecision())}\u00b0${dir}`;
    }
  }

  /**
   * Format latitude.
   * @param {number} latDeg
   * @returns {string}
   */
  formatLat(latDeg) {
    const dir = latDeg >= 0 ? 'N' : 'S';
    return `${Math.abs(latDeg).toFixed(this._getPrecision())}\u00b0${dir}`;
  }

  _getPrecision() {
    const interval = this.autoSpacing ? this.getAdaptiveSpacing().major : this.majorInterval;
    if (interval < 0.1) return 3;
    if (interval < 1) return 2;
    if (interval < 5) return 1;
    return 0;
  }

  /**
   * Render graticule parallels and meridians.
   */
  render() {
    this.layerGroup.clearLayers();
    if (!this.isActive) return;

    const bounds = this.map.getBounds();
    const spacing = this.autoSpacing ? this.getAdaptiveSpacing() : {
      major: this.majorInterval,
      minor: this.minorInterval
    };

    const minLat = Math.max(bounds.getSouth(), -88);
    const maxLat = Math.min(bounds.getNorth(), 88);
    const minLon = bounds.getWest();
    const maxLon = bounds.getEast();

    // 1. Minor Parallels (Latitude)
    if (this.showMinor && spacing.minor < spacing.major) {
      const startMinorLat = Math.floor(minLat / spacing.minor) * spacing.minor;
      for (let lat = startMinorLat; lat <= maxLat; lat += spacing.minor) {
        if (Math.abs(lat % spacing.major) > 1e-4) {
          const line = L.polyline([[lat, minLon - 5], [lat, maxLon + 5]], {
            color: this.minorColor,
            weight: 1,
            opacity: this.minorOpacity,
            dashArray: '2,4',
            interactive: false
          });
          this.layerGroup.addLayer(line);
        }
      }
    }

    // 2. Major Parallels (Latitude)
    const startMajorLat = Math.floor(minLat / spacing.major) * spacing.major;
    for (let lat = startMajorLat; lat <= maxLat; lat += spacing.major) {
      const line = L.polyline([[lat, minLon - 5], [lat, maxLon + 5]], {
        color: this.majorColor,
        weight: lat === 0 ? 2 : 1.2,
        opacity: lat === 0 ? 0.9 : this.majorOpacity,
        interactive: false
      });
      this.layerGroup.addLayer(line);

      // Lat label
      if (this.showLabels) {
        const labelPos = [lat, minLon + (maxLon - minLon) * 0.05];
        const labelIcon = L.divIcon({
          className: 'grid-lat-label',
          html: `<span style="font-size:10px; color:${this.majorColor}; font-weight:bold; background:rgba(0,0,0,0.6); padding:1px 3px; border-radius:2px;">${this.formatLat(lat)}</span>`,
          iconSize: [60, 16],
          iconAnchor: [0, 8]
        });
        this.layerGroup.addLayer(L.marker(labelPos, { icon: labelIcon, interactive: false }));
      }
    }

    // 3. Minor Meridians (Longitude)
    if (this.showMinor && spacing.minor < spacing.major) {
      const startMinorLon = Math.floor(minLon / spacing.minor) * spacing.minor;
      for (let lon = startMinorLon; lon <= maxLon; lon += spacing.minor) {
        if (Math.abs(lon % spacing.major) > 1e-4) {
          const line = L.polyline([[minLat - 5, lon], [maxLat + 5, lon]], {
            color: this.minorColor,
            weight: 1,
            opacity: this.minorOpacity,
            dashArray: '2,4',
            interactive: false
          });
          this.layerGroup.addLayer(line);
        }
      }
    }

    // 4. Major Meridians (Longitude)
    const startMajorLon = Math.floor(minLon / spacing.major) * spacing.major;
    for (let lon = startMajorLon; lon <= maxLon; lon += spacing.major) {
      const isPrime = (Math.abs(lon % 360) < 1e-4);
      const line = L.polyline([[minLat - 5, lon], [maxLat + 5, lon]], {
        color: this.majorColor,
        weight: isPrime ? 2 : 1.2,
        opacity: isPrime ? 0.9 : this.majorOpacity,
        interactive: false
      });
      this.layerGroup.addLayer(line);

      // Lon label
      if (this.showLabels) {
        const labelPos = [minLat + (maxLat - minLat) * 0.05, lon];
        const labelIcon = L.divIcon({
          className: 'grid-lon-label',
          html: `<span style="font-size:10px; color:${this.majorColor}; font-weight:bold; background:rgba(0,0,0,0.6); padding:1px 3px; border-radius:2px;">${this.formatLon(lon)}</span>`,
          iconSize: [60, 16],
          iconAnchor: [30, 0]
        });
        this.layerGroup.addLayer(L.marker(labelPos, { icon: labelIcon, interactive: false }));
      }
    }
  }
}
