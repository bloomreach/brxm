/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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
  let $document;
  let $rootScope;

  let CommunicationService;
  let HstCommentsProcessorService;
  let PageStructureService;

  let registered;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

    CommunicationService = jasmine.createSpyObj('CommunicationService', ['emit']);

    angular.mock.module(($provide) => {
      $provide.value('CommunicationService', CommunicationService);
    });

    inject((
      _$document_,
      _$rootScope_,
      _HstCommentsProcessorService_,
      _PageStructureService_,
    ) => {
      $document = _$document_;
      $rootScope = _$rootScope_;
      HstCommentsProcessorService = _HstCommentsProcessorService_;
      PageStructureService = _PageStructureService_;
    });

    registered = [];

    spyOn(HstCommentsProcessorService, 'run').and.returnValue(registered);

    jasmine.getFixtures().load('iframe/page/page-structure.service.fixture.html');
  });

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

  function endComment(uuid) {
    return `<!-- {
      "HST-End": "true",
      "uuid": "${uuid}"
    } -->`;
  }

  const childComment = element => [...element.childNodes]
    .filter(child => child.nodeType === Node.COMMENT_NODE)
    .shift();

  const lastComment = element => [...element.childNodes]
    .filter(child => child.nodeType === Node.COMMENT_NODE)
    .pop();

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

  const registerParsedElement = element => registered.push({ element, json: JSON.parse(element.data) });

  const registerPage = () => {
    registerParsedElement(childComment($j('#page-meta', $document)[0]));
  };

  const registerEmptyVBoxContainer = () => {
    const container = $j('#container-vbox-empty', $document)[0];

    registerParsedElement(previousComment(container));
    registerParsedElement(nextComment(container));

    return container;
  };

  const registerVBoxContainer = (callback) => {
    const container = $j('#container-vbox', $document)[0];

    registerParsedElement(previousComment(container));
    if (callback) {
      callback();
    }
    registerParsedElement(nextComment(container));

    return container;
  };

  const registerVBoxComponent = (id, callback) => {
    const component = $j(`#${id}`, $document)[0];

    registerParsedElement(childComment(component));
    if (callback) {
      callback();
    }
    registerParsedElement(lastComment(component));

    return component;
  };

  const registerNoMarkupContainer = (callback, id = '#container-no-markup') => {
    const container = $j(id, $document)[0];

    registerParsedElement(childComment(container));
    if (callback) {
      callback();
    }
    registerParsedElement(lastComment(container));

    return container;
  };

  const registerEmptyNoMarkupContainer = () => registerNoMarkupContainer(undefined, '#container-no-markup-empty');

  const registerLowercaseNoMarkupContainer = callback => registerNoMarkupContainer(
    callback,
    '#container-no-markup-lowercase',
  );

  const registerEmptyLowercaseNoMarkupContainer = () => {
    registerNoMarkupContainer(undefined, '#container-no-markup-lowercase-empty');
  };

  const registerNoMarkupContainerWithoutTextNodesAfterEndComment = () => {
    registerNoMarkupContainer(undefined, '#container-no-markup-without-text-nodes-after-end-comment');
  };

  const registerNoMarkupComponent = (callback, id = '#component-no-markup') => {
    const component = $j(id, $document)[0];
    registerParsedElement(previousComment(component));
    if (callback) {
      callback();
    }
    registerParsedElement(nextComment(component));

    return component;
  };

  const registerEmptyNoMarkupComponent = () => {
    registerParsedElement(nextComment(childComment($j('#container-no-markup', $document)[0])));
  };

  const registerLowercaseNoMarkupComponent = callback => registerNoMarkupComponent(
    callback,
    '#component-no-markup-lowercase',
  );

  const registerEmbeddedLink = (selector) => {
    registerParsedElement(childComment($j(selector, $document)[0]));
  };

  describe('initially', () => {
    it('should have no page', () => {
      expect(PageStructureService.getPage()).not.toBeDefined();
    });

    it('should have no embedded links', () => {
      expect(PageStructureService.getEmbeddedLinks()).toEqual([]);
    });
  });

  describe('parseElements', () => {
    it('creates a new page from the HST comments found in the DOM', () => {
      let component1Element;
      let component2Element;
      let component3Element;
      const container1Element = registerVBoxContainer(() => {
        component1Element = registerVBoxComponent('componentA');
        component2Element = registerVBoxComponent('componentB');
      });
      const container2Element = registerNoMarkupContainer(() => {
        component3Element = registerNoMarkupComponent();
      });
      PageStructureService.parseElements();

      const page = PageStructureService.getPage();
      expect(page.getContainers()).toHaveLength(2);

      const [container1, container2] = page.getContainers();
      expect(container1.getLabel()).toEqual('vBox container');
      expect(container1.getBoxElement()[0]).toEqual(container1Element);
      expect(container1.getComponents()).toHaveLength(2);

      expect(container2.getLabel()).toEqual('NoMarkup container');
      expect(container2.getBoxElement()[0]).toEqual(container2Element);
      expect(container2.getComponents()).toHaveLength(1);

      const [component1] = container1.getComponents();
      expect(component1.getLabel()).toEqual('component A');
      expect(component1.getBoxElement()[0]).toEqual(component1Element);

      const [, component2] = container1.getComponents();
      expect(component2.getLabel()).toEqual('component B');
      expect(component2.getBoxElement()[0]).toEqual(component2Element);

      const [component3] = container2.getComponents();
      expect(component3.getLabel()).toEqual('Component in NoMarkup container');
      expect(component3.getBoxElement()[0]).toEqual(component3Element);
    });

    it('registers edit menu links', () => {
      registerEmbeddedLink('#edit-menu-in-page');
      PageStructureService.parseElements();

      const editMenuLinks = PageStructureService.getEmbeddedLinks();
      expect(editMenuLinks).toHaveLength(1);
      expect(editMenuLinks[0].getId()).toBe('menu-in-page');
    });

    it('registers manage content links', () => {
      registerEmbeddedLink('#manage-content-in-page');
      PageStructureService.parseElements();

      expect(PageStructureService.getEmbeddedLinks()).toHaveLength(1);
      const [manageContentLink] = PageStructureService.getEmbeddedLinks();
      expect(manageContentLink.getDocumentTemplateQuery()).toBe('new-test-document');
    });

    it('attaches the embedded link to the enclosing component', () => {
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA', () => {
          registerEmbeddedLink('#edit-menu-in-component-a');
        });
        registerVBoxComponent('componentB');
        registerEmbeddedLink('#manage-content-in-container-vbox');
      });
      registerEmbeddedLink('#edit-menu-in-page');
      registerEmbeddedLink('#manage-content-in-page');
      PageStructureService.parseElements();

      const page = PageStructureService.getPage();
      const [containerVBox] = page.getContainers();
      const [componentA] = containerVBox.getComponents();
      const attachedEmbeddedLinks = PageStructureService.getEmbeddedLinks();

      expect(attachedEmbeddedLinks).toHaveLength(4);
      expect(attachedEmbeddedLinks[0].getComponent()).toBe(componentA);
      expect(attachedEmbeddedLinks[1].getComponent()).toBe(containerVBox);
      expect(attachedEmbeddedLinks[2].getComponent()).toBeUndefined();
      expect(attachedEmbeddedLinks[3].getComponent()).toBeUndefined();
    });

    it('emits event "page:change" after page elements have been parsed', () => {
      const onChange = jasmine.createSpy('on-change');
      const offChange = $rootScope.$on('page:change', onChange);
      PageStructureService.parseElements();

      expect(onChange).toHaveBeenCalled();

      offChange();
    });

    describe('no-markup containers', () => {
      it('finds the DOM element of a no-markup container as parent of the comment', () => {
        const containerElement = registerNoMarkupContainer();
        PageStructureService.parseElements();

        const [container] = PageStructureService.getPage().getContainers();
        expect(container.getBoxElement()[0]).toBe(containerElement);
      });

      it('finds the DOM element of a component of a no-markup container as next sibling of the comment', () => {
        let componentElement;

        registerNoMarkupContainer(() => {
          componentElement = registerNoMarkupComponent();
        });
        PageStructureService.parseElements();

        const [container] = PageStructureService.getPage().getContainers();
        const [component] = container.getComponents();
        expect(component.getBoxElement()[0]).toBe(componentElement);
        expect(component.hasNoIFrameDomElement()).not.toEqual(true);
      });

      it('registers no iframe box element in case of a no-markup, empty component', () => {
        registerNoMarkupContainer(() => registerEmptyNoMarkupComponent());
        PageStructureService.parseElements();

        const [container] = PageStructureService.getPage().getContainers();
        const [component] = container.getComponents();
        expect(component.getBoxElement()[0]).toBeUndefined();
      });
    });

    it('should clear the page structure', () => {
      registerVBoxContainer();
      registerEmbeddedLink('#manage-content-in-page');
      registerEmbeddedLink('#edit-menu-in-page');
      PageStructureService.parseElements();

      expect(PageStructureService.getPage()).toBeDefined();
      expect(PageStructureService.getEmbeddedLinks()).toHaveLength(2);

      registered.splice(0);
      PageStructureService.parseElements();

      expect(PageStructureService.getEmbeddedLinks()).toHaveLength(0);
    });
  });

  describe('getContainerByIframeElement', () => {
    it('returns a container by iframe element', () => {
      const containerElement = registerNoMarkupContainer();
      PageStructureService.parseElements();

      const container = PageStructureService.getContainerByIframeElement(containerElement);

      expect(container).toBeDefined();
      expect(container.getId()).toEqual('container-no-markup');
    });

    it('returns undefined if page is not parsed yet', () => {
      const container = PageStructureService.getContainerByIframeElement();
      expect(container).toBeUndefined();
    });
  });

  describe('updateComponent', () => {
    it('does no throw an error if component is unknown', (done) => {
      PageStructureService.parseElements();
      expect(PageStructureService.getPage()).toBeUndefined();

      PageStructureService.updateComponent('unknown').then((newComponent) => {
        expect(newComponent).toEqual([]);
        done();
      });
      $rootScope.$digest();
    });

    it('updates a component with an edit menu link', () => {
      // set up page structure with component and edit menu link in it
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA', () => {
          registerEmbeddedLink('#edit-menu-in-component-a');
        });
      });
      registerEmbeddedLink('#edit-menu-in-page');
      PageStructureService.parseElements();

      PageStructureService.updateComponent('aaaa', `
        ${itemComment('component A', 'aaaa')}
          <p id="updated-edit-menu-in-component-a">
            ${editMenuLinkComment('updated-menu-in-component-a')}
          </p>
        ${endComment('aaaa')}
      `);
      $rootScope.$digest();

      const page = PageStructureService.getPage();
      const updatedComponentA = page.getComponentById('aaaa');
      const editMenuLinks = PageStructureService.getEmbeddedLinks();

      expect(editMenuLinks).toHaveLength(2);
      expect(editMenuLinks[0].getComponent()).toBeUndefined();
      expect(editMenuLinks[1].getComponent()).toBe(updatedComponentA);
    });

    it('updates a component with no more content link', () => {
      // set up page structure with component and content link in it
      registerNoMarkupContainer(() => {
        registerNoMarkupComponent(() => {
          registerEmbeddedLink('#manage-content-in-component-no-markup');
        });
      });
      PageStructureService.parseElements();

      PageStructureService.updateComponent('component-no-markup', `
        ${itemComment('Component in NoMarkup container', 'component-no-markup')}
          <div id="component-no-markup">
            <p>Some markup in component D</p>
          </div>
        ${endComment('component-no-markup')}
      `);
      $rootScope.$digest();

      expect(PageStructureService.getEmbeddedLinks()).toHaveLength(0);
    });

    it('updates a component, adding an edit menu link', () => {
      // set up page structure with component and edit menu link in it
      registerVBoxContainer(() => registerVBoxComponent('componentA'));
      PageStructureService.parseElements();

      expect(PageStructureService.getEmbeddedLinks()).toHaveLength(0);

      PageStructureService.updateComponent('aaaa', `
        ${itemComment('component A', 'aaaa')}
          <p id="updated-edit-menu-in-component-a">
            ${editMenuLinkComment('updated-menu-in-component-a')}
          </p>
        ${endComment('aaaa')}
      `);
      $rootScope.$digest();

      const page = PageStructureService.getPage();
      const updatedComponentA = page.getComponentById('aaaa');
      const embeddedLinks = PageStructureService.getEmbeddedLinks();
      expect(embeddedLinks).toHaveLength(1);
      expect(embeddedLinks[0].getComponent()).toBe(updatedComponentA);
    });

    it('gracefully updates a component twice quickly after each other', () => {
      // set up page structure with component
      registerVBoxContainer(() => registerVBoxComponent('componentB'));
      PageStructureService.parseElements();

      const updatedMarkup = `
          ${itemComment('component B', 'bbbb')}
            <p>Re-rendered component B</p>
          ${endComment('bbbb')}
        `;
      PageStructureService.updateComponent('bbbb', updatedMarkup);
      PageStructureService.updateComponent('bbbb', updatedMarkup);

      expect(() => {
        $rootScope.$digest();
      }).not.toThrow();
    });

    it('does not add an updated and incorrectly commented component to the page structure', () => {
      registerVBoxContainer(() => registerVBoxComponent('componentA'));
      PageStructureService.parseElements();

      PageStructureService.updateComponent('aaaa', `
        ${itemComment('component A', 'aaaa')}
          <p id="updated-edit-menu-in-component-a">
            ${editMenuLinkComment('updated-menu-in-component-a')}
          </p>
        `);
      $rootScope.$digest();

      const page = PageStructureService.getPage();
      expect(page.getContainers()).toHaveLength(1);
      expect(page.getContainers()[0].getComponents()).toHaveLength(0);
    });

    it('notifies change listeners when updating a component', () => {
      const onPageChange = jasmine.createSpy('on-page-change');
      const offPageChange = $rootScope.$on('page:change', onPageChange);

      registerVBoxContainer(() => registerVBoxComponent('componentA'));
      PageStructureService.parseElements();

      PageStructureService.updateComponent('aaaa', `
        ${itemComment('component A', 'aaaa')}
          <p id="updated-component-with-new-head-contribution">
          </p>
        ${endComment('aaaa')}
      `);
      $rootScope.$digest();
      expect(onPageChange).toHaveBeenCalled();

      offPageChange();
    });
  });

  describe('updateContainer', () => {
    it('does no throw an error if container is unknown', (done) => {
      PageStructureService.parseElements();
      expect(PageStructureService.getPage()).toBeUndefined();

      PageStructureService.updateContainer('unknown').then((newContainer) => {
        expect(newContainer).toEqual([]);
        done();
      });
      $rootScope.$digest();
    });

    it('updates a NoMarkup container', () => {
      registerNoMarkupContainer();
      PageStructureService.parseElements();

      const page = PageStructureService.getPage();
      const container = page.getContainerById('container-no-markup');
      container.getEndComment().after('<p>Trailing element, to be removed</p>'); // insert trailing dom element
      expect(container.getEndComment().next()).toHaveLength(1);

      PageStructureService.updateContainer('container-no-markup', `
        ${containerComment('NoMarkup container', 'HST.nomarkup', 'container-no-markup')}
          ${itemComment('component A', 'aaaa')}
            <p id="test">Some markup in component A</p>
          ${endComment('aaaa')}
        ${endComment('container-no-markup')}
      `);
      $rootScope.$digest();

      const newContainer = page.getContainerById('container-no-markup');
      expect(newContainer).not.toBe(container);
      expect(newContainer.getEndComment().next()).toHaveLength(0);
    });

    it('updates a NoMarkup container without any text nodes after the end comment', () => {
      registerNoMarkupContainerWithoutTextNodesAfterEndComment();
      PageStructureService.parseElements();

      const page = PageStructureService.getPage();
      const [container] = page.getContainers();
      const containerId = container.getId();

      PageStructureService.updateContainer(containerId, `
        ${containerComment('Empty NoMarkup container', 'HST.NoMarkup', containerId)}
        ${endComment(containerId)}
      `);
      $rootScope.$digest();

      const newContainer = page.getContainerById(containerId);
      expect(newContainer).not.toBe(container);
      expect(newContainer.isEmpty()).toBe(true);
    });

    it('updates a container with an edit menu link', () => {
      // set up page structure with component and edit menu link in it
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA', () => registerEmbeddedLink('#edit-menu-in-component-a'));
        registerEmbeddedLink('#manage-content-in-container-vbox');
      });
      registerEmbeddedLink('#edit-menu-in-page');
      registerEmbeddedLink('#manage-content-in-page');
      PageStructureService.parseElements();

      PageStructureService.updateContainer('container-vbox', `
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
      `);
      $rootScope.$digest();

      const page = PageStructureService.getPage();
      expect(page.getContainers()).toHaveLength(1);
      const newContainer = page.getContainerById('container-vbox');
      expect(newContainer).toBeDefined();

      // edit menu link in component A is no longer there
      const embeddedLinks = PageStructureService.getEmbeddedLinks();
      expect(embeddedLinks).toHaveLength(3);
      expect(embeddedLinks[0].getId()).toBe('menu-in-page');
      expect(embeddedLinks[1].getDocumentTemplateQuery()).toBe('new-test-document');
      expect(embeddedLinks[2].getDocumentTemplateQuery()).toBe('new-manage-content-in-container-vbox');
      expect(embeddedLinks[2].getComponent()).toBe(newContainer);
    });

    it('notifies change listeners when updating a container', () => {
      const onPageChange = jasmine.createSpy('on-page-change');
      const offPageChange = $rootScope.$on('page:change', onPageChange);

      registerVBoxContainer(() => registerVBoxComponent('componentA'));
      PageStructureService.parseElements();

      PageStructureService.updateContainer('container-vbox', `
        ${containerComment('vBox container', 'HST.vBox', 'container-vbox')}
        <div id="container-vbox">
          <div id="componentA">
          ${itemComment('component A', 'aaaa')}
            <p id="test">Some markup in component A</p>
            ${endComment('aaaa')}
          </div>
        </div>
        ${endComment('container-vbox')}
      `);
      $rootScope.$digest();

      expect(onPageChange).toHaveBeenCalled();

      offPageChange();
    });
  });

  describe('move component', () => {
    function componentIds(container) {
      return container.getComponents().map(component => component.getId());
    }

    it('can move components in a container', () => {
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA');
        registerVBoxComponent('componentB');
      });
      PageStructureService.parseElements();

      const page = PageStructureService.getPage();
      const [container] = page.getContainers();
      const [componentA, componentB] = container.getComponents();

      expect(componentIds(container)).toEqual(['aaaa', 'bbbb']);

      // move componentA to second position
      PageStructureService.moveComponent(componentA, container, undefined);
      $rootScope.$digest();

      expect(componentIds(container)).toEqual(['bbbb', 'aaaa']);
      expect(CommunicationService.emit).toHaveBeenCalledWith('component:move', {
        componentId: 'aaaa',
        containerId: 'container-vbox',
        nextComponentId: undefined,
      });

      PageStructureService.moveComponent(componentA, container, componentB);
      $rootScope.$digest();

      // move componentA to first position
      expect(componentIds(container)).toEqual(['aaaa', 'bbbb']);
      expect(CommunicationService.emit).toHaveBeenCalledWith('component:move', {
        componentId: 'aaaa',
        containerId: 'container-vbox',
        nextComponentId: 'bbbb',
      });
    });

    it('can move a component to another container', () => {
      registerVBoxContainer(() => {
        registerVBoxComponent('componentA');
        registerVBoxComponent('componentB');
      });
      registerEmptyVBoxContainer();
      PageStructureService.parseElements();

      const page = PageStructureService.getPage();
      const [container1, container2] = page.getContainers();
      const [component1] = container1.getComponents();

      expect(componentIds(container1)).toEqual(['aaaa', 'bbbb']);
      expect(componentIds(container2)).toEqual([]);

      PageStructureService.moveComponent(component1, container2, undefined);
      $rootScope.$digest();

      expect(component1.getContainer()).toEqual(container2);
      expect(componentIds(container1)).toEqual(['bbbb']);
      expect(componentIds(container2)).toEqual(['aaaa']);
      expect(CommunicationService.emit).toHaveBeenCalledWith('component:move', {
        componentId: 'aaaa',
        containerId: 'container-vbox-empty',
        nextComponentId: undefined,
      });
    });
  });

  describe('can move to entities specs', () => {
    it('detects if a container contains DOM elements that represent a container-item', () => {
      registerVBoxContainer();
      registerNoMarkupContainer(() => registerNoMarkupComponent());
      registerLowercaseNoMarkupContainer(() => registerLowercaseNoMarkupComponent());
      PageStructureService.parseElements();

      const containers = PageStructureService.getPage().getContainers();
      expect(containers[0].isEmptyInDom()).toEqual(false);
      expect(containers[1].isEmptyInDom()).toEqual(false);
      expect(containers[2].isEmptyInDom()).toEqual(false);
    });

    it('detects if a container does not contain DOM elements that represent a container-item', () => {
      registerEmptyVBoxContainer();
      registerEmptyNoMarkupContainer();
      registerEmptyLowercaseNoMarkupContainer();
      PageStructureService.parseElements();

      const containers = PageStructureService.getPage().getContainers();
      expect(containers[0].isEmptyInDom()).toEqual(true);
      expect(containers[1].isEmptyInDom()).toEqual(true);
      expect(containers[2].isEmptyInDom()).toEqual(true);
    });

    it('returns a known component', () => {
      registerVBoxContainer();
      registerVBoxContainer(() => registerVBoxComponent('componentA'));
      PageStructureService.parseElements();

      const pageComponent = PageStructureService.getPage().getComponentById('aaaa');

      expect(pageComponent).not.toBeNull();
      expect(pageComponent.getId()).toEqual('aaaa');
      expect(pageComponent.getLabel()).toEqual('component A');
    });

    it('returns null when getting an unknown component', () => {
      registerPage();
      PageStructureService.parseElements();

      expect(PageStructureService.getPage().getComponentById('no-such-component')).toBeNull();
    });
  });
});
