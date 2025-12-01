import { jmarsState } from '../src/jmars-state.js';
import { JMARSWMS } from '../src/jmars-wms.js';
import { EVENTS } from '../src/constants.js';

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
        expect(url).to.include('i=50'); // 1.3.0 defaults
        expect(url).to.include('j=50');
    });
});
