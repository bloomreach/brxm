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
  let ChannelService;
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
      _ChannelService_,
      _DomService_,
      _HippoIframeService_,
      _OverlayService_,
      _PageStructureService_,
      _RenderingService_,
      _ScrollService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
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
      spyOn(ScrollService, 'saveScrollPosition');
      spyOn(ScrollService, 'restoreScrollPosition');
    });

    it('handles the loading of a new page', () => {
      spyOn(DomService, 'hasCssLink').and.returnValue(false);
      spyOn(DomService, 'addCssLinks').and.returnValue($q.resolve());

      RenderingService.createOverlay();
      $rootScope.$digest();

      expect(DomService.addCssLinks).toHaveBeenCalledWith(window, [jasmine.any(String)], 'hippo-css');
      expect(ScrollService.saveScrollPosition).toHaveBeenCalled();
      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(OverlayService.clear).toHaveBeenCalled();
      expect(PageStructureService.parseElements).toHaveBeenCalledWith();
      expect(RenderingService.emitter.emit).toHaveBeenCalledWith('overlay-created');
      expect(ScrollService.restoreScrollPosition).toHaveBeenCalled();
      expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
    });

    it('handles the re-loading of an existing page', () => {
      spyOn(DomService, 'hasCssLink').and.returnValue(true);
      spyOn(DomService, 'addCssLinks');

      RenderingService.createOverlay();
      $rootScope.$digest();

      expect(DomService.addCssLinks).not.toHaveBeenCalled();
      expect(ScrollService.saveScrollPosition).toHaveBeenCalled();
      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(OverlayService.clear).toHaveBeenCalled();
      expect(PageStructureService.parseElements).toHaveBeenCalledWith();
      expect(RenderingService.emitter.emit).toHaveBeenCalledWith('overlay-created');
      expect(ScrollService.restoreScrollPosition).toHaveBeenCalled();
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

      expect(ScrollService.saveScrollPosition).toHaveBeenCalled();
      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(OverlayService.clear).toHaveBeenCalled();
      expect(PageStructureService.parseElements).not.toHaveBeenCalled();
      expect(RenderingService.emitter.emit).not.toHaveBeenCalledWith();
      expect(ScrollService.restoreScrollPosition).not.toHaveBeenCalled();
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
      expect(ScrollService.restoreScrollPosition).not.toHaveBeenCalled();
      expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
    });

    describe('channels switch', () => {
      let pageMeta;

      beforeEach(() => {
        spyOn(ChannelService, 'initializeChannel').and.returnValue($q.resolve());
        spyOn(ChannelService, 'getHostGroup').and.returnValue('theHostGroup');
        spyOn(ChannelService, 'getId');
        spyOn(DomService, 'addCssLinks').and.returnValue($q.resolve());
        spyOn(PageStructureService, 'getPage');
        spyOn(RenderingService, '_parseLinks');

        const page = jasmine.createSpyObj('page', ['getMeta']);
        pageMeta = jasmine.createSpyObj('pageMeta', ['getChannelId', 'getContextPath']);
        pageMeta.getContextPath.and.returnValue('/contextPathX');
        page.getMeta.and.returnValue(pageMeta);
        PageStructureService.getPage.and.returnValue(page);

        RenderingService.createOverlay();
      });

      it('switches channels when the channel id in the page meta-data differs from the current channel id', () => {
        pageMeta.getChannelId.and.returnValue('channelX');
        ChannelService.getId.and.returnValue('channelY');

        $rootScope.$digest();

        expect(ChannelService.initializeChannel).toHaveBeenCalledWith('channelX', '/contextPathX', 'theHostGroup');
      });

      it('does not switch channels when the channel id from the meta same to the current one', () => {
        pageMeta.getChannelId.and.returnValue('channelX');
        ChannelService.getId.and.returnValue('channelX');

        $rootScope.$digest();

        expect(ChannelService.initializeChannel).not.toHaveBeenCalled();
      });

      it('does not switch channels when there is no meta', () => {
        pageMeta.getChannelId.and.returnValue(undefined);
        ChannelService.getId.and.returnValue('channelX');

        $rootScope.$digest();

        expect(ChannelService.initializeChannel).not.toHaveBeenCalled();
      });
    });
  });
});
