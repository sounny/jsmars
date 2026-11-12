/**
 * @module CesiumGlobe
 * @description Full-size CesiumJS globe for the active planetary body.
 *
 * Uses the body's IAU WGCCRE ellipsoid and drapes the same WMS/XYZ sources as
 * the Leaflet map. There is no Cesium ion token and no Earth imagery fallback.
 * The globe is the reference ellipsoid only — elevation terrain is not loaded.
 *
 * When the optional solar terminator is enabled, illumination uses authentic
 * IAU WGCCRE + JPL J2000 planetary ephemeris (`src/util/solarEphemeris.js`)
 * rather than a hardcoded fake solar declination.
 */
import {
  BODY_ELLIPSOIDS,
  minCameraHeightMeters,
  zoomToCameraHeightMeters,
  cameraHeightToZoom
} from './globeViewModel.js';
import {
  computeSubSolarPointForBody,
  computeModeledLocalSolarLight
} from '../../util/solarEphemeris.js';

/**
 * Resolve a Cesium ellipsoid for a body. Built-in Mars/Moon/WGS84 constants
 * are preferred. Other bodies are constructed from BODY_ELLIPSOIDS so a
 * missing name never falls through to the Earth ellipsoid.
 * @param {object} Cesium
 * @param {string} bodyKey
 * @returns {object}
 */
export function resolveEllipsoid(Cesium, bodyKey) {
  const key = String(bodyKey || 'mars').toLowerCase();
  const spec = BODY_ELLIPSOIDS[key] || BODY_ELLIPSOIDS.mars;
  if (spec.cesiumName && Cesium.Ellipsoid[spec.cesiumName]) {
    return Cesium.Ellipsoid[spec.cesiumName];
  }
  const [x, y, z] = spec.radii;
  // Cesium's EllipsoidTerrainProvider quadtree requires x === y (axisymmetric spheroid)
  const eq = (x + y) * 0.5;
  return new Cesium.Ellipsoid(eq, eq, z);
}

/**
 * @param {object} Cesium
 * @param {object} ellipsoid
 * @param {object} descriptor
 * @returns {object|null}
 */
function createImageryProvider(Cesium, ellipsoid, descriptor) {
  if (!descriptor || !descriptor.url) return null;
  const credit = descriptor.attribution || descriptor.name || '';
  if (descriptor.type === 'wms') {
    return new Cesium.WebMapServiceImageryProvider({
      url: descriptor.url,
      layers: descriptor.layers,
      parameters: {
        transparent: descriptor.transparent,
        format: descriptor.format
      },
      tilingScheme: new Cesium.GeographicTilingScheme({ ellipsoid }),
      credit
    });
  }
  if (descriptor.type === 'xyz') {
    const scheme = descriptor.scheme === 'geographic'
      ? new Cesium.GeographicTilingScheme({ ellipsoid })
      : new Cesium.WebMercatorTilingScheme({ ellipsoid });
    return new Cesium.UrlTemplateImageryProvider({
      url: descriptor.url,
      tilingScheme: scheme,
      maximumLevel: descriptor.maxZoom,
      credit,
      ellipsoid
    });
  }
  return null;
}

export class CesiumGlobe {
  /**
   * @param {HTMLElement} container
   */
  constructor(container) {
    this.container = container;
    this.viewer = null;
    this.Cesium = null;
    this.ellipsoid = null;
    this.bodyKey = null;
    this.autoSpin = false;
    this._spinLast = null;
    this._onTick = null;
    this._onCameraMoveEnd = null;
    this._handler = null;
    this._graticuleLayer = null;
    this.onHover = null;
    this.onCameraChanged = null;
    this.onSpinPaused = null;
    this._lightingHour = null;
    this.lightingState = { enabled: false };
  }

  get ready() {
    return !!(this.viewer && !this.viewer.isDestroyed?.());
  }

  /**
   * Create or rebuild the viewer for a body and drape imagery.
   * @param {object} Cesium
   * @param {{body: string, lat: number, lng: number, zoom: number, imagery: Array<object>, viewportHeightPx?: number}} options
   */
  mount(Cesium, options) {
    this.Cesium = Cesium;
    const rawBody = String(options.body || 'mars').toLowerCase();
    const bodyKey = BODY_ELLIPSOIDS[rawBody] ? rawBody : 'mars';
    const ellipsoid = resolveEllipsoid(Cesium, bodyKey);
    Cesium.Ellipsoid.default = ellipsoid;

    if (this.ready && this.bodyKey === bodyKey) {
      this.setImagery(options.imagery);
      this.setCameraView(options);
      return;
    }

    this.autoSpin = false;
    this.destroyViewer();
    Cesium.Ellipsoid.default = ellipsoid;
    // Cesium ships a demo ion token. Clear it so this view never calls ion
    // or falls back to Earth world imagery.
    if (Cesium.Ion) Cesium.Ion.defaultAccessToken = '';
    this.bodyKey = bodyKey;
    this.ellipsoid = ellipsoid;
    const spec = BODY_ELLIPSOIDS[bodyKey];

    const viewer = new Cesium.Viewer(this.container, {
      ellipsoid,
      globe: new Cesium.Globe(ellipsoid),
      baseLayer: false,
      baseLayerPicker: false,
      geocoder: false,
      homeButton: false,
      sceneModePicker: false,
      navigationHelpButton: false,
      animation: false,
      timeline: false,
      fullscreenButton: false,
      vrButton: false,
      infoBox: false,
      selectionIndicator: false,
      skyBox: Cesium.SkyBox.createEarthSkyBox(),
      skyAtmosphere: bodyKey === 'earth' ? new Cesium.SkyAtmosphere(ellipsoid) : false,
      terrainProvider: new Cesium.EllipsoidTerrainProvider({ ellipsoid }),
      contextOptions: {
        webgl: { alpha: false, preserveDrawingBuffer: true }
      }
    });

    this.viewer = viewer;
    const globe = viewer.scene.globe;
    globe.baseColor = Cesium.Color.fromCssColorString(spec.baseColor);
    globe.showGroundAtmosphere = bodyKey === 'earth';
    globe.enableLighting = false;
    globe.depthTestAgainstTerrain = false;
    // Scale Cesium's default Earth-sized lighting fade distances to the body's radius
    // so terminator shading works identically on Deimos (7 km) and Jupiter (71,492 km).
    globe.lightingFadeOutDistance = ellipsoid.maximumRadius * 20;
    globe.lightingFadeInDistance = ellipsoid.maximumRadius * 40;
    globe.nightFadeOutDistance = ellipsoid.maximumRadius * 20;
    globe.nightFadeInDistance = ellipsoid.maximumRadius * 50;

    const controller = viewer.scene.screenSpaceCameraController;
    controller.minimumZoomDistance = minCameraHeightMeters(ellipsoid.maximumRadius);
    controller.maximumZoomDistance = ellipsoid.maximumRadius * 12;

    this._bindInteraction();
    this._bindSpin();
    this.setImagery(options.imagery);
    this.setCameraView(options);
    viewer.resize();
  }

  /**
   * Replace draped imagery. Index 0 is the bottom of the stack.
   * @param {Array<object>} descriptors
   */
  setImagery(descriptors) {
    if (!this.ready) return;
    const { Cesium, viewer, ellipsoid } = this;
    const showGraticule = !!this._graticuleLayer;
    viewer.imageryLayers.removeAll();
    this._graticuleLayer = null;
    (descriptors || []).forEach((desc) => {
      try {
        const provider = createImageryProvider(Cesium, ellipsoid, desc);
        if (!provider) return;
        const layer = viewer.imageryLayers.addImageryProvider(provider);
        layer.alpha = typeof desc.opacity === 'number' ? desc.opacity : 1;
      } catch (err) {
        console.warn('JSMARS globe skipped an imagery layer', desc && desc.id, err);
      }
    });
    if (showGraticule) this.setGraticule(true);
  }

  /**
   * Nadir camera at the Leaflet center, with altitude chosen from zoom.
   * @param {{lat: number, lng: number, zoom?: number, heightMeters?: number, viewportHeightPx?: number}} view
   */
  setCameraView(view) {
    if (!this.ready || !view) return;
    const { Cesium, ellipsoid } = this;
    const rawLat = Number(view.lat);
    const lng = Number(view.lng);
    if (!Number.isFinite(rawLat) || !Number.isFinite(lng)) return;
    const lat = Math.max(-90, Math.min(90, rawLat));
    const height = Number.isFinite(view.heightMeters)
      ? view.heightMeters
      : zoomToCameraHeightMeters(view.zoom, {
        radiusMeters: ellipsoid.maximumRadius,
        viewportHeightPx: view.viewportHeightPx
      });
    this.viewer.camera.setView({
      destination: Cesium.Cartesian3.fromDegrees(lng, lat, height, ellipsoid),
      orientation: {
        heading: 0,
        pitch: Cesium.Math.toRadians(-90),
        roll: 0
      }
    });
    if (typeof this.onCameraChanged === 'function') {
      this.onCameraChanged(this.getCameraView({ viewportHeightPx: view.viewportHeightPx }));
    }
  }

  /**
   * Geographic camera pose for restoring the Leaflet view.
   * The look target is the ellipsoid point under the screen center.
   * @param {{viewportHeightPx?: number}} [options]
   * @returns {{lat: number, lng: number, zoom: number, heightMeters: number}|null}
   */
  getCameraView(options = {}) {
    if (!this.ready) return null;
    const { Cesium, viewer, ellipsoid } = this;
    const canvas = viewer.scene.canvas;
    const center = new Cesium.Cartesian2(canvas.clientWidth / 2, canvas.clientHeight / 2);
    const picked = viewer.camera.pickEllipsoid(center, ellipsoid);
    const carto = picked
      ? Cesium.Cartographic.fromCartesian(picked, ellipsoid)
      : viewer.camera.positionCartographic;
    if (!carto) return null;
    const heightMeters = viewer.camera.positionCartographic
      ? viewer.camera.positionCartographic.height
      : carto.height;
    const viewportHeightPx = options.viewportHeightPx || canvas.clientHeight;
    return {
      lat: Cesium.Math.toDegrees(carto.latitude),
      lng: Cesium.Math.toDegrees(carto.longitude),
      heightMeters,
      zoom: cameraHeightToZoom(heightMeters, {
        radiusMeters: ellipsoid.maximumRadius,
        viewportHeightPx
      })
    };
  }

  zoomBy(direction) {
    if (!this.ready) return;
    const minStep = Math.max(5, minCameraHeightMeters(this.ellipsoid?.maximumRadius || 3396190) * 0.5);
    const height = this.viewer.camera.positionCartographic?.height || 100000;
    const step = Math.max(minStep, height * 0.45);
    if (direction > 0) this.viewer.camera.zoomIn(step);
    else this.viewer.camera.zoomOut(step);
    if (typeof this.onCameraChanged === 'function') {
      this.onCameraChanged(this.getCameraView());
    }
  }

  setAutoSpin(enabled) {
    this.autoSpin = !!enabled;
    this._spinLast = null;
  }

  setGraticule(enabled) {
    if (!this.ready) return;
    const { Cesium, viewer, ellipsoid } = this;
    if (!enabled) {
      if (this._graticuleLayer) {
        viewer.imageryLayers.remove(this._graticuleLayer, true);
        this._graticuleLayer = null;
      }
      return;
    }
    if (this._graticuleLayer) return;
    const provider = new Cesium.GridImageryProvider({
      tilingScheme: new Cesium.GeographicTilingScheme({ ellipsoid }),
      cells: 8,
      color: Cesium.Color.fromCssColorString('#f8fafc').withAlpha(0.45),
      glowColor: Cesium.Color.fromCssColorString('#fbbf24').withAlpha(0.2),
      glowWidth: 1,
      backgroundColor: Cesium.Color.TRANSPARENT
    });
    this._graticuleLayer = viewer.imageryLayers.addImageryProvider(provider);
  }

  /**
   * Apply smooth-ellipsoid day/night solar terminator shading.
   *
   * Modes:
   * - `'ephemeris'` (default): Real astronomical IAU WGCCRE + JPL J2000 solar
   *   ephemeris at `options.date` (defaults to current UTC). Also updates
   *   `viewer.clock.currentTime` so Earth's `SkyAtmosphere` aligns with the ground terminator.
   * - `'local'` (`[Model]`): Synthetic local solar hour (`0..24`, `12` = local noon
   *   at `options.referenceLon` or current camera target longitude) using the
   *   body's authentic seasonal solar declination at `options.date`.
   *
   * @param {{enabled: boolean, mode?: 'ephemeris'|'local', body?: string, date?: Date|number|string, hour?: number, referenceLon?: number}} options
   * @returns {object} Applied lighting state summary
   */
  setLighting(options) {
    if (!options || !options.enabled) {
      this._lightingHour = null;
      this.lightingState = { enabled: false };
      if (this.ready) {
        this.viewer.scene.globe.enableLighting = false;
        this.viewer.scene.light = new this.Cesium.SunLight();
      }
      return this.lightingState;
    }

    const bodyKey = this.bodyKey || options.body || 'mars';
    const mode = options.mode === 'local' ? 'local' : 'ephemeris';
    const parsedDate = options.date ? new Date(options.date) : new Date();
    const validDate = Number.isNaN(parsedDate.getTime()) ? new Date() : parsedDate;

    let calc;
    if (mode === 'local') {
      const hour = Number.isFinite(options.hour) ? options.hour : 12;
      this._lightingHour = hour;
      const camLon = Number.isFinite(options.referenceLon)
        ? options.referenceLon
        : (this.getCameraView()?.lng ?? 0);
      calc = computeModeledLocalSolarLight(bodyKey, hour, camLon, validDate);
    } else {
      this._lightingHour = null;
      calc = computeSubSolarPointForBody(bodyKey, validDate);
    }

    this.lightingState = {
      enabled: true,
      ...calc
    };

    if (!this.ready) return this.lightingState;

    const { Cesium, viewer } = this;
    if (viewer.clock && Cesium.JulianDate?.fromDate) {
      viewer.clock.currentTime = Cesium.JulianDate.fromDate(validDate);
    }
    const [dx, dy, dz] = calc.lightDirectionFixed;
    const direction = new Cesium.Cartesian3(dx, dy, dz);
    Cesium.Cartesian3.normalize(direction, direction);
    viewer.scene.light = new Cesium.DirectionalLight({
      direction,
      intensity: 2.0
    });
    viewer.scene.globe.enableLighting = true;
    return this.lightingState;
  }

  resize() {
    if (this.ready) this.viewer.resize();
  }

  _bindSpin() {
    const { Cesium, viewer } = this;
    this._onTick = () => {
      if (!this.autoSpin || !this.ready) return;
      const now = Cesium.JulianDate.now();
      if (!this._spinLast) {
        this._spinLast = now;
        return;
      }
      const dt = Cesium.JulianDate.secondsDifference(now, this._spinLast);
      this._spinLast = now;
      if (dt <= 0 || dt > 0.5) return;
      const camera = viewer.camera;
      const carto = camera.positionCartographic;
      if (!carto) return;
      const lon = carto.longitude + Cesium.Math.toRadians(8) * dt;
      camera.setView({
        destination: Cesium.Cartesian3.fromRadians(lon, carto.latitude, carto.height, this.ellipsoid),
        orientation: {
          heading: camera.heading,
          pitch: camera.pitch,
          roll: 0
        }
      });
    };
    viewer.clock.onTick.addEventListener(this._onTick);
  }

  _bindInteraction() {
    const { Cesium, viewer, ellipsoid } = this;
    const canvas = viewer.scene.canvas;
    this._handler = new Cesium.ScreenSpaceEventHandler(canvas);
    const pause = () => {
      if (!this.autoSpin) return;
      this.autoSpin = false;
      if (typeof this.onSpinPaused === 'function') this.onSpinPaused();
    };
    this._handler.setInputAction(pause, Cesium.ScreenSpaceEventType.LEFT_DOWN);
    this._handler.setInputAction(pause, Cesium.ScreenSpaceEventType.WHEEL);
    this._handler.setInputAction(pause, Cesium.ScreenSpaceEventType.PINCH_START);
    this._handler.setInputAction((movement) => {
      if (typeof this.onHover !== 'function') return;
      const cartesian = viewer.camera.pickEllipsoid(movement.endPosition, ellipsoid);
      if (!cartesian) {
        this.onHover(null);
        return;
      }
      const carto = Cesium.Cartographic.fromCartesian(cartesian, ellipsoid);
      this.onHover({
        lat: Cesium.Math.toDegrees(carto.latitude),
        lng: Cesium.Math.toDegrees(carto.longitude)
      });
    }, Cesium.ScreenSpaceEventType.MOUSE_MOVE);

    if (viewer.camera?.moveEnd?.addEventListener) {
      this._onCameraMoveEnd = () => {
        if (typeof this.onCameraChanged === 'function' && this.ready) {
          this.onCameraChanged(this.getCameraView());
        }
      };
      viewer.camera.moveEnd.addEventListener(this._onCameraMoveEnd);
    }
  }

  destroyViewer() {
    if (this._handler) {
      this._handler.destroy();
      this._handler = null;
    }
    if (this.viewer && this._onCameraMoveEnd && this.viewer.camera?.moveEnd?.removeEventListener) {
      this.viewer.camera.moveEnd.removeEventListener(this._onCameraMoveEnd);
    }
    this._onCameraMoveEnd = null;
    if (this.viewer && this._onTick) {
      this.viewer.clock.onTick.removeEventListener(this._onTick);
    }
    this._onTick = null;
    this._graticuleLayer = null;
    if (this.viewer && !this.viewer.isDestroyed?.()) {
      this.viewer.destroy();
    }
    this.viewer = null;
    this.ellipsoid = null;
    this.bodyKey = null;
    if (this.Cesium) {
      this.Cesium.Ellipsoid.default = this.Cesium.Ellipsoid.WGS84;
    }
  }

  destroy() {
    this.destroyViewer();
  }
}
