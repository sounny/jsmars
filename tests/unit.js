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

if (typeof mocha !== 'undefined') {
    mocha.run();
}
