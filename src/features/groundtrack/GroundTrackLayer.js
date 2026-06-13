import { EVENTS } from '../../constants.js';

/**
 * GroundTrackLayer visualizes spacecraft ground tracks on the map.
 * Uses pre-computed orbital parameters to generate track paths
 * for Mars orbiters (MRO, Odyssey, MAVEN, Mars Express, MGS).
 *
 * Ground tracks are approximated using Keplerian orbital elements
 * since we cannot access SPICE data from the browser.
 */
export class GroundTrackLayer {
  constructor(map) {
    this.map = map;
    this.trackGroup = L.layerGroup();
    this.isActive = false;
    this.activeTracks = new Map(); // spacecraft -> L.Polyline[]

    /**
     * Approximate orbital parameters for Mars orbiters.
     * period in minutes, inclination in degrees, altitude in km
     */
    this.spacecraft = {
      'MRO': {
        name: 'Mars Reconnaissance Orbiter',
        period: 112,
        inclination: 92.65,
        altitude: 250,
        color: '#4dabf7',
        active: true
      },
      'ODY': {
        name: 'Mars Odyssey',
        period: 118.5,
        inclination: 93.06,
        altitude: 400,
        color: '#51cf66',
        active: true
      },
      'MAVEN': {
        name: 'MAVEN',
        period: 270,
        inclination: 75,
        altitude: 6200,
        color: '#ffd43b',
        active: true
      },
      'MEX': {
        name: 'Mars Express',
        period: 420,
        inclination: 86.35,
        altitude: 10530,
        color: '#ff922b',
        active: true
      },
      'MGS': {
        name: 'Mars Global Surveyor',
        period: 117.65,
        inclination: 92.96,
        altitude: 370,
        color: '#da77f2',
        active: false  // Mission ended 2006
      }
    };
  }

  /**
   * Activate the ground track layer.
   */
  activate() {
    if (this.isActive) return;
    this.isActive = true;
    this.trackGroup.addTo(this.map);
  }

  /**
   * Deactivate the ground track layer.
   */
  deactivate() {
    if (!this.isActive) return;
    this.isActive = false;
    this.map.removeLayer(this.trackGroup);
  }

  /**
   * Toggle visibility of a specific spacecraft track.
   * @param {string} scId - Spacecraft ID (e.g., 'MRO')
   * @param {boolean} show - Whether to show
   */
  toggleSpacecraft(scId, show) {
    if (show) {
      this._generateTrack(scId);
    } else {
      this._removeTrack(scId);
    }
  }

  /**
   * Generate a ground track for a spacecraft.
   * This is an approximation using simple orbital mechanics.
   * @param {string} scId
   * @param {number} [orbits=3] - Number of orbits to display
   */
  _generateTrack(scId, orbits = 3) {
    const sc = this.spacecraft[scId];
    if (!sc) return;

    this._removeTrack(scId);

    const marsRotationPeriod = 24.6229 * 60; // Mars sidereal day in minutes
    const periodMin = sc.period;
    const inclination = sc.inclination * Math.PI / 180;
    const pointsPerOrbit = 180;
    const totalPoints = orbits * pointsPerOrbit;

    // Use current time as epoch reference for varying tracks
    const epochOffset = (Date.now() / 60000) % marsRotationPeriod;

    const tracks = [];
    let currentSegment = [];

    for (let i = 0; i <= totalPoints; i++) {
      const t = (i / pointsPerOrbit) * periodMin; // time in minutes
      const trueAnomaly = (2 * Math.PI * i) / pointsPerOrbit;

      // Latitude: determined by inclination and position in orbit
      let lat = Math.asin(Math.sin(inclination) * Math.sin(trueAnomaly)) * 180 / Math.PI;

      // Longitude: advances due to Mars rotation
      // The ascending node regresses due to J2 perturbation
      const j2Rate = -0.0045; // degrees per minute (approximate for Mars J2)
      const nodeRegression = j2Rate * t;
      const marsRotation = (t / marsRotationPeriod) * 360;
      let lon = (epochOffset / marsRotationPeriod * 360) + nodeRegression - marsRotation +
                Math.atan2(Math.cos(inclination) * Math.sin(trueAnomaly), Math.cos(trueAnomaly)) * 180 / Math.PI;

      // Normalize to -180..180
      lon = ((lon + 540) % 360) - 180;

      // Handle segment breaks at antimeridian crossing
      if (currentSegment.length > 0) {
        const prevLon = currentSegment[currentSegment.length - 1][1];
        if (Math.abs(lon - prevLon) > 170) {
          // Antimeridian crossing: start new segment
          tracks.push([...currentSegment]);
          currentSegment = [];
        }
      }

      currentSegment.push([lat, lon]);
    }
    if (currentSegment.length > 1) tracks.push(currentSegment);

    // Create polylines
    const polylines = tracks.map(segment => {
      return L.polyline(segment, {
        color: sc.color,
        weight: 1.5,
        opacity: 0.7,
        dashArray: '6,4',
        className: 'groundtrack-line'
      });
    });

    polylines.forEach(pl => this.trackGroup.addLayer(pl));

    // Add orbit label at the midpoint
    if (tracks.length > 0 && tracks[0].length > 2) {
      const midIdx = Math.floor(tracks[0].length / 2);
      const midPoint = tracks[0][midIdx];
      const label = L.marker(midPoint, {
        icon: L.divIcon({
          className: 'groundtrack-label',
          html: `<span style="color:${sc.color}; font-size:10px; font-weight:bold; text-shadow: 0 0 3px #000">${scId}</span>`,
          iconSize: [40, 14],
          iconAnchor: [20, 7]
        })
      });
      polylines.push(label);
      this.trackGroup.addLayer(label);
    }

    this.activeTracks.set(scId, polylines);

    document.dispatchEvent(new CustomEvent(EVENTS.GROUNDTRACK_LOADED, {
      detail: { spacecraft: scId, segments: tracks.length }
    }));
  }

  /**
   * Remove a specific spacecraft track.
   * @param {string} scId
   */
  _removeTrack(scId) {
    const layers = this.activeTracks.get(scId);
    if (layers) {
      layers.forEach(l => this.trackGroup.removeLayer(l));
      this.activeTracks.delete(scId);
    }
  }

  /**
   * Clear all tracks.
   */
  clearAll() {
    this.trackGroup.clearLayers();
    this.activeTracks.clear();
  }

  /**
   * Get list of available spacecraft.
   * @returns {Array<{id: string, name: string, active: boolean, color: string}>}
   */
  getSpacecraftList() {
    return Object.entries(this.spacecraft).map(([id, sc]) => ({
      id,
      name: sc.name,
      active: sc.active,
      color: sc.color
    }));
  }
}
