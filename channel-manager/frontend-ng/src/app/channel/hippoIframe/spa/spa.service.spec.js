/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
  let DomService;
  let OverlayService;
  let PageStructureService;
  let RenderingService;
  let SpaService;

  const iframeWindow = {
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$log_,
      _DomService_,
      _OverlayService_,
      _PageStructureService_,
      _RenderingService_,
      _SpaService_,
    ) => {
      $log = _$log_;
      DomService = _DomService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      RenderingService = _RenderingService_;
      SpaService = _SpaService_;
    });

    spyOn(DomService, 'getIframeWindow').and.returnValue(iframeWindow);
  });

  describe('initialization of the service', () => {
    it('initializes the iframe element', () => {
      const iframeJQueryElement = {};
      SpaService.init(iframeJQueryElement);
      SpaService.detectSpa();
      expect(DomService.getIframeWindow).toHaveBeenCalledWith(iframeJQueryElement);
    });
  });

  describe('detection of SPA', () => {
    it('detects when an SPA is not present', () => {
      expect(SpaService.detectSpa()).toBe(false);
      expect(SpaService.spa).toBe(null);
      expect(SpaService.detectedSpa()).toBe(false);
    });

    it('detects when an SPA is present', () => {
      iframeWindow.SPA = {};
      expect(SpaService.detectSpa()).toBe(true);
      expect(SpaService.spa).toEqual(jasmine.any(Object));
      expect(SpaService.detectedSpa()).toBe(true);
    });

    it('knows no SPA is present when there is no iframe window', () => {
      DomService.getIframeWindow.and.returnValue(null);
      expect(SpaService.detectSpa()).toBe(false);
      expect(SpaService.spa).toBe(null);
      expect(SpaService.detectedSpa()).toBe(false);
    });
  });

  describe('initialization of the SPA', () => {
    beforeEach(() => {
      iframeWindow.SPA = jasmine.createSpyObj('SPA', ['init']);
      SpaService.detectSpa();
    });

    it('initializes the SPA', () => {
      SpaService.initSpa();
      expect(iframeWindow.SPA.init).toHaveBeenCalledWith(jasmine.any(Object));
    });

    it('logs errors thrown by the SPA', () => {
      const error = new Error('bad stuff happened');
      iframeWindow.SPA.init.and.throwError(error);
      spyOn($log, 'error');
      SpaService.initSpa();
      expect($log.error).toHaveBeenCalledWith('Failed to initialize Single Page Application', error);
    });
  });

  describe('public API', () => {
    let publicApi;

    beforeEach(() => {
      iframeWindow.SPA = jasmine.createSpyObj('SPA', ['init']);
      SpaService.detectSpa();
      SpaService.initSpa();
      publicApi = iframeWindow.SPA.init.calls.mostRecent().args[0];
    });

    it('can create the overlay', () => {
      spyOn(RenderingService, 'createOverlay');
      publicApi.createOverlay();
      expect(RenderingService.createOverlay).toHaveBeenCalled();
    });

    it('can sync the positions of the overlay elements', () => {
      spyOn(OverlayService, 'sync');
      publicApi.syncOverlay();
      expect(OverlayService.sync).toHaveBeenCalled();
    });
  });

  describe('render component', () => {
    it('ignores the SPA when it does not exist', () => {
      SpaService.detectSpa();
      expect(SpaService.renderComponent('1234')).toBe(false);
    });

    it('ignores the SPA when it does not define a renderComponent function', () => {
      iframeWindow.SPA = {};
      SpaService.detectSpa();
      expect(SpaService.renderComponent('1234')).toBe(false);
    });

    describe('an SPA that defines a renderComponent function', () => {
      beforeEach(() => {
        iframeWindow.SPA = jasmine.createSpyObj('SPA', ['renderComponent']);
        SpaService.detectSpa();
      });

      it('ignores unknown components', () => {
        spyOn(PageStructureService, 'getComponentById').and.returnValue(null);
        spyOn($log, 'warn');
        expect(SpaService.renderComponent('1234')).toBe(false);
        expect($log.warn).toHaveBeenCalledWith('SPA cannot render unknown component with ID \'1234\'');
      });

      describe('with an existing component', () => {
        beforeEach(() => {
          const component = jasmine.createSpyObj('component', ['getReferenceNamespace']);
          component.getReferenceNamespace.and.returnValue('r1_r2_r3');
          spyOn(PageStructureService, 'getComponentById').and.returnValue(component);
        });

        it('renders the component in the SPA', () => {
          expect(SpaService.renderComponent('1234')).toBe(true);
          expect(iframeWindow.SPA.renderComponent).toHaveBeenCalledWith('r1_r2_r3', {});
        });

        it('renders the component with specific parameters in the SPA', () => {
          expect(SpaService.renderComponent('1234', { foo: 1 })).toBe(true);
          expect(iframeWindow.SPA.renderComponent).toHaveBeenCalledWith('r1_r2_r3', { foo: 1 });
        });

        it('can let the SPA indicate it did not render the component ', () => {
          iframeWindow.SPA.renderComponent.and.returnValue(false);
          expect(SpaService.renderComponent('1234')).toBe(false);
          expect(iframeWindow.SPA.renderComponent).toHaveBeenCalledWith('r1_r2_r3', {});
        });
      });
    });
  });
});
