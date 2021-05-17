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

describe('DragDropService', () => {
  let $document;
  let $q;
  let $rootScope;
  let CommunicationService;
  let DomService;
  let DragDropService;
  let PageStructureService;

  let mockCommentData;
  let container1;
  let component1;
  let container2;

  beforeEach(() => {
    angular.mock.module('hippo-cm-iframe');

    CommunicationService = jasmine.createSpyObj('CommunicationService', ['emit', 'getAssetUrl']);

    angular.mock.module(($provide) => {
      $provide.value('CommunicationService', CommunicationService);
    });

    inject((
      _$document_,
      _$q_,
      _$rootScope_,
      _DomService_,
      _DragDropService_,
      _PageStructureService_,
    ) => {
      $document = _$document_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      DomService = _DomService_;
      DragDropService = _DragDropService_;
      PageStructureService = _PageStructureService_;
    });

    jasmine.getFixtures().load('iframe/overlay/drag-drop.service.fixture.html');

    CommunicationService.getAssetUrl.and.callFake(href => href);
    DragDropService.$window = window;

    mockCommentData = {};
  });

  function createContainer(number, xtype = 'HST.NoMarkup') {
    const iframeStartContainerComment = $document.find(`#iframeStartContainerComment${number}`);
    const iframeEndContainerComment = $document.contents().find(`#iframeEndContainerComment${number}`);
    const startCommentData = {
      uuid: `container${number}`,
        'HST-Type': 'CONTAINER_COMPONENT',
        'HST-XType': xtype,
      ...mockCommentData[`container${number}`],
    };
    const endCommentData = {
      uuid: `container${number}`,
      'HST-End': 'true',
    };
    // TODO: temporary workaround
    const startCommentEl = $(`<!-- ${JSON.stringify(startCommentData)} -->`)[0];
    const endCommentEl = $(`<!-- ${JSON.stringify(endCommentData)} -->`)[0];
    iframeStartContainerComment.replaceWith(startCommentEl);
    iframeEndContainerComment.replaceWith(endCommentEl);

    return [
      { element: startCommentEl, json: startCommentData },
      { element: endCommentEl, json: endCommentData },
    ];
  }

  function createComponent(number) {
    const iframeComponentComment = $document.find(`#iframeComponentComment${number}`)[0];
    const commentData = {
      uuid: `component${number}`,
        'HST-Type': 'CONTAINER_ITEM_COMPONENT',
        'HST-Label': `Component ${number}`,
      ...mockCommentData[`component${number}`],
    };
    // TODO: temporary workaround
    iframeComponentComment.replaceWith($(`<!-- ${JSON.stringify(commentData)} -->`)[0]);
  }

  function enableDragDrop(callback) {
    DragDropService.initialize();

    createContainer(1);
    createComponent(1, container1);
    createContainer(2);

    PageStructureService.parseElements();

    container1 = PageStructureService.getPage().getContainerById('container1');
    container2 = PageStructureService.getPage().getContainerById('container2');
    component1 = PageStructureService.getPage().getComponentById('component1');

    DragDropService.enable().then(() => {
      try {
        callback();
      } catch (e) {
        // Karma silently swallows stack traces for synchronous tests, so log them in an explicit fail
        fail(e);
      }
    });

    $rootScope.$digest();
  }

  function eventHandlerCount(jqueryElement, event) {
    const eventHandlers = angular.element._data(jqueryElement[0], 'events');

    return eventHandlers && eventHandlers.hasOwnProperty(event) ? eventHandlers[event].length : 0;
  }

  describe('isEnabled', () => {
    it('should return false when the drag and drop service is not enabled', () => {
      expect(DragDropService.isEnabled()).toBe(false);
    });

    it('should return true when the drag and drop service is enabled', (done) => {
      enableDragDrop(() => {
        expect(DragDropService.isEnabled()).toBe(true);
        done();
      });
    });
  });

  it('is not dragging initially', () => {
    expect(DragDropService.isDragging()).toBeFalsy();
  });

  it('injects dragula.js into the iframe', (done) => {
    enableDragDrop(() => {
      expect(DragDropService.drake).toBeDefined();
      expect(DragDropService.isDragging()).toBeFalsy();
      done();
    });
  });

  it('reloads the dragula containers on a page change', (done) => {
    enableDragDrop(() => {
      const drakeContainers = DragDropService.drake.containers;
      $rootScope.$emit('page:change');
      expect(DragDropService.drake.containers).not.toBe(drakeContainers);
      done();
    });
  });

  describe('startDragOrClick', () => {
    it('forwards a shifted mouse event to the iframe when it starts dragging in an iframe', (done) => {
      enableDragDrop(() => {
        const mockedMouseDownEvent = {
          clientX: 100,
          clientY: 200,
        };
        const iframeComponentElement1 = component1.getBoxElement()[0];

        spyOn(iframeComponentElement1, 'dispatchEvent');

        DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

        expect(DragDropService.isDraggingOrClicking()).toBeTruthy();
        expect(DragDropService.isDragging()).toBeFalsy();

        const dispatchedEvent = iframeComponentElement1.dispatchEvent.calls.argsFor(0)[0];
        expect(dispatchedEvent.type).toEqual('mousedown');
        expect(dispatchedEvent.bubbles).toEqual(true);
        expect(dispatchedEvent.clientX).toEqual(100);
        expect(dispatchedEvent.clientY).toEqual(200);

        done();
      });
    });

    it('stops dragging or clicking when disabled', (done) => {
      enableDragDrop(() => {
        const mockedMouseDownEvent = {
          clientX: 100,
          clientY: 200,
        };

        DragDropService.startDragOrClick(mockedMouseDownEvent, component1);
        expect(DragDropService.isDraggingOrClicking()).toBeTruthy();
        expect(DragDropService.isDragging()).toBeFalsy();

        DragDropService.disable();
        expect(DragDropService.isDraggingOrClicking()).toBeFalsy();
        expect(DragDropService.isDragging()).toBeFalsy();

        done();
      });
    });

    it('should emit component:click event on a left button mouseup event', (done) => {
      enableDragDrop(() => {
        const mockedMouseDownEvent = {
          clientX: 100,
          clientY: 200,
        };

        const componentElement1 = component1.getBoxElement();

        DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

        const mouseUp = angular.element.Event('mouseup');
        mouseUp.which = 1; // left mouse button, see https://api.jquery.com/event.which/
        componentElement1.trigger(mouseUp);

        expect(CommunicationService.emit).toHaveBeenCalledWith('component:click', component1.getId());

        done();
      });
    });

    it('cancels the click simulation for showing a component\'s properties when the mouse cursor leaves a disabled component', (done) => { // eslint-disable-line max-len
      mockCommentData.container1 = {
        'HST-LockedBy': 'anotherUser',
      };
      enableDragDrop(() => {
        const mockedMouseDownEvent = {
          clientX: 100,
          clientY: 200,
        };
        const componentElement1 = component1.getBoxElement();
        DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

        expect(DragDropService.isDraggingOrClicking()).toEqual(true);
        expect(eventHandlerCount(componentElement1, 'mouseup')).toEqual(1);
        expect(eventHandlerCount(componentElement1, 'mouseout')).toEqual(1);

        componentElement1.one('mouseout.test', () => {
          expect(DragDropService.isDraggingOrClicking()).toEqual(false);
          expect(eventHandlerCount(componentElement1, 'mouseup')).toEqual(0);
          expect(eventHandlerCount(componentElement1, 'mouseout')).toEqual(0);
          done();
        });

        componentElement1.trigger('mouseout');
      });
    });
  });

  it('does not register a disabled container', (done) => {
    mockCommentData.container1 = {
      'HST-LockedBy': 'anotherUser',
    };
    mockCommentData.container2 = {
      'HST-Inherited': 'true',
    };
    enableDragDrop(() => {
      expect(angular.element(DragDropService.drake.containers)).toHaveLength(0);
      done();
    });
  });

  it('checks internally whether a container is disabled', (done) => {
    enableDragDrop(() => {
      expect(DragDropService._isContainerEnabled(container1.getBoxElement())).toBeTruthy();

      spyOn(container1, 'isDisabled').and.returnValue(true);
      expect(DragDropService._isContainerEnabled(container1.getBoxElement())).toBeFalsy();

      expect(DragDropService._isContainerEnabled($document)).toBeFalsy();

      done();
    });
  });

  it('updates the drag direction of a container', (done) => {
    mockCommentData.container2 = {
      'HST-XType': 'HST.Span',
    };

    enableDragDrop(() => {
      DragDropService._updateDragDirection(container1.getBoxElement()[0]);
      expect(DragDropService.dragulaOptions.direction).toEqual('vertical');

      DragDropService._updateDragDirection(container2.getBoxElement()[0]);
      expect(DragDropService.dragulaOptions.direction).toEqual('horizontal');

      done();
    });
  });

  describe('DragulaJS injection', () => {
    afterEach(() => {
      delete window.require;
      delete window.requirejs;
    });

    it('injects DragulaJS when RequireJS is not available using DomService', () => {
      spyOn(DomService, 'addScript').and.returnValue($q.resolve());
      CommunicationService.getAssetUrl.and.returnValue($q.resolve('url'));

      DragDropService._injectDragula();
      $rootScope.$digest();

      expect(DomService.addScript).toHaveBeenCalledWith(window, 'url');
    });

    it('injects DragulaJS using DomService when a require function exists that is not RequireJs', () => {
      window.require = () => fail();

      spyOn(DomService, 'addScript').and.returnValue($q.resolve());
      CommunicationService.getAssetUrl.and.returnValue($q.resolve('url'));

      DragDropService._injectDragula();
      $rootScope.$digest();

      expect(DomService.addScript).toHaveBeenCalledWith(window, 'url');
    });

    it('injects DragulaJS when RequireJS is available', () => {
      const requireFn = (modules, callback) => { callback('dragulaLoaded'); };

      window.require = requireFn;
      window.requirejs = requireFn;

      spyOn(DomService, 'addScript');
      CommunicationService.getAssetUrl.and.returnValue($q.resolve('url'));

      DragDropService._injectDragula();
      $rootScope.$digest();

      expect(DomService.addScript).not.toHaveBeenCalled();
      expect(window.dragula).toBe('dragulaLoaded');
    });
  });
});
