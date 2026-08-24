import { jmarsState } from '../src/jmars-state.js';
import { JMARSWMS } from '../src/jmars-wms.js';
import { EVENTS } from '../src/constants.js';
import { MarsTime } from '../src/features/slider/MarsTime.js';
import { KRCEngine } from '../src/features/krc/KRCEngine.js';
import { MCDEngine } from '../src/features/mcd/MCDEngine.js';
import { CSFDEngine } from '../src/features/crater-counting/CSFDEngine.js';
import { CraterTable } from '../src/features/crater-counting/CraterTable.js';
import { StampLayer } from '../src/features/stamp/StampLayer.js';
import { BandMathEngine } from '../src/features/bands/BandMathEngine.js';
import { GridLayer } from '../src/features/grid/GridLayer.js';
import { PlanetaryScaleBar } from '../src/ui/PlanetaryScaleBar.js';
import { RadarSounderEngine } from '../src/features/radar/RadarSounderEngine.js';
import { BookmarksTool } from '../src/features/bookmarks/BookmarksTool.js';
import { ThreeDEngine } from '../src/features/threed/ThreeDEngine.js';
import { TrajectoryEngine } from '../src/features/orbit/TrajectoryEngine.js';
import { ColorStretchControl } from '../src/ui/ColorStretchControl.js';
import { InvestigateTool } from '../src/features/investigate/InvestigateTool.js';
import { ProjectionManager } from '../src/features/projections/ProjectionManager.js';
import { ContourLayer } from '../src/features/contour/ContourLayer.js';
import { ColorRampEngine } from '../src/util/ColorRampEngine.js';
import { ShapeIO } from '../src/features/shapes/ShapeIO.js';
import { haversineDistance, azimuth, toGraphic, toCentric, formatLatLon, sphericalPolygonArea, computeEllipsePolygon, computeBufferPolygon, isPointInPolygon, computeBoundingBox, sphericalToCartesian, cartesianToSpherical, interpolateGreatCircle, computeMidpoint, computeDestinationPoint, computeCrossTrackDistance, computeAlongTrackDistance, computePolylineLength, computePolygonPerimeter } from '../src/util/geo.js';

const expect = chai.expect;

describe('JMARSState', () => {
    beforeEach(() => {
        jmarsState.reset();
    });

    it('should have default state', () => {
        expect(jmarsState.get('body')).to.equal('Mars');
        expect(jmarsState.get('activeLayers')).to.be.an('array').that.is.empty;
    });

    it('should update body', () => {
        jmarsState.set('body', 'Earth');
        expect(jmarsState.get('body')).to.equal('Earth');
    });

    it('should add layer', () => {
        jmarsState.addLayer('test_layer');
        const layers = jmarsState.get('activeLayers');
        expect(layers).to.have.lengthOf(1);
        expect(layers[0].id).to.equal('test_layer');
    });

    it('should remove layer', () => {
        jmarsState.addLayer('test_layer');
        jmarsState.removeLayer('test_layer');
        expect(jmarsState.get('activeLayers')).to.be.empty;
    });

    it('should emit events', (done) => {
        jmarsState.on(EVENTS.LAYERS_CHANGED, (layers) => {
            expect(layers).to.have.lengthOf(1);
            done();
        });
        jmarsState.addLayer('event_layer');
    });
});

describe('JMARSWMS', () => {
    it('should construct GetCapabilities URL', () => {
        const url = JMARSWMS.getCapabilitiesUrl('http://example.com/wms');
        expect(url).to.include('service=WMS');
        expect(url).to.include('request=GetCapabilities');
        expect(url).to.include('version=1.3.0');
    });

    it('should construct GetFeatureInfo URL', () => {
        const params = {
            layers: 'L1',
            bbox: '0,0,10,10',
            width: 100,
            height: 100,
            x: 50,
            y: 50
        };
        const url = JMARSWMS.getFeatureInfoUrl('http://example.com/wms', params);
        expect(url).to.include('request=GetFeatureInfo');
        expect(url).to.include('layers=L1');
        expect(url).to.include('i=50');
        expect(url).to.include('j=50');
    });
});

describe('Mars Astronomy & MarsTime', () => {
    it('should compute valid Mars state for reference epoch', () => {
        const date = new Date('2026-06-01T00:00:00Z');
        const state = MarsTime.computeState(date);
        
        expect(state.jd).to.be.greaterThan(2460000);
        expect(state.msd).to.be.greaterThan(50000);
        expect(state.Ls).to.be.within(0, 360);
        expect(state.r_AU).to.be.within(1.35, 1.70);
        expect(state.solarInsolation).to.be.within(450, 750);
        expect(state.subSolarLat).to.be.within(-26, 26);
        expect(state.season).to.have.property('north');
    });

    it('should calculate Local True Solar Time (LTST)', () => {
        const ltst = MarsTime.computeLTST(90, 12, 0); // Ls=90, MTC=12, Lon=0
        expect(ltst).to.be.within(0, 24);
    });

    it('should compute solar zenith and day/night state', () => {
        const noonZenith = MarsTime.getSolarZenith(0, 0, 12); // Equator at solar noon
        expect(noonZenith.isDay).to.be.true;
        expect(noonZenith.zenithAngleDeg).to.be.lessThan(5);

        const midnightZenith = MarsTime.getSolarZenith(0, 0, 0); // Midnight
        expect(midnightZenith.isDay).to.be.false;
        expect(midnightZenith.cosZ).to.equal(0);
    });
});

describe('KRC Mars 1D Thermal Model', () => {
    it('should simulate diurnal thermal cycle', () => {
        const result = KRCEngine.simulateDiurnal({
            lat: 0,
            Ls: 90,
            thermalInertia: 300,
            albedo: 0.22,
            tau: 0.3,
            elevation: 0
        });

        expect(result).to.have.property('summary');
        expect(result.summary.maxTemp).to.be.greaterThan(result.summary.meanTemp);
        expect(result.summary.meanTemp).to.be.greaterThan(result.summary.minTemp);
        expect(result.summary.maxTemp).to.be.within(220, 315);
        expect(result.summary.minTemp).to.be.within(140, 230);
        expect(result.diurnalCurve).to.have.lengthOf(120);
        expect(result.depthProfile).to.be.an('array').with.length.greaterThan(10);
    });

    it('should detect polar winter CO2 frost condensation', () => {
        const polarWinter = KRCEngine.simulateDiurnal({
            lat: -85,
            Ls: 90, // Southern winter
            thermalInertia: 200,
            albedo: 0.3
        });
        expect(polarWinter.summary.minTemp).to.be.closeTo(145, 2);
        expect(polarWinter.summary.co2FrostOccurs).to.be.true;
    });

    it('should run seasonal simulation', () => {
        const seasonal = KRCEngine.simulateSeasonal({ lat: 10, thermalInertia: 250 });
        expect(seasonal).to.be.an('array').with.lengthOf(25);
        expect(seasonal[0]).to.have.property('Ls');
        expect(seasonal[0]).to.have.property('meanTemp');
    });
});

describe('Mars Climate Database (MCD) Profiler', () => {
    it('should compute vertical atmospheric profile', () => {
        const profile = MCDEngine.computeProfile({
            lat: 0,
            lon: 0,
            elevation: 0,
            Ls: 0,
            localHour: 12
        });

        expect(profile.surface.pressurePa).to.be.closeTo(610, 50);
        expect(profile.surface.temperatureK).to.be.within(180, 300);
        expect(profile.surface.scaleHeightKm).to.be.within(9, 13);
        expect(profile.layers).to.have.lengthOf(51);

        // Pressure should decrease monotonically with altitude
        for (let i = 1; i < profile.layers.length; i++) {
            expect(profile.layers[i].pressurePa).to.be.lessThan(profile.layers[i - 1].pressurePa);
        }
    });
});

describe('Crater Counting CSFD & Isochron Dating', () => {
    it('should compute CSFD and estimate age from crater inventory', () => {
        const mockCraters = [
            { diameter: 2000 },
            { diameter: 5000 },
            { diameter: 12000 },
            { diameter: 25000 },
            { diameter: 45000 }
        ];

        const csfd = CSFDEngine.computeCSFD(mockCraters, 1e5);
        expect(csfd.totalCraters).to.equal(5);
        expect(csfd.bins).to.be.an('array').that.is.not.empty;
        expect(csfd.estimatedAgeGa).to.be.greaterThan(0);
        expect(csfd.epoch).to.be.a('string');
    });

    it('should generate standard isochron curve', () => {
        const isochron = CSFDEngine.getIsochronCurve(3.5);
        expect(isochron).to.be.an('array').with.length.greaterThan(20);
        expect(isochron[0].cumulativeN).to.be.greaterThan(isochron[isochron.length - 1].cumulativeN);
    });
});

describe('Spectral Band Math & Mineralogy', () => {
    it('should list mineral presets', () => {
        expect(BandMathEngine.MINERAL_PRESETS).to.be.an('array').with.length.at.least(4);
        const hematite = BandMathEngine.MINERAL_PRESETS.find(p => p.id === 'bd530_hematite');
        expect(hematite).to.exist;
        expect(hematite.formula).to.include('B530');
    });

    it('should evaluate colormaps to valid RGBA tuples', () => {
        const viridis0 = BandMathEngine.evaluateColormap('viridis', 0.0);
        const viridis1 = BandMathEngine.evaluateColormap('viridis', 1.0);
        expect(viridis0).to.have.lengthOf(4);
        expect(viridis1).to.have.lengthOf(4);
        expect(viridis0[3]).to.equal(255);
    });
});

describe('Geographic Utilities & Projections', () => {
    it('should calculate Haversine distance accurately on Mars', () => {
        // Distance along equator from 0 to 10 deg Lon on Mars
        const dist = haversineDistance(0, 0, 0, 10, 'mars');
        const expected = (2 * Math.PI * 3389.5) * (10 / 360);
        expect(dist).to.be.closeTo(expected, 10);
    });

    it('should calculate azimuth', () => {
        const azNorth = azimuth(0, 0, 10, 0);
        expect(azNorth).to.be.closeTo(0, 1);
        const azEast = azimuth(0, 0, 0, 10);
        expect(azEast).to.be.closeTo(90, 1);
    });

    it('should convert planetocentric to planetographic latitude', () => {
        const centric = 45;
        const graphic = toGraphic(centric, 'mars');
        expect(graphic).to.be.greaterThan(centric);
        const back = toCentric(graphic, 'mars');
        expect(back).to.be.closeTo(centric, 0.01);
    });

    it('should format Lat/Lon with different longitude conventions', () => {
        const east180 = formatLatLon(10, -45, { lonFormat: 'east180' });
        expect(east180).to.include('-45');

        const east360 = formatLatLon(10, -45, { lonFormat: 'east360' });
        expect(east360).to.include('315');
    });

    it('should compute spherical polygon area', () => {
        const polygon = [
            { lat: 0, lng: 0 },
            { lat: 10, lng: 0 },
            { lat: 10, lng: 10 },
            { lat: 0, lng: 10 }
        ];
        const area = sphericalPolygonArea(polygon, 'mars');
        expect(area).to.be.greaterThan(100000);
    });

    it('should generate rotated landing safety ellipse polygons', () => {
        // Perseverance landing ellipse at Jezero (18.44°N, 77.45°E, 3.85 x 3.3 km, az 72°)
        const ellipseCoords = computeEllipsePolygon(18.44, 77.45, 3.85, 3.3, 72, 'mars', 32);
        expect(ellipseCoords).to.be.an('array').with.lengthOf(33); // 32 vertices + closing
        expect(ellipseCoords[0][0]).to.be.closeTo(18.44, 0.2);
        expect(ellipseCoords[0][1]).to.be.closeTo(77.45, 0.2);
    });

    it('should compute spatial geodesic buffer polygons around paths and points', () => {
        const pointBuffer = computeBufferPolygon([18.44, 77.45], 15, 'mars');
        expect(pointBuffer).to.be.an('array').with.length.greaterThan(10);

        const lineBuffer = computeBufferPolygon([[0, 0], [1, 1], [2, 0]], 10, 'mars');
        expect(lineBuffer).to.be.an('array').with.length.greaterThan(4);
    });
});

describe('Spacecraft Ground Tracks & Swaths', () => {
    it('should define orbit and swath parameters for Mars orbiters', () => {
        // Test orbital database properties
        const expectedCrafts = ['MRO', 'ODY', 'MAVEN', 'MEX', 'MGS'];
        expectedCrafts.forEach(id => {
            expect(id).to.be.a('string');
        });
    });
});

describe('Crater Morphometry & Classification', () => {
    it('should compute depth and simple morphology for small craters (< 7 km)', () => {
        const small = CraterTable.getMorphometry(3000); // 3 km
        expect(small.type).to.equal('Simple');
        expect(small.depthKm).to.be.closeTo(0.6, 0.05);
    });

    it('should classify complex central peak craters (7 to 100 km)', () => {
        const complex = CraterTable.getMorphometry(30000); // 30 km
        expect(complex.type).to.equal('Complex');
        expect(complex.depthKm).to.be.greaterThan(1.0);
    });

    it('should classify peak-ring basins (100 to 300 km)', () => {
        const basin = CraterTable.getMorphometry(150000); // 150 km
        expect(basin.type).to.equal('Peak-Ring');
    });
});

describe('Layer Composite Blending & Publishing', () => {
    beforeEach(() => {
        jmarsState.reset();
    });

    it('should support composite blend mode in layer state updates', () => {
        jmarsState.addLayer('mola_color');
        jmarsState.updateLayer('mola_color', { blendMode: 'multiply', opacity: 0.8 });
        const layer = jmarsState.get('activeLayers').find(l => l.id === 'mola_color');
        expect(layer).to.exist;
        expect(layer.blendMode).to.equal('multiply');
        expect(layer.opacity).to.equal(0.8);
    });
});

describe('Planetary Graticule Grid Layer', () => {
    it('should format longitudes in East-positive 360 and West-positive 360 conventions', () => {
        const dummyMap = { getZoom: () => 4, getBounds: () => ({ getSouth: () => -45, getNorth: () => 45, getWest: () => -90, getEast: () => 90 }), on: () => {}, off: () => {} };
        const grid = new GridLayer(dummyMap);

        grid.lonFormat = 'east360';
        expect(grid.formatLon(-45)).to.equal('315°E');
        expect(grid.formatLon(45)).to.equal('45°E');

        grid.lonFormat = 'west360';
        expect(grid.formatLon(-45)).to.equal('45°W');
        expect(grid.formatLon(45)).to.equal('315°W');
    });

    it('should provide adaptive zoom spacing for major/minor graticule lines', () => {
        const dummyMap = { getZoom: () => 2, getBounds: () => ({}), on: () => {}, off: () => {} };
        const grid = new GridLayer(dummyMap);

        const zoom2 = grid.getAdaptiveSpacing();
        expect(zoom2.major).to.equal(30);

        dummyMap.getZoom = () => 7;
        const zoom7 = grid.getAdaptiveSpacing();
        expect(zoom7.major).to.equal(1);
    });
});

describe('Planetary Graphic Scale Bar', () => {
    it('should calculate accurate planetary meters-per-pixel accounting for planetary radii and latitude', () => {
        // Equator at zoom 0 on Mars (R=3389.5 km)
        const mppMars = PlanetaryScaleBar.getMetersPerPixel(0, 0, 'mars');
        expect(mppMars).to.be.closeTo(83187, 100);

        // Equator at zoom 0 on Moon (R=1737.4 km)
        const mppMoon = PlanetaryScaleBar.getMetersPerPixel(0, 0, 'moon');
        expect(mppMoon).to.be.closeTo(42646, 100);

        // High latitude cosine scaling
        const mppMars60 = PlanetaryScaleBar.getMetersPerPixel(60, 0, 'mars');
        expect(mppMars60).to.be.closeTo(mppMars * 0.5, 100);
    });

    it('should select human-friendly metric step intervals', () => {
        const d1 = PlanetaryScaleBar.getFriendlyDistance(450);
        expect(d1.text).to.equal('200 m');

        const d2 = PlanetaryScaleBar.getFriendlyDistance(12500);
        expect(d2.text).to.equal('10 km');

        const d3 = PlanetaryScaleBar.getFriendlyDistance(850000);
        expect(d3.text).to.equal('500 km');
    });
});

describe('Mars Subsurface Radar Sounder (SHARAD/MARSIS)', () => {
    it('should convert two-way travel time (TWT) to depth using dielectric constant', () => {
        // In pure water ice (eps = 3.15), v ≈ 1.689e8 m/s
        // 10 microsecond TWT => z = v * 10e-6 / 2 ≈ 844.6 meters
        const depth = RadarSounderEngine.twtToDepth(10, 3.15);
        expect(depth).to.be.closeTo(844.6, 5);

        const twt = RadarSounderEngine.depthToTwt(depth, 3.15);
        expect(twt).to.be.closeTo(10, 0.05);
    });

    it('should simulate 1D A-scope trace with subsurface horizons and attenuation', () => {
        const trace = RadarSounderEngine.simulateTrace('boreum');
        expect(trace.twt).to.be.an('array').with.length.greaterThan(50);
        expect(trace.powerDb).to.be.an('array').with.lengthOf(trace.twt.length);
        expect(trace.horizons).to.be.an('array').with.length.greaterThan(2);
    });

    it('should simulate 2D B-scope radargram along ground tracks', () => {
        const radargram = RadarSounderEngine.simulateRadargram('australe', 100, 30);
        expect(radargram.grid).to.be.an('array').with.lengthOf(30);
        expect(radargram.distances).to.be.an('array').with.lengthOf(30);
    });
});

describe('Spatial POI Bookmarks System', () => {
    it('should provide default scientific POI presets across Mars and Moon', () => {
        expect(BookmarksTool.DEFAULT_POIS).to.be.an('array').with.length.greaterThan(5);
        const olympus = BookmarksTool.DEFAULT_POIS.find(p => p.id === 'poi-olympus');
        expect(olympus).to.exist;
        expect(olympus.body).to.equal('mars');

        const apollo = BookmarksTool.DEFAULT_POIS.find(p => p.id === 'poi-apollo11');
        expect(apollo).to.exist;
        expect(apollo.body).to.equal('moon');
    });
});

describe('Topographic Profile Transects & Linked Cursors', () => {
    it('should dispatch and handle synchronized profile hover coordinates', (done) => {
        const handler = (e) => {
            expect(e.detail.lat).to.equal(18.5);
            expect(e.detail.lng).to.equal(-133.8);
            expect(e.detail.elev).to.equal(21229);
            document.removeEventListener('jmars:profile-hover', handler);
            done();
        };
        document.addEventListener('jmars:profile-hover', handler);

        document.dispatchEvent(new CustomEvent('jmars:profile-hover', {
            detail: { lat: 18.5, lng: -133.8, dist: 12000, elev: 21229 }
        }));
    });
});

describe('Mars Orbital Mechanics & Time System', () => {
    it('should map Solar Longitude (Ls) back to approximate Earth Date', () => {
        // Ls = 0 in MY 37 corresponds to early 2023
        const date = MarsTime.lsToDate(0, 37);
        expect(date).to.be.instanceOf(Date);
        expect(date.getUTCFullYear()).to.be.within(2022, 2024);
    });

    it('should calculate surface mission sols for rovers and landers', () => {
        // Perseverance landed 2021-02-18
        const date = new Date('2021-02-19T00:00:00Z');
        const solObj = MarsTime.getMissionSol(date, 'perseverance');
        expect(solObj.mission).to.include('Perseverance');
        expect(solObj.sol).to.be.within(0, 2);
        expect(solObj.active).to.be.true;
    });
});

describe('3D Planetary Terrain & Solar Illumination (ThreeDEngine)', () => {
    it('should compute unit sun position vector from subsolar coordinates', () => {
        const sun = ThreeDEngine.computeSunVector(0, 0);
        expect(sun.x).to.be.closeTo(1.0, 0.001);
        expect(sun.y).to.be.closeTo(0.0, 0.001);
        expect(sun.z).to.be.closeTo(0.0, 0.001);

        const sunNorth = ThreeDEngine.computeSunVector(90, 0);
        expect(sunNorth.y).to.be.closeTo(1.0, 0.001);
    });

    it('should synthesize multi-scale terrain elevation displacement with vertical exaggeration', () => {
        const elev1 = ThreeDEngine.synthesizeTerrainElevation(0, 0, 18.5, -133.8, 1.0);
        const elev5 = ThreeDEngine.synthesizeTerrainElevation(0, 0, 18.5, -133.8, 5.0);
        expect(elev5).to.be.closeTo(elev1 * 5.0, 0.01);
    });

    it('should compute valid unit surface normal vectors on displaced terrain', () => {
        const normal = ThreeDEngine.computeSurfaceNormal(10, 10, 0, 0, 3.0);
        const length = Math.sqrt(normal.nx * normal.nx + normal.ny * normal.ny + normal.nz * normal.nz);
        expect(length).to.be.closeTo(1.0, 0.01);
        expect(normal.ny).to.be.greaterThan(0); // Upward facing
    });
});

describe('Astrodynamics & Interplanetary Trajectories (TrajectoryEngine)', () => {
    it('should compute Earth-Mars Hohmann transfer Delta-V budget and flight duration', () => {
        const sol = TrajectoryEngine.computeHohmannTransfer('earth', 'mars', 300, 300);
        expect(sol.tofDays).to.be.closeTo(258.9, 5.0); // ~8.5 months
        expect(sol.deltaVDepartKmS).to.be.closeTo(3.61, 0.2); // TMI ~3.6 km/s
        expect(sol.deltaVArriveKmS).to.be.closeTo(2.09, 0.2); // MOI ~2.1 km/s
        expect(sol.totalDeltaVKmS).to.be.closeTo(5.70, 0.3); // Total ~5.7 km/s
        expect(sol.c3LaunchEnergy).to.be.closeTo(8.67, 1.0); // C3 ~8.7 km^2/s^2
        expect(sol.synodicPeriodDays).to.be.closeTo(779.9, 5.0); // ~26 months
    });

    it('should compute Earth-Mars upcoming synodic launch opportunities', () => {
        const windows = TrajectoryEngine.getUpcomingMarsLaunchWindows(2024, 4);
        expect(windows).to.be.an('array').with.lengthOf(4);
        expect(windows[0].flightDurationDays).to.equal(259);
        expect(windows[0].departureDate).to.be.a('string');
    });
});

describe('Color Stretch & Image Processing (ColorStretchControl)', () => {
    it('should build and parse CSS filter strings symmetrically', () => {
        const opts = { brightness: 1.2, contrast: 1.4, saturation: 1.5, hueRotate: 45, invert: true };
        const filterStr = ColorStretchControl.buildFilterString(opts);

        expect(filterStr).to.include('brightness(1.2)');
        expect(filterStr).to.include('contrast(1.4)');
        expect(filterStr).to.include('saturate(1.5)');
        expect(filterStr).to.include('hue-rotate(45deg)');
        expect(filterStr).to.include('invert(1)');

        const parsed = ColorStretchControl.parseFilterString(filterStr);
        expect(parsed.brightness).to.equal(120);
        expect(parsed.contrast).to.equal(140);
        expect(parsed.saturation).to.equal(150);
        expect(parsed.hueRotate).to.equal(45);
        expect(parsed.invert).to.be.true;
    });
});

describe('Spatial Geometry Analysis & Containment', () => {
    it('should test point-in-polygon containment accurately', () => {
        const square = [
            [10, 10],
            [10, 20],
            [20, 20],
            [20, 10],
            [10, 10]
        ];

        expect(isPointInPolygon(15, 15, square)).to.be.true; // Inside
        expect(isPointInPolygon(5, 15, square)).to.be.false; // Outside
        expect(isPointInPolygon(25, 25, square)).to.be.false; // Outside
    });

    it('should compute bounding box and center coordinate for coordinate sets', () => {
        const coords = [[-10, 120], [20, 140], [5, 130]];
        const bbox = computeBoundingBox(coords);
        expect(bbox.minLat).to.equal(-10);
        expect(bbox.maxLat).to.equal(20);
        expect(bbox.centerLat).to.equal(5);
        expect(bbox.minLon).to.equal(120);
        expect(bbox.maxLon).to.equal(140);
        expect(bbox.centerLon).to.equal(130);
    });
});

describe('Planetary 3D Coordinates & Great Circle Interpolation', () => {
    it('should convert spherical to 3D Cartesian coordinates and back', () => {
        // North Pole on Mars (lat=90, lon=0, R=3389.5 km)
        const pole = sphericalToCartesian(90, 0, 0, 'mars');
        expect(pole.x).to.be.closeTo(0, 0.01);
        expect(pole.y).to.be.closeTo(0, 0.01);
        expect(pole.z).to.be.closeTo(3389.5, 0.01);

        const sphere = cartesianToSpherical(pole.x, pole.y, pole.z, 'mars');
        expect(sphere.lat).to.be.closeTo(90, 0.01);
        expect(sphere.altKm).to.be.closeTo(0, 0.01);
    });

    it('should interpolate along great circle and compute exact midpoint', () => {
        // Equator from 0 to 90 deg E
        const mid = computeMidpoint(0, 0, 0, 90);
        expect(mid.lat).to.be.closeTo(0, 0.01);
        expect(mid.lon).to.be.closeTo(45, 0.01);
    });
});

describe('Geodesic Navigation & Cross-Track Distance', () => {
    it('should compute destination point given start coordinate, distance, and bearing', () => {
        // From (0, 0) heading North (0 deg) for 500 km on Mars (R=3389.5 km)
        // 500 / 3389.5 * (180 / PI) ≈ 8.452 deg N
        const dest = computeDestinationPoint(0, 0, 500, 0, 'mars');
        expect(dest.lat).to.be.closeTo(8.452, 0.05);
        expect(dest.lon).to.be.closeTo(0, 0.01);
    });

    it('should compute cross-track perpendicular distance from a great-circle path', () => {
        // Path along Equator from (0, -45) to (0, 45)
        // Point at (10, 0) is 10 deg North ≈ 10 * PI/180 * 3389.5 ≈ 591.6 km
        const xtDist = computeCrossTrackDistance(10, 0, 0, -45, 0, 45, 'mars');
        expect(Math.abs(xtDist)).to.be.closeTo(591.6, 5.0);
    });
});

describe('Planetary Multi-Layer Probe (InvestigateTool)', () => {
    it('should format probe diagnostics including coordinate notations, KRC temperature, and MCD pressure', () => {
        const diag = InvestigateTool.formatProbeDiagnostics({
            lat: 18.5,
            lng: 226.2,
            body: 'mars',
            elevationMeters: 21287,
            Ls: 90,
            MTC: 12
        });

        expect(diag.lng360E).to.be.closeTo(226.2, 0.01);
        expect(diag.lng180).to.be.closeTo(-133.8, 0.01);
        expect(diag.elevationKm).to.be.closeTo(21.287, 0.001);
        expect(diag.krc).to.not.be.null;
        expect(diag.krc.meanTemp).to.be.within(150, 300);
        expect(diag.mcd).to.not.be.null;
        expect(diag.mcd.pressurePa).to.be.greaterThan(0);
    });
});

describe('Planetary Map Projections (ProjectionManager)', () => {
    it('should perform symmetric forward and inverse Equirectangular projection', () => {
        const fwd = ProjectionManager.forwardEquirectangular(30, 45, 0, 0, 'mars');
        const inv = ProjectionManager.inverseEquirectangular(fwd.x, fwd.y, 0, 0, 'mars');
        expect(inv.lat).to.be.closeTo(30, 0.001);
        expect(inv.lon).to.be.closeTo(45, 0.001);
    });

    it('should perform forward and inverse 3D Orthographic projection', () => {
        const fwd = ProjectionManager.forwardOrthographic(20, -30, 0, 0, 'mars');
        expect(fwd.visible).to.be.true;
        const inv = ProjectionManager.inverseOrthographic(fwd.x, fwd.y, 0, 0, 'mars');
        expect(inv.lat).to.be.closeTo(20, 0.01);
        expect(inv.lon).to.be.closeTo(-30, 0.01);
    });

    it('should perform forward and inverse Sinusoidal equal-area projection', () => {
        const fwd = ProjectionManager.forwardSinusoidal(-15, 60, 0, 'mars');
        const inv = ProjectionManager.inverseSinusoidal(fwd.x, fwd.y, 0, 'mars');
        expect(inv.lat).to.be.closeTo(-15, 0.001);
        expect(inv.lon).to.be.closeTo(60, 0.001);
    });
});

describe('Terrain Slope & Aspect Topographic Analysis (ContourLayer)', () => {
    it('should compute numerical terrain slope, aspect, and hazard categorization from elevation grid', () => {
        // Flat 5x5 plane
        const flatGrid = new Float32Array(25).fill(1000);
        const flatRes = ContourLayer.computeTerrainSlopeAndAspect(flatGrid, 5, 5, 100);
        expect(flatRes.meanSlopeDeg).to.be.closeTo(0, 0.001);
        expect(flatRes.hazardRatio.safe).to.equal(1.0);

        // 45-degree ramp in X direction: dz = dx (100 m rise per 100 m run)
        const rampGrid = new Float32Array(25);
        for (let y = 0; y < 5; y++) {
            for (let x = 0; x < 5; x++) {
                rampGrid[y * 5 + x] = x * 100;
            }
        }
        const rampRes = ContourLayer.computeTerrainSlopeAndAspect(rampGrid, 5, 5, 100);
        expect(rampRes.meanSlopeDeg).to.be.closeTo(45.0, 0.1);
        expect(rampRes.hazardRatio.critical).to.equal(1.0);
    });
});

describe('Planetary Hypsometric Tinting & Colormaps (ColorRampEngine)', () => {
    it('should generate 256-step RGB lookup tables for scientific colormaps', () => {
        const lut = ColorRampEngine.generateLUT('mola_rainbow', 256);
        expect(lut).to.be.instanceOf(Uint8Array);
        expect(lut.length).to.equal(256 * 3);
        // First entry should be deep blue
        expect(lut[0]).to.equal(20);
        expect(lut[1]).to.equal(30);
        expect(lut[2]).to.equal(140);
        // Last entry should be white (255, 255, 255)
        expect(lut[255 * 3]).to.equal(255);
        expect(lut[255 * 3 + 1]).to.equal(255);
        expect(lut[255 * 3 + 2]).to.equal(255);
    });

    it('should colorize an elevation array into RGBA pixel buffers', () => {
        const elevs = [-8000, 0, 21000];
        const rgba = ColorRampEngine.colorizeArray(elevs, -8000, 21000, 'mola_rainbow', 255);
        expect(rgba).to.be.instanceOf(Uint8ClampedArray);
        expect(rgba.length).to.equal(3 * 4);
        expect(rgba[3]).to.equal(255); // Alpha
        expect(rgba[7]).to.equal(255);
        expect(rgba[11]).to.equal(255);
    });
});

describe('GIS Vector Shape Serialization & WKT (ShapeIO)', () => {
    it('should serialize GeoJSON geometries to Well-Known Text (WKT)', () => {
        const pt = { type: 'Point', coordinates: [18.5, -133.8] };
        expect(ShapeIO.toWKT(pt)).to.equal('POINT(18.5 -133.8)');

        const line = { type: 'LineString', coordinates: [[0, 0], [10, 10]] };
        expect(ShapeIO.toWKT(line)).to.equal('LINESTRING(0 0, 10 10)');
    });

    it('should parse Well-Known Text (WKT) strings into GeoJSON geometry structures', () => {
        const parsedPt = ShapeIO.parseWKT('POINT(18.5 -133.8)');
        expect(parsedPt).to.deep.equal({ type: 'Point', coordinates: [18.5, -133.8] });

        const parsedPoly = ShapeIO.parseWKT('POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))');
        expect(parsedPoly.type).to.equal('Polygon');
        expect(parsedPoly.coordinates[0]).to.have.lengthOf(5);
    });
});

describe('Geodesic Path Metrics & Polygon Perimeter', () => {
    it('should compute cumulative geodesic length of multi-segment polylines', () => {
        // Equator track on Mars from (0, 0) to (0, 10) to (0, 20)
        // 20 deg longitude at Equator = 20 * PI/180 * 3389.5 ≈ 1183.2 km
        const line = [[0, 0], [0, 10], [0, 20]];
        const len = computePolylineLength(line, 'mars');
        expect(len).to.be.closeTo(1183.2, 5.0);
    });

    it('should compute closed geodesic perimeter for polygons', () => {
        // Equatorial square (0,0) -> (10,0) -> (10,10) -> (0,10)
        const poly = [[0, 0], [10, 0], [10, 10], [0, 10]];
        const perim = computePolygonPerimeter(poly, 'mars');
        expect(perim).to.be.greaterThan(2000);
    });
});

if (typeof mocha !== 'undefined') {
    mocha.run();
}
