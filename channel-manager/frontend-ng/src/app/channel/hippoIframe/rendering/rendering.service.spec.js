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

describe('RenderingService', () => {
  let $q;
  let $rootScope;
  let DomService;
  let HippoIframeService;
  let OverlayService;
  let PageStructureService;
  let RenderingService;
  let ScrollService;

  const iframeDocument = {
    location: {
      host: 'localhost',
      protocol: 'http:',
    },
  };

  class EmitteryMock {
    constructor() {
      this.on = jasmine.createSpy('on');
      this.emit = jasmine.createSpy('emit');
    }
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    angular.mock.module(($provide) => {
      $provide.constant('Emittery', EmitteryMock);
    });

    inject((
      _$q_,
      _$rootScope_,
      _DomService_,
      _HippoIframeService_,
      _OverlayService_,
      _PageStructureService_,
      _RenderingService_,
      _ScrollService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      DomService = _DomService_;
      HippoIframeService = _HippoIframeService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      RenderingService = _RenderingService_;
      ScrollService = _ScrollService_;
    });

    spyOn(DomService, 'getIframeDocument').and.returnValue(iframeDocument);
    spyOn(DomService, 'getIframeWindow').and.returnValue(window);
  });

  describe('onOverlayCreated', () => {
    it('registers an "overlay-created" callback', () => {
      RenderingService.onOverlayCreated(angular.noop);
      expect(RenderingService.emitter.on).toHaveBeenCalledWith('overlay-created', jasmine.any(Function));
    });
  });

  describe('createOverlay', () => {
    beforeEach(() => {
      spyOn(HippoIframeService, 'signalPageLoadCompleted');
      spyOn(OverlayService, 'clear');
      spyOn(PageStructureService, 'clearParsedElements');
      spyOn(PageStructureService, 'parseElements');
      spyOn(ScrollService, 'savePosition');
      spyOn(ScrollService, 'restorePosition');
    });

    it('handles the loading of a new page', () => {
      spyOn(DomService, 'hasCssLink').and.returnValue(false);
      spyOn(DomService, 'addCssLinks').and.returnValue($q.resolve());

      RenderingService.createOverlay();
      $rootScope.$digest();

      expect(DomService.addCssLinks).toHaveBeenCalledWith(window, [jasmine.any(String)], 'hippo-css');
      expect(ScrollService.savePosition).toHaveBeenCalled();
      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(OverlayService.clear).toHaveBeenCalled();
      expect(PageStructureService.parseElements).toHaveBeenCalledWith();
      expect(RenderingService.emitter.emit).toHaveBeenCalledWith('overlay-created');
      expect(ScrollService.restorePosition).toHaveBeenCalled();
      expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
    });

    it('handles the re-loading of an existing page', () => {
      spyOn(DomService, 'hasCssLink').and.returnValue(true);
      spyOn(DomService, 'addCssLinks');

      RenderingService.createOverlay();
      $rootScope.$digest();

      expect(DomService.addCssLinks).not.toHaveBeenCalled();
      expect(ScrollService.savePosition).toHaveBeenCalled();
      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(OverlayService.clear).toHaveBeenCalled();
      expect(PageStructureService.parseElements).toHaveBeenCalledWith();
      expect(RenderingService.emitter.emit).toHaveBeenCalledWith('overlay-created');
      expect(ScrollService.restorePosition).toHaveBeenCalled();
      expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
    });

    it('prevents concurrent invocations', () => {
      spyOn(DomService, 'hasCssLink').and.returnValue(false);
      spyOn(DomService, 'addCssLinks').and.returnValue($q.resolve());

      const promise1 = RenderingService.createOverlay();
      const promise2 = RenderingService.createOverlay();
      $rootScope.$digest();

      expect(DomService.addCssLinks).toHaveBeenCalledTimes(1);
      expect(PageStructureService.parseElements).toHaveBeenCalledTimes(1);
      expect(promise1).toBe(promise2);
    });

    it('clears the parsed elements, then stops when loading the hippo-iframe CSS file throws an error', () => {
      spyOn(DomService, 'hasCssLink').and.returnValue(false);
      spyOn(DomService, 'addCssLinks').and.returnValue($q.reject());

      RenderingService.createOverlay();
      $rootScope.$digest();

      expect(ScrollService.savePosition).toHaveBeenCalled();
      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(OverlayService.clear).toHaveBeenCalled();
      expect(PageStructureService.parseElements).not.toHaveBeenCalled();
      expect(RenderingService.emitter.emit).not.toHaveBeenCalledWith();
      expect(ScrollService.restorePosition).not.toHaveBeenCalled();
      expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
    });

    it('clears the parsed elements, then stops if the iframe DOM is not present', () => {
      spyOn(DomService, 'hasIframeDocument').and.returnValue(false);

      RenderingService.createOverlay();
      $rootScope.$digest();

      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(OverlayService.clear).toHaveBeenCalled();
      expect(PageStructureService.parseElements).not.toHaveBeenCalled();
      expect(RenderingService.emitter.emit).not.toHaveBeenCalledWith();
      expect(ScrollService.restorePosition).not.toHaveBeenCalled();
      expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
    });
  });
});
