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
  let $rootScope;
  let $window;
  let CmsService;
  let ContainerService;
  let HippoIframeService;
  let OverlayService;
  let PageStructureService;
  let RenderingService;
  let SpaService;
  let hippoIframeCtrl;
  let scope;

  beforeEach(() => {
    let $compile;
    angular.mock.module('hippo-cm');

    inject((
      $controller,
      _$compile_,
      _$rootScope_,
      _$window_,
      _CmsService_,
      _ContainerService_,
      _HippoIframeService_,
      _OverlayService_,
      _PageStructureService_,
      _RenderingService_,
      _SpaService_,
    ) => {
      $compile = _$compile_;
      $rootScope = _$rootScope_;
      $window = _$window_;
      CmsService = _CmsService_;
      ContainerService = _ContainerService_;
      HippoIframeService = _HippoIframeService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      RenderingService = _RenderingService_;
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

  describe('render component', () => {
    beforeEach(() => {
      spyOn(SpaService, 'renderComponent');
      spyOn(PageStructureService, 'renderComponent');
    });

    it('first tries to render a component via the SPA', () => {
      SpaService.renderComponent.and.returnValue(true);

      $window.CMS_TO_APP.publish('render-component', '1234', { foo: 1 });

      expect(SpaService.renderComponent).toHaveBeenCalledWith('1234', { foo: 1 });
      expect(PageStructureService.renderComponent).not.toHaveBeenCalled();
    });

    it('second renders a component via the PageStructureService', () => {
      SpaService.renderComponent.and.returnValue(false);

      $window.CMS_TO_APP.publish('render-component', '1234', { foo: 1 });

      expect(SpaService.renderComponent).toHaveBeenCalledWith('1234', { foo: 1 });
      expect(PageStructureService.renderComponent).toHaveBeenCalledWith('1234', { foo: 1 });
    });

    it('does not respond to the render-component event anymore when destroyed', () => {
      hippoIframeCtrl.$onDestroy();
      $window.CMS_TO_APP.publish('render-component', '1234', { foo: 1 });

      expect(SpaService.renderComponent).not.toHaveBeenCalled();
      expect(PageStructureService.renderComponent).not.toHaveBeenCalled();
    });
  });

  describe('delete component', () => {
    beforeEach(() => {
      spyOn(ContainerService, 'deleteComponent');
    });

    it('deletes a component via the ContainerService', () => {
      $window.CMS_TO_APP.publish('delete-component', '1234');
      expect(ContainerService.deleteComponent).toHaveBeenCalledWith('1234');
    });

    it('does not respond to the delete-component event anymore when destroyed', () => {
      hippoIframeCtrl.$onDestroy();
      $window.CMS_TO_APP.publish('delete-component', '1234');
      expect(ContainerService.deleteComponent).not.toHaveBeenCalled();
    });
  });

  it('unsubscribes "delete-component" event when the controller is destroyed', () => {
    spyOn(CmsService, 'unsubscribe');
    hippoIframeCtrl.$onDestroy();
    expect(CmsService.unsubscribe).toHaveBeenCalledWith('delete-component', jasmine.any(Function), hippoIframeCtrl);
  });

  it('creates the overlay when loading a new page', () => {
    spyOn(SpaService, 'detectSpa').and.returnValue(false);
    spyOn(RenderingService, 'createOverlay');

    hippoIframeCtrl.onLoad();
    $rootScope.$digest();

    expect(RenderingService.createOverlay).toHaveBeenCalled();
  });

  it('initializes the SPA when a SPA is detected', () => {
    spyOn(SpaService, 'detectSpa').and.returnValue(true);
    spyOn(SpaService, 'initSpa');

    hippoIframeCtrl.onLoad();
    $rootScope.$digest();

    expect(SpaService.initSpa).toHaveBeenCalled();
  });

  it('updates drag-drop when the components overlay is toggled and the iframe finished loading', () => {
    spyOn(RenderingService, 'updateDragDrop');

    HippoIframeService.pageLoaded = false;
    hippoIframeCtrl.showComponentsOverlay = true;
    $rootScope.$digest();

    expect(RenderingService.updateDragDrop).not.toHaveBeenCalled();

    hippoIframeCtrl.showComponentsOverlay = false;
    $rootScope.$digest();

    expect(RenderingService.updateDragDrop).not.toHaveBeenCalled();

    HippoIframeService.pageLoaded = true;
    hippoIframeCtrl.showComponentsOverlay = true;
    $rootScope.$digest();

    expect(RenderingService.updateDragDrop).toHaveBeenCalled();

    RenderingService.updateDragDrop.calls.reset();
    hippoIframeCtrl.showComponentsOverlay = false;
    $rootScope.$digest();

    expect(RenderingService.updateDragDrop).toHaveBeenCalled();
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
