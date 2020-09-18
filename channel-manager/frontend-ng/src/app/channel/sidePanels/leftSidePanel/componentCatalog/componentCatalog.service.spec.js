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

describe('ComponentCatalogService', () => {
  let $q;
  let $rootScope;
  let CommunicationService;
  let ComponentCatalogService;
  let ConfigService;
  let ContainerService;
  let EditComponentService;
  let HippoIframeService;
  let MaskService;
  let PageStructureService;
  let RightSidePanelService;
  let SidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ConfigService = {};
    CommunicationService = jasmine.createSpyObj('CommunicationService', ['toggleAddMode']);
    ContainerService = jasmine.createSpyObj('ContainerService', ['addComponent']);
    EditComponentService = jasmine.createSpyObj('EditComponentService', ['startEditing']);
    MaskService = jasmine.createSpyObj('MaskService', ['mask', 'unmask']);
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['liftIframeAboveMask', 'lowerIframeBeneathMask']);
    PageStructureService = jasmine.createSpyObj('PageStructureService', ['getPage']);
    RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', ['close']);
    SidePanelService = jasmine.createSpyObj('SidePanelService', [
      'liftSidePanelAboveMask',
      'lowerSidePanelBeneathMask',
    ]);

    angular.mock.module(($provide) => {
      $provide.value('CommunicationService', CommunicationService);
      $provide.value('ConfigService', ConfigService);
      $provide.value('ContainerService', ContainerService);
      $provide.value('EditComponentService', EditComponentService);
      $provide.value('MaskService', MaskService);
      $provide.value('HippoIframeService', HippoIframeService);
      $provide.value('PageStructureService', PageStructureService);
      $provide.value('RightSidePanelService', RightSidePanelService);
      $provide.value('SidePanelService', SidePanelService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _ComponentCatalogService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ComponentCatalogService = _ComponentCatalogService_;
    });
  });

  describe('selectComponent', () => {
    it('should update the user interface on select component', () => {
      CommunicationService.toggleAddMode.and.returnValue($q.defer());
      ComponentCatalogService.selectComponent();

      expect(MaskService.mask).toHaveBeenCalledWith('mask-add-component');
      expect(SidePanelService.liftSidePanelAboveMask).toHaveBeenCalled();
      expect(HippoIframeService.liftIframeAboveMask).toHaveBeenCalled();
    });

    it('should restore the user interface after selecting a component', () => {
      CommunicationService.toggleAddMode.and.returnValue($q.reject());
      ComponentCatalogService.selectComponent();
      $rootScope.$digest();

      expect(MaskService.unmask).toHaveBeenCalledWith();
      expect(SidePanelService.lowerSidePanelBeneathMask).toHaveBeenCalled();
      expect(HippoIframeService.lowerIframeBeneathMask).toHaveBeenCalled();
    });

    it('should toggle on overlay add mode', () => {
      ComponentCatalogService.selectComponent();

      expect(CommunicationService.toggleAddMode).toHaveBeenCalledWith(true);
    });

    it('should toggle off overlay add mode on mask click', () => {
      ComponentCatalogService.selectComponent();
      $rootScope.$emit('mask:click');

      expect(CommunicationService.toggleAddMode).toHaveBeenCalledWith(false);
    });

    it('should stop reacting on mask clicks when it is done', () => {
      CommunicationService.toggleAddMode.and.returnValue($q.resolve());
      ComponentCatalogService.selectComponent();
      $rootScope.$digest();
      CommunicationService.toggleAddMode.calls.reset();
      $rootScope.$emit('mask:click');

      expect(CommunicationService.toggleAddMode).not.toHaveBeenCalled();
    });
  });

  describe('_addComponent', () => {
    const component = {};
    const container = {};
    let page;

    beforeEach(() => {
      CommunicationService.toggleAddMode.and.returnValue($q.resolve({
        container: 'container-id',
        nextComponent: 'component-id',
      }));

      page = jasmine.createSpyObj('Page', ['getComponentById', 'getContainerById']);
      page.getComponentById.and.returnValue(component);
      page.getContainerById.and.returnValue(container);

      PageStructureService.getPage.and.returnValue(page);
      ContainerService.addComponent.and.returnValue('new-component-id');
    });

    it('should call the container service to add a component', () => {
      const selectedComponent = {};
      ComponentCatalogService.selectComponent(selectedComponent);
      $rootScope.$digest();

      expect(page.getContainerById).toHaveBeenCalledWith('container-id');
      expect(ContainerService.addComponent).toHaveBeenCalledWith(selectedComponent, container, 'component-id');
    });

    it('should close the right side panel', () => {
      ComponentCatalogService.selectComponent();
      $rootScope.$digest();

      expect(RightSidePanelService.close).toHaveBeenCalled();
    });

    it('should open the component editor', () => {
      ComponentCatalogService.selectComponent();
      $rootScope.$digest();

      expect(page.getComponentById).toHaveBeenCalledWith('new-component-id');
      expect(EditComponentService.startEditing).toHaveBeenCalledWith(component);
    });
  });

  describe('getSelectedComponent', () => {
    it('should return the selected component', () => {
      CommunicationService.toggleAddMode.and.returnValue($q.defer());
      ComponentCatalogService.selectComponent({ id: 'component' });

      expect(ComponentCatalogService.getSelectedComponent()).toEqual({ id: 'component' });
    });
  });
});
