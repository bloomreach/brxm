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
  let DomService;
  let OverlayService;
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
      _RenderingService_,
      _SpaService_,
    ) => {
      $log = _$log_;
      DomService = _DomService_;
      OverlayService = _OverlayService_;
      RenderingService = _RenderingService_;
      SpaService = _SpaService_;
    });

    spyOn(DomService, 'getIframeWindow').and.returnValue(iframeWindow);
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
      spyOn(RenderingService, 'createOverlay');
      publicApi.createOverlay();
      expect(RenderingService.createOverlay).toHaveBeenCalled();
    });

    it('can sync the positions of the overlay elements', () => {
      spyOn(OverlayService, 'sync');
      publicApi.syncOverlay();
      expect(OverlayService.sync).toHaveBeenCalled();
    });

    it('can sync the positions and meta-data of the overlay elements', () => {
      spyOn(RenderingService, 'createOverlay');
      publicApi.sync();
      expect(RenderingService.createOverlay).toHaveBeenCalled();
    });
  });

  describe('renderComponent', () => {
    it('ignores the SPA when it does not exist', () => {
      SpaService.initLegacy();
      expect(SpaService.renderComponent({})).toBe(false);
    });

    it('ignores the SPA when it does not define a renderComponent function', () => {
      iframeWindow.SPA = {};
      SpaService.initLegacy();
      expect(SpaService.renderComponent({})).toBe(false);
    });

    describe('an SPA that defines a renderComponent function', () => {
      beforeEach(() => {
        iframeWindow.SPA = jasmine.createSpyObj('SPA', ['renderComponent']);
        SpaService.initLegacy();
      });

      it('ignores null and undefined components', () => {
        expect(SpaService.renderComponent()).toBe(false);
        expect(SpaService.renderComponent(null)).toBe(false);
      });

      describe('with an existing component', () => {
        let component;
        beforeEach(() => {
          component = jasmine.createSpyObj('component', ['getReferenceNamespace']);
          component.getReferenceNamespace.and.returnValue('r1_r2_r3');
        });

        it('renders the component in the SPA', () => {
          expect(SpaService.renderComponent(component)).toBe(true);
          expect(iframeWindow.SPA.renderComponent).toHaveBeenCalledWith('r1_r2_r3', {});
        });

        it('renders the component with specific parameters in the SPA', () => {
          expect(SpaService.renderComponent(component, { foo: 1 })).toBe(true);
          expect(iframeWindow.SPA.renderComponent).toHaveBeenCalledWith('r1_r2_r3', { foo: 1 });
        });

        it('can let the SPA indicate it did not render the component ', () => {
          iframeWindow.SPA.renderComponent.and.returnValue(false);
          expect(SpaService.renderComponent(component)).toBe(false);
          expect(iframeWindow.SPA.renderComponent).toHaveBeenCalledWith('r1_r2_r3', {});
        });

        it('logs an error when the SPA throws an error while rendering the component', () => {
          spyOn($log, 'error');
          iframeWindow.SPA.renderComponent.and.throwError('Failed to render');

          expect(SpaService.renderComponent(component)).toBe(true);
          expect($log.error).toHaveBeenCalledWith(new Error('Failed to render'));
        });
      });
    });
  });
});
