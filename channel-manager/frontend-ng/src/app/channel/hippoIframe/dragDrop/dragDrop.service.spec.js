/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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
  let $q;
  let ChannelService;
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
      _ChannelService_,
      _ConfigService_,
      _DomService_,
      _DragDropService_,
      _PageStructureService_,
    ) => {
      $q = _$q_;
      ChannelService = _ChannelService_;
      ConfigService = _ConfigService_;
      DomService = _DomService_;
      DragDropService = _DragDropService_;
      PageStructureService = _PageStructureService_;
    });

    spyOn(ChannelService, 'recordOwnChange');
    jasmine.getFixtures().load('channel/hippoIframe/dragDrop/dragDrop.service.fixture.html');

    iframe = $('#testIframe');
    canvas = $('#testCanvas');
    mockCommentData = {};
  });

  function createContainer(number, xtype = 'HST.NoMarkup') {
    const iframeContainerComment = iframe.contents().find(`#iframeContainerComment${number}`)[0];
    const commentData = Object.assign(
      {
        uuid: `container${number}`,
        'HST-Type': 'CONTAINER_COMPONENT',
        'HST-XType': xtype,
      },
      mockCommentData[`container${number}`],
    );
    PageStructureService.registerParsedElement(iframeContainerComment, commentData);
    const containers = PageStructureService.getContainers();
    return containers[containers.length - 1];
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
    PageStructureService.registerParsedElement(iframeComponentComment, commentData);
    return PageStructureService.getComponentById(`component${number}`);
  }

  function loadIframeFixture(callback) {
    DragDropService.init(iframe, canvas);

    iframe.one('load', () => {
      const iframeWindow = iframe[0].contentWindow;

      container1 = createContainer(1);
      component1 = createComponent(1, container1);

      container2 = createContainer(2);

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
    const eventHandlers = $._data(jqueryElement[0], 'events');
    return eventHandlers && eventHandlers[event] ? eventHandlers[event].length : 0;
  }

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

  it('emits event "component-click" when a component receives a mouseup event from the left mouse button', (done) => {
    loadIframeFixture(() => {
      spyOn(DragDropService.emitter, 'emit').and.callThrough();

      const mockedMouseDownEvent = {
        clientX: 100,
        clientY: 200,
      };

      const componentElement1 = component1.getBoxElement();

      DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

      componentElement1.on('mouseup', () => {
        expect(DragDropService.emitter.emit).toHaveBeenCalledWith('component-click', component1);
        done();
      });

      const mouseUp = $.Event('mouseup');
      mouseUp.which = 1; // left mouse button, see https://api.jquery.com/event.which/
      componentElement1.trigger(mouseUp);
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

  describe('onClick', () => {
    describe('callback handler', () => {
      let clickHandler;

      beforeEach(() => {
        clickHandler = jasmine.createSpy('onClickHandler');
      });

      it('registers a callback', () => {
        DragDropService.onClick(clickHandler);

        loadIframeFixture(() => {
          DragDropService._onComponentClick(component1);
          expect(clickHandler).toHaveBeenCalled();
        });
      });

      it('returns an unbind function to clear a registered callback', () => {
        const unbind = DragDropService.onClick(clickHandler);

        loadIframeFixture(() => {
          unbind();
          DragDropService._onComponentClick(component1);
          expect(clickHandler).not.toHaveBeenCalled();
        });
      });
    });
  });

  describe('onDrop', () => {
    describe('callback handler', () => {
      let dropHandler;

      beforeEach(() => {
        dropHandler = jasmine.createSpy('onDropHandler');
      });

      it('registers a callback', (done) => {
        DragDropService.onDrop(dropHandler);

        loadIframeFixture(() => {
          DragDropService._onDrop(component1.getBoxElement(), container2.getBoxElement(), container1.getBoxElement())
            .then(() => {
              expect(dropHandler).toHaveBeenCalled();
              done();
            });
        });
      });

      it('returns an unbind function to clear a registered callback', (done) => {
        const unbind = DragDropService.onDrop(dropHandler);
        loadIframeFixture(() => {
          unbind();
          DragDropService._onDrop(component1.getBoxElement(), container2.getBoxElement(), container1.getBoxElement())
            .then(() => {
              expect(dropHandler).not.toHaveBeenCalled();
              done();
            });
        });
      });
    });

    describe('"component-drop" event', () => {
      let emit;
      beforeEach(() => {
        emit = spyOn(DragDropService.emitter, 'emit');
      });

      it('emits event "component-drop" when a component is dropped', (done) => {
        emit.and.returnValue($q.resolve());

        loadIframeFixture(() => {
          DragDropService._onDrop(component1.getBoxElement(), container2.getBoxElement(), container1.getBoxElement())
            .then(() => {
              expect(emit).toHaveBeenCalledWith('component-drop', [component1, container2, undefined]);
              done();
            });
        });
      });

      it('sets dropping state to true while emitting "component-drop"', () => {
        emit.and.returnValue($q(angular.noop));

        loadIframeFixture(() => {
          expect(DragDropService.dropping).toBe(false);
          DragDropService._onDrop(component1.getBoxElement(), container2.getBoxElement(), container1.getBoxElement());
          expect(DragDropService.dropping).toBe(true);
        });
      });

      it('sets dropping state back to false when "component-drop" emit resolves', (done) => {
        emit.and.returnValue($q.resolve());

        loadIframeFixture(() => {
          DragDropService._onDrop(component1.getBoxElement(), container2.getBoxElement(), container1.getBoxElement())
            .then(() => {
              expect(DragDropService.dropping).toBe(false);
              done();
            });
        });
      });

      it('sets dropping state back to false when "component-drop" emit rejects', (done) => {
        emit.and.returnValue($q.reject());

        loadIframeFixture(() => {
          DragDropService._onDrop(component1.getBoxElement(), container2.getBoxElement(), container1.getBoxElement())
            .catch(() => {
              expect(DragDropService.dropping).toBe(false);
              done();
            });
        });
      });
    });
  });

  it('replaces a container', (done) => {
    loadIframeFixture(() => {
      const reRenderedContainer1 = createContainer(3);

      DragDropService.replaceContainer(container1, reRenderedContainer1);

      // wait until the current $digest is done before expecting results
      setTimeout(() => {
        expect(DragDropService.drake.containers.map(angular.element)).toEqual([
          reRenderedContainer1.getBoxElement(),
          container2.getBoxElement(),
        ]);
        done();
      }, 0);
    });
  });

  it('ignores the replacement of an unknown container', (done) => {
    loadIframeFixture(() => {
      const unknownContainer = createContainer(3);

      DragDropService.replaceContainer(unknownContainer, container1);

      // wait until the current $digest is done before expecting results
      setTimeout(() => {
        expect(DragDropService.drake.containers.map(angular.element)).toEqual([
          container1.getBoxElement(),
          container2.getBoxElement(),
        ]);
        done();
      }, 0);
    });
  });

  it('ignores the replacement of a non-existing container', (done) => {
    loadIframeFixture(() => {
      DragDropService.replaceContainer(container1, null);

      // wait until the current $digest is done before expecting results
      setTimeout(() => {
        expect(DragDropService.drake.containers.map(angular.element)).toEqual([
          container1.getBoxElement(),
          container2.getBoxElement(),
        ]);
        done();
      }, 0);
    });
  });

  it('updates the drag direction of a container', (done) => {
    loadIframeFixture(() => {
      DragDropService._updateDragDirection(container1.getBoxElement()[0]);
      expect(DragDropService.dragulaOptions.direction).toEqual('vertical');

      const spanContainer = createContainer(5, 'HST.Span');
      DragDropService._updateDragDirection(spanContainer.getBoxElement()[0]);
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
