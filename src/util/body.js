import { JMARS_CONFIG } from '../jmars-config.js';
import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';

/**
 * Normalize an arbitrary body identifier to the app's canonical lowercase key.
 * Falls back to Mars for unknown or missing values.
 * @param {string} bodyKey
 * @returns {'mars'|'moon'|'earth'}
 */
export function normalizeBodyKey(bodyKey) {
  const key = typeof bodyKey === 'string' ? bodyKey.trim().toLowerCase() : '';
  return JMARS_CONFIG.bodies[key] ? key : 'mars';
}

/**
 * Canonical body-switch operation used by UI, sessions, bookmarks, and deep links.
 * Ensures the map switches first, then the store is updated, then BODY_CHANGED is emitted.
 * @param {object} jmarsMap
 * @param {string} bodyKey
 * @param {{ emitEvent?: boolean, force?: boolean }} [options]
 * @returns {string|Promise<string>}
 */
export function switchActiveBody(jmarsMap, bodyKey, options = {}) {
  const key = normalizeBodyKey(bodyKey);
  const emitEvent = options.emitEvent !== false;
  const force = options.force === true;

  if (!jmarsMap || typeof jmarsMap.switchBody !== 'function') {
    throw new Error('A JMARSMap controller is required for body switching.');
  }

  const finishSwitch = () => {
    if (jmarsState.get('body') !== key) {
      jmarsState.set('body', key);
    }
    if (emitEvent) {
      document.dispatchEvent(new CustomEvent(EVENTS.BODY_CHANGED, { detail: { body: key } }));
    }
    return key;
  };

  if (!force && jmarsMap.currentBody === key) {
    if (jmarsState.get('body') !== key) {
      jmarsState.set('body', key);
    }
    return key;
  }

  const result = jmarsMap.switchBody(key);
  if (result && typeof result.then === 'function') {
    return result.then(() => finishSwitch());
  }

  return finishSwitch();
}
