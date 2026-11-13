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
      'You are CAPCOM, the Space Data Flight Director inside "Mission JSMARS: Space Data Analysts" (co-created by Dr. Moulay Anwar Sounny-Slitine and Sara Bensalem).',
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
    const q = String(promptText || '').toLowerCase();

    const isCritiqueIntent = /(critique|évalue|evaluer|rover|traverse|jezero|artemis|lola|lunar|lune|site|improve|score higher|améliorer)/i.test(q);
    const isEuropaQuestion = /(ice|raft|broken|rotat|puzzle|chaos|conamara|europa|europe|ocean|océan|galileo|tidal|glace|iceberg|fractur)/i.test(q);
    const isMarsQuestion = /(olympus|volcan|valles|marineris|canyon|tharsis|mola|laser|plate|tectonic)/i.test(q);
    const isMoonQuestion = /(psr|shadow|permanently|water|shackleton|malapert|gerlache|tilt|ombre|polaire|eau)/i.test(q);
    const isClueIntent = /(clue|indice|physics|physique|sentinel|spectral|chlorophyll|noor|morocco|maroc)/i.test(q);

    // 1. Cross-round Engineering Critique (works from any active round tab)
    if (isCritiqueIntent) {
      const parts = [];
      if (ctx.roverSummary && (rNum === 3 || !ctx.lunarSummary || /rover|jezero|mola|traverse/i.test(q))) {
        const r = ctx.roverSummary;
        parts.push(lang === 'fr'
          ? `Télémétrie Rover MOLA (${r.distanceKm} km, pente max ${r.maxSlopeDeg}°, dénivelé +${r.totalClimbM} m, ${r.totalPoints}/75 pts) : ${
              r.maxSlopeDeg <= 5.5
                ? 'excellent tracé à faible pente dans le chenal ! Vérifiez que le dernier point arrive bien dans la zone cible de Nili.'
                : 'votre itinéraire franchit un escarpement raide (> 6°). Contournez le rempart ouest par le chenal de Neretva Vallis (~18.52°N, 77.33°E) pour réduire la pente.'
            }`
          : `MOLA Rover Telemetry (${r.distanceKm} km, max slope ${r.maxSlopeDeg}°, +${r.totalClimbM} m climb, ${r.totalPoints}/75 pts): ${
              r.maxSlopeDeg <= 5.5
                ? 'clean low-angle traverse through the valley channel! Make sure your final waypoint lands inside the Nili target zone.'
                : 'your rover crosses a steep scarp (> 6°). Route through the Neretva Vallis outflow channel (~18.52°N, 77.33°E) to lower max slope and gain safety points.'
            }`
        );
      }
      if (ctx.lunarSummary && (rNum === 4 || !ctx.roverSummary || /artemis|lola|lunar|lune|site|psr/i.test(q))) {
        const l = ctx.lunarSummary;
        parts.push(lang === 'fr'
          ? `Analyse LOLA Pôle Sud (${l.totalPoints}/150 pts, pente ${l.effectiveSlopeDeg}°) : ${l.adviceFr}`
          : `LOLA South Pole Readout (${l.totalPoints}/150 pts, slope ${l.effectiveSlopeDeg}°): ${l.adviceEn}`
        );
      }
      if (parts.length > 0) {
        return parts.join(' ');
      }
      return lang === 'fr'
        ? `Aucune télémétrie d'ingénierie encore enregistrée pour l'instant : tracez et évaluez un itinéraire de rover dans la Manche 3 (Jezero) ou cliquez sur une crête lunaire LOLA dans la Manche 4 (Artemis).`
        : `No engineering traverse or landing site scored yet! Draw and score a Mars rover path in Round 3 (Jezero) or click a polar ridge inside Round 4's Lunar Base Site Planner to get live engineering feedback.`;
    }

    // 2. Europa / Conamara Chaos / Ice Rafts
    if (isEuropaQuestion) {
      if (lang === 'fr') {
        return `À Conamara Chaos (${latStr}, ${lonStr} sur ${body.toUpperCase()}), le réchauffement par effet de marée de Jupiter alimente un océan d'eau salée liquide (~2x nos océans, prouvé par le magnétomètre de Galileo) sous la banquise. Des panaches chauds ont partiellement fondu la croûte de glace par-dessous : des blocs kilométriques se sont brisés, ont dérivé et pivoté dans une matrice fluide avant de regeler sur place !`;
      }
      return `At Conamara Chaos (${latStr}, ${lonStr} on ${body.toUpperCase()}), Jupiter's gravitational flexing drives tidal heating in a global subsurface saltwater ocean (~2x Earth's oceans, proved by Galileo's magnetometer). Warm upwelling plumes partially melted the ice shell from below, breaking kilometer-sized crust plates loose so they drifted and rotated like puzzle pieces before refreezing in a slushy matrix!`;
    }

    // 3. Mars Volcanism / Tectonic Rift / MOLA
    if (isMarsQuestion) {
      if (lang === 'fr') {
        return `Contrairement à la Terre, Mars ne possède pas de tectonique des plaques active : la croûte reste fixe au-dessus d'un point chaud, accumulant Olympus Mons jusqu'à +21,9 km d'altitude, tandis que le soulèvement de Tharsis a fissuré Valles Marineris sur 4 000 km (-7 km). L'altimètre MOLA mesure ce relief au mètre près en chronométrant le retour d'impulsions laser infrarouges à 1064 nm.`;
      }
      return `Because Mars lacks active Earth-style plate tectonics, crust sat stationary over a mantle hotspot, stacking lava flows to build Olympus Mons to +21.9 km while Tharsis uplift tore Valles Marineris 4,000 km long and 7 km deep. MGS MOLA measured that 29 km relief span to ~1 m precision by timing 1064 nm infrared laser pulses from orbit.`;
    }

    // 4. Lunar South Pole / PSR Cold Traps
    if (isMoonQuestion) {
      if (lang === 'fr') {
        return `Avec seulement 1,54° d'inclinaison axiale, le Soleil rase l'horizon au Pôle Sud lunaire : les crêtes élevées (Shackleton Rim, Connecting Ridge) reçoivent un ensoleillement quasi continu et une vue directe sur la Terre, tandis que le fond des cratères voisins reste en Ombre Permanente (PSR, < 110 K), piégeant de la glace d'eau accessible aux rovers.`;
      }
      return `Because the Moon tilts only 1.54° on its axis, sunlight grazes the South Pole nearly horizontally: high crater rims (Shackleton Rim, Connecting Ridge) stay bathed in sunlight with Earth radio line-of-sight, while adjacent crater floors remain in Permanent Shadow (PSRs below 110 K) that cold-trap water ice within 0.2 to 2.5 km of rover landing zones.`;
    }

    // 5. Round-specific telemetry & Sensor Clues (without spoiling Round 1)
    if (rNum === 1 || isClueIntent && body === 'earth') {
      if (lang === 'fr') {
        return `Vue actuelle sur ${body.toUpperCase()} (${latStr}, ${lonStr} · couches : ${layers}). Observez la texture Sentinel-2 à 10 m : une canopée sombre irrégulière absorbe le rouge sur le relief montagneux, le bâti forme une trame routière claire, les cultures irriguées découpent des parcelles géométriques et les dunes montrent des crêtes ocre riches en oxydes de fer. Utilisez aussi Measurement pour +12 pts !`;
      }
      return `Current ${body.toUpperCase()} lock (${latStr}, ${lonStr} · layers: ${layers}). Inspect the 10 m Sentinel-2 texture closely: healthy mountain canopy absorbs visible red light and looks dark green and rough, urban centers show bright rectilinear street grids, irrigated farms form crisp rectangular plots, and sand seas show ochre dune ridges. Don't forget Measurement for +12 bonus pts!`;
    }

    if (rNum === 2) {
      if (lang === 'fr') {
        return `Capteur centré à (${latStr}, ${lonStr}) sur ${body.toUpperCase()} (${layers}). Activez « MOLA Colorized Elevation » dans Layer Manager : les édifices volcaniques géants ressortent en rouge/blanc (+21 km) à l'ouest de Tharsis, tandis que le grand rift tectonique s'ouvre en bleu/violet (-7 km) à l'est. Tracez ensuite un profil dans Tools -> Profiles !`;
      }
      return `Scanner at (${latStr}, ${lonStr}) on ${body.toUpperCase()} (${layers}). Turn on 'MOLA Colorized Elevation' in Layer Manager: giant shield volcanoes glow white/red (+21 km) in western Tharsis, while Mars's deepest tectonic rift canyon drops into blue/purple (-7 km) further east. Then slice a cross-section in Tools -> Profiles!`;
    }

    if (rNum === 3) {
      if (lang === 'fr') {
        return `Secteur Jezero à (${latStr}, ${lonStr}) sur ${body.toUpperCase()} (${layers}). Pour limiter les pentes rover (< 5°), suivez l'ancien chenal fluvial de Neretva Vallis qui entaille le rempart ouest (~18.52°N, 77.33°E) avant de rejoindre le plateau de Nili, puis cliquez sur « Évaluer l'Itinéraire ».`;
      }
      return `Jezero sector at (${latStr}, ${lonStr}) on ${body.toUpperCase()} (${layers}). To keep rover slopes safe (< 5°), route west into the ancient Neretva Vallis river channel (~18.52°N, 77.33°E) where water breached the crater rim before climbing to Nili Highlands, then click Score Rover Plan!`;
    }

    if (rNum === 4) {
      if (lang === 'fr') {
        return `Secteur lunaire à (${latStr}, ${lonStr}) (${layers}). Lancez le Lunar Base Site Planner pour interroger les rasters NASA LOLA : visez une crête plate (Pente <= 5°), très ensoleillée (>= 75 %), visible depuis la Terre (>= 50 %) et proche (0,2 à 2,5 km) d'un piège à glace PSR sans atterrir dans l'ombre.`;
      }
      return `Lunar view at (${latStr}, ${lonStr}) (${layers}). Launch the Lunar Base Site Planner to sample live NASA LOLA grids: aim for a flat ridge (Slope <= 5°), high solar power (>= 75%), direct Earth comms (>= 50%), and rover access (0.2 to 2.5 km) to a PSR ice trap without landing inside the shadow.`;
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
