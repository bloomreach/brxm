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

describe('PageStructureService', function () {
  'use strict';

  var PageStructureService;
  var $document;
  var $log;

  beforeEach(function () {
    module('hippo-cm.channel.page');

    inject(function (_$log_, _$document_, _PageStructureService_) {
      $log = _$log_;
      $document = _$document_;
      PageStructureService = _PageStructureService_;
    });
  });

  beforeEach(function () {
    jasmine.getFixtures().load('channel/page/pageStructure.service.fixture.html');
  });

  it('should be initialized to have no containers', function () {
    expect(PageStructureService.containers).toEqual([]);
  });

  it('should reject components if there is no container yet', function () {
    spyOn($log, 'warn');
    PageStructureService.registerParsedElement(undefined, {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
    });

    expect(PageStructureService.containers).toEqual([]);
    expect($log.warn).toHaveBeenCalled();
  });

  it('should register containers in the correct order', function () {
    var container1 = $j('#container1', $document);
    var container2 = $j('#container2', $document);

    PageStructureService.registerParsedElement(container1[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
      'HST-Label': 'container 1',
    });
    PageStructureService.registerParsedElement(container2[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
      'HST-Label': 'container 2',
    });

    expect(PageStructureService.containers.length).toEqual(2);

    expect(PageStructureService.containers[0].type).toEqual('container');
    expect(PageStructureService.containers[0].isEmpty()).toEqual(true);
    expect(PageStructureService.containers[0].getComponents()).toEqual([]);
    expect(PageStructureService.containers[0].getJQueryElement('iframe')).toEqual(container1);
    expect(PageStructureService.containers[0].getLabel()).toEqual('container 1');

    expect(PageStructureService.containers[1].type).toEqual('container');
    expect(PageStructureService.containers[1].isEmpty()).toEqual(true);
    expect(PageStructureService.containers[1].getComponents()).toEqual([]);
    expect(PageStructureService.containers[1].getJQueryElement('iframe')).toEqual(container2);
    expect(PageStructureService.containers[1].getLabel()).toEqual('container 2');
  });

  it('should add components to the most recently registered container', function () {
    var container = $j('#container1', $document);
    var componentA = $j('#componentA', $document);
    var componentB = $j('#componentB', $document);

    PageStructureService.registerParsedElement(container[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
    });
    PageStructureService.registerParsedElement(container[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
    });
    PageStructureService.registerParsedElement(componentA[0].childNodes[0], {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
      'HST-Label': 'component A',
    });
    PageStructureService.registerParsedElement(componentB[0].childNodes[0], {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
      'HST-Label': 'component B',
    });

    expect(PageStructureService.containers.length).toEqual(2);
    expect(PageStructureService.containers[0].isEmpty()).toEqual(true);
    expect(PageStructureService.containers[1].isEmpty()).toEqual(false);
    expect(PageStructureService.containers[1].getComponents().length).toEqual(2);

    expect(PageStructureService.containers[1].getComponents()[0].type).toEqual('component');
    expect(PageStructureService.containers[1].getComponents()[0].getJQueryElement('iframe')).toEqual(componentA);
    expect(PageStructureService.containers[1].getComponents()[0].getLabel()).toEqual('component A');
    expect(PageStructureService.containers[1].getComponents()[0].container).toEqual(PageStructureService.containers[1]);

    expect(PageStructureService.containers[1].getComponents()[1].type).toEqual('component');
    expect(PageStructureService.containers[1].getComponents()[1].getJQueryElement('iframe')).toEqual(componentB);
    expect(PageStructureService.containers[1].getComponents()[1].getLabel()).toEqual('component B');
    expect(PageStructureService.containers[1].getComponents()[1].container).toEqual(PageStructureService.containers[1]);
  });

  it('should clear the page structure', function () {
    var container = $j('#container1', $document);

    PageStructureService.registerParsedElement(container[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
    });

    expect(PageStructureService.containers.length).toEqual(1);

    PageStructureService.clearParsedElements();

    expect(PageStructureService.containers.length).toEqual(0);
  });

  it('should register additional elements', function () {
    var container1 = $j('#container1', $document);
    var container2 = $j('#container2', $document);

    PageStructureService.registerParsedElement(container1[0].previousSibling, {
      'HST-Type': 'CONTAINER_COMPONENT',
    });
    PageStructureService.containers[0].setJQueryElement('test', container2);

    expect(PageStructureService.containers[0].getJQueryElement('test')).toEqual(container2);
  });

  it('should find the DOM element of a transparent container as parent of the comment', function () {
    var containerT = $j('#container-transparent', $document);

    PageStructureService.registerParsedElement(containerT[0].childNodes[0], {
      'HST-Type': 'CONTAINER_COMPONENT',
      'HST-XType': 'hst.transparent',
    });

    expect(PageStructureService.containers.length).toEqual(1);
    expect(PageStructureService.containers[0].getJQueryElement('iframe')).toEqual(containerT);
  });

  it('should find the DOM element of a component of a transparent container as next sibling of the comment', function () {
    var containerT = $j('#container-transparent', $document);
    var componentT = $j('#component-transparent', $document);

    PageStructureService.registerParsedElement(containerT[0].childNodes[0], {
      'HST-Type': 'CONTAINER_COMPONENT',
      'HST-XType': 'hst.transparent',
    });
    PageStructureService.registerParsedElement(componentT[0].previousSibling, {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
    });

    expect(PageStructureService.containers.length).toEqual(1);
    expect(PageStructureService.containers[0].isEmpty()).toEqual(false);
    expect(PageStructureService.containers[0].getComponents().length).toEqual(1);
    expect(PageStructureService.containers[0].getComponents()[0].getJQueryElement('iframe')).toEqual(componentT);
  });

  it('should ignore components of a transparent container which have no root DOM element', function () {
    var containerT = $j('#container-transparent', $document);
    var componentT = $j('#component-transparent', $document);
    spyOn($log, 'debug');

    PageStructureService.registerParsedElement(containerT[0].childNodes[0], {
      'HST-Type': 'CONTAINER_COMPONENT',
      'HST-XType': 'hst.transparent',
    });
    PageStructureService.registerParsedElement(componentT[0].nextSibling, {
      'HST-Type': 'CONTAINER_ITEM_COMPONENT',
    });

    expect(PageStructureService.containers[0].isEmpty()).toEqual(true);
    expect($log.debug).toHaveBeenCalled();
  });
});
