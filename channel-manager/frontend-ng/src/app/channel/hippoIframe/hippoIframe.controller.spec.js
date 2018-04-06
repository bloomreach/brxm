/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

describe('hippoIframeCtrl', () => {
  let $q;
  let $rootScope;
  let ChannelRenderingService;
  let CmsService;
  let DialogService;
  let DragDropService;
  let HippoIframeService;
  let OverlayService;
  let PageStructureService;
  let SpaService;
  let hippoIframeCtrl;
  let scope;

  beforeEach(() => {
    let $compile;
    angular.mock.module('hippo-cm');

    inject((
      $controller,
      _$compile_,
      _$q_,
      _$rootScope_,
      _ChannelRenderingService_,
      _CmsService_,
      _DialogService_,
      _DragDropService_,
      _HippoIframeService_,
      _OverlayService_,
      _PageStructureService_,
      _SpaService_,
    ) => {
      $compile = _$compile_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelRenderingService = _ChannelRenderingService_;
      CmsService = _CmsService_;
      DialogService = _DialogService_;
      DragDropService = _DragDropService_;
      HippoIframeService = _HippoIframeService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      SpaService = _SpaService_;
      scope = $rootScope.$new();
    });

    spyOn(OverlayService, 'onEditMenu');

    scope.testEditMode = false;
    scope.onEditMenu = jasmine.createSpy('onEditMenu');

    const el = angular.element(
      `<hippo-iframe show-components-overlay="false"
                     show-content-overlay="false"
                     on-edit-menu="onEditMenu(menuUuid)">
      </hippo-iframe>`);
    $compile(el)(scope);
    scope.$digest();

    hippoIframeCtrl = el.controller('hippo-iframe');
  });

  it('unsubscribes "delete-component" event when the controller is destroyed', () => {
    spyOn(CmsService, 'unsubscribe');
    hippoIframeCtrl.$onDestroy();
    expect(CmsService.unsubscribe).toHaveBeenCalledWith('delete-component', jasmine.any(Function), hippoIframeCtrl);
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

  it('creates the overlay when loading a new page', () => {
    spyOn(SpaService, 'detectSpa').and.returnValue(false);
    spyOn(ChannelRenderingService, 'createOverlay');

    hippoIframeCtrl.onLoad();
    $rootScope.$digest();

    expect(ChannelRenderingService.createOverlay).toHaveBeenCalled();
  });

  it('initializes the SPA when a SPA is detected', () => {
    spyOn(SpaService, 'detectSpa').and.returnValue(true);
    spyOn(SpaService, 'initSpa');

    hippoIframeCtrl.onLoad();
    $rootScope.$digest();

    expect(SpaService.initSpa).toHaveBeenCalled();
  });

  it('updates drag-drop when the components overlay is toggled and the iframe finished loading', () => {
    spyOn(ChannelRenderingService, 'updateDragDrop');

    HippoIframeService.pageLoaded = false;
    hippoIframeCtrl.showComponentsOverlay = true;
    $rootScope.$digest();

    expect(ChannelRenderingService.updateDragDrop).not.toHaveBeenCalled();

    hippoIframeCtrl.showComponentsOverlay = false;
    $rootScope.$digest();

    expect(ChannelRenderingService.updateDragDrop).not.toHaveBeenCalled();

    HippoIframeService.pageLoaded = true;
    hippoIframeCtrl.showComponentsOverlay = true;
    $rootScope.$digest();

    expect(ChannelRenderingService.updateDragDrop).toHaveBeenCalled();

    ChannelRenderingService.updateDragDrop.calls.reset();
    hippoIframeCtrl.showComponentsOverlay = false;
    $rootScope.$digest();

    expect(ChannelRenderingService.updateDragDrop).toHaveBeenCalled();
  });

  it('toggles the components overlay', () => {
    spyOn(OverlayService, 'showComponentsOverlay');

    hippoIframeCtrl.showComponentsOverlay = true;
    $rootScope.$digest();

    expect(OverlayService.showComponentsOverlay).toHaveBeenCalledWith(true);

    hippoIframeCtrl.showComponentsOverlay = false;
    $rootScope.$digest();

    expect(OverlayService.showComponentsOverlay).toHaveBeenCalledWith(false);
  });

  it('toggles the content overlay', () => {
    spyOn(OverlayService, 'showContentOverlay');

    hippoIframeCtrl.showContentOverlay = true;
    $rootScope.$digest();

    expect(OverlayService.showContentOverlay).toHaveBeenCalledWith(true);

    hippoIframeCtrl.showContentOverlay = false;
    $rootScope.$digest();

    expect(OverlayService.showContentOverlay).toHaveBeenCalledWith(false);
  });

  it('calls its edit menu function when the overlay service wants to edit a menu', () => {
    const callback = OverlayService.onEditMenu.calls.mostRecent().args[0];
    callback('menu-uuid');
    expect(scope.onEditMenu).toHaveBeenCalledWith('menu-uuid');
  });
});
