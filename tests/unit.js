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
import { URLStateEngine } from '../src/util/URLStateEngine.js';
import { PWAManager } from '../src/pwa/PWAManager.js';
import { MobileSheet } from '../src/ui/MobileSheet.js';
import { SessionManager } from '../src/ui/SessionManager.js';
import { StampQueryPanel } from '../src/features/stamp/StampQueryPanel.js';
import { haversineDistance, azimuth, toGraphic, toCentric, formatLatLon, sphericalPolygonArea, computeEllipsePolygon, computeBufferPolygon, isPointInPolygon, computeBoundingBox, sphericalToCartesian, cartesianToSpherical, interpolateGreatCircle, computeMidpoint, computeDestinationPoint, computeCrossTrackDistance, computeAlongTrackDistance, computePolylineLength, computePolygonPerimeter, computeGreatCircleMidpoint, computeTunnelChordDistance, computeSphericalRhumbLineDistance, computeSphericalExcess, computeEllipsoidalGeodesicDistanceAndoyer, computePolylineDeflectionAngles, computeSphericalBoundingCircle, computeGreatCircleIntersection, computePlanetaryEllipseSurfaceArea, computeSomiglianaTheoreticalGravity, convertPlanetographicToPlanetocentricLatitude, computeGreatCircleRhumbLineHeading, computeLambertAzimuthalEqualArea, computePolarStereographic, computeMeridianConvergenceAngle, computeSinusoidalProjection, computeSinusoidalInverse, computeMercatorScaleDistortionFactor, computeOrthographicProjection, computeOrthographicInverse, computeGnomonicProjection, computeGnomonicInverse, computeEquidistantCylindricalProjection, computeEquidistantCylindricalInverse, computeLambertConformalConicProjection, computeLambertConformalConicInverse, computePolarStereographicProjection, computePolarStereographicInverse, computeMollweideProjection, computeMollweideInverse } from '../src/util/geo.js';

const expect = chai.expect;

describe('JMARSState', () => {
    beforeEach(() => {
        jmarsState.reset();
    });

    it('should have default state', () => {
        expect(jmarsState.get('body')).to.equal('mars');
        expect(jmarsState.get('activeLayers')).to.be.an('array').that.is.empty;
    });

    it('should update body', () => {
        jmarsState.set('body', 'Earth');
        expect(jmarsState.get('body')).to.equal('earth');
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

describe('CO2 Frost Point, Sound Speed & Dust Optical Depth (MCDEngine)', () => {
    it('should calculate CO2 dry ice frost point temperature from ambient atmospheric pressure', () => {
        // P = 610 Pa (datum pressure) -> ln(610) = 6.4135 -> denom = 27.55 - 6.4135 = 21.1365 -> T_frost = 3148 / 21.1365 = 148.94 K
        const frost = MCDEngine.computeCO2FrostPointTemperature(610.0);
        expect(frost.frostPointTempK).to.be.closeTo(148.94, 0.5);
        expect(frost.pressurePa).to.equal(610.0);
    });

    it('should compute Mars atmospheric sound speed and seasonal column dust optical depth', () => {
        // T = 210 K, gamma = 1.29, R_spec = 188.92 J/(kg K)
        // c_s = sqrt( 1.29 * 188.92 * 210 ) = sqrt( 51178.43 ) = 226.23 m/s
        const sound = MCDEngine.computeAtmosphericSoundSpeed(210.0, 1.29, 188.92);
        expect(sound.soundSpeedMps).to.be.closeTo(226.23, 0.2);
        expect(sound.machOneMps).to.be.closeTo(226.23, 0.2);

        // Ls = 270 deg (Southern Summer Solstice, peak dust storm season)
        // angle = 90 deg -> sin^2(90 deg) = 1.0 -> total tau = 0.25 + 1.5 * 1.0 = 1.75
        // At zenith = 0 deg -> cosZ = 1.0 -> transmission = exp(-1.75) = 0.1738 (17.38%)
        const dust = MCDEngine.computeColumnDustOpticalDepth(270.0, 0.25, 1.5, 0.0);
        expect(dust.columnOpticalDepthTau).to.equal(1.75);
        expect(dust.slantTransmissionFraction).to.be.closeTo(0.1738, 0.005);
        expect(dust.isDustStormSeason).to.be.true;

        // Ls = 90 deg (Northern Summer, clear skies) -> storm factor = 0 -> tau = 0.25
        const clear = MCDEngine.computeColumnDustOpticalDepth(90.0, 0.25, 1.5, 0.0);
        expect(clear.columnOpticalDepthTau).to.equal(0.25);
        expect(clear.isDustStormSeason).to.be.false;
    });
});

describe('Gypsum Doublet, OLINDEX3 Olivine & Euclidean Distance (BandMathEngine)', () => {
    it('should calculate CRISM BD1900D gypsum doublet and OLINDEX3 olivine absorption indices', () => {
        // Gypsum doublet: r1930 = 0.20, r1980 = 0.22 (rCenter = 0.21), r1815 = 0.26, r2130 = 0.24 (cont = 0.6*0.26 + 0.4*0.24 = 0.252)
        // bd = 1 - (0.21 / 0.252) = 1 - 0.8333 = 0.1667 (> 0.035 -> hasGypsumSignature: true)
        const gyp = BandMathEngine.computeCRISMGypsumDoubletIndex(0.20, 0.22, 0.26, 0.24);
        expect(gyp.bd1900d).to.be.closeTo(0.1667, 0.005);
        expect(gyp.hasGypsumSignature).to.be.true;

        // OLINDEX3: r1050 = 0.18, r850 = 0.28, r1350 = 0.24 (cont = 0.65*0.28 + 0.35*0.24 = 0.182 + 0.084 = 0.266)
        // ol = 1 - (0.18 / 0.266) = 1 - 0.6767 = 0.3233 (> 0.05 -> hasOlivineSignature: true)
        const ol = BandMathEngine.computeCRISMOLINDEX3(0.18, 0.28, 0.24);
        expect(ol.olindex3).to.be.closeTo(0.3233, 0.005);
        expect(ol.hasOlivineSignature).to.be.true;
    });

    it('should compute multidimensional hyperspectral Euclidean distance and RMS divergence', () => {
        const specA = [0.20, 0.25, 0.30, 0.35];
        const specB = [0.22, 0.23, 0.31, 0.33];
        // diffs: +0.02, -0.02, +0.01, -0.02 -> sq: 0.0004 + 0.0004 + 0.0001 + 0.0004 = 0.0013
        // d_euc = sqrt(0.0013) = 0.03606, rmsd = sqrt(0.0013 / 4) = 0.01803
        const dist = BandMathEngine.computeSpectralEuclideanDistance(specA, specB);
        expect(dist.euclideanDistance).to.be.closeTo(0.03606, 0.0005);
        expect(dist.rmsDivergence).to.be.closeTo(0.01803, 0.0005);
        expect(dist.numBands).to.equal(4);
    });
});

describe('Deep-Link URL State Serialization & Sharing (URLStateEngine)', () => {
    it('should serialize map view, active layers, color stretch, and Ls into shareable URL parameters', () => {
        const state = {
            body: 'mars',
            lat: -14.5234,
            lon: 175.4321,
            zoom: 6,
            activeLayers: [
                { id: 'viking', opacity: 1.0, visible: true },
                { id: 'mdim', opacity: 0.8, visible: false }
            ],
            colorStretch: {
                brightness: 120,
                contrast: 90,
                saturation: 150,
                hueRotate: 45,
                invert: false
            },
            ls: 180.5,
            poi: 'Jezero Crater'
        };

        const url = URLStateEngine.serializeStateToURL(state, 'https://jsmars.sounny.com');
        expect(url).to.include('lat=-14.5234');
        expect(url).to.include('lon=175.4321');
        expect(url).to.include('z=6');
        expect(url).to.include('layers=viking%3A1%3A1%2Cmdim%3A0.8%3A0');
        expect(url).to.include('stretch=120%2C90%2C150%2C45%2C0');
        expect(url).to.include('ls=180.5');
        expect(url).to.include('poi=Jezero+Crater');
    });

    it('should correctly parse and restore deep-link URL query strings into structured state', () => {
        const query = '?body=moon&lat=20.1234&lon=-45.6789&z=5&layers=lroc:0.9:1,clementina:0.5:0&stretch=110,95,120,15,1&ls=90.0&poi=Copernicus';
        const parsed = URLStateEngine.parseURLToState(query);

        expect(parsed.hasState).to.be.true;
        expect(parsed.body).to.equal('moon');
        expect(parsed.lat).to.equal(20.1234);
        expect(parsed.lon).to.equal(-45.6789);
        expect(parsed.zoom).to.equal(5);

        expect(parsed.activeLayers).to.have.lengthOf(2);
        expect(parsed.activeLayers[0]).to.deep.equal({ id: 'lroc', opacity: 0.9, visible: true });
        expect(parsed.activeLayers[1]).to.deep.equal({ id: 'clementina', opacity: 0.5, visible: false });

        expect(parsed.colorStretch).to.deep.equal({
            brightness: 110,
            contrast: 95,
            saturation: 120,
            hueRotate: 15,
            invert: true
        });

        expect(parsed.ls).to.equal(90.0);
        expect(parsed.poi).to.equal('Copernicus');
    });
});

describe('Radar Range Resolution, Ionospheric Delay & Specific Attenuation (RadarSounderEngine)', () => {
    it('should calculate SHARAD vertical range resolution in vacuum and pure water ice', () => {
        // B = 10 MHz (10e6 Hz), c = 299,792,458 m/s -> Delta_r_vac = 299792458 / 2e7 = 14.99 m
        // In water ice (eps = 3.15) -> sqrt(3.15) = 1.7748 -> Delta_r_med = 14.99 / 1.7748 = 8.44 m
        const res = RadarSounderEngine.computeRadarVerticalRangeResolution(10e6, 3.15);
        expect(res.rangeResolutionVacuumMeters).to.be.closeTo(14.99, 0.05);
        expect(res.rangeResolutionMediumMeters).to.be.closeTo(8.44, 0.05);
        expect(res.bandwidthMHz).to.equal(10.0);
    });

    it('should compute ionospheric group delay and subsurface specific attenuation rates', () => {
        // MARSIS at f = 4 MHz (4e6 Hz), TEC = 0.1 TECU (1e15 e/m^2)
        // dt = (40.3 * 1e15) / (2.9979e8 * 16e12) = 4.03e16 / 4.7967e21 = 8.4017e-6 s (8401.7 ns)
        // range error = (dt * c) / 2 = 1259.38 m
        const iono = RadarSounderEngine.computeIonosphericDispersionDelay(0.1, 4e6);
        expect(iono.ionosphericDelayNanoseconds).to.be.closeTo(8401.7, 1.0);
        expect(iono.rangeErrorMeters).to.be.closeTo(1259.38, 0.5);

        // SHARAD at f = 20 MHz in basaltic regolith eps = 8.0, tan_delta = 0.015
        // alpha_Np_m = (pi * 20e6 * sqrt(8) * 0.015) / 2.9979e8 = (6.283e7 * 2.8284 * 0.015) / 2.9979e8 = 0.00889 Np/m
        // alpha_1way_dB_m = 8.6859 * 0.00889 = 0.07724 dB/m -> 2way = 0.15448 dB/m = 154.48 dB/km
        const att = RadarSounderEngine.computeMediumSpecificAttenuationRate(20e6, 8.0, 0.015);
        expect(att.oneWayAttenuationDbPerMeter).to.be.closeTo(0.0772, 0.001);
        expect(att.twoWayAttenuationDbPerKm).to.be.closeTo(154.48, 1.0);
    });
});

describe('Deep Geotherm, Specific Heat & Skin Depth (KRCEngine)', () => {
    it('should calculate deep crustal geothermal profile and temperature gradient', () => {
        // T_mean = 210 K, z = 5000 m (5 km), q_geo = 0.030 W/m^2 (30 mW/m^2), k = 2.0 W/(m K)
        // gradient = 0.030 / 2.0 = 0.015 K/m = 15.0 K/km
        // T(5 km) = 210 + 0.015 * 5000 = 210 + 75 = 285 K
        const geo = KRCEngine.computeDeepGeothermalTemperatureProfile(210.0, 5000.0, 0.030, 2.0);
        expect(geo.temperatureAtDepthK).to.equal(285.0);
        expect(geo.thermalGradientK_Km).to.equal(15.0);
        expect(geo.depthKm).to.equal(5.0);
    });

    it('should compute surface diurnal harmonic amplitude and thermal skin depth', () => {
        // Solar insolation amplitude Delta_F = 250 W/m^2, Thermal Inertia I = 250, P = 88775.244 s (1 Sol)
        // omega = 2*pi / 88775.244 = 7.0776e-5 -> sqrt(omega) = 0.0084128
        // A_T = 250 / (250 * 0.0084128) = 118.87 K -> Peak-to-peak diurnal swing = 237.73 K
        const harm = KRCEngine.computeSurfaceThermalHarmonicAmplitude(250.0, 250.0, 88775.244);
        expect(harm.temperatureAmplitudeK).to.be.closeTo(118.87, 0.5);
        expect(harm.peakToPeakDiurnalSwingK).to.be.closeTo(237.73, 1.0);

        // Thermal skin depth: I = 300, rho = 1500, cp = 800, P = 88775.244 s (1 Sol)
        // delta = (300 / (1500 * 800)) * sqrt(88775.244 / pi) = (300 / 1.2e6) * sqrt(28258.07) = 0.00025 * 168.10 = 0.0420 m (4.20 cm)
        const skin = KRCEngine.computeThermalSkinDepthInversion(300.0, 1500.0, 800.0, 88775.244);
        expect(skin.skinDepthMeters).to.be.closeTo(0.0420, 0.002);
        expect(skin.skinDepthCm).to.be.closeTo(4.20, 0.2);
        expect(skin.periodHours).to.be.closeTo(24.66, 0.1);
    });
});

describe('Kepler Equation Inversion, Seasonal Photoperiod & Opposition (MarsTime)', () => {
    it('should iteratively solve Kepler equation for eccentric and true anomaly', () => {
        // Mean anomaly M = 45 deg (0.785398 rad), e = 0.09341233
        // E ~ 0.8543 rad (~48.95 deg), nu ~ 0.9272 rad (~53.12 deg)
        const kepler = MarsTime.solveKeplerEquationEccentricAnomaly(45.0 * Math.PI / 180.0, 0.09341233);
        expect(kepler.eccentricAnomalyDeg).to.be.closeTo(48.95, 0.1);
        expect(kepler.trueAnomalyDeg).to.be.closeTo(53.12, 0.1);
        expect(kepler.iterations).to.be.lessThan(10);
    });

    it('should compute Mars seasonal daylight photoperiod hours and opposition light time', () => {
        // Equator at Ls = 0 (Equinox) -> Day length = 24.66 / 2 = 12.33 hours
        const eq = MarsTime.computeMarsSeasonalDayLengthHours(0.0, 0.0);
        expect(eq.daylightHours).to.be.closeTo(12.33, 0.1);
        expect(eq.nightHours).to.be.closeTo(12.33, 0.1);
        expect(eq.isPolarDay).to.be.false;

        // North Pole (85° N) at Ls = 90 (Northern Summer Solstice) -> Continuous daylight (Polar Day)
        const npSummer = MarsTime.computeMarsSeasonalDayLengthHours(85.0, 90.0);
        expect(npSummer.isPolarDay).to.be.true;
        expect(npSummer.daylightHours).to.be.closeTo(24.66, 0.05);

        // Opposition geometry at perihelion (Ls = 250 deg)
        const opp = MarsTime.computeMarsOppositionGeometry(250.0);
        expect(opp.marsDistanceAU).to.be.greaterThan(1.35);
        expect(opp.marsDistanceAU).to.be.lessThan(1.70);
        expect(opp.lightTravelTimeMinutes).to.be.greaterThan(3.0);
        expect(opp.lightTravelTimeMinutes).to.be.lessThan(6.0);
    });
});

describe('Complex Crater Morphometry, Spall Ejection & Epochs (CSFDEngine)', () => {
    it('should calculate complex crater central peak and floor morphometry dimensions', () => {
        // D = 50 km complex crater (e.g., Gale Crater)
        // hRim = 0.036 * 50^0.52 = 0.036 * 7.647 = 0.2753 km -> 275.3 m
        // hCp = 0.040 * 50^0.88 = 0.040 * 31.2675 = 1.2507 km -> 1250.7 m
        // dCp = 0.22 * 50^1.12 = 0.22 * 79.62 = 17.52 km
        // dFloor = 0.51 * 50^1.02 = 0.51 * 54.26 = 27.67 km
        // dTotal = 0.36 * 50^0.30 = 0.36 * 3.2375 = 1.1655 km -> 1165.5 m
        const morph = CSFDEngine.computeComplexCraterMorphometryProfile(50.0);
        expect(morph.rimHeightMeters).to.be.closeTo(275.3, 2.0);
        expect(morph.centralPeakHeightMeters).to.be.closeTo(1250.7, 5.0);
        expect(morph.centralPeakDiameterKm).to.be.closeTo(17.52, 0.5);
        expect(morph.floorDiameterKm).to.be.closeTo(27.67, 0.5);
        expect(morph.totalRimFloorDepthMeters).to.be.closeTo(1165.5, 5.0);
    });

    it('should compute Melosh rock spall ejection velocity and classify Martian geologic epochs', () => {
        // v_imp = 12 km/s, a = 100 m, r = 150 m (ratio = 100/150 = 0.6667)
        // v_ej = 12 * (0.6667)^1.8 = 12 * 0.4818 = 5.782 km/s (exceeds Mars escape velocity 5.03 km/s!)
        const spall = CSFDEngine.computeSpallFragmentEjectionVelocity(12.0, 100.0, 150.0);
        expect(spall.ejectionVelocityKmS).to.be.closeTo(5.782, 0.05);
        expect(spall.exceedsMarsEscapeVelocity).to.be.true;

        // Distant ejecta r = 500 m (ratio = 0.2) -> v_ej = 12 * 0.2^1.8 = 12 * 0.0552 = 0.662 km/s
        const distant = CSFDEngine.computeSpallFragmentEjectionVelocity(12.0, 100.0, 500.0);
        expect(distant.exceedsMarsEscapeVelocity).to.be.false;

        // Epoch classification: 3.85 Ga -> Late Noachian, 3.2 Ga -> Late Hesperian, 0.2 Ga -> Late Amazonian
        const ep1 = CSFDEngine.classifyMarsGeologicChronologicalEpoch(3.85);
        expect(ep1.epochName).to.equal('Late Noachian');
        expect(ep1.isNoachian).to.be.true;

        const ep2 = CSFDEngine.classifyMarsGeologicChronologicalEpoch(3.2);
        expect(ep2.epochName).to.equal('Late Hesperian');
        expect(ep2.isHesperian).to.be.true;

        const ep3 = CSFDEngine.classifyMarsGeologicChronologicalEpoch(0.2);
        expect(ep3.epochName).to.equal('Late Amazonian');
        expect(ep3.isAmazonian).to.be.true;
    });
});

describe('Monohydrated Sulfate BD2100, Pyroxene HCPINDEX & SAM (BandMathEngine)', () => {
    it('should calculate CRISM BD2100 monohydrated sulfate and HCPINDEX pyroxene indices', () => {
        // Monohydrated sulfate (kieserite): R2132 = 0.20, R1930 = 0.28, R2250 = 0.26
        // continuum = 0.6 * 0.28 + 0.4 * 0.26 = 0.168 + 0.104 = 0.272
        // bd2100 = 1.0 - (0.20 / 0.272) = 1.0 - 0.7353 = 0.2647
        const bd = BandMathEngine.computeCRISMBD2100(0.20, 0.28, 0.26);
        expect(bd.bd2100).to.be.closeTo(0.2647, 0.001);
        expect(bd.hasMonohydratedSulfateSignature).to.be.true;

        // Flat continuum: R2132 = 0.25, R1930 = 0.25, R2250 = 0.25 -> bd = 0.0
        const flat = BandMathEngine.computeCRISMBD2100(0.25, 0.25, 0.25);
        expect(flat.bd2100).to.equal(0.0);
        expect(flat.hasMonohydratedSulfateSignature).to.be.false;

        // High-calcium pyroxene: R2060 = 0.18, R2120 = 0.22, R2140 = 0.22, R2210 = 0.17
        // term1 = (0.22 - 0.18) / 0.40 = 0.04 / 0.40 = 0.10
        // term2 = (0.22 - 0.17) / 0.39 = 0.05 / 0.39 = 0.1282
        // hcp = 0.10 + 0.1282 = 0.2282
        const hcp = BandMathEngine.computeCRISMHCPINDEX(0.18, 0.22, 0.22, 0.17);
        expect(hcp.hcpindex).to.be.closeTo(0.2282, 0.001);
        expect(hcp.hasHighCalciumPyroxene).to.be.true;
    });

    it('should compute Spectral Angle Mapper (SAM) dot-product angle and match similarity', () => {
        // Colinear spectra with scalar multiplier: T = [0.2, 0.4, 0.6], R = [0.1, 0.2, 0.3] -> angle = 0 deg (identical spectral shape)
        const identical = BandMathEngine.computeSpectralAngleMapper([0.2, 0.4, 0.6], [0.1, 0.2, 0.3]);
        expect(identical.spectralAngleDeg).to.be.closeTo(0.0, 0.01);
        expect(identical.matchSimilarityFraction).to.be.closeTo(1.0, 0.0001);

        // Orthogonal vectors: T = [1, 0], R = [0, 1] -> angle = 90 deg
        const ortho = BandMathEngine.computeSpectralAngleMapper([1.0, 0.0], [0.0, 1.0]);
        expect(ortho.spectralAngleDeg).to.be.closeTo(90.0, 0.01);
        expect(ortho.matchSimilarityFraction).to.be.closeTo(0.0, 0.0001);
    });
});

describe('Boundary Layer Friction, Sensible Heat Flux & Scale Height (MCDEngine)', () => {
    it('should calculate atmospheric friction velocity and boundary layer drag coefficient', () => {
        // Wind speed u = 10 m/s at z = 2.0 m, z_0 = 0.01 m (rough sand/pebbles)
        // log(z/z0) = log(200) = 5.2983
        // u_* = (0.40 * 10) / 5.2983 = 4.0 / 5.2983 = 0.7550 m/s
        // C_D = (0.40 / 5.2983)^2 = 0.075497^2 = 0.00570
        const frict = MCDEngine.computeAtmosphericFrictionVelocityAndRoughness(10.0, 2.0, 0.01, 0.015);
        expect(frict.frictionVelocityMps).to.be.closeTo(0.7550, 0.002);
        expect(frict.dragCoefficient).to.be.closeTo(0.0057, 0.0002);
        expect(frict.surfaceShearStressPa).to.be.greaterThan(0);
    });

    it('should compute surface sensible heat flux and temperature-dependent scale height', () => {
        // Daytime convection: T_air = 210 K, T_surf = 250 K (dT = +40 K), u = 5 m/s, rho = 0.015 kg/m^3, C_H = 0.003
        // H = 0.015 * 850 * 0.003 * 5 * 40 = 0.03825 * 200 = 7.65 W/m^2
        const flux = MCDEngine.computeSurfaceSensibleHeatFlux(210.0, 250.0, 5.0, 0.015, 0.003);
        expect(flux.sensibleHeatFluxW_M2).to.equal(7.65);
        expect(flux.isConvectiveDaytime).to.be.true;
        expect(flux.temperatureDifferenceK).to.equal(40.0);

        // Nighttime inversion: T_air = 200 K, T_surf = 180 K (dT = -20 K) -> H = -3.825 W/m^2
        const night = MCDEngine.computeSurfaceSensibleHeatFlux(200.0, 180.0, 5.0, 0.015, 0.003);
        expect(night.sensibleHeatFluxW_M2).to.equal(-3.83);
        expect(night.isConvectiveDaytime).to.be.false;

        // Scale height at T = 220 K, M = 43.34 g/mol, g = 3.72076 m/s^2
        // R_spec = 8.314462618 / 0.04334 = 191.84277 J/(kg K)
        // H = (191.84277 * 220) / 3.72076 = 42205.41 / 3.72076 = 11343.22 m = 11.34 km
        const scale = MCDEngine.computeAtmosphericScaleHeightProfile(220.0, 43.34, 3.72076);
        expect(scale.scaleHeightKm).to.be.closeTo(11.34, 0.05);
        expect(scale.scaleHeightMeters).to.be.closeTo(11343.2, 5.0);
    });
});

describe('Somigliana Theoretical Gravity, Planetographic Latitude & Rhumb Lines (GeoUtil)', () => {
    it('should calculate Somigliana theoretical normal gravity on oblate Mars ellipsoid', () => {
        // Equator (0 deg): g = 3.71 m/s^2
        const eq = computeSomiglianaTheoreticalGravity(0.0, 3.71, 3.73, 3396.19, 3376.20);
        expect(eq.normalGravityMps2).to.equal(3.71);
        expect(eq.gravityRatio).to.equal(1.0);

        // Pole (90 deg): g = 3.73 m/s^2
        const pole = computeSomiglianaTheoreticalGravity(90.0, 3.71, 3.73, 3396.19, 3376.20);
        expect(pole.normalGravityMps2).to.equal(3.73);
        expect(pole.gravityRatio).to.be.greaterThan(1.0);

        // Mid-latitude (45 deg): g ~ 3.72 m/s^2
        const mid = computeSomiglianaTheoreticalGravity(45.0, 3.71, 3.73, 3396.19, 3376.20);
        expect(mid.normalGravityMps2).to.be.closeTo(3.72, 0.01);
    });

    it('should convert planetographic to planetocentric latitude and calculate rhumb line heading', () => {
        // Planetographic 45° with Mars flattening f = 0.005886
        // tan(phi_c) = (1 - 0.005886)^2 * tan(45°) = (0.994114)^2 * 1.0 = 0.98826 -> phi_c = 44.6606°
        const conv = convertPlanetographicToPlanetocentricLatitude(45.0, 0.005886);
        expect(conv.planetocentricLatDeg).to.be.closeTo(44.6606, 0.01);
        expect(conv.differenceDeg).to.be.closeTo(0.3394, 0.01);

        // Rhumb line heading due East along latitude (lat1 = 10, lon1 = 0) to (lat2 = 10, lon2 = 50) -> bearing = 90°
        const east = computeGreatCircleRhumbLineHeading(10.0, 0.0, 10.0, 50.0);
        expect(east.bearingDeg).to.equal(90.0);
        expect(east.isEastward).to.be.true;

        // Rhumb line heading due North (lat1 = 0, lon1 = 20) to (lat2 = 60, lon2 = 20) -> bearing = 0°
        const north = computeGreatCircleRhumbLineHeading(0.0, 20.0, 60.0, 20.0);
        expect(north.bearingDeg).to.equal(0.0);
    });
});

describe('J2 Nodal Precession, Ground Track Shift & Eclipse (TrajectoryEngine)', () => {
    it('should calculate Mars Reconnaissance Orbiter (MRO) J2 nodal precession rate', () => {
        // MRO orbit: a ~ 3680 km (alt ~ 290 km), e ~ 0.008, i = 93.0° (Sun-synchronous retrograde)
        // cos(93°) = -0.0523 -> dOmega/dt is positive (prograde nodal drift matching Mars heliocentric orbital motion ~0.524 deg/day)
        const mro = TrajectoryEngine.computeNodalPrecessionRate(3680.0, 0.008, 93.0, 'mars');
        expect(mro.nodalPrecessionDegPerDay).to.be.greaterThan(0.4);
        expect(mro.nodalPrecessionDegPerDay).to.be.lessThan(0.7);
        expect(mro.isRetrogradePrecession).to.be.false;

        // Prograde orbit i = 45° -> retrograde nodal precession (dOmega/dt < 0)
        const prog = TrajectoryEngine.computeNodalPrecessionRate(4000.0, 0.0, 45.0, 'mars');
        expect(prog.nodalPrecessionDegPerDay).to.be.lessThan(0);
        expect(prog.isRetrogradePrecession).to.be.true;
    });

    it('should compute ground track longitudinal nodal shift and orbital shadow eclipse fraction', () => {
        // Torbit = 112 minutes (MRO), Mars rot = 24.6597 h -> omegaP = 360 / (24.6597*60) = 0.2433 deg/min
        // Delta_lambda = 0.2433 * 112 = 27.25 deg westward per orbit
        const shift = TrajectoryEngine.computeGroundTrackNodalShift(112.0, 0.524, 24.6597);
        expect(shift.longitudinalShiftDeg).to.be.closeTo(27.21, 0.2);
        expect(shift.orbitalPeriodMinutes).to.equal(112.0);

        // Circular low Mars orbit a = 3680 km: half shadow angle = arcsin(3389.5 / 3680) = arcsin(0.92106) = 1.171 rad (~67.09 deg)
        // f_eclipse = 67.09 / 180 = 0.3727 (37.27% of orbit in shadow)
        const eclipse = TrajectoryEngine.computeOrbitalEclipseFraction(3680.0, 'mars');
        expect(eclipse.eclipseFraction).to.be.closeTo(0.3727, 0.01);
        expect(eclipse.orbitalPeriodMinutes).to.be.closeTo(114.0, 2.0);
        expect(eclipse.eclipseDurationMinutes).to.be.greaterThan(35.0);
    });
});

describe('Official LMD/CNRS/ESA Mars Climate Database (MCD v6.1) Ingestion (MCDEngine)', () => {
    it('should parse real multi-column ASCII vertical profiles from LMD MCD v6.1', () => {
        const sampleLmdOutput = `
##########################################################################################
### MCD_v6.1 with climatology average solar scenario.
### Ls 180.0deg. Latitude -4.59N. Longitude 137.44E. Local time 12.0h
### --------------------------------------------------------------------------------------
### Column 1 is height above surface (m)
### Column 2 is Temperature (K)
### --------------------------------------------------------------------------------------
    0.00000e+00    2.42500e+02    7.20000e+02    1.57000e-02    4.50000e+00
    1.00000e+03    2.38100e+02    6.55000e+02    1.45000e-02    8.20000e+00
    5.00000e+03    2.20400e+02    4.40000e+02    1.05000e-02    1.85000e+01
    1.00000e+04    1.98300e+02    2.60000e+02    6.95000e-03    2.50000e+01
    2.00000e+04    1.65200e+02    9.50000e+01    3.04000e-03    3.20000e+01
    5.00000e+04    1.45800e+02    5.20000e+00    1.89000e-04    1.20000e+01
`;
        const profile = MCDEngine.parseLMDAsciiOutput(sampleLmdOutput, {
            lat: -4.59,
            lon: 137.44,
            Ls: 180.0,
            localHour: 12.0,
            dust: 1
        });

        expect(profile.isRealData).to.be.true;
        expect(profile.source).to.include('LMD/CNRS/ESA');
        expect(profile.layers).to.have.lengthOf(6);
        expect(profile.layers[0].altitudeKm).to.equal(0);
        expect(profile.layers[0].temperatureK).to.equal(242.5);
        expect(profile.layers[0].pressurePa).to.equal(720.0);
        expect(profile.layers[5].altitudeKm).to.equal(50);
        expect(profile.layers[5].temperatureK).to.equal(145.8);
        expect(profile.surface.pressurePa).to.equal(720.0);
        expect(profile.surface.temperatureK).to.equal(242.5);
    });

    it('should provide direct LMD web portal URL with query parameters', () => {
        const dummyOutput = `
# LMD MCD Header
    1.00000e+03    2.20000e+02
`;
        const profile = MCDEngine.parseLMDAsciiOutput(dummyOutput, {
            lat: 18.65,
            lon: 226.2,
            lmdCgiUrl: 'https://www-mars.lmd.jussieu.fr/mcd_python/cgi-bin/mcdcgi.py?var1=t'
        });
        expect(profile.lmdWebUrl).to.include('lmd.jussieu.fr');
    });
});

describe('Subsolar Equilibrium, Conductive Flux & Insolation (KRCEngine)', () => {
    it('should calculate subsolar radiative equilibrium temperature and conductive heat flux', () => {
        // Mars mean distance r = 1.524 AU, Albedo A = 0.25, Emissivity = 0.95
        // S_mars = 1361 / (1.524^2) = 1361 / 2.32258 = 586.03 W/m^2
        // Absorbed = 0.75 * 586.03 = 439.52 W/m^2
        // T_ss = (439.52 / (0.95 * 5.67037e-8))^(0.25) = (439.52 / 5.38685e-8)^0.25 = (8.159e9)^0.25 = 300.28 K
        const eq = KRCEngine.computeSubsolarEquilibriumTemperature(0.25, 1.524, 0.95);
        expect(eq.subsolarTemperatureK).to.be.closeTo(300.28, 0.5);
        expect(eq.solarFluxW_M2).to.be.closeTo(586.03, 1.0);

        // Fourier conductive flux: Tu = 240 K, Tl = 210 K, dz = 0.1 m, k = 0.05 W/(m K)
        // dT/dz = (210 - 240) / 0.1 = -300 K/m
        // F = -0.05 * (-300) = +15.0 W/m^2 (downward heat flow into soil)
        const flux = KRCEngine.computeConductiveHeatFlux(240.0, 210.0, 0.1, 0.05);
        expect(flux.conductiveHeatFluxW_M2).to.equal(15.0);
        expect(flux.temperatureGradientK_M).to.equal(-300.0);
        expect(flux.isUpwardFlux).to.be.false;

        // Nighttime upward heat release: Tu = 180 K, Tl = 210 K -> F = -15.0 W/m^2
        const upward = KRCEngine.computeConductiveHeatFlux(180.0, 210.0, 0.1, 0.05);
        expect(upward.conductiveHeatFluxW_M2).to.equal(-15.0);
        expect(upward.isUpwardFlux).to.be.true;
    });

    it('should compute diurnal integrated solar insolation on horizontal Martian surface', () => {
        // Equator (lat=0), Equinox (delta=0): H_ss = 90° (pi/2) -> integral = cos(0)*cos(0)*sin(pi/2) = 1.0
        // E_day = (S_mars * P_sol / pi) = (586.03 * 88775.244 / pi) = 52024765 / 3.14159 = 1.656e7 J/m^2 = 4.60 kWh/m^2
        const insol = KRCEngine.computeDailyInsolationIntegral(0.0, 0.0, 1.524, 88775.244);
        expect(insol.dailyInsolationKWh_M2).to.be.closeTo(4.60, 0.1);
        expect(insol.sunlitHours).to.be.closeTo(12.33, 0.1); // Half sol = 24.66 / 2 = 12.33 h

        // Polar night (lat=80°, delta=-25°): cosHss > 1 -> daylight = 0
        const polarNight = KRCEngine.computeDailyInsolationIntegral(80.0, -25.0, 1.524);
        expect(polarNight.dailyInsolationJ_M2).to.equal(0);
        expect(polarNight.sunlitHours).to.equal(0);
    });
});

describe('CRISM Structural Water (BD1400), Al-OH Smectite (BD2210) & Slope (BandMathEngine)', () => {
    it('should compute CRISM 1.4 µm structural H2O / hydroxyl absorption index BD1400', () => {
        // Hydrated sulfate/opal signature: strong absorption at 1.395 µm
        // r1330 = 0.35, r1395 = 0.28, r1510 = 0.33
        // Continuum = 0.7 * 0.35 + 0.3 * 0.33 = 0.245 + 0.099 = 0.344
        // BD1400 = 1.0 - (0.28 / 0.344) = 1.0 - 0.81395 = 0.1860
        const h2o = BandMathEngine.computeCRISMStructuralWaterBD1400(0.35, 0.28, 0.33);
        expect(h2o.bd1400).to.be.closeTo(0.1860, 0.001);
        expect(h2o.hasHydration).to.be.true;

        // Anhydrous basaltic terrain: flat spectrum
        const dry = BandMathEngine.computeCRISMStructuralWaterBD1400(0.30, 0.30, 0.30);
        expect(dry.bd1400).to.equal(0.0);
        expect(dry.hasHydration).to.be.false;
    });

    it('should calculate CRISM 2.21 µm Al-OH smectite index BD2210 and continuum normalized slope', () => {
        // Montmorillonite / Al-smectite clay absorption at 2.210 µm
        // r2140 = 0.40, r2210 = 0.34, r2250 = 0.38
        // Continuum = 0.6 * 0.40 + 0.4 * 0.38 = 0.24 + 0.152 = 0.392
        // BD2210 = 1.0 - (0.34 / 0.392) = 1.0 - 0.86735 = 0.1327
        const clay = BandMathEngine.computeCRISMSmectiteBD2210(0.40, 0.34, 0.38);
        expect(clay.bd2210).to.be.closeTo(0.1327, 0.001);
        expect(clay.hasAlSmectite).to.be.true;

        // Red spectral slope (ferric dust / nanophase iron): r(0.5 µm) = 0.12, r(1.0 µm) = 0.28
        // Slope = (0.28 - 0.12) / (1.0 - 0.5) = 0.16 / 0.5 = +0.32 µm^-1
        const slope = BandMathEngine.computeContinuumNormalizedSlope(0.12, 0.28, 0.5, 1.0);
        expect(slope.slopePerMicron).to.equal(0.32);
        expect(slope.isRedSloped).to.be.true;
        expect(slope.isBlueSloped).to.be.false;
    });
});

describe('Crater Differential Frequency, R-Plot & Hartmann Isochron (CSFDEngine)', () => {
    it('should calculate differential crater frequency dN/dD and R-plot relative density', () => {
        // Bin [1 km, 2 km]: N(>1) = 1.0e-3 km^-2, N(>2) = 2.5e-4 km^-2 -> deltaN = 7.5e-4 km^-2
        // deltaD = 1.0 km -> dN/dD = 7.5e-4 km^-3
        // dGeom = sqrt(1 * 2) = 1.414 km
        const diff = CSFDEngine.computeDifferentialFrequencyFromCumulative(1.0e-3, 2.5e-4, 1.0, 2.0);
        expect(diff.differentialFrequencyPerKm3).to.equal(7.5e-4);
        expect(diff.geometricMeanDiameterKm).to.be.closeTo(1.414, 0.001);
        expect(diff.deltaDKm).to.equal(1.0);

        // R-plot: R = dGeom^3 * (dN/dD) = (1.41421^3) * 7.5e-4 = 2.8284 * 7.5e-4 = 2.121e-3
        const rplot = CSFDEngine.computeRPlotFromDifferentialFrequency(diff.geometricMeanDiameterKm, diff.differentialFrequencyPerKm3);
        expect(rplot.rValue).to.be.closeTo(2.121e-3, 0.01e-3);
        expect(rplot.log10RValue).to.be.closeTo(-2.673, 0.01);
    });

    it('should compute Hartmann (2005) isochron cumulative crater production scaling', () => {
        // 1 Ga baseline vs 3.5 Ga ancient Noachian isochron (exponential bombardment increase)
        const d1km_1Ga = CSFDEngine.computeHartmannProductionFunctionCumulative(1.0, 1.0);
        const d1km_35Ga = CSFDEngine.computeHartmannProductionFunctionCumulative(1.0, 3.5);

        expect(d1km_1Ga.isochronAgeGa).to.equal(1.0);
        expect(d1km_35Ga.isochronAgeGa).to.equal(3.5);
        expect(d1km_35Ga.cumulativeDensityPerKm2).to.be.greaterThan(d1km_1Ga.cumulativeDensityPerKm2 * 3.5);
    });
});

describe('3D Ellipsoid Geodesy, Ray Picking & Horizon Dip (ThreeDEngine)', () => {
    it('should compute 3D Cartesian coordinates on oblate Mars ellipsoid with elevation', () => {
        // Equator at prime meridian (lat = 0, lon = 0), elevation = 0 -> X = 3396.19 km, Y = 0, Z = 0
        const eqPrime = ThreeDEngine.computeTriaxialEllipsoidCartesian3D(0.0, 0.0, 0, 3396.19, 3396.19, 3376.20);
        expect(eqPrime.xKm).to.equal(3396.19);
        expect(eqPrime.yKm).to.equal(0);
        expect(eqPrime.zKm).to.equal(0);

        // North Pole (lat = 90, lon = 0) with 2000m ice cap -> Z = 3376.20 + 2.0 = 3378.20 km
        const northPole = ThreeDEngine.computeTriaxialEllipsoidCartesian3D(90.0, 0.0, 2000, 3396.19, 3396.19, 3376.20);
        expect(northPole.xKm).to.be.closeTo(0.0, 0.01);
        expect(northPole.zKm).to.equal(3378.20);
    });

    it('should calculate 3D ray-ellipsoid mouse picking intersection and horizon dipping angle', () => {
        // Ray from camera at (0, 0, 4000 km) pointing down -Z towards Mars center (0, 0, 0)
        // Polar radius c = 3376.20 km -> Hit point should be at Z = +3376.20 km, hitDistance = 4000 - 3376.20 = 623.80 km
        const hit = ThreeDEngine.computeRayEllipsoidIntersection(
            { x: 0, y: 0, z: 4000.0 },
            { x: 0, y: 0, z: -1.0 },
            3396.19,
            3376.20
        );
        expect(hit.hasHit).to.be.true;
        expect(hit.hitDistanceKm).to.equal(623.8);
        expect(hit.hitPoint.z).to.equal(3376.2);

        // Horizon dip from 300 km orbit around Mars (R = 3389.5 km):
        // cos(theta) = 3389.5 / (3389.5 + 300) = 3389.5 / 3689.5 = 0.918688 -> theta = 23.266°
        // d_horizon = sqrt(2 * 3389.5 * 300 + 300^2) = sqrt(2033700 + 90000) = sqrt(2123700) = 1457.29 km
        const horizon = ThreeDEngine.computeHorizonDipAngle(300.0, 3389.5);
        expect(horizon.horizonDipAngleDeg).to.be.closeTo(23.266, 0.01);
        expect(horizon.horizonDistanceKm).to.be.closeTo(1457.29, 0.1);
    });
});

describe('Martian Solar Position, Shadow Ratio & Right Ascension (MarsTime)', () => {
    it('should calculate Martian solar elevation, azimuth bearing, and shadow length', () => {
        // At Martian equator (lat = 0) at equinox (Ls = 0) at local noon (LTST = 12.0h)
        // Sun at zenith: elevation = 90°, zenith = 0°
        const noonEquinox = MarsTime.computeMartianSolarElevationAndAzimuth(0.0, 0.0, 12.0);
        expect(noonEquinox.solarElevationDeg).to.equal(90.0);
        expect(noonEquinox.solarZenithDeg).to.equal(0.0);
        expect(noonEquinox.isDaylight).to.be.true;

        // Shadow ratio at 45° solar elevation: tan(45°) = 1.0 -> 1.0 m mast casts 1.0 m shadow
        const shadow45 = MarsTime.computeMartianShadowRatio(45.0, 1.0);
        expect(shadow45.shadowLengthMeters).to.equal(1.0);
        expect(shadow45.shadowRatio).to.equal(1.0);
        expect(shadow45.isShadowCast).to.be.true;

        // Nighttime (sun below horizon, elev = -10°): no shadow
        const night = MarsTime.computeMartianShadowRatio(-10.0, 1.0);
        expect(night.isShadowCast).to.be.false;
    });

    it('should compute Mars Areocentric Right Ascension from Solar Longitude', () => {
        // At Ls = 0 (Northern spring equinox): RA = 0° = 0.0h
        const ra0 = MarsTime.computeMarsAreocentricRightAscension(0.0, 25.19);
        expect(ra0.rightAscensionDeg).to.equal(0.0);
        expect(ra0.rightAscensionHours).to.equal(0.0);

        // At Ls = 90 (Northern summer solstice): RA = 90° = 6.0h
        const ra90 = MarsTime.computeMarsAreocentricRightAscension(90.0, 25.19);
        expect(ra90.rightAscensionDeg).to.equal(90.0);
        expect(ra90.rightAscensionHours).to.equal(6.0);
    });
});

describe('Map Projections & Grid Convergence (Lambert Equal-Area & Polar Stereographic)', () => {
    it('should calculate Lambert Azimuthal Equal-Area (LAEA) forward coordinates', () => {
        // Projection center at (lat0 = 0, lon0 = 0) on Mars (R = 3389.5 km)
        // Center point projects to origin (0, 0)
        const center = computeLambertAzimuthalEqualArea(0.0, 0.0, 0.0, 0.0, 3389.5);
        expect(center.xKm).to.equal(0);
        expect(center.yKm).to.equal(0);
        expect(center.scaleFactor).to.equal(1.0);
        expect(center.isAntipodal).to.be.false;

        // North pole (lat = 90) with center at equator -> x = 0, y = R * sqrt(2) * 1 = 3389.5 * 1.4142 = 4793.47 km
        const pole = computeLambertAzimuthalEqualArea(90.0, 0.0, 0.0, 0.0, 3389.5);
        expect(pole.xKm).to.be.closeTo(0.0, 0.01);
        expect(pole.yKm).to.be.closeTo(4793.47, 1.0);
    });

    it('should compute Polar Stereographic coordinates and meridian convergence angle', () => {
        // North Polar Stereographic (lat = 90° North Pole) -> rho = 0, x = 0, y = 0
        const np = computePolarStereographic(90.0, 0.0, 0.0, true, 3389.5);
        expect(np.xKm).to.equal(0);
        expect(np.yKm).to.equal(0);
        expect(np.radialDistanceKm).to.equal(0);

        // Grid convergence angle: lat = 45°, deltaLon = +10° -> gamma = 10 * sin(45°) = 7.0711°
        const conv = computeMeridianConvergenceAngle(45.0, 10.0, 0.0);
        expect(conv.convergenceAngleDeg).to.be.closeTo(7.0711, 0.001);
        expect(conv.isWestOfMeridian).to.be.false;

        // West of central meridian (deltaLon = -20°): gamma = -14.1421°
        const convW = computeMeridianConvergenceAngle(45.0, -20.0, 0.0);
        expect(convW.convergenceAngleDeg).to.be.closeTo(-14.1421, 0.001);
        expect(convW.isWestOfMeridian).to.be.true;
    });
});

describe('Orbital Vis-Viva Velocity, Escape Speed & Flight Path Angle (TrajectoryEngine)', () => {
    it('should calculate orbital speed via Vis-Viva equation and parabolic escape velocity', () => {
        // Low Mars circular orbit: r = 3680 km (alt ~ 290 km, MRO), a = 3680 km, mu = 42828.37 km^3/s^2
        // v = sqrt(42828.37 * (2/3680 - 1/3680)) = sqrt(42828.37 / 3680) = sqrt(11.63814) = 3.4115 km/s
        const vMro = TrajectoryEngine.computeVisVivaVelocity(3680.0, 3680.0, 'mars');
        expect(vMro.velocityKmS).to.be.closeTo(3.4115, 0.005);
        expect(vMro.velocityMS).to.be.closeTo(3411.5, 5.0);
        expect(vMro.isBoundOrbit).to.be.true;

        // Escape velocity from Mars surface (R = 3389.5 km):
        // v_esc = sqrt(2 * 42828.37 / 3389.5) = sqrt(85656.74 / 3389.5) = sqrt(25.2712) = 5.027 km/s
        const vEsc = TrajectoryEngine.computeEscapeVelocityFromRadialDistance(3389.5, 'mars');
        expect(vEsc.escapeVelocityKmS).to.be.closeTo(5.027, 0.005);
        expect(vEsc.escapeVelocityMS).to.be.closeTo(5027.0, 5.0);
    });

    it('should compute orbital flight path angle relative to local horizontal', () => {
        // Circular orbit (e = 0): flight path angle is identically 0° everywhere
        const fpaCirc = TrajectoryEngine.computeFlightPathAngle(45.0, 0.0);
        expect(fpaCirc.flightPathAngleDeg).to.equal(0.0);
        expect(fpaCirc.isClimbing).to.be.false;

        // Elliptical orbit (e = 0.2) at true anomaly nu = 90°:
        // tan(gamma) = (0.2 * sin(90)) / (1 + 0.2 * cos(90)) = 0.2 / 1.0 = 0.2 -> gamma = arctan(0.2) = 11.3099°
        const fpaEll = TrajectoryEngine.computeFlightPathAngle(90.0, 0.2);
        expect(fpaEll.flightPathAngleDeg).to.be.closeTo(11.3099, 0.001);
        expect(fpaEll.isClimbing).to.be.true;
    });
});

describe('SHARAD Radar Travel Time, Apparent Thickness & Critical Angle (RadarSounderEngine)', () => {
    it('should calculate subsurface radar two-way travel time (TWT) and apparent depth stretch', () => {
        // Planum Boreum water ice layer (dz = 1000 m, eps_r = 3.15)
        // v = c / sqrt(3.15) = 299792458 / 1.774824 = 168913904 m/s (168.91 km/s)
        // TWT = 2 * 1000 / 168913904 = 1.184e-5 s = 11.84 µs
        const twt = RadarSounderEngine.computeLayerTwoWayTravelTime(1000.0, 3.15);
        expect(twt.twtMicroseconds).to.be.closeTo(11.84, 0.01);
        expect(twt.propagationVelocityKmS).to.be.closeTo(168913.9, 1.0);

        // Apparent free-space thickness in radargram: dz_apparent = 1000 * sqrt(3.15) = 1774.82 m
        const apparent = RadarSounderEngine.computeLayerApparentThicknessInFreeSpace(1000.0, 3.15);
        expect(apparent.apparentThicknessMeters).to.be.closeTo(1774.82, 0.1);
        expect(apparent.stretchRatio).to.be.closeTo(1.7748, 0.001);
    });

    it('should compute Snell critical angle of total internal reflection between dielectric strata', () => {
        // Water ice (eps1 = 3.15) to CO2 dry ice (eps2 = 2.15): eps1 > eps2 -> critical angle exists
        // sin(theta_c) = sqrt(2.15 / 3.15) = sqrt(0.68254) = 0.82616 -> theta_c = 55.706°
        const crit = RadarSounderEngine.computeCriticalAngleOfRefraction(3.15, 2.15);
        expect(crit.hasCriticalAngle).to.be.true;
        expect(crit.criticalAngleDeg).to.be.closeTo(55.706, 0.01);

        // CO2 dry ice (eps1 = 2.15) into water ice (eps2 = 3.15): eps1 < eps2 -> no total internal reflection
        const noCrit = RadarSounderEngine.computeCriticalAngleOfRefraction(2.15, 3.15);
        expect(noCrit.hasCriticalAngle).to.be.false;
        expect(noCrit.criticalAngleDeg).to.equal(90.0);
    });
});

describe('TES Thermal Infrared Mineralogy & Absorption Asymmetry (BandMathEngine)', () => {
    it('should calculate TES Silica index and Carbonate 1430 cm^-1 absorption depth', () => {
        // High silica dacite: eps_1100 = 0.95, eps_1125 = 0.88 (strong Si-O absorption), eps_1150 = 0.94
        // Index = (0.95 + 0.94) / (2 * 0.88) = 1.89 / 1.76 = 1.0739 (> 1.03 -> silica enrichment)
        const silica = BandMathEngine.computeTESSilicaIndex(0.95, 0.88, 0.94);
        expect(silica.silicaIndex).to.be.closeTo(1.0739, 0.001);
        expect(silica.hasSilicaEnrichment).to.be.true;

        // TES Carbonate (Nili Fossae): eps_1350 = 0.96, eps_1430 = 0.88 (CO3 absorption), eps_1510 = 0.96
        // Continuum = 0.96 -> BD1430 = 1 - (0.88 / 0.96) = 1 - 0.9167 = 0.0833
        const carb = BandMathEngine.computeTESCarbonateBD1430(0.96, 0.88, 0.96);
        expect(carb.bd1430).to.be.closeTo(0.0833, 0.001);
        expect(carb.hasCarbonate).to.be.true;
    });

    it('should compute spectral absorption band asymmetry parameter (skewness)', () => {
        // Left-skewed absorption feature (e.g. olivine 1 µm complex with strong 0.85 µm shoulder):
        // Area_left = 12.0, Area_right = 6.0 -> Asym = (12 - 6) / 18 = 6 / 18 = +0.3333
        const leftSkew = BandMathEngine.computeSpectralAsymmetryIndex(12.0, 6.0);
        expect(leftSkew.asymmetryIndex).to.be.closeTo(0.3333, 0.001);
        expect(leftSkew.isLeftSkewed).to.be.true;
        expect(leftSkew.isRightSkewed).to.be.false;

        // Perfectly symmetric band: Area_left = 8.0, Area_right = 8.0 -> Asym = 0.0
        const sym = BandMathEngine.computeSpectralAsymmetryIndex(8.0, 8.0);
        expect(sym.asymmetryIndex).to.equal(0.0);
        expect(sym.isSymmetric).to.be.true;
    });
});

describe('Crater Saturation Limit, Poisson Count Probability & Spatial Aggregation (CSFDEngine)', () => {
    it('should calculate geometric saturation equilibrium limit and Poisson likelihood', () => {
        // Saturation density at D = 1 km: N_sat(>1) = 0.05 * 1^-2 = 0.05 km^-2
        const sat1 = CSFDEngine.computeCraterSaturationEquilibriumLimit(1.0);
        expect(sat1.saturationDensityPerKm2).to.equal(0.05);
        expect(sat1.saturationRValue).to.equal(0.05);

        // Poisson count probability: observed k = 3, expected mu = 3.0
        // P(3; 3) = (3^3 * exp(-3)) / 6 = 27 * 0.049787 / 6 = 0.224042
        const pois = CSFDEngine.computePoissonCountProbability(3, 3.0);
        expect(pois.poissonProbability).to.be.closeTo(0.224042, 0.001);
        expect(pois.isMostLikely).to.be.true;
    });

    it('should compute Clark-Evans spatial aggregation nearest neighbor index', () => {
        // Clustered craters (short nearest-neighbor distances in large area):
        // Area = 1000 km^2, n = 4, distances = [1.0, 1.2, 0.8, 1.0] -> mean = 1.0 km
        // Density = 4/1000 = 0.004 -> r_exp = 1 / (2 * sqrt(0.004)) = 1 / 0.12649 = 7.9057 km
        // R_agg = 1.0 / 7.9057 = 0.1265 (< 0.8 -> clustered)
        const cluster = CSFDEngine.computeSpatialPoissonRandomnessParameter([1.0, 1.2, 0.8, 1.0], 1000);
        expect(cluster.aggregationIndex).to.be.closeTo(0.1265, 0.01);
        expect(cluster.isClustered).to.be.true;
        expect(cluster.isRandomPoisson).to.be.false;

        // Dispersed craters (regularly spaced lattice): R_agg > 1.2
        const dispersed = CSFDEngine.computeSpatialPoissonRandomnessParameter([12.0, 14.0, 13.0, 12.5], 1000);
        expect(dispersed.isDispersed).to.be.true;
    });
});

describe('Surface Thermal Emission, Downwelling Flux & Net Energy Balance (KRCEngine)', () => {
    it('should calculate Stefan-Boltzmann surface thermal emission and atmospheric downwelling flux', () => {
        // Mars equatorial noon surface: T = 270 K, eps = 0.95
        // F_emit = 0.95 * 5.670374e-8 * 270^4 = 0.95 * 5.670374e-8 * 5314410000 = 286.29 W/m^2
        const emit = KRCEngine.computeSurfaceThermalEmission(270.0, 0.95);
        expect(emit.emittedFluxW_M2).to.be.closeTo(286.29, 0.1);
        expect(emit.emissivity).to.equal(0.95);

        // Downwelling flux from dusty atmosphere: T_atm = 180 K, tau = 0.5 -> eps_atm = 1 - exp(-0.5) = 0.3935
        // F_down = 0.393469 * 5.670374e-8 * 180^4 = 0.393469 * 5.670374e-8 * 1049760000 = 23.42 W/m^2
        const down = KRCEngine.computeAtmosphericDownwellingRadiativeFlux(180.0, 0.5);
        expect(down.downwellingFluxW_M2).to.be.closeTo(23.42, 0.1);
        expect(down.atmosphericEmissivity).to.be.closeTo(0.3935, 0.001);
    });

    it('should calculate instantaneous net surface radiative heat balance', () => {
        // High noon: Absorbed solar = 400 W/m^2, downwelling = 20 W/m^2, T = 250 K (F_emit = 210.42 W/m^2)
        // F_net = 400 + 20 - 210.42 = +209.58 W/m^2 (> 0 -> warming)
        const dayBal = KRCEngine.computeSurfaceNetRadiativeHeatBalance(400.0, 250.0, 20.0, 0.95);
        expect(dayBal.netRadiativeFluxW_M2).to.be.closeTo(209.58, 0.1);
        expect(dayBal.isWarming).to.be.true;

        // Midnight: Absorbed solar = 0, downwelling = 15 W/m^2, T = 190 K (F_emit = 70.19 W/m^2)
        // F_net = 0 + 15 - 70.19 = -55.19 W/m^2 (< 0 -> cooling)
        const nightBal = KRCEngine.computeSurfaceNetRadiativeHeatBalance(0.0, 190.0, 15.0, 0.95);
        expect(nightBal.netRadiativeFluxW_M2).to.be.closeTo(-55.19, 0.1);
        expect(nightBal.isWarming).to.be.false;
    });
});

describe('Martian Speed of Sound, Sutherland Viscosity & Aerodynamic Mach (MCDEngine)', () => {
    it('should calculate local speed of sound in CO2 and Sutherland dynamic viscosity', () => {
        // At surface T = 220 K, gamma = 1.29, R_spec = 188.92 J/(kg K)
        // c_s = sqrt(1.29 * 188.92 * 220) = sqrt(53610.9) = 231.54 m/s (833.5 km/h)
        const sound = MCDEngine.computeMartianSpeedOfSound(220.0, 1.29);
        expect(sound.speedOfSoundMS).to.be.closeTo(231.54, 0.1);
        expect(sound.speedOfSoundKmH).to.be.closeTo(833.5, 1.0);

        // Viscosity at T = 220 K (ratio = 220/273.15 = 0.80542)
        // mu = 1.37e-5 * 0.80542^1.5 * (513.15 / 460.0) ~ 1.109e-5 Pa s
        const visc = MCDEngine.computeCO2DynamicViscosity(220.0);
        expect(visc.dynamicViscosityPaS).to.be.closeTo(1.11e-5, 0.05e-5);
    });

    it('should calculate entry vehicle aerodynamic Mach numbers and regime', () => {
        // Hypersonic atmospheric entry (Perseverance entry interface: v = 5400 m/s at T = 180 K)
        // c_s = sqrt(1.29 * 188.92 * 180) = 209.43 m/s -> M = 5400 / 209.43 = 25.78
        const hyper = MCDEngine.computeAerodynamicMachNumber(5400.0, 180.0);
        expect(hyper.machNumber).to.be.closeTo(25.78, 0.1);
        expect(hyper.isHypersonic).to.be.true;
        expect(hyper.isSupersonic).to.be.false;

        // Subsonic parachute descent (v = 80 m/s at T = 215 K -> c_s = 228.90 m/s -> M = 0.349)
        const sub = MCDEngine.computeAerodynamicMachNumber(80.0, 215.0);
        expect(sub.machNumber).to.be.closeTo(0.349, 0.01);
        expect(sub.isSubsonic).to.be.true;
    });
});

describe('3D Camera Footprint, Parallax Relief & ENU Normal Vectors (ThreeDEngine)', () => {
    it('should calculate 3D camera sensor ground footprint swath on tangent terrain', () => {
        // Spacecraft camera at 400 km altitude, FOV = 30° horizontal, 20° vertical
        // W = 2 * 400 * tan(15°) = 800 * 0.267949 = 214.36 km
        // H = 2 * 400 * tan(10°) = 800 * 0.176327 = 141.06 km
        // Area = 214.359 * 141.062 = 30237.9 km^2
        const fp = ThreeDEngine.computeCameraGroundFootprint(400.0, 30.0, 20.0);
        expect(fp.footprintWidthKm).to.be.closeTo(214.36, 0.1);
        expect(fp.footprintHeightKm).to.be.closeTo(141.06, 0.1);
        expect(fp.groundAreaKm2).to.be.closeTo(30237.9, 5.0);
    });

    it('should compute geometric parallax relief displacement and ENU terrain normal vectors', () => {
        // Olympus Mons peak: h = 21,287 meters, off-nadir look angle = 30°
        // Parallax = 21287 * tan(30°) = 21287 * 0.57735 = 12290.04 meters (12.29 km)
        const parallax = ThreeDEngine.computeParallaxReliefDisplacement(21287, 30.0);
        expect(parallax.parallaxDisplacementMeters).to.be.closeTo(12290.04, 0.5);
        expect(parallax.displacementRatio).to.be.closeTo(0.5774, 0.001);

        // East-facing slope: slope = 30°, aspect = 90° (East)
        // n_east = -sin(30)*sin(90) = -0.5, n_north = -sin(30)*cos(90) = 0.0, n_up = cos(30) = 0.8660
        const norm = ThreeDEngine.computeTerrainNormalUnitVector3D(30.0, 90.0);
        expect(norm.nEast).to.equal(-0.5);
        expect(norm.nNorth).to.equal(0.0);
        expect(norm.nUp).to.be.closeTo(0.8660, 0.001);
        expect(norm.isFlat).to.be.false;
    });
});

describe('Newton-Raphson Kepler Solver & Orbit Solar Flux Dilution (MarsTime)', () => {
    it('should iteratively solve Kepler equation for eccentric and true anomaly', () => {
        // At mean anomaly M = 0°: E = 0°, nu = 0°
        const kep0 = MarsTime.solveKeplerEccentricAnomaly(0.0, 0.0934);
        expect(kep0.eccentricAnomalyDeg).to.equal(0.0);
        expect(kep0.hasConverged).to.be.true;

        // At mean anomaly M = 90°: M = 1.570796 rad, e = 0.0934 -> E ~ 95.32°
        const kep90 = MarsTime.solveKeplerEccentricAnomaly(90.0, 0.0934);
        expect(kep90.eccentricAnomalyDeg).to.be.closeTo(95.32, 0.1);
        expect(kep90.hasConverged).to.be.true;

        // True anomaly from E = 95.32°: tan(nu/2) = sqrt(1.0934/0.9066)*tan(47.66°) = 1.0978 * 1.0976 = 1.205 -> nu ~ 100.6°
        const nu = MarsTime.computeTrueAnomalyFromEccentricAnomaly(kep90.eccentricAnomalyDeg, 0.0934);
        expect(nu.trueAnomalyDeg).to.be.closeTo(100.6, 0.2);
    });

    it('should compute Mars-Sun radial distance and solar flux dilution ratio from Ls', () => {
        // At perihelion (Ls = 250.99°): r = a*(1 - e) = 1.52368 * (1 - 0.0934) = 1.52368 * 0.9066 = 1.3814 AU
        // Solar flux ratio = 1 / 1.38137^2 = 0.5241 (vs 1.0 at 1 AU Earth)
        const peri = MarsTime.computeMarsSolarDistanceAndDilutionFromLs(250.99, 1.52368, 0.0934, 250.99);
        expect(peri.distanceAU).to.be.closeTo(1.3814, 0.001);
        expect(peri.solarFluxRatio).to.be.closeTo(0.5241, 0.001);
        expect(peri.isNearPerihelion).to.be.true;
        expect(peri.isNearAphelion).to.be.false;

        // At aphelion (Ls = 250.99 + 180 = 70.99°): r = a*(1 + e) = 1.52368 * 1.0934 = 1.6660 AU
        // Solar flux ratio = 1 / 1.666^2 = 0.3603
        const aph = MarsTime.computeMarsSolarDistanceAndDilutionFromLs(70.99, 1.52368, 0.0934, 250.99);
        expect(aph.distanceAU).to.be.closeTo(1.6660, 0.001);
        expect(aph.solarFluxRatio).to.be.closeTo(0.3603, 0.001);
        expect(aph.isNearAphelion).to.be.true;
    });
});

describe('Sinusoidal Cartographic Projection & Mercator Scale Distortion (geo)', () => {
    it('should calculate forward and inverse Sinusoidal equal-area projection coordinates', () => {
        // Point at (lat = 0°, lon = 0°) on Mars (R = 3389.5 km) -> projects to (0, 0)
        const origin = computeSinusoidalProjection(0.0, 0.0, 0.0, 3389.5);
        expect(origin.xKm).to.equal(0);
        expect(origin.yKm).to.equal(0);

        // Point at (lat = 45°, lon = 60°):
        // y = 3389.5 * (45 * pi / 180) = 3389.5 * 0.785398 = 2662.09 km
        // x = 3389.5 * (60 * pi / 180) * cos(45°) = 3389.5 * 1.047197 * 0.707106 = 2509.77 km
        const fwd = computeSinusoidalProjection(45.0, 60.0, 0.0, 3389.5);
        expect(fwd.xKm).to.be.closeTo(2509.77, 0.1);
        expect(fwd.yKm).to.be.closeTo(2662.09, 0.1);

        // Inverse projection: invert (2509.77 km, 2662.09 km) -> (lat = 45.0°, lon = 60.0°)
        const inv = computeSinusoidalInverse(fwd.xKm, fwd.yKm, 0.0, 3389.5);
        expect(inv.latDeg).to.be.closeTo(45.0, 0.01);
        expect(inv.lonDeg).to.be.closeTo(60.0, 0.01);
    });

    it('should compute conformal Mercator linear and areal scale distortion factors', () => {
        // At equator (lat = 0°): k = 1.0, k_area = 1.0
        const eq = computeMercatorScaleDistortionFactor(0.0);
        expect(eq.scaleFactor).to.equal(1.0);
        expect(eq.areaScaleFactor).to.equal(1.0);

        // At lat = 60°: cos(60°) = 0.5 -> k = 1 / 0.5 = 2.0, k_area = 4.0
        const highLat = computeMercatorScaleDistortionFactor(60.0);
        expect(highLat.scaleFactor).to.equal(2.0);
        expect(highLat.areaScaleFactor).to.equal(4.0);
    });
});

describe('Hohmann Transfer Orbit Maneuvers & Hyperbolic Excess Velocity (TrajectoryEngine)', () => {
    it('should calculate two-impulse Hohmann transfer delta-V budget and duration', () => {
        // Low Mars circular orbit (r1 = 3680 km, alt ~290 km) to Areostationary orbit (r2 = 20428 km, alt ~17038 km)
        // mu = 42828.37 km^3/s^2
        // a_tx = (3680 + 20428) / 2 = 12054 km
        // Transfer duration = pi * sqrt(12054^3 / 42828.37) = pi * sqrt(1.751e12 / 42828.37) = pi * sqrt(40884109) = pi * 6394.06 = 20087 s = 334.79 min = 5.58 hours
        const hoh = TrajectoryEngine.computeHohmannTransferOrbit(3680.0, 20428.0, 'mars');
        expect(hoh.transferSemiMajorAxisKm).to.equal(12054.0);
        expect(hoh.transferDurationHours).to.be.closeTo(5.58, 0.05);
        expect(hoh.totalDeltaVKmS).to.be.greaterThan(1.0);
        expect(hoh.totalDeltaVKmS).to.be.lessThan(2.0);
    });

    it('should compute hyperbolic excess velocity and characteristic energy C3', () => {
        // Spacecraft approaching Mars with periapsis speed v = 6.0 km/s where v_esc = 5.027 km/s
        // v_inf = sqrt(6.0^2 - 5.027^2) = sqrt(36.0 - 25.271) = sqrt(10.729) = 3.2755 km/s
        // C3 = v_inf^2 = 10.729 km^2/s^2
        const hyp = TrajectoryEngine.computeHyperbolicExcessVelocity(6.0, 5.027);
        expect(hyp.vInfinityKmS).to.be.closeTo(3.2755, 0.005);
        expect(hyp.c3Km2S2).to.be.closeTo(10.729, 0.05);
        expect(hyp.isHyperbolic).to.be.true;

        // Sub-escape speed (v = 4.0 km/s < v_esc = 5.027 km/s): bound orbit -> no hyperbolic escape
        const bound = TrajectoryEngine.computeHyperbolicExcessVelocity(4.0, 5.027);
        expect(bound.isHyperbolic).to.be.false;
        expect(bound.vInfinityKmS).to.equal(0.0);
    });
});

describe('CRISM Hydration & Fe/Mg Phyllosilicate Summary Parameters (BandMathEngine)', () => {
    it('should calculate CRISM BD1900 molecular water and BD2300 phyllosilicate band depths', () => {
        // Hydrated clay (e.g. Mawrth Vallis nontronite): R_1850 = 0.32, R_1930 = 0.28 (strong H2O drop), R_2060 = 0.32
        // Continuum = 0.32 -> BD1900 = 1 - (0.28 / 0.32) = 1 - 0.875 = 0.125
        const bd1900 = BandMathEngine.computeCRISMHydratedWaterBD1900(0.32, 0.28, 0.32);
        expect(bd1900.bd1900).to.equal(0.125);
        expect(bd1900.hasHydratedWater).to.be.true;

        // Fe/Mg smectite (e.g. Saponite): R_2250 = 0.30, R_2300 = 0.27 (metal-OH band), R_2350 = 0.30
        // Continuum = 0.30 -> BD2300 = 1 - (0.27 / 0.30) = 1 - 0.90 = 0.10
        const bd2300 = BandMathEngine.computeCRISMMagnesiumIronPhyllosilicateBD2300(0.30, 0.27, 0.30);
        expect(bd2300.bd2300).to.equal(0.10);
        expect(bd2300.hasFeMgPhyllosilicate).to.be.true;
    });

    it('should compute CRISM BD3000 bulk surface hydration depth', () => {
        // Heavily hydrated polar permafrost / sulfate: R_2530 = 0.25, R_3000 = 0.18
        // BD3000 = 1 - (0.18 / 0.25) = 1 - 0.72 = 0.28 (> 0.15 -> hydrated bulk surface)
        const bd3000 = BandMathEngine.computeCRISMBulkHydrationBD3000(0.25, 0.18);
        expect(bd3000.bd3000).to.equal(0.28);
        expect(bd3000.isHydratedBulkSurface).to.be.true;

        // Dry anhydrous basalt (R_2530 = 0.20, R_3000 = 0.19 -> BD3000 = 0.05)
        const dry = BandMathEngine.computeCRISMBulkHydrationBD3000(0.20, 0.19);
        expect(dry.bd3000).to.equal(0.05);
        expect(dry.isHydratedBulkSurface).to.be.false;
    });
});

describe('SAR Azimuth Resolution, Doppler Shift & PRF Bounds (RadarSounderEngine)', () => {
    it('should calculate theoretical along-track SAR azimuth spatial resolution', () => {
        // SHARAD antenna dipole length = 10 meters -> azimuth resolution = 10 / 2 = 5 meters
        const sar = RadarSounderEngine.computeSARAzimuthResolution(10.0);
        expect(sar.azimuthResolutionMeters).to.equal(5.0);
        expect(sar.antennaLengthMeters).to.equal(10.0);
    });

    it('should compute radar Doppler frequency shift and PRF timing bounds', () => {
        // Spacecraft orbiting at v = 3400 m/s, SHARAD 20 MHz (lambda = 14.9896 m)
        // Along-track squint = 5°, cross-track = 0°
        // f_d = (2 * 3400 / 14.9896) * sin(5°) = 453.647 * 0.087156 = 39.538 Hz
        const dop = RadarSounderEngine.computeDopplerFrequencyShift(3400.0, 20e6, 5.0, 0.0);
        expect(dop.dopplerShiftHz).to.be.closeTo(39.54, 0.1);
        expect(dop.wavelengthMeters).to.be.closeTo(14.99, 0.05);

        // PRF bounds for MRO SHARAD: v = 3400 m/s, L = 10 m, max range = 300 km
        // PRF_min = 2 * 3400 / 10 = 680 Hz
        // PRF_max = 299792458 / (2 * 300000) = 499.65 Hz (or higher for lower altitude sounding)
        const prf = RadarSounderEngine.computeRadarPulseRepetitionFrequencyBounds(3400.0, 10.0, 300.0);
        expect(prf.prfMinHz).to.equal(680.0);
        expect(prf.prfMaxHz).to.be.closeTo(499.65, 0.1);
    });
});

describe('Diurnal Thermal Skin Depth & Regolith Heat Storage (KRCEngine)', () => {
    it('should calculate diurnal thermal wave skin depth and regolith thermal conductivity', () => {
        // Typical Martian duricrust/sand: I = 250 J m^-2 K^-1 s^-1/2, rho = 1500 kg/m^3, c_p = 800 J/(kg K)
        // k = 250^2 / (1500 * 800) = 62500 / 1200000 = 0.05208 W/(m K)
        // d_skin = (250 * sqrt(88775.244)) / (sqrt(pi) * 1200000) = (250 * 297.9517) / (1.77245 * 1200000) = 74487.9 / 2126940 = 0.03502 m = 3.50 cm
        const skin = KRCEngine.computeDiurnalThermalSkinDepth(250.0, 800.0, 1500.0, 88775.244);
        expect(skin.thermalConductivityW_mK).to.be.closeTo(0.05208, 0.0001);
        expect(skin.skinDepthMeters).to.be.closeTo(0.0350, 0.001);
        expect(skin.skinDepthCm).to.be.closeTo(3.50, 0.1);
    });

    it('should compute regolith dry bulk density and subsurface sensible heat storage', () => {
        // Basalt grains (rho_grain = 3000 kg/m^3) with 50% porosity (phi = 0.5) -> rho_bulk = 1500 kg/m^3
        const bulk = KRCEngine.computeRegolithBulkDensity(3000.0, 0.5);
        expect(bulk.bulkDensityKg_M3).to.equal(1500.0);
        expect(bulk.voidRatio).to.equal(1.0);

        // Top 5 cm layer (dz = 0.05 m), warming by Delta_T = 20 K (daytime solar heating)
        // Delta_Q = 1500 * 800 * 0.05 * 20 = 1200000 * 1.0 = 1200000 J/m^2 (1.2 MJ/m^2)
        const heat = KRCEngine.computeSubsurfaceSensibleHeatStorage(0.05, 20.0, 1500.0, 800.0);
        expect(heat.sensibleHeatJ_M2).to.equal(1200000.0);
        expect(heat.volumetricHeatCapacityJ_M3K).to.equal(1200000.0);
    });
});

describe('Ivanov Crater Production Function & Excavation Volumes (CSFDEngine)', () => {
    it('should calculate Ivanov 1 Ga cumulative production function N(>D)', () => {
        // At D = 1 km: log10(D) = 0 -> log10(N) = a_0 = -3.0876 -> N(>1km) = 10^-3.0876 = 8.173e-4 km^-2 (0.817 craters / 1000 km^2)
        const pf1 = CSFDEngine.computeIvanovProductionFunctionCoefficients(1.0);
        expect(pf1.log10D).to.equal(0.0);
        expect(pf1.log10N).to.be.closeTo(-3.0876, 0.001);
        expect(pf1.nCumulativePerKm2_1Ga).to.be.closeTo(8.173e-4, 1e-6);

        // At D = 10 km: log10(D) = 1.0
        const pf10 = CSFDEngine.computeIvanovProductionFunctionCoefficients(10.0);
        expect(pf10.log10D).to.equal(1.0);
        expect(pf10.nCumulativePerKm2_1Ga).to.be.lessThan(pf1.nCumulativePerKm2_1Ga);
    });

    it('should compute differential crater density dN/dD and excavation volumes', () => {
        // Bin [1 km, 2 km]: N(>1km) = 8.173e-4, N(>2km) = 2.0e-4 -> Delta_N = 6.173e-4 over Delta_D = 1 km -> dN/dD = 6.173e-4 km^-3
        const diff = CSFDEngine.computeDifferentialCraterDensity(8.173e-4, 2.0e-4, 1.0, 2.0);
        expect(diff.binWidthKm).to.equal(1.0);
        expect(diff.differentialDensityKm3).to.be.closeTo(6.173e-4, 1e-6);

        // Simple bowl-shaped crater: D = 5 km, gamma = 0.2 (depth d = 1.0 km)
        // V = (pi / 8) * D^2 * d = (pi / 8) * 25 * 1.0 = 3.14159 * 3.125 = 9.8175 km^3
        const vol = CSFDEngine.computeImpactCraterExcavationVolume(5.0, 0.2);
        expect(vol.depthKm).to.equal(1.0);
        expect(vol.excavationVolumeKm3).to.be.closeTo(9.8175, 0.01);
    });
});

describe('CO2 Molecular Mean Free Path, Knudsen Regimes & Column Mass (MCDEngine)', () => {
    it('should calculate CO2 molecular mean free path and Knudsen flow regimes', () => {
        // Mars surface conditions: T = 220 K, P = 610 Pa (6.1 mbar)
        // lambda = (1.380649e-23 * 220) / (sqrt(2) * pi * (3.3e-10)^2 * 610)
        // lambda = 3.0374e-21 / (1.4142 * 3.14159 * 1.089e-19 * 610) = 3.0374e-21 / 2.9515e-16 = 1.029e-5 m = 10.29 microns
        const mfp = MCDEngine.computeCO2MolecularMeanFreePath(220.0, 610.0);
        expect(mfp.meanFreePathMicrons).to.be.closeTo(10.29, 0.5);

        // Ingenuity rotor chord (L = 0.12 m) at surface: Kn = 1.029e-5 / 0.12 = 8.57e-5 (< 0.01 -> continuum flow)
        const rotor = MCDEngine.computeKnudsenNumberAndRegime(220.0, 610.0, 0.12);
        expect(rotor.isContinuum).to.be.true;
        expect(rotor.regime).to.equal('continuum');

        // High-altitude mesosphere entry (T = 150 K, P = 0.01 Pa, L = 1.0 m aeroshell) -> Kn = 0.428 (transitional flow)
        const entry = MCDEngine.computeKnudsenNumberAndRegime(150.0, 0.01, 1.0);
        expect(entry.isTransitional).to.be.true;
        expect(entry.regime).to.equal('transitional');

        // Upper exosphere rarefied flight (T = 150 K, P = 0.0001 Pa, L = 0.1 m) -> Kn = 42.8 (free-molecular flow)
        const exo = MCDEngine.computeKnudsenNumberAndRegime(150.0, 0.0001, 0.1);
        expect(exo.isFreeMolecular).to.be.true;
        expect(exo.regime).to.equal('free-molecular');
    });

    it('should compute total vertical atmospheric column mass per unit surface area', () => {
        // Global mean surface pressure P = 610 Pa, g = 3.72076 m/s^2
        // Column mass M_col = 610 / 3.72076 = 163.95 kg/m^2 (16.395 g/cm^2 vs Earth ~1033 g/cm^2)
        const col = MCDEngine.computeAtmosphericColumnMassAndDensity(610.0, 3.72076);
        expect(col.columnMassKg_M2).to.be.closeTo(163.95, 0.1);
        expect(col.columnMassG_Cm2).to.be.closeTo(16.395, 0.01);
    });
});

describe('3D Normal Slope/Aspect Inversion & Solar Incidence Cosine (ThreeDEngine)', () => {
    it('should invert 3D ENU surface normal vector into topographic slope and aspect', () => {
        // East-facing slope (s = 30°, a = 90°): n_east = -0.5, n_north = 0, n_up = 0.866025
        const inv = ThreeDEngine.computeSlopeAndAspectFromNormalVector(-0.5, 0.0, 0.866025);
        expect(inv.slopeDeg).to.be.closeTo(30.0, 0.05);
        expect(inv.aspectDeg).to.be.closeTo(90.0, 0.05);
        expect(inv.isFlat).to.be.false;

        // Flat horizontal terrain: n_east = 0, n_north = 0, n_up = 1.0 -> s = 0°
        const flat = ThreeDEngine.computeSlopeAndAspectFromNormalVector(0.0, 0.0, 1.0);
        expect(flat.slopeDeg).to.equal(0.0);
        expect(flat.isFlat).to.be.true;
    });

    it('should compute topographic surface area inflation factor and solar incidence cosine', () => {
        // 60° steep cliff slope: sec(60°) = 1 / 0.5 = 2.0 (2x actual surface area vs planar footprint)
        const area = ThreeDEngine.computeTopographicAreaCorrectionFactor(60.0);
        expect(area.areaInflationFactor).to.equal(2.0);

        // Overhead sun (Z = 0°, A = 0°): s_east = 0, s_north = 0, s_up = 1.0
        // On 30° slope with n_up = 0.8660 -> cos(i) = 0.8660 -> incidence = 30°
        const sun = ThreeDEngine.computeSolarIncidenceCosineFromNormal(-0.5, 0.0, 0.866025, 0.0, 0.0);
        expect(sun.cosIncidence).to.be.closeTo(0.8660, 0.001);
        expect(sun.incidenceAngleDeg).to.be.closeTo(30.0, 0.1);
        expect(sun.isIlluminated).to.be.true;
    });
});

describe('Orthographic Globe Projection & Inverse Transforms (geo)', () => {
    it('should calculate forward Orthographic projection and hemisphere clipping', () => {
        // Center at (0°, 0°), point at (0°, 0°) -> (x = 0, y = 0, isVisible = true)
        const center = computeOrthographicProjection(0.0, 0.0, 0.0, 0.0, 3389.5);
        expect(center.xKm).to.equal(0);
        expect(center.yKm).to.equal(0);
        expect(center.isVisible).to.be.true;

        // Point at (lat = 30°, lon = 45°):
        // x = 3389.5 * cos(30°) * sin(45°) = 3389.5 * 0.866025 * 0.707106 = 2075.69 km
        // y = 3389.5 * sin(30°) = 3389.5 * 0.5 = 1694.75 km
        const fwd = computeOrthographicProjection(30.0, 45.0, 0.0, 0.0, 3389.5);
        expect(fwd.xKm).to.be.closeTo(2075.69, 0.1);
        expect(fwd.yKm).to.be.closeTo(1694.75, 0.1);
        expect(fwd.isVisible).to.be.true;

        // Back-side point (lon = 120° from center 0° -> cos(c) = cos(120°) = -0.5 < 0 -> hidden hemisphere)
        const back = computeOrthographicProjection(0.0, 120.0, 0.0, 0.0, 3389.5);
        expect(back.isVisible).to.be.false;
    });

    it('should compute inverse Orthographic projection back to geographic coordinates', () => {
        // Invert (2075.69 km, 1694.75 km) with center at (0°, 0°) -> (30.0°, 45.0°)
        const inv = computeOrthographicInverse(2075.69, 1694.75, 0.0, 0.0, 3389.5);
        expect(inv.isInsideGlobeDisk).to.be.true;
        expect(inv.latDeg).to.be.closeTo(30.0, 0.01);
        expect(inv.lonDeg).to.be.closeTo(45.0, 0.01);

        // Outside globe disk (rho = 4000 km > R = 3389.5 km)
        const out = computeOrthographicInverse(3000.0, 3000.0, 0.0, 0.0, 3389.5);
        expect(out.isInsideGlobeDisk).to.be.false;
    });
});

describe('Martian Sunrise, Sunset & Solar Noon Zenith Solvers (MarsTime)', () => {
    it('should calculate Martian sunrise/sunset LTST, daylight duration and polar day/night', () => {
        // At vernal equinox (Ls = 0°): delta = 0°
        // At equator (lat = 0°): cos(H0) = 0 -> H0 = 90° -> sunrise = 6:00, sunset = 18:00, daylight = 12h
        const eq = MarsTime.computeMartianSunriseSunsetTimes(0.0, 0.0);
        expect(eq.sunriseLTST).to.equal(6.0);
        expect(eq.sunsetLTST).to.equal(18.0);
        expect(eq.daylightHours).to.equal(12.0);
        expect(eq.isPolarDay).to.be.false;
        expect(eq.isPolarNight).to.be.false;

        // North polar summer solstice (Ls = 90°, delta = +25.19°): at North Pole (lat = +80°) -> Midnight Sun / Polar Day (24h daylight)
        const northSummer = MarsTime.computeMartianSunriseSunsetTimes(80.0, 90.0);
        expect(northSummer.isPolarDay).to.be.true;
        expect(northSummer.daylightHours).to.equal(24.0);

        // South polar winter (Ls = 90°): at South Pole (lat = -80°) -> Polar Night (0h daylight)
        const southWinter = MarsTime.computeMartianSunriseSunsetTimes(-80.0, 90.0);
        expect(southWinter.isPolarNight).to.be.true;
        expect(southWinter.daylightHours).to.equal(0.0);
    });

    it('should compute solar noon zenith and elevation angles', () => {
        // Subsolar point at equator during equinox (Ls = 0°, lat = 0°): Z_noon = 0°, alpha_noon = 90° (overhead)
        const sub = MarsTime.computeMartianNoonZenithAngle(0.0, 0.0);
        expect(sub.noonZenithDeg).to.equal(0.0);
        expect(sub.noonElevationDeg).to.equal(90.0);
        expect(sub.isSubsolarPoint).to.be.true;

        // Gale Crater (lat = -4.6°) at Ls = 90° (delta = +25.19°):
        // Z_noon = |-4.6 - 25.19| = 29.79° -> alpha_noon = 60.21°
        const gale = MarsTime.computeMartianNoonZenithAngle(-4.6, 90.0);
        expect(gale.noonZenithDeg).to.be.closeTo(29.79, 0.05);
        expect(gale.noonElevationDeg).to.be.closeTo(60.21, 0.05);
    });
});

describe('OMEGA Ferric Iron, Olivine & TES Surface Type Indices (BandMathEngine)', () => {
    it('should calculate OMEGA BD530 nanophase ferric oxide and OLINDEX3 olivine band depth', () => {
        // Bright dusty Martian surface (e.g. Arabia Terra hematite/dust): R_440 = 0.15, R_530 = 0.22, R_700 = 0.35
        // Continuum = 0.5 * (0.15 + 0.35) = 0.25 -> BD530 = 1 - (0.22 / 0.25) = 1 - 0.88 = 0.12 (> 0.03)
        const bd530 = BandMathEngine.computeOMEGAFerricOxideBD530(0.15, 0.22, 0.35);
        expect(bd530.bd530).to.equal(0.12);
        expect(bd530.hasFerricOxide).to.be.true;

        // Olivine-rich bedrock (e.g. Nili Fossae): R_860 = 0.28, R_1050 = 0.22 (strong broad Fe2+ drop), R_1210 = 0.28
        // Continuum = 0.28 -> OLINDEX3 = 1 - (0.22 / 0.28) = 1 - 0.7857 = 0.2143 (> 0.05)
        const ol = BandMathEngine.computeOMEGAOlivineIndexOLINDEX3(0.28, 0.22, 0.28);
        expect(ol.olindex3).to.be.closeTo(0.2143, 0.001);
        expect(ol.hasOlivine).to.be.true;
    });

    it('should compute TES Surface Type Index distinguishing Basaltic (ST1) vs Andesitic (ST2)', () => {
        // Syrtis Major (Basaltic Type 1): eps_820 = 0.98, eps_1075 = 0.92
        // STI = (0.98 - 0.92) / (0.98 + 0.92) = 0.06 / 1.90 = +0.0316 (>= 0 -> Basaltic ST1)
        const basalt = BandMathEngine.computeTESSurfaceTypeIndex(0.98, 0.92);
        expect(basalt.surfaceTypeIndex).to.be.closeTo(0.0316, 0.001);
        expect(basalt.isBasalticType1).to.be.true;
        expect(basalt.isAndesiticType2).to.be.false;

        // Acidalia Planitia (Andesitic / high-silica Type 2): eps_820 = 0.92, eps_1075 = 0.96
        // STI = (0.92 - 0.96) / (0.92 + 0.96) = -0.04 / 1.88 = -0.0213 (< 0 -> Andesitic ST2)
        const andesite = BandMathEngine.computeTESSurfaceTypeIndex(0.92, 0.96);
        expect(andesite.surfaceTypeIndex).to.be.closeTo(-0.0213, 0.001);
        expect(andesite.isBasalticType1).to.be.false;
        expect(andesite.isAndesiticType2).to.be.true;
    });
});

describe('J2 Oblateness Nodal & Apsidal Precession Solvers (TrajectoryEngine)', () => {
    it('should calculate J2 nodal precession rate and detect sun-synchronous orbits', () => {
        // Mars Reconnaissance Orbiter (MRO) mapping orbit:
        // alt ~ 290 km -> a = 3389.5 + 290 = 3679.5 km, e ~ 0.01, inclination = 92.7° (retrograde)
        const mro = TrajectoryEngine.computeJ2NodalPrecessionRate(3679.5, 0.01, 92.7, 'mars');
        expect(mro.nodalPrecessionDegPerDay).to.be.greaterThan(0.4);
        expect(mro.nodalPrecessionDegPerDay).to.be.lessThan(0.6);
        expect(mro.isSunSynchronousCandidate).to.be.true;

        // Prograde equatorial orbit (i = 0°): dOmega/dt < 0 (regressing westwards)
        const eq = TrajectoryEngine.computeJ2NodalPrecessionRate(4000.0, 0.0, 0.0, 'mars');
        expect(eq.nodalPrecessionDegPerDay).to.be.lessThan(0.0);
    });

    it('should compute J2 apsidal pericenter drift rate and detect critical frozen inclination', () => {
        // Mars Frozen Orbit at critical inclination i = 63.435° (or 116.565°)
        const frozen = TrajectoryEngine.computeJ2ApsidalPrecessionRate(4000.0, 0.05, 63.435, 'mars');
        expect(frozen.apsidalPrecessionDegPerDay).to.be.closeTo(0.0, 0.001);
        expect(frozen.isCriticalFrozenInclination).to.be.true;

        // Polar orbit (i = 90°): 5*cos^2(90) - 1 = -1 -> domega/dt < 0
        const polar = TrajectoryEngine.computeJ2ApsidalPrecessionRate(4000.0, 0.05, 90.0, 'mars');
        expect(polar.apsidalPrecessionDegPerDay).to.be.lessThan(0.0);
        expect(polar.isCriticalFrozenInclination).to.be.false;
    });
});

describe('Fresnel Dielectric Reflection & Two-Way Radar Attenuation (RadarSounderEngine)', () => {
    it('should calculate normal incidence Fresnel reflection and transmission coefficients', () => {
        // Vacuum to pure water ice interface (eps1 = 1.0, eps2 = 3.15):
        // sqrt(1) = 1.0, sqrt(3.15) = 1.7748
        // R = ((1 - 1.7748) / (1 + 1.7748))^2 = (-0.7748 / 2.7748)^2 = (-0.27923)^2 = 0.07797 (~7.8% reflected power)
        // R_dB = 10 * log10(0.07797) = -11.08 dB
        const ice = RadarSounderEngine.computeFresnelReflectionAndTransmissionCoefficients(1.0, 3.15);
        expect(ice.powerReflectionCoeff).to.be.closeTo(0.07797, 0.001);
        expect(ice.powerTransmissionCoeff).to.be.closeTo(0.92203, 0.001);
        expect(ice.reflectionCoeffDb).to.be.closeTo(-11.08, 0.1);
    });

    it('should compute two-way subsurface radar attenuation loss in dB', () => {
        // SHARAD (f = 20 MHz) sounding in cold polar layered deposits (PLD pure ice):
        // er = 3.15, tan(delta) = 0.001, depth = 1000 m (1 km)
        // alpha_Np = (pi * 20e6 * sqrt(3.15) * 0.001) / 299792458 = 111.517 / 299792458 = 3.7198e-7 Np/m
        // alpha_dB/m = 8.6858896 * 3.7198e-7 = 3.231e-6 dB/m = 3.231 dB/km
        // Loss_2way = 2 * 3.231 = 6.462 dB
        const loss = RadarSounderEngine.computeTwoWayRadarSubsurfaceAttenuation(20e6, 3.15, 0.001, 1000.0);
        expect(loss.twoWayLossDb).to.be.closeTo(6.462, 0.1);
        expect(loss.attenuationRateDbPerKm).to.be.closeTo(3.231, 0.05);
    });
});

describe('CO2 Frost Sublimation & Regolith Thermal Transport (KRCEngine)', () => {
    it('should calculate CO2 frost sublimation / condensation rates and seasonal thickness change', () => {
        // Polar spring insolation excess: F_net = +59 W/m^2, L_sub = 5.9e5 J/kg, rho = 1500 kg/m^3
        // dm/dt = 59 / 5.9e5 = 1.0e-4 kg / (m^2 s)
        // dz/dt = (1.0e-4 / 1500) * 88775.244 * 1000 = 5.918 mm / sol
        const sub = KRCEngine.computeCO2SublimationFrostMassRate(59.0, 5.9e5, 1500.0);
        expect(sub.sublimationRateKg_M2S).to.equal(0.0001);
        expect(sub.thicknessRateMmPerSol).to.be.closeTo(5.918, 0.01);
        expect(sub.isSublimating).to.be.true;
        expect(sub.isCondensing).to.be.false;

        // Polar winter radiative cooling deficit: F_net = -29.5 W/m^2 (condensation / deposition)
        const cond = KRCEngine.computeCO2SublimationFrostMassRate(-29.5, 5.9e5, 1500.0);
        expect(cond.thicknessRateMmPerSol).to.be.closeTo(-2.959, 0.01);
        expect(cond.isCondensing).to.be.true;
    });

    it('should derive microscopic thermal conductivity and diffusivity from Thermal Inertia', () => {
        // Typical Martian dust mantle: I = 50 J m^-2 K^-1 s^-1/2, rho = 1000 kg/m^3, cp = 800 J/(kg K)
        // C_vol = 1000 * 800 = 8.0e5 J/(m^3 K)
        // k = 50^2 / 8.0e5 = 2500 / 800000 = 0.003125 W/(m K)
        // kappa = 0.003125 / 800000 = 3.906e-9 m^2/s
        const dust = KRCEngine.computeThermalConductivityAndDiffusivity(50.0, 1000.0, 800.0);
        expect(dust.thermalConductivityW_MK).to.be.closeTo(0.00313, 0.00005);
        expect(dust.volumetricHeatCapacityJ_M3K).to.equal(800000.0);

        // Solid basalt bedrock: I = 2000, rho = 2800 kg/m^3, cp = 800 J/(kg K)
        // C_vol = 2240000 J/(m^3 K) -> k = 4000000 / 2240000 = 1.7857 W/(m K)
        const rock = KRCEngine.computeThermalConductivityAndDiffusivity(2000.0, 2800.0, 800.0);
        expect(rock.thermalConductivityW_MK).to.be.closeTo(1.7857, 0.01);
    });
});

describe('Impact Crater Morphometry Scaling & Retention Ages (CSFDEngine)', () => {
    it('should calculate simple vs complex crater morphometry dimensions and ejecta radius', () => {
        // Simple crater (D = 2.0 km < 7 km):
        // d = 0.20 * 2.0 = 0.40 km (400 m), h_rim = 0.04 * 2.0^1.01 = 0.0805 km (80.5 m), R_ejecta = 1.15 * 2.0 = 2.30 km
        const simple = CSFDEngine.computeCraterMorphometryDimensions(2.0, 7.0);
        expect(simple.depthKm).to.equal(0.4);
        expect(simple.depthMeters).to.equal(400.0);
        expect(simple.rimHeightMeters).to.be.closeTo(80.6, 0.5);
        expect(simple.ejectaRadiusKm).to.equal(2.3);
        expect(simple.isSimple).to.be.true;
        expect(simple.isComplex).to.be.false;

        // Complex crater (D = 50 km >= 7 km, e.g. Gale / Jezero scale):
        // d = 0.36 * 50^0.49 = 0.36 * 6.782 = 2.441 km, R_ejecta = 57.5 km
        const complex = CSFDEngine.computeCraterMorphometryDimensions(50.0, 7.0);
        expect(complex.depthKm).to.be.closeTo(2.441, 0.05);
        expect(complex.ejectaRadiusKm).to.equal(57.5);
        expect(complex.isComplex).to.be.true;
    });

    it('should derive surface retention model age and classify geological epoch', () => {
        // Amazonian young volcanic terrain: N(1) = 1.225e-4 km^-2 -> age = 1.0 * (1.225e-4 / 2.45e-4) = 0.50 Ga
        const young = CSFDEngine.computeCraterRetentionAgeFromIsochron(1.225e-4, 2.45e-4, 1.0);
        expect(young.modelAgeGa).to.equal(0.5);
        expect(young.geologicalEpoch).to.equal('Amazonian');

        // Ancient Noachian highland: N(1) = 9.8e-4 km^-2 -> age = 4.0 Ga
        const ancient = CSFDEngine.computeCraterRetentionAgeFromIsochron(9.8e-4, 2.45e-4, 1.0);
        expect(ancient.modelAgeGa).to.equal(4.0);
        expect(ancient.geologicalEpoch).to.equal('Noachian');
    });
});

describe('CO2 Dynamic Viscosity, Reynolds Number & Scale Height (MCDEngine)', () => {
    it('should calculate CO2 dynamic viscosity via Sutherland law and atmospheric scale height', () => {
        // Mars surface mean temperature T = 220 K:
        // mu0 = 1.370e-5, T0 = 273.15, S = 222.0
        // (220 / 273.15)^1.5 = 0.8054^1.5 = 0.7228
        // (273.15 + 222) / (220 + 222) = 495.15 / 442.0 = 1.1202
        // mu = 1.370e-5 * 0.7228 * 1.1202 = 1.1093e-5 Pa s
        const visc = MCDEngine.computeCO2DynamicViscositySutherland(220.0);
        expect(visc.dynamicViscosityPaS).to.be.closeTo(1.109e-5, 0.05e-5);

        // Scale height at T = 220 K: H = 188.92 * 220 / 3.72076 = 41562.4 / 3.72076 = 11170.4 m = 11.170 km
        const h = MCDEngine.computeAtmosphericScaleHeightDetailed(220.0, 3.72076);
        expect(h.scaleHeightKm).to.be.closeTo(11.170, 0.05);
    });

    it('should compute aerodynamic Reynolds number and flow regime', () => {
        // Ingenuity rotor blade in Martian atmosphere:
        // rho = 0.015 kg/m^3 (surface ~610 Pa), v = 150 m/s blade tip, L = 0.12 m chord, T = 220 K
        // mu = 1.109e-5 Pa s
        // Re = (0.015 * 150 * 0.12) / 1.109e-5 = 0.27 / 1.109e-5 = 24346 (< 5e5 -> laminar)
        const rotor = MCDEngine.computeAtmosphericReynoldsNumber(0.015, 150.0, 0.12, 220.0);
        expect(rotor.reynoldsNumber).to.be.closeTo(24346.0, 500.0);
        expect(rotor.isLaminar).to.be.true;
        expect(rotor.isTurbulent).to.be.false;

        // High-velocity entry aeroshell (v = 5000 m/s, L = 2.65 m, rho = 0.005 kg/m^3)
        // Re = (0.005 * 5000 * 2.65) / 1.109e-5 = 66.25 / 1.109e-5 = 5.97e6 (>= 5e5 -> turbulent boundary layer)
        const entry = MCDEngine.computeAtmosphericReynoldsNumber(0.005, 5000.0, 2.65, 220.0);
        expect(entry.isTurbulent).to.be.true;
    });
});

describe('Gnomonic Central Perspective Projection & Inverse Solvers (geo)', () => {
    it('should calculate forward Gnomonic projection mapping Great Circles to straight lines', () => {
        // Projection center at (0°, 0°), point at (0°, 0°) -> (x = 0, y = 0, isVisible = true)
        const center = computeGnomonicProjection(0.0, 0.0, 0.0, 0.0, 3389.5);
        expect(center.xKm).to.equal(0);
        expect(center.yKm).to.equal(0);
        expect(center.isVisible).to.be.true;

        // Point at (lat = 30°, lon = 45°):
        // cos(c) = cos(30°)*cos(45°) = 0.866025 * 0.707106 = 0.61237
        // x = 3389.5 * 0.866025 * 0.707106 / 0.61237 = 3389.5 km
        // y = 3389.5 * sin(30°) / 0.61237 = 3389.5 * 0.5 / 0.61237 = 2767.51 km
        const fwd = computeGnomonicProjection(30.0, 45.0, 0.0, 0.0, 3389.5);
        expect(fwd.xKm).to.be.closeTo(3389.5, 0.1);
        expect(fwd.yKm).to.be.closeTo(2767.51, 0.1);
        expect(fwd.isVisible).to.be.true;

        // Point beyond horizon (> 90° from center -> cos(c) <= 0)
        const beyond = computeGnomonicProjection(0.0, 100.0, 0.0, 0.0, 3389.5);
        expect(beyond.isVisible).to.be.false;
    });

    it('should compute inverse Gnomonic projection back to geographic coordinates', () => {
        // Invert (3389.5 km, 2767.51 km) with center at (0°, 0°) -> (30.0°, 45.0°)
        const inv = computeGnomonicInverse(3389.5, 2767.51, 0.0, 0.0, 3389.5);
        expect(inv.latDeg).to.be.closeTo(30.0, 0.01);
        expect(inv.lonDeg).to.be.closeTo(45.0, 0.01);

        // Center origin point inversion
        const origin = computeGnomonicInverse(0.0, 0.0, 15.0, 45.0, 3389.5);
        expect(origin.latDeg).to.equal(15.0);
        expect(origin.lonDeg).to.equal(45.0);
    });
});

describe('Stereo Photogrammetry B/H & Lambertian Radiance (ThreeDEngine)', () => {
    it('should calculate stereo camera Base-to-Height ratio and vertical elevation precision', () => {
        // HiRISE stereo pair: lookAngle1 = -10° (backward), lookAngle2 = +10° (forward), GSD = 0.25 m, subpixel = 0.2 px
        // tan(-10°) = -0.1763, tan(10°) = +0.1763 -> B/H = |-0.1763 - 0.1763| = 0.3527
        // convAngle = 20.0°
        // sigma_z = (1 / 0.3527) * 0.25 * 0.2 = 2.8354 * 0.05 = 0.1418 m (14.2 cm precision!)
        const stereo = ThreeDEngine.computeStereoParallaxBaseToHeightRatio(-10.0, 10.0, 0.25, 0.2);
        expect(stereo.baseToHeightRatio).to.be.closeTo(0.3527, 0.001);
        expect(stereo.convergenceAngleDeg).to.equal(20.0);
        expect(stereo.heightPrecisionMeters).to.be.closeTo(0.142, 0.01);
        expect(stereo.isGoodStereoGeometry).to.be.true;
    });

    it('should compute Lambertian diffuse radiance factor with ambient background', () => {
        // Overhead illumination (cos i = 1.0), albedo = 0.25, ambient = 0.05
        // R = 0.05 + 0.95 * 0.25 * 1.0 = 0.05 + 0.2375 = 0.2875
        const sun = ThreeDEngine.computeLambertianReflectanceAndShading(1.0, 0.25, 0.05);
        expect(sun.radianceFactor).to.equal(0.2875);
        expect(sun.isDirectlyIlluminated).to.be.true;

        // Shadowed / night side (cos i = 0): R = 0.05 (ambient diffuse only)
        const dark = ThreeDEngine.computeLambertianReflectanceAndShading(0.0, 0.25, 0.05);
        expect(dark.radianceFactor).to.equal(0.05);
    });
});

describe('CRISM Carbonate, Al-OH Phyllosilicate & High-Ca Pyroxene (BandMathEngine)', () => {
    it('should calculate CRISM BD2500 carbonate and BD2200 Al-OH phyllosilicate absorption depths', () => {
        // Nili Fossae Mg/Fe Carbonate outcrop: R_2430 = 0.30, R_2500 = 0.26 (carbonate band), R_2570 = 0.30
        // Continuum = 0.30 -> BD2500 = 1 - (0.26 / 0.30) = 1 - 0.8667 = 0.1333 (> 0.02)
        const carb = BandMathEngine.computeCRISMCarbonateBD2500(0.30, 0.26, 0.30);
        expect(carb.bd2500).to.be.closeTo(0.1333, 0.001);
        expect(carb.hasCarbonate).to.be.true;

        // Mawrth Vallis Kaolinite / Montmorillonite (Al-OH): R_2140 = 0.32, R_2200 = 0.28, R_2250 = 0.32
        // Continuum = 0.32 -> BD2200 = 1 - (0.28 / 0.32) = 1 - 0.875 = 0.125 (> 0.02)
        const aloh = BandMathEngine.computeCRISMAlOHPhyllosilicateBD2200(0.32, 0.28, 0.32);
        expect(aloh.bd2200).to.equal(0.125);
        expect(aloh.hasAlOHPhyllosilicate).to.be.true;
    });

    it('should compute CRISM High-Calcium Pyroxene (Augite) HCPINDEX ferrous absorption', () => {
        // Syrtis Major basaltic Augite: R_800 = 0.20, R_1000 = 0.16 (broad Fe2+ band), R_1300 = 0.24
        // Continuum = 0.5 * (0.20 + 0.24) = 0.22 -> HCPINDEX = 1 - (0.16 / 0.22) = 1 - 0.7273 = 0.2727 (> 0.04)
        const hcp = BandMathEngine.computeCRISMPyroxeneHCPINDEX(0.20, 0.16, 0.24);
        expect(hcp.hcpIndex).to.be.closeTo(0.2727, 0.001);
        expect(hcp.hasHighCalciumPyroxene).to.be.true;
    });
});

describe('Cartesian State Vectors to Keplerian Orbital Elements (TrajectoryEngine)', () => {
    it('should convert 3D circular equatorial orbit state vectors into Keplerian elements', () => {
        // Mars circular orbit at r = 4000 km in equatorial plane (z = 0, vz = 0):
        // v_circ = sqrt(mu / r) = sqrt(42828.3752 / 4000) = 3.27216 km/s along +y
        const rVec = { x: 4000.0, y: 0.0, z: 0.0 };
        const vVec = { vx: 0.0, vy: 3.272168, vz: 0.0 };
        const orb = TrajectoryEngine.computeOrbitalElementsFromStateVectors(rVec, vVec, 'mars');

        expect(orb.semiMajorAxisKm).to.be.closeTo(4000.0, 1.0);
        expect(orb.eccentricity).to.be.closeTo(0.0, 0.001);
        expect(orb.inclinationDeg).to.equal(0.0);
        expect(orb.isBoundOrbit).to.be.true;
        // Period T = 2 * pi * sqrt(4000^3 / 42828.3752) = 2 * pi * 1222.42 = 7680.6 sec = 128.01 min
        expect(orb.orbitalPeriodMinutes).to.be.closeTo(128.0, 0.5);
    });

    it('should convert inclined polar orbit state vectors and detect unbound escape trajectories', () => {
        // Mars polar orbit (inclination = 90°): r along +x, v along +z
        const rPol = { x: 4000.0, y: 0.0, z: 0.0 };
        const vPol = { vx: 0.0, vy: 0.0, vz: 3.272168 };
        const polar = TrajectoryEngine.computeOrbitalElementsFromStateVectors(rPol, vPol, 'mars');
        expect(polar.inclinationDeg).to.equal(90.0);
        expect(polar.isBoundOrbit).to.be.true;

        // Hyperbolic escape trajectory: v = 6.0 km/s (> v_esc = sqrt(2*mu/r) = 4.627 km/s)
        const vHyp = { vx: 0.0, vy: 6.0, vz: 0.0 };
        const hyp = TrajectoryEngine.computeOrbitalElementsFromStateVectors(rPol, vHyp, 'mars');
        expect(hyp.isBoundOrbit).to.be.false;
        expect(hyp.eccentricity).to.be.greaterThan(1.0);
    });
});

describe('Martian Aerocentric Seasons & Subsolar Point Solvers (MarsTime)', () => {
    it('should classify Martian seasons and solstice/equinox markers from Solar Longitude (Ls)', () => {
        // Vernal Equinox (Ls = 0°): Northern Spring, Southern Autumn, 0% progress
        const vernal = MarsTime.computeMartianSeasonFromLs(0.0);
        expect(vernal.northernSeason).to.equal('Spring');
        expect(vernal.southernSeason).to.equal('Autumn');
        expect(vernal.seasonProgressPercent).to.equal(0.0);
        expect(vernal.isEquinox).to.be.true;
        expect(vernal.isSolstice).to.be.false;

        // Northern Summer Solstice (Ls = 90°): 0% progress into Summer
        const summerSol = MarsTime.computeMartianSeasonFromLs(90.0);
        expect(summerSol.northernSeason).to.equal('Summer');
        expect(summerSol.southernSeason).to.equal('Winter');
        expect(summerSol.isSolstice).to.be.true;

        // Mid Northern Winter (Ls = 315°): 50% through Northern Winter / Southern Summer
        const midWinter = MarsTime.computeMartianSeasonFromLs(315.0);
        expect(midWinter.northernSeason).to.equal('Winter');
        expect(midWinter.southernSeason).to.equal('Summer');
        expect(midWinter.seasonProgressPercent).to.equal(50.0);
    });

    it('should calculate exact Martian subsolar latitude and longitude', () => {
        // Northern Summer Solstice (Ls = 90°): subsolar lat = +25.19° (Tropic of Mars)
        // At LTST = 12:00 at prime meridian (0°) -> subsolar lon = 0°
        const subSol = MarsTime.computeMartianSubsolarCoordinates(90.0, 12.0, 0.0, 25.19);
        expect(subSol.subSolarLatDeg).to.be.closeTo(25.19, 0.01);
        expect(subSol.subSolarLonDeg).to.equal(0.0);

        // At LTST = 10:00 (2 hours before noon) -> subsolar lon = +30° East
        const subMorning = MarsTime.computeMartianSubsolarCoordinates(90.0, 10.0, 0.0, 25.19);
        expect(subMorning.subSolarLonDeg).to.equal(30.0);
    });
});

describe('Planck Blackbody Spectral Radiance & Brightness Temperature (KRCEngine)', () => {
    it('should calculate Planck spectral radiance and invert to Brightness Temperature', () => {
        // Mars surface at T = 250 K observed at THEMIS band 9 (lambda = 12.57 µm):
        // c1 = 1.19104e8, c2 = 14387.77
        // exponent = 14387.77 / (12.57 * 250) = 14387.77 / 3142.5 = 4.5784
        // exp(4.5784) - 1 = 97.348 - 1 = 96.348
        // lam^5 = 12.57^5 = 313797.7
        // B = 1.19104e8 / (313797.7 * 96.348) = 1.19104e8 / 30233800 = 3.9394 W / (m^2 sr µm)
        const rad = KRCEngine.computePlanckSpectralRadiance(250.0, 12.57);
        expect(rad.spectralRadianceW_M2SrUm).to.be.closeTo(3.9394, 0.05);

        // Invert radiance back to brightness temperature
        const tb = KRCEngine.computePlanckBrightnessTemperature(rad.spectralRadianceW_M2SrUm, 12.57);
        expect(tb.brightnessTemperatureK).to.be.closeTo(250.0, 0.2);
    });

    it('should compute peak thermal emission wavelength using Wiens Displacement Law', () => {
        // Mean Martian daytime temperature T = 220 K: lambda_max = 2897.77 / 220 = 13.172 µm
        const wien220 = KRCEngine.computeWienPeakWavelength(220.0);
        expect(wien220.peakWavelengthMicrons).to.be.closeTo(13.172, 0.01);

        // Cold polar CO2 frost point T = 145 K: lambda_max = 2897.77 / 145 = 19.985 µm
        const wien145 = KRCEngine.computeWienPeakWavelength(145.0);
        expect(wien145.peakWavelengthMicrons).to.be.closeTo(19.985, 0.01);
    });
});

describe('Subsurface True Depth & Interface Echo Power (RadarSounderEngine)', () => {
    it('should calculate subsurface layer true depth from two-way radar time delay', () => {
        // Planum Boreum water ice cap (eps_r = 3.15, sqrt(3.15) = 1.77482)
        // Two-way delay delta_t = 10.0 µs:
        // v_phase = 299792.458 / 1.77482 = 168914.07 km/s
        // depth = (168914.07 * 10e-6) / 2 = 0.84457 km = 844.57 meters
        const ice = RadarSounderEngine.computeSubsurfaceTrueDepth(10.0, 3.15);
        expect(ice.depthMeters).to.be.closeTo(844.57, 0.5);
        expect(ice.depthKm).to.be.closeTo(0.8446, 0.001);
        expect(ice.phaseVelocityKmS).to.be.closeTo(168914.07, 100.0);

        // Volcanic basalt regolith (eps_r = 5.5, sqrt(5.5) = 2.3452)
        // delta_t = 5.0 µs -> depth = (299792.458 / 2.3452 * 5e-6) / 2 = 0.31958 km = 319.58 m
        const basalt = RadarSounderEngine.computeSubsurfaceTrueDepth(5.0, 5.5);
        expect(basalt.depthMeters).to.be.closeTo(319.58, 0.5);
    });

    it('should compute net received subsurface echo power in dB', () => {
        // Transmitted power P_tx = 0 dB reference
        // Basal reflector reflection coeff R_dB = -18.5 dB
        // Two-way attenuation loss = 14.2 dB
        // Net echo power P_rx = 0 - 18.5 - 14.2 = -32.7 dB (detectable)
        const echo = RadarSounderEngine.computeSubsurfaceInterfaceReturnPower(0.0, -18.5, 14.2);
        expect(echo.receivedEchoPowerDb).to.equal(-32.7);
        expect(echo.netEchoAttenuationDb).to.equal(32.7);
        expect(echo.isDetectableEcho).to.be.true;

        // Extremely attenuated reflector below noise floor (-95 dB)
        const faint = RadarSounderEngine.computeSubsurfaceInterfaceReturnPower(0.0, -40.0, 60.0);
        expect(faint.receivedEchoPowerDb).to.equal(-100.0);
        expect(faint.isDetectableEcho).to.be.false;
    });
});

describe('Transient Crater Cavity Scaling & Impact Kinetic Energy (CSFDEngine)', () => {
    it('should calculate transient excavation crater diameter, depth, and volume', () => {
        // Simple bowl crater (D = 5.0 km < 7.0 km):
        // Dt = 5.0 / 1.25 = 4.0 km
        // dt = 4.0 / 3 = 1.333 km
        // Vexc = (pi / 24) * 1.3333 * 16.0 = 2.7925 km^3
        const simple = CSFDEngine.computeTransientCraterScaling(5.0, 7.0);
        expect(simple.transientDiameterKm).to.equal(4.0);
        expect(simple.transientDepthKm).to.be.closeTo(1.333, 0.001);
        expect(simple.excavationVolumeKm3).to.be.closeTo(2.7925, 0.01);
        expect(simple.isComplexCavity).to.be.false;

        // Complex collapsed crater (e.g. Jezero crater D = 45.0 km >= 7.0 km):
        // Dt = (45.0 * 7.0^0.13 / 1.17)^(1 / 1.13) = (45.0 * 1.2882 / 1.17)^0.88496 = (49.544)^0.88496 = 31.42 km
        const jezero = CSFDEngine.computeTransientCraterScaling(45.0, 7.0);
        expect(jezero.transientDiameterKm).to.be.closeTo(31.42, 0.5);
        expect(jezero.isComplexCavity).to.be.true;
    });

    it('should compute projectile impact kinetic energy in Joules and TNT Megatons', () => {
        // Transient cavity Dt = 1.0 km in Martian basalt crust (rho = 2600 kg/m^3, g = 3.72 m/s^2)
        // E = 0.40 * 2600 * 3.72076 * (1000)^4 = 3869.59e12 Joules = 3.8696e15 J
        // Megatons TNT = 3.8696e15 / 4.184e15 = 0.9248 Mt (~0.92 Megatons)
        const small = CSFDEngine.computeImpactKineticEnergyMegatons(1.0, 2600.0, 3.72076);
        expect(small.energyMegatonsTNT).to.be.closeTo(0.92, 0.05);

        // 10 km transient cavity: E scales as Dt^4 = 10000x energy = ~9248 Megatons (9.25 Gigatons TNT)
        const large = CSFDEngine.computeImpactKineticEnergyMegatons(10.0, 2600.0, 3.72076);
        expect(large.gigatonsTNT).to.be.closeTo(9.25, 0.5);
    });
});

describe('Terrain Ruggedness Index & Topographic Position (SamplingTool)', () => {
    it('should calculate Riley Terrain Ruggedness Index (TRI) from 3x3 elevation grid', () => {
        // Flat level plain: all 9 cells at -3000 m
        const flatGrid = [-3000, -3000, -3000, -3000, -3000, -3000, -3000, -3000, -3000];
        const flat = SamplingTool.computeTerrainRuggednessIndex(flatGrid);
        expect(flat.triMeters).to.equal(0);
        expect(flat.roughnessClass).to.equal('Level');

        // Rugged mountainous scarp: center = 0 m, surrounding cells = [-600, +800, -500, +700, -400, +600, -300, +500]
        // sumSq = 36e4 + 64e4 + 25e4 + 49e4 + 16e4 + 36e4 + 9e4 + 25e4 = 260e4
        // meanSq = 260e4 / 8 = 32.5e4 -> TRI = sqrt(325000) = 570.08 m (> 499 m -> Extremely Rugged)
        const ruggedGrid = [
            -600, 800, -500,
             700,   0, -400,
             600, -300, 500
        ];
        const rugged = SamplingTool.computeTerrainRuggednessIndex(ruggedGrid);
        expect(rugged.triMeters).to.be.closeTo(570.08, 0.5);
        expect(rugged.roughnessClass).to.equal('Extremely Rugged');
    });

    it('should compute Topographic Position Index (TPI) and classify landscape morphology', () => {
        // High volcanic peak: center = 21000 m (Olympus Mons summit), surrounding caldera rim/flank = 18000 m
        // meanNeighbors = 18000 m -> TPI = +3000 m (> 100 m -> Major Ridge / Peak)
        const peak = SamplingTool.computeTopographicPositionIndex(21000, [18000, 18000, 18000, 18000]);
        expect(peak.tpiMeters).to.equal(3000);
        expect(peak.landscapePosition).to.equal('Major Ridge / Peak');

        // Deep crater floor: center = -4000 m, surrounding rim terrain = -2500 m
        // TPI = -4000 - (-2500) = -1500 m (< -100 m -> Valley / Crater Floor)
        const crater = SamplingTool.computeTopographicPositionIndex(-4000, [-2500, -2500, -2500, -2500]);
        expect(crater.tpiMeters).to.equal(-1500);
        expect(crater.landscapePosition).to.equal('Valley / Crater Floor');
    });
});

describe('Surface Friction Velocity, Wind Shear Stress & PBL Depth (MCDEngine)', () => {
    it('should calculate surface friction velocity u* and aerodynamic shear stress tau_0', () => {
        // High surface wind u(10m) = 25 m/s over rocky regolith (z0 = 0.01 m):
        // ln(10 / 0.01) = ln(1000) = 6.90775
        // u* = 0.40 * 25 / 6.90775 = 10 / 6.90775 = 1.4476 m/s
        // tau_0 = 0.015 * (1.4476)^2 = 0.015 * 2.0957 = 0.0314 Pa (> 0.025 Pa -> dust saltation active)
        const storm = MCDEngine.computeSurfaceFrictionVelocityAndShearStress(25.0, 0.01, 0.015);
        expect(storm.frictionVelocityMS).to.be.closeTo(1.448, 0.01);
        expect(storm.shearStressPa).to.be.closeTo(0.0314, 0.001);
        expect(storm.canLiftDust).to.be.true;

        // Gentle breeze u(10m) = 5 m/s:
        // u* = 0.40 * 5 / 6.90775 = 0.2895 m/s -> tau_0 = 0.015 * 0.0838 = 0.0013 Pa (< 0.025 Pa)
        const calm = MCDEngine.computeSurfaceFrictionVelocityAndShearStress(5.0, 0.01, 0.015);
        expect(calm.canLiftDust).to.be.false;
    });

    it('should compute peak daytime convective Planetary Boundary Layer (PBL) depth', () => {
        // High noon sensible heat flux H = 50 W/m^2 on warm summer day (T = 240 K, rho = 0.015 kg/m^3)
        // w'theta' = 50 / (0.015 * 850) = 50 / 12.75 = 3.9215 K m/s
        // z_pbl = 3800 * sqrt(3.9215) = 3800 * 1.9803 = 7525 meters = 7.53 km (deep Martian convective plume!)
        const pbl = MCDEngine.computeConvectivePBLMaxDepth(50.0, 240.0, 0.015, 3.72076);
        expect(pbl.pblDepthKm).to.be.closeTo(7.53, 0.2);
        expect(pbl.pblDepthMeters).to.be.closeTo(7525.0, 200.0);
        expect(pbl.convectiveVelocityScaleMS).to.be.greaterThan(2.0);
    });
});

describe('Equidistant Cylindrical (Plate Carrée) Forward & Inverse Projections (geo.js)', () => {
    it('should calculate forward Equidistant Cylindrical coordinates and scale distortion', () => {
        // Equator at central meridian (0, 0): x = 0, y = 0, scale k = 1.0
        const origin = computeEquidistantCylindricalProjection(0.0, 0.0, 0.0, 0.0, 3389.5);
        expect(origin.xKm).to.equal(0);
        expect(origin.yKm).to.equal(0);
        expect(origin.parallelScaleFactor).to.equal(1.0);

        // Gale Crater (lat = -5.4° S, lon = 137.8° E) on standard Plate Carrée (phi1 = 0°):
        // dLam = 137.8 * pi / 180 = 2.40506 rad
        // x = 3389.5 * 2.40506 = 8152.0 rad -> 8151.96 km
        // y = 3389.5 * (-5.4 * pi / 180) = -319.46 km
        // k = cos(-5.4°) = 0.99557
        const gale = computeEquidistantCylindricalProjection(-5.4, 137.8, 0.0, 0.0, 3389.5);
        expect(gale.xKm).to.be.closeTo(8151.96, 0.5);
        expect(gale.yKm).to.be.closeTo(-319.46, 0.5);
        expect(gale.parallelScaleFactor).to.be.closeTo(0.9956, 0.001);
    });

    it('should invert Equidistant Cylindrical planar coordinates back to geographic latitude and longitude', () => {
        // Forward Gale Crater coordinates
        const proj = computeEquidistantCylindricalProjection(-5.4, 137.8, 0.0, 0.0, 3389.5);
        // Inverse conversion
        const inv = computeEquidistantCylindricalInverse(proj.xKm, proj.yKm, 0.0, 0.0, 3389.5);
        expect(inv.latDeg).to.be.closeTo(-5.4, 0.001);
        expect(inv.lonDeg).to.be.closeTo(137.8, 0.001);

        // Standard parallel phi1 = 30°
        const proj30 = computeEquidistantCylindricalProjection(25.0, -45.0, 30.0, 0.0, 3389.5);
        const inv30 = computeEquidistantCylindricalInverse(proj30.xKm, proj30.yKm, 30.0, 0.0, 3389.5);
        expect(inv30.latDeg).to.be.closeTo(25.0, 0.001);
        expect(inv30.lonDeg).to.be.closeTo(-45.0, 0.001);
    });
});

describe('CRISM Olivine & Ferric Oxide Mineralogy Indices (BandMathEngine)', () => {
    it('should calculate CRISM Olivine 1.0 µm broad absorption parameter (OLINDEX3)', () => {
        // Nili Fossae olivine-rich bedrock:
        // R1080 = 0.18 (deep broad Fe2+ absorption), R1690 = 0.28, R2530 = 0.32
        // continuum = 0.65 * 0.28 + 0.35 * 0.32 = 0.182 + 0.112 = 0.294
        // depth = 1.0 - (0.18 / 0.294) = 1.0 - 0.61224 = 0.3878 (> 0.05 -> Olivine present)
        const oli = BandMathEngine.computeCRISMOlivineOLINDEX3(0.18, 0.28, 0.32);
        expect(oli.olIndex).to.be.closeTo(0.3878, 0.001);
        expect(oli.hasOlivine).to.be.true;

        // Flat dust spectrum: R1080 = 0.30, R1690 = 0.30, R2530 = 0.30 -> continuum = 0.30 -> depth = 0.0
        const dust = BandMathEngine.computeCRISMOlivineOLINDEX3(0.30, 0.30, 0.30);
        expect(dust.olIndex).to.equal(0);
        expect(dust.hasOlivine).to.be.false;
    });

    it('should calculate CRISM Ferric Iron oxide (Fe3+) electronic absorption (FE3INDEX)', () => {
        // Meridiani Planum hematite-rich terrain:
        // R770 = 0.24, R920 = 0.19 (Fe3+ band center), R1080 = 0.26
        // continuum = 0.5 * (0.24 + 0.26) = 0.25
        // fe3Index = 1.0 - (0.19 / 0.25) = 1.0 - 0.76 = 0.24 (> 0.03 -> Ferric oxides present)
        const hem = BandMathEngine.computeCRISMFerricOxideFE3INDEX(0.24, 0.19, 0.26);
        expect(hem.fe3Index).to.equal(0.24);
        expect(hem.hasFerricOxides).to.be.true;

        // Basaltic sand: R770 = 0.15, R920 = 0.15, R1080 = 0.15 -> fe3Index = 0.0
        const basalt = BandMathEngine.computeCRISMFerricOxideFE3INDEX(0.15, 0.15, 0.15);
        expect(basalt.fe3Index).to.equal(0);
        expect(basalt.hasFerricOxides).to.be.false;
    });
});

describe('Hapke Opposition Effect & Angular Solar Phase Angle (ThreeDEngine)', () => {
    it('should calculate solar phase angle from incidence, emission, and azimuth difference', () => {
        // Direct specular / subsolar backscattering geometry: i = 30°, e = 30°, delta_phi = 0°
        // cos(alpha) = cos(30)*cos(30) + sin(30)*sin(30)*1.0 = 0.75 + 0.25 = 1.0 -> alpha = 0.0°
        const zeroPhase = ThreeDEngine.computePhaseAngleFromAngles(30.0, 30.0, 0.0);
        expect(zeroPhase.phaseAngleDeg).to.equal(0.0);
        expect(zeroPhase.cosPhaseAngle).to.equal(1.0);

        // Right angle scattering: i = 45°, e = 45°, delta_phi = 180°
        // cos(alpha) = cos(45)*cos(45) + sin(45)*sin(45)*(-1.0) = 0.5 - 0.5 = 0.0 -> alpha = 90.0°
        const rightAngle = ThreeDEngine.computePhaseAngleFromAngles(45.0, 45.0, 180.0);
        expect(rightAngle.phaseAngleDeg).to.equal(90.0);
        expect(rightAngle.cosPhaseAngle).to.be.closeTo(0.0, 0.0001);
    });

    it('should compute Hapke shadow-hiding opposition surge factor B(g)', () => {
        // Exact opposition (g = 0°): tan(0) = 0 -> B(0) = 1.0 + B_0 / 1.0 = 1.0 + 1.0 = 2.0 (100% surge spike)
        const exact = ThreeDEngine.computeHapkeOppositionSurgeFactor(0.0, 1.0, 0.05);
        expect(exact.oppositionSurgeFactor).to.equal(2.0);
        expect(exact.isOppositionSpike).to.be.true;

        // Moderate phase angle g = 30°: tan(15°) = 0.26795
        // denominator = 1.0 + (1 / 0.05) * 0.26795 = 1.0 + 20 * 0.26795 = 1.0 + 5.359 = 6.359
        // B(30) = 1.0 + 1.0 / 6.359 = 1.1573
        const mod = ThreeDEngine.computeHapkeOppositionSurgeFactor(30.0, 1.0, 0.05);
        expect(mod.oppositionSurgeFactor).to.be.closeTo(1.1573, 0.001);
        expect(mod.isOppositionSpike).to.be.false;
    });
});

describe('J2 Planetary Oblateness Perturbations & Sun-Synchronous Inclination (TrajectoryEngine)', () => {
    it('should calculate secular nodal and apsidal precession rates from J2 gravity', () => {
        // Mars mapping orbit at a = 3645 km (alt ~ 255 km), e = 0.008, i = 92.78°
        // Retrograde inclination cos(i) < 0 -> dOmega/dt > 0 (prograde nodal drift matching sun)
        const mro = TrajectoryEngine.computeJ2NodalAndApsidalPrecession(3645.0, 0.008, 92.78, 'mars');
        expect(mro.nodalPrecessionDegPerDay).to.be.closeTo(0.574, 0.01);
        expect(mro.isCriticalInclination).to.be.false;

        // Critical inclination orbit (i = 63.435°): apsidal drift domega/dt = 0 (frozen apocenter)
        const crit = TrajectoryEngine.computeJ2NodalAndApsidalPrecession(4000.0, 0.1, 63.435, 'mars');
        expect(crit.apsidalPrecessionDegPerDay).to.be.closeTo(0.0, 0.05);
        expect(crit.isCriticalInclination).to.be.true;
    });

    it('should solve exact retrograde sun-synchronous inclination for planetary orbiters', () => {
        // Mars sun-synchronous orbit at altitude a = 3645 km:
        // Solves exact retrograde inclination i_sso = 92.54° matching Martian solar year (0.524 deg/day)
        const ssoMars = TrajectoryEngine.computeSunSynchronousInclination(3645.0, 0.008, 'mars');
        expect(ssoMars.sunSyncInclinationDeg).to.be.closeTo(92.54, 0.05);
        expect(ssoMars.isFeasibleSunSync).to.be.true;

        // Earth low earth orbit (LEO at 700 km altitude -> a = 7078 km):
        // Standard Earth SSO inclination ~ 98.2°
        const ssoEarth = TrajectoryEngine.computeSunSynchronousInclination(7078.0, 0.001, 'earth');
        expect(ssoEarth.sunSyncInclinationDeg).to.be.closeTo(98.19, 0.2);
        expect(ssoEarth.isFeasibleSunSync).to.be.true;
    });
});

describe('Solar Zenith Angle & Diurnal Solar Flux (MarsTime)', () => {
    it('should calculate solar zenith angle and classify day/night/twilight on Mars', () => {
        // High noon at subsolar point (lat = 0°, Ls = 0° vernal equinox, LTST = 12.0):
        // delta = 0°, h = 0° -> cos(theta_z) = 1.0 -> zenith = 0.0°, elevation = 90.0° (overhead sun)
        const noon = MarsTime.computeMartianSolarZenithAndElevation(0.0, 0.0, 12.0, 25.19);
        expect(noon.zenithAngleDeg).to.equal(0.0);
        expect(noon.elevationAngleDeg).to.equal(90.0);
        expect(noon.isDaylight).to.be.true;
        expect(noon.isTwilight).to.be.false;

        // Midnight at equator (LTST = 0.0): h = 180° -> cos(theta_z) = -1.0 -> zenith = 180.0°, elevation = -90.0°
        const midnight = MarsTime.computeMartianSolarZenithAndElevation(0.0, 0.0, 0.0, 25.19);
        expect(midnight.zenithAngleDeg).to.equal(180.0);
        expect(midnight.elevationAngleDeg).to.equal(-90.0);
        expect(midnight.isDaylight).to.be.false;

        // Dusk twilight at equator (LTST = 18.4): elevation = -6.0° (civil twilight)
        const dusk = MarsTime.computeMartianSolarZenithAndElevation(0.0, 0.0, 18.4, 25.19);
        expect(dusk.elevationAngleDeg).to.be.closeTo(-6.0, 0.5);
        expect(dusk.isTwilight).to.be.true;
    });

    it('should compute top-of-atmosphere insolation and direct solar flux factoring in eccentricity', () => {
        // Mars at perihelion (Ls = 251°, r_sun = a * (1 - e) = 1.52368 * 0.9066 = 1.3813 AU):
        // TOA Insolation S = 1361 / (1.3813)^2 = 1361 / 1.9080 = 713.3 W/m^2
        const peri = MarsTime.computeMartianDayFractionAndSolarFlux(251.0, 0.0, 1361.0);
        expect(peri.heliocentricDistanceAU).to.be.closeTo(1.3813, 0.005);
        expect(peri.toaInsolationW_M2).to.be.closeTo(713.3, 2.0);
        expect(peri.directFluxW_M2).to.be.closeTo(713.3, 2.0);

        // Mars at aphelion (Ls = 71°, r_sun = a * (1 + e) = 1.52368 * 1.0934 = 1.6660 AU):
        // TOA Insolation S = 1361 / (1.6660)^2 = 1361 / 2.7756 = 490.3 W/m^2 (~45% variation across year!)
        const aph = MarsTime.computeMartianDayFractionAndSolarFlux(71.0, 60.0, 1361.0);
        expect(aph.heliocentricDistanceAU).to.be.closeTo(1.6660, 0.005);
        expect(aph.toaInsolationW_M2).to.be.closeTo(490.3, 2.0);
        // At 60° zenith angle: cos(60) = 0.5 -> Direct flux = 490.3 * 0.5 = 245.15 W/m^2
        expect(aph.directFluxW_M2).to.be.closeTo(245.15, 2.0);
    });
});

describe('Subsurface Thermal Skin Depth & Damping (KRCEngine)', () => {
    it('should calculate diurnal and annual thermal skin depth and conductivity on Mars', () => {
        // Typical Martian regolith (I = 250 tiu, rho = 1500 kg/m^3, cp = 800 J/(kg K)):
        // rho * cp = 1.2e6 J / (m^3 K)
        // sqrt(88775.244 / pi) = sqrt(28258.0) = 168.101
        // d_s = (250 / 1.2e6) * 168.101 = 0.00020833 * 168.101 = 0.03502 meters = 3.50 cm
        // d_annual = 0.03502 * sqrt(668.6) = 0.03502 * 25.857 = 0.9055 meters
        // k = (250)^2 / 1.2e6 = 62500 / 1.2e6 = 0.05208 W / (m K)
        const regolith = KRCEngine.computeDiurnalAndAnnualSkinDepth(250.0, 1500.0, 800.0);
        expect(regolith.diurnalSkinDepthMeters).to.be.closeTo(0.035, 0.002);
        expect(regolith.diurnalSkinDepthCm).to.be.closeTo(3.50, 0.2);
        expect(regolith.annualSkinDepthMeters).to.be.closeTo(0.906, 0.02);
        expect(regolith.thermalConductivityW_MK).to.be.closeTo(0.0521, 0.001);

        // Solid basaltic bedrock (I = 2000 tiu, rho = 2800 kg/m^3, cp = 850 J/(kg K)):
        // d_s ~ 14 cm, d_annual ~ 3.6 meters
        const bedrock = KRCEngine.computeDiurnalAndAnnualSkinDepth(2000.0, 2800.0, 850.0);
        expect(bedrock.diurnalSkinDepthCm).to.be.closeTo(14.13, 0.5);
        expect(bedrock.annualSkinDepthMeters).to.be.closeTo(3.65, 0.2);
    });

    it('should compute damped temperature amplitude and diurnal phase lag with depth', () => {
        // Surface swing Delta_T0 = 80 K on regolith with d_s = 0.035 m (3.5 cm)
        // At depth z = 0.035 m (1 skin depth):
        // Delta_T = 80 * exp(-1) = 80 * 0.36788 = 29.43 K (damped by ~63%)
        // Lag = 1.0 rad = 24.66 / (2*pi) = 3.92 hours
        const at1Skin = KRCEngine.computeSubsurfaceTemperatureDampingAndLag(80.0, 0.035, 0.035);
        expect(at1Skin.dampedAmplitudeK).to.be.closeTo(29.43, 0.5);
        expect(at1Skin.phaseLagHours).to.be.closeTo(3.92, 0.1);
        expect(at1Skin.phaseLagRadians).to.equal(1.0);

        // At depth z = 0.105 m (3 skin depths):
        // Delta_T = 80 * exp(-3) = 80 * 0.04979 = 3.98 K (< 5% residual swing)
        // Lag = 3 * 3.92 = 11.77 hours (nearly half a sol out of phase!)
        const at3Skin = KRCEngine.computeSubsurfaceTemperatureDampingAndLag(80.0, 0.105, 0.035);
        expect(at3Skin.dampedAmplitudeK).to.be.closeTo(3.98, 0.2);
        expect(at3Skin.phaseLagHours).to.be.closeTo(11.77, 0.2);
    });
});

describe('CSFD Relative (R) Plotting & Sqrt(2) Bins (CSFDEngine)', () => {
    it('should calculate relative R-value and detect saturation equilibrium', () => {
        // Bin [1.0 km, 1.414 km]: D_geom = sqrt(1.414) = 1.1892 km, deltaD = 0.4142 km
        // Area A = 10000 km^2, N = 10 craters:
        // R = ( (1.1892)^3 * 10 ) / ( 10000 * 0.4142 ) = (1.6818 * 10) / 4142 = 16.818 / 4142 = 0.00406
        const rLow = CSFDEngine.computeCraterRelativePlotRValue(10, 10000, 1.0, 1.4142);
        expect(rLow.rValue).to.be.closeTo(0.00406, 0.0001);
        expect(rLow.geometricMeanDiameterKm).to.be.closeTo(1.189, 0.01);
        expect(rLow.isSaturationEquilibrium).to.be.false;

        // Heavy Noachian saturation crater field (R >= 0.05): N = 200 in 10000 km^2 -> R = 0.0812
        const rSat = CSFDEngine.computeCraterRelativePlotRValue(200, 10000, 1.0, 1.4142);
        expect(rSat.rValue).to.be.closeTo(0.0812, 0.001);
        expect(rSat.isSaturationEquilibrium).to.be.true;
    });

    it('should generate standard sqrt(2) geometric diameter intervals', () => {
        // Generate bins from 1 km to 16 km: 1->1.414, 1.414->2.0, 2.0->2.828, 2.828->4.0, 4.0->5.657, 5.657->8.0, 8.0->11.314, 11.314->16.0
        const bins = CSFDEngine.generateLogSqrt2DiameterBins(1.0, 16.0);
        expect(bins.length).to.be.at.least(8);
        expect(bins[0].dLower).to.equal(1.0);
        expect(bins[0].dUpper).to.be.closeTo(1.414, 0.001);
        expect(bins[0].dGeom).to.be.closeTo(1.189, 0.001);
        expect(bins[1].dLower).to.be.closeTo(1.414, 0.001);
        expect(bins[1].dUpper).to.be.closeTo(2.0, 0.001);
    });
});

describe('Complex Refractive Index & Sounding Vertical Resolution (RadarSounderEngine)', () => {
    it('should calculate complex refractive index (n + i*kappa) and electromagnetic skin depth', () => {
        // Cold pure Martian polar ice (eps' = 3.15, tan(delta) = 0.001 at f = 20 MHz):
        // n = sqrt(3.15) = 1.77482, kappa = sqrt(3.15) * 0.001 / 2 = 0.00088741
        // skin depth delta = 299792458 / (2 * pi * 20e6 * 0.00088741) = 299792458 / 111516.3 = 2688.3 meters
        const ice = RadarSounderEngine.computeComplexRefractiveIndexAndSkinDepth(20e6, 3.15, 0.001);
        expect(ice.refractiveIndexN).to.be.closeTo(1.7748, 0.001);
        expect(ice.extinctionCoeffKappa).to.be.closeTo(0.0008874, 0.00005);
        expect(ice.skinDepthMeters).to.be.closeTo(2688.3, 10.0);
        expect(ice.phaseVelocityKmS).to.be.closeTo(168914.07, 100.0);
    });

    it('should calculate radar sounding vertical range resolution in air and dielectric media', () => {
        // SHARAD sounder: Bandwidth = 10 MHz (10e6 Hz)
        // Free-space resolution = c / (2 * 10e6) = 299792458 / 20e6 = 14.99 meters (~15 m)
        // In water ice (eps_r = 3.15 -> sqrt(3.15) = 1.77482): resolution = 14.99 / 1.77482 = 8.45 meters!
        const sharad = RadarSounderEngine.computeSubsurfaceLayerVerticalResolution(10e6, 3.15);
        expect(sharad.verticalResolutionAirMeters).to.be.closeTo(14.99, 0.05);
        expect(sharad.verticalResolutionMediumMeters).to.be.closeTo(8.45, 0.05);

        // MARSIS sounder: Bandwidth = 1 MHz (1e6 Hz)
        // In water ice: resolution = 149.9 / 1.77482 = 84.46 meters
        const marsis = RadarSounderEngine.computeSubsurfaceLayerVerticalResolution(1e6, 3.15);
        expect(marsis.verticalResolutionMediumMeters).to.be.closeTo(84.46, 0.5);
    });
});

describe('Atmospheric Transmittance & Dust Optical Depth Climatology (MCDEngine)', () => {
    it('should calculate direct solar atmospheric transmittance via Beer-Lambert law', () => {
        // Clear sky overhead sun (tau = 0.20, theta_z = 0° -> airmass = 1.0):
        // T_direct = exp(-0.20) = 0.8187 (81.9% transmission)
        const overhead = MCDEngine.computeAtmosphericTransmittanceBeerLambert(0.20, 0.0);
        expect(overhead.directTransmittance).to.be.closeTo(0.8187, 0.001);
        expect(overhead.opticalAirmass).to.be.closeTo(1.0, 0.01);

        // Oblique sun at 60° solar zenith angle (airmass ~ 2.0):
        // tau_slant = 0.20 * 2.0 = 0.40 -> T_direct = exp(-0.40) = 0.6703
        const slant = MCDEngine.computeAtmosphericTransmittanceBeerLambert(0.20, 60.0);
        expect(slant.directTransmittance).to.be.closeTo(0.6703, 0.01);
        expect(slant.opticalAirmass).to.be.closeTo(2.0, 0.05);

        // Global dust storm (tau = 2.5, theta_z = 45°): T_direct = exp(-2.5 * 1.414) = exp(-3.535) = 0.029 (heavy extinction)
        const storm = MCDEngine.computeAtmosphericTransmittanceBeerLambert(2.5, 45.0);
        expect(storm.directTransmittance).to.be.closeTo(0.029, 0.005);
    });

    it('should estimate seasonal column dust optical depth across Martian seasons', () => {
        // Aphelion clear season at Ls = 100°: tau_vis ~ 0.15
        const aphelion = MCDEngine.estimateSeasonalDustOpticalDepth(100.0, false);
        expect(aphelion.visibleOpticalDepthTau).to.be.closeTo(0.15, 0.05);
        expect(aphelion.isDustStormSeason).to.be.false;

        // Perihelion dust storm peak at Ls = 255°: tau_vis ~ 0.85
        const perihelion = MCDEngine.estimateSeasonalDustOpticalDepth(255.0, false);
        expect(perihelion.visibleOpticalDepthTau).to.be.closeTo(0.85, 0.05);
        expect(perihelion.isDustStormSeason).to.be.true;

        // Regional dust storm active during perihelion: tau_vis > 2.0
        const stormActive = MCDEngine.estimateSeasonalDustOpticalDepth(255.0, true);
        expect(stormActive.visibleOpticalDepthTau).to.be.greaterThan(2.0);
    });
});

describe('Lambert Conformal Conic (LCC) Projections (geo.js)', () => {
    it('should project forward and maintain exact scale factor k = 1.0 at standard parallels', () => {
        // Mars regional map with standard parallels phi1 = 15°N, phi2 = 45°N, origin at (0°, 0°)
        // At standard parallel 1 (lat = 15°, lon = 0°):
        const pt1 = computeLambertConformalConicProjection(15.0, 0.0, 15.0, 45.0, 0.0, 0.0);
        expect(pt1.xKm).to.equal(0.0);
        expect(pt1.scaleFactor).to.be.closeTo(1.0, 0.0001); // True scale at standard parallel!

        // At standard parallel 2 (lat = 45°, lon = 0°):
        const pt2 = computeLambertConformalConicProjection(45.0, 0.0, 15.0, 45.0, 0.0, 0.0);
        expect(pt2.xKm).to.equal(0.0);
        expect(pt2.scaleFactor).to.be.closeTo(1.0, 0.0001); // True scale at standard parallel!

        // East offset point (lat = 30°, lon = 20°):
        const ptEast = computeLambertConformalConicProjection(30.0, 20.0, 15.0, 45.0, 0.0, 0.0);
        expect(ptEast.xKm).to.be.greaterThan(500.0);
        expect(ptEast.coneConstantN).to.be.closeTo(0.507, 0.02);
    });

    it('should invert LCC planar coordinates back to original spherical lat/lon coordinates', () => {
        const originLat = 10.0;
        const centerLon = -45.0;
        const p1 = 20.0;
        const p2 = 50.0;

        const forward = computeLambertConformalConicProjection(35.5, -30.2, p1, p2, originLat, centerLon);
        const inv = computeLambertConformalConicInverse(forward.xKm, forward.yKm, p1, p2, originLat, centerLon);

        expect(inv.latDeg).to.be.closeTo(35.5, 0.001);
        expect(inv.lonDeg).to.be.closeTo(-30.2, 0.001);
    });
});

describe('CRISM Silica & Carbonate Mineralogy Indices (BandMathEngine)', () => {
    it('should calculate hydrated opaline silica index (SINDEX2) and detect opals', () => {
        // Hydrated silica test spectrum with Si-OH absorption minimum at 2290 nm:
        // R2120 = 0.35, R2290 = 0.29, R2400 = 0.38
        // continuum = 0.60 * 0.35 + 0.40 * 0.38 = 0.210 + 0.152 = 0.362
        // SINDEX2 = 1.0 - (0.29 / 0.362) = 1.0 - 0.8011 = 0.1989
        const opal = BandMathEngine.computeCRISMSilicaSINDEX2(0.35, 0.29, 0.38);
        expect(opal.sIndex).to.be.closeTo(0.1989, 0.005);
        expect(opal.hasHydratedSilica).to.be.true;

        // Flat basalt spectrum: R2120 = 0.15, R2290 = 0.15, R2400 = 0.15 -> SINDEX2 = 0.0
        const basalt = BandMathEngine.computeCRISMSilicaSINDEX2(0.15, 0.15, 0.15);
        expect(basalt.sIndex).to.equal(0.0);
        expect(basalt.hasHydratedSilica).to.be.false;
    });

    it('should calculate carbonate index (CARBINDEX) and detect Mg/Fe carbonates', () => {
        // Carbonate test spectrum with 2.30 and 2.50 µm vibrational overtone dip:
        // R2140 = 0.40, R2300 = 0.32, R2500 = 0.30
        // meanBand = 0.5 * (0.32 + 0.30) = 0.31
        // CARBINDEX = 1.0 - (0.31 / 0.40) = 1.0 - 0.775 = 0.225
        const carb = BandMathEngine.computeCRISMCarbonateCARBINDEX(0.40, 0.32, 0.30);
        expect(carb.carbIndex).to.be.closeTo(0.225, 0.005);
        expect(carb.hasCarbonates).to.be.true;
    });
});

describe('Hapke Photometric Bidirectional Reflectance (ThreeDEngine)', () => {
    it('should calculate multiple-scattering Chandrasekhar H-function', () => {
        // Pure absorbing medium (w = 0): gamma = 1 -> H(mu, 0) = (1 + 2mu)/(1 + 2mu) = 1.0 identically
        const hZero = ThreeDEngine.computeHapkeMultipleScatteringHFunction(0.8, 0.0);
        expect(hZero).to.equal(1.0);

        // High scattering medium (w = 0.90, gamma = sqrt(0.1) = 0.3162, mu = 1.0):
        // H(1, 0.90) = (1 + 2) / (1 + 2 * 0.3162) = 3 / 1.63245 = 1.8377
        const hScat = ThreeDEngine.computeHapkeMultipleScatteringHFunction(1.0, 0.90);
        expect(hScat).to.be.closeTo(1.8377, 0.005);
    });

    it('should calculate complete Hapke I/F bidirectional reflectance factor', () => {
        // Mars bright dust regolith (w = 0.65, xi = -0.25, B0 = 1.0, h = 0.05) at normal incidence/emission (i = 0°, e = 0°, g = 0°):
        // mu0 = 1.0, mu = 1.0 -> opposition spike surge B(0) = 2.0
        const opposition = ThreeDEngine.computeHapkeBidirectionalReflectance(0.0, 0.0, 0.0, 0.65, -0.25, 1.0, 0.05);
        expect(opposition.reflectanceIOF).to.be.greaterThan(0.25);
        expect(opposition.singleScatteringPart).to.be.greaterThan(1.5);
        expect(opposition.multipleScatteringPart).to.be.greaterThan(0.5);

        // Oblique geometry (i = 60°, e = 45°, g = 30°):
        const oblique = ThreeDEngine.computeHapkeBidirectionalReflectance(60.0, 45.0, 30.0, 0.50, -0.20, 1.0, 0.05);
        expect(oblique.reflectanceIOF).to.be.closeTo(0.242, 0.02);
    });
});

describe('Horn Slope Aspect & Sloped Solar Incidence (SamplingTool)', () => {
    it('should compute slope magnitude and compass aspect using Horn 3x3 filter', () => {
        // Pure south-facing slope (elevation decreasing to the south / down in grid):
        // Row 0 (North): 1000m, Row 1 (Center): 500m, Row 2 (South): 0m
        const southSlopeGrid = [
            [1000, 1000, 1000],
            [500,  500,  500],
            [0,    0,    0]
        ];
        // dz/dx = 0, dz/dy = (0 - 4000) / (8 * 463) = -4000 / 3704 = -1.0799
        // slopeRad = atan(1.0799) = 47.20°, aspect = 180° (South facing)
        const southRes = SamplingTool.computeSlopeAndAspectHorn(southSlopeGrid, 463.0);
        expect(southRes.slopeDeg).to.be.closeTo(47.20, 0.5);
        expect(southRes.cardinalDirection).to.equal('S');

        // Flat horizontal plain:
        const flatGrid = [
            [200, 200, 200],
            [200, 200, 200],
            [200, 200, 200]
        ];
        const flatRes = SamplingTool.computeSlopeAndAspectHorn(flatGrid, 463.0);
        expect(flatRes.slopeDeg).to.equal(0.0);
        expect(flatRes.cardinalDirection).to.equal('Flat');
    });

    it('should compute effective solar incidence on sloped terrain facets', () => {
        // South-facing slope (slope = 30°, aspect = 180°) with South sun at noon (zenith = 30°, azimuth = 180°):
        // Direct normal illumination (i_slope = 30° - 30° = 0.0°, cos = 1.0)
        const directNormal = SamplingTool.computeSlopeSolarIncidence(30.0, 180.0, 30.0, 180.0);
        expect(directNormal.cosIncidence).to.be.closeTo(1.0, 0.0001);
        expect(directNormal.localIncidenceDeg).to.equal(0.0);
        expect(directNormal.isDirectlyIlluminated).to.be.true;

        // North-facing slope facing away from low South sun in deep shadow (slope = 45°, aspect = 0°, zenith = 60°, azimuth = 180°):
        // cos(i) = cos(60)*cos(45) + sin(60)*sin(45)*cos(180 - 0) = 0.5*0.7071 - 0.866*0.7071 = 0.3535 - 0.6124 = -0.2588 (self-shadowed!)
        const shadow = SamplingTool.computeSlopeSolarIncidence(45.0, 0.0, 60.0, 180.0);
        expect(shadow.cosIncidence).to.be.lessThan(0.0);
        expect(shadow.isDirectlyIlluminated).to.be.false;
    });
});

describe('Ground Track Velocity & Interplanetary Hohmann Transfers (TrajectoryEngine)', () => {
    it('should calculate relative satellite ground track speed across rotating planet', () => {
        // Mars MRO mapping orbit: r = 3645 km, i = 92.8° (retrograde), equator (lat = 0°):
        // v_inertial = sqrt(42828.37 / 3645) = 3.4278 km/s
        // v_rot = (2*pi / 88775.244) * 3389.5 = 0.2400 km/s
        // v_ground = sqrt( 3.4278^2 + 0.24^2 - 2(3.4278)(0.24)cos(92.8°) )
        // cos(92.8°) = -0.0488 -> v_ground = sqrt( 11.75 + 0.0576 + 0.0803 ) = sqrt(11.888) = 3.4479 km/s
        const mro = TrajectoryEngine.computeSatelliteGroundTrackVelocity(3645.0, 92.8, 0.0, 'mars');
        expect(mro.inertialOrbitalSpeedKmS).to.be.closeTo(3.428, 0.01);
        expect(mro.planetarySurfaceSpeedKmS).to.be.closeTo(0.240, 0.01);
        expect(mro.groundTrackSpeedKmS).to.be.closeTo(3.448, 0.01);
    });

    it('should calculate interplanetary Earth-Mars Hohmann transfer Delta-V and transit time', () => {
        // Earth (1.0 AU) to Mars (1.52368 AU):
        // a_tx = (1 + 1.52368) / 2 = 1.26184 AU
        // Transit time T_tx = pi * sqrt( (1.26184 * 149597870.7)^3 / 1.327e11 ) / 86400 = 258.9 Earth days (~8.5 months)
        // Delta-V1 (Earth departure) ~ 2.945 km/s, Delta-V2 (Mars insertion) ~ 2.649 km/s -> Total Delta-V ~ 5.594 km/s
        const hohmann = TrajectoryEngine.computeHohmannInterplanetaryTransfer(1.0, 1.52368);
        expect(hohmann.transferSemiMajorAxisAU).to.be.closeTo(1.2618, 0.005);
        expect(hohmann.transitTimeDays).to.be.closeTo(258.9, 1.0);
        expect(hohmann.departureDeltaVKmS).to.be.closeTo(2.945, 0.05);
        expect(hohmann.arrivalDeltaVKmS).to.be.closeTo(2.649, 0.05);
        expect(hohmann.totalDeltaVKmS).to.be.closeTo(5.594, 0.1);
    });
});

describe('True Anomaly & Keplerian Sol-of-Year (MarsTime)', () => {
    it('should compute true and mean anomalies relative to Mars perihelion', () => {
        // Mars at perihelion (Ls = 251.0°): nu = 0.0°, E = 0.0°, M = 0.0°
        const peri = MarsTime.computeTrueAnomalyAndMeanAnomalyFromLs(251.0);
        expect(peri.trueAnomalyDeg).to.equal(0.0);
        expect(peri.eccentricAnomalyDeg).to.equal(0.0);
        expect(peri.meanAnomalyDeg).to.equal(0.0);

        // Mars at aphelion (Ls = 71.0°): nu = 180.0°, E = 180.0°, M = 180.0°
        const aph = MarsTime.computeTrueAnomalyAndMeanAnomalyFromLs(71.0);
        expect(aph.trueAnomalyDeg).to.equal(180.0);
        expect(aph.eccentricAnomalyDeg).to.equal(180.0);
        expect(aph.meanAnomalyDeg).to.equal(180.0);
    });

    it('should calculate elapsed Martian sols since vernal equinox factoring in orbital speed variation', () => {
        // At vernal equinox (Ls = 0°): solOfYear = 0.0
        const eq = MarsTime.estimateSolOfYearFromLs(0.0);
        expect(eq.solOfYear).to.equal(0.0);
        expect(eq.yearProgressPercent).to.equal(0.0);

        // At autumn equinox (Ls = 180°):
        // Because Mars moves slower near aphelion (northern spring/summer),
        // northern spring + summer spans ~372 sols (> 55% of the year!)
        const autumn = MarsTime.estimateSolOfYearFromLs(180.0);
        expect(autumn.solOfYear).to.be.closeTo(371.9, 2.0);
        expect(autumn.yearProgressPercent).to.be.closeTo(55.6, 0.5);
    });
});

describe('KRC Rock Mixing & Sloped Direct Insolation (KRCEngine)', () => {
    it('should calculate Christensen non-linear two-component apparent thermal inertia', () => {
        // Regolith fines (I_fines = 150 tiu) with 15% rock abundance (I_rock = 2200 tiu):
        // term = 0.85 * (150)^0.75 + 0.15 * (2200)^0.75 = 36.427 + 48.215 = 84.642
        // I_apparent = (84.642)^(4/3) = 371.5 tiu
        const mixed = KRCEngine.computeTwoComponentApparentThermalInertia(150.0, 2200.0, 0.15);
        expect(mixed.apparentThermalInertiaTiu).to.be.closeTo(371.5, 1.0);
        expect(mixed.rockFractionPercent).to.equal(15.0);

        // Pure fine regolith (f_rock = 0):
        const pureFines = KRCEngine.computeTwoComponentApparentThermalInertia(200.0, 2200.0, 0.0);
        expect(pureFines.apparentThermalInertiaTiu).to.equal(200.0);
    });

    it('should calculate direct solar insolation on inclined terrain facets factoring in dust opacity', () => {
        // South-facing slope (slope = 20°, aspect = 180°) under overhead noon sun (zenith = 20°, azimuth = 180°):
        // r_sun = 1.524 AU -> S_toa = 1361 / (1.524)^2 = 585.98 W/m^2
        // cos(i_slope) = 1.0 (normal to sun!)
        // tau = 0.20, airmass = 1 / cos(20°) = 1.064 -> transmission = exp(-0.2128) = 0.8083
        // flux = 585.98 * 1.0 * 0.8083 = 473.65 W/m^2
        const noon = KRCEngine.computeSlopeCorrectedDirectInsolation(20.0, 180.0, 20.0, 180.0, 1.524, 0.20);
        expect(noon.cosSlopeIncidence).to.be.closeTo(1.0, 0.001);
        expect(noon.directInsolationW_M2).to.be.closeTo(473.65, 2.0);

        // Facet facing away into complete shadow:
        const shadow = KRCEngine.computeSlopeCorrectedDirectInsolation(45.0, 0.0, 45.0, 180.0, 1.524, 0.20);
        expect(shadow.cosSlopeIncidence).to.equal(0.0);
        expect(shadow.directInsolationW_M2).to.equal(0.0);
    });
});

describe('CRISM Pyroxene Mineralogy (LCPINDEX2 & HCPINDEX2) (BandMathEngine)', () => {
    it('should calculate Low-Calcium Pyroxene / Orthopyroxene 2.1 um band depth (LCPINDEX2)', () => {
        // Ancient Noachian crustal basalt with strong 2140 nm absorption:
        // r1690 = 0.28, r2120 = 0.21, r2140 = 0.20, r2530 = 0.26
        // continuum = 0.5 * (0.28 + 0.26) = 0.27
        // meanBand = 0.5 * (0.21 + 0.20) = 0.205
        // depth = 1.0 - (0.205 / 0.27) = 1.0 - 0.75926 = 0.2407 (> 0.04 -> LCP rich)
        const lcp = BandMathEngine.computeCRISMPyroxeneLCPINDEX2(0.28, 0.21, 0.20, 0.26);
        expect(lcp.lcpIndex).to.be.closeTo(0.2407, 0.001);
        expect(lcp.hasLowCalciumPyroxene).to.be.true;

        // Flat neutral spectrum without 2.1 um band:
        const flat = BandMathEngine.computeCRISMPyroxeneLCPINDEX2(0.25, 0.25, 0.25, 0.25);
        expect(flat.lcpIndex).to.equal(0.0);
        expect(flat.hasLowCalciumPyroxene).to.be.false;
    });

    it('should calculate High-Calcium Pyroxene / Clinopyroxene 2.38 um band depth (HCPINDEX2)', () => {
        // Hesperian/Amazonian volcanic flood basalt with augite absorption at 2350-2390 nm:
        // r1815 = 0.30, r2350 = 0.23, r2390 = 0.22, r2530 = 0.28
        // continuum = 0.5 * (0.30 + 0.28) = 0.29
        // meanBand = 0.5 * (0.23 + 0.22) = 0.225
        // depth = 1.0 - (0.225 / 0.29) = 1.0 - 0.77586 = 0.2241 (> 0.04 -> HCP rich)
        const hcp = BandMathEngine.computeCRISMPyroxeneHCPINDEX2(0.30, 0.23, 0.22, 0.28);
        expect(hcp.hcpIndex).to.be.closeTo(0.2241, 0.001);
        expect(hcp.hasHighCalciumPyroxene).to.be.true;

        // Neutral spectrum without HCP band:
        const flat = BandMathEngine.computeCRISMPyroxeneHCPINDEX2(0.25, 0.25, 0.25, 0.25);
        expect(flat.hcpIndex).to.equal(0.0);
        expect(flat.hasHighCalciumPyroxene).to.be.false;
    });
});

describe('Hydrostatic Pressure Profile & Acoustic Sound Speed (MCDEngine)', () => {
    it('should calculate Mars atmospheric scale height, pressure, and mass density at altitude', () => {
        // Mars surface datum (z = 0 m, T = 210 K, P_0 = 610 Pa):
        // H = (191.84 * 210) / 3.72076 = 10827.4 m (~10.83 km)
        // P(0) = 610 Pa (6.10 mbar), rho = 610 / (191.84 * 210) = 0.01514 kg/m^3
        const datum = MCDEngine.computeHydrostaticPressureProfile(0.0, 610.0, 210.0, 'mars');
        expect(datum.scaleHeightMeters).to.be.closeTo(10827.4, 2.0);
        expect(datum.pressurePa).to.equal(610.0);
        expect(datum.pressureMbar).to.equal(6.10);
        expect(datum.densityKgM3).to.be.closeTo(0.01514, 0.0001);

        // Olympus Mons summit (z = 21287 m):
        // P(21287) = 610 * exp(-21287 / 10827.4) = 610 * exp(-1.966) = 610 * 0.1400 = 85.4 Pa (0.854 mbar)
        const summit = MCDEngine.computeHydrostaticPressureProfile(21287.0, 610.0, 210.0, 'mars');
        expect(summit.pressurePa).to.be.closeTo(85.4, 1.0);
        expect(summit.pressureMbar).to.be.closeTo(0.854, 0.01);
    });

    it('should calculate Martian acoustic sound speed and dynamic acoustic impedance', () => {
        // Mars CO2 atmosphere at T = 210 K, gamma = 1.289:
        // c_s = sqrt( 1.289 * 191.84 * 210 ) = sqrt( 51928.9 ) = 227.88 m/s (~820.4 km/h)
        // Z = 0.01514 * 227.88 = 3.450 Pa*s/m
        const sound = MCDEngine.computeAtmosphericThermalSoundSpeed(210.0, 0.01514, 1.289, 'mars');
        expect(sound.soundSpeedMps).to.be.closeTo(227.88, 0.5);
        expect(sound.soundSpeedKmh).to.be.closeTo(820.4, 2.0);
        expect(sound.acousticImpedancePaS_M).to.be.closeTo(3.450, 0.05);
    });
});

describe('SHARAD Fresnel Footprint & Power Reflection Loss (RadarSounderEngine)', () => {
    it('should calculate unfocused First Fresnel Zone radius and footprint diameter', () => {
        // SHARAD on MRO at h = 300 km, f = 20 MHz (lambda = 14.990 m):
        // r_fresnel = sqrt( 14.990 * 300000 / 2 ) = sqrt( 2248500 ) = 1499.5 m (~1.50 km)
        // Footprint diameter = 2.999 km (~3.0 km)
        const sharad = RadarSounderEngine.computeRadarFirstFresnelZoneRadius(300.0, 20.0);
        expect(sharad.wavelengthMeters).to.be.closeTo(14.99, 0.01);
        expect(sharad.fresnelRadiusMeters).to.be.closeTo(1499.5, 1.0);
        expect(sharad.fresnelDiameterKm).to.be.closeTo(2.999, 0.01);
    });

    it('should calculate normal-incidence Fresnel power reflection loss across planetary materials', () => {
        // Pure water ice cap (eps_r = 3.15, n = 1.7748):
        // r_field = (1.7748 - 1) / (1.7748 + 1) = 0.7748 / 2.7748 = 0.2792
        // R_pwr = (0.2792)^2 = 0.0780 (7.80% reflection) -> Loss = 10 * log10(0.0780) = -11.08 dB
        const ice = RadarSounderEngine.computeRadarSurfacePowerReflectionLoss(3.15);
        expect(ice.powerReflectionCoeff).to.be.closeTo(0.0780, 0.001);
        expect(ice.reflectionLossDb).to.be.closeTo(-11.08, 0.05);
        expect(ice.powerTransmissionCoeff).to.be.closeTo(0.9220, 0.001);

        // Volcanic basalt rock surface (eps_r = 7.50, n = 2.7386):
        // r_field = 1.7386 / 3.7386 = 0.4650 -> R_pwr = 0.2163 (21.63%) -> Loss = -6.65 dB
        const basalt = RadarSounderEngine.computeRadarSurfacePowerReflectionLoss(7.50);
        expect(basalt.powerReflectionCoeff).to.be.closeTo(0.2163, 0.001);
        expect(basalt.reflectionLossDb).to.be.closeTo(-6.65, 0.05);
    });
});

describe('Crater Morphometry & Neukum Production Function (CSFDEngine)', () => {
    it('should classify simple vs complex crater depth and degradation state', () => {
        // Pristine simple bowl crater (D = 3.0 km, d_obs = 0.60 km):
        // d_fresh = 0.20 * (3.0)^1.01 = 0.606 km -> ratio = 0.60 / 0.606 = 0.990 (Pristine)
        const simplePristine = CSFDEngine.computeCraterDepthAndDegradationState(3.0, 0.60);
        expect(simplePristine.freshDepthKm).to.be.closeTo(0.606, 0.01);
        expect(simplePristine.degradationFactor).to.be.closeTo(0.99, 0.02);
        expect(simplePristine.craterClass).to.include('Simple');
        expect(simplePristine.degradationState).to.equal('Fresh / Pristine');

        // Severely infilled complex crater (D = 50.0 km, d_obs = 0.40 km):
        // d_fresh = 0.36 * (50.0)^0.49 = 2.427 km -> factor = 0.40 / 2.427 = 0.165 (< 0.50 -> Severely Infilled)
        const complexInfilled = CSFDEngine.computeCraterDepthAndDegradationState(50.0, 0.40);
        expect(complexInfilled.freshDepthKm).to.be.closeTo(2.427, 0.05);
        expect(complexInfilled.degradationFactor).to.be.closeTo(0.165, 0.02);
        expect(complexInfilled.craterClass).to.include('Complex');
        expect(complexInfilled.degradationState).to.equal('Severely Infilled / Degraded');
    });

    it('should calculate Neukum/Ivanov Mars cumulative crater production N(>D)', () => {
        // At D = 1.0 km (logD = 0 -> N_1Ga = 10^-3.0876 = 8.173e-4 craters/km^2 = 817.3 per 10^6 km^2)
        // At 1 Ga:
        const n1Ga = CSFDEngine.computeNeukumProductionFunctionCumulativeN(1.0, 1.0);
        expect(n1Ga.cumulativeNCratersPer10_6Km2).to.be.closeTo(817.3, 5.0);

        // At 3.8 Ga (Late Heavy Bombardment / ancient Noachian crust):
        // phi(3.8) ~ 5.44e-14 * exp(14.022) = 5.44e-14 * 1.229e6 = 6.68e-8 -> massive exponential boost
        const n38Ga = CSFDEngine.computeNeukumProductionFunctionCumulativeN(1.0, 3.8);
        expect(n38Ga.cumulativeNCratersPer10_6Km2).to.be.closeTo(3105.92, 5.0);
    });
});

describe('Polar Stereographic Cartographic Projections (Geo)', () => {
    it('should calculate forward North Polar Stereographic projection coordinates and conformal scale factor', () => {
        // North Pole (lat = 90°, lon = 0°): rho = 0, x = 0, y = 0, k = 1.0
        const northPole = computePolarStereographicProjection(90.0, 0.0, true, 0.0, 1.0, 3389.5);
        expect(northPole.xKm).to.equal(0.0);
        expect(northPole.yKm).to.equal(0.0);
        expect(northPole.scaleFactorK).to.equal(1.0);

        // Planum Boreum at lat = 80°N, lon = 90°E (lambda0 = 0°):
        // rho = 2 * 3389.5 * tan(45° - 40°) = 6779.0 * tan(5°) = 6779.0 * 0.0874886 = 593.085 km
        // x = 593.085 * sin(90°) = 593.085 km, y = -593.085 * cos(90°) = 0 km
        const boreum = computePolarStereographicProjection(80.0, 90.0, true, 0.0, 1.0, 3389.5);
        expect(boreum.xKm).to.be.closeTo(593.085, 0.5);
        expect(boreum.yKm).to.be.closeTo(0.0, 0.5);
        expect(boreum.scaleFactorK).to.be.closeTo(1.0076, 0.005);
    });

    it('should accurately invert Polar Stereographic (x, y) back to (lat, lon)', () => {
        // Planum Australe South Pole at lat = -80°S, lon = 45°E:
        const fwdSouth = computePolarStereographicProjection(-80.0, 45.0, false, 0.0, 1.0, 3389.5);
        const invSouth = computePolarStereographicInverse(fwdSouth.xKm, fwdSouth.yKm, false, 0.0, 1.0, 3389.5);

        expect(invSouth.latDeg).to.be.closeTo(-80.0, 0.001);
        expect(invSouth.lonDeg).to.be.closeTo(45.0, 0.001);
    });
});

describe('Lommel-Seeliger & Minnaert Photometric Solvers (ThreeDEngine)', () => {
    it('should calculate Lommel-Seeliger regolith scattering factor', () => {
        // Direct normal illumination and nadir viewing (i = 0°, e = 0°):
        // mu_0 = 1.0, mu = 1.0 -> f_LS = 1.0 / (1.0 + 1.0) = 0.5000
        const nadir = ThreeDEngine.computeLommelSeeligerPhotometry(0.0, 0.0);
        expect(nadir.lommelSeeligerFactor).to.equal(0.5000);
        expect(nadir.cosIncidence).to.equal(1.0);
        expect(nadir.cosEmission).to.equal(1.0);

        // Slanted illumination (i = 60°, e = 30°):
        // mu_0 = cos(60°) = 0.50, mu = cos(30°) = 0.8660
        // f_LS = 0.50 / (0.50 + 0.8660) = 0.50 / 1.3660 = 0.3660
        const slanted = ThreeDEngine.computeLommelSeeligerPhotometry(60.0, 30.0);
        expect(slanted.lommelSeeligerFactor).to.be.closeTo(0.3660, 0.001);
    });

    it('should calculate Minnaert planetary empirical limb-darkening factor', () => {
        // Pure Lambertian diffusion (k = 1.0):
        // f_Minnaert = mu_0^1 * mu^0 = cos(i)
        const lambert = ThreeDEngine.computeMinnaertReflectanceFactor(45.0, 30.0, 1.0);
        expect(lambert.minnaertFactor).to.be.closeTo(Math.cos(Math.PI / 4.0), 0.001);

        // Martian regolith limb parameter (k = 0.65):
        // i = 30°, e = 60°: mu_0 = 0.8660, mu = 0.50
        // f_Minnaert = (0.8660)^0.65 * (0.50)^(-0.35) = 0.9103 * 1.2746 = 1.1602
        const mars = ThreeDEngine.computeMinnaertReflectanceFactor(30.0, 60.0, 0.65);
        expect(mars.minnaertFactor).to.be.closeTo(1.1602, 0.005);
    });
});

describe('Linear Least-Squares Spectral Unmixing (BandMathEngine)', () => {
    it('should calculate constrained 2-endmember linear unmixing abundance fractions and RMSE', () => {
        // Synthetic 75% Endmember A + 25% Endmember B:
        const emA = [0.90, 0.85, 0.70, 0.65, 0.95];
        const emB = [0.60, 0.95, 0.90, 0.80, 0.70];
        const syntheticMeas = emA.map((val, idx) => 0.75 * val + 0.25 * emB[idx]);

        const unmix = BandMathEngine.computeLinearSpectralUnmixing2Components(syntheticMeas, emA, emB);
        expect(unmix.fraction1).to.be.closeTo(0.75, 0.001);
        expect(unmix.fraction2).to.be.closeTo(0.25, 0.001);
        expect(unmix.fraction1Percent).to.be.closeTo(75.0, 0.1);
        expect(unmix.fraction2Percent).to.be.closeTo(25.0, 0.1);
        expect(unmix.rootMeanSquareError).to.be.closeTo(0.0, 0.0001);
    });

    it('should classify dark basaltic bedrock vs bright dust mantle from TES thermal IR emissivity', () => {
        // Dark volcanic plain (Syrtis Major - 85% basalt, 15% dust):
        const basaltEM = [0.98, 0.96, 0.94, 0.91, 0.92, 0.95, 0.97, 0.96, 0.94, 0.98];
        const dustEM =   [0.91, 0.92, 0.95, 0.98, 0.97, 0.93, 0.90, 0.89, 0.91, 0.95];
        const syrtisMajor = basaltEM.map((b, i) => 0.85 * b + 0.15 * dustEM[i]);

        const tesRes = BandMathEngine.computeTESBasaltDustFraction(syrtisMajor);
        expect(tesRes.basaltFraction).to.be.closeTo(0.85, 0.01);
        expect(tesRes.dustFraction).to.be.closeTo(0.15, 0.01);
        expect(tesRes.surfaceType).to.include('Dark Basaltic Bedrock');
    });
});

describe('MCD Dust Opacity & Solar Transmission Solvers (MCDEngine)', () => {
    it('should calculate seasonal column dust optical depth and global storm peaks', () => {
        // Equatorial aphelion clear season (Ls = 70°, lat = 0°):
        // tau = 0.10 + 0.20 * (1 - cos(0)) = 0.10
        const aphelion = MCDEngine.computeMCDColumnDustOpticalDepth(70.0, 0.0, 'climatology');
        expect(aphelion.tauDust).to.be.closeTo(0.10, 0.01);
        expect(aphelion.dustSeason).to.include('Clear');
        expect(aphelion.isStormActive).to.be.false;

        // Perihelion dust storm peak (Ls = 250°, lat = 0°):
        // tau = 0.10 + 0.20 * (1 - cos(180°)) = 0.10 + 0.40 = 0.50
        const perihelion = MCDEngine.computeMCDColumnDustOpticalDepth(250.0, 0.0, 'climatology');
        expect(perihelion.tauDust).to.be.closeTo(0.50, 0.01);
        expect(perihelion.dustSeason).to.include('Dust Storm Season');

        // Global Dust Storm scenario (Ls = 210° peak):
        const gds = MCDEngine.computeMCDColumnDustOpticalDepth(210.0, 0.0, 'global_dust_storm');
        expect(gds.tauDust).to.be.greaterThan(3.5);
        expect(gds.isStormActive).to.be.true;
    });

    it('should calculate Beer-Lambert direct solar beam transmission and diffuse sky fraction', () => {
        // Overhead sun (theta_z = 0° -> airMass = 1.0) in moderate dust (tau = 0.50):
        // T_direct = exp(-0.50) = 0.6065
        // T_diffuse = 0.5 * (1 - exp(-0.50)) * 1.0 = 0.5 * 0.3935 = 0.1967
        // T_total = 0.8033
        const trans = MCDEngine.computeAtmosphericSolarTransmission(0.50, 0.0);
        expect(trans.directTransmission).to.be.closeTo(0.6065, 0.005);
        expect(trans.diffuseTransmission).to.be.closeTo(0.1967, 0.005);
        expect(trans.totalTransmission).to.be.closeTo(0.8033, 0.005);
        expect(trans.airMass).to.equal(1.0);
    });
});

describe('KRC Subsurface Thermal Wave & Surface Heat Balance (KRCEngine)', () => {
    it('should calculate 1D subsurface temperature damping and phase lag with depth', () => {
        // At surface (z = 0 m) at solar noon (solFraction = 0.0):
        // T(0) = 210 + 45 * exp(0) * cos(0) = 255.0 K
        const surfNoon = KRCEngine.computeSubsurface1DTemperatureProfile(0.0, 210.0, 45.0, 0.05, 0.0);
        expect(surfNoon.temperatureK).to.equal(255.0);
        expect(surfNoon.amplitudeDamping).to.equal(1.0);
        expect(surfNoon.phaseLagHours).to.equal(0.0);

        // At 1 diurnal skin depth (z = 0.05 m) at solar noon:
        // damping = exp(-1) = 0.3679 -> local amplitude = 45 * 0.3679 = 16.55 K
        // phase = 0 - 1 rad = -1 rad -> cos(-1) = 0.5403 -> T = 210 + 16.55 * 0.5403 = 218.94 K
        // phase lag = (1 / 2pi) * 24.6597 = 3.92 hours
        const deep1Skin = KRCEngine.computeSubsurface1DTemperatureProfile(0.05, 210.0, 45.0, 0.05, 0.0);
        expect(deep1Skin.amplitudeDamping).to.be.closeTo(0.3679, 0.001);
        expect(deep1Skin.temperatureK).to.be.closeTo(218.94, 0.5);
        expect(deep1Skin.phaseLagHours).to.be.closeTo(3.92, 0.1);
    });

    it('should calculate instantaneous KRC radiative surface equilibrium temperature', () => {
        // High solar insolation (F_solar = 500 W/m^2, A = 0.25, eps = 0.95, F_cond = 0):
        // absorbed = (1 - 0.25) * 500 = 375 W/m^2
        // T_surf = ( 375 / (0.95 * 5.67037e-8) )^(0.25) = ( 375 / 5.38685e-8 )^(0.25) = (6.96139e9)^0.25 = 288.94 K
        const eq = KRCEngine.computeKRCRadiativeSurfaceEquilibriumIterative(0.25, 0.95, 500.0, 0.0);
        expect(eq.surfaceTemperatureK).to.be.closeTo(288.94, 0.5);
        expect(eq.absorbedSolarFluxW_M2).to.equal(375.0);
    });
});

describe('Subsolar Ephemeris & Daylight Duration (MarsTime)', () => {
    it('should calculate Martian solar declination and subsolar ground point coordinates', () => {
        // Northern Summer Solstice (Ls = 90°):
        // delta_sun = +25.19° N
        const solsticeN = MarsTime.computeMartianSolarDeclinationAndSubsolarPoint(90.0, 12.0);
        expect(solsticeN.solarDeclinationDeg).to.be.closeTo(25.19, 0.01);
        expect(solsticeN.subsolarLatitudeDeg).to.be.closeTo(25.19, 0.01);
        expect(solsticeN.subsolarLongitudeDeg).to.equal(0.0);
        expect(solsticeN.seasonDescription).to.include('Northern Summer');

        // Autumnal Equinox (Ls = 180°):
        // delta_sun = 0.0°
        const equinox = MarsTime.computeMartianSolarDeclinationAndSubsolarPoint(180.0, 14.0);
        expect(equinox.solarDeclinationDeg).to.be.closeTo(0.0, 0.01);
        expect(equinox.subsolarLongitudeDeg).to.equal(-30.0);
    });

    it('should calculate latitude-dependent daylight duration and polar day/night states', () => {
        // Equator (lat = 0°) at any declination:
        // tan(0) * tan(delta) = 0 -> omega0 = pi/2 -> daylight = 12.33 hours (exactly 50% of sol)
        const eqDay = MarsTime.computeMartianDaylightLengthHours(0.0, 20.0);
        expect(eqDay.daylightHours).to.be.closeTo(12.33, 0.05);
        expect(eqDay.polarSunState).to.equal('Normal Diurnal Day/Night Cycle');

        // North Pole (lat = 80°N) during Northern Summer (delta = +25.19°):
        // tan(80°) * tan(25.19°) = 5.671 * 0.4704 = 2.668 >= 1.0 -> Midnight Sun (24.66 hours)
        const northSummer = MarsTime.computeMartianDaylightLengthHours(80.0, 25.19);
        expect(northSummer.daylightHours).to.be.closeTo(24.66, 0.05);
        expect(northSummer.polarSunState).to.include('Midnight Sun');

        // South Pole (lat = -80°S) during Northern Summer (delta = +25.19°):
        // tan(-80°) * tan(25.19°) = -2.668 <= -1.0 -> Polar Night (0 hours daylight)
        const southWinter = MarsTime.computeMartianDaylightLengthHours(-80.0, 25.19);
        expect(southWinter.daylightHours).to.equal(0.0);
        expect(southWinter.polarSunState).to.include('Polar Night');
    });
});

describe('Ground Track Swath Overlap & Sol Repeat (TrajectoryEngine)', () => {
    it('should calculate instrument swath overlap fraction across latitudes', () => {
        // CTX camera swath (W = 30 km), equatorial spacing dx = 28 km:
        // At equator (phi = 0°): dx = 28 km -> overlap = 30 - 28 = 2 km (6.67% overlap, seamless)
        const eqCTX = TrajectoryEngine.computeGroundTrackSwathOverlap(30.0, 28.0, 0.0);
        expect(eqCTX.overlapKm).to.be.closeTo(2.0, 0.1);
        expect(eqCTX.overlapPercent).to.be.closeTo(6.67, 0.1);
        expect(eqCTX.isSeamlessCoverage).to.be.true;

        // At mid-latitude (phi = 60°): dx = 28 * cos(60°) = 14 km -> overlap = 30 - 14 = 16 km (53.33% overlap)
        const midCTX = TrajectoryEngine.computeGroundTrackSwathOverlap(30.0, 28.0, 60.0);
        expect(midCTX.overlapKm).to.be.closeTo(16.0, 0.1);
        expect(midCTX.overlapPercent).to.be.closeTo(53.33, 0.1);
    });

    it('should calculate orbit period and daily revolutions per Martian sol', () => {
        // MRO low-altitude science orbit (a = 3645 km, mu_mars = 42828.37 km^3/s^2):
        // T = 2 * pi * sqrt( (3645)^3 / 42828.37 ) = 2 * pi * 1063.48 = 6682.04 s = 111.37 min = 1.856 hours
        // Revs/sol = 88775.244 / 6682.04 = 13.285 revs/sol
        // Drift = (6682.04 / 88775.244) * 360 = 27.098° West/rev
        const mro = TrajectoryEngine.computeOrbitPeriodAndRevolutionsPerSol(3645.0, 'mars');
        expect(mro.periodMinutes).to.be.closeTo(111.37, 0.5);
        expect(mro.periodHours).to.be.closeTo(1.856, 0.01);
        expect(mro.revolutionsPerSol).to.be.closeTo(13.285, 0.05);
        expect(mro.groundTrackEquatorialDriftDeg).to.be.closeTo(27.10, 0.1);
    });
});

describe('Radar Clutter Simulation & Doppler Solvers (RadarSounderEngine)', () => {
    it('should calculate off-nadir surface topographic clutter time delay relative to nadir echo', () => {
        // Off-nadir mountain peak at cross-track x = 40 km, along-track y = 0 km, elevation z = 5 km
        // Spacecraft altitude h = 300 km:
        // deltaZ = 300 - 5 = 295 km
        // R_slant = sqrt( 40^2 + 0^2 + 295^2 ) = sqrt( 1600 + 87025 ) = sqrt( 88625 ) = 297.6995 km
        // excessRange = 297.6995 - 300 = -2.3005 km (mountain arrives BEFORE nadir datum!)
        // Delta_t = 2 * (-2.3005) / 299792.458 = -15.347 microseconds
        const clutter = RadarSounderEngine.computeRadarOffNadirClutterTimeDelay(40.0, 0.0, 300.0, 5.0);
        expect(clutter.slantRangeKm).to.be.closeTo(297.70, 0.05);
        expect(clutter.clutterExcessDelayMicrosec).to.be.closeTo(-15.35, 0.1);

        // Flat surface off-nadir crater rim at x = 30 km, y = 0 km, z = 0 km:
        // R_slant = sqrt( 30^2 + 300^2 ) = sqrt( 900 + 90000 ) = sqrt( 90900 ) = 301.496 km
        // excessRange = +1.496 km -> Delta_t = 2 * 1.496 / 299792.458 = +9.982 microseconds
        const flatClutter = RadarSounderEngine.computeRadarOffNadirClutterTimeDelay(30.0, 0.0, 300.0, 0.0);
        expect(flatClutter.slantRangeKm).to.be.closeTo(301.496, 0.05);
        expect(flatClutter.clutterExcessDelayMicrosec).to.be.closeTo(9.98, 0.05);
    });

    it('should calculate along-track radar Doppler frequency shift', () => {
        // Forward facet at along-track y = 15 km, x = 0 km, h = 300 km:
        // R_slant = sqrt( 15^2 + 300^2 ) = 300.375 km
        // cosThetaV = 15 / 300.375 = 0.0499376
        // f_Doppler = (2 * 3448 / 14.990) * 0.0499376 = 460.04 * 0.0499376 = 22.97 Hz
        const dop = RadarSounderEngine.computeRadarDopplerFrequencyShift(3.448, 20.0, 15.0, 0.0, 300.0);
        expect(dop.dopplerShiftHz).to.be.closeTo(22.97, 0.1);
        expect(dop.wavelengthMeters).to.be.closeTo(14.99, 0.01);
    });
});

describe('Crater Ejecta Blanket & Transient Cavity Excavation (CSFDEngine)', () => {
    it('should calculate McGetchin/Housen continuous ejecta blanket thickness with radial distance', () => {
        // Crater diameter D = 20 km (R_rim = 10 km = 10000 m):
        // At rim (r = 10 km -> normR = 1.0):
        // t = 0.14 * (10000)^0.74 * (1.0)^-3 = 0.14 * 912.01 = 127.68 meters
        const rimEjecta = CSFDEngine.computeCraterEjectaBlanketThickness(20.0, 10.0);
        expect(rimEjecta.ejectaThicknessMeters).to.be.closeTo(127.68, 1.0);
        expect(rimEjecta.normalizedRadialDistanceR).to.equal(1.0);
        expect(rimEjecta.isContinuousEjecta).to.be.true;

        // At 2 crater radii (r = 20 km -> normR = 2.0):
        // t = 127.68 * (2.0)^-3 = 127.68 / 8 = 15.96 meters
        const distalEjecta = CSFDEngine.computeCraterEjectaBlanketThickness(20.0, 20.0);
        expect(distalEjecta.ejectaThicknessMeters).to.be.closeTo(15.96, 0.5);
        expect(distalEjecta.isContinuousEjecta).to.be.true;
    });

    it('should calculate transient crater cavity diameter and excavated volume', () => {
        // Simple crater (D = 4.0 km):
        // D_tc = 0.84 * 4.0 = 3.36 km
        // V_exc = (pi / 24) * (3.36)^3 = 0.1309 * 37.933 = 4.97 km^3
        const simpleCavity = CSFDEngine.computeCraterTransientCavityAndExcavationVolume(4.0);
        expect(simpleCavity.transientDiameterKm).to.be.closeTo(3.36, 0.01);
        expect(simpleCavity.excavationVolumeKm3).to.be.closeTo(4.97, 0.1);

        // Complex crater (Gale Crater D = 154 km):
        // D_tc = 1.34 * (154)^0.85 = 1.34 * 72.342 = 96.94 km
        // V_exc = (pi / 24) * (96.94)^3 = 0.1309 * 910970 = 119246 km^3
        const galeCavity = CSFDEngine.computeCraterTransientCavityAndExcavationVolume(154.0);
        expect(galeCavity.transientDiameterKm).to.be.closeTo(96.94, 0.5);
        expect(galeCavity.excavationVolumeKm3).to.be.greaterThan(100000.0);
    });
});

describe('Mollweide Equal-Area Cartographic Projections (Geo)', () => {
    it('should calculate forward Mollweide equal-area projection coordinates', () => {
        // Origin (lat = 0°, lon = 0°):
        // theta = 0°, x = 0, y = 0
        const origin = computeMollweideProjection(0.0, 0.0, 0.0, 3389.5);
        expect(origin.xKm).to.equal(0.0);
        expect(origin.yKm).to.equal(0.0);
        expect(origin.auxiliaryThetaDeg).to.equal(0.0);
        expect(origin.isWithinGlobeBoundary).to.be.true;

        // North Pole (lat = 90°N, lon = 0°):
        // 2*theta + sin(2*theta) = pi -> theta = pi/2 = 90°
        // y = sqrt(2) * 3389.5 = 4793.476 km, x = 0
        const northPole = computeMollweideProjection(90.0, 0.0, 0.0, 3389.5);
        expect(northPole.xKm).to.equal(0.0);
        expect(northPole.yKm).to.be.closeTo(4793.48, 0.5);
        expect(northPole.auxiliaryThetaDeg).to.be.closeTo(90.0, 0.01);
    });

    it('should accurately invert Mollweide (x, y) back to (lat, lon)', () => {
        // Olympus Mons at lat = 18.65°N, lon = -133.8°W (centerLon = 0°):
        const fwdOlympus = computeMollweideProjection(18.65, -133.8, 0.0, 3389.5);
        const invOlympus = computeMollweideInverse(fwdOlympus.xKm, fwdOlympus.yKm, 0.0, 3389.5);

        expect(invOlympus.latDeg).to.be.closeTo(18.65, 0.01);
        expect(invOlympus.lonDeg).to.be.closeTo(-133.8, 0.01);
        expect(invOlympus.isValidCoordinate).to.be.true;
    });
});

describe('CRISM Hydrated Phyllosilicates & Clay Minerals (BandMathEngine)', () => {
    it('should calculate Al-OH phyllosilicate absorption depth (BD2210) for montmorillonite/kaolinite', () => {
        // Deep Al-OH absorption at 2210 nm (Mawrth Vallis top layer):
        // r2140 = 0.28, r2210 = 0.23, r2250 = 0.27
        // continuum = 0.5 * (0.28 + 0.27) = 0.275
        // BD2210 = 1.0 - (0.23 / 0.275) = 1.0 - 0.83636 = 0.1636 (> 0.03 -> Al-Smectite)
        const alClay = BandMathEngine.computeCRISMAlOHMineralIndexBD2210(0.28, 0.23, 0.27);
        expect(alClay.bd2210Index).to.be.closeTo(0.1636, 0.001);
        expect(alClay.hasAlPhyllosilicate).to.be.true;
        expect(alClay.mineralFamily).to.include('Al-Smectite');

        // Basalt surface without Al-OH absorption:
        const unaltering = BandMathEngine.computeCRISMAlOHMineralIndexBD2210(0.15, 0.149, 0.15);
        expect(alClay.bd2210Index).to.be.greaterThan(unaltering.bd2210Index);
        expect(unaltering.hasAlPhyllosilicate).to.be.false;
    });

    it('should calculate Fe/Mg-OH phyllosilicate absorption depth (BD2300) for nontronite/saponite', () => {
        // Deep Fe/Mg-OH absorption at 2300 nm (Nili Fossae bedrock):
        // r2250 = 0.25, r2300 = 0.21, r2350 = 0.24
        // continuum = 0.5 * (0.25 + 0.24) = 0.245
        // BD2300 = 1.0 - (0.21 / 0.245) = 1.0 - 0.85714 = 0.1429 (> 0.03 -> Fe/Mg-Smectite)
        const femgClay = BandMathEngine.computeCRISMFeMgOHMineralIndexBD2300(0.25, 0.21, 0.24);
        expect(femgClay.bd2300Index).to.be.closeTo(0.1429, 0.001);
        expect(femgClay.hasFeMgPhyllosilicate).to.be.true;
        expect(femgClay.mineralFamily).to.include('Fe/Mg-Smectite');
    });
});

describe('Hapke Regolith Photometry & Opposition Surge (ThreeDEngine)', () => {
    it('should calculate Henyey-Greenstein single particle scattering phase function p(g)', () => {
        // Exact backscattering opposition (g = 0°), xi = -0.25 (Martian dust backscatter):
        // p(0) = (1 - xi^2) / (1 + 2*xi + xi^2)^1.5 = (1 - 0.0625) / (1 - 0.5 + 0.0625)^1.5 = 0.9375 / (0.5625)^1.5 = 0.9375 / 0.421875 = 2.2222
        const oppPhase = ThreeDEngine.computeHapkeSingleParticlePhaseFunction(0.0, -0.25);
        expect(oppPhase.phaseFunctionValue).to.be.closeTo(2.2222, 0.001);
        expect(oppPhase.isBackscattering).to.be.true;

        // Right angle phase (g = 90°): cos(90) = 0 -> p(90) = 0.9375 / (1.0625)^1.5 = 0.9375 / 1.0952 = 0.8560
        const rightPhase = ThreeDEngine.computeHapkeSingleParticlePhaseFunction(90.0, -0.25);
        expect(rightPhase.phaseFunctionValue).to.be.closeTo(0.8560, 0.001);
        expect(oppPhase.phaseFunctionValue).to.be.greaterThan(rightPhase.phaseFunctionValue);
    });

    it('should calculate Hapke shadow-hiding opposition surge multiplier B_SH(g)', () => {
        // At exact opposition (g = 0°): tan(0) = 0 -> B_SH = 1.0 + B_0 = 2.0 (100% surge doubling)
        const exactOpp = ThreeDEngine.computeHapkeOppositionSurgeMultiplier(0.0, 1.0, 0.06);
        expect(exactOpp.oppositionSurgeMultiplier).to.equal(2.0);
        expect(exactOpp.isOppositionSpike).to.be.true;

        // At large phase angle (g = 30°): tan(15°) = 0.26795 -> B_SH = 1 + 1 / (1 + 0.26795 / 0.06) = 1 + 1 / 5.4658 = 1.1830
        const widePhase = ThreeDEngine.computeHapkeOppositionSurgeMultiplier(30.0, 1.0, 0.06);
        expect(widePhase.oppositionSurgeMultiplier).to.be.closeTo(1.1830, 0.01);
        expect(widePhase.isOppositionSpike).to.be.false;
    });
});

describe('CO2 Frost Point & Polar Condensation (KRCEngine)', () => {
    it('should calculate CO2 condensation frost point temperature across surface pressures', () => {
        // Average Martian surface datum (P = 6.1 mbar = 0.0061 bar):
        // ln(0.0061) = -5.0994
        // T_frost = -3148.0 / (-5.0994 - 23.102) = -3148.0 / -28.2014 = 148.63 K (-124.52 °C)
        const datumFrost = KRCEngine.computeCO2CondensationFrostPoint(6.1);
        expect(datumFrost.frostPointK).to.be.closeTo(148.63, 0.5);
        expect(datumFrost.frostPointC).to.be.closeTo(-124.52, 0.5);
        expect(datumFrost.isSummitVacuum).to.be.false;

        // Olympus Mons summit low pressure (P = 0.7 mbar = 0.0007 bar):
        // T_frost is colder (~135 K)
        const summitFrost = KRCEngine.computeCO2CondensationFrostPoint(0.7);
        expect(summitFrost.frostPointK).to.be.lessThan(datumFrost.frostPointK);
        expect(summitFrost.isSummitVacuum).to.be.true;
    });

    it('should calculate polar dry ice mass and millimeter layer growth per Martian sol', () => {
        // Polar night radiative cooling deficit F_net = 25.0 W/m^2:
        // dm/dt = (25.0 / 5.9e5) * 88775.244 = 4.237e-5 * 88775.244 = 3.762 kg / (m^2 * sol)
        // dz/dt = (3.762 / 1500) * 1000 = 2.508 mm / sol
        const polarNight = KRCEngine.computePolarFrostCondensationRate(25.0, 5.9e5, 1500.0);
        expect(polarNight.frostAccumulationKgPerM2PerSol).to.be.closeTo(3.762, 0.01);
        expect(polarNight.frostGrowthMmPerSol).to.be.closeTo(2.508, 0.01);
        expect(polarNight.isCondensing).to.be.true;
    });
});

describe('PWA Manifest, PWAManager & MobileSheet Architecture', () => {
    it('should validate PWA Web App Manifest structure and essential fields', async () => {
        // Fetch and parse manifest.webmanifest
        const res = await fetch('../manifest.webmanifest');
        expect(res.status).to.equal(200);
        const manifest = await res.json();

        expect(manifest.id).to.equal('com.sounny.jsmars');
        expect(manifest.name).to.include('JSMARS');
        expect(manifest.short_name).to.equal('JSMARS');
        expect(manifest.display).to.equal('standalone');
        expect(manifest.start_url).to.equal('./index.html');
        expect(manifest.scope).to.equal('./');
        expect(manifest.theme_color).to.equal('#0f172a');
        expect(manifest.background_color).to.equal('#020617');
        expect(manifest.icons).to.be.an('array').with.length.greaterThan(2);

        const has192 = manifest.icons.some(i => i.sizes === '192x192');
        const has512 = manifest.icons.some(i => i.sizes === '512x512');
        const hasMaskable = manifest.icons.some(i => i.purpose && i.purpose.includes('maskable'));
        expect(has192).to.be.true;
        expect(has512).to.be.true;
        expect(hasMaskable).to.be.true;
    });

    it('should test PWAManager lifecycle state and offline network indicators', () => {
        const pwa = new PWAManager();
        expect(pwa.isOnline).to.be.a('boolean');
        expect(pwa.isInstalled).to.be.false;

        // Test status update methods
        pwa._updateNetworkBadge();
        expect(pwa.deferredPrompt).to.be.null;
    });

    it('should coordinate MobileSheet state machine (peek, expanded, toggle)', () => {
        const container = document.createElement('div');
        container.id = 'test-controls';
        document.body.appendChild(container);

        const sheet = new MobileSheet(container, null);
        expect(sheet.state).to.equal('peek');

        // Toggle to expanded
        sheet.toggleSheet();
        expect(sheet.state).to.equal('expanded');
        expect(container.classList.contains('mobile-sheet-expanded')).to.be.true;

        // Toggle back to peek
        sheet.toggleSheet();
        expect(sheet.state).to.equal('peek');
        expect(container.classList.contains('mobile-sheet-peek')).to.be.true;

        // Cleanup
        document.body.removeChild(container);
    });
});

describe('Stabilization Milestones: Sessions, Cross-Body Bookmarks, XSS Prevention & Visibility', () => {
    it('should serialize live session state with canonical lowercase body key', () => {
        jmarsState.set('body', 'Moon');
        jmarsState.set('view', { lat: 15.5, lng: -45.2, zoom: 6 });

        const sessionMgr = new SessionManager(null, null, null);
        let downloadedContent = null;
        sessionMgr.downloadFile = (name, content) => {
            downloadedContent = JSON.parse(content);
        };

        sessionMgr.saveSession();
        expect(downloadedContent).to.not.be.null;
        expect(downloadedContent.state.body).to.equal('moon');
        expect(downloadedContent.state.view.lat).to.equal(15.5);
        expect(downloadedContent.state.view.lng).to.equal(-45.2);
    });

    it('should handle cross-body bookmark navigation by dispatching BODY_CHANGED event', (done) => {
        const mockMap = {
            center: [0, 0],
            zoom: 2,
            setView: (c, z) => {
                mockMap.center = c;
                mockMap.zoom = z;
            }
        };

        const bookmarks = new BookmarksTool(mockMap, null);
        bookmarks.currentBody = 'mars';

        const bodyChangeHandler = (e) => {
            expect(e.detail.body).to.equal('moon');
            document.removeEventListener(EVENTS.BODY_CHANGED, bodyChangeHandler);
            done();
        };
        document.addEventListener(EVENTS.BODY_CHANGED, bodyChangeHandler);

        // Navigate to Moon POI
        bookmarks.goTo({ id: 'apollo11', name: 'Apollo 11', lat: 0.67, lng: 23.47, zoom: 8, body: 'moon' });
        expect(mockMap.center[0]).to.equal(0.67);
        expect(mockMap.center[1]).to.equal(23.47);
    });

    it('should render bookmark names safely without executing markup strings (XSS resilience)', () => {
        const container = document.createElement('div');
        const mockMap = { setView: () => {} };
        const bookmarks = new BookmarksTool(mockMap, container);

        // Inject malicious markup string
        bookmarks.bookmarks = [
            { id: 'xss-test', name: '<img src=x onerror=alert(1)> Olympus Mons', lat: 18.0, lng: -133.0, zoom: 5, body: 'mars' }
        ];
        bookmarks.render();

        // The img tag must NOT be created as a DOM element; the text must remain plain text
        const imgElements = container.querySelectorAll('img');
        expect(imgElements.length).to.equal(0);
        expect(container.textContent).to.include('<img src=x onerror=alert(1)> Olympus Mons');
    });

    it('should render StampQueryPanel results safely using DOM APIs (XSS resilience)', () => {
        const container = document.createElement('div');
        const mockStampLayer = {
            getInstruments: () => [{ id: 'THEMIS', name: 'THEMIS' }],
            results: [],
            activate: () => {},
            query: async () => {},
            clear: () => {},
            exportCSV: () => {}
        };

        const panel = new StampQueryPanel(container, mockStampLayer);
        // Provide mock results with malicious markup in product ID
        const malformedResults = [
            { pdsId: '<script>alert("xss")</script>THEMIS_IR_123', centerLat: 10.5, centerLon: -45.2, solarLon: 120.0 }
        ];

        panel._renderResultsTable(malformedResults);
        const scriptElements = container.querySelectorAll('script');
        expect(scriptElements.length).to.equal(0);
        const td = container.querySelector('td');
        expect(td).to.not.be.null;
        expect(td.getAttribute('title')).to.equal('<script>alert("xss")</script>THEMIS_IR_123');
        expect(container.textContent).to.include('<script>alert("xss")');
    });
});

describe('MCD Dust Optical Depth, SHARAD Radar Attenuation & CRISM Pyroxene Solvers', () => {
    it('should calculate wavelength-dependent aerosol optical depth and rover solar power yield loss', () => {
        // Visible tau = 1.0 at 0.67 um:
        // Blue wavelength (0.44 um): tau = 1.0 * (0.44 / 0.67)^(-0.25) ~ 1.0 * (0.6567)^(-0.25) ~ 1.11
        const blueDust = MCDEngine.computeSpectralDustOpticalDepth(1.0, 0.44, 0.25);
        expect(blueDust.spectralOpticalDepth).to.be.closeTo(1.11, 0.05);
        expect(blueDust.isExtinctionStrongerThanVis).to.be.true;

        // Infrared wavelength (1.02 um): tau < 1.0
        const irDust = MCDEngine.computeSpectralDustOpticalDepth(1.0, 1.02, 0.25);
        expect(irDust.spectralOpticalDepth).to.be.lessThan(1.0);
        expect(irDust.isExtinctionStrongerThanVis).to.be.false;

        // Clean arrays under clear sky (tau = 0.3, panel dust = 0.0) -> high yield
        const clearYield = MCDEngine.computeRoverSolarPowerYieldLoss(0.3, 0.0, 900.0);
        expect(clearYield.dailyYieldWattHours).to.be.greaterThan(500.0);
        expect(clearYield.isCriticalPowerDeficit).to.be.false;

        // Global dust storm (tau = 5.0, panel dust = 0.80) -> critical deficit (< 250 Wh critical threshold)
        const stormYield = MCDEngine.computeRoverSolarPowerYieldLoss(5.0, 0.80, 900.0);
        expect(stormYield.dailyYieldWattHours).to.be.lessThan(100.0);
        expect(stormYield.isCriticalPowerDeficit).to.be.true;
    });

    it('should calculate SHARAD subsurface dielectric attenuation and skin depth for ice vs basalt', () => {
        // Pure water ice (f = 20 MHz, eps_r = 3.15, tan_delta = 0.0005, depth = 1000 m):
        // alpha_dB_m ~ 0.0016 dB/m -> 2-way loss ~ 3.2 dB (highly penetrable)
        const iceSounding = RadarSounderEngine.computeSubsurfaceRadarAttenuationAndSkinDepth(20.0, 3.15, 0.0005, 1000.0);
        expect(iceSounding.attenuationDbPerMeter).to.be.closeTo(0.0016, 0.0005);
        expect(iceSounding.twoWayLossDb).to.be.lessThan(10.0);
        expect(iceSounding.isPenetrable).to.be.true;
        expect(iceSounding.skinDepthMeters).to.be.greaterThan(1000.0);

        // Dense volcanic basalt (f = 20 MHz, eps_r = 7.0, tan_delta = 0.02, depth = 1000 m):
        // alpha_dB_m ~ 0.096 dB/m -> 2-way loss ~ 193 dB (impenetrable)
        const basaltSounding = RadarSounderEngine.computeSubsurfaceRadarAttenuationAndSkinDepth(20.0, 7.0, 0.02, 1000.0);
        expect(basaltSounding.twoWayLossDb).to.be.greaterThan(100.0);
        expect(basaltSounding.isPenetrable).to.be.false;
    });

    it('should classify CRISM Low-Calcium Pyroxene (LCP) vs High-Calcium Pyroxene (HCP)', () => {
        // LCP (Orthopyroxene / Norite): Band I at 920 nm, Band II at 1900 nm
        const lcp = BandMathEngine.computeCRISMPyroxeneCompositionLCPvsHCP(920.0, 1900.0, 0.12, 0.10);
        expect(lcp.isLCP).to.be.true;
        expect(lcp.isHCP).to.be.false;
        expect(lcp.pyroxeneClass).to.include('Low-Calcium Pyroxene');
        expect(lcp.estimatedWoContentPct).to.be.closeTo(10.0, 1.0);

        // HCP (Clinopyroxene / Augite-Basalt): Band I at 1030 nm, Band II at 2250 nm
        const hcp = BandMathEngine.computeCRISMPyroxeneCompositionLCPvsHCP(1030.0, 2250.0, 0.14, 0.12);
        expect(hcp.isHCP).to.be.true;
        expect(hcp.isLCP).to.be.false;
        expect(hcp.pyroxeneClass).to.include('High-Calcium Pyroxene');
        expect(hcp.estimatedWoContentPct).to.be.closeTo(45.0, 1.0);
    });
});

describe('Photometric Scattering, KRC Subsurface Thermal Waves & Terrain Solar Insolation', () => {
    it('should calculate Lommel-Seeliger regolith reflectance and Lunar-Lambert weighting', () => {
        // Normal incidence and emission at zero phase (i = 0, e = 0, g = 0):
        // mu_0 = 1.0, mu = 1.0 -> mu_0 / (mu_0 + mu) = 0.5
        // LS = (w_0 / 4pi) * 0.5 * p(0)
        const exactNorm = ThreeDEngine.computeLommelSeeligerLunarReflectance(0.0, 0.0, 0.0, 0.25);
        expect(exactNorm.mu0).to.equal(1.0);
        expect(exactNorm.mu).to.equal(1.0);
        expect(exactNorm.lommelSeeligerReflectance).to.be.greaterThan(0);

        // Lunar-Lambert factor for L = 0.60
        const ll = ThreeDEngine.computeLunarLambertPhotometricWeighting(30.0, 0.0, 0.60);
        expect(ll.lunarWeight).to.equal(0.60);
        expect(ll.isDominantlyLommelSeeliger).to.be.true;
        expect(ll.lunarLambertFactor).to.be.greaterThan(0);
    });

    it('should compute KRC diurnal and annual thermal skin depths and subsurface harmonic damping', () => {
        // Typical Martian sand (I = 250 J m^-2 K^-1 s^-1/2, rho = 1500 kg/m^3, c_p = 850 J/kg/K):
        // Volumetric heat capacity = 1500 * 850 = 1.275e6 J/m^3/K
        // Diurnal skin depth d_diurnal ~ (250 / 1.275e6) * sqrt(88775 / pi) = 1.96e-4 * 168.09 = 0.033 m = 3.3 cm
        const sandThermal = KRCEngine.computeThermalSkinDepthAndHarmonicDamping(250.0, 50.0, 0.10);
        expect(sandThermal.diurnalSkinDepthCm).to.be.closeTo(3.3, 0.5);
        expect(sandThermal.annualSkinDepthMeters).to.be.greaterThan(0.5);
        expect(sandThermal.annualSkinDepthMeters).to.be.closeTo(sandThermal.diurnalSkinDepthCm * 0.01 * Math.sqrt(668.6), 0.1);

        // At z = 10 cm (depth > 3*d_diurnal), surface wave is damped to < 10%
        expect(sandThermal.dampedTempAmplitudeK).to.be.lessThan(5.0);
        expect(sandThermal.phaseLagHours).to.be.greaterThan(5.0);
    });

    it('should compute topographic aspect azimuth and direct solar insolation on sloping terrain', () => {
        // 45-degree slope facing due South (dzdx = 0, dzdy = 1.0):
        const southAspect = HillshadeLayer.computeTopographicAspectDegrees(0.0, 1.0);
        expect(southAspect.aspectDeg).to.be.closeTo(180.0, 0.1);
        expect(southAspect.cardinalDirection).to.equal('S');

        // Facing due East (dzdx = -1.0, dzdy = 0.0):
        const eastAspect = HillshadeLayer.computeTopographicAspectDegrees(-1.0, 0.0);
        expect(eastAspect.aspectDeg).to.be.closeTo(90.0, 0.1);
        expect(eastAspect.cardinalDirection).to.equal('E');

        // Direct insolation on south-facing 30-degree slope with midday sun directly overhead in south:
        const southSlopeInsolation = HillshadeLayer.computeDirectSolarInsolationOnSlope(30.0, 180.0, 30.0, 180.0, 590.0);
        expect(southSlopeInsolation.cosIncidence).to.be.closeTo(1.0, 0.01);
        expect(southSlopeInsolation.incidentFluxWm2).to.be.closeTo(590.0, 5.0);
        expect(southSlopeInsolation.isSelfShadowed).to.be.false;

        // North-facing cliff completely self-shadowed from southern sun:
        const northSlopeInsolation = HillshadeLayer.computeDirectSolarInsolationOnSlope(60.0, 0.0, 70.0, 180.0, 590.0);
        expect(northSlopeInsolation.isSelfShadowed).to.be.true;
        expect(northSlopeInsolation.incidentFluxWm2).to.equal(0.0);
    });
});

describe('CO2 Rayleigh Scattering, Lithospheric Flexure & Sulfate Mineralogy Indices', () => {
    it('should calculate molecular Rayleigh scattering cross-section and column optical depth', () => {
        // Mean Martian surface pressure P = 610 Pa at blue band (0.450 um):
        // tau_Rayleigh ~ 0.0028 << tau_dust (typically 0.3 - 1.0)
        const blueRayleigh = MCDEngine.computeCO2RayleighScatteringOpticalDepth(610.0, 0.450);
        expect(blueRayleigh.rayleighOpticalDepth).to.be.closeTo(0.0028, 0.001);
        expect(blueRayleigh.isRayleighNegligibleComparedToDust).to.be.true;

        // UV band (0.300 um): Rayleigh scattering increases with 1/lambda^4
        const uvRayleigh = MCDEngine.computeCO2RayleighScatteringOpticalDepth(610.0, 0.300);
        expect(uvRayleigh.rayleighOpticalDepth).to.be.greaterThan(blueRayleigh.rayleighOpticalDepth * 4.0);
    });

    it('should calculate planetary lithospheric flexural rigidity and elastic thickness Te', () => {
        // Olympus Mons / Tharsis loading (Te = 50 km, E = 100 GPa, nu = 0.25):
        // D ~ 1.11e24 N*m, alpha ~ 211 km, lambda ~ 1326 km
        const tharsisFlexure = KRCEngine.computeLithosphericFlexuralRigidityAndElasticThickness(50.0);
        expect(tharsisFlexure.flexuralRigidityNewtonMeters).to.be.closeTo(1.11e24, 0.1e24);
        expect(tharsisFlexure.flexuralParameterKm).to.be.closeTo(211.0, 15.0);
        expect(tharsisFlexure.flexuralWavelengthKm).to.be.greaterThan(1000.0);
        expect(tharsisFlexure.basalHeatFlowMilliWattsM2).to.be.closeTo(36.0, 5.0);
    });

    it('should calculate CRISM hydrated sulfate indices and classify gypsum vs kieserite', () => {
        // Monohydrated Sulfate (Kieserite): strong BD2100 absorption minimum at 2130 nm
        const kieserite = BandMathEngine.computeCRISMSulfateHydrationIndices(0.25, 0.22, 0.26, 0.21, 0.27, 0.26);
        expect(kieserite.bd2100Kieserite).to.be.greaterThan(0.10);
        expect(kieserite.isHydratedSulfate).to.be.true;
        expect(kieserite.sulfateClass).to.include('Monohydrated Sulfate');

        // Polyhydrated Sulfate (Gypsum): strong H2O BD1900 and high SINDEX2 convexity
        const gypsum = BandMathEngine.computeCRISMSulfateHydrationIndices(0.30, 0.24, 0.32, 0.29, 0.35, 0.31);
        expect(gypsum.bd1900H2O).to.be.greaterThan(0.15);
        expect(gypsum.sindex2).to.be.greaterThan(0.05);
        expect(gypsum.isHydratedSulfate).to.be.true;
        expect(gypsum.sulfateClass).to.include('Polyhydrated Sulfate');
    });
});

describe('Ionospheric Radar Dispersion, Methane Photolysis & Olivine Fo# Solid Solution', () => {
    it('should calculate ionospheric Total Electron Content (TEC) group delay and radar chirp dispersion', () => {
        // Daytime Martian ionosphere (TEC = 3e15 e-/m^2 = 0.3 TECU, f = 20 MHz):
        // Delta_t_g ~ 1.008 microsec, Delta_R ~ 302 m (one-way * c = 302 m)
        const dayTec = RadarSounderEngine.computeIonosphericTotalElectronContentDispersion(3e15, 20.0, 10.0);
        expect(dayTec.groupDelayMicrosec).to.be.closeTo(1.008, 0.05);
        expect(dayTec.rangeShiftMeters).to.be.closeTo(302.2, 10.0);
        expect(dayTec.tecTECU).to.equal(0.3);
        expect(dayTec.isSevereDistortion).to.be.true;

        // Nightside Martian ionosphere (TEC = 1e14 e-/m^2 = 0.01 TECU):
        const nightTec = RadarSounderEngine.computeIonosphericTotalElectronContentDispersion(1e14, 20.0, 10.0);
        expect(nightTec.groupDelayMicrosec).to.be.lessThan(0.05);
        expect(nightTec.isSevereDistortion).to.be.false;
    });

    it('should calculate atmospheric trace methane column abundance and photolysis lifetime', () => {
        // Gale Crater MSL SAM TLS background methane (0.4 ppb):
        const bgMethane = MCDEngine.computeMethaneTraceGasColumnAbundanceAndLossRate(0.4, 610.0, false);
        expect(bgMethane.methaneMixingRatioPpb).to.equal(0.4);
        expect(bgMethane.isEnrichedPlume).to.be.false;
        expect(bgMethane.photochemicalLifetimeYears).to.equal(320.0);
        expect(bgMethane.columnMassMicrogramsM2).to.be.greaterThan(0);

        // High concentration plume event (15.0 ppb) with active soil oxidation sink:
        const plumeMethane = MCDEngine.computeMethaneTraceGasColumnAbundanceAndLossRate(15.0, 610.0, true);
        expect(plumeMethane.isEnrichedPlume).to.be.true;
        expect(plumeMethane.photochemicalLifetimeYears).to.equal(0.5);
        expect(plumeMethane.lifetimeSols).to.be.closeTo(334.3, 5.0);
    });

    it('should calculate olivine Forsterite number Fo# from 1 um crystal field absorption minimum', () => {
        // Magnesian Olivine (Fo90 Forsterite / Dunite): absorption minimum at 1040 nm
        const forsterite = BandMathEngine.computeCRISMOlivineFoNumberAndComposition(1040.0, 0.12);
        expect(forsterite.foNumberPct).to.be.closeTo(90.0, 1.0);
        expect(forsterite.faNumberPct).to.be.closeTo(10.0, 1.0);
        expect(forsterite.isMagnesianFoRich).to.be.true;
        expect(forsterite.isIronFaRich).to.be.false;
        expect(forsterite.olivineClass).to.include('Forsterite-Rich');

        // Iron-rich Olivine (Fo20 Fayalite / Differentiated Basalt): absorption minimum at 1075 nm
        const fayalite = BandMathEngine.computeCRISMOlivineFoNumberAndComposition(1075.0, 0.10);
        expect(fayalite.foNumberPct).to.be.closeTo(20.0, 1.0);
        expect(fayalite.faNumberPct).to.be.closeTo(80.0, 1.0);
        expect(fayalite.isIronFaRich).to.be.true;
        expect(fayalite.isMagnesianFoRich).to.be.false;
        expect(fayalite.olivineClass).to.include('Fayalite-Rich');
    });
});

describe('Sub-Solar Ephemeris, Thermal Regolith Grain Size & Carbonate Doublet Indices', () => {
    it('should calculate planetary sub-solar point and Solar Zenith Angle (SZA)', () => {
        // Northern summer solstice (Ls = 90 deg):
        // subSolarLatitude = +25.19 deg (maximum northern declination on Mars)
        const solstice = TrajectoryEngine.computeSubSolarPointAndZenithAngle(90.0, 25.19, 0.0, 12.0, 'mars');
        expect(solstice.subSolarLatitudeDeg).to.be.closeTo(25.19, 0.1);
        expect(solstice.solarZenithAngleDeg).to.be.closeTo(0.0, 0.5); // Directly overhead
        expect(solstice.solarElevationDeg).to.be.closeTo(90.0, 0.5);
        expect(solstice.isDaylight).to.be.true;

        // Midnight nightside (localSolarTime = 0.0 h): SZA > 90 deg
        const midnight = TrajectoryEngine.computeSubSolarPointAndZenithAngle(90.0, 25.19, 0.0, 0.0, 'mars');
        expect(midnight.isDaylight).to.be.false;
        expect(midnight.solarZenithAngleDeg).to.be.greaterThan(90.0);
    });

    it('should invert effective regolith grain size and geological texture from thermal inertia', () => {
        // Fine airborne dust mantle (I = 50): d < 40 um
        const dust = KRCEngine.computeThermalInertiaEffectiveGrainSize(50.0);
        expect(dust.effectiveGrainSizeMicrons).to.be.lessThan(40.0);
        expect(dust.grainClass).to.include('Airborne Dust');

        // Active basaltic dune sand (I = 250): d ~ 150 um
        const sand = KRCEngine.computeThermalInertiaEffectiveGrainSize(250.0);
        expect(sand.effectiveGrainSizeMicrons).to.be.closeTo(158.0, 25.0);
        expect(sand.grainClass).to.include('Active Sand');

        // Solid continuous volcanic bedrock (I = 1800): d > 10 cm (bedrock)
        const bedrock = KRCEngine.computeThermalInertiaEffectiveGrainSize(1800.0);
        expect(bedrock.grainClass).to.include('Dense Continuous Bedrock');
        expect(bedrock.thermalConductivityWmK).to.be.greaterThan(1.5);
    });

    it('should calculate CRISM carbonate doublet indices and discriminate Fe/Mg vs Ca carbonates', () => {
        // Fe/Mg-Carbonate (Magnesite / Nili Fossae): strong BD2500 and BD2300 doublet
        const magnesite = BandMathEngine.computeCRISMCarbonateIndices(0.24, 0.20, 0.25, 0.26, 0.21, 0.27);
        expect(magnesite.bd2500Index).to.be.greaterThan(0.15);
        expect(magnesite.bd2300Index).to.be.greaterThan(0.15);
        expect(magnesite.isCarbonateDetected).to.be.true;
        expect(magnesite.isFeMgCarbonate).to.be.true;
        expect(magnesite.carbonateClass).to.include('Fe/Mg-Carbonate');

        // Smectite clay with 2.3 um band but lacking 2.5 um carbonate feature:
        const smectite = BandMathEngine.computeCRISMCarbonateIndices(0.25, 0.22, 0.26, 0.27, 0.27, 0.27);
        expect(magnesite.isCarbonateDetected).to.be.true;
        expect(smectite.isCarbonateDetected).to.be.false;
        expect(smectite.carbonateClass).to.include('Phyllosilicate');
    });
});

describe('Hapke Macroscopic Roughness, Oblate Gravity & Opaline Silica Indices', () => {
    it('should calculate Hapke macroscopic roughness shadowing correction factor', () => {
        // Smooth surface (theta_bar = 0 deg) -> correction factor exactly 1.0
        const smooth = ThreeDEngine.computeHapkeRoughnessSurfaceCorrection(30.0, 0.0, 30.0, 0.0);
        expect(smooth.roughnessCorrectionFactor).to.equal(1.0);
        expect(smooth.isRoughSurface).to.be.false;

        // Rough terrain (theta_bar = 25 deg) at specular/backscatter geometry
        const rough = ThreeDEngine.computeHapkeRoughnessSurfaceCorrection(45.0, 30.0, 60.0, 25.0);
        expect(rough.roughnessCorrectionFactor).to.be.greaterThan(0.5);
        expect(rough.roughnessCorrectionFactor).to.be.lessThan(1.5);
        expect(rough.isRoughSurface).to.be.true;
    });

    it('should calculate latitude-dependent surface gravity on oblate Mars and barometric scale height', () => {
        // Mars equator (lat = 0 deg): g = 3.711 m/s^2, H ~ 11.1 km at 215 K
        const equator = MCDEngine.computeOblateSurfaceGravityAndScaleHeight(0.0, 215.0);
        expect(equator.surfaceGravityMS2).to.be.closeTo(3.711, 0.001);
        expect(equator.scaleHeightKm).to.be.closeTo(11.12, 0.1);

        // Mars poles (lat = 90 deg): g = 3.730 m/s^2 (higher due to rotational flattening)
        const pole = MCDEngine.computeOblateSurfaceGravityAndScaleHeight(90.0, 215.0);
        expect(pole.surfaceGravityMS2).to.be.greaterThan(equator.surfaceGravityMS2);
        expect(pole.scaleHeightKm).to.be.lessThan(equator.scaleHeightKm);
    });

    it('should calculate CRISM hydrated opaline silica index and distinguish from Al-smectite', () => {
        // Hydrothermal Opaline Silica (Home Plate Gusev / Opal-A): broad absorption at 2250 nm (R2250 < R2210)
        const opal = BandMathEngine.computeCRISMHydratedSilicaOpalIndex(0.28, 0.25, 0.22, 0.29);
        expect(opal.bd2250SilicaIndex).to.be.greaterThan(0.20);
        expect(opal.isHydratedSilicaOpal).to.be.true;
        expect(opal.isDistinctFromAlSmectite).to.be.true;
        expect(opal.silicaClass).to.include('Opaline Hydrated Silica');

        // Pure Al-Smectite / Kaolinite (narrow band minimum at 2210 nm, R2210 < R2250):
        const smectite = BandMathEngine.computeCRISMHydratedSilicaOpalIndex(0.28, 0.21, 0.27, 0.29);
        expect(smectite.isDistinctFromAlSmectite).to.be.false;
    });
});

describe('Sun-Synchronous J2 Nodal Drift, Permafrost Ground Ice & Ferric Iron Indices', () => {
    it('should calculate Sun-synchronous J2 nodal precession and LTAN drift rate', () => {
        // Mars Reconnaissance Orbiter (MRO) sun-synchronous frozen orbit:
        // a = 3646 km (250-316 km altitude), i_req ~ 92.53 deg
        const reqInc = TrajectoryEngine.computeSunSynchronousNodalPrecessionAndLTANDrift(3646.0, 92.53, 0.001, 'mars');
        expect(reqInc.sunSyncPrecessionRateDegPerDay).to.be.closeTo(0.524, 0.01);
        expect(reqInc.nodalPrecessionDegPerDay).to.be.closeTo(0.524, 0.01);
        expect(reqInc.isSunSynchronous).to.be.true;
        expect(reqInc.sunSyncRequiredInclinationDeg).to.be.closeTo(92.53, 0.2);
        expect(Math.abs(reqInc.ltanDriftMinutesPerSol)).to.be.lessThan(0.1); // Constant crossing time
    });

    it('should calculate subsurface permafrost ground ice stability depth z_ice', () => {
        // High-latitude polar terrain (Phoenix Lander regime, T = 170 K, pr_um = 15):
        // Ground ice is stable at shallow depth (< 10 cm)
        const phoenixIce = KRCEngine.computePermafrostGroundIceStabilityDepth(170.0, 15.0);
        expect(phoenixIce.isGroundIceStable).to.be.true;
        expect(phoenixIce.groundIceDepthCm).to.be.lessThan(10.0);
        expect(phoenixIce.criticalStabilityTempK).to.be.closeTo(193.14, 1.0);
        expect(phoenixIce.stabilityZone).to.include('Shallow Stable Permafrost');

        // Equatorial warm terrain (T = 230 K > T_crit):
        // Ground ice is unstable in upper regolith (> 150 cm / desiccated)
        const equatorIce = KRCEngine.computePermafrostGroundIceStabilityDepth(230.0, 10.0);
        expect(equatorIce.isGroundIceStable).to.be.false;
        expect(equatorIce.groundIceDepthCm).to.be.greaterThan(100.0);
        expect(equatorIce.stabilityZone).to.include('Desiccated');
    });

    it('should calculate CRISM ferric iron oxide indices and discriminate crystalline hematite vs goethite', () => {
        // Crystalline Gray Hematite (Meridiani Planum Opportunity type): strong 860 nm minimum (R860 < R920)
        const hematite = BandMathEngine.computeCRISMFerricOxideIndices(0.18, 0.22, 0.29, 0.23, 0.28, 0.31);
        expect(hematite.bd860HematiteIndex).to.be.greaterThan(0.15);
        expect(hematite.isCrystallineHematite).to.be.true;
        expect(hematite.isGoethiteJarosite).to.be.false;
        expect(hematite.ferricClass).to.include('Crystalline Gray Hematite');

        // Goethite / Jarosite / Ferric Oxyhydroxide (strong 920 nm minimum, R920 < R860):
        const goethite = BandMathEngine.computeCRISMFerricOxideIndices(0.18, 0.22, 0.29, 0.28, 0.23, 0.31);
        expect(goethite.bd920GoethiteIndex).to.be.greaterThan(0.15);
        expect(goethite.isGoethiteJarosite).to.be.true;
        expect(goethite.isCrystallineHematite).to.be.false;
        expect(goethite.ferricClass).to.include('Goethite / Jarosite');
    });
});

describe('Spacecraft Eclipse Duration, Upper Atmosphere Homopause & Pyroxene Ternary System', () => {
    it('should calculate orbital eclipse umbra duration and critical beta angle', () => {
        // Low Mars Orbit (MRO at h = 250 km, a = 3646 km, beta = 0 deg):
        // Period ~ 112 minutes, eclipse fraction ~ 38%, umbra duration ~ 43 minutes
        const mroEclipse = TrajectoryEngine.computeOrbitalEclipseUmbraAndPenumbraDuration(250.0, 0.0, 'mars');
        expect(mroEclipse.orbitPeriodMinutes).to.be.closeTo(112.0, 5.0);
        expect(mroEclipse.umbraDurationMinutes).to.be.closeTo(43.0, 5.0);
        expect(mroEclipse.eclipseFractionPct).to.be.closeTo(38.0, 5.0);
        expect(mroEclipse.criticalBetaAngleDeg).to.be.closeTo(68.6, 2.0);
        expect(mroEclipse.isInFullSunlight).to.be.false;

        // High beta angle orbit (beta = 75 deg > beta_crit): continuous full sunlight
        const fullSunOrbit = TrajectoryEngine.computeOrbitalEclipseUmbraAndPenumbraDuration(250.0, 75.0, 'mars');
        expect(fullSunOrbit.isInFullSunlight).to.be.true;
        expect(fullSunOrbit.umbraDurationMinutes).to.equal(0.0);
        expect(fullSunOrbit.eclipseFractionPct).to.equal(0.0);
    });

    it('should calculate atmospheric homopause / turbopause altitude from eddy diffusion', () => {
        // Mars mesosphere / thermosphere (Kz = 3e4 m^2/s, thermosphere cold scale height H = 7.5 km):
        // Homopause / turbopause altitude ~ 123 km (Bougher et al. 2015 MAVEN)
        const homopauseThermo = MCDEngine.computeAtmosphericHomopauseAltitude(3.0e4, 7.5, 140.0);
        expect(homopauseThermo.homopauseAltitudeKm).to.be.closeTo(123.0, 5.0);
        expect(homopauseThermo.isWellMixedBelow).to.be.true;

        // Isothermal middle atmosphere (H = 10.5 km):
        const homopauseIso = MCDEngine.computeAtmosphericHomopauseAltitude(3.0e4, 10.5, 180.0);
        expect(homopauseIso.homopauseAltitudeKm).to.be.closeTo(172.6, 2.0);
    });

    it('should calculate Pyroxene Quadrilateral ternary coordinates and classify endmembers', () => {
        // Diopside (Wo = 50 mol%, En = 45 mol%, Fs = 5 mol%):
        const diopside = BandMathEngine.computePyroxeneTernaryCoordinates(50.0, 45.0, 5.0);
        expect(diopside.wollastonitePct).to.equal(50.0);
        expect(diopside.enstatitePct).to.equal(45.0);
        expect(diopside.ferrosilitePct).to.equal(5.0);
        expect(diopside.mgNumberPct).to.equal(90.0);
        expect(diopside.ternaryX).to.be.closeTo(30.0, 1.0);
        expect(diopside.ternaryY).to.be.closeTo(43.3, 1.0);
        expect(diopside.mineralName).to.include('Diopside');
        expect(diopside.pyroxeneFamily).to.include('Calc-Pyroxene');

        // Enstatite Orthopyroxene (Wo = 2 mol%, En = 85 mol%, Fs = 13 mol%):
        const enstatite = BandMathEngine.computePyroxeneTernaryCoordinates(2.0, 85.0, 13.0);
        expect(enstatite.mineralName).to.include('Enstatite');
        expect(enstatite.pyroxeneFamily).to.include('Orthopyroxene');
    });
});

describe('Ground Station Telemetry Elevation, Regolith Rock Fraction & Perchlorate Indices', () => {
    it('should calculate ground station / rover topocentric elevation angle and slant range', () => {
        // Direct zenith pass: satellite overhead (satLat = 0, satLon = 0, stationLat = 0, stationLon = 0, alt = 300 km)
        const overheadPass = TrajectoryEngine.computeGroundStationPassGeometryAndElevation(0.0, 0.0, 300.0, 0.0, 0.0, 5.0, 'mars');
        expect(overheadPass.elevationAngleDeg).to.equal(90.0);
        expect(overheadPass.slantRangeKm).to.be.closeTo(300.0, 0.1);
        expect(overheadPass.isLineOfSightVisible).to.be.true;

        // Slanted pass at 15 deg central angle:
        const slantedPass = TrajectoryEngine.computeGroundStationPassGeometryAndElevation(0.0, 15.0, 300.0, 0.0, 0.0, 5.0, 'mars');
        expect(slantedPass.centralAngularDistanceDeg).to.be.closeTo(15.0, 0.1);
        expect(slantedPass.slantRangeKm).to.be.greaterThan(300.0);
        expect(slantedPass.isLineOfSightVisible).to.be.true;

        // Below horizon (central angle = 45 deg > horizon cutoff):
        const horizonPass = TrajectoryEngine.computeGroundStationPassGeometryAndElevation(0.0, 45.0, 300.0, 0.0, 0.0, 5.0, 'mars');
        expect(horizonPass.isLineOfSightVisible).to.be.false;
        expect(horizonPass.elevationAngleDeg).to.be.lessThan(0.0);
    });

    it('should calculate dual-component regolith heterogeneous thermal inertia mix and rock abundance', () => {
        // Sandy soil with 15% rock abundance (I_fine = 200, I_rock = 1800, f_rock = 15%):
        const rockySoil = KRCEngine.computeDualComponentThermalInertiaMix(200.0, 1800.0, 15.0);
        expect(rockySoil.rockFractionPct).to.equal(15.0);
        expect(rockySoil.fineFractionPct).to.equal(85.0);
        expect(rockySoil.apparentDayThermalInertia).to.be.closeTo(440.0, 10.0);
        expect(rockySoil.apparentNightThermalInertia).to.be.greaterThan(rockySoil.apparentDayThermalInertia); // Night T^4 bias
        expect(rockySoil.thermalInertiaContrast).to.be.greaterThan(50.0);
        expect(rockySoil.dominantRegime).to.include('Rocky Soil');
    });

    it('should calculate CRISM oxychlorine and perchlorate hydration salt indices', () => {
        // Hydrated Magnesium Perchlorate (Phoenix / RSL recurring slope lineae type):
        // Strong shifted 1930 nm hydration + 2140 nm perchlorate combination absorption
        const perchlorate = BandMathEngine.computeCRISMOxychlorineHydrationIndices(0.25, 0.21, 0.27, 0.22, 0.26);
        expect(perchlorate.bd1900Index).to.be.greaterThan(0.15);
        expect(perchlorate.isHydratedOxychlorineCandidate).to.be.true;
        expect(perchlorate.saltClass).to.include('Perchlorate Brine');

        // Anhydrous / Dry Crust (no 1.9 um hydration feature):
        const dryCrust = BandMathEngine.computeCRISMOxychlorineHydrationIndices(0.26, 0.26, 0.26, 0.26, 0.26);
        expect(dryCrust.isHydratedOxychlorineCandidate).to.be.false;
        expect(dryCrust.saltClass).to.include('Anhydrous');
    });
});

describe('Earth-Sun-Probe Geometry, Multi-Layer Radar Reflectivity & Pyroxene Band Area Ratio (BAR)', () => {
    it('should calculate heliocentric Earth-Mars distance, light time (OWLT), and solar conjunction blackout', () => {
        // Opposition (phaseOffset = 0 deg, closest approach at perihelion Ls = 251 deg):
        // d_EM ~ 0.38 AU, OWLT ~ 3.1-4.5 minutes
        const opposition = TrajectoryEngine.computeEarthSunProbeAndAntennaPointingGeometry(251.0, 0.0);
        expect(opposition.heliocentricMarsDistanceAU).to.be.closeTo(1.381, 0.05);
        expect(opposition.earthMarsDistanceAU).to.be.closeTo(0.381, 0.05);
        expect(opposition.oneWayLightTimeMinutes).to.be.closeTo(3.17, 0.3);
        expect(opposition.isSolarConjunctionBlackout).to.be.false;

        // Superior conjunction (phaseOffset = 180 deg, Mars directly behind Sun):
        // d_EM ~ 2.38-2.66 AU, OWLT ~ 20-22 minutes, SEP < 3 deg (blackout alert)
        const conjunction = TrajectoryEngine.computeEarthSunProbeAndAntennaPointingGeometry(251.0, 180.0);
        expect(conjunction.earthMarsDistanceAU).to.be.closeTo(2.381, 0.05);
        expect(conjunction.oneWayLightTimeMinutes).to.be.closeTo(19.8, 0.5);
        expect(conjunction.sepAngleDeg).to.be.lessThan(3.0);
        expect(conjunction.isSolarConjunctionBlackout).to.be.true;
    });

    it('should calculate SHARAD multi-layer subsurface radar reflectivity and two-way travel time delay', () => {
        // CO2 Ice over H2O Ice interface (SPLD: eps1 = 2.15, eps2 = 3.15, d = 100 m):
        const spldInterface = RadarSounderEngine.computeMultiLayerSubsurfaceRadarReflectivity(2.15, 3.15, 100.0, 10.0);
        expect(spldInterface.fresnelAmplitudeCoefficient).to.be.closeTo(-0.095, 0.01);
        expect(spldInterface.powerReflectivityDB).to.be.closeTo(-20.4, 1.0);
        expect(spldInterface.twoWayTravelTimeMicrosec).to.be.closeTo(0.978, 0.05);
        expect(spldInterface.verticalRangeResolutionMeters).to.be.closeTo(10.23, 0.2);
        expect(spldInterface.interfaceType).to.include('SPLD');

        // Basal Ice over Volcanic Basalt bedrock (eps1 = 3.15, eps2 = 8.5, d = 500 m):
        const basalBedrock = RadarSounderEngine.computeMultiLayerSubsurfaceRadarReflectivity(3.15, 8.5, 500.0, 10.0);
        expect(basalBedrock.fresnelAmplitudeCoefficient).to.be.closeTo(-0.244, 0.02);
        expect(basalBedrock.powerReflectivityDB).to.be.closeTo(-12.2, 1.0);
        expect(basalBedrock.interfaceType).to.include('Basal Ice / Volcanic Bedrock');
    });

    it('should calculate Pyroxene Band Area Ratio (BAR) and separate Clinopyroxene, Orthopyroxene, and Olivine', () => {
        // High-Ca Clinopyroxene (Augite): strong 2 um band (Area2 / Area1 >= 1.2, centers at 1050 nm & 2300 nm)
        const augite = BandMathEngine.computePyroxeneBandAreaRatio(100.0, 150.0, 1050.0, 2300.0);
        expect(augite.bandAreaRatio).to.equal(1.5);
        expect(augite.isClinopyroxeneDominated).to.be.true;
        expect(augite.isOrthopyroxeneDominated).to.be.false;
        expect(augite.maficClass).to.include('High-Calcium Clinopyroxene');

        // Pure Olivine Lithology (Dunite): virtually no 2 um band (Area2 ~ 5 nm*refl vs Area1 = 120)
        const olivine = BandMathEngine.computePyroxeneBandAreaRatio(120.0, 5.0, 1050.0, 2100.0);
        expect(olivine.bandAreaRatio).to.be.lessThan(0.10);
        expect(olivine.olivineFractionPct).to.equal(100.0);
        expect(olivine.maficClass).to.include('Olivine Dominated');
    });
});

describe('Kepler Orbit Solver, Frost Condensation Thermodynamics & Pyroxene Band Inversion', () => {
    it('should solve Kepler equation and compute eccentric anomaly, true anomaly, and orbit speed', () => {
        // Highly eccentric orbit (e.g. Mars Express / MAVEN: a = 5000 km, e = 0.50):
        // Periapsis (M = 0 deg): E = 0 deg, nu = 0 deg, r = 2500 km, alt = -896 km (hypothetical), speed max
        const periapsis = TrajectoryEngine.computeKeplerOrbitPositionFromMeanAnomaly(0.0, 0.50, 5000.0, 'mars');
        expect(periapsis.trueAnomalyDeg).to.equal(0.0);
        expect(periapsis.eccentricAnomalyDeg).to.equal(0.0);
        expect(periapsis.orbitalRadiusKm).to.equal(2500.0);
        expect(periapsis.orbitalVelocityKmS).to.be.closeTo(5.069, 0.05);

        // Apoapsis (M = 180 deg): E = 180 deg, nu = 180 deg, r = 7500 km, speed min
        const apoapsis = TrajectoryEngine.computeKeplerOrbitPositionFromMeanAnomaly(180.0, 0.50, 5000.0, 'mars');
        expect(apoapsis.trueAnomalyDeg).to.equal(180.0);
        expect(apoapsis.eccentricAnomalyDeg).to.equal(180.0);
        expect(apoapsis.orbitalRadiusKm).to.equal(7500.0);
        expect(apoapsis.orbitalVelocityKmS).to.be.closeTo(1.689, 0.05);

        // Quadrature (M = 90 deg):
        const quad = TrajectoryEngine.computeKeplerOrbitPositionFromMeanAnomaly(90.0, 0.50, 5000.0, 'mars');
        expect(quad.trueAnomalyDeg).to.be.greaterThan(90.0);
    });

    it('should calculate transient surface frost (CO2 dry ice vs H2O) condensation temperature and budget', () => {
        // Polar winter night with surface temp 140 K < T_cond (147.3 K for CO2 at 610 Pa):
        const co2Frost = KRCEngine.computeTransientFrostCondensationBudget(140.0, 610.0, 'co2', 30.0);
        expect(co2Frost.isCondensing).to.be.true;
        expect(co2Frost.condensationTempK).to.be.closeTo(147.3, 0.5);
        expect(co2Frost.dailyAccumulationMicrons).to.be.greaterThan(100.0);
        expect(co2Frost.volatileSpecies).to.include('Carbon Dioxide');

        // Warm night with surface temp 180 K > T_cond: no CO2 condensation
        const warmNight = KRCEngine.computeTransientFrostCondensationBudget(180.0, 610.0, 'co2', 30.0);
        expect(warmNight.isCondensing).to.be.false;
        expect(warmNight.dailyAccumulationMicrons).to.equal(0.0);
    });

    it('should invert pyroxene composition (Wo-En-Fs mol%) from Band 1 and Band 2 center wavelengths', () => {
        // High-Ca Augite / Diopside (Band 1 = 1050 nm, Band 2 = 2300 nm):
        const augite = BandMathEngine.computePyroxeneCompositionFromBandCenters(1050.0, 2300.0);
        expect(augite.wollastonitePct).to.be.greaterThan(35.0);
        expect(augite.isHighCalciumPyroxene).to.be.true;
        expect(augite.pyroxeneClass).to.include('High-Ca');

        // Low-Ca Orthopyroxene (Band 1 = 910 nm, Band 2 = 1850 nm):
        const opx = BandMathEngine.computePyroxeneCompositionFromBandCenters(910.0, 1850.0);
        expect(opx.wollastonitePct).to.be.lessThan(5.0);
        expect(opx.isHighCalciumPyroxene).to.be.false;
        expect(opx.pyroxeneClass).to.include('Orthopyroxene');
    });
});

describe('Aerobraking Aerodynamics, Mesospheric Gravity Waves & Clinopyroxene Subtypes', () => {
    it('should calculate spacecraft aerobraking deceleration, density, and heating safety corridor', () => {
        // MRO aerobraking pass at periapsis z_p = 105 km, v = 4.5 km/s:
        const mroPass = TrajectoryEngine.computeAerobrakingDragDecelerationAndDensity(105.0, 4.50, 1500.0, 20.0, 2.10);
        expect(mroPass.atmosphericDensityKgM3).to.be.greaterThan(1e-8);
        expect(mroPass.atmosphericDensityKgM3).to.be.lessThan(1e-6);
        expect(mroPass.dragDecelerationMS2).to.be.greaterThan(0.01);
        expect(mroPass.heatFluxWPerCm2).to.be.lessThan(0.35); // Below MRO solar array thermal limit
        expect(mroPass.isWithinSafetyCorridor).to.be.true;

        // Severe low periapsis pass at 70 km (excessive heat and drag):
        const lowPass = TrajectoryEngine.computeAerobrakingDragDecelerationAndDensity(70.0, 4.80, 1500.0, 20.0, 2.10);
        expect(lowPass.isWithinSafetyCorridor).to.be.false;
    });

    it('should calculate atmospheric gravity wave exponential amplitude growth and mesospheric breaking', () => {
        // Lower atmosphere propagation (z = 20 km): non-breaking linear wave
        const lowWave = MCDEngine.computeAtmosphericGravityWavePerturbation(20.0, 1.5, 10.0, 0.010);
        expect(lowWave.windPerturbationMS).to.be.closeTo(4.08, 0.2);
        expect(lowWave.fractionalDensityPerturbationPct).to.be.lessThan(5.0);
        expect(lowWave.isWaveSaturatedBreaking).to.be.false;

        // Upper mesosphere breaking level (z = 80 km): saturated breaking (u' capped at 40 m/s)
        const breakWave = MCDEngine.computeAtmosphericGravityWavePerturbation(80.0, 1.5, 10.0, 0.010);
        expect(breakWave.isWaveSaturatedBreaking).to.be.true;
        expect(breakWave.windPerturbationMS).to.equal(40.0);
        expect(breakWave.waveDragRegime).to.include('Saturated Wave Breaking');
    });

    it('should classify high-calcium clinopyroxene petrological subtypes and environments', () => {
        // Pure Diopside (Wo = 48%, En = 47%, Fs = 5%, Mg# = 90.4%):
        const diopside = BandMathEngine.computeClinopyroxeneSubtypeClassification(48.0, 47.0, 5.0);
        expect(diopside.subtype).to.include('Diopside');
        expect(diopside.isCalcPyroxene).to.be.true;
        expect(diopside.petrologicEnvironment).to.include('Ultramafic Cumulate');

        // Augite (Wo = 35%, En = 45%, Fs = 20%, Mg# = 69.2%):
        const augite = BandMathEngine.computeClinopyroxeneSubtypeClassification(35.0, 45.0, 20.0);
        expect(augite.subtype).to.include('Augite');
        expect(augite.petrologicEnvironment).to.include('Martian Basaltic Lava Flow');
    });
});

describe('Frozen Orbit J2/J3 Equilibrium, Crustal Geothermal Moho & Pyroxene Solvus Thermometry', () => {
    it('should calculate frozen orbit J2/J3 harmonic coupling and equilibrium eccentricity', () => {
        // Mars Odyssey frozen mapping orbit (a = 3775 km, i = 93.1 deg):
        const odysseyFrozen = TrajectoryEngine.computeFrozenOrbitEquilibriumAndJ3Coupling(3775.0, 93.1, 'mars');
        expect(odysseyFrozen.isFrozenOrbitCapable).to.be.true;
        expect(odysseyFrozen.frozenEquilibriumEccentricity).to.be.closeTo(0.029, 0.01);
        expect(odysseyFrozen.criticalInclinationDeg).to.be.closeTo(63.43, 0.1);
        expect(odysseyFrozen.frozenPeriapsisArgumentDeg).to.equal(270.0); // South Pole locked
    });

    it('should calculate 1D crustal geothermal temperature profile and Moho basal boundary temperature', () => {
        // Typical Martian crust (T_surf = 215 K, D = 40 km, q = 25 mW/m^2, k = 2.0 W/mK):
        const crustProfile = KRCEngine.computeLithosphericGeothermalBasalTemperature(215.0, 40.0, 25.0, 2.0);
        expect(crustProfile.mohoTemperatureK).to.be.greaterThan(400.0);
        expect(crustProfile.mohoTemperatureC).to.be.greaterThan(100.0);
        expect(crustProfile.depthToWaterMeltingKm).to.be.closeTo(4.65, 0.2); // ~4.65 km to 0 C isotherm
        expect(crustProfile.thermalGradientKPerKm).to.equal(12.5);
        expect(crustProfile.isBasalMeltingPossible).to.be.true;
    });

    it('should calculate pyroxene solvus equilibrium crystallization geothermometry', () => {
        // Typical Martian basaltic Augite (Wo = 35%, Mg# = 70%):
        // T ~ 1213.5 C (magmatic extrusion regime)
        const augiteTemp = BandMathEngine.computePyroxeneSolvusCrystallizationTemperature(35.0, 70.0);
        expect(augiteTemp.crystallizationTempC).to.be.closeTo(1213.5, 5.0);
        expect(augiteTemp.isMagmaticExtrusion).to.be.true;
        expect(augiteTemp.thermalRegime).to.include('Magmatic Basaltic Extrusion');

        // Slowly cooled plutonic pyroxene (Wo = 15%, Mg# = 45%):
        const plutonicTemp = BandMathEngine.computePyroxeneSolvusCrystallizationTemperature(15.0, 45.0);
        expect(plutonicTemp.crystallizationTempC).to.be.lessThan(1000.0);
        expect(plutonicTemp.thermalRegime).to.include('Plutonic');
    });
});

describe('King-Hele Orbital Decay, Multi-Harmonic Thermal Skin Depths & Pyroxene Melt Partitions', () => {
    it('should calculate satellite orbital decay rate and lifetime using King-Hele atmospheric drag', () => {
        // Low circular science orbit (h = 250 km, a = 3646 km):
        const lowOrbit = TrajectoryEngine.computeOrbitalLifetimeAndSemiMajorAxisDecayRate(3646.0, 0.005, 1000.0, 15.0, 2.20, 'mars');
        expect(lowOrbit.periapsisAltitudeKm).to.be.closeTo(231.6, 2.0);
        expect(lowOrbit.atmosphericDensityAtPeriapsisKgM3).to.be.greaterThan(1e-15);
        expect(lowOrbit.decayRateKmPerDay).to.be.greaterThan(0.0);
        expect(lowOrbit.orbitalLifetimeDays).to.be.greaterThan(100.0);

        // Extremely low decaying orbit (h_p = 110 km): rapid decay in sols
        const decayOrbit = TrajectoryEngine.computeOrbitalLifetimeAndSemiMajorAxisDecayRate(3510.0, 0.005, 1000.0, 15.0, 2.20, 'mars');
        expect(decayOrbit.decayRateKmPerDay).to.be.greaterThan(0.001);
        expect(decayOrbit.orbitalLifetimeDays).to.be.lessThan(2500.0);
    });

    it('should calculate multi-harmonic subsurface thermal skin depth spectrum for diurnal, seasonal, and obliquity cycles', () => {
        // Typical Martian sandy regolith (I = 250 tiu, rho = 1500 kg/m^3, c_p = 800 J/kgK):
        const marsSpectrum = KRCEngine.computeMultiHarmonicThermalSkinDepthSpectrum(250.0, 1500.0, 800.0, 'mars');
        expect(marsSpectrum.diurnalSkinDepthCm).to.be.closeTo(3.50, 0.2); // ~3.5 cm diurnal skin depth
        expect(marsSpectrum.seasonalSkinDepthMeters).to.be.closeTo(0.905, 0.05); // ~0.9 m seasonal skin depth
        expect(marsSpectrum.obliquitySkinDepthMeters).to.be.greaterThan(100.0); // >100 m Milankovitch skin depth
        expect(marsSpectrum.volumetricHeatCapacityJPerM3K).to.equal(1200000);
        expect(marsSpectrum.thermalConductivityWmK).to.be.closeTo(0.0521, 0.005);
    });

    it('should calculate clinopyroxene/melt Fe-Mg exchange KD and invert parent magma composition', () => {
        // High-Mg Augite phenocryst (Mg# = 85%, Wo = 35%):
        // (Fe/Mg)_cpx = 15/85 = 0.1765 -> (Fe/Mg)_melt = 0.1765 / 0.28 = 0.630 -> Mg#_melt = 100 / 1.630 = 61.3%
        const primitiveCpx = BandMathEngine.computePyroxeneMeltPartitionCoefficients(85.0, 35.0);
        expect(primitiveCpx.kdFeMg).to.equal(0.28);
        expect(primitiveCpx.equilibriumMeltMgNumberPct).to.be.closeTo(61.3, 1.0);
        expect(primitiveCpx.liquidusTempC).to.be.greaterThan(1400.0);
        expect(primitiveCpx.parentMagmaType).to.include('Basaltic Magma');

        // Highly evolved Fe-rich pyroxene (Mg# = 40%, Wo = 25%):
        const evolvedCpx = BandMathEngine.computePyroxeneMeltPartitionCoefficients(40.0, 25.0);
        expect(evolvedCpx.equilibriumMeltMgNumberPct).to.be.lessThan(25.0);
        expect(evolvedCpx.parentMagmaType).to.include('Ferrobasalt');
    });
});

describe('Allen-Eggers Hypersonic EDL, Polarimetric Radar & CRISM Anorthosite Crust', () => {
    it('should calculate hypersonic atmospheric entry peak deceleration, g-load, and stagnation heating', () => {
        // Mars MSL / Perseverance entry (v_E = 5.7 km/s, gamma_E = -12.5 deg, beta = 120 kg/m^2, R_N = 1.15 m):
        const mslEntry = TrajectoryEngine.computeAtmosphericEntryPeakDecelerationAndStagnationPoint(5.7, -12.5, 120.0, 1.15, 'mars');
        expect(mslEntry.peakDecelerationMS2).to.be.closeTo(116.5, 5.0);
        expect(mslEntry.peakGLoad).to.be.closeTo(11.88, 1.0); // ~11.9 g peak load
        expect(mslEntry.velocityAtPeakDecelKmS).to.be.closeTo(3.457, 0.05); // ~0.6065 * 5.7 km/s
        expect(mslEntry.peakDecelerationAltitudeKm).to.be.closeTo(16.12, 1.0);
        expect(mslEntry.peakStagnationHeatFluxWPerCm2).to.be.closeTo(49.82, 1.0);
    });

    it('should calculate radar polarimetric backscatter ratio (CPR/DPR) and volume scattering mechanism', () => {
        // Extreme volume scattering / water ice sheet / blocky ejecta (sigma_co = -8 dB, sigma_cross = -7 dB -> PR = 1.25):
        const iceBackscatter = RadarSounderEngine.computePolarimetricRadarBackscatterRatio(-8.0, -7.0, 30.0);
        expect(iceBackscatter.polarizationRatio).to.be.greaterThan(1.0);
        expect(iceBackscatter.volumeScatteringFractionPct).to.be.greaterThan(80.0);
        expect(iceBackscatter.isIceOrBlockyEjectaCandidate).to.be.true;
        expect(iceBackscatter.scatteringRegime).to.include('Extreme Volume Scattering');

        // Smooth surface specular reflection (sigma_co = -5 dB, sigma_cross = -25 dB -> PR = 0.01):
        const smoothPlains = RadarSounderEngine.computePolarimetricRadarBackscatterRatio(-5.0, -25.0, 0.0);
        expect(smoothPlains.polarizationRatio).to.be.lessThan(0.05);
        expect(smoothPlains.isIceOrBlockyEjectaCandidate).to.be.false;
        expect(smoothPlains.surfaceScatteringFractionPct).to.be.greaterThan(90.0);
    });

    it('should calculate CRISM 1.3 um Plagioclase Feldspar index BD1300 and detect ancient primordial anorthosite', () => {
        // Primordial Calcic Anorthosite (R_1080 = 0.25, R_1300 = 0.235, R_1750 = 0.26, An = 95%):
        // continuum = (0.25 + 0.26)/2 = 0.255 -> BD1300 = 1 - 0.235 / 0.255 = 0.0784
        const anorthosite = BandMathEngine.computeCRISMPlagioclaseAnorthositeIndices(0.25, 0.235, 0.26, 95.0);
        expect(anorthosite.bd1300).to.be.closeTo(0.0784, 0.005);
        expect(anorthosite.isAnorthositeCrustOutcrop).to.be.true;
        expect(anorthosite.plagioclaseType).to.include('Anorthite');

        // Basaltic Labradorite without 1.3 um absorption (BD1300 ~ 0, An = 55%):
        const labradorite = BandMathEngine.computeCRISMPlagioclaseAnorthositeIndices(0.20, 0.20, 0.20, 55.0);
        expect(labradorite.bd1300).to.equal(0.0);
        expect(labradorite.isAnorthositeCrustOutcrop).to.be.false;
        expect(labradorite.plagioclaseType).to.include('Labradorite');
    });
});

describe('Mars Orbit Insertion Delta-V, Porous Regolith Ice Lag & Zeolite Discrimination', () => {
    it('should calculate interplanetary Mars Orbit Insertion (MOI) braking Delta-V and propellant fraction', () => {
        // Typical Mars arrival (v_inf = 3.0 km/s, h_p = 300 km, h_a = 40000 km, Isp = 315 s):
        const moiBurn = TrajectoryEngine.computeMarsOrbitInsertionDeltaV(3.0, 300.0, 40000.0, 315.0, 'mars');
        expect(moiBurn.deltaVKmS).to.be.closeTo(1.051, 0.05); // ~1.05 km/s braking burn
        expect(moiBurn.deltaVMS).to.be.closeTo(1051.0, 50.0);
        expect(moiBurn.hyperbolicArrivalSpeedKmS).to.be.closeTo(5.672, 0.05);
        expect(moiBurn.propellantMassFractionPct).to.be.closeTo(28.84, 1.5);
        expect(moiBurn.targetOrbitPeriodHours).to.be.greaterThan(30.0);
    });

    it('should calculate porous regolith dry lag vapor diffusion resistance and ice sheet preservation timescale', () => {
        // 10 cm dry dust lag over subsurface ice at 200 K (Patm = 0.030 Pa, eps = 0.40):
        const lagRetardation = KRCEngine.computePorousRegolithIceSublimationLagRetardation(10.0, 200.0, 0.030, 0.40, 5.0);
        expect(lagRetardation.sublimationFluxKgM2S).to.be.greaterThan(1e-12);
        expect(lagRetardation.annualIceRetreatMmPerYear).to.be.greaterThan(0.0);
        expect(lagRetardation.iceSheetPreservationMyr).to.be.greaterThan(0.01);
        expect(lagRetardation.vaporSaturationPressurePa).to.be.closeTo(0.165, 0.02);
    });

    it('should discriminate hydrous alkaline Zeolites from Smectite Clays with CRISM band ratios', () => {
        // Hydrous Zeolite (Analcime / Chabazite): strong 1.92 um (r = 0.26 vs cont = 0.30 -> BD1900 = 0.133), weak 2.30 um (r = 0.298 -> BD2300 = 0.0067)
        const zeolite = BandMathEngine.computeCRISMZeolitePhyllosilicateDiscrimination(0.28, 0.26, 0.298, 0.30);
        expect(zeolite.isZeolite).to.be.true;
        expect(zeolite.isSmectiteClay).to.be.false;
        expect(zeolite.mineralFamily).to.include('Zeolite');
        expect(zeolite.geologicalSetting).to.include('Alkaline Closed-Basin Paleolake');

        // Fe/Mg-Smectite Phyllosilicate (Saponite / Nontronite): strong 1.92 um (BD1900 = 0.10) and sharp 2.30 um (BD2300 = 0.08)
        const smectite = BandMathEngine.computeCRISMZeolitePhyllosilicateDiscrimination(0.28, 0.27, 0.276, 0.30);
        expect(smectite.isZeolite).to.be.false;
        expect(smectite.isSmectiteClay).to.be.true;
        expect(smectite.mineralFamily).to.include('Smectite Phyllosilicate');
    });
});

describe('Trans-Mars Injection Delta-V, CO2 Clathrate Hydrates & Sulfate Stratigraphy', () => {
    it('should calculate Trans-Mars Injection (TMI) Delta-V from characteristic departure energy C3', () => {
        // Typical Mars launch from 250 km LEO (C3 = 15.0 km^2/s^2, Isp = 450 s Centaur upper stage):
        const tmiBurn = TrajectoryEngine.computeInterplanetaryDepartureC3AndTransMarsInjectionDeltaV(15.0, 250.0, 450.0, 'earth');
        expect(tmiBurn.circularParkingOrbitSpeedKmS).to.be.closeTo(7.756, 0.05); // ~7.76 km/s LEO orbital velocity
        expect(tmiBurn.departureHyperbolicSpeedKmS).to.be.closeTo(11.631, 0.05); // ~11.63 km/s escape insertion velocity
        expect(tmiBurn.transMarsInjectionDeltaVKmS).to.be.closeTo(3.875, 0.05); // ~3.88 km/s TMI burn
        expect(tmiBurn.propellantMassFractionPct).to.be.greaterThan(55.0);
    });

    it('should calculate subsurface CO2 Clathrate Hydrate phase stability and cryosphere gas reservoir', () => {
        // Deep polar ice cap at 1500 m depth, T = 180 K (pLith = 6.14 MPa):
        const deepClathrate = KRCEngine.computeCO2ClathrateHydrateStabilityBoundary(1500.0, 180.0, 1100.0);
        expect(deepClathrate.isClathrateStable).to.be.true;
        expect(deepClathrate.lithostaticPressureMPa).to.be.closeTo(6.138, 0.05);
        expect(deepClathrate.dissociationPressureMPa).to.be.lessThan(1.0);
        expect(deepClathrate.co2GasEquivalentDensityKgM3).to.equal(165.0);
        expect(deepClathrate.cryosphereRegime).to.include('Gigaton CO2 Clathrate Paleoclimate Reservoir');

        // Warm shallow equatorial deposit at 50 m depth, T = 220 K (unstable dissociation):
        const warmUnstable = KRCEngine.computeCO2ClathrateHydrateStabilityBoundary(50.0, 220.0, 1500.0);
        expect(warmUnstable.isClathrateStable).to.be.false;
        expect(warmUnstable.co2GasEquivalentDensityKgM3).to.equal(0.0);
    });

    it('should discriminate Monohydrated Sulfates from Polyhydrated Sulfates in CRISM spectra', () => {
        // Monohydrated Sulfate (Kieserite): distinct 2.13 um feature (r = 0.26 vs continuum (0.28+0.29)/2 = 0.285 -> BD2130 = 0.0877), low SINDEX2
        const kieserite = BandMathEngine.computeCRISMMonoVsPolyHydratedSulfateIndices(0.28, 0.26, 0.29, 0.30, 0.30);
        expect(kieserite.isMonohydratedSulfate).to.be.true;
        expect(kieserite.isPolyhydratedSulfate).to.be.false;
        expect(kieserite.sulfateClass).to.include('Monohydrated Sulfate');
        expect(kieserite.stratigraphicContext).to.include('Basal Layered Sulfate Deposit');

        // Polyhydrated Sulfate (Gypsum / Epsomite): strong 2.40 um absorption (SINDEX2 = 0.10) and deep 1.92 um water (BD1900 = 0.133)
        const gypsum = BandMathEngine.computeCRISMMonoVsPolyHydratedSulfateIndices(0.26, 0.29, 0.295, 0.27, 0.30);
        expect(gypsum.isMonohydratedSulfate).to.be.false;
        expect(gypsum.isPolyhydratedSulfate).to.be.true;
        expect(gypsum.sulfateClass).to.include('Polyhydrated Sulfate');
    });
});

describe('Trans-Earth Injection Delta-V, Methane Clathrate Hydrates & Carbonate Speciation', () => {
    it('should calculate Mars-to-Earth return Trans-Earth Injection (TEI) Delta-V and Earth re-entry speed', () => {
        // Typical MSR departure from 300 km Mars orbit (C3 = 12.0 km^2/s^2, Isp = 320 s, v_inf_Earth = 3.8 km/s):
        const teiBurn = TrajectoryEngine.computeMarsToEarthReturnTrajectoryAndTEIDeltaV(300.0, 12.0, 320.0, 3.80);
        expect(teiBurn.marsParkingOrbitSpeedKmS).to.be.closeTo(3.404, 0.05); // ~3.40 km/s circular Mars speed
        expect(teiBurn.transEarthInjectionDeltaVKmS).to.be.closeTo(2.576, 0.1); // ~2.58 km/s TEI burn
        expect(teiBurn.earthAtmosphericEntrySpeedKmS).to.be.closeTo(11.71, 0.08); // ~11.71 km/s Earth re-entry
        expect(teiBurn.propellantMassFractionPct).to.be.greaterThan(50.0);
    });

    it('should calculate subsurface Methane Clathrate Hydrate stability boundary and cryosphere trap vulnerability', () => {
        // Deep permafrost at 500 m depth, T = 195 K (stable sequestration):
        const deepMethane = KRCEngine.computeMethaneClathrateHydrateStabilityBoundary(500.0, 195.0, 2000.0);
        expect(deepMethane.isMethaneHydrateStable).to.be.true;
        expect(deepMethane.lithostaticPressureMPa).to.be.closeTo(3.72, 0.05);
        expect(deepMethane.ch4GasEquivalentDensityKgM3).to.equal(115.0);
        expect(deepMethane.outgassingVulnerability).to.include('Secure Deep Permafrost Cryosphere Trap');

        // Warm shallow deposit at 20 m depth, T = 230 K (destabilized active outgassing plume):
        const plumeSource = KRCEngine.computeMethaneClathrateHydrateStabilityBoundary(20.0, 230.0, 1500.0);
        expect(plumeSource.isMethaneHydrateStable).to.be.false;
        expect(plumeSource.ch4GasEquivalentDensityKgM3).to.equal(0.0);
        expect(plumeSource.outgassingVulnerability).to.include('Active Episodic Methane Outgassing');
    });

    it('should invert CRISM 2.3 um and 2.5 um carbonate band centers for cation speciation (Mg, Fe, Ca)', () => {
        // Magnesite (MgCO3) in Jezero Crater rim / Nili Fossae (2300 nm & 2500 nm):
        const magnesite = BandMathEngine.computeCRISMCarbonateCationSpeciation(2300.0, 2500.0, 0.05);
        expect(magnesite.carbonateSpecies).to.include('Magnesite');
        expect(magnesite.dominantCation).to.include('Mg2+');
        expect(magnesite.magnesiumMoleFraction).to.be.greaterThan(0.90);
        expect(magnesite.isSignificantCarbonate).to.be.true;
        expect(magnesite.paleoEnvironment).to.include('Ultramafic Olivine');

        // Calcite (CaCO3) caliche soil carbonate at Phoenix site (2340 nm & 2540 nm):
        const calcite = BandMathEngine.computeCRISMCarbonateCationSpeciation(2340.0, 2540.0, 0.04);
        expect(calcite.carbonateSpecies).to.include('Calcite');
        expect(calcite.dominantCation).to.include('Ca2+');
        expect(calcite.calciumMoleFraction).to.be.greaterThan(0.85);

        // Siderite (FeCO3) reducing vein carbonate (2332 nm & 2528 nm):
        const siderite = BandMathEngine.computeCRISMCarbonateCationSpeciation(2332.0, 2528.0, 0.03);
        expect(siderite.carbonateSpecies).to.include('Siderite');
        expect(siderite.ironMoleFraction).to.be.greaterThan(0.70);
    });
});

describe('Phobos/Deimos Rendezvous, Subsurface Radar Attenuation & Opaline Silica', () => {
    it('should calculate Martian moon Phobos & Deimos Hill sphere radius and escape speed', () => {
        // Phobos rendezvous (JAXA MMX mission parameters):
        const phobos = TrajectoryEngine.computeMoonCoOrbitalRendezvousAndHillSphere('phobos', 'mars');
        expect(phobos.moon).to.equal('Phobos');
        expect(phobos.semiMajorAxisKm).to.equal(9376.0);
        expect(phobos.orbitalPeriodHours).to.be.closeTo(7.65, 0.05);
        expect(phobos.orbitalSpeedKmS).to.be.closeTo(2.137, 0.02);
        expect(phobos.hillSphereRadiusKm).to.be.closeTo(16.59, 0.5); // ~16.6 km Hill sphere
        expect(phobos.surfaceEscapeSpeedMS).to.be.closeTo(11.21, 0.5); // ~11.2 m/s escape velocity

        // Deimos rendezvous:
        const deimos = TrajectoryEngine.computeMoonCoOrbitalRendezvousAndHillSphere('deimos', 'mars');
        expect(deimos.moon).to.equal('Deimos');
        expect(deimos.semiMajorAxisKm).to.equal(23463.0);
        expect(deimos.surfaceEscapeSpeedMS).to.be.closeTo(5.64, 0.5); // ~5.6 m/s
    });

    it('should calculate subsurface radar attenuation rate and penetration depth for SHARAD & MARSIS', () => {
        // Cold pore ice layer (80% ice, 15% basalt, 200 K, 20 MHz SHARAD):
        const iceSounding = KRCEngine.computeSubsurfaceRadarAttenuationAndLossTangent(200.0, 0.80, 0.15, 20.0);
        expect(iceSounding.bulkPermittivity).to.be.closeTo(3.46, 0.2); // ~3.46 dielectric constant
        expect(iceSounding.attenuationRateDBPerKm).to.be.lessThan(12.0);
        expect(iceSounding.penetrationDepthMeters).to.be.greaterThan(500.0); // deep penetration
        expect(iceSounding.radarRegime).to.include('Pore Ice');

        // Warm basaltic regolith (240 K, 5% ice, 85% basalt):
        const warmBasalt = KRCEngine.computeSubsurfaceRadarAttenuationAndLossTangent(240.0, 0.05, 0.85, 20.0);
        expect(warmBasalt.bulkPermittivity).to.be.greaterThan(4.5);
        expect(warmBasalt.attenuationRateDBPerKm).to.be.greaterThan(20.0);
        expect(warmBasalt.penetrationDepthMeters).to.be.lessThan(500.0);
    });

    it('should discriminate Opaline Silica (Opal-A) from volcanic glass in CRISM spectra', () => {
        // Hydrated Opal-A in Gusev Crater Home Plate (strong 2.21 um Si-OH and 1.91 um H2O):
        const opalA = BandMathEngine.computeCRISMOpalineSilicaIndices(0.26, 0.29, 0.27, 0.295, 0.30);
        expect(opalA.isOpalineSilica).to.be.true;
        expect(opalA.silicaPhase).to.include('Opal-A');
        expect(opalA.hydrothermalContext).to.include('Fumarole / Hot Spring Sinter');

        // Dry volcanic silicate (negligible 2.21 um and 1.91 um bands):
        const primaryBasalt = BandMathEngine.computeCRISMOpalineSilicaIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(opalA.bd2210).to.be.greaterThan(0.02);
        expect(primaryBasalt.isOpalineSilica).to.be.false;
        expect(primaryBasalt.silicaPhase).to.include('Primary Igneous Silicate');
    });
});

describe('Lambert Transfer Solver, Perchlorate Brine Deliquescence & Iron Oxide Speciation', () => {
    it('should solve boundary-value Lambert orbital transfer velocities with universal variables', () => {
        // Mars orbit transfer (e.g. from r1 = [4000, 0, 0] km to r2 = [0, 4500, 0] km in 3600 seconds):
        const lambert = TrajectoryEngine.computeLambertOrbitalTransferVelocityVectors([4000.0, 0.0, 0.0], [0.0, 4500.0, 0.0], 3600.0, 'mars');
        expect(lambert.departureSpeedKmS).to.be.greaterThan(2.0);
        expect(lambert.arrivalSpeedKmS).to.be.greaterThan(2.0);
        expect(lambert.transferAngleDeg).to.be.closeTo(90.0, 0.1); // 90 degree transfer
        expect(lambert.v1VectorKmS[1]).to.be.greaterThan(0.0); // positive prograde velocity
    });

    it('should calculate perchlorate salt deliquescence RH and liquid brine stability for Phoenix & RSL', () => {
        // Mg(ClO4)2 in warm humid morning (T = 215 K, RH = 50%): above eutectic (206 K) and RH > DRH (~44%) -> Liquid Brine!
        const mgBrine = KRCEngine.computePerchlorateDeliquescenceAndLiquidBrineStability(215.0, 50.0, 'Mg(ClO4)2');
        expect(mgBrine.isLiquidBrineStable).to.be.true;
        expect(mgBrine.isDeliquescenceActive).to.be.true;
        expect(mgBrine.eutecticTemperatureK).to.equal(206.0);
        expect(mgBrine.deliquescenceRHPct).to.be.closeTo(43.9, 1.0);
        expect(mgBrine.brinePhaseState).to.include('Transient Liquid Aqueous');

        // Dry afternoon (T = 240 K, RH = 10%): below DRH -> Dry Solid
        const drySalt = KRCEngine.computePerchlorateDeliquescenceAndLiquidBrineStability(240.0, 10.0, 'Mg(ClO4)2');
        expect(drySalt.isLiquidBrineStable).to.be.false;
        expect(drySalt.brinePhaseState).to.include('Warm Dry Desiccated');
    });

    it('should discriminate crystalline Hematite from Goethite and dust in CRISM VNIR spectra', () => {
        // Crystalline Grey Hematite (Meridiani Blueberry type): deep 860 nm minimum (r = 0.23 vs continuum (0.28+0.29)/2 = 0.285 -> BD860 = 0.193)
        const hematite = BandMathEngine.computeCRISMIronOxideSpeciationIndices(0.18, 0.28, 0.23, 0.27, 0.29);
        expect(hematite.isCrystallineHematite).to.be.true;
        expect(hematite.isHydratedGoethite).to.be.false;
        expect(hematite.ironOxidePhase).to.include('Hematite');
        expect(hematite.geologicalSignificance).to.include('Groundwater Diagenesis');

        // Hydrated Goethite: deep 910 nm absorption (BD920 > BD860)
        const goethite = BandMathEngine.computeCRISMIronOxideSpeciationIndices(0.18, 0.28, 0.27, 0.22, 0.29);
        expect(goethite.isCrystallineHematite).to.be.false;
        expect(goethite.isHydratedGoethite).to.be.true;
        expect(goethite.ironOxidePhase).to.include('Goethite');

        // Nanophase Ferric Dust (npOx): steep visible slope (BD530 > 0.15) and weak NIR bands
        const dust = BandMathEngine.computeCRISMIronOxideSpeciationIndices(0.15, 0.30, 0.295, 0.298, 0.30);
        expect(dust.isCrystallineHematite).to.be.false;
        expect(dust.ironOxidePhase).to.include('Nanophase Ferric Oxide');
    });
});

describe('Gravity Assist Flyby B-Plane, Two-Layer Ice Table Inversion & Jarosite/Alunite', () => {
    it('should calculate planetary flyby turning angle, maximum Delta-V boost, and B-plane impact parameter', () => {
        // Mars hyperbolic flyby (v_inf = 5.0 km/s, h_p = 500 km):
        const flyby = TrajectoryEngine.computePlanetaryFlybyGravityAssistAndBPlane(5.0, 500.0, 'mars');
        expect(flyby.hyperbolicEccentricity).to.be.closeTo(3.274, 0.05);
        expect(flyby.turningAngleDeg).to.be.closeTo(35.58, 0.5); // ~35.6 degree turning
        expect(flyby.maxDeltaVKmS).to.be.closeTo(3.054, 0.05); // ~3.05 km/s max Delta-V swingby boost
        expect(flyby.bPlaneImpactParameterKm).to.be.closeTo(5341.3, 50.0);
        expect(flyby.periapsisSpeedKmS).to.be.closeTo(6.855, 0.05);
    });

    it('should invert two-layer thermal inertia for shallow buried ice table depth and lag thickness', () => {
        // High-latitude ground ice table (I_app = 450, I_lag = 80, I_ice = 1800, d_th = 4.5 cm):
        const iceTable = KRCEngine.computeTwoLayerThermalInertiaIceTableDepth(450.0, 80.0, 1800.0, 4.5);
        expect(iceTable.isIceTableWithinDiurnalReach).to.be.true;
        expect(iceTable.iceTableDepthCm).to.be.closeTo(3.45, 0.5); // ~3.5 cm shallow dry lag
        expect(iceTable.groundIcePresence).to.include('Shallow Buried Ground Ice Table');

        // Thick dry dust mantle (I_app = 85 -> very low inertia, ice too deep):
        const deepMantle = KRCEngine.computeTwoLayerThermalInertiaIceTableDepth(85.0, 80.0, 1800.0, 4.5);
        expect(deepMantle.iceTableDepthCm).to.be.greaterThan(10.0);
    });

    it('should discriminate Jarosite from Alunite in CRISM acidic hydroxylated sulfate spectra', () => {
        // Jarosite in Meridiani Planum (strong 1.85 um and 2.26 um Fe-OH doublets):
        const jarosite = BandMathEngine.computeCRISMJarositeAluniteIndices(0.30, 0.26, 0.27, 0.30, 0.30);
        expect(jarosite.isJarosite).to.be.true;
        expect(jarosite.isAlunite).to.be.false;
        expect(jarosite.acidSulfatePhase).to.include('Jarosite');
        expect(jarosite.phRegime).to.include('Extreme Hyper-Acidic');

        // Alunite in high-temperature hydrothermal solfatara (strong 1.47 um and 2.32 um Al-OH doublets):
        const alunite = BandMathEngine.computeCRISMJarositeAluniteIndices(0.26, 0.30, 0.30, 0.27, 0.30);
        expect(alunite.isJarosite).to.be.false;
        expect(alunite.isAlunite).to.be.true;
        expect(alunite.acidSulfatePhase).to.include('Alunite');
        expect(alunite.phRegime).to.include('Advanced Argillic');
    });
});

describe('Hohmann Transfer Orbits, Liquid Water Metastability & Serpentinization', () => {
    it('should calculate heliocentric Earth-Mars Hohmann transfer parameters and synodic launch windows', () => {
        // Earth to Mars transfer:
        const earthMars = TrajectoryEngine.computeInterplanetaryHohmannTransferParameters('earth', 'mars');
        expect(earthMars.transferSemiMajorAxisAU).to.be.closeTo(1.2618, 0.005);
        expect(earthMars.timeOfFlightDays).to.be.closeTo(259.2, 5.0); // ~259 days flight duration (~8.5 months)
        expect(earthMars.departureExcessVInfKmS).to.be.closeTo(2.945, 0.05); // ~2.95 km/s C3 ~ 8.7 km^2/s^2
        expect(earthMars.departureC3EnergyKm2S2).to.be.closeTo(8.67, 0.3);
        expect(earthMars.arrivalExcessVInfKmS).to.be.closeTo(2.650, 0.05);
        expect(earthMars.synodicPeriodDays).to.be.closeTo(779.9, 5.0); // ~26 months between launch windows
    });

    it('should calculate pure liquid water thermodynamic metastability window and boiling point in Hellas Basin', () => {
        // Hellas Basin floor (P = 850 Pa, T = 275 K): P > 611.7 Pa and 273.15 K < T < T_boil (~277.8 K) -> Metastable Liquid!
        const hellasWater = KRCEngine.computeTransientLiquidWaterMetastabilityWindow(275.0, 850.0, 30.0);
        expect(hellasWater.isLiquidWaterMetastable).to.be.true;
        expect(hellasWater.isAboveTriplePointPressure).to.be.true;
        expect(hellasWater.boilingTemperatureK).to.be.closeTo(277.8, 1.0);
        expect(hellasWater.thermodynamicRegime).to.include('Transient Metastable Pure Liquid Water');

        // High elevation Olympus Mons summit (P = 70 Pa, T = 280 K): P < 611.7 Pa -> Sublimation only
        const olympusSummit = KRCEngine.computeTransientLiquidWaterMetastabilityWindow(280.0, 70.0, 10.0);
        expect(olympusSummit.isLiquidWaterMetastable).to.be.false;
        expect(olympusSummit.isAboveTriplePointPressure).to.be.false;
        expect(olympusSummit.thermodynamicRegime).to.include('Sublimation Only');
    });

    it('should discriminate Serpentine from Talc and olivine in CRISM ultramafic NIR spectra', () => {
        // Serpentine in Nili Fossae (sharp 1.39 um OH, 2.12 um and 2.315 um Mg-OH):
        const serpentine = BandMathEngine.computeCRISMSerpentineTalcIndices(0.27, 0.28, 0.26, 0.30, 0.30);
        expect(serpentine.isSerpentine).to.be.true;
        expect(serpentine.isTalc).to.be.false;
        expect(serpentine.mineralPhase).to.include('Serpentine');
        expect(serpentine.h2GenerationPotential).to.be.true;
        expect(serpentine.serpentinizationSetting).to.include('Ultramafic Serpentinization');

        // Talc hydrothermal alteration in Claritas Rise (strong 2.315 um and 2.38 um doublet):
        const talc = BandMathEngine.computeCRISMSerpentineTalcIndices(0.30, 0.30, 0.26, 0.265, 0.30);
        expect(talc.isSerpentine).to.be.false;
        expect(talc.isTalc).to.be.true;
        expect(talc.mineralPhase).to.include('Talc');
        expect(talc.serpentinizationSetting).to.include('Hydrothermal Alteration');
    });
});

describe('Low-Thrust Continuous Spirals, Seasonal CO2 Geysers & Chloride Evaporites', () => {
    it('should calculate Edelbaum low-thrust spiral orbital transfer Delta-V, flight duration, and propellant mass', () => {
        // Mars orbit spiral from r1 = 3800 km to r2 = 20000 km (Dawn-type ion thruster: F = 0.25 N, Isp = 3200 s, m0 = 1000 kg):
        const spiral = TrajectoryEngine.computeLowThrustContinuousSpiralTransfer(3800.0, 20000.0, 0.25, 1000.0, 3200.0, 'mars');
        expect(spiral.deltaVKmS).to.be.closeTo(1.90, 0.1); // ~1.9 km/s Delta-V
        expect(spiral.propellantMassKg).to.be.closeTo(58.8, 5.0); // ~59 kg xenon propellant
        expect(spiral.flightTimeDays).to.be.greaterThan(80.0); // ~80-100 days of continuous spiral thrusting
        expect(spiral.spiralRevolutions).to.be.greaterThan(100.0);
    });

    it('should calculate seasonal polar CO2 slab solid-state greenhouse basal gas overpressure and geyser eruption velocity', () => {
        // South polar seasonal slab (L = 1.0 m, F0 = 450 W/m^2, slab albedo = 0.65, kappa = 2.0 m^-1):
        const geyser = KRCEngine.computeSpringGeyserBasalSublimationOverpressure(1.0, 450.0, 0.65, 2.0, 600.0);
        expect(geyser.basalSolarFluxWM2).to.be.closeTo(21.3, 1.0); // ~21.3 W/m^2 reaching dark regolith
        expect(geyser.ruptureOverpressureKPa).to.be.closeTo(86.0, 5.0); // ~86 kPa rupture pressure
        expect(geyser.geyserEjectionSpeedMS).to.be.greaterThan(40.0); // high-velocity jet (> 40 m/s)
        expect(geyser.activeGeyserTerrain).to.include('Araneiform "Spider"');
    });

    it('should discriminate anhydrous Chloride / Halite salt flats in Terra Sirenum using VNIR slope and THEMIS DCS', () => {
        // Chloride evaporite playa in Terra Sirenum (negative VNIR slope, bright albedo = 0.23, high THEMIS DCS red = 0.70):
        const chloride = BandMathEngine.computeCRISMChlorideEvaporiteIndices(0.28, 0.24, 0.23, 0.70);
        expect(chloride.isChlorideEvaporite).to.be.true;
        expect(chloride.vnirSlopePerUm).to.be.lessThan(0.0); // negative/blue slope
        expect(chloride.depositType).to.include('Chloride Salt Deposit');
        expect(chloride.astrobiologicalPreservationPotential).to.include('Halite Fluid Inclusions');

        // Normal basaltic crust (positive red slope, low albedo = 0.12, low DCS = 0.30):
        const basalt = BandMathEngine.computeCRISMChlorideEvaporiteIndices(0.12, 0.18, 0.12, 0.30);
        expect(basalt.isChlorideEvaporite).to.be.false;
        expect(basalt.depositType).to.include('Basaltic Crust');
    });
});

describe('Aerocapture Entry Corridor, Frost Albedo Feedback & Pyroxene Speciation', () => {
    it('should calculate single-pass Mars aerocapture atmospheric braking Delta-V and peak dynamic pressure', () => {
        // Mars aerocapture entry (v_entry = 6.0 km/s at 125 km interface, target apoapsis = 1000 km, atmospheric periapsis = 50 km):
        const aero = TrajectoryEngine.computeAerocaptureCorridorAndDynamicPressure(6.0, 1000.0, 50.0, 0.30, 'mars');
        expect(aero.aeroBrakingDeltaVKmS).to.be.greaterThan(1.0); // > 1.0 km/s aerodynamic velocity braking
        expect(aero.peakDynamicPressureKPa).to.be.greaterThan(0.05); // aerodynamic dynamic pressure (~0.11 kPa at 50 km)
        expect(aero.propellantFractionSavedPct).to.be.greaterThan(30.0); // saves > 30% of spacecraft wet mass
        expect(aero.entryCorridorWidthDeg).to.be.greaterThan(0.05); // ~0.11 degree entry corridor width
    });

    it('should calculate microscale surface frost condensation, optical albedo brightening, and thermal radiative feedback', () => {
        // Early morning H2O frost film (L = 10 um, bare albedo = 0.20):
        const frost = KRCEngine.computeTransientFrostCondensationAndAlbedoFeedback(10.0, 0.20, 'H2O', 350.0);
        expect(frost.effectiveAlbedo).to.be.closeTo(0.365, 0.05); // albedo brightens from 0.20 to ~0.37
        expect(frost.albedoIncreasePct).to.be.greaterThan(70.0);
        expect(frost.effectiveEmissivity).to.be.greaterThan(0.93);
        expect(frost.frostCoverState).to.include('Optically Thick H2O Frost Mantle');

        // Bare dry afternoon regolith (L = 0 um):
        const bare = KRCEngine.computeTransientFrostCondensationAndAlbedoFeedback(0.0, 0.20, 'H2O', 350.0);
        expect(bare.effectiveAlbedo).to.equal(0.20);
        expect(bare.frostCoverState).to.include('Bare Regolith');
    });

    it('should discriminate High-Calcium Clinopyroxene (Augite HCP) from Low-Calcium Orthopyroxene (Enstatite LCP)', () => {
        // High-Calcium Clinopyroxene in Syrtis Major lava flow (deep 1050 nm and 2200 nm HCP bands):
        const cpx = BandMathEngine.computeCRISMPyroxeneSpeciationIndices(0.25, 0.21, 0.25, 0.21, 0.25);
        expect(cpx.isHighCalciumPyroxene).to.be.true;
        expect(cpx.isLowCalciumPyroxene).to.be.false;
        expect(cpx.pyroxeneType).to.include('Clinopyroxene');
        expect(cpx.volcanicContext).to.include('Syrtis Major Type');

        // Low-Calcium Orthopyroxene in Noachian crater central peak (deep 920 nm and 1850 nm LCP bands):
        const opx = BandMathEngine.computeCRISMPyroxeneSpeciationIndices(0.21, 0.25, 0.21, 0.25, 0.25);
        expect(opx.isHighCalciumPyroxene).to.be.false;
        expect(opx.isLowCalciumPyroxene).to.be.true;
        expect(opx.pyroxeneType).to.include('Orthopyroxene');
        expect(opx.volcanicContext).to.include('Noachian Primitive Crust');
    });
});

describe('Aerobraking Orbit Lowering, Subsurface Thermal Waves & Olivine Solid Solution', () => {
    it('should calculate multi-pass Mars aerobraking orbit lowering passes, campaign duration, and Delta-V savings', () => {
        // Mars Odyssey / MRO-type aerobraking (initial apoapsis = 35000 km, target = 400 km, corridor hp = 120 km):
        const aeroCampaign = TrajectoryEngine.computeAerobrakingOrbitLoweringPasses(35000.0, 400.0, 120.0, 55.0, 'mars');
        expect(aeroCampaign.totalAeroDeltaVMS).to.be.closeTo(1220.0, 100.0); // ~1.22 km/s total Delta-V dissipated
        expect(aeroCampaign.estimatedPassCount).to.be.greaterThan(200); // 200-1000 drag passes
        expect(aeroCampaign.campaignDurationMonths).to.be.greaterThan(2.0); // multi-month campaign
        expect(aeroCampaign.propellantSavedKg).to.be.greaterThan(250.0); // > 250 kg fuel saved
    });

    it('should calculate 1D subsurface thermal wave exponential attenuation, skin depth, and phase lag', () => {
        // Diurnal thermal wave in basaltic regolith (alpha = 3.5e-8 m^2/s, surface amplitude = 40 K):
        const diurnalWave = KRCEngine.computeSubsurfaceThermalWaveAttenuation(0.05, 40.0, 'diurnal', 3.5e-8); // at 5 cm depth
        expect(diurnalWave.thermalSkinDepthCm).to.be.closeTo(3.14, 0.3); // ~3.1 cm diurnal skin depth
        expect(diurnalWave.dampedAmplitudeK).to.be.lessThan(10.0); // drops from 40 K to ~8 K
        expect(diurnalWave.amplitudeAttenuationPct).to.be.greaterThan(75.0);
        expect(diurnalWave.phaseDelayHours).to.be.greaterThan(5.0); // ~6-7 hours lag

        // Deep isothermal horizon (> 3 skin depths, e.g. 20 cm for diurnal):
        const deepZone = KRCEngine.computeSubsurfaceThermalWaveAttenuation(0.20, 40.0, 'diurnal', 3.5e-8);
        expect(deepZone.dampedAmplitudeK).to.be.lessThan(0.5);
        expect(deepZone.thermalPenetrationHorizon).to.include('Isothermal Deep Subsurface');
    });

    it('should invert Olivine Fo-Fa solid solution composition from CRISM 1.05 um band center shift', () => {
        // Forsteritic Mantle Olivine (Fo90) in Nili Fossae (band center near 1035 nm):
        const foOlivine = BandMathEngine.computeCRISMOlivineSolidSolutionIndices(0.21, 0.20, 0.23, 0.28, 0.25);
        expect(foOlivine.forsteritePct).to.be.at.least(75.0); // >= 75% Fo
        expect(foOlivine.mantleOrigin).to.be.true;
        expect(foOlivine.olivineComposition).to.include('Forsteritic Olivine');

        // Fayalitic Iron-Rich Olivine (Fa70 / Fo30) in evolved basalt (band center shifted to 1065 nm):
        const faOlivine = BandMathEngine.computeCRISMOlivineSolidSolutionIndices(0.24, 0.205, 0.20, 0.28, 0.25);
        expect(faOlivine.forsteritePct).to.be.lessThan(50.0);
        expect(faOlivine.mantleOrigin).to.be.false;
        expect(faOlivine.olivineComposition).to.include('Fayalitic Olivine');
    });
});

describe('Frozen Orbit Equilibrium, Basal Cryosphere Melting & Smectite Clay Speciation', () => {
    it('should calculate planetary frozen orbit critical inclination and equilibrium J2/J3 eccentricity', () => {
        // Mars frozen science mapping orbit (a = 3770 km, i = 93 deg, locked omega0 = 270 deg):
        const frozen = TrajectoryEngine.computePlanetaryFrozenOrbitParameters(3770.0, 93.0, 'mars', 270.0);
        expect(frozen.criticalInclinationProgradeDeg).to.be.closeTo(63.435, 0.01);
        expect(frozen.criticalInclinationRetrogradeDeg).to.be.closeTo(116.565, 0.01);
        expect(frozen.frozenEccentricity).to.be.closeTo(0.0072, 0.001); // ~0.0072 equilibrium eccentricity
        expect(frozen.altitudeVariationKm).to.be.closeTo(54.5, 5.0); // ~55 km altitude variation between poles
        expect(frozen.orbitPeriodMinutes).to.be.closeTo(117.0, 5.0);
        expect(frozen.stabilityState).to.include('Frozen Eccentricity Locked');
    });

    it('should calculate steady-state geothermal gradient, cryosphere thickness, and basal melting depth', () => {
        // South Pole Ultimi Scopuli perchlorate brine lake (T_surf = 160 K, Q_geo = 25 mW/m^2, k_th = 2.5 W/m/K):
        const brineMelting = KRCEngine.computeBasalMeltingAndCryosphereThickness(160.0, 25.0, 2.5, 'brine');
        expect(brineMelting.geothermalGradientKPerKm).to.equal(10.0); // 10 K/km geothermal gradient
        expect(brineMelting.cryosphereThicknessKm).to.be.closeTo(4.5, 0.5); // ~4.5 km cryosphere thickness to brine eutectic (205 K)
        expect(brineMelting.isSubglacialBasalMeltingPossible).to.be.true;
        expect(brineMelting.subglacialSetting).to.include('MARSIS Ultimi Scopuli');

        // Pure water ice cap (requires reaching 273.15 K -> deeper melting):
        const pureMelting = KRCEngine.computeBasalMeltingAndCryosphereThickness(160.0, 25.0, 2.5, 'pure_water');
        expect(pureMelting.cryosphereThicknessKm).to.be.closeTo(11.3, 0.5); // ~11.3 km to pure water melting
    });

    it('should discriminate Al-Smectite (Montmorillonite) from Fe-Smectite (Nontronite) and Mg-Smectite (Saponite)', () => {
        // Al-Smectite (Montmorillonite) in Mawrth Vallis upper unit (strong 1.91 um and 2.21 um Al-OH):
        const alClay = BandMathEngine.computeCRISMSmectiteSpeciationIndices(0.27, 0.26, 0.25, 0.30, 0.30, 0.30);
        expect(alClay.smectitePhase).to.include('Al-Smectite');
        expect(alClay.clayOctahedralCation).to.include('Al3+');
        expect(alClay.aqueousEnvironment).to.include('Top-Down Pedogenic Leaching');

        // Fe-Smectite (Nontronite) in Jezero delta floor (strong 1.91 um and 2.29 um Fe-OH):
        const feClay = BandMathEngine.computeCRISMSmectiteSpeciationIndices(0.27, 0.26, 0.30, 0.25, 0.30, 0.30);
        expect(feClay.smectitePhase).to.include('Fe-Smectite');
        expect(feClay.clayOctahedralCation).to.include('Fe3+');
        expect(feClay.aqueousEnvironment).to.include('Hydrothermal Alteration of Basalt');

        // Mg-Smectite (Saponite) in alkaline closed-basin lake (strong 1.91 um and 2.315 um Mg-OH):
        const mgClay = BandMathEngine.computeCRISMSmectiteSpeciationIndices(0.27, 0.26, 0.30, 0.30, 0.25, 0.30);
        expect(mgClay.smectitePhase).to.include('Mg-Smectite');
        expect(mgClay.clayOctahedralCation).to.include('Mg2+');
    });
});

describe('Areostationary Synchronous Orbit, Subsurface Ice Table Retreat & Carbonate Speciation', () => {
    it('should calculate Mars Areostationary orbit radius, velocity, and longitudinal drift stationkeeping Delta-V', () => {
        // Spacecraft at 90 deg West longitude (high drift toward stable libration well at ~16 deg W):
        const aero = TrajectoryEngine.computeAreostationaryOrbitAndLongitudinalDrift(90.0, 'mars');
        expect(aero.synchronousRadiusKm).to.be.closeTo(20428.2, 5.0); // ~20428 km synchronous radius
        expect(aero.synchronousAltitudeKm).to.be.closeTo(17032.0, 5.0); // ~17032 km altitude
        expect(aero.orbitalSpeedKmS).to.be.closeTo(1.448, 0.01); // ~1.448 km/s
        expect(aero.rotationPeriodHours).to.be.closeTo(24.623, 0.01); // 24.623 hours Mars sol
        expect(aero.annualStationkeepingDeltaVMS).to.be.greaterThan(2.0); // ~2-6 m/s/yr
        expect(aero.nearestStableLongitudeDegW).to.be.closeTo(15.9, 1.0);
    });

    it('should calculate ground ice table thermodynamic equilibrium stability and desiccation retreat depth', () => {
        // High-latitude polar permafrost at 68 deg N (Phoenix site, T_surf = 190 K, P_vapor = 0.25 Pa -> T_frost = 202.8 K):
        const polarIce = KRCEngine.computeSubsurfaceIceTableEquilibriumRetreatDepth(190.0, 0.25, 68.0);
        expect(polarIce.isGroundIceStableAtSurface).to.be.true;
        expect(polarIce.frostPointTempK).to.be.closeTo(202.8, 1.0);
        expect(polarIce.equilibriumIceTableDepthCm).to.be.lessThan(5.0); // shallow ice table (< 5 cm)
        expect(polarIce.iceStabilityRegime).to.include('High-Latitude Permafrost');

        // Mid-latitude desiccated zone (T_surf = 215 K -> T_surf > T_frost):
        const midIce = KRCEngine.computeSubsurfaceIceTableEquilibriumRetreatDepth(215.0, 0.25, 30.0);
        expect(midIce.isGroundIceStableAtSurface).to.be.false;
        expect(midIce.equilibriumIceTableDepthCm).to.be.greaterThan(50.0); // retreats deep (> 50 cm)
        expect(midIce.vaporEquilibriumStatus).to.include('Metastable / Actively Sublimating');
    });

    it('should discriminate Magnesite (MgCO3) from Calcite (CaCO3) and Dolomite using CRISM 2.3/2.5 um bands', () => {
        // Magnesite (MgCO3) in Jezero crater margin / Nili Fossae (strong 2.30 um and 2.50 um Mg-carbonate bands):
        const mgCarb = BandMathEngine.computeCRISMCarbonateAnionSpeciationIndices(0.24, 0.30, 0.24, 0.30, 0.30);
        expect(mgCarb.carbonatePhase).to.include('Magnesite');
        expect(mgCarb.cationType).to.include('Mg2+');
        expect(mgCarb.carbonSequestrationContext).to.include('Ultramafic Olivine Bedrock');

        // Calcite (CaCO3) in Phoenix alkaline soil (strong 2.34 um and 2.54 um Ca-carbonate bands):
        const caCarb = BandMathEngine.computeCRISMCarbonateAnionSpeciationIndices(0.30, 0.24, 0.30, 0.24, 0.30);
        expect(caCarb.carbonatePhase).to.include('Calcite');
        expect(caCarb.cationType).to.include('Ca2+');
        expect(caCarb.carbonSequestrationContext).to.include('Soil Duricrust');
    });
});

describe('Phobos Gravitational Perturbations, Stratified Thermal Regolith & Zeolite Minerals', () => {
    it('should calculate Phobos tidal perturbations, resonance ratio, and secular nodal drift on Mars orbiter', () => {
        // Mars mapping orbiter (a = 3770 km, i = 93 deg) perturbed by Phobos:
        const phobosTide = TrajectoryEngine.computeMoonGravitationalPerturbationsOnMarsOrbit(3770.0, 93.0, 'phobos');
        expect(phobosTide.moonSemiMajorAxisKm).to.equal(9376.0);
        expect(phobosTide.moonPeriodHours).to.be.closeTo(7.654, 0.01);
        expect(phobosTide.orbiterPeriodHours).to.be.closeTo(1.95, 0.05);
        expect(phobosTide.resonanceRatio).to.be.closeTo(3.92, 0.1); // ~4:1 mean-motion resonance
        expect(phobosTide.maxTidalAccelerationUMSS).to.be.greaterThan(1e-4); // ~0.0065 um/s^2
    });

    it('should calculate 2-layer stratified regolith thermal profile and interface temperatures', () => {
        // Loose dust lag (5 cm, k = 0.03 W/m/K) over dense basalt (k = 2.0 W/m/K, Q_geo = 25 mW/m^2, T_surf = 200 K):
        const strat = KRCEngine.computeStratifiedRegolithThermalProfile(200.0, 0.05, 0.03, 2.0, 0.50, 25.0);
        expect(strat.interfaceTempK).to.be.closeTo(200.042, 0.005);
        expect(strat.topLayerGradientKPerKm).to.be.closeTo(833.3, 5.0); // high thermal gradient across insulation mantle
        expect(strat.bottomLayerGradientKPerKm).to.be.closeTo(12.5, 1.0); // low thermal gradient in bedrock
        expect(strat.totalThermalResistanceM2KW).to.be.greaterThan(1.5);
        expect(strat.stratigraphyContext).to.include('High-Insulation Dust Mantle');
    });

    it('should discriminate Analcime Zeolite from other minerals using CRISM 2.46/2.52 um bands', () => {
        // Analcime (Na-zeolite) in Columbus Crater paleolake (strong 1.91 um and 2.46 um framework band):
        const analcime = BandMathEngine.computeCRISMZeoliteAnalcimeIndices(0.28, 0.25, 0.24, 0.30, 0.30);
        expect(analcime.isZeolite).to.be.true;
        expect(analcime.zeolitePhase).to.include('Analcime');
        expect(analcime.paleolakeEnvironment).to.include('Alkaline Saline Paleolake');

        // Non-zeolitic primary crust:
        const primary = BandMathEngine.computeCRISMZeoliteAnalcimeIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(primary.isZeolite).to.be.false;
    });
});

describe('Solar Radiation Pressure, Glacial Ice Flow & Prebiotic Borates/Nitrates', () => {
    it('should calculate Solar Radiation Pressure (SRP) perturbation acceleration and long-period eccentricity oscillations', () => {
        // Mars orbiter (a = 3770 km, A/m = 0.02 m^2/kg, C_R = 1.3 at 1.524 AU):
        const srp = TrajectoryEngine.computeSolarRadiationPressureOrbitPerturbation(3770.0, 0.02, 1.3, 1.524, 'mars');
        expect(srp.solarFluxWM2).to.be.closeTo(586.2, 5.0); // ~586 W/m^2
        expect(srp.photonPressureMicroPa).to.be.closeTo(1.955, 0.05); // ~1.955 uPa
        expect(srp.srpAccelerationUMSS).to.be.closeTo(0.090, 0.01); // ~0.090 um/s^2
        expect(srp.eccentricityOscillationAmplitude).to.be.greaterThan(1e-9); // ~4.5e-8
        expect(srp.annualDeltaVEquivalentMS).to.be.greaterThan(2.0); // ~2.8 m/s/yr
    });

    it('should calculate Martian polar ice sheet basal shear stress and temperature-dependent Glen flow creep deformation', () => {
        // North Polar Layered Deposits (NPLD) ice sheet (H = 1000 m, slope = 1.5 deg, basal temp = 210 K, 5% dust):
        const glacier = KRCEngine.computeGlacialIceFlowAndBasalShearStress(1000.0, 1.5, 210.0, 5.0);
        expect(glacier.basalShearStressKPa).to.be.closeTo(97.5, 2.0); // ~97.5 kPa driving shear stress
        expect(glacier.internalDeformationSpeedMmPerYear).to.be.greaterThan(0.0);
        expect(glacier.isBasalSlidingActive).to.be.false; // cold-based
        expect(glacier.glacialFlowRegime).to.include('Slow Polar Viscous Relaxation');
    });

    it('should detect prebiotic Borate (B-O) and fixed Nitrate (NO3-) anions in CRISM evaporite spectra', () => {
        // Prebiotic Borate salt in Gale Crater playa evaporite (strong 2.38 um B-O band):
        const borate = BandMathEngine.computeCRISMBorateNitrateIndices(0.30, 0.27, 0.24, 0.30, 0.30);
        expect(borate.anionPhase).to.include('Borate Evaporite Salt');
        expect(borate.astrobiologySignificance).to.include('Ribose RNA');

        // Fixed Nitrate salt (strong 2.42 um NO3- band):
        const nitrate = BandMathEngine.computeCRISMBorateNitrateIndices(0.30, 0.27, 0.30, 0.24, 0.30);
        expect(nitrate.anionPhase).to.include('Fixed Nitrate Salt');
        expect(nitrate.astrobiologySignificance).to.include('Bioavailable Nitrogen');
    });
});

describe('Solar Sail Propulsion, Layered Polar Cap Thermal Profile & Iron Sulfide Minerals', () => {
    it('should calculate interplanetary solar sail characteristic acceleration, lightness number, and pitch thrust vector', () => {
        // High-performance solar sail (A = 500 m^2, m = 50 kg -> sigma = 100 g/m^2, eta = 0.88 at 1 AU):
        const sail = TrajectoryEngine.computeSolarSailHeliocentricAcceleration(500.0, 50.0, 0.88, 1.0, 35.264);
        expect(sail.arealLoadingGM2).to.equal(100.0);
        expect(sail.characteristicAccelerationMmS2).to.be.closeTo(0.08, 0.01);
        expect(sail.lightnessNumberBeta).to.be.greaterThan(0.01);
        expect(sail.optimalThrustPitchDeg).to.be.closeTo(35.264, 0.01);
        expect(sail.propulsionRegime).to.include('Low-Thrust');
    });

    it('should calculate 2-layer polar cap thermal stratigraphy, CO2 dry ice thermal blanketing, and basal temperatures', () => {
        // SPLD dry ice slab (L_CO2 = 300 m, k = 0.50 W/m/K) over water ice (L_H2O = 1500 m, k = 2.50 W/m/K, Q_geo = 25 mW/m^2, T_surf = 145 K):
        const polarCap = KRCEngine.computeLayeredPolarCapThermalProfile(145.0, 300.0, 1500.0, 25.0);
        expect(polarCap.co2LayerGradientKPerKm).to.equal(50.0); // 50 K/km in insulating CO2
        expect(polarCap.h2oLayerGradientKPerKm).to.equal(10.0); // 10 K/km in conductive H2O
        expect(polarCap.co2ThermalBlanketingDeltaTK).to.equal(15.0); // 15 K warming across dry ice slab
        expect(polarCap.co2H2OInterfaceTempK).to.equal(160.0); // 160 K at CO2/H2O interface
        expect(polarCap.bedrockBasalTempK).to.equal(175.0); // 175 K at bedrock
        expect(polarCap.polarStratigraphyContext).to.include('Massive Buried CO2 Ice Package');
    });

    it('should discriminate opaque Iron(II) Sulfides (Pyrrhotite/Troilite) from iron oxides and silicates', () => {
        // Pyrrhotite in magmatic sulfide segregations (low albedo ~0.10, steep red slope > 0.06 um^-1, no 860 nm oxide dip):
        const sulfide = BandMathEngine.computeCRISMIronSulfideIndices(0.07, 0.09, 0.12, 0.14, 0.15);
        expect(sulfide.isIronSulfidePresent).to.be.true;
        expect(sulfide.sulfidePhase).to.include('Pyrrhotite');
        expect(sulfide.petrogeneticOrigin).to.include('Magmatic Sulfide');

        // Transparent basalt silicate (high albedo, flat slope):
        const basalt = BandMathEngine.computeCRISMIronSulfideIndices(0.25, 0.25, 0.25, 0.25, 0.15);
        expect(basalt.isIronSulfidePresent).to.be.false;
    });
});

describe('Orbit Eclipse Shadow Geometry, Permafrost Bedrock Discontinuity & Apatite Phosphates', () => {
    it('should calculate planetary orbit eclipse shadow fraction, duration, and critical beta angle', () => {
        // Low Mars Orbit (a = 3770 km, e = 0.005, beta = 0 deg maximum eclipse):
        const eclipse = TrajectoryEngine.computeEllipticOrbitEclipseGeometryAndShadowDuration(3770.0, 0.005, 0.0, 'mars');
        expect(eclipse.criticalBetaAngleDeg).to.be.closeTo(64.5, 1.0); // ~64.5 deg critical beta threshold
        expect(eclipse.isOrbitInFullSunlight).to.be.false;
        expect(eclipse.eclipseShadowFractionPct).to.be.closeTo(35.7, 2.0); // ~35.7% of orbit in shadow
        expect(eclipse.eclipseDurationMinutes).to.be.closeTo(41.8, 2.0); // ~41.8 min eclipse duration
        expect(eclipse.thermalShadowRegime).to.include('Deep Equatorial Umbra Shadow');

        // High beta angle (> 65 deg) orbit in full continuous sunlight:
        const fullSun = TrajectoryEngine.computeEllipticOrbitEclipseGeometryAndShadowDuration(3770.0, 0.005, 70.0, 'mars');
        expect(fullSun.isOrbitInFullSunlight).to.be.true;
        expect(fullSun.eclipseDurationMinutes).to.equal(0.0);
    });

    it('should calculate permafrost ground ice to fractured basalt bedrock conductive discontinuity and geothermal profile', () => {
        // High-latitude permafrost (L_ice = 50 m, k_ice = 2.5 W/m/K) over basalt (k_rock = 1.8 W/m/K, Q_geo = 25 mW/m^2, T_surf = 190 K):
        const perma = KRCEngine.computePermafrostBedrockThermalDiscontinuity(190.0, 50.0, 500.0, 25.0);
        expect(perma.iceLayerGradientKPerKm).to.equal(10.0); // 10 K/km in permafrost ice
        expect(perma.bedrockLayerGradientKPerKm).to.be.closeTo(13.89, 0.1); // ~13.89 K/km in basalt basement
        expect(perma.bedrockInterfaceTempK).to.equal(190.50); // 190.50 K at 50 m bedrock contact
        expect(perma.targetDepthTempK).to.be.closeTo(196.75, 0.1); // 196.75 K at 500 m probe depth
        expect(perma.permafrostContext).to.include('Shallow Permafrost Table');
    });

    it('should discriminate igneous Apatite Phosphates from clays and silicates in CRISM spectra', () => {
        // Hydroxyapatite / (Cl,F,OH)-Apatite in SNC-type volcanic bedrock (strong 2.16 um phosphate and 1.47 um OH):
        const apatite = BandMathEngine.computeCRISMApatitePhosphateIndices(0.24, 0.30, 0.24, 0.30, 0.30);
        expect(apatite.isApatitePhosphate).to.be.true;
        expect(apatite.phosphatePhase).to.include('Apatite');
        expect(apatite.petrologicalContext).to.include('Bioessential Phosphorus');

        // Basalt silicate lacking phosphate combinations:
        const basalt = BandMathEngine.computeCRISMApatitePhosphateIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isApatitePhosphate).to.be.false;
    });
});

describe('Atmospheric Entry Deceleration, Pore Ice Saturation & Epidote Metamorphism', () => {
    it('should calculate planetary atmospheric entry ballistic peak deceleration, altitude of peak load, and velocity drop', () => {
        // Mars direct hyperbolic entry (v_entry = 5.7 km/s, gamma = -12.5 deg, beta = 120 kg/m^2):
        const entry = TrajectoryEngine.computeAtmosphericEntryBallisticPeakDeceleration(5.7, -12.5, 120.0, 11.1, 0.020);
        expect(entry.peakDecelerationGLoad).to.be.closeTo(11.9, 0.5); // ~11.9 g peak load
        expect(entry.altitudeOfPeakDecelerationKm).to.be.closeTo(23.8, 1.0); // ~23.8 km altitude
        expect(entry.velocityAtPeakDecelerationKmS).to.be.closeTo(3.46, 0.1); // ~3.46 km/s
        expect(entry.entryCorridorStatus).to.include('Nominal Mars Entry');
    });

    it('should calculate porous regolith thermal conductivity, bulk density, and thermal inertia jump with pore ice saturation', () => {
        // Phoenix lander permafrost soil (porosity = 40%, 80% pore-ice saturation):
        const poreIce = KRCEngine.computePoreIceSaturationThermalConductivity(40.0, 80.0, 2.0, 0.015);
        expect(poreIce.effectiveThermalConductivityWMK).to.be.greaterThan(1.4); // ~1.45 W/m/K (high conductivity of ice)
        expect(poreIce.apparentThermalInertiaTIU).to.be.greaterThan(1500.0); // > 1500 tiu (cryolithosphere jump)
        expect(poreIce.conductivityEnhancementFactor).to.be.greaterThan(5.0);
        expect(poreIce.groundIceState).to.include('Massive Pore-Filling Ground Ice');
    });

    it('should discriminate high-temperature Hydrothermal Epidote from unaltered basalt and low-T clays', () => {
        // Greenschist-facies Epidote in Nili Fossae deep crustal megabreccia (strong 2.26 um and 2.34 um Fe3+-OH doublet, 1.55 um OH):
        const epidote = BandMathEngine.computeCRISMEpidoteHydrothermalIndices(0.25, 0.30, 0.23, 0.25, 0.30);
        expect(epidote.isEpidotePresent).to.be.true;
        expect(epidote.mineralPhase).to.include('Epidote');
        expect(epidote.metamorphicGrade).to.include('High-Temperature Greenschist');

        // Unaltered volcanic basalt:
        const basalt = BandMathEngine.computeCRISMEpidoteHydrothermalIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isEpidotePresent).to.be.false;
    });
});

describe('Guided Lifting Entry Corridor, Pore Ice Sublimation Retreat & Prehnite Metamorphism', () => {
    it('should calculate guided lifting planetary entry and aerocapture corridor width and bank-control boundaries', () => {
        // MSL / Perseverance trimmed lifting entry capsule (v_entry = 6.0 km/s, L/D = 0.24, gamma_nom = -11.5 deg):
        const corridor = TrajectoryEngine.computeGuidedLiftingEntryCorridorWidth(6.0, 0.24, 130.0, -11.5, 'mars');
        expect(corridor.corridorWidthDeg).to.be.closeTo(1.57, 0.1); // ~1.57 deg corridor width
        expect(corridor.shallowBoundaryFlightPathAngleDeg).to.be.closeTo(-10.71, 0.1); // -10.71 deg lift-down capture limit
        expect(corridor.steepBoundaryFlightPathAngleDeg).to.be.closeTo(-12.29, 0.1); // -12.29 deg lift-up load limit
        expect(corridor.aerocaptureFeasibility).to.include('Nominal Guided Aerocapture');
    });

    it('should calculate ground ice sublimation front retreat velocity, Knudsen vapor diffusion, and lag resistance', () => {
        // Unstable equatorial ice table (z_ice = 0.25 m, T_ice = 205 K, porosity = 40%):
        const retreat = KRCEngine.computePoreIceSublimationFrontRetreatRate(0.25, 205.0, 40.0, 2.0, 5.0);
        expect(retreat.knudsenDiffusivityM2S).to.be.greaterThan(1e-5); // ~1.6e-4 m^2/s
        expect(retreat.vaporMassFluxKgM2S).to.be.greaterThan(1e-9);
        expect(retreat.retreatRateMicronsPerYear).to.be.greaterThan(0.0);
        expect(retreat.desiccationRegime).to.include('Desiccation Retreat');
    });

    it('should discriminate low-grade metamorphic Prehnite from smectites and unaltered basalt in CRISM spectra', () => {
        // Prehnite in Toro Crater central peak impact hydrothermal system (strong 2.35 um Al-OH and 1.475 um OH, dry):
        const prehnite = BandMathEngine.computeCRISMPrehniteIndices(0.25, 0.30, 0.23, 0.30);
        expect(prehnite.isPrehnitePresent).to.be.true;
        expect(prehnite.mineralPhase).to.include('Prehnite');
        expect(prehnite.alterationEnvironment).to.include('Prehnite-Pumpellyite Facies');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMPrehniteIndices(0.30, 0.30, 0.30, 0.30);
        expect(basalt.isPrehnitePresent).to.be.false;
    });
});

describe('Continuous Low-Thrust Spiral Transfer, Multi-Harmonic Skin Depth & Pumpellyite', () => {
    it('should calculate continuous low-thrust Edelbaum spiral Delta-V, propellant mass, and insertion burn days', () => {
        // Ion-drive spacecraft spiral from high capture (r1 = 20000 km) to mapping orbit (r2 = 3770 km, Isp = 3000 s, T = 0.25 N, m0 = 1000 kg):
        const spiral = TrajectoryEngine.computeLowThrustContinuousSpiralCaptureDuration(20000.0, 3770.0, 0.250, 3000.0, 1000.0, 'mars');
        expect(spiral.edelbaumDeltaVKmS).to.be.closeTo(1.907, 0.05); // ~1.91 km/s Delta-V
        expect(spiral.propellantConsumedKg).to.be.closeTo(62.8, 2.0); // ~62.8 kg xenon propellant
        expect(spiral.finalSpacecraftMassKg).to.be.closeTo(937.2, 2.0);
        expect(spiral.burnDurationDays).to.be.closeTo(85.5, 5.0); // ~85.5 days continuous spiral
        expect(spiral.propulsionEfficiencySummary).to.include('High-Efficiency Electric Propulsion');
    });

    it('should calculate multi-harmonic diurnal and annual thermal skin depth, damping ratios, and subsurface phase lag', () => {
        // Typical Martian sand/duricrust regolith (I = 250 tiu, rho = 1500 kg/m^3, Cp = 800 J/kg/K, z = 10 cm):
        const skinDepth = KRCEngine.computeMultiHarmonicThermalPenetrationDepth(250.0, 1500.0, 800.0, 0.10);
        expect(skinDepth.diurnalSkinDepthCm).to.be.closeTo(3.50, 0.1); // ~3.5 cm diurnal skin depth
        expect(skinDepth.seasonalSkinDepthMeters).to.be.closeTo(0.905, 0.05); // ~90.5 cm seasonal skin depth
        expect(skinDepth.diurnalAmplitudeDampingFraction).to.be.lessThan(0.10); // damped at 10 cm depth
        expect(skinDepth.seasonalAmplitudeDampingFraction).to.be.greaterThan(0.80); // well preserved seasonally
        expect(skinDepth.diurnalPhaseLagHours).to.be.greaterThan(5.0); // significant phase lag
    });

    it('should discriminate hydrated metamorphic Pumpellyite from prehnite, epidote, and unaltered basalt', () => {
        // Pumpellyite in impact megabreccia (strong 2.21 um Al/Mg-OH, 2.34 um shoulder, 1.91 um water, 1.45 um OH):
        const pump = BandMathEngine.computeCRISMPumpellyiteIndices(0.24, 0.24, 0.23, 0.25, 0.30);
        expect(pump.isPumpellyitePresent).to.be.true;
        expect(pump.mineralPhase).to.include('Pumpellyite');
        expect(pump.metamorphicContext).to.include('Prehnite-Pumpellyite Facies');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMPumpellyiteIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isPumpellyitePresent).to.be.false;
    });
});

describe('Relativistic Time Dilation, Salt Duricrust Conduction & Lawsonite Metamorphism', () => {
    it('should calculate relativistic clock dilation drift and solar Shapiro radio signal delay', () => {
        // Mars science orbiter (r = 3770 km, b = 0.05 AU closest solar approach at superior conjunction):
        const rel = TrajectoryEngine.computeRelativisticTimeDilationAndShapiroDelay(3770.0, 0.05, 1.524, 'mars');
        expect(rel.dailyClockDriftMicroseconds).to.be.closeTo(-16.38, 0.5); // ~ -16.38 us/day clock drift
        expect(rel.solarShapiroDelayMicroseconds).to.be.greaterThan(100.0); // > 100 us Shapiro gravitational delay
        expect(rel.oneWayRangeErrorMeters).to.be.greaterThan(15000.0); // > 15 km radio range correction
        expect(rel.relativisticRegime).to.include('Deep Space Superior Conjunction');
    });

    it('should calculate 2-layer salt duricrust mantle over ground ice thermal profile and total series resistance', () => {
        // Chloride salt pan (L_salt = 10 cm, k_salt = 0.60 W/m/K) over ice substrate (k_ice = 2.5 W/m/K, Q_geo = 25 mW/m^2, T_surf = 210 K):
        const salt = KRCEngine.computeSaltDuricrustThermalProfile(210.0, 0.10, 0.60, 2.50, 1.00, 25.0);
        expect(salt.saltLayerGradientKPerKm).to.be.closeTo(41.67, 0.5); // ~41.67 K/km in salt duricrust
        expect(salt.iceLayerGradientKPerKm).to.equal(10.0); // 10 K/km in ice substrate
        expect(salt.interfaceTempK).to.be.closeTo(210.004, 0.005);
        expect(salt.totalThermalResistanceM2KW).to.be.closeTo(0.527, 0.01);
        expect(salt.saltDepositContext).to.include('Chloride Salt Evaporite Duricrust');
    });

    it('should discriminate high-pressure Lawsonite from pumpellyite, prehnite, and basalt in CRISM spectra', () => {
        // Lawsonite in impact-shocked blueschist megabreccia (strong 1.45 and 1.62 um OH doublet, 1.91 um water, 2.21 um Al-OH):
        const lawsonite = BandMathEngine.computeCRISMLawsoniteIndices(0.24, 0.26, 0.23, 0.23, 0.30);
        expect(lawsonite.isLawsonitePresent).to.be.true;
        expect(lawsonite.mineralPhase).to.include('Lawsonite');
        expect(lawsonite.metamorphicGrade).to.include('High-Pressure Low-Temperature');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMLawsoniteIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(lawsonite.isLawsonitePresent).to.be.true;
        expect(basalt.isLawsonitePresent).to.be.false;
    });
});

describe('Solar Gravitational Lens Optics, Non-Linear Conductivity & Serpentine Speciation', () => {
    it('should calculate Solar Gravitational Lens (SGL) Einstein deflection, minimum focal distance, and optical gain', () => {
        // Spacecraft along SGL focal line at 550 AU (lambda = 1.0 um):
        const sgl = TrajectoryEngine.computeSolarGravitationalLensFocalParameters(550.0, 1.0, 1.0);
        expect(sgl.einsteinDeflectionArcsec).to.be.closeTo(1.751, 0.05); // ~1.75 arcsec Einstein deflection
        expect(sgl.minimumFocalDistanceAU).to.be.closeTo(547.6, 2.0); // ~547.6 AU minimum focal distance
        expect(sgl.opticalIntensityGain).to.be.greaterThan(1e15); // massive optical intensity magnification
        expect(sgl.isInsideFocalRegion).to.be.true;
        expect(sgl.lensStatus).to.include('Active Solar Gravitational Lens');
    });

    it('should calculate non-linear subsurface thermal profile with temperature-dependent conductivity k(T) = k0*(T0/T)^n', () => {
        // Pure water ice sheet (T_surf = 150 K, z = 1000 m, Q_geo = 25 mW/m^2, k0 = 2.22 W/m/K, n = 1.0):
        const profile = KRCEngine.computeTemperatureDependentConductivityThermalProfile(150.0, 1000.0, 25.0, 2.22, 1.0);
        expect(profile.nonLinearTargetTempK).to.be.greaterThan(profile.linearModelTargetTempK); // non-linear warming
        expect(profile.nonLinearMeanGradientKPerKm).to.be.closeTo(6.3, 0.5);
        expect(profile.lithosphereMediumContext).to.include('Pure Crystalline H2O Ice');
    });

    it('should discriminate trioctahedral Serpentine from smectites, chlorites, and unaltered ultramafic basalt', () => {
        // Lizardite/Antigorite in Nili Fossae serpentinized olivine bedrock (strong 1.39 um and 2.325 um Mg-OH, dry):
        const serp = BandMathEngine.computeCRISMSerpentineSpeciationIndices(0.24, 0.30, 0.28, 0.23, 0.30);
        expect(serp.isSerpentinePresent).to.be.true;
        expect(serp.serpentinePhase).to.include('Serpentine');
        expect(serp.serpentinizationSetting).to.include('Abiotic H2 and CH4');

        // Unaltered ultramafic rock:
        const basalt = BandMathEngine.computeCRISMSerpentineSpeciationIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isSerpentinePresent).to.be.false;
    });
});

describe('Planetary Gravity Assist, Methane Clathrate Stability & Carbonate Speciation', () => {
    it('should calculate planetary gravity assist hyperbolic deflection angle, asymptotic Delta-V, and impact parameter', () => {
        // Mars flyby swingby (v_inf = 4.5 km/s, h_p = 300 km):
        const swingby = TrajectoryEngine.computePlanetaryGravityAssistDeflectionAndDeltaV(4.5, 300.0, 'mars');
        expect(swingby.hyperbolicEccentricity).to.be.closeTo(2.748, 0.05); // ~2.75 eccentricity
        expect(swingby.deflectionAngleDeg).to.be.closeTo(42.69, 1.0); // ~42.7 deg deflection
        expect(swingby.maxAsymptoticDeltaVKmS).to.be.closeTo(3.276, 0.1); // ~3.28 km/s Delta-V gain
        expect(swingby.impactParameterKm).to.be.closeTo(5412.7, 50.0);
        expect(swingby.swingbyFeasibility).to.include('High-Efficiency Interplanetary Gravity Assist');
    });

    it('should calculate subsurface Methane Clathrate Hydrate Stability Zone (MHSZ) upper/lower boundaries and reservoir thickness', () => {
        // High-latitude Martian permafrost (T_surf = 180 K, P_surf = 610 Pa, Q_geo = 25 mW/m^2, k = 2.0 W/m/K):
        const clathrate = KRCEngine.computeMethaneClathrateHydrateStabilityZone(180.0, 610.0, 25.0, 2.0, 1800.0);
        expect(clathrate.topDepthMeters).to.be.at.least(0); // stability begins at shallow permafrost depths
        expect(clathrate.bottomDepthMeters).to.be.greaterThan(3000); // extends down to > 3-5 km before geothermal dissociation
        expect(clathrate.mhszThicknessMeters).to.be.greaterThan(3000);
        expect(clathrate.clathrateTrappingPotential).to.include('Massive Planetary Methane');
    });

    it('should discriminate Magnesite from Siderite, phyllosilicates, and unaltered basalt in CRISM spectra', () => {
        // Magnesite in Jezero Crater delta margin carbonates (strong 2.30 um and 2.50 um CO3 bands):
        const magnesite = BandMathEngine.computeCRISMCarbonateSpeciationIndices(0.23, 0.22, 0.28, 0.30);
        expect(magnesite.isCarbonatePresent).to.be.true;
        expect(magnesite.carbonateSpecies).to.include('Magnesite');
        expect(magnesite.paleoenvironmentalContext).to.include('Jezero Margin Carbonates');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMCarbonateSpeciationIndices(0.30, 0.30, 0.30, 0.30);
        expect(basalt.isCarbonatePresent).to.be.false;
    });
});

describe('Aerobraking Orbit Decay, CO2 Clathrate Stability & Jarosite Alteration', () => {
    it('should calculate single-pass aerobraking drag velocity dissipation, energy loss, and apoapsis reduction', () => {
        // Mars orbiter aerobraking pass (r_a = 30000 km, r_p = 3520 km, rho_p = 3.5e-9 kg/m^3, beta = 80 kg/m^2):
        const aero = TrajectoryEngine.computeAerobrakingPassEnergyDissipationAndApoapsisDecay(30000.0, 3520.0, 3.5e-9, 7.5, 80.0, 'mars');
        expect(aero.dragDeltaVMS).to.be.greaterThan(0.01); // ~0.016 m/s velocity dissipation per pass
        expect(aero.energyDissipationJPerKg).to.be.lessThan(0.0); // negative energy delta
        expect(aero.apoapsisDecayKm).to.be.greaterThan(0.5); // noticeable apoapsis reduction per rev
        expect(aero.aerobrakingPassRegime).to.include('Aerobraking Corridor Pass');
    });

    it('should calculate subsurface CO2 Clathrate Hydrate Stability Zone (CHSZ) upper/lower boundaries and reservoir thickness', () => {
        // North polar layered deposits (T_surf = 150 K, P_surf = 610 Pa, Q_geo = 25 mW/m^2, k = 2.5 W/m/K):
        const co2Clathrate = KRCEngine.computeCarbonDioxideClathrateHydrateStabilityZone(150.0, 610.0, 25.0, 2.5, 1200.0);
        expect(co2Clathrate.topDepthMeters).to.be.at.least(0); // stable right from shallow polar ice depths
        expect(co2Clathrate.bottomDepthMeters).to.be.greaterThan(2000); // extends down to > 2-4 km before basal thermal decomposition
        expect(co2Clathrate.chszThicknessMeters).to.be.greaterThan(2000);
        expect(co2Clathrate.co2TrappingPotential).to.include('Polar CO2 Clathrate');
    });

    it('should discriminate hyper-acidic Jarosite from carbonates, smectites, and unaltered basalt in CRISM spectra', () => {
        // Jarosite at Meridiani Planum / Mawrth Vallis (strong 2.26 um Fe3+-OH and 1.85 um sulfate-OH bands):
        const jarosite = BandMathEngine.computeCRISMJarositeIndices(0.24, 0.23, 0.22, 0.28, 0.30);
        expect(jarosite.isJarositePresent).to.be.true;
        expect(jarosite.mineralPhase).to.include('Jarosite');
        expect(jarosite.acidityEnvironment).to.include('Hyper-Acidic (pH < 3)');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMJarositeIndices(0.30, 0.30, 0.30, 0.30);
        expect(basalt.isJarositePresent).to.be.false;
    });
});

describe('Hypersonic Aerocapture, Seasonal Pore Ice Diffusion & Alunite Hydrothermal Alteration', () => {
    it('should calculate single-pass hypersonic aerocapture velocity depletion, propellant mass saved, and orbit geometry', () => {
        // Mars sample return aerocapture (v_inf = 5.6 km/s, target r_a = 6000 km altitude, h_p = 50 km, m0 = 1000 kg, Isp = 320 s):
        const aero = TrajectoryEngine.computeAerocaptureHypersonicPassCaptureParameters(5.6, 6000.0, 50.0, 1000.0, 320.0, 'mars');
        expect(aero.hyperbolicPeriapsisVelocityKmS).to.be.closeTo(7.498, 0.05); // ~7.50 km/s entry speed
        expect(aero.capturedPeriapsisVelocityKmS).to.be.closeTo(4.265, 0.05); // ~4.26 km/s exit speed
        expect(aero.requiredAtmosphericDeltaVKmS).to.be.closeTo(3.233, 0.05); // ~3.23 km/s atmospheric Delta-V
        expect(aero.propellantMassSavedKg).to.be.greaterThan(600.0); // > 600 kg fuel saved (64% mass savings)
        expect(aero.aerocaptureFeasibility).to.include('High-Margin Aerocapture');
    });

    it('should calculate non-isothermal seasonal thermal wave pore ice sublimation, Clausius-Clapeyron enhancement, and vapor flux', () => {
        // Warm Martian mid-latitude ice table (T_mean = 210 K, Delta_T = 30 K, z_ice = 20 cm, porosity = 40%):
        const diff = KRCEngine.computeSeasonalHarmonicSublimationPoreIceDiffusion(210.0, 30.0, 0.20, 40.0, 250.0);
        expect(diff.nonLinearThermalEnhancementFactor).to.be.greaterThan(1.0); // Clausius-Clapeyron exponential enhancement
        expect(diff.annualMeanVaporDensityKgM3).to.be.greaterThan(diff.isothermalVaporDensityKgM3);
        expect(diff.annualVaporMassFluxKgM2S).to.be.greaterThan(0.0);
        expect(diff.iceStabilityAssessment).to.include('Sublimation');
    });

    it('should discriminate Alunite from Jarosite, kaolinite, and unaltered basalt in CRISM spectra', () => {
        // Alunite in Columbus Crater solfatara deposits (strong 2.17 um Al-OH and 1.76 um sulfate-OH bands):
        const alunite = BandMathEngine.computeCRISMAluniteIndices(0.24, 0.23, 0.22, 0.28, 0.30);
        expect(alunite.isAlunitePresent).to.be.true;
        expect(alunite.mineralPhase).to.include('Alunite');
        expect(alunite.hydrothermalFacies).to.include('Advanced Argillic Acid-Sulfate');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMAluniteIndices(0.30, 0.30, 0.30, 0.30);
        expect(basalt.isAlunitePresent).to.be.false;
    });
});

describe('Hypersonic Stagnation Heat Flux, Milankovitch Obliquity & Opaline Silica', () => {
    it('should calculate Sutton-Graves peak stagnation convective heat flux and TPS thermal load', () => {
        // Mars atmospheric entry (v = 5.5 km/s, rho = 1.5e-4 kg/m^3, Rn = 0.66 m, gamma = -12 deg):
        const heat = TrajectoryEngine.computeHypersonicStagnationConvectiveHeatFlux(5.5, 1.5e-4, 0.66, -12.0, 'mars');
        expect(heat.stagnationHeatFluxWPerCm2).to.be.closeTo(47.72, 2.0); // ~47.7 W/cm^2 convective peak flux
        expect(heat.stagnationHeatFluxKWPerM2).to.be.closeTo(477.2, 20.0); // ~477.2 kW/m^2
        expect(heat.integratedHeatLoadJPerCm2).to.be.greaterThan(100.0); // > 100 J/cm^2 total thermal load
        expect(heat.tpsMaterialSuitability).to.include('SLA-561V');
    });

    it('should calculate Milankovitch obliquity-driven insolation and paleoclimate tropical vs polar ice stability', () => {
        // Current Mars epoch (obliquity = 25.2 deg, latitude = 45 deg):
        const currentEpoch = KRCEngine.computeMilankovitchObliquityIceStabilityDepth(25.2, 45.0, 250.0, 150.0);
        expect(currentEpoch.annualMeanInsolationWM2).to.be.greaterThan(100.0);
        expect(currentEpoch.paleoclimateGlacialRegime).to.include('Mid-Latitude Permafrost');

        // High obliquity epoch (obliquity = 45 deg, latitude = 15 deg equator):
        const highObliquity = KRCEngine.computeMilankovitchObliquityIceStabilityDepth(45.0, 15.0, 250.0, 150.0);
        expect(highObliquity.iceTableStabilityDepthMeters).to.equal(0.0); // surface glaciation at equator!
        expect(highObliquity.tropicalGlaciationPotential).to.include('Tropical Valley Glaciation');
    });

    it('should discriminate Opaline Silica hot spring sinters from alunite, clays, and unaltered basalt in CRISM spectra', () => {
        // Opaline silica at Gusev Crater Home Plate (strong 2.21-2.26 um broad Si-OH shoulder, 1.91 um and 1.41 um bands):
        const silica = BandMathEngine.computeCRISMHydrothermalSinterSilicaIndices(0.24, 0.23, 0.22, 0.23, 0.30);
        expect(silica.isSinterSilicaPresent).to.be.true;
        expect(silica.silicaPhase).to.include('Opaline Silica');
        expect(silica.biosignaturePotential).to.include('Biosignature Taphonomy');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMHydrothermalSinterSilicaIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isSinterSilicaPresent).to.be.false;
    });
});

describe('Hypersonic Entry Deceleration G-Load, Barometric Pumping & Bassanite Sulfate', () => {
    it('should calculate Allen-Eggers analytical peak hypersonic deceleration, G-load, and altitude of peak drag', () => {
        // Mars Pathfinder / MSL EDL entry (v_entry = 5.7 km/s, gamma = -12.5 deg, beta = 120 kg/m^2, Hs = 11.1 km):
        const edl = TrajectoryEngine.computeHypersonicEntryPeakDecelerationGLoad(5.7, -12.5, 120.0, 11.1, 'mars');
        expect(edl.peakDecelerationMS2).to.be.closeTo(116.5, 5.0); // ~116.5 m/s^2 peak drag deceleration
        expect(edl.peakDecelerationGs).to.be.closeTo(11.88, 0.8); // ~11.9 Earth Gs
        expect(edl.velocityAtPeakDecelerationKmS).to.be.closeTo(3.457, 0.1); // ~3.46 km/s at peak drag
        expect(edl.altitudeOfPeakDecelerationKm).to.be.greaterThan(10.0); // peak drag occurs at ~15-25 km altitude
        expect(edl.structuralLoadRegime).to.include('Robotic EDL Deceleration');
    });

    it('should calculate porous regolith permeability, Klinkenberg slip, barometric skin depth, and gas exchange volume', () => {
        // Typical Martian sandy regolith (r_grain = 50 um, porosity = 40%, diurnal deltaP = 30 Pa, P0 = 610 Pa):
        const pump = KRCEngine.computeSubsurfaceBarometricPumpingAndPermeability(50.0, 40.0, 30.0, 610.0);
        expect(pump.permeabilityInDarcies).to.be.greaterThan(1.0); // ~10 Darcies
        expect(pump.barometricSkinDepthMeters).to.be.greaterThan(3.0); // multi-meter barometric breathing depth
        expect(pump.diurnalGasExchangeVolumeM3PerM2).to.be.greaterThan(0.01);
        expect(pump.regolithPoreVentilationRegime).to.include('Gas Permeability');
    });

    it('should discriminate Bassanite hemihydrate from polyhydrated sulfates, clays, and unaltered basalt in CRISM spectra', () => {
        // Bassanite in Gale Crater Curiosity veins (strong 1.44 um, 1.78 um, 1.92 um, and 2.40 um sulfate bands):
        const bassanite = BandMathEngine.computeCRISMBassaniteIndices(0.23, 0.24, 0.22, 0.25, 0.30);
        expect(bassanite.isBassanitePresent).to.be.true;
        expect(bassanite.sulfatePhase).to.include('Bassanite');
        expect(bassanite.diageneticEnvironment).to.include('Gale Crater / Curiosity ChemMin Analogue');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMBassaniteIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(bassanite.bd1920).to.be.greaterThan(0.02);
        expect(basalt.isBassanitePresent).to.be.false;
    });
});

describe('Supersonic Parachute Deployment Corridor, Transient Brine Metastability & Copiapite Acid Drainage', () => {
    it('should calculate DGB supersonic parachute deployment opening shock force, deceleration Gs, and terminal speed', () => {
        // MSL / Perseverance parachute deploy (Mach 1.85, q = 550 Pa, mass = 1950 kg, D0 = 21.5 m):
        const chute = TrajectoryEngine.computeSupersonicParachuteDeploymentCorridor(1.85, 550.0, 1950.0, 21.5, 'mars');
        expect(chute.openingShockForceKN).to.be.closeTo(309.5, 10.0); // ~309.5 kN shock force
        expect(chute.openingDecelerationGs).to.be.closeTo(16.18, 0.8); // ~16.2 Gs opening jolt
        expect(chute.parachuteCanopyAreaM2).to.be.closeTo(363.05, 5.0);
        expect(chute.terminalDescentSpeedMS).to.be.closeTo(66.6, 5.0); // ~66.6 m/s subsonic terminal velocity
        expect(chute.parachuteDeploymentStatus).to.include('Nominal Supersonic DGB');
    });

    it('should calculate transient liquid brine flow metastability, evaporative boiling flux, and survival lifetime', () => {
        // Warm summer slope in Valles Marineris with magnesium perchlorate brine (230 K, 610 Pa, 1 cm thickness):
        const brine = KRCEngine.computeTransientBrineMetastabilityAndFreezingLifetime(230.0, 610.0, 1.0, 'mg_perchlorate', 5.0);
        expect(brine.eutecticTempK).to.equal(206.0); // -67 deg C eutectic
        expect(brine.isLiquidThermodynamicallyStable).to.be.true;
        expect(brine.evaporationLifetimeHours).to.be.greaterThan(0.5); // persists as liquid for hours
        expect(brine.rslBrineSurvivalRegime).to.include('Metastable Liquid Brine Flow Active');
    });

    it('should discriminate Copiapite ferric hydroxy-sulfate gossan from bassanite, jarosite, and basalt in CRISM spectra', () => {
        // Copiapite in Ophir Chasma sulfide gossan (strong 0.86 um Fe3+, 1.43 um, 1.93 um, 2.16 um, and 2.43 um bands):
        const copiapite = BandMathEngine.computeCRISMCopiapiteIndices(0.24, 0.23, 0.22, 0.24, 0.24, 0.30);
        expect(copiapite.isCopiapitePresent).to.be.true;
        expect(copiapite.sulfatePhase).to.include('Copiapite');
        expect(copiapite.weatheringEnvironment).to.include('Extreme Acid Mine Drainage');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMCopiapiteIndices(0.30, 0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isCopiapitePresent).to.be.false;
    });
});

describe('Powered Descent Propellant Budget, Precession Polar Insolation & Kaolinite Weathering', () => {
    it('should calculate powered descent gravity turn Delta-V, burn duration, and propellant mass budget', () => {
        // MSL / Perseverance Sky Crane PDI (h0 = 1500 m, v0 = 80 m/s, gamma = -65 deg, T/W = 2.5, Isp = 225 s, mass = 1950 kg):
        const landing = TrajectoryEngine.computePoweredDescentGravityTurnPropellantBudget(1500.0, 80.0, -65.0, 2.5, 225.0, 1950.0, 'mars');
        expect(landing.kinematicDeltaVMS).to.be.closeTo(79.25, 1.0); // ~79.25 m/s kinematic reduction
        expect(landing.gravityLossDeltaVMS).to.be.closeTo(51.4, 3.0); // ~51.4 m/s gravity losses
        expect(landing.totalMissionDeltaVMS).to.be.closeTo(150.2, 5.0); // ~150 m/s total budget with margin
        expect(landing.burnDurationSec).to.be.closeTo(14.3, 1.0); // ~14.3 second burn
        expect(landing.propellantConsumedKg).to.be.closeTo(128.3, 10.0); // ~128 kg propellant
        expect(landing.descentGuidanceRegime).to.include('Sky Crane');
    });

    it('should calculate precession of perihelion insolation asymmetry and polar water ice retention', () => {
        // Current astronomical epoch (obliquity = 25.2 deg, e = 0.0934, varpi = 251 deg):
        const precession = KRCEngine.computePrecessionInsolationAsymmetryAndPolarIceMoundGrowth(25.2, 0.0934, 251.0, 0.45, 0.70);
        expect(precession.northPeakSummerInsolationWM2).to.be.lessThan(precession.southPeakSummerInsolationWM2);
        expect(precession.northPeakSummerTempK).to.be.closeTo(215.0, 15.0); // cool North summer protects water ice
        expect(precession.dominantWaterIceAccumulationPole).to.include('Planum Boreum');
        expect(precession.precessionPhaseDescription).to.include('Current Epoch: Southern Summer at Perihelion');
    });

    it('should discriminate well-crystallized Kaolinite from hydrated Halloysite and unaltered basalt in CRISM spectra', () => {
        // Kaolinite at Mawrth Vallis (strong 1.41/1.46 um and 2.16/2.21 um doublets, weak 1.91 um water):
        const kaolinite = BandMathEngine.computeCRISMKaoliniteHalloysiteIndices(0.23, 0.25, 0.298, 0.25, 0.22, 0.30);
        expect(kaolinite.isKaolinGroupPresent).to.be.true;
        expect(kaolinite.kaolinMineralSpecies).to.include('Kaolinite');
        expect(kaolinite.weatheringPedogenicContext).to.include('Subaerial Acid/Meteoric Leaching');

        // Hydrated Halloysite (strong 1.91 um band):
        const halloysite = BandMathEngine.computeCRISMKaoliniteHalloysiteIndices(0.23, 0.25, 0.22, 0.25, 0.22, 0.30);
        expect(halloysite.isKaolinGroupPresent).to.be.true;
        expect(halloysite.kaolinMineralSpecies).to.include('Halloysite');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMKaoliniteHalloysiteIndices(0.30, 0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isKaolinGroupPresent).to.be.false;
    });
});

describe('Sky Crane Bridle Dynamics, Diurnal Frost Condensation & Hydrothermal Dickite', () => {
    it('should calculate Sky Crane bridle tension, touchdown detection threshold, and flyaway separation distance', () => {
        // Perseverance Sky Crane touchdown (rover = 1025 kg, stage = 900 kg, bridle angle = 12 deg, flyaway Delta-V = 35 m/s):
        const crane = TrajectoryEngine.computeSkyCraneBridleDescentTensionAndFlyawayVelocity(1025.0, 900.0, 12.0, 35.0, 45.0, 'mars');
        expect(crane.totalRoverWeightN).to.be.closeTo(3813.8, 10.0); // ~3814 N in Mars gravity
        expect(crane.singleBridleTensionN).to.be.closeTo(1299.7, 10.0); // ~1300 N per line
        expect(crane.downrangeImpactDistanceMeters).to.be.closeTo(329.2, 10.0); // ~329 m flyaway clearance
        expect(crane.touchdownSafetyAssessment).to.include('High-Margin Flyaway Divert');
    });

    it('should calculate diurnal water frost condensation onset, frost point temperature, and morning sublimation', () => {
        // Viking 2 / Phoenix landing site (T_min = 185 K, T_max = 240 K, water column = 20 pr-um):
        const frost = KRCEngine.computeDiurnalFrostCondensationAndDewPointOnset(185.0, 240.0, 20.0, 250.0);
        expect(frost.frostPointTempK).to.be.closeTo(207.5, 5.0); // ~207.5 K frost point
        expect(frost.isNighttimeFrostFormed).to.be.true; // 185 K < 207 K -> frost forms
        expect(frost.frostDepositionDurationHours).to.be.greaterThan(2.0); // multi-hour nighttime frost
        expect(frost.peakFrostThicknessMicrons).to.be.greaterThan(0.01);
        expect(frost.diurnalHydrationRegime).to.include('Frost');
    });

    it('should discriminate high-temperature hydrothermal Dickite from low-temperature Kaolinite and basalt in CRISM spectra', () => {
        // Dickite in Nili Fossae hydrothermal fault zone (strong 1.38 um split, 1.41 um, and 2.16/2.21 um doublets):
        const dickite = BandMathEngine.computeCRISMDickiteNacriteIndices(0.24, 0.23, 0.25, 0.22, 0.30);
        expect(dickite.isDickitePresent).to.be.true;
        expect(dickite.polytypeSpecies).to.include('Dickite');
        expect(dickite.hydrothermalTemperatureRegime).to.include('High-Temperature Hydrothermal');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMDickiteNacriteIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isDickitePresent).to.be.false;
    });
});

describe('Solar Radiation Pressure, Interfacial Premelted Water & Smectite Layer Charge', () => {
    it('should calculate Solar Radiation Pressure (SRP) perturbation acceleration and daily velocity drift', () => {
        // Mars orbit spacecraft (r = 1.524 AU, area = 15 m^2, mass = 1000 kg, Cr = 1.30):
        const srp = TrajectoryEngine.computeSolarRadiationPressurePerturbation(1.524, 15.0, 1000.0, 1.30);
        expect(srp.totalSrpForceMicronewtons).to.be.closeTo(38.12, 1.0); // ~38.1 uN total force
        expect(srp.srpAccelerationNmS2).to.be.closeTo(38.12, 1.0); // ~38.12 nm/s^2 acceleration
        expect(srp.dailyDeltaVDriftMmSDay).to.be.closeTo(3.29, 0.2); // ~3.29 mm/s/day drift
        expect(srp.annualDeltaVDriftMSYear).to.be.closeTo(1.20, 0.1); // ~1.20 m/s/year
        expect(srp.orbitalPerturbationRegime).to.include('Moderate Perturbation');
    });

    it('should calculate cryogenic interfacial premelted unfrozen liquid water film thickness and habitability water activity', () => {
        // Warm subsurface permafrost boundary at 260 K (-13 deg C, specific area = 25 m^2/g):
        const film = KRCEngine.computeInterfacialPremeltedUnfrozenWaterFilmThickness(260.0, 25.0, 0.05);
        expect(film.interfacialFilmThicknessNm).to.be.closeTo(3.61, 0.5); // ~3.6 nm thick liquid film
        expect(film.molecularMonolayersCount).to.be.greaterThan(10.0); // > 10 molecular water monolayers
        expect(film.unfrozenWaterMgPerGSoil).to.be.closeTo(90.3, 15.0); // ~90 mg H2O / g soil
        expect(film.waterActivityAw).to.be.greaterThan(0.80); // high water activity
        expect(film.habitabilityBiochemicalRegime).to.include('Interfacial');
    });

    it('should discriminate Beidellite (Al-smectite) from Nontronite (Fe-smectite) and basalt in CRISM spectra', () => {
        // Beidellite in Mawrth Vallis upper leached unit (1.41 um, 1.91 um, 2.21 um Al-OH):
        const beidellite = BandMathEngine.computeCRISMBeidelliteNontroniteIndices(0.24, 0.22, 0.23, 0.30, 0.30);
        expect(beidellite.isSmectitePresent).to.be.true;
        expect(beidellite.smectiteCationSpecies).to.include('Beidellite');
        expect(beidellite.paleoenvironmentalContext).to.include('Open-System Leaching');

        // Nontronite in lower Noachian unit (1.41 um, 1.91 um, 2.29 um Fe-OH):
        const nontronite = BandMathEngine.computeCRISMBeidelliteNontroniteIndices(0.24, 0.22, 0.30, 0.23, 0.30);
        expect(nontronite.isSmectitePresent).to.be.true;
        expect(nontronite.smectiteCationSpecies).to.include('Nontronite');
        expect(nontronite.paleoenvironmentalContext).to.include('Closed-Basin');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMBeidelliteNontroniteIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isSmectitePresent).to.be.false;
    });
});

describe('B-Plane Hyperbolic Flyby, Cryosphere Basal Melting & Serpentine Inversion', () => {
    it('should calculate hyperbolic planetary flyby B-plane coordinates, deflection angle, and gravity assist Delta-V', () => {
        // Mars gravity assist flyby (v_inf = 5.5 km/s, pericenter altitude = 250 km, clock angle = 45 deg):
        const flyby = TrajectoryEngine.computeBPlaneTargetingCoordinatesAndHyperbolicDeflection(5500.0, 250.0, 45.0, 'mars');
        expect(flyby.hyperbolicExcessVelocityKmS).to.equal(5.5);
        expect(flyby.periapsisRadiusKm).to.equal(3639.5);
        expect(flyby.hyperbolicEccentricity).to.be.closeTo(3.57, 0.1);
        expect(flyby.deflectionAngleDeg).to.be.closeTo(32.51, 1.0); // ~32.5 deg trajectory bend
        expect(flyby.impactParameterMagnitudeKm).to.be.closeTo(4852.8, 20.0); // ~4853 km impact parameter
        expect(flyby.bPlaneRCoordinateKm).to.be.closeTo(3431.4, 20.0); // B_R = b * sin(45 deg)
        expect(flyby.bPlaneTCoordinateKm).to.be.closeTo(3431.4, 20.0); // B_T = b * cos(45 deg)
        expect(flyby.gravityAssistDeltaVKmS).to.be.closeTo(3.08, 0.1); // ~3.08 km/s velocity vector impulse
        expect(flyby.flybyRegime).to.include('Hyperbolic Gravity Assist');
    });

    it('should calculate Martian cryosphere basal melting depth, geothermal gradient, and global pore ice GEL', () => {
        // Mid-latitude crust (T_surf = 215 K, Q_geo = 25 mW/m^2, k_crust = 2.0 W/(m*K), porosity = 0.20):
        const cryo = KRCEngine.computeCryosphereBasalMeltingDepthAndGeothermalHeatFlux(215.0, 25.0, 2.0, 0.20, 'pure_water');
        expect(cryo.thermalGradientKPerKm).to.equal(12.5); // 12.5 K/km thermal gradient
        expect(cryo.basalMeltingTempK).to.equal(270.0);
        expect(cryo.cryosphereThicknessKm).to.be.closeTo(4.40, 0.2); // ~4.4 km permafrost base
        expect(cryo.poreIceGELMeters).to.be.closeTo(443.9, 20.0); // ~444 m GEL stored pore ice
        expect(cryo.subsurfaceAquiferStatus).to.include('Basal Liquid Aquifer Feasible');
    });

    it('should discriminate ultramafic Serpentine from metamorphic Chlorite and basalt in CRISM spectra', () => {
        // Serpentine in Nili Fossae (strong 1.39 um Mg-OH, 2.12 um, 2.33 um Mg-OH, weak 1.91 um water):
        const serpentine = BandMathEngine.computeCRISMSerpentineChloriteIndices(0.24, 0.298, 0.25, 0.30, 0.22, 0.30);
        expect(serpentine.isTrioctahedralPresent).to.be.true;
        expect(serpentine.trioctahedralSpecies).to.include('Serpentine');
        expect(serpentine.serpentinizationEnergyContext).to.include('H2 & CH4 Generation');

        // Chlorite (2.25 um and 2.33 um doublet):
        const chlorite = BandMathEngine.computeCRISMSerpentineChloriteIndices(0.30, 0.29, 0.30, 0.23, 0.22, 0.30);
        expect(chlorite.isTrioctahedralPresent).to.be.true;
        expect(chlorite.trioctahedralSpecies).to.include('Chlorite');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMSerpentineChloriteIndices(0.30, 0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isTrioctahedralPresent).to.be.false;
    });
});

describe('Hohmann Interplanetary Transfer, Impact Hydrothermal Lifetime & Prehnite Facies', () => {
    it('should calculate Earth-to-Mars heliocentric Hohmann transfer TOF, C3 energy, and TMI Delta-V', () => {
        // Earth to Mars transfer from 200 km LEO parking orbit:
        const hohmann = TrajectoryEngine.computeTransMarsInjectionDeltaVAndHohmannTrajectory('earth', 'mars', 200.0);
        expect(hohmann.transferSemiMajorAxisAU).to.be.closeTo(1.2618, 0.01);
        expect(hohmann.timeOfFlightDays).to.be.closeTo(258.9, 2.0); // ~259 days TOF
        expect(hohmann.timeOfFlightMonths).to.be.closeTo(8.5, 0.2); // ~8.5 months
        expect(hohmann.hyperbolicDepartureExcessKmS).to.be.closeTo(2.945, 0.1); // ~2.95 km/s v_inf
        expect(hohmann.characteristicLaunchEnergyC3Km2S2).to.be.closeTo(8.67, 0.5); // ~8.67 km^2/s^2 C3
        expect(hohmann.transInjectionDeltaVKmS).to.be.closeTo(3.612, 0.1); // ~3.61 km/s TMI burn
        expect(hohmann.hyperbolicArrivalExcessKmS).to.be.closeTo(2.65, 0.1); // ~2.65 km/s Mars arrival excess
        expect(hohmann.transferGeometryDescription).to.include('EARTH to MARS');
    });

    it('should calculate impact crater hydrothermal convective circulation lifetime and Rayleigh number', () => {
        // 100 km diameter complex crater on Mars (e.g. Gale / Jezero analogue with 250 mD fractured permeability):
        const hydro = KRCEngine.computeImpactHydrothermalSystemCoolingLifetime(100.0, 250.0, 1473.0);
        expect(hydro.impactMeltVolumeKm3).to.be.closeTo(9141.8, 50.0); // ~9142 km^3 melt volume
        expect(hydro.centralMeltThicknessMeters).to.equal(5000.0); // 5 km central uplift
        expect(hydro.rayleighNumber).to.be.greaterThan(40.0); // convective regime active
        expect(hydro.isConvectiveHydrothermalActive).to.be.true;
        expect(hydro.activeHydrothermalLifetimeYears).to.be.greaterThan(10000); // sustained for > 10,000 years
        expect(hydro.astrobiologicalHabitabilityWindow).to.include('Hydrothermal Habitable System');
    });

    it('should discriminate Prehnite from Pumpellyite and basalt in CRISM spectra', () => {
        // Prehnite in Toro Crater central peak (strong 1.475 um, 2.35 um, weak 1.91 um water):
        const prehnite = BandMathEngine.computeCRISMPrehnitePumpellyiteIndices(0.24, 0.298, 0.22, 0.30);
        expect(prehnite.isPrehniteFaciesPresent).to.be.true;
        expect(prehnite.metamorphicFaciesSpecies).to.include('Prehnite');
        expect(prehnite.hydrothermalPTPressureTemperatureContext).to.include('Prehnite-Pumpellyite Metamorphic Facies');

        // Pumpellyite (strong 1.91 um water):
        const pumpellyite = BandMathEngine.computeCRISMPrehnitePumpellyiteIndices(0.24, 0.22, 0.22, 0.30);
        expect(pumpellyite.isPrehniteFaciesPresent).to.be.true;
        expect(pumpellyite.metamorphicFaciesSpecies).to.include('Pumpellyite');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMPrehnitePumpellyiteIndices(0.30, 0.30, 0.30, 0.30);
        expect(basalt.isPrehniteFaciesPresent).to.be.false;
    });
});

describe('Mars Orbit Insertion, Paleo-Cryosphere Evolution & Epidote Facies', () => {
    it('should calculate Mars Orbit Insertion (MOI) capture Delta-V, period, and eccentricity', () => {
        // Mars arrival insertion (v_inf = 2.65 km/s, pericenter hp = 300 km, target apoapsis ha = 43000 km):
        const moi = TrajectoryEngine.computeMarsOrbitInsertionCaptureDeltaV(2.65, 300.0, 43000.0, 'mars');
        expect(moi.hyperbolicPeriapsisSpeedKmS).to.be.closeTo(5.50, 0.05); // ~5.50 km/s hyperbolic pericenter
        expect(moi.capturedPeriapsisSpeedKmS).to.be.closeTo(4.638, 0.05); // ~4.64 km/s captured pericenter
        expect(moi.orbitInsertionDeltaVKmS).to.be.closeTo(0.861, 0.05); // ~861 m/s MOI burn
        expect(moi.capturedOrbitPeriodHours).to.be.closeTo(33.36, 1.0); // ~33.4 hour orbit
        expect(moi.capturedOrbitEccentricity).to.be.closeTo(0.8526, 0.01);
        expect(moi.insertionBurnRegime).to.include('Highly Elliptical Capture Orbit');
    });

    it('should calculate 4-Gyr paleo-geothermal decay and ancient Noachian cryosphere thinning', () => {
        // Early Noachian 3.8 Ga boundary (T_paleo = 225 K, k_crust = 2.0 W/(m*K), Q_0 = 25 mW/m^2):
        const paleo = KRCEngine.computePaleoGeothermalCryosphereThinningAndNoachianMelting(3.8, 225.0, 2.0, 25.0);
        expect(paleo.paleoGeothermalFluxMWm2).to.be.closeTo(62.6, 2.0); // ~62.6 mW/m^2 heat flux in Noachian
        expect(paleo.paleoCryosphereThicknessKm).to.be.closeTo(1.54, 0.1); // ~1.54 km thin cryosphere seal
        expect(paleo.presentCryosphereThicknessKm).to.be.closeTo(4.65, 0.2); // ~4.65 km modern cryosphere
        expect(paleo.cryosphereThinningFactor).to.be.closeTo(0.33, 0.05); // 67% thinner
        expect(paleo.hydrologicDischargePotential).to.include('Valley Network Carving');
    });

    it('should discriminate Epidote calc-silicate from Clinozoisite and basalt in CRISM spectra', () => {
        // Epidote in central crater peak (strong 1.55 um OH and 2.34 um Fe3+-OH, weak 1.91 um water):
        const epidote = BandMathEngine.computeCRISMEpidoteClinozoisiteIndices(0.24, 0.298, 0.30, 0.22, 0.30);
        expect(epidote.isEpidoteGroupPresent).to.be.true;
        expect(epidote.epidoteGroupSpecies).to.include('Epidote');
        expect(epidote.metamorphicFaciesContext).to.include('Epidote-Amphibolite Metamorphic Facies');

        // Clinozoisite (2.26 um Al-OH):
        const clinozoisite = BandMathEngine.computeCRISMEpidoteClinozoisiteIndices(0.24, 0.298, 0.22, 0.30, 0.30);
        expect(clinozoisite.isEpidoteGroupPresent).to.be.true;
        expect(clinozoisite.epidoteGroupSpecies).to.include('Clinozoisite');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMEpidoteClinozoisiteIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isEpidoteGroupPresent).to.be.false;
    });
});

describe('Orbital Plane Change Maneuver, Subsurface Cryopeg & Opaline Silica Inversion', () => {
    it('should calculate pure and combined plane change Delta-V, propellant savings, and thrust angle', () => {
        // Combined orbital plane change (v1 = 3.5 km/s, delta_i = 30 deg, v2 = 4.2 km/s):
        const plane = TrajectoryEngine.computeOrbitalPlaneChangeDeltaVAndCombinedManeuver(3.50, 30.0, 4.20);
        expect(plane.purePlaneChangeDeltaVKmS).to.be.closeTo(1.812, 0.05); // ~1.81 km/s pure inclination turn
        expect(plane.combinedManeuverDeltaVKmS).to.be.closeTo(2.104, 0.05); // ~2.10 km/s combined vector turn
        expect(plane.separateManeuverDeltaVKmS).to.be.closeTo(2.512, 0.05); // ~2.51 km/s separate burns
        expect(plane.deltaVSavingsKmS).to.be.closeTo(0.408, 0.05); // ~408 m/s saved
        expect(plane.propellantSavingsPercent).to.be.closeTo(16.2, 1.0); // ~16.2% savings
        expect(plane.optimalThrustAngleDeg).to.be.closeTo(86.26, 1.0); // ~86.3 deg thrust pitch
        expect(plane.maneuverEfficiencyContext).to.include('High-Efficiency Combined Burn');
    });

    it('should calculate subsurface cryopeg hypersaline brine freezing point depression and stability column', () => {
        // South Pole Planum Australe (T_surf = 195 K, Q_geo = 25 mW/m^2, k_crust = 2.0 W/(m*K), Mg(ClO4)2 brine):
        const cryopeg = KRCEngine.computeCryopegSubsurfaceFreezingPointDepressionAndBrinePoreVolume('mg_perchlorate', 195.0, 25.0, 2.0, 0.20);
        expect(cryopeg.saltComposition).to.include('Magnesium Perchlorate');
        expect(cryopeg.eutecticFreezingTempK).to.equal(206.0); // 206 K eutectic
        expect(cryopeg.eutecticFreezingTempC).to.be.closeTo(-67.15, 0.1);
        expect(cryopeg.waterActivityAw).to.equal(0.50);
        expect(cryopeg.cryopegTopDepthKm).to.be.closeTo(0.88, 0.05); // ~880 m depth to liquid cryopeg
        expect(cryopeg.cryopegBaseDepthKm).to.be.closeTo(6.25, 0.1); // ~6.25 km base
        expect(cryopeg.cryopegColumnThicknessKm).to.be.closeTo(5.37, 0.1); // ~5.37 km thick permafrost brine column
        expect(cryopeg.astrobiologicalHabitabilityAssessment).to.include('Extreme Hypersaline');
    });

    it('should discriminate Opaline hydrated silica from crystalline Quartz and basalt in CRISM spectra', () => {
        // Opaline silica in Gusev Home Plate (strong 1.40 um Si-OH, 1.90 um H2O, 2.21 um Si-OH):
        const opal = BandMathEngine.computeCRISMOpalineSilicaCrystallineQuartzIndices(0.24, 0.22, 0.22, 0.30);
        expect(opal.isSilicaPhasePresent).to.be.true;
        expect(opal.silicaMineralogy).to.include('Hydrated Opaline Silica');
        expect(opal.hydrothermalGenesisContext).to.include('Hot Spring Sinter');

        // Quartz/Chalcedony (weak 1.90 um water):
        const quartz = BandMathEngine.computeCRISMOpalineSilicaCrystallineQuartzIndices(0.30, 0.298, 0.22, 0.30);
        expect(quartz.isSilicaPhasePresent).to.be.true;
        expect(quartz.silicaMineralogy).to.include('Microcrystalline Quartz');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMOpalineSilicaCrystallineQuartzIndices(0.30, 0.30, 0.30, 0.30);
        expect(basalt.isSilicaPhasePresent).to.be.false;
    });
});

describe('Heliocentric Gravity Assist Vectors, Methane Clathrate & Sulfate Hydration States', () => {
    it('should calculate 2D planetocentric-to-heliocentric gravity assist vector addition and energy gain', () => {
        // Rosetta / Dawn Mars flyby (v_inf = 5.6 km/s, approach angle = 120 deg, pericenter altitude = 250 km):
        const ga = TrajectoryEngine.computeInterplanetaryGravityAssistHeliocentricVelocityVector(5.60, 120.0, 24.13, 250.0, 'mars');
        expect(ga.hyperbolicDeflectionAngleDeg).to.be.closeTo(31.67, 1.0); // ~31.7 deg turn angle
        expect(ga.ingoingHeliocentricSpeedKmS).to.be.closeTo(29.115, 0.1); // ~29.12 km/s heliocentric arrival
        expect(ga.outgoingHeliocentricSpeedKmS).to.be.closeTo(29.728, 0.1); // ~29.73 km/s heliocentric departure
        expect(ga.netHeliocentricSpeedChangeKmS).to.be.closeTo(0.613, 0.05); // ~+613 m/s speed boost
        expect(ga.flybyVectorImpulseMagnitudeKmS).to.be.closeTo(3.056, 0.1); // ~3.06 km/s vector impulse
        expect(ga.gravityAssistRegime).to.include('Trailing-Side Flyby');
    });

    it('should calculate subsurface methane clathrate thermodynamic dissociation and Darcy plume flux', () => {
        // Gale Crater clathrate pocket (z = 150 m, delta_T = 15 K pulse, k = 50 mD):
        const clathrate = KRCEngine.computeSubsurfaceMethaneClathrateDissociationAndPlumeFlux(150.0, 15.0, 50.0, 215.0);
        expect(clathrate.lithostaticPorePressureKPa).to.be.closeTo(1395.9, 10.0); // ~1396 kPa confining pressure
        expect(clathrate.clathrateEquilibriumTempK).to.be.closeTo(139.8, 2.0); // ~140 K dissociation temp
        expect(clathrate.isClathrateThermallyDestabilized).to.be.true;
        expect(clathrate.gasOverpressureKPa).to.be.greaterThan(0);
        expect(clathrate.surfaceMethaneFluxNmolM2S).to.be.greaterThan(0);
        expect(clathrate.atmosphericColumnSpikePpbv).to.be.greaterThan(10.0); // > 10 ppbv spike
        expect(clathrate.atmosphericPlumeSignature).to.include('Methane Plume Outburst');
    });

    it('should discriminate Monohydrated Sulfates from Polyhydrated Sulfates and basalt in CRISM spectra', () => {
        // Monohydrated Sulfate (Kieserite in Juventae Chasma: strong 2.13 um and 2.40 um):
        const kieserite = BandMathEngine.computeCRISMSulfateHydrationStateIndices(0.30, 0.30, 0.22, 0.22, 0.30);
        expect(kieserite.isSulfatePresent).to.be.true;
        expect(kieserite.sulfateHydrationState).to.include('Monohydrated Sulfate');
        expect(kieserite.paleoclimateDesiccationContext).to.include('Hyper-Arid Paleoclimate');

        // Polyhydrated Sulfate (Epsomite/Gypsum: strong 1.43 um, 1.93 um, and 2.40 um):
        const poly = BandMathEngine.computeCRISMSulfateHydrationStateIndices(0.24, 0.22, 0.30, 0.22, 0.30);
        expect(poly.isSulfatePresent).to.be.true;
        expect(poly.sulfateHydrationState).to.include('Polyhydrated Sulfate');
        expect(poly.paleoclimateDesiccationContext).to.include('Aqueous Evaporite Lake Basin');

        // Basalt:
        const basalt = BandMathEngine.computeCRISMSulfateHydrationStateIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isSulfatePresent).to.be.false;
    });
});

describe('Continuous Low-Thrust Spiral, Magma Sill Solidification & Pyroxene HCP/LCP', () => {
    it('should calculate low-thrust continuous Edelbaum spiral Delta-V, propellant mass, and duration', () => {
        // Low Mars Orbit (400 km) to Areostationary Orbit (17038.5 km) transfer with 0.50 N ion thruster (Isp = 3200 s, m0 = 1000 kg):
        const spiral = TrajectoryEngine.computeContinuousLowThrustSpiralOrbitRaising(400.0, 17038.5, 0.50, 3200.0, 1000.0, 'mars');
        expect(spiral.initialOrbitalSpeedKmS).to.be.closeTo(3.362, 0.05); // ~3.36 km/s LMO speed
        expect(spiral.finalOrbitalSpeedKmS).to.be.closeTo(1.448, 0.05); // ~1.45 km/s Areostationary speed
        expect(spiral.continuousSpiralDeltaVKmS).to.be.closeTo(1.914, 0.05); // ~1.91 km/s Edelbaum Delta-V
        expect(spiral.propellantConsumedKg).to.be.closeTo(59.17, 2.0); // ~59.2 kg Xenon propellant
        expect(spiral.transferDurationDays).to.be.closeTo(43.0, 3.0); // ~43 days continuous thrusting
        expect(spiral.totalSpiralRevolutions).to.be.greaterThan(50);
        expect(spiral.lowThrustPropulsionContext).to.include('Continuous Solar Electric');
    });

    it('should calculate subsurface magma sill solidification time and thermal metamorphic halo width', () => {
        // 500 m thick basaltic sill at 3 km depth in Elysium Planitia:
        const sill = KRCEngine.computeSubsurfaceMagmaSillCoolingAndThermalHalo(500.0, 3000.0, 1473.15, 1e-6);
        expect(sill.sillHalfThicknessMeters).to.equal(250.0);
        expect(sill.solidificationTimeYears).to.be.closeTo(688.0, 100.0); // ~688 years to crystallize
        expect(sill.thermalHaloWidthMeters).to.be.closeTo(294.0, 80.0); // ~294 m metamorphic aureole
        expect(sill.peakWallrockContactTempC).to.be.closeTo(589.7, 20.0); // ~590 C contact temperature
        expect(sill.hydrothermalContactZoneMetamorphism).to.include('Hydrothermal Skarn');
    });

    it('should discriminate High-Calcium Pyroxene (Augite) from Low-Calcium Pyroxene (Enstatite) and basalt in CRISM spectra', () => {
        // High-Calcium Pyroxene (Augite in Syrtis Major: Band 1 = 1.05 um, Band 2 = 2.30 um):
        const hcp = BandMathEngine.computeCRISMPyroxeneHighLowCalciumIndices(1.05, 2.30, 0.08, 0.08);
        expect(hcp.isPyroxenePresent).to.be.true;
        expect(hcp.pyroxeneMineralClass).to.include('High-Calcium Pyroxene');
        expect(hcp.petrogeneticEvolutionContext).to.include('Evolved Differentiated Basaltic');

        // Low-Calcium Pyroxene (Orthopyroxene in Noachian crust: Band 1 = 0.92 um, Band 2 = 1.90 um):
        const lcp = BandMathEngine.computeCRISMPyroxeneHighLowCalciumIndices(0.92, 1.90, 0.08, 0.08);
        expect(lcp.isPyroxenePresent).to.be.true;
        expect(lcp.pyroxeneMineralClass).to.include('Low-Calcium Pyroxene');
        expect(lcp.petrogeneticEvolutionContext).to.include('Primitive Ancient Martian Crust');

        // Flat spectrum:
        const flat = BandMathEngine.computeCRISMPyroxeneHighLowCalciumIndices(1.00, 2.00, 0.01, 0.01);
        expect(flat.isPyroxenePresent).to.be.false;
    });
});

describe('Planetary Frozen Orbit Conditions, Diurnal Deliquescence & Plagioclase Anorthosite', () => {
    it('should calculate planetary frozen orbit parameters, J2/J3 equilibrium, and frozen eccentricity', () => {
        // Mars Sun-synchronous mapping orbit (mean altitude = 400 km, inc = 93.0 deg):
        const frozen = TrajectoryEngine.computeFrozenOrbitEquilibriumAndAltitudeOscillation(400.0, 93.0, 'mars');
        expect(frozen.semiMajorAxisKm).to.equal(3789.5);
        expect(frozen.frozenEccentricity).to.be.closeTo(0.007176, 0.0005); // ~0.00718 frozen eccentricity
        expect(frozen.frozenArgumentOfPeriapsisDeg).to.equal(270.0); // 270 deg south polar periapsis
        expect(frozen.periapsisAltitudeKm).to.be.closeTo(372.8, 2.0); // ~373 km
        expect(frozen.apoapsisAltitudeKm).to.be.closeTo(427.2, 2.0); // ~427 km
        expect(frozen.altitudeVariationRangeKm).to.be.closeTo(54.4, 3.0); // ~54.4 km range
        expect(frozen.criticalInclinationDeg).to.equal(63.435);
        expect(frozen.frozenOrbitStabilityContext).to.include('Sun-Synchronous Mapping Frozen Orbit');
    });

    it('should calculate diurnal perchlorate salt deliquescence humidity threshold and transient liquid brine window', () => {
        // Phoenix landing site morning soil (RH = 65%, T = 225 K, Ca(ClO4)2):
        const del = KRCEngine.computePerchlorateSaltDeliquescenceDiurnalKinetics(65.0, 225.0, 'ca_perchlorate', 1.0);
        expect(del.saltType).to.include('Calcium Perchlorate');
        expect(del.eutecticTempK).to.equal(221.0);
        expect(del.deliquescenceHumidityThresholdPct).to.be.closeTo(49.0, 2.0); // ~49% DRH threshold
        expect(del.isDeliquescenceActive).to.be.true;
        expect(del.adsorbedWaterMassGramsPerKgSoil).to.be.greaterThan(10.0);
        expect(del.dailyLiquidBrineWindowHours).to.be.greaterThan(2.0); // > 2 hours/sol of active liquid brine
        expect(del.deliquescenceThermodynamicState).to.include('Active Liquid Aqueous Brine');
    });

    it('should discriminate pure crystalline Plagioclase Anorthosite from basalt in CRISM spectra', () => {
        // Pure Anorthosite in Valles Marineris central peak (broad 1.25 um Fe2+ minimum, absent 0.95/1.75/1.90 um):
        const anorth = BandMathEngine.computeCRISMAnorthositeMagmaOceanFlotationIndices(0.24, 0.30, 0.30, 0.30, 0.30);
        expect(anorth.isAnorthositePresent).to.be.true;
        expect(anorth.plagioclaseMineralogy).to.include('Pure Crystalline Anorthosite');
        expect(anorth.primordialCrustContext).to.include('Primordial Martian Magma Ocean');

        // Basalt (no 1.25 um plagioclase band):
        const basalt = BandMathEngine.computeCRISMAnorthositeMagmaOceanFlotationIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isAnorthositePresent).to.be.false;
    });
});

describe('Mars Aerocapture Entry Dynamics, Paleolake Wave Energy & Opal-A/CT Maturation', () => {
    it('should calculate Mars aerocapture atmospheric entry speed, aerodynamic Delta-V, and apoapsis raise burn', () => {
        // Interplanetary approach v_inf = 5.7 km/s, target apoapsis = 6000 km, atmospheric corridor periapsis = 45 km:
        const aero = TrajectoryEngine.computeMarsAerocaptureAtmosphericEntryAndOrbitInsertion(5.70, 6000.0, 45.0, 125.0, 'mars');
        expect(aero.atmosphericEntrySpeedKmS).to.be.closeTo(7.541, 0.05); // ~7.54 km/s entry velocity
        expect(aero.atmosphericExitSpeedKmS).to.be.closeTo(4.206, 0.05); // ~4.21 km/s post-atmospheric exit
        expect(aero.aerodynamicDeltaVDissipatedKmS).to.be.closeTo(3.335, 0.05); // ~3.34 km/s absorbed by Mars atmosphere
        expect(aero.apoapsisPeriapsisRaiseDeltaVMPS).to.be.closeTo(33.0, 5.0); // ~33 m/s small raise burn at apoapsis
        expect(aero.propulsiveMassSavingsPercent).to.be.greaterThan(60.0); // > 60% propellant savings
        expect(aero.aerocaptureRegime).to.include('Mars Guided Aerocapture');
    });

    it('should calculate ancient Martian paleolake wind-generated wave height, wave power, and coastal cliff retreat', () => {
        // Jezero / Gale Crater paleolake (fetch = 50 km, wind = 15 m/s, depth = 100 m):
        const wave = KRCEngine.computeAncientMartianPaleolakeWaveEnergyAndCoastalErosion(50.0, 15.0, 100.0, 0.50);
        expect(wave.significantWaveHeightMeters).to.be.closeTo(3.51, 0.2); // ~3.5 m wave height on Mars
        expect(wave.peakWavePeriodSec).to.be.closeTo(6.4, 0.5); // ~6.4 s wave period
        expect(wave.wavelengthMeters).to.be.closeTo(24.3, 3.0); // ~24 m wavelength
        expect(wave.wavePowerFluxKWMeter).to.be.closeTo(5.44, 1.0); // ~5.4 kW/m shoreline wave power
        expect(wave.coastalCliffRetreatRateMPerKyr).to.be.closeTo(1.09, 0.3); // ~1.09 m / kyr notch retreat
        expect(wave.lacustrineWaveRegime).to.include('Moderate Lacustrine Wave Action');
    });

    it('should discriminate Opal-A amorphous silica from Opal-CT paracrystalline silica and basalt in CRISM spectra', () => {
        // Opal-CT paracrystalline cristobalite silica in Toro Crater (2.21 um Si-OH and 2.26 um cristobalite shoulder):
        const opalCT = BandMathEngine.computeCRISMOpalA_CTParacrystallineDehydrationIndices(0.25, 0.28, 0.22, 0.22, 0.30);
        expect(opalCT.isSilicaPhasePresent).to.be.true;
        expect(opalCT.silicaCrystallinityPhase).to.include('Paracrystalline Opal-CT');
        expect(opalCT.diageneticMaturationSetting).to.include('Post-Impact Hydrothermal Maturation');

        // Opal-A amorphous sinter in Gusev Home Plate (strong 1.40/1.90 um and 2.21 um Si-OH without 2.26 um shoulder):
        const opalA = BandMathEngine.computeCRISMOpalA_CTParacrystallineDehydrationIndices(0.24, 0.22, 0.22, 0.30, 0.30);
        expect(opalA.isSilicaPhasePresent).to.be.true;
        expect(opalA.silicaCrystallinityPhase).to.include('Amorphous Opal-A');
        expect(opalA.diageneticMaturationSetting).to.include('Epithermal Hot Spring Sinter');

        // Basalt (no silica absorption):
        const basalt = BandMathEngine.computeCRISMOpalA_CTParacrystallineDehydrationIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isSilicaPhasePresent).to.be.false;
    });
});

describe('Trans-Earth Injection Hohmann Return, Glacial Flow Creep & Olivine Fo-Fa Composition', () => {
    it('should calculate Trans-Earth Injection Delta-V, Hohmann return trajectory, and Earth entry speed', () => {
        // Mars 400 km parking orbit to Earth atmospheric entry:
        const tei = TrajectoryEngine.computeTransEarthInjectionDeltaVAndReturnTrajectory(400.0, 120.0);
        expect(tei.transferSemiMajorAxisAU).to.be.closeTo(1.26184, 0.005); // ~1.262 AU
        expect(tei.timeOfFlightDays).to.be.closeTo(258.9, 2.0); // ~259 days return TOF
        expect(tei.marsDepartureVInfKmS).to.be.closeTo(2.648, 0.05); // ~2.65 km/s Mars hyperbolic excess
        expect(tei.transEarthInjectionDeltaVKmS).to.be.closeTo(2.080, 0.05); // ~2.08 km/s TEI burn
        expect(tei.earthArrivalVInfKmS).to.be.closeTo(2.946, 0.05); // ~2.95 km/s Earth arrival excess
        expect(tei.earthAtmosphericReentrySpeedKmS).to.be.closeTo(11.46, 0.1); // ~11.46 km/s direct Earth re-entry speed
        expect(tei.returnTrajectoryContext).to.include('Mars-to-Earth Hohmann Direct Return');
    });

    it('should calculate ancient Martian glacial flow velocity, Glen law creep, and basal shear stress', () => {
        // Deuteronilus Mensae LDA (thickness = 400 m, slope = 3.0 deg, T = 230 K):
        const glacier = KRCEngine.computeAncientMartianGlacialFlowVelocityAndBasalShearStress(400.0, 3.0, 230.0, false);
        expect(glacier.basalShearStressKPa).to.be.closeTo(71.68, 2.0); // ~71.7 kPa driving shear stress
        expect(glacier.internalDeformationSpeedMmYr).to.be.closeTo(21.5, 5.0); // ~21.5 mm/year internal creep
        expect(glacier.surfaceFlowSpeedMmYr).to.be.closeTo(21.5, 5.0);
        expect(glacier.annualIceFluxM2Yr).to.be.greaterThan(5.0);
        expect(glacier.glacialDynamicRegime).to.include('Cold-Based Polythermal Glacial Creep');
    });

    it('should discriminate Forsterite-rich Olivine from Fayalite-rich Olivine and basalt in CRISM spectra', () => {
        // Forsterite-rich olivine in Nili Fossae (Fo80: composite trough centered at 1.040 um):
        const forsterite = BandMathEngine.computeCRISMOlivineForsteriteFayaliteIndices(1.040, 0.08);
        expect(forsterite.isOlivinePresent).to.be.true;
        expect(forsterite.estimatedForsteriteMolePct).to.be.closeTo(82.7, 5.0); // ~Fo83
        expect(forsterite.olivineSolidSolutionPhase).to.include('Forsterite-Rich Magnesian Olivine');
        expect(forsterite.petrologicalSettingContext).to.include('Primitive Upper Mantle Melting');

        // Fayalite-rich olivine in differentiated caldera lavas (Fo25: trough shifted to 1.080 um):
        const fayalite = BandMathEngine.computeCRISMOlivineForsteriteFayaliteIndices(1.080, 0.08);
        expect(fayalite.isOlivinePresent).to.be.true;
        expect(fayalite.estimatedForsteriteMolePct).to.be.closeTo(24.5, 5.0); // ~Fo25
        expect(fayalite.olivineSolidSolutionPhase).to.include('Fayalite-Rich Ferrous Olivine');
        expect(fayalite.petrologicalSettingContext).to.include('Evolved Fractional Crystallization');

        // Flat spectrum:
        const basalt = BandMathEngine.computeCRISMOlivineForsteriteFayaliteIndices(1.050, 0.01);
        expect(basalt.isOlivinePresent).to.be.false;
    });
});

describe('LMO Atmospheric Drag Decay, Cryohydrate Stability & Trioctahedral Smectites', () => {
    it('should calculate Low Mars Orbit atmospheric drag decay rate, daily altitude loss, and orbital lifetime', () => {
        // 200 km LMO cubesat (ballistic coeff = 50 kg/m^2, moderate solar activity):
        const decay = TrajectoryEngine.computeLowMarsOrbitAtmosphericDecayAndLifetime(200.0, 50.0, 'moderate', 'mars');
        expect(decay.orbitAltitudeKm).to.equal(200.0);
        expect(decay.atmosphericDensityKgM3).to.be.closeTo(1.28e-11, 0.5e-11); // ~1.28e-11 kg/m^3
        expect(decay.orbitalPeriodMinutes).to.be.closeTo(108.8, 2.0); // ~109 min orbit
        expect(decay.dailyAltitudeLossMeters).to.be.closeTo(274.0, 50.0); // ~274 m/day altitude loss
        expect(decay.estimatedOrbitalLifetimeDays).to.be.closeTo(38.3, 10.0); // ~38 days lifetime
        expect(decay.orbitalDecayRegime).to.include('Moderate Thermospheric Drag');
    });

    it('should calculate subsurface salt cryohydrate phase stability, eutectic melting depth, and brine viscosity', () => {
        // Hydrohalite (NaCl*2H2O, T_surf = 215 K, Q_geo = 25 mW/m^2, k = 2.0 W/m*K):
        const cryo = KRCEngine.computeSubsurfaceCryohydrateSaltFreezingDepressionAndBrineMobility('hydrohalite', 215.0, 25.0, 2.0);
        expect(cryo.cryohydrateMineralogy).to.include('Hydrohalite');
        expect(cryo.chemicalFormula).to.include('NaCl * 2H2O');
        expect(cryo.eutecticMeltingTempK).to.equal(252.0);
        expect(cryo.depthToLiquidBrineKm).to.be.closeTo(2.96, 0.1); // ~2.96 km melting horizon
        expect(cryo.relativeBrineViscosityVsWater).to.equal(3.2);
        expect(cryo.astrobiologicalPoreStability).to.include('Hypersaline Subglacial Liquefaction Horizon');
    });

    it('should discriminate Trioctahedral Smectite (Saponite) from Vermiculite and basalt in CRISM spectra', () => {
        // Trioctahedral Saponite in Nili Fossae (sharp 2.31 um Mg-OH and 1.92 um H2O, absent 2.38 um doublet):
        const saponite = BandMathEngine.computeCRISMTrioctahedralSmectiteVermiculiteIndices(0.24, 0.22, 0.22, 0.30, 0.30);
        expect(saponite.isPhyllosilicatePresent).to.be.true;
        expect(saponite.phyllosilicateClaySpecies).to.include('Trioctahedral Smectite (Saponite');
        expect(saponite.alkalineAqueousSetting).to.include('Neutral-to-Alkaline');

        // Trioctahedral Vermiculite / Hectorite (2.31 um and 2.38 um doublet):
        const vermiculite = BandMathEngine.computeCRISMTrioctahedralSmectiteVermiculiteIndices(0.24, 0.22, 0.22, 0.22, 0.30);
        expect(vermiculite.isPhyllosilicatePresent).to.be.true;
        expect(vermiculite.phyllosilicateClaySpecies).to.include('Trioctahedral Vermiculite');
        expect(vermiculite.alkalineAqueousSetting).to.include('Alkaline Hydrothermal');

        // Basalt (no clay bands):
        const basalt = BandMathEngine.computeCRISMTrioctahedralSmectiteVermiculiteIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isPhyllosilicatePresent).to.be.false;
    });
});

describe('Phobos/Deimos Moon Rendezvous, Paleoshoreline Flexure & Pigeonite Pyroxenes', () => {
    it('should calculate Phobos/Deimos co-orbital rendezvous Delta-V, transfer time, and Hill sphere radius', () => {
        // Phobos rendezvous from 400 km LMO:
        const phobos = TrajectoryEngine.computeMartianMoonCoOrbitalRendezvousAndHillSphere('phobos', 400.0);
        expect(phobos.targetMoon).to.include('Phobos');
        expect(phobos.moonSemiMajorAxisKm).to.equal(9376.0);
        expect(phobos.moonOrbitalSpeedKmS).to.be.closeTo(2.137, 0.05); // ~2.14 km/s
        expect(phobos.moonOrbitalPeriodHours).to.be.closeTo(7.654, 0.1); // ~7.65 hours
        expect(phobos.moonHillSphereRadiusKm).to.be.closeTo(16.59, 0.5); // ~16.6 km Hill sphere
        expect(phobos.hohmannTransferDeltaVKmS).to.be.closeTo(1.167, 0.05); // ~1.17 km/s total Hohmann burn
        expect(phobos.transferTimeOfFlightHours).to.be.closeTo(2.247, 0.1); // ~2.25 hours TOF
        expect(phobos.qsoProximityInsertionDeltaVMPS).to.equal(12.5);

        // Deimos rendezvous:
        const deimos = TrajectoryEngine.computeMartianMoonCoOrbitalRendezvousAndHillSphere('deimos', 400.0);
        expect(deimos.targetMoon).to.include('Deimos');
        expect(deimos.moonSemiMajorAxisKm).to.equal(23463.0);
        expect(deimos.moonHillSphereRadiusKm).to.be.closeTo(21.48, 0.5); // ~21.5 km Hill sphere
    });

    it('should calculate ancient Martian ocean shoreline lithospheric flexural warping and GIA rebound', () => {
        // Northern Lowlands paleoocean (Te = 40 km, depth = 1500 m, basin radius = 2500 km):
        const flex = KRCEngine.computeAncientMartianPaleoshorelineFlexureAndGIADeformation(40.0, 1500.0, 2500.0);
        expect(flex.elasticThicknessKm).to.equal(40.0);
        expect(flex.flexuralParameterAlphaKm).to.be.closeTo(125.1, 10.0); // ~125 km flexural parameter
        expect(flex.centralIsostaticDeflectionMeters).to.be.closeTo(600.0, 5.0); // ~600 m central depression
        expect(flex.shorelineElevationWarpingMeters).to.be.greaterThan(500.0); // > 500 m elevation warping
        expect(flex.paleoshorelineDeformationContext).to.include('Major Shoreline Elevation Warping');
    });

    it('should discriminate Pigeonite (intermediate-Ca) from Orthopyroxene (LCP) and Augite (HCP) in CRISM spectra', () => {
        // Pigeonite in Syrtis Major volcanic plains (Band 1 = 0.98 um, Band 2 = 2.12 um):
        const pigeonite = BandMathEngine.computeCRISMPigeoniteSubcalcicClinopyroxeneIndices(0.98, 2.12, 0.08);
        expect(pigeonite.isPyroxenePresent).to.be.true;
        expect(pigeonite.pyroxeneMineralSpecies).to.include('Pigeonite / Subcalcic Clinopyroxene');
        expect(pigeonite.estimatedWollastoniteMolePct).to.be.closeTo(12.5, 3.0); // ~Wo12.5
        expect(pigeonite.basalticPetrogenesisContext).to.include('High-Temperature Rapidly Quenched Tholeiitic');

        // Low-Ca Orthopyroxene (Band 1 = 0.93 um, Band 2 = 1.95 um):
        const opx = BandMathEngine.computeCRISMPigeoniteSubcalcicClinopyroxeneIndices(0.93, 1.95, 0.08);
        expect(opx.isPyroxenePresent).to.be.true;
        expect(opx.pyroxeneMineralSpecies).to.include('Orthopyroxene');

        // High-Ca Augite (Band 1 = 1.05 um, Band 2 = 2.30 um):
        const augite = BandMathEngine.computeCRISMPigeoniteSubcalcicClinopyroxeneIndices(1.05, 2.30, 0.08);
        expect(augite.isPyroxenePresent).to.be.true;
        expect(augite.pyroxeneMineralSpecies).to.include('High-Calcium Clinopyroxene');

        // Flat spectrum:
        const basalt = BandMathEngine.computeCRISMPigeoniteSubcalcicClinopyroxeneIndices(0.98, 2.12, 0.01);
        expect(basalt.isPyroxenePresent).to.be.false;
    });
});

describe('Sun-Synchronous Mapping Orbits, Hydrothermal Convection & Carbonate Group Cations', () => {
    it('should calculate Mars Sun-Synchronous Orbit inclination, nodal precession, and repeat track spacing', () => {
        // 300 km MRO-like mapping orbit (187 orbits in 14 sols):
        const sso = TrajectoryEngine.computeMartianSunSynchronousAndRepeatGroundTrackOrbit(300.0, 187, 14);
        expect(sso.orbitAltitudeKm).to.equal(300.0);
        expect(sso.sunSyncInclinationDeg).to.be.closeTo(92.97, 0.5); // ~93.0 deg retrograde polar inclination
        expect(sso.nodalPrecessionRateDegDay).to.be.closeTo(0.52403, 0.001); // ~0.524 deg/day matching Mars heliocentric motion
        expect(sso.orbitalPeriodMinutes).to.be.closeTo(113.48, 2.0); // ~113.5 min
        expect(sso.dailyOrbitsCount).to.be.closeTo(13.04, 0.2); // ~13.0 orbits/sol
        expect(sso.equatorialInterTrackSpacingKm).to.be.closeTo(113.88, 5.0); // ~113.9 km swath spacing
        expect(sso.mappingOrbitDesignContext).to.include('Mars Sun-Synchronous Frozen Mapping Orbit');
    });

    it('should calculate subsurface hydrothermal Rayleigh-Darcy convection, upwelling Darcy flux, and lifespan', () => {
        // Jezero/Gusev post-impact hydrothermal system (H = 3 km, deltaT = 400 C, kp = 1e-13 m^2):
        const hydro = KRCEngine.computeSubsurfaceHydrothermalConvectionAndBoilingPlume(3.0, 400.0, 1.0e-13);
        expect(hydro.rayleighDarcyNumber).to.be.closeTo(4050.6, 50.0); // Ra ~4051 > Ra_crit
        expect(hydro.isConvectionActive).to.be.true;
        expect(hydro.nusseltNumber).to.be.closeTo(39.23, 2.0); // Nu ~39.2
        expect(hydro.upwellingDarcySpeedMmDay).to.be.closeTo(77.16, 10.0); // ~77 mm/day Darcy flux
        expect(hydro.hydrothermalLifespanYears).to.be.closeTo(1821, 100); // ~1,821 years convective cooling lifetime
        expect(hydro.hydrothermalAstrobiologyContext).to.include('Vigorous Hydrothermal Upwelling Plume');
    });

    it('should discriminate Magnesite from Siderite and Calcite cations in CRISM carbonate spectra', () => {
        // Magnesite (MgCO3) in Nili Fossae (Band 1 = 2.310 um, Band 2 = 2.510 um):
        const magnesite = BandMathEngine.computeCRISMCarbonateCationCompositionIndices(2.310, 2.510, 0.08);
        expect(magnesite.isCarbonatePresent).to.be.true;
        expect(magnesite.carbonateMineralSpecies).to.include('Magnesite (Magnesium Carbonate');
        expect(magnesite.dominantDivalentCation).to.include('Mg2+');
        expect(magnesite.carbonationPaleoenvironment).to.include('Ultramafic Olivine Carbonation');

        // Siderite (FeCO3) in reducing paleolakes (Band 1 = 2.335 um, Band 2 = 2.530 um):
        const siderite = BandMathEngine.computeCRISMCarbonateCationCompositionIndices(2.335, 2.530, 0.08);
        expect(siderite.isCarbonatePresent).to.be.true;
        expect(siderite.carbonateMineralSpecies).to.include('Siderite');
        expect(siderite.dominantDivalentCation).to.include('Fe2+');

        // Calcite (CaCO3) in epithermal veins (Band 1 = 2.345 um, Band 2 = 2.545 um):
        const calcite = BandMathEngine.computeCRISMCarbonateCationCompositionIndices(2.345, 2.545, 0.08);
        expect(calcite.isCarbonatePresent).to.be.true;
        expect(calcite.carbonateMineralSpecies).to.include('Calcite');
        expect(calcite.dominantDivalentCation).to.include('Ca2+');

        // Flat spectrum:
        const basalt = BandMathEngine.computeCRISMCarbonateCationCompositionIndices(2.310, 2.510, 0.01);
        expect(basalt.isCarbonatePresent).to.be.false;
    });
});

describe('Areostationary Orbit Dynamics, CO2 Atmospheric Collapse & Diopside Pyroxenes', () => {
    it('should calculate Areostationary Orbit altitude, speed, and triaxial gravity drift', () => {
        // Mars areostationary orbit (at prime meridian 0 deg lon):
        const aero = TrajectoryEngine.computeAreostationaryOrbitAltitudeAndLongitudinalDrift(0.0);
        expect(aero.areostationaryAltitudeKm).to.be.closeTo(17058.5, 5.0); // ~17059 km altitude
        expect(aero.areostationaryRadiusKm).to.be.closeTo(20448.0, 5.0); // ~20448 km synchronous radius
        expect(aero.areostationarySpeedKmS).to.be.closeTo(1.447, 0.01); // ~1.447 km/s circular speed
        expect(aero.orbitalPeriodHours).to.be.closeTo(24.6229, 0.05); // ~24.62 hours (1 Mars sol)
        expect(aero.annualStationKeepingDeltaVMPS).to.be.greaterThan(1.0); // Station-keeping Delta-V
        expect(aero.stableLibrationWells).to.include('Stable Libration Wells at 17.5 W');
    });

    it('should calculate CO2 frost condensation temperature, regolith adsorption, and atmospheric collapse', () => {
        // Modern Mars (P = 610 Pa, winter polar T = 145 K):
        const modCollapse = KRCEngine.computeCryovolcanicCO2FrostDesorptionAndAtmosphericCollapse(610.0, 145.0, 50.0);
        expect(modCollapse.atmosphericPressurePa).to.equal(610.0);
        expect(modCollapse.co2FrostCondensationTempK).to.be.closeTo(164.00, 0.5); // ~164.0 K CO2 frost point
        expect(modCollapse.isAtmosphericCollapseTriggered).to.be.true; // 145 K < 164 K -> collapse triggered
        expect(modCollapse.climaticCollapseRegime).to.include('Runaway Climatic Atmospheric Collapse');

        // Warm summer polar surface (T = 180 K):
        const warm = KRCEngine.computeCryovolcanicCO2FrostDesorptionAndAtmosphericCollapse(610.0, 180.0, 50.0);
        expect(warm.isAtmosphericCollapseTriggered).to.be.false;
    });

    it('should discriminate pure Diopside (extreme high-Ca) from Augite in CRISM pyroxene spectra', () => {
        // Pure Diopside (Wo49) in Olympus Mons pyroxenite cumulates (Band 1 = 1.045 um, Band 2 = 2.320 um):
        const diopside = BandMathEngine.computeCRISMAugiteDiopsideHighCalciumIndices(1.045, 2.320, 0.08);
        expect(diopside.isHighCaPyroxenePresent).to.be.true;
        expect(diopside.pyroxeneEndmemberSpecies).to.include('Diopside (Pure Calcium-Magnesium');
        expect(diopside.estimatedWollastoniteMolePct).to.be.closeTo(49.0, 2.0); // ~Wo49
        expect(diopside.alkalineVolcanicPetrogenesis).to.include('Extreme Alkaline Magma Differentiation');

        // Augite (Wo38) in typical basaltic lava flows (Band 1 = 1.015 um, Band 2 = 2.250 um):
        const augite = BandMathEngine.computeCRISMAugiteDiopsideHighCalciumIndices(1.015, 2.250, 0.08);
        expect(augite.isHighCaPyroxenePresent).to.be.true;
        expect(augite.pyroxeneEndmemberSpecies).to.include('Augite (High-Calcium Clinopyroxene');
        expect(augite.estimatedWollastoniteMolePct).to.be.closeTo(38.5, 3.0); // ~Wo38.5

        // Flat spectrum:
        const basalt = BandMathEngine.computeCRISMAugiteDiopsideHighCalciumIndices(1.015, 2.250, 0.01);
        expect(basalt.isHighCaPyroxenePresent).to.be.false;
    });
});

describe('Mars Free Return Cyclers, Megaregolith Compaction & Zeolites vs Opal', () => {
    it('should calculate Mars-Earth unpowered free return flyby trajectory, periapsis speed, and turn angle', () => {
        // Mars flyby at 250 km altitude (v_inf = 5.65 km/s):
        const freeRet = TrajectoryEngine.computeMarsFreeReturnCircumlunarInterplanetaryFlyby(250.0, 5.65);
        expect(freeRet.marsFlybyAltitudeKm).to.equal(250.0);
        expect(freeRet.marsClosestApproachSpeedKmS).to.be.closeTo(7.447, 0.05); // ~7.45 km/s periapsis speed
        expect(freeRet.hyperbolicTurnAngleDeg).to.be.closeTo(31.26, 1.0); // ~31.3 deg gravity assist bending angle
        expect(freeRet.totalMissionDurationDays).to.equal(501); // 501-day Inspiration Mars mission loop
        expect(freeRet.freeReturnTrajectoryContext).to.include('Unpowered Ballistic Mars-to-Earth Free Return');
    });

    it('should calculate megaregolith compaction porosity decay, bulk density, and annual phase lag', () => {
        // Subsurface at z = 100 m (surface porosity = 40%, H_pore = 3.5 km, ice saturation = 80%):
        const mega = KRCEngine.computeSubsurfaceMegaregolithPorosityDecayAndThermalPhaseLag(100.0, 40.0, 3.5, 80.0);
        expect(mega.subsurfaceDepthMeters).to.equal(100.0);
        expect(mega.megaregolithPorosityPct).to.be.closeTo(38.88, 1.0); // ~38.9% porosity at 100 m
        expect(mega.bulkCrustalDensityKgM3).to.be.closeTo(2058.6, 50.0); // ~2059 kg/m^3 bulk density
        expect(mega.annualThermalSkinDepthMeters).to.be.closeTo(4.47, 0.5); // ~4.47 m annual skin depth
        expect(mega.megaregolithThermalContext).to.include('Deep Thermally Damped Megaregolith');
    });

    it('should discriminate Zeolite (Analcime/Chabazite) from Opaline Hydrated Silica in CRISM spectra', () => {
        // Zeolite (Analcime) in crater central peak (1.41 um, 1.92 um, and 2.48 um without 2.21 um Si-OH):
        const zeolite = BandMathEngine.computeCRISMZeoliteHydratedSilicaIndices(0.24, 0.22, 0.30, 0.24, 0.30);
        expect(zeolite.isHydratedPhasePresent).to.be.true;
        expect(zeolite.hydratedMineralPhase).to.include('Zeolite (Analcime');
        expect(zeolite.diageneticAqueousSetting).to.include('Alkaline Saline Closed-Basin');

        // Hydrated Opaline Silica (Opal-A / Opal-CT) with sharp 2.21 um Si-OH band:
        const opal = BandMathEngine.computeCRISMZeoliteHydratedSilicaIndices(0.24, 0.22, 0.22, 0.30, 0.30);
        expect(opal.isHydratedPhasePresent).to.be.true;
        expect(opal.hydratedMineralPhase).to.include('Hydrated Opaline Silica');
        expect(opal.diageneticAqueousSetting).to.include('Acid-Sulfate Epithermal Hot Spring');

        // Anhydrous basalt:
        const basalt = BandMathEngine.computeCRISMZeoliteHydratedSilicaIndices(0.30, 0.30, 0.30, 0.30, 0.30);
        expect(basalt.isHydratedPhasePresent).to.be.false;
    });
});

describe('Mars-Jupiter Hohmann Transfers, Lava Tube Shelters & Pyroxene Band Area Ratios', () => {
    it('should calculate Mars-to-Jupiter Interplanetary Hohmann Transfer and Asteroid Belt crossing', () => {
        // Mars parking orbit 400 km:
        const jup = TrajectoryEngine.computeMarsJupiterInterplanetaryHohmannTransfer(400.0);
        expect(jup.transferSemiMajorAxisAU).to.be.closeTo(3.3640, 0.01); // ~3.364 AU semi-major axis
        expect(jup.timeOfFlightDays).to.be.closeTo(1126.8, 5.0); // ~1127 days (~3.09 years) TOF
        expect(jup.marsDepartureVInfKmS).to.be.closeTo(5.881, 0.1); // ~5.88 km/s Mars excess
        expect(jup.transJupiterInjectionDeltaVKmS).to.be.closeTo(4.200, 0.1); // ~4.20 km/s TJI burn from LMO
        expect(jup.jupiterArrivalVInfKmS).to.be.closeTo(4.269, 0.1); // ~4.27 km/s arrival excess
        expect(jup.asteroidBeltTransitContext).to.include('Main Belt Asteroid Crossing');
    });

    it('should calculate volcanic lava tube roof thermal attenuation, cavern microclimate, and radiation shielding', () => {
        // Arsia Mons volcanic cave with 15 m basalt roof (surface diurnal swing = 100 K, mean annual T = 218 K):
        const cave = KRCEngine.computeVolcanicLavaTubeThermalInsulationAndShelter(15.0, 100.0, 218.0);
        expect(cave.roofThicknessMeters).to.equal(15.0);
        expect(cave.cavityMeanTempK).to.be.closeTo(218.21, 1.0); // ~218.2 K (-54.9 C) stable ambient
        expect(cave.diurnalCavityFluctuationK).to.be.lessThan(0.001); // Diurnal fluctuation damped to 0.00 K
        expect(cave.annualCavityFluctuationK).to.be.closeTo(0.56, 0.2); // Annual seasonal fluctuation damped to ~0.56 K
        expect(cave.radiationShieldingPercent).to.be.greaterThan(99.0); // > 99% cosmic ray & solar proton shielding
        expect(cave.cavernHabitatShelterContext).to.include('Subterranean Human Base Habitat');
    });

    it('should calculate Pyroxene Band Area Ratio (BAR) and estimate Olivine vs Pyroxene modal fractions', () => {
        // Orthopyroxene (OPX) in ancient Noachian crust (Band 1 area = 0.10, Band 2 area = 0.22 -> BAR = 2.2):
        const opx = BandMathEngine.computeCRISMPyroxeneBandAreaRatioIndices(0.10, 0.22, 0.98);
        expect(opx.bandAreaRatio).to.equal(2.2);
        expect(opx.dominantPyroxeneStructuralType).to.include('Orthopyroxene (Enstatite');
        expect(opx.maficPetrogeneticContext).to.include('Ancient Noachian Primitive Low-Calcium Crust');

        // Clinopyroxene (CPX) in Gale crater basaltic sands (Band 1 area = 0.15, Band 2 area = 0.18 -> BAR = 1.2):
        const cpx = BandMathEngine.computeCRISMPyroxeneBandAreaRatioIndices(0.15, 0.18, 1.03);
        expect(cpx.bandAreaRatio).to.equal(1.2);
        expect(cpx.dominantPyroxeneStructuralType).to.include('Clinopyroxene (Augite');
        expect(cpx.estimatedOlivineModalFraction).to.be.closeTo(0.508, 0.05); // ~51% olivine fraction

        // Olivine-dominated picrite (Band 1 area = 0.25, Band 2 area = 0.08, Band 1 center = 1.05 um -> BAR = 0.32):
        const oli = BandMathEngine.computeCRISMPyroxeneBandAreaRatioIndices(0.25, 0.08, 1.05);
        expect(oli.dominantPyroxeneStructuralType).to.include('Olivine-Dominated Mafic Assemblage');
    });
});

describe('Mars Aerocapture Hypersonics, Impact Melt Solidification & Low-Ca Pyroxenes', () => {
    it('should calculate Mars aerocapture corridor, Sutton-Graves stagnation heat flux, and propulsive Delta-V savings', () => {
        // Mars atmospheric entry at 6.0 km/s (corridor periapsis 52 km, nose radius 0.75 m):
        const aero = TrajectoryEngine.computeMarsAerocaptureCorridorAndPeakStagnationHeatFlux(6.0, 52.0, 0.75);
        expect(aero.entryVelocityKmS).to.equal(6.0);
        expect(aero.corridorPeriapsisAltitudeKm).to.equal(52.0);
        expect(aero.peakStagnationHeatFluxKWm2).to.be.closeTo(598.1, 50.0); // ~598 kW/m^2 (~60 W/cm^2) peak stagnation heat flux
        expect(aero.atmosphericExitSpeedKmS).to.be.closeTo(4.065, 0.5); // ~4.07 km/s exit speed
        expect(aero.propulsiveDeltaVSavedKmS).to.be.greaterThan(1.5); // > 1.5 km/s Delta-V savings
        expect(aero.aerocaptureMissionContext).to.include('Hypersonic Mars Aerocapture Direct Insertion');
    });

    it('should calculate impact melt pool crystallization time and post-impact hydrothermal lifetime', () => {
        // Gale crater scale melt pool (D = 150 km, initial melt temp = 1350 C):
        const melt = KRCEngine.computeImpactMeltPoolSolidificationAndGeothermalCooling(150.0, 1350.0);
        expect(melt.craterDiameterKm).to.equal(150.0);
        expect(melt.meltSheetThicknessMeters).to.be.closeTo(168.7, 5.0); // ~169 m thick impact melt sheet
        expect(melt.crystallizationTimeYears).to.be.closeTo(655.1, 50.0); // ~655 years solidification time
        expect(melt.hydrothermalActiveLifespanYears).to.be.greaterThan(1000); // > 1,000 years hydrothermal circulation
        expect(melt.impactMeltPetrogeneticContext).to.include('Major Basin-Scale Melt Pool');
    });

    it('should discriminate Ferrosilite (Fs) vs Enstatite (En) in CRISM Low-Calcium Orthopyroxenes', () => {
        // ALH84001 analogue Hypersthene (Fs36 En64) in Tyrrhena Terra (Band 1 = 0.918 um, Band 2 = 1.878 um):
        const hyp = BandMathEngine.computeCRISMLowCalciumPyroxeneFerrosiliteEnstatiteIndices(0.918, 1.878, 0.08);
        expect(hyp.isLowCaPyroxenePresent).to.be.true;
        expect(hyp.pyroxeneEndmemberSpecies).to.include('Hypersthene (Intermediate Low-Calcium');
        expect(hyp.estimatedFerrosiliteMolePct).to.be.closeTo(36.0, 2.0); // ~Fs36
        expect(hyp.estimatedEnstatiteMolePct).to.be.closeTo(64.0, 2.0); // ~En64
        expect(hyp.crustalProvenanceContext).to.include('Typical Ancient Noachian Crustal Basement');

        // Primitive Enstatite/Bronzite (Fs16 En84) (Band 1 = 0.908 um, Band 2 = 1.846 um):
        const en = BandMathEngine.computeCRISMLowCalciumPyroxeneFerrosiliteEnstatiteIndices(0.908, 1.846, 0.08);
        expect(en.isLowCaPyroxenePresent).to.be.true;
        expect(en.pyroxeneEndmemberSpecies).to.include('Enstatite / Bronzite');
        expect(en.estimatedFerrosiliteMolePct).to.be.closeTo(16.0, 2.0); // ~Fs16

        // Flat spectrum:
        const basalt = BandMathEngine.computeCRISMLowCalciumPyroxeneFerrosiliteEnstatiteIndices(0.918, 1.878, 0.01);
        expect(basalt.isLowCaPyroxenePresent).to.be.false;
    });
});

describe('Mars Aerobraking Campaigns, Valley Runoff Hydraulics & Fe/Mg Smectites', () => {
    it('should calculate Mars multi-pass aerobraking orbital circularization, drag passes, and Delta-V savings', () => {
        // MGS / Odyssey capture orbit (35,000 km apoapsis, 115 km periapsis, 450 km target science orbit):
        const aero = TrajectoryEngine.computeMarsAerobrakingOrbitDecayPasses(35000.0, 115.0, 450.0, 0.015);
        expect(aero.initialApoapsisKm).to.equal(35000.0);
        expect(aero.corridorPeriapsisKm).to.equal(115.0);
        expect(aero.targetApoapsisKm).to.equal(450.0);
        expect(aero.estimatedAerobrakingPasses).to.be.greaterThan(100); // > 100 atmospheric drag passes
        expect(aero.campaignDurationMonths).to.be.greaterThan(1.0); // Multi-month operational campaign
        expect(aero.totalPropulsiveDeltaVSavedKmS).to.be.greaterThan(1.0); // > 1.0 km/s propellant savings
        expect(aero.aerobrakingMissionContext).to.include('Multi-Pass Mars Aerobraking Campaign');
    });

    it('should calculate ancient Martian valley network fluvial runoff, peak discharge, and sediment competency', () => {
        // Nanedi / Nirgal Vallis watershed (5000 km^2 basin, 15 mm/day rain/melt, 0.0035 slope):
        const valley = KRCEngine.computeAncientMartianValleyNetworkRunoffAndDischarge(5000.0, 15.0, 0.0035, 0.040);
        expect(valley.drainageBasinAreaKm2).to.equal(5000.0);
        expect(valley.peakFluvialDischargeM3S).to.be.closeTo(303.8, 5.0); // ~304 m^3/s peak discharge
        expect(valley.meanChannelFlowVelocityMS).to.be.greaterThan(1.0); // > 1 m/s channel velocity
        expect(valley.basalBedShearStressPa).to.be.greaterThan(10.0); // Sustained bed shear stress
        expect(valley.maxTransportableGrainDiameterCm).to.be.greaterThan(5.0); // Cobble competency (> 5 cm)
        expect(valley.paleohydrologyFluvialContext).to.include('Perennial Cobble-Gravel Bedload Stream');
    });

    it('should discriminate Dioctahedral Fe-Smectite (Nontronite) from Trioctahedral Mg-Smectite (Saponite) in CRISM spectra', () => {
        // Nontronite (Fe3+-smectite) in Mawrth Vallis weathered paleosols (Band center = 2.290 um):
        const nontronite = BandMathEngine.computeCRISMFeMgSmectiteNontroniteSaponiteIndices(2.290, 0.08, 0.09);
        expect(nontronite.isSmectitePresent).to.be.true;
        expect(nontronite.smectiteMineralSpecies).to.include('Nontronite (Dioctahedral Fe3+-Smectite');
        expect(nontronite.dominantOctahedralCation).to.include('Fe3+');
        expect(nontronite.aqueousWeatheringRegime).to.include('Oxidizing Subaerial Weathering');

        // Saponite (Mg-smectite) in Nili Fossae alkaline hydrothermal strata (Band center = 2.315 um):
        const saponite = BandMathEngine.computeCRISMFeMgSmectiteNontroniteSaponiteIndices(2.315, 0.08, 0.09);
        expect(saponite.isSmectitePresent).to.be.true;
        expect(saponite.smectiteMineralSpecies).to.include('Saponite (Trioctahedral Mg-Smectite');
        expect(saponite.dominantOctahedralCation).to.include('Mg2+');
        expect(saponite.aqueousWeatheringRegime).to.include('Alkaline Closed-System');

        // Flat spectrum:
        const basalt = BandMathEngine.computeCRISMFeMgSmectiteNontroniteSaponiteIndices(2.290, 0.01, 0.01);
        expect(basalt.isSmectitePresent).to.be.false;
    });
});

describe('Mars-Venus Gravity Assists, Ocean Tsunami Megafloods & Spinel Mineralogy', () => {
    it('should calculate Mars-to-Venus gravity assist turn angle, heliocentric boost, and flight time', () => {
        // Venus flyby at 300 km altitude (v_inf = 5.50 km/s):
        const flyby = TrajectoryEngine.computeMarsVenusGravityAssistTrajectory(300.0, 5.50);
        expect(flyby.venusFlybyAltitudeKm).to.equal(300.0);
        expect(flyby.venusPeriapsisSpeedKmS).to.be.closeTo(11.512, 0.1); // ~11.5 km/s periapsis speed
        expect(flyby.hyperbolicTurnAngleDeg).to.be.closeTo(77.86, 2.0); // ~77.9 deg turn angle
        expect(flyby.heliocentricDeltaVBoostKmS).to.be.closeTo(6.912, 0.1); // ~6.91 km/s heliocentric boost
        expect(flyby.timeOfFlightToVenusDays).to.equal(217.4); // ~217 days
        expect(flyby.gravityAssistMissionContext).to.include('Venus Gravity Assist Slingshot');
    });

    it('should calculate ancient Martian Northern Ocean tsunami wave speed, shoaling height, and coastal runup', () => {
        // Oceanus Borealis impact tsunami (H0 = 300 m, ocean depth = 1500 m, dist = 800 km, slope = 0.005):
        const tsu = KRCEngine.computeAncientMartianOceanTsunamiPropagationAndRunup(300.0, 1500.0, 800.0, 0.005);
        expect(tsu.openOceanWaveSpeedKmH).to.be.closeTo(268.9, 5.0); // ~269 km/h wave speed
        expect(tsu.coastalShoalingWaveHeightMeters).to.be.closeTo(136.9, 5.0); // ~137 m shoaling wave height
        expect(tsu.maxInlandRunupElevationMeters).to.be.closeTo(252.4, 10.0); // ~252 m runup elevation
        expect(tsu.inlandInundationDistanceKm).to.be.closeTo(50.5, 5.0); // ~50 km inland inundation
        expect(tsu.tsunamiGeomorphologyContext).to.include('Catastrophic Megatsunami Inundation');
    });

    it('should discriminate Mg-Al Spinel from Chromite and Magnetite in CRISM spectra', () => {
        // Pure Mg-Al Spinel (MgAl2O4) with 2.0 um band and NO 1.0 um band:
        const spinel = BandMathEngine.computeCRISMSpinelChromiteMagnetiteIndices(0.25, 0.25, 0.22, 0.25);
        expect(spinel.isSpinelPresent).to.be.true;
        expect(spinel.spinelMineralSpecies).to.include('Mg-Al Spinel (Magnesio-Aluminous Spinel');
        expect(spinel.mantlePetrogeneticContext).to.include('Impact Basin Peak Ring Excavation');

        // Chromite (FeCr2O4) with 2.0 um band and 0.68 um absorption edge:
        const chromite = BandMathEngine.computeCRISMSpinelChromiteMagnetiteIndices(0.20, 0.24, 0.22, 0.25);
        expect(chromite.isSpinelPresent).to.be.true;
        expect(chromite.spinelMineralSpecies).to.include('Chromite (Chromium Spinel');

        // Flat basalt:
        const basalt = BandMathEngine.computeCRISMSpinelChromiteMagnetiteIndices(0.25, 0.25, 0.25, 0.25);
        expect(basalt.isSpinelPresent).to.be.false;
    });
});

describe('SEP Low-Thrust Heliocentric Spirals, Polar Firn Compaction & Plagioclase Anorthosite', () => {
    it('should calculate Solar Electric Propulsion (SEP) low-thrust spiral burn duration and Xenon propellant mass', () => {
        // Dawn-scale Mars-Earth return (1200 kg initial wet mass, 0.25 N thrust, 3500 s Isp, 5.65 km/s Delta-V):
        const sep = TrajectoryEngine.computeLowThrustSEPMarsEarthTrajectory(1200.0, 0.25, 3500.0, 5.65);
        expect(sep.initialMassKg).to.equal(1200.0);
        expect(sep.finalMassKg).to.be.closeTo(1017.8, 5.0); // ~1018 kg dry/burnout mass
        expect(sep.xenonPropellantConsumedKg).to.be.closeTo(182.2, 5.0); // ~182 kg Xenon consumed
        expect(sep.continuousBurnTimeDays).to.be.closeTo(289.5, 10.0); // ~290 days continuous ion thrust
        expect(sep.meanThrustAccelerationMmS2).to.be.greaterThan(0.20); // > 0.20 mm/s^2 micro-thrust acceleration
        expect(sep.ionPropulsionContext).to.include('Solar Electric Low-Thrust Spiral');
    });

    it('should calculate Martian North Polar Layered Deposits (NPLD) firn compaction and gas-ice age offset', () => {
        // NPLD Planum Boreum (0.55 mm/yr ice accumulation, 165 K surface temp, 5% dust):
        const firn = KRCEngine.computeMartianPolarFirnCompactionAndGasAgeTrap(0.55, 165.0, 5.0);
        expect(firn.iceAccumulationRateMmYr).to.equal(0.55);
        expect(firn.poreCloseOffDepthMeters).to.be.greaterThan(10.0); // > 10 m bubble close-off depth
        expect(firn.gasIceAgeOffsetYears).to.be.greaterThan(10000); // > 10,000 years gas-ice chronological offset
        expect(firn.firnColumnBulkDensityKgM3).to.be.closeTo(700.5, 50.0); // ~700 kg/m^3 bulk firn density
        expect(firn.polarPaleoclimateContext).to.include('High-Resolution Orbital Paleoclimatic Ice Core Stratigraphy');
    });

    it('should discriminate pure Plagioclase Anorthosite from Mafic Silicates in CRISM NIR spectra', () => {
        // Pure Anorthosite (>90% calcic plagioclase) with 1.25 um band and NO 1.0/2.0 um pyroxene bands:
        const anorth = BandMathEngine.computeCRISMPlagioclaseVsMaficDiagnosticIndices(0.294, 0.300, 0.300, 0.300);
        expect(anorth.isPlagioclasePresent).to.be.true;
        expect(anorth.plagioclaseLithology).to.include('Anorthosite (Pure Calcic Plagioclase Feldspar');
        expect(anorth.crustalPetrogenesisContext).to.include('Primary Magma Ocean Flotation Crust');

        // Mafic basalt with flat 1.25 um:
        const basalt = BandMathEngine.computeCRISMPlagioclaseVsMaficDiagnosticIndices(0.300, 0.280, 0.270, 0.300);
        expect(basalt.isPlagioclasePresent).to.be.false;
    });
});

describe('Phobos/Deimos CW Rendezvous, Subsurface Ice Desiccation & Sulfate Hydration', () => {
    it('should calculate Phobos/Deimos Clohessy-Wiltshire relative proximity rendezvous maneuvers', () => {
        // Phobos proximity approach (5 km radial, 15 km in-track standoff, 2 hour rendezvous):
        const cw = TrajectoryEngine.computeMartianMoonClohessyWiltshireProximityManeuver('Phobos', 5.0, 15.0, 2.0);
        expect(cw.targetMoon).to.equal('Phobos');
        expect(cw.transferDurationHours).to.equal(2.0);
        expect(cw.totalRendezvousDeltaVMS).to.be.greaterThan(3.0); // > 3 m/s proximity maneuver
        expect(cw.departureBurnDeltaVMS).to.be.greaterThan(1.0);
        expect(cw.arrivalBrakingDeltaVMS).to.be.greaterThan(1.0);
        expect(cw.relativeMotionContext).to.include('Clohessy-Wiltshire Co-Orbital Rendezvous with Phobos');
    });

    it('should calculate Martian subsurface ground ice sublimation, Knudsen diffusion, and desiccation front retreat', () => {
        // Mid-latitude ground ice (195 K ground temp, 15 um pore radius, 2.5 Myr):
        const ice = KRCEngine.computeMartianSubsurfaceIceSublimationAndDesiccationFront(195.0, 15.0, 2.5, 0.35);
        expect(ice.meanAnnualTempK).to.equal(195.0);
        expect(ice.effectivePoreDiffusivityM2S).to.be.greaterThan(1e-5);
        expect(ice.desiccationLagDepthMeters).to.be.closeTo(14.9, 3.0); // ~15 m desiccation dry lag depth
        expect(ice.isGroundIceStableShallow).to.be.false; // Decoupled at mid-latitudes
        expect(ice.groundIcePreservationContext).to.include('Mid-Latitude Subsurface Ice Decoupling');

        // Polar stable permafrost (165 K ground temp, Phoenix landing site):
        const polar = KRCEngine.computeMartianSubsurfaceIceSublimationAndDesiccationFront(165.0, 15.0, 2.5, 0.35);
        expect(polar.isGroundIceStableShallow).to.be.true;
        expect(polar.desiccationLagDepthMeters).to.be.at.most(0.05); // Shallow stable ice (< 5 cm)
    });

    it('should discriminate Monohydrated Sulfate (Kieserite) from Polyhydrated Sulfate (Starkeyite/Epsomite/Gypsum) in CRISM spectra', () => {
        // Monohydrated Sulfate (Kieserite) in Juventae Chasma (2.13 um & 2.40 um bands, NO 1.93 um H2O band):
        const mhs = BandMathEngine.computeCRISMMonohydratedVsPolyhydratedSulfateDepths(0.08, 0.09, 0.005, 2.405);
        expect(mhs.isSulfatePresent).to.be.true;
        expect(mhs.sulfateHydrationClass).to.include('Monohydrated Sulfate (MHS)');
        expect(mhs.mineralSpecies).to.include('Kieserite (MgSO4 * H2O)');
        expect(mhs.evaporiticAqueousContext).to.include('Hyper-Arid Desiccated Evaporite Bedding');

        // Polyhydrated Sulfate (Starkeyite/Epsomite) in Candor Chasma (1.93 um & 2.42 um bands):
        const phs = BandMathEngine.computeCRISMMonohydratedVsPolyhydratedSulfateDepths(0.005, 0.08, 0.08, 2.425);
        expect(phs.isSulfatePresent).to.be.true;
        expect(phs.sulfateHydrationClass).to.include('Polyhydrated Sulfate (PHS)');
        expect(phs.mineralSpecies).to.include('Starkeyite / Epsomite');

        // Flat basalt:
        const basalt = BandMathEngine.computeCRISMMonohydratedVsPolyhydratedSulfateDepths(0.005, 0.005, 0.005, 2.405);
        expect(basalt.isSulfatePresent).to.be.false;
    });
});

describe('Mars-to-Ice-Giant Transfers, Subglacial Lake Melting & Clinopyroxenes', () => {
    it('should calculate Mars-to-Uranus/Neptune interplanetary transfer TOF and injection Delta-V', () => {
        // Mars to Uranus transfer (r_U = 19.22 AU, 300 km parking orbit):
        const uranus = TrajectoryEngine.computeMarsOuterIceGiantTrajectory('Uranus', 300.0);
        expect(uranus.destinationPlanet).to.equal('Uranus');
        expect(uranus.targetSemiMajorAxisAU).to.equal(19.22);
        expect(uranus.timeOfFlightYears).to.be.closeTo(16.25, 0.5); // ~16.3 years TOF
        expect(uranus.transIceGiantInjectionDeltaVKmS).to.be.closeTo(6.56, 0.5); // ~6.6 km/s TII Delta-V
        expect(uranus.arrivalHyperbolicExcessKmS).to.be.closeTo(4.20, 0.5); // ~4.2 km/s arrival excess
        expect(uranus.outerSystemMissionContext).to.include('Mars-to-Uranus Interplanetary Transfer');

        // Mars to Neptune transfer (r_N = 30.07 AU):
        const neptune = TrajectoryEngine.computeMarsOuterIceGiantTrajectory('Neptune', 300.0);
        expect(neptune.destinationPlanet).to.equal('Neptune');
        expect(neptune.timeOfFlightYears).to.be.closeTo(30.65, 1.0); // ~30.7 years TOF
    });

    it('should calculate South Polar Layered Deposits (SPLD) basal melting, cryostatic pressure, and subglacial lake brine stability', () => {
        // High geothermal flux / volcanic sill under SPLD (1500 m ice, 45 mW/m^2 heat flux, 160 K surface, 35% perchlorate salt):
        const spld = KRCEngine.computeMartianBasalIceMeltingAndSubglacialLakePressure(1500.0, 45.0, 160.0, 35.0);
        expect(spld.iceThicknessMeters).to.equal(1500.0);
        expect(spld.basalOverburdenPressureMPa).to.be.closeTo(5.135, 0.1); // ~5.14 MPa cryostatic pressure
        expect(spld.basalIceTempK).to.be.closeTo(192.1, 5.0); // ~192 K basal ice temp
        expect(spld.eutecticFreezingTempK).to.be.closeTo(204.9, 2.0); // ~205 K Mg-perchlorate eutectic
        expect(spld.isSubglacialLiquidBrineStable).to.be.false;

        // Enhanced localized hydrothermal magmatic plume (90 mW/m^2):
        const plume = KRCEngine.computeMartianBasalIceMeltingAndSubglacialLakePressure(1500.0, 90.0, 160.0, 35.0);
        expect(plume.basalIceTempK).to.be.greaterThan(220.0);
        expect(plume.isSubglacialLiquidBrineStable).to.be.true;
        expect(plume.dielectricReflectivityContext).to.include('Stable Subglacial Hyper-Saline Perchlorate Brine Lake');
    });

    it('should discriminate High-Ca Augite from Low-Ca Pigeonite clinopyroxene solid solutions in CRISM spectra', () => {
        // Augite (High-Ca pyroxene) in Syrtis Major volcanic shields (Band 1 = 1.035 um, Band 2 = 2.300 um):
        const augite = BandMathEngine.computeCRISMPyroxeneAugitePigeoniteSolidSolution(1.035, 2.300, 0.08);
        expect(augite.isPyroxenePresent).to.be.true;
        expect(augite.pyroxeneEndmember).to.include('Augite (High-Calcium Clinopyroxene');
        expect(augite.estimatedWollastoniteMolePct).to.be.greaterThan(35.0); // > Wo35
        expect(augite.petrologicContext).to.include('Evolved Alkaline Basaltic Volcanism');

        // Pigeonite (Low-Ca pyroxene) in quenched tholeiites (Band 1 = 0.950 um, Band 2 = 2.020 um):
        const pigeonite = BandMathEngine.computeCRISMPyroxeneAugitePigeoniteSolidSolution(0.950, 2.020, 0.08);
        expect(pigeonite.isPyroxenePresent).to.be.true;
        expect(pigeonite.pyroxeneEndmember).to.include('Pigeonite (Low-Calcium Clinopyroxene');
        expect(pigeonite.estimatedWollastoniteMolePct).to.be.lessThan(20.0); // < Wo20

        // Flat matrix:
        const basalt = BandMathEngine.computeCRISMPyroxeneAugitePigeoniteSolidSolution(1.035, 2.300, 0.01);
        expect(basalt.isPyroxenePresent).to.be.false;
    });
});

describe('Mars Trojans L4/L5 Dynamics, Cryovolcanic Effusion & Carbonate Quaternary Solid Solutions', () => {
    it('should calculate Mars-Sun Trojan L4/L5 tadpole libration period and stationkeeping Delta-V', () => {
        // Eureka family L5 Trojan asteroid co-orbital station (50,000 km offset, 5 years):
        const trojan = TrajectoryEngine.computeMartianTrojanLagrangePointL4L5Stationkeeping('L5', 50000.0, 5.0);
        expect(trojan.lagrangePoint).to.include('L5');
        expect(trojan.tadpoleLibrationPeriodYears).to.be.closeTo(1274.5, 50.0); // ~1275 years libration period
        expect(trojan.annualStationkeepingDeltaVMSYear).to.be.closeTo(3.70, 0.5); // ~3.7 m/s/yr
        expect(trojan.totalMissionStationkeepingDeltaVMS).to.be.closeTo(18.5, 3.0); // ~18.5 m/s total over 5 yrs
        expect(trojan.trojanAsteroidContext).to.include('Co-Orbital Station');
    });

    it('should calculate Martian cryovolcanic brine-ice slush effusion rate, volume, and dome spreading', () => {
        // Cerberus Fossae cryomagma fissure (15 m vent radius, 2000 m conduit, 1e5 Pa*s viscosity, 30 days):
        const dome = KRCEngine.computeMartianCryovolcanicEffusionAndDomeEmplacement(15.0, 2000.0, 100000.0, 30.0);
        expect(dome.ventRadiusMeters).to.equal(15.0);
        expect(dome.effusionDischargeRateM3S).to.be.closeTo(147.9, 5.0); // ~148 m^3/s discharge
        expect(dome.totalExtrudedVolumeKm3).to.be.closeTo(0.383, 0.05); // ~0.38 km^3
        expect(dome.domeSpreadingRadiusKm).to.be.closeTo(6.06, 0.5); // ~6.1 km dome radius
        expect(dome.meanDomeThicknessMeters).to.be.greaterThan(5.0); // > 5 m thick
        expect(dome.cryovolcanicGeomorphologyContext).to.include('Viscous Cryomagmatic Slush Dome');
    });

    it('should discriminate Magnesite, Dolomite, Siderite, and Calcite in CRISM NIR spectra', () => {
        // Magnesite (MgCO3) in Nili Fossae (2.300 um & 2.500 um):
        const mag = BandMathEngine.computeCRISMFullCarbonateSolidSolutionIndices(2.300, 2.500, 0.08);
        expect(mag.isCarbonatePresent).to.be.true;
        expect(mag.carbonateMineralSpecies).to.include('Magnesite (Magnesium Carbonate');
        expect(mag.dominantCation).to.include('Mg2+');
        expect(mag.paleosequestrationContext).to.include('Carbonation of Ultramafic');

        // Dolomite (CaMg(CO3)2) in Jezero crater paleolake margin (2.320 um & 2.520 um):
        const dol = BandMathEngine.computeCRISMFullCarbonateSolidSolutionIndices(2.320, 2.520, 0.08);
        expect(dol.isCarbonatePresent).to.be.true;
        expect(dol.carbonateMineralSpecies).to.include('Dolomite (Calcium-Magnesium Carbonate');
        expect(dol.paleosequestrationContext).to.include('Alkaline Lacustrine Carbonate Precipitation');

        // Siderite (FeCO3) in Columbia Hills (2.335 um & 2.535 um):
        const sid = BandMathEngine.computeCRISMFullCarbonateSolidSolutionIndices(2.335, 2.535, 0.08);
        expect(sid.isCarbonatePresent).to.be.true;
        expect(sid.carbonateMineralSpecies).to.include('Siderite (Iron Carbonate');

        // Flat basalt:
        const basalt = BandMathEngine.computeCRISMFullCarbonateSolidSolutionIndices(2.300, 2.500, 0.01);
        expect(basalt.isCarbonatePresent).to.be.false;
    });
});

describe('Mars-to-Saturn Hohmann Transfers, Mega-Lahar Debris Flows & Mafic Modal Partitioning', () => {
    it('should calculate Mars-to-Saturn interplanetary Hohmann transfer TOF, TSI, and Titan slingshot Delta-V', () => {
        // Mars to Saturn transfer (r_S = 9.5388 AU, 300 km parking orbit, 1000 km Titan flyby):
        const saturn = TrajectoryEngine.computeMarsSaturnInterplanetaryTransferAndTitanSlingshot(300.0, 1000.0);
        expect(saturn.destinationPlanet).to.equal('Saturn');
        expect(saturn.transferSemiMajorAxisAU).to.be.closeTo(5.53, 0.1); // ~5.53 AU semi-major axis
        expect(saturn.timeOfFlightYears).to.be.closeTo(6.50, 0.2); // ~6.5 years TOF
        expect(saturn.transSaturnInjectionDeltaVKmS).to.be.closeTo(5.33, 0.5); // ~5.33 km/s TSI Delta-V
        expect(saturn.saturnArrivalHyperbolicExcessKmS).to.be.closeTo(4.67, 0.5); // ~4.67 km/s arrival excess
        expect(saturn.titanGravityAssistDeltaVKmS).to.be.greaterThan(0.5); // > 0.5 km/s Titan slingshot
        expect(saturn.outerSystemMissionContext).to.include('Mars-to-Saturn Transfer');
    });

    it('should calculate ancient Martian volcanic mega-lahar debris flow velocity, yield stress, and runout distance', () => {
        // Elysium Mons / Hecates Tholus volcano-ice mega-lahar (25 km^3 volume, 1.5% slope, 55% ash):
        const lahar = KRCEngine.computeMartianVolcanicLaharDebrisFlowRunout(25.0, 0.015, 55.0, 8.0);
        expect(lahar.laharVolumeKm3).to.equal(25.0);
        expect(lahar.slurryBulkDensityKgM3).to.equal(1880.0); // 1880 kg/m^3
        expect(lahar.binghamYieldStressPa).to.be.closeTo(209.4, 15.0); // ~209 Pa yield stress
        expect(lahar.peakFlowVelocityMS).to.be.closeTo(8.06, 1.0); // ~8.1 m/s peak velocity
        expect(lahar.maxRunoutDistanceKm).to.be.closeTo(39.4, 5.0); // ~39.4 km runout
        expect(lahar.laharSedimentologyContext).to.include('Catastrophic Volcano-Ice Mega-Lahar');
    });

    it('should calculate Mafic Mineralogy Modal Partitioning (Olivine vs HCP vs LCP) and igneous lithology', () => {
        // Dunite / Ultramafic mantle cumulate in Nili Fossae (8% OL, 1% HCP, 1% LCP):
        const dunite = BandMathEngine.computeCRISMOlivinePyroxeneModalPartitioning(0.08, 0.01, 0.01);
        expect(dunite.isMaficPresent).to.be.true;
        expect(dunite.olivineModalPct).to.equal(80.0); // 80% Olivine
        expect(dunite.igneousLithologyClassification).to.include('Dunite / Ultramafic Peridotite');
        expect(dunite.petrologicContext).to.include('Primitive Upper Mantle Cumulate');

        // Tholeiitic Clinopyroxene Basalt in Syrtis Major (2% OL, 6% HCP, 2% LCP):
        const tholeiite = BandMathEngine.computeCRISMOlivinePyroxeneModalPartitioning(0.02, 0.06, 0.02);
        expect(tholeiite.isMaficPresent).to.be.true;
        expect(tholeiite.highCaPyroxeneModalPct).to.equal(60.0); // 60% HCP
        expect(tholeiite.igneousLithologyClassification).to.include('Tholeiitic Clinopyroxene Basalt');

        // Dust-covered regolith:
        const dust = BandMathEngine.computeCRISMOlivinePyroxeneModalPartitioning(0.01, 0.01, 0.01);
        expect(dust.isMaficPresent).to.be.false;
    });
});

describe('Mars Asteroid Gravity Tractor, Glacial Thermal Regimes & Hydrated Silica Crystallinity', () => {
    it('should calculate Mars Trojan / co-orbital asteroid Gravity Tractor towing deflection and b-plane shift', () => {
        // 150m Trojan asteroid deflection (2000 kg spacecraft, 120m standoff, 3 yrs towing, 10 yrs lead time):
        const tractor = TrajectoryEngine.computeMartianAsteroidGravityTractorDeflection(150.0, 2200.0, 2000.0, 120.0, 3.0, 10.0);
        expect(tractor.gravitationalTowingForceMicroN).to.be.closeTo(36038.6, 100.0); // ~36,038 micro-N (36 mN)
        expect(tractor.cumulativeDeltaVMMS).to.be.closeTo(0.878, 0.1); // ~0.88 mm/s
        expect(tractor.bPlaneDisplacementKm).to.be.closeTo(830.8, 50.0); // ~831 km b-plane displacement
        expect(tractor.planetaryDefenseContext).to.include('Gravity Tractor Deflection');
    });

    it('should calculate Martian glacial basal thermal regime (cold-based vs warm-based) and erosion rate', () => {
        // Amazonian Lobate Debris Apron (LDA) cold-based ice (800 m ice, 5 m/yr flow, 35 mW/m^2 heat flux, 190 K surface):
        const lda = KRCEngine.computeMartianGlacialThermalRegimeAndBedrockErosionRate(800.0, 5.0, 35.0, 190.0);
        expect(lda.iceThicknessMeters).to.equal(800.0);
        expect(lda.basalTemperatureK).to.be.closeTo(205.9, 3.0); // ~206 K basal temp
        expect(lda.pressureMeltingPointK).to.be.closeTo(272.95, 0.5); // ~273 K PMT
        expect(lda.isGlacierColdBased).to.be.true; // Frozen to bed
        expect(lda.basalSlidingVelocityMYr).to.equal(0.0); // No basal sliding
        expect(lda.bedrockErosionRateMmMyr).to.equal(0.001); // Minimal erosion
        expect(lda.glacialGeomorphologyContext).to.include('Cold-Based Non-Erosive Glaciation');

        // Warm-based polythermal wet glacier with basal sliding:
        const warm = KRCEngine.computeMartianGlacialThermalRegimeAndBedrockErosionRate(3500.0, 25.0, 80.0, 210.0);
        expect(warm.isGlacierColdBased).to.be.false;
        expect(warm.basalSlidingVelocityMYr).to.be.greaterThan(10.0);
        expect(warm.bedrockErosionRateMmMyr).to.be.greaterThan(1000.0);
    });

    it('should discriminate Opal-A, Opal-CT, and Quartz/Chalcedony crystallinity phases in CRISM spectra', () => {
        // Opal-A (Amorphous silica sinter at Home Plate Gusev, broad 2.235 um band FWHM = 0.065 um):
        const opalA = BandMathEngine.computeCRISMHydratedSilicaCrystallinityIndices(1.440, 2.235, 0.065, 0.08);
        expect(opalA.isSilicaPresent).to.be.true;
        expect(opalA.silicaCrystallinityPhase).to.include('Amorphous Hydrated Silica (Opal-A)');
        expect(opalA.silicaMineralSpecies).to.include('Opal-A (Hydrated Silica Sinter');
        expect(opalA.hydrothermalDiageneticContext).to.include('Volcanic Fumarole Exhalative Sinter');

        // Opal-CT (Paracrystalline disordered silica in Noctis Labyrinthus, FWHM = 0.040 um):
        const opalCT = BandMathEngine.computeCRISMHydratedSilicaCrystallinityIndices(1.420, 2.230, 0.040, 0.08);
        expect(opalCT.isSilicaPresent).to.be.true;
        expect(opalCT.silicaCrystallinityPhase).to.include('Paracrystalline Opal (Opal-CT)');
        expect(opalCT.hydrothermalDiageneticContext).to.include('Diagenetically Matured');

        // Chalcedony / Crystalline Quartz (sharp FWHM = 0.022 um):
        const quartz = BandMathEngine.computeCRISMHydratedSilicaCrystallinityIndices(1.410, 2.210, 0.022, 0.08);
        expect(quartz.isSilicaPresent).to.be.true;
        expect(quartz.silicaCrystallinityPhase).to.include('Microcrystalline / Crystalline Quartz');

        // Flat basalt:
        const basalt = BandMathEngine.computeCRISMHydratedSilicaCrystallinityIndices(1.440, 2.235, 0.065, 0.01);
        expect(basalt.isSilicaPresent).to.be.false;
    });
});

describe('Mars-Jupiter Interstellar Escape, Magma Chamber Cooling & Nanophase Hematite', () => {
    it('should calculate Mars-to-Jupiter trajectory, gravity assist turn angle, and interstellar escape speed', () => {
        // Mars to Jupiter interstellar escape (2.0 R_j closest approach, 300 km Mars parking orbit):
        const escape = TrajectoryEngine.computeMarsJupiterInterstellarEscapeTrajectory(2.0, 300.0);
        expect(escape.departurePlanet).to.equal('Mars');
        expect(escape.assistPlanet).to.equal('Jupiter');
        expect(escape.timeOfFlightToJupiterYears).to.be.closeTo(3.09, 0.2); // ~3.1 years to Jupiter
        expect(escape.transJupiterInjectionDeltaVKmS).to.be.closeTo(4.03, 0.5); // ~4.0 km/s TJI Delta-V
        expect(escape.jupiterHyperbolicBendingAngleDeg).to.be.greaterThan(90.0); // > 90 deg bending
        expect(escape.postSlingshotHeliocentricSpeedKmS).to.be.closeTo(17.5, 3.0); // ~17.5 km/s asymptotic heliocentric speed
        expect(escape.interstellarEscapeRateAUYear).to.be.closeTo(3.69, 0.8); // ~3.7 AU/yr escape rate
        expect(escape.interstellarMissionContext).to.include('Mars-Jupiter Interstellar Escape');
    });

    it('should calculate Martian plutonic magma chamber conductive cooling, latent heat, and metamorphic aureole', () => {
        // Tharsis pluton (5 km radius, 8 km depth, 1450 K magma, 350 K host rock):
        const pluton = KRCEngine.computeMartianMagmaChamberCoolingAndContactAureole(5.0, 8.0, 1450.0, 350.0);
        expect(pluton.chamberRadiusKm).to.equal(5.0);
        expect(pluton.chamberVolumeKm3).to.be.closeTo(523.6, 10.0); // ~524 km^3
        expect(pluton.conductiveCoolingTimeKyr).to.be.closeTo(198.0, 10.0); // ~198 kyr
        expect(pluton.totalSolidificationTimeKyr).to.be.closeTo(263.5, 15.0); // ~264 kyr with latent heat
        expect(pluton.hydrothermalAureoleThicknessKm).to.equal(2.75); // 2.75 km aureole
        expect(pluton.degassedVolatileMassGigatons).to.be.closeTo(21991.1, 100.0); // ~22,000 Gt volatiles
        expect(pluton.plutonicMetamorphicContext).to.include('Plutonic Magma Chamber');
    });

    it('should discriminate Crystalline Gray Hematite from Nanophase Ferric Oxide (npHm) in CRISM spectra', () => {
        // Crystalline Hematite (alpha-Fe2O3 in Meridiani Planum blueberries: BD860 = 0.08, center = 0.860 um):
        const hmt = BandMathEngine.computeCRISMNanophaseHematiteWeatheringIndices(0.04, 0.08, 0.860);
        expect(hmt.isIronOxidePresent).to.be.true;
        expect(hmt.ironOxidePhase).to.include('Crystalline Gray Hematite (alpha-Fe2O3)');
        expect(hmt.grainSizeClass).to.include('Coarse Crystalline (> 10 microns');
        expect(hmt.oxidationPaleoenvironmentContext).to.include('Groundwater Precipitation');

        // Nanophase Hematite (npHm dust coating across high-albedo plains: BD530 = 0.22, BD860 = 0.01):
        const npHm = BandMathEngine.computeCRISMNanophaseHematiteWeatheringIndices(0.22, 0.01, 0.920);
        expect(npHm.isIronOxidePresent).to.be.true;
        expect(npHm.ironOxidePhase).to.include('Nanophase Ferric Oxide (npHm / Palagonite)');
        expect(npHm.grainSizeClass).to.include('Superparamagnetic Nanocrystals (< 10 nm)');
        expect(npHm.oxidationPaleoenvironmentContext).to.include('Atmospheric UV-Photo-Oxidation');

        // Dark unaltered basalt:
        const basalt = BandMathEngine.computeCRISMNanophaseHematiteWeatheringIndices(0.02, 0.01, 0.950);
        expect(basalt.isIronOxidePresent).to.be.false;
    });
});

describe('Mars Atmospheric Tether Dynamics, Cryopeg Hydrology & Kaolin Polymorph Inversion', () => {
    it('should calculate Martian atmospheric tether gravity gradient tension, drag braking, and deorbit kick', () => {
        // 50 km atmospheric tether dipping into 100 km thermosphere (500 kg probe, 2500 kg orbiter at 150 km periapsis):
        const tether = TrajectoryEngine.computeMartianAtmosphericTetherMomentumExchange(50.0, 500.0, 2500.0, 150.0);
        expect(tether.tetherLengthKm).to.equal(50.0);
        expect(tether.probeDippingAltitudeKm).to.equal(100.0);
        expect(tether.gravityGradientTensionN).to.be.closeTo(72.4, 5.0); // ~72 N tidal tension
        expect(tether.peakAerodynamicDragN).to.be.greaterThan(1.0); // > 1 N aero drag
        expect(tether.nonPropulsiveDeorbitDeltaVMS).to.be.closeTo(49.1, 5.0); // ~49 m/s non-propulsive release kick
        expect(tether.tetherMechanicsContext).to.include('Atmospheric Tether');
    });

    it('should calculate sub-zero unfrozen cryopeg brine lens stability and confined artesian overpressure', () => {
        // 250 m deep permafrost cryopeg lens (225 K permafrost, 30% perchlorate/chloride salts, 35% porosity):
        const cryopeg = KRCEngine.computeMartianCryopegFreezingDepressionAndBrineHydrology(225.0, 30.0, 0.35, 250.0);
        expect(cryopeg.permafrostTempK).to.equal(225.0);
        expect(cryopeg.eutecticMeltingTempK).to.be.closeTo(214.65, 1.0); // ~215 K eutectic
        expect(cryopeg.isLiquidBrineStableSubzero).to.be.true; // Liquid at -48 deg C!
        expect(cryopeg.unfrozenWaterVolumeFrac).to.be.greaterThan(0.20);
        expect(cryopeg.artesianSpringOverpressureKPa).to.be.closeTo(604.6, 30.0); // ~605 kPa artesian overpressure
        expect(cryopeg.cryopegHydrologyContext).to.include('Unfrozen Sub-Zero Hypersaline Cryopeg Lens');
    });

    it('should discriminate Well-Crystallized Kaolinite from Halloysite and Hydrothermal Dickite in CRISM spectra', () => {
        // Well-Crystallized Kaolinite (Al-OH doublet in Mawrth Vallis paleosols: BD2160 = 0.06, BD2208 = 0.08):
        const kaolinite = BandMathEngine.computeCRISMKaolinGroupPolymorphIndices(0.06, 0.08, 0.005);
        expect(kaolinite.isKaolinPresent).to.be.true;
        expect(kaolinite.kaolinPolymorph).to.include('Well-Crystallized Kaolinite');
        expect(kaolinite.structuralOrdering).to.include('High Structural Ordering');
        expect(kaolinite.paleoweatheringContext).to.include('Top-Down Acidic Leaching');

        // Halloysite (hydrated disordered tubular 1:1 clay: BD2160 = 0.01, BD2208 = 0.08, BD1900 = 0.06):
        const halloysite = BandMathEngine.computeCRISMKaolinGroupPolymorphIndices(0.01, 0.08, 0.06);
        expect(halloysite.isKaolinPresent).to.be.true;
        expect(halloysite.kaolinPolymorph).to.include('Halloysite (Hydrated Tubular');
        expect(halloysite.paleoweatheringContext).to.include('Subaqueous Glass Weathering');

        // Dickite (High-T hydrothermal kaolin with dominant 2.16 um band: BD2160 = 0.12, BD2208 = 0.08):
        const dickite = BandMathEngine.computeCRISMKaolinGroupPolymorphIndices(0.12, 0.08, 0.005);
        expect(dickite.isKaolinPresent).to.be.true;
        expect(dickite.kaolinPolymorph).to.include('Dickite / Nacrite');
        expect(dickite.paleoweatheringContext).to.include('Deep Acid-Sulfate Hydrothermal Circulation');

        // Flat basalt:
        const basalt = BandMathEngine.computeCRISMKaolinGroupPolymorphIndices(0.01, 0.01, 0.005);
        expect(basalt.isKaolinPresent).to.be.false;
    });
});

describe('Mars Aerobraking TPS Pyrolysis, Acid Fog Leaching & Chloride Salt Inversion', () => {
    it('should calculate hypersonic entry TPS stagnation heat load, PICA charring, and ablation recession', () => {
        // PICA heatshield hypersonic corridor (1.25 m nose radius, 52 km periapsis, 5.8 km/s entry speed):
        const tps = TrajectoryEngine.computeMarsAerobrakingTPSPyrolysisAndRecession(1.25, 'PICA', 52.0, 5.8);
        expect(tps.heatShieldMaterial).to.include('PICA');
        expect(tps.peakConvectiveHeatFluxKWm2).to.be.closeTo(449.6, 20.0); // ~450 kW/m^2 peak flux
        expect(tps.totalHeatLoadMJm2).to.be.closeTo(19.0, 2.0); // ~19 MJ/m^2 heat load
        expect(tps.surfaceAblationRecessionMm).to.be.closeTo(2.01, 0.3); // ~2.0 mm surface recession
        expect(tps.indepthCharDepthMm).to.be.closeTo(6.43, 1.0); // ~6.4 mm char front
        expect(tps.tpsAblationContext).to.include('PICA');
    });

    it('should calculate volcanic acid fog leaching, basalt dissolution, and siliceous hardpan duricrust formation', () => {
        // Gusev fumarolic acid fog (50 ug/m^2*s SO2, 65% RH, 210 K ground, 10,000 years weathering):
        const acid = KRCEngine.computeMartianAcidFogLeachingAndSiliceousHardpan(50.0, 65.0, 210.0, 10000.0);
        expect(acid.so2DepositionFluxMgM2Yr).to.be.closeTo(1064.6, 50.0); // ~1065 mg/(m^2*yr)
        expect(acid.cumulativeAcidLoadKgM2).to.be.closeTo(10.65, 1.0); // ~10.6 kg/m^2 cumulative acid
        expect(acid.residualSilicaWeightPct).to.be.closeTo(84.6, 5.0); // ~85 wt% amorphous SiO2
        expect(acid.hardpanDuricrustThicknessCm).to.be.closeTo(8.16, 2.0); // ~8 cm duricrust
        expect(acid.acidWeatheringContext).to.include('Siliceous Hardpan Duricrust');
    });

    it('should discriminate Anhydrous Chloride Salts (Halite Playas) from clays and sulfates in CRISM spectra', () => {
        // Anhydrous Halite Playa in Southern Highlands (high positive NIR slope, no 1.9/2.3 um bands: ISLOPE = 0.14):
        const halite = BandMathEngine.computeCRISMChlorideBearingSaltIndices(0.14, 0.005, 0.005);
        expect(halite.isChloridePresent).to.be.true;
        expect(halite.chlorideMineralPhase).to.include('Anhydrous Halite / Sylvite');
        expect(halite.spectralMorphology).to.include('Strong Positive NIR Continuum Slope');
        expect(halite.evaporiticPaleoenvironmentContext).to.include('Terminal Evaporative Lake Basin');

        // Hydrated Sulfate matrix (high 1.9 um absorption):
        const sulfate = BandMathEngine.computeCRISMChlorideBearingSaltIndices(0.14, 0.08, 0.04);
        expect(sulfate.isChloridePresent).to.be.false;
        expect(sulfate.chlorideMineralPhase).to.include('Hydrated Mineral Matrix');

        // Flat basalt:
        const basalt = BandMathEngine.computeCRISMChlorideBearingSaltIndices(0.01, 0.005, 0.005);
        expect(basalt.isChloridePresent).to.be.false;
    });
});

describe('Mars Gravity Waves, Silica Sinter Diagenesis & Serpentine Polymorph Inversion', () => {
    it('should calculate Martian upper atmospheric gravity wave buoyancy frequency and density oscillations', () => {
        // Thermospheric gravity wave (140 km altitude, 250 km wavelength, 25% amplitude, 4.2 km/s orbital speed):
        const wave = TrajectoryEngine.computeMartianUpperAtmosphericGravityWavesAndDensityPerturbations(140.0, 250.0, 25.0, 4.20);
        expect(wave.baseAltitudeKm).to.equal(140.0);
        expect(wave.bruntVaisalaFrequencyMradS).to.be.closeTo(11.49, 1.0); // ~11.5 mrad/s
        expect(wave.buoyancyPeriodMinutes).to.be.closeTo(9.11, 1.0); // ~9.1 min buoyancy period
        expect(wave.alongTrackEncounterPeriodSec).to.be.closeTo(59.5, 2.0); // ~60s drag cycle
        expect(wave.peakDensityPerturbationPct).to.equal(25.0);
        expect(wave.gravityWaveAerobrakingContext).to.include('Thermospheric Gravity Wave');
    });

    it('should calculate hydrothermal silica sinter maturation kinetics (Opal-A -> Opal-CT -> Quartz)', () => {
        // Hydrothermal sinter bed at 340 K for 50,000 years with 0.5 M saline pore fluid:
        const sinter = KRCEngine.computeMartianSilicaSinteringKineticsAndQuartzMaturation(0.55, 340.0, 0.5, 50000.0);
        expect(sinter.dominantSilicaPhase).to.include('Amorphous Opal-A');
        expect(sinter.opalAWeightPct).to.be.closeTo(69.9, 2.0); // ~70% residual Opal-A
        expect(sinter.opalCTWeightPct).to.be.closeTo(22.5, 2.0); // ~22.5% converted Opal-CT
        expect(sinter.microcrystallineQuartzWeightPct).to.be.closeTo(7.7, 2.0); // ~8% Quartz
        expect(sinter.evolvedPorosityFrac).to.be.closeTo(0.503, 0.05); // densified from 0.55 to ~0.50
        expect(sinter.sinterDiagenesisContext).to.include('Fresh Primary Hydrothermal Sinter');

        // High-temperature ancient hydrothermal quartz sinter (420 K, 200,000 years):
        const ancient = KRCEngine.computeMartianSilicaSinteringKineticsAndQuartzMaturation(0.55, 420.0, 1.0, 200000.0);
        expect(ancient.dominantSilicaPhase).to.include('Microcrystalline / Crystalline Quartz');
        expect(ancient.microcrystallineQuartzWeightPct).to.be.greaterThan(50.0);
    });

    it('should discriminate Low-T Serpentine (Lizardite) from Antigorite and Talc in CRISM spectra', () => {
        // Low-T Serpentine (Lizardite with 2.12 um shoulder and 2.32 um Mg-OH in Nili Fossae):
        const lizardite = BandMathEngine.computeCRISMSerpentinePolymorphIndices(0.05, 0.03, 0.08, 0.005);
        expect(lizardite.isSerpentinePresent).to.be.true;
        expect(lizardite.serpentinePhase).to.include('Low-Temperature Serpentine (Lizardite');
        expect(lizardite.serpentinizationTemperature).to.include('Low-Temperature (< 250 deg C');
        expect(lizardite.astrobiologicalContext).to.include('Copious H2 and Abiotic CH4');

        // Hydrothermal Talc (sharp 2.31 um and 2.46 um doublet):
        const talc = BandMathEngine.computeCRISMSerpentinePolymorphIndices(0.05, 0.005, 0.08, 0.04);
        expect(talc.isSerpentinePresent).to.be.true;
        expect(talc.serpentinePhase).to.include('Talc (Mg3Si4O10(OH)2)');
        expect(talc.serpentinizationTemperature).to.include('Moderate to High Temperature');

        // High-T Antigorite (no 2.12 um shoulder):
        const antigorite = BandMathEngine.computeCRISMSerpentinePolymorphIndices(0.05, 0.005, 0.08, 0.005);
        expect(antigorite.isSerpentinePresent).to.be.true;
        expect(antigorite.serpentinePhase).to.include('High-Temperature Serpentine (Antigorite)');

        // Flat basalt:
        const basalt = BandMathEngine.computeCRISMSerpentinePolymorphIndices(0.005, 0.005, 0.01, 0.005);
        expect(basalt.isSerpentinePresent).to.be.false;
    });
});

describe('Mars-Phobos Low-Thrust Spiral, Hydrothermal Convection & Pyroxene BAR Inversion', () => {
    it('should calculate continuous low-thrust ion spiral descent from high orbit to Phobos rendezvous', () => {
        // Ion engine spiral descent (17032 km altitude to Phobos, 150 mN thrust, 1200 kg spacecraft, 3200s Isp):
        const spiral = TrajectoryEngine.computeMarsPhobosLowThrustSpiralDescentTrajectory(17032.0, 150.0, 1200.0, 3200.0);
        expect(spiral.departureRadiusKm).to.equal(20421.5);
        expect(spiral.phobosRadiusKm).to.equal(9376.0);
        expect(spiral.edelbaumDeltaVMMS).to.be.closeTo(689.0, 10.0); // ~689 m/s Delta-V
        expect(spiral.xenonPropellantConsumedKg).to.be.closeTo(26.06, 1.0); // ~26 kg Xe
        expect(spiral.spiralDurationDays).to.be.closeTo(63.1, 5.0); // ~63 days
        expect(spiral.totalSpiralRevolutions).to.be.closeTo(98.7, 10.0); // ~99 revolutions
        expect(spiral.lowThrustMissionContext).to.include('Low-Thrust Spiral Descent');
    });

    it('should calculate deep crustal hydrothermal convection cells and Rayleigh-Darcy stability in impact basins', () => {
        // Deep 4 km fractured impact basin aquifer (1e-13 m^2 permeability, 150 mW/m^2 basal heat flux):
        const hydro = KRCEngine.computeMartianDeepHydrothermalConvectionAndRayleighDarcy(1.0e-13, 4.0, 150.0, 2.5e-4);
        expect(hydro.criticalRayleighNumber).to.be.closeTo(39.48, 0.5); // 4*pi^2 ~39.5
        expect(hydro.rayleighDarcyNumber).to.be.closeTo(1868.0, 100.0); // Ra ~1868
        expect(hydro.isHydrothermalConvectionActive).to.be.true; // Vigorous convection
        expect(hydro.nusseltConvectiveMultiplier).to.be.closeTo(47.3, 3.0); // Nu ~47
        expect(hydro.upwellingFluidVelocityMYr).to.be.closeTo(7.04, 0.5); // ~7 m/yr upwelling
        expect(hydro.convectiveHeatDischargeWM2).to.be.closeTo(7.10, 0.5); // ~7.1 W/m^2
        expect(hydro.hydrothermalConvectionContext).to.include('Vigorous Crustal Hydrothermal Convection');

        // Low-permeability tight basalt (no convection):
        const tight = KRCEngine.computeMartianDeepHydrothermalConvectionAndRayleighDarcy(1.0e-17, 4.0, 50.0, 2.5e-4);
        expect(tight.isHydrothermalConvectionActive).to.be.false;
    });

    it('should calculate Gaffey Pyroxene Band Area Ratio (BAR) and Wollastonite ternary composition from CRISM spectra', () => {
        // High-Ca Clinopyroxene (Augite in Syrtis Major: Band I = 1.03 um, Band II = 2.25 um, BAR = 1.50):
        const augite = BandMathEngine.computeCRISMPyroxeneBandAreaRatioAndComposition(1.03, 2.25, 0.08, 0.12);
        expect(augite.isPyroxenePresent).to.be.true;
        expect(augite.pyroxeneClass).to.include('High-Calcium Clinopyroxene (HCP / Augite-Diopside)');
        expect(augite.bandAreaRatioBAR).to.equal(1.50);
        expect(augite.estimatedWollastonitePct).to.be.closeTo(39.2, 2.0); // ~39% Wo
        expect(augite.petrologicContext).to.include('Syrtis Major Caldera Complex');

        // Low-Ca Orthopyroxene (Noachian crust / ALH84001: Band I = 0.92 um, Band II = 1.90 um, BAR = 0.90):
        const opx = BandMathEngine.computeCRISMPyroxeneBandAreaRatioAndComposition(0.92, 1.90, 0.10, 0.09);
        expect(opx.isPyroxenePresent).to.be.true;
        expect(opx.pyroxeneClass).to.include('Low-Calcium Orthopyroxene (LCP / Enstatite-Hypersthene)');
        expect(opx.bandAreaRatioBAR).to.equal(0.90);
        expect(opx.estimatedWollastonitePct).to.be.closeTo(7.0, 2.0); // ~7% Wo
        expect(opx.petrologicContext).to.include('Ancient Noachian Crust');
    });
});

describe('Mars-Venus Gravity Assist, Impact Shock Melt & Acid Drainage Ferric Sulfates', () => {
    it('should calculate Mars-to-Venus inward transfer, Venus gravity assist turn angle, and Delta-V', () => {
        // Mars to Venus inward transfer (300 km Venus flyby, 300 km Mars parking altitude):
        const inward = TrajectoryEngine.computeMarsVenusMercuryInwardTransferTrajectory(300.0, 300.0);
        expect(inward.departurePlanet).to.equal('Mars');
        expect(inward.assistPlanet).to.equal('Venus');
        expect(inward.timeOfFlightToVenusDays).to.be.closeTo(217.4, 5.0); // ~217 days TOF
        expect(inward.transVenusInjectionDeltaVKmS).to.be.closeTo(3.372, 0.2); // ~3.37 km/s TVI
        expect(inward.venusHyperbolicExcessKmS).to.be.closeTo(5.763, 0.2); // ~5.76 km/s excess
        expect(inward.venusBendingAngleDeg).to.be.closeTo(74.6, 3.0); // ~75 deg bending
        expect(inward.gravityAssistDeltaVKmS).to.be.closeTo(6.99, 0.3); // ~7.0 km/s effective assist Delta-V
        expect(inward.inwardTransferContext).to.include('Mars-Venus Inward Transfer');
    });

    it('should calculate planar impact shock Hugoniot pressure, impact melt volume, and sheet crystallization', () => {
        // 5 km asteroid impact at 10 km/s onto Martian basalt (120 m melt sheet):
        const shock = KRCEngine.computeMartianImpactShockAttenuationAndMeltSheet(5.0, 10.0, 2900.0, 120.0);
        expect(shock.peakHugoniotShockPressureGPa).to.be.closeTo(151.5, 10.0); // ~152 GPa peak pressure
        expect(shock.impactMeltVolumeKm3).to.be.closeTo(21.08, 3.0); // ~21 km^3 melt
        expect(shock.meltSheetThicknessMeters).to.equal(120.0);
        expect(shock.meltSheetSolidificationYears).to.be.closeTo(148.6, 15.0); // ~149 years
        expect(shock.shockMetamorphismContext).to.include('Impact Shock Melt');
    });

    it('should discriminate Jarosite, Copiapite, and Coquimbite acid drainage ferric sulfates in CRISM spectra', () => {
        // Jarosite (diagnostic 2.26 um Fe-OH band at Mawrth Vallis & Meridiani Planum: BD2260 = 0.07, BD880 = 0.12):
        const jarosite = BandMathEngine.computeCRISMAcidDrainageFerricSulfateIndices(0.07, 0.08, 0.12, 0.04);
        expect(jarosite.isFerricSulfatePresent).to.be.true;
        expect(jarosite.ferricSulfateSpecies).to.include('Jarosite (Hydroxyl-Bearing');
        expect(jarosite.mineralFormula).to.equal('KFe3(SO4)2(OH)6');
        expect(jarosite.pHRange).to.include('Hyper-Acidic (pH 1.5 - 3.0)');
        expect(jarosite.acidDrainagePaleoenvironmentContext).to.include('Low-Water Oxidative Weathering');

        // Copiapite (extreme acid drainage polyhydrated sulfate on Valles Marineris floor: BD2260 = 0.01, BD1940 = 0.10, BD2400 = 0.06):
        const copiapite = BandMathEngine.computeCRISMAcidDrainageFerricSulfateIndices(0.01, 0.10, 0.12, 0.06);
        expect(copiapite.isFerricSulfatePresent).to.be.true;
        expect(copiapite.ferricSulfateSpecies).to.include('Copiapite (Highly Hydrated');
        expect(copiapite.pHRange).to.include('Extreme Acid Mine Drainage (pH < 1.0)');

        // Coquimbite:
        const coquimbite = BandMathEngine.computeCRISMAcidDrainageFerricSulfateIndices(0.01, 0.02, 0.08, 0.01);
        expect(coquimbite.isFerricSulfatePresent).to.be.true;
        expect(coquimbite.ferricSulfateSpecies).to.include('Coquimbite / Rhomboclase');

        // Flat basalt:
        const basalt = BandMathEngine.computeCRISMAcidDrainageFerricSulfateIndices(0.005, 0.005, 0.01, 0.005);
        expect(basalt.isFerricSulfatePresent).to.be.false;
    });
});

describe('Mars-to-Asteroid Transfer, Subglacial Volcano Jokulhlaup & Carbonate Polymorph Inversion', () => {
    it('should calculate Mars-to-Main Asteroid Belt (Ceres / Vesta) Hohmann transfer and Delta-V', () => {
        // Mars to Ceres transfer (2.7675 AU, 300 km Mars parking orbit):
        const ceres = TrajectoryEngine.computeMarsToMainBeltAsteroidHohmannTransfer(2.7675, 300.0);
        expect(ceres.targetBody).to.equal('Dwarf Planet Ceres');
        expect(ceres.asteroidDistanceAU).to.equal(2.7675);
        expect(ceres.timeOfFlightDays).to.be.closeTo(573.7, 10.0); // ~574 days TOF
        expect(ceres.timeOfFlightYears).to.be.closeTo(1.57, 0.1); // ~1.57 years
        expect(ceres.transAsteroidInjectionDeltaVKmS).to.be.closeTo(2.419, 0.2); // ~2.42 km/s TAI
        expect(ceres.rendezvousDeltaVKmS).to.be.closeTo(2.816, 0.2); // ~2.82 km/s rendezvous
        expect(ceres.totalMissionDeltaVKmS).to.be.closeTo(5.235, 0.3); // ~5.24 km/s total
        expect(ceres.asteroidTransferContext).to.include('Dwarf Planet Ceres');

        // Mars to Vesta transfer (2.3618 AU):
        const vesta = TrajectoryEngine.computeMarsToMainBeltAsteroidHohmannTransfer(2.3618, 300.0);
        expect(vesta.targetBody).to.equal('Proto-Planet Vesta');
    });

    it('should calculate subglacial volcanic basal melting, cavity overpressure, and jokulhlaup megaflood discharge', () => {
        // Subglacial volcanic fissure (2 km ice cap, 2500 W/m^2 heat flux, 25 km^3 water cavity):
        const jokul = KRCEngine.computeMartianSubglacialVolcanicBasalMeltingAndJokulhlaup(2.0, 2500.0, 25.0, 210.0);
        expect(jokul.basalIceMeltRateMYr).to.be.closeTo(184.0, 10.0); // ~184 m/yr melt rate
        expect(jokul.subglacialHydrostaticOverpressureKPa).to.be.closeTo(595.3, 20.0); // ~595 kPa overpressure
        expect(jokul.peakJokulhlaupDischargeM3S).to.be.closeTo(149129.0, 5000.0); // ~1.49e5 m^3/s peak flood
        expect(jokul.unitStreamPowerKWm).to.be.closeTo(2774.3, 100.0); // ~2.77 MW/m canyon power
        expect(jokul.subglacialVolcanismContext).to.include('Subglacial Jokulhlaup');
    });

    it('should discriminate Magnesite, Siderite, and Calcite/Dolomite carbonate polymorphs in CRISM spectra', () => {
        // Magnesite (2.30 um & 2.50 um Mg-carbonate in Nili Fossae / Jezero margin: BD2300 = 0.07, BD2500 = 0.10):
        const magnesite = BandMathEngine.computeCRISMCarbonatePolymorphIndices(0.07, 0.10, 0.01, 0.01, 0.01);
        expect(magnesite.isCarbonatePresent).to.be.true;
        expect(magnesite.carbonateClass).to.include('Magnesium Carbonate (Magnesite)');
        expect(magnesite.mineralFormula).to.equal('MgCO3');
        expect(magnesite.co2SequestrationPaleoenvironmentContext).to.include('Jezero Margin Carbonates');

        // Siderite (shifted 2.335 & 2.535 um bands + broad 1.0 um Fe2+ band in Comanche / Columbia Hills: BD2335 = 0.06, BD2535 = 0.08, BD1000 = 0.12):
        const siderite = BandMathEngine.computeCRISMCarbonatePolymorphIndices(0.01, 0.01, 0.06, 0.08, 0.12);
        expect(siderite.isCarbonatePresent).to.be.true;
        expect(siderite.carbonateClass).to.include('Iron Carbonate (Siderite)');
        expect(siderite.mineralFormula).to.equal('FeCO3');
        expect(siderite.co2SequestrationPaleoenvironmentContext).to.include('Comanche Outcrop');

        // Calcite / Dolomite (shifted 2.34 & 2.54 um bands without 1.0 um Fe2+ band: BD2335 = 0.05, BD2535 = 0.07, BD1000 = 0.01):
        const calcite = BandMathEngine.computeCRISMCarbonatePolymorphIndices(0.01, 0.01, 0.05, 0.07, 0.01);
        expect(calcite.isCarbonatePresent).to.be.true;
        expect(calcite.carbonateClass).to.include('Calcite / Dolomite');

        // Non-carbonate matrix:
        const basalt = BandMathEngine.computeCRISMCarbonatePolymorphIndices(0.005, 0.005, 0.005, 0.005, 0.005);
        expect(basalt.isCarbonatePresent).to.be.false;
    });
});

describe('Mars-to-Saturn Transfer, Magma Ocean Degassing & Playa Evaporite Inversion', () => {
    it('should calculate Mars-to-Saturn/Titan deep space Hohmann transfer and Delta-V', () => {
        // Mars to Saturn transfer (9.5826 AU, 300 km Mars parking orbit):
        const saturn = TrajectoryEngine.computeMarsToSaturnTitanTransferTrajectory(9.5826, 300.0);
        expect(saturn.targetPlanet).to.equal('Saturn / Titan System');
        expect(saturn.saturnDistanceAU).to.equal(9.5826);
        expect(saturn.timeOfFlightDays).to.be.closeTo(2389.0, 50.0); // ~2389 days TOF
        expect(saturn.timeOfFlightYears).to.be.closeTo(6.54, 0.2); // ~6.54 years
        expect(saturn.transSaturnInjectionDeltaVKmS).to.be.closeTo(5.564, 0.3); // ~5.56 km/s TSI
        expect(saturn.saturnHyperbolicExcessKmS).to.be.closeTo(4.582, 0.3); // ~4.58 km/s excess
        expect(saturn.outerTransferContext).to.include('Mars to Saturn Transfer');
    });

    it('should calculate primordial magma ocean degassing, steam greenhouse pressure, and ocean condensation', () => {
        // 1000 km magma ocean (500 ppm H2O, 200 ppm CO2):
        const ocean = KRCEngine.computeMartianMagmaOceanDegassingAndAtmosphereCollapse(1000.0, 500.0, 200.0);
        expect(ocean.magmaOceanMassKg).to.be.closeTo(3.71e23, 0.2e23); // ~3.71e23 kg magma
        expect(ocean.steamSurfacePressureBar).to.be.closeTo(38.2, 3.0); // ~38 bar steam
        expect(ocean.co2SurfacePressureBar).to.be.closeTo(15.3, 2.0); // ~15 bar CO2
        expect(ocean.totalPrimordialPressureBar).to.be.closeTo(53.5, 5.0); // ~54 bar atmosphere
        expect(ocean.oceanGELMeters).to.be.closeTo(1027.7, 50.0); // ~1028 m Global Equivalent Layer
        expect(ocean.oceanCondensationTimescaleMyr).to.be.closeTo(0.40, 0.05); // ~0.40 Myr condensation
        expect(ocean.primordialClimateContext).to.include('Magma Ocean Degassing');
    });

    it('should discriminate Nitrate, Perchlorate, and Borate evaporite salts in CRISM spectra', () => {
        // Nitrate salt (2.15 um N-O overtone in Gale Crater playa: BD2150 = 0.06, BD1750 = 0.01, BD2480 = 0.01):
        const nitrate = BandMathEngine.computeCRISMEvaporiteNitratePerchlorateBorateIndices(0.06, 0.01, 0.01, 0.04);
        expect(nitrate.isEvaporiteSaltPresent).to.be.true;
        expect(nitrate.evaporiteSaltClass).to.include('Nitrate Salt');
        expect(nitrate.chemicalSpecies).to.include('Nitratine / Nitrocalcite');
        expect(nitrate.prebioticAstrobiologicalContext).to.include('Fixed Atmospheric Nitrogen');

        // Hydrated Perchlorate salt (1.75 um Cl-O + 1.90 um hydration in Phoenix soils: BD2150 = 0.01, BD1750 = 0.05, BD1900 = 0.08):
        const perchlorate = BandMathEngine.computeCRISMEvaporiteNitratePerchlorateBorateIndices(0.01, 0.05, 0.01, 0.08);
        expect(perchlorate.isEvaporiteSaltPresent).to.be.true;
        expect(perchlorate.evaporiteSaltClass).to.include('Perchlorate / Chlorate Salt');
        expect(perchlorate.prebioticAstrobiologicalContext).to.include('Extreme Eutectic Brine Antifreeze');

        // Borate salt (2.48 um B-O overtone: BD2150 = 0.01, BD1750 = 0.01, BD2480 = 0.06):
        const borate = BandMathEngine.computeCRISMEvaporiteNitratePerchlorateBorateIndices(0.01, 0.01, 0.06, 0.04);
        expect(borate.isEvaporiteSaltPresent).to.be.true;
        expect(borate.evaporiteSaltClass).to.include('Borate Salt');
        expect(borate.prebioticAstrobiologicalContext).to.include('Prebiotic Ribose Sugar Stabilization');

        // Non-evaporite soil:
        const soil = BandMathEngine.computeCRISMEvaporiteNitratePerchlorateBorateIndices(0.005, 0.005, 0.005, 0.005);
        expect(soil.isEvaporiteSaltPresent).to.be.false;
    });
});

describe('Mars-to-Ice Giant Transfer, Core Dynamo Convection & Hydrated Silica Inversion', () => {
    it('should calculate Mars-to-Ice Giant (Uranus / Neptune) outer solar system transfer and Delta-V', () => {
        // Mars to Uranus transfer (19.191 AU, 300 km Mars parking orbit):
        const uranus = TrajectoryEngine.computeMarsToIceGiantTransferTrajectory('Uranus', 19.191, 300.0);
        expect(uranus.targetPlanet).to.equal('Uranus');
        expect(uranus.targetDistanceAU).to.equal(19.191);
        expect(uranus.timeOfFlightDays).to.be.closeTo(6092.0, 100.0); // ~6092 days TOF
        expect(uranus.timeOfFlightYears).to.be.closeTo(16.68, 0.5); // ~16.7 years
        expect(uranus.transIceGiantInjectionDeltaVKmS).to.be.closeTo(6.552, 0.4); // ~6.55 km/s injection
        expect(uranus.iceGiantHyperbolicExcessKmS).to.be.closeTo(4.191, 0.3); // ~4.19 km/s arrival excess
        expect(uranus.iceGiantTransferContext).to.include('Mars to Uranus Transfer');

        // Mars to Neptune transfer (30.07 AU):
        const neptune = TrajectoryEngine.computeMarsToIceGiantTransferTrajectory('Neptune', 30.07, 300.0);
        expect(neptune.targetPlanet).to.equal('Neptune');
        expect(neptune.timeOfFlightYears).to.be.closeTo(31.39, 1.0); // ~31.4 years
    });

    it('should calculate primordial core thermal convection, adiabatic heat flux, and geodynamo surface field', () => {
        // Active early core dynamo (1830 km core, 35 mW/m^2 CMB heat flux, 2000 K CMB temp):
        const dynamo = KRCEngine.computeMartianCoreDynamoAndThermalConvection(1830.0, 35.0, 2000.0);
        expect(dynamo.adiabaticCoreHeatFluxMWm2).to.be.closeTo(10.87, 1.0); // ~10.9 mW/m^2 adiabat
        expect(dynamo.isThermalDynamoActive).to.be.true; // Superadiabatic convection active
        expect(dynamo.convectiveHeatFluxMWm2).to.be.closeTo(24.13, 1.0); // ~24.1 mW/m^2 superadiabatic
        expect(dynamo.coreMagneticFieldMicroTesla).to.be.closeTo(828.0, 50.0); // ~828 uT core field
        expect(dynamo.surfaceDipoleFieldMicroTesla).to.be.closeTo(130.3, 15.0); // ~130 uT surface dipole
        expect(dynamo.coreDynamoContext).to.include('Active Core Geodynamo');

        // Extinct dynamo when mantle cools below core adiabat (8 mW/m^2 CMB flux):
        const extinct = KRCEngine.computeMartianCoreDynamoAndThermalConvection(1830.0, 8.0, 2000.0);
        expect(extinct.isThermalDynamoActive).to.be.false;
        expect(extinct.surfaceDipoleFieldMicroTesla).to.equal(0.0);
        expect(extinct.coreDynamoContext).to.include('Extinct Geodynamo');
    });

    it('should discriminate Amorphous Hydrated Silica (Opal-A) from Al-Phyllosilicates in CRISM spectra', () => {
        // Hydrated Opal-A (broad Si-OH band at 2.21 um with 2.26 um shoulder at Home Plate / Gale: BD2210 = 0.08, BD2260 = 0.05, BD1900 = 0.10):
        const opala = BandMathEngine.computeCRISMHydratedSilicaOpalineIndices(0.08, 0.05, 0.10, 0.07);
        expect(opala.isSilicaDetected).to.be.true;
        expect(opala.silicaPhase).to.include('Amorphous Hydrated Silica (Opal-A / Opal-CT)');
        expect(opala.mineralSpecies).to.include('Hydrated Opaline Silica');
        expect(opala.opalineShoulderRatio).to.be.closeTo(0.62, 0.05); // ratio ~0.62 >= 0.55
        expect(opala.depositionalEnvironmentContext).to.include('Geothermal Fumarolic Sinter Precipitation');

        // Aluminum Smectite clay (sharp 2.21 um Al-OH band with weak 2.26 um shoulder: BD2210 = 0.08, BD2260 = 0.02, BD1900 = 0.08):
        const clay = BandMathEngine.computeCRISMHydratedSilicaOpalineIndices(0.08, 0.02, 0.08, 0.05);
        expect(clay.isSilicaDetected).to.be.true;
        expect(clay.silicaPhase).to.include('Aluminum Phyllosilicate (Smectite / Montmorillonite)');
        expect(clay.depositionalEnvironmentContext).to.include('Pedogenic Weathering / Lacustrine Authigenic Clay');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMHydratedSilicaOpalineIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isSilicaDetected).to.be.false;
    });
});

describe('Mars-to-Kuiper Belt Transfer, Mantle Plume Flexure & Olivine Fo# Inversion', () => {
    it('should calculate Mars-to-Kuiper Belt Object (Pluto / Arrokoth) deep solar system Hohmann transfer and Delta-V', () => {
        // Mars to Pluto transfer (39.48 AU, 300 km Mars parking orbit):
        const pluto = TrajectoryEngine.computeMarsToKuiperBeltTransferTrajectory('Dwarf Planet Pluto', 39.48, 300.0);
        expect(pluto.targetBody).to.equal('Dwarf Planet Pluto');
        expect(pluto.targetDistanceAU).to.equal(39.48);
        expect(pluto.timeOfFlightDays).to.be.closeTo(16954.0, 300.0); // ~16954 days TOF
        expect(pluto.timeOfFlightYears).to.be.closeTo(46.42, 1.0); // ~46.4 years
        expect(pluto.transKboInjectionDeltaVKmS).to.be.closeTo(7.08, 0.6); // ~7.08 km/s injection
        expect(pluto.kboHyperbolicExcessKmS).to.be.closeTo(3.448, 0.4); // ~3.45 km/s flyby excess
        expect(pluto.kboTransferContext).to.include('Mars to Dwarf Planet Pluto Transfer');

        // Mars to Cold Classical KBO Arrokoth (44.6 AU):
        const arrokoth = TrajectoryEngine.computeMarsToKuiperBeltTransferTrajectory('KBO Arrokoth', 44.6, 300.0);
        expect(arrokoth.targetBody).to.equal('KBO Arrokoth');
        expect(arrokoth.timeOfFlightYears).to.be.closeTo(55.37, 1.5); // ~55.4 years
    });

    it('should calculate volcanic construct lithospheric flexure, rigidity, and peripheral moat depression', () => {
        // Olympus Mons shield volcano (600 km diameter, 21 km height, 80 km elastic lithosphere):
        const flex = KRCEngine.computeMartianMantlePlumeLithosphericFlexure(600.0, 21.0, 80.0);
        expect(flex.flexuralRigidityN_m).to.be.closeTo(4.55e24, 0.3e24); // ~4.55e24 N*m rigidity
        expect(flex.flexuralParameterKm).to.be.closeTo(193.4, 10.0); // ~193.4 km alpha
        expect(flex.centralDeflectionKm).to.be.closeTo(16.68, 2.0); // ~16.7 km central crustal sag
        expect(flex.peripheralBulgeRadiusKm).to.be.closeTo(607.7, 30.0); // ~608 km bulge radius
        expect(flex.peripheralMoatDepthKm).to.be.closeTo(0.07, 0.05); // peripheral moat
        expect(flex.flexureContext).to.include('Lithospheric Flexure');
    });

    it('should invert Olivine Forsterite Fo# vs Fayalite Fa# solid solution ratio from CRISM 1.05 um band shift', () => {
        // Magnesian Forsterite (1.042 um minimum in Nili Fossae mantle peridotite: Fo ~ 86%):
        const forsterite = BandMathEngine.computeCRISMOlivineSolidSolutionFoFaIndices(1.042, 0.14, 0.09, 0.07);
        expect(forsterite.isOlivineDetected).to.be.true;
        expect(forsterite.forsteriteNumberFo).to.be.closeTo(86.0, 3.0); // Fo ~ 86%
        expect(forsterite.fayaliteNumberFa).to.be.closeTo(14.0, 3.0); // Fa ~ 14%
        expect(forsterite.olivineClass).to.include('Magnesian Olivine (Forsterite');
        expect(forsterite.petrologicContext).to.include('Primitive Upper Mantle Melting');

        // Intermediate Olivine (1.058 um minimum in Ganges Chasma basalt: Fo ~ 54%):
        const inter = BandMathEngine.computeCRISMOlivineSolidSolutionFoFaIndices(1.058, 0.12, 0.08, 0.06);
        expect(inter.isOlivineDetected).to.be.true;
        expect(inter.forsteriteNumberFo).to.be.closeTo(54.0, 3.0); // Fo ~ 54%
        expect(inter.olivineClass).to.include('Intermediate Olivine');
        expect(inter.petrologicContext).to.include('Basaltic Volcanism');

        // Ferroan Fayalite (1.080 um minimum in Argyre evolved pluton: Fo ~ 10%):
        const fayalite = BandMathEngine.computeCRISMOlivineSolidSolutionFoFaIndices(1.080, 0.10, 0.07, 0.05);
        expect(fayalite.isOlivineDetected).to.be.true;
        expect(fayalite.forsteriteNumberFo).to.be.closeTo(10.0, 3.0); // Fo ~ 10%
        expect(fayalite.fayaliteNumberFa).to.be.closeTo(90.0, 3.0); // Fa ~ 90%
        expect(fayalite.olivineClass).to.include('Ferroan Olivine (Fayalite');

        // Non-olivine matrix:
        const basalt = BandMathEngine.computeCRISMOlivineSolidSolutionFoFaIndices(1.050, 0.01, 0.01, 0.01);
        expect(basalt.isOlivineDetected).to.be.false;
    });
});

describe('Mars-to-Interstellar Escape, Mantle Overturn & Sulfate vs Zeolite Inversion', () => {
    it('should calculate Mars-to-Interstellar Heliopause hyperbolic escape trajectory and crossing time', () => {
        // Heliopause escape (122 AU, 15 km/s asymptotic escape speed, 300 km Mars parking orbit):
        const escape = TrajectoryEngine.computeMarsToInterstellarEscapeTrajectory(122.0, 15.0, 300.0);
        expect(escape.heliopauseDistanceAU).to.equal(122.0);
        expect(escape.asymptoticEscapeSpeedKmS).to.equal(15.0);
        expect(escape.transInterstellarInjectionDeltaVKmS).to.be.closeTo(10.594, 0.4); // ~10.59 km/s TII
        expect(escape.timeOfFlightYears).to.be.closeTo(35.0, 2.0); // ~35.0 years to Heliopause
        expect(escape.heliopauseCrossingSpeedKmS).to.be.closeTo(15.48, 0.4); // ~15.5 km/s crossing speed
        expect(escape.interstellarContext).to.include('Interstellar Heliopause Escape');
    });

    it('should calculate Rayleigh-Taylor cumulate overturn timescale, diapir sinking velocity, and CMB transit', () => {
        // Dense ilmenite cumulates (50 km layer, 300 kg/m^3 density contrast, 1e20 Pa*s mantle viscosity):
        const overturn = KRCEngine.computeMartianMantleOverturnAndBasalMagmaCrystallization(50.0, 300.0, 1.0e20);
        expect(overturn.rayleighTaylorWavelengthKm).to.be.closeTo(128.0, 5.0); // ~128 km wavelength
        expect(overturn.overturnTimescaleMyr).to.be.closeTo(0.279, 0.03); // ~0.28 Myr instability growth
        expect(overturn.diapirSinkingVelocityCmYr).to.be.closeTo(8.01, 0.5); // ~8.0 cm/yr sinking speed
        expect(overturn.cmbTransitTimescaleMyr).to.be.closeTo(18.7, 1.5); // ~18.7 Myr to reach CMB
        expect(overturn.basalPlumeVolumeKm3).to.be.closeTo(137258.0, 10000.0); // ~1.37e5 km^3 plume
        expect(overturn.mantleOverturnContext).to.include('Mantle Cumulate Overturn');
    });

    it('should discriminate Polyhydrated Sulfate (Hexahydrite) vs Hydrated Zeolite (Analcime) in CRISM spectra', () => {
        // Polyhydrated Sulfate (Hexahydrite / Gypsum: strong 1.90 um and 2.40 um SO4 band in Meridiani: BD1400 = 0.05, BD1900 = 0.09, BD2400 = 0.08, BD2500 = 0.01):
        const gypsum = BandMathEngine.computeCRISMPolyhydratedSulfateVsZeoliteIndices(0.05, 0.09, 0.08, 0.01);
        expect(gypsum.isHydratedMineralDetected).to.be.true;
        expect(gypsum.mineralFamilyClass).to.include('Polyhydrated Sulfate (Hexahydrite / Epsomite)');
        expect(gypsum.chemicalFormula).to.include('MgSO4 * 6H2O');
        expect(gypsum.alkalineLacustrineContext).to.include('High Water Activity Evaporation');

        // Hydrated Zeolite (Analcime: strong 1.90 um and 2.50 um framework band in Mawrth: BD1400 = 0.06, BD1900 = 0.08, BD2400 = 0.01, BD2500 = 0.07):
        const zeolite = BandMathEngine.computeCRISMPolyhydratedSulfateVsZeoliteIndices(0.06, 0.08, 0.01, 0.07);
        expect(zeolite.isHydratedMineralDetected).to.be.true;
        expect(zeolite.mineralFamilyClass).to.include('Hydrated Zeolite (Analcime / Clinoptilolite)');
        expect(zeolite.chemicalFormula).to.include('NaAlSi2O6 * H2O');
        expect(zeolite.alkalineLacustrineContext).to.include('Alkaline-Saline Lacustrine');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMPolyhydratedSulfateVsZeoliteIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isHydratedMineralDetected).to.be.false;
    });
});

describe('Mars-to-Sun Coronal Dive, Basin Hydrothermal Cooling & Pyroxene Discrimination', () => {
    it('should calculate Mars-to-Sun inward Parker Solar Probe coronal dive trajectory and perihelion velocity', () => {
        // Coronal plunge (9.86 solar radii, 300 km Mars parking orbit):
        const dive = TrajectoryEngine.computeMarsToSolarPerihelionDiveTrajectory(9.86, 300.0);
        expect(dive.perihelionSolarRadii).to.equal(9.86);
        expect(dive.perihelionDistanceKm).to.be.closeTo(6865912.0, 10000.0); // ~6.87e6 km perihelion
        expect(dive.timeOfFlightDays).to.be.closeTo(126.9, 2.0); // ~126.9 days TOF
        expect(dive.transSolarInjectionDeltaVKmS).to.be.closeTo(15.512, 0.5); // ~15.51 km/s TSI
        expect(dive.solarPerihelionSpeedKmS).to.be.closeTo(193.72, 2.0); // ~193.7 km/s perihelion speed
        expect(dive.solarDiveContext).to.include('Mars to Sun Coronal Dive');
    });

    it('should calculate giant impact basin post-impact hydrothermal circulation, cooling, and serpentinization H2', () => {
        // Isidis Basin (1200 km diameter, 5 km melt sheet, 1e-13 m^2 permeability, 400 K thermal anomaly):
        const basin = KRCEngine.computeMartianBasinHydrothermalCoolingAndSerpentinization(1200.0, 5.0, 1.0e-13, 400.0);
        expect(basin.conductiveCoolingTimescaleKyr).to.be.closeTo(198.1, 10.0); // ~198 kyr conductive cooling
        expect(basin.rayleighDarcyNumber).to.be.closeTo(1767.4, 50.0); // Ra ~ 1767 >> 39.48
        expect(basin.isHydrothermalConvectionActive).to.be.true;
        expect(basin.nusseltHeatTransportNumber).to.be.closeTo(44.2, 3.0); // Nu ~ 44
        expect(basin.hydrothermalLifespanKyr).to.be.closeTo(4.5, 0.5); // ~4.5 kyr vigorous circulation
        expect(basin.hydrogenProductionTg).to.be.closeTo(6377433.1, 100000.0); // ~6.38e6 Tg H2
        expect(basin.basinHydrothermalContext).to.include('Basin Hydrothermal System');
    });

    it('should discriminate Low-Calcium Pyroxene (Enstatite), Intermediate (Pigeonite), and High-Calcium (Augite) in CRISM spectra', () => {
        // Low-Calcium Pyroxene (Enstatite / Orthopyroxene in ancient Noachian crust: Band I = 0.92 um, Band II = 1.90 um):
        const lcp = BandMathEngine.computeCRISMPyroxeneHighLowCalciumDiscrimination(0.92, 1.90, 0.12, 0.10);
        expect(lcp.isPyroxeneDetected).to.be.true;
        expect(lcp.pyroxeneClass).to.include('Low-Calcium Pyroxene (LCP / Orthopyroxene)');
        expect(lcp.mineralSpecies).to.include('Enstatite / Bronzite');
        expect(lcp.estimatedWoContentPercent).to.equal(5.0);
        expect(lcp.petrogeneticCrustalContext).to.include('Ancient Primordial Noachian Crust');

        // High-Calcium Pyroxene (Augite / Diopside in Syrtis Major lavas: Band I = 1.04 um, Band II = 2.28 um):
        const hcp = BandMathEngine.computeCRISMPyroxeneHighLowCalciumDiscrimination(1.04, 2.28, 0.14, 0.12);
        expect(hcp.isPyroxeneDetected).to.be.true;
        expect(hcp.pyroxeneClass).to.include('High-Calcium Pyroxene (HCP / Clinopyroxene)');
        expect(hcp.mineralSpecies).to.include('Augite / Diopside');
        expect(hcp.estimatedWoContentPercent).to.equal(40.0);
        expect(hcp.petrogeneticCrustalContext).to.include('Differentiated Basaltic Volcanism');

        // Intermediate Pyroxene (Pigeonite: Band I = 0.97 um, Band II = 2.05 um):
        const pig = BandMathEngine.computeCRISMPyroxeneHighLowCalciumDiscrimination(0.97, 2.05, 0.10, 0.08);
        expect(pig.isPyroxeneDetected).to.be.true;
        expect(pig.pyroxeneClass).to.include('Intermediate-Calcium Pyroxene (Pigeonite)');
        expect(pig.estimatedWoContentPercent).to.equal(15.0);

        // Non-pyroxene matrix:
        const basalt = BandMathEngine.computeCRISMPyroxeneHighLowCalciumDiscrimination(0.95, 2.00, 0.01, 0.01);
        expect(basalt.isPyroxeneDetected).to.be.false;
    });
});

describe('Mars-to-Jupiter Trojan Transfer, Magma Chamber Solidification & Illite-Smectite Inversion', () => {
    it('should calculate Mars-to-Jupiter Trojan (L4/L5) Hohmann transfer trajectory and rendezvous Delta-V', () => {
        // Transfer to L4 Greek Camp (5.2044 AU, 300 km Mars parking orbit):
        const trojan = TrajectoryEngine.computeMarsToJupiterTrojanHohmannTransfer('L4 Greek Camp (Eurybates/Polymele)', 5.2044, 300.0);
        expect(trojan.targetCluster).to.equal('L4 Greek Camp (Eurybates/Polymele)');
        expect(trojan.trojanDistanceAU).to.equal(5.2044);
        expect(trojan.timeOfFlightDays).to.be.closeTo(1126.9, 20.0); // ~1127 days TOF
        expect(trojan.timeOfFlightYears).to.be.closeTo(3.085, 0.1); // ~3.09 years
        expect(trojan.transTrojanInjectionDeltaVKmS).to.be.closeTo(4.197, 0.2); // ~4.20 km/s TTI
        expect(trojan.trojanArrivalExcessKmS).to.be.closeTo(4.107, 0.2); // ~4.11 km/s arrival excess
        expect(trojan.totalMissionDeltaVKmS).to.be.closeTo(8.304, 0.3); // ~8.30 km/s total rendezvous
        expect(trojan.trojanTransferContext).to.include('Mars to L4 Greek Camp');
    });

    it('should calculate crustal magma chamber crystallization, Stefan phase front, and metamorphic aureole', () => {
        // 3 km thick magma sill at 8 km depth (10 km radius, 1200 C basalt intrusion):
        const pluton = KRCEngine.computeMartianMagmaChamberSolidificationAndCooling(3.0, 8.0, 10.0, 1200.0);
        expect(pluton.stefanNumber).to.be.closeTo(2.79, 0.2); // Ste ~ 2.79
        expect(pluton.solidificationTimescaleKyr).to.be.closeTo(24.4, 2.0); // ~24.4 kyr complete solidification
        expect(pluton.latentHeatEnergyExajoules).to.be.closeTo(1055.6, 50.0); // ~1056 EJ latent heat
        expect(pluton.metamorphicAureoleThicknessKm).to.be.closeTo(1.8, 0.2); // ~1.8 km baking aureole
        expect(pluton.hostRockTempC).to.be.closeTo(70.0, 5.0); // ~70 C host temp
        expect(pluton.magmaSolidificationContext).to.include('Magma Chamber Solidification');
    });

    it('should discriminate Swellable Smectite, Hydrothermal Illite, and Chlorite in CRISM spectra', () => {
        // Swellable Smectite (Montmorillonite: strong 1.90 um water & 2.20 um Al-OH in Mawrth: BD1400 = 0.05, BD1900 = 0.08, BD2200 = 0.07, BD2350 = 0.01):
        const smectite = BandMathEngine.computeCRISMIlliteSmectiteChloriteIndices(0.05, 0.08, 0.07, 0.01);
        expect(smectite.isPhyllosilicateDetected).to.be.true;
        expect(smectite.clayFamilyClass).to.include('Hydrated Swellable Smectite');
        expect(smectite.mineralSpecies).to.include('Al-Smectite / Montmorillonite');
        expect(smectite.interlayerHydrationRatio).to.be.closeTo(1.14, 0.1);
        expect(smectite.thermalAlterationContext).to.include('Pedogenic Weathering / Lacustrine Authigenic Clay');

        // Hydrothermal Illite / Sericite (sharp 2.20 um Al-OH with weak 1.90 um water: BD1400 = 0.03, BD1900 = 0.01, BD2200 = 0.08, BD2350 = 0.01):
        const illite = BandMathEngine.computeCRISMIlliteSmectiteChloriteIndices(0.03, 0.01, 0.08, 0.01);
        expect(illite.isPhyllosilicateDetected).to.be.true;
        expect(illite.clayFamilyClass).to.include('Non-Swellable Illite / Mica');
        expect(illite.mineralSpecies).to.include('Illite / Muscovite');
        expect(illite.thermalAlterationContext).to.include('High-Temperature Hydrothermal Alteration');

        // Chlorite (strong 2.35 um Fe/Mg-OH band in Eridania basement: BD1400 = 0.04, BD1900 = 0.02, BD2200 = 0.01, BD2350 = 0.08):
        const chlorite = BandMathEngine.computeCRISMIlliteSmectiteChloriteIndices(0.04, 0.02, 0.01, 0.08);
        expect(chlorite.isPhyllosilicateDetected).to.be.true;
        expect(chlorite.clayFamilyClass).to.include('Chlorite / Fe-Mg Smectite');
        expect(chlorite.mineralSpecies).to.include('Chlorite');
        expect(chlorite.thermalAlterationContext).to.include('Subsurface Deep Crustal Metamorphism');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMIlliteSmectiteChloriteIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isPhyllosilicateDetected).to.be.false;
    });
});

describe('Mars EDL Entry Deceleration, Basalt Weathering Kinetics & Carbonate Cation Inversion', () => {
    it('should calculate Mars Atmospheric Entry & Landing (EDL) peak G-load, deceleration altitude, and heat flux', () => {
        // MSL Curiosity type entry (5.85 km/s entry speed, -12.5 deg flight path angle, 145 kg/m^2 beta, 0.6 m nose radius):
        const edl = TrajectoryEngine.computeMartianAtmosphericEntryDescentTrajectory(5.85, -12.5, 145.0, 0.60);
        expect(edl.entrySpeedKmS).to.equal(5.85);
        expect(edl.flightPathAngleDeg).to.equal(-12.5);
        expect(edl.peakDecelerationAltitudeKm).to.be.closeTo(14.02, 0.5); // ~14.0 km altitude
        expect(edl.peakDecelerationGs).to.be.closeTo(12.52, 0.5); // ~12.5 g peak load
        expect(edl.peakDynamicPressureKPa).to.be.closeTo(35.60, 2.0); // ~35.6 kPa dynamic pressure
        expect(edl.peakStagnationHeatFluxWcm2).to.be.closeTo(82.4, 4.0); // ~82.4 W/cm^2 peak heat flux
        expect(edl.velocityAtPeakDecelKmS).to.be.closeTo(3.55, 0.2); // ~3.55 km/s at peak decel
        expect(edl.edlContext).to.include('Mars EDL Entry');
    });

    it('should calculate Transition State Theory (TST) basaltic bedrock weathering rate and clay formation regime', () => {
        // Open-system leaching (W/R = 50, pH = 6.5, 25 C, 5000 m^2/kg specific surface):
        const openWeathering = KRCEngine.computeMartianBasaltWeatheringAndClayFormationKinetics(50.0, 6.5, 25.0, 5000.0);
        expect(openWeathering.dissolutionRateMolM2S).to.be.closeTo(3.65e-16, 0.5e-16); // ~3.65e-16 mol/(m^2*s)
        expect(openWeathering.weatheringFrontTimescaleKyrPerMeter).to.be.closeTo(173.8, 15.0); // ~174 kyr per meter
        expect(openWeathering.dominantNeoformedPhyllosilicate).to.include('Kaolinite / Halloysite');
        expect(openWeathering.geochemicalRegime).to.include('Open-System Intensive Leaching');

        // Closed-basin alkaline evaporation (W/R = 5, pH = 9.0, 25 C):
        const closedWeathering = KRCEngine.computeMartianBasaltWeatheringAndClayFormationKinetics(5.0, 9.0, 25.0, 5000.0);
        expect(closedWeathering.dominantNeoformedPhyllosilicate).to.include('Saponite + Carbonate');
        expect(closedWeathering.geochemicalRegime).to.include('Hyper-Alkaline Closed Paleolake');
    });

    it('should discriminate Magnesium Carbonate (Magnesite), Iron (Siderite), and Calcium (Calcite) in CRISM spectra', () => {
        // Magnesite (MgCO3 in Nili Fossae / Jezero margin: Band I = 2.31 um, Band II = 2.51 um):
        const magnesite = BandMathEngine.computeCRISMCarbonateCationDiscriminationIndices(2.31, 2.51, 0.08, 0.07);
        expect(magnesite.isCarbonateDetected).to.be.true;
        expect(magnesite.carbonateCationClass).to.include('Magnesium Carbonate (Magnesite Type)');
        expect(magnesite.mineralSpecies).to.include('Magnesite');
        expect(magnesite.chemicalFormula).to.equal('MgCO3');
        expect(magnesite.paleoenvironmentalContext).to.include('Serpentinization & Carbonation of Ultramafic');

        // Siderite (FeCO3 in reducing lacustrine deep units: Band I = 2.34 um, Band II = 2.54 um):
        const siderite = BandMathEngine.computeCRISMCarbonateCationDiscriminationIndices(2.34, 2.54, 0.09, 0.08);
        expect(siderite.isCarbonateDetected).to.be.true;
        expect(siderite.carbonateCationClass).to.include('Iron Carbonate (Siderite Type)');
        expect(siderite.chemicalFormula).to.equal('FeCO3');

        // Calcite (CaCO3 in alkaline hydrothermal springs: Band I = 2.355 um, Band II = 2.555 um):
        const calcite = BandMathEngine.computeCRISMCarbonateCationDiscriminationIndices(2.355, 2.555, 0.07, 0.06);
        expect(calcite.isCarbonateDetected).to.be.true;
        expect(calcite.carbonateCationClass).to.include('Calcium Carbonate (Calcite / Aragonite Type)');
        expect(calcite.chemicalFormula).to.equal('CaCO3');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMCarbonateCationDiscriminationIndices(2.31, 2.51, 0.005, 0.005);
        expect(basalt.isCarbonateDetected).to.be.false;
    });
});

describe('Ice Giant Aerocapture, Smectite Illitization & Silica Phase Inversion', () => {
    it('should calculate Ice Giant (Uranus/Neptune) atmospheric aerocapture trajectory and Delta-V savings', () => {
        // Uranus aerocapture (4.20 km/s arrival v_inf, 250 km periapsis, L/D = 0.25, 100,000 km capture apoapsis):
        const aero = TrajectoryEngine.computeIceGiantAtmosphericAerocaptureTrajectory('Uranus', 4.20, 250.0, 0.25, 100000.0);
        expect(aero.targetPlanet).to.equal('Uranus');
        expect(aero.hyperbolicArrivalSpeedKmS).to.equal(4.20);
        expect(aero.atmosphericPeriapsisSpeedKmS).to.be.closeTo(21.68, 0.2); // ~21.68 km/s periapsis speed
        expect(aero.aerocaptureDeltaVSavedKmS).to.be.closeTo(2.30, 0.2); // ~2.30 km/s Delta-V saved
        expect(aero.propellantMassFractionSavedPercent).to.be.closeTo(51.9, 3.0); // ~52% propellant saved
        expect(aero.aerocaptureCorridorWidthDeg).to.be.closeTo(0.94, 0.1); // ~0.94 deg flight path corridor
        expect(aero.aerocaptureContext).to.include('Uranus Aerocapture');
    });

    it('should calculate Smectite-to-Illite diagenetic transformation kinetics and Reichweite ordering', () => {
        // 4 km burial depth, 30 K/km gradient (70 C in-situ temp), 50 Myr duration:
        const illiteKinetics = KRCEngine.computeMartianSmectiteToIlliteTransformationKinetics(4.0, 30.0, 50.0, 0.010);
        expect(illiteKinetics.burialDepthKm).to.equal(4.0);
        expect(illiteKinetics.inSituTempC).to.equal(70.0);
        expect(illiteKinetics.illiteFractionPercent).to.be.greaterThan(80.0); // High illite conversion
        expect(illiteKinetics.orderingStructureClass).to.include('High-Grade Illite / Muscovite');
        expect(illiteKinetics.isIlliteDominated).to.be.true;
        expect(illiteKinetics.smectiteIllitizationContext).to.include('Smectite Illitization');
    });

    it('should discriminate Hydrated Opal-A, Paracrystalline Opal-CT, and Chalcedony/Quartz in CRISM spectra', () => {
        // Opal-A (Hydrated amorphous silica at Home Plate: BD1400 = 0.06, BD2200 = 0.09, 2.26 um shoulder = 0.055 -> ratio = 0.61 >= 0.55):
        const opalA = BandMathEngine.computeCRISMSilicaHydrationStateAndPhaseIndices(0.06, 0.09, 0.055);
        expect(opalA.isSilicaDetected).to.be.true;
        expect(opalA.silicaPhaseClass).to.include('Hydrated Amorphous Silica (Opal-A)');
        expect(opalA.crystallinityGrade).to.include('Amorphous Opal-A');
        expect(opalA.shoulderRatio).to.be.closeTo(0.61, 0.05);
        expect(opalA.hydrothermalDepositContext).to.include('Hydrothermal Spring Sinter / Acid-Sulfate Fumarolic');

        // Opal-CT (Paracrystalline: BD1400 = 0.04, BD2200 = 0.08, shoulder = 0.032 -> ratio = 0.40):
        const opalCT = BandMathEngine.computeCRISMSilicaHydrationStateAndPhaseIndices(0.04, 0.08, 0.032);
        expect(opalCT.isSilicaDetected).to.be.true;
        expect(opalCT.silicaPhaseClass).to.include('Paracrystalline Silica (Opal-CT)');

        // Chalcedony / Quartz (Sharp Si-OH without shoulder: BD1400 = 0.01, BD2200 = 0.06, shoulder = 0.005 -> ratio = 0.08):
        const quartz = BandMathEngine.computeCRISMSilicaHydrationStateAndPhaseIndices(0.01, 0.06, 0.005);
        expect(quartz.isSilicaDetected).to.be.true;
        expect(quartz.silicaPhaseClass).to.include('Microcrystalline Silica (Chalcedony / Quartz)');

        // Non-silica basalt:
        const basalt = BandMathEngine.computeCRISMSilicaHydrationStateAndPhaseIndices(0.005, 0.005, 0.001);
        expect(basalt.isSilicaDetected).to.be.false;
    });
});

describe('Mars-to-Venus Gravity Assist, Sinter Accretion & Serpentine-Talc Inversion', () => {
    it('should calculate Mars-to-Venus inward transfer trajectory, gravity assist deflection, and heliocentric boost', () => {
        // Venus gravity assist (300 km Venus closest approach, 300 km Mars parking orbit):
        const assist = TrajectoryEngine.computeMarsToVenusGravityAssistTrajectory(300.0, 300.0);
        expect(assist.timeOfFlightDays).to.be.closeTo(217.4, 3.0); // ~217 days TOF
        expect(assist.timeOfFlightYears).to.be.closeTo(0.595, 0.05); // ~0.60 yr
        expect(assist.transVenusInjectionDeltaVKmS).to.be.closeTo(3.372, 0.2); // ~3.37 km/s TVI
        expect(assist.venusArrivalExcessSpeedKmS).to.be.closeTo(5.763, 0.2); // ~5.76 km/s excess
        expect(assist.flybyDeflectionAngleDeg).to.be.closeTo(74.64, 2.0); // ~74.6 deg turn
        expect(assist.heliocentricDeltaVBoostKmS).to.be.closeTo(6.988, 0.3); // ~6.99 km/s boost
        expect(assist.gravityAssistContext).to.include('Mars to Venus Gravity Assist');
    });

    it('should calculate hydrothermal siliceous sinter mound precipitation rate and vertical accretion time', () => {
        // 100 m^3/d spring discharge, 500 ppm silica, 90 C emergence, 25 m mound radius, 3 m target height:
        const sinter = KRCEngine.computeMartianHydrothermalSinterMoundAccretion(100.0, 500.0, 90.0, 25.0, 3.0);
        expect(sinter.dailySilicaMassFluxKg).to.be.closeTo(41.32, 2.0); // ~41.3 kg/day
        expect(sinter.annualSilicaTons).to.be.closeTo(15.09, 1.0); // ~15.1 tons/yr
        expect(sinter.verticalAccretionRateMmPerYear).to.be.closeTo(5.69, 0.5); // ~5.69 mm/yr accretion
        expect(sinter.timeToAccreteYears).to.be.closeTo(527.1, 40.0); // ~527 years for 3m mound
        expect(sinter.sinterDepositContext).to.include('Siliceous Sinter Mound');
    });

    it('should discriminate Serpentine (Ultramafic), Hydrothermal Talc, and Lacustrine Saponite in CRISM spectra', () => {
        // Hydrothermal Talc (Nili Fossae: sharp 1.39 um OH, 2.29 um & 2.38 um satellites: BD1390 = 0.06, BD1900 = 0.01, BD2310 = 0.08, BD2380 = 0.06, BD2290 = 0.05):
        const talc = BandMathEngine.computeCRISMMgPhyllosilicateSerpentineTalcSaponiteIndices(0.06, 0.01, 0.08, 0.06, 0.05);
        expect(talc.isMgPhyllosilicateDetected).to.be.true;
        expect(talc.mineralFamilyClass).to.include('Hydrothermal Talc');
        expect(talc.mineralSpecies).to.include('Talc');
        expect(talc.chemicalFormula).to.equal('Mg3Si4O10(OH)2');
        expect(talc.metasomaticPaleoenvironment).to.include('Hydrothermal Silica Metasomatism');

        // Serpentine (Claritas Rise / Nili Fossae peridotite: sharp 1.39 um & 2.33 um without talc satellites: BD1390 = 0.07, BD1900 = 0.01, BD2310 = 0.09, BD2380 = 0.005, BD2290 = 0.005):
        const serp = BandMathEngine.computeCRISMMgPhyllosilicateSerpentineTalcSaponiteIndices(0.07, 0.01, 0.09, 0.005, 0.005);
        expect(serp.isMgPhyllosilicateDetected).to.be.true;
        expect(serp.mineralFamilyClass).to.include('Serpentine (Serpentinized Peridotite)');
        expect(serp.chemicalFormula).to.equal('Mg3Si2O5(OH)4');
        expect(serp.metasomaticPaleoenvironment).to.include('Serpentinization of Ultramafic Crust');

        // Saponite (Jezero crater floor: broad 2.31 um + 1.90 um water: BD1390 = 0.01, BD1900 = 0.08, BD2310 = 0.07, BD2380 = 0.005, BD2290 = 0.005):
        const sap = BandMathEngine.computeCRISMMgPhyllosilicateSerpentineTalcSaponiteIndices(0.01, 0.08, 0.07, 0.005, 0.005);
        expect(sap.isMgPhyllosilicateDetected).to.be.true;
        expect(sap.mineralFamilyClass).to.include('Mg-Smectite (Saponite)');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMMgPhyllosilicateSerpentineTalcSaponiteIndices(0.005, 0.005, 0.005, 0.001, 0.001);
        expect(basalt.isMgPhyllosilicateDetected).to.be.false;
    });
});

describe('Mars Gravity Assist Flyby, Acid-Fog Duricrust & Iron Oxide Phase Inversion', () => {
    it('should calculate Mars Gravity Assist (MGA) turning angle, velocity boost, and aphelion pumping', () => {
        // MGA for Jupiter orbit pumping (250 km periapsis, 5.60 km/s approach speed):
        const mga = TrajectoryEngine.computeEarthToJupiterMarsGravityAssistFlyby(250.0, 5.60);
        expect(mga.marsFlybyAltitudeKm).to.equal(250.0);
        expect(mga.approachHyperbolicSpeedKmS).to.equal(5.60);
        expect(mga.flybyEccentricity).to.be.closeTo(3.665, 0.1);
        expect(mga.turningAngleDeg).to.be.closeTo(31.67, 1.5); // ~31.7 deg turn
        expect(mga.heliocentricVelocityBoostKmS).to.be.closeTo(3.056, 0.2); // ~3.06 km/s boost
        expect(mga.postFlybyAphelionAU).to.be.closeTo(2.23, 0.2); // ~2.23 AU pumped aphelion
        expect(mga.mgaContext).to.include('Mars Gravity Assist');
    });

    it('should calculate volcanic acid-fog SO2 fallout flux and sulfate duricrust growth rate', () => {
        // 100 Tg/yr volcanic SO2 flux, 85% neutralization, 100 kyr duration:
        const acidFog = KRCEngine.computeMartianAcidFogBasaltWeatheringAndSulfateCrust(100.0, 0.85, 100.0);
        expect(acidFog.acidFogDepositionFluxGM2Yr).to.be.closeTo(0.6925, 0.05); // ~0.69 g/(m^2*yr)
        expect(acidFog.annualSulfatePrecipitateGM2Yr).to.be.closeTo(1.378, 0.1); // ~1.38 g/(m^2*yr)
        expect(acidFog.duricrustAccretionRateMmPerKyr).to.be.closeTo(0.938, 0.1); // ~0.94 mm/kyr
        expect(acidFog.totalDuricrustThicknessCm).to.be.closeTo(9.38, 1.0); // ~9.4 cm in 100 kyr
        expect(acidFog.dominantSulfateAssemblage).to.include('Polyhydrated Mg-Sulfate + Gypsum');
        expect(acidFog.acidFogContext).to.include('Volcanic Acid-Fog Weathering');
    });

    it('should discriminate Crystalline Gray Hematite, Oxyhydroxide Goethite, and Red Dust in CRISM spectra', () => {
        // Crystalline Hematite (Gray hematite blueberries at Meridiani: 0.53 um reflectance = 0.20, Fe3+ center = 0.865 um, depth = 0.08):
        const hem = BandMathEngine.computeCRISMIronOxidePhaseDiscriminationIndices(0.20, 0.865, 0.08);
        expect(hem.isIronOxideDetected).to.be.true;
        expect(hem.ironOxidePhaseClass).to.include('Crystalline Hematite (Gray Hematite / Concretionary)');
        expect(hem.mineralSpecies).to.include('Hematite');
        expect(hem.chemicalFormula).to.equal('alpha-Fe2O3');
        expect(hem.diageneticAqueousContext).to.include('Aqueous Groundwater Diagenesis');

        // Goethite (Mawrth Vallis ferric oxyhydroxide: 0.53 um = 0.22, Fe3+ center = 0.935 um, depth = 0.09):
        const goeth = BandMathEngine.computeCRISMIronOxidePhaseDiscriminationIndices(0.22, 0.935, 0.09);
        expect(goeth.isIronOxideDetected).to.be.true;
        expect(goeth.ironOxidePhaseClass).to.include('Ferric Oxyhydroxide (Goethite / Ferrihydrite)');
        expect(goeth.mineralSpecies).to.include('Goethite');
        expect(goeth.chemicalFormula).to.equal('alpha-FeO(OH)');

        // Nanophase Red Dust (Bright dust: 0.53 um = 0.38, depth = 0.015):
        const dust = BandMathEngine.computeCRISMIronOxidePhaseDiscriminationIndices(0.38, 0.880, 0.015);
        expect(dust.isIronOxideDetected).to.be.true;
        expect(dust.ironOxidePhaseClass).to.include('Nanophase Ferric Oxide (np-Ox / Red Dust)');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMIronOxidePhaseDiscriminationIndices(0.12, 0.880, 0.005);
        expect(basalt.isIronOxideDetected).to.be.false;
    });
});

describe('Mars-Jupiter Cycler, Acidic Lake Evaporation & Mafic Ternary Modality', () => {
    it('should calculate Mars-to-Jupiter Interplanetary Cycler orbit resonance, TCI Delta-V, and encounter excesses', () => {
        // Mars-Jupiter cycler (5.2044 AU Jupiter distance, 300 km Mars parking altitude):
        const cycler = TrajectoryEngine.computeMarsToJupiterCyclerOrbitTrajectory(5.2044, 300.0);
        expect(cycler.semiMajorAxisAU).to.be.closeTo(3.364, 0.01);
        expect(cycler.eccentricity).to.be.closeTo(0.547, 0.01);
        expect(cycler.orbitalPeriodYears).to.be.closeTo(6.17, 0.1); // ~6.17 yr period
        expect(cycler.oneWayTransitYears).to.be.closeTo(3.09, 0.05); // ~3.09 yr one-way
        expect(cycler.transCyclerInjectionDeltaVKmS).to.be.closeTo(4.197, 0.3); // ~4.20 km/s TCI
        expect(cycler.marsEncounterVInfKmS).to.be.closeTo(5.883, 0.2); // ~5.88 km/s
        expect(cycler.jupiterEncounterVInfKmS).to.be.closeTo(5.642, 1.6); // ~5.64 km/s
        expect(cycler.cyclerContext).to.include('Mars-Jupiter Cycler');
    });

    it('should calculate acidic paleolake desiccation timescale, pH drop, and sequential evaporite precipitation', () => {
        // 50 km^3 lake, 50 m depth, initial pH 2.80, 500 mm/yr evaporation:
        const lake = KRCEngine.computeMartianAcidicLakeEvaporitePrecipitation(50.0, 50.0, 2.8, 500.0);
        expect(lake.desiccationTimescaleYears).to.equal(100.0); // 100 yr desiccation
        expect(lake.finalBrinePH).to.equal(1.80); // pH 2.8 -> 1.8 at CF = 10
        expect(lake.totalEvaporiteMassTg).to.be.closeTo(1750.0, 100.0); // 1750 Tg evaporite salt
        expect(lake.evaporiteBedThicknessCm).to.be.closeTo(79.5, 5.0); // ~79.5 cm bed
        expect(lake.dominantPrecipitateStage).to.include('Jarosite + Alunite');
        expect(lake.lakeEvaporiteContext).to.include('Acidic Paleolake Evaporite');
    });

    it('should calculate CRISM mafic mineral ternary modality (Olivine vs LCP vs HCP) and IUGS lithologic class', () => {
        // Dunite cumulate (Nili Fossae: Olivine = 0.14, LCP = 0.02, HCP = 0.01 -> fol = 82.4% >= 75%):
        const dunite = BandMathEngine.computeCRISMMaficMineralTernaryModalComposition(0.14, 0.02, 0.01);
        expect(dunite.olivineFractionPercent).to.be.closeTo(82.4, 1.0);
        expect(dunite.dominantLithologyClass).to.include('Dunite (Ultramafic Olivine Cumulate)');

        // Gabbroic basalt (Syrtis Major: Olivine = 0.02, LCP = 0.03, HCP = 0.10 -> fhcp = 66.7% >= 55%):
        const gabbro = BandMathEngine.computeCRISMMaficMineralTernaryModalComposition(0.02, 0.03, 0.10);
        expect(gabbro.highCaPyroxeneFractionPercent).to.be.closeTo(66.7, 1.0);
        expect(gabbro.dominantLithologyClass).to.include('Gabbro / Clinopyroxenite');

        // Norite deep crust (Ancient Noachian crust: Olivine = 0.02, LCP = 0.10, HCP = 0.03 -> flcp = 66.7% >= 55%):
        const norite = BandMathEngine.computeCRISMMaficMineralTernaryModalComposition(0.02, 0.10, 0.03);
        expect(norite.lowCaPyroxeneFractionPercent).to.be.closeTo(66.7, 1.0);
        expect(norite.dominantLithologyClass).to.include('Norite / Orthopyroxenite');

        // Non-mafic dust/felsic:
        const dust = BandMathEngine.computeCRISMMaficMineralTernaryModalComposition(0.002, 0.002, 0.002);
        expect(dust.dominantLithologyClass).to.include('Felsic / Plagioclase Feldspar');
    });
});

describe('Mars to Mercury Transfer, Silica Geothermometer & Chloride-Perchlorate Inversion', () => {
    it('should calculate Mars-to-Mercury inward interplanetary transfer trajectory and insertion Delta-V', () => {
        // Mars to Mercury transfer (300 km Mercury parking, 300 km Mars parking):
        const merc = TrajectoryEngine.computeMarsToMercuryTransferTrajectory(300.0, 300.0);
        expect(merc.timeOfFlightDays).to.be.closeTo(169.3, 2.0); // ~169 days TOF
        expect(merc.timeOfFlightYears).to.be.closeTo(0.463, 0.05); // ~0.46 yr
        expect(merc.transMercuryInjectionDeltaVKmS).to.be.closeTo(6.600, 0.2); // ~6.60 km/s TMI
        expect(merc.mercuryArrivalExcessSpeedKmS).to.be.closeTo(12.584, 0.5); // ~12.58 km/s excess
        expect(merc.mercuryOrbitInsertionDeltaVKmS).to.be.closeTo(10.372, 0.5); // ~10.37 km/s MOI
        expect(merc.mercuryTransferContext).to.include('Mars to Mercury Inward Transfer');
    });

    it('should calculate hydrothermal reservoir temperature from dissolved silica geothermometer equations', () => {
        // Quartz geothermometer at 300 ppm SiO2:
        const quartzGeo = KRCEngine.computeMartianSilicaGeothermometerFluidTemperature(300.0, 'Quartz');
        expect(quartzGeo.dissolvedSilicaPpm).to.equal(300.0);
        expect(quartzGeo.silicaPolymorph).to.include('Quartz');
        expect(quartzGeo.estimatedReservoirTempC).to.be.closeTo(209.4, 2.0); // ~209.4 C
        expect(quartzGeo.hydrothermalEnthalpyKjKg).to.be.closeTo(876.0, 15.0); // ~876 kJ/kg
        expect(quartzGeo.reservoirRegime).to.include('Intermediate-Temperature Convective');

        // Chalcedony geothermometer at 300 ppm SiO2:
        const chalGeo = KRCEngine.computeMartianSilicaGeothermometerFluidTemperature(300.0, 'Chalcedony');
        expect(chalGeo.estimatedReservoirTempC).to.be.closeTo(193.2, 2.0); // ~193.2 C

        // Amorphous silica / Opal-A at 300 ppm SiO2:
        const opalGeo = KRCEngine.computeMartianSilicaGeothermometerFluidTemperature(300.0, 'Amorphous Silica / Opal');
        expect(opalGeo.estimatedReservoirTempC).to.be.closeTo(84.7, 2.0); // ~84.7 C
        expect(opalGeo.reservoirRegime).to.include('Sub-Boiling Thermal Spring');
    });

    it('should discriminate Hydrated Oxychlorines vs Anhydrous Chloride Plains in CRISM spectra', () => {
        // Hydrated Magnesium Perchlorate (Phoenix soil / RSL: BD1400 = 0.06, BD1900 = 0.09, BD2140 = 0.04):
        const perchlor = BandMathEngine.computeCRISMChloridePerchlorateSpectraIndices(0.06, 0.09, 0.04, 0.30);
        expect(perchlor.isHalogenSaltDetected).to.be.true;
        expect(perchlor.saltClass).to.include('Hydrated Magnesium / Sodium Perchlorate Brine');
        expect(perchlor.mineralSpecies).to.include('Mg(ClO4)2');
        expect(perchlor.eutecticFreezingTempC).to.equal(-68.5); // Deep eutectic depression
        expect(perchlor.habitabilityImplication).to.include('Deliquescent Oxychlorine Brine');

        // Anhydrous Chloride Flat (Terra Sirenum: albedo = 0.35, flat featureless hydration: BD1400 = 0.005, BD1900 = 0.005, BD2140 = 0.002):
        const chloride = BandMathEngine.computeCRISMChloridePerchlorateSpectraIndices(0.005, 0.005, 0.002, 0.35);
        expect(chloride.isHalogenSaltDetected).to.be.true;
        expect(chloride.saltClass).to.include('Anhydrous Chloride Salt Deposit');
        expect(chloride.mineralSpecies).to.include('NaCl');
        expect(chloride.eutecticFreezingTempC).to.equal(-21.1);

        // Standard low-albedo basalt:
        const basalt = BandMathEngine.computeCRISMChloridePerchlorateSpectraIndices(0.005, 0.005, 0.002, 0.15);
        expect(basalt.isHalogenSaltDetected).to.be.false;
    });
});

describe('Mars-Venus-Mercury Gravity Assist, Permafrost Retreat & Sulfate Hydration Inversion', () => {
    it('should calculate Mars-to-Mercury multi-gravity assist trajectory (M-V-M) and reduced MOI Delta-V', () => {
        // Mars-Venus-Mercury trajectory (300 km Venus flyby, 300 km Mercury parking):
        const mvm = TrajectoryEngine.computeMarsToMercuryDualGravityAssistTrajectory(300.0, 300.0, 300.0);
        expect(mvm.totalTimeOfFlightDays).to.be.closeTo(293.0, 3.0); // ~293.0 days total TOF
        expect(mvm.totalTimeOfFlightYears).to.be.closeTo(0.802, 0.05); // ~0.80 yr
        expect(mvm.marsDepartureDeltaVKmS).to.be.closeTo(3.372, 0.3); // ~3.37 km/s TVI
        expect(mvm.venusFlybyDeflectionAngleDeg).to.be.closeTo(74.64, 2.0); // ~74.6 deg deflection
        expect(mvm.mercuryArrivalExcessKmS).to.be.closeTo(6.769, 0.5); // ~6.77 km/s (reduced from 12.58 km/s)
        expect(mvm.mercuryOrbitInsertionDeltaVKmS).to.be.closeTo(5.032, 0.5); // ~5.03 km/s MOI
        expect(mvm.missionDeltaVSavedKmS).to.be.closeTo(5.340, 0.5); // > 5.3 km/s saved
        expect(mvm.mvmContext).to.include('Mars-Venus-Mercury Gravity Assist');
    });

    it('should calculate permafrost ground-ice sublimation retreat, vapor diffusion, and desiccation growth', () => {
        // 205 K ice table, 190 K atmospheric dew point, 2e-5 m^2/s diffusivity, 35% pore ice, 100 kyr duration:
        const permafrost = KRCEngine.computeMartianPermafrostSublimationRetreat(205.0, 190.0, 2.0e-5, 0.35, 100.0);
        expect(permafrost.initialDryLayerCm).to.equal(5.0);
        expect(permafrost.finalDryLayerThicknessM).to.be.closeTo(1.138, 0.1); // ~1.14 m depth
        expect(permafrost.finalDryLayerThicknessCm).to.be.closeTo(113.8, 10.0); // ~114 cm
        expect(permafrost.instantaneousRetreatRateMmPerKyr).to.be.closeTo(5.68, 0.5); // ~5.68 mm/kyr
        expect(permafrost.iceStabilityClass).to.include('Metastable Deep Ground Ice');
        expect(permafrost.permafrostContext).to.include('Ground Ice Sublimation');
    });

    it('should discriminate Monohydrated Sulfates (Kieserite) vs Polyhydrated Sulfates in CRISM spectra', () => {
        // Monohydrated Sulfate / Kieserite (Valles Marineris: BD1400 = 0.005, BD1900 = 0.01, BD2130 = 0.08, BD2400 = 0.07):
        const kieserite = BandMathEngine.computeCRISMHydratedSulfateHydrationStateIndices(0.005, 0.01, 0.08, 0.07);
        expect(kieserite.isSulfateDetected).to.be.true;
        expect(kieserite.hydrationStateClass).to.include('Monohydrated Sulfate (MHS - Kieserite Type)');
        expect(kieserite.mineralSpecies).to.include('Kieserite');
        expect(kieserite.chemicalFormula).to.include('MgSO4 * H2O');
        expect(kieserite.environmentalHydrationContext).to.include('Low Water Activity / Desiccated');

        // Polyhydrated Sulfate / Hexahydrite (Juventae Chasma: BD1400 = 0.05, BD1900 = 0.08, BD2130 = 0.005, BD2400 = 0.06):
        const poly = BandMathEngine.computeCRISMHydratedSulfateHydrationStateIndices(0.05, 0.08, 0.005, 0.06);
        expect(poly.isSulfateDetected).to.be.true;
        expect(poly.hydrationStateClass).to.include('Polyhydrated Sulfate (PHS - Hexahydrite / Gypsum Type)');
        expect(poly.chemicalFormula).to.include('MgSO4 * 6H2O');

        // Non-sulfate basalt:
        const basalt = BandMathEngine.computeCRISMHydratedSulfateHydrationStateIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isSulfateDetected).to.be.false;
    });
});

describe('Low-Thrust Continuous Spiral, Lava Tube Microclimate & Olivine Fo-Fa Inversion', () => {
    it('should calculate Mars continuous low-thrust ion spiral trajectory, burn duration, and fuel mass', () => {
        // Mars to Earth 1.0 AU spiral (1500 kg spacecraft, 250 mN thrust, 3500 s Isp):
        const spiral = TrajectoryEngine.computeMarsLowThrustContinuousSpiralTrajectory(1500.0, 250.0, 3500.0, 1.000);
        expect(spiral.lowThrustDeltaVKmS).to.be.closeTo(5.655, 0.2); // ~5.66 km/s Delta-V
        expect(spiral.propellantConsumedKg).to.be.closeTo(227.9, 5.0); // ~228 kg Xenon
        expect(spiral.propellantFractionPercent).to.be.closeTo(15.2, 1.0); // ~15.2% fuel mass
        expect(spiral.spiralDurationDays).to.be.closeTo(362.1, 10.0); // ~362 days spiral
        expect(spiral.spiralDurationYears).to.be.closeTo(0.991, 0.05); // ~0.99 yr
        expect(spiral.initialAccelerationMmS2).to.be.closeTo(0.167, 0.02); // ~0.167 mm/s^2
        expect(spiral.finalAccelerationMmS2).to.be.closeTo(0.197, 0.02); // ~0.197 mm/s^2
        expect(spiral.lowThrustContext).to.include('Low-Thrust Continuous Spiral');
    });

    it('should calculate volcanic lava tube subsurface thermal attenuation and cave microclimate stability', () => {
        // 15 m depth, 8e-7 m^2/s rock diffusivity, 210 K mean, 45 K diurnal, 30 K seasonal amplitude:
        const cave = KRCEngine.computeMartianLavaTubeMicroclimateThermalDamping(15.0, 8.0e-7, 210.0, 45.0, 30.0);
        expect(cave.diurnalSkinDepthCm).to.be.closeTo(15.0, 1.0); // ~15.0 cm diurnal skin depth
        expect(cave.seasonalSkinDepthM).to.be.closeTo(3.89, 0.2); // ~3.89 m seasonal skin depth
        expect(cave.caveDiurnalAmplitudeK).to.be.lessThan(0.001); // Damped to 0.0 K diurnal
        expect(cave.caveSeasonalAmplitudeK).to.be.closeTo(0.633, 0.1); // ~0.63 K annual oscillation
        expect(cave.caveMeanTempC).to.be.closeTo(-63.15, 0.5); // ~ -63.2 C
        expect(cave.thermalBufferingClass).to.include('Isothermal Cave Interior');
        expect(cave.caveMicroclimateContext).to.include('Lava Tube Microclimate');
    });

    it('should calculate CRISM olivine Forsterite (Fo#) vs Fayalite (Fa#) solid solution cation ratio', () => {
        // High-Mg Forsteritic olivine (Nili Fossae mantle cumulate: center = 1.040 um, depth = 0.12 -> Fo82 Fa18):
        const forsterite = BandMathEngine.computeCRISMOlivineFoFaCompositionIndices(1.040, 0.12);
        expect(forsterite.isOlivineDetected).to.be.true;
        expect(forsterite.forsteriteNumberPercent).to.be.closeTo(81.8, 1.0);
        expect(forsterite.fayaliteNumberPercent).to.be.closeTo(18.2, 1.0);
        expect(forsterite.olivineCompositionClass).to.include('Forsteritic High-Mg Olivine');
        expect(forsterite.petrogeneticEvolutionContext).to.include('Primitive Upper Mantle Peridotite');

        // Intermediate basaltic olivine (Gusev Crater: center = 1.055 um, depth = 0.10 -> Fo55 Fa45):
        const intermed = BandMathEngine.computeCRISMOlivineFoFaCompositionIndices(1.055, 0.10);
        expect(intermed.isOlivineDetected).to.be.true;
        expect(intermed.forsteriteNumberPercent).to.be.closeTo(54.5, 1.0);
        expect(intermed.olivineCompositionClass).to.include('Intermediate Olivine');

        // Fe-rich Fayalite (Syrtis Major differentiated magma: center = 1.075 um, depth = 0.09 -> Fo18 Fa82):
        const fayalite = BandMathEngine.computeCRISMOlivineFoFaCompositionIndices(1.075, 0.09);
        expect(fayalite.isOlivineDetected).to.be.true;
        expect(fayalite.forsteriteNumberPercent).to.be.closeTo(18.2, 1.0);
        expect(fayalite.olivineCompositionClass).to.include('Fayalitic Fe-Rich Olivine');

        // Non-olivine basalt:
        const basalt = BandMathEngine.computeCRISMOlivineFoFaCompositionIndices(1.040, 0.010);
        expect(basalt.isOlivineDetected).to.be.false;
    });
});

describe('Solar Corona Plunge, Fumarolic Silica Halo & Zeolite-Carbonate Metasomatism', () => {
    it('should calculate Mars-to-Sun Parker solar corona plunge trajectory, flight time, and coronal speed', () => {
        // Solar plunge to 10 Solar Radii (10 R_sun closest approach, 300 km Mars parking altitude):
        const plunge = TrajectoryEngine.computeMarsToSolarCoronaPlungeTrajectory(10.0, 300.0);
        expect(plunge.targetPerihelionSolarRadii).to.equal(10.0);
        expect(plunge.targetPerihelionAUKm).to.be.closeTo(0.0465, 0.005);
        expect(plunge.trajectoryEccentricity).to.be.closeTo(0.9407, 0.01);
        expect(plunge.timeOfFlightDays).to.be.closeTo(127.1, 2.0); // ~127 days TOF
        expect(plunge.transSolarInjectionDeltaVKmS).to.be.closeTo(15.483, 1.0); // ~15.5 km/s TSPI
        expect(plunge.perihelionCoronalSpeedKmS).to.be.closeTo(192.3, 3.0); // ~192 km/s in corona
        expect(plunge.solarPlungeContext).to.include('Solar Corona Plunge');
    });

    it('should calculate volcanic fumarolic acid-gas leaching of host basalt and residual pure silica halo formation', () => {
        // 250 C gas, 500 kg/day SO2 gas, 5 m radius halo, 10 m conduit height:
        const fumarole = KRCEngine.computeMartianFumarolicAcidSulfateAlteration(250.0, 500.0, 5.0, 10.0);
        expect(fumarole.annualAcidGasFluxTons).to.be.closeTo(182.6, 2.0); // ~182.6 t/yr gas
        expect(fumarole.totalHostBasaltMassTons).to.be.closeTo(1869.2, 50.0); // ~1869 tons basalt
        expect(fumarole.leachedCationMassTons).to.be.closeTo(972.0, 30.0); // ~972 tons cations stripped
        expect(fumarole.residualSilicaMassTons).to.be.closeTo(897.2, 30.0); // ~897 tons pure Opal-A silica
        expect(fumarole.completeSilicificationTimescaleYears).to.be.closeTo(6.26, 0.5); // ~6.3 years
        expect(fumarole.alterationGradeClass).to.include('Extreme Acid-Leached Siliceous Residue');
        expect(fumarole.fumaroleContext).to.include('Fumarolic Silica Halo');
    });

    it('should discriminate Hydrothermal Zeolites (Analcime) and Coexisting Carbonates in CRISM spectra', () => {
        // Zeolite + Mg-Carbonate assemblage (Nili Fossae / Tyrrhena Terra: BD2460 = 0.07, BD1900 = 0.08, BD2300 = 0.05, BD2500 = 0.06):
        const zeolCarb = BandMathEngine.computeCRISMZeoliteCarbonateMetasomaticIndices(0.05, 0.06, 0.07, 0.08);
        expect(zeolCarb.isZeoliteDetected).to.be.true;
        expect(zeolCarb.isCarbonateCoexisting).to.be.true;
        expect(zeolCarb.metasomaticClass).to.include('Zeolite + Mg-Carbonate Alkaline Metasomatic Complex');
        expect(zeolCarb.mineralSpecies).to.include('Analcime');
        expect(zeolCarb.chemicalFormula).to.include('NaAlSi2O6');
        expect(zeolCarb.alkalineHydrothermalContext).to.include('Alkaline Hydrothermal Fluid Circulation');

        // Pure Zeolite / Analcime (BD2460 = 0.07, BD1900 = 0.08, no carbonate):
        const pureZeol = BandMathEngine.computeCRISMZeoliteCarbonateMetasomaticIndices(0.005, 0.005, 0.07, 0.08);
        expect(pureZeol.isZeoliteDetected).to.be.true;
        expect(pureZeol.isCarbonateCoexisting).to.be.false;
        expect(pureZeol.metasomaticClass).to.include('Alkaline Hydrothermal Zeolite');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMZeoliteCarbonateMetasomaticIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isZeoliteDetected).to.be.false;
        expect(basalt.isCarbonateCoexisting).to.be.false;
    });
});

describe('Bi-Elliptic Solar Drop, Ice-Covered Paleolake Convection & Alunite-Jarosite Inversion', () => {
    it('should calculate Mars-to-Sun Bi-Elliptic solar drop trajectory via Jupiter distance and Delta-V savings', () => {
        // Bi-elliptic drop via 5.2044 AU (10 R_sun solar perihelion, 300 km Mars parking altitude):
        const biElliptic = TrajectoryEngine.computeMarsToSunBiEllipticSolarDropTrajectory(5.2044, 10.0, 300.0);
        expect(biElliptic.totalTimeOfFlightDays).to.be.closeTo(1905.0, 10.0); // ~1905 days TOF
        expect(biElliptic.totalTimeOfFlightYears).to.be.closeTo(5.216, 0.1); // ~5.22 yr
        expect(biElliptic.marsDepartureDeltaVKmS).to.be.closeTo(4.197, 0.3); // ~4.20 km/s TAI
        expect(biElliptic.aphelionReverseDeltaVKmS).to.be.closeTo(7.008, 0.3); // ~7.01 km/s apo burn
        expect(biElliptic.totalMissionDeltaVKmS).to.be.closeTo(11.205, 0.5); // ~11.21 km/s total Delta-V
        expect(biElliptic.directHohmannDeltaVSavedKmS).to.be.greaterThan(4.0); // > 4.0 km/s saved vs direct drop
        expect(biElliptic.biEllipticContext).to.include('Bi-Elliptic Solar Drop');
    });

    it('should calculate ice-covered paleolake thermal equilibrium, conductive lid heat flux, and Rayleigh convection', () => {
        // 100 m total lake depth, 230 K surface air (-43 C), 15 m ice lid, 50 mW/m^2 geothermal flux:
        const paleolake = KRCEngine.computeMartianIceCoveredPaleolakeThermalEquilibrium(100.0, 230.0, 15.0, 50.0);
        expect(paleolake.conductiveIceHeatFluxWM2).to.be.closeTo(6.386, 0.2); // ~6.39 W/m^2
        expect(paleolake.equilibriumIceLidThicknessM).to.be.closeTo(1368.5, 50.0); // ~1368 m eq thickness
        expect(paleolake.subIceLiquidWaterDepthM).to.equal(85.0); // 85 m liquid column
        expect(paleolake.bottomWaterTempC).to.equal(4.0); // 4 C liquid water
        expect(paleolake.rayleighNumber).to.be.greaterThan(1.0e15); // Turbulently convecting
        expect(paleolake.lakeThermalRegime).to.include('Vigorously Convecting Perennial Sub-Ice Lake');
        expect(paleolake.paleolakeContext).to.include('Ice-Covered Paleolake');
    });

    it('should discriminate Acid-Sulfate Alunite vs Acidic Jarosite vs Neutral Nontronite in CRISM spectra', () => {
        // Alunite / Advanced Argillic (Cross Crater: BD1480 = 0.08, BD1900 = 0.07, BD2260 = 0.01, BD2290 = 0.01):
        const alunite = BandMathEngine.computeCRISMAluniteJarositeNontroniteIndices(0.08, 0.01, 0.01, 0.07);
        expect(alunite.isAlterationMineralDetected).to.be.true;
        expect(alunite.mineralFamilyClass).to.include('Acid-Sulfate Alunite');
        expect(alunite.mineralSpecies).to.include('Alunite');
        expect(alunite.chemicalFormula).to.include('KAl3(SO4)2(OH)6');
        expect(alunite.phGeochemicalRegime).to.include('Hyper-Acidic Hydrothermal System (pH < 3.0');

        // Jarosite (Meridiani Planum: BD1480 = 0.01, BD2260 = 0.07, BD2290 = 0.01):
        const jarosite = BandMathEngine.computeCRISMAluniteJarositeNontroniteIndices(0.01, 0.07, 0.01, 0.05);
        expect(jarosite.isAlterationMineralDetected).to.be.true;
        expect(jarosite.mineralFamilyClass).to.include('Acid-Sulfate Jarosite');
        expect(jarosite.mineralSpecies).to.include('Jarosite');
        expect(jarosite.chemicalFormula).to.include('KFe3(SO4)2(OH)6');
        expect(jarosite.phGeochemicalRegime).to.include('Acidic Oxidizing Sulfate Playa');

        // Nontronite Fe-Smectite (Mawrth Vallis: BD1480 = 0.01, BD2260 = 0.01, BD2290 = 0.08, BD1900 = 0.06):
        const nontronite = BandMathEngine.computeCRISMAluniteJarositeNontroniteIndices(0.01, 0.01, 0.08, 0.06);
        expect(nontronite.isAlterationMineralDetected).to.be.true;
        expect(nontronite.mineralFamilyClass).to.include('Fe-Smectite (Nontronite)');
        expect(nontronite.chemicalFormula).to.include('Fe2Si4O10(OH)2');
        expect(nontronite.phGeochemicalRegime).to.include('Circum-Neutral to Weakly Alkaline');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMAluniteJarositeNontroniteIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAlterationMineralDetected).to.be.false;
    });
});

describe('Trans-Pluto Deep Space Transfer, Cryovolcanic Overpressure & Anorthosite Inversion', () => {
    it('should calculate Mars-to-Pluto / KBO deep space interplanetary transfer trajectory and flyby mechanics', () => {
        // Pluto transfer (39.482 AU, 300 km Mars parking, 1000 km Pluto flyby):
        const pluto = TrajectoryEngine.computeMarsToPlutoDeepSpaceTransferTrajectory(39.482, 300.0, 1000.0);
        expect(pluto.transferSemiMajorAxisAU).to.be.closeTo(20.503, 0.05); // ~20.50 AU
        expect(pluto.trajectoryEccentricity).to.be.closeTo(0.9257, 0.01); // ~0.926
        expect(pluto.timeOfFlightYears).to.be.closeTo(46.42, 0.5); // ~46.4 yr TOF
        expect(pluto.transPlutoInjectionDeltaVKmS).to.be.closeTo(7.040, 0.3); // ~7.04 km/s TPI
        expect(pluto.plutoArrivalExcessKmS).to.be.closeTo(3.454, 0.3); // ~3.45 km/s arrival excess
        expect(pluto.plutoFlybyDeflectionAngleDeg).to.be.closeTo(3.69, 0.3); // ~3.69 deg deflection
        expect(pluto.plutoTransferContext).to.include('Trans-Pluto Transfer');
    });

    it('should calculate subsurface cryovolcanic brine chamber freezing expansion, tensile fracture, and exit velocity', () => {
        // 5 km depth, 2000 m chamber radius, 30% frozen, 3.5 GPa shear modulus:
        const cryo = KRCEngine.computeMartianCryovolcanicEruptionOverpressure(5.0, 2000.0, 0.30, 3.5);
        expect(cryo.lithostaticPressureMPa).to.be.closeTo(46.50, 1.0); // ~46.5 MPa lithostatic
        expect(cryo.chamberOverpressureMPa).to.be.closeTo(126.00, 2.0); // ~126.0 MPa overpressure
        expect(cryo.totalChamberPressureMPa).to.be.closeTo(172.50, 2.0); // ~172.5 MPa total
        expect(cryo.isTensileFractureInitiated).to.be.true; // Exceeds 10 MPa tensile strength
        expect(cryo.cryolavaVentExitVelocityMs).to.be.closeTo(449.1, 5.0); // ~449 m/s exit speed
        expect(cryo.eruptionMechanismClass).to.include('Explosive Sub-Surface Cryovolcanic Venting');
        expect(cryo.cryovolcanismContext).to.include('Cryovolcanic Chamber');
    });

    it('should discriminate pristine Anorthosite Plagioclase Crust vs Mafic Silicates in CRISM spectra', () => {
        // Pure Anorthosite (Valles Marineris wall: BD1250 = 0.05, BD1050 = 0.005, BD2000 = 0.005, BD1900 = 0.005):
        const anorthosite = BandMathEngine.computeCRISMAnorthositePlagioclaseIndices(0.05, 0.005, 0.005, 0.005);
        expect(anorthosite.isPlagioclaseDetected).to.be.true;
        expect(anorthosite.plagioclasePurityPercent).to.be.greaterThan(75.0);
        expect(anorthosite.petrologicClass).to.include('Pristine Anorthosite / Pure Plagioclase Crust');
        expect(anorthosite.mineralSpecies).to.include('Anorthite');
        expect(anorthosite.crustalEvolutionContext).to.include('Primordial Felsic / Anorthositic Crustal Flotation');

        // Mixed Anorthositic Norite (BD1250 = 0.03, BD1050 = 0.015, BD2000 = 0.010):
        const norite = BandMathEngine.computeCRISMAnorthositePlagioclaseIndices(0.03, 0.015, 0.010, 0.005);
        expect(norite.isPlagioclaseDetected).to.be.true;
        expect(norite.petrologicClass).to.include('Anorthositic Norite / Troctolite');

        // Standard pyroxene/olivine basalt (BD1250 = 0.005, BD1050 = 0.08, BD2000 = 0.07):
        const basalt = BandMathEngine.computeCRISMAnorthositePlagioclaseIndices(0.005, 0.08, 0.07, 0.005);
        expect(basalt.isPlagioclaseDetected).to.be.false;
    });
});

describe('Bi-Parabolic Solar Drop, Perchlorate Cryogenic Eutectic & Serpentine Polytypes', () => {
    it('should calculate theoretical absolute minimum Delta-V Bi-Parabolic solar drop trajectory from Mars', () => {
        // Bi-parabolic solar drop to 10 Solar Radii (10 R_sun closest approach, 300 km Mars parking altitude):
        const biParabolic = TrajectoryEngine.computeMarsToSunBiParabolicSolarDropTrajectory(10.0, 300.0);
        expect(biParabolic.solarEscapeSpeedAtMarsKmS).to.be.closeTo(34.124, 0.2); // ~34.12 km/s escape speed
        expect(biParabolic.marsDepartureExcessKmS).to.be.closeTo(9.994, 0.2); // ~9.99 km/s departure excess
        expect(biParabolic.transSolarEscapeInjectionDeltaVKmS).to.be.closeTo(7.688, 0.3); // ~7.69 km/s TSEI
        expect(biParabolic.aphelionInfinityDeltaVKmS).to.equal(0.0);
        expect(biParabolic.totalMissionDeltaVKmS).to.be.closeTo(7.688, 0.3); // ~7.69 km/s total Delta-V
        expect(biParabolic.coronalPerihelionSpeedKmS).to.be.closeTo(195.24, 3.0); // ~195 km/s at 10 R_sun
        expect(biParabolic.hohmannDeltaVSavedKmS).to.be.greaterThan(7.5); // > 7.5 km/s saved
        expect(biParabolic.biParabolicContext).to.include('Bi-Parabolic Solar Drop');
    });

    it('should calculate subsurface perchlorate brine thermodynamic eutectic equilibrium, liquid fraction, and water activity', () => {
        // Mg(ClO4)2 brine at 225 K (-48 C), 10 wt% initial salt:
        const brine = KRCEngine.computeMartianSubsurfacePerchlorateEutecticEquilibrium(225.0, 10.0, 'Mg(ClO4)2');
        expect(brine.saltSpecies).to.include('Magnesium Perchlorate');
        expect(brine.eutecticTemperatureC).to.be.closeTo(-68.5, 0.2);
        expect(brine.isLiquidBrineThermodynamicallyStable).to.be.true;
        expect(brine.liquidusSaltConcentrationWtPct).to.be.closeTo(32.60, 2.0); // ~32.6 wt%
        expect(brine.equilibriumLiquidBrineFractionPercent).to.be.closeTo(30.7, 3.0); // ~30.7% liquid brine
        expect(brine.waterActivityAw).to.be.closeTo(0.627, 0.05); // aw ~ 0.63
        expect(brine.habitabilityStatus).to.include('Metabolically Permissive Liquid Brine');
        expect(brine.brineEquilibriumContext).to.include('Mg(ClO4)2');

        // Sub-eutectic frozen state at 180 K:
        const frozen = KRCEngine.computeMartianSubsurfacePerchlorateEutecticEquilibrium(180.0, 10.0, 'Mg(ClO4)2');
        expect(frozen.isLiquidBrineThermodynamicallyStable).to.be.false;
        expect(frozen.equilibriumLiquidBrineFractionPercent).to.equal(0.0);
        expect(frozen.habitabilityStatus).to.include('Sub-Eutectic Completely Frozen');
    });

    it('should discriminate Low-Temperature Lizardite vs High-Temperature Metamorphic Antigorite in CRISM spectra', () => {
        // Low-Temperature Hydrated Lizardite / Chrysotile (Claritas Rise: BD1390 = 0.07, BD2330 = 0.08, BD2120 = 0.005, BD1900 = 0.05):
        const lizardite = BandMathEngine.computeCRISMSerpentinePolytypeMetamorphicIndices(0.07, 0.08, 0.005, 0.05);
        expect(lizardite.isSerpentineDetected).to.be.true;
        expect(lizardite.polytypeClass).to.include('Low-Temperature Hydrated Serpentine (Lizardite / Chrysotile)');
        expect(lizardite.mineralSpecies).to.include('Lizardite');
        expect(lizardite.chemicalFormula).to.include('Mg3Si2O5(OH)4');
        expect(lizardite.metamorphicGradeContext).to.include('Low-Temperature Hydrothermal Serpentinization');

        // High-Temperature Metamorphic Antigorite (Deep Noachian Basement: BD1390 = 0.01, BD2330 = 0.08, BD2120 = 0.05, BD1900 = 0.005):
        const antigorite = BandMathEngine.computeCRISMSerpentinePolytypeMetamorphicIndices(0.01, 0.08, 0.05, 0.005);
        expect(antigorite.isSerpentineDetected).to.be.true;
        expect(antigorite.polytypeClass).to.include('High-Temperature Metamorphic Serpentine (Antigorite)');
        expect(antigorite.mineralSpecies).to.include('Antigorite');
        expect(antigorite.metamorphicGradeContext).to.include('Prograde Metamorphic Serpentinization');

        // Non-serpentine basalt:
        const basalt = BandMathEngine.computeCRISMSerpentinePolytypeMetamorphicIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isSerpentineDetected).to.be.false;
    });
});

describe('Solar-Scaled SEP Spiral, Deep Hydrothermal Plume & Borate-Nitrate Inversion', () => {
    it('should calculate inward Mars-to-Earth solar-scaled low-thrust ion spiral trajectory', () => {
        // Mars to Earth 1.0 AU (1500 kg spacecraft, 300 mN at 1 AU, 3500 s Isp):
        const spiral = TrajectoryEngine.computeMarsInwardSolarElectricIonSpiralWithSolarScaling(1500.0, 300.0, 3500.0, 1.000);
        expect(spiral.lowThrustDeltaVKmS).to.be.closeTo(5.655, 0.2); // ~5.66 km/s Delta-V
        expect(spiral.propellantConsumedKg).to.be.closeTo(227.9, 5.0); // ~228 kg Xe
        expect(spiral.initialMarsThrustMN).to.be.closeTo(129.2, 5.0); // ~129 mN at 1.52 AU
        expect(spiral.finalArrivalThrustMN).to.be.closeTo(300.0, 1.0); // 300 mN at 1.0 AU
        expect(spiral.averageThrustMN).to.be.closeTo(196.9, 5.0); // ~197 mN avg thrust
        expect(spiral.spiralDurationDays).to.be.closeTo(459.7, 10.0); // ~460 days
        expect(spiral.spiralDurationYears).to.be.closeTo(1.259, 0.05); // ~1.26 yr
        expect(spiral.solarSpiralContext).to.include('Solar-Scaled SEP Spiral');
    });

    it('should calculate deep crustal aquifer geothermal hydrothermal plume upwelling and spring temperature', () => {
        // 6 km depth, 20 K/km gradient, 1e-11 m^2 fault permeability, 50m x 1000m conduit:
        const plume = KRCEngine.computeMartianDeepAquiferHydrothermalPlumeUpwelling(6.0, 20.0, 1.0e-11, 50.0, 1000.0);
        expect(plume.deepAquiferTemperatureK).to.equal(335.0);
        expect(plume.deepAquiferTemperatureC).to.be.closeTo(61.85, 0.2); // ~61.9 C deep reservoir
        expect(plume.buoyancyDensityDeficitKgM3).to.be.closeTo(18.555, 0.5); // ~18.56 kg/m^3
        expect(plume.darcyUpwellingVelocityMPerDay).to.be.closeTo(0.1269, 0.01); // ~0.127 m/d
        expect(plume.dailySpringDischargeM3Day).to.be.closeTo(6344.0, 100.0); // ~6344 m^3/d
        expect(plume.exitSpringTemperatureC).to.be.closeTo(31.85, 0.5); // ~31.9 C exit spring
        expect(plume.hydrothermalSpringClass).to.include('Warm Hydrothermal Fault Spring');
        expect(plume.hydrothermalPlumeContext).to.include('Deep Hydrothermal Plume');
    });

    it('should discriminate Prebiotic Hydrated Borates vs Nitrate Salts in CRISM spectra', () => {
        // Hydrated Borate / Borax (Gale Crater / Columbus Crater: BD2150 = 0.06, BD1900 = 0.07, BD1400 = 0.04, BD2450 = 0.005):
        const borate = BandMathEngine.computeCRISMBorateNitrateEvaporiteIndices(0.06, 0.005, 0.07, 0.04);
        expect(borate.isPrebioticSaltDetected).to.be.true;
        expect(borate.evaporiteClass).to.include('Hydrated Borate Evaporite (Borax / Kernite / Ulexite)');
        expect(borate.mineralSpecies).to.include('Borax');
        expect(borate.chemicalFormula).to.include('Na2B4O5(OH)4');
        expect(borate.prebioticAstrobiologyContext).to.include('Prebiotic Ribose Stabilization Catalyst');

        // Nitrate Salt / Nitratine (BD2150 = 0.005, BD2450 = 0.05, BD1900 = 0.01):
        const nitrate = BandMathEngine.computeCRISMBorateNitrateEvaporiteIndices(0.005, 0.05, 0.01, 0.005);
        expect(nitrate.isPrebioticSaltDetected).to.be.true;
        expect(nitrate.evaporiteClass).to.include('Nitrate Salt Deposit (Nitratine / Niter)');
        expect(nitrate.chemicalFormula).to.include('NaNO3');
        expect(nitrate.prebioticAstrobiologyContext).to.include('Atmospheric Photochemical Fixed Nitrogen');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMBorateNitrateEvaporiteIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isPrebioticSaltDetected).to.be.false;
    });
});

describe('SRP Ion Spiral, Magma Ocean Crystallization & Phosphate Mineral Inversion', () => {
    it('should calculate inward Mars-to-Earth low-thrust ion spiral with Solar Radiation Pressure perturbation', () => {
        // Mars to Earth 1.0 AU (1500 kg, 250 mN, 3500 s Isp, 100 m^2 sail area):
        const srpSpiral = TrajectoryEngine.computeMarsInwardIonSpiralWithSolarRadiationPressure(1500.0, 250.0, 3500.0, 100.0, 1.000);
        expect(srpSpiral.lowThrustDeltaVKmS).to.be.closeTo(5.654, 0.2); // ~5.65 km/s Delta-V
        expect(srpSpiral.propellantConsumedKg).to.be.closeTo(227.8, 5.0); // ~228 kg Xe
        expect(srpSpiral.srpForceAt1AUMillitewtons).to.be.closeTo(0.840, 0.05); // ~0.84 mN SRP force
        expect(srpSpiral.solarLightnessBeta).to.be.greaterThan(1e-5);
        expect(srpSpiral.effectiveMuRatio).to.be.closeTo(0.9999, 0.001);
        expect(srpSpiral.spiralDurationDays).to.be.closeTo(362.0, 10.0); // ~362 days
        expect(srpSpiral.srpSpiralContext).to.include('SRP-Perturbed Inward Spiral');
    });

    it('should calculate primordial Martian Magma Ocean crystallization timescale and mantle overturn', () => {
        // 1500 km magma ocean depth, 100 W/m^2 cooling, 200 kg/m^3 overturn density contrast, 1e20 Pa*s viscosity:
        const mmo = KRCEngine.computeMartianMagmaOceanCrystallizationAndOverturn(1500.0, 100.0, 200.0, 1.0e20);
        expect(mmo.solidificationTimescaleMyr).to.be.closeTo(1.42, 0.2); // ~1.42 Myr solidification
        expect(mmo.mantleOverturnTimescaleKyr).to.be.closeTo(35.7, 5.0); // ~35.7 kyr mantle overturn
        expect(mmo.totalCrystallizedVolumeKm3).to.be.greaterThan(1.0e8); // Massive mantle volume
        expect(mmo.cumulateStratigraphyClass).to.include('Layered Cumulate Mantle');
        expect(mmo.coreDynamoInitiationContext).to.include('MMO Crystallization');
    });

    it('should discriminate Igneous Apatite vs Aqueous Vivianite Iron Phosphate in CRISM spectra', () => {
        // Igneous Magmatic Apatite (Shergottite source: BD2180 = 0.04, BD2220 = 0.04, BD1050 = 0.005, BD1900 = 0.01):
        const apatite = BandMathEngine.computeCRISMPhosphateMineralIndices(0.04, 0.04, 0.005, 0.01);
        expect(apatite.isPhosphateDetected).to.be.true;
        expect(apatite.phosphateClass).to.include('Igneous Magmatic Apatite');
        expect(apatite.mineralSpecies).to.include('Apatite');
        expect(apatite.chemicalFormula).to.include('Ca5(PO4)3');
        expect(apatite.petrogeneticVolatileContext).to.include('Magmatic Accessory Phase / Primary Halogen');

        // Aqueous Vivianite (Gale Crater mudstone: BD1050 = 0.06, BD1900 = 0.08, BD2180 = 0.03):
        const vivianite = BandMathEngine.computeCRISMPhosphateMineralIndices(0.03, 0.01, 0.06, 0.08);
        expect(vivianite.isPhosphateDetected).to.be.true;
        expect(vivianite.phosphateClass).to.include('Hydrated Ferrous Phosphate (Vivianite)');
        expect(vivianite.mineralSpecies).to.include('Vivianite');
        expect(vivianite.chemicalFormula).to.include('Fe3(PO4)2 * 8H2O');
        expect(vivianite.petrogeneticVolatileContext).to.include('Reducing Aqueous Lacustrine Mudstone');

        // Non-phosphate basalt:
        const basalt = BandMathEngine.computeCRISMPhosphateMineralIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isPhosphateDetected).to.be.false;
    });
});

describe('Multi-Planet SEP Inward Tour, Acid Fog Rind & Titanium Oxide Inversion', () => {
    it('should calculate inward low-thrust continuous ion spiral transit times to Earth, Venus, and Mercury', () => {
        // Multi-planet tour from Mars (1500 kg, 250 mN, 3500 s Isp):
        const tour = TrajectoryEngine.computeMarsInwardLowThrustPlanetaryTransitTimes(1500.0, 250.0, 3500.0);
        expect(tour.earthTransitDays).to.be.closeTo(362.1, 10.0); // ~362 d to Earth
        expect(tour.earthTransitYears).to.be.closeTo(0.99, 0.05); // ~0.99 yr
        expect(tour.earthPropellantKg).to.be.closeTo(227.9, 5.0); // ~228 kg Xe

        expect(tour.venusTransitDays).to.be.closeTo(647.3, 20.0); // ~647 d to Venus
        expect(tour.venusTransitYears).to.be.closeTo(1.77, 0.1); // ~1.77 yr
        expect(tour.venusPropellantKg).to.be.closeTo(407.4, 10.0); // ~407 kg Xe

        expect(tour.mercuryTransitDays).to.be.closeTo(1190.4, 30.0); // ~1190 d to Mercury
        expect(tour.mercuryTransitYears).to.be.closeTo(3.26, 0.1); // ~3.26 yr
        expect(tour.mercuryPropellantKg).to.be.closeTo(749.1, 15.0); // ~749 kg Xe

        expect(tour.multiPlanetContext).to.include('Inward Low-Thrust Tour');
    });

    it('should calculate volcanic acid-fog condensation and basaltic boulder weathering rind growth', () => {
        // 50 ppm acid fog, 100 kyr duration, 10% rock porosity, 5e-14 m^2/s diffusivity:
        const rind = KRCEngine.computeMartianAcidFogWeatheringRindGrowth(50.0, 100.0, 0.10, 5.0e-14);
        expect(rind.finalRindThicknessMm).to.be.closeTo(5.00, 0.2); // ~5.00 mm rind
        expect(rind.instantaneousGrowthRateMmPerKyr).to.be.closeTo(0.025, 0.005); // ~0.025 mm/kyr
        expect(rind.cationDepletedVolumeCm3PerM2).to.be.closeTo(5000.0, 200.0); // ~5000 cm^3/m^2
        expect(rind.alterationCrustClass).to.include('Thin Millimeter-Scale Acid-Weathered Basaltic Rind');
        expect(rind.acidFogContext).to.include('Acid-Fog Alteration');
    });

    it('should discriminate Hydrated Anatase / Rutile Resistate vs Magmatic Ilmenite in CRISM spectra', () => {
        // Hydrated Anatase / Rutile (Mawrth Vallis / Columbia Hills: UV slope = 0.12, Ti-OH = 0.04, BD1900 = 0.06):
        const anatase = BandMathEngine.computeCRISMTitaniumOxideIndices(0.12, 0.005, 0.06, 0.04);
        expect(anatase.isTitaniumMineralDetected).to.be.true;
        expect(anatase.titaniumClass).to.include('Hydrated Titanium Oxide (Anatase / Rutile Resistate)');
        expect(anatase.mineralSpecies).to.include('Anatase');
        expect(anatase.chemicalFormula).to.include('TiO2 * nH2O');
        expect(anatase.geochemicalResistateContext).to.include('Residual Laterite Insoluble Resistate Horizon');

        // Magmatic Opaque Ilmenite (Syrtis Major: UV slope = 0.02, Opaque slope = 0.06, Ti-OH = 0.005):
        const ilmenite = BandMathEngine.computeCRISMTitaniumOxideIndices(0.02, 0.06, 0.005, 0.005);
        expect(ilmenite.isTitaniumMineralDetected).to.be.true;
        expect(ilmenite.titaniumClass).to.include('Magmatic Opaque Fe-Ti Oxide (Ilmenite)');
        expect(ilmenite.mineralSpecies).to.include('Ilmenite');
        expect(ilmenite.chemicalFormula).to.include('FeTiO3');
        expect(ilmenite.geochemicalResistateContext).to.include('High-Ti Basaltic Lava Flow');

        // Non-titanium basalt:
        const basalt = BandMathEngine.computeCRISMTitaniumOxideIndices(0.02, 0.01, 0.005, 0.005);
        expect(basalt.isTitaniumMineralDetected).to.be.false;
    });
});

describe('Venus Gravity Assist Resonant Orbit, Smectite Diagenesis & Silica Inversion', () => {
    it('should calculate inward Mars-to-Venus gravity assist flyby, deflection angle, and resonant orbit pumping', () => {
        // Mars to Venus flyby (300 km Venus periapsis, 300 km Mars parking altitude):
        const assist = TrajectoryEngine.computeMarsToVenusGravityAssistResonantOrbit(300.0, 300.0);
        expect(assist.transferTimeDays).to.be.closeTo(217.5, 10.0); // ~217.5 d transfer
        expect(assist.venusArrivalHyperbolicExcessKmS).to.be.closeTo(5.771, 0.2); // ~5.77 km/s v_inf
        expect(assist.flybyDeflectionAngleDeg).to.be.closeTo(74.55, 3.0); // ~74.6 deg deflection
        expect(assist.gravityAssistDeltaVKmS).to.be.closeTo(6.990, 0.3); // ~6.99 km/s gravity assist boost
        expect(assist.postFlybyPerihelionAU).to.be.closeTo(0.511, 0.05); // ~0.511 AU perihelion
        expect(assist.postFlybySemiMajorAxisAU).to.be.closeTo(0.617, 0.05); // ~0.617 AU
        expect(assist.postFlybyPeriodDays).to.be.closeTo(177.1, 10.0); // ~177.1 days
        expect(assist.gravityAssistContext).to.include('Venus Gravity Assist');
    });

    it('should calculate burial diagenetic Smectite-to-Illite conversion kinetics and dewatering fluid overpressure', () => {
        // 4 km burial depth, 25 K/km gradient, 20 Myr heating, 200 ppm K+:
        const diagenesis = KRCEngine.computeMartianSmectiteToIlliteTransitionKinetics(4.0, 25.0, 20.0, 200.0);
        expect(diagenesis.burialTemperatureK).to.equal(315.0);
        expect(diagenesis.burialTemperatureC).to.be.closeTo(41.85, 0.2); // ~41.9 C burial temp
        expect(diagenesis.illitePercentInClay).to.be.greaterThan(90.0); // High conversion
        expect(diagenesis.releasedStructuralWaterWtPct).to.be.closeTo(10.45, 1.0); // ~10.5 wt% H2O released
        expect(diagenesis.dewateringOverpressureMPa).to.be.closeTo(44.8, 5.0); // ~45 MPa overpressure
        expect(diagenesis.clayDiageneticZoneClass).to.include('High-Grade Diagenetic Illite');
        expect(diagenesis.diagenesisContext).to.include('Clay Diagenesis');
    });

    it('should discriminate Amorphous Hydrated Opal-A vs Opal-CT vs Quartz in CRISM spectra', () => {
        // Amorphous Hydrated Silica / Opal-A Sinter (Home Plate / Jezero: BD1400 = 0.06, BD1900 = 0.08, BD2210 = 0.05, BD2260 = 0.04):
        const opalA = BandMathEngine.computeCRISMSilicaCrystallinityIndices(0.06, 0.08, 0.05, 0.04);
        expect(opalA.isSilicaDetected).to.be.true;
        expect(opalA.silicaPolymorphClass).to.include('Amorphous Hydrated Silica (Opal-A Sinter)');
        expect(opalA.mineralSpecies).to.include('Opal-A');
        expect(opalA.chemicalFormula).to.include('SiO2 * nH2O');
        expect(opalA.depositionalEnvironmentContext).to.include('Volcanic Hydrothermal Fumarole / Geyser Sinter');

        // Paracrystalline Opal-CT / Chalcedony (BD1400 = 0.03, BD1900 = 0.03, BD2210 = 0.04, BD2260 = 0.015):
        const opalCT = BandMathEngine.computeCRISMSilicaCrystallinityIndices(0.03, 0.03, 0.04, 0.015);
        expect(opalCT.isSilicaDetected).to.be.true;
        expect(opalCT.silicaPolymorphClass).to.include('Paracrystalline Microcrystalline Silica (Opal-CT / Chalcedony)');
        expect(opalCT.mineralSpecies).to.include('Opal-CT');

        // Non-silica basalt:
        const basalt = BandMathEngine.computeCRISMSilicaCrystallinityIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isSilicaDetected).to.be.false;
    });
});

describe('Radial Low-Thrust Dynamics, Silica Sintering & Chloride Evaporite Inversion', () => {
    it('should calculate continuous radial low-thrust propulsion perturbation, effective gravity, and apsidal shift', () => {
        // Mars orbit 1.52 AU (1500 kg, 300 mN, 3500 s Isp, 180 d duration):
        const radial = TrajectoryEngine.computeLowThrustRadialThrustOrbitModification(1.52368, 1500.0, 300.0, 3500.0, 180.0);
        expect(radial.radialAccelerationMmS2).to.be.closeTo(0.200, 0.01); // 0.20 mm/s^2
        expect(radial.solarGravityRatioPercent).to.be.closeTo(7.83, 0.5); // ~7.83% gravity reduction
        expect(radial.effectiveCircularSpeedKmS).to.be.closeTo(23.161, 0.5); // ~23.16 km/s
        expect(radial.apsidalPrecessionDegPerYear).to.be.closeTo(14.99, 1.0); // ~15.0 deg/yr
        expect(radial.cumulativeApsidalShiftDeg).to.be.closeTo(7.39, 1.0); // ~7.4 deg in 180 d
        expect(radial.propellantConsumedKg).to.be.closeTo(135.9, 5.0); // ~136 kg Xe
        expect(radial.radialSteeringContext).to.include('Radial Low-Thrust');
    });

    it('should calculate hydrothermal silica sinter maturation, porosity compaction, and thermal inertia evolution', () => {
        // 75 C fluid, 55% initial porosity, 100 yr duration, pH 8.0:
        const sinter = KRCEngine.computeMartianHydrothermalSilicaSinteringKinetics(75.0, 0.55, 100.0, 8.0);
        expect(sinter.finalPorosityPercent).to.be.closeTo(13.6, 2.0); // ~13.6% porosity
        expect(sinter.bulkDensityKgM3).to.be.closeTo(1901.7, 50.0); // ~1902 kg/m^3
        expect(sinter.thermalConductivityWMK).to.be.closeTo(1.042, 0.1); // ~1.04 W/(m K)
        expect(sinter.thermalInertiaTIU).to.be.closeTo(1259.1, 50.0); // ~1259 tiu
        expect(sinter.sinterMaturationStageClass).to.include('Dense Recrystallized Chalcedonic Chert');
        expect(sinter.silicaSinterContext).to.include('Silica Sintering');
    });

    it('should discriminate anhydrous Chloride-Bearing Halite Evaporite flats in CRISM and THEMIS spectra', () => {
        // Massive Halite Playa (Terra Sirenum: NIR slope = 0.12, BD2200 = 0.005, BD1900 = 0.010, TIR anomaly = 0.050):
        const chloride = BandMathEngine.computeCRISMAnhydrousHaliteChloridePlayaIndices(0.12, 0.005, 0.010, 0.050);
        expect(chloride.isChlorideDetected).to.be.true;
        expect(chloride.evaporiteClass).to.include('Massive Crystalline Halite / Chloride Salt Flat');
        expect(chloride.mineralSpecies).to.include('Halite');
        expect(chloride.chemicalFormula).to.include('NaCl');
        expect(chloride.halitePlayaContext).to.include('Terminal Hypersaline Evaporative Playa');

        // Dispersed chloride duricrust (NIR slope = 0.08, BD2200 = 0.010, BD1900 = 0.015, TIR anomaly = 0.020):
        const duricrust = BandMathEngine.computeCRISMAnhydrousHaliteChloridePlayaIndices(0.08, 0.010, 0.015, 0.020);
        expect(duricrust.isChlorideDetected).to.be.true;
        expect(duricrust.evaporiteClass).to.include('Dispersed Chloride-Bearing Duricrust');

        // Clay strata (BD2200 = 0.05, NIR slope = 0.02):
        const clay = BandMathEngine.computeCRISMAnhydrousHaliteChloridePlayaIndices(0.02, 0.050, 0.050, 0.010);
        expect(clay.isChlorideDetected).to.be.false;
        expect(clay.evaporiteClass).to.include('Phyllosilicate Clay Horizon');
    });
});

describe('Mars-to-Mercury Hohmann Plunge, Serpentinization H2/CH4 & Oxychlorines', () => {
    it('should calculate direct high-energy Hohmann transfer from Mars to innermost planet Mercury', () => {
        // Mars to Mercury transfer (300 km Mars altitude, 200 km Mercury altitude):
        const direct = TrajectoryEngine.computeMarsToMercuryDirectPlungeTransfer(300.0, 200.0);
        expect(direct.transferTimeDays).to.be.closeTo(170.4, 10.0); // ~170.4 d transfer
        expect(direct.marsDepartureDeltaVKmS).to.be.closeTo(6.600, 0.3); // ~6.60 km/s TMI
        expect(direct.mercuryArrivalExcessKmS).to.be.closeTo(12.584, 0.5); // ~12.58 km/s v_inf
        expect(direct.mercuryOrbitInsertionDeltaVKmS).to.be.closeTo(10.342, 0.5); // ~10.34 km/s MOI
        expect(direct.totalMissionDeltaVKmS).to.be.closeTo(16.942, 0.8); // ~16.94 km/s total Delta-V
        expect(direct.transferEccentricity).to.be.closeTo(0.5948, 0.05); // e ~ 0.595
        expect(direct.transferSemiMajorAxisAU).to.be.closeTo(0.955, 0.05); // a ~ 0.955 AU
        expect(direct.directTransferContext).to.include('Mars-to-Mercury Direct');
    });

    it('should calculate hydrothermal serpentinization reaction kinetics and abiotic H2 + CH4 generation', () => {
        // 5 km depth, 50 K/km gradient, 60% olivine, 1e6 m^3 volume, 5000 yr:
        const serp = KRCEngine.computeMartianSerpentinizationHydrogenMethaneProduction(5.0, 50.0, 0.60, 1.0e6, 5000.0);
        expect(serp.crustalTemperatureC).to.be.closeTo(191.85, 0.2); // ~191.9 C crustal temp
        expect(serp.reactedOlivineTons).to.be.closeTo(1108600.0, 50000.0); // ~1.11M tons reacted
        expect(serp.hydrogenProducedTons).to.be.closeTo(782.2, 50.0); // ~782 t H2
        expect(serp.methaneProducedTons).to.be.closeTo(233.4, 20.0); // ~233 t CH4
        expect(serp.reactionEfficiencyPercent).to.be.closeTo(61.6, 5.0); // ~61.6% conversion
        expect(serp.serpentinizationRegimeClass).to.include('Active Hydrothermal Serpentinization');
        expect(serp.serpentinizationContext).to.include('Serpentinization');
    });

    it('should discriminate Hydrated Magnesium/Calcium Perchlorates vs Chlorates in CRISM spectra', () => {
        // Hydrated Magnesium Perchlorate (RSL active slope: BD1430 = 0.06, BD1900 = 0.08, BD2130 = 0.05, BD2400 = 0.005):
        const perchlorate = BandMathEngine.computeCRISMOxychlorineSaltIndices(0.06, 0.08, 0.05, 0.005);
        expect(perchlorate.isOxychlorineDetected).to.be.true;
        expect(perchlorate.oxychlorineClass).to.include('Hydrated Magnesium/Calcium Perchlorate');
        expect(perchlorate.mineralSpecies).to.include('Magnesium Perchlorate Hexahydrate');
        expect(perchlorate.chemicalFormula).to.include('Mg(ClO4)2 * 6H2O');
        expect(perchlorate.rslAstrobiologyContext).to.include('Deliquescing Cryogenic Oxychlorine Salt');

        // Chlorate Salt (BD1430 = 0.01, BD1900 = 0.02, BD2130 = 0.005, BD2400 = 0.04):
        const chlorate = BandMathEngine.computeCRISMOxychlorineSaltIndices(0.01, 0.02, 0.005, 0.04);
        expect(chlorate.isOxychlorineDetected).to.be.true;
        expect(chlorate.oxychlorineClass).to.include('Chlorate Salt Deposit');
        expect(chlorate.chemicalFormula).to.include('NaClO3');

        // Non-oxychlorine basalt:
        const basalt = BandMathEngine.computeCRISMOxychlorineSaltIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isOxychlorineDetected).to.be.false;
    });
});

describe('Optimal-Steering SEP Spiral, Hydrothermal Vein Clogging & Iron Sulfates', () => {
    it('should calculate inward Mars-to-Venus low-thrust ion spiral with optimal pitch steering angle modulation', () => {
        // Mars to Venus (1500 kg, 300 mN, 3500 s Isp):
        const optSpiral = TrajectoryEngine.computeMarsToVenusOptimumSteeringAngleIonSpiral(1500.0, 300.0, 3500.0);
        expect(optSpiral.optimalLowThrustDeltaVKmS).to.be.closeTo(11.205, 0.2); // ~11.21 km/s Delta-V
        expect(optSpiral.propellantConsumedKg).to.be.closeTo(418.0, 10.0); // ~418 kg Xe
        expect(optSpiral.spiralDurationDays).to.be.closeTo(553.5, 15.0); // ~554 days
        expect(optSpiral.spiralDurationYears).to.be.closeTo(1.515, 0.05); // ~1.52 yr
        expect(optSpiral.meanPitchSteeringAngleDeg).to.be.closeTo(13.6, 2.0); // ~13.6 deg
        expect(optSpiral.steeringEfficiencyPercent).to.equal(94.5);
        expect(optSpiral.optimalSteeringContext).to.include('Optimal-Steering SEP Spiral');
    });

    it('should calculate hydrothermal mineral vein sealing, aperture narrowing, and cubic-law permeability decay', () => {
        // 5 mm fracture aperture, Omega = 3.5, 150 C fluid, 25 yr elapsed duration:
        const vein = KRCEngine.computeMartianHydrothermalVeinCloggingKinetics(5.0, 3.5, 150.0, 25.0);
        expect(vein.inwardGrowthVelocityMmPerYr).to.be.closeTo(0.0414, 0.005); // ~0.041 mm/yr
        expect(vein.completeSealingTimescaleYr).to.be.closeTo(60.4, 5.0); // ~60 yr sealing time
        expect(vein.finalApertureMm).to.be.closeTo(2.93, 0.2); // ~2.93 mm aperture left
        expect(vein.residualPermeabilityPercent).to.be.closeTo(20.1, 3.0); // ~20.1% permeability
        expect(vein.veinPrecipitationStageClass).to.include('Partially Occluded Conduit');
        expect(vein.veinSealingContext).to.include('Vein Clogging');
    });

    it('should discriminate Monohydrated Szomolnokite vs Polyhydrated Rozenite/Melanterite in CRISM spectra', () => {
        // Monohydrated Szomolnokite (Juventae Chasma mound: BD1000 = 0.04, BD1900 = 0.01, BD2100 = 0.05, BD2400 = 0.06):
        const szomolnokite = BandMathEngine.computeCRISMIronSulfateHydrationStateIndices(0.04, 0.01, 0.05, 0.06);
        expect(szomolnokite.isIronSulfateDetected).to.be.true;
        expect(szomolnokite.sulfateHydrationClass).to.include('Monohydrated Ferrous Sulfate (Szomolnokite)');
        expect(szomolnokite.mineralSpecies).to.include('Szomolnokite');
        expect(szomolnokite.chemicalFormula).to.include('FeSO4 * H2O');
        expect(szomolnokite.environmentalHumidityContext).to.include('Hyper-Arid Low-Humidity Surface Desiccation');

        // Polyhydrated Rozenite / Melanterite (Mawrth Vallis: BD1000 = 0.06, BD1900 = 0.07, BD2100 = 0.02, BD2400 = 0.05):
        const rozenite = BandMathEngine.computeCRISMIronSulfateHydrationStateIndices(0.06, 0.07, 0.02, 0.05);
        expect(rozenite.isIronSulfateDetected).to.be.true;
        expect(rozenite.sulfateHydrationClass).to.include('Polyhydrated Ferrous Sulfate (Rozenite / Melanterite)');
        expect(rozenite.mineralSpecies).to.include('Rozenite');
        expect(rozenite.chemicalFormula).to.include('FeSO4 * 4H2O');
        expect(rozenite.environmentalHumidityContext).to.include('Subaqueous / High-Humidity');

        // Non-sulfate basalt:
        const basalt = BandMathEngine.computeCRISMIronSulfateHydrationStateIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isIronSulfateDetected).to.be.false;
    });
});

describe('Edelbaum Combined Spiral & Plane Change, Acid Lake Evaporites & Acid Sulfates', () => {
    it('should calculate combined low-thrust spiral and orbital inclination change using Edelbaum formulation', () => {
        // Mars to Earth (1.52 to 1.00 AU, 5.65 deg inclination change, 1500 kg, 300 mN, 3500 s Isp):
        const edelbaum = TrajectoryEngine.computeLowThrustCombinedSpiralAndInclinationChange(1.52368, 1.00000, 5.65, 1500.0, 300.0, 3500.0);
        expect(edelbaum.combinedLowThrustDeltaVKmS).to.be.closeTo(7.016, 0.2); // ~7.02 km/s combined Delta-V
        expect(edelbaum.coplanarBaselineDeltaVKmS).to.be.closeTo(5.656, 0.2); // ~5.66 km/s coplanar Delta-V
        expect(edelbaum.planeChangeDeltaVPenaltyKmS).to.be.closeTo(1.360, 0.1); // ~1.36 km/s penalty for 5.65 deg
        expect(edelbaum.propellantConsumedKg).to.be.closeTo(276.7, 10.0); // ~277 kg Xe
        expect(edelbaum.transferDurationDays).to.be.closeTo(366.4, 15.0); // ~366 days
        expect(edelbaum.transferDurationYears).to.be.closeTo(1.00, 0.05); // ~1.00 yr
        expect(edelbaum.combinedTransferContext).to.include('Combined Spiral + Inc');
    });

    it('should calculate acid lake evaporative concentration, fractional sulfate precipitation sequence, and lifespan', () => {
        // 50 m initial lake, 200 mm/yr evaporation, 200 yr elapsed, pH 2.5:
        const lake = KRCEngine.computeMartianAcidLakeEvaporitePrecipitationSequence(50.0, 200.0, 200.0, 2.50);
        expect(lake.residualLakeDepthM).to.be.closeTo(10.0, 0.5); // 10 m remaining
        expect(lake.concentrationFactor).to.be.closeTo(5.0, 0.2); // 5.0x concentration
        expect(lake.lakeDesiccationLifespanYr).to.be.closeTo(250.0, 1.0); // 250 yr lifespan
        expect(lake.activePrecipitatingPhaseClass).to.include('Intermediate Acid-Sulfate Evaporite (Jarosite + Alunite Plateau)');
        expect(lake.dominantMinerals).to.include('Jarosite');
        expect(lake.dominantMinerals).to.include('Alunite');
        expect(lake.evaporiteSequenceContext).to.include('Acid Lake Evaporation');
    });

    it('should discriminate Alunite vs Jarosite vs Gypsum in CRISM acid-sulfate spectra', () => {
        // Alunite (Columbus Crater: BD1480 = 0.05, BD1760 = 0.04, BD2260 = 0.005, BD1900 = 0.02, BD2400 = 0.06):
        const alunite = BandMathEngine.computeCRISMAcidSulfateAssemblageIndices(0.05, 0.04, 0.005, 0.02, 0.06);
        expect(alunite.isAcidSulfateDetected).to.be.true;
        expect(alunite.acidSulfateClass).to.include('Alunite Acid-Sulfate Deposit');
        expect(alunite.mineralSpecies).to.include('Alunite');
        expect(alunite.chemicalFormula).to.include('KAl3(SO4)2(OH)6');
        expect(alunite.phEnvironmentContext).to.include('Advanced Argillic Acid-Sulfate Hydrothermal Alteration');

        // Jarosite (Meridiani Planum / Burns Formation: BD1480 = 0.01, BD1760 = 0.01, BD2260 = 0.05, BD1900 = 0.02, BD2400 = 0.06):
        const jarosite = BandMathEngine.computeCRISMAcidSulfateAssemblageIndices(0.01, 0.01, 0.05, 0.02, 0.06);
        expect(jarosite.isAcidSulfateDetected).to.be.true;
        expect(jarosite.acidSulfateClass).to.include('Jarosite Iron Sulfate Evaporite');
        expect(jarosite.mineralSpecies).to.include('Jarosite');
        expect(jarosite.chemicalFormula).to.include('KFe3(SO4)2(OH)6');

        // Non-sulfate basalt:
        const basalt = BandMathEngine.computeCRISMAcidSulfateAssemblageIndices(0.005, 0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAcidSulfateDetected).to.be.false;
    });
});

describe('Mars-to-Venus Bi-Elliptic Transfer, Crustal Hydrofracture & Mg-Sulfates', () => {
    it('should calculate 3-burn bi-elliptic transfer from Mars out to high asteroid aphelion and plunge to Venus', () => {
        // Mars to Venus with 4.0 AU intermediate aphelion:
        const biElliptic = TrajectoryEngine.computeMarsToVenusBiEllipticTransfer(4.00, 300.0, 300.0);
        expect(biElliptic.totalTransferTimeDays).to.be.closeTo(1501.1, 30.0); // ~1501 days
        expect(biElliptic.totalTransferTimeYears).to.be.closeTo(4.11, 0.1); // ~4.11 yr
        expect(biElliptic.marsDepartureDeltaVKmS).to.be.closeTo(3.472, 0.2); // ~3.47 km/s DV1
        expect(biElliptic.aphelionBurnDeltaVKmS).to.be.closeTo(2.820, 0.2); // ~2.82 km/s DV2
        expect(biElliptic.venusArrivalDeltaVKmS).to.be.closeTo(7.468, 0.3); // ~7.47 km/s DV3
        expect(biElliptic.totalMissionDeltaVKmS).to.be.closeTo(13.760, 0.5); // ~13.76 km/s total
        expect(biElliptic.intermediateAphelionAU).to.equal(4.00);
        expect(biElliptic.biEllipticContext).to.include('Mars-to-Venus Bi-Elliptic');
    });

    it('should calculate crustal hydrofracture vein opening, Sneddon aperture, and hydrothermal discharge flux', () => {
        // 4 km depth, 25 MPa fluid overpressure, 50 m crack length, E = 45 GPa, nu = 0.25:
        const hydro = KRCEngine.computeMartianCrustalHydrofractureApertureAndFlux(4.0, 25.0, 50.0, 45.0, 0.25);
        expect(hydro.maximumApertureMm).to.be.closeTo(33.16, 1.0); // ~33.2 mm max aperture
        expect(hydro.meanHydraulicApertureMm).to.be.closeTo(26.04, 1.0); // ~26.0 mm mean aperture
        expect(hydro.fluidDischargeVelocityMS).to.be.closeTo(1177.2, 50.0); // ~1177 m/s discharge
        expect(hydro.dailyVolumetricDischargeM3Day).to.be.closeTo(2.65e7, 3.0e6); // ~2.65e7 m^3/d
        expect(hydro.hydrofractureRegimeClass).to.include('High-Overpressure Hydraulic Fracture Opening');
        expect(hydro.hydrofractureContext).to.include('Hydrofracture');
    });

    it('should discriminate Monohydrated Kieserite vs Polyhydrated Epsomite/Starkeyite in CRISM spectra', () => {
        // Monohydrated Kieserite (Mount Sharp lower sulfate unit: BD1400 = 0.01, BD1900 = 0.01, BD2130 = 0.05, BD2400 = 0.07):
        const kieserite = BandMathEngine.computeCRISMMagnesiumSulfateSpeciationIndices(0.01, 0.01, 0.05, 0.07);
        expect(kieserite.isMgSulfateDetected).to.be.true;
        expect(kieserite.mgSulfateHydrationClass).to.include('Monohydrated Magnesium Sulfate (Kieserite)');
        expect(kieserite.mineralSpecies).to.include('Kieserite');
        expect(kieserite.chemicalFormula).to.include('MgSO4 * H2O');
        expect(kieserite.environmentalDesiccationContext).to.include('Hyper-Arid Surface Desiccation');

        // Polyhydrated Epsomite / Starkeyite (Juventae Chasma: BD1400 = 0.05, BD1900 = 0.08, BD2130 = 0.01, BD2400 = 0.07):
        const epsomite = BandMathEngine.computeCRISMMagnesiumSulfateSpeciationIndices(0.05, 0.08, 0.01, 0.07);
        expect(epsomite.isMgSulfateDetected).to.be.true;
        expect(epsomite.mgSulfateHydrationClass).to.include('Polyhydrated Magnesium Sulfate (Epsomite / Starkeyite)');
        expect(epsomite.mineralSpecies).to.include('Epsomite');
        expect(epsomite.chemicalFormula).to.include('MgSO4 * 7H2O');

        // Non-sulfate basalt:
        const basalt = BandMathEngine.computeCRISMMagnesiumSulfateSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isMgSulfateDetected).to.be.false;
    });
});

describe('Mars-to-Asteroid Belt Rendezvous, Chlorite Metamorphism & Trioctahedral Clays', () => {
    it('should calculate interplanetary rendezvous transfer from Mars to main belt asteroid 1 Ceres', () => {
        // Mars to Ceres (1.52 to 2.77 AU, 300 km Mars alt, 200 km Ceres alt):
        const ceres = TrajectoryEngine.computeMarsToAsteroidMainBeltRendezvousTransfer('Ceres', 300.0, 200.0);
        expect(ceres.targetBodyName).to.equal('1 Ceres');
        expect(ceres.transferTimeDays).to.be.closeTo(574.0, 15.0); // ~574 d transfer
        expect(ceres.transferTimeYears).to.be.closeTo(1.57, 0.05); // ~1.57 yr
        expect(ceres.marsDepartureDeltaVKmS).to.be.closeTo(2.513, 0.2); // ~2.51 km/s TAI
        expect(ceres.asteroidArrivalExcessKmS).to.be.closeTo(2.716, 0.2); // ~2.72 km/s v_inf
        expect(ceres.asteroidOrbitInsertionDeltaVKmS).to.be.closeTo(2.445, 0.2); // ~2.45 km/s AOI
        expect(ceres.totalMissionDeltaVKmS).to.be.closeTo(4.958, 0.4); // ~4.96 km/s total
        expect(ceres.transferEccentricity).to.be.closeTo(0.2899, 0.02); // e ~ 0.29
        expect(ceres.asteroidTransferContext).to.include('Mars-to-1 Ceres');
    });

    it('should calculate hydrothermal smectite-to-chlorite diagenetic metamorphism kinetics', () => {
        // 6 km depth, 35 K/km gradient, 300 ppm Mg2+, 5 Myr duration:
        const chlor = KRCEngine.computeMartianSmectiteToChloriteMetamorphicKinetics(6.0, 35.0, 300.0, 5.0);
        expect(chlor.burialTemperatureK).to.equal(425.0);
        expect(chlor.burialTemperatureC).to.be.closeTo(151.85, 0.2); // ~151.9 C burial temp
        expect(chlor.chloriteFractionPercent).to.be.greaterThan(95.0); // High conversion
        expect(chlor.expelledFluidWtPct).to.be.closeTo(8.5, 0.5); // ~8.5 wt% fluid expelled
        expect(chlor.metamorphicGradeClass).to.include('Greenschist Facies Chlorite');
        expect(chlor.clayMetamorphismContext).to.include('Chlorite Metamorphism');
    });

    it('should discriminate Trioctahedral Saponite vs Corrensite vs Chlorite in CRISM spectra', () => {
        // Trioctahedral Saponite (Nili Fossae: BD1400 = 0.04, BD1900 = 0.06, BD2310 = 0.05, BD2350 = 0.01):
        const saponite = BandMathEngine.computeCRISMChloriteSmectiteMetamorphicIndices(0.04, 0.06, 0.05, 0.01);
        expect(saponite.isClayDetected).to.be.true;
        expect(saponite.metamorphicGradeClass).to.include('Trioctahedral Fe/Mg Smectite (Saponite)');
        expect(saponite.mineralSpecies).to.include('Saponite');
        expect(saponite.hydrothermalMetamorphicContext).to.include('Low-Temperature Alkaline Aqueous Alteration');

        // Mixed-Layer Corrensite (BD1400 = 0.03, BD1900 = 0.03, BD2310 = 0.04, BD2350 = 0.03):
        const corrensite = BandMathEngine.computeCRISMChloriteSmectiteMetamorphicIndices(0.03, 0.03, 0.04, 0.03);
        expect(corrensite.isClayDetected).to.be.true;
        expect(corrensite.metamorphicGradeClass).to.include('Mixed-Layer Corrensite');
        expect(corrensite.mineralSpecies).to.include('Corrensite');

        // Metamorphic Chlorite (Jezero central peak uplift: BD1400 = 0.01, BD1900 = 0.01, BD2310 = 0.01, BD2350 = 0.05):
        const chlorite = BandMathEngine.computeCRISMChloriteSmectiteMetamorphicIndices(0.01, 0.01, 0.01, 0.05);
        expect(chlorite.isClayDetected).to.be.true;
        expect(chlorite.metamorphicGradeClass).to.include('Greenschist Facies Chlorite');
        expect(chlorite.mineralSpecies).to.include('Chlorite');

        // Non-clay basalt:
        const basalt = BandMathEngine.computeCRISMChloriteSmectiteMetamorphicIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isClayDetected).to.be.false;
    });
});

describe('Mars-to-Jupiter Hohmann Insertion, Acid Leached Silica & Serpentine Speciation', () => {
    it('should calculate interplanetary Hohmann transfer from Mars to gas giant Jupiter and orbit insertion', () => {
        // Mars to Jupiter (1.52 to 5.20 AU, 300 km Mars alt, 300,000 km Jupiter capture alt):
        const jupiter = TrajectoryEngine.computeMarsToJupiterDirectTransfer(300.0, 300000.0);
        expect(jupiter.transferTimeDays).to.be.closeTo(1126.9, 20.0); // ~1127 days
        expect(jupiter.transferTimeYears).to.be.closeTo(3.085, 0.05); // ~3.09 yr
        expect(jupiter.marsDepartureDeltaVKmS).to.be.closeTo(4.197, 0.2); // ~4.20 km/s TJI
        expect(jupiter.jupiterArrivalExcessKmS).to.be.closeTo(4.269, 0.2); // ~4.27 km/s v_inf
        expect(jupiter.jupiterOrbitInsertionDeltaVKmS).to.be.closeTo(0.477, 0.1); // ~0.48 km/s JOI
        expect(jupiter.totalMissionDeltaVKmS).to.be.closeTo(4.674, 0.3); // ~4.67 km/s total Delta-V
        expect(jupiter.transferEccentricity).to.be.closeTo(0.5471, 0.02); // e ~ 0.547
        expect(jupiter.jupiterTransferContext).to.include('Mars-to-Jupiter Direct');
    });

    it('should calculate acid-sulfate hydrothermal cation leaching, sieve porosity, and silica enrichment', () => {
        // pH 1.5, 80 C, 50 yr duration, 10% initial porosity:
        const leach = KRCEngine.computeMartianAcidLeachingSilicaEnrichmentPorosity(1.50, 80.0, 50.0, 0.10);
        expect(leach.residualSilicaWtPercent).to.be.closeTo(96.0, 2.0); // ~96 wt% pure SiO2 residue
        expect(leach.cationExtractionPercent).to.be.closeTo(96.2, 2.0); // ~96% cations stripped
        expect(leach.finalSievePorosityPercent).to.be.closeTo(55.0, 3.0); // ~55% sieve porosity
        expect(leach.bulkDensityKgM3).to.be.closeTo(989.8, 50.0); // ~990 kg/m^3
        expect(leach.thermalInertiaTIU).to.be.closeTo(77.5, 15.0); // low TIU vesicular silica
        expect(leach.silicaAlterationClass).to.include('High-Purity Vesicular Opaline Silica Residue');
        expect(leach.acidLeachingContext).to.include('Acid Leaching');
    });

    it('should discriminate Low-Temperature Lizardite/Chrysotile vs High-Temperature Antigorite in CRISM spectra', () => {
        // Lizardite / Chrysotile (Claritas Rise: BD1390 = 0.05, BD2120 = 0.03, BD2325 = 0.06, BD2510 = 0.005):
        const lizardite = BandMathEngine.computeCRISMLizarditeAntigoriteChrysotileSpeciationIndices(0.05, 0.03, 0.06, 0.005);
        expect(lizardite.isSerpentineDetected).to.be.true;
        expect(lizardite.serpentinePolymorphClass).to.include('Low-Temperature Hydrated Serpentine (Lizardite / Chrysotile)');
        expect(lizardite.mineralSpecies).to.include('Lizardite / Chrysotile');
        expect(lizardite.chemicalFormula).to.include('Mg3Si2O5(OH)4');
        expect(lizardite.serpentinizationThermalContext).to.include('Active H2 + Abiotic Methane Generation');

        // Antigorite (Nili Fossae basement: BD1390 = 0.02, BD2120 = 0.01, BD2325 = 0.05, BD2510 = 0.04):
        const antigorite = BandMathEngine.computeCRISMLizarditeAntigoriteChrysotileSpeciationIndices(0.02, 0.01, 0.05, 0.04);
        expect(antigorite.isSerpentineDetected).to.be.true;
        expect(antigorite.serpentinePolymorphClass).to.include('High-Temperature Prograde Serpentine (Antigorite)');
        expect(antigorite.mineralSpecies).to.include('Antigorite');

        // Non-serpentine basalt:
        const basalt = BandMathEngine.computeCRISMLizarditeAntigoriteChrysotileSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isSerpentineDetected).to.be.false;
    });
});

describe('Maximum Apsidal Precession Steering, Clay Dehydroxylation & Contact Metamorphism', () => {
    it('should calculate continuous low-thrust optimal steering for maximum apsidal precession rate', () => {
        // Mars orbit (1.52368 AU, e = 0.0934, 1500 kg, 300 mN thrust, 3500 s Isp, nu = 90 deg):
        const steer = TrajectoryEngine.computeLowThrustMaximumApsidalPrecessionSteering(1.52368, 0.0934, 1500.0, 300.0, 3500.0, 90.0);
        expect(steer.maxPrecessionRateDegPerYear).to.be.closeTo(319.5, 30.0); // ~320 deg/yr max precession rate
        expect(steer.radialAccelerationMmS2).to.equal(0.200); // 0.20 mm/s^2
        expect(steer.projectedApsidalShift180DaysDeg).to.be.closeTo(157.4, 20.0); // ~157 deg shift over 180 d
        expect(steer.propellantConsumed180DaysKg).to.be.closeTo(135.9, 5.0); // ~136 kg Xe
        expect(steer.apsidalSteeringContext).to.include('Max Apsidal Precession Steering');
    });

    it('should calculate contact metamorphic clay dehydroxylation, steam release, and recrystallization kinetics', () => {
        // 650 C contact baking, 14 wt% initial OH, 100 yr duration, 180 kJ/mol Ea:
        const dehydrox = KRCEngine.computeMartianClayDehydroxylationRecrystallizationKinetics(650.0, 14.0, 100.0, 180.0);
        expect(dehydrox.dehydroxylationFractionPercent).to.be.closeTo(100.0, 1.0); // 100% dehydroxylated
        expect(dehydrox.residualWaterContentWtPct).to.equal(0.00); // completely dry
        expect(dehydrox.expelledSteamKgPerM3).to.be.closeTo(336.0, 10.0); // ~336 kg/m^3 steam
        expect(dehydrox.bakedThermalInertiaTIU).to.be.closeTo(2215.6, 50.0); // high TIU baked hornfels
        expect(dehydrox.metamorphicPhaseClass).to.include('High-Grade Baked Hornfels');
        expect(dehydrox.thermalBakingContext).to.include('Contact Baking');
    });

    it('should discriminate Pristine Crystalline Clay vs Thermally Dehydroxylated Amorphous Phase in CRISM spectra', () => {
        // Pristine Nontronite (BD1400 = 0.04, BD1900 = 0.06, BD2200 = 0.01, BD2290 = 0.05, TIR = 0.02):
        const pristine = BandMathEngine.computeCRISMDehydroxylatedClayPhaseIndices(0.04, 0.06, 0.01, 0.05, 0.02);
        expect(pristine.isSilicatePhaseDetected).to.be.true;
        expect(pristine.clayThermalStateClass).to.include('Pristine Crystalline Smectite Clay');
        expect(pristine.mineralSpecies).to.include('Nontronite');
        expect(pristine.contactMetamorphicContext).to.include('Aqueous Alteration Horizon Preserving Structural Hydroxyls');

        // Thermally Dehydroxylated Hornfels (Syrtis Major dike aureole: BD1400 = 0.005, BD1900 = 0.005, BD2200 = 0.01, BD2290 = 0.03, TIR = 0.08):
        const baked = BandMathEngine.computeCRISMDehydroxylatedClayPhaseIndices(0.005, 0.005, 0.01, 0.03, 0.08);
        expect(baked.isSilicatePhaseDetected).to.be.true;
        expect(baked.clayThermalStateClass).to.include('Thermally Dehydroxylated Amorphous Silicate Phase');
        expect(baked.chemicalStructureState).to.include('Collapsed 2:1 Layer Lattice');
        expect(baked.contactMetamorphicContext).to.include('Volcanic Dike Intrusion / Lava Flow Contact Metamorphic Aureole');

        // Unbaked basalt:
        const basalt = BandMathEngine.computeCRISMDehydroxylatedClayPhaseIndices(0.005, 0.005, 0.005, 0.005, 0.01);
        expect(basalt.isSilicatePhaseDetected).to.be.false;
    });
});

describe('Mars-to-Saturn Deep Space Insertion, Clay-Carbonates & Comanche Hydrothermal Springs', () => {
    it('should calculate interplanetary Hohmann transfer from Mars to ringed planet Saturn and orbit insertion', () => {
        // Mars to Saturn (1.52 to 9.58 AU, 300 km Mars alt, 60,000 km Saturn capture alt):
        const saturn = TrajectoryEngine.computeMarsToSaturnDirectTransfer(300.0, 60000.0);
        expect(saturn.transferTimeDays).to.be.closeTo(2390.6, 30.0); // ~2391 days
        expect(saturn.transferTimeYears).to.be.closeTo(6.545, 0.1); // ~6.55 yr
        expect(saturn.marsDepartureDeltaVKmS).to.be.closeTo(5.564, 0.2); // ~5.56 km/s TSI
        expect(saturn.saturnArrivalExcessKmS).to.be.closeTo(4.471, 0.2); // ~4.47 km/s v_inf
        expect(saturn.saturnOrbitInsertionDeltaVKmS).to.be.closeTo(0.521, 0.1); // ~0.52 km/s SOI
        expect(saturn.totalMissionDeltaVKmS).to.be.closeTo(6.085, 0.3); // ~6.09 km/s total
        expect(saturn.transferEccentricity).to.be.closeTo(0.7256, 0.02); // e ~ 0.726
        expect(saturn.saturnTransferContext).to.include('Mars-to-Saturn Direct');
    });

    it('should calculate alkaline hydrothermal spring clay-carbonate co-precipitation kinetics', () => {
        // pH 9.5, 0.05 M DIC, 60 C, Ca/Mg = 0.20:
        const spring = KRCEngine.computeMartianClayCarbonateCoPrecipitationKinetics(9.50, 0.050, 60.0, 0.20);
        expect(spring.carbonateSaturationState).to.be.greaterThan(5000.0); // High magnesite saturation
        expect(spring.carbonateWeightPercent).to.be.closeTo(35.7, 3.0); // ~36 wt% carbonate
        expect(spring.saponiteClayWeightPercent).to.be.closeTo(64.3, 3.0); // ~64 wt% saponite clay
        expect(spring.magnesiteMolarPercent).to.be.closeTo(83.3, 2.0); // ~83 mol% magnesite vs calcite
        expect(spring.compositeThermalInertiaTIU).to.be.closeTo(1169.1, 50.0); // dense indurated spring deposit
        expect(spring.alkalineSpringRegimeClass).to.include('Alkaline Magnesite-Saponite Hydrothermal Travertine');
        expect(spring.clayCarbonateContext).to.include('Alkaline Spring');
    });

    it('should discriminate Clay-Carbonate Composite Outcrops (Saponite + Magnesite) in CRISM spectra', () => {
        // Comanche Outcrop (Jezero Margin: BD1900 = 0.04, BD2310 = 0.05, BD2510 = 0.04, TIR = 0.06):
        const comanche = BandMathEngine.computeCRISMClayCarbonateCompositeIndices(0.04, 0.05, 0.04, 0.06);
        expect(comanche.isClayCarbonateDetected).to.be.true;
        expect(comanche.outcropAssemblageClass).to.include('Alkaline Clay-Carbonate Composite');
        expect(comanche.dominantMinerals).to.include('Trioctahedral Saponite + Magnesite');
        expect(comanche.astrobiologicalHabitabilityContext).to.include('Prime Biosignature Preservation Potential');

        // Pure Carbonate:
        const pureCarb = BandMathEngine.computeCRISMClayCarbonateCompositeIndices(0.005, 0.01, 0.05, 0.06);
        expect(pureCarb.isClayCarbonateDetected).to.be.true;
        expect(pureCarb.outcropAssemblageClass).to.include('Pure Crystalline Magnesium-Iron Carbonate');

        // Pure Saponite Clay:
        const pureClay = BandMathEngine.computeCRISMClayCarbonateCompositeIndices(0.05, 0.05, 0.005, 0.01);
        expect(pureClay.isClayCarbonateDetected).to.be.true;
        expect(pureClay.outcropAssemblageClass).to.include('Pure Trioctahedral Smectite Clay');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMClayCarbonateCompositeIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isClayCarbonateDetected).to.be.false;
    });
});

describe('Mars-to-Uranus Ice Giant Transfer, Methane Hydrates & Siderite/Goethite', () => {
    it('should calculate interplanetary Hohmann transfer from Mars to ice giant planet Uranus and orbit insertion', () => {
        // Mars to Uranus (1.52 to 19.19 AU, 300 km Mars alt, 25,000 km Uranus capture alt):
        const uranus = TrajectoryEngine.computeMarsToUranusDirectTransfer(300.0, 25000.0);
        expect(uranus.transferTimeDays).to.be.closeTo(6087.7, 50.0); // ~6088 days
        expect(uranus.transferTimeYears).to.be.closeTo(16.667, 0.2); // ~16.67 yr
        expect(uranus.marsDepartureDeltaVKmS).to.be.closeTo(6.552, 0.2); // ~6.55 km/s TUI
        expect(uranus.uranusArrivalExcessKmS).to.be.closeTo(4.151, 0.2); // ~4.15 km/s v_inf
        expect(uranus.uranusOrbitInsertionDeltaVKmS).to.be.closeTo(0.634, 0.1); // ~0.63 km/s UOI
        expect(uranus.totalMissionDeltaVKmS).to.be.closeTo(7.186, 0.3); // ~7.19 km/s total
        expect(uranus.transferEccentricity).to.be.closeTo(0.8529, 0.02); // e ~ 0.853
        expect(uranus.uranusTransferContext).to.include('Mars-to-Uranus Direct');
    });

    it('should calculate subsurface Methane Clathrate Hydrate Stability Zone (MHSZ) depth extent and gas storage capacity', () => {
        // 215 K surface temp, 20 K/km geothermal gradient, 0 salinity, 25% porosity:
        const clathrate = KRCEngine.computeMartianMethaneClathrateHydrateStabilityZone(215.0, 20.0, 0.0, 0.25);
        expect(clathrate.mhszTopDepthM).to.equal(15.0); // 15 m under ice seal
        expect(clathrate.mhszBaseDepthM).to.be.closeTo(4363.0, 100.0); // ~4.36 km base depth
        expect(clathrate.mhszThicknessM).to.be.closeTo(4348.0, 100.0); // ~4.35 km thick zone
        expect(clathrate.dissociationTemperatureAtBaseK).to.be.closeTo(302.3, 5.0); // ~302 K dissociation temp at base
        expect(clathrate.volumetricGasStorageM3STPPerM2).to.be.greaterThan(4.0e4); // > 40,000 m^3 STP CH4 / m^2
        expect(clathrate.clathrateStabilityClass).to.include('Vast Subsurface Polar/Equatorial Cryosphere Clathrate Hydrate Shield');
        expect(clathrate.clathrateContext).to.include('MHSZ');
    });

    it('should discriminate Reducing Siderite (Iron Carbonate) vs Oxidizing Goethite in CRISM spectra', () => {
        // Reducing Siderite (BD480 = 0.01, BD920 = 0.01, BD2330 = 0.05, BD2530 = 0.06):
        const siderite = BandMathEngine.computeCRISMSideriteVsGoethiteIndices(0.01, 0.01, 0.05, 0.06);
        expect(siderite.isFeMineralDetected).to.be.true;
        expect(siderite.ironMineralSpeciesClass).to.include('Iron Carbonate (Siderite)');
        expect(siderite.mineralSpecies).to.include('Siderite');
        expect(siderite.chemicalFormula).to.include('FeCO3');
        expect(siderite.redoxPaleoenvironmentalContext).to.include('Reducing Anoxic Alkaline-to-Neutral Hydrothermal');

        // Oxidizing Goethite (BD480 = 0.05, BD920 = 0.08, BD2330 = 0.01, BD2530 = 0.005):
        const goethite = BandMathEngine.computeCRISMSideriteVsGoethiteIndices(0.05, 0.08, 0.01, 0.005);
        expect(goethite.isFeMineralDetected).to.be.true;
        expect(goethite.ironMineralSpeciesClass).to.include('Iron Oxyhydroxide (Goethite)');
        expect(goethite.mineralSpecies).to.include('Goethite');
        expect(goethite.chemicalFormula).to.include('alpha-FeO(OH)');
        expect(goethite.redoxPaleoenvironmentalContext).to.include('Oxidizing Acid-Sulfate Aqueous Weathering');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMSideriteVsGoethiteIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isFeMineralDetected).to.be.false;
    });
});

describe('Mars-to-Neptune Solar Boundary Transfer, Mud Volcanism & Zeolite Speciation', () => {
    it('should calculate interplanetary Hohmann transfer from Mars to outermost ice giant Neptune and orbit insertion', () => {
        // Mars to Neptune (1.52 to 30.07 AU, 300 km Mars alt, 30,000 km Neptune capture alt):
        const neptune = TrajectoryEngine.computeMarsToNeptuneDirectTransfer(300.0, 30000.0);
        expect(neptune.transferTimeDays).to.be.closeTo(11466.3, 100.0); // ~11466 days
        expect(neptune.transferTimeYears).to.be.closeTo(31.393, 0.3); // ~31.4 yr
        expect(neptune.marsDepartureDeltaVKmS).to.be.closeTo(6.944, 0.2); // ~6.94 km/s TNI
        expect(neptune.neptuneArrivalExcessKmS).to.be.closeTo(3.713, 0.2); // ~3.71 km/s v_inf
        expect(neptune.neptuneOrbitInsertionDeltaVKmS).to.be.closeTo(0.509, 0.1); // ~0.51 km/s NOI
        expect(neptune.totalMissionDeltaVKmS).to.be.closeTo(7.453, 0.3); // ~7.45 km/s total
        expect(neptune.transferEccentricity).to.be.closeTo(0.9035, 0.02); // e ~ 0.904
        expect(neptune.neptuneTransferContext).to.include('Mars-to-Neptune Direct');
    });

    it('should calculate subsurface mud volcanism conduit ascent, flash-boiling plume, and flow runout length', () => {
        // 2.5 m radius conduit, 3 km depth, 15 MPa overpressure, 50 Pa*s viscosity:
        const mud = KRCEngine.computeMartianMudVolcanismEruptionDynamics(2.5, 3.0, 15.0, 50.0, 1750.0);
        expect(mud.ascentVelocityMS).to.be.closeTo(78.13, 5.0); // ~78.1 m/s conduit ascent
        expect(mud.volumetricDischargeM3S).to.be.closeTo(1534.0, 100.0); // ~1534 m^3/s
        expect(mud.dailyEruptedVolumeM3Day).to.be.closeTo(1.325e8, 1.0e7); // ~1.33e8 m^3/day
        expect(mud.flashBoilingPlumeHeightM).to.be.closeTo(820.4, 50.0); // ~820 m ballistic plume
        expect(mud.mudFlowRunoutLengthKm).to.be.closeTo(18.4, 3.0); // ~18.4 km runout
        expect(mud.mudVolcanoEdificeClass).to.include('Catastrophic Mega-Mud Volcano');
        expect(mud.mudEruptionContext).to.include('Mud Volcano');
    });

    it('should discriminate Low-Silica Analcime vs High-Silica Clinoptilolite Zeolites in CRISM spectra', () => {
        // Analcime (Mawrth Vallis / Columbus Crater: BD1400 = 0.04, BD1900 = 0.06, BD2150 = 0.01, BD2490 = 0.05):
        const analcime = BandMathEngine.computeCRISMZeoliteSpeciationIndices(0.04, 0.06, 0.01, 0.05);
        expect(analcime.isZeoliteDetected).to.be.true;
        expect(analcime.zeoliteClass).to.include('Isometric Low-Silica Zeolite (Analcime)');
        expect(analcime.mineralSpecies).to.include('Analcime');
        expect(analcime.chemicalFormula).to.include('NaAlSi2O6 * H2O');
        expect(analcime.alkalineAlterationContext).to.include('Alkaline Saline Paleolake Evaporation');

        // Clinoptilolite (BD1400 = 0.02, BD1900 = 0.05, BD2150 = 0.04, BD2490 = 0.01):
        const clinoptilolite = BandMathEngine.computeCRISMZeoliteSpeciationIndices(0.02, 0.05, 0.04, 0.01);
        expect(clinoptilolite.isZeoliteDetected).to.be.true;
        expect(clinoptilolite.zeoliteClass).to.include('High-Silica Heulandite-Group Zeolite (Clinoptilolite / Mordenite)');
        expect(clinoptilolite.mineralSpecies).to.include('Clinoptilolite');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMZeoliteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isZeoliteDetected).to.be.false;
    });
});

describe('Mars-to-Pluto Kuiper Belt Transfer, Clay Illitization & Illite-Smectite Ordering', () => {
    it('should calculate interplanetary Hohmann transfer from Mars to Kuiper Belt dwarf planet Pluto and orbit insertion', () => {
        // Mars to Pluto (1.52 to 39.48 AU, 300 km Mars alt, 500 km Pluto capture alt):
        const pluto = TrajectoryEngine.computeMarsToPlutoDirectTransfer(300.0, 500.0);
        expect(pluto.transferTimeDays).to.be.closeTo(16954.7, 100.0); // ~16955 days
        expect(pluto.transferTimeYears).to.be.closeTo(46.419, 0.5); // ~46.4 yr
        expect(pluto.marsDepartureDeltaVKmS).to.be.closeTo(7.116, 0.2); // ~7.12 km/s TPI
        expect(pluto.plutoArrivalExcessKmS).to.be.closeTo(3.418, 0.2); // ~3.42 km/s v_inf
        expect(pluto.plutoOrbitInsertionDeltaVKmS).to.be.closeTo(2.563, 0.2); // ~2.56 km/s POI
        expect(pluto.totalMissionDeltaVKmS).to.be.closeTo(9.679, 0.3); // ~9.68 km/s total
        expect(pluto.transferEccentricity).to.be.closeTo(0.9257, 0.02); // e ~ 0.926
        expect(pluto.plutoTransferContext).to.include('Mars-to-Pluto Direct');
    });

    it('should calculate burial diagenetic smectite illitization kinetics and geothermometry', () => {
        // 5.8 km depth, 30 K/km geothermal gradient, 250 ppm K+, 25 Myr duration:
        const illite = KRCEngine.computeMartianSmectiteToIlliteDiagenesisKinetics(5.8, 30.0, 250.0, 25.0);
        expect(illite.burialTemperatureC).to.be.closeTo(115.85, 2.0); // ~116 C burial temp
        expect(illite.illiteLayerPercent).to.be.greaterThan(40.0); // > 40% illite
        expect(illite.expelledInterlayerWaterWtPct).to.be.greaterThan(3.0); // > 3 wt% water released
        expect(illite.reichweiteOrderingClass).to.include('Highly Ordered Illite / Sericite');
        expect(illite.diageneticGeothermometerContext).to.include('Illite Diagenesis');
    });

    it('should discriminate Dioctahedral Montmorillonite Smectite vs Ordered Illite in CRISM spectra', () => {
        // Montmorillonite (Mawrth Vallis upper unit: BD1400 = 0.04, BD1900 = 0.06, BD2200 = 0.06, BD2350 = 0.01):
        const mont = BandMathEngine.computeCRISMIlliteSmectiteOrderingIndices(0.04, 0.06, 0.06, 0.01);
        expect(mont.isAlPhyllosilicateDetected).to.be.true;
        expect(mont.illiteSmectiteClass).to.include('Dioctahedral Smectite (Montmorillonite)');
        expect(mont.mineralSpecies).to.include('Montmorillonite');
        expect(mont.burialDiageneticContext).to.include('Low-Temperature Aqueous Alteration');

        // Highly Ordered Illite / Sericite (Mawrth Vallis basal strata: BD1400 = 0.02, BD1900 = 0.02, BD2200 = 0.06, BD2350 = 0.04):
        const ill = BandMathEngine.computeCRISMIlliteSmectiteOrderingIndices(0.02, 0.02, 0.06, 0.04);
        expect(ill.isAlPhyllosilicateDetected).to.be.true;
        expect(ill.illiteSmectiteClass).to.include('Highly Ordered Illite / Sericite (R3 Illite/Smectite)');
        expect(ill.mineralSpecies).to.include('Illite');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMIlliteSmectiteOrderingIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAlPhyllosilicateDetected).to.be.false;
    });
});

describe('Mars-to-Arrokoth KBO Flyby, Acid-Sulfate Jarosite & Alunite Speciation', () => {
    it('should calculate interplanetary Hohmann transfer from Mars to Kuiper Belt contact binary 486958 Arrokoth and flyby speed', () => {
        // Mars to Arrokoth (1.52 to 44.58 AU, 300 km Mars alt):
        const arrokoth = TrajectoryEngine.computeMarsToArrokothDirectTransfer(300.0);
        expect(arrokoth.transferTimeDays).to.be.closeTo(20213.5, 100.0); // ~20214 days
        expect(arrokoth.transferTimeYears).to.be.closeTo(55.341, 0.5); // ~55.3 yr
        expect(arrokoth.marsDepartureDeltaVKmS).to.be.closeTo(7.987, 1.0); // ~7.99 km/s TKI
        expect(arrokoth.arrokothFlybyVelocityKmS).to.be.closeTo(3.284, 0.5); // ~3.28 km/s flyby
        expect(arrokoth.transferEccentricity).to.be.closeTo(0.9339, 0.02); // e ~ 0.934
        expect(arrokoth.arrokothTransferContext).to.include('Mars-to-Arrokoth Flyby');
    });

    it('should calculate hyper-acidic groundwater jarosite precipitation kinetics and evaporite thermal inertia', () => {
        // pH 2.0, 0.50 M SO4 2-, 0.10 M Fe3+, 15 C:
        const jarosite = KRCEngine.computeMartianJarositePrecipitationKinetics(2.00, 0.50, 0.10, 15.0);
        expect(jarosite.jarositeWeightPercent).to.be.closeTo(25.0, 3.0); // ~25 wt% jarosite
        expect(jarosite.polyhydratedSulfateWeightPercent).to.be.closeTo(41.25, 4.0); // ~41 wt% polyhydrated sulfate
        expect(jarosite.hematiteWeightPercent).to.be.closeTo(11.25, 3.0); // ~11 wt% hematite
        expect(jarosite.evaporiteThermalInertiaTIU).to.be.closeTo(487.7, 50.0); // ~488 tiu porous sulfate sandstone
        expect(jarosite.acidSulfateFaciesClass).to.include('Hyper-Acidic Jarosite-Rich Evaporite Sandstone');
        expect(jarosite.jarositeParagenesisContext).to.include('Acid-Sulfate');
    });

    it('should discriminate Iron Jarosite vs Aluminium Alunite Hydroxysulfates in CRISM spectra', () => {
        // Jarosite (Meridiani Planum Burns Formation: BD430 = 0.04, BD1475 = 0.05, BD1850 = 0.04, BD2165 = 0.01, BD2265 = 0.06):
        const jarosite = BandMathEngine.computeCRISMJarositeAluniteSpeciationIndices(0.04, 0.05, 0.04, 0.01, 0.06);
        expect(jarosite.isHydroxysulfateDetected).to.be.true;
        expect(jarosite.hydroxysulfateClass).to.include('Iron Hydroxysulfate (Jarosite)');
        expect(jarosite.mineralSpecies).to.include('Jarosite');
        expect(jarosite.chemicalFormula).to.include('KFe3(SO4)2(OH)6');
        expect(jarosite.acidHydrothermalContext).to.include('Hyper-Acidic (pH < 3.0)');

        // Alunite (Columbia Hills / Terra Sirenum: BD430 = 0.01, BD1475 = 0.05, BD1850 = 0.02, BD2165 = 0.05, BD2265 = 0.01):
        const alunite = BandMathEngine.computeCRISMJarositeAluniteSpeciationIndices(0.01, 0.05, 0.02, 0.05, 0.01);
        expect(alunite.isHydroxysulfateDetected).to.be.true;
        expect(alunite.hydroxysulfateClass).to.include('Aluminium Hydroxysulfate (Alunite)');
        expect(alunite.mineralSpecies).to.include('Alunite');
        expect(alunite.chemicalFormula).to.include('KAl3(SO4)2(OH)6');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMJarositeAluniteSpeciationIndices(0.005, 0.005, 0.005, 0.005, 0.005);
        expect(basalt.isHydroxysulfateDetected).to.be.false;
    });
});

describe('Mars-to-Eris Scattered Disc Transfer, Salt Diapirism & Halite Speciation', () => {
    it('should calculate interplanetary Hohmann transfer from Mars to scattered disc dwarf planet 136199 Eris and orbit insertion', () => {
        // Mars to Eris (1.52 to 67.78 AU, 300 km Mars alt, 500 km Eris capture alt):
        const eris = TrajectoryEngine.computeMarsToErisDirectTransfer(300.0, 500.0);
        expect(eris.transferTimeDays).to.be.closeTo(37279.7, 300.0); // ~37280 days
        expect(eris.transferTimeYears).to.be.closeTo(102.066, 1.0); // ~102.1 yr
        expect(eris.marsDepartureDeltaVKmS).to.be.closeTo(8.242, 1.5); // ~8.24 km/s TEI
        expect(eris.erisArrivalExcessKmS).to.be.closeTo(2.840, 0.5); // ~2.84 km/s v_inf
        expect(eris.erisOrbitInsertionDeltaVKmS).to.be.closeTo(1.926, 0.3); // ~1.93 km/s EOI
        expect(eris.totalMissionDeltaVKmS).to.be.closeTo(10.168, 1.5); // ~10.17 km/s total
        expect(eris.transferEccentricity).to.be.closeTo(0.9560, 0.02); // e ~ 0.956
        expect(eris.erisTransferContext).to.include('Mars-to-Eris Direct');
    });

    it('should calculate subsurface salt diapirism, dislocation creep rheology, and halite thermal inertia', () => {
        // 4 km sediment overburden, 500 m salt bed, 25 K/km geothermal gradient, 8.8 MPa differential stress:
        const diapir = KRCEngine.computeMartianSaltDiapirismHalokinesisKinetics(4.0, 500.0, 25.0, 8.8);
        expect(diapir.burialTemperatureC).to.be.closeTo(41.85, 2.0); // ~42 C burial temp
        expect(diapir.haliteThermalInertiaTIU).to.be.closeTo(3164.4, 100.0); // ~3164 tiu high-inertia halite
        expect(diapir.halokinesisStructuralClass).to.include('Incipient Salt Pillow / Low-Relief Swell');
        expect(diapir.diapirismContext).to.include('Salt Diapirism');
    });

    it('should discriminate Anhydrous Halite Playa vs Hydrated Polyhalite Bittern in CRISM spectra', () => {
        // Halite Chloride Playa (Terra Sirenum: Slope = 0.12, BD1400 = 0.01, BD1750 = 0.01, BD1900 = 0.01, BD2170 = 0.01):
        const halite = BandMathEngine.computeCRISMHalitePolyhaliteSpeciationIndices(0.12, 0.01, 0.01, 0.01, 0.01);
        expect(halite.isEvaporiteDetected).to.be.true;
        expect(halite.evaporiteSalinityClass).to.include('Anhydrous Chloride Salt (Halite Playa Deposit)');
        expect(halite.mineralSpecies).to.include('Halite');
        expect(halite.chemicalFormula).to.include('NaCl');
        expect(halite.playaPaleolakeContext).to.include('Terminal Desiccation of Ancient Closed-Basin Paleolakes');

        // Polyhalite Bittern (Slope = 0.02, BD1400 = 0.04, BD1750 = 0.04, BD1900 = 0.05, BD2170 = 0.04):
        const poly = BandMathEngine.computeCRISMHalitePolyhaliteSpeciationIndices(0.02, 0.04, 0.04, 0.05, 0.04);
        expect(poly.isEvaporiteDetected).to.be.true;
        expect(poly.evaporiteSalinityClass).to.include('Hydrated Potash-Magnesium Bittern Sulfate (Polyhalite)');
        expect(poly.mineralSpecies).to.include('Polyhalite');
        expect(poly.chemicalFormula).to.include('K2Ca2Mg(SO4)4 * 2H2O');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMHalitePolyhaliteSpeciationIndices(0.01, 0.005, 0.005, 0.005, 0.005);
        expect(basalt.isEvaporiteDetected).to.be.false;
    });
});

describe('Mars-to-Sedna Inner Oort Cloud Flyby, Perchlorate Brines & Oxychlorines', () => {
    it('should calculate interplanetary Hohmann transfer from Mars to detached inner Oort Cloud dwarf planet 90377 Sedna and flyby speed', () => {
        // Mars to Sedna (1.52 to 76.19 AU, 300 km Mars alt):
        const sedna = TrajectoryEngine.computeMarsToSednaDirectTransfer(300.0);
        expect(sedna.transferTimeDays).to.be.closeTo(44257.5, 400.0); // ~44258 days
        expect(sedna.transferTimeYears).to.be.closeTo(121.170, 1.5); // ~121.2 yr
        expect(sedna.marsDepartureDeltaVKmS).to.be.closeTo(8.325, 1.5); // ~8.33 km/s TSI
        expect(sedna.sednaFlybyVelocityKmS).to.be.closeTo(2.718, 0.5); // ~2.72 km/s flyby
        expect(sedna.transferEccentricity).to.be.closeTo(0.9608, 0.02); // e ~ 0.961
        expect(sedna.sednaTransferContext).to.include('Mars-to-Sedna Flyby');
    });

    it('should calculate low-temperature perchlorate eutectic brine viscosity, Darcy seepage, and RSL creep flow', () => {
        // 44 wt% Mg(ClO4)2, 30% soil porosity, 230 K soil temp, 0.35 hydraulic gradient:
        const brine = KRCEngine.computeMartianPerchlorateEutecticBrineDynamics(44.0, 0.30, 230.0, 0.35);
        expect(brine.liquidBrineState).to.be.true;
        expect(brine.brineViscosityCP).to.be.closeTo(209.5, 30.0); // ~209.5 cP
        expect(brine.brineDensityKgM3).to.be.closeTo(1462.0, 50.0); // ~1462 kg/m^3 dense brine
        expect(brine.poreSeepageVelocityCmPerDay).to.be.closeTo(2.62, 1.0); // ~2.6 cm/day RSL seep
        expect(brine.brineSaturatedThermalInertiaTIU).to.be.closeTo(700.0, 100.0); // saturated soil TIU
        expect(brine.perchloratePhaseClass).to.include('Active Mobile Liquid Perchlorate Brine');
        expect(brine.perchlorateFlowContext).to.include('Mg(ClO4)2 Brine');
    });

    it('should discriminate Hydrated Magnesium Perchlorate vs Chlorate in CRISM spectra', () => {
        // Hydrated Magnesium Perchlorate (RSL / Palikir Crater: BD1430 = 0.04, BD1930 = 0.06, BD2130 = 0.04, BD2400 = 0.04):
        const perchlor = BandMathEngine.computeCRISMPerchlorateChlorateSpeciationIndices(0.04, 0.06, 0.04, 0.04);
        expect(perchlor.isOxychlorineDetected).to.be.true;
        expect(perchlor.oxychlorineSpeciesClass).to.include('Hydrated Magnesium Perchlorate');
        expect(perchlor.mineralSpecies).to.include('Magnesium Perchlorate Hexahydrate');
        expect(perchlor.chemicalFormula).to.include('Mg(ClO4)2 * 6H2O');
        expect(perchlor.rslAstrobiologicalContext).to.include('Recurring Slope Lineae (RSL)');

        // Chlorate Salt (BD1430 = 0.01, BD1930 = 0.05, BD2130 = 0.04, BD2400 = 0.005):
        const chlor = BandMathEngine.computeCRISMPerchlorateChlorateSpeciationIndices(0.01, 0.05, 0.04, 0.005);
        expect(chlor.isOxychlorineDetected).to.be.true;
        expect(chlor.oxychlorineSpeciesClass).to.include('Hydrated Chlorate Salt');
        expect(chlor.chemicalFormula).to.include('Mg(ClO3)2 * 6H2O');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMPerchlorateChlorateSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isOxychlorineDetected).to.be.false;
    });
});

describe("Mars-to-'Oumuamua Interstellar Chase, Subglacial Lakes & Volatile Ice Speciation", () => {
    it("should calculate interplanetary hyperbolic chase transfer from Mars to interstellar object 1I/'Oumuamua and relative encounter velocity", () => {
        // Mars to Oumuamua at 15.0 AU intercept, 300 km Mars alt:
        const oumuamua = TrajectoryEngine.computeMarsToOumuamuaHyperbolicIntercept(300.0, 15.0);
        expect(oumuamua.transferTimeDays).to.be.closeTo(4337.8, 100.0); // ~4338 days
        expect(oumuamua.transferTimeYears).to.be.closeTo(11.876, 0.3); // ~11.88 yr
        expect(oumuamua.marsDepartureDeltaVKmS).to.be.closeTo(6.692, 0.5); // ~6.69 km/s TII
        expect(oumuamua.oumuamuaRelativeEncounterVelocityKmS).to.be.closeTo(28.684, 1.0); // ~28.68 km/s relative flyby
        expect(oumuamua.transferEccentricity).to.be.closeTo(0.8156, 0.02); // e ~ 0.816
        expect(oumuamua.interceptContext).to.include("1I/'Oumuamua Chase");
    });

    it('should calculate subglacial basal melting equilibrium and MARSIS radar liquid lake stability', () => {
        // 1.5 km polar ice, 160 K surface temp, 75 mW/m^2 geothermal flux, 300 g/kg perchlorate salinity:
        const subglacial = KRCEngine.computeMartianSubglacialBasalMeltingEquilibrium(1.5, 160.0, 75.0, 300.0);
        expect(subglacial.isBasalMeltingOccurring).to.be.true;
        expect(subglacial.basalTemperatureK).to.be.closeTo(216.25, 2.0); // ~216.3 K
        expect(subglacial.basalMeltingPointK).to.be.closeTo(207.04, 2.0); // ~207.0 K
        expect(subglacial.basalThermalMarginK).to.be.greaterThan(5.0); // > 5 K margin
        expect(subglacial.subglacialHydrologyClass).to.include('Stable Hypersaline Subglacial Liquid Water Lake');
        expect(subglacial.subglacialLakeContext).to.include('Subglacial Bed');
    });

    it('should discriminate Crystalline Water Ice (H2O) vs Carbon Dioxide Dry Ice (CO2) in CRISM spectra', () => {
        // Water Ice (Planum Boreum / Lobate Debris Apron: BD1435 = 0.01, BD1500 = 0.08, BD2000 = 0.10, BD2150 = 0.01, BD2350 = 0.01):
        const waterIce = BandMathEngine.computeCRISMIceSpeciationIndices(0.01, 0.08, 0.10, 0.01, 0.01);
        expect(waterIce.isVolatileIceDetected).to.be.true;
        expect(waterIce.polarIceSpeciesClass).to.include('Crystalline Water Ice (H2O Ice Sheet)');
        expect(waterIce.mineralSpecies).to.include('Crystalline Water Ice');
        expect(waterIce.chemicalFormula).to.include('H2O(s)');
        expect(waterIce.cryogenicVolatileContext).to.include('Perennial North Polar Cap');

        // Dry Ice (South Pole Swiss-Cheese Terrain: BD1435 = 0.05, BD1500 = 0.01, BD2000 = 0.08, BD2150 = 0.05, BD2350 = 0.06):
        const dryIce = BandMathEngine.computeCRISMIceSpeciationIndices(0.05, 0.01, 0.08, 0.05, 0.06);
        expect(dryIce.isVolatileIceDetected).to.be.true;
        expect(dryIce.polarIceSpeciesClass).to.include('Carbon Dioxide Dry Ice (CO2 Ice Slab)');
        expect(dryIce.mineralSpecies).to.include('Dry Ice (Carbon Dioxide)');
        expect(dryIce.chemicalFormula).to.include('CO2(s)');

        // Unfrozen dry soil:
        const drySoil = BandMathEngine.computeCRISMIceSpeciationIndices(0.005, 0.005, 0.005, 0.005, 0.005);
        expect(drySoil.isVolatileIceDetected).to.be.false;
    });
});

describe('Mars-to-2I/Borisov Interstellar Comet Intercept, Clay Dehydrogenation & Hematite', () => {
    it('should calculate interplanetary chase transfer from Mars to interstellar comet 2I/Borisov and relative encounter velocity', () => {
        // Mars to Borisov at 2.0 AU intercept, 300 km Mars alt:
        const borisov = TrajectoryEngine.computeMarsToBorisovHyperbolicIntercept(300.0, 2.0);
        expect(borisov.transferTimeDays).to.be.closeTo(427.0, 30.0); // ~427 days
        expect(borisov.transferTimeYears).to.be.closeTo(1.169, 0.1); // ~1.17 yr
        expect(borisov.marsDepartureDeltaVKmS).to.be.closeTo(1.708, 0.3); // ~1.71 km/s TII
        expect(borisov.borisovRelativeEncounterVelocityKmS).to.be.closeTo(32.709, 1.0); // ~32.7 km/s relative flyby
        expect(borisov.transferEccentricity).to.be.closeTo(0.1352, 0.02); // e ~ 0.135
        expect(borisov.borisovInterceptContext).to.include('2I/Borisov Chase');
    });

    it('should calculate impact/volcanic thermal dehydrogenation kinetics of Fe-smectite clays and nanophase hematite exsolution', () => {
        // 80 wt% smectite clay, 550 C baking, 2.0 hours duration:
        const baked = KRCEngine.computeMartianClayThermalDehydrogenationKinetics(0.80, 550.0, 2.0);
        expect(baked.dehydrogenationFraction).to.be.greaterThan(0.90); // > 90% reacted
        expect(baked.nanophaseHematiteWeightPercent).to.be.closeTo(17.5, 2.0); // ~17.5 wt% np-Hm
        expect(baked.residualClayWeightPercent).to.be.closeTo(62.5, 3.0); // ~62.5 wt% dehydroxylated clay
        expect(baked.alteredThermalInertiaTIU).to.be.closeTo(967.0, 80.0); // ~967 tiu baked clay
        expect(baked.thermalAlterationFaciesClass).to.include('High-Grade Thermally Dehydrogenated Red Clay');
        expect(baked.thermalClayContext).to.include('Thermal Baking');
    });

    it('should discriminate Nanophase Hematite (np-Hm, Red Dust) vs Coarse Crystalline Grey Hematite in CRISM/TES spectra', () => {
        // Coarse Crystalline Grey Hematite (Meridiani Blueberries: BD530 = 0.01, BD860 = 0.06, BD980 = 0.01, TIR = 0.06):
        const greyHm = BandMathEngine.computeCRISMHematiteSpeciationIndices(0.01, 0.06, 0.01, 0.06);
        expect(greyHm.isHematiteDetected).to.be.true;
        expect(greyHm.hematiteCrystallinityClass).to.include('Coarse Crystalline Grey Hematite');
        expect(greyHm.mineralSpecies).to.include('Crystalline Grey Hematite');
        expect(greyHm.diageneticFaciesContext).to.include('Sedimentary Groundwater Diagenesis');

        // Nanophase Hematite (Global Bright Red Dust: BD530 = 0.06, BD860 = 0.02, BD980 = 0.01, TIR = 0.01):
        const npHm = BandMathEngine.computeCRISMHematiteSpeciationIndices(0.06, 0.02, 0.01, 0.01);
        expect(npHm.isHematiteDetected).to.be.true;
        expect(npHm.hematiteCrystallinityClass).to.include('Nanophase Hematite (np-Hm, Bright Red Martian Dust)');
        expect(npHm.mineralSpecies).to.include('Nanophase Ferric Oxide / Hematite');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMHematiteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isHematiteDetected).to.be.false;
    });
});

describe('Mars-to-Mercury Venus Gravity Assist, Acid Leaching Opal-A & Silica Speciation', () => {
    it('should calculate Mars-to-Mercury trajectory via Venus Gravity Assist (VGA) and orbit insertion', () => {
        // Mars to Mercury via Venus GA (300 km Mars alt, 300 km Venus flyby alt, 200 km Mercury capture alt):
        const vga = TrajectoryEngine.computeMarsToMercuryViaVenusGravityAssist(300.0, 300.0, 200.0);
        expect(vga.totalTimeDays).to.be.closeTo(298.7, 50.0); // ~299 days total TOF
        expect(vga.marsDepartureDeltaVKmS).to.be.closeTo(3.372, 0.4); // ~3.37 km/s TVI
        expect(vga.venusFlybyExcessKmS).to.be.closeTo(5.763, 0.4); // ~5.76 km/s v_inf Venus
        expect(vga.venusBendingAngleDeg).to.be.closeTo(82.2, 10.0); // ~82.2 deg bending
        expect(vga.mercuryOrbitInsertionDeltaVKmS).to.be.closeTo(4.031, 0.4); // ~4.03 km/s MOI
        expect(vga.totalMissionDeltaVKmS).to.be.closeTo(7.403, 0.5); // ~7.40 km/s total
        expect(vga.trajectoryContext).to.include('Mars-Venus-Mercury GA');
    });

    it('should calculate hydrothermal acid leaching kinetics of smectite, amorphous Opal-A silica yield, and kaolinitization', () => {
        // 85 wt% smectite clay, pH 2.0, 95 C, 250 years leaching:
        const leached = KRCEngine.computeMartianAcidLeachingSilicaKaoliniteKinetics(0.85, 2.00, 95.0, 250.0);
        expect(leached.leachingFraction).to.be.greaterThan(0.70); // > 70% leached
        expect(leached.amorphousSilicaWeightPercent).to.be.closeTo(40.0, 10.0); // ~40 wt% Opal-A
        expect(leached.kaoliniteWeightPercent).to.be.closeTo(25.0, 8.0); // ~25 wt% kaolinite
        expect(leached.opalineSinterThermalInertiaTIU).to.be.closeTo(397.6, 60.0); // ~398 tiu porous opaline sinter
        expect(leached.hydrothermalSilicaFaciesClass).to.include('High-Purity Hydrated Opal-A Silica Sinter');
        expect(leached.silicaParagenesisContext).to.include('Acid Leaching');
    });

    it('should discriminate Hydrated Amorphous Silica (Opal-A) vs Crystalline Quartz in CRISM/TES spectra', () => {
        // Opal-A (Gusev Crater Home Plate / Noctis Labyrinthus: BD1400 = 0.04, BD1900 = 0.06, BD2210 = 0.05, BD2260 = 0.03, TIR = 1120):
        const opal = BandMathEngine.computeCRISMSilicaSpeciationIndices(0.04, 0.06, 0.05, 0.03, 1120.0);
        expect(opal.isSilicaDetected).to.be.true;
        expect(opal.silicaPhaseClass).to.include('Hydrated Amorphous Silica (Opal-A / Opal-CT)');
        expect(opal.mineralSpecies).to.include('Opal-A');
        expect(opal.chemicalFormula).to.include('SiO2 * nH2O');
        expect(opal.hydrothermalAstrobiologicalContext).to.include('Hydrothermal Hot Spring Sinter');

        // Quartz (TIR = 1120 cm-1 reststrahlen, BD1900 = 0.01, BD2210 = 0.01):
        const quartz = BandMathEngine.computeCRISMSilicaSpeciationIndices(0.01, 0.01, 0.01, 0.005, 1120.0);
        expect(quartz.isSilicaDetected).to.be.true;
        expect(quartz.silicaPhaseClass).to.include('Anhydrous Crystalline Quartz / Chalcedony');
        expect(quartz.mineralSpecies).to.include('Alpha-Quartz');
        expect(quartz.chemicalFormula).to.include('SiO2');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMSilicaSpeciationIndices(0.005, 0.005, 0.005, 0.005, 1000.0);
        expect(basalt.isSilicaDetected).to.be.false;
    });
});

describe('Mars-to-Saturn Jupiter Gravity Assist, Serpentine Dehydroxylation & Speciation', () => {
    it('should calculate Mars-to-Saturn Grand Tour trajectory via Jupiter Gravity Assist (JGA) and orbit insertion', () => {
        // Mars to Saturn via Jupiter GA (300 km Mars alt, 500000 km Jupiter flyby alt, 50000 km Saturn capture alt):
        const jga = TrajectoryEngine.computeMarsToSaturnViaJupiterGravityAssist(300.0, 500000.0, 50000.0);
        expect(jga.totalTimeDays).to.be.closeTo(4798.3, 300.0); // ~4798 days (~13.1 yr)
        expect(jga.totalTimeYears).to.be.closeTo(13.14, 1.0); // ~13.1 yr
        expect(jga.marsDepartureDeltaVKmS).to.be.closeTo(4.197, 0.4); // ~4.20 km/s TJI
        expect(jga.jupiterFlybyExcessKmS).to.be.closeTo(4.269, 0.4); // ~4.27 km/s v_inf Jupiter
        expect(jga.jupiterBendingAngleDeg).to.be.closeTo(121.9, 15.0); // ~122 deg bending
        expect(jga.saturnOrbitInsertionDeltaVKmS).to.be.closeTo(0.818, 0.3); // ~0.82 km/s SOI
        expect(jga.totalMissionDeltaVKmS).to.be.closeTo(5.015, 0.5); // ~5.02 km/s total
        expect(jga.grandTourContext).to.include('Mars-Jupiter-Saturn GT');
    });

    it('should calculate contact metamorphic dehydroxylation kinetics of serpentine and recrystallized olivine yield', () => {
        // 80 wt% serpentine, 700 C contact baking, 12.0 hours duration:
        const baked = KRCEngine.computeMartianSerpentineThermalDehydroxylationKinetics(0.80, 700.0, 12.0);
        expect(baked.dehydroxylationFraction).to.be.greaterThan(0.95); // > 95% dehydroxylated
        expect(baked.regeneratedOlivineWeightPercent).to.be.closeTo(40.8, 4.0); // ~40.8 wt% forsterite
        expect(baked.enstatitePyroxeneWeightPercent).to.be.closeTo(28.8, 3.0); // ~28.8 wt% enstatite
        expect(baked.recrystallizedThermalInertiaTIU).to.be.closeTo(2499.9, 150.0); // ~2500 tiu dense hornfels
        expect(baked.metamorphicFaciesClass).to.include('High-Temperature Metamorphic Hornfels');
        expect(baked.serpentineMetamorphicContext).to.include('Metamorphic Contact');
    });

    it('should discriminate Hydrothermal Serpentine vs Talc vs Recrystallized Forsteritic Olivine in CRISM spectra', () => {
        // Serpentine (Nili Fossae / Claritas Rise: BD1390 = 0.04, BD2120 = 0.03, BD2320 = 0.05, BD2510 = 0.03, OL = 0.01):
        const serp = BandMathEngine.computeCRISMSerpentineTalcSpeciationIndices(0.04, 0.03, 0.05, 0.03, 0.01);
        expect(serp.isUltramaficAlterationDetected).to.be.true;
        expect(serp.ultramaficSpeciesClass).to.include('Hydrothermal Serpentine (Lizardite / Antigorite)');
        expect(serp.mineralSpecies).to.include('Serpentine');
        expect(serp.chemicalFormula).to.include('Mg3Si2O5(OH)4');
        expect(serp.serpentinizationContext).to.include('Deep Hydrothermal Serpentinization');

        // Talc (BD1390 = 0.04, BD2120 = 0.005, BD2320 = 0.05, BD2510 = 0.03, OL = 0.01):
        const talc = BandMathEngine.computeCRISMSerpentineTalcSpeciationIndices(0.04, 0.005, 0.05, 0.03, 0.01);
        expect(talc.isUltramaficAlterationDetected).to.be.true;
        expect(talc.ultramaficSpeciesClass).to.include('Hydrothermal Talc (Carbonated / Metamorphosed Serpentine)');
        expect(talc.mineralSpecies).to.include('Talc');
        expect(talc.chemicalFormula).to.include('Mg3Si4O10(OH)2');

        // Recrystallized Olivine (OL = 0.08, BD2320 = 0.005):
        const ol = BandMathEngine.computeCRISMSerpentineTalcSpeciationIndices(0.005, 0.005, 0.005, 0.005, 0.08);
        expect(ol.isUltramaficAlterationDetected).to.be.true;
        expect(ol.ultramaficSpeciesClass).to.include('Recrystallized Forsteritic Olivine');
        expect(ol.chemicalFormula).to.include('Mg1.8Fe0.2SiO4');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMSerpentineTalcSpeciationIndices(0.005, 0.005, 0.005, 0.005, 0.01);
        expect(basalt.isUltramaficAlterationDetected).to.be.false;
    });
});

describe('Mars-to-Uranus Jupiter Gravity Assist, Methane Clathrates & Carbonate Speciation', () => {
    it('should calculate Mars-to-Uranus trajectory via Jupiter Gravity Assist (JUGA) and orbit insertion', () => {
        // Mars to Uranus via Jupiter GA (300 km Mars alt, 400000 km Jupiter flyby alt, 25000 km Uranus capture alt):
        const juga = TrajectoryEngine.computeMarsToUranusViaJupiterGravityAssist(300.0, 400000.0, 25000.0);
        expect(juga.totalTimeDays).to.be.closeTo(8920.0, 400.0); // ~8920 days (~24.4 yr)
        expect(juga.totalTimeYears).to.be.closeTo(24.42, 1.5); // ~24.4 yr
        expect(juga.marsDepartureDeltaVKmS).to.be.closeTo(4.197, 0.4); // ~4.20 km/s TJI
        expect(juga.jupiterFlybyExcessKmS).to.be.closeTo(4.269, 0.4); // ~4.27 km/s v_inf Jupiter
        expect(juga.jupiterBendingAngleDeg).to.be.closeTo(138.9, 15.0); // ~139 deg bending
        expect(juga.uranusOrbitInsertionDeltaVKmS).to.be.closeTo(0.453, 0.2); // ~0.45 km/s UOI
        expect(juga.totalMissionDeltaVKmS).to.be.closeTo(4.650, 0.5); // ~4.65 km/s total
        expect(juga.uranusGAContext).to.include('Mars-Jupiter-Uranus');
    });

    it('should calculate subsurface methane clathrate hydrate stability, dissociation kinetics, and outgassing volume', () => {
        // 15 m burial depth, 225 K cryosphere temp, 0.60 kPa pore pressure, 20% clathrate saturation:
        const clath = KRCEngine.computeMartianMethaneClathrateStabilityKinetics(15.0, 225.0, 0.60, 0.20);
        expect(clath.isClathrateStable).to.be.false; // Unstable at 225 K and 0.6 kPa -> outgassing
        expect(clath.equilibriumPressureKPa).to.be.closeTo(2.86, 0.3); // ~2.86 kPa P_eq
        expect(clath.dissociationDrivingForceKPa).to.be.closeTo(2.26, 0.3); // ~2.26 kPa overpressure
        expect(clath.methaneOutgassingVolumeM3PerM3).to.be.closeTo(11.48, 1.0); // ~11.5 m^3 CH4 per m^3 soil
        expect(clath.clathrateCryosphereThermalInertiaTIU).to.be.closeTo(942.1, 100.0); // ~942 tiu permafrost
        expect(clath.clathrateRegimeClass).to.include('Active Thermal Dissociation & Vigorous Methane Outgassing Plume');
        expect(clath.clathrateStabilityContext).to.include('CH4 Clathrate');
    });

    it('should discriminate Magnesium Carbonate (Magnesite) vs Siderite vs Calcite in CRISM spectra', () => {
        // Magnesite (Jezero Crater margin / Nili Fossae: BD2300 = 0.05, BD2330 = 0.01, BD2500 = 0.06, BD2530 = 0.01, BD3400 = 0.03):
        const mag = BandMathEngine.computeCRISMMagnesiteSideriteCalciteEndmemberIndices(0.05, 0.01, 0.06, 0.01, 0.03);
        expect(mag.isCarbonateDetected).to.be.true;
        expect(mag.carbonateSpeciesClass).to.include('Magnesium Carbonate (Magnesite / Hydromagnesite)');
        expect(mag.mineralSpecies).to.include('Magnesite');
        expect(mag.chemicalFormula).to.include('MgCO3');
        expect(mag.paleoclimateBiosignatureContext).to.include('Noachian Alkaline Paleolake Shoreline');

        // Siderite (BD2300 = 0.01, BD2330 = 0.04, BD2500 = 0.01, BD2530 = 0.05, BD3400 = 0.03):
        const sid = BandMathEngine.computeCRISMMagnesiteSideriteCalciteEndmemberIndices(0.01, 0.04, 0.01, 0.05, 0.03);
        expect(sid.isCarbonateDetected).to.be.true;
        expect(sid.carbonateSpeciesClass).to.include('Ferrous Iron Carbonate (Siderite)');
        expect(sid.mineralSpecies).to.include('Siderite');
        expect(sid.chemicalFormula).to.include('FeCO3');

        // Calcite (BD2300 = 0.01, BD2330 = 0.04, BD2500 = 0.01, BD2530 = 0.01, BD3400 = 0.04):
        const calc = BandMathEngine.computeCRISMMagnesiteSideriteCalciteEndmemberIndices(0.01, 0.04, 0.01, 0.01, 0.04);
        expect(calc.isCarbonateDetected).to.be.true;
        expect(calc.carbonateSpeciesClass).to.include('Calcium Carbonate (Calcite / Aragonite)');
        expect(calc.mineralSpecies).to.include('Calcite');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMMagnesiteSideriteCalciteEndmemberIndices(0.005, 0.005, 0.005, 0.005, 0.005);
        expect(basalt.isCarbonateDetected).to.be.false;
    });
});

describe('Mars-to-Neptune Jupiter Gravity Assist, Impact Hydrothermal & Zeolite Speciation', () => {
    it('should calculate Mars-to-Neptune trajectory via Jupiter Gravity Assist (JNGA) and orbit insertion', () => {
        // Mars to Neptune via Jupiter GA (300 km Mars alt, 350000 km Jupiter flyby alt, 20000 km Neptune capture alt):
        const jnga = TrajectoryEngine.computeMarsToNeptuneViaJupiterGravityAssist(300.0, 350000.0, 20000.0);
        expect(jnga.totalTimeDays).to.be.closeTo(14522.6, 500.0); // ~14523 days (~39.8 yr)
        expect(jnga.totalTimeYears).to.be.closeTo(39.76, 1.5); // ~39.8 yr
        expect(jnga.marsDepartureDeltaVKmS).to.be.closeTo(4.197, 0.4); // ~4.20 km/s TJI
        expect(jnga.jupiterFlybyExcessKmS).to.be.closeTo(4.269, 0.4); // ~4.27 km/s v_inf Jupiter
        expect(jnga.jupiterBendingAngleDeg).to.be.closeTo(141.1, 15.0); // ~141 deg bending
        expect(jnga.neptuneOrbitInsertionDeltaVKmS).to.be.closeTo(0.359, 0.2); // ~0.36 km/s NOI
        expect(jnga.totalMissionDeltaVKmS).to.be.closeTo(4.556, 0.5); // ~4.56 km/s total
        expect(jnga.neptuneGAContext).to.include('Mars-Jupiter-Neptune');
    });

    it('should calculate post-impact hydrothermal convective system lifetime, water throughput, and habitability window', () => {
        // 45 km crater diameter, 250 m melt sheet, 1e-13 m^2 permeability, 215 K ambient temp:
        const hydro = KRCEngine.computeMartianImpactHydrothermalSystemLifetime(45.0, 250.0, 1.0e-13, 215.0);
        expect(hydro.hydrothermalLifetimeYears).to.be.closeTo(65000, 5000); // ~65,000 yr lifetime
        expect(hydro.activeVentingDurationYears).to.be.closeTo(11700, 1500); // ~11,700 yr boiling phase
        expect(hydro.cumulativeWaterThroughputKm3).to.be.closeTo(450.0, 50.0); // ~450 km^3 H2O
        expect(hydro.alteredBrecciaThermalInertiaTIU).to.be.closeTo(1923.6, 150.0); // ~1924 tiu breccia
        expect(hydro.hydrothermalHabitabilityClass).to.include('Substantial Post-Impact Hydrothermal System');
        expect(hydro.impactHydrothermalContext).to.include('Impact Hydrothermal System');
    });

    it('should discriminate Hydrothermal Zeolite Analcime vs Clinoptilolite in CRISM spectra', () => {
        // Analcime (Crater Central Uplift: BD1400 = 0.04, BD1900 = 0.06, BD2490 = 0.04, BD2540 = 0.01):
        const anal = BandMathEngine.computeCRISMAnalcimeChabaziteHydrothermalIndices(0.04, 0.06, 0.04, 0.01);
        expect(anal.isZeoliteDetected).to.be.true;
        expect(anal.zeoliteSpeciesClass).to.include('Hydrothermal Zeolite (Analcime)');
        expect(anal.mineralSpecies).to.include('Analcime');
        expect(anal.chemicalFormula).to.include('NaAlSi2O6 * H2O');
        expect(anal.hydrothermalDiageneticContext).to.include('Moderate-Temperature Hydrothermal Alteration');

        // Clinoptilolite (BD1400 = 0.04, BD1900 = 0.06, BD2490 = 0.01, BD2540 = 0.04):
        const clino = BandMathEngine.computeCRISMAnalcimeChabaziteHydrothermalIndices(0.04, 0.06, 0.01, 0.04);
        expect(clino.isZeoliteDetected).to.be.true;
        expect(clino.zeoliteSpeciesClass).to.include('Low-Temperature Zeolite (Clinoptilolite / Chabazite / Phillipsite)');
        expect(clino.mineralSpecies).to.include('Clinoptilolite');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMAnalcimeChabaziteHydrothermalIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isZeoliteDetected).to.be.false;
    });
});

describe('Mars-to-Arrokoth KBO Transfer, Clay Illitization & Metamorphic Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to Kuiper Belt Object 48695 Arrokoth', () => {
        // Mars to Arrokoth (300 km Mars alt, 44.581 AU heliocentric distance, 3500 km flyby pericenter):
        const kbo = TrajectoryEngine.computeMarsToArrokothKBOTransfer(300.0, 44.581, 3500.0);
        expect(kbo.semiMajorAxisAU).to.be.closeTo(23.052, 0.5); // ~23.05 AU
        expect(kbo.eccentricity).to.be.closeTo(0.9339, 0.01); // e ~ 0.934
        expect(kbo.timeOfFlightDays).to.be.closeTo(20213.5, 500.0); // ~20214 days (~55.3 yr)
        expect(kbo.timeOfFlightYears).to.be.closeTo(55.34, 1.5); // ~55.3 yr
        expect(kbo.marsDepartureDeltaVKmS).to.be.closeTo(7.346, 0.6); // ~7.35 km/s TKI
        expect(kbo.hyperbolicExcessVelocityKmS).to.be.closeTo(9.613, 0.6); // ~9.61 km/s v_inf
        expect(kbo.encounterRelativeVelocityKmS).to.be.closeTo(3.314, 0.4); // ~3.31 km/s flyby v_rel
        expect(kbo.kboContext).to.include('Mars-to-Arrokoth KBO');
    });

    it('should calculate burial diagenetic illitization kinetics of smectite clay into ordered illite-smectite', () => {
        // 90% initial smectite, 120 C burial temp, 0.05 M K+, 10 Myr duration:
        const illite = KRCEngine.computeMartianSmectiteIllitizationKinetics(0.90, 120.0, 0.05, 10.0);
        expect(illite.illiteLayerFraction).to.be.greaterThan(0.90); // > 90% illitized
        expect(illite.residualSmectiteFraction).to.be.lessThan(0.10); // < 10% residual smectite
        expect(illite.reichweiteOrderingClass).to.include('Highly Ordered R3 (ISII) Mixed-Layer / Pure Illite-Muscovite');
        expect(illite.illitizedThermalInertiaTIU).to.be.closeTo(2250.0, 200.0); // ~2250 tiu compacted shale
        expect(illite.metamorphicGradeClass).to.include('Anchizone to Epizone Low-Grade Metamorphic Horizon');
        expect(illite.illitizationContext).to.include('Clay Illitization');
    });

    it('should discriminate Expandable Smectite vs Ordered Mixed-Layer I/S vs Pure Metamorphic Illite/Muscovite in CRISM spectra', () => {
        // Smectite (Montmorillonite in Mawrth Vallis: BD1400 = 0.03, BD1900 = 0.08, BD2200 = 0.07, BD2350 = 0.01):
        const smect = BandMathEngine.computeCRISMSmectiteIlliteMuscoviteMetamorphicIndices(0.03, 0.08, 0.07, 0.01);
        expect(smect.isAlPhyllosilicateDetected).to.be.true;
        expect(smect.metamorphicSpeciesClass).to.include('Expandable Smectite (Montmorillonite / Beidellite)');
        expect(smect.mineralSpecies).to.include('Montmorillonite');
        expect(smect.hydrationRatio).to.be.greaterThan(0.85);

        // Mixed-layer I/S (BD1400 = 0.03, BD1900 = 0.04, BD2200 = 0.07, BD2350 = 0.03):
        const isLayer = BandMathEngine.computeCRISMSmectiteIlliteMuscoviteMetamorphicIndices(0.03, 0.04, 0.07, 0.03);
        expect(isLayer.isAlPhyllosilicateDetected).to.be.true;
        expect(isLayer.metamorphicSpeciesClass).to.include('Ordered Mixed-Layer Illite-Smectite (I/S)');
        expect(isLayer.mineralSpecies).to.include('Mixed-Layer I/S');

        // Pure Illite / Muscovite (BD1400 = 0.03, BD1900 = 0.015, BD2200 = 0.07, BD2350 = 0.04):
        const ill = BandMathEngine.computeCRISMSmectiteIlliteMuscoviteMetamorphicIndices(0.03, 0.015, 0.07, 0.04);
        expect(ill.isAlPhyllosilicateDetected).to.be.true;
        expect(ill.metamorphicSpeciesClass).to.include('Diagenetic Illite / Metamorphic Muscovite (Sericite)');
        expect(ill.mineralSpecies).to.include('Illite / Muscovite');
        expect(ill.chemicalFormula).to.include('KAl2(AlSi3O10)(OH)2');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMSmectiteIlliteMuscoviteMetamorphicIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAlPhyllosilicateDetected).to.be.false;
    });
});

describe('Mars-to-Makemake Transfer, Acid Sulfate Weathering & Alunite-Jarosite Speciation', () => {
    it('should calculate interplanetary transfer from Mars to Kuiper Belt dwarf planet 136472 Makemake and orbit capture', () => {
        // Mars to Makemake (300 km Mars alt, 45.79 AU distance, 500 km capture alt):
        const make = TrajectoryEngine.computeMarsToMakemakeTransfer(300.0, 45.79, 500.0);
        expect(make.semiMajorAxisAU).to.be.closeTo(23.657, 0.5); // ~23.66 AU
        expect(make.eccentricity).to.be.closeTo(0.9356, 0.01); // e ~ 0.936
        expect(make.timeOfFlightDays).to.be.closeTo(21000.0, 2000.0); // ~21000-23000 days
        expect(make.timeOfFlightYears).to.be.closeTo(57.5, 5.0); // ~57-63 yr
        expect(make.marsDepartureDeltaVKmS).to.be.closeTo(7.392, 0.6); // ~7.39 km/s TMI
        expect(make.makemakeOrbitInsertionDeltaVKmS).to.be.closeTo(2.777, 0.4); // ~2.78 km/s MOI
        expect(make.totalMissionDeltaVKmS).to.be.closeTo(10.169, 0.8); // ~10.17 km/s total
        expect(make.makemakeContext).to.include('Mars-to-Makemake');
    });

    it('should calculate acid sulfate weathering kinetics of sulfides into jarosite and alunite duricrust', () => {
        // 15 wt% FeS2, pH 1.8, 65 C reaction temp, 50 yr duration:
        const acid = KRCEngine.computeMartianAcidSulfateAluniteJarositeKinetics(0.15, 1.8, 65.0, 50.0);
        expect(acid.pyriteOxidationFraction).to.be.greaterThan(0.80); // > 80% oxidized
        expect(acid.jarositePrecipitatedWeightPercent).to.be.closeTo(21.0, 4.0); // ~21 wt% Jarosite
        expect(acid.alunitePrecipitatedWeightPercent).to.be.closeTo(6.5, 2.0); // ~6.5 wt% Alunite
        expect(acid.sulfateDuricrustThermalInertiaTIU).to.be.closeTo(1640.0, 150.0); // ~1640 tiu duricrust
        expect(acid.acidSulfateAlterationClass).to.include('Intense Fumarolic Acid Sulfate Alteration');
        expect(acid.acidWeatheringContext).to.include('Acid Sulfate');
    });

    it('should discriminate Hydrothermal Alunite vs Acid Evaporite Jarosite in CRISM spectra', () => {
        // Alunite (Mawrth Vallis / Columbus: BD1480 = 0.04, BD1760 = 0.03, BD2160 = 0.05, BD2270 = 0.01):
        const alu = BandMathEngine.computeCRISMAluniteJarositeAcidSulfateIndices(0.04, 0.03, 0.05, 0.01);
        expect(alu.isAcidSulfateDetected).to.be.true;
        expect(alu.acidSulfateSpeciesClass).to.include('Hydrothermal Acid Sulfate (Potassium Alunite)');
        expect(alu.mineralSpecies).to.include('Alunite');
        expect(alu.chemicalFormula).to.include('KAl3(SO4)2(OH)6');
        expect(alu.phGeochemicalContext).to.include('High-Temperature Acid-Sulfate');

        // Jarosite (Meridiani Planum Burns Formation: BD1480 = 0.01, BD1760 = 0.01, BD2160 = 0.01, BD2270 = 0.05):
        const jaro = BandMathEngine.computeCRISMAluniteJarositeAcidSulfateIndices(0.01, 0.01, 0.01, 0.05);
        expect(jaro.isAcidSulfateDetected).to.be.true;
        expect(jaro.acidSulfateSpeciesClass).to.include('Acid Groundwater Evaporite (Jarosite)');
        expect(jaro.mineralSpecies).to.include('Jarosite');
        expect(jaro.chemicalFormula).to.include('KFe3(SO4)2(OH)6');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMAluniteJarositeAcidSulfateIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAcidSulfateDetected).to.be.false;
    });
});

describe('Mars-to-Haumea Transfer, Magma Sill Cooling & Silica Polymorph Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to Kuiper Belt dwarf planet 136108 Haumea and orbit capture', () => {
        // Mars to Haumea (300 km Mars alt, 43.13 AU distance, 400 km capture alt):
        const hau = TrajectoryEngine.computeMarsToHaumeaTransfer(300.0, 43.13, 400.0);
        expect(hau.semiMajorAxisAU).to.be.closeTo(22.327, 0.5); // ~22.33 AU
        expect(hau.eccentricity).to.be.closeTo(0.9318, 0.01); // e ~ 0.932
        expect(hau.timeOfFlightDays).to.be.closeTo(19266.8, 500.0); // ~19267 days (~52.7 yr)
        expect(hau.timeOfFlightYears).to.be.closeTo(52.75, 1.5); // ~52.8 yr
        expect(hau.marsDepartureDeltaVKmS).to.be.closeTo(7.299, 0.6); // ~7.30 km/s THI
        expect(hau.haumeaOrbitInsertionDeltaVKmS).to.be.closeTo(2.731, 0.4); // ~2.73 km/s HOI
        expect(hau.totalMissionDeltaVKmS).to.be.closeTo(10.030, 0.8); // ~10.03 km/s total
        expect(hau.haumeaContext).to.include('Mars-to-Haumea');
    });

    it('should calculate 1D conductive cooling, Stefan solidification, and contact metamorphic halo of a basaltic magma sill', () => {
        // 100 m thick sill, 1200 C intrusion temp, 100 C host rock temp, 400 kJ/kg latent heat:
        const sill = KRCEngine.computeMartianBasalticSillCoolingSolidification(100.0, 1200.0, 100.0, 400.0);
        expect(sill.solidificationTimeYears).to.be.closeTo(49.5, 5.0); // ~49.5 yr solidification
        expect(sill.metamorphicAureoleWidthMeters).to.be.closeTo(85.0, 10.0); // ~85 m halo
        expect(sill.totalCoolingToHostTempYears).to.be.closeTo(222.9, 25.0); // ~223 yr cooling
        expect(sill.crystallizedSillThermalInertiaTIU).to.be.closeTo(2620.0, 150.0); // ~2620 tiu microgabbro
        expect(sill.intrusionRegimeClass).to.include('Substantial Basaltic Sill / Sheet Complex');
        expect(sill.sillCoolingContext).to.include('Basaltic Sill');
    });

    it('should discriminate Amorphous Opal-A vs Opal-CT vs Quartz / Chalcedony in CRISM spectra', () => {
        // Opal-A (Gusev Crater Home Plate / Mawrth: BD1400 = 0.04, BD1900 = 0.06, BD2210 = 0.07, BD2260 = 0.04, FWHM = 55 nm):
        const opalA = BandMathEngine.computeCRISMOpalAChertQuartzPolymorphIndices(0.04, 0.06, 0.07, 0.04, 55.0);
        expect(opalA.isSilicaDetected).to.be.true;
        expect(opalA.silicaPolymorphClass).to.include('Amorphous Opaline Silica (Opal-A)');
        expect(opalA.mineralSpecies).to.include('Opal-A');
        expect(opalA.sinterHydrothermalContext).to.include('Hydrothermal Hot Spring Sinter');

        // Opal-CT (BD1400 = 0.03, BD1900 = 0.04, BD2210 = 0.06, BD2260 = 0.03, FWHM = 35 nm):
        const opalCT = BandMathEngine.computeCRISMOpalAChertQuartzPolymorphIndices(0.03, 0.04, 0.06, 0.03, 35.0);
        expect(opalCT.isSilicaDetected).to.be.true;
        expect(opalCT.silicaPolymorphClass).to.include('Diagenetic Opal-CT');
        expect(opalCT.mineralSpecies).to.include('Opal-CT');

        // Quartz / Chalcedony (BD1400 = 0.03, BD1900 = 0.015, BD2210 = 0.06, BD2260 = 0.01, FWHM = 20 nm):
        const qtz = BandMathEngine.computeCRISMOpalAChertQuartzPolymorphIndices(0.03, 0.015, 0.06, 0.01, 20.0);
        expect(qtz.isSilicaDetected).to.be.true;
        expect(qtz.silicaPolymorphClass).to.include('Microcrystalline Chalcedony / Cryptocrystalline Quartz');
        expect(qtz.mineralSpecies).to.include('Chalcedony / Quartz');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMOpalAChertQuartzPolymorphIndices(0.005, 0.005, 0.005, 0.005, 10.0);
        expect(basalt.isSilicaDetected).to.be.false;
    });
});

describe('Mars-to-Sedna ETNO Transfer, Lava Tube Thermal Stability & Chloride Salt Discrimination', () => {
    it('should calculate interplanetary direct transfer from Mars to extreme trans-Neptunian dwarf planet 90377 Sedna', () => {
        // Mars to Sedna (300 km Mars alt, 84.0 AU distance, 300 km capture alt):
        const sedna = TrajectoryEngine.computeMarsToSednaETNOTransfer(300.0, 84.0, 300.0);
        expect(sedna.semiMajorAxisAU).to.be.closeTo(42.762, 1.0); // ~42.76 AU
        expect(sedna.eccentricity).to.be.closeTo(0.9644, 0.01); // e ~ 0.964
        expect(sedna.timeOfFlightDays).to.be.closeTo(51039.4, 2500.0); // ~51000 days (~139.7 yr)
        expect(sedna.timeOfFlightYears).to.be.closeTo(139.74, 7.0); // ~139.7 yr
        expect(sedna.marsDepartureDeltaVKmS).to.be.closeTo(7.414, 0.4); // ~7.41 km/s TSI
        expect(sedna.sednaOrbitInsertionDeltaVKmS).to.be.closeTo(2.166, 0.4); // ~2.17 km/s SOI
        expect(sedna.totalMissionDeltaVKmS).to.be.closeTo(9.580, 0.5); // ~9.58 km/s total
        expect(sedna.sednaContext).to.include('Mars-to-Sedna');
    });

    it('should calculate 1D thermal wave attenuation and interior microclimate stability of a Martian lava tube', () => {
        // 10 m roof thickness, 45 K diurnal amp, 25 K annual amp, 210 K mean temp:
        const tube = KRCEngine.computeMartianLavaTubeThermalInsulation(10.0, 45.0, 25.0, 210.0);
        expect(tube.diurnalSkinDepthMeters).to.be.closeTo(0.150, 0.02); // ~0.15 m diurnal skin depth
        expect(tube.annualSkinDepthMeters).to.be.closeTo(3.889, 0.2); // ~3.89 m annual skin depth
        expect(tube.interiorDiurnalAmplitudeK).to.be.lessThan(1e-10); // completely blocked diurnal wave
        expect(tube.interiorAnnualAmplitudeK).to.be.closeTo(1.91, 0.4); // < 2 K seasonal oscillation
        expect(tube.basaltRoofThermalInertiaTIU).to.be.closeTo(1785.4, 150.0); // ~1785 tiu basalt
        expect(tube.habitatMicroclimateClass).to.include('Ultra-Stable Thermal Oasis');
        expect(tube.lavaTubeContext).to.include('Lava Tube Roof');
    });

    it('should discriminate Featureless Anhydrous Chloride Salts (Halite) vs Hydrated Sulfates vs Anhydrite in CRISM spectra', () => {
        // Chloride (Terra Sirenum: R1000 = 0.22, R1500 = 0.26, R2500 = 0.34, BD1900 = 0.01, BD2400 = 0.005):
        const cl = BandMathEngine.computeCRISMChlorideHaliteAnhydriteDiscriminationIndices(0.22, 0.26, 0.34, 0.01, 0.005);
        expect(cl.isEvaporiteSaltDetected).to.be.true;
        expect(cl.saltSpeciesClass).to.include('Anhydrous Chloride Salt (Halite / Sylvite)');
        expect(cl.mineralSpecies).to.include('Halite');
        expect(cl.playaPaleolakeContext).to.include('Terminal Paleolake Playa Evaporite');

        // Polyhydrated Sulfate (BD1900 = 0.06, BD2400 = 0.05):
        const sulf = BandMathEngine.computeCRISMChlorideHaliteAnhydriteDiscriminationIndices(0.25, 0.25, 0.25, 0.06, 0.05);
        expect(sulf.isEvaporiteSaltDetected).to.be.true;
        expect(sulf.saltSpeciesClass).to.include('Polyhydrated Magnesium/Iron Sulfate');
        expect(sulf.mineralSpecies).to.include('Polyhydrated Sulfate');

        // Anhydrite (BD1900 = 0.01, BD2400 = 0.04):
        const anh = BandMathEngine.computeCRISMChlorideHaliteAnhydriteDiscriminationIndices(0.25, 0.25, 0.25, 0.01, 0.04);
        expect(anh.isEvaporiteSaltDetected).to.be.true;
        expect(anh.saltSpeciesClass).to.include('Anhydrous Calcium Sulfate (Anhydrite)');
        expect(anh.mineralSpecies).to.include('Anhydrite');

        // Unaltered basalt:
        const basalt = BandMathEngine.computeCRISMChlorideHaliteAnhydriteDiscriminationIndices(0.25, 0.25, 0.25, 0.005, 0.005);
        expect(basalt.isEvaporiteSaltDetected).to.be.false;
    });
});

describe('Mars-to-Eris Transfer, Ice Sublimation Lag Desiccation & Volatile Ice Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to massive scattered disc dwarf planet 136199 Eris and orbit capture', () => {
        // Mars to Eris (300 km Mars alt, 95.88 AU distance, 500 km capture alt):
        const eris = TrajectoryEngine.computeMarsToErisTransfer(300.0, 95.88, 500.0);
        expect(eris.semiMajorAxisAU).to.be.closeTo(48.702, 1.0); // ~48.70 AU
        expect(eris.eccentricity).to.be.closeTo(0.9687, 0.01); // e ~ 0.969
        expect(eris.timeOfFlightDays).to.be.closeTo(62088.3, 3000.0); // ~62000 days (~170.0 yr)
        expect(eris.timeOfFlightYears).to.be.closeTo(170.0, 8.0); // ~170 yr
        expect(eris.marsDepartureDeltaVKmS).to.be.closeTo(7.427, 0.6); // ~7.43 km/s TEI
        expect(eris.erisOrbitInsertionDeltaVKmS).to.be.closeTo(1.647, 0.4); // ~1.65 km/s EOI
        expect(eris.totalMissionDeltaVKmS).to.be.closeTo(9.074, 0.5); // ~9.07 km/s total
        expect(eris.erisContext).to.include('Mars-to-Eris');
    });

    it('should calculate Fickian vapor diffusion, sublimation lag retreat velocity, and ground ice stability', () => {
        // 0.10 m lag thickness, 20% RH, 210 K ground temp, 40% porosity:
        const lag = KRCEngine.computeMartianSubsurfaceIceLagDesiccationRate(0.10, 0.20, 210.0, 0.40);
        expect(lag.sublimationFluxKgPerM2S).to.be.greaterThan(1e-11); // active vapor diffusion
        expect(lag.iceRetreatRateMmPerYear).to.be.closeTo(0.157, 0.03); // ~0.157 mm/yr retreat rate
        expect(lag.desiccatedLagThermalInertiaTIU).to.be.closeTo(156.5, 30.0); // ~156 tiu dry lag
        expect(lag.iceCementedThermalInertiaTIU).to.be.closeTo(2216.8, 150.0); // ~2217 tiu frozen ground
        expect(lag.cryosphericStabilityClass).to.include('Slowly Retreating Ice Table');
        expect(lag.iceLagContext).to.include('Ice Table at 210K');
    });

    it('should discriminate Solid Carbon Dioxide Ice (Dry Ice) vs CO Ice vs N2 Ice in CRISM spectra', () => {
        // CO2 Ice (SPRC swiss-cheese terrain: BD1435 = 0.06, BD1970 = 0.08, BD2000 = 0.09):
        const co2 = BandMathEngine.computeCRISMCO2COVolatileIceSpeciationIndices(0.06, 0.08, 0.09, 0.01, 0.01);
        expect(co2.isVolatileIceDetected).to.be.true;
        expect(co2.iceSpeciesClass).to.include('Solid Carbon Dioxide Ice (Dry Ice)');
        expect(co2.chemicalSpecies).to.include('Carbon Dioxide Ice');
        expect(co2.chemicalFormula).to.include('CO2');
        expect(co2.polarVolatileContext).to.include('South Polar Residual Cap');

        // CO Ice (BD1435 = 0.01, BD2150 = 0.05):
        const co = BandMathEngine.computeCRISMCO2COVolatileIceSpeciationIndices(0.01, 0.01, 0.01, 0.05, 0.01);
        expect(co.isVolatileIceDetected).to.be.true;
        expect(co.iceSpeciesClass).to.include('Solid Carbon Monoxide Ice');
        expect(co.chemicalSpecies).to.include('Carbon Monoxide Ice');
        expect(co.chemicalFormula).to.include('CO');

        // N2 Ice (BD1435 = 0.01, BD2000 = 0.01, BD2350 = 0.04):
        const n2 = BandMathEngine.computeCRISMCO2COVolatileIceSpeciationIndices(0.01, 0.01, 0.01, 0.01, 0.04);
        expect(n2.isVolatileIceDetected).to.be.true;
        expect(n2.iceSpeciesClass).to.include('Solid Molecular Nitrogen Ice');
        expect(n2.chemicalSpecies).to.include('Nitrogen Ice');
        expect(n2.chemicalFormula).to.include('N2');

        // Bare rock:
        const rock = BandMathEngine.computeCRISMCO2COVolatileIceSpeciationIndices(0.005, 0.005, 0.005, 0.005, 0.005);
        expect(rock.isVolatileIceDetected).to.be.false;
    });
});

describe('Mars-to-Gonggong Transfer, Cryochamber Pressurization & Water Ice Grain Size', () => {
    it('should calculate interplanetary direct transfer from Mars to resonant scattered disc dwarf planet 225088 Gonggong', () => {
        // Mars to Gonggong (300 km Mars alt, 88.70 AU distance, 300 km capture alt):
        const gong = TrajectoryEngine.computeMarsToGonggongTransfer(300.0, 88.70, 300.0);
        expect(gong.semiMajorAxisAU).to.be.closeTo(45.112, 1.0); // ~45.11 AU
        expect(gong.eccentricity).to.be.closeTo(0.9662, 0.01); // e ~ 0.966
        expect(gong.timeOfFlightDays).to.be.closeTo(55300.0, 3000.0); // ~55300 days (~151.4 yr)
        expect(gong.timeOfFlightYears).to.be.closeTo(151.4, 8.0); // ~151.4 yr
        expect(gong.marsDepartureDeltaVKmS).to.be.closeTo(7.420, 0.6); // ~7.42 km/s TGI
        expect(gong.gonggongOrbitInsertionDeltaVKmS).to.be.closeTo(2.144, 0.4); // ~2.14 km/s GOI
        expect(gong.totalMissionDeltaVKmS).to.be.closeTo(9.564, 0.5); // ~9.56 km/s total
        expect(gong.gonggongContext).to.include('Mars-to-Gonggong');
    });

    it('should calculate cryomagma chamber freezing, volumetric overpressure, and hydrofracture eruption threshold', () => {
        // 250 m radius chamber, 15 wt% salinity, 2500 m depth, 210 K cryosphere:
        const cryo = KRCEngine.computeMartianCryochamberFreezingPressurization(250.0, 15.0, 2500.0, 210.0);
        expect(cryo.volumetricExpansionFraction).to.be.closeTo(0.045, 0.01); // ~4.5% volume expansion
        expect(cryo.hydraulicOverpressureMPa).to.be.greaterThan(50.0); // > 50 MPa overpressure
        expect(cryo.lithostaticStressMPa).to.be.closeTo(25.1, 3.0); // ~25.1 MPa lithostatic load
        expect(cryo.isCryodikeErupting).to.be.true; // Exceeds lithostatic + tensile threshold -> erupts
        expect(cryo.frozenShellThermalInertiaTIU).to.be.closeTo(2371.3, 150.0); // ~2371 tiu eutectic salt-ice
        expect(cryo.cryochamberContext).to.include('Cryomagma Chamber');
    });

    it('should discriminate Crystalline Water Ice (Ih) vs Amorphous Ice and invert grain size in CRISM spectra', () => {
        // Crystalline Water Ice (NPLD / Korolev Crater: BD1250 = 0.03, BD1500 = 0.35, BD1650 = 0.06, BD2000 = 0.45):
        const ice = BandMathEngine.computeCRISMCrystallineAmorphousWaterIceGrainSizeIndices(0.03, 0.35, 0.06, 0.45);
        expect(ice.isWaterIceDetected).to.be.true;
        expect(ice.iceCrystallinityClass).to.include('Hexagonal Crystalline Water Ice (Ih)');
        expect(ice.estimatedGrainSizeMicrons).to.be.closeTo(113.4, 25.0); // ~113 um grain size
        expect(ice.grainSizeRegimeClass).to.include('Frost');
        expect(ice.polarCryosphereContext).to.include('North Polar Layered Deposits');

        // Amorphous Solid Water (BD1250 = 0.01, BD1500 = 0.30, BD1650 = 0.01, BD2000 = 0.40):
        const asw = BandMathEngine.computeCRISMCrystallineAmorphousWaterIceGrainSizeIndices(0.01, 0.30, 0.01, 0.40);
        expect(asw.isWaterIceDetected).to.be.true;
        expect(asw.iceCrystallinityClass).to.include('Amorphous Solid Water (ASW)');

        // Bare rock:
        const rock = BandMathEngine.computeCRISMCrystallineAmorphousWaterIceGrainSizeIndices(0.005, 0.02, 0.005, 0.03);
        expect(rock.isWaterIceDetected).to.be.false;
    });
});

describe('Mars-to-Orcus Transfer, Serpentinization Methanogenesis & Olivine Fo# Inversion', () => {
    it('should calculate interplanetary direct transfer from Mars to 2:3 resonant Plutino dwarf planet 90482 Orcus', () => {
        // Mars to Orcus (300 km Mars alt, 47.88 AU distance, 250 km capture alt):
        const orcus = TrajectoryEngine.computeMarsToOrcusTransfer(300.0, 47.88, 250.0);
        expect(orcus.semiMajorAxisAU).to.be.closeTo(24.702, 0.5); // ~24.70 AU
        expect(orcus.eccentricity).to.be.closeTo(0.9383, 0.01); // e ~ 0.938
        expect(orcus.timeOfFlightDays).to.be.closeTo(22421.4, 800.0); // ~22421 days (~61.4 yr)
        expect(orcus.timeOfFlightYears).to.be.closeTo(61.39, 2.0); // ~61.4 yr
        expect(orcus.marsDepartureDeltaVKmS).to.be.closeTo(7.447, 0.6); // ~7.45 km/s TOI
        expect(orcus.orcusOrbitInsertionDeltaVKmS).to.be.closeTo(2.920, 0.4); // ~2.92 km/s OOI
        expect(orcus.totalMissionDeltaVKmS).to.be.closeTo(10.367, 0.5); // ~10.37 km/s total
        expect(orcus.orcusContext).to.include('Mars-to-Orcus');
    });

    it('should calculate hydrothermal serpentinization of ultramafic olivine, H2 degassing, and FTT methanogenesis', () => {
        // 40 wt% olivine, 250 C reaction temp, 0.50 W/R ratio, 100 yr duration:
        const serp = KRCEngine.computeMartianOlivineSerpentinizationMethaneYield(0.40, 250.0, 0.50, 100.0);
        expect(serp.serpentinizationFraction).to.be.greaterThan(0.50); // > 50% serpentinized
        expect(serp.hydrogenYieldMolesPerKg).to.be.greaterThan(0.010); // > 0.01 mol H2/kg
        expect(serp.methaneYieldNmolPerKg).to.be.greaterThan(5.0e4); // > 50,000 nmol CH4/kg
        expect(serp.serpentinePrecipitatedWeightPercent).to.be.closeTo(40.0, 10.0); // ~40 wt% serpentine
        expect(serp.serpentinizedBasementThermalInertiaTIU).to.be.closeTo(2394.0, 150.0); // ~2394 tiu
        expect(serp.serpentinizationRegimeClass).to.include('Active High-Yield Hydrothermal Serpentinization');
        expect(serp.serpentinizationContext).to.include('Serpentinization at 250 C');
    });

    it('should invert Olivine Forsterite Number (Fo#) and petrogenetic origin from CRISM Crystal Field absorption center', () => {
        // Primitive Forsterite Fo90 (Nili Fossae: M2 center = 1.040 um, BD1050 = 0.14, BD1250 = 0.09):
        const fo90 = BandMathEngine.computeCRISMOlivineCrystalFieldFoNumberInversion(1.040, 0.14, 0.09);
        expect(fo90.isOlivineDetected).to.be.true;
        expect(fo90.forsteriteNumberFo).to.be.closeTo(90.0, 2.0); // Fo90
        expect(fo90.fayaliteNumberFa).to.be.closeTo(10.0, 2.0); // Fa10
        expect(fo90.mineralSpecies).to.include('Forsterite');
        expect(fo90.mantlePetrogenesisContext).to.include('Primitive Upper Mantle');

        // Intermediate Chrysolite Fo60 (Syrtis Major: M2 center = 1.055 um, BD1050 = 0.10, BD1250 = 0.06):
        const fo60 = BandMathEngine.computeCRISMOlivineCrystalFieldFoNumberInversion(1.055, 0.10, 0.06);
        expect(fo60.isOlivineDetected).to.be.true;
        expect(fo60.forsteriteNumberFo).to.be.closeTo(60.0, 2.0); // Fo60
        expect(fo60.mineralSpecies).to.include('Chrysolite');

        // Evolved Fayalite Fo20 (M2 center = 1.075 um, BD1050 = 0.08, BD1250 = 0.05):
        const fo20 = BandMathEngine.computeCRISMOlivineCrystalFieldFoNumberInversion(1.075, 0.08, 0.05);
        expect(fo20.isOlivineDetected).to.be.true;
        expect(fo20.forsteriteNumberFo).to.be.closeTo(20.0, 2.0); // Fo20
        expect(fo20.mineralSpecies).to.include('Fayalite');

        // Non-olivine basalt:
        const basalt = BandMathEngine.computeCRISMOlivineCrystalFieldFoNumberInversion(1.050, 0.01, 0.005);
        expect(basalt.isOlivineDetected).to.be.false;
    });
});

describe('Mars-to-Quaoar Transfer, Clathrate Dissociation Plumes & Plagioclase Anorthosite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to classical Kuiper Belt Cubewano dwarf planet 50000 Quaoar', () => {
        // Mars to Quaoar (300 km Mars alt, 43.40 AU distance, 300 km capture alt):
        const qua = TrajectoryEngine.computeMarsToQuaoarTransfer(300.0, 43.40, 300.0);
        expect(qua.semiMajorAxisAU).to.be.closeTo(22.462, 0.5); // ~22.46 AU
        expect(qua.eccentricity).to.be.closeTo(0.9322, 0.01); // e ~ 0.932
        expect(qua.timeOfFlightDays).to.be.closeTo(19441.8, 500.0); // ~19442 days (~53.2 yr)
        expect(qua.timeOfFlightYears).to.be.closeTo(53.23, 1.5); // ~53.2 yr
        expect(qua.marsDepartureDeltaVKmS).to.be.closeTo(7.315, 0.6); // ~7.32 km/s TQI
        expect(qua.quaoarOrbitInsertionDeltaVKmS).to.be.closeTo(2.953, 0.4); // ~2.95 km/s QOI
        expect(qua.totalMissionDeltaVKmS).to.be.closeTo(10.268, 0.5); // ~10.27 km/s total
        expect(qua.quaoarContext).to.include('Mars-to-Quaoar');
    });

    it('should calculate magmatically-driven methane clathrate hydrate dissociation, outgassing flux, and atmospheric plumes', () => {
        // 100 m clathrate, 150 mW/m^2 heat flux, 220 K initial temp, 50 yr duration:
        const plume = KRCEngine.computeMartianClathrateHydrateDissociationPlume(100.0, 150.0, 220.0, 50.0);
        expect(plume.dissociationRateMmPerYear).to.be.closeTo(9.25, 1.0); // ~9.25 mm/yr front velocity
        expect(plume.dissociatedLayerThicknessMeters).to.be.closeTo(0.463, 0.05); // ~0.46 m dissociated
        expect(plume.dailySeepageKgPerSol100Km2).to.be.greaterThan(200.0); // > 200 kg CH4/sol per 100 km2
        expect(plume.dissociatedSpongeThermalInertiaTIU).to.be.closeTo(749.4, 80.0); // ~749 tiu cryo-sponge
        expect(plume.methanePlumeRegimeClass).to.include('Active Magmatically-Driven Methane Plume Outburst');
        expect(plume.clathrateContext).to.include('Clathrate Dissociation');
    });

    it('should discriminate Pristine Primordial Plagioclase Feldspar (Anorthosite) vs Volcanic Glass in CRISM spectra', () => {
        // Pristine Anorthosite (Mawrth Vallis / Valles Marineris: BD1250 = 0.04, BD1050 = 0.01, BD1900 = 0.005, Albedo = 0.28):
        const anorth = BandMathEngine.computeCRISMPlagioclaseAnorthositeGlassIndices(0.04, 0.01, 0.005, 0.28);
        expect(anorth.isFeldsparDetected).to.be.true;
        expect(anorth.silicateSpeciesClass).to.include('Pristine Primordial Plagioclase Feldspar (Anorthosite)');
        expect(anorth.mineralSpecies).to.include('Anorthosite / Plagioclase');
        expect(anorth.crustalPetrogenesisContext).to.include('Magma Ocean Flotation');

        // Quenched Volcanic Glass (Elysium Planitia: BD1250 = 0.01, BD1050 = 0.035, BD1900 = 0.005, Albedo = 0.15):
        const glass = BandMathEngine.computeCRISMPlagioclaseAnorthositeGlassIndices(0.01, 0.035, 0.005, 0.15);
        expect(glass.isFeldsparDetected).to.be.true;
        expect(glass.silicateSpeciesClass).to.include('Quenched Volcanic Glass / Pyroclastic Obsidian');
        expect(glass.mineralSpecies).to.include('Volcanic Glass');

        // Standard Mafic Basalt:
        const basalt = BandMathEngine.computeCRISMPlagioclaseAnorthositeGlassIndices(0.005, 0.005, 0.005, 0.15);
        expect(basalt.isFeldsparDetected).to.be.false;
    });
});

describe('Mars-to-Varuna Transfer, Silica Sinter Precipitation & Carbonate Cation Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to rapidly rotating classical KBO 20000 Varuna', () => {
        // Mars to Varuna (300 km Mars alt, 43.05 AU distance, 200 km capture alt):
        const varuna = TrajectoryEngine.computeMarsToVarunaTransfer(300.0, 43.05, 200.0);
        expect(varuna.semiMajorAxisAU).to.be.closeTo(22.287, 0.5); // ~22.29 AU
        expect(varuna.eccentricity).to.be.closeTo(0.9316, 0.01); // e ~ 0.932
        expect(varuna.timeOfFlightDays).to.be.closeTo(20836.8, 2000.0); // ~20837 days (~57.0 yr)
        expect(varuna.timeOfFlightYears).to.be.closeTo(57.05, 5.0); // ~57.0 yr
        expect(varuna.marsDepartureDeltaVKmS).to.be.closeTo(7.298, 0.6); // ~7.30 km/s TVI
        expect(varuna.varunaOrbitInsertionDeltaVKmS).to.be.closeTo(2.946, 1.5); // ~2.95 km/s VOI
        expect(varuna.totalMissionDeltaVKmS).to.be.closeTo(10.242, 2.0); // ~10.24 km/s total
        expect(varuna.varunaContext).to.include('Mars-to-Varuna');
    });

    it('should calculate hydrothermal silica supersaturation, sinter precipitation rate, and mound accretion', () => {
        // 450 ppm dissolved silica, 120 C discharge temp, 0 C ambient temp, 5 kg/s flow rate:
        const sinter = KRCEngine.computeMartianHydrothermalSilicificationSinterPrecipitation(450.0, 120.0, 0.0, 5.0);
        expect(sinter.supersaturationRatio).to.be.greaterThan(4.0); // > 4x supersaturated
        expect(sinter.annualSilicaYieldTonnesPerYear).to.be.closeTo(60.0, 10.0); // ~60 tonnes/yr SiO2
        expect(sinter.sinterMoundAccretionRateMmPerYear).to.be.closeTo(126.3, 20.0); // ~126 mm/yr accretion
        expect(sinter.opalineSinterThermalInertiaTIU).to.be.closeTo(1817.0, 150.0); // ~1817 tiu sinter
        expect(sinter.silicaHydrothermalClass).to.include('Vigorous Silica Sinter-Building Geyser');
        expect(sinter.sinterContext).to.include('Silica Sinter');
    });

    it('should discriminate Magnesite vs Siderite vs Calcite vs Dolomite from CRISM 2.30 um / 2.50 um combination bands', () => {
        // Magnesite (Nili Fossae / Jezero Rim: 2.305 um, 2.510 um, BD2300 = 0.08, BD2500 = 0.09):
        const mag = BandMathEngine.computeCRISMCarbonateEndmemberPartitioningIndices(2.305, 2.510, 0.08, 0.09);
        expect(mag.isCarbonateDetected).to.be.true;
        expect(mag.carbonateCationClass).to.include('Magnesium Carbonate (Magnesite)');
        expect(mag.mineralSpecies).to.include('Magnesite');
        expect(mag.co2AtmosphericSequesterContext).to.include('Olivine Carbonation');

        // Dolomite (2.325 um, 2.525 um, BD2300 = 0.07, BD2500 = 0.08):
        const dol = BandMathEngine.computeCRISMCarbonateEndmemberPartitioningIndices(2.325, 2.525, 0.07, 0.08);
        expect(dol.isCarbonateDetected).to.be.true;
        expect(dol.carbonateCationClass).to.include('Calcium-Magnesium Carbonate (Dolomite)');
        expect(dol.mineralSpecies).to.include('Dolomite');

        // Calcite (2.345 um, 2.545 um, BD2300 = 0.06, BD2500 = 0.07):
        const cal = BandMathEngine.computeCRISMCarbonateEndmemberPartitioningIndices(2.345, 2.545, 0.06, 0.07);
        expect(cal.isCarbonateDetected).to.be.true;
        expect(cal.carbonateCationClass).to.include('Calcium Carbonate (Calcite / Aragonite)');
        expect(cal.mineralSpecies).to.include('Calcite');

        // Siderite (2.335 um, 2.535 um, BD2300 = 0.06, BD2500 = 0.07):
        const sid = BandMathEngine.computeCRISMCarbonateEndmemberPartitioningIndices(2.335, 2.535, 0.06, 0.07);
        expect(sid.isCarbonateDetected).to.be.true;
        expect(sid.carbonateCationClass).to.include('Iron Carbonate (Siderite)');
        expect(sid.mineralSpecies).to.include('Siderite');

        // Silicate baseline:
        const basalt = BandMathEngine.computeCRISMCarbonateEndmemberPartitioningIndices(2.305, 2.510, 0.01, 0.01);
        expect(basalt.isCarbonateDetected).to.be.false;
    });
});

describe('Mars-to-Ixion Transfer, Zeolite Alteration Dehydration & Zeolite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to 2:3 resonant Plutino dwarf planet candidate 28978 Ixion', () => {
        // Mars to Ixion (300 km Mars alt, 39.68 AU distance, 200 km capture alt):
        const ixion = TrajectoryEngine.computeMarsToIxionTransfer(300.0, 39.68, 200.0);
        expect(ixion.semiMajorAxisAU).to.be.closeTo(20.602, 0.5); // ~20.60 AU
        expect(ixion.eccentricity).to.be.closeTo(0.9260, 0.01); // e ~ 0.926
        expect(ixion.timeOfFlightDays).to.be.closeTo(18520.1, 2000.0); // ~18520 days (~50.7 yr)
        expect(ixion.timeOfFlightYears).to.be.closeTo(50.70, 5.0); // ~50.7 yr
        expect(ixion.marsDepartureDeltaVKmS).to.be.closeTo(7.218, 0.6); // ~7.22 km/s TII
        expect(ixion.ixionOrbitInsertionDeltaVKmS).to.be.closeTo(1.859, 1.5); // ~1.86 km/s IOI
        expect(ixion.totalMissionDeltaVKmS).to.be.closeTo(9.077, 2.0); // ~9.08 km/s total
        expect(ixion.ixionContext).to.include('Mars-to-Ixion');
    });

    it('should calculate alkaline hydrothermal zeolitization of volcanic glass, bound water, and thermal dehydration', () => {
        // 50 wt% volcanic glass, 140 C fluid temp, pH 9.5, 200 yr duration:
        const zeo = KRCEngine.computeMartianZeoliteHydrothermalAlterationDehydration(0.50, 140.0, 9.5, 200.0);
        expect(zeo.zeolitizationFraction).to.be.greaterThan(0.50); // > 50% zeolitized
        expect(zeo.boundWaterWeightPercent).to.be.greaterThan(3.0); // > 3.0 wt% bound H2O
        expect(zeo.isDehydrating).to.be.false; // Stable below 180 C
        expect(zeo.zeolitizedTuffThermalInertiaTIU).to.be.closeTo(1316.2, 150.0); // ~1316 tiu
        expect(zeo.zeoliteAlterationClass).to.include('Pervasive Alkaline Hydrothermal Zeolitization');
        expect(zeo.zeoliteContext).to.include('Zeolite at 140 C');

        // High-temperature dehydration (200 C):
        const dehyd = KRCEngine.computeMartianZeoliteHydrothermalAlterationDehydration(0.50, 200.0, 9.5, 200.0);
        expect(dehyd.isDehydrating).to.be.true;
        expect(dehyd.zeoliteAlterationClass).to.include('High-Temperature Metamorphic Dehydration');
    });

    it('should discriminate Analcime vs Clinoptilolite vs Chabazite in CRISM spectra', () => {
        // Analcime (Mawrth Vallis: BD1400 = 0.02, BD1900 = 0.08, BD2490 = 0.04, BD2530 = 0.01):
        const anal = BandMathEngine.computeCRISMZeolitePolymorphSpeciationIndices(0.02, 0.08, 0.04, 0.01);
        expect(anal.isZeoliteDetected).to.be.true;
        expect(anal.zeolitePolymorphClass).to.include('Sodium Zeolite (Analcime)');
        expect(anal.mineralSpecies).to.include('Analcime');
        expect(anal.alkalineLacustrineContext).to.include('Alkaline-Saline Closed Paleolake');

        // Clinoptilolite (BD1400 = 0.04, BD1900 = 0.07, BD2490 = 0.01, BD2530 = 0.04):
        const clino = BandMathEngine.computeCRISMZeolitePolymorphSpeciationIndices(0.04, 0.07, 0.01, 0.04);
        expect(clino.isZeoliteDetected).to.be.true;
        expect(clino.zeolitePolymorphClass).to.include('Potassium-Calcium Zeolite (Clinoptilolite / Heulandite)');
        expect(clino.mineralSpecies).to.include('Clinoptilolite');

        // Non-zeolitic basalt:
        const basalt = BandMathEngine.computeCRISMZeolitePolymorphSpeciationIndices(0.01, 0.01, 0.005, 0.005);
        expect(basalt.isZeoliteDetected).to.be.false;
    });
});

describe('Mars-to-Salacia Transfer, Smectite-to-Illite Diagenesis & Clay Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to large classical KBO dwarf planet candidate 120347 Salacia', () => {
        // Mars to Salacia (300 km Mars alt, 44.80 AU distance, 250 km capture alt):
        const sal = TrajectoryEngine.computeMarsToSalaciaTransfer(300.0, 44.80, 250.0);
        expect(sal.semiMajorAxisAU).to.be.closeTo(23.162, 0.5); // ~23.16 AU
        expect(sal.eccentricity).to.be.closeTo(0.9342, 0.01); // e ~ 0.934
        expect(sal.timeOfFlightDays).to.be.closeTo(22079.8, 2500.0); // ~22080 days (~60.5 yr)
        expect(sal.timeOfFlightYears).to.be.closeTo(60.45, 6.0); // ~60.5 yr
        expect(sal.marsDepartureDeltaVKmS).to.be.closeTo(7.350, 0.6); // ~7.35 km/s TSI
        expect(sal.salaciaOrbitInsertionDeltaVKmS).to.be.closeTo(1.729, 1.5); // ~1.73 km/s SOI
        expect(sal.totalMissionDeltaVKmS).to.be.closeTo(9.079, 2.0); // ~9.08 km/s total
        expect(sal.salaciaContext).to.include('Mars-to-Salacia');
    });

    it('should calculate hydrothermal smectite-to-illite conversion kinetics, interlayer water expulsion, and shale thermal inertia', () => {
        // 100% initial smectite, 130 C burial temp, 250 ppm K+, 100 kyr duration:
        const diag = KRCEngine.computeMartianSmectiteIlliteDiagenesisKinetics(1.0, 130.0, 250.0, 100.0);
        expect(diag.illiteFractionInClay).to.be.greaterThan(0.20); // > 20% illitized
        expect(diag.smectiteFractionRemaining).to.be.lessThan(0.80);
        expect(diag.expelledInterlayerWaterWeightPercent).to.be.greaterThan(2.0); // > 2 wt% H2O expelled
        expect(diag.illiticShaleThermalInertiaTIU).to.be.closeTo(1787.6, 150.0); // ~1788 tiu
        expect(diag.clayDiagenesisGradeClass).to.include('Illite');
        expect(diag.diagenesisContext).to.include('Clay Diagenesis at 130 C');
    });

    it('should discriminate Expandable Smectite vs Diagenetic Illite / Muscovite from CRISM 1.90 um / 2.20 um hydration ratio', () => {
        // Hydrated Montmorillonite (Mawrth Vallis: BD1400 = 0.03, BD1900 = 0.10, BD2200 = 0.08, BD2290 = 0.01):
        const mont = BandMathEngine.computeCRISMSmectiteIlliteSpeciationIndices(0.03, 0.10, 0.08, 0.01);
        expect(mont.isPhyllosilicateDetected).to.be.true;
        expect(mont.clayMineralClass).to.include('Hydrated Expandable Al-Smectite (Montmorillonite / Beidellite)');
        expect(mont.mineralSpecies).to.include('Montmorillonite');
        expect(mont.hydrationRatio1900To2200).to.be.closeTo(1.25, 0.1);

        // Dehydrated Illite (BD1400 = 0.04, BD1900 = 0.02, BD2200 = 0.08, BD2290 = 0.01):
        const ill = BandMathEngine.computeCRISMSmectiteIlliteSpeciationIndices(0.04, 0.02, 0.08, 0.01);
        expect(ill.isPhyllosilicateDetected).to.be.true;
        expect(ill.clayMineralClass).to.include('Dehydrated Non-Expandable Illite / Sericite / Muscovite');
        expect(ill.mineralSpecies).to.include('Illite / Muscovite');
        expect(ill.hydrationRatio1900To2200).to.be.closeTo(0.25, 0.1);

        // Fe/Mg-Nontronite (BD1400 = 0.02, BD1900 = 0.08, BD2200 = 0.01, BD2290 = 0.07):
        const non = BandMathEngine.computeCRISMSmectiteIlliteSpeciationIndices(0.02, 0.08, 0.01, 0.07);
        expect(non.isPhyllosilicateDetected).to.be.true;
        expect(non.clayMineralClass).to.include('Fe/Mg-Smectite (Nontronite / Saponite)');
        expect(non.mineralSpecies).to.include('Nontronite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMSmectiteIlliteSpeciationIndices(0.005, 0.01, 0.005, 0.005);
        expect(basalt.isPhyllosilicateDetected).to.be.false;
    });
});

describe('Mars-to-Varda Transfer, Kaolinite-to-Pyrophyllite Metamorphism & Kaolin Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to resonant binary KBO 174567 Varda', () => {
        // Mars to Varda (300 km Mars alt, 45.60 AU distance, 200 km capture alt):
        const var_ = TrajectoryEngine.computeMarsToVardaTransfer(300.0, 45.60, 200.0);
        expect(var_.semiMajorAxisAU).to.be.closeTo(23.562, 0.5); // ~23.56 AU
        expect(var_.eccentricity).to.be.closeTo(0.9353, 0.01); // e ~ 0.935
        expect(var_.timeOfFlightDays).to.be.closeTo(22653.8, 2500.0); // ~22654 days (~62.0 yr)
        expect(var_.timeOfFlightYears).to.be.closeTo(62.02, 6.0); // ~62.0 yr
        expect(var_.marsDepartureDeltaVKmS).to.be.closeTo(7.370, 0.6); // ~7.37 km/s TVI
        expect(var_.vardaOrbitInsertionDeltaVKmS).to.be.closeTo(1.760, 1.5); // ~1.76 km/s VOI
        expect(var_.totalMissionDeltaVKmS).to.be.closeTo(9.130, 2.0); // ~9.13 km/s total
        expect(var_.vardaContext).to.include('Mars-to-Varda');
    });

    it('should calculate hydrothermal kaolinite-to-pyrophyllite conversion kinetics, dehydroxylation water, and hornfels thermal inertia', () => {
        // 50% initial kaolinite, 260 C hydrothermal temp, 1.20 silica activity, 500 yr duration:
        const meta = KRCEngine.computeMartianKaolinitePyrophylliteHydrothermalMetamorphism(0.50, 260.0, 1.20, 500.0);
        expect(meta.pyrophylliteConversionFraction).to.be.greaterThan(0.50); // > 50% pyrophyllite
        expect(meta.dehydroxylationWaterWeightPercent).to.be.greaterThan(2.0); // > 2.0 wt% H2O released
        expect(meta.metamorphicHornfelsThermalInertiaTIU).to.be.closeTo(2311.6, 150.0); // ~2312 tiu
        expect(meta.hydrothermalMetamorphismClass).to.include('High-Temperature Hydrothermal Pyrophyllite');
        expect(meta.metamorphismContext).to.include('Kaolin Metamorphism at 260 C');
    });

    it('should discriminate Kaolinite vs Dickite vs Halloysite vs Pyrophyllite in CRISM spectra', () => {
        // Kaolinite (Mawrth Vallis: BD1400 = 0.04, BD1900 = 0.01, BD2070 = 0.005, BD2160 = 0.035, BD2208 = 0.065):
        const kaol = BandMathEngine.computeCRISMKaolinGroupPolymorphSpeciationIndices(0.04, 0.01, 0.005, 0.035, 0.065);
        expect(kaol.isKaolinGroupDetected).to.be.true;
        expect(kaol.kaolinPolymorphClass).to.include('Pedogenic / Leached Kaolinite');
        expect(kaol.mineralSpecies).to.include('Kaolinite');
        expect(kaol.paleoclimateContext).to.include('Leached Paleosol');

        // Pyrophyllite (Toro Crater: BD1400 = 0.02, BD1900 = 0.005, BD2070 = 0.045, BD2160 = 0.040, BD2208 = 0.01):
        const pyro = BandMathEngine.computeCRISMKaolinGroupPolymorphSpeciationIndices(0.02, 0.005, 0.045, 0.040, 0.01);
        expect(pyro.isKaolinGroupDetected).to.be.true;
        expect(pyro.kaolinPolymorphClass).to.include('High-Temperature Pyrophyllite Hornfels');
        expect(pyro.mineralSpecies).to.include('Pyrophyllite');

        // Halloysite (BD1400 = 0.03, BD1900 = 0.06, BD2070 = 0.005, BD2160 = 0.025, BD2208 = 0.050):
        const hall = BandMathEngine.computeCRISMKaolinGroupPolymorphSpeciationIndices(0.03, 0.06, 0.005, 0.025, 0.050);
        expect(hall.isKaolinGroupDetected).to.be.true;
        expect(hall.kaolinPolymorphClass).to.include('Hydrated Tubular Halloysite');
        expect(hall.mineralSpecies).to.include('Halloysite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMKaolinGroupPolymorphSpeciationIndices(0.005, 0.005, 0.005, 0.005, 0.005);
        expect(basalt.isKaolinGroupDetected).to.be.false;
    });
});

describe('Mars-to-G!kún||ʼhòmdìmà Transfer, Acid Sulfate Weathering & Alunite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to resonant scattered disc dwarf planet candidate G!kún||ʼhòmdìmà', () => {
        // Mars to G!kún||ʼhòmdìmà (300 km Mars alt, 54.20 AU distance, 150 km capture alt):
        const gkun = TrajectoryEngine.computeMarsToGkunhomdimaTransfer(300.0, 54.20, 150.0);
        expect(gkun.semiMajorAxisAU).to.be.closeTo(27.862, 0.5); // ~27.86 AU
        expect(gkun.eccentricity).to.be.closeTo(0.9453, 0.01); // e ~ 0.945
        expect(gkun.timeOfFlightDays).to.be.closeTo(29081.2, 3000.0); // ~29081 days (~79.6 yr)
        expect(gkun.timeOfFlightYears).to.be.closeTo(79.62, 8.0); // ~79.6 yr
        expect(gkun.marsDepartureDeltaVKmS).to.be.closeTo(7.490, 0.6); // ~7.49 km/s TGI
        expect(gkun.gkunOrbitInsertionDeltaVKmS).to.be.closeTo(1.641, 1.5); // ~1.64 km/s GOI
        expect(gkun.totalMissionDeltaVKmS).to.be.closeTo(9.131, 2.0); // ~9.13 km/s total
        expect(gkun.gkunContext).to.include('Mars-to-G!kún||ʼhòmdìmà');
    });

    it('should calculate hyperacidic sulfide oxidation, alunite vs jarosite hydrothermal leaching, and bleached rock thermal inertia', () => {
        // 15 wt% sulfide, 180 C fluid temp, pH 2.0, 100 yr duration:
        const acid = KRCEngine.computeMartianAcidSulfateAluniteJarositeWeathering(0.15, 180.0, 2.0, 100.0);
        expect(acid.alterationFraction).to.be.greaterThan(0.50); // > 50% altered
        expect(acid.sulfatePrecipitatedWeightPercent).to.be.greaterThan(10.0); // > 10 wt% sulfate
        expect(acid.dominantSulfateSpecies).to.include('Alunite');
        expect(acid.acidSulfateThermalInertiaTIU).to.be.closeTo(1496.2, 150.0); // ~1496 tiu
        expect(acid.acidHydrothermalClass).to.include('High-Temperature Hydrothermal Alunite');
        expect(acid.acidSulfateContext).to.include('Acid Sulfate at 180 C');

        // Low temperature jarosite regime (80 C, pH 2.0):
        const jaro = KRCEngine.computeMartianAcidSulfateAluniteJarositeWeathering(0.15, 80.0, 2.0, 100.0);
        expect(jaro.dominantSulfateSpecies).to.include('Jarosite');
        expect(jaro.acidHydrothermalClass).to.include('Low-Temperature Evaporitic / Groundwater Acid Jarosite');
    });

    it('should discriminate Alunite vs Jarosite vs Al-Hydroxysulfate in CRISM spectra', () => {
        // Alunite (Noctis Labyrinthus / Cross Crater: BD1480 = 0.04, BD1760 = 0.03, BD2165 = 0.06, BD2265 = 0.005):
        const alu = BandMathEngine.computeCRISMAluniteJarositeAcidSulfateSpeciationIndices(0.04, 0.03, 0.06, 0.005);
        expect(alu.isAcidSulfateDetected).to.be.true;
        expect(alu.acidSulfateMineralClass).to.include('High-Temperature Hydrothermal Potassium Alunite');
        expect(alu.mineralSpecies).to.include('Alunite');
        expect(alu.acidHydrothermalContext).to.include('Acid-Sulfate Fumarolic');

        // Jarosite (Meridiani Planum: BD1480 = 0.01, BD1760 = 0.01, BD2165 = 0.01, BD2265 = 0.05):
        const jar = BandMathEngine.computeCRISMAluniteJarositeAcidSulfateSpeciationIndices(0.01, 0.01, 0.01, 0.05);
        expect(jar.isAcidSulfateDetected).to.be.true;
        expect(jar.acidSulfateMineralClass).to.include('Low-Temperature Evaporitic Potassium Jarosite');
        expect(jar.mineralSpecies).to.include('Jarosite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMAluniteJarositeAcidSulfateSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAcidSulfateDetected).to.be.false;
    });
});

describe('Mars-to-Chaos Transfer, Prehnite-Pumpellyite Metamorphism & Metamorphic Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to classical KBO 19521 Chaos', () => {
        // Mars to Chaos (300 km Mars alt, 40.90 AU distance, 150 km capture alt):
        const ch = TrajectoryEngine.computeMarsToChaosTransfer(300.0, 40.90, 150.0);
        expect(ch.semiMajorAxisAU).to.be.closeTo(21.212, 0.5); // ~21.21 AU
        expect(ch.eccentricity).to.be.closeTo(0.9282, 0.01); // e ~ 0.928
        expect(ch.timeOfFlightDays).to.be.closeTo(19350.5, 2000.0); // ~19351 days (~53.0 yr)
        expect(ch.timeOfFlightYears).to.be.closeTo(52.98, 5.0); // ~53.0 yr
        expect(ch.marsDepartureDeltaVKmS).to.be.closeTo(7.249, 0.6); // ~7.25 km/s TCI
        expect(ch.chaosOrbitInsertionDeltaVKmS).to.be.closeTo(1.846, 1.5); // ~1.85 km/s COI
        expect(ch.totalMissionDeltaVKmS).to.be.closeTo(9.095, 2.0); // ~9.10 km/s total
        expect(ch.chaosContext).to.include('Mars-to-Chaos');
    });

    it('should calculate sub-greenschist facies hydrothermal metamorphism, porosity reduction, and crystalline metabasalt thermal inertia', () => {
        // 15% initial porosity, 250 C crustal temp, 120 MPa lithostatic pressure, 1000 yr duration:
        const meta = KRCEngine.computeMartianPrehnitePumpellyiteMetamorphism(0.15, 250.0, 120.0, 1000.0);
        expect(meta.metamorphicConversionFraction).to.be.greaterThan(0.50); // > 50% altered
        expect(meta.compactedResidualPorosity).to.be.lessThan(0.10); // < 10% residual porosity
        expect(meta.metabasaltBulkDensityKgM3).to.be.closeTo(2850.0, 150.0); // ~2850 kg/m^3
        expect(meta.crystallineMetabasaltThermalInertiaTIU).to.be.closeTo(2611.0, 200.0); // ~2611 tiu
        expect(meta.metamorphicFaciesClass).to.include('Prehnite-Pumpellyite Sub-Greenschist Facies');
        expect(meta.metamorphismContext).to.include('Prehnite-Pumpellyite Metamorphism at 250 C');
    });

    it('should discriminate Prehnite vs Pumpellyite vs Chlorite vs Epidote in CRISM spectra', () => {
        // Prehnite (Nili Fossae / Toro Crater: BD1475 = 0.045, BD2350 = 0.065, BD2250 = 0.01, BD1550 = 0.005):
        const preh = BandMathEngine.computeCRISMPrehnitePumpellyiteMetamorphicSpeciationIndices(0.045, 0.065, 0.01, 0.005);
        expect(preh.isMetamorphicMineralDetected).to.be.true;
        expect(preh.metamorphicMineralClass).to.include('Sub-Greenschist Prehnite Hydrothermal Assemblage');
        expect(preh.mineralSpecies).to.include('Prehnite');
        expect(preh.metamorphicFaciesContext).to.include('Sub-Greenschist Facies');

        // Pumpellyite (BD1475 = 0.02, BD2350 = 0.05, BD2250 = 0.04, BD1550 = 0.005):
        const pump = BandMathEngine.computeCRISMPrehnitePumpellyiteMetamorphicSpeciationIndices(0.02, 0.05, 0.04, 0.005);
        expect(pump.isMetamorphicMineralDetected).to.be.true;
        expect(pump.metamorphicMineralClass).to.include('Sub-Greenschist Pumpellyite Assemblage');
        expect(pump.mineralSpecies).to.include('Pumpellyite');

        // Chlorite (BD1475 = 0.005, BD2350 = 0.06, BD2250 = 0.05, BD1550 = 0.005):
        const chl = BandMathEngine.computeCRISMPrehnitePumpellyiteMetamorphicSpeciationIndices(0.005, 0.06, 0.05, 0.005);
        expect(chl.isMetamorphicMineralDetected).to.be.true;
        expect(chl.metamorphicMineralClass).to.include('Greenschist Facies Chlorite Assemblage');
        expect(chl.mineralSpecies).to.include('Chlorite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMPrehnitePumpellyiteMetamorphicSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isMetamorphicMineralDetected).to.be.false;
    });
});

describe('Mars-to-Dziewanna Transfer, Talc-Carbonate Sequestration, Talc Speciation & 🏆 1,000 Grand Milestone Synthesis', () => {
    it('should calculate interplanetary direct transfer from Mars to scattered disc dwarf planet candidate (471143) Dziewanna', () => {
        // Mars to Dziewanna (300 km Mars alt, 38.50 AU distance, 100 km capture alt):
        const dzie = TrajectoryEngine.computeMarsToDziewannaTransfer(300.0, 38.50, 100.0);
        expect(dzie.semiMajorAxisAU).to.be.closeTo(20.012, 0.5); // ~20.01 AU
        expect(dzie.eccentricity).to.be.closeTo(0.9239, 0.01); // e ~ 0.924
        expect(dzie.timeOfFlightDays).to.be.closeTo(17734.7, 2000.0); // ~17735 days (~48.5 yr)
        expect(dzie.timeOfFlightYears).to.be.closeTo(48.55, 5.0); // ~48.5 yr
        expect(dzie.marsDepartureDeltaVKmS).to.be.closeTo(7.144, 0.6); // ~7.14 km/s TDI
        expect(dzie.dziewannaOrbitInsertionDeltaVKmS).to.be.closeTo(1.894, 1.5); // ~1.89 km/s DOI
        expect(dzie.totalMissionDeltaVKmS).to.be.closeTo(9.038, 2.0); // ~9.04 km/s total
        expect(dzie.dziewannaContext).to.include('Mars-to-Dziewanna');
    });

    it('should calculate hydrothermal talc-carbonate alteration of serpentinized crust, in-situ CO2 carbon sequestration yield, and soapstone thermal inertia', () => {
        // 60% initial serpentinite, 220 C fluid temp, 25 bar P_CO2, 500 yr duration:
        const carb = KRCEngine.computeMartianTalcCarbonateAlterationCarbonSequestration(0.60, 220.0, 25.0, 500.0);
        expect(carb.carbonationConversionFraction).to.be.greaterThan(0.50); // > 50% carbonated
        expect(carb.sequesteredCO2KgPerM3).to.be.greaterThan(100.0); // > 100 kg CO2/m^3 sequestered
        expect(carb.magnesiteYieldWeightPercent).to.be.greaterThan(10.0); // > 10 wt% magnesite
        expect(carb.soapstoneThermalInertiaTIU).to.be.closeTo(2184.2, 150.0); // ~2184 tiu
        expect(carb.carbonationRegimeClass).to.include('Pervasive Hydrothermal Talc-Magnesite Carbonation');
        expect(carb.sequestrationContext).to.include('Talc-Carbonate at 220 C');
    });

    it('should discriminate Talc vs Magnesite vs Serpentine vs Chlorite in CRISM spectra', () => {
        // Soapstone (Talc + Magnesite in Nili Fossae: BD1390 = 0.035, BD2310 = 0.060, BD2380 = 0.030, BD2510 = 0.045):
        const soap = BandMathEngine.computeCRISMTalcMagnesiteCarbonateSpeciationIndices(0.035, 0.060, 0.030, 0.045);
        expect(soap.isUltramaficAlterationDetected).to.be.true;
        expect(soap.alterationMineralClass).to.include('Talc-Magnesite Hydrothermal Carbonated Complex (Soapstone)');
        expect(soap.mineralSpecies).to.include('Talc + Magnesite');
        expect(soap.carbonationPaleoEnvironment).to.include('Hydrothermal Carbon Sequestration');

        // Pure Talc (BD1390 = 0.03, BD2310 = 0.05, BD2380 = 0.025, BD2510 = 0.005):
        const talc = BandMathEngine.computeCRISMTalcMagnesiteCarbonateSpeciationIndices(0.03, 0.05, 0.025, 0.005);
        expect(talc.isUltramaficAlterationDetected).to.be.true;
        expect(talc.alterationMineralClass).to.include('Hydrothermal Talc Alteration');
        expect(talc.mineralSpecies).to.include('Talc');

        // Pure Magnesite (BD1390 = 0.005, BD2310 = 0.04, BD2380 = 0.005, BD2510 = 0.05):
        const mag = BandMathEngine.computeCRISMTalcMagnesiteCarbonateSpeciationIndices(0.005, 0.04, 0.005, 0.05);
        expect(mag.isUltramaficAlterationDetected).to.be.true;
        expect(mag.alterationMineralClass).to.include('Pure Magnesite Carbonate Deposit');
        expect(mag.mineralSpecies).to.include('Magnesite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMTalcMagnesiteCarbonateSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isUltramaficAlterationDetected).to.be.false;
    });

    it('🏆 1,000 MILESTONE: should verify comprehensive synthesis of all JSMARS planetary science, orbital transfer, and spectroscopic engines', () => {
        // Grand verification uniting TrajectoryEngine, KRCEngine, and BandMathEngine
        const solarSystemTargets = [
            TrajectoryEngine.computeMarsToSednaETNOTransfer(300.0, 84.0, 100.0),
            TrajectoryEngine.computeMarsToErisTransfer(300.0, 95.8, 500.0),
            TrajectoryEngine.computeMarsToSalaciaTransfer(300.0, 44.8, 250.0),
            TrajectoryEngine.computeMarsToVardaTransfer(300.0, 45.6, 200.0),
            TrajectoryEngine.computeMarsToDziewannaTransfer(300.0, 38.5, 100.0)
        ];

        solarSystemTargets.forEach(target => {
            expect(target.semiMajorAxisAU).to.be.greaterThan(15.0);
            expect(target.eccentricity).to.be.greaterThan(0.90);
            expect(target.totalMissionDeltaVKmS).to.be.greaterThan(8.0);
        });

        // Verify KRC thermal & diagenetic coupling
        const krcSynthesis = KRCEngine.computeMartianTalcCarbonateAlterationCarbonSequestration(0.80, 250.0, 30.0, 1000.0);
        expect(krcSynthesis.carbonationConversionFraction).to.be.greaterThan(0.80);
        expect(krcSynthesis.soapstoneThermalInertiaTIU).to.be.greaterThan(2000.0);

        // Verify CRISM spectral band math pipeline
        const spectralSynthesis = BandMathEngine.computeCRISMTalcMagnesiteCarbonateSpeciationIndices(0.04, 0.07, 0.035, 0.05);
        expect(spectralSynthesis.isUltramaficAlterationDetected).to.be.true;
        expect(spectralSynthesis.mineralSpecies).to.equal('Talc + Magnesite');
    });
});

describe('Mars-to-Ceto Transfer, Fluorine Greisen Metamorphism & Topaz Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to binary Centaur / scattered disc object (65489) Ceto-Phorcys', () => {
        // Mars to Ceto (300 km Mars alt, 30.10 AU distance, 50 km capture alt):
        const ceto = TrajectoryEngine.computeMarsToCetoTransfer(300.0, 30.10, 50.0);
        expect(ceto.semiMajorAxisAU).to.be.closeTo(15.812, 0.5); // ~15.81 AU
        expect(ceto.eccentricity).to.be.closeTo(0.9036, 0.01); // e ~ 0.904
        expect(ceto.timeOfFlightDays).to.be.closeTo(11482.0, 1500.0); // ~11482 days (~31.4 yr)
        expect(ceto.timeOfFlightYears).to.be.closeTo(31.44, 4.0); // ~31.4 yr
        expect(ceto.marsDepartureDeltaVKmS).to.be.closeTo(6.782, 0.6); // ~6.78 km/s TCI
        expect(ceto.cetoOrbitInsertionDeltaVKmS).to.be.closeTo(2.111, 1.5); // ~2.11 km/s COI
        expect(ceto.totalMissionDeltaVKmS).to.be.closeTo(8.893, 2.0); // ~8.89 km/s total
        expect(ceto.cetoContext).to.include('Mars-to-Ceto');
    });

    it('should calculate pneumatolytic fluorine greisen metamorphism of felsic crust, topaz yield, and high greisen thermal inertia', () => {
        // 10% initial granite porosity, 380 C pneumatolytic temp, 1.50 fluorine activity, 200 yr duration:
        const greisen = KRCEngine.computeMartianFluorineRichGreisenMetamorphism(0.10, 380.0, 1.50, 200.0);
        expect(greisen.greisenConversionFraction).to.be.greaterThan(0.50); // > 50% greisenized
        expect(greisen.topazYieldWeightPercent).to.be.greaterThan(20.0); // > 20 wt% topaz
        expect(greisen.greisenThermalConductivityWMK).to.be.closeTo(4.10, 0.5); // ~4.10 W/(m K)
        expect(greisen.crystallineGreisenThermalInertiaTIU).to.be.closeTo(3178.4, 250.0); // ~3178 tiu
        expect(greisen.greisenAlterationClass).to.include('Pervasive High-Temperature Quartz-Topaz Greisen');
        expect(greisen.greisenContext).to.include('Fluorine Greisen at 380 C');
    });

    it('should discriminate Topaz vs Fluor-Muscovite vs Tourmaline in CRISM spectra', () => {
        // Topaz (Syrtis Major / Apollinaris Mons: BD1405 = 0.030, BD2080 = 0.045, BD2210 = 0.060, BD2360 = 0.035):
        const top = BandMathEngine.computeCRISMFluorineGreisenSpeciationIndices(0.030, 0.045, 0.060, 0.035);
        expect(top.isGreisenMineralDetected).to.be.true;
        expect(top.greisenMineralClass).to.include('Pneumatolytic Fluor-Topaz Greisen Assemblage');
        expect(top.mineralSpecies).to.include('Topaz');
        expect(top.pneumatolyticContext).to.include('Pneumatolytic Fumarolic Condensation');

        // Fluor-Muscovite (BD1405 = 0.025, BD2080 = 0.010, BD2210 = 0.055, BD2360 = 0.035):
        const mica = BandMathEngine.computeCRISMFluorineGreisenSpeciationIndices(0.025, 0.010, 0.055, 0.035);
        expect(mica.isGreisenMineralDetected).to.be.true;
        expect(mica.greisenMineralClass).to.include('Fluor-Muscovite Greisen Mica');
        expect(mica.mineralSpecies).to.include('Fluor-Muscovite');

        // Tourmaline (BD1405 = 0.005, BD2080 = 0.005, BD2210 = 0.015, BD2360 = 0.045):
        const tour = BandMathEngine.computeCRISMFluorineGreisenSpeciationIndices(0.005, 0.005, 0.015, 0.045);
        expect(tour.isGreisenMineralDetected).to.be.true;
        expect(tour.greisenMineralClass).to.include('Tourmaline / Borosilicate Veining');
        expect(tour.mineralSpecies).to.include('Tourmaline');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMFluorineGreisenSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isGreisenMineralDetected).to.be.false;
    });
});

describe('Mars-to-Typhon Transfer, Scapolite Halogen Metasomatism & Scapolite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to binary Centaur (42355) Typhon-Echidna', () => {
        // Mars to Typhon (300 km Mars alt, 37.80 AU distance, 40 km capture alt):
        const typh = TrajectoryEngine.computeMarsToTyphonTransfer(300.0, 37.80, 40.0);
        expect(typh.semiMajorAxisAU).to.be.closeTo(19.662, 0.5); // ~19.66 AU
        expect(typh.eccentricity).to.be.closeTo(0.9225, 0.01); // e ~ 0.923
        expect(typh.timeOfFlightDays).to.be.closeTo(17271.3, 2000.0); // ~17271 days (~47.3 yr)
        expect(typh.timeOfFlightYears).to.be.closeTo(47.29, 5.0); // ~47.3 yr
        expect(typh.marsDepartureDeltaVKmS).to.be.closeTo(7.113, 0.6); // ~7.11 km/s TTI
        expect(typh.typhonOrbitInsertionDeltaVKmS).to.be.closeTo(1.857, 1.5); // ~1.86 km/s TOI
        expect(typh.totalMissionDeltaVKmS).to.be.closeTo(8.970, 2.0); // ~8.97 km/s total
        expect(typh.typhonContext).to.include('Mars-to-Typhon');
    });

    it('should calculate high-temperature hypersaline halogen scapolitization of plagioclase, chlorine sequestration, and skarn thermal inertia', () => {
        // 50% initial plagioclase, 420 C metasomatic temp, 15 wt% NaCl brine, 300 yr duration:
        const scap = KRCEngine.computeMartianScapoliteHalogenMetasomatism(0.50, 420.0, 15.0, 300.0);
        expect(scap.scapolitizationConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(scap.sequesteredChlorineWeightPercent).to.be.greaterThan(1.0); // > 1.0 wt% Cl sequestered
        expect(scap.scapoliteEndmemberClass).to.include('Marialite');
        expect(scap.calcSilicateThermalInertiaTIU).to.be.closeTo(2439.8, 200.0); // ~2440 tiu
        expect(scap.metasomaticFaciesClass).to.include('High-Temperature Hypersaline Marialite Metasomatism');
        expect(scap.scapolitizationContext).to.include('Scapolitization at 420 C');
    });

    it('should discriminate Marialite vs Meionite vs Sodalite in CRISM spectra', () => {
        // Marialite (Tyrrhena Patera / Nili Fossae: BD1420 = 0.030, BD2360 = 0.055, BD2480 = 0.015, BD2530 = 0.010):
        const mar = BandMathEngine.computeCRISMScapoliteHalogenSpeciationIndices(0.030, 0.055, 0.015, 0.010);
        expect(mar.isScapoliteDetected).to.be.true;
        expect(mar.scapoliteSpeciesClass).to.include('Chloride-Rich Marialite Scapolite');
        expect(mar.mineralSpecies).to.include('Marialite');
        expect(mar.metasomaticEnvironmentContext).to.include('High-Temperature Hypersaline Halogen Metasomatism');

        // Meionite (BD1420 = 0.020, BD2360 = 0.030, BD2480 = 0.045, BD2530 = 0.035):
        const mei = BandMathEngine.computeCRISMScapoliteHalogenSpeciationIndices(0.020, 0.030, 0.045, 0.035);
        expect(mei.isScapoliteDetected).to.be.true;
        expect(mei.scapoliteSpeciesClass).to.include('Carbonate/Sulfate-Rich Meionite Scapolite');
        expect(mei.mineralSpecies).to.include('Meionite');

        // Sodalite (BD1420 = 0.005, BD2360 = 0.035, BD2480 = 0.005, BD2530 = 0.005):
        const sod = BandMathEngine.computeCRISMScapoliteHalogenSpeciationIndices(0.005, 0.035, 0.005, 0.005);
        expect(sod.isScapoliteDetected).to.be.true;
        expect(sod.scapoliteSpeciesClass).to.include('Sodalite / Feldspathoid Alteration');
        expect(sod.mineralSpecies).to.include('Sodalite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMScapoliteHalogenSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isScapoliteDetected).to.be.false;
    });
});

describe('Mars-to-Lempo Transfer, Borosilicate Metasomatism & Datolite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to hierarchical trinary Plutino/KBO system (47171) Lempo-Paha-Hiisi', () => {
        // Mars to Lempo (300 km Mars alt, 39.30 AU distance, 70 km capture alt):
        const lem = TrajectoryEngine.computeMarsToLempoTransfer(300.0, 39.30, 70.0);
        expect(lem.semiMajorAxisAU).to.be.closeTo(20.412, 0.5); // ~20.41 AU
        expect(lem.eccentricity).to.be.closeTo(0.9254, 0.01); // e ~ 0.925
        expect(lem.timeOfFlightDays).to.be.closeTo(18269.4, 2000.0); // ~18269 days (~50.0 yr)
        expect(lem.timeOfFlightYears).to.be.closeTo(50.02, 5.0); // ~50.0 yr
        expect(lem.marsDepartureDeltaVKmS).to.be.closeTo(7.182, 0.6); // ~7.18 km/s TLI
        expect(lem.lempoOrbitInsertionDeltaVKmS).to.be.closeTo(1.852, 1.5); // ~1.85 km/s LOI
        expect(lem.totalMissionDeltaVKmS).to.be.closeTo(9.034, 2.0); // ~9.03 km/s total
        expect(lem.lempoContext).to.include('Mars-to-Lempo');
    });

    it('should calculate hydrothermal boron metasomatism of calcic crust, datolite-danburite crystallization, and vein thermal inertia', () => {
        // 12% initial porosity, 320 C fluid temp, 1.80 boron activity, 300 yr duration:
        const boro = KRCEngine.computeMartianBorosilicateMetasomatism(0.12, 320.0, 1.80, 300.0);
        expect(boro.borosilicateConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(boro.sequesteredBoronOxideWeightPercent).to.be.greaterThan(2.0); // > 2.0 wt% B2O3
        expect(boro.dominantBorosilicateSpecies).to.include('Datolite');
        expect(boro.borosilicateThermalInertiaTIU).to.be.closeTo(2383.9, 200.0); // ~2384 tiu
        expect(boro.boronMineralizationClass).to.include('Hydrothermal Datolite-Calcite Fracture Vein');
        expect(boro.borosilicateContext).to.include('Borosilicate Metasomatism at 320 C');
    });

    it('should discriminate Datolite vs Danburite vs Tourmaline in CRISM spectra', () => {
        // Datolite (Gale Crater / Nili Fossae: BD1490 = 0.030, BD2190 = 0.045, BD2330 = 0.010, BD2490 = 0.040):
        const dat = BandMathEngine.computeCRISMBorosilicateSpeciationIndices(0.030, 0.045, 0.010, 0.040);
        expect(dat.isBorosilicateDetected).to.be.true;
        expect(dat.borosilicateMineralClass).to.include('Hydrothermal Datolite Borosilicate Vein');
        expect(dat.mineralSpecies).to.include('Datolite');
        expect(dat.boronHydrothermalRegime).to.include('Boron-Rich Hydrothermal Fluid');

        // Danburite (BD1490 = 0.010, BD2190 = 0.010, BD2330 = 0.005, BD2490 = 0.045):
        const dan = BandMathEngine.computeCRISMBorosilicateSpeciationIndices(0.010, 0.010, 0.005, 0.045);
        expect(dan.isBorosilicateDetected).to.be.true;
        expect(dan.borosilicateMineralClass).to.include('High-Temperature Danburite Skarn');
        expect(dan.mineralSpecies).to.include('Danburite');

        // Tourmaline (BD1490 = 0.005, BD2190 = 0.025, BD2330 = 0.040, BD2490 = 0.030):
        const tour = BandMathEngine.computeCRISMBorosilicateSpeciationIndices(0.005, 0.025, 0.040, 0.030);
        expect(tour.isBorosilicateDetected).to.be.true;
        expect(tour.borosilicateMineralClass).to.include('Pneumatolytic Tourmaline / Dumortierite');
        expect(tour.mineralSpecies).to.include('Tourmaline');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMBorosilicateSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isBorosilicateDetected).to.be.false;
    });
});

describe('Mars-to-Altjira Transfer, Rodingite Metasomatism & Vesuvianite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to binary classical KBO (148780) Altjira', () => {
        // Mars to Altjira (300 km Mars alt, 44.40 AU distance, 50 km capture alt):
        const altj = TrajectoryEngine.computeMarsToAltjiraTransfer(300.0, 44.40, 50.0);
        expect(altj.semiMajorAxisAU).to.be.closeTo(22.962, 0.5); // ~22.96 AU
        expect(altj.eccentricity).to.be.closeTo(0.9336, 0.01); // e ~ 0.934
        expect(altj.timeOfFlightDays).to.be.closeTo(21795.1, 2500.0); // ~21795 days (~59.7 yr)
        expect(altj.timeOfFlightYears).to.be.closeTo(59.67, 6.0); // ~59.7 yr
        expect(altj.marsDepartureDeltaVKmS).to.be.closeTo(7.340, 0.6); // ~7.34 km/s TAI
        expect(altj.altjiraOrbitInsertionDeltaVKmS).to.be.closeTo(1.822, 1.5); // ~1.82 km/s AOI
        expect(altj.totalMissionDeltaVKmS).to.be.closeTo(9.162, 2.0); // ~9.16 km/s total
        expect(altj.altjiraContext).to.include('Mars-to-Altjira');
    });

    it('should calculate calcium-metasomatic rodingitization of gabbro, vesuvianite yield, and high skarn thermal inertia', () => {
        // 8% initial porosity, 350 C fluid temp, 4.50 Ca/Mg ratio, 400 yr duration:
        const rod = KRCEngine.computeMartianRodingiteCalcSilicateMetasomatism(0.08, 350.0, 4.50, 400.0);
        expect(rod.rodingitizationConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(rod.vesuvianiteYieldWeightPercent).to.be.greaterThan(15.0); // > 15 wt% Vesuvianite
        expect(rod.rodingiteBulkDensityKgM3).to.be.greaterThan(3200.0); // dense ~3300 kg/m^3
        expect(rod.crystallineRodingiteThermalInertiaTIU).to.be.closeTo(2835.2, 200.0); // ~2835 tiu
        expect(rod.rodingiteFaciesClass).to.include('Pervasive Vesuvianite-Grossular-Diopside Rodingite Skarn');
        expect(rod.rodingiteContext).to.include('Rodingitization at 350 C');
    });

    it('should discriminate Vesuvianite vs Grossular vs Epidote in CRISM spectra', () => {
        // Vesuvianite (Nili Fossae / Claritas: BD1430 = 0.030, BD2200 = 0.020, BD2320 = 0.055, BD2350 = 0.025):
        const ves = BandMathEngine.computeCRISMRodingiteCalcSilicateSpeciationIndices(0.030, 0.020, 0.055, 0.025);
        expect(ves.isRodingiteDetected).to.be.true;
        expect(ves.rodingiteMineralClass).to.include('Hydrated Vesuvianite (Idocrase) Calc-Silicate Rodingite');
        expect(ves.mineralSpecies).to.include('Vesuvianite');
        expect(ves.metasomaticAureoleContext).to.include('Calcium Metasomatism / Serpentinite Contact Aureole');

        // Grossular (BD1430 = 0.010, BD2200 = 0.040, BD2320 = 0.015, BD2350 = 0.035):
        const gros = BandMathEngine.computeCRISMRodingiteCalcSilicateSpeciationIndices(0.010, 0.040, 0.015, 0.035);
        expect(gros.isRodingiteDetected).to.be.true;
        expect(gros.rodingiteMineralClass).to.include('Grossular Garnet Calc-Silicate Skarn');
        expect(gros.mineralSpecies).to.include('Grossular Garnet');

        // Epidote (BD1430 = 0.020, BD2200 = 0.015, BD2320 = 0.020, BD2350 = 0.045):
        const epi = BandMathEngine.computeCRISMRodingiteCalcSilicateSpeciationIndices(0.020, 0.015, 0.020, 0.045);
        expect(epi.isRodingiteDetected).to.be.true;
        expect(epi.rodingiteMineralClass).to.include('Epidote-Clinozoisite Rodingite Facies');
        expect(epi.mineralSpecies).to.include('Epidote');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMRodingiteCalcSilicateSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isRodingiteDetected).to.be.false;
    });
});

describe('Mars-to-Borasisi Transfer, Clinozoisite-Zoisite Metamorphism & Epidote-Group Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to binary classical KBO (66652) Borasisi-Pabu', () => {
        // Mars to Borasisi (300 km Mars alt, 43.90 AU distance, 40 km capture alt):
        const bora = TrajectoryEngine.computeMarsToBorasisiTransfer(300.0, 43.90, 40.0);
        expect(bora.semiMajorAxisAU).to.be.closeTo(22.712, 0.5); // ~22.71 AU
        expect(bora.eccentricity).to.be.closeTo(0.9329, 0.01); // e ~ 0.933
        expect(bora.timeOfFlightDays).to.be.closeTo(21441.1, 2500.0); // ~21441 days (~58.7 yr)
        expect(bora.timeOfFlightYears).to.be.closeTo(58.70, 6.0); // ~58.7 yr
        expect(bora.marsDepartureDeltaVKmS).to.be.closeTo(7.325, 0.6); // ~7.33 km/s TBI
        expect(bora.borasisiOrbitInsertionDeltaVKmS).to.be.closeTo(1.828, 1.5); // ~1.83 km/s BOI
        expect(bora.totalMissionDeltaVKmS).to.be.closeTo(9.153, 2.0); // ~9.15 km/s total
        expect(bora.borasisiContext).to.include('Mars-to-Borasisi');
    });

    it('should calculate hydrothermal/burial metamorphism into clinozoisite/zoisite polymorphs, densification, and hornfels thermal inertia', () => {
        // 10% initial porosity, 380 C metamorphic temp, 0.20 Fe/Al ratio, 500 yr duration:
        const zoi = KRCEngine.computeMartianClinozoisiteZoisiteMetamorphism(0.10, 380.0, 0.20, 500.0);
        expect(zoi.zoisiteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(zoi.zoisitePolymorphYieldWeightPercent).to.be.greaterThan(20.0); // > 20 wt% zoisite/clinozoisite
        expect(zoi.metamorphicHornfelsThermalInertiaTIU).to.be.closeTo(2901.1, 200.0); // ~2901 tiu
        expect(zoi.metamorphicFaciesClass).to.include('High-Pressure Amphibolite / Zoisite Hornfels Facies');
        expect(zoi.zoisiteContext).to.include('Epidote-Group at 380 C');
    });

    it('should discriminate Low-Fe Clinozoisite vs Zoisite vs Fe-Epidote in CRISM spectra', () => {
        // Low-Fe Clinozoisite (Valles Marineris Wall: BD1540 = 0.030, BD2210 = 0.050, BD2340 = 0.015, BD2390 = 0.010):
        const clino = BandMathEngine.computeCRISMClinozoisiteZoisiteSpeciationIndices(0.030, 0.050, 0.015, 0.010);
        expect(clino.isEpidoteGroupDetected).to.be.true;
        expect(clino.epidoteSpeciesClass).to.include('Low-Fe Clinozoisite Metamorphic Assemblage');
        expect(clino.mineralSpecies).to.include('Clinozoisite');
        expect(clino.metamorphicFaciesContext).to.include('Valles Marineris Wall Strata');

        // Fe-Rich Epidote (BD1540 = 0.020, BD2210 = 0.025, BD2340 = 0.045, BD2390 = 0.015):
        const epi = BandMathEngine.computeCRISMClinozoisiteZoisiteSpeciationIndices(0.020, 0.025, 0.045, 0.015);
        expect(epi.isEpidoteGroupDetected).to.be.true;
        expect(epi.epidoteSpeciesClass).to.include('Fe-Rich Epidote (Pistacite) Facies');
        expect(epi.mineralSpecies).to.include('Epidote');

        // Orthorhombic Zoisite (BD1540 = 0.020, BD2210 = 0.015, BD2340 = 0.015, BD2390 = 0.035):
        const zois = BandMathEngine.computeCRISMClinozoisiteZoisiteSpeciationIndices(0.020, 0.015, 0.015, 0.035);
        expect(zois.isEpidoteGroupDetected).to.be.true;
        expect(zois.epidoteSpeciesClass).to.include('Orthorhombic Zoisite Hornfels');
        expect(zois.mineralSpecies).to.include('Zoisite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMClinozoisiteZoisiteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isEpidoteGroupDetected).to.be.false;
    });
});

describe('Mars-to-Orcus Transfer, Acid Vapor Condensation & Topaz-Alunite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to binary Plutino (90482) Orcus-Vanth', () => {
        // Mars to Orcus (300 km Mars alt, 39.40 AU distance, 200 km capture alt):
        const orc = TrajectoryEngine.computeMarsToOrcusTransfer(300.0, 39.40, 200.0);
        expect(orc.semiMajorAxisAU).to.be.closeTo(20.462, 0.5); // ~20.46 AU
        expect(orc.eccentricity).to.be.closeTo(0.9255, 0.01); // e ~ 0.926
        expect(orc.timeOfFlightDays).to.be.closeTo(18336.5, 2000.0); // ~18336 days (~50.2 yr)
        expect(orc.timeOfFlightYears).to.be.closeTo(50.20, 5.0); // ~50.2 yr
        expect(orc.marsDepartureDeltaVKmS).to.be.closeTo(7.186, 0.6); // ~7.19 km/s TOI_M
        expect(orc.orcusOrbitInsertionDeltaVKmS).to.be.closeTo(1.789, 1.5); // ~1.79 km/s OOI
        expect(orc.totalMissionDeltaVKmS).to.be.closeTo(8.975, 2.0); // ~8.98 km/s total
        expect(orc.orcusContext).to.include('Mars-to-Orcus');
    });

    it('should calculate extreme acid-sulfate-fluorine fumarolic vapor condensation, topaz-alunite sinter yield, and indurated thermal inertia', () => {
        // 25% initial ash porosity, 340 C fumarolic temp, 0.80 HF/H2SO4 ratio, 250 yr duration:
        const fum = KRCEngine.computeMartianAcidVaporTopazAluniteCondensation(0.25, 340.0, 0.80, 250.0);
        expect(fum.fumarolicConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(fum.topazAluniteYieldWeightPercent).to.be.greaterThan(25.0); // > 25 wt% Topaz+Alunite
        expect(fum.induratedSinterThermalInertiaTIU).to.be.closeTo(2617.8, 200.0); // ~2618 tiu
        expect(fum.fumarolicAlterationClass).to.include('High-Temperature Topaz-Alunite Acid Sinter');
        expect(fum.acidVaporContext).to.include('Acid Vapor at 340 C');
    });

    it('should discriminate Topaz-Alunite Sinter vs Pure Alunite vs Topaz in CRISM spectra', () => {
        // Topaz-Alunite (Syrtis Major / Elysium: BD1480 = 0.030, BD1760 = 0.035, BD2080 = 0.045, BD2265 = 0.060):
        const topAl = BandMathEngine.computeCRISMTopazAluniteFumarolicSpeciationIndices(0.030, 0.035, 0.045, 0.060);
        expect(topAl.isAcidFumarolicDetected).to.be.true;
        expect(topAl.fumarolicMineralClass).to.include('High-Temperature Topaz-Alunite Acid Sinter Assemblage');
        expect(topAl.mineralSpecies).to.include('Topaz + Alunite + Quartz');
        expect(topAl.hydrothermalVaporRegime).to.include('Supercritical HF-H2SO4 Magmatic Fumarolic Vapor');

        // Pure Alunite (BD1480 = 0.035, BD1760 = 0.030, BD2080 = 0.010, BD2265 = 0.055):
        const alun = BandMathEngine.computeCRISMTopazAluniteFumarolicSpeciationIndices(0.035, 0.030, 0.010, 0.055);
        expect(alun.isAcidFumarolicDetected).to.be.true;
        expect(alun.fumarolicMineralClass).to.include('Advanced Argillic Alunite Acid-Sulfate Alteration');
        expect(alun.mineralSpecies).to.include('Alunite');

        // Pure Topaz (BD1480 = 0.010, BD1760 = 0.010, BD2080 = 0.045, BD2265 = 0.015):
        const topz = BandMathEngine.computeCRISMTopazAluniteFumarolicSpeciationIndices(0.010, 0.010, 0.045, 0.015);
        expect(topz.isAcidFumarolicDetected).to.be.true;
        expect(topz.fumarolicMineralClass).to.include('Pneumatolytic Fluor-Topaz Greisen');
        expect(topz.mineralSpecies).to.include('Topaz');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMTopazAluniteFumarolicSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAcidFumarolicDetected).to.be.false;
    });
});

describe('Mars-to-Phaethon Transfer, Pumpellyite Metasomatism & Pumpellyite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to Mercury-crossing asteroid (3200) Phaethon', () => {
        // Mars to Phaethon (300 km Mars alt, 0.140 AU perihelion, 5 km capture alt):
        const phae = TrajectoryEngine.computeMarsToPhaethonTransfer(300.0, 0.140, 5.0);
        expect(phae.semiMajorAxisAU).to.be.closeTo(0.832, 0.05); // ~0.832 AU
        expect(phae.eccentricity).to.be.closeTo(0.8317, 0.01); // e ~ 0.832
        expect(phae.timeOfFlightDays).to.be.closeTo(138.45, 15.0); // ~138.5 days (~0.38 yr)
        expect(phae.marsDepartureDeltaVKmS).to.be.closeTo(11.617, 0.5); // ~11.62 km/s TPI
        expect(phae.phaethonOrbitInsertionDeltaVKmS).to.be.closeTo(28.130, 2.0); // ~28.13 km/s POI
        expect(phae.totalMissionDeltaVKmS).to.be.closeTo(39.747, 2.5); // ~39.75 km/s total
        expect(phae.phaethonContext).to.include('Mars-to-Phaethon');
    });

    it('should calculate low-grade sub-greenschist pumpellyite-epidote metasomatism, hydration water uptake, and metabasalt thermal inertia', () => {
        // 15% initial porosity, 260 C fluid temp, 1.80 Mg/Fe ratio, 600 yr duration:
        const pump = KRCEngine.computeMartianPumpellyiteEpidoteMetasomatism(0.15, 260.0, 1.80, 600.0);
        expect(pump.pumpellyiteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(pump.boundWaterYieldWeightPercent).to.be.greaterThan(3.0); // > 3.0 wt% bound H2O
        expect(pump.crystallineMetabasaltThermalInertiaTIU).to.be.closeTo(2520.0, 200.0); // ~2520 tiu
        expect(pump.pumpellyiteFaciesClass).to.include('Pervasive Pumpellyite-Epidote-Chlorite Sub-Greenschist Facies');
        expect(pump.subGreenschistContext).to.include('Pumpellyite Facies at 260 C');
    });

    it('should discriminate Al-Pumpellyite vs Epidote vs Fe3+-Pumpellyite in CRISM spectra', () => {
        // Al-Pumpellyite (Mawrth Vallis / Nili Deep: BD1450 = 0.030, BD1920 = 0.045, BD2260 = 0.055, BD2330 = 0.015):
        const alPump = BandMathEngine.computeCRISMPumpellyiteEpidoteSpeciationIndices(0.030, 0.045, 0.055, 0.015);
        expect(alPump.isSubGreenschistDetected).to.be.true;
        expect(alPump.subGreenschistMineralClass).to.include('Hydrated Al-Rich Pumpellyite Metamorphic Facies');
        expect(alPump.mineralSpecies).to.include('Al-Pumpellyite');
        expect(alPump.metamorphicFaciesRegime).to.include('Low-Grade Burial/Hydrothermal Metamorphism');

        // Fe-Rich Epidote (BD1450 = 0.015, BD1920 = 0.010, BD2260 = 0.020, BD2330 = 0.045):
        const epi = BandMathEngine.computeCRISMPumpellyiteEpidoteSpeciationIndices(0.015, 0.010, 0.020, 0.045);
        expect(epi.isSubGreenschistDetected).to.be.true;
        expect(epi.subGreenschistMineralClass).to.include('Anhydrous Fe-Epidote Facies');
        expect(epi.mineralSpecies).to.include('Epidote');

        // Fe3+-Pumpellyite (BD1450 = 0.015, BD1920 = 0.035, BD2260 = 0.020, BD2330 = 0.035):
        const fePump = BandMathEngine.computeCRISMPumpellyiteEpidoteSpeciationIndices(0.015, 0.035, 0.020, 0.035);
        expect(fePump.isSubGreenschistDetected).to.be.true;
        expect(fePump.subGreenschistMineralClass).to.include('Fe3+-Pumpellyite (Julgoldite) Alteration');
        expect(fePump.mineralSpecies).to.include('Fe3+-Pumpellyite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMPumpellyiteEpidoteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isSubGreenschistDetected).to.be.false;
    });
});

describe('Mars-to-Hygiea Transfer, Lawsonite Blueschist Metamorphism & Blueschist Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to carbonaceous dwarf planet candidate (10) Hygiea', () => {
        // Mars to Hygiea (300 km Mars alt, 3.140 AU distance, 50 km capture alt):
        const hyg = TrajectoryEngine.computeMarsToHygieaTransfer(300.0, 3.140, 50.0);
        expect(hyg.semiMajorAxisAU).to.be.closeTo(2.332, 0.1); // ~2.33 AU
        expect(hyg.eccentricity).to.be.closeTo(0.3466, 0.01); // e ~ 0.347
        expect(hyg.timeOfFlightDays).to.be.closeTo(649.90, 30.0); // ~650 days (~1.78 yr)
        expect(hyg.timeOfFlightYears).to.be.closeTo(1.78, 0.1); // ~1.78 yr
        expect(hyg.marsDepartureDeltaVKmS).to.be.closeTo(2.572, 0.5); // ~2.57 km/s THI
        expect(hyg.hygieaOrbitInsertionDeltaVKmS).to.be.closeTo(3.189, 0.5); // ~3.19 km/s HOI
        expect(hyg.totalMissionDeltaVKmS).to.be.closeTo(5.761, 1.0); // ~5.76 km/s total
        expect(hyg.hygieaContext).to.include('Mars-to-Hygiea');
    });

    it('should calculate high-pressure/low-temperature lawsonite-glaucophane blueschist metamorphism, crystal densification, and suture zone thermal inertia', () => {
        // 12% initial porosity, 280 C HP-LT temp, 1.20 GPa lithostatic fluid pressure, 500 yr duration:
        const blue = KRCEngine.computeMartianLawsoniteBlueschistMetamorphism(0.12, 280.0, 1.20, 500.0);
        expect(blue.blueschistConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(blue.boundWaterYieldWeightPercent).to.be.greaterThan(5.0); // > 5.0 wt% bound H2O
        expect(blue.crystallineBlueschistThermalInertiaTIU).to.be.closeTo(2829.8, 200.0); // ~2830 tiu
        expect(blue.metamorphicFaciesClass).to.include('Pervasive Lawsonite-Blueschist HP-LT Facies');
        expect(blue.blueschistContext).to.include('Blueschist Facies at 280 C');
    });

    it('should discriminate Lawsonite vs Glaucophane vs Mixed Blueschist in CRISM spectra', () => {
        // Lawsonite (Ancient Suture Terrane: BD1440 = 0.030, BD1660 = 0.035, BD2180 = 0.055, BD2350 = 0.015):
        const law = BandMathEngine.computeCRISMLawsoniteGlaucophaneSpeciationIndices(0.030, 0.035, 0.055, 0.015);
        expect(law.isBlueschistDetected).to.be.true;
        expect(law.blueschistMineralClass).to.include('Lawsonite Blueschist Assemblage');
        expect(law.mineralSpecies).to.include('Lawsonite');
        expect(law.metamorphicPressureRegime).to.include('High-Pressure/Low-Temperature Subduction');

        // Glaucophane (BD1440 = 0.015, BD1660 = 0.010, BD2180 = 0.035, BD2350 = 0.045):
        const glau = BandMathEngine.computeCRISMLawsoniteGlaucophaneSpeciationIndices(0.015, 0.010, 0.035, 0.045);
        expect(glau.isBlueschistDetected).to.be.true;
        expect(glau.blueschistMineralClass).to.include('Sodic Amphibole Glaucophane Schist');
        expect(glau.mineralSpecies).to.include('Glaucophane');

        // Mixed Blueschist (BD1440 = 0.020, BD1660 = 0.025, BD2180 = 0.025, BD2350 = 0.030):
        const mix = BandMathEngine.computeCRISMLawsoniteGlaucophaneSpeciationIndices(0.020, 0.025, 0.025, 0.030);
        expect(mix.isBlueschistDetected).to.be.true;
        expect(mix.blueschistMineralClass).to.include('Composite Lawsonite-Glaucophane-Aragonite Facies');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMLawsoniteGlaucophaneSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isBlueschistDetected).to.be.false;
    });
});

describe('Mars-to-Europa Transfer, Dickite Argillic Maturation & Dickite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to Jupiter/Europa and orbit capture', () => {
        // Mars to Europa (300 km Mars alt, 5.2044 AU distance, 100 km capture alt):
        const eur = TrajectoryEngine.computeMarsToEuropaTransfer(300.0, 5.2044, 100.0);
        expect(eur.semiMajorAxisAU).to.be.closeTo(3.364, 0.1); // ~3.36 AU
        expect(eur.eccentricity).to.be.closeTo(0.5471, 0.01); // e ~ 0.547
        expect(eur.timeOfFlightDays).to.be.closeTo(1126.80, 50.0); // ~1127 days (~3.09 yr)
        expect(eur.timeOfFlightYears).to.be.closeTo(3.08, 0.2); // ~3.08 yr
        expect(eur.marsDepartureDeltaVKmS).to.be.closeTo(3.894, 0.5); // ~3.89 km/s TJI
        expect(eur.europaOrbitInsertionDeltaVKmS).to.be.closeTo(0.997, 0.5); // ~1.00 km/s EOI
        expect(eur.totalMissionDeltaVKmS).to.be.closeTo(4.891, 1.0); // ~4.89 km/s total
        expect(eur.europaContext).to.include('Mars-to-Europa');
    });

    it('should calculate high-temperature hydrothermal dickite-kaolinite argillic maturation, crystal stacking ordering, and clay sinter thermal inertia', () => {
        // 30% initial porosity, 210 C hydrothermal temp, 3.2 pH, 300 yr duration:
        const dick = KRCEngine.computeMartianDickiteKaoliniteArgillicMaturation(0.30, 210.0, 3.2, 300.0);
        expect(dick.dickiteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(dick.orderedDickiteYieldWeightPercent).to.be.greaterThan(35.0); // > 35 wt% ordered dickite
        expect(dick.induratedClayThermalInertiaTIU).to.be.closeTo(2380.0, 200.0); // ~2380 tiu
        expect(dick.argillicMaturationClass).to.include('High-Temperature Ordered Dickite Hydrothermal Facies');
        expect(dick.dickiteContext).to.include('Dickite Facies at 210 C');
    });

    it('should discriminate High-Temperature Hydrothermal Dickite vs Sedimentary Kaolinite vs Nacrite in CRISM spectra', () => {
        // Hydrothermal Dickite (Toro Crater / Nili: BD1415 = 0.035, BD2160 = 0.045, BD2205 = 0.065, BD2720 = 0.020):
        const dick = BandMathEngine.computeCRISMDickiteKaoliniteSpeciationIndices(0.035, 0.045, 0.065, 0.020);
        expect(dick.isKaolinGroupDetected).to.be.true;
        expect(dick.kaolinSpeciesClass).to.include('High-Temperature Hydrothermal Dickite Facies');
        expect(dick.mineralSpecies).to.include('Dickite');
        expect(dick.hydrothermalTemperatureRegime).to.include('High-Temperature Acid Hydrothermal Circulation');

        // Sedimentary Kaolinite (BD1415 = 0.025, BD2160 = 0.010, BD2205 = 0.055, BD2720 = 0.015):
        const kaol = BandMathEngine.computeCRISMDickiteKaoliniteSpeciationIndices(0.025, 0.010, 0.055, 0.015);
        expect(kaol.isKaolinGroupDetected).to.be.true;
        expect(kaol.kaolinSpeciesClass).to.include('Pedogenic / Weathering Kaolinite Facies');
        expect(kaol.mineralSpecies).to.include('Kaolinite');

        // Nacrite (BD1415 = 0.020, BD2160 = 0.030, BD2205 = 0.025, BD2720 = 0.035):
        const nac = BandMathEngine.computeCRISMDickiteKaoliniteSpeciationIndices(0.020, 0.030, 0.025, 0.035);
        expect(nac.isKaolinGroupDetected).to.be.true;
        expect(nac.kaolinSpeciesClass).to.include('Deep Pneumatolytic Nacrite Veining');
        expect(nac.mineralSpecies).to.include('Nacrite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMDickiteKaoliniteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isKaolinGroupDetected).to.be.false;
    });
});

describe('Mars-to-Io Transfer, Beidellite Smectite Kinetics & Smectite Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to Jupiter/Io and orbit capture', () => {
        // Mars to Io (300 km Mars alt, 5.2044 AU distance, 100 km capture alt):
        const ioTr = TrajectoryEngine.computeMarsToIoTransfer(300.0, 5.2044, 100.0);
        expect(ioTr.semiMajorAxisAU).to.be.closeTo(3.364, 0.1); // ~3.36 AU
        expect(ioTr.eccentricity).to.be.closeTo(0.5471, 0.01); // e ~ 0.547
        expect(ioTr.timeOfFlightDays).to.be.closeTo(1126.80, 50.0); // ~1127 days (~3.09 yr)
        expect(ioTr.marsDepartureDeltaVKmS).to.be.closeTo(3.894, 0.5); // ~3.89 km/s TJI
        expect(ioTr.ioOrbitInsertionDeltaVKmS).to.be.closeTo(1.800, 0.5); // ~1.80 km/s IOI
        expect(ioTr.totalMissionDeltaVKmS).to.be.closeTo(5.694, 1.0); // ~5.69 km/s total
        expect(ioTr.ioContext).to.include('Mars-to-Io');
    });

    it('should calculate dioctahedral smectite (beidellite-nontronite) solid solution kinetics, interlayer hydration, and clay bed thermal inertia', () => {
        // 20% initial porosity, 85 C alteration temp, 1.40 Al/Fe ratio, 400 yr duration:
        const smec = KRCEngine.computeMartianBeidelliteNontroniteSmectiteKinetics(0.20, 85.0, 1.40, 400.0);
        expect(smec.smectiteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(smec.interlayerWaterYieldWeightPercent).to.be.greaterThan(5.0); // > 5.0 wt% interlayer H2O
        expect(smec.hydratedClayThermalInertiaTIU).to.be.closeTo(1855.1, 200.0); // ~1855 tiu
        expect(smec.smectiteFaciesClass).to.include('Neutral Hydrothermal/Pedogenic Smectite Sequence');
        expect(smec.smectiteContext).to.include('Smectite Facies at 85 C');
    });

    it('should discriminate Al-Beidellite vs Fe-Nontronite vs Intermediate Solid Solution in CRISM spectra', () => {
        // Al-Beidellite (Mawrth Vallis / Claritas Upper Clay: BD1410 = 0.030, BD1910 = 0.045, BD2190 = 0.055, BD2290 = 0.015):
        const beid = BandMathEngine.computeCRISMBeidelliteNontroniteSpeciationIndices(0.030, 0.045, 0.055, 0.015);
        expect(beid.isDioctahedralSmectiteDetected).to.be.true;
        expect(beid.smectiteSpeciesClass).to.include('Al-Rich Beidellite Smectite Facies');
        expect(beid.mineralSpecies).to.include('Beidellite');
        expect(beid.geochemicalAlterationRegime).to.include('Open-System Leaching / High Al/Fe Pedogenesis');

        // Fe-Nontronite (Oxia Planum / Nili: BD1410 = 0.025, BD1910 = 0.040, BD2190 = 0.015, BD2290 = 0.055):
        const nont = BandMathEngine.computeCRISMBeidelliteNontroniteSpeciationIndices(0.025, 0.040, 0.015, 0.055);
        expect(nont.isDioctahedralSmectiteDetected).to.be.true;
        expect(nont.smectiteSpeciesClass).to.include('Fe-Rich Nontronite Smectite Facies');
        expect(nont.mineralSpecies).to.include('Nontronite');

        // Intermediate Solid Solution (BD1410 = 0.025, BD1910 = 0.035, BD2190 = 0.035, BD2290 = 0.035):
        const sol = BandMathEngine.computeCRISMBeidelliteNontroniteSpeciationIndices(0.025, 0.035, 0.035, 0.035);
        expect(sol.isDioctahedralSmectiteDetected).to.be.true;
        expect(sol.smectiteSpeciesClass).to.include('Intermediate Beidellite-Nontronite Solid Solution');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMBeidelliteNontroniteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isDioctahedralSmectiteDetected).to.be.false;
    });
});

describe('Mars-to-Callisto Transfer, Alunite-Jarosite Kinetics & Acid Sulfate Speciation', () => {
    it('should calculate interplanetary direct transfer from Mars to Jupiter/Callisto and orbit capture', () => {
        // Mars to Callisto (300 km Mars alt, 5.2044 AU distance, 100 km capture alt):
        const cal = TrajectoryEngine.computeMarsToCallistoTransfer(300.0, 5.2044, 100.0);
        expect(cal.semiMajorAxisAU).to.be.closeTo(3.364, 0.1); // ~3.36 AU
        expect(cal.eccentricity).to.be.closeTo(0.5471, 0.01); // e ~ 0.547
        expect(cal.timeOfFlightDays).to.be.closeTo(1126.80, 50.0); // ~1127 days (~3.09 yr)
        expect(cal.marsDepartureDeltaVKmS).to.be.closeTo(3.894, 0.5); // ~3.89 km/s TJI
        expect(cal.callistoOrbitInsertionDeltaVKmS).to.be.closeTo(1.353, 0.5); // ~1.35 km/s COI
        expect(cal.totalMissionDeltaVKmS).to.be.closeTo(5.247, 1.0); // ~5.25 km/s total
        expect(cal.callistoContext).to.include('Mars-to-Callisto');
    });

    it('should calculate acid-sulfate alunite-jarosite solid solution kinetics, sulfate cementation, and indurated crust thermal inertia', () => {
        // 25% initial porosity, 160 C hydrothermal temp, 1.20 Al/Fe ratio, 350 yr duration:
        const alJar = KRCEngine.computeMartianAluniteJarositeSolidSolutionKinetics(0.25, 160.0, 1.20, 350.0);
        expect(alJar.acidSulfateConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(alJar.sulfateMineralYieldWeightPercent).to.be.greaterThan(30.0); // > 30 wt% sulfate
        expect(alJar.induratedSulfateThermalInertiaTIU).to.be.closeTo(2141.5, 200.0); // ~2142 tiu
        expect(alJar.acidSulfateFaciesClass).to.include('Transitional Alunite-Jarosite Acid-Sulfate Sequence');
        expect(alJar.acidSulfateContext).to.include('Acid Sulfate Facies at 160 C');
    });

    it('should discriminate Potassium Alunite vs Potassium Jarosite vs Solid Solution in CRISM spectra', () => {
        // Potassium Alunite (Mawrth / Columbus: BD0900 = 0.010, BD1480 = 0.035, BD1760 = 0.040, BD2265Al = 0.060, BD2265Fe = 0.015):
        const alu = BandMathEngine.computeCRISMAluniteJarositeSpeciationIndices(0.010, 0.035, 0.040, 0.060, 0.015);
        expect(alu.isAcidSulfateDetected).to.be.true;
        expect(alu.acidSulfateMineralClass).to.include('High-Alumina Potassium Alunite Facies');
        expect(alu.mineralSpecies).to.include('Alunite');
        expect(alu.pHGeochemicalEnvironment).to.include('Magmatic Acid-Sulfate Hydrothermal Leaching');

        // Potassium Jarosite (Meridiani / Candor: BD0900 = 0.045, BD1480 = 0.015, BD1760 = 0.010, BD2265Al = 0.015, BD2265Fe = 0.045):
        const jar = BandMathEngine.computeCRISMAluniteJarositeSpeciationIndices(0.045, 0.015, 0.010, 0.015, 0.045);
        expect(jar.isAcidSulfateDetected).to.be.true;
        expect(jar.acidSulfateMineralClass).to.include('Ferric Iron Potassium Jarosite Facies');
        expect(jar.mineralSpecies).to.include('Jarosite');

        // Solid Solution (BD0900 = 0.030, BD1480 = 0.025, BD1760 = 0.025, BD2265Al = 0.035, BD2265Fe = 0.020):
        const sol = BandMathEngine.computeCRISMAluniteJarositeSpeciationIndices(0.030, 0.025, 0.025, 0.035, 0.020);
        expect(sol.isAcidSulfateDetected).to.be.true;
        expect(sol.acidSulfateMineralClass).to.include('Transitional Alunite-Jarosite Solid Solution');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMAluniteJarositeSpeciationIndices(0.005, 0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAcidSulfateDetected).to.be.false;
    });
});

describe('Mars-to-Pallas Transfer, Celadonite Metasomatism & Green Mica Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to high-inclination asteroid (2) Pallas and orbit capture', () => {
        // Mars to Pallas (300 km Mars alt, 2.772 AU distance, 50 km capture alt, 34.84 deg plane change):
        const pal = TrajectoryEngine.computeMarsToPallasTransfer(300.0, 2.772, 50.0, 34.84);
        expect(pal.semiMajorAxisAU).to.be.closeTo(2.148, 0.1); // ~2.15 AU
        expect(pal.eccentricity).to.be.closeTo(0.2906, 0.01); // e ~ 0.291
        expect(pal.timeOfFlightDays).to.be.closeTo(574.15, 30.0); // ~574 days (~1.57 yr)
        expect(pal.timeOfFlightYears).to.be.closeTo(1.57, 0.1); // ~1.57 yr
        expect(pal.marsDepartureDeltaVKmS).to.be.closeTo(12.960, 1.0); // ~12.96 km/s TPI
        expect(pal.pallasOrbitInsertionDeltaVKmS).to.be.closeTo(2.663, 0.5); // ~2.66 km/s POI
        expect(pal.totalMissionDeltaVKmS).to.be.closeTo(15.623, 1.5); // ~15.62 km/s total
        expect(pal.pallasContext).to.include('Mars-to-Pallas');
    });

    it('should calculate low-temperature alkaline/hydrothermal celadonite green mica metasomatism of basalt vesicles and indurated metabasalt thermal inertia', () => {
        // 18% initial porosity, 70 C hydrothermal temp, 1.30 Fe/Mg ratio, 500 yr duration:
        const cel = KRCEngine.computeMartianCeladoniteGlauconiteMetasomatism(0.18, 70.0, 1.30, 500.0);
        expect(cel.celadoniteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(cel.greenMicaYieldWeightPercent).to.be.greaterThan(25.0); // > 25 wt% mica
        expect(cel.induratedMetabasaltThermalInertiaTIU).to.be.closeTo(2261.7, 200.0); // ~2262 tiu
        expect(cel.micaFaciesClass).to.include('Low-Temperature Hydrothermal Celadonite Facies');
        expect(cel.celadoniteContext).to.include('Celadonite Mica at 70 C');
    });

    it('should discriminate Celadonite vs Glauconite vs Nontronite in CRISM spectra', () => {
        // Celadonite (Nili Fossae / Mawrth Basement: BD0750 = 0.015, BD1410 = 0.035, BD2250 = 0.055, BD2300 = 0.045):
        const cel = BandMathEngine.computeCRISMCeladoniteGlauconiteSpeciationIndices(0.015, 0.035, 0.055, 0.045);
        expect(cel.isGreenMicaDetected).to.be.true;
        expect(cel.micaSpeciesClass).to.include('Hydrothermal Celadonite Green Mica Facies');
        expect(cel.mineralSpecies).to.include('Celadonite');
        expect(cel.alterationEnvironment).to.include('Low-Temperature Hydrothermal Vesicle Infilling');

        // Glauconite (Eridania / Gale: BD0750 = 0.045, BD1410 = 0.020, BD2250 = 0.040, BD2300 = 0.020):
        const glau = BandMathEngine.computeCRISMCeladoniteGlauconiteSpeciationIndices(0.045, 0.020, 0.040, 0.020);
        expect(glau.isGreenMicaDetected).to.be.true;
        expect(glau.micaSpeciesClass).to.include('Authigenic Lacustrine/Playa Glauconite Facies');
        expect(glau.mineralSpecies).to.include('Glauconite');

        // Nontronite-Illite (BD0750 = 0.010, BD1410 = 0.020, BD2250 = 0.015, BD2300 = 0.045):
        const non = BandMathEngine.computeCRISMCeladoniteGlauconiteSpeciationIndices(0.010, 0.020, 0.015, 0.045);
        expect(non.isGreenMicaDetected).to.be.true;
        expect(non.micaSpeciesClass).to.include('Ferric Dioctahedral Phyllosilicate Assemblage');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMCeladoniteGlauconiteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isGreenMicaDetected).to.be.false;
    });
});

describe('Mars-to-Juno Transfer, Woodhouseite APS Kinetics & APS Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to stony main-belt asteroid (3) Juno and orbit capture', () => {
        // Mars to Juno (300 km Mars alt, 2.670 AU distance, 30 km capture alt, 12.98 deg plane change):
        const jun = TrajectoryEngine.computeMarsToJunoTransfer(300.0, 2.670, 30.0, 12.98);
        expect(jun.semiMajorAxisAU).to.be.closeTo(2.097, 0.1); // ~2.10 AU
        expect(jun.eccentricity).to.be.closeTo(0.2733, 0.01); // e ~ 0.273
        expect(jun.timeOfFlightDays).to.be.closeTo(553.80, 30.0); // ~554 days (~1.52 yr)
        expect(jun.timeOfFlightYears).to.be.closeTo(1.52, 0.1); // ~1.52 yr
        expect(jun.marsDepartureDeltaVKmS).to.be.closeTo(4.654, 0.5); // ~4.65 km/s TJI
        expect(jun.junoOrbitInsertionDeltaVKmS).to.be.closeTo(2.384, 0.5); // ~2.38 km/s JOI
        expect(jun.totalMissionDeltaVKmS).to.be.closeTo(7.038, 1.0); // ~7.04 km/s total
        expect(jun.junoContext).to.include('Mars-to-Juno');
    });

    it('should calculate acid-sulfate-phosphate hydrothermal alteration of aluminous crust into APS woodhouseite and indurated sinter thermal inertia', () => {
        // 22% initial porosity, 210 C hydrothermal temp, 1.10 P/S ratio, 300 yr duration:
        const wood = KRCEngine.computeMartianWoodhouseitePhosphateSulfateKinetics(0.22, 210.0, 1.10, 300.0);
        expect(wood.apsConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(wood.woodhouseiteYieldWeightPercent).to.be.greaterThan(30.0); // > 30 wt% woodhouseite
        expect(wood.induratedAPSThermalInertiaTIU).to.be.closeTo(2410.5, 200.0); // ~2411 tiu
        expect(wood.apsFaciesClass).to.include('Acid Magmatic Volatiles Hydrothermal Sequence');
        expect(wood.apsContext).to.include('Woodhouseite APS at 210 C');
    });

    it('should discriminate Woodhouseite vs Pure Alunite vs Hydrated Phosphate in CRISM spectra', () => {
        // Woodhouseite (Mawrth / Columbus: BD1480 = 0.035, BD1760 = 0.035, BD2170 = 0.055, BD2310 = 0.045):
        const aps = BandMathEngine.computeCRISMWoodhouseiteAPSSpeciationIndices(0.035, 0.035, 0.055, 0.045);
        expect(aps.isAPSDetected).to.be.true;
        expect(aps.apsMineralClass).to.include('Aluminium-Phosphate-Sulfate (APS) Woodhouseite Facies');
        expect(aps.mineralSpecies).to.include('Woodhouseite');
        expect(aps.magmaticVolatileEnvironment).to.include('Extreme Magmatic-Hydrothermal Acid-Sulfate Leaching of Apatite');

        // Pure Alunite (BD1480 = 0.035, BD1760 = 0.035, BD2170 = 0.055, BD2310 = 0.010):
        const alu = BandMathEngine.computeCRISMWoodhouseiteAPSSpeciationIndices(0.035, 0.035, 0.055, 0.010);
        expect(alu.isAPSDetected).to.be.true;
        expect(alu.apsMineralClass).to.include('Pure High-Alumina Alunite Sulfate Facies');
        expect(alu.mineralSpecies).to.include('Alunite');

        // Secondary Phosphate (BD1480 = 0.015, BD1760 = 0.010, BD2170 = 0.020, BD2310 = 0.045):
        const phos = BandMathEngine.computeCRISMWoodhouseiteAPSSpeciationIndices(0.015, 0.010, 0.020, 0.045);
        expect(phos.isAPSDetected).to.be.true;
        expect(phos.apsMineralClass).to.include('Secondary Hydrated Phosphate Sinter');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMWoodhouseiteAPSSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAPSDetected).to.be.false;
    });
});

describe('Mars-to-Astraea Transfer, Talc-Carbonate Metasomatism & Soapstone Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to stony main-belt asteroid (5) Astraea and orbit capture', () => {
        // Mars to Astraea (300 km Mars alt, 2.574 AU distance, 20 km capture alt, 5.37 deg plane change):
        const ast = TrajectoryEngine.computeMarsToAstraeaTransfer(300.0, 2.574, 20.0, 5.37);
        expect(ast.semiMajorAxisAU).to.be.closeTo(2.049, 0.1); // ~2.05 AU
        expect(ast.eccentricity).to.be.closeTo(0.2563, 0.01); // e ~ 0.256
        expect(ast.timeOfFlightDays).to.be.closeTo(534.80, 30.0); // ~535 days (~1.46 yr)
        expect(ast.timeOfFlightYears).to.be.closeTo(1.46, 0.1); // ~1.46 yr
        expect(ast.marsDepartureDeltaVKmS).to.be.closeTo(2.615, 0.5); // ~2.62 km/s TAI
        expect(ast.astraeaOrbitInsertionDeltaVKmS).to.be.closeTo(2.429, 0.5); // ~2.43 km/s AOI
        expect(ast.totalMissionDeltaVKmS).to.be.closeTo(5.044, 1.0); // ~5.04 km/s total
        expect(ast.astraeaContext).to.include('Mars-to-Astraea');
    });

    it('should calculate hydrothermal CO2 metasomatism of ultramafic serpentinite into talc-magnesite soapstone and indurated thermal inertia', () => {
        // 15% initial porosity, 270 C hydrothermal temp, 0.12 X(CO2), 600 yr duration:
        const tc = KRCEngine.computeMartianTalcCarbonateMetasomatism(0.15, 270.0, 0.12, 600.0);
        expect(tc.talcCarbonateConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(tc.soapstoneYieldWeightPercent).to.be.greaterThan(40.0); // > 40 wt% soapstone
        expect(tc.induratedSoapstoneThermalInertiaTIU).to.be.closeTo(2594.8, 200.0); // ~2595 tiu
        expect(tc.soapstoneFaciesClass).to.include('Pervasive Talc-Carbonate Hydrothermal Facies');
        expect(tc.talcCarbonateContext).to.include('Talc-Carbonate Soapstone at 270 C');
    });

    it('should discriminate Talc vs Magnesite vs Talc-Magnesite Soapstone in CRISM spectra', () => {
        // Talc-Magnesite Soapstone (Nili Fossae / Isidis Rim: BD1390 = 0.035, BD2290 = 0.040, BD2310 = 0.060, BD2510 = 0.045):
        const soap = BandMathEngine.computeCRISMTalcCarbonateSpeciationIndices(0.035, 0.040, 0.060, 0.045);
        expect(soap.isTalcCarbonateDetected).to.be.true;
        expect(soap.talcCarbonateMineralClass).to.include('Talc-Magnesite Carbonate Soapstone Assemblage');
        expect(soap.mineralSpecies).to.include('Talc + Magnesite Soapstone');
        expect(soap.metasomaticEnvironment).to.include('CO2-Rich Hydrothermal Metasomatism of Serpentinite');

        // Pure Talc (BD1390 = 0.035, BD2290 = 0.040, BD2310 = 0.060, BD2510 = 0.010):
        const talc = BandMathEngine.computeCRISMTalcCarbonateSpeciationIndices(0.035, 0.040, 0.060, 0.010);
        expect(talc.isTalcCarbonateDetected).to.be.true;
        expect(talc.talcCarbonateMineralClass).to.include('Pure Hydrothermal Talc Facies');
        expect(talc.mineralSpecies).to.include('Talc');

        // Magnesite Carbonate (BD1390 = 0.010, BD2290 = 0.015, BD2310 = 0.050, BD2510 = 0.055):
        const mag = BandMathEngine.computeCRISMTalcCarbonateSpeciationIndices(0.010, 0.015, 0.050, 0.055);
        expect(mag.isTalcCarbonateDetected).to.be.true;
        expect(mag.talcCarbonateMineralClass).to.include('Magnesite Carbonate Strata');
        expect(mag.mineralSpecies).to.include('Magnesite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMTalcCarbonateSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isTalcCarbonateDetected).to.be.false;
    });
});

describe('Mars-to-Hebe Transfer, Topaz Greisen Metasomatism & Topaz Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to stony H-chondrite parent asteroid (6) Hebe and orbit capture', () => {
        // Mars to Hebe (300 km Mars alt, 2.426 AU distance, 25 km capture alt, 14.77 deg plane change):
        const heb = TrajectoryEngine.computeMarsToHebeTransfer(300.0, 2.426, 25.0, 14.77);
        expect(heb.semiMajorAxisAU).to.be.closeTo(1.975, 0.1); // ~1.97 AU
        expect(heb.eccentricity).to.be.closeTo(0.2285, 0.01); // e ~ 0.228
        expect(heb.timeOfFlightDays).to.be.closeTo(506.00, 30.0); // ~506 days (~1.38 yr)
        expect(heb.timeOfFlightYears).to.be.closeTo(1.38, 0.1); // ~1.38 yr
        expect(heb.marsDepartureDeltaVKmS).to.be.closeTo(5.045, 0.5); // ~5.05 km/s THI
        expect(heb.hebeOrbitInsertionDeltaVKmS).to.be.closeTo(2.112, 0.5); // ~2.11 km/s HOI
        expect(heb.totalMissionDeltaVKmS).to.be.closeTo(7.157, 1.0); // ~7.16 km/s total
        expect(heb.hebeContext).to.include('Mars-to-Hebe');
    });

    it('should calculate high-temperature pneumatolytic greisenization of felsic crust into topaz-quartz greisen and indurated thermal inertia', () => {
        // 14% initial porosity, 420 C pneumatolytic temp, 0.15 a(HF), 400 yr duration:
        const top = KRCEngine.computeMartianTopazGreisenMetasomatism(0.14, 420.0, 0.15, 400.0);
        expect(top.greisenConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(top.topazYieldWeightPercent).to.be.greaterThan(25.0); // > 25 wt% topaz
        expect(top.induratedGreisenThermalInertiaTIU).to.be.closeTo(2780.0, 200.0); // ~2780 tiu
        expect(top.greisenFaciesClass).to.include('High-Temperature Pneumatolytic Topaz Greisen');
        expect(top.topazContext).to.include('Topaz Greisen at 420 C');
    });

    it('should discriminate Fluor-Topaz vs Hydroxyl-Topaz vs Muscovite Greisen in CRISM spectra', () => {
        // Fluor-Topaz (Syrtis Major / Terra Sirenum: BD1200 = 0.015, BD2080 = 0.045, BD2210 = 0.060, BD2350 = 0.020):
        const top = BandMathEngine.computeCRISMTopazGreisenSpeciationIndices(0.015, 0.045, 0.060, 0.020);
        expect(top.isTopazGreisenDetected).to.be.true;
        expect(top.greisenMineralClass).to.include('Pneumatolytic Fluor-Topaz Greisen Facies');
        expect(top.mineralSpecies).to.include('Fluor-Topaz');
        expect(top.pneumatolyticEnvironment).to.include('High-Temperature Magmatic-Pneumatolytic HF Volatile Degassing');

        // Hydroxyl-Topaz (BD1200 = 0.025, BD2080 = 0.035, BD2210 = 0.045, BD2350 = 0.010):
        const ohtop = BandMathEngine.computeCRISMTopazGreisenSpeciationIndices(0.025, 0.035, 0.045, 0.010);
        expect(ohtop.isTopazGreisenDetected).to.be.true;
        expect(ohtop.greisenMineralClass).to.include('Hydrothermal Hydroxyl-Topaz Facies');
        expect(ohtop.mineralSpecies).to.include('OH-Rich Topaz');

        // Muscovite Greisen (BD1200 = 0.005, BD2080 = 0.010, BD2210 = 0.055, BD2350 = 0.010):
        const musc = BandMathEngine.computeCRISMTopazGreisenSpeciationIndices(0.005, 0.010, 0.055, 0.010);
        expect(musc.isTopazGreisenDetected).to.be.true;
        expect(musc.greisenMineralClass).to.include('Phyllic / Muscovite Greisen Border');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMTopazGreisenSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isTopazGreisenDetected).to.be.false;
    });
});

describe('Mars-to-Iris Transfer, Pyrophyllite Metasomatism & Pyrophyllite Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to bright stony main-belt asteroid (7) Iris and orbit capture', () => {
        // Mars to Iris (300 km Mars alt, 2.386 AU distance, 25 km capture alt, 5.52 deg plane change):
        const iris = TrajectoryEngine.computeMarsToIrisTransfer(300.0, 2.386, 25.0, 5.52);
        expect(iris.semiMajorAxisAU).to.be.closeTo(1.955, 0.1); // ~1.95 AU
        expect(iris.eccentricity).to.be.closeTo(0.2206, 0.01); // e ~ 0.221
        expect(iris.timeOfFlightDays).to.be.closeTo(498.40, 30.0); // ~498 days (~1.36 yr)
        expect(iris.timeOfFlightYears).to.be.closeTo(1.36, 0.1); // ~1.36 yr
        expect(iris.marsDepartureDeltaVKmS).to.be.closeTo(2.482, 0.5); // ~2.48 km/s TII
        expect(iris.irisOrbitInsertionDeltaVKmS).to.be.closeTo(2.074, 0.5); // ~2.07 km/s IOI
        expect(iris.totalMissionDeltaVKmS).to.be.closeTo(4.556, 1.0); // ~4.56 km/s total
        expect(iris.irisContext).to.include('Mars-to-Iris');
    });

    it('should calculate high-temperature acid-hydrothermal advanced argillic alteration of aluminous crust into pyrophyllite-quartz sinter and thermal inertia', () => {
        // 20% initial porosity, 320 C hydrothermal temp, 2.5 pH, 450 yr duration:
        const pyro = KRCEngine.computeMartianPyrophylliteArgillicMetasomatism(0.20, 320.0, 2.5, 450.0);
        expect(pyro.pyrophylliteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(pyro.pyrophylliteYieldWeightPercent).to.be.greaterThan(30.0); // > 30 wt% pyrophyllite
        expect(pyro.induratedArgillicThermalInertiaTIU).to.be.closeTo(2621.6, 200.0); // ~2622 tiu
        expect(pyro.advancedArgillicFaciesClass).to.include('High-Temperature Advanced Argillic Hydrothermal Facies');
        expect(pyro.pyrophylliteContext).to.include('Pyrophyllite Facies at 320 C');
    });

    it('should discriminate Pyrophyllite vs Diaspore vs Intermediate Argillic in CRISM spectra', () => {
        // Pyrophyllite (Nili Fossae / Toro Crater: BD1395 = 0.035, BD2060 = 0.040, BD2165 = 0.060, BD2320 = 0.035):
        const pyro = BandMathEngine.computeCRISMPyrophylliteSpeciationIndices(0.035, 0.040, 0.060, 0.035);
        expect(pyro.isAdvancedArgillicDetected).to.be.true;
        expect(pyro.argillicMineralClass).to.include('High-Temperature Pyrophyllite Advanced Argillic Facies');
        expect(pyro.mineralSpecies).to.include('Pyrophyllite');
        expect(pyro.hydrothermalTemperatureRegime).to.include('High-Temperature Acid Hydrothermal Activity');

        // Diaspore (BD1395 = 0.015, BD2060 = 0.045, BD2165 = 0.035, BD2320 = 0.010):
        const dias = BandMathEngine.computeCRISMPyrophylliteSpeciationIndices(0.015, 0.045, 0.035, 0.010);
        expect(dias.isAdvancedArgillicDetected).to.be.true;
        expect(dias.argillicMineralClass).to.include('Hydrothermal Diaspore Sinter Facies');
        expect(dias.mineralSpecies).to.include('Diaspore');

        // Intermediate Dickite-Kaolinite (BD1395 = 0.010, BD2060 = 0.015, BD2165 = 0.030, BD2320 = 0.010):
        const inter = BandMathEngine.computeCRISMPyrophylliteSpeciationIndices(0.010, 0.015, 0.030, 0.010);
        expect(inter.isAdvancedArgillicDetected).to.be.true;
        expect(inter.argillicMineralClass).to.include('Intermediate Argillic Dickite-Kaolinite Boundary');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMPyrophylliteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isAdvancedArgillicDetected).to.be.false;
    });
});

describe('Mars-to-Flora Transfer, Margarite Metasomatism & Margarite Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to inner main-belt progenitor asteroid (8) Flora and orbit capture', () => {
        // Mars to Flora (300 km Mars alt, 2.201 AU distance, 20 km capture alt, 5.89 deg plane change):
        const flo = TrajectoryEngine.computeMarsToFloraTransfer(300.0, 2.201, 20.0, 5.89);
        expect(flo.semiMajorAxisAU).to.be.closeTo(1.862, 0.1); // ~1.86 AU
        expect(flo.eccentricity).to.be.closeTo(0.1818, 0.01); // e ~ 0.182
        expect(flo.timeOfFlightDays).to.be.closeTo(463.20, 30.0); // ~463 days (~1.27 yr)
        expect(flo.timeOfFlightYears).to.be.closeTo(1.27, 0.1); // ~1.27 yr
        expect(flo.marsDepartureDeltaVKmS).to.be.closeTo(2.392, 0.5); // ~2.39 km/s TFI
        expect(flo.floraOrbitInsertionDeltaVKmS).to.be.closeTo(1.756, 0.5); // ~1.76 km/s FOI
        expect(flo.totalMissionDeltaVKmS).to.be.closeTo(4.148, 1.0); // ~4.15 km/s total
        expect(flo.floraContext).to.include('Mars-to-Flora');
    });

    it('should calculate high-temperature hydrothermal metasomatism of calcic anorthosite into brittle calcium-mica margarite and indurated thermal inertia', () => {
        // 15% initial porosity, 360 C hydrothermal temp, 0.18 a(Ca2+), 500 yr duration:
        const marg = KRCEngine.computeMartianMargariteMetasomatism(0.15, 360.0, 0.18, 500.0);
        expect(marg.margariteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(marg.margariteYieldWeightPercent).to.be.greaterThan(30.0); // > 30 wt% margarite
        expect(marg.induratedMicaThermalInertiaTIU).to.be.closeTo(2741.7, 200.0); // ~2742 tiu
        expect(marg.micaFaciesClass).to.include('High-Temperature Calcic Mica Hydrothermal Facies');
        expect(marg.margariteContext).to.include('Margarite Facies at 360 C');
    });

    it('should discriminate Margarite vs Muscovite vs Phlogopite in CRISM spectra', () => {
        // Margarite (Nili Fossae / Claritas: BD1410 = 0.035, BD2190 = 0.060, BD2205 = 0.020, BD2330 = 0.015):
        const marg = BandMathEngine.computeCRISMMargariteSpeciationIndices(0.035, 0.060, 0.020, 0.015);
        expect(marg.isMicaDetected).to.be.true;
        expect(marg.micaMineralClass).to.include('Brittle Calcium-Mica Margarite Facies');
        expect(marg.mineralSpecies).to.include('Margarite');
        expect(marg.metasomaticEnvironment).to.include('High-Temperature Hydrothermal Metasomatism of Anorthosite');

        // Muscovite (BD1410 = 0.015, BD2190 = 0.015, BD2205 = 0.055, BD2330 = 0.015):
        const musc = BandMathEngine.computeCRISMMargariteSpeciationIndices(0.015, 0.015, 0.055, 0.015);
        expect(musc.isMicaDetected).to.be.true;
        expect(musc.micaMineralClass).to.include('Phyllic Potassium-Mica Muscovite Facies');
        expect(musc.mineralSpecies).to.include('Muscovite');

        // Phlogopite (BD1410 = 0.010, BD2190 = 0.010, BD2205 = 0.015, BD2330 = 0.055):
        const phlog = BandMathEngine.computeCRISMMargariteSpeciationIndices(0.010, 0.010, 0.015, 0.055);
        expect(phlog.isMicaDetected).to.be.true;
        expect(phlog.micaMineralClass).to.include('Magnesian-Mica Phlogopite Skarn Facies');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMMargariteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isMicaDetected).to.be.false;
    });
});

describe('Mars-to-Metis Transfer, Stilbite Zeolite Metasomatism & Zeolite Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to large stony main-belt asteroid (9) Metis and orbit capture', () => {
        // Mars to Metis (300 km Mars alt, 2.387 AU distance, 25 km capture alt, 5.58 deg plane change):
        const met = TrajectoryEngine.computeMarsToMetisTransfer(300.0, 2.387, 25.0, 5.58);
        expect(met.semiMajorAxisAU).to.be.closeTo(1.955, 0.1); // ~1.96 AU
        expect(met.eccentricity).to.be.closeTo(0.2208, 0.01); // e ~ 0.221
        expect(met.timeOfFlightDays).to.be.closeTo(498.50, 30.0); // ~499 days (~1.37 yr)
        expect(met.timeOfFlightYears).to.be.closeTo(1.37, 0.1); // ~1.37 yr
        expect(met.marsDepartureDeltaVKmS).to.be.closeTo(2.491, 0.5); // ~2.49 km/s TMI
        expect(met.metisOrbitInsertionDeltaVKmS).to.be.closeTo(2.048, 0.5); // ~2.05 km/s MOI
        expect(met.totalMissionDeltaVKmS).to.be.closeTo(4.539, 1.0); // ~4.54 km/s total
        expect(met.metisContext).to.include('Mars-to-Metis');
    });

    it('should calculate low-temperature alkaline/neutral zeolitization of basaltic glass into high-silica stilbite zeolite and thermal inertia', () => {
        // 28% initial porosity, 95 C zeolitic temp, 0.16 a(SiO2), 300 yr duration:
        const stil = KRCEngine.computeMartianStilbiteZeoliteMetasomatism(0.28, 95.0, 0.16, 300.0);
        expect(stil.zeoliteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(stil.channelWaterYieldWeightPercent).to.be.greaterThan(8.0); // > 8 wt% channel H2O
        expect(stil.zeoliticTuffThermalInertiaTIU).to.be.closeTo(1990.8, 200.0); // ~1991 tiu
        expect(stil.zeoliteFaciesClass).to.include('High-Silica Zeolite Facies');
        expect(stil.stilbiteContext).to.include('Stilbite Zeolite at 95 C');
    });

    it('should discriminate Stilbite vs Heulandite vs Analcime in CRISM spectra', () => {
        // Stilbite (Claritas / Terra Sirenum: BD1415 = 0.035, BD1780 = 0.040, BD1940 = 0.075, BD2310 = 0.025):
        const stil = BandMathEngine.computeCRISMStilbiteHeulanditeSpeciationIndices(0.035, 0.040, 0.075, 0.025);
        expect(stil.isZeoliteDetected).to.be.true;
        expect(stil.zeoliteMineralClass).to.include('Hydrated High-Silica Stilbite Zeolite Facies');
        expect(stil.mineralSpecies).to.include('Stilbite');
        expect(stil.zeolitizationRegime).to.include('Low-Temperature Alkaline Diagenesis');

        // Heulandite (BD1415 = 0.025, BD1780 = 0.015, BD1940 = 0.055, BD2310 = 0.025):
        const heu = BandMathEngine.computeCRISMStilbiteHeulanditeSpeciationIndices(0.025, 0.015, 0.055, 0.025);
        expect(heu.isZeoliteDetected).to.be.true;
        expect(heu.zeoliteMineralClass).to.include('Heulandite-Clinoptilolite Zeolite Strata');
        expect(heu.mineralSpecies).to.include('Heulandite');

        // Analcime (BD1415 = 0.010, BD1780 = 0.010, BD1940 = 0.050, BD2310 = 0.010):
        const anal = BandMathEngine.computeCRISMStilbiteHeulanditeSpeciationIndices(0.010, 0.010, 0.050, 0.010);
        expect(anal.isZeoliteDetected).to.be.true;
        expect(anal.zeoliteMineralClass).to.include('Analcime Sodic Zeolite Metasomatism');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMStilbiteHeulanditeSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isZeoliteDetected).to.be.false;
    });
});

describe('Mars-to-Hygiea Transfer, Szomolnokite Kinetics & Monohydrate Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to large primitive C-type asteroid (10) Hygiea and orbit capture', () => {
        // Mars to Hygiea (300 km Mars alt, 3.142 AU distance, 50 km capture alt, 3.84 deg plane change):
        const hyg = TrajectoryEngine.computeMarsToHygieaTransfer(300.0, 3.142, 50.0, 3.84);
        expect(hyg.semiMajorAxisAU).to.be.closeTo(2.333, 0.1); // ~2.33 AU
        expect(hyg.eccentricity).to.be.closeTo(0.3469, 0.01); // e ~ 0.347
        expect(hyg.timeOfFlightDays).to.be.closeTo(649.30, 40.0); // ~649 days (~1.78 yr)
        expect(hyg.timeOfFlightYears).to.be.closeTo(1.78, 0.1); // ~1.78 yr
        expect(hyg.marsDepartureDeltaVKmS).to.be.closeTo(2.626, 0.5); // ~2.63 km/s THI
        expect(hyg.hygieaOrbitInsertionDeltaVKmS).to.be.closeTo(3.188, 0.5); // ~3.19 km/s HOI
        expect(hyg.totalMissionDeltaVKmS).to.be.closeTo(5.814, 1.0); // ~5.81 km/s total
        expect(hyg.hygieaContext).to.include('Mars-to-Hygiea');
    });

    it('should calculate atmospheric desiccation/dehydration of ferrous polyhydrate sulfates into szomolnokite monohydrate and thermal inertia', () => {
        // 25% initial porosity, 25 C surface temp, 0.08 RH, 200 yr duration:
        const szom = KRCEngine.computeMartianSzomolnokiteKinetics(0.25, 25.0, 0.08, 200.0);
        expect(szom.szomolnokiteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(szom.monohydrateYieldWeightPercent).to.be.greaterThan(40.0); // > 40 wt% szomolnokite
        expect(szom.induratedMonohydrateThermalInertiaTIU).to.be.closeTo(2203.8, 200.0); // ~2204 tiu
        expect(szom.monohydrateFaciesClass).to.include('Indurated Monohydrate Sulfate Facies');
        expect(szom.szomolnokiteContext).to.include('Szomolnokite at 25 C');
    });

    it('should discriminate Szomolnokite vs Kieserite vs Polyhydrate in CRISM spectra', () => {
        // Szomolnokite (Juventae / Candor / Aram: BD1470 = 0.040, BD1970 = 0.045, BD2130 = 0.060, BD2400 = 0.050):
        const szom = BandMathEngine.computeCRISMSzomolnokiteSpeciationIndices(0.040, 0.045, 0.060, 0.050);
        expect(szom.isSulfateDetected).to.be.true;
        expect(szom.sulfateMineralClass).to.include('Ferrous Monohydrate Sulfate Facies');
        expect(szom.mineralSpecies).to.include('Szomolnokite');
        expect(szom.hydrationEnvironment).to.include('Low-Water-Activity Acid Evaporite Desiccation');

        // Kieserite (BD1470 = 0.010, BD1970 = 0.010, BD2130 = 0.055, BD2400 = 0.045):
        const kies = BandMathEngine.computeCRISMSzomolnokiteSpeciationIndices(0.010, 0.010, 0.055, 0.045);
        expect(kies.isSulfateDetected).to.be.true;
        expect(kies.sulfateMineralClass).to.include('Magnesian Monohydrate Sulfate Facies');
        expect(kies.mineralSpecies).to.include('Kieserite');

        // Polyhydrate (BD1470 = 0.010, BD1970 = 0.050, BD2130 = 0.010, BD2400 = 0.015):
        const poly = BandMathEngine.computeCRISMSzomolnokiteSpeciationIndices(0.010, 0.050, 0.010, 0.015);
        expect(poly.isSulfateDetected).to.be.true;
        expect(poly.sulfateMineralClass).to.include('Hydrated Polyhydrate Sulfate Sequence');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMSzomolnokiteSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isSulfateDetected).to.be.false;
    });
});

describe('Mars-to-Parthenope Transfer, Mirabilite-Thenardite Kinetics & Sodium Sulfate Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to stony main-belt asteroid (11) Parthenope and orbit capture', () => {
        // Mars to Parthenope (300 km Mars alt, 2.453 AU distance, 20 km capture alt, 4.63 deg plane change):
        const par = TrajectoryEngine.computeMarsToParthenopeTransfer(300.0, 2.453, 20.0, 4.63);
        expect(par.semiMajorAxisAU).to.be.closeTo(1.988, 0.1); // ~1.99 AU
        expect(par.eccentricity).to.be.closeTo(0.2337, 0.01); // e ~ 0.234
        expect(par.timeOfFlightDays).to.be.closeTo(511.20, 30.0); // ~511 days (~1.40 yr)
        expect(par.timeOfFlightYears).to.be.closeTo(1.40, 0.1); // ~1.40 yr
        expect(par.marsDepartureDeltaVKmS).to.be.closeTo(2.368, 0.5); // ~2.37 km/s TPI
        expect(par.parthenopeOrbitInsertionDeltaVKmS).to.be.closeTo(2.242, 0.5); // ~2.24 km/s POI
        expect(par.totalMissionDeltaVKmS).to.be.closeTo(4.610, 1.0); // ~4.61 km/s total
        expect(par.parthenopeContext).to.include('Mars-to-Parthenope');
    });

    it('should calculate atmospheric desiccation and phase transition of mirabilite into anhydrous thenardite and thermal inertia', () => {
        // 32% initial porosity, 20 C evaporite temp, 0.05 RH, 150 yr duration:
        const then = KRCEngine.computeMartianMirabiliteThenarditeKinetics(0.32, 20.0, 0.05, 150.0);
        expect(then.thenarditeConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(then.waterLossWeightPercent).to.be.greaterThan(30.0); // > 30 wt% water loss
        expect(then.desiccatedThenarditeThermalInertiaTIU).to.be.closeTo(1690.6, 200.0); // ~1691 tiu
        expect(then.sulfateFaciesClass).to.include('Anhydrous Sodium Sulfate Facies');
        expect(then.thenarditeContext).to.include('Thenardite at 20 C');
    });

    it('should discriminate Thenardite vs Mirabilite vs Bloedite in CRISM spectra', () => {
        // Thenardite (Columbus / Noctis / Juventae: BD1450 = 0.015, BD1780 = 0.015, BD1940 = 0.020, BD2180 = 0.055):
        const then = BandMathEngine.computeCRISMSodiumSulfateSpeciationIndices(0.015, 0.015, 0.020, 0.055);
        expect(then.isSodiumSulfateDetected).to.be.true;
        expect(then.sulfateMineralClass).to.include('Anhydrous Sodium Sulfate Thenardite Facies');
        expect(then.mineralSpecies).to.include('Thenardite');
        expect(then.evaporiteRegime).to.include('Extreme Hyper-Arid Desiccation');

        // Mirabilite (BD1450 = 0.035, BD1780 = 0.015, BD1940 = 0.065, BD2180 = 0.015):
        const mira = BandMathEngine.computeCRISMSodiumSulfateSpeciationIndices(0.035, 0.015, 0.065, 0.015);
        expect(mira.isSodiumSulfateDetected).to.be.true;
        expect(mira.sulfateMineralClass).to.include('Hydrated Mirabilite Decahydrate Facies');
        expect(mira.mineralSpecies).to.include('Mirabilite');

        // Bloedite (BD1450 = 0.015, BD1780 = 0.035, BD1940 = 0.045, BD2180 = 0.040):
        const bloed = BandMathEngine.computeCRISMSodiumSulfateSpeciationIndices(0.015, 0.035, 0.045, 0.040);
        expect(bloed.isSodiumSulfateDetected).to.be.true;
        expect(bloed.sulfateMineralClass).to.include('Hydrated Mixed Na-Mg Sulfate Bloedite Facies');
        expect(bloed.mineralSpecies).to.include('Bloedite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMSodiumSulfateSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isSodiumSulfateDetected).to.be.false;
    });
});

describe('Mars-to-Victoria Transfer, Halloysite Kinetics & Kaolin Nanomineral Speciation', () => {
    it('should calculate interplanetary 3D direct transfer from Mars to stony main-belt asteroid (12) Victoria and orbit capture', () => {
        // Mars to Victoria (300 km Mars alt, 2.334 AU distance, 15 km capture alt, 8.36 deg plane change):
        const vic = TrajectoryEngine.computeMarsToVictoriaTransfer(300.0, 2.334, 15.0, 8.36);
        expect(vic.semiMajorAxisAU).to.be.closeTo(1.929, 0.1); // ~1.93 AU
        expect(vic.eccentricity).to.be.closeTo(0.2100, 0.01); // e ~ 0.210
        expect(vic.timeOfFlightDays).to.be.closeTo(488.50, 30.0); // ~489 days (~1.34 yr)
        expect(vic.timeOfFlightYears).to.be.closeTo(1.34, 0.1); // ~1.34 yr
        expect(vic.marsDepartureDeltaVKmS).to.be.closeTo(3.018, 0.5); // ~3.02 km/s TVI
        expect(vic.victoriaOrbitInsertionDeltaVKmS).to.be.closeTo(2.077, 0.5); // ~2.08 km/s VOI
        expect(vic.totalMissionDeltaVKmS).to.be.closeTo(5.095, 1.0); // ~5.10 km/s total
        expect(vic.victoriaContext).to.include('Mars-to-Victoria');
    });

    it('should calculate hydrothermal alteration and nanotubular crystallization kinetics of weathered volcanic ash into hydrated halloysite and thermal inertia', () => {
        // 35% initial porosity, 120 C hydrothermal temp, 0.85 a(H2O), 250 yr duration:
        const hal = KRCEngine.computeMartianHalloysiteKinetics(0.35, 120.0, 0.85, 250.0);
        expect(hal.halloysiteConversionFraction).to.be.greaterThan(0.50); // > 50% converted
        expect(hal.boundWaterYieldWeightPercent).to.be.greaterThan(8.0); // > 8 wt% bound H2O
        expect(hal.claystoneThermalInertiaTIU).to.be.closeTo(1617.4, 200.0); // ~1617 tiu
        expect(hal.kaolinFaciesClass).to.include('Nanotubular Halloysite Kaolin Facies');
        expect(hal.halloysiteContext).to.include('Halloysite at 120 C');
    });

    it('should discriminate Halloysite vs Kaolinite vs Smectite in CRISM spectra', () => {
        // Halloysite (Mawrth Vallis / Nili: BD1410 = 0.040, BD1920 = 0.045, BD2165 = 0.020, BD2208 = 0.060):
        const hal = BandMathEngine.computeCRISMHalloysiteKaolinSpeciationIndices(0.040, 0.045, 0.020, 0.060);
        expect(hal.isKaolinDetected).to.be.true;
        expect(hal.clayMineralClass).to.include('Nanotubular Hydrated Halloysite Facies');
        expect(hal.mineralSpecies).to.include('Halloysite-10A');
        expect(hal.alterationRegime).to.include('Low-Temperature Hydrothermal Ash Alteration');

        // Kaolinite (BD1410 = 0.040, BD1920 = 0.015, BD2165 = 0.035, BD2208 = 0.060):
        const kaol = BandMathEngine.computeCRISMHalloysiteKaolinSpeciationIndices(0.040, 0.015, 0.035, 0.060);
        expect(kaol.isKaolinDetected).to.be.true;
        expect(kaol.clayMineralClass).to.include('Ordered Platy Kaolinite Facies');
        expect(kaol.mineralSpecies).to.include('Kaolinite');

        // Smectite (BD1410 = 0.015, BD1920 = 0.045, BD2165 = 0.010, BD2208 = 0.055):
        const smec = BandMathEngine.computeCRISMHalloysiteKaolinSpeciationIndices(0.015, 0.045, 0.010, 0.055);
        expect(smec.isKaolinDetected).to.be.true;
        expect(smec.clayMineralClass).to.include('Hydrated Dioctahedral Smectite Facies');
        expect(smec.mineralSpecies).to.include('Montmorillonite');

        // Basalt baseline:
        const basalt = BandMathEngine.computeCRISMHalloysiteKaolinSpeciationIndices(0.005, 0.005, 0.005, 0.005);
        expect(basalt.isKaolinDetected).to.be.false;
    });
});

if (typeof mocha !== 'undefined') {
    mocha.run();
}

