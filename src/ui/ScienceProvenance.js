/**
 * @module ScienceProvenance
 * @description Shared Model vs Live/Measured provenance markup (AGENTS.md §9.D).
 * Badges use visible text, not color alone. Dynamic values are escaped.
 */

export const PROVENANCE_KIND = {
  MODEL: 'model',
  LIVE: 'live',
  FALLBACK: 'fallback'
};

export const PROVENANCE_LABELS = {
  model: 'Model',
  live: 'Live / Measured',
  fallback: 'Offline Fallback'
};

/** Authoritative archive links for measured data that JSMARS does not fetch. */
export const SCIENCE_ARCHIVES = {
  SHARAD: {
    href: 'https://pds-geosciences.wustl.edu/missions/mro/sharad.htm',
    label: 'PDS SHARAD archive'
  },
  MARSIS: {
    href: 'https://pds-geosciences.wustl.edu/missions/mars_express/marsis.htm',
    label: 'PDS MARSIS archive'
  },
  CRISM: {
    href: 'https://pds-geosciences.wustl.edu/missions/mro/crism.htm',
    label: 'PDS CRISM archive'
  },
  THEMIS: {
    href: 'https://pds-geosciences.wustl.edu/missions/odyssey/themis.html',
    label: 'PDS THEMIS archive'
  },
  LMD_MCD: {
    href: 'https://www-mars.lmd.jussieu.fr/mcd_python/',
    label: 'LMD MCD portal'
  },
  EUROPA_REASON: {
    href: 'https://europa.nasa.gov/spacecraft/instruments/reason/',
    label: 'Europa Clipper REASON'
  }
};

/**
 * Escape text interpolated into provenance HTML.
 * @param {string} value
 * @returns {string}
 */
export function escapeProvenanceText(value) {
  return String(value == null ? '' : value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

/**
 * Build a compact provenance banner (title, kind badge, body, optional archive links).
 * @param {object} options
 * @param {string} [options.kind='model']
 * @param {string} [options.titleId]
 * @param {string} options.title
 * @param {string} [options.body]
 * @param {string} [options.bodyId]
 * @param {Array<{href: string, label: string}>} [options.links]
 * @param {string} [options.linksId]
 * @param {string} [options.source]
 * @param {string} [options.sourceId]
 * @returns {string} HTML
 */
export function provenanceBannerHTML(options = {}) {
  const kind = options.kind && PROVENANCE_LABELS[options.kind]
    ? options.kind
    : PROVENANCE_KIND.MODEL;
  const badgeLabel = PROVENANCE_LABELS[kind];
  const title = escapeProvenanceText(options.title || 'Simulation');
  const body = options.body ? escapeProvenanceText(options.body) : '';
  const source = options.source ? escapeProvenanceText(options.source) : '';
  const titleIdAttr = options.titleId ? ` id="${escapeProvenanceText(options.titleId)}"` : '';
  const bodyIdAttr = options.bodyId ? ` id="${escapeProvenanceText(options.bodyId)}"` : '';
  const sourceIdAttr = options.sourceId ? ` id="${escapeProvenanceText(options.sourceId)}"` : '';
  const linksIdAttr = options.linksId ? ` id="${escapeProvenanceText(options.linksId)}"` : '';

  const links = Array.isArray(options.links) ? options.links : [];
  const linksHtml = links.map((link) => {
    const href = escapeProvenanceText(link.href || '');
    const label = escapeProvenanceText(link.label || link.href || 'Archive');
    return `<a class="science-provenance__link" href="${href}" target="_blank" rel="noopener noreferrer">${label}</a>`;
  }).join('');

  return `
    <div class="science-provenance" role="note" data-provenance="${kind}">
      <div class="science-provenance__header">
        <span class="science-provenance__title"${titleIdAttr}>${title}</span>
        <span class="science-provenance__badge science-provenance__badge--${kind}">${badgeLabel}</span>
      </div>
      ${body ? `<p class="science-provenance__body"${bodyIdAttr}>${body}</p>` : ''}
      ${source ? `<p class="science-provenance__source"${sourceIdAttr}>${source}</p>` : ''}
      ${linksHtml ? `<div class="science-provenance__links"${linksIdAttr}>${linksHtml}</div>` : ''}
    </div>
  `;
}

/**
 * Update an existing banner's kind badge (text + class, not color-only).
 * @param {HTMLElement} root
 * @param {string} kind
 * @param {string} [badgeLabel]
 */
export function applyProvenanceKind(root, kind, badgeLabel) {
  if (!root) return;
  const nextKind = PROVENANCE_LABELS[kind] ? kind : PROVENANCE_KIND.MODEL;
  const wrap = root.classList.contains('science-provenance')
    ? root
    : root.querySelector('.science-provenance');
  if (wrap) wrap.setAttribute('data-provenance', nextKind);
  const badge = (wrap || root).querySelector('.science-provenance__badge');
  if (badge) {
    badge.className = `science-provenance__badge science-provenance__badge--${nextKind}`;
    badge.textContent = badgeLabel || PROVENANCE_LABELS[nextKind];
  }
}
