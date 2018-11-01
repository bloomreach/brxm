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
  let $q;
  let $rootScope;
  let $window;
  let CmsService;
  let ComponentRenderingService;
  let ContainerService;
  let DragDropService;
  let EditComponentService;
  let FeedbackService;
  let HippoIframeService;
  let HstComponentService;
  let OverlayService;
  let PageStructureService;
  let PickerService;
  let RenderingService;
  let SpaService;
  let ViewportService;
  let $ctrl;
  let onEditMenu;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ComponentRenderingService = jasmine.createSpyObj('ComponentRenderingService', ['renderComponent']);
    EditComponentService = jasmine.createSpyObj('EditComponentService', ['startEditing']);
    FeedbackService = jasmine.createSpyObj('FeedbackService', ['showErrorResponse', 'showNotification']);
    HstComponentService = jasmine.createSpyObj('HstComponentService', ['setPathParameter']);
    PickerService = jasmine.createSpyObj('PickerService', ['pickPath']);

    angular.mock.module(($provide) => {
      $provide.value('ComponentRenderingService', ComponentRenderingService);
      $provide.value('EditComponentService', EditComponentService);
      $provide.value('FeedbackService', FeedbackService);
      $provide.value('HstComponentService', HstComponentService);
      $provide.value('PickerService', PickerService);
    });

    inject((
      $componentController,
      _$compile_,
      _$q_,
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
      $q = _$q_;
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
      spyOn(OverlayService, 'onSelectDocument');
      spyOn(DragDropService, 'onClick').and.callThrough();
      spyOn(DragDropService, 'onDrop').and.callThrough();
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

  describe('click component', () => {
    it('starts editing a component when it receives an "onClick" event from the DragDropService', () => {
      const onClickHandler = DragDropService.onClick.calls.mostRecent().args[0];
      onClickHandler({ id: 'testId' });

      expect(EditComponentService.startEditing).toHaveBeenCalledWith({ id: 'testId' });
    });

    it('removes the on-click event handler when destroyed', () => {
      const unbind = jasmine.createSpy('unbind');
      DragDropService.onClick.and.returnValue(unbind);

      $ctrl.$onInit();
      $ctrl.$onDestroy();

      expect(unbind).toHaveBeenCalled();
    });
  });

  describe('render component', () => {
    it('renders a component when it receives a "render-component" event from the CMS', () => {
      $window.CMS_TO_APP.publish('render-component', '1234', { foo: 1 });
      expect(ComponentRenderingService.renderComponent).toHaveBeenCalledWith('1234', { foo: 1 });
    });

    it('does not respond to the render-component event anymore when destroyed', () => {
      $ctrl.$onDestroy();
      $window.CMS_TO_APP.publish('render-component', '1234', { foo: 1 });
      expect(ComponentRenderingService.renderComponent).not.toHaveBeenCalled();
    });
  });

  describe('move component', () => {
    it('moves a component via the ContainerService', () => {
      spyOn(ContainerService, 'moveComponent').and.returnValue($q.resolve());

      const onDropHandler = DragDropService.onDrop.calls.mostRecent().args[0];
      const component = {};
      const targetContainer = {};
      const targetContainerNextComponent = {};
      onDropHandler([component, targetContainer, targetContainerNextComponent]);

      expect(ContainerService.moveComponent).toHaveBeenCalledWith(component, targetContainer, targetContainerNextComponent);
    });

    it('removes the on-drop event handler when destroyed', () => {
      const unbind = jasmine.createSpy('unbind');
      DragDropService.onDrop.and.returnValue(unbind);

      $ctrl.$onInit();
      $ctrl.$onDestroy();

      expect(unbind).toHaveBeenCalled();
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
    spyOn(RenderingService, 'createOverlay').and.returnValue($q.resolve());

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

  describe('onSelectDocument', () => {
    let component;
    let pickerConfig;
    let onSelectDocument;

    beforeEach(() => {
      component = {
        getId: () => 'componentId',
        getLabel: () => 'componentLabel',
        getRenderVariant: () => 'hippo-default',
      };
      pickerConfig = {};
      onSelectDocument = OverlayService.onSelectDocument.calls.mostRecent().args[0];
    });

    it('can pick a path and update the component', (done) => {
      PickerService.pickPath.and.returnValue($q.resolve({ path: '/base/pickedPath' }));
      HstComponentService.setPathParameter.and.returnValue($q.resolve());

      onSelectDocument(component, 'parameterName', '/base/currentPath', pickerConfig, '/base')
        .then(() => {
          expect(PickerService.pickPath).toHaveBeenCalledWith(pickerConfig, '/base/currentPath');
          expect(HstComponentService.setPathParameter).toHaveBeenCalledWith(
            'componentId', 'hippo-default', 'parameterName', '/base/pickedPath', '/base',
          );
          expect(ComponentRenderingService.renderComponent).toHaveBeenCalledWith('componentId');
          expect(FeedbackService.showNotification).toHaveBeenCalledWith('NOTIFICATION_DOCUMENT_SELECTED_FOR_COMPONENT', {
            componentName: 'componentLabel',
          });

          done();
        });
      $rootScope.$digest();
    });

    it('can pick a path but fail to update the component', (done) => {
      const errorData = {};
      PickerService.pickPath.and.returnValue($q.resolve({ path: '/base/pickedPath' }));
      HstComponentService.setPathParameter.and.returnValue($q.reject({ data: errorData }));
      spyOn(HippoIframeService, 'reload');

      onSelectDocument(component, 'parameterName', '/base/currentPath', pickerConfig, '/base')
        .then(() => {
          expect(PickerService.pickPath).toHaveBeenCalledWith(pickerConfig, '/base/currentPath');
          expect(HstComponentService.setPathParameter).toHaveBeenCalledWith(
            'componentId', 'hippo-default', 'parameterName', '/base/pickedPath', '/base',
          );
          expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(errorData, 'ERROR_DOCUMENT_SELECTED_FOR_COMPONENT',
            jasmine.any(Object), { componentName: 'componentLabel' });
          expect(HippoIframeService.reload).toHaveBeenCalled();

          done();
        });
      $rootScope.$digest();
    });
  });
});
