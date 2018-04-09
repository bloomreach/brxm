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

describe('ContainerService', () => {
  let $log;
  let $q;
  let $rootScope;
  let CmsService;
  let ContainerService;
  let DialogService;
  let DragDropService;
  let FeedbackService;
  let HippoIframeService;
  let PageStructureService;
  let SpaService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$log_,
      _$q_,
      _$rootScope_,
      _CmsService_,
      _ContainerService_,
      _DialogService_,
      _DragDropService_,
      _FeedbackService_,
      _HippoIframeService_,
      _PageStructureService_,
      _SpaService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      CmsService = _CmsService_;
      ContainerService = _ContainerService_;
      DialogService = _DialogService_;
      DragDropService = _DragDropService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      PageStructureService = _PageStructureService_;
      SpaService = _SpaService_;
    });
  });

  describe('add component', () => {
    beforeEach(() => {
      spyOn($log, 'info');
      spyOn(HippoIframeService, 'reload').and.returnValue($q.resolve());
      spyOn(PageStructureService, 'getContainerByOverlayElement');
      spyOn(PageStructureService, 'addComponentToContainer').and.returnValue($q.resolve({
        getContainer() {},
        getLabel() {},
      }));
      spyOn(PageStructureService, 'renderNewComponentInContainer');
      spyOn(SpaService, 'detectedSpa');
    });

    it('adds a component to a container in the page structure', () => {
      SpaService.detectedSpa.and.returnValue(false);
      ContainerService.addComponent();
      $rootScope.$digest();
      expect(PageStructureService.addComponentToContainer).toHaveBeenCalled();
      expect(HippoIframeService.reload).not.toHaveBeenCalled();
      expect(PageStructureService.renderNewComponentInContainer).toHaveBeenCalled();
    });

    it('reloads the iframe if there is an SPA', () => {
      SpaService.detectedSpa.and.returnValue(true);
      ContainerService.addComponent();
      $rootScope.$digest();
      expect(PageStructureService.addComponentToContainer).toHaveBeenCalled();
      expect(HippoIframeService.reload).toHaveBeenCalled();
      expect(PageStructureService.renderNewComponentInContainer).not.toHaveBeenCalled();
    });

    it('handles errors when thrown, upon adding a component to container', () => {
      PageStructureService.addComponentToContainer.and.returnValue($q.reject());
      spyOn(FeedbackService, 'showError');

      ContainerService.addComponent({ label: 'Banner' });
      $rootScope.$digest();
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT', {
        component: 'Banner',
      });
    });
  });

  describe('move component', () => {
    it('moves components in the page structure, re-renders containers and updates the drag-drop logic', () => {
      const component = {};
      const sourceContainer = {};
      const targetContainer = {};
      const newContainerNextComponent = {};
      const rerenderedSourceContainer = {};
      const rerenderedTargetContainer = {};

      spyOn(PageStructureService, 'moveComponent').and.returnValue($q.resolve([sourceContainer, targetContainer]));
      spyOn(PageStructureService, 'renderContainer').and.returnValues($q.resolve(rerenderedSourceContainer), $q.resolve(rerenderedTargetContainer));
      spyOn(DragDropService, 'replaceContainer').and.returnValues($q.resolve(), $q.resolve());

      ContainerService.moveComponent(component, targetContainer, newContainerNextComponent);
      $rootScope.$digest();

      expect(PageStructureService.moveComponent).toHaveBeenCalledWith(component, targetContainer, newContainerNextComponent);
      expect(PageStructureService.renderContainer).toHaveBeenCalledWith(sourceContainer);
      expect(PageStructureService.renderContainer).toHaveBeenCalledWith(targetContainer);
      expect(DragDropService.replaceContainer).toHaveBeenCalledWith(sourceContainer, rerenderedSourceContainer);
      expect(DragDropService.replaceContainer).toHaveBeenCalledWith(targetContainer, rerenderedTargetContainer);
    });
  });

  describe('delete component', () => {
    it('shows the confirmation dialog and deletes selected component on confirmation', () => {
      const mockComponent = jasmine.createSpyObj('ComponentElement', ['getLabel']);
      const oldContainer = {};
      const newContainer = {};
      spyOn(DragDropService, 'replaceContainer');
      spyOn(PageStructureService, 'getComponentById').and.returnValue(mockComponent);
      spyOn(PageStructureService, 'removeComponentById').and.returnValue($q.when(oldContainer));
      spyOn(PageStructureService, 'renderContainer').and.returnValue($q.when(newContainer));
      spyOn(DialogService, 'show').and.returnValue($q.resolve());
      spyOn(DialogService, 'confirm').and.callThrough();
      spyOn(CmsService, 'publish');

      ContainerService.deleteComponent('1234');
      $rootScope.$digest();

      expect(mockComponent.getLabel).toHaveBeenCalled();
      expect(DialogService.confirm).toHaveBeenCalled();
      expect(DialogService.show).toHaveBeenCalled();
      expect(PageStructureService.removeComponentById).toHaveBeenCalledWith('1234');
      expect(DragDropService.replaceContainer).toHaveBeenCalledWith(oldContainer, newContainer);
      expect(CmsService.publish).toHaveBeenCalledWith('destroy-component-properties-window');
    });

    it('shows the confirmation dialog and reloads the page if there is an SPA', () => {
      const mockComponent = jasmine.createSpyObj('ComponentElement', ['getLabel']);
      const oldContainer = {};
      spyOn(SpaService, 'detectedSpa').and.returnValue(true);
      spyOn(DragDropService, 'replaceContainer');
      spyOn(PageStructureService, 'getComponentById').and.returnValue(mockComponent);
      spyOn(PageStructureService, 'removeComponentById').and.returnValue($q.when(oldContainer));
      spyOn(DialogService, 'show').and.returnValue($q.resolve());
      spyOn(DialogService, 'confirm').and.callThrough();
      spyOn(HippoIframeService, 'reload');
      spyOn(CmsService, 'publish');

      ContainerService.deleteComponent('1234');
      $rootScope.$digest();

      expect(mockComponent.getLabel).toHaveBeenCalled();
      expect(DialogService.confirm).toHaveBeenCalled();
      expect(DialogService.show).toHaveBeenCalled();
      expect(PageStructureService.removeComponentById).toHaveBeenCalledWith('1234');
      expect(DragDropService.replaceContainer).not.toHaveBeenCalled();
      expect(HippoIframeService.reload).toHaveBeenCalled();
      expect(CmsService.publish).toHaveBeenCalledWith('destroy-component-properties-window');
    });

    it('shows component properties dialog after rejecting the delete operation', () => {
      const mockComponent = jasmine.createSpyObj('ComponentElement', ['getLabel']);
      spyOn(PageStructureService, 'getComponentById').and.returnValue(mockComponent);
      spyOn(PageStructureService, 'showComponentProperties');
      spyOn(DialogService, 'show').and.returnValue($q.reject());
      spyOn(DialogService, 'confirm').and.callThrough();

      ContainerService.deleteComponent('1234');
      $rootScope.$digest();

      expect(mockComponent.getLabel).toHaveBeenCalled();
      expect(DialogService.confirm).toHaveBeenCalled();
      expect(DialogService.show).toHaveBeenCalled();
      expect(PageStructureService.showComponentProperties).toHaveBeenCalledWith(mockComponent);
    });

    it('logs a warning for unknown components', () => {
      spyOn($log, 'warn');
      spyOn(PageStructureService, 'getComponentById').and.returnValue(null);
      spyOn(PageStructureService, 'removeComponentById');

      ContainerService.deleteComponent('unknown');
      $rootScope.$digest();

      expect($log.warn).toHaveBeenCalledWith('Cannot delete unknown component with id \'unknown\'');
      expect(PageStructureService.removeComponentById).not.toHaveBeenCalled();
    });
  });
});
