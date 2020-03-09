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
  let $injector;
  let $q;
  let $rootScope;
  let angularElement;
  let ConfigService;
  let DomService;
  let DragDropService;
  let PageStructureService;
  let iframe;
  let canvas;

  let mockCommentData;
  let container1;
  let component1;
  let container2;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.hippoIframe');

    inject((
      _$q_,
      _$rootScope_,
      _ConfigService_,
      _DomService_,
      _DragDropService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      ConfigService = _ConfigService_;
      DomService = _DomService_;
      DragDropService = _DragDropService_;
    });

    const fake = angular.element('<div>');

    angular.bootstrap(fake, ['hippo-cm-iframe']);
    window.$Promise = $q;

    $injector = fake.injector();
    PageStructureService = fake.injector().get('PageStructureService');
    PageStructureService.$rootScope = $rootScope;
    angularElement = angular.element;
    spyOn(angular, 'element').and.callThrough();

    jasmine.getFixtures().load('channel/hippoIframe/dragDrop/dragDrop.service.fixture.html');

    iframe = $('#testIframe');
    canvas = $('#testCanvas');
    mockCommentData = {};
  });

  function createContainer(number, xtype = 'HST.NoMarkup') {
    const iframeStartContainerComment = iframe.contents().find(`#iframeStartContainerComment${number}`);
    const iframeEndContainerComment = iframe.contents().find(`#iframeEndContainerComment${number}`);
    const startCommentData = Object.assign(
      {
        uuid: `container${number}`,
        'HST-Type': 'CONTAINER_COMPONENT',
        'HST-XType': xtype,
      },
      mockCommentData[`container${number}`],
    );
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
    const iframeComponentComment = iframe.contents().find(`#iframeComponentComment${number}`)[0];
    const commentData = Object.assign(
      {
        uuid: `component${number}`,
        'HST-Type': 'CONTAINER_ITEM_COMPONENT',
        'HST-Label': `Component ${number}`,
      },
      mockCommentData[`component${number}`],
    );
    // TODO: temporary workaround
    iframeComponentComment.replaceWith($(`<!-- ${JSON.stringify(commentData)} -->`)[0]);
  }

  function loadIframeFixture(callback) {
    DragDropService.init(iframe, canvas);

    iframe.one('load', () => {
      const iframeWindow = iframe[0].contentWindow;

      iframeWindow.angular = angular;
      PageStructureService.$document = angular.element(iframeWindow.document);

      angular.element.and.callFake((selector, ...rest) => {
        const result = angularElement(selector, ...rest);

        if (selector === iframeWindow.document) {
          result.injector = () => $injector;
        }

        return result;
      });
      $rootScope.$emit('hippo-iframe:load');

      createContainer(1);
      createComponent(1, container1);
      createContainer(2);

      PageStructureService.parseElements();

      container1 = PageStructureService.getPage().getContainerById('container1');
      container2 = PageStructureService.getPage().getContainerById('container2');
      component1 = PageStructureService.getPage().getComponentById('component1');

      DragDropService.enable().then(() => {
        try {
          callback(iframeWindow);
        } catch (e) {
          // Karma silently swallows stack traces for synchronous tests, so log them in an explicit fail
          fail(e);
        }
      });
    });

    iframe.attr('src', `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/dragDrop/dragDrop.service.iframe.fixture.html`); // eslint-disable-line max-len
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
      loadIframeFixture(() => {
        expect(DragDropService.isEnabled()).toBe(true);
        done();
      });
    });
  });

  it('is not dragging initially', () => {
    expect(DragDropService.isDragging()).toBeFalsy();
  });

  it('injects dragula.js into the iframe', (done) => {
    loadIframeFixture((iframeWindow) => {
      expect(typeof iframeWindow.dragula).toEqual('function');
      expect(DragDropService.drake).toBeDefined();
      expect(DragDropService.isDragging()).toBeFalsy();
      done();
    });
  });

  it('destroys dragula on iframe unload', (done) => {
    loadIframeFixture((iframeWindow) => {
      expect(DragDropService.drake).not.toBeNull();
      $(iframeWindow).trigger('unload');
      expect(DragDropService.drake).toBeNull();
      expect(DragDropService.dragulaOptions).toBeNull();
      expect(DragDropService.isDragging()).toBeFalsy();
      done();
    });
  });

  it('reloads the dragula containers on a page change', (done) => {
    loadIframeFixture(() => {
      const drakeContainers = DragDropService.drake.containers;
      $rootScope.$emit('iframe:page:change');
      expect(DragDropService.drake.containers).not.toBe(drakeContainers);
      done();
    });
  });

  describe('startDragOrClick', () => {
    it('forwards a shifted mouse event to the iframe when it starts dragging in an iframe', (done) => {
      loadIframeFixture(() => {
        const mockedMouseDownEvent = {
          clientX: 100,
          clientY: 200,
        };
        const iframeComponentElement1 = component1.getBoxElement()[0];

        iframe.offset({
          left: 10,
          top: 20,
        });
        spyOn(iframeComponentElement1, 'dispatchEvent');

        DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

        expect(DragDropService.isDraggingOrClicking()).toBeTruthy();
        expect(DragDropService.isDragging()).toBeFalsy();

        const dispatchedEvent = iframeComponentElement1.dispatchEvent.calls.argsFor(0)[0];
        expect(dispatchedEvent.type).toEqual('mousedown');
        expect(dispatchedEvent.bubbles).toEqual(true);
        expect(dispatchedEvent.clientX).toEqual(90);
        expect(dispatchedEvent.clientY).toEqual(180);

        done();
      });
    });

    it('stops dragging or clicking when disabled', (done) => {
      loadIframeFixture(() => {
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
      loadIframeFixture(() => {
        spyOn($rootScope, '$emit');

        const mockedMouseDownEvent = {
          clientX: 100,
          clientY: 200,
        };

        const componentElement1 = component1.getBoxElement();

        DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

        const mouseUp = angular.element.Event('mouseup');
        mouseUp.which = 1; // left mouse button, see https://api.jquery.com/event.which/
        componentElement1.trigger(mouseUp);

        expect($rootScope.$emit).toHaveBeenCalledWith('component:click', component1);

        done();
      });
    });

    it('cancels the click simulation for showing a component\'s properties when the mouse cursor leaves a disabled component', (done) => { // eslint-disable-line max-len
      mockCommentData.container1 = {
        'HST-LockedBy': 'anotherUser',
      };
      loadIframeFixture(() => {
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
    loadIframeFixture(() => {
      expect(angular.element(DragDropService.drake.containers)).toEqual(container2.getBoxElement());
      done();
    });
  });

  it('checks internally whether a container is disabled', (done) => {
    loadIframeFixture(() => {
      expect(DragDropService._isContainerEnabled(container1.getBoxElement())).toBeTruthy();

      spyOn(container1, 'isDisabled').and.returnValue(true);
      expect(DragDropService._isContainerEnabled(container1.getBoxElement())).toBeFalsy();

      expect(DragDropService._isContainerEnabled(iframe)).toBeFalsy();

      done();
    });
  });

  it('updates the drag direction of a container', (done) => {
    mockCommentData.container2 = {
      'HST-XType': 'HST.Span',
    };

    loadIframeFixture(() => {
      DragDropService._updateDragDirection(container1.getBoxElement()[0]);
      expect(DragDropService.dragulaOptions.direction).toEqual('vertical');

      DragDropService._updateDragDirection(container2.getBoxElement()[0]);
      expect(DragDropService.dragulaOptions.direction).toEqual('horizontal');

      done();
    });
  });

  describe('DragulaJS injection', () => {
    let mockIframe = {}; // "require" and "requirejs" functions are not defined

    it('injects DragulaJS when RequireJS is not available using DomService', (done) => {
      loadIframeFixture(() => {
        spyOn(DomService, 'getAppRootUrl').and.returnValue('http://localhost:8080/cms/');
        ConfigService.antiCache = '123';
        const DragulaJSPath = `${DomService.getAppRootUrl()}scripts/dragula.min.js?antiCache=${ConfigService.antiCache}`; // eslint-disable-line max-len

        spyOn(DomService, 'addScript').and.returnValue($q.resolve());

        DragDropService._injectDragula(mockIframe);
        expect(DomService.addScript).toHaveBeenCalledWith(mockIframe, DragulaJSPath);

        done();
      });
    });

    it('injects DragulaJS using DomService when a require function exists that is not RequireJs', (done) => {
      loadIframeFixture(() => {
        mockIframe = {
          require: () => fail(),
        };

        spyOn(DomService, 'getAppRootUrl').and.returnValue('http://localhost:8080/cms/');
        ConfigService.antiCache = '123';
        const DragulaJSPath = `${DomService.getAppRootUrl()}scripts/dragula.min.js?antiCache=${ConfigService.antiCache}`; // eslint-disable-line max-len

        spyOn(DomService, 'addScript').and.returnValue($q.resolve());

        DragDropService._injectDragula(mockIframe);
        expect(DomService.addScript).toHaveBeenCalledWith(mockIframe, DragulaJSPath);

        done();
      });
    });

    it('injects DragulaJS when RequireJS is available', (done) => {
      loadIframeFixture(() => {
        const requireFn = (modules, callback) => { callback('dragulaLoaded'); };

        mockIframe = {
          require: requireFn,
          requirejs: requireFn,
        };

        spyOn(DomService, 'getAppRootUrl').and.returnValue('http://localhost:8080/cms/');
        ConfigService.antiCache = '123';

        spyOn(DomService, 'addScript');

        DragDropService._injectDragula(mockIframe);
        expect(DomService.addScript).not.toHaveBeenCalled();

        expect(mockIframe.dragula).toBe('dragulaLoaded');
        done();
      });
    });
  });
});
