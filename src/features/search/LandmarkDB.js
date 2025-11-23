export const LandmarkDB = [
    { name: "Olympus Mons", lat: 18.65, lng: 226.2, type: "Mountain" },
    { name: "Gale Crater", lat: -5.4, lng: 137.8, type: "Crater" },
    { name: "Valles Marineris", lat: -14.0, lng: 290.0, type: "Canyon" },
    { name: "Jezero Crater", lat: 18.38, lng: 77.58, type: "Crater" },
    { name: "Hellas Planitia", lat: -42.7, lng: 70.0, type: "Basin" },
    { name: "Tharsis Montes", lat: 1.0, lng: 247.0, type: "Volcano" },
    { name: "Elysium Mons", lat: 25.0, lng: 147.0, type: "Volcano" },
    { name: "Victoria Crater", lat: -2.05, lng: 354.5, type: "Crater" },
    { name: "Gusev Crater", lat: -14.6, lng: 175.4, type: "Crater" },
    { name: "Meridiani Planum", lat: 0.2, lng: 357.5, type: "Plain" }
];

export function searchLandmarks(query) {
    if (!query) return [];
    const lowerQuery = query.toLowerCase();
    return LandmarkDB.filter(l => l.name.toLowerCase().includes(lowerQuery));
}
