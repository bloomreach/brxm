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

import angular from 'angular';
import 'angular-mocks';

function containerComment(label, type, uuid) {
  return `<!-- {
    "HST-Type": "CONTAINER_COMPONENT",
    "HST-Label": "${label}",
    "HST-XType": "${type}",
    "uuid": "${uuid}"
  } -->`;
}

function itemComment(label, uuid) {
  return `<!-- {
    "HST-Type": "CONTAINER_ITEM_COMPONENT",
    "HST-Label": "${label}",
    "uuid": "${uuid}"
  } -->`;
}

function manageContentLinkComment(documentTemplateQuery) {
  return `<!-- {
    "HST-Type": "MANAGE_CONTENT_LINK",
    "documentTemplateQuery": "${documentTemplateQuery}"
  } -->`;
}

function editMenuLinkComment(uuid) {
  return `<!-- {
    "HST-Type": "EDIT_MENU_LINK",
    "uuid": "${uuid}"
  } -->`;
}

function unprocessedHeadContributionsComment(headElements) {
  return `<!-- {
    "HST-Type": "HST_UNPROCESSED_HEAD_CONTRIBUTIONS",
    "headElements": ["${headElements}"]
  } -->`;
}

function endComment(uuid) {
  return `<!-- {
    "HST-End": "true",
    "uuid": "${uuid}"
  } -->`;
}

describe('PageStructureService', () => {
  let $document;
  let $log;
  let $rootScope;
  let $q;

  let ChannelService;
  let FeedbackService;
  let HippoIframeService;
  let HstComponentService;
  let HstService;
  let MarkupService;
  let PageMetaDataService;
  let PageStructureService;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe.page');

    inject((
      _$document_,
      _$log_,
      _$q_,
      _$rootScope_,
      _$window_,
      _ChannelService_,
      _EditComponentService_,
      _FeedbackService_,
      _HippoIframeService_,
      _HstComponentService_,
      _HstService_,
      _MarkupService_,
      _PageMetaDataService_,
      _PageStructureService_,
    ) => {
      $document = _$document_;
      $log = _$log_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ChannelService = _ChannelService_;
      FeedbackService = _FeedbackService_;
      HippoIframeService = _HippoIframeService_;
      HstComponentService = _HstComponentService_;
      HstService = _HstService_;
      MarkupService = _MarkupService_;
      PageMetaDataService = _PageMetaDataService_;
      PageStructureService = _PageStructureService_;
    });

    spyOn(ChannelService, 'recordOwnChange');
  });

  beforeEach(() => {
    jasmine.getFixtures().load('channel/hippoIframe/page/pageStructure.service.fixture.html');
  });

  it('has no containers initially', () => {
    expect(PageStructureService.getContainers()).toEqual([]);
  });

  it('has no embedded links initially', () => {
    expect(PageStructureService.getEmbeddedLinks()).toEqual([]);
  });

  it('rejects components if there is no container yet', () => {
    spyOn($log, 'warn');
    PageStructureService.registerParsedElement(undefined, {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
    });

    expect(PageStructureService.getContainers()).toEqual([]);
    expect($log.warn).toHaveBeenCalled();
  });

  const childComment = (element) => {
    const children = element.childNodes;
    for (let i = 0; i < children.length; i += 1) {
      const child = children[i];
      if (child.nodeType === 8) {
        return child;
      }
    }
    return null;
  };

  const previousComment = (element) => {
    while (element.previousSibling) {
      element = element.previousSibling;
      if (element.nodeType === 8) {
        return element;
      }
    }
    return null;
  };

  const nextComment = (element) => {
    while (element.nextSibling) {
      element = element.nextSibling;
      if (element.nodeType === 8) {
        return element;
      }
    }
    return null;
  };

  const registerParsedElement = (commentElement) => {
    PageStructureService.registerParsedElement(commentElement, JSON.parse(commentElement.data));
  };

  const registerEmptyVBoxContainer = () => {
    const container = $j('#container-vbox-empty', $document)[0];
    registerParsedElement(previousComment(container));
    return container;
  };

  const registerVBoxContainer = () => {
    const container = $j('#container-vbox', $document)[0];
    registerParsedElement(previousComment(container));
    return container;
  };

  const registerVBoxComponent = (id) => {
    const component = $j(`#${id}`, $document)[0];
    registerParsedElement(childComment(component));
    return component;
  };

  const registerNoMarkupContainer = (id = '#container-no-markup') => {
    const container = $j(id, $document)[0];
    registerParsedElement(childComment(container));
    return container;
  };

  const registerEmptyNoMarkupContainer = () => registerNoMarkupContainer('#container-no-markup-empty');

  const registerLowercaseNoMarkupContainer = () => registerNoMarkupContainer('#container-no-markup-lowercase');

  const registerEmptyLowercaseNoMarkupContainer = () => {
    registerNoMarkupContainer('#container-no-markup-lowercase-empty');
  };

  const registerNoMarkupContainerWithoutTextNodesAfterEndComment = () => {
    registerNoMarkupContainer('#container-no-markup-without-text-nodes-after-end-comment');
  };

  const registerNoMarkupComponent = () => {
    const component = $j('#component-no-markup', $document)[0];
    registerParsedElement(previousComment(component));
    return component;
  };

  const registerEmptyNoMarkupComponent = () => {
    registerParsedElement(nextComment(childComment($j('#container-no-markup', $document)[0])));
  };

  const registerEmbeddedLink = (selector) => {
    registerParsedElement(childComment($j(selector, $document)[0]));
  };

  const registerHeadContributions = (selector) => {
    registerParsedElement(childComment($j(selector, $document)[0]));
  };

  it('registers containers in the correct order', () => {
    const container1 = registerVBoxContainer();
    const container2 = registerNoMarkupContainer();

    const containers = PageStructureService.getContainers();
    expect(containers.length).toEqual(2);

    expect(containers[0].type).toEqual('container');
    expect(containers[0].isEmpty()).toEqual(true);
    expect(containers[0].getComponents()).toEqual([]);
    expect(containers[0].getBoxElement()[0]).toEqual(container1);
    expect(containers[0].getLabel()).toEqual('vBox container');

    expect(containers[1].type).toEqual('container');
    expect(containers[1].isEmpty()).toEqual(true);
    expect(containers[1].getComponents()).toEqual([]);
    expect(containers[1].getBoxElement()[0]).toEqual(container2);
    expect(containers[1].getLabel()).toEqual('NoMarkup container');

    expect(PageStructureService.hasContainer(containers[0])).toEqual(true);
    expect(PageStructureService.hasContainer(containers[1])).toEqual(true);
  });

  it('adds components to the most recently registered container', () => {
    registerVBoxContainer();
    registerVBoxContainer();

    const componentA = registerVBoxComponent('componentA');
    const componentB = registerVBoxComponent('componentB');

    const containers = PageStructureService.getContainers();
    expect(containers.length).toEqual(2);
    expect(containers[0].isEmpty()).toEqual(true);
    expect(containers[1].isEmpty()).toEqual(false);
    expect(containers[1].getComponents().length).toEqual(2);

    expect(containers[1].getComponents()[0].type).toEqual('component');
    expect(containers[1].getComponents()[0].getBoxElement()[0]).toBe(componentA);
    expect(containers[1].getComponents()[0].getLabel()).toEqual('component A');
    expect(containers[1].getComponents()[0].container).toEqual(containers[1]);

    expect(containers[1].getComponents()[1].type).toEqual('component');
    expect(containers[1].getComponents()[1].getBoxElement()[0]).toBe(componentB);
    expect(containers[1].getComponents()[1].getLabel()).toEqual('component B');
    expect(containers[1].getComponents()[1].container).toEqual(containers[1]);
  });

  it('registers edit menu links', () => {
    registerEmbeddedLink('#edit-menu-in-page');
    const editMenuLinks = PageStructureService.getEmbeddedLinks();
    expect(editMenuLinks.length).toBe(1);
    expect(editMenuLinks[0].getUuid()).toBe('menu-in-page');
  });

  it('registers manage content links', () => {
    registerEmbeddedLink('#manage-content-in-page');
    const manageContentLinks = PageStructureService.getEmbeddedLinks();
    const manageContentLink = manageContentLinks[0];
    expect(manageContentLink.getDefaultPath()).toBe('test-default-path');
    expect(manageContentLink.getDocumentTemplateQuery()).toBe('new-test-document');
    expect(manageContentLink.getParameterName()).toBe('test-component-parameter');
    expect(manageContentLink.getParameterValue()).toBe('test-component-value');
    expect(manageContentLink.getPickerConfig()).toEqual({
      configuration: 'test-component-picker configuration',
      initialPath: 'test-component-picker-initial-path',
      isRelativePath: false,
      remembersLastVisited: false,
      rootPath: 'test-component-picker-root-path',
      selectableNodeTypes: ['test-node-type-1', 'test-node-type-2'],
    });
    expect(manageContentLink.getRootPath()).toBe('test-root-path');
    expect(manageContentLink.isParameterValueRelativePath()).toBe(true);
  });

  it('recognizes a manage content link for a parameter that stores an absolute path', () => {
    registerEmbeddedLink('#manage-content-with-absolute-path');
    const manageContentLinks = PageStructureService.getEmbeddedLinks();
    const manageContentLink = manageContentLinks[0];
    expect(manageContentLink.getDocumentTemplateQuery()).toBe('new-test-document');
    expect(manageContentLink.getDefaultPath()).toBe('test-default-path');
    expect(manageContentLink.getRootPath()).toBe('test-root-path');
    expect(manageContentLink.getParameterName()).toBe('test-component-parameter');
    expect(manageContentLink.getParameterValue()).toBe('test-component-value');
    expect(manageContentLink.getPickerConfig()).toEqual({
      configuration: 'test-component-picker configuration',
      initialPath: 'test-component-picker-initial-path',
      isRelativePath: false,
      remembersLastVisited: false,
      rootPath: 'test-component-picker-root-path',
      selectableNodeTypes: ['test-node-type-1', 'test-node-type-2'],
    });
    expect(manageContentLink.isParameterValueRelativePath()).toBe(false);
  });

  it('registers processed and unprocessed head contributions', () => {
    registerHeadContributions('#processed-head-contributions');
    registerHeadContributions('#unprocessed-head-contributions');

    expect(PageStructureService.headContributions).toEqual([
      '<title>processed</title>',
      '<script>window.processed = true</script>',
      '<link href="unprocessed.css">',
    ]);
  });

  it('clears the page structure', () => {
    registerVBoxContainer();
    registerEmbeddedLink('#manage-content-in-page');
    registerEmbeddedLink('#edit-menu-in-page');
    registerHeadContributions('#processed-head-contributions');

    expect(PageStructureService.getContainers().length).toEqual(1);
    expect(PageStructureService.getEmbeddedLinks().length).toEqual(2);
    expect(PageStructureService.headContributions.length).toBe(2);

    PageStructureService.clearParsedElements();

    expect(PageStructureService.getContainers().length).toEqual(0);
    expect(PageStructureService.getEmbeddedLinks().length).toEqual(0);
    expect(PageStructureService.headContributions.length).toBe(0);
  });

  it('registers additional elements', () => {
    registerVBoxContainer();

    const testElement = $j('#test', $document);

    const containers = PageStructureService.getContainers();
    containers[0].setJQueryElement('test', testElement);

    expect(containers[0].getJQueryElement('test')).toEqual(testElement);
  });

  it('finds the DOM element of a no-markup container as parent of the comment', () => {
    const container = registerNoMarkupContainer();

    const containers = PageStructureService.getContainers();
    expect(containers.length).toEqual(1);
    expect(containers[0].getBoxElement()[0]).toBe(container);
  });

  it('finds the DOM element of a component of a no-markup container as next sibling of the comment', () => {
    registerNoMarkupContainer();

    const component = registerNoMarkupComponent();

    const containers = PageStructureService.getContainers();
    expect(containers.length).toEqual(1);
    expect(containers[0].isEmpty()).toEqual(false);
    expect(containers[0].getComponents().length).toEqual(1);
    expect(containers[0].getComponents()[0].getBoxElement()[0]).toBe(component);
    expect(containers[0].getComponents()[0].hasNoIFrameDomElement()).not.toEqual(true);
  });

  it('registers no iframe box element in case of a no-markup, empty component', () => {
    registerNoMarkupContainer();
    registerEmptyNoMarkupComponent();

    const containers = PageStructureService.getContainers();
    expect(containers.length).toEqual(1);
    expect(containers[0].isEmpty()).toEqual(false);
    expect(containers[0].getComponents().length).toEqual(1);
    expect(containers[0].getComponents()[0].getBoxElement().length).toEqual(0);
  });

  it('detects if a container contains DOM elements that represent a container-item', () => {
    registerVBoxContainer();
    registerNoMarkupContainer();
    registerLowercaseNoMarkupContainer();

    const containers = PageStructureService.getContainers();
    expect(containers[0].isEmptyInDom()).toEqual(false);
    expect(containers[1].isEmptyInDom()).toEqual(false);
    expect(containers[2].isEmptyInDom()).toEqual(false);
  });

  it('detects if a container does not contain DOM elements that represent a container-item', () => {
    registerEmptyVBoxContainer();
    registerEmptyNoMarkupContainer();
    registerEmptyLowercaseNoMarkupContainer();

    const containers = PageStructureService.getContainers();
    expect(containers[0].isEmptyInDom()).toEqual(true);
    expect(containers[1].isEmptyInDom()).toEqual(true);
    expect(containers[2].isEmptyInDom()).toEqual(true);
  });

  it('parses the page meta-data and adds it to the PageMetaDataService', () => {
    spyOn(PageMetaDataService, 'add');

    PageStructureService.registerParsedElement(null, {
      'HST-Type': 'PAGE-META-DATA',
      'HST-Mount-Id': 'testMountId',
    });

    expect(PageMetaDataService.add).toHaveBeenCalledWith({
      'HST-Mount-Id': 'testMountId',
    });
  });

  it('ignores unknown HST types', () => {
    PageStructureService.registerParsedElement(null, {
      'HST-Type': 'unknown type',
    });
  });

  it('returns a known component', () => {
    registerVBoxContainer();
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    const pageComponent = PageStructureService.getComponentById('aaaa');

    expect(pageComponent).not.toBeNull();
    expect(pageComponent.getId()).toEqual('aaaa');
    expect(pageComponent.getLabel()).toEqual('component A');
  });

  it('returns null when getting an unknown component', () => {
    expect(PageStructureService.getComponentById('no-such-component')).toBeNull();
  });

  it('removes a valid component and calls HST successfully', () => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.when([]));
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(''));

    PageStructureService.removeComponentById('aaaa');

    $rootScope.$digest();

    expect(HstComponentService.deleteComponent).toHaveBeenCalledWith('container-vbox', 'aaaa');
    expect(ChannelService.recordOwnChange).toHaveBeenCalled();
  });

  it('removes a valid component but fails to call HST due to an unknown reason, then iframe should be reloaded and a feedback toast should be shown', () => { // eslint-disable-line max-len
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    spyOn(FeedbackService, 'showError');
    spyOn(HippoIframeService, 'reload').and.returnValue($q.when(''));
    // mock the call to HST to be failed
    spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.reject({ error: 'unknown', parameterMap: {} }));

    PageStructureService.removeComponentById('aaaa');
    $rootScope.$digest();

    expect(HstComponentService.deleteComponent).toHaveBeenCalledWith('container-vbox', 'aaaa');

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_COMPONENT',
      jasmine.objectContaining({ component: 'component A' }));

    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('removes a valid component but fails to call HST due to locked component then iframe should be reloaded and a feedback toast should be shown', () => { // eslint-disable-line max-len
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    spyOn(FeedbackService, 'showError');
    spyOn(HippoIframeService, 'reload').and.returnValue($q.when(''));
    // mock the call to HST to be failed
    spyOn(HstComponentService, 'deleteComponent')
      .and.returnValue($q.reject({ error: 'ITEM_ALREADY_LOCKED', parameterMap: {} }));

    PageStructureService.removeComponentById('aaaa');
    $rootScope.$digest();

    expect(HstComponentService.deleteComponent).toHaveBeenCalledWith('container-vbox', 'aaaa');

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_DELETE_COMPONENT_ITEM_ALREADY_LOCKED',
      jasmine.objectContaining({ component: 'component A' }));

    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('removes an invalid component', () => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    spyOn(HstComponentService, 'deleteComponent').and.returnValue($q.when([]));

    PageStructureService.removeComponentById('unknown-component');
    $rootScope.$digest();

    expect(HstComponentService.deleteComponent).not.toHaveBeenCalled();
  });

  it('returns a container by iframe element', () => {
    registerVBoxContainer();
    const containerElement = registerNoMarkupContainer();

    const container = PageStructureService.getContainerByIframeElement(containerElement);

    expect(container).not.toBeNull();
    expect(container.getId()).toEqual('container-no-markup');
  });

  it('shows the default error message when failed to add a new component from catalog', () => {
    const catalogComponent = {
      id: 'foo-bah',
      name: 'Foo Bah Component',
    };
    const mockContainer = jasmine.createSpyObj(['getId']);
    mockContainer.getId.and.returnValue('container-1');

    spyOn(FeedbackService, 'showError');
    spyOn(HippoIframeService, 'reload').and.returnValue($q.when());
    const deferred = $q.defer();
    spyOn(HstService, 'addHstComponent').and.returnValue(deferred.promise);

    PageStructureService.addComponentToContainer(catalogComponent, mockContainer);
    deferred.reject({
      error: 'cafebabe-error-key',
      parameterMap: {},
    });
    $rootScope.$digest();

    expect(HstService.addHstComponent).toHaveBeenCalledWith(catalogComponent, 'container-1');
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT', { component: 'Foo Bah Component' });
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('shows the locked error message when adding a new component and the container was locked by another user', () => {
    const catalogComponent = {
      id: 'foo-bah',
      name: 'Foo Bah Component',
    };
    const mockContainer = jasmine.createSpyObj(['getId']);
    mockContainer.getId.and.returnValue('container-1');

    spyOn(HippoIframeService, 'reload').and.returnValue($q.when());
    spyOn(FeedbackService, 'showError');
    const deferred = $q.defer();
    spyOn(HstService, 'addHstComponent').and.returnValue(deferred.promise);

    PageStructureService.addComponentToContainer(catalogComponent, mockContainer);
    deferred.reject({
      error: 'ITEM_ALREADY_LOCKED',
      parameterMap: {
        lockedBy: 'another-user',
        lockedOn: 1234,
      },
    });
    $rootScope.$digest();

    expect(HstService.addHstComponent).toHaveBeenCalledWith(catalogComponent, 'container-1');
    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_ADD_COMPONENT_ITEM_ALREADY_LOCKED', {
      lockedBy: 'another-user',
      lockedOn: 1234,
      component: 'Foo Bah Component',
    });
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('records a change when adding a new component to a container successfully', (done) => {
    const catalogComponent = {
      id: 'foo-bah',
      name: 'Foo Bah Component',
    };
    const mockContainer = jasmine.createSpyObj(['getId']);
    mockContainer.getId.and.returnValue('container-1');

    spyOn(HstService, 'addHstComponent').and.returnValue($q.resolve({ id: 'newUuid' }));

    PageStructureService.addComponentToContainer(catalogComponent, mockContainer)
      .then((newComponentId) => {
        expect(HstService.addHstComponent).toHaveBeenCalledWith(catalogComponent, 'container-1');
        expect(ChannelService.recordOwnChange).toHaveBeenCalled();
        expect(newComponentId).toEqual('newUuid');
        done();
      });
    $rootScope.$digest();
  });

  it('prints parsed elements', () => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');
    registerVBoxComponent('componentB');

    spyOn($log, 'debug');

    PageStructureService.printParsedElements();

    expect($log.debug.calls.count()).toEqual(3);
  });

  it('attaches the embedded link to the enclosing component', () => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');
    registerVBoxComponent('componentB');
    registerEmbeddedLink('#edit-menu-in-component-a');
    registerEmbeddedLink('#edit-menu-in-page');
    registerEmbeddedLink('#manage-content-in-page');
    registerEmbeddedLink('#manage-content-in-container-vbox');

    const containerVBox = PageStructureService.getContainers()[0];
    const componentA = containerVBox.getComponents()[0];
    const embeddedLinks = PageStructureService.getEmbeddedLinks();

    expect(embeddedLinks.length).toBe(4);
    embeddedLinks.forEach(embeddedLink => expect(embeddedLink.getEnclosingElement()).toBeUndefined());

    expect(embeddedLinks[0].getBoxElement().length).toBe(0);

    PageStructureService.attachEmbeddedLinks();

    const attachedEmbeddedLinks = PageStructureService.getEmbeddedLinks();
    expect(attachedEmbeddedLinks.length).toBe(4);
    expect(attachedEmbeddedLinks[0].getEnclosingElement()).toBe(componentA);
    expect(attachedEmbeddedLinks[1].getEnclosingElement()).toBe(null);
    expect(attachedEmbeddedLinks[2].getEnclosingElement()).toBe(null);
    expect(attachedEmbeddedLinks[3].getEnclosingElement()).toBe(containerVBox);

    expect(attachedEmbeddedLinks[0].getBoxElement().length).toBe(1);
    expect(attachedEmbeddedLinks[0].getBoxElement().attr('class')).toBe('hst-fab');
  });

  it('re-renders a component with an edit menu link', () => {
    // set up page structure with component and edit menu link in it
    registerVBoxContainer();
    registerVBoxComponent('componentA');
    registerEmbeddedLink('#edit-menu-in-component-a');
    registerEmbeddedLink('#edit-menu-in-page');
    PageStructureService.attachEmbeddedLinks();

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-edit-menu-in-component-a">
          ${editMenuLinkComment('updated-menu-in-component-a')}
        </p>
      ${endComment('aaaa')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    const updatedComponentA = PageStructureService.getContainers()[0].getComponents()[0];
    const editMenuLinks = PageStructureService.getEmbeddedLinks();
    expect(editMenuLinks.length).toBe(2);
    expect(editMenuLinks[0].getEnclosingElement()).toBe(null);
    expect(editMenuLinks[1].getEnclosingElement()).toBe(updatedComponentA);
  });

  it('re-renders a component with no more content link', () => {
    // set up page structure with component and content link in it
    registerNoMarkupContainer();
    registerNoMarkupComponent();
    registerEmbeddedLink('#manage-content-in-component-no-markup');
    PageStructureService.attachEmbeddedLinks();

    const updatedMarkup = `
      ${itemComment('Component in NoMarkup container', 'component-no-markup')}
        <div id="component-no-markup">
          <p>Some markup in component D</p>
        </div>
      ${endComment('component-no-markup')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getComponentById('component-no-markup');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    expect(PageStructureService.getEmbeddedLinks().length).toBe(0);
  });

  it('re-renders a component, adding an edit menu link', () => {
    // set up page structure with component and edit menu link in it
    registerVBoxContainer();
    registerVBoxComponent('componentA');
    PageStructureService.attachEmbeddedLinks();

    expect(PageStructureService.getEmbeddedLinks().length).toBe(0);

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-edit-menu-in-component-a">
          ${editMenuLinkComment('updated-menu-in-component-a')}
        </p>
      ${endComment('aaaa')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    const updatedComponentA = PageStructureService.getContainers()[0].getComponents()[0];
    const embeddedLinks = PageStructureService.getEmbeddedLinks();
    expect(embeddedLinks.length).toBe(1);
    expect(embeddedLinks[0].getEnclosingElement()).toBe(updatedComponentA);
  });

  it('gracefully re-renders a component twice quickly after eachother', () => {
    // set up page structure with component
    registerVBoxContainer();
    registerVBoxComponent('componentB');

    const updatedMarkup = `
      ${itemComment('component B', 'bbbb')}
        <p>Re-rendered component B</p>
      ${endComment('bbbb')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getComponentById('bbbb');
    PageStructureService.renderComponent(component);
    PageStructureService.renderComponent(component);

    expect(() => {
      $rootScope.$digest();
    }).not.toThrow();
  });

  it('gracefully handles requests to re-render an undefined or null component', () => {
    spyOn(MarkupService, 'fetchComponentMarkup');

    PageStructureService.renderComponent();
    PageStructureService.renderComponent(null);

    expect(MarkupService.fetchComponentMarkup).not.toHaveBeenCalled();
  });

  it('shows an error message and reloads the page when a component has been deleted', (done) => {
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.reject({ status: 404 }));
    spyOn(HippoIframeService, 'reload');
    spyOn(FeedbackService, 'showDismissible');

    PageStructureService.renderComponent({}).catch(() => {
      expect(HippoIframeService.reload).toHaveBeenCalled();
      expect(FeedbackService.showDismissible).toHaveBeenCalledWith('FEEDBACK_NOT_FOUND_MESSAGE');
      done();
    });
    $rootScope.$digest();
  });

  it('does nothing if markup for a component cannot be retrieved but status is not 404', (done) => {
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.reject({}));
    spyOn(HippoIframeService, 'reload');
    spyOn(FeedbackService, 'showError');

    PageStructureService.renderComponent({}).then(() => {
      expect(HippoIframeService.reload).not.toHaveBeenCalled();
      expect(FeedbackService.showError).not.toHaveBeenCalled();
      done();
    });
    $rootScope.$digest();
  });

  it('does not add a re-rendered and incorrectly commented component to the page structure', () => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-edit-menu-in-component-a">
          ${editMenuLinkComment('updated-menu-in-component-a')}
        </p>
      `;
    spyOn($log, 'error');
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));

    const component = PageStructureService.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    expect(PageStructureService.getContainers().length).toBe(1);
    expect(PageStructureService.getContainers()[0].getComponents().length).toBe(0);
    expect($log.error).toHaveBeenCalled();
  });

  it('knows that a re-rendered component contains new head contributions', () => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-component-with-new-head-contribution">
        </p>
      ${endComment('aaaa')}
      ${unprocessedHeadContributionsComment('<script>window.newScript=true</script>')}
      `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));
    spyOn(HippoIframeService, 'reload');

    const component = PageStructureService.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    const updatedComponent = PageStructureService.getContainers()[0].getComponents()[0];
    expect(PageStructureService.containsNewHeadContributions(updatedComponent)).toBe(true);
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('knows that a re-rendered component does not contain new head contributions', () => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
      <p id="updated-component-with-new-head-contribution">
      </p>
      ${endComment('aaaa')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));
    spyOn(HippoIframeService, 'reload');

    const component = PageStructureService.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    const updatedComponentA = PageStructureService.getContainers()[0].getComponents()[0];
    expect(PageStructureService.containsNewHeadContributions(updatedComponentA)).toBe(false);
    expect(HippoIframeService.reload).not.toHaveBeenCalled();
  });

  it('knows that a re-rendered component does not contain new head contributions if they have already been rendered by the page', () => { // eslint-disable-line max-len
    registerVBoxContainer();
    registerVBoxComponent('componentA');
    registerHeadContributions('#processed-head-contributions');

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-component-with-new-head-contribution">
        </p>
      ${endComment('aaaa')}
      ${unprocessedHeadContributionsComment('<script>window.processed = true</script>')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));
    spyOn(HippoIframeService, 'reload');

    const component = PageStructureService.getComponentById('aaaa');
    PageStructureService.renderComponent(component);
    $rootScope.$digest();

    const updatedComponent = PageStructureService.getContainers()[0].getComponents()[0];
    expect(PageStructureService.containsNewHeadContributions(updatedComponent)).toBe(false);
    expect(HippoIframeService.reload).not.toHaveBeenCalled();
  });

  it('does not reload the page when a component is re-rendered with custom properties', () => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-component-with-new-head-contribution">
        </p>
      ${endComment('aaaa')}
      ${unprocessedHeadContributionsComment('<script>window.newScript=true</script>')}
    `;
    spyOn(MarkupService, 'fetchComponentMarkup').and.returnValue($q.when({ data: updatedMarkup }));
    spyOn(HippoIframeService, 'reload');

    const component = PageStructureService.getComponentById('aaaa');
    const propertiesMap = {
      parameter: 'customValue',
    };
    PageStructureService.renderComponent(component, propertiesMap);
    $rootScope.$digest();

    const updatedComponent = PageStructureService.getContainers()[0].getComponents()[0];
    expect(PageStructureService.containsNewHeadContributions(updatedComponent)).toBe(true);
    expect(HippoIframeService.reload).not.toHaveBeenCalled();
  });

  it('notifies change listeners when updating a component', () => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    const container = PageStructureService.getContainers()[0];
    const component = container.getComponents()[0];
    const updatedMarkup = `
      ${itemComment('component A', 'aaaa')}
        <p id="updated-component-with-new-head-contribution">
        </p>
      ${endComment('aaaa')}
    `;

    spyOn(PageStructureService, '_notifyChangeListeners').and.callThrough();

    PageStructureService._updateComponent(component, updatedMarkup);
    expect(PageStructureService._notifyChangeListeners).toHaveBeenCalled();
  });

  it('retrieves a container by overlay element', () => {
    registerVBoxContainer();
    const container = PageStructureService.getContainers()[0];
    const overlayElement = { };

    expect(PageStructureService.getContainerByOverlayElement({ })).toBeUndefined();
    container.setOverlayElement(overlayElement);

    expect(PageStructureService.getContainerByOverlayElement({ })).toBeUndefined();
    expect(PageStructureService.getContainerByOverlayElement(overlayElement)).toBeUndefined();
  });

  it('re-renders a NoMarkup container', () => {
    registerNoMarkupContainer();
    PageStructureService.attachEmbeddedLinks();

    const container = PageStructureService.getContainers()[0];
    container.getEndComment().after('<p>Trailing element, to be removed</p>'); // insert trailing dom element
    expect(container.getEndComment().next().length).toBe(1);
    const updatedMarkup = `
      ${containerComment('NoMarkup container', 'HST.nomarkup', 'container-nomarkup')}
        ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
        ${endComment('aaaa')}
      ${endComment('container-nomarkup')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    PageStructureService.renderContainer(container);
    $rootScope.$digest();

    const newContainer = PageStructureService.getContainers()[0];
    expect(newContainer).not.toBe(container);
    expect(newContainer.getEndComment().next().length).toBe(0);
  });

  it('re-renders a NoMarkup container without any text nodes after the end comment', () => {
    registerNoMarkupContainerWithoutTextNodesAfterEndComment();

    const container = PageStructureService.getContainers()[0];
    const updatedMarkup = `
      ${containerComment('Empty NoMarkup container', 'HST.NoMarkup', 'no-markup-no-text-nodes-after-end-comment')}
      ${endComment('no-markup-no-text-nodes-after-end-comment')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    PageStructureService.renderContainer(container);
    $rootScope.$digest();

    const newContainer = PageStructureService.getContainers()[0];
    expect(newContainer).not.toBe(container);
    expect(newContainer.isEmpty()).toBe(true);
  });

  it('re-renders a container with an edit menu link', (done) => {
    // set up page structure with component and edit menu link in it
    registerVBoxContainer();
    registerVBoxComponent('componentA');
    registerEmbeddedLink('#edit-menu-in-component-a');
    registerEmbeddedLink('#edit-menu-in-page');
    registerEmbeddedLink('#manage-content-in-page');
    registerEmbeddedLink('#manage-content-in-container-vbox');

    PageStructureService.attachEmbeddedLinks();

    const container = PageStructureService.getContainers()[0];
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
          ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
        <p id="new-manage-content-in-container-vbox">
          ${manageContentLinkComment('new-manage-content-in-container-vbox')}
        </p>
      </div>
      ${endComment('container-vbox')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    PageStructureService.renderContainer(container).then((newContainer) => {
      expect(PageStructureService.getContainers().length).toBe(1);
      expect(PageStructureService.getContainers()[0]).toBe(newContainer);

      // edit menu link in component A is no longer there
      const embeddedLinks = PageStructureService.getEmbeddedLinks();
      expect(embeddedLinks.length).toBe(3);
      expect(embeddedLinks[0].getUuid()).toBe('menu-in-page');
      expect(embeddedLinks[1].getDocumentTemplateQuery()).toBe('new-test-document');
      expect(embeddedLinks[2].getDocumentTemplateQuery()).toBe('new-manage-content-in-container-vbox');
      expect(embeddedLinks[2].getEnclosingElement()).toBe(newContainer);
      done();
    });
    $rootScope.$digest();
  });

  it('known that a re-rendered container contains new head contributions', (done) => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    const container = PageStructureService.getContainers()[0];
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
          ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
      </div>
      ${endComment('container-vbox')}
      ${unprocessedHeadContributionsComment('<script>window.newScript=true</script>')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    PageStructureService.renderContainer(container).then((newContainer) => {
      expect(PageStructureService.containsNewHeadContributions(newContainer)).toBe(true);
      done();
    });
    $rootScope.$digest();
  });

  it('notifies change listeners when updating a container', (done) => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    const container = PageStructureService.getContainers()[0];
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
        ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
      </div>
      ${endComment('container-vbox')}
    `;

    spyOn(PageStructureService, '_notifyChangeListeners').and.callThrough();
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));

    PageStructureService.renderContainer(container).then(() => {
      expect(PageStructureService._notifyChangeListeners).toHaveBeenCalled();
      done();
    });

    $rootScope.$digest();
  });

  it('knows that a re-rendered container does not contain new head contributions', (done) => {
    registerVBoxContainer();
    registerVBoxComponent('componentA');

    const container = PageStructureService.getContainers()[0];
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
          ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
      </div>
      ${endComment('container-vbox')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    PageStructureService.renderContainer(container).then((newContainer) => {
      expect(PageStructureService.containsNewHeadContributions(newContainer)).toBe(false);
      done();
    });
    $rootScope.$digest();
  });

  it('known that a re-rendered container does not contain new head contributions if they have already been rendered by the page', (done) => { // eslint-disable-line max-len
    registerVBoxContainer();
    registerVBoxComponent('componentA');
    registerHeadContributions('#processed-head-contributions');

    const container = PageStructureService.getContainers()[0];
    const updatedMarkup = `
      ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
      <div id="container-vbox">
        <div id="componentA">
          ${itemComment('component A', 'aaaa')}
          <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        </div>
      </div>
      ${endComment('container-vbox')}
      ${unprocessedHeadContributionsComment('<script>window.processed = true</script>')}
    `;
    spyOn(MarkupService, 'fetchContainerMarkup').and.returnValue($q.when(updatedMarkup));
    PageStructureService.renderContainer(container).then((newContainer) => {
      expect(PageStructureService.containsNewHeadContributions(newContainer)).toBe(false);
      done();
    });
    $rootScope.$digest();
  });

  function expectUpdateHstContainer(id, container) {
    expect(HstService.updateHstContainer).toHaveBeenCalledWith(id, container.getHstRepresentation());
  }

  describe('move component', () => {
    function componentIds(container) {
      return container.getComponents().map(component => component.getId());
    }

    it('can move the first component to the second position in the container', () => {
      registerVBoxContainer();
      registerVBoxComponent('componentA');
      registerVBoxComponent('componentB');

      const container = PageStructureService.getContainers()[0];
      const componentA = container.getComponents()[0];

      spyOn(HstService, 'updateHstContainer');
      expect(componentIds(container)).toEqual(['aaaa', 'bbbb']);

      PageStructureService.moveComponent(componentA, container, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container);
      expect(componentIds(container)).toEqual(['bbbb', 'aaaa']);
      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('can move the second component to the first position in the container', () => {
      registerVBoxContainer();
      registerVBoxComponent('componentA');
      registerVBoxComponent('componentB');

      const container = PageStructureService.getContainers()[0];
      const componentA = container.getComponents()[0];
      const componentB = container.getComponents()[1];

      spyOn(HstService, 'updateHstContainer');
      expect(componentIds(container)).toEqual(['aaaa', 'bbbb']);

      PageStructureService.moveComponent(componentB, container, componentA);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container);
      expect(componentIds(container)).toEqual(['bbbb', 'aaaa']);
      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('can move a component to another container', () => {
      registerVBoxContainer();
      registerVBoxComponent('componentA');
      registerVBoxComponent('componentB');
      registerEmptyVBoxContainer();

      const container1 = PageStructureService.getContainers()[0];
      const component = container1.getComponents()[0];
      const container2 = PageStructureService.getContainers()[1];

      spyOn(HstService, 'updateHstContainer');
      expect(componentIds(container1)).toEqual(['aaaa', 'bbbb']);
      expect(componentIds(container2)).toEqual([]);

      PageStructureService.moveComponent(component, container2, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container1);
      expectUpdateHstContainer('container-vbox-empty', container2);
      expect(componentIds(container1)).toEqual(['bbbb']);
      expect(componentIds(container2)).toEqual(['aaaa']);
      expect(ChannelService.recordOwnChange).toHaveBeenCalled();
    });

    it('shows an error when a component is moved within a container that just got locked by another user', () => {
      registerVBoxContainer();
      registerVBoxComponent('componentA');
      registerVBoxComponent('componentB');

      const container = PageStructureService.getContainers()[0];
      const component = container.getComponents()[0];

      spyOn(HstService, 'updateHstContainer').and.returnValue($q.reject());
      spyOn(FeedbackService, 'showError');

      PageStructureService.moveComponent(component, container, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container);
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
        component: 'component A',
      });
      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });

    it('shows an error when a component is moved out of a container that just got locked by another user', () => {
      registerVBoxContainer();
      registerVBoxComponent('componentA');
      registerEmptyVBoxContainer();

      const container1 = PageStructureService.getContainers()[0];
      const component = container1.getComponents()[0];
      const container2 = PageStructureService.getContainers()[1];

      spyOn(HstService, 'updateHstContainer').and.returnValues($q.reject(), $q.resolve());
      spyOn(FeedbackService, 'showError');

      PageStructureService.moveComponent(component, container2, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container1);
      expectUpdateHstContainer('container-vbox-empty', container2);
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
        component: 'component A',
      });
      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });

    it('shows an error when a component is moved into a container that just got locked by another user', () => {
      registerVBoxContainer();
      registerVBoxComponent('componentA');
      registerEmptyVBoxContainer();

      const container1 = PageStructureService.getContainers()[0];
      const component = container1.getComponents()[0];
      const container2 = PageStructureService.getContainers()[1];

      spyOn(HstService, 'updateHstContainer').and.returnValues($q.resolve(), $q.reject());
      spyOn(FeedbackService, 'showError');

      PageStructureService.moveComponent(component, container2, undefined);
      $rootScope.$digest();

      expectUpdateHstContainer('container-vbox', container1);
      expectUpdateHstContainer('container-vbox-empty', container2);
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_MOVE_COMPONENT_FAILED', {
        component: 'component A',
      });
      expect(ChannelService.recordOwnChange).not.toHaveBeenCalled();
    });
  });

  describe('getContainerById', () => {
    let mockContainer;
    beforeEach(() => {
      mockContainer = { getId() { return 123; } };
      PageStructureService.containers = [mockContainer, { getId() { return 456; } }];
    });

    it('returns a known container', () => {
      expect(PageStructureService.getContainerById(123)).toBe(mockContainer);
    });

    it('returns undefined when getting an unknown container', () => {
      expect(PageStructureService.getContainerById(789)).toBeUndefined();
    });
  });
});
