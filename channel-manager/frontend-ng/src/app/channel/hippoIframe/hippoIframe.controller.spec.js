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
  let $element;
  let $rootScope;
  let $window;
  let CmsService;
  let ContainerService;
  let DragDropService;
  let HippoIframeService;
  let OverlayService;
  let PageStructureService;
  let RenderingService;
  let SpaService;
  let ViewportService;
  let $ctrl;
  let onEditMenu;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      $componentController,
      _$compile_,
      _$rootScope_,
      _$window_,
      _CmsService_,
      _ContainerService_,
      _DragDropService_,
      _HippoIframeService_,
      _OverlayService_,
      _PageStructureService_,
      _RenderingService_,
      _SpaService_,
      _ViewportService_,
    ) => {
      $rootScope = _$rootScope_;
      $window = _$window_;
      CmsService = _CmsService_;
      ContainerService = _ContainerService_;
      DragDropService = _DragDropService_;
      HippoIframeService = _HippoIframeService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      RenderingService = _RenderingService_;
      SpaService = _SpaService_;
      ViewportService = _ViewportService_;

      $element = angular.element('<div></div>');

      spyOn(OverlayService, 'onEditMenu');
      spyOn(DragDropService, 'onDrop');
      onEditMenu = jasmine.createSpy('onEditMenu');

      $ctrl = $componentController('hippoIframe', {
        $element,
        CmsService,
        ContainerService,
        DragDropService,
        HippoIframeService,
        OverlayService,
        PageStructureService,
        RenderingService,
        SpaService,
        ViewportService,
      }, {
        showComponentsOverlay: false,
        showContentOverlay: false,
        onEditMenu,
      });

      $ctrl.$onInit();
    });
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
      $ctrl.$onDestroy();
      $window.CMS_TO_APP.publish('render-component', '1234', { foo: 1 });

      expect(SpaService.renderComponent).not.toHaveBeenCalled();
      expect(PageStructureService.renderComponent).not.toHaveBeenCalled();
    });
  });

  describe('move component', () => {
    let callback;

    beforeEach(() => {
      spyOn(ContainerService, 'moveComponent');
      callback = DragDropService.onDrop.calls.mostRecent().args[0];
    });

    it('moves a component via the ContainerService', () => {
      const component = {};
      const targetContainer = {};
      const targetContainerNextComponent = {};
      callback(component, targetContainer, targetContainerNextComponent);
      expect(ContainerService.moveComponent).toHaveBeenCalledWith(component, targetContainer, targetContainerNextComponent);
    });

    it('does not call the on-drop callback anymore when destroyed', () => {
      spyOn(DragDropService, 'offDrop');
      $ctrl.$onDestroy();
      expect(DragDropService.offDrop).toHaveBeenCalled();
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
      $ctrl.$onDestroy();
      $window.CMS_TO_APP.publish('delete-component', '1234');
      expect(ContainerService.deleteComponent).not.toHaveBeenCalled();
    });
  });

  it('unsubscribes "delete-component" event when the controller is destroyed', () => {
    spyOn(CmsService, 'unsubscribe');
    $ctrl.$onDestroy();
    expect(CmsService.unsubscribe).toHaveBeenCalledWith('delete-component', jasmine.any(Function), $ctrl);
  });

  it('creates the overlay when loading a new page', () => {
    spyOn(SpaService, 'detectSpa').and.returnValue(false);
    spyOn(RenderingService, 'createOverlay');

    $ctrl.onLoad();
    $rootScope.$digest();

    expect(RenderingService.createOverlay).toHaveBeenCalled();
  });

  it('initializes the SPA when a SPA is detected', () => {
    spyOn(SpaService, 'detectSpa').and.returnValue(true);
    spyOn(SpaService, 'initSpa');

    $ctrl.onLoad();
    $rootScope.$digest();

    expect(SpaService.initSpa).toHaveBeenCalled();
  });

  it('updates drag-drop when the components overlay is toggled and the iframe finished loading', () => {
    spyOn(RenderingService, 'updateDragDrop');

    HippoIframeService.pageLoaded = false;
    $ctrl.$onChanges({
      showComponentsOverlay: { currentValue: true },
    });
    expect(RenderingService.updateDragDrop).not.toHaveBeenCalled();

    $ctrl.$onChanges({
      showComponentsOverlay: { currentValue: false },
    });
    expect(RenderingService.updateDragDrop).not.toHaveBeenCalled();

    HippoIframeService.pageLoaded = true;
    $ctrl.$onChanges({
      showComponentsOverlay: { currentValue: true },
    });
    expect(RenderingService.updateDragDrop).toHaveBeenCalled();

    RenderingService.updateDragDrop.calls.reset();
    $ctrl.$onChanges({
      showComponentsOverlay: { currentValue: false },
    });
    expect(RenderingService.updateDragDrop).toHaveBeenCalled();
  });

  it('toggles the components overlay', () => {
    spyOn(OverlayService, 'showComponentsOverlay');

    $ctrl.$onChanges({
      showComponentsOverlay: { currentValue: true },
    });
    expect(OverlayService.showComponentsOverlay).toHaveBeenCalledWith(true);

    $ctrl.$onChanges({
      showComponentsOverlay: { currentValue: false },
    });
    expect(OverlayService.showComponentsOverlay).toHaveBeenCalledWith(false);
  });

  it('toggles the content overlay', () => {
    spyOn(OverlayService, 'showContentOverlay');

    $ctrl.$onChanges({
      showContentOverlay: { currentValue: true },
    });
    expect(OverlayService.showContentOverlay).toHaveBeenCalledWith(true);

    $ctrl.$onChanges({
      showContentOverlay: { currentValue: false },
    });
    expect(OverlayService.showContentOverlay).toHaveBeenCalledWith(false);
  });

  it('calls its edit menu function when the overlay service wants to edit a menu', () => {
    const callback = OverlayService.onEditMenu.calls.mostRecent().args[0];
    callback('menu-uuid');
    expect(onEditMenu).toHaveBeenCalledWith({ menuUuid: 'menu-uuid' });
  });
});
