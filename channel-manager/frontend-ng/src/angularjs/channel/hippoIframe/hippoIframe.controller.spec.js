/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import 'angular-mocks';

describe('hippoIframeCtrl', () => {
  let $q;
  let $rootScope;
  let ChannelService;
  let CmsService;
  let DialogService;
  let DomService;
  let DragDropService;
  let HippoIframeService;
  let OverlayService;
  let PageMetaDataService;
  let PageStructureService;
  let hippoIframeCtrl;
  let hstCommentsProcessorService;
  let scope;
  const iframeDom = {
    defaultView: window,
    location: {
      host: 'localhost',
      protocol: 'http:',
    },
  };

  beforeEach(() => {
    let $compile;
    angular.mock.module('hippo-cm');

    inject(($controller,
            _$compile_,
            _$q_,
            _$rootScope_,
            _ChannelService_,
            _ChannelSidePanelService_,
            _CmsService_,
            _DialogService_,
            _DomService_,
            _DragDropService_,
            _HippoIframeService_,
            _OverlayService_,
            _PageMetaDataService_,
            _PageStructureService_,
            _hstCommentsProcessorService_) => {
      $compile = _$compile_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      CmsService = _CmsService_;
      DialogService = _DialogService_;
      DomService = _DomService_;
      DragDropService = _DragDropService_;
      HippoIframeService = _HippoIframeService_;
      OverlayService = _OverlayService_;
      PageMetaDataService = _PageMetaDataService_;
      PageStructureService = _PageStructureService_;
      hstCommentsProcessorService = _hstCommentsProcessorService_;
      scope = $rootScope.$new();
    });

    spyOn(DomService, 'addCss').and.returnValue($q.resolve());
    spyOn(DragDropService, 'init');
    spyOn(OverlayService, 'init');
    spyOn(OverlayService, 'onEditMenu');
    spyOn(OverlayService, 'onEditContent');

    scope.testEditMode = false;
    scope.onEditMenu = jasmine.createSpy('onEditMenu');
    scope.onEditContent = jasmine.createSpy('onEditContent');

    const el = angular.element(
      `<hippo-iframe edit-mode="testEditMode" 
                    on-edit-content="onEditContent(contentUuid)" 
                    on-edit-menu="onEditMenu(menuUuid)">
      </hippo-iframe>`);
    $compile(el)(scope);
    scope.$digest();

    hippoIframeCtrl = el.controller('hippo-iframe');
    spyOn(hippoIframeCtrl, '_getIframeDom').and.returnValue(iframeDom);
  });

  it('unsubscribes "delete-component" event when the scope is destroyed', () => {
    spyOn(CmsService, 'unsubscribe');
    scope.$destroy();
    expect(CmsService.unsubscribe).toHaveBeenCalledWith('delete-component', jasmine.any(Function));
  });

  it('shows the confirmation dialog and deletes selected component on confirmation', () => {
    const mockComponent = jasmine.createSpyObj('ComponentElement', ['getLabel']);
    spyOn(DragDropService, 'replaceContainer');
    spyOn(PageStructureService, 'getComponentById').and.returnValue(mockComponent);
    spyOn(PageStructureService, 'removeComponentById').and.returnValue($q.when({ oldContainer: 'old', newContainer: 'new' }));
    spyOn(DialogService, 'show').and.returnValue($q.resolve());
    spyOn(DialogService, 'confirm').and.callThrough();

    hippoIframeCtrl.deleteComponent('1234');

    scope.$digest();

    expect(mockComponent.getLabel).toHaveBeenCalled();
    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(PageStructureService.removeComponentById).toHaveBeenCalledWith('1234');
    expect(DragDropService.replaceContainer).toHaveBeenCalledWith('old', 'new');
  });

  it('shows component properties dialog after rejecting the delete operation', () => {
    const mockComponent = jasmine.createSpyObj('ComponentElement', ['getLabel']);
    spyOn(PageStructureService, 'getComponentById').and.returnValue(mockComponent);
    spyOn(PageStructureService, 'showComponentProperties');
    spyOn(DialogService, 'show').and.returnValue($q.reject());
    spyOn(DialogService, 'confirm').and.callThrough();

    hippoIframeCtrl.deleteComponent('1234');

    scope.$digest();

    expect(mockComponent.getLabel).toHaveBeenCalled();
    expect(DialogService.confirm).toHaveBeenCalled();
    expect(DialogService.show).toHaveBeenCalled();
    expect(PageStructureService.showComponentProperties).toHaveBeenCalledWith(mockComponent);
  });

  it('switches channels when the channel id in the page meta-data differs from the current channel id', () => {
    const deferred = $q.defer();

    spyOn(PageStructureService, 'clearParsedElements');
    spyOn(hstCommentsProcessorService, 'run');
    spyOn(PageMetaDataService, 'getChannelId').and.returnValue('channelX');
    spyOn(ChannelService, 'getId').and.returnValue('channelY');
    spyOn(ChannelService, 'switchToChannel').and.returnValue(deferred.promise);
    spyOn(hippoIframeCtrl, '_parseLinks');
    spyOn(hippoIframeCtrl, '_updateDragDrop');
    spyOn(HippoIframeService, 'signalPageLoadCompleted');

    hippoIframeCtrl.onLoad();
    $rootScope.$digest();

    expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
    expect(hstCommentsProcessorService.run).toHaveBeenCalled();

    $rootScope.$digest();

    expect(hippoIframeCtrl._updateDragDrop).toHaveBeenCalled();
    expect(PageMetaDataService.getChannelId).toHaveBeenCalled();
    expect(ChannelService.getId).toHaveBeenCalled();
    expect(hippoIframeCtrl._parseLinks).not.toHaveBeenCalled();
    expect(HippoIframeService.signalPageLoadCompleted).not.toHaveBeenCalled();

    deferred.resolve();
    $rootScope.$digest();

    expect(hippoIframeCtrl._parseLinks).toHaveBeenCalled();
    expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
  });

  it('handles the loading of a new page', () => {
    spyOn(PageStructureService, 'clearParsedElements');
    spyOn(PageStructureService, 'attachEmbeddedLinks');
    spyOn(hstCommentsProcessorService, 'run');
    spyOn(ChannelService, 'getPreviewPaths').and.callThrough();
    spyOn(HippoIframeService, 'signalPageLoadCompleted');

    hippoIframeCtrl.onLoad();
    $rootScope.$digest();

    expect(DomService.addCss).toHaveBeenCalledWith(window, jasmine.any(String));
    expect(PageStructureService.clearParsedElements).toHaveBeenCalled();
    expect(hstCommentsProcessorService.run).toHaveBeenCalled();
    expect(PageStructureService.attachEmbeddedLinks).toHaveBeenCalled();
    expect(ChannelService.getPreviewPaths).toHaveBeenCalled();
    expect(HippoIframeService.signalPageLoadCompleted).toHaveBeenCalled();
  });

  it('enables/disables drag-drop when edit-mode is toggled', () => {
    const enableSpy = spyOn(DragDropService, 'enable').and.returnValue($q.resolve());
    const disableSpy = spyOn(DragDropService, 'disable');

    hippoIframeCtrl.editMode = true;
    $rootScope.$digest();

    expect(enableSpy).toHaveBeenCalled();
    expect(disableSpy).not.toHaveBeenCalled();

    enableSpy.calls.reset();
    hippoIframeCtrl.editMode = false;
    $rootScope.$digest();

    expect(enableSpy).not.toHaveBeenCalled();
    expect(disableSpy).toHaveBeenCalled();
  });

  it('attaches/detaches component mousedown handler when edit-mode is toggled', () => {
    spyOn(DragDropService, 'enable').and.returnValue($q.resolve());
    spyOn(DragDropService, 'disable');
    const attachSpy = spyOn(OverlayService, 'attachComponentMouseDown');
    const detachSpy = spyOn(OverlayService, 'detachComponentMouseDown');

    hippoIframeCtrl.editMode = true;
    $rootScope.$digest();

    expect(attachSpy).toHaveBeenCalled();
    expect(detachSpy).not.toHaveBeenCalled();

    attachSpy.calls.reset();
    hippoIframeCtrl.editMode = false;
    $rootScope.$digest();

    expect(attachSpy).not.toHaveBeenCalled();
    expect(detachSpy).toHaveBeenCalled();
  });

  it('calls its edit menu function when the overlay service wants to edit a menu', () => {
    const callback = OverlayService.onEditMenu.calls.mostRecent().args[0];
    callback('menu-uuid');
    expect(scope.onEditMenu).toHaveBeenCalledWith('menu-uuid');
  });

  it('opens right side panel when clicking the edit content button', () => {
    const callback = OverlayService.onEditContent.calls.mostRecent().args[0];
    callback('document-uuid');
    expect(scope.onEditContent).toHaveBeenCalledWith('document-uuid');
  });
});
