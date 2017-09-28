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

describe('ComponentCatalogService', () => {
  let $q;
  let $log;
  let $rootScope;
  let SidePanelService;
  let ComponentCatalogService;
  let HippoIframeService;
  let MaskService;
  let OverlayService;
  let PageStructureService;
  let FeedbackService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$q_,
      _$log_,
      _$rootScope_,
      _SidePanelService_,
      _ComponentCatalogService_,
      _HippoIframeService_,
      _MaskService_,
      _OverlayService_,
      _PageStructureService_,
      _FeedbackService_,
    ) => {
      $q = _$q_;
      $log = _$log_;
      $rootScope = _$rootScope_;
      SidePanelService = _SidePanelService_;
      ComponentCatalogService = _ComponentCatalogService_;
      HippoIframeService = _HippoIframeService_;
      MaskService = _MaskService_;
      OverlayService = _OverlayService_;
      PageStructureService = _PageStructureService_;
      FeedbackService = _FeedbackService_;
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
      spyOn(ComponentCatalogService, 'addComponentToContainer');
    });

    it('should forward mask and zindexes handling and setup clickhandlers', () => {
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
    beforeEach(() => {
      spyOn($log, 'info');
      spyOn(HippoIframeService, 'reload').and.returnValue($q.resolve());
      spyOn(PageStructureService, 'getContainerByOverlayElement');
      spyOn(PageStructureService, 'showComponentProperties');
      spyOn(PageStructureService, 'addComponentToContainer').and.returnValue($q.resolve({
        getContainer() {},
        getLabel() {},
      }));
    });

    it('should handle container click', () => {
      let isDisabled = false;
      const mockContainer = {
        isDisabled() {
          return isDisabled;
        },
      };
      const mockEvent = jasmine.createSpyObj('mockEvent', ['target', 'stopPropagation']);
      spyOn(ComponentCatalogService, 'addComponentToContainer');

      ComponentCatalogService._handleContainerClick(mockEvent, mockContainer);

      expect(ComponentCatalogService.addComponentToContainer).toHaveBeenCalled();

      isDisabled = true;

      ComponentCatalogService._handleContainerClick(mockEvent, mockContainer);

      expect(ComponentCatalogService.addComponentToContainer).toHaveBeenCalled();
      expect(mockEvent.stopPropagation).toHaveBeenCalled();
    });

    it('should add component to container', () => {
      ComponentCatalogService.addComponentToContainer();
      $rootScope.$apply();
      expect(PageStructureService.addComponentToContainer).toHaveBeenCalled();
    });

    it('should handle error when thrown, upon adding a component to container', () => {
      PageStructureService.addComponentToContainer.and.returnValue($q.reject());
      spyOn(FeedbackService, 'showError');

      ComponentCatalogService.addComponentToContainer({ label: 'Banner' });
      $rootScope.$apply();
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT', {
        component: 'Banner',
      });
    });

    it('remove overlay if it was added by ComponentCatalogService', () => {
      spyOn(OverlayService, 'showComponentsOverlay');
      PageStructureService.addComponentToContainer.and.returnValue($q.reject());

      OverlayService.toggleOverlayByComponent = true;
      ComponentCatalogService.addComponentToContainer({ label: 'Banner' });
      $rootScope.$apply();

      expect(OverlayService.toggleOverlayByComponent).toEqual(false);
      expect(OverlayService.showComponentsOverlay).toHaveBeenCalledWith(false);
    });

    it('should show component properties dialog if component contains no head contributions', () => {
      spyOn(PageStructureService, 'containsNewHeadContributions').and.callFake(() => false);
      ComponentCatalogService.addComponentToContainer();
      $rootScope.$apply();
      expect(PageStructureService.showComponentProperties).toHaveBeenCalled();
    });

    it('should reload and then show component properties dialog if component contains head contributions', () => {
      spyOn(PageStructureService, 'containsNewHeadContributions').and.callFake(() => true);
      ComponentCatalogService.addComponentToContainer();
      $rootScope.$apply();
      expect(HippoIframeService.reload).toHaveBeenCalled();
      $rootScope.$apply();
      expect(PageStructureService.showComponentProperties).toHaveBeenCalled();
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

