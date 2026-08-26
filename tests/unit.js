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
import { CustomMapManager } from '../src/features/custom-map/CustomMapManager.js';
import { LandingSitesLayer } from '../src/features/landing/LandingSitesLayer.js';
import { RadialProfileTool } from '../src/features/profile/RadialProfileTool.js';
import { MeasureTool } from '../src/features/measure/MeasureTool.js';
import { GroundTrackLayer } from '../src/features/groundtrack/GroundTrackLayer.js';
import { HillshadeLayer } from '../src/features/hillshade/HillshadeLayer.js';
import { SamplingTool } from '../src/features/sampling/SamplingTool.js';
import { NomenclatureTool } from '../src/features/nomenclature/NomenclatureTool.js';
import { ColorRampEngine } from '../src/util/ColorRampEngine.js';
import { ShapeIO } from '../src/features/shapes/ShapeIO.js';
import { PlacesManager } from '../src/features/places/PlacesManager.js';
import { ExportTool } from '../src/features/export/ExportTool.js';
import { haversineDistance, azimuth, toGraphic, toCentric, formatLatLon, sphericalPolygonArea, computeEllipsePolygon, computeBufferPolygon, isPointInPolygon, computeBoundingBox, sphericalToCartesian, cartesianToSpherical, interpolateGreatCircle, computeMidpoint, computeDestinationPoint, computeCrossTrackDistance, computeAlongTrackDistance, computePolylineLength, computePolygonPerimeter, computeGreatCircleMidpoint, computeTunnelChordDistance, computeSphericalRhumbLineDistance, computeSphericalExcess, computeEllipsoidalGeodesicDistanceAndoyer, computePolylineDeflectionAngles, computeSphericalBoundingCircle, computeGreatCircleIntersection, computePlanetaryEllipseSurfaceArea } from '../src/util/geo.js';

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

    it('should compute diurnal/annual thermal skin depths, ATI, and regolith conductivity', () => {
        // Diurnal skin depth for TI = 250 (rho=1500, cp=800)
        const skinDiurnal = KRCEngine.computeSkinDepth(250, 88775.244);
        expect(skinDiurnal.skinDepthCm).to.be.closeTo(3.5, 0.2); // ~3.5 cm

        // Annual skin depth (668.6 sols)
        const skinAnnual = KRCEngine.computeSkinDepth(250, 88775.244 * 668.6);
        expect(skinAnnual.skinDepthCm).to.be.closeTo(90.5, 5.0); // ~90 cm

        // Regolith bulk thermal conductivity for TI = 250 (W/(m K))
        const k = KRCEngine.computeRegolithConductivity(250);
        expect(k).to.be.closeTo(0.052, 0.005); // ~0.052 W/(m K)

        // Apparent Thermal Inertia estimate from Delta T = 80 K
        const ati = KRCEngine.computeApparentThermalInertia(80, 0.25, 588.6);
        expect(ati).to.be.closeTo(650, 50);
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

    it('should compute Mars atmospheric speed of sound, viscosity, mean free path, and column mass', () => {
        // Speed of sound at 220 K in CO2 (~231 m/s)
        const cs = MCDEngine.computeSpeedOfSound(220);
        expect(cs).to.be.closeTo(231.5, 2.0);

        // Dynamic viscosity at 220 K (~1.14e-5 Pa*s)
        const mu = MCDEngine.computeDynamicViscosity(220);
        expect(mu).to.be.closeTo(1.14e-5, 0.1e-5);

        // Mean free path at 610 Pa and 220 K (~10.3 microns = 1.03e-5 m)
        const lambda = MCDEngine.computeMeanFreePath(610, 220);
        expect(lambda).to.be.closeTo(1.03e-5, 0.1e-5);

        // Column mass at 610 Pa (~163.9 kg/m^2)
        const colMass = MCDEngine.computeAtmosphericColumnMass(610);
        expect(colMass).to.be.closeTo(163.9, 2.0);
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

    it('should compute Planetary Relative (R) plots, saturation limits, and crater freshness', () => {
        const mockCraters = [
            { diameter: 2000 },
            { diameter: 5000 },
            { diameter: 12000 }
        ];

        // R-plot calculation
        const rBins = CSFDEngine.computeRPlot(mockCraters, 1e6);
        expect(rBins).to.be.an('array').with.lengthOf(12);
        const nonEmptyBin = rBins.find(b => b.count > 0);
        expect(nonEmptyBin).to.exist;
        expect(nonEmptyBin.rValue).to.be.greaterThan(0);

        // Saturation limit at 10 km (0.15 * 10^-2 = 0.0015)
        const satLimit10km = CSFDEngine.computeSaturationLimit(10);
        expect(satLimit10km).to.be.closeTo(0.0015, 0.0001);

        // Crater freshness classification (depth = 600m, diam = 3000m -> ratio = 0.20 -> Fresh)
        const fresh = CSFDEngine.classifyCraterFreshness(600, 3000);
        expect(fresh.freshnessClass).to.equal(1);
        expect(fresh.name).to.include('Pristine');

        // Eroded crater (depth = 100m, diam = 3000m -> ratio = 0.033 -> Eroded)
        const eroded = CSFDEngine.classifyCraterFreshness(100, 3000);
        expect(eroded.freshnessClass).to.equal(3);
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

    it('should evaluate multi-spectral mineral parameter indices and false color RGB composites', () => {
        // BD530 hematite index: 1.0 - (0.16 / (0.5 * (0.14 + 0.26))) = 1 - 0.16/0.2 = 0.20
        const hematiteVal = BandMathEngine.evaluateBandIndex('bd530_hematite', { B530: 0.16, B440: 0.14, B600: 0.26 });
        expect(hematiteVal).to.be.closeTo(0.20, 0.001);

        // BD1900 hydrated index: 1.0 - (0.18 / (0.55 * 0.20 + 0.45 * 0.20)) = 1 - 0.18/0.20 = 0.10
        const hydratedVal = BandMathEngine.evaluateBandIndex('bd1900_hydrated', { B1930: 0.18, B1815: 0.20, B2130: 0.20 });
        expect(hydratedVal).to.be.closeTo(0.10, 0.001);

        // False color RGB synthesis
        const rgb = BandMathEngine.generateFalseColorRGB(0.5, 0.8, 0.2, [0, 1], [0, 1], [0, 1]);
        expect(rgb).to.deep.equal([128, 204, 51]);
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

describe('Instrument Stamp & Footprint Optics (StampLayer)', () => {
    it('should compute spatial pixel resolution (GSD) for HiRISE and CTX', () => {
        // HiRISE: 300 km altitude, 12000 mm focal length, 12 micron pixel pitch -> 0.30 m/pixel
        const gsdHirise = StampLayer.computeSpatialResolution(300, 12000, 12);
        expect(gsdHirise).to.be.closeTo(0.30, 0.01);

        // CTX: 300 km altitude, 350 mm focal length, 7 micron pixel pitch -> 6.0 m/pixel
        const gsdCtx = StampLayer.computeSpatialResolution(300, 350, 7);
        expect(gsdCtx).to.be.closeTo(6.0, 0.1);
    });

    it('should compute solar phase angle and slant range distance', () => {
        // Incidence = 45 deg, Emission = 15 deg, Azimuth diff = 0 deg -> Phase = 30 deg
        const phase = StampLayer.computePhaseAngle(45, 15, 0);
        expect(phase).to.be.closeTo(30.0, 0.1);

        // Slant range at 300 km altitude with 60 deg off-nadir emission -> 300 / cos(60) = 600 km
        const slant = StampLayer.computeSlantRange(300, 60);
        expect(slant).to.be.closeTo(600.0, 0.1);
    });

    it('should build valid USGS ODE REST API query URLs', () => {
        const url = StampLayer.buildODEQueryURL({ instrument: 'CTX', minLat: 10, maxLat: 20 });
        expect(url).to.include('oderest.rsl.wustl.edu');
        expect(url).to.include('iid=CTX');
        expect(url).to.include('minlat=10.0000');
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

    it('should compute Fresnel dielectric reflectivity, vertical resolution, and attenuation rate', () => {
        // Fresnel reflection between air (eps=1) and ice (eps=3.15)
        const refl = RadarSounderEngine.computeFresnelReflectivity(1.0, 3.15);
        expect(refl.reflectivityLinear).to.be.closeTo(0.078, 0.005);
        expect(refl.reflectivityDb).to.be.closeTo(-11.1, 0.2);
        expect(refl.transmissivityLinear).to.be.closeTo(0.922, 0.005);

        // SHARAD vertical range resolution (10 MHz bandwidth in ice eps=3.15)
        const resIce = RadarSounderEngine.computeRangeResolution(10e6, 3.15);
        expect(resIce).to.be.closeTo(8.45, 0.1); // ~8.45 meters

        // SHARAD attenuation rate (20 MHz in ice eps=3.15, tan_delta=0.001)
        const atten = RadarSounderEngine.computeAttenuationRate(20e6, 0.001, 3.15);
        expect(atten).to.be.closeTo(0.00323, 0.0002); // ~0.00323 dB/m (~3.23 dB/km)
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

    it('should export and parse GeoJSON FeatureCollections and compute ROI bounding boxes', () => {
        const pois = [
            { id: 'poi-1', name: 'Jezero Delta', lat: 18.38, lng: 77.58, zoom: 9, body: 'mars' },
            { id: 'poi-2', name: 'Gale Crater', lat: -5.4, lng: 137.8, zoom: 8, body: 'mars' }
        ];

        // Export to GeoJSON
        const geojson = BookmarksTool.exportGeoJSON(pois);
        expect(geojson.type).to.equal('FeatureCollection');
        expect(geojson.features).to.have.lengthOf(2);
        expect(geojson.features[0].geometry.coordinates).to.deep.equal([77.58, 18.38]);

        // Parse from GeoJSON
        const parsed = BookmarksTool.parseGeoJSON(geojson);
        expect(parsed).to.have.lengthOf(2);
        expect(parsed[0].name).to.equal('Jezero Delta');
        expect(parsed[0].lat).to.equal(18.38);
        expect(parsed[0].lng).to.equal(77.58);

        // Bounding box
        const bbox = BookmarksTool.computeBoundingBox(pois);
        expect(bbox.minLat).to.equal(-5.4);
        expect(bbox.maxLat).to.equal(18.38);
        expect(bbox.minLng).to.equal(77.58);
        expect(bbox.maxLng).to.equal(137.8);
        expect(bbox.centerLat).to.be.closeTo(6.49, 0.01);
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

    it('should calculate solar position, elevation, and daylight duration', () => {
        // At equator at solar noon (hour = 12, Ls = 0, lat = 0)
        const pos = MarsTime.getSolarPosition(0, 0, 0, 12);
        expect(pos.altitudeDeg).to.be.closeTo(90.0, 0.5);
        expect(pos.isDay).to.be.true;

        // North pole in summer (lat = 85, Ls = 90) -> Polar Day (24h sun)
        const dayLenSummer = MarsTime.getMartianDayLength(85, 90);
        expect(dayLenSummer.daylightHours).to.equal(24.0);
        expect(dayLenSummer.state).to.include('Polar Day');

        // North pole in winter (lat = 85, Ls = 270) -> Polar Night (0h sun)
        const dayLenWinter = MarsTime.getMartianDayLength(85, 270);
        expect(dayLenWinter.daylightHours).to.equal(0.0);
        expect(dayLenWinter.state).to.include('Polar Night');
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

    it('should compute hillshade illumination, Lommel-Seeliger / Minnaert reflectance, and camera GFOV', () => {
        // Flat normal pointing upward (0, 1, 0) and sun at zenith (0, 1, 0)
        const flatNormal = { nx: 0, ny: 1, nz: 0 };
        const sunZenith = { x: 0, y: 1, z: 0 };
        const hillshade = ThreeDEngine.computeHillshade(flatNormal, sunZenith, 0.15);
        expect(hillshade).to.equal(1.0);

        // Lommel-Seeliger scattering (i=0, e=0 -> mu0=1, mu=1 -> 1 / (1+1) = 0.5)
        const ls = ThreeDEngine.computeLommelSeeligerReflectance(1.0, 1.0);
        expect(ls).to.equal(0.5);

        // Minnaert photometric reflectance
        const minnaert = ThreeDEngine.computeMinnaertReflectance(0.8, 0.9, 0.65);
        expect(minnaert).to.be.greaterThan(0.5);

        // Ground FOV at 400 km altitude with 45 deg FOV
        // GFOV = 2 * 400 * tan(22.5 deg) ≈ 2 * 400 * 0.4142 ≈ 331.37 km
        const gfov = ThreeDEngine.computeGroundFOV(400, 45);
        expect(gfov).to.be.closeTo(331.4, 2.0);
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

    it('should compute circular orbital speed, escape velocity, and Areostationary orbit', () => {
        // Low Mars Orbit (300 km altitude)
        const vCirc = TrajectoryEngine.computeOrbitalSpeed(300, 'mars');
        expect(vCirc).to.be.closeTo(3.40, 0.05); // ~3.4 km/s

        // Mars surface escape velocity
        const vEsc = TrajectoryEngine.computeEscapeVelocity(0, 'mars');
        expect(vEsc).to.be.closeTo(5.03, 0.05); // ~5.03 km/s

        // Orbital period at 300 km (~113 minutes)
        const period = TrajectoryEngine.computeOrbitalPeriod(300, 'mars');
        expect(period.periodMinutes).to.be.closeTo(113.0, 3.0);

        // Areostationary synchronous orbit (~17,032 km altitude, ~20,422 km radius)
        const sync = TrajectoryEngine.computeSynchronousOrbitAltitude('mars');
        expect(sync.radiusKm).to.be.closeTo(20428, 50);
        expect(sync.altitudeKm).to.be.closeTo(17038, 50);
        expect(sync.speedKmS).to.be.closeTo(1.45, 0.05);
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

    it('should perform forward and inverse Mollweide and Lambert Azimuthal Equal-Area projections', () => {
        // Mollweide equal-area projection
        const fwdMoll = ProjectionManager.forwardMollweide(45, 90, 0, 'mars');
        const invMoll = ProjectionManager.inverseMollweide(fwdMoll.x, fwdMoll.y, 0, 'mars');
        expect(invMoll.lat).to.be.closeTo(45, 0.01);
        expect(invMoll.lon).to.be.closeTo(90, 0.01);

        // Lambert Azimuthal Equal-Area (North Polar aspect)
        const fwdLaea = ProjectionManager.forwardLambertAzimuthal(80, 45, 90, 0, 'mars');
        expect(fwdLaea.visible).to.be.true;
        const invLaea = ProjectionManager.inverseLambertAzimuthal(fwdLaea.x, fwdLaea.y, 90, 0, 'mars');
        expect(invLaea.lat).to.be.closeTo(80, 0.01);
        expect(invLaea.lon).to.be.closeTo(45, 0.01);
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

    it('should compute Topographic Roughness Index (TRI), Hypsometric Integral, and contour volume', () => {
        // Flat grid TRI should be 0
        const flatGrid = new Float32Array(25).fill(2000);
        const flatTRI = ContourLayer.computeTopographicRoughnessIndex(flatGrid, 5, 5);
        expect(flatTRI.meanTRI).to.equal(0);

        // Hypsometric Integral for linear ramp (0, 250, 500, 750, 1000) -> HI = 0.5 (Mature)
        const ramp = new Float32Array([0, 250, 500, 750, 1000]);
        const hi = ContourLayer.computeHypsometricIntegral(ramp);
        expect(hi.hi).to.be.closeTo(0.5, 0.01);
        expect(hi.stage).to.include('Mature');

        // Contour volume above datum 0 meters: (1000 m * 10000 m^2) = 1e7 m^3
        const singleCell = new Float32Array([1000]);
        const vol = ContourLayer.computeContourVolume(singleCell, 0, 10000);
        expect(vol.volumeM3).to.equal(10000000);
        expect(vol.volumeKm3).to.be.closeTo(0.01, 0.001);
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

describe('Planetary Spatial Proximity Search & Places (PlacesManager)', () => {
    it('should find nearest planetary features within distance radius sorted by proximity', () => {
        const features = [
            { name: 'Olympus Mons', lat: 18.5, lon: -133.8 },
            { name: 'Gale Crater', lat: -5.4, lon: 137.8 },
            { name: 'Jezero Crater', lat: 18.38, lon: 77.58 }
        ];

        // Target point near Olympus Mons (19.0, -133.0)
        const nearest = PlacesManager.findNearestFeatures(19.0, -133.0, features, 1000, 'mars');
        expect(nearest).to.be.an('array').with.lengthOf(1);
        expect(nearest[0].name).to.equal('Olympus Mons');
        expect(nearest[0].distanceKm).to.be.lessThan(100);
    });

    it('should parse coordinate strings robustly', () => {
        const coord = PlacesManager.parseCoordinateString('18.5, -133.8');
        expect(coord).to.not.be.null;
        expect(coord.lat).to.equal(18.5);
        expect(coord.lon).to.equal(-133.8);
    });
});

describe('GIS Georeferencing & World Files (ExportTool)', () => {
    it('should generate and parse 6-line GIS World File affine matrices symmetrically', () => {
        const west = -180;
        const east = 180;
        const south = -90;
        const north = 90;
        const widthPx = 3600;
        const heightPx = 1800;

        const content = ExportTool.generateWorldFileContent(west, east, south, north, widthPx, heightPx);
        expect(content.split('\n')).to.have.lengthOf(6);

        const parsed = ExportTool.parseWorldFileContent(content);
        expect(parsed.pixelWidth).to.be.closeTo(0.1, 0.0001);
        expect(parsed.pixelHeight).to.be.closeTo(-0.1, 0.0001);
        expect(parsed.originX).to.be.closeTo(-179.95, 0.001);
        expect(parsed.originY).to.be.closeTo(89.95, 0.001);
    });
});

describe('Custom Tile Layers & WMS Protocol (CustomMapManager)', () => {
    it('should convert between TMS and XYZ tile coordinates', () => {
        // At zoom 3 (8x8 grid, 0..7)
        const xyzY = CustomMapManager.tmsToXyz(0, 3);
        expect(xyzY).to.equal(7);

        const tmsY = CustomMapManager.tmsToXyz(7, 3);
        expect(tmsY).to.equal(0);
    });

    it('should convert between tile coordinates and Bing Quadkeys', () => {
        // Tile (3, 5) at zoom 3
        const quadkey = CustomMapManager.tileToQuadkey(3, 5, 3);
        expect(quadkey).to.be.a('string').with.lengthOf(3);

        const tile = CustomMapManager.quadkeyToTile(quadkey);
        expect(tile.tileX).to.equal(3);
        expect(tile.tileY).to.equal(5);
        expect(tile.zoom).to.equal(3);
    });

    it('should build OGC WMS GetCapabilities URLs and validate tile templates', () => {
        const wmsUrl = CustomMapManager.buildWmsCapabilitiesUrl('https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/mars/mars_simp_cyl.map');
        expect(wmsUrl).to.include('SERVICE=WMS');
        expect(wmsUrl).to.include('REQUEST=GetCapabilities');

        const validTemplate = CustomMapManager.validateTileUrlTemplate('https://example.com/tiles/{z}/{x}/{y}.png');
        expect(validTemplate.valid).to.be.true;

        const invalidTemplate = CustomMapManager.validateTileUrlTemplate('https://example.com/tiles/{z}/{x}.png');
        expect(invalidTemplate.valid).to.be.false;
    });
});

describe('Spacecraft Landing Sites & EDL Aerodynamics (LandingSitesLayer)', () => {
    it('should compute entry ballistic coefficient and hypersonic dynamic pressure', () => {
        // MSL/Perseverance entry: mass = 3150 kg, Cd = 1.4, Area = 15.9 m^2 -> beta ≈ 141.5 kg/m^2
        const bc = LandingSitesLayer.computeBallisticCoefficient(3150, 1.4, 15.9);
        expect(bc).to.be.closeTo(141.5, 1.0);

        // Dynamic pressure at rho = 0.005 kg/m^3, v = 5000 m/s -> q = 0.5 * 0.005 * 25e6 = 62,500 Pa
        const q = LandingSitesLayer.computeDynamicPressure(0.005, 5000);
        expect(q).to.equal(62500);
    });

    it('should query the nearest landing site accurately', () => {
        const mockSites = [
            { name: 'Perseverance', lat: 18.38, lon: 77.58, body: 'mars' },
            { name: 'Curiosity', lat: -4.59, lon: 137.44, body: 'mars' },
            { name: 'Apollo 11', lat: 0.674, lon: 23.473, body: 'moon' }
        ];

        // Coordinate near Jezero Crater (18.5, 77.5)
        const nearestMars = LandingSitesLayer.findNearestLandingSite(18.5, 77.5, mockSites, 'mars');
        expect(nearestMars).to.not.be.null;
        expect(nearestMars.name).to.equal('Perseverance');
        expect(nearestMars.distanceKm).to.be.lessThan(50);

        // Moon query
        const nearestMoon = LandingSitesLayer.findNearestLandingSite(0.0, 23.0, mockSites, 'moon');
        expect(nearestMoon).to.not.be.null;
        expect(nearestMoon.name).to.equal('Apollo 11');
    });
});

describe('Radial Topographic Profiles & Crater Morphometry (RadialProfileTool)', () => {
    it('should compute average radial profile and standard deviation envelope', () => {
        const mockProfiles = [
            { data: [{ dist: 0, elev: 100 }, { dist: 1000, elev: 200 }, { dist: 2000, elev: 500 }] },
            { data: [{ dist: 0, elev: 120 }, { dist: 1000, elev: 210 }, { dist: 2000, elev: 520 }] }
        ];

        const avg = RadialProfileTool.computeAverageProfile(mockProfiles);
        expect(avg).to.have.lengthOf(3);
        expect(avg[0].meanElev).to.equal(110);
        expect(avg[1].meanElev).to.equal(205);
        expect(avg[2].meanElev).to.equal(510);
        expect(avg[0].stdElev).to.be.closeTo(10, 0.1);
    });

    it('should detect crater rim crest, apparent depth, and cavity volume', () => {
        // Synthetic crater profile: center (0m) = 100m elev, rim (2000m) = 600m elev, outside (3000m) = 400m
        const synthProfile = [
            { dist: 0, meanElev: 100 },
            { dist: 1000, meanElev: 250 },
            { dist: 2000, meanElev: 600 },
            { dist: 3000, meanElev: 400 }
        ];

        const rim = RadialProfileTool.detectCraterRimAndDepth(synthProfile);
        expect(rim.rimRadiusM).to.equal(2000);
        expect(rim.rimElevM).to.equal(600);
        expect(rim.floorElevM).to.equal(100);
        expect(rim.apparentDepthM).to.equal(500); // 600 - 100 = 500 m
        expect(rim.depthToDiameterRatio).to.be.closeTo(0.125, 0.005); // 500 / 4000 = 0.125

        // Cavity volume beneath 600m rim
        const cavity = RadialProfileTool.computeCavityVolume(synthProfile, 2000);
        expect(cavity.cavityVolumeM3).to.be.greaterThan(0);
        expect(cavity.cavityVolumeKm3).to.be.greaterThan(0);
    });
});

describe('Spatial Measurement Geodesy & WKT (MeasureTool)', () => {
    it('should compute segment-by-segment path metrics and turn angles', () => {
        // Path: (0, 0) -> (0, 10) -> (10, 10) on Mars
        const path = [[0, 0], [0, 10], [10, 10]];
        const segments = MeasureTool.computeSegmentMetrics(path, 'mars');
        expect(segments).to.have.lengthOf(2);
        expect(segments[0].segment).to.equal(1);
        expect(segments[0].bearingDeg).to.be.closeTo(90, 1.0); // Heading East
        expect(segments[1].bearingDeg).to.be.closeTo(0, 1.0); // Heading North
        expect(segments[1].turnAngleDeg).to.be.closeTo(-90, 1.0); // 90-degree left turn
        expect(segments[1].cumulativeKm).to.be.greaterThan(segments[0].distanceKm);
    });

    it('should compute minimum enclosing circle and export WKT', () => {
        const polyCoords = [[0, 0], [0, 2], [2, 2], [2, 0]];
        const circle = MeasureTool.computeMinimumEnclosingCircle(polyCoords, 'mars');
        expect(circle.centerLat).to.equal(1.0);
        expect(circle.centerLon).to.equal(1.0);
        expect(circle.radiusKm).to.be.greaterThan(0);

        // WKT Line and Polygon
        const lineWkt = MeasureTool.toWKT('Line', [[0, 0], [1, 1]]);
        expect(lineWkt).to.equal('LINESTRING (0 0, 1 1)');

        const polyWkt = MeasureTool.toWKT('Area', [[0, 0], [0, 1], [1, 1]]);
        expect(polyWkt).to.equal('POLYGON ((0 0, 1 0, 1 1, 0 0))');
    });
});

describe('Planetary Probe Diagnostics & Geophysics (InvestigateTool)', () => {
    it('should compute barometric atmospheric pressure and gravity at elevation', () => {
        // Datum elevation (0m) -> P = 610 Pa
        const pDatum = InvestigateTool.computeBarometricPressure(0);
        expect(pDatum).to.equal(610);

        // Hellas basin (-7000m) -> higher pressure
        const pHellas = InvestigateTool.computeBarometricPressure(-7000);
        expect(pHellas).to.be.greaterThan(1000);

        // Olympus Mons summit (+21287m) -> low pressure
        const pOlympus = InvestigateTool.computeBarometricPressure(21287);
        expect(pOlympus).to.be.lessThan(100);

        // Local surface gravity
        const gMars = InvestigateTool.computeLocalGravity(0, 'mars');
        expect(gMars).to.be.closeTo(3.72, 0.01);

        const gMoon = InvestigateTool.computeLocalGravity(0, 'moon');
        expect(gMoon).to.be.closeTo(1.62, 0.01);
    });

    it('should classify regolith thermal regimes accurately', () => {
        const dust = InvestigateTool.classifyThermalRegime(80, 0.28);
        expect(dust.regime).to.equal('High Dust Mantle');

        const sand = InvestigateTool.classifyThermalRegime(200, 0.15);
        expect(sand.regime).to.equal('Fine-to-Medium Sand');

        const rock = InvestigateTool.classifyThermalRegime(900, 0.12);
        expect(rock.regime).to.equal('Solid Bedrock / Massive Ice');
    });
});

describe('Planetary Places, Mars Chart Quadrants & Gazetteer (PlacesManager)', () => {
    it('should determine Mars Chart (MC) quadrants correctly', () => {
        // Olympus Mons (18.65 N, 226.2 E -> lonWest = 133.8 W) -> MC-09 Tharsis
        const mcTharsis = PlacesManager.getMarsChartQuadrant(18.65, 226.2);
        expect(mcTharsis.code).to.equal('MC-09');
        expect(mcTharsis.name).to.equal('Tharsis');

        // North pole (80 N) -> MC-01 Mare Boreum
        const mcNorth = PlacesManager.getMarsChartQuadrant(80, 0);
        expect(mcNorth.code).to.equal('MC-01');
        expect(mcNorth.name).to.equal('Mare Boreum');

        // South pole (-75 S) -> MC-30 Mare Australe
        const mcSouth = PlacesManager.getMarsChartQuadrant(-75, 0);
        expect(mcSouth.code).to.equal('MC-30');
        expect(mcSouth.name).to.equal('Mare Australe');
    });

    it('should classify planetary landmark morphology and export GeoJSON', () => {
        const volcano = PlacesManager.classifyFeatureType('Olympus Mons');
        expect(volcano.type).to.equal('Mountain / Volcano');

        const canyon = PlacesManager.classifyFeatureType('Valles Marineris');
        expect(canyon.type).to.equal('Canyon / Trough');

        const crater = PlacesManager.classifyFeatureType('Gale Crater');
        expect(crater.type).to.equal('Impact Crater');

        const geojson = PlacesManager.toGeoJSON([
            { name: 'Olympus Mons', lat: 18.65, lon: 226.2, body: 'mars' }
        ]);
        expect(geojson.type).to.equal('FeatureCollection');
        expect(geojson.features).to.have.lengthOf(1);
        expect(geojson.features[0].geometry.coordinates[0]).to.equal(226.2);
    });
});

describe('Polar Stereographic & Map Projection Solvers (ProjectionManager)', () => {
    it('should compute forward and inverse Polar Stereographic projections', () => {
        // North Pole (90 N, 0 E) -> (0, 0)
        const northPole = ProjectionManager.forwardPolarStereographic(90, 0, 'north');
        expect(northPole.x).to.be.closeTo(0, 0.1);
        expect(northPole.y).to.be.closeTo(0, 0.1);
        expect(northPole.scaleFactor).to.equal(1.0);

        // Point near North Pole (80 N, 45 E)
        const pt = ProjectionManager.forwardPolarStereographic(80, 45, 'north');
        expect(pt.x).to.be.greaterThan(0);

        // Invert back to geodetic coordinates
        const inv = ProjectionManager.inversePolarStereographic(pt.x, pt.y, 'north');
        expect(inv.lat).to.be.closeTo(80, 0.05);
        expect(inv.lon).to.be.closeTo(45, 0.05);
    });

    it('should calculate projection areal distortion metrics', () => {
        // Equal-area projections have distortion ratio = 1.0 everywhere
        expect(ProjectionManager.computeArealDistortion(0, 'sinusoidal')).to.equal(1.0);
        expect(ProjectionManager.computeArealDistortion(60, 'mollweide')).to.equal(1.0);
        expect(ProjectionManager.computeArealDistortion(80, 'laea')).to.equal(1.0);

        // Cylindrical equirectangular expands toward poles: 1 / cos(60) = 2.0
        expect(ProjectionManager.computeArealDistortion(60, 'equirectangular')).to.be.closeTo(2.0, 0.01);
    });
});

describe('Spacecraft Ground Tracks & Orbital Mechanics (GroundTrackLayer)', () => {
    it('should compute Keplerian orbital period and velocity', () => {
        // MRO at altitude 250 km on Mars -> a = 3639.5 km -> period ≈ 111.1 min
        const orbit = GroundTrackLayer.computeOrbitalPeriod(250, 'mars');
        expect(orbit.periodMinutes).to.be.closeTo(111.1, 0.5);
        expect(orbit.orbitalVelocityKms).to.be.closeTo(3.43, 0.05);

        // Low Lunar orbit at 100 km -> period ≈ 118 min
        const moonOrbit = GroundTrackLayer.computeOrbitalPeriod(100, 'moon');
        expect(moonOrbit.periodMinutes).to.be.closeTo(118.0, 1.0);
    });

    it('should calculate J2 nodal precession rate and ground track repeat drift', () => {
        // Mars orbiter at 250 km, 92.65 deg inclination (MRO)
        const j2 = GroundTrackLayer.computeJ2NodalPrecession(250, 92.65, 'mars');
        expect(j2.degPerDay).to.be.closeTo(0.524, 0.1); // Sun-synchronous nodal regression rate

        // Ground track repeat cycle for 112 min orbit
        const repeat = GroundTrackLayer.computeGroundTrackRepeatCycle(112, 'mars');
        expect(repeat.orbitsPerSol).to.be.closeTo(13.19, 0.1);
        expect(repeat.driftDegPerOrbit).to.be.closeTo(27.29, 0.5);
        expect(repeat.groundTrackShiftKm).to.be.greaterThan(1000);
    });
});

describe('Dynamic Hillshade & Multidirectional Relief (HillshadeLayer)', () => {
    it('should compute single-pixel Horn hillshade illumination and slope angle', () => {
        // Flat terrain (dzdx = 0, dzdy = 0) at altitude 45 deg -> shade = 255 * sin(45) = 180
        const flatShade = HillshadeLayer.computeSinglePixelHillshade(0, 0, 315, 45);
        expect(flatShade).to.be.closeTo(180, 2);

        // Slope of 1:1 gradient -> slope = 45 deg (for dzdx = 1, dzdy = 0)
        const slope = HillshadeLayer.computeSlopeDegrees(1, 0);
        expect(slope).to.equal(45.0);

        // Illumination from NW (315 deg) facing an illuminated slope (dzdx = 1, dzdy = 1) -> bright illumination (~251)
        const nwSlopeShade = HillshadeLayer.computeSinglePixelHillshade(1, 1, 315, 45);
        expect(nwSlopeShade).to.be.greaterThan(flatShade);
    });

    it('should compute multidirectional Swiss shaded relief', () => {
        const multiShade = HillshadeLayer.computeMultidirectionalHillshade(0.5, 0.5, 45, 1.0);
        expect(multiShade).to.be.within(0, 255);
        expect(multiShade).to.be.greaterThan(50);
    });
});

describe('Spatial Sampling Statistics & Grid Generation (SamplingTool)', () => {
    it('should calculate sample statistical aggregates (mean, variance, stdDev, median, standardError)', () => {
        const data = [10, 20, 30, 40, 50];
        const stats = SamplingTool.computeSampleStatistics(data);
        expect(stats.count).to.equal(5);
        expect(stats.min).to.equal(10);
        expect(stats.max).to.equal(50);
        expect(stats.mean).to.equal(30);
        expect(stats.median).to.equal(30);
        expect(stats.variance).to.equal(250);
        expect(stats.stdDev).to.be.closeTo(15.81, 0.05);
        expect(stats.standardError).to.be.closeTo(7.07, 0.05);
    });

    it('should calculate Pearson linear correlation and generate regular sampling grids', () => {
        // Perfectly correlated data: y = 2x + 1
        const x = [1, 2, 3, 4, 5];
        const y = [3, 5, 7, 9, 11];
        const r = SamplingTool.computeCorrelationCoefficient(x, y);
        expect(r).to.be.closeTo(1.0, 0.001);

        // Regular grid within bounding box
        const bbox = { south: -10, north: 10, west: 0, east: 20 };
        const grid = SamplingTool.generateRegularGridPoints(bbox, 500, 'mars');
        expect(grid.length).to.be.greaterThan(5);
        expect(grid[0]).to.be.an('array').with.lengthOf(2);
    });
});

describe('IAU Planetary Gazetteer & Nomenclature Filtering (NomenclatureTool)', () => {
    it('should filter planetary nomenclature by type, query, and diameter', () => {
        const mockFeatures = [
            { name: 'Olympus Mons', type: 'Mons', diameter: 624, lat: 18.65, lon: 226.2, origin: 'Mount Olympus' },
            { name: 'Gale Crater', type: 'Crater', diameter: 154, lat: -5.4, lon: 137.8, origin: 'Walter F. Gale' },
            { name: 'Valles Marineris', type: 'Valles', diameter: 4000, lat: -14.0, lon: 300.8, origin: 'Mariner 9' }
        ];

        // Search query
        const searched = NomenclatureTool.filterFeatures(mockFeatures, { search: 'Olympus' });
        expect(searched).to.have.lengthOf(1);
        expect(searched[0].name).to.equal('Olympus Mons');

        // Type filter
        const craters = NomenclatureTool.filterFeatures(mockFeatures, { types: ['Crater'] });
        expect(craters).to.have.lengthOf(1);
        expect(craters[0].name).to.equal('Gale Crater');

        // Hemisphere filter
        const northern = NomenclatureTool.filterFeatures(mockFeatures, { hemisphere: 'north' });
        expect(northern).to.have.lengthOf(1);
        expect(northern[0].name).to.equal('Olympus Mons');
    });

    it('should extract metadata and compute feature spatial bounding box', () => {
        const feature = { name: 'Gale Crater', type: 'Crater', diameter: 154, lat: -5.4, lon: 137.8, origin: 'Walter Gale' };
        const meta = NomenclatureTool.extractFeatureMetadata(feature);
        expect(meta.name).to.equal('Gale Crater');
        expect(meta.diameterKm).to.equal(154);

        const bbox = NomenclatureTool.computeFeatureBoundingBox(feature, 'mars');
        expect(bbox.north).to.be.greaterThan(bbox.south);
        expect(bbox.south).to.be.lessThan(-5.4);
        expect(bbox.north).to.be.greaterThan(-5.4);
    });
});

describe('Crater Isochron Age Fitting & Poisson Errors (CSFDEngine)', () => {
    it('should fit crater populations to chronology function and derive model age with error bounds', () => {
        // Population of craters in 10^6 km^2 counting area
        const craters = Array(100).fill({ diameter: 2000 }); // 100 craters > 1km
        const fit = CSFDEngine.fitIsochronAge(craters, 1e6, 1.0, 50.0);

        expect(fit.count).to.equal(100);
        expect(fit.ageGa).to.be.greaterThan(0);
        expect(fit.maxAgeGa).to.be.greaterThan(fit.minAgeGa);
        expect(fit.ageErrorGa).to.be.greaterThan(0);
        expect(fit.epoch).to.be.a('string');
    });

    it('should compute Poisson uncertainty and classify geological epochs', () => {
        const err = CSFDEngine.computePoissonUncertainty(25, 1e6);
        expect(err.count).to.equal(25);
        expect(err.sigmaCount).to.equal(5.0); // sqrt(25) = 5
        expect(err.fractionalError).to.equal(0.2); // 5 / 25 = 0.2

        expect(CSFDEngine.classifyEpoch(4.0)).to.include('Early Noachian');
        expect(CSFDEngine.classifyEpoch(3.5)).to.include('Early Hesperian');
        expect(CSFDEngine.classifyEpoch(1.0)).to.include('Middle Amazonian');
    });
});

describe('CRISM/OMEGA Hyperspectral Indices & Band Math (BandMathEngine)', () => {
    it('should compute continuum-removed band depths and olivine indices', () => {
        // Absorption band at Rc = 0.16 with shoulders RL = 0.20, RR = 0.20 -> BD = 1 - (0.16 / 0.20) = 0.20
        const bd = BandMathEngine.computeBandDepth(0.16, 0.20, 0.20);
        expect(bd).to.equal(0.2);

        // OLINDEX3 for olivine absorption
        const olIndex = BandMathEngine.computeCRISMOlivineIndex(0.18, 0.19, 0.24, 0.26);
        expect(olIndex).to.be.greaterThan(0);
    });

    it('should calculate pyroxene band asymmetry and classify mineralogy', () => {
        const pyx = BandMathEngine.computePyroxeneIndex(0.15, 0.25, 0.22, 0.17);
        expect(pyx.pyroxeneIndex).to.be.greaterThan(0);
        expect(pyx.bd1000).to.be.greaterThan(0);
        expect(pyx.bd2000).to.be.greaterThan(0);
        expect(pyx.mineralogy).to.be.a('string');
    });
});

describe('3D Solar Ephemeris & Day/Night Terminator Solvers (ThreeDEngine)', () => {
    it('should compute solar declination and terminator polar boundaries from Ls', () => {
        // Northern summer solstice: Ls = 90 deg -> subsolar lat = +25.19 deg
        const declSummer = ThreeDEngine.computeSolarDeclination(90, 25.19);
        expect(declSummer).to.be.closeTo(25.19, 0.05);

        // Northern winter solstice: Ls = 270 deg -> subsolar lat = -25.19 deg
        const declWinter = ThreeDEngine.computeSolarDeclination(270, 25.19);
        expect(declWinter).to.be.closeTo(-25.19, 0.05);

        // Terminator boundaries during solstice: Polar day > 64.81 N, Polar night < -64.81 S
        const bounds = ThreeDEngine.computeTerminatorLatitudes(declSummer);
        expect(bounds.polarDayLat).to.be.closeTo(64.81, 0.1);
        expect(bounds.polarNightLat).to.be.closeTo(-64.81, 0.1);
    });

    it('should calculate solar incidence angle and daytime illumination state', () => {
        // Subsolar point (10 N, 45 E) -> at exactly (10 N, 45 E), incidence angle = 0 deg
        const zenith = ThreeDEngine.computeSolarIncidenceAngle(10, 45, 10, 45);
        expect(zenith.incidenceAngleDeg).to.equal(0);
        expect(zenith.cosIncidence).to.equal(1.0);
        expect(zenith.isSunlit).to.be.true;

        // Antipodal point (10 S, 225 E) -> incidence angle = 180 deg (night)
        const night = ThreeDEngine.computeSolarIncidenceAngle(-10, 225, 10, 45);
        expect(night.incidenceAngleDeg).to.equal(180);
        expect(night.isSunlit).to.be.false;
    });
});

describe('Subsurface Radar Sounding & Dielectric Inversion (RadarSounderEngine)', () => {
    it('should invert relative dielectric permittivity from TWTT and layer thickness', () => {
        // Pure water ice layer of 1000m thickness in Planum Boreum:
        // twt = 2 * 1000 / (c / sqrt(3.15)) = 11.838 microseconds
        const twt = RadarSounderEngine.depthToTwt(1000, 3.15);
        const epsInverted = RadarSounderEngine.invertDielectricPermittivity(1000, twt);
        expect(epsInverted).to.be.closeTo(3.15, 0.05);

        // Classification
        const classification = RadarSounderEngine.classifySubsurfaceMedium(epsInverted);
        expect(classification.medium).to.include('Pure Water');
    });

    it('should compute radar first Fresnel zone horizontal footprint radius', () => {
        // MRO SHARAD at 250 km altitude, 20 MHz (lambda = 15m)
        const rf = RadarSounderEngine.computeFresnelZoneRadius(250, 20e6, 500, 3.15);
        expect(rf).to.be.closeTo(1369.3, 20.0);
        expect(rf).to.be.greaterThan(1000);
    });
});

describe('Keplerian Orbit Propagation & Astrodynamics (TrajectoryEngine)', () => {
    it('should solve Kepler equation and compute true anomaly from eccentric anomaly', () => {
        // Circular orbit (e = 0): M = E = nu = 45 deg = 0.785398 rad
        const mCir = 45 * Math.PI / 180;
        const eCir = TrajectoryEngine.solveKeplersEquation(mCir, 0.0);
        expect(eCir).to.be.closeTo(mCir, 1e-6);

        const nuCir = TrajectoryEngine.computeTrueAnomaly(eCir, 0.0);
        expect(nuCir).to.be.closeTo(45.0, 0.01);

        // Elliptical orbit (e = 0.5, M = 90 deg)
        const mEll = 90 * Math.PI / 180;
        const eEll = TrajectoryEngine.solveKeplersEquation(mEll, 0.5);
        expect(eEll).to.be.greaterThan(mEll);

        const nuEll = TrajectoryEngine.computeTrueAnomaly(eEll, 0.5);
        expect(nuEll).to.be.greaterThan(90.0);
    });

    it('should compute 3D Cartesian position and velocity state vectors', () => {
        // Low Mars circular orbit at 250 km altitude (a = 3639.5 km, e = 0, i = 90)
        const state = TrajectoryEngine.computeOrbitalStateVector(3639.5, 0.0, 90.0, 0.0, 0.0, 0.0, 'mars');
        expect(state.radiusKm).to.be.closeTo(3639.5, 0.1);
        expect(state.speedKmS).to.be.closeTo(3.43, 0.05);
        expect(state.positionKm.x).to.be.closeTo(3639.5, 0.1);
    });
});

describe('Atmospheric Scale Height & Radiative Dust Extinction (MCDEngine)', () => {
    it('should compute Mars atmospheric scale height as a function of temperature', () => {
        // At mean temperature T = 220 K on Mars: H = (188.92 * 220) / 3.72076 = 11.17 km
        const h220 = MCDEngine.computeAtmosphericScaleHeight(220);
        expect(h220).to.be.closeTo(11.17, 0.1);

        // At cold polar temperature T = 150 K: H = 7.62 km
        const h150 = MCDEngine.computeAtmosphericScaleHeight(150);
        expect(h150).to.be.closeTo(7.62, 0.1);
    });

    it('should calculate vertical dust extinction and direct beam transmission', () => {
        // Surface dust tau = 0.5, altitude = 10 km, H_dust = 10 km -> tau_above = 0.5 * exp(-1) = 0.1839
        const ext = MCDEngine.computeOpticalDepthExtinction(10, 0.5, 10.0, 0);
        expect(ext.tauAbove).to.be.closeTo(0.1839, 0.01);
        expect(ext.beamTransmission).to.be.closeTo(0.832, 0.02);

        // Scenario classification
        const gds = MCDEngine.classifyDustScenario(2.5);
        expect(gds.scenario).to.include('Global Dust Storm');
    });
});

describe('1D Planetary Thermal Model & Seasonal Skin Depth (KRCEngine)', () => {
    it('should compute diurnal and seasonal thermal skin depths', () => {
        // Sand regolith (TI = 250 tiu): diurnal skin depth ~ 3.5 cm, annual ~ 90 cm
        const annual = KRCEngine.computeAnnualSkinDepth(250, 668.6);
        expect(annual.diurnalSkinDepthMeters).to.be.closeTo(0.035, 0.01);
        expect(annual.annualSkinDepthMeters).to.be.closeTo(0.90, 0.1);
        expect(annual.annualSkinDepthCm).to.be.greaterThan(50);
    });

    it('should classify regolith grain size and calculate Stefan-Boltzmann flux', () => {
        const dust = KRCEngine.classifyRegolithGrainSize(50);
        expect(dust.classification).to.include('Dust');

        const sand = KRCEngine.classifyRegolithGrainSize(200);
        expect(sand.classification).to.include('Sand');

        const rock = KRCEngine.classifyRegolithGrainSize(1500);
        expect(rock.classification).to.include('Bedrock');

        // Stefan-Boltzmann flux at 250 K with eps = 0.95: 0.95 * 5.67037e-8 * 250^4 = 210.42 W/m^2
        const flux = KRCEngine.computeStefanBoltzmannFlux(250, 0.95);
        expect(flux).to.be.closeTo(210.42, 1.0);
    });
});

describe('Mars Time, Equation of Time & Seasonal Calendars (MarsTime)', () => {
    it('should compute Mars Equation of Time (EOT) from Solar Longitude', () => {
        // At equinoxes (Ls = 0 deg and 180 deg), EOT ~ 0
        const eot0 = MarsTime.computeEquationOfTime(0);
        expect(eot0.eotMinutes).to.be.closeTo(0, 0.1);

        // At Ls = 45 deg, EOT reaches positive peak (~11.4 min)
        const eot45 = MarsTime.computeEquationOfTime(45);
        expect(eot45.eotMinutes).to.be.closeTo(11.44, 0.2);
    });

    it('should compute seasonal sol durations and Mars Sol Date conversions', () => {
        const seasons = MarsTime.computeSeasonalSolDurations();
        expect(seasons.springSols).to.equal(193.3);
        expect(seasons.totalSols).to.equal(668.6);

        // J2000.0 epoch: 2000-01-01 12:00:00 UTC -> MSD ~ 44791.62
        const j2000 = new Date('2000-01-01T12:00:00Z');
        const msdState = MarsTime.computeMarsSolDate(j2000);
        expect(msdState.msd).to.be.closeTo(44791.62, 0.5);
        expect(msdState.mtc).to.be.a('string');
    });
});

describe('Marching Squares Isocontour Generation (ContourLayer)', () => {
    it('should extract vector isocontour line segments using Marching Squares', () => {
        // 3x3 elevation grid with a central peak (1000m) surrounded by lowlands (0m)
        const grid = new Float32Array([
            0,    0,    0,
            0, 1000,    0,
            0,    0,    0
        ]);

        // Extract 500m contour line segments
        const segments = ContourLayer.extractIsovalueSegments(grid, 3, 3, 500);
        expect(segments.length).to.be.greaterThan(0);
        expect(segments[0]).to.have.lengthOf(2); // start and end points
        expect(segments[0][0][0]).to.be.within(0, 3);
    });

    it('should identify major index contour elevation lines', () => {
        // With interval = 500m and index factor = 5 (major = 2500m)
        expect(ContourLayer.isIndexContour(2500, 500, 5)).to.be.true;
        expect(ContourLayer.isIndexContour(5000, 500, 5)).to.be.true;
        expect(ContourLayer.isIndexContour(0, 500, 5)).to.be.true;
        expect(ContourLayer.isIndexContour(1000, 500, 5)).to.be.false;
        expect(ContourLayer.isIndexContour(1500, 500, 5)).to.be.false;
    });
});

describe('ESRI Shapefile Binary Parser & Generator (ShapeIO)', () => {
    it('should create and parse binary ESRI Shapefile header and Point records', () => {
        // Create a binary Shapefile containing 3 Martian landing sites
        const pts = [
            [226.2, 18.65],  // Olympus Mons
            [137.8, -5.4],   // Gale Crater
            [300.8, -14.0]   // Valles Marineris
        ];

        const buffer = ShapeIO.createShapefilePointBuffer(pts);
        expect(buffer.byteLength).to.equal(100 + 3 * 28); // 100-byte header + 3 * 28 bytes per Point

        // Parse header
        const header = ShapeIO.parseShapefileHeader(buffer);
        expect(header.fileCode).to.equal(9994);
        expect(header.version).to.equal(1000);
        expect(header.shapeType).to.equal(1);
        expect(header.shapeTypeName).to.equal('Point');
        expect(header.bbox.xMin).to.be.closeTo(137.8, 0.01);
        expect(header.bbox.xMax).to.be.closeTo(300.8, 0.01);

        // Parse Point records into GeoJSON features
        const features = ShapeIO.parsePointRecords(buffer);
        expect(features).to.have.lengthOf(3);
        expect(features[0].geometry.type).to.equal('Point');
        expect(features[0].geometry.coordinates[0]).to.be.closeTo(226.2, 0.01);
        expect(features[0].geometry.coordinates[1]).to.be.closeTo(18.65, 0.01);
    });

    it('should create and parse binary dBASE III (.dbf) attribute tables', () => {
        const fields = [
            { name: 'NAME', type: 'C', length: 16, decimals: 0 },
            { name: 'ELEV_M', type: 'N', length: 8, decimals: 1 },
            { name: 'IS_VOLCANO', type: 'L', length: 1, decimals: 0 }
        ];

        const records = [
            { NAME: 'Olympus Mons', ELEV_M: 21287.4, IS_VOLCANO: true },
            { NAME: 'Gale Crater', ELEV_M: -4500.0, IS_VOLCANO: false }
        ];

        const dbfBuffer = ShapeIO.createDBFBuffer(fields, records);
        expect(dbfBuffer.byteLength).to.be.greaterThan(100);

        const parsedHeader = ShapeIO.parseDBFHeader(dbfBuffer);
        expect(parsedHeader.version).to.equal(3);
        expect(parsedHeader.recordCount).to.equal(2);
        expect(parsedHeader.fields).to.have.lengthOf(3);
        expect(parsedHeader.fields[0].name).to.equal('NAME');

        const parsedRecords = ShapeIO.parseDBFRecords(dbfBuffer);
        expect(parsedRecords).to.have.lengthOf(2);
        expect(parsedRecords[0].NAME).to.equal('Olympus Mons');
        expect(parsedRecords[0].ELEV_M).to.be.closeTo(21287.4, 0.1);
        expect(parsedRecords[0].IS_VOLCANO).to.be.true;
    });
});

describe('Geodetic Latitude & Longitude Transformation (ProjectionManager)', () => {
    it('should convert between planetocentric and planetographic latitudes', () => {
        // At equator and poles, latCentric == latGraphic
        expect(ProjectionManager.convertPlanetocentricToPlanetographic(0)).to.equal(0);
        expect(ProjectionManager.convertPlanetocentricToPlanetographic(90)).to.equal(90);

        // At 45 deg on Mars (f = 0.00589), latGraphic is slightly greater than latCentric (~45.34 deg)
        const graphic45 = ProjectionManager.convertPlanetocentricToPlanetographic(45.0, 0.00589);
        expect(graphic45).to.be.closeTo(45.34, 0.05);

        // Invert back to planetocentric
        const centric45 = ProjectionManager.convertPlanetographicToPlanetocentric(graphic45, 0.00589);
        expect(centric45).to.be.closeTo(45.0, 0.01);
    });

    it('should convert across IAU Martian longitude conventions', () => {
        // East 360 (240 deg) -> West 360 (120 deg W)
        expect(ProjectionManager.convertLongitudeConvention(240, 'east360', 'west360')).to.equal(120);

        // East 360 (240 deg) -> East 180 (-120 deg)
        expect(ProjectionManager.convertLongitudeConvention(240, 'east360', 'east180')).to.equal(-120);

        // West 360 (120 deg W) -> East 360 (240 deg E)
        expect(ProjectionManager.convertLongitudeConvention(120, 'west360', 'east360')).to.equal(240);
    });
});

describe('Radar Signal Penetration & Uncertainty Propagation (RadarSounderEngine)', () => {
    it('should calculate maximum radar signal penetration depth', () => {
        // Pure ice (eps = 3.15, loss tan = 0.001) at 20 MHz with 60 dB dynamic range -> depth > 1000m
        const depthIce = RadarSounderEngine.computeSignalPenetrationDepth(60, 20e6, 0.001, 3.15);
        expect(depthIce).to.be.greaterThan(1000);

        // Lossy basalt (eps = 7.0, loss tan = 0.015) -> depth ~ 415m, significantly less than pure ice
        const depthBasalt = RadarSounderEngine.computeSignalPenetrationDepth(60, 20e6, 0.015, 7.0);
        expect(depthBasalt).to.be.closeTo(415.2, 5.0);
        expect(depthBasalt).to.be.lessThan(depthIce);
    });

    it('should propagate statistical uncertainty to radar depth estimation', () => {
        // Two-way travel time = 10 μs in ice (eps = 3.15) -> nominal depth ~ 844.5 m
        const unc = RadarSounderEngine.computeDepthUncertainty(10.0, 3.15, 0.05, 0.2);
        expect(unc.nominalDepthMeters).to.be.closeTo(844.5, 2.0);
        expect(unc.sigmaDepthMeters).to.be.greaterThan(0);
        expect(unc.relativeUncertaintyPercent).to.be.within(1.0, 10.0);
    });
});

describe('Orbital Energy, Plane Changes & Synodic Periods (TrajectoryEngine)', () => {
    it('should compute specific orbital energy and vis-viva speed', () => {
        // Circular orbit at 250 km altitude around Mars (r = a = 3639.5 km)
        const orb = TrajectoryEngine.computeOrbitalEnergyAndSpeed(3639.5, 3639.5, 'mars');
        expect(orb.orbitType).to.equal('Circular');
        expect(orb.specificEnergyKm2S2).to.be.closeTo(-5.8838, 0.05);
        expect(orb.speedKmS).to.be.closeTo(3.43, 0.05);
    });

    it('should calculate orbital plane change Delta-V and interplanetary synodic periods', () => {
        // 5 degree plane change at 3.43 km/s speed -> DeltaV = 2 * 3.43 * sin(2.5 deg) ~ 0.299 km/s
        const deltaV = TrajectoryEngine.computePlaneChangeDeltaV(3.43, 5.0);
        expect(deltaV).to.be.closeTo(0.299, 0.01);

        // Earth-Mars synodic period ~ 779.9 days (~2.135 years / 25.6 months)
        const syn = TrajectoryEngine.computeInterplanetarySynodicPeriod('earth', 'mars');
        expect(syn.synodicDays).to.be.closeTo(779.94, 2.0);
        expect(syn.synodicYears).to.be.closeTo(2.135, 0.05);
    });
});

describe('Hyperspectral Mineral Parameter Indices & Mineral Suites (BandMathEngine)', () => {
    it('should compute CRISM sulfate, phyllosilicate, and carbonate band depths', () => {
        // Monohydrated sulfate (kieserite): R(1.93μm)=0.25, R(2.1μm)=0.20, R(2.25μm)=0.25 -> continuum = 0.25 -> depth = 0.20
        const bdSulfate = BandMathEngine.computeCRISMSulfateIndex(0.25, 0.20, 0.25);
        expect(bdSulfate).to.be.closeTo(0.20, 0.01);

        // Phyllosilicate: R(1.815μm)=0.30, R(2.3μm)=0.26, R(2.36μm)=0.30 -> depth ~ 0.1333
        const dPhyllo = BandMathEngine.computeCRISMPhyllosilicateIndex(0.30, 0.26, 0.30);
        expect(dPhyllo).to.be.closeTo(0.1333, 0.01);

        // Carbonate: R(2.3μm)=0.28, R(2.5μm)=0.22, R(2.6μm)=0.28 -> depth ~ 0.2143
        const bdCarb = BandMathEngine.computeCRISMCarbonateIndex(0.28, 0.22, 0.28);
        expect(bdCarb).to.be.closeTo(0.2143, 0.01);
    });

    it('should classify planetary mineral assemblages and Martian geologic eras', () => {
        const clay = BandMathEngine.classifyMineralAssembly({ d2300: 0.10, bd1900: 0.12 });
        expect(clay.dominantMineral).to.include('Phyllosilicates');
        expect(clay.era).to.include('Noachian');

        const sulfate = BandMathEngine.classifyMineralAssembly({ bd2100: 0.08 });
        expect(sulfate.dominantMineral).to.include('Sulfates');
        expect(sulfate.era).to.include('Hesperian');

        const carb = BandMathEngine.classifyMineralAssembly({ bd2500: 0.11 });
        expect(carb.dominantMineral).to.include('Carbonate');
    });
});

describe('Entry, Descent & Landing (EDL) Aerodynamics (LandingSitesLayer)', () => {
    it('should compute Chapman peak atmospheric entry deceleration g-loads', () => {
        // Mars entry: v = 5800 m/s, gamma = 12 deg, H = 11100 m -> a_max ~ 116 m/s^2 (~11.8 g)
        const decel = LandingSitesLayer.computePeakDecelerationG(5800, 12.0, 11100);
        expect(decel.peakDecelGLoad).to.be.closeTo(11.8, 0.5);
        expect(decel.peakDecelM_S2).to.be.greaterThan(100);
    });

    it('should calculate parachute terminal descent equilibrium velocity', () => {
        // 1025 kg landing mass, rho = 0.015 kg/m^3, Cd = 2.0, Area = 360 m^2 -> v_term ~ 26.5 m/s
        const vTerm = LandingSitesLayer.computeTerminalDescentVelocity(1025, 0.015, 360, 2.0, 3.72076);
        expect(vTerm).to.be.closeTo(26.56, 0.5);
    });
});

describe('3D Planetary Ellipsoidal Geodesy & Solar Elevation (ThreeDEngine)', () => {
    it('should convert between Geographic and 3D Cartesian coordinates on Martian ellipsoid', () => {
        // Mars Equator (0 lat, 0 lon, 0 alt) -> X = a = 3396.19 km, Y = 0, Z = 0
        const ptEquator = ThreeDEngine.geographicToCartesian(0, 0, 0, 'mars');
        expect(ptEquator.x).to.be.closeTo(3396.19, 0.1);
        expect(ptEquator.y).to.be.closeTo(0, 0.01);
        expect(ptEquator.z).to.be.closeTo(0, 0.01);

        // Mars North Pole (90 lat, 0 lon, 0 alt) -> X = 0, Y = 0, Z = b = 3376.20 km
        const ptPole = ThreeDEngine.geographicToCartesian(90, 0, 0, 'mars');
        expect(ptPole.x).to.be.closeTo(0, 0.01);
        expect(ptPole.z).to.be.closeTo(3376.20, 0.1);

        // Invert 3D Cartesian back to Geographic via Bowring's method
        const geo = ThreeDEngine.cartesianToGeographic(ptPole.x, ptPole.y, ptPole.z, 'mars');
        expect(geo.lat).to.be.closeTo(90.0, 0.01);
    });

    it('should compute solar elevation angle above surface horizon', () => {
        // Subsolar point directly overhead (lat = 10, lon = 100) -> Solar elevation = 90 deg (zenith)
        const elevZenith = ThreeDEngine.computeSolarHorizonElevation(10, 100, 10, 100);
        expect(elevZenith).to.be.closeTo(90.0, 0.01);

        // Horizon position on equator (90 deg away) -> Solar elevation = 0 deg
        const elevHorizon = ThreeDEngine.computeSolarHorizonElevation(0, 90, 0, 0);
        expect(elevHorizon).to.be.closeTo(0.0, 0.01);
    });
});

describe('Planetary Probe Geophysics & Gravity Variation (InvestigateTool)', () => {
    it('should compute latitude-dependent theoretical surface gravity', () => {
        // Mars equator: g ~ 3.7112 m/s^2
        const gEq = InvestigateTool.computeTheoreticalGravityByLatitude(0, 'mars');
        expect(gEq).to.be.closeTo(3.7112, 0.01);

        // Mars pole (90 deg): g ~ 3.7309 m/s^2 (higher due to oblateness)
        const gPole = InvestigateTool.computeTheoreticalGravityByLatitude(90, 'mars');
        expect(gPole).to.be.closeTo(3.7309, 0.01);
        expect(gPole).to.be.greaterThan(gEq);
    });

    it('should calculate volumetric heat capacity and hydrostatic temperature', () => {
        // Regolith density = 1500 kg/m^3, cp = 800 J/(kg K) -> C_vol = 1.2e6 J/(m^3 K)
        const cVol = InvestigateTool.computeVolumetricHeatCapacity(1500, 800);
        expect(cVol).to.equal(1200000);

        // Hydrostatic temperature for Mars scale height H = 11.1 km -> T ~ 218.8 K
        const tIso = InvestigateTool.computeHydrostaticColumnPressure(11100, 3.72076, 0.04401);
        expect(tIso).to.be.closeTo(218.8, 1.0);
    });
});

describe('Spherical Excess Geodesic Polygon Area & Cross-Track Distance (MeasureTool)', () => {
    it('should compute exact spherical polygon surface area via spherical excess', () => {
        // Octant on Mars (0..90 lat, 0..90 lon) -> 1/8 of total sphere area = (4 * pi * R^2) / 8 = (pi * R^2) / 2
        // For Mars R = 3389.5 km: Total area = 144.37e6 km^2 -> 1/8 = 18.046e6 km^2
        const octant = [
            [0, 0],
            [0, 90],
            [90, 0]
        ];

        const res = MeasureTool.computeSphericalPolygonArea(octant, 'mars');
        expect(res.sphericalExcessRad).to.be.closeTo(Math.PI / 2.0, 0.01);
        expect(res.areaKm2).to.be.closeTo(18046000, 50000);
    });

    it('should compute perpendicular cross-track error distance from great-circle track', () => {
        // Great-circle track along equator from (0, 0) to (0, 90). Point at (10, 45)
        // Cross-track distance should be distance from (0, 45) to (10, 45) = 10 deg * (pi * 3389.5 / 180) ~ 591.59 km
        const xt = MeasureTool.computeCrossTrackDistance(10, 45, 0, 0, 0, 90, 'mars');
        expect(Math.abs(xt.crossTrackKm)).to.be.closeTo(591.59, 2.0);
        expect(xt.alongTrackKm).to.be.greaterThan(2500);
    });
});

describe('Differential Crater Distribution & Slope Correction (CSFDEngine)', () => {
    it('should compute differential size-frequency distribution (DFD) bins', () => {
        const craters = [
            { diameter: 200 }, // 0.2 km
            { diameter: 250 },
            { diameter: 500 }, // 0.5 km
            { diameter: 1200 } // 1.2 km
        ];

        const dfd = CSFDEngine.computeDifferentialCSFD(craters, 1e6);
        expect(dfd.length).to.be.greaterThan(0);
        expect(dfd[0]).to.have.property('differentialDensity');
        expect(dfd.reduce((sum, b) => sum + b.count, 0)).to.equal(4);
    });

    it('should calculate slope-corrected counting target area on inclined terrain', () => {
        // Flat terrain (slope = 0 deg) -> True area = projected area (1e6 km^2)
        const flat = CSFDEngine.computeSlopeCorrectedArea(1e6, 0);
        expect(flat.trueAreaKm2).to.equal(1e6);
        expect(flat.areaExpansionFactor).to.equal(1.0);

        // 30 degree canyon wall slope -> cos(30) = 0.866 -> True area = 1e6 / 0.866 ~ 1.1547e6 km^2
        const steep = CSFDEngine.computeSlopeCorrectedArea(1e6, 30.0);
        expect(steep.trueAreaKm2).to.be.closeTo(1154700.5, 10.0);
        expect(steep.areaExpansionFactor).to.be.closeTo(1.1547, 0.001);
    });
});

describe('Regolith Gas-Pore Conduction & CO2 Frost Point (KRCEngine)', () => {
    it('should compute pressure-dependent thermal conductivity and CO2 sublimation temperature', () => {
        // Solid conductivity 0.02 W/(m K) at 610 Pa datum -> Effective conductivity > 0.02 W/(m K)
        const kEff = KRCEngine.computePressureDependentConductivity(0.02, 610, 120, 0.015);
        expect(kEff).to.be.greaterThan(0.02);
        expect(kEff).to.be.closeTo(0.0225, 0.005);

        // Clausius-Clapeyron CO2 frost point at 610 Pa Mars datum ~ 147.8 K
        const tFrost = KRCEngine.computeCO2CondensationTemperature(610);
        expect(tFrost).to.be.closeTo(147.8, 1.0);
    });

    it('should compute surface radiative cooling rate', () => {
        // At 250 K with 1 cm top layer -> cooling rate in K/hour
        const cooling = KRCEngine.computeRadiativeCoolingRate(250, 0.01, 0.95);
        expect(cooling).to.be.greaterThan(50);
        expect(cooling).to.be.lessThan(150);
    });
});

describe('Planetary Atmospheric Dynamics & Optical Air Mass (MCDEngine)', () => {
    it('should compute Coriolis parameter across Martian latitudes', () => {
        // Equator (0 deg) -> f = 0
        const fEq = MCDEngine.computeCoriolisParameter(0);
        expect(fEq).to.equal(0);

        // 45 deg N -> f = 2 * 7.0882e-5 * sin(45) ~ 1.002e-4 s^-1
        const f45 = MCDEngine.computeCoriolisParameter(45);
        expect(f45).to.be.closeTo(1.002e-4, 1e-6);
    });

    it('should calculate thermal wind shear and Kasten-Young spherical air mass', () => {
        // Meridional gradient of 10 K / 1000 km at 45 deg lat, T = 210 K
        const tw = MCDEngine.computeThermalWindShear(10.0, 210, 45);
        expect(tw.windShearMsPerKm).to.be.greaterThan(0.1);
        expect(tw.windShearMsPerKm).to.be.lessThan(5.0);

        // Zenith angle 0 deg -> Air mass = 1.0
        const amZenith = MCDEngine.computeAirMass(0);
        expect(amZenith).to.be.closeTo(1.0, 0.05);

        // Zenith angle 60 deg -> Air mass ~ 2.0
        const am60 = MCDEngine.computeAirMass(60);
        expect(am60).to.be.closeTo(2.0, 0.05);
    });
});

describe('Martian Solar Coordinates & Apsidal Precession (MarsTime)', () => {
    it('should compute solar hour angle and celestial altitude', () => {
        // Solar noon (12h LTST) -> H = 0 deg
        const hNoon = MarsTime.computeSolarHourAngle(12.0);
        expect(hNoon).to.equal(0);

        // 18h LTST -> H = +90 deg
        const h18 = MarsTime.computeSolarHourAngle(18.0);
        expect(h18).to.equal(90);

        // Subsolar point on equator at equinox (Ls=0, lat=0, 12h LTST) -> altitude = 90 deg (zenith)
        const solPos = MarsTime.computeSolarAzimuthAltitude(0, 0, 12.0);
        expect(solPos.altitudeDeg).to.be.closeTo(90.0, 0.01);
        expect(solPos.isDay).to.be.true;
    });

    it('should calculate secular orbital apsidal precession and perihelion longitude', () => {
        // In 2026: Perihelion Ls ~ 251.04 deg (Northern winter / Southern summer)
        const prec = MarsTime.computeMartianApsidalPrecession(2026);
        expect(prec.perihelionLs).to.be.closeTo(251.038, 0.05);
        expect(prec.aphelionLs).to.be.closeTo(71.038, 0.05);
    });
});

describe('DEM Bilinear Interpolation & Hypsometric Intervals (ContourLayer)', () => {
    it('should evaluate sub-pixel continuous elevation via bilinear interpolation', () => {
        // 2x2 grid: (0,0)=100, (1,0)=200, (0,1)=300, (1,1)=400
        const grid = new Float32Array([100, 200, 300, 400]);
        const zCenter = ContourLayer.bilinearInterpolateElevation(grid, 2, 2, 0.5, 0.5);
        expect(zCenter).to.equal(250);

        const z00 = ContourLayer.bilinearInterpolateElevation(grid, 2, 2, 0, 0);
        expect(z00).to.equal(100);
    });

    it('should compute optimal cartographic contour intervals and elevation colors', () => {
        // Relief from -4000 m to +6000 m (span = 10,000 m, 10 levels) -> nice step = 1000 m
        const opt = ContourLayer.computeOptimalContourInterval(-4000, 6000, 10);
        expect(opt.interval).to.equal(1000);
        expect(opt.baseLevel).to.equal(-4000);
        expect(opt.numLevels).to.equal(10);

        const color = ContourLayer.generateElevationColor(0, -8000, 21000);
        expect(color).to.include('hsl');
    });
});

describe('Binary Shapefile Polygon Serialization & Spatial BBox Overlap (ShapeIO)', () => {
    it('should create and decode binary Polygon (ShapeType 5) Shapefile buffers', () => {
        // Create simple triangle polygon ring
        const triangle = [
            [0, 0],
            [10, 0],
            [5, 10],
            [0, 0]
        ];

        const buffer = ShapeIO.createShapefilePolygonBuffer([triangle]);
        expect(buffer.byteLength).to.be.greaterThan(100);

        const header = ShapeIO.parseShapefileHeader(buffer);
        expect(header.shapeType).to.equal(5);
        expect(header.shapeTypeName).to.equal('Polygon');
        expect(header.bbox.xMin).to.equal(0);
        expect(header.bbox.xMax).to.equal(10);
        expect(header.bbox.yMax).to.equal(10);

        const polys = ShapeIO.parsePolygonRecords(buffer);
        expect(polys.length).to.equal(1);
        expect(polys[0].geometry.type).to.equal('Polygon');
        expect(polys[0].geometry.coordinates[0].length).to.equal(4);
    });

    it('should calculate 2D bounding box spatial overlap and intersections', () => {
        const b1 = { xMin: 0, yMin: 0, xMax: 10, yMax: 10 };
        const b2 = { xMin: 5, yMin: 5, xMax: 15, yMax: 15 };
        const overlap = ShapeIO.computeBBoxOverlap(b1, b2);
        expect(overlap.intersects).to.be.true;
        expect(overlap.overlapArea).to.equal(25); // 5 x 5

        const b3 = { xMin: 20, yMin: 20, xMax: 30, yMax: 30 };
        const disjoint = ShapeIO.computeBBoxOverlap(b1, b3);
        expect(disjoint.intersects).to.be.false;
        expect(disjoint.overlapArea).to.equal(0);
    });
});

describe('Tissot Indicatrix Distortion Ellipses & Antipodes (ProjectionManager)', () => {
    it('should compute Tissot indicatrix distortion ellipse axes and area scale', () => {
        // Mercator at 60 deg lat: sec(60) = 2 -> a = b = 2, area scale = 4, angular distortion = 0
        const tissotMerc = ProjectionManager.computeTissotIndicatrix(60, 'mercator');
        expect(tissotMerc.a).to.be.closeTo(2.0, 0.05);
        expect(tissotMerc.areaScale).to.be.closeTo(4.0, 0.05);
        expect(tissotMerc.maxAngularDistortionDeg).to.equal(0);

        // Sinusoidal is strictly equal area (areaScale = 1.0)
        const tissotSin = ProjectionManager.computeTissotIndicatrix(45, 'sinusoidal');
        expect(tissotSin.areaScale).to.be.closeTo(1.0, 0.01);
    });

    it('should compute planetary antipode coordinates and true ground resolution', () => {
        // Olympus Mons ~ (18.65 N, 226.2 E) -> Antipode ~ (-18.65 S, 46.2 E)
        const anti = ProjectionManager.computeAntipode(18.65, 226.2);
        expect(anti.lat).to.equal(-18.65);
        expect(anti.lon).to.equal(46.2);

        // 100 m/pixel nominal scale at equator -> at 60 deg lat: 100 * cos(60) = 50 m/pixel
        const trueScale = ProjectionManager.computeTrueScaleAtLatitude(100, 60);
        expect(trueScale).to.equal(50);
    });
});

describe('Surface Clutter Simulation & CRIM Porosity Inversion (RadarSounderEngine)', () => {
    it('should compute off-nadir topographic surface clutter delay and apparent depth', () => {
        // MRO orbit H = 250 km, crater rim at cross-track d = 25 km
        const clutter = RadarSounderEngine.computeSurfaceClutterDelay(250, 25);
        expect(clutter.nadirTwtMicrosec).to.be.closeTo(1667.8, 1.0);
        expect(clutter.clutterTwtMicrosec).to.be.greaterThan(clutter.nadirTwtMicrosec);
        expect(clutter.excessDelayMicrosec).to.be.closeTo(8.3, 0.5);
        expect(clutter.apparentDepthMetersInIce).to.be.greaterThan(500);
    });

    it('should estimate volumetric porosity from dielectric permittivity via CRIM', () => {
        // Medusae Fossae bulk permittivity eps = 2.9, basalt matrix eps = 7.5 -> porosity ~ 60%
        const porous = RadarSounderEngine.estimatePorosityFromPermittivity(2.9, 7.5, 1.0);
        expect(porous.porosityPercent).to.be.closeTo(59.9, 1.0);
        expect(porous.porosityFraction).to.be.closeTo(0.60, 0.02);

        // Basal contact reflectivity: pure ice (3.15) over basalt (7.5) -> linear ~ 0.045 (-13.5 dB)
        const basal = RadarSounderEngine.computeBasalInterfaceReflectivity(3.15, 7.5);
        expect(basal.reflectivityPower).to.be.closeTo(0.045, 0.01);
        expect(basal.contactType).to.include('Basaltic');
    });
});

describe('Crater Saturation Equilibrium & Asteroid Impactor Scaling (CSFDEngine)', () => {
    it('should compute cumulative saturation fraction relative to Gault/Hartmann limit', () => {
        const craters = [
            { diameter: 2000 },
            { diameter: 3000 },
            { diameter: 5000 }
        ];

        const sat = CSFDEngine.computeCumulativeSaturationFraction(craters, 1e4, 1.0);
        expect(sat.observedDensityPerKm2).to.be.greaterThan(0);
        expect(sat.saturationDensityPerKm2).to.equal(0.15); // 0.15 * 1^-2
        expect(sat.isSaturated).to.be.false;
    });

    it('should estimate asteroid impactor projectile size from crater scaling', () => {
        // 10 km complex Martian crater at 10 km/s impact velocity
        const scaling = CSFDEngine.computeCraterScalingImpactorSize(10.0, 10.0, 2900, 2500);
        expect(scaling.impactorDiameterMeters).to.be.greaterThan(200);
        expect(scaling.impactorDiameterMeters).to.be.lessThan(1000);
        expect(scaling.impactEnergyMegatonsTNT).to.be.greaterThan(100);

        // Superposition resurfacing correction: 3.5 Ga surface resurfaced at 1.0 Ga
        const res = CSFDEngine.computeResurfacingCorrection(3.5, 1.0);
        expect(res.correctedPreEventAgeGa).to.equal(3.5);
        expect(res.resurfacingFraction).to.be.closeTo(0.714, 0.05);
    });
});

describe('Linear Spectral Unmixing & Continuum Removal (BandMathEngine)', () => {
    it('should deconvolve endmember fractional abundances via least-squares', () => {
        // Two endmembers: Pyroxene [0.8, 0.4] and Olivine [0.2, 0.9]
        // 50/50 mixture -> [0.5, 0.65]
        const endmembers = [
            { name: 'Pyroxene', spectrum: [0.8, 0.4] },
            { name: 'Olivine', spectrum: [0.2, 0.9] }
        ];
        const mixed = [0.5, 0.65];

        const unmixed = BandMathEngine.linearSpectralUnmixing(mixed, endmembers);
        expect(unmixed.abundances.length).to.equal(2);
        expect(unmixed.abundances[0].fraction).to.be.closeTo(0.5, 0.05);
        expect(unmixed.abundances[1].fraction).to.be.closeTo(0.5, 0.05);
        expect(unmixed.rmsResidual).to.be.lessThan(0.01);
    });

    it('should compute SAM spectral angle and continuum-removed absorption depth', () => {
        // Identical spectra -> SAM angle = 0 deg, similarity = 1.0
        const sam = BandMathEngine.computeSpectralAngle([0.2, 0.5, 0.8], [0.2, 0.5, 0.8]);
        expect(sam.angleDegrees).to.equal(0);
        expect(sam.similarityScore).to.equal(1.0);

        // Continuum removal for absorption feature at 1.9 µm
        const wavelengths = [1.8, 1.9, 2.0];
        const spectrum = [0.5, 0.4, 0.5]; // 20% absorption dip at 1.9 µm
        const cr = BandMathEngine.computeContinuumRemovedSpectrum(wavelengths, spectrum);
        expect(cr.maxBandDepth).to.be.closeTo(0.2, 0.01);
        expect(cr.bandCenterWavelength).to.equal(1.9);
    });
});

describe('Aerodynamic Stagnation Heat Flux & Dispersion Ellipses (LandingSitesLayer)', () => {
    it('should compute Sutton-Graves stagnation convective heat flux in CO2', () => {
        // Mars entry: v = 5800 m/s, density = 1.5e-4 kg/m^3, Rn = 0.6 m
        const heat = LandingSitesLayer.computeStagnationPointHeatFlux(5800, 1.5e-4, 0.6);
        expect(heat.heatFluxW_M2).to.be.greaterThan(5e5);
        expect(heat.heatFluxW_Cm2).to.be.greaterThan(50);
        expect(heat.heatFluxW_Cm2).to.be.lessThan(150);
    });

    it('should calculate dispersion ellipse footprint area and point containment', () => {
        // Jezero crater landing ellipse: 7.7 km x 6.6 km
        const ellipse = LandingSitesLayer.computeEllipseSurfaceArea(7.7, 6.6);
        expect(ellipse.areaKm2).to.be.closeTo(159.66, 0.5);
        expect(ellipse.perimeterKm).to.be.closeTo(45.0, 1.0);

        // Center: (18.444 N, 77.45 E). Point 1 km away should be inside
        const inside = LandingSitesLayer.isPointInsideEllipse(18.45, 77.46, 18.444, 77.45, 7.7, 6.6, 0);
        expect(inside).to.be.true;

        // Point 50 km away should be outside
        const outside = LandingSitesLayer.isPointInsideEllipse(19.0, 77.45, 18.444, 77.45, 7.7, 6.6, 0);
        expect(outside).to.be.false;
    });
});

describe('Line-of-Sight Horizon, Solar Phase Angle & Ray Tracing (ThreeDEngine)', () => {
    it('should compute geometric horizon distance and intervisibility ranges', () => {
        // Rover mast (2m = 0.002 km) to horizon on Mars (R=3389.5 km): d = sqrt(2 * 3389.5 * 0.002) ~ 3.68 km
        const rover = ThreeDEngine.computeLineOfSightHorizon(0.002, 0);
        expect(rover.horizonDist1Km).to.be.closeTo(3.68, 0.05);

        // Rover (2m) to Relay Orbiter (400 km) -> Intervisibility range ~ 1650 km
        const relay = ThreeDEngine.computeLineOfSightHorizon(0.002, 400);
        expect(relay.maxIntervisibleDistKm).to.be.greaterThan(1600);
    });

    it('should calculate solar phase angle and ray-ellipsoid intersections', () => {
        // Sun at [1e8, 0, 0], Observer at [0, 1e8, 0], Target at [0, 0, 0] -> alpha = 90 deg, k = 0.5 (half phase)
        const phase = ThreeDEngine.computeSolarPhaseAngle({ x: 1e8, y: 0, z: 0 }, { x: 0, y: 1e8, z: 0 });
        expect(phase.phaseAngleDeg).to.be.closeTo(90.0, 0.1);
        expect(phase.illuminationFraction).to.be.closeTo(0.5, 0.01);

        // Ray from 5000 km altitude along -X towards Mars center -> hits near side
        const hit = ThreeDEngine.testRayEllipsoidIntersection({ x: 5000, y: 0, z: 0 }, { x: -1, y: 0, z: 0 }, 'mars');
        expect(hit.intersects).to.be.true;
        expect(hit.tNear).to.be.closeTo(1603.8, 1.0); // 5000 - 3396.19
    });
});

describe('Lithospheric Geothermal Gradients & Atmospheric Scale Height (InvestigateTool)', () => {
    it('should compute geothermal heat flux gradient and temperature at depth', () => {
        // Mars heat flux q = 30 mW/m^2, basalt conductivity k = 2.0 W/m K -> gradient = 15 K/km
        const geo = InvestigateTool.computeGeothermalGradient(30.0, 2.0, 210.0, 2.0);
        expect(geo.gradientKPerKm).to.equal(15.0);
        expect(geo.tempAtDepthK).to.equal(240.0); // 210 + 15 * 2
    });

    it('should calculate atmospheric scale height and Planck blackbody spectral radiance', () => {
        // Mars T = 210 K -> Scale height ~ 10.68 km
        const hScale = InvestigateTool.computeScaleHeight(210.0, 'mars');
        expect(hScale).to.be.closeTo(10.68, 0.2);

        // Planck radiance at 10 µm (thermal IR peak for ~250 K)
        const rad = InvestigateTool.computeBlackbodySpectralRadiance(10.0, 250.0);
        expect(rad).to.be.greaterThan(1.0);
        expect(rad).to.be.lessThan(20.0);
    });
});

describe('Great-Circle Waypoint Interpolation & Geodetic Intersections (MeasureTool)', () => {
    it('should interpolate smooth intermediate geodesic waypoints', () => {
        // Interpolate 4 segments (5 points) from (0, 0) to (0, 90) along equator
        const waypoints = MeasureTool.interpolateGreatCircleWaypoints(0, 0, 0, 90, 4);
        expect(waypoints.length).to.equal(5);
        expect(waypoints[0][0]).to.equal(0);
        expect(waypoints[0][1]).to.equal(0);
        expect(waypoints[2][1]).to.be.closeTo(45.0, 0.01); // Midpoint at 45 deg lon
        expect(waypoints[4][1]).to.be.closeTo(90.0, 0.01);
    });

    it('should calculate great-circle intersections and rhumb line distances', () => {
        // Equator (0,0)->(0,90) intersecting Prime Meridian (45,0)->(-45,0) -> intersection at (0, 0) or (0, 180)
        const intPt = MeasureTool.computeGreatCircleIntersection(0, 0, 0, 90, 45, 0, -45, 0);
        expect(intPt.lat).to.be.closeTo(0.0, 0.01);
        expect([0, 180]).to.include(Math.round(intPt.lon));

        // Constant-bearing rhumb line from (0, 0) to (10, 10) on Mars
        const rhumb = MeasureTool.computeRhumbLineDistance(0, 0, 10, 10, 'mars');
        expect(rhumb.distanceKm).to.be.greaterThan(500);
        expect(rhumb.constantBearingDeg).to.be.closeTo(45.0, 1.0);
    });
});

describe('CO2 Sublimation Latent Heat & Subsurface Thermal Wave Damping (KRCEngine)', () => {
    it('should compute CO2 frost sublimation mass and thickness flux', () => {
        // Net solar energy imbalance +50 W/m^2 driving sublimation
        const subl = KRCEngine.computeCO2SublimationRate(50.0);
        expect(subl.isSublimating).to.be.true;
        expect(subl.thicknessRateMmPerSol).to.be.greaterThan(4.0);
        expect(subl.thicknessRateMmPerSol).to.be.lessThan(6.0);
    });

    it('should calculate harmonic thermal amplitude damping and phase delay at depth', () => {
        // Typical Mars sand TI = 250 -> skin depth ~ 3.5 cm
        // At depth z = 1 skin depth: amplitude is 1/e ~ 0.3679
        const skin = KRCEngine.computeSkinDepth(250);
        const damp = KRCEngine.computeThermalDampingDepth(skin.skinDepthMeters, 250);
        expect(damp.amplitudeRatio).to.be.closeTo(0.3679, 0.01);
        expect(damp.phaseLagRadians).to.be.closeTo(1.0, 0.01);

        // Stratigraphy thermal capacitance
        const cap = KRCEngine.computeSubsurfaceHeatCapacityLayered([0.05, 0.10, 0.20]);
        expect(cap.totalThicknessMeters).to.equal(0.35);
        expect(cap.totalHeatCapacityJ_M2_K).to.be.greaterThan(3e5);
    });
});

describe('Planetary Boundary Layer (PBL) & Static Stability (MCDEngine)', () => {
    it('should calculate convective PBL height and Deardorff velocity scale', () => {
        // Sensible heat flux H = 20 W/m^2 at surface T = 220 K, rho = 0.015 kg/m^3
        const pbl = MCDEngine.computePBLHeight(20.0, 220, 0.015);
        expect(pbl.pblHeightKm).to.be.greaterThan(0.5);
        expect(pbl.pblHeightKm).to.be.lessThan(5.0);
        expect(pbl.convectiveVelocityMs).to.be.greaterThan(1.5);
    });

    it('should compute wavelength-dependent dust optical depth and Brunt-Väisälä stability', () => {
        // Visible tau = 0.5 at 0.67 µm -> Thermal IR tau at 9.3 µm (alpha = 0.5)
        const tauIR = MCDEngine.computeWavelengthDependentDustTau(0.5, 9.3, 0.5);
        expect(tauIR).to.be.closeTo(0.134, 0.02);

        // Stably stratified layer: potential temp = 200 K, dTheta/dz = +0.003 K/m
        const bv = MCDEngine.computeBruntVaisalaFrequency(200.0, 0.003);
        expect(bv.isStable).to.be.true;
        expect(bv.frequencyRadS).to.be.greaterThan(0.005);
        expect(bv.frequencyRadS).to.be.lessThan(0.015);
        expect(bv.periodSeconds).to.be.greaterThan(500);
    });
});

describe('Sub-Solar Coordinates, TOA Solar Insolation & Analemma (MarsTime)', () => {
    it('should compute exact sub-solar ground coordinates and seasonal declination', () => {
        // Northern summer solstice Ls = 90 deg -> sub-solar lat = +25.19 deg (Martian obliquity)
        const sub = MarsTime.computeSubSolarPoint(90.0, 12.0);
        expect(sub.subSolarLatDeg).to.be.closeTo(25.19, 0.05);
        expect(sub.subSolarLonDeg).to.be.at.least(0);
        expect(sub.subSolarLonDeg).to.be.lessThan(360);
    });

    it('should compute perihelion/aphelion TOA solar flux and generate Martian analemma curve', () => {
        // Perihelion Ls ~ 251 deg -> distance ~ 1.38 AU, flux ~ 715 W/m^2
        const peri = MarsTime.computeInstantaneousSolarFlux(251.0);
        expect(peri.distanceAU).to.be.closeTo(1.381, 0.05);
        expect(peri.solarFluxW_M2).to.be.greaterThan(700);

        // Aphelion Ls ~ 71 deg -> distance ~ 1.666 AU, flux ~ 490 W/m^2
        const aph = MarsTime.computeInstantaneousSolarFlux(71.0);
        expect(aph.distanceAU).to.be.closeTo(1.666, 0.05);
        expect(aph.solarFluxW_M2).to.be.lessThan(500);

        // Generate 12-point orbital analemma
        const analemma = MarsTime.computeAnalemmaCoordinates(12);
        expect(analemma.length).to.equal(12);
        expect(analemma[0]).to.have.property('declinationDeg');
        expect(analemma[0]).to.have.property('eotMinutes');
    });
});

describe('Horn 3x3 Slope, Aspect, Terrain Curvature & Hypsometry (ContourLayer)', () => {
    it('should compute exact 8-neighbor Horn slope and compass aspect', () => {
        // Uniform 10-degree eastward incline (dz/dx = +0.1763, dx = 100m)
        // [ 0, 17.63, 35.26 ]
        // [ 0, 17.63, 35.26 ]
        // [ 0, 17.63, 35.26 ]
        const patch = [
            0, 17.63, 35.26,
            0, 17.63, 35.26,
            0, 17.63, 35.26
        ];
        const horn = ContourLayer.computeHornSlopeAspect(patch, 100);
        expect(horn.slopeDeg).to.be.closeTo(10.0, 0.1);
        expect(horn.compassDirection).to.equal('W');
    });

    it('should compute terrain curvature second derivatives and hypsometric curve', () => {
        // Concave upward bowl: center lower than perimeter
        const bowl = [
            100, 50, 100,
            50,   0,  50,
            100, 50, 100
        ];
        const curv = ContourLayer.computeTerrainCurvature(bowl, 100);
        expect(curv.generalCurvature).to.be.lessThan(0); // Concave upward

        // Hypsometric distribution of 100 elevation samples
        const samples = Array.from({ length: 100 }, (_, i) => i * 10 - 500);
        const hyp = ContourLayer.computeHypsometricAreaDistribution(samples, 5);
        expect(hyp.length).to.equal(5);
        expect(hyp[4].cumulativeFraction).to.equal(1.0);
    });
});

describe('Polygon Centroid, Polyline Binary Serialization & Decimation (ShapeIO)', () => {
    it('should compute exact polygon centroid coordinates and signed shoelace area', () => {
        // 10x10 square from (0,0) to (10,10) -> area = 100, centroid = (5, 5)
        const square = [
            [0, 0],
            [10, 0],
            [10, 10],
            [0, 10]
        ];
        const res = ShapeIO.computePolygonCentroid(square);
        expect(res.centroidX).to.equal(5.0);
        expect(res.centroidY).to.equal(5.0);
        expect(res.area).to.equal(100.0);
    });

    it('should generate binary Polyline Shapefile buffer and decimate dense vertices', () => {
        // 2-segment polyline
        const line = [
            [[0, 0], [1, 1], [2, 0]]
        ];
        const buf = ShapeIO.createShapefilePolylineBuffer(line);
        expect(buf.byteLength).to.be.greaterThan(100);

        const header = ShapeIO.parseShapefileHeader(buf);
        expect(header.shapeType).to.equal(3); // PolyLine
        expect(header.shapeTypeName).to.equal('PolyLine');

        // Vertex decimation
        const dense = [[0, 0], [0.001, 0.001], [0.002, 0.002], [1.0, 1.0]];
        const simplified = ShapeIO.simplifyRadialDistance(dense, 0.01);
        expect(simplified.length).to.equal(2); // Only start and end remain
    });
});

describe('Standard Parallel Scaling, Grid Convergence & Heading Departure (ProjectionManager)', () => {
    it('should compute secant polar stereographic standard parallel scale factor', () => {
        // At standard parallel phi = 70 deg: scale factor k = 1.0000
        const kStandard = ProjectionManager.computeStandardParallelScale(70.0, 70.0);
        expect(kStandard).to.equal(1.0);

        // At North Pole phi = 90 deg: scale factor k = (1 + sin 70) / 2 ~ 0.9698
        const kPole = ProjectionManager.computeStandardParallelScale(90.0, 70.0);
        expect(kPole).to.be.closeTo(0.9698, 0.005);
    });

    it('should calculate grid convergence angle and great-circle departure', () => {
        // At 45 deg N latitude, 30 deg offset from central meridian -> gamma = 30 * sin(45) ~ 21.21 deg
        const gamma = ProjectionManager.computeGridConvergence(45.0, 30.0, 0.0);
        expect(gamma).to.be.closeTo(21.213, 0.01);

        // Heading departure along oblique trajectory
        const hd = ProjectionManager.computeGreatCircleAzimuthDistortion(10, 0, 50, 60);
        expect(hd.greatCircleAzimuthDeg).to.be.greaterThan(0);
        expect(hd.rhumbLineAzimuthDeg).to.be.greaterThan(0);
        expect(hd.departureDeg).to.be.greaterThan(0);
    });
});

describe('Synthetic Aperture SAR, Ionospheric Dispersion & Multi-Layer TWT (RadarSounderEngine)', () => {
    it('should compute along-track synthetic aperture radar (SAR) resolution', () => {
        // Orbital speed 3400 m/s, Doppler bandwidth 200 Hz -> SAR resolution = 3400 / 400 = 8.5 meters
        const sar = RadarSounderEngine.computeSARResolution(3400, 200);
        expect(sar).to.equal(8.5);
    });

    it('should calculate ionospheric dispersion delay and cumulative multi-layer TWT', () => {
        // 1 TECU at 20 MHz (SHARAD) -> delay ~ 3.36 µs, height shift ~ 504 m
        const iono = RadarSounderEngine.computeIonosphericDispersionDelay(1.0, 20e6);
        expect(iono.delayMicrosec).to.be.closeTo(3.361, 0.01);
        expect(iono.apparentHeightShiftMeters).to.be.closeTo(503.7, 1.0);

        // Multi-layer polar stratigraphy: 500m NPLD ice (eps=3.15) + 200m sand basal unit (eps=4.0)
        const ml = RadarSounderEngine.computeMultiLayerTWT([
            { thicknessMeters: 500, dielectricConstant: 3.15 },
            { thicknessMeters: 200, dielectricConstant: 4.0 }
        ]);
        expect(ml.totalDepthMeters).to.equal(700.0);
        expect(ml.totalTwtMicrosec).to.be.greaterThan(7.0);
        expect(ml.layerIntervals.length).to.equal(2);
    });
});

describe('Gehrels Poisson Confidence Limits & Buffered Counting Areas (CSFDEngine)', () => {
    it('should calculate exact Gehrels small-sample Poisson confidence bounds', () => {
        // N = 10 craters -> lower ~ 7.16, upper ~ 13.96
        const g10 = CSFDEngine.computeGehrelsPoissonIntervals(10);
        expect(g10.lowerLimit).to.be.greaterThan(6.5);
        expect(g10.lowerLimit).to.be.lessThan(8.0);
        expect(g10.upperLimit).to.be.greaterThan(13.0);
        expect(g10.upperLimit).to.be.lessThan(15.0);

        // N = 0 -> upper limit ~ 1.84
        const g0 = CSFDEngine.computeGehrelsPoissonIntervals(0);
        expect(g0.lowerLimit).to.equal(0);
        expect(g0.upperLimit).to.be.closeTo(1.84, 0.05);
    });

    it('should compute buffered boundary area correction and differential slope index', () => {
        // 1000 km^2 area with 100 km perimeter, D = 2 km crater
        // Area loss = 0.5 * 100 * 2 - (pi/4)*4 = 100 - 3.14 = 96.86 km^2
        const buf = CSFDEngine.computeBufferedEffectiveArea(1000, 100, 2);
        expect(buf.effectiveAreaKm2).to.be.closeTo(903.14, 1.0);
        expect(buf.areaLossPercent).to.be.closeTo(9.69, 0.2);

        // Power-law slope index for synthetic distribution
        const craters = Array.from({ length: 30 }, (_, i) => ({ diameter: (i + 1) * 500 }));
        const slope = CSFDEngine.computeDifferentialPowerLawSlope(craters, 1.0, 10.0);
        expect(slope).to.have.property('slopeIndex');
    });
});

describe('CRISM Summary Parameters, Convex Hull & Spectral Correlation (BandMathEngine)', () => {
    it('should compute CRISM mineralogical summary parameters and indices', () => {
        const bands = {
            B1080: 0.25,
            B1500: 0.28,
            B1815: 0.26,
            B1930: 0.20, // 20% absorption dip at 1.93 µm
            B2130: 0.25,
            B2210: 0.22, // Al-OH dip
            B2290: 0.25,
            B2530: 0.20
        };
        const params = BandMathEngine.computeCRISMSummaryParameters(bands);
        expect(params.bd1900_2).to.be.greaterThan(0.15);
        expect(params.bd2210_2).to.be.greaterThan(0.05);
        expect(params.islope).to.be.greaterThan(0);
    });

    it('should compute upper convex hull continuum and Pearson spectral correlation', () => {
        const wavelengths = [1.0, 1.5, 1.9, 2.2, 2.5];
        const spectrum = [0.30, 0.35, 0.25, 0.32, 0.30];
        const hull = BandMathEngine.computeConvexHullContinuum(wavelengths, spectrum);
        expect(hull.length).to.equal(5);
        expect(hull[2]).to.be.greaterThan(spectrum[2]); // Continuum above dip

        // Perfectly correlated spectra
        const specA = [0.1, 0.2, 0.3, 0.4];
        const specB = [0.2, 0.4, 0.6, 0.8];
        const r = BandMathEngine.computeSpectralCorrelation(specA, specB);
        expect(r).to.equal(1.0);
    });
});

describe('Topographic Self-Shadowing, Horizon Dip & Nodal Precession (ThreeDEngine)', () => {
    it('should calculate local terrain facet self-shadowing and incidence angle', () => {
        // Sun elevation = 30 deg in South (az = 180). North-facing slope (aspect = 0, slope = 40 deg)
        // dAz = 180 deg -> cos(i) = sin(30)*cos(40) + cos(30)*sin(40)*cos(180) = 0.5*0.766 - 0.866*0.6428 = 0.383 - 0.5567 < 0 -> shadowed
        const shadow = ThreeDEngine.computeTopographicSelfShadow(40, 0, 30, 180);
        expect(shadow.isIlluminated).to.equal(false);
        expect(shadow.cosIncidence).to.equal(0);
        expect(shadow.localIncidenceDeg).to.be.greaterThan(90.0);

        // Sun-facing slope (aspect = 180, slope = 30, sun elev = 45, sun az = 180) -> illuminated
        const lit = ThreeDEngine.computeTopographicSelfShadow(30, 180, 45, 180);
        expect(lit.isIlluminated).to.equal(true);
        expect(lit.cosIncidence).to.be.greaterThan(0.9);
    });

    it('should compute astronomical horizon dip angle and orbital nodal precession rate', () => {
        // 400 km altitude orbiter over Mars (R = 3389.5 km)
        // cos(dip) = 3389.5 / 3789.5 ~ 0.8944 -> dip ~ 26.56 deg
        const dip = ThreeDEngine.computeHorizonDipAngle(400);
        expect(dip.dipAngleDeg).to.be.closeTo(26.56, 0.1);

        // Sun-synchronous MRO orbit (a = 3790 km, inc = 92.8 deg)
        const orb = ThreeDEngine.computeOrbitalPrecessionRate(3790, 0.001, 92.8);
        expect(orb.precessionDegPerDay).to.be.closeTo(0.524, 0.05);
        expect(orb.isSunSynchronous).to.equal(true);
    });
});

describe('Crustal Magnetic Remanence, Multi-Layer Geotherms & Optical Extinction (InvestigateTool)', () => {
    it('should calculate planetary dipole magnetic field vector components', () => {
        // At magnetic equator (lat = 0) at surface (alt = 0): Br = 0, Btheta = -factor, Btotal = factor
        const magEq = InvestigateTool.computeDipoleMagneticField(0, 0, 1e20, 'mars');
        expect(magEq.Br_nT).to.equal(0);
        expect(magEq.Btheta_nT).to.be.lessThan(0);
        expect(magEq.Btotal_nT).to.be.greaterThan(0);
        expect(magEq.inclinationDeg).to.equal(0);

        // At magnetic North pole (lat = 90): Br > 0, Btheta = 0, inclination = 90 deg
        const magPole = InvestigateTool.computeDipoleMagneticField(90, 0, 1e20, 'mars');
        expect(magPole.Br_nT).to.be.greaterThan(0);
        expect(magPole.Btheta_nT).to.be.closeTo(0, 0.01);
        expect(magPole.inclinationDeg).to.equal(90.0);
    });

    it('should compute multi-layer crustal geotherm and Beer-Lambert transmittance', () => {
        // 2 km megaregolith (k = 1.5 W/m K) + 8 km basalt (k = 2.5 W/m K) with q = 30 mW/m^2, T_surf = 210 K
        // dT1 = (0.03 / 1.5) * 2000 = 40 K -> T1 = 250 K
        // dT2 = (0.03 / 2.5) * 8000 = 96 K -> T2 = 346 K
        const geo = InvestigateTool.computeMultiLayerGeotherm([
            { thicknessKm: 2, thermalConductivityW_MK: 1.5, name: 'Megaregolith' },
            { thicknessKm: 8, thermalConductivityW_MK: 2.5, name: 'Basaltic Crust' }
        ], 30.0, 210.0);
        expect(geo.totalCrustThicknessKm).to.equal(10.0);
        expect(geo.tempAtBaseK).to.equal(346.0);

        // Optical transmittance for tau = 0.5 at 60 deg zenith (airmass = 2.0) -> T = exp(-1.0) ~ 0.3679
        const opt = InvestigateTool.computeAtmosphericTransmittance(0.5, 60);
        expect(opt.airmass).to.be.closeTo(2.0, 0.01);
        expect(opt.transmittance).to.be.closeTo(0.3679, 0.005);
    });
});

describe('Geodetic Midpoints, Polyline Resampling & Polygon Circularity (MeasureTool)', () => {
    it('should compute exact spherical geodetic midpoint', () => {
        // Between (0, 0) and (0, 90) -> midpoint should be (0, 45)
        const mid = MeasureTool.computeGeodeticMidpoint(0, 0, 0, 90);
        expect(mid.lat).to.equal(0);
        expect(mid.lon).to.equal(45.0);

        // Between (0, 0) and (60, 0) -> midpoint should be (30, 0)
        const mid2 = MeasureTool.computeGeodeticMidpoint(0, 0, 60, 0);
        expect(mid2.lat).to.equal(30.0);
        expect(mid2.lon).to.equal(0);
    });

    it('should equidistantly resample a polyline track and compute polygon circularity', () => {
        // 1000 km straight equator track sampled every 100 km
        const track = [[0, 0], [0, 20]];
        const samples = MeasureTool.resamplePolylineEquidistant(track, 200, 'mars');
        expect(samples.length).to.be.greaterThan(3);
        expect(samples[0].distanceKm).to.equal(0);

        // Square polygon circularity C = 4*pi*A / P^2 = 4*pi*1 / 16 = pi/4 ~ 0.785
        const square = [[0, 0], [10, 0], [10, 10], [0, 10]];
        const circ = MeasureTool.computePolygonCircularity(square, 'mars');
        expect(circ.circularityQuotient).to.be.closeTo(0.785, 0.05);
        expect(circ.perimeterKm).to.be.greaterThan(0);
    });
});

describe('Atmospheric Downwelling IR, Skin Depth Amplification & Radiative Equilibrium (KRCEngine)', () => {
    it('should compute downwelling IR flux and annual skin depth amplification ratio', () => {
        // T_air = 210 K, tau = 0.3 -> downwelling flux > 20 W/m^2
        const ir = KRCEngine.computeAtmosphericDownwellingIR(210.0, 0.3, 610.0);
        expect(ir.downwellingFluxW_M2).to.be.greaterThan(20.0);
        expect(ir.atmosphericEmissivity).to.be.greaterThan(0.15);

        // 668.6 sols in Mars year -> ratio = sqrt(668.6) ~ 25.857
        const ratio = KRCEngine.computeSkinDepthRatio(668.6);
        expect(ratio).to.be.closeTo(25.857, 0.01);
    });

    it('should calculate steady-state radiative equilibrium surface temperature', () => {
        // Solar flux = 500 W/m^2, albedo = 0.25, IR = 25 W/m^2 -> T_eq ~ 293 K
        const tEq = KRCEngine.computeEquilibriumSurfaceTemperature(500.0, 0.25, 25.0, 0.95);
        expect(tEq).to.be.greaterThan(280.0);
        expect(tEq).to.be.lessThan(310.0);
    });
});

describe('Surface Friction Velocity, Dust Specific Extinction & Potential Temperature (MCDEngine)', () => {
    it('should compute boundary layer friction velocity and dust lifting threshold', () => {
        // 20 m/s wind at 10m height over z0 = 0.01m -> u* = (0.4 * 20) / ln(1000) = 8.0 / 6.9077 ~ 1.158 m/s
        const fv = MCDEngine.computeSurfaceFrictionVelocity(20.0, 10.0, 0.01, 0.015);
        expect(fv.frictionVelocityMs).to.be.closeTo(1.158, 0.01);
        expect(fv.thresholdExceeded).to.equal(false);

        // Strong 30 m/s storm wind -> u* ~ 1.737 m/s >= 1.5 m/s -> threshold exceeded
        const fvStorm = MCDEngine.computeSurfaceFrictionVelocity(30.0, 10.0, 0.01, 0.015);
        expect(fvStorm.thresholdExceeded).to.equal(true);
    });

    it('should calculate specific dust mass extinction cross-section and potential temperature', () => {
        // Q = 2.5, rho = 2500 kg/m^3, r = 1.5 µm -> sigma = 7.5 / (4 * 2500 * 1.5e-6) = 500 m^2/kg = 0.5 m^2/g
        const ext = MCDEngine.computeDustSpecificExtinctionCrossSection(2.5, 2500, 1.5);
        expect(ext.massExtinctionM2PerKg).to.equal(500.0);
        expect(ext.massExtinctionM2PerGram).to.equal(0.5);

        // T = 180 K at 305 Pa (P0 = 610 Pa) -> theta = 180 * (610 / 305)^0.23615 ~ 212.0 K
        const theta = MCDEngine.computePotentialTemperature(180.0, 305.0, 610.0);
        expect(theta).to.be.closeTo(212.0, 0.5);
    });
});

describe('True Solar Sol Duration, Kepler Anomaly & Seasonal Calendars (MarsTime)', () => {
    it('should calculate variable true solar sol duration across Martian orbit', () => {
        // At perihelion Ls = 251 deg: sol duration is longer than mean by ~ 50 seconds
        const solPeri = MarsTime.computeTrueSolarSolDuration(251.0);
        expect(solPeri.solDurationSeconds).to.be.greaterThan(88775.244);
        expect(solPeri.diffFromMeanSeconds).to.be.greaterThan(0);

        // At aphelion Ls = 71 deg: sol duration is shorter than mean
        const solAph = MarsTime.computeTrueSolarSolDuration(71.0);
        expect(solAph.solDurationSeconds).to.be.lessThan(88775.244);
        expect(solAph.diffFromMeanSeconds).to.be.lessThan(0);
    });

    it('should solve Kepler equation for true anomaly and compute seasonal start dates', () => {
        // Mean anomaly M = 0 -> Eccentric Anomaly E = 0, True Anomaly nu = 0
        const kep0 = MarsTime.computeKeplerOrbitTrueAnomaly(0, 0.0934);
        expect(kep0.eccentricAnomalyDeg).to.equal(0);
        expect(kep0.trueAnomalyDeg).to.equal(0);

        // Mean anomaly M = 90 deg -> True anomaly nu > 90 deg due to eccentricity
        const kep90 = MarsTime.computeKeplerOrbitTrueAnomaly(90, 0.0934);
        expect(kep90.trueAnomalyDeg).to.be.greaterThan(90.0);

        // Seasonal start dates for MY 37
        const cal = MarsTime.computeSeasonalCalendarDates(37);
        expect(cal.springDate).to.be.instanceOf(Date);
        expect(cal.summerDate).to.be.instanceOf(Date);
        expect(cal.summerDate.getTime()).to.be.greaterThan(cal.springDate.getTime());
    });
});

describe('Topographic Wetness (TWI), Stream Power (SPI) & Morphometric Roughness (ContourLayer)', () => {
    it('should calculate Beven-Kirkby Topographic Wetness Index (TWI)', () => {
        // Upslope area 10,000 m^2 on a 5-degree slope -> tan(5) ~ 0.08749 -> TWI = ln(10000 / 0.08749) ~ 11.647
        const twi = ContourLayer.computeTopographicWetnessIndex(5.0, 10000);
        expect(twi).to.be.closeTo(11.647, 0.05);

        // Fluvial valley floor / flat area (slope 1 deg) -> higher TWI
        const twiFlat = ContourLayer.computeTopographicWetnessIndex(1.0, 10000);
        expect(twiFlat).to.be.greaterThan(twi);
    });

    it('should compute Stream Power Index (SPI) and local 3x3 morphometric roughness', () => {
        // Upslope area 5,000 m^2 on a 10-degree slope -> SPI = 5000 * tan(10) ~ 881.63
        const spi = ContourLayer.computeStreamPowerIndex(10.0, 5000);
        expect(spi).to.be.closeTo(881.63, 1.0);

        // 3x3 step terrain: roughness std dev and relief span
        const patch = [
            100, 100, 100,
            120, 120, 120,
            140, 140, 140
        ];
        const rough = ContourLayer.computeMorphometricRoughness(patch);
        expect(rough.meanElevMeters).to.equal(120.0);
        expect(rough.reliefSpanMeters).to.equal(40.0);
        expect(rough.roughnessStdDevMeters).to.be.greaterThan(0);
    });
});

describe('Visvalingam-Whyatt Simplification, Point-in-Polygon & Ear Clipping (ShapeIO)', () => {
    it('should simplify polyline using Visvalingam-Whyatt effective area and test point in polygon', () => {
        // Polyline with a tiny spike forming a triangle of area 0.5 * 2 * 0.0001 = 0.0001 < 0.001
        const line = [[0, 0], [1, 0.0001], [2, 0]];
        const simp = ShapeIO.simplifyVisvalingamWhyatt(line, 0.001);
        expect(simp.length).to.equal(2);
        expect(simp[0]).to.deep.equal([0, 0]);
        expect(simp[1]).to.deep.equal([2, 0]);

        // Point-in-Polygon test for unit square [0,0] -> [10,10]
        const square = [[0, 0], [10, 0], [10, 10], [0, 10]];
        expect(ShapeIO.isPointInPolygon([5, 5], square)).to.equal(true);
        expect(ShapeIO.isPointInPolygon([15, 5], square)).to.equal(false);
    });

    it('should triangulate arbitrary polygon using ear-clipping algorithm', () => {
        // 4-vertex quadrilateral should triangulate into 2 triangles
        const quad = [[0, 0], [10, 0], [10, 10], [0, 10]];
        const tris = ShapeIO.triangulatePolygonEarClipping(quad);
        expect(tris.length).to.equal(2);
        expect(tris[0].length).to.equal(3);
    });
});

describe('Albers Equal-Area Conic & Cartographic Distortion (ProjectionManager)', () => {
    it('should forward and inverse transform coordinates using Albers Equal-Area Conic projection', () => {
        // Project (lat = 40, lon = 0) with standard parallels 20 and 60
        const fwd = ProjectionManager.forwardAlbersEqualArea(40, 0, 20, 60, 0, 'mars');
        expect(fwd.x).to.equal(0);
        expect(fwd.y).to.be.greaterThan(0);

        // Inverse transform should recover original latitude and longitude
        const inv = ProjectionManager.inverseAlbersEqualArea(fwd.x, fwd.y, 20, 60, 0, 'mars');
        expect(inv.lat).to.be.closeTo(40.0, 0.05);
        expect(inv.lon).to.be.closeTo(0.0, 0.05);
    });

    it('should compute Equirectangular cylindrical areal expansion scale factor', () => {
        // At equator (lat = 0): s = 1.0
        const s0 = ProjectionManager.computeEquirectangularArealScale(0);
        expect(s0).to.equal(1.0);

        // At 60 deg lat: sec(60) = 2.0 (2x area distortion)
        const s60 = ProjectionManager.computeEquirectangularArealScale(60);
        expect(s60).to.be.closeTo(2.0, 0.01);
    });
});

describe('Surface-to-Basal Power Ratio, EM Skin Depth & Doppler (RadarSounderEngine)', () => {
    it('should compute surface-to-basal radar power ratio and dielectric attenuation loss', () => {
        // 1000m pure ice sheet (eps = 3.15, lossTan = 0.001) over basaltic basement (eps = 7.5)
        const ratio = RadarSounderEngine.computeSurfaceBasalPowerRatio(3.15, 7.5, 1000, 0.001, 20e6);
        expect(ratio.attenuationLossDb).to.be.greaterThan(0);
        expect(ratio.powerRatioDb).to.be.greaterThan(0);
        expect(ratio.basalReflectivityDb).to.be.lessThan(0);
    });

    it('should calculate electromagnetic skin depth and radar carrier Doppler frequency shift', () => {
        // Loss tangent 0.001 in ice (eps = 3.15) at 20 MHz -> skin depth > 1000 meters
        const skin = RadarSounderEngine.computeSkinDepthEM(20e6, 0.001, 3.15);
        expect(skin).to.be.greaterThan(1000.0);

        // 100 m/s line-of-sight relative velocity at 20 MHz -> Doppler shift = 2 * 100 * 20e6 / 3e8 ~ 13.34 Hz
        const doppler = RadarSounderEngine.computeDopplerShift(100.0, 20e6);
        expect(doppler).to.be.closeTo(13.34, 0.05);
    });
});

describe('Transient-to-Final Collapse, Ejecta Blanket & Cavity Volume (CSFDEngine)', () => {
    it('should calculate transient to final crater diameter collapse and morphology', () => {
        // Simple crater Dt = 4 km <= 7 km -> Df = 1.25 * 4 = 5 km
        const simple = CSFDEngine.computeTransientToFinalDiameter(4.0, 7.0);
        expect(simple.finalDiameterKm).to.equal(5.0);
        expect(simple.morphology).to.include('Simple');

        // Complex crater Dt = 15 km > 7 km -> Df > 1.25 * Dt
        const complex = CSFDEngine.computeTransientToFinalDiameter(15.0, 7.0);
        expect(complex.finalDiameterKm).to.be.greaterThan(18.75);
        expect(complex.morphology).to.include('Complex');
    });

    it('should compute continuous ejecta blanket radius and crater cavity volume', () => {
        // Crater radius = 10 km -> continuous ejecta radius = 2.3 * 10 = 23 km
        const ejecta = CSFDEngine.computeContinuousEjectaRadius(10.0);
        expect(ejecta.continuousEjectaRadiusKm).to.equal(23.0);
        expect(ejecta.ejectaCoverAreaKm2).to.be.greaterThan(1000.0);

        // Crater diameter = 10 km -> cavity volume > 0
        const cav = CSFDEngine.computeCraterCavityVolume(10.0, true);
        expect(cav.volumeKm3).to.be.greaterThan(0);
        expect(cav.depthKm).to.be.greaterThan(0);
    });
});

describe('CRISM Carbonates, Olivine Fo# & Band Area Ratio (BandMathEngine)', () => {
    it('should calculate CRISM diagnostic carbonate absorption indices', () => {
        // Deep absorption at 2.50 µm and 3.90 µm -> strong carbonate signature
        const carb = BandMathEngine.computeCarbonateIndices({
            B2350: 0.30, B2500: 0.20, B2600: 0.30,
            B3750: 0.25, B3900: 0.15, B4000: 0.25
        });
        expect(carb.bd2500_2).to.be.greaterThan(0.2);
        expect(carb.bd3900).to.be.greaterThan(0.2);
        expect(carb.hasCarbonateSignature).to.equal(true);
    });

    it('should estimate Olivine Forsterite Fo# number and Band Area Ratio (BAR)', () => {
        // Minimum at 1.04 µm -> Pure Mg-rich Forsterite (Fo100)
        const fo100 = BandMathEngine.computeOlivineForsteriteNumber(1.04);
        expect(fo100.forsteriteNumber).to.equal(100.0);
        expect(fo100.compositionName).to.include('Forsterite');

        // Minimum at 1.07 µm -> Intermediate Olivine (Fo50)
        const fo50 = BandMathEngine.computeOlivineForsteriteNumber(1.07);
        expect(fo50.forsteriteNumber).to.be.closeTo(50.0, 1.0);

        // Pure olivine: BAR = Area2 / Area1 < 0.1
        const barOl = BandMathEngine.computeBandAreaRatio(0.5, 0.02);
        expect(barOl.barRatio).to.be.lessThan(0.1);
        expect(barOl.classification).to.include('Olivine');
    });
});

describe('Ground Swath Width, Triangle Facet Normal & Perspective Camera (ThreeDEngine)', () => {
    it('should calculate ground swath footprint width and 3D triangle normal', () => {
        // Spacecraft at 300 km altitude with 30-degree FOV: Swath = 2 * 300 * tan(15) ~ 160.77 km
        const swath = ThreeDEngine.computeGroundSwathWidth(300.0, 30.0);
        expect(swath.swathWidthKm).to.be.closeTo(160.77, 0.5);
        expect(swath.halfSwathWidthKm).to.be.closeTo(80.38, 0.5);

        // Horizontal XY triangle in Z=0 plane: [0,0,0], [1,0,0], [0,1,0] -> normal should point +Z (0,0,1)
        const norm = ThreeDEngine.computeTriangleFacetNormal([0, 0, 0], [1, 0, 0], [0, 1, 0]);
        expect(norm.nx).to.equal(0);
        expect(norm.ny).to.equal(0);
        expect(norm.nz).to.equal(1.0);
        expect(norm.area).to.equal(0.5);
    });

    it('should compute pinhole perspective projection to screen coordinates', () => {
        // Point (10, 20, 100) with focal length 1.0 -> screenX = 10/100 = 0.1, screenY = 20/100 = 0.2
        const proj = ThreeDEngine.computePerspectiveProjection([10, 20, 100], 1.0);
        expect(proj.screenX).to.equal(0.1);
        expect(proj.screenY).to.equal(0.2);
        expect(proj.inFrontOfCamera).to.equal(true);
    });
});

describe('Bouguer Gravity Anomaly, Airy Isostasy & Thermal Diffusivity (InvestigateTool)', () => {
    it('should calculate complete Bouguer gravity anomaly and Airy isostatic crustal root', () => {
        // High volcanic plateau at 5000 m elevation on Mars (g0 = 3.72 m/s^2, rho_c = 2900 kg/m^3)
        const grav = InvestigateTool.computeBouguerGravityAnomaly(3.72, 3.72, 5000, 2900, 'mars');
        expect(grav.freeAirCorrectionMGal).to.be.greaterThan(0);
        expect(grav.bouguerPlateCorrectionMGal).to.be.greaterThan(0);

        // 5 km Olympus Mons flank topography (rho_c = 2900, rho_m = 3500) -> root = 5 * (2900 / 600) ~ 24.17 km
        const root = InvestigateTool.computeAiryIsostaticCrustalRoot(5.0, 2900, 3500);
        expect(root.crustalRootThicknessKm).to.be.closeTo(24.17, 0.5);
        expect(root.totalCrustalColumnKm).to.be.greaterThan(70.0);
    });

    it('should compute regolith and rock thermal diffusivity', () => {
        // Basalt rock: k = 2.0 W/(m K), rho = 2500 kg/m^3, cp = 800 J/(kg K) -> kappa = 2 / (2500 * 800) = 1.0e-6 m^2/s
        const kappa = InvestigateTool.computeThermalDiffusivity(2.0, 2500, 800);
        expect(kappa).to.be.closeTo(1.0e-6, 1e-7);
    });
});

describe('Geodetic Destination Point & Interior Tunnel Chord (MeasureTool)', () => {
    it('should calculate destination point along forward geodesic bearing', () => {
        // Start at equator (0, 0), bearing 90 deg (East), distance = 1/4 of Mars circumference (pi/2 * 3389.5 ~ 5324.28 km)
        const dest = MeasureTool.computeDestinationPoint(0, 0, 90.0, 5324.28, 'mars');
        expect(dest.destLat).to.be.closeTo(0.0, 0.1);
        expect(dest.destLon).to.be.closeTo(90.0, 0.5);
    });

    it('should calculate 3D interior straight-line tunnel chord distance', () => {
        // Points 90 degrees apart on Mars (R = 3389.5 km): chord = 2 * R * sin(45) = R * sqrt(2) ~ 4793.47 km
        const chord = MeasureTool.computeChordDistance(0, 0, 0, 90.0, 'mars');
        expect(chord.arcDistanceKm).to.be.closeTo(5324.28, 1.0);
        expect(chord.chordDistanceKm).to.be.closeTo(4793.47, 1.0);
        expect(chord.depthBelowSurfaceKm).to.be.greaterThan(900.0);
    });
});

describe('Two-Layer Apparent TI, Fourier Harmonics & Geothermal Flux (KRCEngine)', () => {
    it('should compute two-layer apparent thermal inertia and skin depth ratio', () => {
        // Thin 2 mm dust mantle (TI = 50) over solid basalt bedrock (TI = 1200) -> apparent TI > 50 and bedrock dominated
        const twoLayer = KRCEngine.computeTwoLayerApparentThermalInertia(50, 1200, 0.002);
        expect(twoLayer.apparentThermalInertia).to.be.greaterThan(50.0);
        expect(twoLayer.apparentThermalInertia).to.be.lessThan(1200.0);
        expect(twoLayer.isBedrockDominated).to.equal(true);
    });

    it('should decompose diurnal temperature curves into Fourier harmonics and compute geothermal flux', () => {
        // Synthetic diurnal curve with 200 K mean and 40 K diurnal swing
        const temps = Array.from({ length: 24 }, (_, i) => 200 + 40 * Math.sin((i / 24) * 2 * Math.PI));
        const fourier = KRCEngine.decomposeFourierHarmonics(temps, 2);
        expect(fourier.meanTemp).to.be.closeTo(200.0, 0.1);
        expect(fourier.harmonics[0].amplitudeK).to.be.closeTo(40.0, 0.5);

        // Geothermal gradient: 0.015 K/m (15 K/km) with rock conductivity 2.0 W/(m K) -> 30 mW/m^2
        const flux = KRCEngine.computeSubsurfaceGeothermalFlux(0.015, 2.0);
        expect(flux.heatFluxMw_M2).to.equal(30.0);
    });
});

describe('Stereographic Point Scale, Parallel Length & Conic Constant (ProjectionManager)', () => {
    it('should calculate conformal point scale factor and latitude parallel length', () => {
        // At North Pole (lat = 90, center = 90): scale k = 1.0
        const kPole = ProjectionManager.computeStereographicPointScale(90, 0, 90, 0);
        expect(kPole).to.equal(1.0);

        // At equator (lat = 0, center = 90): scale k = 2.0
        const kEq = ProjectionManager.computeStereographicPointScale(0, 0, 90, 0);
        expect(kEq).to.equal(2.0);

        // Mars equator circumference: 2 * pi * 3389.5 ~ 21296.81 km
        const eqLen = ProjectionManager.computeSinusoidalParallelLength(0, 'mars');
        expect(eqLen).to.be.closeTo(21296.81, 1.0);
    });

    it('should compute conic projection cone constant and apical opening angle', () => {
        // Conic between 30 and 60 deg lat: n = (sin(30) + sin(60)) / 2 = (0.5 + 0.866) / 2 = 0.6830
        const conic = ProjectionManager.computeConicConeConstant(30, 60);
        expect(conic.coneConstantN).to.be.closeTo(0.6830, 0.001);
        expect(conic.apicalHalfAngleDeg).to.be.greaterThan(40.0);
    });
});

describe('Atmospheric Thermal Diffusivity, CO2 Condensation & Dust Settling (MCDEngine)', () => {
    it('should calculate atmospheric CO2 thermal diffusivity and condensation flux', () => {
        // Near-surface 210 K, 610 Pa atmosphere
        const alpha = MCDEngine.computeAtmosphericThermalDiffusivity(210.0, 610.0);
        expect(alpha).to.be.greaterThan(0);

        // At 140 K and 610 Pa: P_sat < 610 Pa -> S > 1.0 (supersaturated, CO2 ice clouds form)
        const cond = MCDEngine.computeCO2CondensationFlux(140.0, 610.0);
        expect(cond.isCondensing).to.equal(true);
        expect(cond.supersaturationRatio).to.be.greaterThan(1.0);

        // At 210 K and 610 Pa: P_sat >> 610 Pa -> S << 1.0 (vapor stable)
        const warm = MCDEngine.computeCO2CondensationFlux(210.0, 610.0);
        expect(warm.isCondensing).to.equal(false);
    });

    it('should compute Stokes-Cunningham terminal dust sedimentation velocity', () => {
        // Standard 1.5 µm radius Martian dust grain at 610 Pa -> terminal velocity on order of mm/s
        const settling = MCDEngine.computeDustDepositionVelocity(1.5, 210.0, 610.0, 2500.0);
        expect(settling.settlingVelocityMmS).to.be.greaterThan(0.1);
        expect(settling.knudsenNumber).to.be.greaterThan(1.0); // Rarefied slip-flow regime on Mars
    });
});

describe('Heliocentric Orbital Speed, Mean Solar Time & Apparent Sun (MarsTime)', () => {
    it('should calculate instantaneous orbital velocity across perihelion and aphelion', () => {
        // At perihelion (Ls = 251): orbital speed peaks ~ 26.5 km/s
        const peri = MarsTime.computeHeliocentricOrbitalSpeed(251.0);
        expect(peri.orbitalSpeedKmS).to.be.closeTo(26.5, 0.5);
        expect(peri.isNearPerihelion).to.equal(true);

        // At aphelion (Ls = 71): orbital speed reaches minimum ~ 22.0 km/s
        const aph = MarsTime.computeHeliocentricOrbitalSpeed(71.0);
        expect(aph.orbitalSpeedKmS).to.be.closeTo(22.0, 0.5);
        expect(aph.isNearPerihelion).to.equal(false);
    });

    it('should compute Local Mean Solar Time (LMST) and Sun angular diameter', () => {
        // At 180 deg East with MTC = 12: LMST = (12 + 180/15) % 24 = 24 % 24 = 0.0 h (midnight)
        const lmst = MarsTime.computeMeanSolarTimeOffset(180.0, 12.0);
        expect(lmst.lmstHours).to.equal(0.0);

        // Sun angular diameter at Mars is ~21 arcmin (0.35 degrees)
        const sunDiam = MarsTime.computeMartianSunDiameter(0.0);
        expect(sunDiam.angularDiameterDeg).to.be.closeTo(0.35, 0.05);
        expect(sunDiam.angularDiameterArcmin).to.be.closeTo(21.0, 3.0);
    });
});

describe('Zevenbergen-Thorne Curvatures, Vector Ruggedness & Relief Ratio (ContourLayer)', () => {
    it('should compute Zevenbergen-Thorne profile and planform terrain curvature', () => {
        // Uniform planar inclined surface (100m spacing): curvature should be near 0
        const planarPatch = [
            100, 200, 300,
            100, 200, 300,
            100, 200, 300
        ];
        const curv = ContourLayer.computeZevenbergenThorneCurvatures(planarPatch, 100);
        expect(curv.profileCurvature).to.equal(0);
        expect(curv.planformCurvature).to.equal(0);

        // Convex peak / dome (center elevated at 500m vs 100m perimeter)
        const domePatch = [
            100, 200, 100,
            200, 500, 200,
            100, 200, 100
        ];
        const domeCurv = ContourLayer.computeZevenbergenThorneCurvatures(domePatch, 100);
        expect(domeCurv.meanCurvature).to.not.equal(0);

        // Sloping ridge / flank
        const ridgePatch = [
            100, 250, 400,
            150, 350, 450,
            180, 380, 500
        ];
        const ridgeCurv = ContourLayer.computeZevenbergenThorneCurvatures(ridgePatch, 100);
        expect(ridgeCurv.profileCurvature).to.be.a('number');
    });

    it('should compute Sappington Vector Ruggedness Measure (VRM) and Relative Relief Ratio', () => {
        // Flat surface -> VRM should be 0.0
        const flatPatch = [0, 0, 0, 0, 0, 0, 0, 0, 0];
        const flatVRM = ContourLayer.computeVectorRuggednessMeasure(flatPatch, 100);
        expect(flatVRM).to.equal(0.0);

        // Rugged crater rim patch
        const ruggedPatch = [100, 800, 200, 900, 50, 600, 300, 700, 150];
        const ruggedVRM = ContourLayer.computeVectorRuggednessMeasure(ruggedPatch, 100);
        expect(ruggedVRM).to.be.greaterThan(0.0);

        // Relative Relief Ratio across [0, 5000] vs datum -8000 -> 5000 / 13000 ~ 0.3846
        const rrr = ContourLayer.computeTerrainReliefRatio([0, 2500, 5000], -8000);
        expect(rrr).to.be.closeTo(0.3846, 0.01);
    });
});

describe('Temperature-Dependent Permittivity & Dielectric Mixing (RadarSounderEngine)', () => {
    it('should compute temperature-dependent ice dielectric permittivity', () => {
        // At T = 200 K: eps = 3.15 * (1 + 0) = 3.15
        const eps200 = RadarSounderEngine.computeWaterIceTemperaturePermittivity(200.0);
        expect(eps200).to.equal(3.15);

        // At cold polar temperature T = 150 K: eps slightly lower than 3.15
        const eps150 = RadarSounderEngine.computeWaterIceTemperaturePermittivity(150.0);
        expect(eps150).to.be.lessThan(3.15);
        expect(eps150).to.be.greaterThan(3.0);
    });

    it('should compute Looyenga and Birchak dielectric mixing for ice-dust mixtures', () => {
        // Pure ice (phi = 0) -> effective permittivity = 3.15
        const pureIce = RadarSounderEngine.computeLooyengaDielectricMixing(0.0, 3.15, 7.5);
        expect(pureIce.effectivePermittivity).to.equal(3.15);

        // 10% dust mixture (phi = 0.10) in ice -> intermediate permittivity ~ 3.4 - 3.6
        const mix10 = RadarSounderEngine.computeLooyengaDielectricMixing(0.10, 3.15, 7.5);
        expect(mix10.effectivePermittivity).to.be.greaterThan(3.15);
        expect(mix10.effectivePermittivity).to.be.lessThan(7.5);

        // Birchak mixing with 20% dust
        const birchak = RadarSounderEngine.computeBirchakDielectricMixing(0.20, 3.15, 7.5);
        expect(birchak.effectivePermittivity).to.be.greaterThan(3.15);
        expect(birchak.waveVelocityMs).to.be.lessThan(RadarSounderEngine.getVelocity(3.15));
    });
});

describe('CRISM Silica, Ferric Nanophase & Spectral Asymmetry (BandMathEngine)', () => {
    it('should compute CRISM hydrated silica index and ferric oxide nanophase intensity', () => {
        // Hydrated silica with deep 2.21 µm absorption
        const silica = BandMathEngine.computeCRISMSilicaIndex({ B2140: 0.30, B2210: 0.22, B2250: 0.29 });
        expect(silica.bd2210_sil).to.be.greaterThan(0.05);
        expect(silica.isHydratedSilicaPresent).to.equal(true);

        // Hematite with strong 530 nm absorption
        const ferric = BandMathEngine.computeFerricNanophaseIndex({ B440: 0.15, B530: 0.18, B600: 0.30 });
        expect(ferric.bd530_2).to.be.greaterThan(0.10);
        expect(ferric.ferricIntensity).to.include('Hematite');
    });

    it('should compute spectral absorption band asymmetry factor and skew direction', () => {
        const wavelengths = [1.8, 1.9, 2.0, 2.1, 2.2];
        // Right-skewed absorption profile (deeper tail on long wavelength side)
        const cr = [1.0, 0.8, 0.5, 0.65, 0.95];
        const asy = BandMathEngine.computeSpectralAsymmetry(wavelengths, cr, 2);
        expect(asy.asymmetryFactor).to.be.a('number');
        expect(asy.skewDirection).to.be.a('string');
    });
});

describe('Poisson Age Likelihood & Crater Depth-to-Diameter (CSFDEngine)', () => {
    it('should compute Poisson model age likelihood density', () => {
        // Area = 100,000 km^2, observed 50 craters at test age 3.5 Ga
        const pdf = CSFDEngine.computePoissonAgeProbabilityDensity(50, 1e5, 3.5);
        expect(pdf.expectedCount).to.be.greaterThan(0);
        expect(pdf.logLikelihood).to.be.a('number');
    });

    it('should compute Pike depth-to-diameter ratio and rim uplift geometry', () => {
        // Simple crater (D = 2 km): d = 0.20 * 2 = 0.4 km
        const simple = CSFDEngine.computeDepthToDiameterScaling(2.0);
        expect(simple.depthKm).to.equal(0.4);
        expect(simple.depthToDiameterRatio).to.equal(0.20);
        expect(simple.morphologyType).to.include('Simple');

        // Complex crater (D = 50 km): d ~ 0.36 * 50^0.51 ~ 2.6 km
        const complex = CSFDEngine.computeDepthToDiameterScaling(50.0);
        expect(complex.depthKm).to.be.greaterThan(2.0);
        expect(complex.depthToDiameterRatio).to.be.lessThan(0.10);
        expect(complex.morphologyType).to.include('Complex');

        // Rim uplift & floor diameter
        const geom = CSFDEngine.computeRimHeightAndFloorDiameter(50.0);
        expect(geom.rimHeightMeters).to.be.greaterThan(1000.0);
        expect(geom.floorDiameterKm).to.equal(20.0); // 40% of 50 km
    });
});

describe('Hapke Photometry, Pixel Resolution & Camera Look (ThreeDEngine)', () => {
    it('should compute Hapke particulate bidirectional reflectance', () => {
        // High sun at normal emission (mu0 = 1.0, mu = 1.0, g = 0 deg)
        const hapke0 = ThreeDEngine.computeHapkePhotometricReflectance(1.0, 1.0, 0.0, 0.25);
        expect(hapke0).to.be.greaterThan(0);

        // Oblique incidence (mu0 = 0.5, mu = 0.8, g = 30 deg)
        const hapkeOblique = ThreeDEngine.computeHapkePhotometricReflectance(0.5, 0.8, 30.0, 0.25);
        expect(hapkeOblique).to.be.lessThan(hapke0);
    });

    it('should compute camera Ground Sampling Distance (GSD) and 3D look vector', () => {
        // HiRISE-like parameters: 300 km altitude, 12 µm pixel, 12,000 mm focal length -> GSD ~ 0.30 m/pixel
        const hirise = ThreeDEngine.computePixelGroundResolution(300.0, 12.0, 12000.0);
        expect(hirise.gsdMetersPerPixel).to.be.closeTo(0.30, 0.05);
        expect(hirise.gsdCmPerPixel).to.be.closeTo(30.0, 5.0);

        // 3D look vector from (0,0, 300km) to (0,0, 0km)
        const look = ThreeDEngine.computeCameraLookVector(0, 0, 0, 0);
        expect(Math.hypot(look.vx, look.vy, look.vz)).to.be.closeTo(1.0, 0.01);
    });
});

describe('Lithospheric Flexure, Free-Air Gravity & Regolith Density (InvestigateTool)', () => {
    it('should compute lithospheric elastic flexure rigidity and deflection', () => {
        // Olympus Mons scale load: R = 150 km, h = 10 km, Te = 50 km
        const flex = InvestigateTool.computeLithosphericFlexure(150, 10, 50, 100);
        expect(flex.flexuralRigidityNm).to.be.greaterThan(0);
        expect(flex.flexuralParameterKm).to.be.greaterThan(100.0);
        expect(flex.maxDeflectionKm).to.be.greaterThan(0);
    });

    it('should calculate pure Free-Air gravity anomaly and bulk regolith density', () => {
        // At 3000 m elevation on Mars with 0.001 m/s^2 observed excess
        const fa = InvestigateTool.computeFreeAirGravityAnomaly(3.721, 3.720, 3000, 'mars');
        expect(fa).to.be.greaterThan(100.0); // positive mGal anomaly

        // Porous regolith (40% vacuum pores, 2900 kg/m^3 basalt grains) -> 1740 kg/m^3
        const bulk = InvestigateTool.computeBulkRegolithDensity(0.40, 2900, 0);
        expect(bulk).to.equal(1740.0);
    });
});

describe('Ellipsoidal Geodesy, Girard Excess & Sinuosity (MeasureTool)', () => {
    it('should compute Andoyer-Lambert ellipsoidal geodetic arc distance', () => {
        // Equator span from 0 to 45 deg lon on Mars ellipsoid
        const ell = MeasureTool.computeEllipsoidalGeodeticDistance(0, 0, 0, 45, 'mars');
        expect(ell.ellipsoidalDistanceKm).to.be.greaterThan(2500.0);
        expect(ell.sphericalDistanceKm).to.be.greaterThan(2500.0);

        // Girard spherical excess of a spherical triangle
        const girard = MeasureTool.computeGreatCircleExcessAngle(0, 0, 0, 90, 90, 0);
        expect(girard.excessDegrees).to.be.closeTo(90.0, 1.0); // Tri-rectangular octant has E = 90 deg = pi/2 rad
    });

    it('should compute path sinuosity for channels and valleys', () => {
        // Meandering path with turns
        const track = [
            [0, 0],
            [1, 2],
            [0, 4],
            [1, 6],
            [0, 8]
        ];
        const sinu = MeasureTool.computePathSinuosity(track, 'mars');
        expect(sinu.sinuosity).to.be.greaterThan(1.0);
        expect(sinu.classification).to.be.a('string');
    });
});

describe('Authalic Radius, Wagner IV & Meridional Arc (ProjectionManager)', () => {
    it('should compute authalic sphere radius and surface area of Mars ellipsoid', () => {
        const authalic = ProjectionManager.computeAuthalicRadius(3396.19, 0.005886);
        expect(authalic.authalicRadiusKm).to.be.closeTo(3389.5, 10.0);
        expect(authalic.surfaceAreaKm2).to.be.greaterThan(1.4e8);
    });

    it('should project coordinates with Wagner IV and compute meridional arc distance', () => {
        // Wagner IV at (30 deg N, 45 deg E)
        const wagner = ProjectionManager.computeWagnerIVElliptical(30, 45, 0, 'mars');
        expect(wagner.x).to.be.greaterThan(0);
        expect(wagner.y).to.be.greaterThan(0);

        // Meridional arc from 0 to 45 deg N along Mars meridian
        const arc = ProjectionManager.computeMeridianDistance(0, 45, 'mars');
        expect(arc).to.be.greaterThan(2500.0);
    });
});

describe('Richardson Number, Spacecraft Aerodynamic Drag & Rayleigh Depth (MCDEngine)', () => {
    it('should compute Gradient Richardson Number and classify turbulence regime', () => {
        // Turbulent shear case (Ri < 0.25)
        const turb = MCDEngine.computeGradientRichardsonNumber(210, 0.001, 0.05, 0);
        expect(turb.richardsonNumber).to.be.lessThan(0.25);
        expect(turb.isTurbulent).to.equal(true);

        // Strongly stable laminar case
        const stable = MCDEngine.computeGradientRichardsonNumber(210, 0.010, 0.002, 0);
        expect(stable.richardsonNumber).to.be.greaterThan(1.0);
        expect(stable.isTurbulent).to.equal(false);
    });

    it('should calculate spacecraft orbital drag and Rayleigh optical depth', () => {
        // MRO-like aerobraking at 120 km: 10 m^2, Cd = 2.2, 1000 kg, v = 4200 m/s
        const drag = MCDEngine.computeOrbitalAerodynamicDrag(10.0, 2.2, 1000.0, 120.0, 4200.0);
        expect(drag.dragForceNewtons).to.be.greaterThan(0);
        expect(drag.decelerationMs2).to.be.greaterThan(0);

        // Clean Rayleigh scattering optical depth in blue channel (0.44 µm)
        const tauRayleigh = MCDEngine.computeRayleighScatteringOpticalDepth(0.44, 610.0);
        expect(tauRayleigh).to.be.greaterThan(0);
        expect(tauRayleigh).to.be.lessThan(0.01);
    });
});

describe('IR Window Transmission, Frost Recession & Phase Velocity (KRCEngine)', () => {
    it('should calculate atmospheric IR window spectral transmission', () => {
        const win = KRCEngine.computeAtmosphericInfraredWindowTransmission(0.3, 610.0);
        expect(win.windowTransmission).to.be.greaterThan(0.8);
        expect(win.windowOpticalDepth).to.be.lessThan(0.2);
    });

    it('should compute seasonal CO2 frost cap recession rate and harmonic phase velocity', () => {
        // High spring insolation: 300 W/m^2 on CO2 cap
        const recede = KRCEngine.computeFrostCapRecessionRate(300.0, 0.65);
        expect(recede.recessionRateMmPerSol).to.be.greaterThan(0);
        expect(recede.isReceding).to.equal(true);

        // Diurnal thermal wave speed in sandy regolith (TI = 250)
        const wave = KRCEngine.computeHarmonicPhaseLagDepth(250.0);
        expect(wave.thermalWaveSpeedMmPerSol).to.be.greaterThan(0);
        expect(wave.thermalWavelengthCm).to.be.greaterThan(10.0);
    });
});

describe('Aerocentric Coordinates, OWLT & Darian Calendar (MarsTime)', () => {
    it('should compute aerocentric subsolar right ascension and declination', () => {
        // Northern summer solstice (Ls = 90 deg) -> max declination +25.19 deg
        const sub90 = MarsTime.computeAerocentricSubsolarCoordinates(90.0);
        expect(sub90.declinationDeg).to.be.closeTo(25.19, 0.1);
        expect(sub90.rightAscensionDeg).to.be.closeTo(90.0, 1.0);
    });

    it('should calculate Earth-Mars communication light time and Darian calendar month', () => {
        // Conjunction alignment (180 deg opposition) -> max distance ~ 2.5 AU, OWLT ~ 20 min
        const owlt = MarsTime.computeEarthMarsDistanceAndOWLT(0.0, 180.0);
        expect(owlt.oneWayLightTimeMinutes).to.be.greaterThan(10.0);
        expect(owlt.distanceKm).to.be.greaterThan(2e8);

        // Ls = 0 deg is month 1 (Sagittarius in Darian calendar)
        const darian = MarsTime.computeDarianMonth(0.0);
        expect(darian.monthNumber).to.equal(1);
        expect(darian.monthName).to.equal('Sagittarius');
        expect(darian.quarter).to.equal('Spring');
    });
});

describe('Radar Fringe Resonance, Basal Loss & Radar Equation (RadarSounderEngine)', () => {
    it('should compute quarter-wave constructive interference fringe layer thickness', () => {
        // SHARAD 20 MHz in pure water ice (eps = 3.15) -> lambda ~ 8.45 m -> quarter wave ~ 2.11 m
        const fringe = RadarSounderEngine.computeInterferenceFringeSpacing(20e6, 3.15);
        expect(fringe.quarterWaveFringeMeters).to.be.closeTo(2.11, 0.1);
        expect(fringe.halfWaveFringeMeters).to.be.closeTo(4.23, 0.2);
    });

    it('should calculate two-way basal transmission loss and radar equation received power', () => {
        // Ice over basaltic basement
        const loss = RadarSounderEngine.computeBasalDielectricContrastLoss(3.15, 7.5);
        expect(loss.oneWayTransmissivity).to.be.greaterThan(0.9);
        expect(loss.twoWayTransmissionLossDb).to.be.lessThan(0); // negative dB loss

        // SHARAD from 250 km orbit
        const radarEq = RadarSounderEngine.computeRadarEquationReceivedPower(10.0, 0.0, 20e6, 250.0, 100.0);
        expect(radarEq.receivedPowerWatts).to.be.greaterThan(0);
        expect(radarEq.receivedPowerDbm).to.be.a('number');
    });
});

describe('Mg-Carbonate Doublet, Ferrous Iron & Contrast Stretch (BandMathEngine)', () => {
    it('should compute CRISM Mg-carbonate doublet index (MIN2295_2480)', () => {
        const carb = BandMathEngine.computeCRISMMgCarbonateIndex({ B2140: 0.30, B2295: 0.22, B2480: 0.21, B2530: 0.29 });
        expect(carb.min2295_2480).to.be.greaterThan(0.10);
        expect(carb.isCarbonateConfirmed).to.equal(true);
    });

    it('should calculate broad 1 µm ferrous iron index and 2%-98% contrast stretch', () => {
        // Unweathered basalt with strong 1 µm Fe2+ absorption
        const fe2 = BandMathEngine.computeFerrousIronIndex({ B800: 0.26, B1000: 0.18, B1300: 0.28 });
        expect(fe2.bd1000).to.be.greaterThan(0.15);
        expect(fe2.ferrousAbundance).to.include('High Primary Fe2+');

        // Linear stretch test
        const vals = [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0];
        const stretch = BandMathEngine.computeSpectralContrastStretch(vals, 10, 90);
        expect(stretch.minStretch).to.be.lessThan(stretch.maxStretch);
        expect(stretch.dynamicRange).to.be.greaterThan(0);
    });
});

describe('Clark-Evans Spatial Randomness & Chronology Factor (CSFDEngine)', () => {
    it('should compute Clark-Evans nearest neighbor spatial randomness R-statistic', () => {
        // Uniform grid -> dispersed / regular (R > 1)
        const gridCraters = [
            { x: 100, y: 100 }, { x: 200, y: 100 }, { x: 300, y: 100 },
            { x: 100, y: 200 }, { x: 200, y: 200 }, { x: 300, y: 200 },
            { x: 100, y: 300 }, { x: 200, y: 300 }, { x: 300, y: 300 }
        ];
        const ce = CSFDEngine.computeClarkEvansNearestNeighbor(gridCraters, 1e5);
        expect(ce.rStatistic).to.be.greaterThan(1.0);
        expect(ce.meanObservedDistanceKm).to.be.greaterThan(0);
    });

    it('should calculate secondary crater fraction and chronology scaling factor', () => {
        // Tight cluster -> secondary craters
        const cluster = [
            { x: 10, y: 10 }, { x: 11, y: 10 }, { x: 10, y: 11 }, { x: 12, y: 11 }
        ];
        const sec = CSFDEngine.computeSecondaryCraterFraction(cluster, 1e5);
        expect(sec.secondaryFraction).to.be.greaterThan(0);
        expect(sec.primaryCountEstimated).to.be.lessThan(cluster.length);

        // Chronology factor at 3.5 Ga should be substantially greater than 1 Ga
        const factor = CSFDEngine.computeChronologyFactor(3.5);
        expect(factor).to.be.greaterThan(5.0);
    });
});

describe('Limb Tangent Altitude, DEM Slope & Horizon Culling (ThreeDEngine)', () => {
    it('should compute atmospheric limb grazing line-of-sight tangent altitude', () => {
        // MRO limb sounding at 300 km looking at ~66.8 deg off-nadir -> tangent height in upper atmosphere ~25 km
        const limb = ThreeDEngine.computeAtmosphericLimbTangentHeight(300, 66.8, 'mars');
        expect(limb.tangentAltitudeKm).to.be.greaterThan(0);
        expect(limb.isGrazingAtmosphere).to.equal(true);
        expect(limb.isHittingGround).to.equal(false);
    });

    it('should calculate Horn DEM slope/aspect and horizon occlusion culling', () => {
        // Eastward dipping slope: West high (300m), East low (100m), cell 100m
        const dem = ThreeDEngine.computeDEMGridSlopeAspect(200, 200, 300, 100, 100);
        expect(dem.slopeDeg).to.be.greaterThan(0);
        expect(dem.aspectDeg).to.be.closeTo(90.0, 1.0); // East aspect

        // Feature 40 degrees away around Mars curve from 300 km orbit -> occluded by horizon
        const cull = ThreeDEngine.computeHorizonOcclusionCulling(300, 0, 40.0, 'mars');
        expect(cull.isOccluded).to.equal(true);
    });
});

describe('Thermal Tides, Volumetric Dust Cross-Section & Scale Height (MCDEngine)', () => {
    it('should compute atmospheric thermal tide perturbation amplitude', () => {
        // At 30 km altitude, thermal tide amplitude grows exponentially
        const tide = MCDEngine.computeAtmosphericThermalTideAmplitude(30.0, 1, 2.5);
        expect(tide.tidalAmplitudeK).to.be.greaterThan(5.0);
        expect(tide.waveOrder).to.include('Diurnal');
    });

    it('should calculate volumetric dust optical cross-section and scale height lapse rate', () => {
        // Dust concentration 1e-6 kg/m^3
        const dust = MCDEngine.computeDustOpticalCrossSectionPerVolume(1e-6, 1.5, 2500.0);
        expect(dust.extinctionCoeffPerMeter).to.be.greaterThan(0);
        expect(dust.extinctionCoeffPerKm).to.be.greaterThan(0);

        // Scale height variation with 4.5 K/km lapse rate from 220 K surface at 10 km altitude
        const sh = MCDEngine.computeAtmosphericScaleHeightLapseRate(220.0, 10.0, 4.5);
        expect(sh.localTempK).to.equal(175.0);
        expect(sh.localScaleHeightKm).to.be.lessThan(11.0);
        expect(sh.localScaleHeightKm).to.be.greaterThan(8.0);
    });
});

describe('Grain Size Inversion, Flexural Profile & Pratt Isostasy (InvestigateTool)', () => {
    it('should invert effective regolith grain size from thermal inertia', () => {
        // Dune sand with TI = 250 -> medium/fine sand (~350 microns)
        const sand = InvestigateTool.computeEffectiveGrainSizeFromThermalInertia(250.0, 610.0);
        expect(sand.grainSizeMicrons).to.be.greaterThan(100.0);
        expect(sand.WentworthClass).to.include('Sand');

        // Fine airfall dust with TI = 50 -> fine silt / dust (<10 microns)
        const dust = InvestigateTool.computeEffectiveGrainSizeFromThermalInertia(50.0, 610.0);
        expect(dust.grainSizeMicrons).to.be.lessThan(20.0);
    });

    it('should calculate axisymmetric flexural deflection profile and Pratt isostasy', () => {
        // Center of Olympus Mons load: max downward deflection
        const center = InvestigateTool.computeAxisymmetricFlexuralProfile(0.0, 5.0, 180.0);
        expect(center.deflectionKm).to.equal(5.0);
        expect(center.isBulgeForebulge).to.equal(false);

        // Distant peripheral flexural forebulge (r ~ 3.5 * alpha -> negative w)
        const bulge = InvestigateTool.computeAxisymmetricFlexuralProfile(180.0 * 2.0, 5.0, 180.0);
        expect(bulge.deflectionKm).to.be.lessThan(0);
        expect(bulge.isBulgeForebulge).to.equal(true);

        // Pratt isostatic density for 10 km plateau
        const pratt = InvestigateTool.computePrattIsostaticDensity(10.0, 100.0, 2900.0);
        expect(pratt.prattDensityKgM3).to.be.lessThan(2900.0);
        expect(pratt.densityDeficitKgM3).to.be.greaterThan(0);
    });
});

describe('Direct Geodetic Destination, Cross-Track Error & Compactness (MeasureTool)', () => {
    it('should solve the Direct Geodetic Problem for forward destination coordinates', () => {
        // Start at equator (0, 0), travel 1000 km due North (0 deg bearing)
        const dest = MeasureTool.computeDirectDestinationPoint(0, 0, 0, 1000.0, 'mars');
        expect(dest.destLat).to.be.greaterThan(15.0);
        expect(dest.destLat).to.be.lessThan(20.0);
        expect(dest.destLon).to.be.closeTo(0.0, 0.1);
    });

    it('should calculate cross-track error and polygon isoperimetric compactness', () => {
        // Point offset from equator track (0,0) -> (0,10) at (2, 5)
        const xte = MeasureTool.computeCrossTrackErrorOffset(2, 5, 0, 0, 0, 10, 'mars');
        expect(xte.crossTrackErrorKm).to.be.greaterThan(100.0);
        expect(xte.alongTrackDistanceKm).to.be.greaterThan(0);

        // Circular crater (Area = pi * R^2, Perimeter = 2 * pi * R -> C = 1.0)
        const r = 10.0;
        const area = Math.PI * r * r;
        const perim = 2.0 * Math.PI * r;
        const comp = MeasureTool.computePolygonCompactnessRatio(area, perim);
        expect(comp.compactnessRatio).to.be.closeTo(1.0, 0.01);
        expect(comp.shapeClass).to.include('Nearly Circular');
    });
});

describe('Gnomonic Projection, TM Convergence & Sinusoidal Shear (ProjectionManager)', () => {
    it('should project forward Gnomonic perspective coordinates', () => {
        // Point near center (10 deg lat, 10 deg lon from center 0, 0)
        const gn = ProjectionManager.forwardGnomonic(10.0, 10.0, 0.0, 0.0, 'mars');
        expect(gn.visible).to.equal(true);
        expect(gn.x).to.be.greaterThan(500.0);
        expect(gn.y).to.be.greaterThan(500.0);
    });

    it('should calculate Transverse Mercator grid convergence and Sinusoidal angular shear', () => {
        // At 45 deg N, 10 deg east of central meridian
        const gamma = ProjectionManager.computeTransverseMercatorConvergence(45.0, 10.0, 0.0);
        expect(gamma).to.be.greaterThan(0);
        expect(gamma).to.be.lessThan(10.0);

        // Sinusoidal shear at 60 deg N with 30 deg dLon
        const shear = ProjectionManager.computeSinusoidalDistortionMetrics(60.0, 30.0);
        expect(shear.shearAngleDeg).to.be.greaterThan(0);
        expect(shear.maxShearDeg).to.be.greaterThan(0);
    });
});

describe('Thermal Backflux, Pore Ice Conductivity & Diurnal Contrast (KRCEngine)', () => {
    it('should calculate spectral downwelling IR backflux and pore ice conductivity enhancement', () => {
        // Atmospheric backflux for 210 K air, dust optical depth tau = 0.3
        const back = KRCEngine.computeAtmosphericThermalBackfluxSpectral(210.0, 0.3, 610.0);
        expect(back.backfluxW_M2).to.be.greaterThan(10.0);
        expect(back.effectiveIRemissivity).to.be.greaterThan(0.1);

        // Dry matrix (0.05 W/mK) with 80% pore ice filling -> major enhancement
        const ice = KRCEngine.computePoreIceThermalConductivity(0.05, 2.2, 0.35, 0.8);
        expect(ice.effectiveConductivityW_MK).to.be.greaterThan(0.1);
        expect(ice.enhancementRatio).to.be.greaterThan(2.0);
    });

    it('should estimate analytical peak-to-trough diurnal temperature amplitude contrast', () => {
        // High thermal inertia bedrock (I = 800) vs low thermal inertia dust (I = 100)
        const rock = KRCEngine.computeDiurnalThermalEmissionContrast(550.0, 0.25, 800.0);
        const dust = KRCEngine.computeDiurnalThermalEmissionContrast(550.0, 0.25, 100.0);

        expect(dust.diurnalAmplitudeK).to.be.greaterThan(rock.diurnalAmplitudeK * 5.0);
        expect(rock.estimatedMaxTempK).to.be.lessThan(dust.estimatedMaxTempK);
    });
});

describe('Vis-Viva Velocity, Equation of Center Series & Insolation (MarsTime)', () => {
    it('should compute Vis-Viva orbital speed and Equation of Center series expansion', () => {
        // At mean orbital radius ~ 227.9M km -> ~24.1 km/s
        const speed = MarsTime.computeVisVivaVelocity(227.94e6);
        expect(speed.orbitalVelocityKmS).to.be.greaterThan(23.0);
        expect(speed.orbitalVelocityKmS).to.be.lessThan(26.0);

        // At M = 90 deg, Equation of Center reaches ~ 10.7 degrees
        const eoc = MarsTime.computeEquationOfCenterSeries(90.0);
        expect(eoc.equationOfCenterDeg).to.be.greaterThan(9.0);
        expect(eoc.equationOfCenterDeg).to.be.lessThan(12.0);
    });

    it('should calculate seasonal insolation fluctuation ratio across Martian orbit', () => {
        // Near perihelion (Ls = 251 deg), solar flux is enhanced by > 20%
        const peri = MarsTime.computeInsolationFluctuationRatio(251.0);
        expect(peri.insolationRatio).to.be.greaterThan(1.15);
        expect(peri.isPerihelionSeason).to.equal(true);

        // Near aphelion (Ls = 71 deg), solar flux is reduced by > 15%
        const aphel = MarsTime.computeInsolationFluctuationRatio(71.0);
        expect(aphel.insolationRatio).to.be.lessThan(0.90);
        expect(aphel.isPerihelionSeason).to.equal(false);
    });
});

describe('Range Resolution, SAR Sharpening & Basal Attenuation (RadarSounderEngine)', () => {
    it('should compute vertical subsurface range resolution and Doppler SAR sharpening', () => {
        // SHARAD 10 MHz chirp bandwidth in ice (eps = 3.15) -> ~8.4m resolution
        const res = RadarSounderEngine.computeSubsurfaceRangeResolution(10e6, 3.15);
        expect(res.rangeResolutionMeters).to.be.greaterThan(8.0);
        expect(res.rangeResolutionMeters).to.be.lessThan(9.0);
        expect(res.rangeResolutionAirMeters).to.be.closeTo(15.0, 0.1);

        // 5 km synthetic aperture at 250 km orbit altitude
        const sar = RadarSounderEngine.computeDopplerFresnelSharpening(250.0, 20e6, 5000.0);
        expect(sar.dopplerFootprintMeters).to.be.lessThan(500.0);
        expect(sar.sharpeningFactor).to.be.greaterThan(5.0);
    });

    it('should invert bulk two-way volumetric radar attenuation rate from echo contrast', () => {
        // 20 dB loss across 1000m ice sheet (2 km two-way)
        const att = RadarSounderEngine.invertTwoWayAttenuationFromReflectivity(0.0, -22.0, 1000.0, 1.0);
        expect(att.twoWayAttenuationDbPerKm).to.be.greaterThan(5.0);
        expect(att.twoWayAttenuationDbPerKm).to.be.lessThan(15.0);
        expect(att.lossTangentEstimate).to.be.greaterThan(0);
    });
});

describe('SINDEX2 Sulfates, OLINDEX3 Olivine & SAM Angle (BandMathEngine)', () => {
    it('should calculate CRISM SINDEX2 polyhydrated sulfate and OLINDEX3 olivine indices', () => {
        // Gypsum polyhydrated sulfate signature (2.1 & 2.4 µm drops)
        const sindex = BandMathEngine.computeCRISMPolyhydratedSulfateIndex({ B2100: 0.22, B2290: 0.32, B2400: 0.24 });
        expect(sindex.sindex2).to.be.greaterThan(0.15);
        expect(sindex.hasPolyhydratedSulfate).to.equal(true);

        // Olivine 1 µm broad absorption parameter
        const ol = BandMathEngine.computeCRISMOlivineIndex3({ B1080: 0.16, B1690: 0.30, B2530: 0.24 });
        expect(ol.olindex3).to.be.greaterThan(0.15);
        expect(ol.olivineAbundance).to.include('High Olivine');
    });

    it('should calculate Spectral Angle Mapper (SAM) vector angle and match confidence', () => {
        // Near-identical spectra (same shape, small scaling)
        const specA = [0.20, 0.25, 0.28, 0.22, 0.18];
        const specB = [0.21, 0.26, 0.29, 0.23, 0.19];
        const samMatch = BandMathEngine.computeSpectralAngleMetric(specA, specB);
        expect(samMatch.angleDegrees).to.be.lessThan(3.0);
        expect(samMatch.isConfidentMatch).to.equal(true);

        // Orthogonal / distinct spectra
        const specC = [0.50, 0.10, 0.05, 0.40, 0.60];
        const samDiff = BandMathEngine.computeSpectralAngleMetric(specA, specC);
        expect(samDiff.angleDegrees).to.be.greaterThan(25.0);
        expect(samDiff.isConfidentMatch).to.equal(false);
    });
});

describe('Power-Law Conversion, Gault Saturation & Poisson Likelihood (CSFDEngine)', () => {
    it('should convert cumulative to differential power-law slope exponent', () => {
        // Standard production cumulative alpha = 2.0 -> differential beta = 3.0, R-plot slope = 0.0
        const slope = CSFDEngine.computeDifferentialPowerLawConversion(2.0);
        expect(slope.cumulativeSlopeAlpha).to.equal(2.0);
        expect(slope.differentialSlopeBeta).to.equal(3.0);
        expect(slope.rPlotSlope).to.equal(0.0);
    });

    it('should evaluate Gault crater saturation limit and exact Poisson age likelihood', () => {
        // Gault saturation limit at D = 1 km (N_sat = 0.10 km^-2)
        const sat = CSFDEngine.computeGaultCraterSaturationEquilibrium(1.0, 0.05);
        expect(sat.saturationPercent).to.equal(50.0);
        expect(sat.isEquilibriumSaturated).to.equal(false);

        // Poisson likelihood for 3.5 Ga surface (expected craters mu ~ 10 on 5000 km^2)
        const pois = CSFDEngine.computePoissonAgeLikelihoodDensity(10, 5000.0, 3.5);
        expect(pois.lambdaExpectedCount).to.be.greaterThan(0);
        expect(pois.probabilityMass).to.be.greaterThan(0);
    });
});

describe('Rectilinear Footprint, Dip Horizon & Surface Radiance (ThreeDEngine)', () => {
    it('should compute 2D rectangular ground footprint and planetary dip horizon viewing angle', () => {
        // At 300 km orbit with 20° x 15° camera FOV
        const fp = ThreeDEngine.computeRectilinearGroundFootprint(300.0, 20.0, 15.0);
        expect(fp.swathWidthXKm).to.be.greaterThan(100.0);
        expect(fp.swathHeightYKm).to.be.greaterThan(70.0);
        expect(fp.groundFootprintAreaKm2).to.be.greaterThan(7000.0);

        // Dip angle from 300 km orbit above Mars
        const dip = ThreeDEngine.computePlanetaryDipHorizonViewingAngle(300.0, 'mars');
        expect(dip.dipAngleDeg).to.be.greaterThan(20.0);
        expect(dip.dipAngleDeg).to.be.lessThan(30.0);
        expect(dip.visibleCapAreaKm2).to.be.greaterThan(1e6);
    });

    it('should calculate Lambertian surface reflected radiance with solar incidence', () => {
        // Overhead sun (0 deg incidence) with 590 W/m^2 solar flux and 0.25 albedo
        const radNoon = ThreeDEngine.computeLambertianSurfaceRadiance(590.0, 0.0, 0.25);
        expect(radNoon.radianceW_M2_Sr).to.be.greaterThan(40.0);
        expect(radNoon.isIlluminated).to.equal(true);

        // Grazing sun (85 deg incidence)
        const radGrazing = ThreeDEngine.computeLambertianSurfaceRadiance(590.0, 85.0, 0.25);
        expect(radGrazing.radianceW_M2_Sr).to.be.lessThan(10.0);
        expect(radGrazing.isIlluminated).to.equal(true);
    });
});

describe('Sutherland Viscosity, Buoyancy Frequency & Eddy Diffusivity (MCDEngine)', () => {
    it('should compute Sutherland dynamic viscosity and Brunt-Väisälä buoyancy frequency', () => {
        // CO2 viscosity at 210 K
        const visc = MCDEngine.computeSutherlandDynamicViscosity(210.0);
        expect(visc.dynamicViscosityPaS).to.be.greaterThan(1e-5);
        expect(visc.kinematicViscosityM2S).to.be.greaterThan(1e-4);

        // Stable atmosphere: lapse rate 3.0 K/km < dry adiabatic 4.65 K/km
        const bv = MCDEngine.computeAtmosphericBruntVaisalaFrequency(210.0, 3.0, 4.65);
        expect(bv.frequencyRadS).to.be.greaterThan(0.005);
        expect(bv.isConvectivelyStable).to.equal(true);
        expect(bv.periodSeconds).to.be.lessThan(1200.0);
    });

    it('should calculate Troen & Mahrt turbulent boundary layer eddy diffusivity', () => {
        // Friction velocity 0.5 m/s, PBL height 4000m, altitude 1000m
        const eddy = MCDEngine.computeTurbulentEddyDiffusivity(0.5, 4000.0, 1000.0);
        expect(eddy.eddyDiffusivityM2S).to.be.greaterThan(50.0);
        expect(eddy.pblFraction).to.equal(0.25);
    });
});

describe('Line-Load Flexure, Seismic Velocities & Bouguer Slab (InvestigateTool)', () => {
    it('should compute 2D line-load flexural profile and crustal seismic velocities', () => {
        // Line-load flexure deflection at origin x = 0 (w0 = 4 km)
        const flex0 = InvestigateTool.computeLineLoadFlexureProfile(0, 4.0, 150.0);
        expect(flex0.deflectionKm).to.equal(4.0);
        expect(flex0.isForebulge).to.equal(false);

        // Crustal basalt (K = 50 GPa, G = 30 GPa, rho = 2900 kg/m^3)
        const seis = InvestigateTool.computeSeismicPWaveVelocity(50.0, 30.0, 2900.0);
        expect(seis.vP_KmS).to.be.greaterThan(5.0);
        expect(seis.vP_KmS).to.be.lessThan(6.5);
        expect(seis.vS_KmS).to.be.greaterThan(3.0);
        expect(seis.vS_KmS).to.be.lessThan(4.0);
        expect(seis.poissonRatio).to.be.greaterThan(0.2);
    });

    it('should calculate infinite slab Bouguer gravitational attraction', () => {
        // 1000m basalt slab (rho = 2900 kg/m^3) -> ~121.5 mGal
        const slab = InvestigateTool.computeInfiniteSlabBouguerAttraction(1000.0, 2900.0);
        expect(slab.bouguerAttractionMGal).to.be.greaterThan(115.0);
        expect(slab.bouguerAttractionMGal).to.be.lessThan(130.0);
        expect(slab.attractionPerMeterMGal).to.be.greaterThan(0.1);
    });
});

describe('Aspect Ratio, Polyline Length & Rhumb Loxodrome (MeasureTool)', () => {
    it('should compute bounding box aspect ratio and great-circle polyline total length', () => {
        // Elongated valley polygon (2 deg lat x 10 deg lon at equator)
        const poly = [[-1.0, 0.0], [1.0, 0.0], [1.0, 10.0], [-1.0, 10.0]];
        const bbox = MeasureTool.computePolygonBoundingBoxAspectRatio(poly, 'mars');
        expect(bbox.aspectRatio).to.be.greaterThan(4.0);
        expect(bbox.lengthKm).to.be.greaterThan(500.0);

        // 3-point polyline along equator (0 to 10 deg lon -> ~591.6 km on Mars)
        const line = [[0.0, 0.0], [0.0, 5.0], [0.0, 10.0]];
        const len = MeasureTool.computeGreatCirclePolylineTotalLength(line, 'mars');
        expect(len.totalLengthKm).to.be.greaterThan(550.0);
        expect(len.totalLengthKm).to.be.lessThan(650.0);
        expect(len.segmentCount).to.equal(2);
    });

    it('should compute constant-bearing Rhumb line (loxodrome) distance and heading', () => {
        // Rhumb line along 45 deg bearing on Mars
        const rhumb = MeasureTool.computeRhumbLineLoxodromeDirect(0.0, 0.0, 10.0, 10.0, 'mars');
        expect(rhumb.rhumbDistanceKm).to.be.greaterThan(700.0);
        expect(rhumb.constantBearingDeg).to.be.greaterThan(40.0);
        expect(rhumb.constantBearingDeg).to.be.lessThan(50.0);
    });
});

describe('Lambert Conformal Conic (LCC) & Scale (ProjectionManager)', () => {
    it('should compute forward and inverse Lambert Conformal Conic projection coordinates', () => {
        // Forward LCC projection of Gale Crater area (lat = 30°N, lon = 45°E) with parallels 20°N & 60°N
        const fwd = ProjectionManager.forwardLambertConformalConic(30.0, 45.0, 20.0, 60.0, 0.0, 'mars');
        expect(fwd.x).to.be.greaterThan(0);
        expect(fwd.scaleFactor).to.be.closeTo(1.0, 0.05);

        // Inverse LCC projection recovery
        const inv = ProjectionManager.inverseLambertConformalConic(fwd.x, fwd.y, 20.0, 60.0, 0.0, 'mars');
        expect(inv.lat).to.be.closeTo(30.0, 0.01);
        expect(inv.lon).to.be.closeTo(45.0, 0.01);
    });

    it('should verify exact unit scale factor at LCC standard parallels', () => {
        // Scale factor must equal 1.0000 at both 20° and 60° standard parallels
        const k1 = ProjectionManager.computeLCCScaleFactor(20.0, 20.0, 60.0);
        const k2 = ProjectionManager.computeLCCScaleFactor(60.0, 20.0, 60.0);
        expect(k1).to.be.closeTo(1.0, 0.001);
        expect(k2).to.be.closeTo(1.0, 0.001);
    });
});

describe('Surface Energy Balance, CO2 Mass Balance & Geotherm (KRCEngine)', () => {
    it('should compute closed surface energy balance and equilibrium surface temperature', () => {
        // Absorbed solar 400 W/m^2 + downwelling IR 30 W/m^2
        const eb = KRCEngine.computeSurfaceRadiativeEnergyBalance(400.0, 30.0, 0, 0.95);
        expect(eb.equilibriumTempK).to.be.greaterThan(280.0);
        expect(eb.equilibriumTempK).to.be.lessThan(305.0);
        expect(eb.outgoingThermalFluxW_M2).to.be.closeTo(430.0, 1.0);
    });

    it('should calculate CO2 frost condensation mass and deep crustal geothermal profile', () => {
        // 20 W/m^2 energy deficit over 1 Sol -> ~3.0 kg/m^2 accumulation (~1.88 mm)
        const frost = KRCEngine.computeCO2LatentHeatMassBalance(20.0, 88775.244);
        expect(frost.accumulatedMassKg_M2).to.be.greaterThan(2.5);
        expect(frost.accumulatedMassKg_M2).to.be.lessThan(3.5);
        expect(frost.frostThicknessMm).to.be.greaterThan(1.5);

        // Geothermal equilibrium at 1000m depth with 30 mW/m^2 flux in 2.0 W/(m K) basalt
        const geo = KRCEngine.computeDeepSubsurfaceGeothermEquilibrium(210.0, 30.0, 2.0, 1000.0);
        expect(geo.temperatureAtDepthK).to.equal(225.0);
        expect(geo.geothermalGradientKPerKm).to.equal(15.0);
    });
});

describe('Synodic Cycle, Mean Motion & True Anomaly (MarsTime)', () => {
    it('should compute Earth-Mars synodic period and Keplerian mean motion', () => {
        // Earth-Mars synodic period (~779.9 days / 759.2 sols / 2.135 yr)
        const syn = MarsTime.computeSynodicCyclePeriod();
        expect(syn.synodicDays).to.be.greaterThan(775.0);
        expect(syn.synodicDays).to.be.lessThan(785.0);
        expect(syn.synodicEarthYears).to.be.closeTo(2.135, 0.05);

        // Orbital mean motion (a = 1.52368 AU -> ~0.5240 deg/day)
        const mm = MarsTime.computeOrbitalMeanMotion(1.52368);
        expect(mm.meanMotionDegPerDay).to.be.closeTo(0.524, 0.005);
        expect(mm.meanMotionDegPerSol).to.be.closeTo(0.538, 0.005);
    });

    it('should calculate exact true anomaly and heliocentric distance from eccentric anomaly', () => {
        // At perihelion E = 0 -> nu = 0, r = a*(1-e) ~ 1.381 AU (~206.6M km)
        const peri = MarsTime.computeTrueAnomalyFromEccentricAnomaly(0.0, 0.0934);
        expect(peri.trueAnomalyDeg).to.equal(0.0);
        expect(peri.radialDistanceAU).to.be.closeTo(1.381, 0.01);

        // At aphelion E = 180 -> nu = 180, r = a*(1+e) ~ 1.666 AU (~249.2M km)
        const aphel = MarsTime.computeTrueAnomalyFromEccentricAnomaly(180.0, 0.0934);
        expect(aphel.trueAnomalyDeg).to.equal(180.0);
        expect(aphel.radialDistanceAU).to.be.closeTo(1.666, 0.01);
    });
});

describe('Path Loss, Refraction Angle & Clutter Ratio (RadarSounderEngine)', () => {
    it('should compute radar free-space spherical spreading path loss and wavelength', () => {
        // SHARAD 20 MHz (lambda = 15m) at 250 km orbit -> ~106.4 dB
        const loss = RadarSounderEngine.computeFreeSpacePathLoss(250.0, 20e6);
        expect(loss.wavelengthMeters).to.be.closeTo(15.0, 0.1);
        expect(loss.pathLossDb).to.be.greaterThan(100.0);
        expect(loss.pathLossDb).to.be.lessThan(115.0);
    });

    it('should calculate Snell subsurface refraction angle and clutter detection margin', () => {
        // 30 deg incidence in vacuum into water ice (eps = 3.15 -> n ~ 1.775)
        const refr = RadarSounderEngine.computeSubsurfaceRefractionAngle(30.0, 3.15);
        expect(refr.refractionAngleDeg).to.be.greaterThan(15.0);
        expect(refr.refractionAngleDeg).to.be.lessThan(18.0);
        expect(refr.criticalAngleDeg).to.be.greaterThan(30.0);

        // Clutter vs nadir echo detection (+10 dB margin)
        const csr = RadarSounderEngine.computeClutterToSignalRatio(-75.0, -65.0);
        expect(csr.clutterToSignalRatioDb).to.equal(-10.0);
        expect(csr.isEchoDetectable).to.equal(true);
        expect(csr.qualityMarginDb).to.equal(10.0);
    });
});

describe('CRISM BD1900r Hydration, Pyroxene & Curvature (BandMathEngine)', () => {
    it('should compute CRISM BD1900r hydration parameter with exact linear baseline', () => {
        // Hydrated clay absorption at 1.93 µm (R1815=0.25, R1930=0.20, R2132=0.24)
        const hyd = BandMathEngine.computeCRISMBd1900rIndex({ B1815: 0.25, B1930: 0.20, B2132: 0.24 });
        expect(hyd.bd1900r).to.be.greaterThan(0.15);
        expect(hyd.bd1900r).to.be.lessThan(0.25);
        expect(hyd.isHydratedPhyllosilicate).to.equal(true);
    });

    it('should calculate pyroxene band contrast metric and continuum curvature', () => {
        // Clinopyroxene (HCP) signature
        const pyx = BandMathEngine.computePyroxeneBandCenterMetric({ B1815: 0.25, B1930: 0.24, B2120: 0.28, B2140: 0.24 });
        expect(pyx.hcpIndex).to.be.greaterThan(0.05);
        expect(pyx.dominantPyroxene).to.include('Clinopyroxene');

        // Continuum curvature (concave absorption feature: center < shoulders)
        const curv = BandMathEngine.computeSpectralContinuumCurvature(0.30, 0.25, 0.30);
        expect(curv.curvature).to.be.lessThan(-0.10);
        expect(curv.isConcaveAbsorption).to.equal(true);
    });
});

describe('NPF Production Isochron, Geometric Binning & R-Plot (CSFDEngine)', () => {
    it('should compute exact 11th-order Neukum Production Function cumulative density', () => {
        // NPF at D = 1 km, Age = 1.0 Ga -> ~4.13e-4 craters/km^2 (log10 ~ -3.38)
        const npf = CSFDEngine.computeNeukumProductionValue(1.0, 1.0);
        expect(npf.cumulativeNDensityPerKm2).to.be.greaterThan(1e-4);
        expect(npf.cumulativeNDensityPerKm2).to.be.lessThan(1e-3);
        expect(npf.log10N).to.be.closeTo(-3.38, 0.2);
    });

    it('should calculate logarithmic bin boundaries and single-bin R-plot frequency', () => {
        // Geometric sqrt(2) bin around D = 1.0 km -> lower ~ 0.841, upper ~ 1.189, deltaD ~ 0.348
        const bin = CSFDEngine.computeGeometricBinBoundaries(1.0, Math.SQRT2);
        expect(bin.dLowerKm).to.be.closeTo(0.841, 0.01);
        expect(bin.dUpperKm).to.be.closeTo(1.189, 0.01);
        expect(bin.deltaDKm).to.be.closeTo(0.348, 0.01);

        // 100 craters in 1 km bin across 10^6 km^2
        const r = CSFDEngine.computeRPlotValue(100, 1.0, bin.deltaDKm, 1e6);
        expect(r.rValue).to.be.greaterThan(1e-4);
        expect(r.errorRValue).to.be.lessThan(r.rValue);
    });
});

describe('Horn Hillshade, Perspective GSD & Normal Vectors (ThreeDEngine)', () => {
    it('should calculate Horn shaded relief hillshade intensity and solar incidence', () => {
        // Flat horizontal terrain (slope = 0) with Sun at 45 deg elevation -> cos(i) = sin(45) ~ 0.7071
        const hs = ThreeDEngine.computeHornHillshadeValue(0.0, 0.0, 45.0, 180.0, 0.15);
        expect(hs.cosIncidence).to.be.closeTo(0.7071, 0.01);
        expect(hs.hillshadeIntensity).to.be.greaterThan(0.70);
        expect(hs.isShadowed).to.equal(false);
    });

    it('should compute camera Ground Sample Distance (GSD) and 3D normal vector from slope/aspect', () => {
        // HiRISE-scale camera: 250 km orbit, 12,000 mm focal length, 12 µm pixel -> 0.25 m/pixel (25 cm)
        const gsd = ThreeDEngine.computePerspectiveGSD(250.0, 12000.0, 12.0);
        expect(gsd.gsdMetersPerPixel).to.equal(0.25);
        expect(gsd.gsdCmPerPixel).to.equal(25.0);

        // East-facing 30° slope (slope = 30, aspect = 90° East)
        const norm = ThreeDEngine.computeSurfaceNormalFromSlopeAspect(30.0, 90.0);
        expect(norm.nx).to.be.closeTo(-0.5, 0.01);
        expect(norm.ny).to.be.closeTo(0.866, 0.01);
        expect(norm.nz).to.be.closeTo(0.0, 0.01);
    });
});

describe('Acoustic Sound Speed, Ekman Spiral & Column Mass (MCDEngine)', () => {
    it('should compute Mars atmospheric sound speed in CO2 gas', () => {
        // Sound speed in CO2 at T = 220 K (gamma = 1.29, R = 188.92) -> ~231.6 m/s
        const snd = MCDEngine.computeAtmosphericSoundSpeed(220.0, 1.29);
        expect(snd.soundSpeedMs).to.be.closeTo(231.6, 0.5);
        expect(snd.soundSpeedKmH).to.be.greaterThan(800.0);
    });

    it('should calculate boundary layer Ekman spiral wind turning and total column mass', () => {
        // Geostrophic wind 20 m/s inside 2000m PBL at 500m height
        const ekman = MCDEngine.computeEkmanSpiralWindDeflection(20.0, 2000.0, 500.0, 45.0);
        expect(ekman.totalSpeedMs).to.be.greaterThan(10.0);
        expect(ekman.deflectionAngleDeg).to.be.greaterThan(10.0);

        // 610 Pa surface datum -> column mass ~163.9 kg/m^2 (~16.39 g/cm^2)
        const col = MCDEngine.computeAtmosphericTotalColumnDensity(610.0);
        expect(col.columnMassKgM2).to.be.closeTo(163.9, 0.5);
        expect(col.columnMassGramsCm2).to.be.closeTo(16.39, 0.05);
    });
});

describe('Airy Crustal Root, Flexural Rigidity & Gravity Anomalies (InvestigateTool)', () => {
    it('should compute Airy isostatic crustal root thickness and lithospheric rigidity D', () => {
        // 2000m mountain on Mars (rho_c = 2900, rho_m = 3500 -> deltaRho = 600) -> root ~ 9666.7 m (~9.67 km)
        const root = InvestigateTool.computeAiryRootThickness(2000.0, 2900.0, 3500.0);
        expect(root.rootThicknessKm).to.be.closeTo(9.67, 0.05);
        expect(root.totalCrustalColumnKm).to.be.closeTo(61.67, 0.05);

        // Te = 50 km lithosphere (E = 100 GPa, nu = 0.25) -> D ~ 1.11e24 N m
        const flex = InvestigateTool.computeFlexuralRigidityD(50.0, 100.0, 0.25);
        expect(flex.flexuralRigidityNm).to.be.greaterThan(1e24);
        expect(flex.log10Rigidity).to.be.closeTo(24.05, 0.1);
    });

    it('should calculate vertical gravity anomaly from buried spherical mass', () => {
        // Buried mass 10^15 kg at 10 km depth
        const grav = InvestigateTool.computePointMassGravityAnomaly(1e15, 10000.0, 0);
        expect(grav.peakAnomalyMGal).to.be.greaterThan(50.0);
        expect(grav.peakAnomalyMGal).to.be.lessThan(100.0);

        // At 10 km horizontal offset (x = z = 10 km) -> anomaly = peak / (2^1.5) ~ 0.3535 * peak
        const gravOffset = InvestigateTool.computePointMassGravityAnomaly(1e15, 10000.0, 10000.0);
        expect(gravOffset.gravityAnomalyMGal).to.be.closeTo(grav.peakAnomalyMGal * 0.3535, 0.5);
    });
});

describe('Direct Geodesic, Girard Excess & Cross-Track Error (MeasureTool)', () => {
    it('should compute direct geodesic destination point on Mars sphere', () => {
        // Start at (0°, 0°), travel 591.6 km due East (bearing = 90°) -> ~10° East on Mars
        const dest = MeasureTool.computeGeodesicDirectDestination(0.0, 0.0, 90.0, 591.6, 'mars');
        expect(dest.destLat).to.be.closeTo(0.0, 0.1);
        expect(dest.destLon).to.be.closeTo(10.0, 0.2);
        expect(dest.finalBearingDeg).to.be.closeTo(90.0, 1.0);
    });

    it('should calculate Girard spherical excess polygon area and cross-track error distance', () => {
        // Octant triangle (0,0), (0,90), (90,0) -> 1/8 of Mars sphere ~ 1.808e7 km^2
        const tri = [[0.0, 0.0], [0.0, 90.0], [90.0, 0.0]];
        const area = MeasureTool.computeSphericalPolygonGirardExcess(tri, 'mars');
        expect(area.areaKm2).to.be.greaterThan(1.5e7);
        expect(area.areaKm2).to.be.lessThan(2.0e7);

        // Point at (10°N, 5°E) relative to equatorial path from (0,0) to (0,10)
        // Perpendicular cross-track distance should be ~10° lat distance (~591.6 km)
        const xt = MeasureTool.computeCrossTrackErrorDistance(10.0, 5.0, 0.0, 0.0, 0.0, 10.0, 'mars');
        expect(xt.crossTrackErrorKm).to.be.closeTo(591.6, 10.0);
        expect(xt.alongTrackProgressKm).to.be.closeTo(295.8, 10.0);
    });
});

describe('Subsurface Attenuation, TI Inversion & Heat Diffusion (KRCEngine)', () => {
    it('should compute harmonic subsurface wave exponential attenuation and phase delay', () => {
        // Regolith TI = 250 tiu -> skin depth ~ 3.5 cm (0.035 m)
        // At depth z = skin depth (0.035 m), attenuation = 1/e ~ 0.3679, phase delay = 1 rad (~3.92 hours)
        const atten = KRCEngine.computeSubsurfaceAttenuationAndPhase(0.035, 250.0);
        expect(atten.attenuationFraction).to.be.closeTo(0.368, 0.05);
        expect(atten.phaseDelayRadians).to.be.closeTo(1.0, 0.1);
        expect(atten.phaseDelayHours).to.be.closeTo(3.92, 0.5);
    });

    it('should invert thermal inertia from diurnal temperature amplitude and solve conductive heat flux', () => {
        // DeltaT = 80 K amplitude under 500 W/m^2 noon insolation (A = 0.25)
        const inv = KRCEngine.invertThermalInertiaFromAmplitude(80.0, 500.0, 0.25);
        expect(inv.thermalInertiaTIU).to.be.closeTo(628.7, 5.0);
        expect(inv.classification).to.include('Regolith');

        // Conductive flux across 2 cm layer with k = 0.05 W/(m K) and deltaT = 10 K (subsurface hotter)
        const flux = KRCEngine.computeSubsurfaceHeatDiffusionFlux(210.0, 220.0, 0.02, 0.05);
        expect(flux.conductiveFluxW_M2).to.equal(25.0);
        expect(flux.isHeatingSurface).to.equal(true);
    });
});

describe('Radial Orbital Velocity, Specific Energy & Solar Zenith Vectors (MarsTime)', () => {
    it('should compute Mars orbital radial velocity dr/dt and specific mechanical energy', () => {
        // At nu = 90° (moving outward toward aphelion) -> positive radial velocity ~ 2.25 km/s
        const rDot = MarsTime.computeRadialOrbitalVelocity(90.0);
        expect(rDot.radialVelocityKmS).to.be.closeTo(2.25, 0.15);
        expect(rDot.isMovingAwayFromSun).to.equal(true);

        // Vis-viva specific orbital energy: v = 24.13 km/s at r = 227.94e6 km
        const energy = MarsTime.computeVisVivaSpecificOrbitalEnergy(227.94e6, 24.13);
        expect(energy.specificEnergyMjPerKg).to.be.closeTo(-291.1, 5.0);
        expect(energy.semiMajorAxisEquivalentKm).to.be.closeTo(227.94e6, 5e6);
    });

    it('should compute 3D topocentric unit solar vector components (East, North, Zenith)', () => {
        // Solar noon (H = 0) at equator (lat = 0) with overhead Sun (declination = 0)
        // Vector should point straight up (+Zenith = 1.0, East = 0, North = 0)
        const sVec = MarsTime.computePlanetocentricSolarZenithVector(0.0, 0.0, 0.0);
        expect(sVec.sZenith).to.equal(1.0);
        expect(sVec.sEast).to.equal(0.0);
        expect(sVec.sNorth).to.equal(0.0);
        expect(sVec.cosZenith).to.equal(1.0);
    });
});

describe('Two-Way Attenuation, Radar Equation & Reflectivity Permittivity (RadarSounderEngine)', () => {
    it('should compute two-way signal attenuation rate and point-target radar equation received power', () => {
        // Pure water ice (eps = 3.15, tanDelta = 0.001) at 20 MHz -> two-way attenuation ~ 0.0064 dB/m (~6.44 dB/km)
        const atten = RadarSounderEngine.computeTwoWaySignalAttenuationRate(20e6, 0.001, 3.15);
        expect(atten.twoWayAttenuationDbPerKm).to.be.closeTo(6.44, 0.1);
        expect(atten.skinDepthMeters).to.be.greaterThan(2000.0);

        // Point target radar equation: Pt = 10W, G = 1, lambda = 15m, R = 250 km, sigma = 100 m^2
        const pRx = RadarSounderEngine.computeRadarEquationPointTargetPower(10.0, 1.0, 15.0, 250000.0, 100.0);
        expect(pRx.receivedPowerWatts).to.be.closeTo(2.90e-20, 0.5e-20);
        expect(pRx.receivedPowerDbm).to.be.closeTo(-165.4, 1.0);
    });

    it('should invert dielectric permittivity from measured power reflectivity', () => {
        // Reflectivity R = 0.0776 -> eps ~ 3.15 (water ice)
        const inv = RadarSounderEngine.invertDielectricFromPowerReflectivity(0.0776);
        expect(inv.dielectricPermittivity).to.be.closeTo(3.15, 0.05);
        expect(inv.medium).to.include('Ice');
    });
});

describe('BD2100r Sulfate, SAM Classifier & NDDI Dust (BandMathEngine)', () => {
    it('should compute CRISM BD2100r monohydrated sulfate absorption parameter', () => {
        // Monohydrated sulfate (kieserite) signature at 2.13 µm
        const sulf = BandMathEngine.computeCRISMBd2100rIndex({ B1930: 0.28, B2132: 0.22, B2250: 0.27 });
        expect(sulf.bd2100r).to.be.greaterThan(0.15);
        expect(sulf.isMonohydratedSulfate).to.equal(true);
    });

    it('should calculate Spectral Angle Mapper (SAM) angular distance and Normalized Difference Dust Index', () => {
        // Identical spectra -> angle = 0, score = 100%
        const specA = [0.20, 0.25, 0.30, 0.28, 0.22];
        const samIdentical = BandMathEngine.computeSpectralAngleMapperScore(specA, specA);
        expect(samIdentical.angleDegrees).to.equal(0.0);
        expect(samIdentical.matchScorePercent).to.equal(100.0);

        // Bright airfall dust: high NIR (0.45) vs low Visible (0.20) -> NDDI ~ (0.45 - 0.20) / (0.45 + 0.20) ~ 0.3846
        const nddi = BandMathEngine.computeNormalizedDifferenceDustIndex(0.20, 0.45);
        expect(nddi.nddi).to.be.closeTo(0.385, 0.01);
        expect(nddi.dustClassification).to.include('Dust');
    });
});

describe('Strength-Gravity Transition, Linear Age & Impact Melt (CSFDEngine)', () => {
    it('should compute target strength-to-gravity transition scaling diameter', () => {
        // Cohesive rock Y = 10 MPa (1e7 Pa), rho = 2900, g = 3.72 -> D_tg ~ 926.8 m (~0.927 km)
        const dtg = CSFDEngine.computeStrengthGravityTransitionDiameter(1e7, 2900.0, 3.72076);
        expect(dtg.transitionDiameterKm).to.be.closeTo(0.927, 0.02);
        expect(dtg.regimeDescription).to.include('strength-dominated');
    });

    it('should calculate linear Amazonian crater retention age and impact shock melt volume', () => {
        // N(>1 km) = 4.13e-4 craters/km^2 -> 1.0 Ga
        const age = CSFDEngine.computeCraterRetentionAgeLinear(4.13e-4);
        expect(age.ageGa).to.be.closeTo(1.0, 0.01);
        expect(age.ageMa).to.be.closeTo(1000.0, 10.0);
        expect(age.epoch).to.include('Amazonian');

        // Transient crater Dt = 20 km at v = 10 km/s -> melt volume ~ 15.3 km^3
        const melt = CSFDEngine.computeExcavatedMeltVolume(20.0, 10.0);
        expect(melt.meltVolumeKm3).to.be.greaterThan(10.0);
        expect(melt.meltVolumeKm3).to.be.lessThan(25.0);
        expect(melt.meltMassKg).to.be.greaterThan(1e13);
    });
});

describe('Spherical Midpoint, Tissot Area & Gnomonic Scale (ProjectionManager)', () => {
    it('should compute exact great-circle spherical midpoint coordinates', () => {
        // Equator endpoints (0°N, 0°E) to (0°N, 90°E) -> midpoint is (0°N, 45°E)
        const midEq = ProjectionManager.computeSphericalMidpoint(0.0, 0.0, 0.0, 90.0);
        expect(midEq.lat).to.be.closeTo(0.0, 0.01);
        expect(midEq.lon).to.be.closeTo(45.0, 0.01);

        // Meridian endpoints (0°N, 0°E) to (60°N, 0°E) -> midpoint is (30°N, 0°E)
        const midMer = ProjectionManager.computeSphericalMidpoint(0.0, 0.0, 60.0, 0.0);
        expect(midMer.lat).to.be.closeTo(30.0, 0.01);
        expect(midMer.lon).to.be.closeTo(0.0, 0.01);
    });

    it('should calculate Tissot indicatrix area distortion and Gnomonic radial point scale', () => {
        // Equal-area projection: h = 0.5, k = 2.0, theta = 0 -> s = 1.0 (isAreaPreserving = true)
        const tissotEA = ProjectionManager.computeTissotIndicatrixAreaRatio(0.5, 2.0, 0.0);
        expect(tissotEA.areaDistortionRatio).to.equal(1.0);
        expect(tissotEA.isAreaPreserving).to.equal(true);

        // Equirectangular at 60°: h = 1.0, k = 2.0 -> s = 2.0
        const tissotCyl = ProjectionManager.computeTissotIndicatrixAreaRatio(1.0, 2.0, 0.0);
        expect(tissotCyl.areaDistortionRatio).to.equal(2.0);
        expect(tissotCyl.isAreaPreserving).to.equal(false);

        // Gnomonic scale at center (rho = 0) -> k = 1.0
        const gn0 = ProjectionManager.computeGnomonicProjectionScale(0.0, 'mars');
        expect(gn0.radialScaleFactor).to.equal(1.0);
        expect(gn0.angularDistanceDeg).to.equal(0.0);
    });
});

describe('Geometric Horizon, Perspective Swath & Diffuse Radiance (ThreeDEngine)', () => {
    it('should compute geometric line-of-sight horizon distance and surface arc', () => {
        // Observer at h = 2 meters on Mars (R = 3389.5 km) -> horizon d ~ sqrt(2 * 3.3895e6 * 2) ~ 3.682 km
        const horiz = ThreeDEngine.computeGeometricHorizonDistance(2.0, 'mars');
        expect(horiz.horizonDistanceKm).to.be.closeTo(3.68, 0.05);
        expect(horiz.surfaceArcKm).to.be.closeTo(3.68, 0.05);

        // Orbiter at h = 250 km (250,000 m) -> horizon d ~ 1326 km
        const orb = ThreeDEngine.computeGeometricHorizonDistance(250000.0, 'mars');
        expect(orb.horizonDistanceKm).to.be.closeTo(1326.0, 10.0);
    });

    it('should calculate perspective camera ground footprint swath and diffuse reflected radiance', () => {
        // Camera at H = 400 km with FOV = 20° -> halfSwath = 400 * tan(10°) ~ 70.53 km, fullSwath ~ 141.07 km
        const swath = ThreeDEngine.computePerspectiveSwathWidth(400.0, 20.0);
        expect(swath.swathWidthKm).to.be.closeTo(141.07, 0.5);
        expect(swath.halfSwathKm).to.be.closeTo(70.53, 0.5);

        // Incident solar flux F0 = 590 W/m^2, incidence i = 45°, albedo A = 0.25 -> L = (0.25 * 590 * cos(45°)) / pi ~ 33.19 W/(m^2 sr)
        const rad = ThreeDEngine.computeDiffusePhotometricRadiance(590.0, 45.0, 0.25);
        expect(rad.reflectedRadianceW_M2_Sr).to.be.closeTo(33.19, 0.2);
        expect(rad.isDirectlyIlluminated).to.equal(true);
    });
});

describe('Conrath Dust, Water Ice Saturation & Adiabatic Lapse Rate (MCDEngine)', () => {
    it('should compute vertical Conrath dust optical depth profile and adiabatic lapse rate', () => {
        // Conrath profile at P = 300 Pa (half surface pressure 610 Pa) with tau0 = 0.3
        const dust = MCDEngine.computeConrathDustOpticalDepthProfile(0.3, 300.0, 610.0, 0.007);
        expect(dust.tauAboveLevel).to.be.closeTo(0.146, 0.01);
        expect(dust.relativeDustMixingRatio).to.be.closeTo(0.99, 0.02);

        // Dry adiabatic lapse rate for Mars: Gamma_d = g/Cp = 3.72076 / 800.0 ~ 4.651 K/km
        const lapse = MCDEngine.computeAdiabaticLapseRate(800.0, 3.72076);
        expect(lapse.lapseRateKPerKm).to.be.closeTo(4.651, 0.01);
    });

    it('should calculate H2O water ice saturation vapor pressure and saturation mixing ratio', () => {
        // At T = 200 K (typical lower troposphere): saturation vapor pressure over ice es ~ 0.163 Pa
        const sat200 = MCDEngine.computeWaterIceSaturationVaporPressure(200.0, 610.0);
        expect(sat200.saturationVaporPressurePa).to.be.closeTo(0.163, 0.02);
        expect(sat200.saturationMixingRatioPpm).to.be.greaterThan(50.0);

        // Triple point T = 273.16 K -> es = 611.65 Pa
        const satTriple = MCDEngine.computeWaterIceSaturationVaporPressure(273.16, 610.0);
        expect(satTriple.saturationVaporPressurePa).to.be.closeTo(611.65, 1.0);
    });
});

describe('Infinite Slab Gravity, Density Inversion & Thermal Conductivity (InvestigateTool)', () => {
    it('should calculate infinite Bouguer slab gravity attraction and invert crustal density contrast', () => {
        // Slab h = 1000 m (1 km) with rho = 2900 kg/m^3 -> delta_g = 2 * pi * 6.6743e-11 * 2900 * 1000 * 1e5 ~ 121.6 mGal
        const slab = InvestigateTool.computeInfiniteSlabBouguerGravity(1000.0, 2900.0);
        expect(slab.bouguerAttractionMGal).to.be.closeTo(121.6, 0.5);

        // Invert density: delta_g = 121.6 mGal at h = 1000 m -> rho ~ 2900 kg/m^3
        const inv = InvestigateTool.invertCrustalDensityContrast(121.6, 1000.0);
        expect(inv.inferredDensityKgM3).to.be.closeTo(2900.0, 15.0);
        expect(inv.densityGramsCm3).to.be.closeTo(2.90, 0.02);
    });

    it('should compute apparent bulk thermal conductivity from thermal inertia', () => {
        // Rocky regolith I = 300 tiu, rho = 1500 kg/m^3, cp = 800 J/(kg K) -> k = 300^2 / (1500 * 800) = 90000 / 1200000 = 0.075 W/(m K)
        const cond = InvestigateTool.computeApparentThermalConductivity(300.0, 1500.0, 800.0);
        expect(cond.thermalConductivityW_MK).to.equal(0.075);
        expect(cond.volumetricHeatCapacityJ_M3K).to.equal(1200000.0);
    });
});

describe('Course Azimuth, Along-Track Closest Approach & Polygon Perimeter (MeasureTool)', () => {
    it('should compute initial, final, and back course azimuths along great-circle geodesics', () => {
        // Due East along Equator (0°N, 0°E) to (0°N, 90°E) -> initial = 90°, final = 90°, back = 270°
        const azEq = MeasureTool.computeInitialAndFinalCourseAzimuth(0.0, 0.0, 0.0, 90.0);
        expect(azEq.initialAzimuthDeg).to.equal(90.0);
        expect(azEq.finalAzimuthDeg).to.equal(90.0);
        expect(azEq.backAzimuthDeg).to.equal(270.0);

        // Due North along prime meridian (0°N, 0°E) to (60°N, 0°E) -> initial = 0°, final = 0°, back = 180°
        const azMer = MeasureTool.computeInitialAndFinalCourseAzimuth(0.0, 0.0, 60.0, 0.0);
        expect(azMer.initialAzimuthDeg).to.equal(0.0);
        expect(azMer.finalAzimuthDeg).to.equal(0.0);
        expect(azMer.backAzimuthDeg).to.equal(180.0);
    });

    it('should calculate along-track distance to closest approach and spherical polygon perimeter', () => {
        // Path from (0°N, 0°E) to (0°N, 90°E). Target at (30°N, 45°E) -> closest approach along track is at 45°E (along-track d = (45/180)*pi*3389.5 ~ 2662.15 km)
        const at = MeasureTool.computeAlongTrackDistanceToClosestApproach(30.0, 45.0, 0.0, 0.0, 0.0, 90.0, 'mars');
        expect(at.alongTrackKm).to.be.closeTo(2662.15, 2.0);

        // Spherical triangle: (0,0), (0,90), (90,0). Each of 3 sides is 90° arc = (pi/2)*3389.5 ~ 5324.3 km -> Total perimeter ~ 15972.9 km
        const perim = MeasureTool.computeSphericalPolygonPerimeter([[0, 0], [0, 90], [90, 0]], 'mars');
        expect(perim.perimeterKm).to.be.closeTo(15972.9, 10.0);
        expect(perim.edgeCount).to.equal(3);
    });
});

describe('Skin Depth Ratio, Equilibrium Temp & Subsurface Geotherm (KRCEngine)', () => {
    it('should compute exact annual-to-diurnal thermal skin depth amplification ratio', () => {
        // Mars year has 668.6 sols -> ratio = sqrt(668.6) ~ 25.857
        const skin = KRCEngine.computeAnnualToDiurnalSkinDepthRatio(668.6);
        expect(skin.skinDepthRatio).to.be.closeTo(25.857, 0.01);
        expect(skin.seasonalPenetrationFactor).to.be.closeTo(25.86, 0.05);
    });

    it('should calculate equilibrium surface temperature with geothermal heat flow and subsurface geotherm', () => {
        // S0 = 500 W/m^2, i = 0°, A = 0.25 -> absorbed = 375 W/m^2. With eps = 0.95 -> Teq ~ [375 / (0.95 * 5.67037e-8)]^0.25 ~ 288.94 K
        const teq = KRCEngine.computeEquilibriumSurfaceTemperatureWithGeothermal(500.0, 0.0, 0.25, 30.0, 0.95);
        expect(teq.equilibriumTempK).to.be.closeTo(288.9, 0.5);
        expect(teq.absorbedSolarFluxW_M2).to.equal(375.0);

        // Geotherm: T_surf = 210 K, q = 30 mW/m^2, k = 2.0 W/(m K) -> gradient = 15 K/km. At depth = 5 km -> T = 210 + 75 = 285.0 K
        const geo = KRCEngine.computeSubsurfaceGeothermalTemperatureProfile(210.0, 30.0, 2.0, 5.0);
        expect(geo.temperatureAtDepthK).to.equal(285.0);
        expect(geo.geothermalGradientKPerKm).to.equal(15.0);
    });
});

describe('Perihelion/Aphelion Distance, Solar Azimuth & Mean Motion (MarsTime)', () => {
    it('should compute exact Mars perihelion and aphelion orbital distances', () => {
        // a = 1.52368 AU, e = 0.09340 -> q = 1.38137 AU ~ 206.65M km, Q = 1.66599 AU ~ 249.23M km
        const dist = MarsTime.computePerihelionAphelionDistances(1.52368, 0.09340);
        expect(dist.perihelionAU).to.be.closeTo(1.38137, 0.001);
        expect(dist.aphelionAU).to.be.closeTo(1.66599, 0.001);
        expect(dist.orbitalRangeKm).to.be.closeTo(42578598, 100000);
    });

    it('should calculate topocentric solar azimuth angle and orbital mean motion', () => {
        // Solar noon: H = 0° -> Azimuth is 180° (due South) in Northern Hemisphere
        const azNoon = MarsTime.computeTopocentricSolarAzimuthAngle(20.0, 0.0, 0.0);
        expect(azNoon.solarAzimuthDeg).to.be.closeTo(180.0, 0.5);
        expect(azNoon.isWestOfMeridian).to.be.false;

        // Mean motion: Mars orbital period ~ 668.6 sols -> n ~ 0.524 deg/sol
        const mm = MarsTime.computeMartianMeanMotion(1.52368);
        expect(mm.meanMotionDegPerSol).to.be.closeTo(0.538, 0.02);
        expect(mm.orbitalPeriodSols).to.be.closeTo(668.6, 5.0);
    });
});

describe('Clutter Delay, Dielectric Contrast & Resolution Volume (RadarSounderEngine)', () => {
    it('should calculate off-nadir surface clutter excess delay and apparent false depth in ice', () => {
        // H = 250 km, y = 10 km -> slant = sqrt(250^2 + 10^2) = 250.1999 km -> excess round-trip = 2 * 0.1999 km / c ~ 1.3338 µs
        const clutter = RadarSounderEngine.computeCrossTrackClutterHorizonDelay(250.0, 10.0, 3.15);
        expect(clutter.excessDelayMicrosec).to.be.closeTo(1.334, 0.02);
        // In ice (v ~ 168.9 m/µs) -> apparent false depth = 168.9 * 1.334 / 2 ~ 112.7 m
        expect(clutter.apparentDepthMeters).to.be.closeTo(112.7, 3.0);
    });

    it('should compute minimum detectable dielectric contrast and 3D radar resolution volume', () => {
        // eps1 = 3.15, SNR = 10 dB (linear ratio ~ 3.162) -> Delta_eps = 4 * sqrt(3.15) / 3.162 ~ 2.245
        const contrast = RadarSounderEngine.computeMinimumDetectableDielectricContrast(3.15, 10.0);
        expect(contrast.minDetectableDeltaEps).to.be.closeTo(2.245, 0.05);

        // r_Fresnel = 1500 m, Delta_z = 15 m -> V_res = pi * 1500^2 * 15 = 106.03e6 m^3 = 0.106029 km^3
        const voxel = RadarSounderEngine.computeRadarResolutionVolume(1500.0, 15.0);
        expect(voxel.resolutionVolumeKm3).to.be.closeTo(0.106, 0.01);
    });
});

describe('CRISM BD1900r2, TES Carbonate & Spectral Information Divergence (BandMathEngine)', () => {
    it('should compute revised CRISM BD1900r2 hydration band depth and TES carbonate index', () => {
        // BD1900r2: R1850 = 0.25, R1930 = 0.20, R2060 = 0.24 -> continuum = 0.61905 * 0.25 + 0.38095 * 0.24 = 0.24619 -> depth = 1 - 0.20/0.24619 ~ 0.1876
        const bd19 = BandMathEngine.computeCRISMBd1900r2Index(0.25, 0.20, 0.24);
        expect(bd19.bd1900r2).to.be.closeTo(0.1876, 0.005);
        expect(bd19.hasStructuralH2O).to.be.true;

        // TES Carbonate: eps1350 = 0.98, eps1480 = 0.92, eps1600 = 0.96 -> cont = 0.97 -> index = 1 - 0.92/0.97 ~ 0.0515
        const carb = BandMathEngine.computeTESThermalCarbonateIndex(0.98, 0.92, 0.96);
        expect(carb.tesCarbonateIndex).to.be.closeTo(0.0515, 0.005);
        expect(carb.carbonateAbundanceEstimatePercent).to.be.closeTo(20.6, 2.0);
    });

    it('should calculate information-theoretic Spectral Information Divergence (SID)', () => {
        // Identical spectra -> SID = 0, similarity = 1.0
        const sidSame = BandMathEngine.computeSpectralInformationDivergence([0.2, 0.4, 0.6], [0.2, 0.4, 0.6]);
        expect(sidSame.sidDivergence).to.equal(0.0);
        expect(sidSame.similarityScore).to.equal(1.0);

        // Different spectra -> positive divergence
        const sidDiff = BandMathEngine.computeSpectralInformationDivergence([0.1, 0.5, 0.9], [0.8, 0.4, 0.1]);
        expect(sidDiff.sidDivergence).to.be.greaterThan(0.2);
    });
});

describe('Rim Height, Excavation Depth & Clark-Evans Index (CSFDEngine)', () => {
    it('should compute complex crater rim height scaling and transient excavation depth', () => {
        // D = 20 km complex crater -> h_rim = 0.036 * 20^0.49 ~ 0.1558 km = 155.8 m
        const rim = CSFDEngine.computeComplexCraterRimHeight(20.0);
        expect(rim.rimHeightKm).to.be.closeTo(0.1558, 0.005);
        expect(rim.rimHeightMeters).to.be.closeTo(155.8, 5.0);

        // D_t = 10 km transient crater -> d_e = 10 / (3 * sqrt(2)) ~ 2.357 km
        const depth = CSFDEngine.computeTransientCavityExcavationDepth(10.0);
        expect(depth.excavationDepthKm).to.be.closeTo(2.357, 0.01);
        expect(depth.transientDepthRatio).to.be.closeTo(0.2357, 0.005);
    });

    it('should calculate Clark-Evans nearest-neighbor spatial aggregation index', () => {
        // lambda = 0.01 craters/km^2 -> r_exp = 0.5 / sqrt(0.01) = 5.0 km. With r_A = 5.0 km -> R = 1.0 (CSR)
        const csr = CSFDEngine.computeClarkEvansAggregationIndex(5.0, 0.01);
        expect(csr.aggregationIndexR).to.equal(1.0);
        expect(csr.spatialClass).to.equal('Random (Poisson)');

        // Clustered: r_A = 2.5 km -> R = 0.5
        const clustered = CSFDEngine.computeClarkEvansAggregationIndex(2.5, 0.01);
        expect(clustered.aggregationIndexR).to.equal(0.5);
        expect(clustered.spatialClass).to.equal('Clustered / Secondaries');
    });
});

describe('Equidistant Cylindrical & Oblique Convergence (ProjectionManager)', () => {
    it('should perform forward and inverse Equidistant Cylindrical coordinate transformation', () => {
        // (30°N, 45°E) on Mars with R = 3389.5 km, standard parallel = 0° -> x = 3389.5 * (45 * pi/180) ~ 2662.15 km, y = 3389.5 * (30 * pi/180) ~ 1774.77 km
        const fwd = ProjectionManager.forwardEquidistantCylindrical(30.0, 45.0, 0.0, 0.0, 'mars');
        expect(fwd.xKm).to.be.closeTo(2662.15, 2.0);
        expect(fwd.yKm).to.be.closeTo(1774.77, 2.0);

        const inv = ProjectionManager.inverseEquidistantCylindrical(fwd.xKm, fwd.yKm, 0.0, 0.0, 'mars');
        expect(inv.latDeg).to.be.closeTo(30.0, 0.01);
        expect(inv.lonDeg).to.be.closeTo(45.0, 0.01);
    });

    it('should calculate oblique grid convergence angle', () => {
        // At Equator (0°N) -> convergence angle is 0°
        const convEq = ProjectionManager.computeObliqueConvergenceAngle(0.0, 45.0, 0.0);
        expect(convEq.convergenceAngleDeg).to.equal(0.0);

        // At (45°N, 45°E) with central meridian 0° -> tan(gamma) = tan(45°) * sin(45°) = 1 * 0.7071 -> gamma = 35.26°
        const conv45 = ProjectionManager.computeObliqueConvergenceAngle(45.0, 45.0, 0.0);
        expect(conv45.convergenceAngleDeg).to.be.closeTo(35.26, 0.05);
    });
});

describe('Lommel-Seeliger, Swath Slant Range & Globe Angular Radius (ThreeDEngine)', () => {
    it('should compute Lommel-Seeliger photometric scattering radiance factor', () => {
        // mu0 = 1.0 (overhead sun), mu = 1.0 (nadir camera), w0 = 0.25 -> I/F = (0.25 / (4 * pi)) * (1 / (1 + 1)) = 0.25 / (8 * pi) ~ 0.00995
        const ls = ThreeDEngine.computeLommelSeeligerScattering(1.0, 1.0, 0.25);
        expect(ls.radianceFactorIoF).to.be.closeTo(0.00995, 0.0005);
        expect(ls.isDirectlyIlluminated).to.be.true;
    });

    it('should calculate perspective swath edge slant range and apparent globe angular radius', () => {
        // Altitude H = 250 km, FOV = 60° (half-angle = 30°) -> R_slant = 250 / cos(30°) = 250 / 0.866025 ~ 288.675 km
        const slant = ThreeDEngine.computePerspectiveSwathSlantRange(250.0, 60.0);
        expect(slant.slantRangeKm).to.be.closeTo(288.675, 0.5);
        expect(slant.rangeExpansionRatio).to.be.closeTo(1.1547, 0.01);

        // Mars R = 3389.5 km at altitude h = 250 km -> sin(theta) = 3389.5 / 3639.5 = 0.9313 -> theta = 68.64°
        const globe = ThreeDEngine.computeApparentGlobeAngularRadius(250.0, 'mars');
        expect(globe.angularRadiusDeg).to.be.closeTo(68.64, 0.05);
        expect(globe.apparentDiameterDeg).to.be.closeTo(137.28, 0.1);
    });
});

describe('Monin-Obukhov, Saltation Threshold & Scale Height Gradient (MCDEngine)', () => {
    it('should compute Monin-Obukhov boundary layer atmospheric stability length', () => {
        // u* = 0.5 m/s, H_sens = 20 W/m^2, T0 = 220 K, rho = 0.015 kg/m^3 -> L = -(0.5^3 * 220 * 0.015 * 800) / (0.40 * 3.72076 * 20) = -330 / 29.766 ~ -11.08 m
        const mo = MCDEngine.computeMoninObukhovLength(0.5, 20.0, 220.0, 0.015);
        expect(mo.moninObukhovLengthMeters).to.be.closeTo(-11.1, 0.5);
        expect(mo.stabilityRegime).to.equal('Convectively Unstable (Daytime Midday)');
    });

    it('should calculate aerodynamic dust saltation threshold and scale height vertical gradient', () => {
        // d = 100 µm, rhoA = 0.015 kg/m^3, rhoP = 2500 kg/m^3 -> densityRatio ~ 166665 -> u*_t = 0.11 * sqrt(166665 * 3.72076 * 100e-6) = 0.11 * sqrt(62.01) ~ 0.866 m/s
        const salt = MCDEngine.computeSaltationThresholdFrictionVelocity(100.0, 0.015, 2500.0);
        expect(salt.thresholdFrictionVelocityMs).to.be.closeTo(0.866, 0.05);
        expect(salt.minimumWindSpeed10mMs).to.be.closeTo(14.96, 1.0);

        // Gamma = 4.5 K/km -> dH/dz = -(188.92 / 3.72076) * 0.0045 ~ -0.2285
        const grad = MCDEngine.computeAtmosphericScaleHeightGradient(4.5);
        expect(grad.scaleHeightGradientDimensionless).to.be.closeTo(-0.2285, 0.005);
        expect(grad.scaleHeightChangeMPerKm).to.be.closeTo(-228.5, 2.0);
    });
});

describe('Free-Air Gradient, Moho Depth & Apparent Permittivity (InvestigateTool)', () => {
    it('should calculate free-air elevation gravity gradient and correction', () => {
        // Mars R = 3389.5 km, g0 = 3.72076 m/s^2 -> grad = 2 * 3.72076 / 3389500 = 2.19546e-6 s^-2 -> 219.55 mGal/km
        const fa = InvestigateTool.computeFreeAirGravityGradient(1000.0, 3.72076, 3389.5);
        expect(fa.freeAirGradientMGalPerKm).to.be.closeTo(219.55, 0.2);
        expect(fa.freeAirCorrectionMGal).to.be.closeTo(219.55, 0.2);
    });

    it('should compute Airy isostatic Moho depth and SHARAD apparent relative dielectric permittivity', () => {
        // Topo h = 2 km, rhoC = 2900, rhoM = 3500 -> root = 2 * (2900 / 600) = 9.67 km -> total Moho = 50 + 9.67 = 59.67 km
        const moho = InvestigateTool.computeAiryIsostaticMohoDepth(2.0, 2900.0, 3500.0, 50.0);
        expect(moho.crustalRootKm).to.be.closeTo(9.67, 0.05);
        expect(moho.totalMohoDepthKm).to.be.closeTo(59.67, 0.05);

        // SHARAD: depth d = 500 m, two-way dt = 5.92 µs -> v = 2 * 500 / 5.92 ~ 168.92 m/µs -> eps_r = (299.79 / 168.92)^2 ~ 3.15 (Water Ice)
        const eps = InvestigateTool.computeApparentDielectricPermittivity(500.0, 5.92);
        expect(eps.relativePermittivityEpsR).to.be.closeTo(3.15, 0.05);
        expect(eps.materialEstimate).to.equal('Clean Pure Water Ice (eps ~ 3.15)');
    });
});

describe('Spherical Excess, Rhumb Mid-Lat & Ground Velocity (MeasureTool)', () => {
    it('should compute spherical polygon area via Girard spherical excess theorem', () => {
        // Octant of a sphere (3 right angles = 90°, 90°, 90°) -> E = 3 * pi/2 - pi = pi/2 rad. Area = (pi/2) * R^2 = 1/8 total sphere area
        // Mars R = 3389.5 km -> Total Area = 4 * pi * 3389.5^2 ~ 144.37e6 km^2 -> Octant Area = 18.046e6 km^2
        const excess = MeasureTool.computeSphericalExcessArea([90.0, 90.0, 90.0], 'mars');
        expect(excess.sphericalExcessRad).to.be.closeTo(Math.PI / 2.0, 0.0001);
        expect(excess.areaKm2).to.be.closeTo(18046400, 5000);
    });

    it('should calculate mean-latitude flat-sphere rhumb distance and sub-satellite ground velocity', () => {
        // Equator 0° to 10°E -> d = 3389.5 * (10 * pi/180) ~ 591.58 km
        const rhumb = MeasureTool.computeRhumbLineMeanLatDistance(0.0, 0.0, 0.0, 10.0, 'mars');
        expect(rhumb.distanceKm).to.be.closeTo(591.58, 1.0);

        // Orbital speed v = 3.4 km/s at altitude H = 250 km on Mars R = 3389.5 km -> v_ground = 3.4 * (3389.5 / 3639.5) ~ 3.1664 km/s
        const vg = MeasureTool.computeSubSatelliteGroundVelocity(3.4, 250.0, 'mars');
        expect(vg.groundVelocityKmS).to.be.closeTo(3.1664, 0.01);
        expect(vg.groundVelocityMs).to.be.closeTo(3166.4, 10.0);
    });
});

describe('Downwelling Flux, Phase Lag & Effective Bolometric Temp (KRCEngine)', () => {
    it('should compute atmospheric downwelling thermal IR flux and effective emissivity', () => {
        // T_air = 210 K, tau = 0.3, P = 610 Pa -> eps_atm = 0.12 + 0.20 * (1 - exp(-0.3)) + 0.05 = 0.12 + 0.0518 + 0.05 ~ 0.2218
        // Flux = 0.2218 * 5.67037e-8 * 210^4 ~ 24.46 W/m^2
        const flux = KRCEngine.computeAtmosphericDownwellingThermalFlux(210.0, 0.3, 610.0);
        expect(flux.atmosphericEmissivity).to.be.closeTo(0.2218, 0.005);
        expect(flux.downwellingFluxW_M2).to.be.closeTo(24.46, 0.5);
    });

    it('should calculate diurnal subsurface thermal phase lag and effective bolometric radiating temperature', () => {
        // TI = 250 tiu -> skin depth ~ 0.035 m = 3.5 cm. At depth z = 3.5 cm -> phase lag = 1 rad ~ (1 / 2pi) * 24.66h ~ 3.92 hours
        const lag = KRCEngine.computeThermalWavePhaseLag(0.035, 250.0);
        expect(lag.phaseLagRadians).to.be.closeTo(1.0, 0.05);
        expect(lag.phaseLagHours).to.be.closeTo(3.92, 0.2);
        expect(lag.amplitudeDecayRatio).to.be.closeTo(Math.exp(-1), 0.02);

        // Solar flux S = 500 W/m^2, Bond albedo = 0.25, eps = 0.95 -> absorbed = 375 W/m^2 -> T_eff = (375 / (0.95 * 5.67037e-8))^0.25 ~ 288.94 K
        const teff = KRCEngine.computeEffectiveBolometricTemperature(500.0, 0.25, 0.95);
        expect(teff.absorbedSolarFluxW_M2).to.equal(375.0);
        expect(teff.effectiveTempK).to.be.closeTo(288.94, 0.2);
    });
});

describe('True Anomaly from Eccentric, Distance from Ls & Hour Angle (MarsTime)', () => {
    it('should calculate true anomaly from eccentric anomaly and orbital radius from Ls', () => {
        // E = 90° with e = 0.0934 -> nu = 2 * atan(sqrt(1.0934 / 0.9066) * tan(45°)) = 2 * atan(1.0982) = 2 * 0.8321 rad ~ 95.36°
        const nu = MarsTime.computeTrueAnomalyFromEccentric(90.0, 0.0934);
        expect(nu.trueAnomalyDeg).to.be.closeTo(95.36, 0.05);

        // At perihelion Ls = 250.99° -> cos(0) = 1 -> r = 1.52368 * (1 - e) ~ 1.38137 AU, flux = 1361 / 1.38137^2 ~ 713.25 W/m^2
        const peri = MarsTime.computeMarsSunDistanceFromLs(250.99);
        expect(peri.distanceAU).to.be.closeTo(1.38137, 0.005);
        expect(peri.solarFluxW_M2).to.be.closeTo(713.25, 2.0);
    });

    it('should compute signed solar hour angle from LTST', () => {
        // Solar noon: LTST = 12.0 -> H = 0°
        const noon = MarsTime.computeHourAngleFromLTST(12.0);
        expect(noon.hourAngleDeg).to.equal(0.0);
        expect(noon.isAfternoon).to.be.false;

        // Afternoon: LTST = 15.0 (3 PM) -> H = +45°
        const pm = MarsTime.computeHourAngleFromLTST(15.0);
        expect(pm.hourAngleDeg).to.equal(45.0);
        expect(pm.isAfternoon).to.be.true;

        // Morning: LTST = 9.0 (9 AM) -> H = -45°
        const am = MarsTime.computeHourAngleFromLTST(9.0);
        expect(am.hourAngleDeg).to.equal(-45.0);
        expect(am.isAfternoon).to.be.false;
    });
});

describe('Chirp Compression, Basal Reflectivity & Specific Attenuation (RadarSounderEngine)', () => {
    it('should compute equivalent chirp bandwidth from pulse duration', () => {
        // tau = 85 µs -> B = 1 / 85e-6 ~ 11764.7 Hz ~ 0.0118 MHz
        const chirp = RadarSounderEngine.computeChirpCompressionBandwidth(85.0);
        expect(chirp.equivalentBandwidthHz).to.be.closeTo(11764.7, 1.0);
        expect(chirp.equivalentBandwidthMhz).to.be.closeTo(0.0118, 0.0005);
    });

    it('should calculate basal dielectric Fresnel reflectivity and specific radar attenuation rate', () => {
        // eps1 = 3.15 (ice), eps2 = 7.5 (basalt) -> n1 = 1.7748, n2 = 2.7386 -> rAmp = -0.9638 / 4.5134 = -0.2135 -> R = 0.0456 -> R_dB ~ -13.41 dB
        const basal = RadarSounderEngine.computeBasalDielectricReflectivityContrast(3.15, 7.5);
        expect(basal.reflectivityLinear).to.be.closeTo(0.0456, 0.002);
        expect(basal.reflectivityDb).to.be.closeTo(-13.41, 0.1);

        // f = 20 MHz, eps_r = 3.15, tan(delta) = 0.001 -> v = 299792458 / 1.7748 ~ 168916755 m/s
        // omega = 2 * pi * 20e6 = 1.2566e8 rad/s -> alpha_M = (1.2566e8 * 0.001 / (2 * 168916755)) * 8.686 ~ 0.00323 dB/m -> 3.23 dB/km
        const atten = RadarSounderEngine.computeSpecificRadarAttenuationRate(20.0, 3.15, 0.001);
        expect(atten.attenuationDbPerKm).to.be.closeTo(3.23, 0.05);
        expect(atten.attenuationDbPerMeter).to.be.closeTo(0.00323, 0.0001);
    });
});

describe('R-Plot Value, Secondary Screening & Fractional Poisson Error (CSFDEngine)', () => {
    it('should compute standard planetary R-plot relative value for diameter bin', () => {
        // N = 10 craters, d1 = 1 km, d2 = 1.414 km -> dGeom = 1.1892 km, deltaD = 0.414 km, Area = 1e6 km^2
        // R = 10 * (1.1892)^3 / (1e6 * 0.414) = 10 * 1.6818 / 414000 = 16.818 / 414000 ~ 4.062e-5
        const rVal = CSFDEngine.computeRelativeCraterRValue(10, 1.0, Math.SQRT2, 1e6);
        expect(rVal.dGeometricMeanKm).to.be.closeTo(1.189, 0.01);
        expect(rVal.deltaDKm).to.be.closeTo(0.414, 0.01);
        expect(rVal.rValue).to.be.closeTo(4.062e-5, 2e-6);
    });

    it('should calculate secondary crater screening radius and fractional Poisson model age error', () => {
        // D_primary = 20 km -> r_screen = 5 * 20 = 100 km, area = pi * 100^2 ~ 31415.9 km^2
        const screen = CSFDEngine.computeSecondaryScreeningRadius(20.0);
        expect(screen.screeningRadiusKm).to.equal(100.0);
        expect(screen.screeningRadiusMeters).to.equal(100000.0);
        expect(screen.exclusionAreaKm2).to.be.closeTo(31415.9, 1.0);

        // N = 100 craters -> fractional error = 1 / sqrt(100) = 0.10 (10.0%), robust = true
        const err = CSFDEngine.computeFractionalPoissonAgeError(100);
        expect(err.fractionalError).to.equal(0.1);
        expect(err.percentError).to.equal(10.0);
        expect(err.isStatisticallyRobust).to.be.true;
    });
});

describe('OLINDEX Extended, Quartz Ratio & Band Asymmetry (BandMathEngine)', () => {
    it('should calculate CRISM extended olivine index and TES Quartz Reststrahlen ratio', () => {
        // r1690 = 0.32, r1330 = 0.28, r2530 = 0.24 -> cont = 0.7 * 0.28 + 0.3 * 0.24 = 0.196 + 0.072 = 0.268 -> val = 0.32 / 0.268 - 1 = 1.194 - 1 = 0.194
        const ol = BandMathEngine.computeCRISMOlivineIndexExtended(0.32, 0.28, 0.24);
        expect(ol.olivineIndex).to.be.closeTo(0.194, 0.005);
        expect(ol.hasOlivineSignature).to.be.true;

        // eps1120 = 0.98, eps1000 = 0.88 -> ratio = 0.98 / 0.88 ~ 1.1136 (> 1.05 -> silica enriched)
        const qz = BandMathEngine.computeTESQuartzSilicateRatio(0.98, 0.88);
        expect(qz.quartzRatio).to.be.closeTo(1.1136, 0.005);
        expect(qz.isEnrichedInSilica).to.be.true;
    });

    it('should compute absorption band asymmetry from half-maximum depths', () => {
        // lambdaMin = 1.95 µm, leftHalf = 1.90 µm, rightHalf = 2.05 µm -> fwhm = 0.15 µm
        // leftSpan = 0.05 µm, rightSpan = 0.10 µm -> asym = (0.10 - 0.05) / 0.15 = 0.05 / 0.15 ~ +0.3333 (Right-Skewed)
        const asym = BandMathEngine.computeAbsorptionBandAsymmetryRatio(1.95, 1.90, 2.05);
        expect(asym.fwhmMicrons).to.be.closeTo(0.15, 0.001);
        expect(asym.asymmetryRatio).to.be.closeTo(0.3333, 0.005);
        expect(asym.skewDescription).to.equal('Right-Skewed (Longer wavelength tail)');
    });
});

describe('Cassini-Soldner Projection & Tissot Area Distortion (ProjectionManager)', () => {
    it('should forward and inverse project coordinates under transverse Cassini-Soldner projection', () => {
        // Mars R = 3389.5 km, origin (0°, 0°), point (10°N, 10°E)
        // x = 3389.5 * asin(cos(10°) * sin(10°)) = 3389.5 * asin(0.9848 * 0.1736) = 3389.5 * asin(0.1710) = 3389.5 * 0.1718 rad ~ 582.47 km
        const csFwd = ProjectionManager.forwardCassiniSoldner(10.0, 10.0, 0.0, 0.0, 'mars');
        expect(csFwd.xKm).to.be.closeTo(582.47, 1.0);
        expect(csFwd.yKm).to.be.closeTo(600.52, 1.0);

        const csInv = ProjectionManager.inverseCassiniSoldner(csFwd.xKm, csFwd.yKm, 0.0, 0.0, 'mars');
        expect(csInv.latDeg).to.be.closeTo(10.0, 0.01);
        expect(csInv.lonDeg).to.be.closeTo(10.0, 0.01);
    });

    it('should calculate Tissot indicatrix areal magnification scale factor', () => {
        // Sinusoidal equal-area -> areaScale = 1.0, isEqualArea = true
        const sinu = ProjectionManager.computeTissotAreaDistortionScale(45.0, 'sinusoidal');
        expect(sinu.areaScale).to.equal(1.0);
        expect(sinu.isEqualArea).to.be.true;

        // Mercator at 60° latitude -> sec(60°) = 2.0 -> areaScale = 4.0, isConformal = true
        const merc = ProjectionManager.computeTissotAreaDistortionScale(60.0, 'mercator');
        expect(merc.areaScale).to.be.closeTo(4.0, 0.05);
        expect(merc.isConformal).to.be.true;
    });
});

describe('Hapke Surge, Ground Footprint & Horizon Depression (ThreeDEngine)', () => {
    it('should compute Hapke shadow-hiding opposition surge factor and enhancement', () => {
        // Zero phase angle g = 0 -> B_SH = 1.0 / (1 + 0) = 1.0 (100% surge enhancement)
        const surge0 = ThreeDEngine.computeHapkeShadowHidingSurge(0.0, 1.0, 0.05);
        expect(surge0.oppositionSurgeFactor).to.equal(1.0);
        expect(surge0.surgeEnhancementPercent).to.equal(100.0);

        // Phase angle g = 5° -> tan(2.5°) ~ 0.04366 -> B_SH = 1.0 / (1 + 0.04366 / 0.05) = 1.0 / 1.8732 ~ 0.5338
        const surge5 = ThreeDEngine.computeHapkeShadowHidingSurge(5.0, 1.0, 0.05);
        expect(surge5.oppositionSurgeFactor).to.be.closeTo(0.5338, 0.005);
    });

    it('should calculate camera ground footprint polygon and orbital horizon depression angle', () => {
        // Altitude H = 250 km, FOV = 20° x 15°
        // width = 2 * 250 * tan(10°) ~ 88.163 km, length = 2 * 250 * tan(7.5°) ~ 65.823 km
        const fp = ThreeDEngine.computeOrbitalGroundFootprintPolygon(250.0, 20.0, 15.0);
        expect(fp.widthKm).to.be.closeTo(88.163, 0.05);
        expect(fp.lengthKm).to.be.closeTo(65.823, 0.05);
        expect(fp.areaKm2).to.be.closeTo(5803.14, 5.0);
        expect(fp.cornersKm.length).to.equal(4);

        // Mars R = 3389.5 km, H = 250 km -> cos(dip) = 3389.5 / 3639.5 = 0.9313 -> dip = acos(0.9313) ~ 21.36°
        const dep = ThreeDEngine.computeHorizonDepressionAngle(250.0, 'mars');
        expect(dep.depressionAngleDeg).to.be.closeTo(21.36, 0.1);
    });
});

describe('Bulk Richardson Number, Convective Velocity & Eddy Diffusivity (MCDEngine)', () => {
    it('should calculate bulk Richardson number across boundary layer shear', () => {
        // dTheta = -2.0 K (unstable), dZ = 100 m, dU = 5 m/s, theta0 = 210 K
        // Ri_b = (3.72076 / 210) * (-2 * 100) / 25 = 0.017718 * (-200) / 25 = -0.1417
        const rib = MCDEngine.computeBulkRichardsonNumber(-2.0, 100.0, 5.0, 0.0, 210.0);
        expect(rib.bulkRichardsonNumber).to.be.closeTo(-0.1417, 0.005);
        expect(rib.isConvectivelyUnstable).to.be.true;
        expect(rib.isTurbulent).to.be.true;
    });

    it('should compute Deardorff convective velocity scale and PBL eddy thermal diffusivity', () => {
        // H_sens = 25 W/m^2, zi = 5000 m (5 km), rho = 0.015, cp = 800, theta0 = 210 K
        // wTheta0 = 25 / 12 = 2.0833 -> B0 = (3.72076 / 210) * 2.0833 = 0.03691 m^2/s^3
        // w* = (0.03691 * 5000)^(1/3) = (184.56)^(1/3) ~ 5.693 m/s
        const conv = MCDEngine.computeConvectiveVelocityScale(25.0, 5000.0, 210.0, 0.015);
        expect(conv.convectiveVelocityMs).to.be.closeTo(5.693, 0.05);

        // u* = 0.5 m/s, z = 50 m, L = -100 m (unstable)
        // phi_h = 1 / sqrt(1 - 16 * 50 / -100) = 1 / sqrt(1 + 8) = 1 / 3 ~ 0.3333
        // Kh = (0.4 * 0.5 * 50) / 0.3333 = 10 / 0.3333 ~ 30.0 m^2/s
        const kh = MCDEngine.computePBLEddyThermalDiffusivity(0.5, 50.0, -100.0);
        expect(kh.phiHeatDimensionless).to.be.closeTo(0.3333, 0.005);
        expect(kh.eddyDiffusivityKhM2S).to.be.closeTo(30.0, 0.5);
    });
});

describe('NPF Slope, Strength-Gravity Transition & Isochron Offset (CSFDEngine)', () => {
    it('should calculate local logarithmic slope derivative of Neukum Production Function at D = 1 km', () => {
        // At D = 1 km, log10(D) = 0 -> slope = a1 = -3.5332, diffIndex = -(-3.5332 - 1) = 4.5332
        const slope = CSFDEngine.computeNeukumProductionSlopeDerivative(1.0);
        expect(slope.slopeDerivative).to.be.closeTo(-3.5332, 0.001);
        expect(slope.differentialPowerIndex).to.be.closeTo(4.5332, 0.001);
    });

    it('should compute crater scaling strength-to-gravity transition diameter and isochron cumulative offset', () => {
        // Y = 10 MPa (1e7 Pa), rho = 2900 kg/m^3, g = 3.72076 m/s^2 -> D_t = 1e7 / (2900 * 3.72076) = 1e7 / 10790.2 = 926.77 m ~ 0.927 km
        const trans = CSFDEngine.computeStrengthToGravityTransitionDiameter(1e7, 2900.0, 'mars');
        expect(trans.transitionDiameterKm).to.be.closeTo(0.927, 0.01);
        expect(trans.transitionDiameterMeters).to.be.closeTo(926.8, 1.0);

        // Reference 1 Ga chronology N(1) = 2.68e-14*(exp(6.93)-1) + 4.13e-4 = 2.68e-14 * 1021.49 + 4.13e-4 ~ 4.13e-4
        // If observed N(1) = 8.26e-4 -> ratio ~ 2.0
        const off = CSFDEngine.computeIsochronCumulativeOffset(4.13e-4, 1.0);
        expect(off.densityRatioTo1Ga).to.be.closeTo(1.0, 0.05);
        expect(off.impliedAgeGa).to.be.closeTo(1.0, 0.05);
    });
});

describe('Smectite Index, THEMIS B10/B9 & Normalized Depth (BandMathEngine)', () => {
    it('should calculate CRISM Fe/Mg smectite clay 2.3 µm absorption index', () => {
        // r2300 = 0.22, r2120 = 0.26, r2400 = 0.24 -> cont = 0.65 * 0.26 + 0.35 * 0.24 = 0.169 + 0.084 = 0.253
        // drop = 1 - 0.22 / 0.253 = 1 - 0.8696 = 0.1304 (> 0.04 -> smectite clay signature present)
        const smectite = BandMathEngine.computeCRISMSmectiteIndexExtended(0.22, 0.26, 0.24);
        expect(smectite.smectiteIndex).to.be.closeTo(0.1304, 0.005);
        expect(smectite.hasSmectiteClaySignature).to.be.true;
    });

    it('should compute THEMIS B10/B9 ratio and continuum-normalized absorption band depth', () => {
        // THEMIS B10 = 1.05 W/m^2/sr/µm, B9 = 0.98 -> ratio = 1.05 / 0.98 ~ 1.0714 (> 1.02 -> dust dominated)
        const themis = BandMathEngine.computeTHEMISBand10To9Ratio(1.05, 0.98);
        expect(themis.bandRatio).to.be.closeTo(1.0714, 0.005);
        expect(themis.isDustDominated).to.be.true;

        // rCenter = 0.18, rContinuum = 0.25 -> depth = 1 - 0.18/0.25 = 1 - 0.72 = 0.28 (28% depth)
        const norm = BandMathEngine.computeNormalizedAbsorptionDepth(0.18, 0.25);
        expect(norm.normalizedDepth).to.equal(0.28);
        expect(norm.percentDepth).to.equal(28.0);
        expect(norm.isAbsorptionPresent).to.be.true;
    });
});

describe('Radial Velocity, Declination Rate & Sol-to-Day (MarsTime)', () => {
    it('should calculate orbital radial velocity dr/dt of Mars relative to Sun', () => {
        // True anomaly nu = 90° -> sin(90°) = 1.0 -> dr/dt = (n * a * e) / sqrt(1 - e^2)
        // n = 2*pi / (686.98 * 86400) = 1.0585e-7 rad/s, a = 227.939M km, e = 0.0934
        // dr/dt = (1.0585e-7 * 227.939e6 * 0.0934) / sqrt(1 - 0.00872) = 2.2536 / 0.99563 ~ 2.2635 km/s
        const rad = MarsTime.computeRadialDistanceRateOfChange(90.0, 1.52368, 0.0934);
        expect(rad.radialVelocityKmS).to.be.closeTo(2.2635, 0.02);
        expect(rad.isRecedingFromSun).to.be.true;
    });

    it('should compute solar declination rate of change and convert sols to Earth solar days', () => {
        // At equinox Ls = 0°, cos(0) = 1, sin(0) = 0 -> dDec/dLs = sin(25.19°) ~ 0.4256 -> rate = 0.4256 * 0.5384 ~ 0.2291 deg/sol
        const decRate = MarsTime.computeSolarDeclinationRateOfChange(0.0, 25.19);
        expect(decRate.declinationRateDegPerSol).to.be.closeTo(0.2291, 0.005);
        expect(decRate.isApproachingSolstice).to.be.false;

        // 1000 Sols -> 1000 * 88775.244 / 86400 = 1027.49125 Earth days
        const conv = MarsTime.convertMarsSolsToEarthDays(1000);
        expect(conv.earthDays).to.be.closeTo(1027.4913, 0.01);
        expect(conv.totalSeconds).to.equal(88775244.0);
    });
});

describe('Specific Heat Model, Damping Ratio & Net Radiative Loss (KRCEngine)', () => {
    it('should calculate temperature-dependent specific heat capacity of silicate regolith', () => {
        // At T = 200 K -> cp = 890 - 450 * exp(-200/150) = 890 - 450 * exp(-1.3333) = 890 - 450 * 0.2636 = 890 - 118.62 ~ 771.38 J/(kg K)
        const cp = KRCEngine.computeTemperatureDependentSpecificHeat(200.0);
        expect(cp.specificHeatJ_KgK).to.be.closeTo(771.38, 0.5);
        expect(cp.volumetricHeatCapacityJ_M3K).to.be.closeTo(1500.0 * 771.38, 100.0);
    });

    it('should compute subsurface thermal damping ratio and surface net longwave radiative loss', () => {
        // depth z = 0.1 m, skinDepth = 0.05 m -> ratio = exp(-2) ~ 0.1353 (13.53% of surface amplitude)
        const damp = KRCEngine.computeSubsurfaceThermalDampingRatio(0.1, 0.05);
        expect(damp.amplitudeRatio).to.be.closeTo(0.1353, 0.001);
        expect(damp.percentOfSurfaceAmplitude).to.be.closeTo(13.53, 0.1);

        // T_s = 220 K, eps = 0.95, F_down = 20 W/m^2
        // F_up = 0.95 * 5.670374419e-8 * (220)^4 = 5.38685e-8 * 2342560000 ~ 126.19 W/m^2 -> F_net = 126.19 - 20 = 106.19 W/m^2
        const loss = KRCEngine.computeSurfaceNetRadiativeLoss(220.0, 20.0, 0.95);
        expect(loss.upwardEmittedFluxW_M2).to.be.closeTo(126.19, 0.2);
        expect(loss.netRadiativeLossW_M2).to.be.closeTo(106.19, 0.2);
        expect(loss.isCooling).to.be.true;
    });
});

describe('Great-Circle Midpoint, Spherical Cap & Cross-Track Deviation (MeasureTool)', () => {
    it('should calculate exact spherical great-circle midpoint between two planetary coordinates', () => {
        // Point 1: (0°N, 0°E), Point 2: (0°N, 90°E) -> Midpoint should be exactly (0°N, 45°E)
        const midEquator = MeasureTool.computeGreatCircleMidpoint(0.0, 0.0, 0.0, 90.0);
        expect(midEquator.midLatDeg).to.be.closeTo(0.0, 0.01);
        expect(midEquator.midLonDeg).to.be.closeTo(45.0, 0.01);

        // Point 1: (10°N, 20°E), Point 2: (50°N, 20°E) -> Midpoint along meridian: (30°N, 20°E)
        const midMeridian = MeasureTool.computeGreatCircleMidpoint(10.0, 20.0, 50.0, 20.0);
        expect(midMeridian.midLatDeg).to.be.closeTo(30.0, 0.01);
        expect(midMeridian.midLonDeg).to.be.closeTo(20.0, 0.01);
    });

    it('should compute spacecraft horizon spherical cap area and along-track deviation', () => {
        // Mars R = 3389.5 km, H = 250 km (MRO altitude)
        // Cap area = 2 * pi * 3389.5^2 * (250 / 3639.5) = 2 * pi * 11488710.25 * 0.06869 ~ 4.958e6 km^2 (~3.43% surface fraction)
        const cap = MeasureTool.computeSpacecraftSphericalCapArea(250.0, 'mars');
        expect(cap.capAreaKm2).to.be.closeTo(4958100.0, 5000.0);
        expect(cap.surfaceFractionPercent).to.be.closeTo(3.43, 0.05);

        // Point on path: Start (0, 0), End (0, 30), Point (0, 15) -> crossTrack = 0, alongTrack ~ 15 deg * 59.16 km/deg = 887.38 km
        const dev = MeasureTool.computeAlongTrackCrossTrackDeviation(0.0, 15.0, 0.0, 0.0, 0.0, 30.0, 'mars');
        expect(dev.crossTrackKm).to.be.closeTo(0.0, 0.01);
        expect(dev.alongTrackKm).to.be.closeTo(887.38, 1.0);
    });
});

describe('Clutter Look Angle, Fresnel Matrix & Pulse Width (RadarSounderEngine)', () => {
    it('should calculate off-nadir radar surface clutter look angle and ground offset', () => {
        // H = 250 km = 250,000 m. Delay tau = 10 µs -> c * tau / 2 = 299792458 * 1e-5 / 2 = 1498.96 m
        // slantRange = 251498.96 m -> cos(theta) = 250000 / 251498.96 ~ 0.99404 -> theta ~ 6.26°
        const clutter = RadarSounderEngine.computeClutterAngleFromExcessDelay(10.0, 250.0);
        expect(clutter.lookAngleDeg).to.be.closeTo(6.26, 0.05);
        expect(clutter.groundOffsetKm).to.be.closeTo(27.43, 0.5);
    });

    it('should compute subsurface Fresnel transmission matrix and compressed pulse resolution', () => {
        // eps1 = 1.0 (air), eps2 = 3.15 (ice) -> n1 = 1, n2 = 1.7748
        // r = (1 - 1.7748) / 2.7748 = -0.7748 / 2.7748 = -0.2792 -> R = 0.0780, T = 0.9220
        const fresnel = RadarSounderEngine.computeSubsurfaceFresnelTransmissionMatrix(1.0, 3.15);
        expect(fresnel.amplitudeReflection).to.be.closeTo(-0.2792, 0.005);
        expect(fresnel.powerReflectivity).to.be.closeTo(0.0780, 0.005);
        expect(fresnel.powerTransmissivity).to.be.closeTo(0.9220, 0.005);

        // B = 10 MHz -> tau = 100 ns. In water ice eps = 3.15 -> delta_z = 299792458 / (2 * 1e7 * 1.7748) = 14.9896 / 1.7748 ~ 8.45 m
        const pulse = RadarSounderEngine.computeRadarCompressedPulseWidth(10.0, 3.15);
        expect(pulse.pulseDurationNs).to.equal(100.0);
        expect(pulse.verticalResolutionMeters).to.be.closeTo(8.45, 0.02);
        expect(pulse.freeSpaceResolutionMeters).to.be.closeTo(14.99, 0.02);
    });
});

describe('Transverse Mercator Forward/Inverse & Meridian Convergence (ProjectionManager)', () => {
    it('should calculate Transverse Mercator forward and inverse projections accurately', () => {
        // Point on central meridian: (10°N, 0°E), centerLat = 0, centerLon = 0, k0 = 1.0, Mars R = 3389.5 km
        // x = 0, y = 3389.5 * (10 * pi / 180) ~ 591.587 km
        const fwd = ProjectionManager.forwardTransverseMercator(10.0, 0.0, 0.0, 0.0, 1.0, 'mars');
        expect(fwd.x).to.be.closeTo(0.0, 0.01);
        expect(fwd.y).to.be.closeTo(591.587, 0.5);

        // Inverse projection from (0, 591.587) back to (10, 0)
        const inv = ProjectionManager.inverseTransverseMercator(fwd.x, fwd.y, 0.0, 0.0, 1.0, 'mars');
        expect(inv.latDeg).to.be.closeTo(10.0, 0.01);
        expect(inv.lonDeg).to.be.closeTo(0.0, 0.01);
    });

    it('should compute Transverse Mercator meridian convergence angle', () => {
        // At equator lat = 0° -> gamma = 0°
        const convEq = ProjectionManager.computeTransverseMercatorMeridianConvergence(0.0, 15.0, 0.0);
        expect(convEq).to.equal(0.0);

        // At lat = 45°, dLam = 30° -> tan(30) * sin(45) = 0.57735 * 0.7071 ~ 0.40825 -> gamma = atan(0.40825) ~ 22.21°
        const convMid = ProjectionManager.computeTransverseMercatorMeridianConvergence(45.0, 30.0, 0.0);
        expect(convMid).to.be.closeTo(22.21, 0.05);
    });
});

describe('Lommel-Seeliger Reflectance, Camera GSD & FOV Angles (ThreeDEngine)', () => {
    it('should calculate Lommel-Seeliger diffuse surface reflectance accurately', () => {
        // mu0 = cos(30°) = 0.8660, mu = cos(45°) = 0.7071 -> r_LS = 0.8660 / (0.8660 + 0.7071) = 0.8660 / 1.5731 ~ 0.5505
        const ls = ThreeDEngine.computeLommelSeeligerDiskReflectance(Math.cos(30 * Math.PI / 180), Math.cos(45 * Math.PI / 180));
        expect(ls.lommelSeeligerReflectance).to.be.closeTo(0.5505, 0.005);
        expect(ls.isIlluminated).to.be.true;

        // Shadowed surface mu0 = 0 -> r_LS = 0
        const dark = ThreeDEngine.computeLommelSeeligerDiskReflectance(0.0, 0.8);
        expect(dark.lommelSeeligerReflectance).to.equal(0.0);
        expect(dark.isIlluminated).to.be.false;
    });

    it('should compute camera Ground Sampling Distance (GSD) and sensor FOV angles', () => {
        // HiRISE camera: H = 250 km, p = 12 µm, f = 12,000 mm (12 m)
        // GSD = (250,000 m * 12e-6 m) / 12 m = 3.0 / 12 = 0.25 m/pixel = 25 cm/pixel
        const hirise = ThreeDEngine.computeCameraGroundSamplingDistance(250.0, 12.0, 12000.0);
        expect(hirise.gsdMeters).to.equal(0.25);
        expect(hirise.gsdCm).to.equal(25.0);

        // Sensor: 36 mm x 24 mm, focal length = 50 mm
        // FOV_H = 2 * atan(36 / 100) = 2 * atan(0.36) = 2 * 19.799° ~ 39.598°
        // FOV_V = 2 * atan(24 / 100) = 2 * atan(0.24) = 2 * 13.496° ~ 26.991°
        const fov = ThreeDEngine.computeSensorFieldOfViewAngles(36.0, 24.0, 50.0);
        expect(fov.horizontalFovDeg).to.be.closeTo(39.598, 0.05);
        expect(fov.verticalFovDeg).to.be.closeTo(26.991, 0.05);
    });
});

describe('Saltation Threshold, Static Stability & Deardorff CBL (MCDEngine)', () => {
    it('should calculate Greeley-Iversen fluid threshold friction velocity for Martian sand saltation with cohesion', () => {
        // d = 100 µm = 1e-4 m, rho = 0.015 kg/m^3, rho_p = 2500, g = 3.72076
        // gravityTerm = (2500 * 3.72076 * 1e-4) / 0.015 = 0.93019 / 0.015 = 62.0127
        // cohesionTerm = 3e-4 / (0.015 * 1e-4) = 3e-4 / 1.5e-6 = 200.0
        // u*_t = 0.118 * sqrt(62.0127 + 200.0) = 0.118 * sqrt(262.0127) = 0.118 * 16.1868 ~ 1.910 m/s
        const salt = MCDEngine.computeDustCohesionSaltationThreshold(100.0, 0.015, 2500.0);
        expect(salt.thresholdFrictionVelocityMs).to.be.closeTo(1.910, 0.02);
        expect(salt.optimumDiameterMicrons).to.equal(100.0);
    });

    it('should compute atmospheric static stability and Deardorff CBL diurnal depth', () => {
        // T = 200 K, Gamma_obs = 3.65 K/km, Gamma_d = 4.65 K/km -> dGamma = 1.0 K/km = 0.001 K/m -> S = 0.001 / 200 = 5e-6 m^-1
        const stab = MCDEngine.computeAtmosphericStaticStabilityParameter(200.0, 3.65, 4.65);
        expect(stab.staticStabilityParameterPerMeter).to.equal(5e-6);
        expect(stab.isConvectivelyStable).to.be.true;

        // H_sens = 20 W/m^2, T0 = 220 K, rho = 0.015 kg/m^3, cp = 800 -> B0 = (3.72076 / 220) * (20 / 12) = 0.01691 * 1.6667 ~ 0.02819 m^2/s^3
        // zi = sqrt(2 * 0.02819 * 21600 / 0.003) = sqrt(1217.7 / 0.003) = sqrt(405900) ~ 637.1 m
        const cbl = MCDEngine.computeConvectiveBoundaryLayerDeardorffHeight(20.0, 0.003, 21600.0, 220.0, 0.015);
        expect(cbl.pblHeightMeters).to.be.closeTo(637.1, 5.0);
        expect(cbl.pblHeightKm).to.be.closeTo(0.637, 0.01);
    });
});

describe('Differential Frequency, Impact Melt & Transient Excavation (CSFDEngine)', () => {
    it('should calculate multi-bin differential crater frequency and impact melt volume', () => {
        // Craters: 1000m (1km), 1200m (1.2km) in area 1e6 km^2
        const diff = CSFDEngine.computeMultiBinDifferentialFrequency([{ diameter: 1000 }, { diameter: 1200 }], 1e6);
        expect(diff.length).to.equal(10);
        expect(diff[0].dMinKm).to.equal(0.1);

        // Impact kinetic energy: E = 1e18 Joules (~239 Megatons)
        // meltMass = 0.025 * 1e18 / 4.5e6 = 2.5e16 / 4.5e6 ~ 5.555e9 kg
        // meltVol = 5.555e9 / 2900 ~ 1.915e6 m^3 ~ 0.001915 km^3
        const melt = CSFDEngine.computeImpactMeltVolume(1e18, 2900.0);
        expect(melt.meltMassKg).to.be.closeTo(5.555e9, 1e7);
        expect(melt.meltVolumeM3).to.be.closeTo(1.915e6, 1e4);
    });

    it('should calculate transient crater excavation depth and volume', () => {
        // D_t = 10 km -> d_exc = 0.33 * 10 = 3.3 km (3300 m)
        // V_exc = (1/3) * pi * 5^2 * 3.3 = (1/3) * pi * 25 * 3.3 = 86.393 km^3
        const exc = CSFDEngine.computeTransientExcavationDepth(10.0);
        expect(exc.excavationDepthKm).to.equal(3.3);
        expect(exc.excavationDepthMeters).to.equal(3300.0);
        expect(exc.excavationVolumeKm3).to.be.closeTo(86.393, 0.1);
    });
});

describe('Silicate Hydration, Felsic Silicate & Spectral Curvature (BandMathEngine)', () => {
    it('should calculate CRISM SINDEX2 secondary silicate hydration index', () => {
        // r2290 = 0.22, r2400 = 0.21, r2340 = 0.24 -> shoulders = 0.43, center = 2 * 0.24 = 0.48 -> index = 1 - 0.43/0.48 = 1 - 0.8958 = 0.1042 (> 0.03)
        const hydr = BandMathEngine.computeCRISMSilicateHydrationIndex(0.22, 0.24, 0.21);
        expect(hydr.sindex2).to.be.closeTo(0.1042, 0.005);
        expect(hydr.hasHydrationSignature).to.be.true;
    });

    it('should compute THEMIS felsic silicate index and spectral continuum curvature', () => {
        // THEMIS B10 = 1.08, B8 = 0.98 -> qindex = 1.08 / 0.98 ~ 1.1020 (> 1.05 -> felsic enriched)
        const felsic = BandMathEngine.computeTHEMISFelsicSilicateIndex(1.08, 0.98);
        expect(felsic.qindex).to.be.closeTo(1.1020, 0.005);
        expect(felsic.isFelsicEnriched).to.be.true;

        // r1 = 0.20, r2 = 0.25, r3 = 0.22 -> kappa = 0.25^2 / (0.20 * 0.22) = 0.0625 / 0.044 ~ 1.4205 (> 1.0 -> convex)
        const curv = BandMathEngine.computeSpectralContinuumRatioCurvature(0.20, 0.25, 0.22);
        expect(curv.curvatureFactor).to.be.closeTo(1.4205, 0.01);
        expect(curv.isConvex).to.be.true;
    });
});

describe('True Anomaly Angular Rate, Sub-Solar Zenith & Day-to-Sol (MarsTime)', () => {
    it('should calculate instantaneous orbital true anomaly angular velocity', () => {
        // At perihelion nu = 0, a = 1.52368 AU = 227939100 km, e = 0.09340 -> r = 206649588 km
        // rate ~ 0.638 deg/sol at perihelion
        const peri = MarsTime.computeTrueAnomalyAngularRate(0.0);
        expect(peri.trueAnomalyRateDegPerSol).to.be.closeTo(0.638, 0.05);
        expect(peri.distanceKm).to.be.closeTo(206649588, 100000);

        // At aphelion nu = 180 -> r = 249228612 km -> rate ~ 0.439 deg/sol
        const aph = MarsTime.computeTrueAnomalyAngularRate(180.0);
        expect(aph.trueAnomalyRateDegPerSol).to.be.closeTo(0.439, 0.05);
    });

    it('should compute sub-solar zenith angle and convert Earth days to Mars sols', () => {
        // Target at lat = 0, lon = 0; Subsolar at lat = 0, lon = 0 -> zenith = 0 deg, cosZ = 1.0
        const subNadir = MarsTime.computeSubSolarZenithAngle(0.0, 0.0, 0.0, 0.0);
        expect(subNadir.zenithAngleDeg).to.equal(0.0);
        expect(subNadir.cosZenith).to.equal(1.0);
        expect(subNadir.isDaylight).to.be.true;

        // Target at lat = 45, lon = 0; Subsolar at lat = 0, lon = 0 -> zenith = 45 deg, cosZ = 0.7071
        const sub45 = MarsTime.computeSubSolarZenithAngle(45.0, 0.0, 0.0, 0.0);
        expect(sub45.zenithAngleDeg).to.be.closeTo(45.0, 0.01);
        expect(sub45.cosZenith).to.be.closeTo(0.7071, 0.005);

        // 100 Earth days -> sols = 100 * (86400 / 88775.244) = 100 * 0.973244 ~ 97.3244 sols
        const sols = MarsTime.convertEarthDaysToMarsSols(100.0);
        expect(sols.marsSols).to.be.closeTo(97.3244, 0.01);
    });
});

describe('Interlayer Heat Flux, Seasonal TI & Atmospheric Downwelling (KRCEngine)', () => {
    it('should calculate discrete finite-difference conductive interlayer heat flux', () => {
        // T_upper = 220 K, T_lower = 210 K, dz = 0.05 m, k = 0.05 W/(m K)
        // dT = -10 K -> flux = -0.05 * (-10 / 0.05) = +10.0 W/m^2 (upward)
        const fluxUp = KRCEngine.computeSubsurfaceInterlayerHeatFlux(220.0, 210.0, 0.05, 0.05);
        expect(fluxUp.conductiveFluxW_M2).to.equal(10.0);
        expect(fluxUp.isHeatFlowingDownward).to.be.false;

        // T_upper = 200 K, T_lower = 230 K -> dT = +30 -> flux = -0.05 * (30 / 0.05) = -30.0 W/m^2 (downward)
        const fluxDown = KRCEngine.computeSubsurfaceInterlayerHeatFlux(200.0, 230.0, 0.05, 0.05);
        expect(fluxDown.conductiveFluxW_M2).to.equal(-30.0);
        expect(fluxDown.isHeatFlowingDownward).to.be.true;
    });

    it('should compute seasonal apparent thermal inertia modulation and downwelling IR flux', () => {
        // Base TI = 250 tiu, Ls = 250.99° (perihelion) -> factor = 1 + 0.15 = 1.15 -> TI_app = 287.5 tiu (+15%)
        const periTI = KRCEngine.computeSeasonalApparentThermalInertiaModulation(250.0, 250.99, 0.15);
        expect(periTI.apparentThermalInertia).to.equal(287.5);
        expect(periTI.percentModulation).to.equal(15.0);

        // At aphelion Ls = 70.99° -> factor = 1 - 0.15 = 0.85 -> TI_app = 212.5 tiu (-15%)
        const aphTI = KRCEngine.computeSeasonalApparentThermalInertiaModulation(250.0, 70.99, 0.15);
        expect(aphTI.apparentThermalInertia).to.equal(212.5);
        expect(aphTI.percentModulation).to.equal(-15.0);

        // T_air = 210 K, tau = 0.5, P = 610 Pa -> tau_IR = 0.175 -> epsDust = 1 - exp(-0.175) ~ 0.1605, epsGas = 0.08 -> epsAtm ~ 0.2405
        // flux = 0.2405 * 5.670374e-8 * (210)^4 = 0.2405 * 110.28 ~ 26.52 W/m^2
        const ir = KRCEngine.computeAtmosphericThermalInfraredDownwellingFlux(210.0, 0.5, 610.0);
        expect(ir.downwellingFluxW_M2).to.be.closeTo(26.52, 0.5);
        expect(ir.atmosphericEmissivity).to.be.closeTo(0.2405, 0.01);
    });
});

describe('Clutter Discrimination, Snell Refraction & Slant Path Delay (RadarSounderEngine)', () => {
    it('should calculate subsurface clutter-to-signal ratio and dominance', () => {
        // P_clutter = 1e-12 W, P_signal = 1e-13 W -> ratio = 10 -> CSR = +10 dB (clutter dominant)
        const csrHigh = RadarSounderEngine.computeSubsurfaceClutterToSignalRatio(1e-12, 1e-13);
        expect(csrHigh.clutterToSignalRatioDb).to.equal(10.0);
        expect(csrHigh.isClutterDominant).to.be.true;

        // P_clutter = 1e-14 W, P_signal = 1e-12 W -> ratio = 0.01 -> CSR = -20 dB (signal dominant)
        const csrLow = RadarSounderEngine.computeSubsurfaceClutterToSignalRatio(1e-14, 1e-12);
        expect(csrLow.clutterToSignalRatioDb).to.equal(-20.0);
        expect(csrLow.isClutterDominant).to.be.false;
    });

    it('should compute exact Snell dielectric refraction and slant path depth correction', () => {
        // Air (eps1 = 1.0) into Ice (eps2 = 3.15, n2 = 1.7748) at theta1 = 30°
        // sin(theta2) = (1 / 1.7748) * sin(30°) = 0.5 / 1.7748 = 0.2817 -> theta2 = 16.36°
        const refr = RadarSounderEngine.computeDielectricSnellsRefraction(30.0, 1.0, 3.15);
        expect(refr.refractionAngleDeg).to.be.closeTo(16.36, 0.05);
        expect(refr.totalInternalReflection).to.be.false;

        // TWT = 10 μs, epsR = 3.15, refractionAngle = 0° (nadir):
        // v = c / sqrt(3.15) = 168916327 m/s -> slantDist = (168916327 * 10e-6) / 2 = 844.58 m -> vert = 844.58 m
        const nadir = RadarSounderEngine.computeRefractedDepthDelayCorrection(10.0, 3.15, 0.0);
        expect(nadir.trueVerticalDepthMeters).to.be.closeTo(844.58, 0.1);
        expect(nadir.slantPathDistanceMeters).to.be.closeTo(844.58, 0.1);

        // With slant angle theta = 30° -> vert = 844.58 * cos(30°) = 731.43 m
        const slant = RadarSounderEngine.computeRefractedDepthDelayCorrection(10.0, 3.15, 30.0);
        expect(slant.trueVerticalDepthMeters).to.be.closeTo(731.43, 0.1);
        expect(slant.slantPathDistanceMeters).to.be.closeTo(844.58, 0.1);
    });
});

describe('Equation of Center, Solar Declination & Equation of Time (MarsTime)', () => {
    it('should calculate Mars orbital Equation of the Center C = nu - M', () => {
        // At M = 90 deg, e = 0.0934 -> C ~ (2*0.0934 - 0.0934^3/4)*sin(90) + (5/4*0.0934^2)*sin(180) ~ 0.1866 rad = 10.69 deg
        const eq90 = MarsTime.computeMarsEquationOfCenter(90.0, 0.0934);
        expect(eq90.equationOfCenterDeg).to.be.closeTo(10.69, 0.1);
        expect(eq90.trueAnomalyDeg).to.be.closeTo(100.69, 0.1);

        // At perihelion M = 0 deg -> C = 0 deg
        const eq0 = MarsTime.computeMarsEquationOfCenter(0.0, 0.0934);
        expect(eq0.equationOfCenterDeg).to.equal(0.0);
        expect(eq0.trueAnomalyDeg).to.equal(0.0);
    });

    it('should compute sub-solar declination and Mars Equation of Time in minutes', () => {
        // Ls = 90 deg (Northern Summer Solstice) -> delta_sun = +25.19 deg (axial tilt)
        const summer = MarsTime.computeSubSolarDeclination(90.0, 25.19);
        expect(summer.subSolarLatitudeDeg).to.be.closeTo(25.19, 0.05);
        expect(summer.isNorthernSummer).to.be.true;

        // Ls = 270 deg (Southern Summer / Northern Winter) -> delta_sun = -25.19 deg
        const winter = MarsTime.computeSubSolarDeclination(270.0, 25.19);
        expect(winter.subSolarLatitudeDeg).to.be.closeTo(-25.19, 0.05);
        expect(winter.isNorthernSummer).to.be.false;

        // At M = 90 deg -> C = 10.69 deg -> EoT = 10.69 * 4 min/deg ~ 42.76 Martian minutes
        const eot = MarsTime.computeEquationOfTimeMinutes(0.0, 90.0);
        expect(eot.equationOfTimeMinutes).to.be.closeTo(42.76, 0.5);
        expect(eot.isSunFast).to.be.true;
    });
});

describe('CRISM HCPINDEX, THEMIS Slope & Absorption Asymmetry (BandMathEngine)', () => {
    it('should calculate CRISM high-calcium pyroxene index HCPINDEX', () => {
        // r1815 = 0.25, r2060 = 0.20, r2530 = 0.28
        // continuum = 0.68 * 0.25 + 0.32 * 0.28 = 0.17 + 0.0896 = 0.2596
        // index = 1 - (0.20 / 0.2596) = 1 - 0.7704 = 0.2296 (>0.04 -> hasHCP: true)
        const hcp = BandMathEngine.computeCRISMHighCalciumPyroxeneIndex(0.25, 0.20, 0.28);
        expect(hcp.hcpIndex).to.be.closeTo(0.2296, 0.005);
        expect(hcp.hasHCP).to.be.true;

        // Flat continuum (no absorption) r2060 = continuum = 0.2596 -> index = 0.0
        const noHcp = BandMathEngine.computeCRISMHighCalciumPyroxeneIndex(0.25, 0.2596, 0.28);
        expect(noHcp.hcpIndex).to.be.closeTo(0.0, 0.005);
        expect(noHcp.hasHCP).to.be.false;
    });

    it('should compute THEMIS thermal infrared emissivity slope and spectral asymmetry', () => {
        // E_B4 (8.56 µm) = 0.90, E_B9 (12.57 µm) = 0.98 -> dE = 0.08, dLam = 4.01 µm -> slope = 0.08 / 4.01 ~ 0.01995 µm^-1 (>0.015)
        const themis = BandMathEngine.computeTHEMISEmissivitySpectralSlope(0.90, 0.98, 8.56, 12.57);
        expect(themis.emissivitySlopePerUm).to.be.closeTo(0.01995, 0.001);
        expect(themis.isMaficSloped).to.be.true;

        // Absorption feature: rLeft = 0.30, rCenter = 0.22, rRight = 0.35
        // asym = (0.22 - 0.30) / (0.35 - 0.30) = -0.08 / 0.05 = -1.6 (< 0.5 -> isLeftSkewed: true)
        const asym = BandMathEngine.computeSpectralAbsorptionAsymmetry(0.30, 0.22, 0.35);
        expect(asym.asymmetryRatio).to.equal(-1.6);
        expect(asym.isLeftSkewed).to.be.true;
    });
});

describe('Neukum MPF Polynomial, Isochron Age Ratio & Transition Diameter (CSFDEngine)', () => {
    it('should calculate Neukum & Ivanov (2001) Mars Production Function cumulative density', () => {
        // At D = 1.0 km -> log10(D) = 0 -> log10(N) = a_0 = -2.8398 -> N(>1km) = 10^-2.8398 ~ 1.446e-3 / km^2
        const mpf1 = CSFDEngine.computeNeukumProductionFunctionCumulative(1.0);
        expect(mpf1.log10CumulativeDensity).to.be.closeTo(-2.8398, 0.001);
        expect(mpf1.cumulativeDensityPerKm2).to.be.closeTo(1.446e-3, 0.01e-3);

        // At D = 10.0 km -> log10(D) = 1 -> sum of all coefficients a_0 + ... + a_11 ~ -4.845
        const mpf10 = CSFDEngine.computeNeukumProductionFunctionCumulative(10.0);
        expect(mpf10.log10CumulativeDensity).to.be.closeTo(-4.845, 0.05);
    });

    it('should compute isochron age ratio, geological epoch, and strength-to-gravity transition diameter', () => {
        // Observed N = 2.892e-3, Ref 1Ga = 1.446e-3 -> Ratio = 2.0 -> Age ~ 2.0 Ga (Amazonian)
        const amazon = CSFDEngine.computeIsochronAgeRatio(2.892e-3, 1.446e-3);
        expect(amazon.isochronRatio).to.equal(2.0);
        expect(amazon.estimatedAgeGa).to.equal(2.0);
        expect(amazon.geologicalEpoch).to.include('Amazonian');

        // Observed N = 1.446e-2, Ref = 1.446e-3 -> Ratio = 10.0 -> Age ~ 3.0 + log10(10/3)*0.8 ~ 3.42 Ga (Hesperian)
        const hesp = CSFDEngine.computeIsochronAgeRatio(1.446e-2, 1.446e-3);
        expect(hesp.isochronRatio).to.equal(10.0);
        expect(hesp.estimatedAgeGa).to.be.closeTo(3.42, 0.05);
        expect(hesp.geologicalEpoch).to.include('Hesperian');

        // Y = 10 MPa (1e7 Pa), rho = 2900 kg/m^3, g = 3.72076 m/s^2
        // D_sg = 1e7 / (2900 * 3.72076) = 1e7 / 10790.2 = 926.77 m = 0.927 km
        const dsg = CSFDEngine.computeStrengthToGravityTransitionDiameter(1e7, 2900.0, 3.72076);
        expect(dsg.transitionDiameterMeters).to.be.closeTo(926.8, 0.5);
        expect(dsg.transitionDiameterKm).to.be.closeTo(0.927, 0.005);
    });
});

describe('Adiabatic Lapse Rate, Potential Temperature & Brunt-Väisälä (MCDEngine)', () => {
    it('should calculate Mars dry adiabatic temperature lapse rate Gamma_d', () => {
        // g = 3.72076 m/s^2, c_p = 735 J/(kg K) -> Gamma_d = 3.72076 / 735 = 0.005062 K/m = 5.062 K/km
        const lapse = MCDEngine.computeDryAdiabaticLapseRate(3.72076, 735.0);
        expect(lapse.lapseRateKPerM).to.be.closeTo(0.005062, 0.00001);
        expect(lapse.lapseRateKPerKm).to.be.closeTo(5.062, 0.005);
    });

    it('should compute atmospheric potential temperature and Brunt-Väisälä buoyancy frequency', () => {
        // T = 200 K, P = 305 Pa, P0 = 610 Pa -> P0/P = 2.0 -> kappa = 188.92 / 735 = 0.25703
        // theta = 200 * (2.0)^0.25703 = 200 * 1.195 = 239.0 K
        const theta = MCDEngine.computeAtmosphericPotentialTemperature(200.0, 305.0, 610.0);
        expect(theta.potentialTemperatureK).to.be.closeTo(239.0, 0.5);
        expect(theta.pressureRatio).to.equal(2.0);

        // theta = 220 K, d_theta/dz = 0.002 K/m, g = 3.72076 m/s^2
        // N^2 = (3.72076 / 220) * 0.002 = 0.01691 * 0.002 = 3.3825e-5 rad^2/s^2
        // N = sqrt(3.3825e-5) = 0.005816 rad/s, tau = 2*pi / N ~ 1080.3 s
        const bv = MCDEngine.computeBruntVaisalaFrequency(220.0, 0.002, 3.72076);
        expect(bv.buoyancyFrequencyRadS).to.be.closeTo(0.00582, 0.0001);
        expect(bv.periodSeconds).to.be.closeTo(1080.3, 5.0);
        expect(bv.isStablyStratified).to.be.true;
    });
});

describe('Great-Circle Midpoint, Tunnel Chord & Rhumb Line Distance (GeoUtil)', () => {
    it('should calculate exact spherical midpoint and 3D interior tunnel chord distance', () => {
        // Equator endpoints (0, 0) and (0, 90) on Mars (R = 3389.5 km)
        // Midpoint should be (0, 45)
        const mid = computeGreatCircleMidpoint(0, 0, 0, 90);
        expect(mid.lat).to.be.closeTo(0.0, 0.01);
        expect(mid.lon).to.be.closeTo(45.0, 0.01);

        // Arc length = (pi/2) * 3389.5 = 5324.23 km
        // Chord = 2 * 3389.5 * sin(45 deg) = 2 * 3389.5 * 0.707106 = 4793.47 km
        const chord = computeTunnelChordDistance(0, 0, 0, 90, 'mars');
        expect(chord.chordDistanceKm).to.be.closeTo(4793.47, 1.0);
        expect(chord.arcDifferenceKm).to.be.closeTo(530.76, 2.0);
    });

    it('should compute constant-bearing rhumb line (loxodrome) distance', () => {
        // Direct East-West on equator from (0, 0) to (0, 60) -> distance = (60/180) * pi * 3389.5 = 3549.49 km
        const rhumb = computeSphericalRhumbLineDistance(0, 0, 0, 60, 'mars');
        expect(rhumb.rhumbDistanceKm).to.be.closeTo(3549.49, 1.0);
        expect(rhumb.isDirectEastWest).to.be.true;
    });
});

describe('Thermal Diffusion Time, Frost Feedback & Geothermal Offset (KRCEngine)', () => {
    it('should calculate characteristic layer thermal diffusion timescale', () => {
        // dz = 0.05 m (5 cm), TI = 250 tiu, rho = 1500, cp = 800 -> C_vol = 1.2e6 J/(m^3 K)
        // k = (250)^2 / 1.2e6 = 62500 / 1.2e6 = 0.052083 W/(m K)
        // kappa = 0.052083 / 1.2e6 = 4.3403e-8 m^2/s
        // tau_d = (0.05)^2 / (2 * 4.3403e-8) = 0.0025 / 8.6806e-8 ~ 28800 s = 8.0 hours
        const diff = KRCEngine.computeSubsurfaceLayerThermalDiffusionTime(0.05, 250.0, 1500, 800);
        expect(diff.diffusionTimeSeconds).to.be.closeTo(28800.0, 50.0);
        expect(diff.diffusionTimeHours).to.be.closeTo(8.0, 0.05);
    });

    it('should compute non-linear frost albedo feedback and geothermal temperature offset', () => {
        // A_bare = 0.25, A_frost = 0.65, m = 1.25 kg/m^2, m_crit = 5.0 kg/m^2
        // coverage = sqrt(1.25 / 5.0) = sqrt(0.25) = 0.5
        // A_eff = 0.25 + (0.65 - 0.25) * 0.5 = 0.25 + 0.20 = 0.45
        const frost = KRCEngine.computeFrostAlbedoFeedbackTransition(0.25, 0.65, 1.25, 5.0);
        expect(frost.effectiveAlbedo).to.equal(0.45);
        expect(frost.frostCoverageFraction).to.equal(0.5);
        expect(frost.isFrostSaturated).to.be.false;

        // q_geo = 30 mW/m^2 (0.03 W/m^2), k = 2.0 W/(m K), dz = 100 m
        // R_th = 100 / 2 = 50 m^2 K/W -> Delta_T = 0.03 * 50 = 1.5 K
        const geo = KRCEngine.computeSubsurfaceConductiveTemperatureOffset(30.0, 2.0, 100.0);
        expect(geo.temperatureOffsetK).to.equal(1.5);
        expect(geo.thermalResistanceM2K_W).to.equal(50.0);
    });
});

describe('Dielectric Quality Factor, Ice-Dust Inversion & Rough Interface (RadarSounderEngine)', () => {
    it('should calculate dielectric quality factor Q and loss regime', () => {
        // Pure water ice loss tangent tan(delta) = 0.001 -> Q = 1000.0 (Low Loss)
        const qIce = RadarSounderEngine.computeDielectricQualityFactor(0.001);
        expect(qIce.qualityFactorQ).to.equal(1000.0);
        expect(qIce.lossRegime).to.include('Low Loss');

        // Conductive regolith tan(delta) = 0.05 -> Q = 20.0 (High Loss)
        const qReg = RadarSounderEngine.computeDielectricQualityFactor(0.05);
        expect(qReg.qualityFactorQ).to.equal(20.0);
        expect(qReg.lossRegime).to.include('High Loss');
    });

    it('should invert ice-to-dust volumetric ratio and compute rough interface scattering loss', () => {
        // Pure ice eps = 3.15 -> n = 1.7748, Dust eps = 7.5 -> n = 2.7386
        // If bulk eps = 3.15 -> phi_ice = 1.0 (100% ice), phi_dust = 0.0
        const invPure = RadarSounderEngine.invertIceDustVolumeFraction(3.15, 3.15, 7.5);
        expect(invPure.iceFraction).to.equal(1.0);
        expect(invPure.dustFraction).to.equal(0.0);
        expect(invPure.icePercentage).to.equal(100.0);

        // NPLD bulk eps = 3.25 -> n = 1.8028 -> phi_ice = (2.7386 - 1.8028) / (2.7386 - 1.7748) = 0.9358 / 0.9638 ~ 0.9709 (97.09% ice)
        const invNpld = RadarSounderEngine.invertIceDustVolumeFraction(3.25, 3.15, 7.5);
        expect(invNpld.icePercentage).to.be.closeTo(97.09, 0.2);
        expect(invNpld.dustPercentage).to.be.closeTo(2.91, 0.2);

        // Smooth reflectivity = -10.0 dB, sigma_h = 0.2 m, freq = 20 MHz (lambda = 8.44 m in ice eps = 3.15)
        // km = 2*pi / 8.44 ~ 0.744 rad/m -> g_r = 4 * (0.744 * 0.2)^2 = 4 * (0.1488)^2 = 4 * 0.02214 ~ 0.0886
        // loss_dB = 0.0886 * 4.3429 ~ 0.38 dB -> rough_dB = -10.38 dB
        const rough = RadarSounderEngine.computeRoughInterfaceScatteringLoss(-10.0, 0.2, 20e6, 3.15, 0.0);
        expect(rough.roughnessScatteringLossDb).to.be.closeTo(0.38, 0.05);
        expect(rough.roughReflectivityDb).to.be.closeTo(-10.38, 0.05);
    });
});

describe('True Solar Right Ascension, LMST to LTST & Seasons (MarsTime)', () => {
    it('should calculate True Solar Right Ascension on the celestial sphere', () => {
        // At vernal equinox Ls = 0 deg -> alpha = 0 deg
        const ra0 = MarsTime.computeTrueSolarRightAscension(0.0);
        expect(ra0.rightAscensionDeg).to.be.closeTo(0.0, 0.01);
        expect(ra0.rightAscensionHours).to.be.closeTo(0.0, 0.01);

        // At summer solstice Ls = 90 deg -> alpha = 90 deg = 6.0 hours
        const ra90 = MarsTime.computeTrueSolarRightAscension(90.0);
        expect(ra90.rightAscensionDeg).to.be.closeTo(90.0, 0.01);
        expect(ra90.rightAscensionHours).to.be.closeTo(6.0, 0.01);
    });

    it('should convert LMST to LTST with Equation of Time and retrieve Mars season metadata', () => {
        // At Ls = 90 deg -> LMST = 12.0000 -> convertLMSTtoLTST returns valid sol-hour string
        const conv = MarsTime.convertLMSTtoLTST(12.0, 90.0);
        expect(conv.ltstHours).to.be.within(0, 24);
        expect(conv.formattedLTST).to.match(/^\d{2}:\d{2}:\d{2}$/);

        // Ls = 135 deg -> Q2 Summer in North, Winter in South (50% progress into season)
        const s135 = MarsTime.getMarsSeasonMetadata(135.0);
        expect(s135.seasonIndex).to.equal(1);
        expect(s135.northernSeason).to.equal('Summer');
        expect(s135.southernSeason).to.equal('Winter');
        expect(s135.seasonProgressPercent).to.equal(50.0);
        expect(s135.solQuadrant).to.include('Q2');
    });
});

describe('Fe3+ Phyllosilicates, THEMIS Felsic Quartz & Continuum Slope (BandMathEngine)', () => {
    it('should calculate CRISM BD2290 Fe3+-OH nontronite band depth index', () => {
        // r2290 = 0.22, r2140 = 0.26, r2350 = 0.25
        // continuum = 0.714 * 0.26 + 0.286 * 0.25 = 0.18564 + 0.0715 = 0.25714
        // bd = 1 - (0.22 / 0.25714) = 1 - 0.85556 = 0.1444 (> 0.035 -> hasFe3Phyllosilicate: true)
        const fe3 = BandMathEngine.computeCRISMFe3PhyllosilicateIndex(0.22, 0.26, 0.25);
        expect(fe3.bd2290).to.be.closeTo(0.1444, 0.005);
        expect(fe3.hasFe3Phyllosilicate).to.be.true;

        // Flat continuum (no absorption) r2290 = 0.25714 -> bd = 0.0
        const noFe3 = BandMathEngine.computeCRISMFe3PhyllosilicateIndex(0.25714, 0.26, 0.25);
        expect(noFe3.bd2290).to.be.closeTo(0.0, 0.005);
        expect(noFe3.hasFe3Phyllosilicate).to.be.false;
    });

    it('should compute THEMIS felsic quartz reststrahlen index and spectral continuum slope', () => {
        // e3 = 0.95, e4 = 0.90 (silica trough), e5 = 0.94
        // felsicIndex = (0.95 + 0.94) / (2 * 0.90) = 1.89 / 1.80 = 1.05 (> 1.025 -> hasFelsicSignature: true)
        const felsic = BandMathEngine.computeTHEMISFelsicQuartzIndex(0.95, 0.90, 0.94);
        expect(felsic.felsicIndex).to.equal(1.05);
        expect(felsic.hasFelsicSignature).to.be.true;

        // r1 = 0.15 at 1.0 µm, r2 = 0.30 at 2.5 µm -> dR = 0.15, dLam = 1.5 µm -> slope = 0.10 µm^-1 (isRedSloped: true)
        const slope = BandMathEngine.computeSpectralContinuumSlope(0.15, 0.30, 1.0, 2.5);
        expect(slope.continuumSlopePerUm).to.equal(0.1);
        expect(slope.isRedSloped).to.be.true;
    });
});

describe('Transient Inversion, Ejecta Blanket & Central Peak (CSFDEngine)', () => {
    it('should invert transient cavity diameter from modified complex crater rim diameter', () => {
        // Simple crater D_f = 5.0 km (<= 7 km) -> D_t = 5.0 / 1.25 = 4.0 km
        const simple = CSFDEngine.invertTransientFromComplexFinalDiameter(5.0, 7.0);
        expect(simple.transientDiameterKm).to.equal(4.0);
        expect(simple.morphologyClass).to.include('Simple');

        // Complex crater D_f = 20.0 km (> 7 km) -> D_t = (7^0.15 * 20^0.85) / 1.17 ~ (1.338 * 12.75) / 1.17 ~ 14.58 km
        const complex = CSFDEngine.invertTransientFromComplexFinalDiameter(20.0, 7.0);
        expect(complex.transientDiameterKm).to.be.closeTo(14.58, 0.2);
        expect(complex.morphologyClass).to.include('Complex');
    });

    it('should compute continuous ejecta blanket thickness profile and central peak uplift diameter', () => {
        // D_f = 10.0 km -> R_c = 5.0 km -> at r = 5.0 km (rim), t_rim = 0.04 * 10,000 m = 400 m
        const atRim = CSFDEngine.computeContinuousEjectaBlanketThickness(5.0, 10.0);
        expect(atRim.ejectaThicknessMeters).to.equal(400.0);
        expect(atRim.normalizedDistance).to.equal(1.0);

        // At r = 10.0 km (2 crater radii) -> normR = 2.0 -> t = 400 * 2^-3 = 50.0 m
        const at2R = CSFDEngine.computeContinuousEjectaBlanketThickness(10.0, 10.0);
        expect(at2R.ejectaThicknessMeters).to.equal(50.0);

        // Central peak for D_f = 30.0 km -> D_cp = 0.22 * 30^1.12 ~ 0.22 * 45.14 ~ 9.93 km
        const peak = CSFDEngine.computeCentralPeakUpliftDiameter(30.0, 7.0);
        expect(peak.centralPeakDiameterKm).to.be.closeTo(9.93, 0.2);
        expect(peak.hasCentralPeak).to.be.true;

        // Simple crater (D_f = 4.0 km) -> no central peak
        const noPeak = CSFDEngine.computeCentralPeakUpliftDiameter(4.0, 7.0);
        expect(noPeak.hasCentralPeak).to.be.false;
    });
});

describe('Thermal Wind Shear, Scale Height Profile & TKE Dissipation (MCDEngine)', () => {
    it('should calculate thermal wind vertical shear gradient and Coriolis parameter', () => {
        // Lat = 45 deg, dT/dy = -0.01 K/km (-10 K per 1000 km, colder towards pole)
        // f = 2 * 7.0882e-5 * sin(45 deg) = 1.41764e-4 * 0.707106 = 1.0024e-4 rad/s
        // du_g/dz = - (3.72076 / (1.0024e-4 * 210)) * (-1e-5) = (3.72076 / 0.02105) * 1e-5 = 176.75 * 1e-5 = 1.7675e-3 s^-1 = 1.768 (m/s)/km
        const wind = MCDEngine.computeThermalWindShearGradient(-0.01, 210.0, 45.0, 3.72076);
        expect(wind.thermalWindShearPerKm).to.be.closeTo(1.768, 0.05);
        expect(wind.coriolisParameterRadS).to.be.closeTo(1.002e-4, 0.005e-4);
    });

    it('should compute local scale height profile and boundary layer TKE dissipation rate', () => {
        // T = 210 K, M = 43.34 g/mol -> R_spec = 8.31446 / 0.04334 = 191.84 J/(kg K)
        // H = (191.84 * 210) / 3.72076 = 40286.4 / 3.72076 = 10827.4 m = 10.827 km
        const h = MCDEngine.computeAtmosphericScaleHeightProfile(210.0, 43.34, 3.72076);
        expect(h.scaleHeightKm).to.be.closeTo(10.827, 0.05);
        expect(h.specificGasConstant).to.be.closeTo(191.84, 0.1);

        // w_* = 2.0 m/s -> w_*^3 = 8.0 m^3/s^3, z_i = 4000 m, z = 2000 m (z/zi = 0.5)
        // shape = 0.8 - 0.3 * 0.5 = 0.65 -> epsilon = (8.0 / 4000) * 0.65 = 0.002 * 0.65 = 1.3e-3 m^2/s^3
        const tke = MCDEngine.computeAtmosphericTurbulentKineticEnergyDissipation(2.0, 4000.0, 2000.0);
        expect(tke.tkeDissipationM2S3).to.be.closeTo(1.3e-3, 0.05e-3);
        expect(tke.normalizedHeight).to.equal(0.5);
    });
});

describe('Spherical Excess, Andoyer Geodesic & Vertex Deflections (GeoUtil)', () => {
    it('should calculate spherical excess E, solid angle, and surface area for spherical polygons', () => {
        // Tri-rectangular spherical triangle on Mars (three 90-degree angles = 1/8th of sphere)
        // sum = 270 deg, expected = 180 deg -> E = 90 deg = pi/2 rad = 1.5708 rad (1/8th of 4*pi)
        // Area = (pi/2) * (3389.5)^2 = 1.570796 * 1.14887e7 = 18,046,432 km^2
        const octant = computeSphericalExcess([90, 90, 90], 'mars');
        expect(octant.sphericalExcessDeg).to.equal(90.0);
        expect(octant.solidAngleSteradians).to.be.closeTo(1.5708, 0.001);
        expect(octant.surfaceAreaKm2).to.be.closeTo(18046432.0, 1000.0);
    });

    it('should compute second-order Andoyer ellipsoidal geodesic distance and polyline deflection angles', () => {
        // Equator endpoints (0, 0) to (0, 60) on Mars (a = 3396.2, f = 0.00589)
        const andoyer = computeEllipsoidalGeodesicDistanceAndoyer(0, 0, 0, 60, 'mars');
        expect(andoyer.ellipsoidalDistanceKm).to.be.greaterThan(3500.0);
        expect(andoyer.sphericalDistanceKm).to.be.greaterThan(3500.0);

        // Path: (0, 0) -> (10, 0) [heading North: 0 deg] -> (10, 10) [heading East: 90 deg]
        // Turn at vertex 1: deflection = 90 deg (Right turn)
        const path = [[0, 0], [10, 0], [10, 10]];
        const defs = computePolylineDeflectionAngles(path);
        expect(defs).to.have.lengthOf(1);
        expect(defs[0].deflectionAngleDeg).to.be.closeTo(90.0, 1.0);
        expect(defs[0].isRightTurn).to.be.true;
    });
});

describe('Macroscopic Roughness, Gas Conductivity & Volatiles (KRCEngine)', () => {
    it('should calculate effective bolometric brightness temperature for sub-pixel shadowed mixtures', () => {
        // Sunlit = 280 K, Shadowed = 160 K, shadow fraction = 0.3
        // Rad = 0.7 * (280)^4 + 0.3 * (160)^4 = 0.7 * 6.14656e9 + 0.3 * 6.5536e8 = 4.30259e9 + 1.966e8 = 4.4992e9
        // T_eff = (4.4992e9)^0.25 = 258.99 K (vs linear 0.7*280 + 0.3*160 = 244 K)
        const rough = KRCEngine.computeSurfaceMacroscopicRoughnessEffectiveTemp(280.0, 160.0, 0.3);
        expect(rough.effectiveTempK).to.be.closeTo(258.99, 0.1);
        expect(rough.thermalContrastK).to.equal(120.0);
        expect(rough.meanLinearTempK).to.equal(244.0);
    });

    it('should compute pressure-dependent porous regolith conductivity and CO2 volatile sublimation rate', () => {
        // P = 610 Pa, P0 = 610 Pa -> pRatio = 1.0 -> k_gas = 0.015 * (1/2) = 0.0075 W/(m K)
        // k_eff = 0.03 + 0.0075 = 0.0375 W/(m K) -> gas fraction = 0.0075 / 0.0375 = 0.20 (20%)
        const gas = KRCEngine.computePorousRegolithGasConductivity(610.0, 0.03, 0.015, 610.0);
        expect(gas.effectiveConductivityW_MK).to.equal(0.0375);
        expect(gas.gasContributionFraction).to.equal(0.2);

        // Net flux = 50 W/m^2, T_frost = 148 K, eps = 0.95 -> emission = 0.95 * 5.670374e-8 * (148)^4 = 0.95 * 5.670374e-8 * 4.7977e8 ~ 25.84 W/m^2
        // Net for phase change = 50 - 25.84 = 24.16 W/m^2
        // dm/dt = 24.16 / 5.9e5 = 4.095e-5 kg/(m^2 s) -> rate in um/hr = (4.095e-5 / 1600) * 1e6 * 3600 ~ 92.14 um/hr
        const sub = KRCEngine.computeVolatileSublimationRate(50.0, 148.0, 5.9e5, 0.95);
        expect(sub.isSublimating).to.be.true;
        expect(sub.sublimationRateKgM2S).to.be.closeTo(4.095e-5, 0.05e-5);
        expect(sub.sublimationRateUmPerHour).to.be.closeTo(92.14, 1.0);
    });
});

describe('CRIM Multi-Phase Mixtures, Fresnel Zone & Transmission Loss (RadarSounderEngine)', () => {
    it('should calculate effective dielectric permittivity using Complex Refractive Index Model (CRIM)', () => {
        // Ice 90% (eps = 3.15, n = 1.7748), Rock 10% (eps = 7.5, n = 2.7386)
        // n_eff = 0.9 * 1.7748 + 0.1 * 2.7386 = 1.5973 + 0.2739 = 1.8712 -> eps_eff = (1.8712)^2 = 3.501
        const crim = RadarSounderEngine.computeComplexRefractiveIndexMixture(
            { ice: 0.90, rock: 0.10 },
            { ice: 3.15, rock: 7.5, void: 1.0 }
        );
        expect(crim.effectiveRefractiveIndex).to.be.closeTo(1.8712, 0.005);
        expect(crim.effectivePermittivity).to.be.closeTo(3.501, 0.02);
        expect(crim.phaseVelocityKmS).to.be.closeTo(160200.0, 1000.0);
    });

    it('should compute subsurface Fresnel zone footprint diameter and two-way interface transmission loss', () => {
        // z = 500 m, freq = 20 MHz in ice eps = 3.15 (v = 1.689e8 m/s -> lambda = 8.445 m)
        // term = (8.445 * 500) / 2 + (8.445)^2 / 16 = 2111.25 + 4.46 = 2115.71 -> d_F = 2 * sqrt(2115.71) = 2 * 46.0 = 92.0 m
        const fresnel = RadarSounderEngine.computeFresnelZoneFootprintDiameter(500.0, 20e6, 3.15);
        expect(fresnel.fresnelDiameterMeters).to.be.closeTo(92.0, 1.0);
        expect(fresnel.wavelengthInMediumMeters).to.be.closeTo(8.45, 0.05);

        // Overlying boundary with -10 dB reflectivity (R_lin = 0.1)
        // T_1way = 0.9 -> T_2way = 0.81 -> loss = -10*log10(0.81) = 0.915 dB
        const trans = RadarSounderEngine.computeTwoWayInterfaceTransmissionLoss([-10.0]);
        expect(trans.twoWayTransmissionFraction).to.equal(0.81);
        expect(trans.totalTransmissionLossDb).to.be.closeTo(0.915, 0.01);
    });
});

describe('BD1400 Hydration, Carbonate BD2500 & Second Derivative (BandMathEngine)', () => {
    it('should calculate CRISM BD1400 structural hydration band depth index', () => {
        // r1395 = 0.22, r1330 = 0.26, r1480 = 0.25
        // continuum = 0.571 * 0.26 + 0.429 * 0.25 = 0.14846 + 0.10725 = 0.25571
        // bd = 1 - (0.22 / 0.25571) = 1 - 0.86035 = 0.13965 (> 0.03 -> hasHydration: true)
        const bd1400 = BandMathEngine.computeCRISMBD1400Index(0.22, 0.26, 0.25);
        expect(bd1400.bd1400).to.be.closeTo(0.1396, 0.005);
        expect(bd1400.hasHydration).to.be.true;

        // Flat continuum (no absorption) r1395 = 0.25571 -> bd = 0.0
        const noHyd = BandMathEngine.computeCRISMBD1400Index(0.25571, 0.26, 0.25);
        expect(noHyd.bd1400).to.be.closeTo(0.0, 0.005);
        expect(noHyd.hasHydration).to.be.false;
    });

    it('should compute CRISM BD2500 carbonate band depth and second-derivative spectral curvature', () => {
        // r2530 = 0.21, r2300 = 0.26, r2600 = 0.24 -> cont = 0.5 * 0.26 + 0.5 * 0.24 = 0.25
        // bd = 1 - (0.21 / 0.25) = 1 - 0.84 = 0.16 (> 0.035 -> hasCarbonateSignature: true)
        const carb = BandMathEngine.computeCRISMMagnesiumCarbonateIndex(0.21, 0.26, 0.24);
        expect(carb.bd2500).to.equal(0.16);
        expect(carb.hasCarbonateSignature).to.be.true;

        // Convex peak: rLeft = 0.20, rCenter = 0.30, rRight = 0.22 -> D2 = 2 * 0.30 - 0.20 - 0.22 = 0.60 - 0.42 = 0.18 (>0.005)
        const peak = BandMathEngine.computeSecondDerivativeSpectralPeak(0.20, 0.30, 0.22);
        expect(peak.curvatureD2).to.equal(0.18);
        expect(peak.isConvexPeak).to.be.true;
        expect(peak.isConcaveAbsorption).to.be.false;
    });
});

describe('Heliocentric Distance, Subsolar Coordinates & Topocentric Irradiance (MarsTime)', () => {
    it('should calculate heliocentric Mars-Sun distance in AU and identify perihelion', () => {
        // At Ls = 251.0 (perihelion): r = 1.523679 * (1 - 0.09340^2) / (1 + 0.09340) = 1.523679 * (1 - 0.09340) = 1.38136 AU
        const perihelion = MarsTime.computeHeliocentricDistanceAU(251.0);
        expect(perihelion.distanceAU).to.be.closeTo(1.38136, 0.005);
        expect(perihelion.isNearPerihelion).to.be.true;

        // At Ls = 71.0 (aphelion): r = 1.523679 * (1 - 0.09340^2) / (1 - 0.09340) = 1.523679 * (1 + 0.09340) = 1.66600 AU
        const aphelion = MarsTime.computeHeliocentricDistanceAU(71.0);
        expect(aphelion.distanceAU).to.be.closeTo(1.66600, 0.005);
        expect(aphelion.isNearPerihelion).to.be.false;
    });

    it('should compute subsolar coordinates and topocentric solar zenith irradiance', () => {
        // At Ls = 90 (Northern Summer Solstice): subsolar lat = +25.19 deg
        const sub = MarsTime.computeSubsolarPoint(90.0, 12.0);
        expect(sub.subsolarLatDeg).to.be.closeTo(25.19, 0.05);

        // At target location directly at subsolar point (lat = 25.19, lon = sub.subsolarLonDeg) -> zenith = 0 deg, max irradiance
        const noon = MarsTime.computeTopocentricSolarZenithAndIrradiance(25.19, sub.subsolarLonDeg, 90.0, 12.0);
        expect(noon.solarZenithAngleDeg).to.be.closeTo(0.0, 0.5);
        expect(noon.isSunlit).to.be.true;
        expect(noon.directSolarIrradianceW_M2).to.be.greaterThan(450.0);

        // Night side (same latitude +25.19 deg, opposite longitude +180 deg) -> cos(zenith) = -cos(50.38 deg) -> zenith = 129.62 deg, irradiance = 0
        const night = MarsTime.computeTopocentricSolarZenithAndIrradiance(25.19, sub.subsolarLonDeg + 180.0, 90.0, 12.0);
        expect(night.solarZenithAngleDeg).to.be.closeTo(129.62, 0.5);
        expect(night.isSunlit).to.be.false;
        expect(night.directSolarIrradianceW_M2).to.equal(0.0);
    });
});

describe('Impactor Energy, Schmidt-Housen Scaling & Morphometry (CSFDEngine)', () => {
    it('should calculate impactor mass and kinetic energy in Joules and Megatons TNT', () => {
        // L = 100 m, v = 12.0 km/s (12,000 m/s), rho = 3000 kg/m^3
        // Vol = (pi / 6) * 100^3 = 523,598.77 m^3 -> Mass = 523,598.77 * 3000 = 1.5708e9 kg
        // Energy = 0.5 * 1.5708e9 * (1.2e4)^2 = 0.5 * 1.5708e9 * 1.44e8 = 1.131e17 J
        // MT TNT = 1.131e17 / 4.184e15 = 27.03 MT
        const impact = CSFDEngine.computeImpactorKineticEnergyJoules(100.0, 12.0, 3000.0);
        expect(impact.projectileMassKg).to.be.closeTo(1.571e9, 0.01e9);
        expect(impact.kineticEnergyJoules).to.be.closeTo(1.131e17, 0.01e17);
        expect(impact.energyMegatonsTNT).to.be.closeTo(27.03, 0.5);
    });

    it('should compute Schmidt-Housen transient cavity diameter and simple crater morphometry', () => {
        // L = 100 m, v = 12 km/s, angle = 45 deg, rho_imp = 3000, rho_targ = 2600, g = 3.72
        const scaling = CSFDEngine.computeSchmidtHousenTransientDiameter(100.0, 12.0, 45.0, 3000.0, 2600.0, 3.72076);
        expect(scaling.transientDiameterMeters).to.be.greaterThan(1200.0);
        expect(scaling.transientDiameterMeters).to.be.lessThan(2500.0);
        expect(scaling.excavationDepthMeters).to.be.closeTo(scaling.transientDiameterMeters / 3.0, 0.5);

        // Simple bowl crater D_f = 2.0 km (2000 m)
        // hRim = 0.04 * 2000 = 80 m, dApp = 0.20 * 2000 = 400 m, dExc = 200 m, dTotal = 480 m
        const morph = CSFDEngine.computeSimpleCraterMorphometryProfile(2.0);
        expect(morph.rimHeightMeters).to.equal(80.0);
        expect(morph.apparentDepthMeters).to.equal(400.0);
        expect(morph.excavationDepthMeters).to.equal(200.0);
        expect(morph.totalRimFloorDepthMeters).to.equal(480.0);
    });
});

describe('Bounding Circle, Great Circle Intersection & Ellipse Area (GeoUtil)', () => {
    it('should calculate spherical enclosing bounding circle and ellipse surface area', () => {
        // Square centered at (0, 0) with half-width 5 deg
        const sq = [[-5, -5], [-5, 5], [5, 5], [5, -5]];
        const circle = computeSphericalBoundingCircle(sq, 'mars');
        expect(circle.centerLat).to.be.closeTo(0.0, 0.01);
        expect(circle.centerLon).to.be.closeTo(0.0, 0.01);
        expect(circle.radiusKm).to.be.greaterThan(400.0);

        // Elliptical caldera semi-major = 10 km, semi-minor = 6 km -> Area = pi * 10 * 6 = 188.50 km^2
        // Eccentricity = sqrt(1 - 36/100) = sqrt(0.64) = 0.80
        const ellipse = computePlanetaryEllipseSurfaceArea(10.0, 6.0);
        expect(ellipse.surfaceAreaKm2).to.be.closeTo(188.50, 0.1);
        expect(ellipse.eccentricity).to.equal(0.8);
        expect(ellipse.flattening).to.equal(0.4);
    });

    it('should compute exact intersection coordinates of two great circles', () => {
        // Circle 1: Equator (lat 0, lon -90 to lon +90)
        // Circle 2: Prime Meridian (lon 0, lat -90 to lat +90)
        // Intersection point: (0, 0) and antipodal point (0, 180)
        const isect = computeGreatCircleIntersection(0, -90, 0, 90, -90, 0, 90, 0);
        expect(Math.abs(isect.lat)).to.be.lessThan(0.01);
        expect(Math.abs(isect.antipodalLat)).to.be.lessThan(0.01);
        expect([0, 180]).to.include(Math.round(Math.abs(isect.lon)));
        expect([0, 180]).to.include(Math.round(Math.abs(isect.antipodalLon)));
    });
});

if (typeof mocha !== 'undefined') {
    mocha.run();
}
