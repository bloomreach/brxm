/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('PageStructureService', () => {
  'use strict';

  let PageStructureService;
  let PageMetaDataService;
  let ChannelService;
  let HstService;
  let RenderingService;
  let $document;
  let $q;
  let $log;
  let $window;
  let $rootScope;

  beforeEach(() => {
    module('hippo-cm.channel.page');

    inject((_$q_, _$rootScope_, _$log_, _$document_, _$window_, _PageStructureService_, _PageMetaDataService_,
            _ChannelService_, _HstService_, _RenderingService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $log = _$log_;
      $document = _$document_;
      $window = _$window_;
      PageStructureService = _PageStructureService_;
      PageMetaDataService = _PageMetaDataService_;
      ChannelService = _ChannelService_;
      HstService = _HstService_;
      RenderingService = _RenderingService_;
    });
  });

  beforeEach(() => {
    jasmine.getFixtures().load('channel/page/pageStructure.service.fixture.html');
  });

  it('has no containers initially', () => {
    expect(PageStructureService.containers).toEqual([]);
  });

  it('rejects components if there is no container yet', () => {
    spyOn($log, 'warn');
    PageStructureService.registerParsedElement(undefined, {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
    });

    expect(PageStructureService.containers).toEqual([]);
    expect($log.warn).toHaveBeenCalled();
  });

  const jsonify = (element) => JSON.parse(element.data);

  const childComment = (element) => {
    var children = element.childNodes;
    for (let i = 0; i < children.length; i++) {
      const child = children[i];
      if (child.nodeType === 8) {
        return child;
      }
    }
    console.log('No child comment found!');
    return null;
  };

  const previousComment = (element) => {
    while (element.previousSibling) {
      element = element.previousSibling;
      if (element.nodeType === 8) {
        return element;
      }
    }
    console.log('no previous comment found!');
    return null;
  };

  const nextComment = (element) => {
    while (element.nextSibling) {
      element = element.nextSibling;
      if (element.nodeType === 8) {
        return element;
      }
    }
    console.log('no next comment found!');
    return null;
  };

  it('registers containers in the correct order', () => {
    const container1 = $j('#container11', $document)[0];
    const container2 = $j('#container22', $document)[0];
    const comment1 = previousComment(container1);
    const comment2 = childComment(container2);

    PageStructureService.registerParsedElement(comment1, jsonify(comment1));
    PageStructureService.registerParsedElement(comment2, jsonify(comment2));

    expect(PageStructureService.containers.length).toEqual(2);

    expect(PageStructureService.containers[0].type).toEqual('container');
    expect(PageStructureService.containers[0].isEmpty()).toEqual(true);
    expect(PageStructureService.containers[0].getComponents()).toEqual([]);
    expect(PageStructureService.containers[0].getJQueryElement('iframeBoxElement')[0]).toEqual(container1);
    expect(PageStructureService.containers[0].getLabel()).toEqual('container 1');

    expect(PageStructureService.containers[1].type).toEqual('container');
    expect(PageStructureService.containers[1].isEmpty()).toEqual(true);
    expect(PageStructureService.containers[1].getComponents()).toEqual([]);
    expect(PageStructureService.containers[1].getJQueryElement('iframeBoxElement')[0]).toEqual(container2);
    expect(PageStructureService.containers[1].getLabel()).toEqual('container 2');
  });

  it('adds components to the most recently registered container', () => {
    const container1 = $j('#container11', $document)[0];
    const componentA = $j('#componentAA', $document)[0];
    const componentB = $j('#componentBB', $document)[0];
    const comment1 = previousComment(container1);
    const commentA = childComment(componentA);
    const commentB = childComment(componentB);

    PageStructureService.registerParsedElement(comment1, jsonify(comment1));
    PageStructureService.registerParsedElement(comment1, jsonify(comment1));
    PageStructureService.registerParsedElement(commentA, jsonify(commentA));
    PageStructureService.registerParsedElement(commentB, jsonify(commentB));

    expect(PageStructureService.containers.length).toEqual(2);
    expect(PageStructureService.containers[0].isEmpty()).toEqual(true);
    expect(PageStructureService.containers[1].isEmpty()).toEqual(false);
    expect(PageStructureService.containers[1].getComponents().length).toEqual(2);

    expect(PageStructureService.containers[1].getComponents()[0].type).toEqual('component');
    expect(PageStructureService.containers[1].getComponents()[0].getJQueryElement('iframeBoxElement')[0]).toBe(componentA);
    expect(PageStructureService.containers[1].getComponents()[0].getLabel()).toEqual('component A');
    expect(PageStructureService.containers[1].getComponents()[0].container).toEqual(PageStructureService.containers[1]);

    expect(PageStructureService.containers[1].getComponents()[1].type).toEqual('component');
    expect(PageStructureService.containers[1].getComponents()[1].getJQueryElement('iframeBoxElement')[0]).toBe(componentB);
    expect(PageStructureService.containers[1].getComponents()[1].getLabel()).toEqual('component B');
    expect(PageStructureService.containers[1].getComponents()[1].container).toEqual(PageStructureService.containers[1]);
  });

  it('clears the page structure', () => {
    const container = $j('#container11', $document)[0];
    const comment = previousComment(container);

    PageStructureService.registerParsedElement(comment, jsonify(comment));

    expect(PageStructureService.containers.length).toEqual(1);

    PageStructureService.clearParsedElements();

    expect(PageStructureService.containers.length).toEqual(0);
  });

  it('registers additional elements', () => {
    const container1 = $j('#container11', $document)[0];
    const container2 = $j('#container22', $document);
    const comment1 = previousComment(container1);

    PageStructureService.registerParsedElement(comment1, jsonify(comment1));
    PageStructureService.containers[0].setJQueryElement('test', container2);

    expect(PageStructureService.containers[0].getJQueryElement('test')).toEqual(container2);
  });

  it('finds the DOM element of a transparent container as parent of the comment', () => {
    const container = $j('#container22', $document)[0];
    const comment = childComment(container);

    PageStructureService.registerParsedElement(comment, jsonify(comment));

    expect(PageStructureService.containers.length).toEqual(1);
    expect(PageStructureService.containers[0].getJQueryElement('iframeBoxElement')[0]).toBe(container);
  });

  it('finds the DOM element of a component of a transparent container as next sibling of the comment', () => {
    const container2 = $j('#container22', $document)[0];
    const componentD = $j('#componentDD', $document)[0];
    const comment2 = childComment(container2);
    const commentD = previousComment(componentD);

    PageStructureService.registerParsedElement(comment2, jsonify(comment2));
    PageStructureService.registerParsedElement(commentD, jsonify(commentD));

    expect(PageStructureService.containers.length).toEqual(1);
    expect(PageStructureService.containers[0].isEmpty()).toEqual(false);
    expect(PageStructureService.containers[0].getComponents().length).toEqual(1);
    expect(PageStructureService.containers[0].getComponents()[0].getJQueryElement('iframeBoxElement')[0]).toBe(componentD);
    expect(PageStructureService.containers[0].getComponents()[0].hasNoIFrameDomElement()).not.toEqual(true);
  });

  it('registers no iframe box element in case of a transparent, empty component', () => {
    const container2 = $j('#container22', $document)[0];
    const comment2 = childComment(container2);
    const commentC = nextComment(comment2);

    PageStructureService.registerParsedElement(comment2, jsonify(comment2));
    PageStructureService.registerParsedElement(commentC, jsonify(commentC));

    expect(PageStructureService.containers.length).toEqual(1);
    expect(PageStructureService.containers[0].isEmpty()).toEqual(false);
    expect(PageStructureService.containers[0].getComponents().length).toEqual(1);
    expect(PageStructureService.containers[0].getComponents()[0].getJQueryElement('iframeBoxElement').length).toEqual(0);
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

  it('switches channels when the channel id in the page meta-data differs from the current channel id', () => {
    spyOn(ChannelService, 'getId').and.returnValue('testChannelId');
    spyOn(ChannelService, 'switchToChannel');

    PageStructureService.registerParsedElement(null, {
      'HST-Type': 'PAGE-META-DATA',
      'HST-Channel-Id': 'anotherChannelId',
    });

    expect(ChannelService.switchToChannel).toHaveBeenCalledWith('anotherChannelId');
  });

  it('switches channels when the channel id in the page meta-data is the same as the current channel id', () => {
    spyOn(ChannelService, 'getId').and.returnValue('testChannelId');
    spyOn(ChannelService, 'switchToChannel');

    PageStructureService.registerParsedElement(null, {
      'HST-Type': 'PAGE-META-DATA',
      'HST-Channel-Id': 'testChannelId',
    });

    expect(ChannelService.switchToChannel).not.toHaveBeenCalled();
  });

  it('ignores unknown HST types', () => {
    PageStructureService.registerParsedElement(null, {
      'HST-Type': 'unknown type',
    });
  });

  it('returns a known component', () => {
    const container1 = $j('#container11', $document)[0];
    const componentA = $j('#componentAA', $document)[0];
    const comment1 = previousComment(container1);
    const commentA = childComment(componentA);

    PageStructureService.registerParsedElement(comment1, jsonify(comment1));
    PageStructureService.registerParsedElement(comment1, jsonify(comment1));
    PageStructureService.registerParsedElement(commentA, jsonify(commentA));

    const pageComponent = PageStructureService.getComponentById('aaaa');
    expect(pageComponent).not.toBeNull();
    expect(pageComponent.getId()).toEqual('aaaa');
    expect(pageComponent.getLabel()).toEqual('component A');
  });

  it('returns null when getting an unknown component', () => {
    expect(PageStructureService.getComponentById('no-such-component')).toBeNull();
  });

  it('removes a valid component and calls HST successfully', () => {
    const container1 = $j('#container11', $document)[0];
    const componentA = $j('#componentAA', $document)[0];
    const comment1 = previousComment(container1);
    const commentA = childComment(componentA);

    PageStructureService.registerParsedElement(comment1, jsonify(comment1));
    PageStructureService.registerParsedElement(commentA, jsonify(commentA));

    spyOn(HstService, 'removeHstComponent').and.returnValue($q.when([]));
    spyOn(RenderingService, 'fetchContainerMarkup').and.returnValue($q.when(''));

    PageStructureService.removeComponentById('aaaa');

    $rootScope.$digest();

    expect(HstService.removeHstComponent).toHaveBeenCalledWith('1111', 'aaaa');
  });

  it('removes a valid component but fails to call HST', () => {
    const container = $j('#container1', $document);
    const component = $j('#componentA', $document);

    PageStructureService.registerParsedElement(container[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
      uuid: 'container-123',
    });
    PageStructureService.registerParsedElement(component[0].childNodes[0], {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
      'HST-Label': 'Test Component',
      uuid: 'component-1234',
    });

    // mock the call to HST to be failed
    spyOn(HstService, 'doGet').and.returnValue($q.reject());

    PageStructureService.removeComponentById('component-1234');
    $rootScope.$digest();

    expect(HstService.doGet).toHaveBeenCalledWith('container-123', 'delete', 'component-1234');
  });

  it('removes an invalid component', () => {
    const container = $j('#container1', $document);
    const component = $j('#componentA', $document);

    PageStructureService.registerParsedElement(container[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
      uuid: 'container-123',
    });
    PageStructureService.registerParsedElement(component[0].childNodes[0], {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
      'HST-Label': 'Test Component',
      uuid: 'component-1234',
    });
    spyOn(HstService, 'doGet').and.returnValue($q.when([]));

    PageStructureService.removeComponentById('component-123');
    $rootScope.$digest();

    expect(HstService.doGet).not.toHaveBeenCalled();
  });

  it('returns a container by iframe element', () => {
    const container1 = $j('#container1', $document);
    const container2 = $j('#container2', $document);

    PageStructureService.registerParsedElement(container1[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
      uuid: 'container-1',
    });
    PageStructureService.registerParsedElement(container2[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
      uuid: 'container-2',
    });

    const container = PageStructureService.getContainerByIframeElement(container2[0]);
    expect(container).not.toBeNull();
    expect(container.getId()).toEqual('container-2');
  });

  it('triggers an event to show the component properties dialog', () => {
    const componentElement = jasmine.createSpyObj(['getId', 'getLabel', 'getLastModified']);
    componentElement.getId.and.returnValue('testId');
    componentElement.getLabel.and.returnValue('testLabel');
    componentElement.getLastModified.and.returnValue(12345);
    componentElement.container = jasmine.createSpyObj(['isDisabled', 'isInherited']);
    componentElement.container.isDisabled.and.returnValue(true);
    componentElement.container.isInherited.and.returnValue(false);

    spyOn(PageMetaDataService, 'get').and.returnValue({
      testMetaData: 'foo',
    });

    spyOn(window.APP_TO_CMS, 'publish');

    PageStructureService.showComponentProperties(componentElement);

    expect($window.APP_TO_CMS.publish).toHaveBeenCalledWith('show-component-properties', {
      component: {
        id: 'testId',
        label: 'testLabel',
        lastModified: 12345,
      },
      container: {
        isDisabled: true,
        isInherited: false,
      },
      page: {
        testMetaData: 'foo',
      },
    });
  });

  it('prints parsed elements', () => {
    const container = $j('#container1', $document);
    const componentA = $j('#componentA', $document);
    const componentB = $j('#componentB', $document);

    PageStructureService.registerParsedElement(container[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
    });
    PageStructureService.registerParsedElement(componentA[0].childNodes[0], {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
      'HST-Label': 'component A',
    });
    PageStructureService.registerParsedElement(container[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
    });
    PageStructureService.registerParsedElement(componentB[0].childNodes[0], {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
      'HST-Label': 'component B',
    });

    spyOn($log, 'debug');

    PageStructureService.printParsedElements();

    expect($log.debug.calls.count()).toEqual(4);
  });
});
