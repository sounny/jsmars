/**
 * @module spacecamp/SpaceCampCookies
 * @description Cookie & localStorage persistence, gamer handle validation,
 * cryptographic player secret generation, and collision-free handle suggestions.
 */

const COOKIE_MAX_AGE = 60 * 60 * 24 * 30; // 30 days
export const COOKIE_KEYS = Object.freeze({
  PLAYER_ID: 'jsmars_sc_pid',
  SECRET: 'jsmars_sc_secret',
  HANDLE: 'jsmars_sc_handle',
  LANG: 'jsmars_sc_lang'
});

export const STORAGE_KEY = 'jsmars_spacecamp_state_v2';

const HANDLE_REGEX = /^[A-Za-z0-9_-]{3,20}$/;

const SPACE_SUFFIXES = [
  'MRO', 'EO', 'BG', 'Mars', 'Luna', 'Orbit', 'Atlas', 'Rover', 'LOLA', '26', '42', '99'
];
const SPACE_PREFIXES = ['Orbit_', 'Astro_', 'Rover_', 'Atlas_', 'Nova_'];

/**
 * Read a cookie value from a cookie header string (defaults to document.cookie).
 * @param {string} name
 * @param {string} [cookieSource]
 * @returns {string}
 */
export function getCookie(name, cookieSource) {
  const src = typeof cookieSource === 'string'
    ? cookieSource
    : (typeof document !== 'undefined' ? document.cookie : '');
  if (!src) return '';
  const target = `${encodeURIComponent(name)}=`;
  const parts = src.split(';');
  for (const raw of parts) {
    const part = raw.trim();
    if (part.startsWith(target)) {
      try {
        return decodeURIComponent(part.slice(target.length));
      } catch {
        return part.slice(target.length);
      }
    }
  }
  return '';
}

/**
 * Write a persistent SameSite=Lax root cookie.
 * @param {string} name
 * @param {string} value
 * @param {number} [maxAgeSec=COOKIE_MAX_AGE]
 */
export function setCookie(name, value, maxAgeSec = COOKIE_MAX_AGE) {
  if (typeof document === 'undefined') return;
  const secure = typeof location !== 'undefined' && location.protocol === 'https:' ? '; Secure' : '';
  document.cookie = `${encodeURIComponent(name)}=${encodeURIComponent(value)}; max-age=${maxAgeSec}; path=/; SameSite=Lax${secure}`;
}

/**
 * Validate a gamer handle (3-20 chars, alphanumeric + _ or -).
 * @param {string} raw
 * @returns {{ valid: boolean, handle: string, handleKey: string }}
 */
export function validateHandle(raw) {
  const handle = String(raw || '').trim();
  const valid = HANDLE_REGEX.test(handle);
  return {
    valid,
    handle,
    handleKey: handle.toLowerCase()
  };
}

/**
 * Sanitize arbitrary input into a clean base string for handle suggestions.
 * @param {string} raw
 * @returns {string}
 */
export function sanitizeHandleBase(raw) {
  const cleaned = String(raw || '')
    .trim()
    .replace(/\s+/g, '_')
    .replace(/[^A-Za-z0-9_-]/g, '')
    .replace(/^[_-]+|[_-]+$/g, '');
  if (cleaned.length >= 3) return cleaned.slice(0, 13);
  return 'Explorer';
}

/**
 * Generate verified-available gamer handle suggestions when a desired handle is taken.
 * @param {string} desiredHandle
 * @param {Object<string, any>|Set<string>} takenHandles - Map or Set of lowercase handleKeys
 * @param {number} [count=3]
 * @returns {string[]}
 */
export function generateHandleSuggestions(desiredHandle, takenHandles = {}, count = 3) {
  const isTaken = (candidate) => {
    const key = candidate.toLowerCase();
    if (takenHandles instanceof Set) return takenHandles.has(key);
    return Boolean(takenHandles && Object.prototype.hasOwnProperty.call(takenHandles, key));
  };

  const base = sanitizeHandleBase(desiredHandle);
  const results = [];
  const seen = new Set();

  const tryPush = (cand) => {
    const clipped = cand.slice(0, 20);
    const { valid, handleKey } = validateHandle(clipped);
    if (!valid || seen.has(handleKey) || isTaken(clipped)) return;
    seen.add(handleKey);
    results.push(clipped);
  };

  for (const suffix of SPACE_SUFFIXES) {
    const maxBaseLen = Math.max(3, 20 - (suffix.length + 1));
    tryPush(`${base.slice(0, maxBaseLen)}_${suffix}`);
    if (results.length >= count) return results;
  }

  for (const prefix of SPACE_PREFIXES) {
    const maxBaseLen = Math.max(3, 20 - prefix.length);
    tryPush(`${prefix}${base.slice(0, maxBaseLen)}`);
    if (results.length >= count) return results;
  }

  for (let n = 101; n < 999 && results.length < count; n += 7) {
    tryPush(`${base.slice(0, 15)}_${n}`);
  }

  return results;
}

function randomHex(byteCount = 24) {
  const arr = new Uint8Array(byteCount);
  if (typeof crypto !== 'undefined' && crypto.getRandomValues) {
    crypto.getRandomValues(arr);
  } else {
    for (let i = 0; i < byteCount; i++) arr[i] = Math.floor(Math.random() * 256);
  }
  return Array.from(arr, (b) => b.toString(16).padStart(2, '0')).join('');
}

/**
 * Load or initialize persistent player identity (playerId + unreadable write secret + saved handle).
 * @returns {{ playerId: string, secret: string, handle: string, lang: string }}
 */
export function ensurePlayerIdentity() {
  let saved = {};
  try {
    saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '{}') || {};
  } catch {
    saved = {};
  }

  let playerId = getCookie(COOKIE_KEYS.PLAYER_ID) || saved.playerId || '';
  if (!/^sc_[a-z0-9]{12,40}$/.test(playerId)) {
    playerId = `sc_${randomHex(10)}`;
  }

  let secret = getCookie(COOKIE_KEYS.SECRET) || saved.secret || '';
  if (secret.length < 32) {
    secret = randomHex(24); // 48 hex chars
  }

  const rawHandle = getCookie(COOKIE_KEYS.HANDLE) || saved.handle || '';
  const handle = validateHandle(rawHandle).valid ? rawHandle.trim() : '';

  const urlLang = typeof window !== 'undefined'
    ? (new URLSearchParams(window.location.search).get('lang') || '').toLowerCase()
    : '';
  const rawLang = (urlLang || getCookie(COOKIE_KEYS.LANG) || saved.lang || '').toLowerCase();
  const lang = rawLang === 'fr' ? 'fr' : 'en';

  setCookie(COOKIE_KEYS.PLAYER_ID, playerId);
  setCookie(COOKIE_KEYS.SECRET, secret);
  if (handle) setCookie(COOKIE_KEYS.HANDLE, handle);
  setCookie(COOKIE_KEYS.LANG, lang);

  return { playerId, secret, handle, lang };
}

/**
 * Persist handle & language to both cookies and localStorage.
 * @param {string} handle
 * @param {string} [lang]
 */
export function persistPlayerHandle(handle, lang) {
  if (handle && validateHandle(handle).valid) {
    setCookie(COOKIE_KEYS.HANDLE, handle.trim());
  }
  if (lang === 'en' || lang === 'fr') {
    setCookie(COOKIE_KEYS.LANG, lang);
  }
}

/**
 * Reset player identity and progress so a new student on a shared lab computer
 * can claim a fresh callsign and start at 0 points.
 * @param {string} [lang='en']
 * @returns {{ playerId: string, secret: string, handle: string, lang: string }}
 */
export function resetPlayerIdentity(lang = 'en') {
  const safeLang = lang === 'fr' ? 'fr' : 'en';
  const playerId = `sc_${randomHex(10)}`;
  const secret = randomHex(24);
  setCookie(COOKIE_KEYS.PLAYER_ID, playerId);
  setCookie(COOKIE_KEYS.SECRET, secret);
  setCookie(COOKIE_KEYS.HANDLE, '', 0);
  setCookie(COOKIE_KEYS.LANG, safeLang);
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Ignore storage errors
  }
  return { playerId, secret, handle: '', lang: safeLang };
}

/**
 * Load full local Space Camp progress state.
 * @returns {object|null}
 */
export function loadLocalProgress() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

/**
 * Save full local Space Camp progress state.
 * @param {object} state
 */
export function saveLocalProgress(state) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
  } catch {
    // Ignore storage quota errors in private browsing
  }
}
