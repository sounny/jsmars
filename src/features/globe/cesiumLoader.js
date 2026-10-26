/**
 * @module cesiumLoader
 * @description Lazy loader for the pinned CesiumJS build.
 *
 * The script and stylesheet are injected the first time the user switches
 * to 3D. They are not referenced from index.html, so the initial page
 * weight does not include the multi-megabyte bundle.
 */
import { CESIUM_CDN_BASE } from './globeViewModel.js';

/** @type {Promise<object>|null} */
let cesiumPromise = null;

/**
 * Load window.Cesium from the pinned CDN build.
 * @returns {Promise<object>} The Cesium namespace
 */
export function loadCesium() {
  if (typeof window !== 'undefined' && window.Cesium) {
    return Promise.resolve(window.Cesium);
  }
  if (cesiumPromise) return cesiumPromise;

  cesiumPromise = new Promise((resolve, reject) => {
    const fail = (error) => {
      cesiumPromise = null;
      reject(error instanceof Error ? error : new Error(String(error)));
    };

    window.CESIUM_BASE_URL = CESIUM_CDN_BASE;

    const existingCss = document.querySelector('link[data-jsmars-cesium="css"]');
    if (!existingCss) {
      const css = document.createElement('link');
      css.rel = 'stylesheet';
      css.href = `${CESIUM_CDN_BASE}Widgets/widgets.css`;
      css.dataset.jsmarsCesium = 'css';
      document.head.appendChild(css);
    }

    const script = document.createElement('script');
    script.src = `${CESIUM_CDN_BASE}Cesium.js`;
    script.async = true;
    script.dataset.jsmarsCesium = 'js';
    script.onload = () => {
      if (window.Cesium) resolve(window.Cesium);
      else fail(new Error('CesiumJS loaded without a Cesium global'));
    };
    script.onerror = () => fail(new Error('Failed to load CesiumJS'));
    document.head.appendChild(script);
  });

  const timeout = new Promise((_, reject) => {
    window.setTimeout(() => reject(new Error('CesiumJS load timed out')), 45000);
  });

  const raced = Promise.race([cesiumPromise, timeout]);
  raced.catch(() => {
    cesiumPromise = null;
  });
  return raced;
}
