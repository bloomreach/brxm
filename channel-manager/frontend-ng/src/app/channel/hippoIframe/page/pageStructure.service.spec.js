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

import angular from 'angular';
import 'angular-mocks';

describe('PageStructureService', () => {
  let $log;
  let $rootScope;
  let $q;

  let ChannelService;
  let CommunicationService;
  let FeedbackService;
  let HstComponentService;
  let HstService;
  let MarkupService;
  let ModelFactoryService;
  let PageStructureService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe.page');

    inject((
      _$log_,
      _$q_,
      _$rootScope_,
      _ChannelService_,
      _CommunicationService_,
      _EditComponentService_,
      _FeedbackService_,
      _HstComponentService_,
      _HstService_,
      _MarkupService_,
      _ModelFactoryService_,
      _PageStructureService_,
    ) => {
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      CommunicationService = _CommunicationService_;
      FeedbackService = _FeedbackService_;
      HstComponentService = _HstComponentService_;
      HstService = _HstService_;
      MarkupService = _MarkupService_;
      ModelFactoryService = _ModelFactoryService_;
      PageStructureService = _PageStructureService_;
    });

    spyOn(ChannelService, 'checkChanges').and.returnValue($q.resolve());
    spyOn(CommunicationService, 'ready').and.returnValue($q.resolve());
  });

  function mockUpdateComponent(data = []) {
    const spy = CommunicationService.updateComponent.calls
      ? CommunicationService.updateComponent
      : spyOn(CommunicationService, 'updateComponent');
    spy.and.returnValue($q.resolve(data));
  }

  function mockUpdateContainer(data = []) {
    const spy = CommunicationService.updateContainer.calls
      ? CommunicationService.updateContainer
      : spyOn(CommunicationService, 'updateContainer');
    spy.and.returnValue($q.resolve(data));
  }

  function mockParseElements(data = []) {
    const spy = CommunicationService.parseElements.calls
      ? CommunicationService.parseElements
      : spyOn(CommunicationService, 'parseElements');
    spy.and.returnValue($q.resolve(data));
  }

  function mockEnd(uuid) {
    return {
      'HST-End': 'true',
      uuid,
    };
  }

  function mockUnprocessedHeadContributions(headElements) {
    return {
      'HST-Type': 'HST_UNPROCESSED_HEAD_CONTRIBUTIONS',
      headElements,
    };
  }

  function mockProcessedHeadContributions(headElements) {
    return {
      'HST-Type': 'HST_PROCESSED_HEAD_CONTRIBUTIONS',
      headElements,
    };
  }

  function mockPage(uuid, ...containers) {
    return [
      {
        'HST-Type': 'PAGE-META-DATA',
        'HST-Page-Id': uuid,
      },
      ...containers.flat(),
    ];
  }

  function mockContainer(label, uuid, type, ...components) {
    return [
      {
        'HST-Type': 'CONTAINER_COMPONENT',
        'HST-Label': label,
        'HST-XType': type,
        uuid,
      },
      ...components.flat(),
      mockEnd(uuid),
    ];
  }

  function mockItem(label, uuid, ...components) {
    return [
      {
        'HST-Type': 'CONTAINER_ITEM_COMPONENT',
        'HST-Label': label,
        uuid,
      },
      ...components,
      mockEnd(uuid),
    ];
  }

  function mockManageContentLink(documentTemplateQuery) {
    return {
      'HST-Type': 'MANAGE_CONTENT_LINK',
      documentTemplateQuery,
    };
  }

  function mockEditMenuLink(uuid) {
    return {
      'HST-Type': 'EDIT_MENU_LINK',
      uuid,
    };
  }

  describe('initially', () => {
    it('should have no page', () => {
      expect(PageStructureService.getPage()).not.toBeDefined();
    });
  });

  describe('parseElements', () => {
    it('creates a new page from the HST comments found in the DOM of the iframe', () => {
      mockParseElements(
        mockPage('page-1',
          mockContainer('Container 1', 'container-1', 'HST.vBox',
            mockItem('Component 1', 'component-1'),
            mockItem('Component 2', 'component-2',
              mockManageContentLink('doc-tpl-query'))),
          mockContainer('Container 2', 'container-2', 'HST.vBox'),
          mockEditMenuLink('edit-menu-link')),
      );

      PageStructureService.parseElements();
      $rootScope.$digest();

      const page = PageStructureService.getPage();
      expect(page).toBeTruthy();
      expect(page.getMeta().getPageId()).toBe('page-1');
      expect(page.getContainers()).toHaveLength(2);

      const container1 = page.getContainerById('container-1');
      expect(container1).toBeDefined();
      expect(container1.isEmpty()).toBe(false);

      expect(container1.getComponents()).toHaveLength(2);
      const component1 = container1.getComponent('component-1');
      expect(component1).toBeDefined();
      const component2 = container1.getComponent('component-2');
      expect(component2).toBeDefined();

      const container2 = page.getContainerById('container-2');
      expect(container2).toBeDefined();
      expect(container2.isEmpty()).toBe(true);

      expect(page.getLinks()).toHaveLength(1);
      const [editMenuLink] = page.getLinks();
      expect(editMenuLink.getId()).toBe('edit-menu-link');

      expect(component2.getLinks()).toHaveLength(1);
      const [manageContentLink] = component2.getLinks();
      expect(manageContentLink.getDocumentTemplateQuery()).toBe('doc-tpl-query');
    });

    it('emits event "page:change" after page elements have been parsed', () => {
      spyOn($rootScope, '$emit');

      mockParseElements();
      PageStructureService.parseElements(true);
      $rootScope.$digest();

      expect($rootScope.$emit).toHaveBeenCalledWith('page:change', { initial: true });
    });

    it('registers processed and unprocessed head contributions', () => {
      const unprocessedHeadContributions = ['<link href="unprocessed.css">'];
      const processedHeadContributions = ['<script>window.processed = true</script>'];

      mockParseElements(
        mockPage('page-1',
          mockUnprocessedHeadContributions(unprocessedHeadContributions),
          mockProcessedHeadContributions(processedHeadContributions)),
      );

      PageStructureService.parseElements();
      $rootScope.$digest();

      expect([...PageStructureService.headContributions])
        .toEqual([...unprocessedHeadContributions, ...processedHeadContributions]);
    });

    describe('channels switch', () => {
      let pageMeta;

      beforeEach(() => {
        spyOn(ChannelService, 'initializeChannel').and.returnValue($q.resolve());
        spyOn(ChannelService, 'getHostGroup').and.returnValue('theHostGroup');
        spyOn(ChannelService, 'getId');
        spyOn(ModelFactoryService, 'createPage');

        const page = jasmine.createSpyObj('page', ['getMeta']);
        pageMeta = jasmine.createSpyObj('pageMeta', ['getChannelId', 'getContextPath']);
        pageMeta.getContextPath.and.returnValue('/contextPathX');
        page.getMeta.and.returnValue(pageMeta);
        ModelFactoryService.createPage.and.returnValue(page);

        PageStructureService.parseElements();
      });

      it('switches channels when the channel id in the page meta-data differs from the current channel id', () => {
        pageMeta.getChannelId.and.returnValue('channelX');
        ChannelService.getId.and.returnValue('channelY');

        $rootScope.$digest();

        expect(ChannelService.initializeChannel).toHaveBeenCalledWith(
          'channelX',
          '/contextPathX',
          'theHostGroup',
          undefined,
        );
      });

      it('does not switch channels when the channel id from the meta same to the current one', () => {
        pageMeta.getChannelId.and.returnValue('channelX');
        ChannelService.getId.and.returnValue('channelX');

        $rootScope.$digest();

        expect(ChannelService.initializeChannel).not.toHaveBeenCalled();
      });

      it('does not switch channels when there is no meta', () => {
        pageMeta.getChannelId.and.returnValue(undefined);
        ChannelService.getId.and.returnValue('channelX');

        $rootScope.$digest();

        expect(ChannelService.initializeChannel).not.toHaveBeenCalled();
      });
    });

    it('should clear the page structure', () => {
      spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.resolve('new-markup'));
      mockParseElements(
        mockPage('page-1',
          mockContainer('Container 1', 'container-1', 'HST.vBox',
            mockItem('Component 1', 'component-1')),
          mockContainer('Container 2', 'container-2', 'HST.vBox',
            mockUnprocessedHeadContributions(['head-contribution']))),
      );

      PageStructureService.parseElements();
      $rootScope.$digest();

      expect(PageStructureService.getPage()).toBeDefined();
      expect(PageStructureService.headContributions.size).toBe(1);

      mockParseElements([]);
      PageStructureService.parseElements();
      $rootScope.$digest();

      expect(PageStructureService.headContributions.size).toBe(0);
    });
  });

  describe('renderComponent', () => {
    beforeEach(() => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.resolve({ data: 'new-markup' }));
      mockUpdateComponent();

      mockParseElements(
        mockPage('page-1',
          mockContainer('Container 1', 'container-1', 'HST.vBox',
            mockItem('Component 1', 'component-1'),
            mockItem('Component 2', 'component-2'))),
      );

      PageStructureService.parseElements();
      $rootScope.$digest();
    });

    it('gracefully handles requests to render an undefined or null component', () => {
      $q.all(
        PageStructureService.renderComponent(),
        PageStructureService.renderComponent(null),
      );
      $rootScope.$digest();

      expect(MarkupService.fetchComponentMarkup).not.toHaveBeenCalled();
    });

    it('loads the component markup and forwards it to the iframe', () => {
      const component1 = PageStructureService.getPage().getComponentById('component-1');
      const properties = {};

      PageStructureService.renderComponent(component1, properties);
      $rootScope.$digest();

      expect(MarkupService.fetchComponentMarkup).toHaveBeenCalledWith(component1, properties);
      expect(CommunicationService.updateComponent).toHaveBeenCalledWith('component-1', 'new-markup');
    });

    it('creates a new component with the data returned from the iframe', () => {
      spyOn(ModelFactoryService, 'createComponent').and.callThrough();

      const updatedComponentData = mockItem('Update component', 'component-1');
      mockUpdateComponent(updatedComponentData);

      const page = PageStructureService.getPage();
      const component1 = page.getComponentById('component-1');
      PageStructureService.renderComponent(component1);
      $rootScope.$digest();

      expect(ModelFactoryService.createComponent).toHaveBeenCalledWith(updatedComponentData);

      const updatedComponent1 = page.getComponentById('component-1');
      expect(updatedComponent1).not.toEqual(component1);
      expect(updatedComponent1.getContainer().getId()).toBe('container-1');
    });

    it('removes the component from the container if it is not created by the model-factory', () => {
      spyOn(ModelFactoryService, 'createComponent').and.returnValue(null);

      const page = PageStructureService.getPage();
      const component1 = page.getComponentById('component-1');
      const container1 = component1.getContainer();

      PageStructureService.renderComponent(component1);
      $rootScope.$digest();

      expect(container1.getComponent('component1')).toBeFalsy();
    });

    it('emits "page:change" after a model change', () => {
      spyOn($rootScope, '$emit');

      const updatedComponentData = mockItem('Update component', 'component-1');
      mockUpdateComponent(updatedComponentData);

      const page = PageStructureService.getPage();
      const component1 = page.getComponentById('component-1');
      PageStructureService.renderComponent(component1);
      $rootScope.$digest();

      expect($rootScope.$emit).toHaveBeenCalledWith('page:change', undefined);
    });

    it('emits "page:new-head-contributions" for new head contributions', () => {
      spyOn($rootScope, '$emit');
      const headContribution = mockUnprocessedHeadContributions(['<style/>', '<script/>']);

      const updatedComponentData = mockItem('Update component', 'component-1', headContribution);
      mockUpdateComponent(updatedComponentData);

      const page = PageStructureService.getPage();
      const component1 = page.getComponentById('component-1');
      PageStructureService.renderComponent(component1);
      $rootScope.$digest();

      const updatedComponent1 = page.getComponentById('component-1');

      expect($rootScope.$emit).toHaveBeenCalledWith('page:new-head-contributions', updatedComponent1);

      $rootScope.$emit.calls.reset();
      PageStructureService.renderComponent(component1);
      $rootScope.$digest();

      expect($rootScope.$emit).not.toHaveBeenCalledWith('page:new-head-contributions', jasmine.anything());
    });

    it('ignores "page:new-head-contributions" if component properties are passed', () => {
      spyOn($rootScope, '$emit');

      const headContribution = mockUnprocessedHeadContributions(['<style/>', '<script/>']);
      const updateComponentData = mockItem('Update component', 'component-1', headContribution);
      mockUpdateComponent(updateComponentData);

      const page = PageStructureService.getPage();
      const component1 = page.getComponentById('component-1');
      PageStructureService.renderComponent(component1, { text: 'value' });
      $rootScope.$digest();

      expect($rootScope.$emit).not.toHaveBeenCalledWith('page:new-head-contributions', jasmine.anything());
    });

    it('shows an error message and reloads the page when a component has been deleted', (done) => {
      MarkupService.fetchComponentMarkup.and.returnValue($q.reject({ status: 404 }));
      spyOn(FeedbackService, 'showDismissible');

      PageStructureService.renderComponent({}).catch(() => {
        expect(FeedbackService.showDismissible).toHaveBeenCalledWith('FEEDBACK_NOT_FOUND_MESSAGE');
        done();
      });

      $rootScope.$digest();
    });

    it('does nothing if markup for a component cannot be retrieved but status is not 404', () => {
      MarkupService.fetchComponentMarkup.and.returnValue($q.reject({}));
      spyOn(FeedbackService, 'showError');

      PageStructureService.renderComponent({});
      $rootScope.$digest();

      expect(FeedbackService.showError).not.toHaveBeenCalled();
    });

    it('logs an error if the new component can not be created', () => {
      spyOn($log, 'error');
      spyOn(ModelFactoryService, 'createComponent')
        .and.throwError(new Error('bad stuff happened'));

      const component1 = PageStructureService.getPage().getComponentById('component-1');
      PageStructureService.renderComponent(component1);
      $rootScope.$digest();

      expect($log.error).toHaveBeenCalledWith('bad stuff happened');
    });
  });

  describe('renderContainer', () => {
    beforeEach(() => {
      spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.resolve('new-markup'));
      mockParseElements(
        mockPage('page-1',
          mockContainer('Container 1', 'container-1', 'HST.vBox',
            mockItem('Component 1', 'component-1'),
            mockItem('Component 2', 'component-2'))),
      );

      PageStructureService.parseElements();
      $rootScope.$digest();
    });

    it('loads the container markup and forwards it to the iframe', () => {
      const updatedContainerData = mockContainer('Updated container', 'container-1');
      mockUpdateContainer(updatedContainerData);

      const container1 = PageStructureService.getPage().getContainerById('container-1');
      PageStructureService.renderContainer(container1);
      $rootScope.$digest();

      expect(MarkupService.fetchContainerMarkup).toHaveBeenCalledWith(container1);
      expect(CommunicationService.updateContainer).toHaveBeenCalledWith('container-1', 'new-markup');
    });

    it('creates a new container with the data returned from the iframe', () => {
      spyOn(ModelFactoryService, 'createContainer').and.callThrough();

      const updatedContainerData = mockContainer('Updated container', 'container-1', 'HST.vBox');
      mockUpdateContainer(updatedContainerData);

      const page = PageStructureService.getPage();
      const container = page.getContainerById('container-1');
      PageStructureService.renderContainer(container);
      $rootScope.$digest();

      expect(ModelFactoryService.createContainer).toHaveBeenCalledWith(updatedContainerData);

      const updatedContainer1 = page.getContainerById('container-1');
      expect(updatedContainer1).not.toEqual(container);
      expect(updatedContainer1.getLabel()).toBe('Updated container');
    });

    it('emits "page:change" after a model change', () => {
      const updatedContainerData = mockContainer('Updated container', 'container-1', 'HST.vBox');
      mockUpdateContainer(updatedContainerData);

      const page = PageStructureService.getPage();
      const container1 = page.getContainerById('container-1');

      spyOn($rootScope, '$emit');
      PageStructureService.renderContainer(container1);
      $rootScope.$digest();

      expect($rootScope.$emit).toHaveBeenCalledWith('page:change', undefined);
    });

    it('emits "page:new-head-contributions" for new head contributions', () => {
      const onNewHeadContributions = jasmine.createSpy('new-head-contributions');
      const offNewHeadContributions = $rootScope.$on('page:new-head-contributions', onNewHeadContributions);

      const headContribution = mockUnprocessedHeadContributions(['<style/>', '<script/>']);
      const updatedContainerData = mockContainer('Updated container', 'container-1', 'HST.vBox', headContribution);
      mockUpdateContainer(updatedContainerData);

      const page = PageStructureService.getPage();
      let container1 = page.getContainerById('container-1');
      PageStructureService.renderContainer(container1);
      $rootScope.$digest();

      container1 = page.getContainerById('container-1');
      expect(onNewHeadContributions).toHaveBeenCalledWith(jasmine.anything(), container1);

      onNewHeadContributions.calls.reset();
      container1 = page.getContainerById('container-1');
      PageStructureService.renderContainer(container1);
      $rootScope.$digest();

      expect(onNewHeadContributions).not.toHaveBeenCalled();

      offNewHeadContributions();
    });

    it('logs an error if the new container can not be created', () => {
      spyOn($log, 'error');
      spyOn(ModelFactoryService, 'createContainer')
        .and.throwError(new Error('bad stuff happened'));

      const container1 = PageStructureService.getPage().getContainerById('container-1');
      PageStructureService.renderContainer(container1);
      $rootScope.$digest();

      expect($log.error).toHaveBeenCalledWith('bad stuff happened');
    });
  });

  describe('addComponentToContainer', () => {
    let component;
    let container;

    beforeEach(() => {
      component = {
        id: 'mock-component',
        name: 'Mock Component',
      };
      container = jasmine.createSpyObj(['getId']);
      container.getId.and.returnValue('mock-container');
    });

    it('uses the HstService to add a new catalog component to the backend', (done) => {
      spyOn(HstService, 'addHstComponent').and.returnValue($q.resolve({
        reloadRequired: false,
        data: {
          id: 'new-component',
        },
      }));

      PageStructureService.addComponentToContainer(component, container)
        .then(({ reloadRequired, newComponentId }) => {
          expect(HstService.addHstComponent).toHaveBeenCalledWith(component, container, undefined);
          expect(reloadRequired).toBe(false);
          expect(newComponentId).toBe('new-component');
          done();
        });

      $rootScope.$digest();
    });

    it('checks changes after adding a new component to a container successfully', (done) => {
      spyOn(HstService, 'addHstComponent').and.returnValue($q.resolve({
        reloadRequired: false,
        data: {
          id: 'new-component',
        },
      }));

      PageStructureService.addComponentToContainer(component, container)
        .then(() => {
          expect(ChannelService.checkChanges).toHaveBeenCalled();
          done();
        });

      $rootScope.$digest();
    });

    it('shows the default error message when failed to add a new component from catalog', (done) => {
      spyOn(FeedbackService, 'showError');
      spyOn(HstService, 'addHstComponent').and.returnValue(
        $q.reject({
          message: 'error-message',
          data: {
            error: 'cafebabe-error-key',
            parameterMap: {},
          },
        }),
      );

      PageStructureService.addComponentToContainer(component, container)
        .catch((errorMessage) => {
          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT', {
            component: 'Mock Component',
          });
          expect(errorMessage).toBe('error-message');
          done();
        });

      $rootScope.$digest();
    });

    it('shows the locked error message when adding a new component on a container locked by another user', (done) => {
      spyOn(FeedbackService, 'showError');
      spyOn(HstService, 'addHstComponent').and.returnValue(
        $q.reject({
          message: 'error-message',
          data: {
            error: 'ITEM_ALREADY_LOCKED',
            parameterMap: {
              lockedBy: 'another-user',
              lockedOn: 1234,
            },
          },
        }),
      );

      PageStructureService.addComponentToContainer(component, container)
        .catch((errorMessage) => {
          expect(HstService.addHstComponent).toHaveBeenCalledWith(component, container, undefined);
          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT_ITEM_ALREADY_LOCKED', {
            lockedBy: 'another-user',
            lockedOn: 1234,
            component: 'Mock Component',
          });
          expect(errorMessage).toBe('error-message');
          done();
        });

      $rootScope.$digest();
    });
  });

  describe('removeComponentById', () => {
    beforeEach(() => {
      spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.resolve('new-markup'));
      mockParseElements(
        mockPage('page-1',
          mockContainer('Container 1', 'container-1', 'HST.vBox',
            mockItem('Component 1', 'component-1'))),
      );

      PageStructureService.parseElements();
      $rootScope.$digest();
    });

    it('removes a valid component and calls HST successfully', (done) => {
      spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.resolve([]));

      PageStructureService.removeComponentById('component-1').then((container) => {
        expect(container.getId()).toBe('container-1');
        expect(HstComponentService.deleteComponent).toHaveBeenCalledWith('container-1', 'component-1');
        expect(ChannelService.checkChanges).toHaveBeenCalled();
        done();
      });

      $rootScope.$digest();
    });

    it('rejects and shows an error feedback message when the HST call fails due to an unknown reason', (done) => {
      spyOn(FeedbackService, 'showError');
      // mock the call to HST to be failed
      spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.reject({ error: 'unknown', parameterMap: {} }));

      PageStructureService.removeComponentById('component-1')
        .catch(() => {
          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_COMPONENT',
            jasmine.objectContaining({ component: 'Component 1' }));
          done();
        });

      $rootScope.$digest();
    });

    it('shows an error feedback message if a locked item is removed', (done) => {
      spyOn(FeedbackService, 'showError');
      // mock the call to HST to be failed
      spyOn(HstComponentService, 'deleteComponent')
        .and.returnValue($q.reject({ error: 'ITEM_ALREADY_LOCKED', parameterMap: {} }));

      PageStructureService.removeComponentById('component-1')
        .catch(() => {
          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_COMPONENT_ITEM_ALREADY_LOCKED',
            jasmine.objectContaining({ component: 'Component 1' }));
          done();
        });

      $rootScope.$digest();
    });

    it('rejects if an unknown component is removed', (done) => {
      spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.when([]));

      PageStructureService.removeComponentById('unknown-component')
        .catch(() => {
          expect(HstComponentService.deleteComponent).not.toHaveBeenCalled();
          done();
        });

      $rootScope.$digest();
    });
  });

  describe('moveComponent', () => {
    beforeEach(() => {
      spyOn(HstService, 'updateHstContainer').and.returnValue($q.resolve({}));
      spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.resolve('new-markup'));
      mockParseElements(
        mockPage('page-1',
          mockContainer('Container 1', 'container-1', 'HST.vBox',
            mockItem('Component 1', 'component-1'),
            mockItem('Component 2', 'component-2')),
          mockContainer('Container 2', 'container-2', 'HST.vBox'),
          mockEditMenuLink('edit-menu-link')),
      );

      PageStructureService.parseElements();
      $rootScope.$digest();
    });

    function componentIds(container) {
      return container.getComponents().map(component => component.getId());
    }

    it('can move the first component to the second position in the container', () => {
      const container = PageStructureService.getPage().getContainerById('container-1');
      const componentA = container.getComponent('component-1');

      expect(componentIds(container)).toEqual(['component-1', 'component-2']);

      PageStructureService.moveComponent(componentA, container, undefined);
      $rootScope.$digest();

      expect(HstService.updateHstContainer).toHaveBeenCalledWith('container-1', container.getHstRepresentation());
      expect(componentIds(container)).toEqual(['component-2', 'component-1']);
      expect(ChannelService.checkChanges).toHaveBeenCalled();
    });

    it('can move the second component to the first position in the container', () => {
      const container = PageStructureService.getPage().getContainerById('container-1');
      const component1 = container.getComponent('component-1');
      const component2 = container.getComponent('component-2');

      expect(componentIds(container)).toEqual(['component-1', 'component-2']);

      PageStructureService.moveComponent(component2, container, component1);
      $rootScope.$digest();

      expect(HstService.updateHstContainer).toHaveBeenCalledWith('container-1', container.getHstRepresentation());
      expect(componentIds(container)).toEqual(['component-2', 'component-1']);
      expect(ChannelService.checkChanges).toHaveBeenCalled();
    });

    it('can move a component to another container', () => {
      const page = PageStructureService.getPage();
      const container1 = page.getContainerById('container-1');
      const component1 = container1.getComponent('component-1');
      const container2 = page.getContainerById('container-2');

      PageStructureService.moveComponent(component1, container2, undefined);
      $rootScope.$digest();

      expect(HstService.updateHstContainer).toHaveBeenCalledWith('container-1', container1.getHstRepresentation());
      expect(HstService.updateHstContainer).toHaveBeenCalledWith('container-2', container2.getHstRepresentation());
      expect(componentIds(container1)).toEqual(['component-2']);
      expect(componentIds(container2)).toEqual(['component-1']);
      expect(ChannelService.checkChanges).toHaveBeenCalled();
    });

    it('returns the affected containers', (done) => {
      const page = PageStructureService.getPage();
      const container1 = page.getContainerById('container-1');
      const component1 = container1.getComponent('component-1');
      const container2 = page.getContainerById('container-2');

      PageStructureService.moveComponent(component1, container2, undefined)
        .then(({ changedContainers }) => {
          const [changedContainer1, changedContainer2] = changedContainers;
          expect(container1).toEqual(changedContainer1);
          expect(container2).toEqual(changedContainer2);
          done();
        });

      $rootScope.$digest();
    });

    it('returns "reloadRequired" if one of the backend calls requires it', (done) => {
      const page = PageStructureService.getPage();
      const container1 = page.getContainerById('container-1');
      const component1 = container1.getComponent('component-1');
      const container2 = page.getContainerById('container-2');

      HstService.updateHstContainer.and.returnValues(
        $q.resolve({ reloadRequired: false }),
        $q.resolve({ reloadRequired: true }),
      );

      PageStructureService.moveComponent(component1, container2, undefined)
        .then(({ reloadRequired }) => {
          expect(reloadRequired).toBe(true);
          done();
        });

      $rootScope.$digest();
    });

    it('shows an error when a component is moved within a container that just got locked by another user', () => {
      const page = PageStructureService.getPage();
      const container1 = page.getContainerById('container-1');
      const component1 = container1.getComponent('component-1');

      HstService.updateHstContainer.and.returnValue($q.reject());
      spyOn(FeedbackService, 'showError');

      PageStructureService.moveComponent(component1, container1, undefined);
      $rootScope.$digest();

      expect(HstService.updateHstContainer).toHaveBeenCalledWith('container-1', container1.getHstRepresentation());
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
        component: 'Component 1',
      });
      expect(ChannelService.checkChanges).not.toHaveBeenCalled();
    });

    it('shows an error when a component is moved out of a container that just got locked by another user', () => {
      const page = PageStructureService.getPage();
      const container1 = page.getContainerById('container-1');
      const component1 = container1.getComponent('component-1');
      const container2 = page.getContainerById('container-2');

      HstService.updateHstContainer.and.returnValues($q.reject(), $q.resolve());
      spyOn(FeedbackService, 'showError');

      PageStructureService.moveComponent(component1, container2, undefined);
      $rootScope.$digest();

      expect(HstService.updateHstContainer).toHaveBeenCalledWith('container-1', container1.getHstRepresentation());
      expect(HstService.updateHstContainer).toHaveBeenCalledWith('container-2', container2.getHstRepresentation());
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
        component: 'Component 1',
      });
      expect(ChannelService.checkChanges).not.toHaveBeenCalled();
    });

    it('shows an error when a component is moved into a container that just got locked by another user', () => {
      const page = PageStructureService.getPage();
      const container1 = page.getContainerById('container-1');
      const component1 = container1.getComponent('component-1');
      const container2 = page.getContainerById('container-2');

      HstService.updateHstContainer.and.returnValues($q.resolve(), $q.reject());
      spyOn(FeedbackService, 'showError');

      PageStructureService.moveComponent(component1, container2, undefined);
      $rootScope.$digest();

      expect(HstService.updateHstContainer).toHaveBeenCalledWith('container-1', container1.getHstRepresentation());
      expect(HstService.updateHstContainer).toHaveBeenCalledWith('container-2', container2.getHstRepresentation());
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
        component: 'Component 1',
      });
      expect(ChannelService.checkChanges).not.toHaveBeenCalled();
    });
  });

  describe('renderComponent', () => {
    it('gracefully handles requests to re-render an undefined or null component', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup');

      $q.all(
        PageStructureService.renderComponent(),
        PageStructureService.renderComponent(null),
      ).then(() => {
        expect(MarkupService.fetchComponentMarkup).not.toHaveBeenCalled();
        done();
      });

      $rootScope.$digest();
    });

    it('loads the component markup from the backend using the MarkupService', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.resolve('markup'));
      const component = {};
      const properties = {};

      PageStructureService.renderComponent(component, properties)
        .then(() => {
          expect(MarkupService.fetchComponentMarkup).toHaveBeenCalledWith(component, properties);
          done();
        });

      $rootScope.$digest();
    });

    it('shows an error message and reloads the page when a component has been deleted', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.reject({ status: 404 }));
      spyOn(FeedbackService, 'showDismissible');

      PageStructureService.renderComponent({}).catch(() => {
        expect(FeedbackService.showDismissible).toHaveBeenCalledWith('FEEDBACK_NOT_FOUND_MESSAGE');
        done();
      });

      $rootScope.$digest();
    });

    it('does nothing if markup for a component cannot be retrieved but status is not 404', (done) => {
      spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.reject({}));
      spyOn(FeedbackService, 'showError');

      PageStructureService.renderComponent({}).then(() => {
        expect(FeedbackService.showError).not.toHaveBeenCalled();
        done();
      });

      $rootScope.$digest();
    });
  });
});
