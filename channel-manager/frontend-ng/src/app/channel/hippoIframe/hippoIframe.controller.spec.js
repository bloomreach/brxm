/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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
  let CommunicationService;
  let ComponentRenderingService;
  let ContainerService;
  let CreateContentService;
  let DomService;
  let EditComponentService;
  let EditContentService;
  let FeedbackService;
  let HippoIframeService;
  let HstComponentService;
  let OverlayService;
  let PageStructureService;
  let PickerService;
  let RenderingService;
  let ScrollService;
  let SpaService;
  let ViewportService;
  let $ctrl;
  let onEditMenu;
  let contentWindow;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ComponentRenderingService = jasmine.createSpyObj('ComponentRenderingService', ['renderComponent']);
    CreateContentService = jasmine.createSpyObj('CreateContentService', ['start']);
    DomService = jasmine.createSpyObj('DomService', ['addScript', 'isFrameAccessible']);
    EditComponentService = jasmine.createSpyObj('EditComponentService', ['startEditing']);
    EditContentService = jasmine.createSpyObj('EditContentService', ['startEditing']);
    FeedbackService = jasmine.createSpyObj('FeedbackService', ['showErrorResponse', 'showNotification']);
    HstComponentService = jasmine.createSpyObj('HstComponentService', ['setPathParameter']);
    PickerService = jasmine.createSpyObj('PickerService', ['pickPath']);
    ScrollService = jasmine.createSpyObj('ScrollService', ['enable', 'disable', 'init']);

    angular.mock.module(($provide) => {
      $provide.value('ComponentRenderingService', ComponentRenderingService);
      $provide.value('CreateContentService', CreateContentService);
      $provide.value('DomService', DomService);
      $provide.value('EditComponentService', EditComponentService);
      $provide.value('EditContentService', EditContentService);
      $provide.value('FeedbackService', FeedbackService);
      $provide.value('HstComponentService', HstComponentService);
      $provide.value('PickerService', PickerService);
      $provide.value('ScrollService', ScrollService);
    });

    inject((
      $componentController,
      _$compile_,
      _$q_,
      _$rootScope_,
      _$window_,
      _CmsService_,
      _CommunicationService_,
      _ContainerService_,
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
      CommunicationService = _CommunicationService_;
      ContainerService = _ContainerService_;
      HippoIframeService = _HippoIframeService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      RenderingService = _RenderingService_;
      SpaService = _SpaService_;
      ViewportService = _ViewportService_;

      $element = angular.element(`<div>
        <div class="channel-iframe-canvas">
          <iframe />
        </div>
      </div>`);

      spyOn(CommunicationService, 'connect').and.returnValue($q.resolve());
      spyOn(HippoIframeService, 'getAssetUrl').and.returnValue('url');
      onEditMenu = jasmine.createSpy('onEditMenu');
      contentWindow = {
        document: {
          body: { innerHTML: 'something' },
        },
      };

      $ctrl = $componentController('hippoIframe', {
        $element,
        CmsService,
        ContainerService,
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
      Object.defineProperty($ctrl.iframeJQueryElement[0], 'contentWindow', { value: contentWindow });
    });
  });

  describe('click component', () => {
    it('should start editing a component on component:click event', () => {
      const component = { id: 'testId' };
      spyOn(PageStructureService, 'getPage').and.returnValue({
        getComponentById: () => component,
      });
      $rootScope.$emit('iframe:component:click', 'testId');

      expect(EditComponentService.startEditing).toHaveBeenCalledWith({ id: 'testId' });
    });

    it('should not start editing a component if the page is not ready', () => {
      spyOn(PageStructureService, 'getPage');
      $rootScope.$emit('iframe:component:click', 'testId');

      expect(EditComponentService.startEditing).not.toHaveBeenCalled();
    });

    it('should not start editing a component if the component is not on the page', () => {
      spyOn(PageStructureService, 'getPage').and.returnValue({
        getComponentById: () => {},
      });
      $rootScope.$emit('iframe:component:click', 'testId');

      expect(EditComponentService.startEditing).not.toHaveBeenCalled();
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

      const component = {};
      const container = {};

      spyOn(PageStructureService, 'getPage').and.returnValue({
        getComponentById: jasmine.createSpy().and.returnValue(component),
        getContainerById: jasmine.createSpy().and.returnValue(container),
      });

      $rootScope.$emit('iframe:component:move', {
        componentId: 'component-id',
        containerId: 'container-id',
        nextComponentId: 'next-component-id',
      });
      $rootScope.$digest();

      expect(ContainerService.moveComponent).toHaveBeenCalledWith(component, container, component);
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

  it('initiates a connection with the iframe bundle', () => {
    DomService.isFrameAccessible.and.returnValue(true);
    $ctrl.onLoad();
    $rootScope.$digest();

    expect(CommunicationService.connect).toHaveBeenCalledWith(jasmine.objectContaining({
      target: $ctrl.iframeJQueryElement[0],
    }));
  });

  it('injects the iframe bundle into the iframe', () => {
    DomService.isFrameAccessible.and.returnValue(true);

    $ctrl.onLoad();
    $rootScope.$digest();

    expect(HippoIframeService.getAssetUrl).toHaveBeenCalledWith(jasmine.stringMatching('iframe'));
    expect(DomService.addScript).toHaveBeenCalledWith(contentWindow, 'url');
  });

  it('does not inject the iframe bundle into a cross-origin iframe', () => {
    DomService.isFrameAccessible.and.returnValue(false);

    $ctrl.onLoad();
    $rootScope.$digest();

    expect(DomService.addScript).not.toHaveBeenCalled();
  });

  it('creates the overlay when loading a new page', () => {
    spyOn($rootScope, '$emit');
    spyOn(SpaService, 'initLegacy').and.returnValue(false);
    spyOn(RenderingService, 'createOverlay').and.returnValue($q.resolve());
    DomService.isFrameAccessible.and.returnValue(true);

    $ctrl.onLoad();
    $rootScope.$digest();

    expect(RenderingService.createOverlay).toHaveBeenCalledWith(true);
  });

  it('triggers an event when page is loaded', () => {
    spyOn($rootScope, '$emit');
    spyOn(SpaService, 'initLegacy').and.returnValue(true);
    DomService.isFrameAccessible.and.returnValue(true);

    $ctrl.onLoad();
    $rootScope.$digest();

    expect($rootScope.$emit).toHaveBeenCalledWith('hippo-iframe:load');
  });

  it('reloads the iframe when it receives a "hippo-iframe:new-head-contributions" event', () => {
    spyOn(HippoIframeService, 'reload');
    const mockComponent = jasmine.createSpyObj('ComponentElement', ['getLabel']);
    $rootScope.$emit('hippo-iframe:new-head-contributions', mockComponent);

    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('initializes the legacy SPA integration', () => {
    spyOn($rootScope, '$emit');
    spyOn(SpaService, 'initLegacy').and.returnValue(true);
    DomService.isFrameAccessible.and.returnValue(true);

    $ctrl.onLoad();
    $rootScope.$digest();

    expect(SpaService.initLegacy).toHaveBeenCalled();
  });

  it('initiates a connection with the iframe bundle when the SPA SDK is ready', () => {
    spyOn(SpaService, 'getOrigin').and.returnValue('http://localhost:3000');
    DomService.isFrameAccessible.and.returnValue(false);

    $rootScope.$emit('spa:ready');
    $rootScope.$digest();

    expect(CommunicationService.connect).toHaveBeenCalledWith(jasmine.objectContaining({
      origin: 'http://localhost:3000',
      target: $ctrl.iframeJQueryElement[0],
    }));
  });

  it('injects the iframe bundle when the SPA SDK is ready', () => {
    spyOn(SpaService, 'inject');
    DomService.isFrameAccessible.and.returnValue(false);

    $rootScope.$emit('spa:ready');
    $rootScope.$digest();

    expect(HippoIframeService.getAssetUrl).toHaveBeenCalledWith(jasmine.stringMatching('iframe'));
    expect(SpaService.inject).toHaveBeenCalledWith('url');
  });

  it('triggers an event when the SPA SDK is ready', () => {
    spyOn(SpaService, 'inject');
    DomService.isFrameAccessible.and.returnValue(false);

    $rootScope.$emit('spa:ready');
    spyOn($rootScope, '$emit');
    $rootScope.$digest();

    expect($rootScope.$emit).toHaveBeenCalledWith('hippo-iframe:load');
  });

  it('does not trigger an event when the iframe has the same origin', () => {
    DomService.isFrameAccessible.and.returnValue(true);

    const listener = jasmine.createSpy('listener');

    $rootScope.$on('hippo-iframe:load', listener);
    $rootScope.$emit('spa:ready');

    $rootScope.$digest();

    expect(listener).not.toHaveBeenCalled();
  });

  it('disconnects with the iframe bundle on the iframe unload', () => {
    spyOn(CommunicationService, 'disconnect');
    $rootScope.$emit('iframe:unload');
    $rootScope.$digest();

    expect(CommunicationService.disconnect).toHaveBeenCalled();
  });

  it('disables scrolling on the iframe unload', () => {
    $rootScope.$emit('iframe:unload');

    expect(ScrollService.disable).toHaveBeenCalled();
  });

  it('disconnects with the iframe bundle on the component destruction', () => {
    spyOn(CommunicationService, 'disconnect');
    $ctrl.$onDestroy();

    expect(CommunicationService.disconnect).toHaveBeenCalled();
  });

  it('destroys an SPA integration on the component destruction', () => {
    spyOn(SpaService, 'destroy');
    $ctrl.$onDestroy();

    expect(SpaService.destroy).toHaveBeenCalled();
  });

  it('toggles the components overlay', () => {
    spyOn(OverlayService, 'toggleComponentsOverlay');

    $ctrl.$onChanges({
      showComponentsOverlay: { currentValue: true },
    });
    expect(OverlayService.toggleComponentsOverlay).toHaveBeenCalledWith(true);

    $ctrl.$onChanges({
      showComponentsOverlay: { currentValue: false },
    });
    expect(OverlayService.toggleComponentsOverlay).toHaveBeenCalledWith(false);
  });

  it('toggles the content overlay', () => {
    spyOn(OverlayService, 'toggleContentsOverlay');

    $ctrl.$onChanges({
      showContentOverlay: { currentValue: true },
    });
    expect(OverlayService.toggleContentsOverlay).toHaveBeenCalledWith(true);

    $ctrl.$onChanges({
      showContentOverlay: { currentValue: false },
    });
    expect(OverlayService.toggleContentsOverlay).toHaveBeenCalledWith(false);
  });

  it('calls its edit menu function on menu:edit event', () => {
    $rootScope.$emit('menu:edit', 'menu-uuid');

    expect(onEditMenu).toHaveBeenCalledWith({ menuUuid: 'menu-uuid' });
  });

  it('should start create content service on document:create event', () => {
    const data = {};
    $rootScope.$emit('document:create', data);

    expect(CreateContentService.start).toHaveBeenCalledWith(data);
  });

  it('should start create content service on document:edit event', () => {
    spyOn(CmsService, 'reportUsageStatistic');
    $rootScope.$emit('document:edit', 'content-uuid');

    expect(EditContentService.startEditing).toHaveBeenCalledWith('content-uuid');
    expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsEditContent');
  });

  describe('_onDocumentSelect', () => {
    const eventData = {
      containerItem: {
        getId: () => 'componentId',
        getLabel: () => 'componentLabel',
        getRenderVariant: () => 'hippo-default',
      },
      parameterName: 'parameterName',
      parameterValue: '/base/currentPath',
      parameterBasePath: '/base',
      pickerConfig: {},
    };

    beforeEach(() => {
      spyOn(CmsService, 'reportUsageStatistic');
    });

    it('can pick a path and update the component', () => {
      PickerService.pickPath.and.returnValue($q.resolve({ path: '/base/pickedPath' }));
      HstComponentService.setPathParameter.and.returnValue($q.resolve());

      $rootScope.$emit('document:select', eventData);
      $rootScope.$digest();

      expect(PickerService.pickPath).toHaveBeenCalledWith(eventData.pickerConfig, '/base/currentPath');
      expect(HstComponentService.setPathParameter).toHaveBeenCalledWith(
        'componentId', 'hippo-default', 'parameterName', '/base/pickedPath', '/base',
      );
      expect(ComponentRenderingService.renderComponent).toHaveBeenCalledWith('componentId');
      expect(FeedbackService.showNotification).toHaveBeenCalledWith(
        'NOTIFICATION_DOCUMENT_SELECTED_FOR_COMPONENT', { componentName: 'componentLabel' },
      );
    });

    it('can pick a path but fail to update the component', () => {
      const errorData = {};
      PickerService.pickPath.and.returnValue($q.resolve({ path: '/base/pickedPath' }));
      HstComponentService.setPathParameter.and.returnValue($q.reject({ data: errorData }));
      spyOn(HippoIframeService, 'reload');

      $rootScope.$emit('document:select', { ...eventData, parameterValue: '/base/currentPath' });
      $rootScope.$digest();

      expect(PickerService.pickPath).toHaveBeenCalledWith(eventData.pickerConfig, '/base/currentPath');
      expect(HstComponentService.setPathParameter).toHaveBeenCalledWith(
        'componentId', 'hippo-default', 'parameterName', '/base/pickedPath', '/base',
      );
      expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(
        errorData, 'ERROR_DOCUMENT_SELECTED_FOR_COMPONENT',
        jasmine.any(Object), { componentName: 'componentLabel' },
      );
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('should not proceed if the default behavior prevented', () => {
      $rootScope.$on('document:select', event => event.preventDefault());
      $rootScope.$emit('document:select', eventData);
      $rootScope.$digest();

      expect(PickerService.pickPath).not.toHaveBeenCalled();
    });

    it('should report usage statistic', () => {
      $rootScope.$emit('document:select', eventData);
      $rootScope.$digest();

      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('PickContentButton');
    });
  });

  describe('_onDragStart', () => {
    beforeEach(() => {
      $rootScope.$emit('iframe:drag:start');
    });

    it('should set hippo-dragging class on the canvas element', () => {
      expect($element.find('.channel-iframe-canvas')).toHaveClass('hippo-dragging');
    });

    it('should enable scrolling', () => {
      expect(ScrollService.enable).toHaveBeenCalled();
    });
  });

  describe('_onDragStop', () => {
    beforeEach(() => {
      $rootScope.$emit('iframe:drag:start');
      $rootScope.$emit('iframe:drag:stop');
    });

    it('should remove hippo-dragging class from the canvas element', () => {
      expect($element.find('.channel-iframe-canvas')).not.toHaveClass('hippo-dragging');
    });

    it('should disable scrolling', () => {
      expect(ScrollService.disable).toHaveBeenCalled();
    });
  });
});
