/**
 * @module LandmarkDB
 * @description Static database of notable Mars surface landmarks.
 *
 * Longitudes are normalized to the -180 to 180 range so they work
 * correctly with Leaflet's default CRS. The original IAU values use
 * 0-360 East longitude; entries above 180 are converted by subtracting 360.
 */
export const LandmarkDB = [
    { name: "Olympus Mons",     lat:  18.65, lng: 226.2 - 360,  type: "Mountain" },  // -133.8
    { name: "Gale Crater",      lat:  -5.4,  lng: 137.8,         type: "Crater"   },
    { name: "Valles Marineris",  lat: -14.0,  lng: 290.0 - 360,  type: "Canyon"   },  // -70.0
    { name: "Jezero Crater",    lat:  18.38, lng:  77.58,        type: "Crater"   },
    { name: "Hellas Planitia",  lat: -42.7,  lng:  70.0,         type: "Basin"    },
    { name: "Tharsis Montes",   lat:   1.0,  lng: 247.0 - 360,  type: "Volcano"  },  // -113.0
    { name: "Elysium Mons",     lat:  25.0,  lng: 147.0,         type: "Volcano"  },
    { name: "Victoria Crater",  lat:  -2.05, lng: 354.5 - 360,  type: "Crater"   },  //  -5.5
    { name: "Gusev Crater",     lat: -14.6,  lng: 175.4,         type: "Crater"   },
    { name: "Meridiani Planum", lat:   0.2,  lng: 357.5 - 360,  type: "Plain"    }   //  -2.5
];

/**
 * Search the landmark database by name (case-insensitive substring match).
 * @param {string} query - Search string.
 * @returns {Array<object>} Matching landmark records.
 */
export function searchLandmarks(query) {
    if (!query) return [];
    const lowerQuery = query.toLowerCase();
    return LandmarkDB.filter(l => l.name.toLowerCase().includes(lowerQuery));
}
