/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
  let FeedbackService;
  let HippoIframeService;
  let PageStructureService;
  let SpaService;

  class EmitteryMock {
    constructor() {
      this.on = jasmine.createSpy('on');
      this.emit = jasmine.createSpy('emit');
    }
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    angular.mock.module(($provide) => {
      $provide.constant('Emittery', EmitteryMock);
    });

    inject((
      _$log_,
      _$q_,
      _$rootScope_,
      _CmsService_,
      _ContainerService_,
      _DialogService_,
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
      spyOn(PageStructureService, 'addComponentToContainer').and.returnValue($q.resolve({
        getContainer() {},
        getLabel() {},
      }));
      spyOn(PageStructureService, 'renderContainer');
      spyOn(SpaService, 'isSpa');
    });

    it('adds a component to a container in the page structure', () => {
      SpaService.isSpa.and.returnValue(false);

      const component = {};
      const container = {};
      ContainerService.addComponent(component, container);
      $rootScope.$digest();

      expect(PageStructureService.addComponentToContainer).toHaveBeenCalledWith(component, container, undefined);
      expect(HippoIframeService.reload).not.toHaveBeenCalled();
      expect(PageStructureService.renderContainer).toHaveBeenCalledWith(container);
    });

    it('reloads the iframe if there is an SPA', () => {
      SpaService.isSpa.and.returnValue(true);

      ContainerService.addComponent();
      $rootScope.$digest();

      expect(PageStructureService.addComponentToContainer).toHaveBeenCalled();
      expect(HippoIframeService.reload).toHaveBeenCalled();
      expect(PageStructureService.renderContainer).not.toHaveBeenCalled();
    });

    it('handles errors and reloads the iframe, upon adding a component to container', () => {
      PageStructureService.addComponentToContainer.and.returnValue($q.reject());
      spyOn(FeedbackService, 'showError');

      ContainerService.addComponent({ label: 'Banner' });
      $rootScope.$digest();

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT', {
        component: 'Banner',
      });
      expect(HippoIframeService.reload).toHaveBeenCalled();
    });
  });

  describe('move component', () => {
    const component = {};
    const sourceContainer = {};
    const targetContainer = {};
    const newContainerNextComponent = {};
    const rerenderedSourceContainer = {};
    const rerenderedTargetContainer = {};

    beforeEach(() => {
      spyOn(PageStructureService, 'moveComponent').and.returnValue($q.resolve([sourceContainer, targetContainer]));
      spyOn(PageStructureService, 'renderContainer')
        .and.returnValues($q.resolve(rerenderedSourceContainer), $q.resolve(rerenderedTargetContainer));
    });

    it('moves components in the page structure, re-renders containers and updates the drag-drop logic', () => {
      ContainerService.moveComponent(component, targetContainer, newContainerNextComponent);
      $rootScope.$digest();

      expect(PageStructureService.moveComponent).toHaveBeenCalledWith(
        component, targetContainer, newContainerNextComponent,
      );
      expect(PageStructureService.renderContainer).toHaveBeenCalledWith(sourceContainer);
      expect(PageStructureService.renderContainer).toHaveBeenCalledWith(targetContainer);
    });

    it('should emit component:moved event in the end', () => {
      spyOn($rootScope, '$emit');
      ContainerService.moveComponent(component, targetContainer, newContainerNextComponent);
      $rootScope.$digest();

      expect($rootScope.$emit).toHaveBeenCalledWith('component:moved');
    });
  });

  describe('deleteComponent', () => {
    let mockComponent;

    beforeEach(() => {
      spyOn(CmsService, 'publish');
      spyOn(DialogService, 'confirm').and.callThrough();
      spyOn(DialogService, 'show').and.returnValue($q.resolve());
      spyOn(HippoIframeService, 'reload');

      mockComponent = jasmine.createSpyObj('ComponentElement', ['getId', 'getLabel']);
      mockComponent.getId.and.returnValue('1234');
      spyOn(PageStructureService, 'removeComponentById');
      spyOn(PageStructureService, 'renderContainer');
    });

    it('shows the confirmation dialog and deletes selected component on confirmation', () => {
      const oldContainer = {};
      const newContainer = {};
      PageStructureService.removeComponentById.and.returnValue($q.when(oldContainer));
      PageStructureService.renderContainer.and.returnValue($q.when(newContainer));

      ContainerService.deleteComponent(mockComponent);
      $rootScope.$digest();

      expect(mockComponent.getLabel).toHaveBeenCalled();
      expect(DialogService.confirm).toHaveBeenCalled();
      expect(DialogService.show).toHaveBeenCalled();
      expect(PageStructureService.removeComponentById).toHaveBeenCalledWith('1234');
      expect(CmsService.publish).toHaveBeenCalledWith('destroy-component-properties-window');
    });

    it('shows the confirmation dialog and reloads the page if there is an SPA', () => {
      const oldContainer = {};
      spyOn(SpaService, 'isSpa').and.returnValue(true);
      PageStructureService.removeComponentById.and.returnValue($q.when(oldContainer));

      ContainerService.deleteComponent(mockComponent);
      $rootScope.$digest();

      expect(mockComponent.getLabel).toHaveBeenCalled();
      expect(DialogService.confirm).toHaveBeenCalled();
      expect(DialogService.show).toHaveBeenCalled();
      expect(PageStructureService.removeComponentById).toHaveBeenCalledWith('1234');
      expect(HippoIframeService.reload).toHaveBeenCalled();
      expect(CmsService.publish).toHaveBeenCalledWith('destroy-component-properties-window');
    });

    it('reloads the iframe when removal fails', () => {
      PageStructureService.removeComponentById.and.returnValue($q.reject());

      ContainerService.deleteComponent(mockComponent);
      $rootScope.$digest();

      expect(HippoIframeService.reload).toHaveBeenCalled();
    });

    it('reloads the iframe when rendering fails', () => {
      PageStructureService.removeComponentById.and.returnValue($q.resolve());
      PageStructureService.renderContainer.and.returnValue($q.reject());

      ContainerService.deleteComponent(mockComponent);
      $rootScope.$digest();

      expect(HippoIframeService.reload).toHaveBeenCalled();
    });
  });

  describe('renderComponent', () => {
    let mockComponent;

    beforeEach(() => {
      mockComponent = { getId: () => '1234' };

      spyOn(PageStructureService, 'renderComponent');
      spyOn(SpaService, 'renderComponent').and.returnValue(false);
    });

    it('first tries to render a component via the SPA', (done) => {
      spyOn(SpaService, 'isSpa').and.returnValue(true);

      ContainerService.renderComponent(mockComponent, { foo: 1 })
        .then(() => {
          expect(SpaService.renderComponent).toHaveBeenCalledWith(mockComponent, { foo: 1 });
          expect(PageStructureService.renderComponent).not.toHaveBeenCalled();
          done();
        });

      $rootScope.$apply();
    });

    it('then tries to render a component via the PageStructureService', (done) => {
      spyOn(SpaService, 'isSpa').and.returnValue(false);
      PageStructureService.renderComponent.and.returnValue($q.resolve());

      ContainerService.renderComponent(mockComponent, { foo: 1 })
        .then(() => {
          expect(SpaService.renderComponent).not.toHaveBeenCalled();
          expect(PageStructureService.renderComponent).toHaveBeenCalledWith(mockComponent, { foo: 1 });
          done();
        });

      $rootScope.$apply();
    });

    it('rejects when render component via the PageStructureService fails', (done) => {
      PageStructureService.renderComponent.and.returnValue($q.reject());

      ContainerService.renderComponent(mockComponent, { foo: 1 })
        .then(() => fail('Should be rejected'))
        .catch(done);

      $rootScope.$apply();
    });

    it('rejects when render component via the SPA fails ', (done) => {
      spyOn(SpaService, 'isSpa').and.returnValue(true);
      SpaService.renderComponent.and.returnValue($q.reject('error'));
      spyOn($log, 'error');

      ContainerService.renderComponent(mockComponent, { foo: 1 })
        .catch((error) => {
          expect(error).toBe('error');
          expect($log.error).toHaveBeenCalledWith('error');
        })
        .then(done);

      $rootScope.$apply();
    });

    it('reloads the iframe when render component via the PageStructureService fails', (done) => {
      PageStructureService.renderComponent.and.returnValue($q.reject());
      spyOn(HippoIframeService, 'reload');

      ContainerService.renderComponent(mockComponent, { foo: 1 });
      $rootScope.$apply();

      expect(HippoIframeService.reload).toHaveBeenCalled();

      done();
    });
  });
});
