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

describe('ChannelRenderingService', () => {
  let $q;
  let $rootScope;
  let ChannelService;
  let DomService;
  let DragDropService;
  let HippoIframeService;
  let OverlayService;
  let PageMetaDataService;
  let PageStructureService;
  let ProjectService;
  let ChannelRenderingService;
  let HstCommentsProcessorService;

  const iframeDom = {
    defaultView: window,
    location: {
      host: 'localhost',
      protocol: 'http:',
    },
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$q_,
      _$rootScope_,
      _ChannelRenderingService_,
      _ChannelService_,
      _DomService_,
      _DragDropService_,
      _HippoIframeService_,
      _HstCommentsProcessorService_,
      _OverlayService_,
      _PageMetaDataService_,
      _PageStructureService_,
      _ProjectService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelRenderingService = _ChannelRenderingService_;
      ChannelService = _ChannelService_;
      DomService = _DomService_;
      DragDropService = _DragDropService_;
      HippoIframeService = _HippoIframeService_;
      HstCommentsProcessorService = _HstCommentsProcessorService_;
      OverlayService = _OverlayService_;
      PageMetaDataService = _PageMetaDataService_;
      PageStructureService = _PageStructureService_;
      ProjectService = _ProjectService_;
    });

    spyOn(ChannelRenderingService, '_getIframeDom').and.returnValue(iframeDom);
  });

  describe('createOverlay', () => {
    beforeEach(() => {
      spyOn(PageStructureService, 'clearParsedElements');
      spyOn(PageStructureService, 'attachEmbeddedLinks');
      spyOn(HstCommentsProcessorService, 'run');
      spyOn(ChannelRenderingService, 'updateDragDrop');
      spyOn(ChannelService, 'getPreviewPaths').and.callThrough();
      spyOn(HippoIframeService, 'signalPageLoadCompleted');
    });

    it('handles the loading of a new page', () => {
      spyOn(DomService, 'addCss');

      ChannelRenderingService.createOverlay();
      $rootScope.$digest();

      expect(DomService.addCss).toHaveBeenCalledWith(window, jasmine.any(String));
      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(HstCommentsProcessorService.run).toHaveBeenCalledWith(iframeDom, jasmine.any(Function));
      expect(PageStructureService.attachEmbeddedLinks).toHaveBeenCalled();
      expect(ChannelRenderingService.updateDragDrop).toHaveBeenCalled();
      expect(ChannelService.getPreviewPaths).toHaveBeenCalled();
      expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
    });

    it('clears the parsed elements, then stops when loading the hippo-iframe CSS file throws an error', () => {
      spyOn(DomService, 'addCss').and.throwError();

      ChannelRenderingService.createOverlay();
      $rootScope.$digest();

      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(HstCommentsProcessorService.run).not.toHaveBeenCalled();
      expect(PageStructureService.attachEmbeddedLinks).not.toHaveBeenCalled();
      expect(ChannelRenderingService.updateDragDrop).not.toHaveBeenCalled();
      expect(ChannelService.getPreviewPaths).not.toHaveBeenCalled();
      expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
    });

    it('clears the parsed elements, then stops if the iframe DOM is not present', () => {
      spyOn(ChannelRenderingService, '_isIframeDomPresent').and.returnValue(false);

      ChannelRenderingService.createOverlay();
      $rootScope.$digest();

      expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
      expect(HstCommentsProcessorService.run).not.toHaveBeenCalled();
      expect(PageStructureService.attachEmbeddedLinks).not.toHaveBeenCalled();
      expect(ChannelRenderingService.updateDragDrop).not.toHaveBeenCalled();
      expect(ChannelService.getPreviewPaths).not.toHaveBeenCalled();

      // TODO: is this intentional?
      expect(HippoIframeService.signalPageLoadCompleted).not.toHaveBeenCalled();
    });

    it('switches channels when the channel id in the page meta-data differs from the current channel id', () => {
      const deferred = $q.defer();

      spyOn(ChannelService, 'loadChannel').and.returnValue(deferred.promise);
      spyOn(ChannelRenderingService, '_parseLinks');
      spyOn(ProjectService, 'getBaseChannelId').and.callFake(channelId => channelId);

      spyOn(PageMetaDataService, 'getChannelId').and.returnValue('channelX');
      spyOn(ChannelService, 'getId').and.returnValue('channelY');

      ChannelRenderingService.createOverlay();
      $rootScope.$digest();

      expect(HstCommentsProcessorService.run).toHaveBeenCalled();

      $rootScope.$digest();

      expect(ChannelRenderingService.updateDragDrop).toHaveBeenCalled();
      expect(PageMetaDataService.getChannelId).toHaveBeenCalled();
      expect(ChannelService.getId).toHaveBeenCalled();

      deferred.resolve();
      $rootScope.$digest();

      expect(ChannelRenderingService._parseLinks).toHaveBeenCalled();
      expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
    });
  });

  describe('updateDragDrop', () => {
    beforeEach(() => {
      spyOn(DragDropService, 'enable').and.returnValue($q.resolve());
      spyOn(DragDropService, 'disable');
    });

    it('enables/disables drag-drop when the components overlay is toggled', () => {
      OverlayService.isComponentsOverlayDisplayed = true;
      ChannelRenderingService.updateDragDrop();
      $rootScope.$digest();

      expect(DragDropService.enable).toHaveBeenCalled();
      expect(DragDropService.disable).not.toHaveBeenCalled();

      DragDropService.enable.calls.reset();
      OverlayService.isComponentsOverlayDisplayed = false;
      ChannelRenderingService.updateDragDrop();
      $rootScope.$digest();

      expect(DragDropService.enable).not.toHaveBeenCalled();
      expect(DragDropService.disable).toHaveBeenCalled();
    });

    it('attaches/detaches component mousedown handler when the components overlay is toggled', () => {
      spyOn(OverlayService, 'attachComponentMouseDown');
      spyOn(OverlayService, 'detachComponentMouseDown');

      OverlayService.isComponentsOverlayDisplayed = true;
      ChannelRenderingService.updateDragDrop();
      $rootScope.$digest();

      expect(OverlayService.attachComponentMouseDown).toHaveBeenCalled();
      expect(OverlayService.detachComponentMouseDown).not.toHaveBeenCalled();

      OverlayService.attachComponentMouseDown.calls.reset();
      OverlayService.isComponentsOverlayDisplayed = false;
      ChannelRenderingService.updateDragDrop();
      $rootScope.$digest();

      expect(OverlayService.attachComponentMouseDown).not.toHaveBeenCalled();
      expect(OverlayService.detachComponentMouseDown).toHaveBeenCalled();
    });
  });
});
