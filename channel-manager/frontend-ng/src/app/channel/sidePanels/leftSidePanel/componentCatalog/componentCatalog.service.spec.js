/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

describe('ComponentCatalogService', () => {
  let $log;
  let $q;
  let $rootScope;
  let ComponentCatalogService;
  let ConfigService;
  let ContainerService;
  let EditComponentService;
  let HippoIframeService;
  let MaskService;
  let OverlayService;
  let PageStructureService;
  let RightSidePanelService;
  let SidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$log_,
      _$q_,
      _$rootScope_,
      _ComponentCatalogService_,
      _ConfigService_,
      _ContainerService_,
      _EditComponentService_,
      _HippoIframeService_,
      _MaskService_,
      _OverlayService_,
      _PageStructureService_,
      _RightSidePanelService_,
      _SidePanelService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ComponentCatalogService = _ComponentCatalogService_;
      ConfigService = _ConfigService_;
      ContainerService = _ContainerService_;
      EditComponentService = _EditComponentService_;
      HippoIframeService = _HippoIframeService_;
      MaskService = _MaskService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      RightSidePanelService = _RightSidePanelService_;
      SidePanelService = _SidePanelService_;
    });
  });

  describe('component add mode', () => {
    beforeEach(() => {
      spyOn(MaskService, 'mask');
      spyOn(MaskService, 'unmask');
      spyOn(MaskService, 'removeClickHandler');
      spyOn(MaskService, 'onClick');
      spyOn(SidePanelService, 'liftSidePanelAboveMask');
      spyOn(SidePanelService, 'lowerSidePanelBeneathMask');
      spyOn(HippoIframeService, 'liftIframeAboveMask');
      spyOn(HippoIframeService, 'lowerIframeBeneathMask');
      spyOn(OverlayService, 'onContainerClick');
      spyOn(OverlayService, 'enableAddMode');
      spyOn(OverlayService, 'disableAddMode');
      spyOn(OverlayService, 'offContainerClick');
      spyOn(OverlayService, 'showComponentsOverlay');
      spyOn(ContainerService, 'addComponent');
    });

    it('should forward mask and z-indexes handling and setup click handlers', () => {
      ComponentCatalogService.selectComponent({ id: 'componentId' });

      expect(MaskService.mask).toHaveBeenCalled();
      expect(SidePanelService.liftSidePanelAboveMask).toHaveBeenCalled();
      expect(HippoIframeService.liftIframeAboveMask).toHaveBeenCalled();
      expect(OverlayService.enableAddMode).toHaveBeenCalled();
      expect(OverlayService.onContainerClick).toHaveBeenCalledWith(jasmine.any(Function));
      expect(MaskService.onClick).toHaveBeenCalledWith(jasmine.any(Function));
    });

    it('should disable on mask click', () => {
      ComponentCatalogService._handleMaskClick();

      expect(ComponentCatalogService.selectedComponent).toBe(undefined);
      expect(MaskService.unmask).toHaveBeenCalled();
      expect(SidePanelService.lowerSidePanelBeneathMask).toHaveBeenCalled();
      expect(HippoIframeService.lowerIframeBeneathMask).toHaveBeenCalled();
      expect(OverlayService.disableAddMode).toHaveBeenCalled();
      expect(OverlayService.offContainerClick).toHaveBeenCalled();
      expect(MaskService.removeClickHandler).toHaveBeenCalled();
    });

    it('remove overlay if it was added by ComponentCatalogService', () => {
      OverlayService.isComponentsOverlayDisplayed = true;
      OverlayService.toggleOverlayByComponent = true;
      ComponentCatalogService._handleMaskClick();

      expect(OverlayService.showComponentsOverlay).toHaveBeenCalledWith(false);
      expect(OverlayService.toggleOverlayByComponent).toEqual(false);
    });
  });

  describe('adding a component to container', () => {
    let isDisabled;
    let mockContainer;
    let mockComponent;
    let mockComponent2;
    let mockComponent3;
    let mockEvent;
    let mockEventTarget;
    const selectedComponent = {};

    beforeEach(() => {
      [mockEventTarget] = Array.from(angular.element('<div></div>'));
      isDisabled = false;

      mockEvent = {
        stopPropagation: jasmine.createSpy('stopPropagation'),
        get target() { return mockEventTarget; },
      };

      mockComponent = {
        getId: () => 456,
        getContainer: () => mockContainer,
      };

      mockComponent2 = {
        getId: () => 789,
        getContainer: () => mockContainer,
      };

      mockComponent3 = {
        getId: () => 123,
        getContainer: () => mockContainer,
      };

      mockContainer = {
        isDisabled() {
          return isDisabled;
        },
        getId() {
          return 123;
        },
        items: [
          mockComponent,
          mockComponent2,
        ],
      };

      ComponentCatalogService.selectedComponent = selectedComponent;

      spyOn($log, 'info');
      spyOn(ContainerService, 'addComponent');
      spyOn(EditComponentService, 'startEditing');
      spyOn(HippoIframeService, 'reload').and.returnValue($q.resolve());
      spyOn(PageStructureService, 'getContainerById').and.returnValue(mockContainer);
      spyOn(RightSidePanelService, 'close');
    });

    describe('positioning of component within container', () => {
      const addedComponent = mockComponent3;

      beforeEach(() => {
        ComponentCatalogService.selectedComponent = addedComponent;
      });

      it('should position the component after the click target', () => {
        const clickedComponent = mockComponent;
        const expectedNextComponentId = mockComponent2.getId();
        [mockEventTarget] = Array.from(
          angular.element('<div class="hippo-overlay-element-component-drop-area-after"></div>'),
        );

        ComponentCatalogService._handleComponentClick(mockEvent, clickedComponent);
        $rootScope.$digest();

        expect(ContainerService.addComponent)
          .toHaveBeenCalledWith(addedComponent, mockContainer, expectedNextComponentId);
      });

      it('should position the component after the click target last in the container', () => {
        const clickedComponent = mockComponent2;
        const expectedNextComponentId = undefined;
        [mockEventTarget] = Array.from(
          angular.element('<div class="hippo-overlay-element-component-drop-area-after"></div>'),
        );

        ComponentCatalogService._handleComponentClick(mockEvent, clickedComponent);
        $rootScope.$digest();

        expect(ContainerService.addComponent)
          .toHaveBeenCalledWith(addedComponent, mockContainer, expectedNextComponentId);
      });

      it('should position the component before the click target', () => {
        const clickedComponent = mockComponent2;
        const expectedNextComponentId = mockComponent2.getId();
        [mockEventTarget] = Array.from(
          angular.element('<div class="hippo-overlay-element-component-drop-area-before"></div>'),
        );

        ComponentCatalogService._handleComponentClick(mockEvent, clickedComponent);
        $rootScope.$digest();

        expect(ContainerService.addComponent)
          .toHaveBeenCalledWith(addedComponent, mockContainer, expectedNextComponentId);
      });

      afterEach(() => {
        ComponentCatalogService.selectedComponent = selectedComponent;
      });
    });

    it('adds component to a container', () => {
      ContainerService.addComponent.and.returnValue('789');
      RightSidePanelService.close.and.returnValue($q.resolve());
      spyOn(PageStructureService, 'getComponentById').and.returnValue({ id: 789 });

      ComponentCatalogService._handleContainerClick(mockEvent, mockContainer);
      $rootScope.$digest();

      expect(ContainerService.addComponent).toHaveBeenCalledWith(selectedComponent, mockContainer, undefined);
      expect(EditComponentService.startEditing).toHaveBeenCalledWith({ id: 789 });
      expect(RightSidePanelService.close).toHaveBeenCalled();
    });

    it('does not open the component editor when relevance feature is present', () => {
      ConfigService.relevancePresent = true;

      ComponentCatalogService._handleContainerClick(mockEvent, mockContainer);
      $rootScope.$digest();

      expect(ContainerService.addComponent).toHaveBeenCalledWith(selectedComponent, mockContainer, undefined);
      expect(EditComponentService.startEditing).not.toHaveBeenCalled();
      expect(RightSidePanelService.close).not.toHaveBeenCalled();
    });

    it('ignores component add if the right side panel was not closed', () => {
      RightSidePanelService.close.and.returnValue($q.reject());
      ComponentCatalogService._handleContainerClick(mockEvent, mockContainer);

      expect(ContainerService.addComponent).not.toHaveBeenCalled();
    });

    it('ignores component add to a disabled container', () => {
      isDisabled = true;
      ComponentCatalogService._handleContainerClick(mockEvent, mockContainer);

      expect(ContainerService.addComponent).not.toHaveBeenCalled();
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
    });
  });

  describe('returning the selected component', () => {
    it('should return the selected component', () => {
      const component = { id: 'component' };
      ComponentCatalogService.selectedComponent = component;

      expect(ComponentCatalogService.getSelectedComponent()).toEqual(component);
    });
  });
});
