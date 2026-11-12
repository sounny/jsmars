/**
 * @module spacecamp/SpaceCampCopilot
 * @description Telemetry-grounded Mission CapCom assistant for JSMARS Space Camp.
 * Uses the official Gemini Interactions REST API (`POST /v1beta/interactions`,
 * `Api-Revision: 2026-05-20`, `model: "gemini-3.8-flash"`) when a user/workshop
 * Gemini API key is configured, and provides deterministic live JSMARS telemetry
 * diagnostics when running without an API key.
 */

const GEMINI_KEY_STORAGE = 'jsmars_sc_gemini_key';
const INTERACTIONS_ENDPOINT = 'https://generativelanguage.googleapis.com/v1beta/interactions';
const INTERACTIONS_REVISION = '2026-05-20';
const DEFAULT_GEMINI_MODEL = 'gemini-3.8-flash';

/**
 * Extract trailing text from an Interactions API response (`steps` array).
 * @param {object} interaction
 * @returns {string}
 */
export function extractInteractionOutputText(interaction) {
  if (!interaction || typeof interaction !== 'object') return '';
  if (typeof interaction.output_text === 'string' && interaction.output_text.trim()) {
    return interaction.output_text.trim();
  }
  const steps = Array.isArray(interaction.steps) ? interaction.steps : [];
  const chunks = [];
  for (let i = steps.length - 1; i >= 0; i--) {
    const step = steps[i];
    if (step?.type !== 'model_output') {
      if (chunks.length > 0) break;
      continue;
    }
    const content = Array.isArray(step.content) ? step.content : [];
    const stepTexts = content
      .filter((part) => part?.type === 'text' && typeof part.text === 'string')
      .map((part) => part.text);
    if (stepTexts.length) {
      chunks.unshift(stepTexts.join(''));
    }
  }
  return chunks.join('\n').trim();
}

export class SpaceCampCopilot {
  constructor() {
    this.lastInteractionId = null;
    this._ingestUrlHashKey();
  }

  /**
   * Support optional `#gemini_key=...` hash provisioning for classroom QR codes/links.
   * Hash fragments are never sent over HTTP; we immediately scrub it from the address bar.
   */
  _ingestUrlHashKey() {
    if (typeof window === 'undefined' || !window.location.hash) return;
    const rawHash = window.location.hash.slice(1);
    if (!rawHash.includes('gemini_key=')) return;
    try {
      const params = new URLSearchParams(rawHash);
      const candidate = (params.get('gemini_key') || '').trim();
      if (candidate.length >= 20) {
        sessionStorage.setItem(GEMINI_KEY_STORAGE, candidate);
        params.delete('gemini_key');
        const remaining = params.toString();
        const cleanUrl = `${window.location.pathname}${window.location.search}${remaining ? `#${remaining}` : ''}`;
        window.history.replaceState(null, '', cleanUrl);
      }
    } catch {
      // Ignore malformed hash fragments
    }
  }

  getApiKey() {
    if (typeof window === 'undefined') return '';
    try {
      return (
        sessionStorage.getItem(GEMINI_KEY_STORAGE) ||
        localStorage.getItem(GEMINI_KEY_STORAGE) ||
        ''
      ).trim();
    } catch {
      return '';
    }
  }

  hasApiKey() {
    return this.getApiKey().length >= 20;
  }

  setApiKey(rawKey, persistAcrossTabs = false) {
    const cleaned = String(rawKey || '').trim();
    this.lastInteractionId = null;
    if (typeof window === 'undefined') return;
    try {
      if (!cleaned) {
        sessionStorage.removeItem(GEMINI_KEY_STORAGE);
        localStorage.removeItem(GEMINI_KEY_STORAGE);
        return;
      }
      sessionStorage.setItem(GEMINI_KEY_STORAGE, cleaned);
      if (persistAcrossTabs) {
        localStorage.setItem(GEMINI_KEY_STORAGE, cleaned);
      }
    } catch {
      // Ignore storage restrictions
    }
  }

  resetConversation() {
    this.lastInteractionId = null;
  }

  /**
   * Build system instructions grounded in live JSMARS map, sensor, and mission state.
   * @param {object} ctx
   * @param {string} lang
   * @returns {string}
   */
  buildSystemInstruction(ctx = {}, lang = 'en') {
    const languageDirective = lang === 'fr'
      ? 'Respond strictly in clear, natural French suitable for 16-18 year old Space Camp analysts.'
      : 'Respond strictly in clear, energetic, grounded English suitable for 16-18 year old Space Camp analysts.';

    const lines = [
      'You are CAPCOM, the Space Data Flight Director inside "Mission JSMARS: Space Data Analysts" (created by Dr. Moulay Anwar Sounny-Slitine).',
      languageDirective,
      'Keep replies concise (3 to 5 short sentences max, under 110 words). Use concrete remote-sensing and planetary GIS reasoning, never vague fluff, and never use em dashes.',
      'CRITICAL GAME INTEGRITY RULE: In Round 1 (Earth Observation land-cover classification), give Socratic spectral/texture clues (canopy reflectance, rectilinear street grids, geometric farm pivots, sand-dune slipfaces) WITHOUT naming the exact multiple-choice answer label directly.',
      `Live JSMARS Telemetry: Active Body = ${String(ctx.body || 'earth').toUpperCase()}, Map Center = (${Number(ctx.lat || 0).toFixed(3)} deg, ${Number(ctx.lon || 0).toFixed(3)} deg), Zoom = ${ctx.zoom ?? 4}.`,
      `Active Layers = ${(ctx.activeLayers && ctx.activeLayers.length) ? ctx.activeLayers.join(', ') : 'Default basemap'}.`,
      `Active Round = Round ${ctx.roundNumber || 1} (${ctx.roundTitle || ''}), Player Score = ${ctx.totalScore || 0} / 600 pts.`
    ];

    if (ctx.roverSummary) {
      const r = ctx.roverSummary;
      lines.push(
        `Round 3 Mars Rover Telemetry: Distance = ${r.distanceKm} km, Max MOLA Slope = ${r.maxSlopeDeg} deg, Total Climb = ${r.totalClimbM} m, Reached Goal = ${r.reachedGoal}, Route Score = ${r.totalPoints}/75 pts.`
      );
    }
    if (ctx.lunarSummary) {
      const l = ctx.lunarSummary;
      lines.push(
        `Round 4 NASA LOLA Lunar South Pole Telemetry: Slope = ${l.effectiveSlopeDeg} deg, Inside PSR Shadow = ${l.inPsr}, SlopePts = ${l.slopePoints}/45, SunPts = ${l.sunPoints}/45, EarthCommsPts = ${l.earthPoints}/30, IcePts = ${l.icePoints}/30 (Total = ${l.totalPoints}/150 pts).`
      );
    }
    return lines.join('\n');
  }

  /**
   * Deterministic live-telemetry briefing when no Gemini API key is configured.
   * Uses real JSMARS viewport coordinates, active layers, MOLA slopes, and LOLA grids.
   * @param {object} ctx
   * @param {string} promptText
   * @param {string} lang
   * @returns {string}
   */
  buildLocalTelemetryReply(ctx = {}, promptText = '', lang = 'en') {
    const body = String(ctx.body || 'earth').toLowerCase();
    const latStr = `${Math.abs(Number(ctx.lat || 0)).toFixed(2)}°${Number(ctx.lat || 0) >= 0 ? 'N' : 'S'}`;
    const lonStr = `${Math.abs(Number(ctx.lon || 0)).toFixed(2)}°${Number(ctx.lon || 0) >= 0 ? 'E' : 'W'}`;
    const rNum = Number(ctx.roundNumber) || 1;
    const layers = (ctx.activeLayers && ctx.activeLayers.length) ? ctx.activeLayers.join(', ') : 'basemap';

    if (rNum === 3 && ctx.roverSummary) {
      const r = ctx.roverSummary;
      if (lang === 'fr') {
        return `Télémétrie Rover MOLA (${r.distanceKm} km, pente max ${r.maxSlopeDeg}°, dénivelé +${r.totalClimbM} m) : ${
          r.maxSlopeDeg <= 5.5
            ? 'excellent tracé à faible pente dans le chenal ! Vérifiez que le dernier point arrive bien dans le rayon cible de Nili.'
            : 'attention, votre itinéraire franchit un escarpement raide (> 6°). Contournez le rempart ouest par le chenal de Neretva Vallis pour gagner plus de points de sécurité.'
        }`;
      }
      return `MOLA Rover Telemetry (${r.distanceKm} km, max slope ${r.maxSlopeDeg}°, +${r.totalClimbM} m climb): ${
        r.maxSlopeDeg <= 5.5
          ? 'clean low-angle traverse through the valley channel! Make sure your final waypoint lands inside the Nili target zone.'
          : 'your rover hits a steep scarp (> 6°). Try routing north into the Neretva Vallis outflow channel to lower max slope and boost your engineering score.'
      }`;
    }

    if (rNum === 4 && ctx.lunarSummary) {
      const l = ctx.lunarSummary;
      if (lang === 'fr') {
        return `Analyse LOLA Pôle Sud (${l.totalPoints}/150 pts, pente ${l.effectiveSlopeDeg}°) : ${l.adviceFr}`;
      }
      return `LOLA South Pole Readout (${l.totalPoints}/150 pts, slope ${l.effectiveSlopeDeg}°): ${l.adviceEn}`;
    }

    if (rNum === 1) {
      if (lang === 'fr') {
        return `Vue actuelle sur ${body.toUpperCase()} (${latStr}, ${lonStr} · couches : ${layers}). Observez la texture Sentinel-2 à 10 m : une canopée sombre irrégulière suit le relief montagneux, le bâti forme une trame routière grise/blanche, les cultures irriguées découpent des parcelles géométriques et le désert montre des crêtes dunaires ocre. Utilisez aussi Mesure (Distance/Surface) pour +12 pts !`;
      }
      return `Current ${body.toUpperCase()} lock (${latStr}, ${lonStr} · layers: ${layers}). Inspect the 10 m Sentinel-2 texture closely: mountain canopy looks dark green and irregular, cities show bright street grids, irrigated farms form crisp geometric plots, and sand seas show ochre dune ridges. Don't forget Tools -> Measurement for +12 bonus pts!`;
    }

    if (rNum === 2) {
      if (lang === 'fr') {
        return `Capteur centré à (${latStr}, ${lonStr}) sur ${body.toUpperCase()} (${layers}). Activez « MOLA Colorized Elevation » dans Layer Manager : les édifices volcaniques géants ressortent en rouge/blanc (+21 km) à l'ouest de Tharsis, tandis que le grand rift tectonique s'ouvre en bleu/violet (-7 km) à l'est. Tracez ensuite un profil dans Tools -> Profiles !`;
      }
      return `Scanner at (${latStr}, ${lonStr}) on ${body.toUpperCase()} (${layers}). Turn on 'MOLA Colorized Elevation' in Layer Manager: giant shield volcanoes glow white/red (+21 km) in western Tharsis, while Mars's deepest tectonic rift canyon drops into blue/purple (-7 km) further east. Then slice a cross-section in Tools -> Profiles!`;
    }

    if (lang === 'fr') {
      return `Position (${latStr}, ${lonStr}) sur ${body.toUpperCase()} (${layers}). Sur Europe, visez Conamara Chaos (8.85°N, 86.15°E), activez la mosaïque Galileo SSI 9 m/px dans Layer Manager, puis cartographiez un radeau de glace avec Shapes ou passez en vue Globe 3D !`;
    }
    return `Tracking (${latStr}, ${lonStr}) on ${body.toUpperCase()} (${layers}). On Europa, lock onto Conamara Chaos (8.85°N, 86.15°E), enable the 9 m/px Galileo SSI mosaic in Layer Manager, then outline a rotated ice raft with Shapes or toggle the 3D Globe!`;
  }

  /**
   * Ask CapCom a question or run a contextual mission analysis.
   * Uses Gemini 3.8 Flash via `/v1beta/interactions` if an API key is set,
   * otherwise returns live telemetry diagnostics cleanly.
   *
   * @param {string} userPrompt
   * @param {object} telemetryContext
   * @param {string} [lang='en']
   * @returns {Promise<{ text: string, source: 'gemini' | 'telemetry', model?: string, error?: string }>}
   */
  async ask(userPrompt, telemetryContext = {}, lang = 'en') {
    const prompt = String(userPrompt || '').trim();
    const apiKey = this.getApiKey();

    if (!apiKey) {
      return {
        text: this.buildLocalTelemetryReply(telemetryContext, prompt, lang),
        source: 'telemetry'
      };
    }

    const bodyPayload = {
      model: DEFAULT_GEMINI_MODEL,
      system_instruction: this.buildSystemInstruction(telemetryContext, lang),
      input: prompt || (lang === 'fr' ? 'Analyse ma vue actuelle et guide-moi.' : 'Analyze my current JSMARS view and advise.'),
      generation_config: {
        thinking_level: 'minimal',
        max_output_tokens: 420
      }
    };

    if (this.lastInteractionId) {
      bodyPayload.previous_interaction_id = this.lastInteractionId;
    }

    try {
      const resp = await fetch(INTERACTIONS_ENDPOINT, {
        method: 'POST',
        headers: {
          'x-goog-api-key': apiKey,
          'Content-Type': 'application/json',
          'Api-Revision': INTERACTIONS_REVISION
        },
        body: JSON.stringify(bodyPayload)
      });

      if (!resp.ok) {
        const errText = await resp.text().catch(() => '');
        // If previous_interaction_id expired, clear it and retry once statelessly
        if (this.lastInteractionId && (resp.status === 400 || resp.status === 404)) {
          this.lastInteractionId = null;
          delete bodyPayload.previous_interaction_id;
          const retry = await fetch(INTERACTIONS_ENDPOINT, {
            method: 'POST',
            headers: {
              'x-goog-api-key': apiKey,
              'Content-Type': 'application/json',
              'Api-Revision': INTERACTIONS_REVISION
            },
            body: JSON.stringify(bodyPayload)
          });
          if (retry.ok) {
            const retryData = await retry.json();
            if (retryData?.id) this.lastInteractionId = retryData.id;
            const retryOut = extractInteractionOutputText(retryData);
            if (retryOut) {
              return { text: retryOut, source: 'gemini', model: DEFAULT_GEMINI_MODEL };
            }
          }
        }
        console.warn('[SpaceCampCopilot] Gemini API HTTP error:', resp.status, errText);
        return {
          text: this.buildLocalTelemetryReply(telemetryContext, prompt, lang),
          source: 'telemetry',
          error: `Gemini HTTP ${resp.status}`
        };
      }

      const data = await resp.json();
      if (data?.id) {
        this.lastInteractionId = data.id;
      }
      const out = extractInteractionOutputText(data);
      if (!out) {
        return {
          text: this.buildLocalTelemetryReply(telemetryContext, prompt, lang),
          source: 'telemetry'
        };
      }
      return {
        text: out,
        source: 'gemini',
        model: DEFAULT_GEMINI_MODEL
      };
    } catch (err) {
      console.warn('[SpaceCampCopilot] Gemini request fallback:', err?.message || err);
      return {
        text: this.buildLocalTelemetryReply(telemetryContext, prompt, lang),
        source: 'telemetry',
        error: err?.message || 'Network fallback'
      };
    }
  }
}
