/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

describe('SpaService', () => {
  let $log;
  let $rootScope;
  let ChannelService;
  let DomService;
  let PageStructureService;
  let RpcService;
  let SpaService;
  let iframeWindow;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$log_,
      _$rootScope_,
      _ChannelService_,
      _DomService_,
      _PageStructureService_,
      _RpcService_,
      _SpaService_,
    ) => {
      $log = _$log_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      DomService = _DomService_;
      PageStructureService = _PageStructureService_;
      RpcService = _RpcService_;
      SpaService = _SpaService_;
    });

    iframeWindow = {};
    spyOn(DomService, 'getIframeWindow').and.returnValue(iframeWindow);
  });

  describe('init', () => {
    let iframeJQueryElement;

    beforeEach(() => {
      iframeJQueryElement = angular.element('<iframe />');
      spyOn(RpcService, 'initialize');
      spyOn(SpaService, 'getOrigin');
    });

    it('uses the iframe content window as a target', () => {
      SpaService.init(iframeJQueryElement);

      expect(RpcService.initialize).toHaveBeenCalledWith(jasmine.objectContaining({
        target: iframeJQueryElement[0].contentWindow,
      }));
    });

    it('passes an SPA origin', () => {
      SpaService.getOrigin.and.returnValue('http://example.com:3000');
      SpaService.init(iframeJQueryElement);

      expect(RpcService.initialize).toHaveBeenCalledWith(jasmine.objectContaining({
        origin: 'http://example.com:3000',
      }));
    });

    it('registers sync overlay callback', () => {
      let sync;

      spyOn(RpcService, 'register').and.callFake((command, callback) => { sync = callback; });
      spyOn(PageStructureService, 'parseElements');
      SpaService.init(iframeJQueryElement);

      expect(RpcService.register).toHaveBeenCalledWith('sync', jasmine.any(Function));

      sync();
      expect(PageStructureService.parseElements).toHaveBeenCalledWith(true);
    });
  });

  describe('getOrigin', () => {
    beforeEach(() => {
      spyOn(ChannelService, 'getChannel');
      spyOn(ChannelService, 'getProperties');
    });

    it('returns an origin from the preview url', () => {
      ChannelService.getProperties.and.returnValue({
        'org.hippoecm.hst.configuration.channel.PreviewURLChannelInfo_url': 'http://example.com:3000/something',
      });

      expect(SpaService.getOrigin()).toBe('http://example.com:3000');
    });

    it('returns an origin from the channel url', () => {
      ChannelService.getChannel.and.returnValue({ url: 'http://localhost:8080/_cmsinternal' });

      expect(SpaService.getOrigin()).toBe('http://localhost:8080');
    });

    it('returns an empty origin when there is no configured url', () => {
      expect(SpaService.getOrigin()).toBeUndefined();
    });

    it('returns an empty origin when the url is invalid', () => {
      ChannelService.getChannel.and.returnValue({ url: '/_cmsinternal' });

      expect(SpaService.getOrigin()).toBeUndefined();
    });
  });

  describe('destroy', () => {
    let iframeJQueryElement;

    beforeEach(() => {
      iframeJQueryElement = angular.element('<iframe />');
      spyOn(RpcService, 'initialize');
      spyOn(ChannelService, 'getChannel');
      spyOn(ChannelService, 'getProperties');

      SpaService.init(iframeJQueryElement);
    });

    it('stops reacting on the SPA ready event', () => {
      SpaService.destroy();
      $rootScope.$emit('spa:ready');
      $rootScope.$digest();

      expect(SpaService.isSpa()).toBe(false);
    });

    it('resets the SPA flag', () => {
      $rootScope.$emit('spa:ready');
      $rootScope.$digest();
      SpaService.destroy();

      expect(SpaService.isSpa()).toBe(false);
    });

    it('resets the legacy handle', () => {
      iframeWindow.SPA = {};
      SpaService.initLegacy();
      SpaService.destroy();

      expect(SpaService.isSpa()).toBe(false);
    });

    it('destroys a remote connection', () => {
      spyOn(RpcService, 'destroy');
      SpaService.destroy();

      expect(RpcService.destroy).toHaveBeenCalled();
    });
  });

  describe('isSpa', () => {
    it('returns false when a legacy SPA is not present', () => {
      SpaService.initLegacy();
      expect(SpaService.isSpa()).toBe(false);
    });

    it('returns true when a legacy SPA is present', () => {
      iframeWindow.SPA = {};
      SpaService.initLegacy();
      expect(SpaService.isSpa()).toBe(true);
    });

    it('returns false when there is no iframe window', () => {
      DomService.getIframeWindow.and.returnValue(null);
      SpaService.initLegacy();
      expect(SpaService.isSpa()).toBe(false);
    });

    it('returns true when there was a ready event', () => {
      const element = angular.element('<iframe />');

      SpaService.init(element);
      $rootScope.$emit('spa:ready');
      $rootScope.$digest();

      expect(SpaService.isSpa()).toBe(true);
    });

    it('returns false when the SPA was unloaded', () => {
      const element = angular.element('<iframe />');

      SpaService.init(element);
      $rootScope.$emit('spa:ready');
      $rootScope.$emit('iframe:unload');
      $rootScope.$digest();

      expect(SpaService.isSpa()).toBe(false);
    });

    it('returns false when the legacy SPA was unloaded', () => {
      const element = angular.element('<iframe />');

      iframeWindow.SPA = {};
      SpaService.init(element);
      SpaService.initLegacy();
      $rootScope.$emit('iframe:unload');
      $rootScope.$digest();

      expect(SpaService.isSpa()).toBe(false);
    });
  });

  describe('initLegacy', () => {
    let publicApi;

    beforeEach(() => {
      iframeWindow.SPA = jasmine.createSpyObj('SPA', ['init']);
      SpaService.initLegacy();
      [publicApi] = iframeWindow.SPA.init.calls.mostRecent().args;
    });

    it('calls an initialization handle', () => {
      expect(iframeWindow.SPA.init).toHaveBeenCalledWith(jasmine.any(Object));
    });

    it('logs errors thrown by the SPA', () => {
      const error = new Error('bad stuff happened');
      iframeWindow.SPA.init.and.throwError(error);
      spyOn($log, 'error');
      SpaService.initLegacy();
      expect($log.error).toHaveBeenCalledWith('Failed to initialize Single Page Application', error);
    });

    it('can create the overlay', () => {
      spyOn(PageStructureService, 'parseElements');
      publicApi.createOverlay();
      expect(PageStructureService.parseElements).toHaveBeenCalled();
    });

    it('can sync the positions of the overlay elements', () => {
      spyOn(PageStructureService, 'parseElements');
      publicApi.syncOverlay();
      expect(PageStructureService.parseElements).toHaveBeenCalled();
    });

    it('can sync the positions and meta-data of the overlay elements', () => {
      spyOn(PageStructureService, 'parseElements');
      publicApi.sync();
      expect(PageStructureService.parseElements).toHaveBeenCalled();
    });
  });

  describe('inject', () => {
    it('calls a remote procedure', () => {
      spyOn(RpcService, 'call');
      SpaService.inject('something');

      expect(RpcService.call).toHaveBeenCalledWith('inject', 'something');
    });
  });


  describe('renderComponent', () => {
    it('ignores the SPA when it does not exist', (done) => {
      SpaService.initLegacy();
      SpaService.renderComponent({})
        .catch((error) => {
          expect(error).toEqual(new Error('Cannot render the component in the SPA.'));
        })
        .then(done);

      $rootScope.$digest();
    });

    it('ignores the SPA when it does not define a renderComponent function', (done) => {
      iframeWindow.SPA = {};
      SpaService.initLegacy();
      SpaService.renderComponent({})
        .catch((error) => {
          expect(error).toEqual(new Error('The SPA does not support the component rendering.'));
        })
        .then(done);

      $rootScope.$digest();
    });

    it('calls a remote procedure when it is not a legacy SPA', () => {
      spyOn(SpaService, 'isSpa').and.returnValue(true);
      spyOn(RpcService, 'trigger');

      const properties = { a: 'b' };
      const component = jasmine.createSpyObj('component', ['getReferenceNamespace']);
      component.getReferenceNamespace.and.returnValue('r1_r2_r3');

      SpaService.renderComponent(component, properties);
      expect(RpcService.trigger).toHaveBeenCalledWith('update', jasmine.objectContaining({
        properties,
        id: 'r1_r2_r3',
      }));
    });

    it('does not call a remote procedure when it is not an SPA', (done) => {
      spyOn(SpaService, 'isSpa').and.returnValue(false);
      spyOn(RpcService, 'trigger');

      SpaService.renderComponent({})
        .catch((error) => {
          expect(RpcService.trigger).not.toHaveBeenCalled();
          expect(error).toEqual(new Error('Cannot render the component in the SPA.'));
        })
        .then(done);

      $rootScope.$digest();
    });

    it('should reject a promise on iframe unload event', (done) => {
      spyOn(SpaService, 'isSpa').and.returnValue(true);
      spyOn(RpcService, 'trigger');

      const component = jasmine.createSpyObj('component', ['getReferenceNamespace']);
      component.getReferenceNamespace.and.returnValue('r1_r2_r3');

      SpaService.init(angular.element('<iframe>'));
      SpaService.renderComponent(component, {})
        .catch((error) => {
          expect(error).toEqual(new Error('Could not update the component.'));
        })
        .then(done);

      $rootScope.$emit('iframe:unload');
      $rootScope.$digest();
    });

    it('should resolve a promise on the next sync call', (done) => {
      spyOn(SpaService, 'isSpa').and.returnValue(true);
      spyOn(PageStructureService, 'parseElements');
      spyOn(RpcService, 'register');
      spyOn(RpcService, 'trigger');

      const component = jasmine.createSpyObj('component', ['getReferenceNamespace']);
      component.getReferenceNamespace.and.returnValue('r1_r2_r3');

      SpaService.init(angular.element('<iframe>'));
      SpaService.renderComponent(component, {})
        .then(() => {
          expect(PageStructureService.parseElements).toHaveBeenCalledWith(false);
        })
        .then(done);

      const { args: [, onSync] } = RpcService.register.calls.mostRecent();

      onSync();
      $rootScope.$digest();
    });

    describe('an SPA that defines a renderComponent function', () => {
      beforeEach(() => {
        iframeWindow.SPA = jasmine.createSpyObj('SPA', ['renderComponent']);
        SpaService.initLegacy();
      });

      it('ignores null component', (done) => {
        SpaService.renderComponent(null)
          .catch((error) => {
            expect(error).toEqual(new Error('Cannot render the component in the SPA.'));
          })
          .then(done);

        $rootScope.$digest();
      });

      it('ignores undefined component', (done) => {
        SpaService.renderComponent(null)
          .catch((error) => {
            expect(error).toEqual(new Error('Cannot render the component in the SPA.'));
          })
          .then(done);

        $rootScope.$digest();
      });

      describe('with an existing component', () => {
        let component;
        beforeEach(() => {
          component = jasmine.createSpyObj('component', ['getReferenceNamespace']);
          component.getReferenceNamespace.and.returnValue('r1_r2_r3');
        });

        it('renders the component in the SPA', () => {
          SpaService.renderComponent(component);

          expect(iframeWindow.SPA.renderComponent).toHaveBeenCalledWith('r1_r2_r3', {});
        });

        it('renders the component with specific parameters in the SPA', () => {
          SpaService.renderComponent(component, { foo: 1 });

          expect(iframeWindow.SPA.renderComponent).toHaveBeenCalledWith('r1_r2_r3', { foo: 1 });
        });

        it('rejects with an error when the SPA throws an error while rendering the component', (done) => {
          spyOn($log, 'error');
          iframeWindow.SPA.renderComponent.and.throwError('Failed to render.');

          SpaService.renderComponent(component)
            .catch((error) => {
              expect(error).toEqual(new Error('Failed to render.'));
            })
            .then(done);

          $rootScope.$digest();
        });
      });
    });
  });
});
