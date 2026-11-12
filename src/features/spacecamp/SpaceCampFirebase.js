/**
 * @module spacecamp/SpaceCampFirebase
 * @description Real-time Firebase backend client for JSMARS Space Camp (`sounny-com`).
 * Uses Firebase Modular RTDB SDK (v10.14.1) for sub-second WebSocket updates with an
 * automatic HTTPS REST fallback (`sounny-com-default-rtdb.firebaseio.com`) so handle
 * claiming, score syncing, and classroom projector leaderboards never fail on restricted networks.
 */

import { validateHandle, generateHandleSuggestions } from './SpaceCampCookies.js';

export const FIREBASE_CONFIG = Object.freeze({
  apiKey: 'AIzaSyDOvvKUfJ_mU6uFBmm4_8onC7CfYDksq9E',
  authDomain: 'sounny-com.firebaseapp.com',
  databaseURL: 'https://sounny-com-default-rtdb.firebaseio.com',
  projectId: 'sounny-com',
  storageBucket: 'sounny-com.firebasestorage.app',
  messagingSenderId: '319087162666',
  appId: '1:319087162666:web:d2e3c2cd65775a6bfdda53'
});

const RTDB_BASE = FIREBASE_CONFIG.databaseURL;

export class SpaceCampFirebase {
  constructor() {
    this.sdk = null;
    this.db = null;
    this._initPromise = null;
    this._lastNonce = Date.now();
    this._secretRegistered = false;
    this._pollTimers = [];
  }

  _nextNonce() {
    const now = Date.now();
    this._lastNonce = now > this._lastNonce ? now : this._lastNonce + 1;
    return this._lastNonce;
  }

  async init() {
    if (this._initPromise) return this._initPromise;
    this._initPromise = (async () => {
      try {
        const [appMod, dbMod] = await Promise.all([
          import('https://www.gstatic.com/firebasejs/10.14.1/firebase-app.js'),
          import('https://www.gstatic.com/firebasejs/10.14.1/firebase-database.js')
        ]);
        const app = appMod.getApps().length
          ? appMod.getApp()
          : appMod.initializeApp(FIREBASE_CONFIG);
        this.db = dbMod.getDatabase(app, FIREBASE_CONFIG.databaseURL);
        this.sdk = dbMod;
        return 'sdk';
      } catch (err) {
        console.warn('[SpaceCampFirebase] WebSocket SDK unavailable, using direct HTTPS REST mode:', err?.message || err);
        this.sdk = null;
        this.db = null;
        return 'rest';
      }
    })();
    return this._initPromise;
  }

  async _restGet(path) {
    const resp = await fetch(`${RTDB_BASE}${path}.json`, { cache: 'no-store' });
    if (!resp.ok) throw new Error(`RTDB GET ${path} HTTP ${resp.status}`);
    return resp.json();
  }

  async _restPatch(path, payload) {
    const resp = await fetch(`${RTDB_BASE}${path}.json`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    if (!resp.ok) {
      const txt = await resp.text().catch(() => '');
      const err = new Error(`RTDB PATCH ${path} HTTP ${resp.status}: ${txt}`);
      err.status = resp.status;
      throw err;
    }
    return resp.json();
  }

  async _getNode(path) {
    await this.init();
    if (this.sdk && this.db) {
      const snap = await this.sdk.get(this.sdk.ref(this.db, path.replace(/^\//, '')));
      return snap.exists() ? snap.val() : null;
    }
    return this._restGet(path);
  }

  async _patchSpaceCamp(multiPathUpdates) {
    await this.init();
    if (this.sdk && this.db) {
      await this.sdk.update(this.sdk.ref(this.db, 'spacecamp'), multiPathUpdates);
      return true;
    }
    await this._restPatch('/spacecamp', multiPathUpdates);
    return true;
  }

  /**
   * Atomically verify and claim a gamer handle in Firebase RTDB.
   * If already claimed by another player, returns 3 verified-available suggestions.
   *
   * @param {string} rawHandle
   * @param {{ playerId: string, secret: string }} identity
   * @param {object} [initialSnapshot]
   * @returns {Promise<{ ok: boolean, handle?: string, taken?: boolean, invalid?: boolean, suggestions?: string[], error?: string }>}
   */
  async claimHandle(rawHandle, identity, initialSnapshot = {}) {
    const { valid, handle, handleKey } = validateHandle(rawHandle);
    if (!valid) {
      return { ok: false, invalid: true, suggestions: generateHandleSuggestions(rawHandle, {}, 3) };
    }

    try {
      const existing = await this._getNode(`/spacecamp/handles/${handleKey}`);
      if (existing && existing.playerId && existing.playerId !== identity.playerId) {
        const allHandles = (await this._getNode('/spacecamp/handles')) || {};
        return {
          ok: false,
          taken: true,
          suggestions: generateHandleSuggestions(handle, allHandles, 3)
        };
      }

      // Sync nonce above any existing stored leaderboard/handle record for this player
      const existingBoard = await this._getNode(`/spacecamp/leaderboard/${identity.playerId}`);
      if (existingBoard && Number.isFinite(existingBoard.nonce) && existingBoard.nonce >= this._lastNonce) {
        this._lastNonce = existingBoard.nonce + 1;
      }
      if (existing && Number.isFinite(existing.nonce) && existing.nonce >= this._lastNonce) {
        this._lastNonce = existing.nonce + 1;
      }

      const nonce = this._nextNonce();
      const createdAt = existing?.createdAt || nonce;

      const basePayload = {
        [`proofs/${identity.playerId}`]: { secret: identity.secret, nonce },
        [`handles/${handleKey}`]: {
          playerId: identity.playerId,
          handle,
          createdAt,
          nonce
        },
        [`leaderboard/${identity.playerId}`]: {
          handle,
          handleKey,
          score: Math.min(600, Math.max(0, Number(initialSnapshot.score) || 0)),
          currentRound: Math.min(5, Math.max(1, Number(initialSnapshot.currentRound) || 1)),
          completedRounds: Math.min(5, Math.max(0, Number(initialSnapshot.completedRounds) || 0)),
          completedCount: Math.min(20, Math.max(0, Number(initialSnapshot.completedCount) || 0)),
          currentBody: String(initialSnapshot.currentBody || 'earth').slice(0, 24),
          roundScores: initialSnapshot.roundScores || { r1: 0, r2: 0, r3: 0, r4: 0, r5: 0 },
          updatedAt: nonce,
          nonce
        }
      };

      // Try registering write-once secret first (if new player); if secret already exists, retry without secrets/$pid
      if (!this._secretRegistered && !existingBoard && !existing) {
        try {
          await this._patchSpaceCamp({
            [`secrets/${identity.playerId}`]: identity.secret,
            ...basePayload
          });
          this._secretRegistered = true;
          return { ok: true, handle };
        } catch {
          // Secret already existed from previous session or handle raced; retry update below
        }
      }

      await this._patchSpaceCamp(basePayload);
      this._secretRegistered = true;
      return { ok: true, handle };
    } catch (err) {
      // Re-check if handle was claimed in a simultaneous race condition
      try {
        const allHandles = (await this._getNode('/spacecamp/handles')) || {};
        const winner = allHandles[handleKey];
        if (winner && winner.playerId !== identity.playerId) {
          return {
            ok: false,
            taken: true,
            suggestions: generateHandleSuggestions(handle, allHandles, 3)
          };
        }
      } catch {
        // Ignore secondary network error
      }
      console.warn('[SpaceCampFirebase] claimHandle warning:', err?.message || err);
      return { ok: false, error: err?.message || 'Network error' };
    }
  }

  /**
   * Push updated score, round progress, and optional live discovery feed event atomically.
   * @param {{ playerId: string, secret: string, handle: string }} identity
   * @param {object} progress
   * @param {object|null} [feedEvent]
   */
  async syncProgress(identity, progress, feedEvent = null) {
    if (!identity?.playerId || !identity?.secret || !identity?.handle) return false;
    const { valid, handle, handleKey } = validateHandle(identity.handle);
    if (!valid) return false;

    const nonce = this._nextNonce();
    const updates = {
      [`proofs/${identity.playerId}`]: {
        secret: identity.secret,
        nonce
      },
      [`leaderboard/${identity.playerId}`]: {
        handle,
        handleKey,
        score: Math.min(600, Math.max(0, Math.round(Number(progress.score) || 0))),
        currentRound: Math.min(5, Math.max(1, Number(progress.currentRound) || 1)),
        completedRounds: Math.min(5, Math.max(0, Number(progress.completedRounds) || 0)),
        completedCount: Math.min(20, Math.max(0, Number(progress.completedCount) || 0)),
        currentBody: String(progress.currentBody || 'earth').toLowerCase().slice(0, 24),
        roundScores: progress.roundScores || { r1: 0, r2: 0, r3: 0, r4: 0, r5: 0 },
        updatedAt: nonce,
        nonce
      }
    };

    if (feedEvent && Number.isFinite(feedEvent.points) && feedEvent.points >= 0) {
      const evtId = `evt_${nonce}_${identity.playerId.slice(-6)}`;
      updates[`feed/${evtId}`] = {
        playerId: identity.playerId,
        handle,
        kind: String(feedEvent.kind || 'step').slice(0, 32),
        labelEn: String(feedEvent.labelEn || '').slice(0, 80),
        labelFr: String(feedEvent.labelFr || '').slice(0, 80),
        roundNum: Math.min(5, Math.max(1, Number(feedEvent.roundNum) || 1)),
        points: Math.min(180, Math.max(0, Math.round(feedEvent.points))),
        ts: nonce,
        nonce
      };
    }

    try {
      await this._patchSpaceCamp(updates);
      return true;
    } catch (err) {
      console.warn('[SpaceCampFirebase] syncProgress warning:', err?.message || err);
      return false;
    }
  }

  /**
   * Subscribe to real-time sorted leaderboard updates.
   * @param {(rows: Array<object>) => void} callback
   * @returns {() => void} Unsubscribe function
   */
  subscribeLeaderboard(callback) {
    let active = true;
    let sdkUnsub = null;

    const emitSorted = (val) => {
      if (!active) return;
      const obj = val || {};
      const rows = Object.entries(obj)
        .map(([playerId, item]) => ({ playerId, ...item }))
        .filter((r) => r && typeof r.handle === 'string' && Number.isFinite(r.score))
        .sort((a, b) => {
          if (b.score !== a.score) return b.score - a.score;
          if ((b.completedRounds || 0) !== (a.completedRounds || 0)) {
            return (b.completedRounds || 0) - (a.completedRounds || 0);
          }
          return (a.updatedAt || 0) - (b.updatedAt || 0);
        });
      callback(rows);
    };

    this.init().then((mode) => {
      if (!active) return;
      if (mode === 'sdk' && this.sdk && this.db) {
        const lbRef = this.sdk.ref(this.db, 'spacecamp/leaderboard');
        sdkUnsub = this.sdk.onValue(lbRef, (snap) => {
          emitSorted(snap.exists() ? snap.val() : {});
        });
      } else {
        const poll = async () => {
          if (!active) return;
          try {
            const data = await this._restGet('/spacecamp/leaderboard');
            emitSorted(data);
          } catch {
            // Keep existing standings on transient error
          }
        };
        poll();
        const id = setInterval(poll, 3500);
        this._pollTimers.push(id);
      }
    });

    return () => {
      active = false;
      if (sdkUnsub) sdkUnsub();
      this._pollTimers.forEach((id) => clearInterval(id));
      this._pollTimers = [];
    };
  }

  /**
   * Subscribe to the 20 most recent live discovery feed events.
   * @param {(events: Array<object>) => void} callback
   * @returns {() => void} Unsubscribe function
   */
  subscribeFeed(callback) {
    let active = true;
    let sdkUnsub = null;

    const emitRecent = (val) => {
      if (!active) return;
      const obj = val || {};
      const events = Object.entries(obj)
        .map(([id, item]) => ({ id, ...item }))
        .filter((e) => e && typeof e.handle === 'string' && Number.isFinite(e.ts))
        .sort((a, b) => b.ts - a.ts)
        .slice(0, 20);
      callback(events);
    };

    this.init().then((mode) => {
      if (!active) return;
      if (mode === 'sdk' && this.sdk && this.db) {
        const q = this.sdk.query(
          this.sdk.ref(this.db, 'spacecamp/feed'),
          this.sdk.orderByChild('ts'),
          this.sdk.limitToLast(20)
        );
        sdkUnsub = this.sdk.onValue(q, (snap) => {
          emitRecent(snap.exists() ? snap.val() : {});
        });
      } else {
        const poll = async () => {
          if (!active) return;
          try {
            const data = await this._restGet('/spacecamp/feed');
            emitRecent(data);
          } catch {
            // Ignore transient error
          }
        };
        poll();
        const id = setInterval(poll, 4500);
        this._pollTimers.push(id);
      }
    });

    return () => {
      active = false;
      if (sdkUnsub) sdkUnsub();
    };
  }
}
