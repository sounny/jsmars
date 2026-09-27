/**
 * @module CesiumGlobe
 * @description Full-size CesiumJS globe for the active planetary body.
 *
 * Uses the body's ellipsoid and drapes the same WMS/XYZ sources as the
 * Leaflet map. There is no Cesium ion token and no Earth imagery fallback.
 * The globe is the reference ellipsoid only — elevation terrain is not loaded.
 */
import {
  BODY_ELLIPSOIDS,
  zoomToCameraHeightMeters,
  cameraHeightToZoom
} from './globeViewModel.js';

/**
 * Resolve a Cesium ellipsoid for a body. Built-in Mars/Moon/WGS84 constants
 * are preferred. Other bodies are constructed from BODY_ELLIPSOIDS so a
 * missing name never falls through to the Earth ellipsoid.
 * @param {object} Cesium
 * @param {string} bodyKey
 * @returns {object}
 */
export function resolveEllipsoid(Cesium, bodyKey) {
  const spec = BODY_ELLIPSOIDS[bodyKey] || BODY_ELLIPSOIDS.mars;
  if (spec.cesiumName && Cesium.Ellipsoid[spec.cesiumName]) {
    return Cesium.Ellipsoid[spec.cesiumName];
  }
  const [x, y, z] = spec.radii;
  return new Cesium.Ellipsoid(x, y, z);
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
    this._handler = null;
    this._graticuleLayer = null;
    this.onHover = null;
    this.onSpinPaused = null;
    this._lightingHour = null;
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
    const bodyKey = BODY_ELLIPSOIDS[options.body] ? options.body : 'mars';
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
        webgl: { alpha: false }
      }
    });

    this.viewer = viewer;
    const globe = viewer.scene.globe;
    globe.baseColor = Cesium.Color.fromCssColorString(spec.baseColor);
    globe.showGroundAtmosphere = bodyKey === 'earth';
    globe.enableLighting = false;
    globe.depthTestAgainstTerrain = false;

    const controller = viewer.scene.screenSpaceCameraController;
    controller.minimumZoomDistance = 1000;
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
    const lat = Number(view.lat);
    const lng = Number(view.lng);
    if (!Number.isFinite(lat) || !Number.isFinite(lng)) return;
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
    const height = this.viewer.camera.positionCartographic?.height || 100000;
    const step = Math.max(500, height * 0.45);
    if (direction > 0) this.viewer.camera.zoomIn(step);
    else this.viewer.camera.zoomOut(step);
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
   * Visual sunlight only. Hour 12 lights the prime meridian.
   * This is not a body-specific solar ephemeris.
   * @param {{enabled: boolean, hour?: number}} options
   */
  setLighting(options) {
    if (!this.ready) return;
    const { Cesium, viewer } = this;
    if (!options || !options.enabled) {
      this._lightingHour = null;
      viewer.scene.globe.enableLighting = false;
      viewer.scene.light = new Cesium.SunLight();
      return;
    }
    const hour = Number.isFinite(options.hour) ? options.hour : 12;
    this._lightingHour = hour;
    const sunLon = (12 - hour) * (Math.PI / 12);
    const direction = new Cesium.Cartesian3(-Math.cos(sunLon), -Math.sin(sunLon), -0.15);
    Cesium.Cartesian3.normalize(direction, direction);
    viewer.scene.light = new Cesium.DirectionalLight({
      direction,
      intensity: 2.2
    });
    viewer.scene.globe.enableLighting = true;
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
  }

  destroyViewer() {
    if (this._handler) {
      this._handler.destroy();
      this._handler = null;
    }
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
