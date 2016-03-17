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

import { ContainerElement } from '../page/element/containerElement';
import { ComponentElement } from '../page/element/componentElement';

describe('DragDropService', () => {
  'use strict';

  let DragDropService;
  let hstCommentsProcessor;
  let ScalingService;
  let PageStructureService;

  let iframe;
  let base;

  let container1;
  let container2;
  let component1;
  // let component2;

  beforeEach(() => {
    module('hippo-cm.channel.hippoIframe');

    inject((_DragDropService_, _hstCommentsProcessorService_, _ScalingService_, _PageStructureService_) => {
      DragDropService = _DragDropService_;
      hstCommentsProcessor = _hstCommentsProcessorService_;
      ScalingService = _ScalingService_;
      PageStructureService = _PageStructureService_;
    });

    jasmine.getFixtures().load('channel/hippoIframe/dragDrop.service.fixture.html');

    iframe = $j('#testIframe');
    base = $j('#testBase');
  });

  function createContainer(number) {
    const iframeContainerComment = iframe.contents().find(`#iframeContainerComment${number}`);
    const overlayContainer = $j(`#overlayContainer${number}`);
    const result = new ContainerElement(iframeContainerComment[0], {
      uuid: `container${number}`,
      'HST-Type': 'HST.vBox',
    }, hstCommentsProcessor);
    result.setJQueryElement('overlay', overlayContainer);
    return result;
  }

  function createComponent(number, container) {
    const iframeComponentComment = iframe.contents().find(`#iframeComponentComment${number}`);
    const overlayComponent = $j(`#overlayComponent${number}`);
    const metaData = {
      uuid: `component${number}`,
      'HST-Label': `Component ${number}`,
    };
    const result = new ComponentElement(iframeComponentComment[0], metaData, container, hstCommentsProcessor);
    result.setJQueryElement('overlay', overlayComponent);
    container.addComponent(result);
    return result;
  }

  function loadIframeFixture(callback) {
    DragDropService.init(iframe, base);
    iframe.one('load', () => {
      const iframeWindow = iframe[0].contentWindow;

      // make ESLint happy; it claims that "'container1' is never reassigned, use 'const' instead" :(
      container1 = '';
      container1 = createContainer(1);
      component1 = createComponent(1, container1);
      createComponent(2, container1);

      // make ESLint happy; it claims that "'container2' is never reassigned, use 'const' instead" :(
      container2 = '';
      container2 = createContainer(2);

      DragDropService.enable([container1, container2]).then(() => {
        try {
          callback(iframeWindow);
        } catch (e) {
          // Karma silently swallows stack traces for synchronous tests, so log them in an explicit fail
          fail(e);
        }
      });
    });
    iframe.attr('src', `/${jasmine.getFixtures().fixturesPath}/channel/hippoIframe/dragDrop.service.iframe.fixture.html`);
  }

  it('is not dragging initially', () => {
    expect(DragDropService.isDragging()).toEqual(false);
  });

  it('injects dragula.js into the iframe', (done) => {
    loadIframeFixture((iframeWindow) => {
      expect(typeof iframeWindow.dragula).toEqual('function');
      expect(DragDropService.drake).toBeDefined();
      expect(DragDropService.isDragging()).toEqual(false);
      done();
    });
  });

  it('destroys dragula on iframe unload', (done) => {
    loadIframeFixture((iframeWindow) => {
      expect(DragDropService.drake).not.toBeNull();
      $(iframeWindow).trigger('beforeunload');
      expect(DragDropService.drake).toBeNull();
      expect(DragDropService.isDragging()).toEqual(false);
      done();
    });
  });

  it('forwards a shifted mouse event to the iframe when it starts dragging in a non-scaled iframe', (done) => {
    loadIframeFixture(() => {
      const mockedMouseDownEvent = {
        type: 'mousedown',
        clientX: 100,
        clientY: 200,
      };
      const iframeComponentElement1 = component1.getJQueryElement('iframe')[0];

      iframe.offset({
        left: 10,
        top: 20,
      });
      spyOn(ScalingService, 'getScaleFactor').and.returnValue(1.0);
      spyOn(iframeComponentElement1, 'dispatchEvent');

      DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

      expect(DragDropService.isDragging()).toEqual(true);

      const dispatchedEvent = iframeComponentElement1.dispatchEvent.calls.argsFor(0)[0];
      expect(dispatchedEvent.type).toEqual('mousedown');
      expect(dispatchedEvent.bubbles).toEqual(true);
      expect(dispatchedEvent.clientX).toEqual(90);
      expect(dispatchedEvent.clientY).toEqual(180);

      done();
    });
  });

  it('forwards a shifted mouse event to the iframe when it starts dragging in a scaled iframe', (done) => {
    loadIframeFixture(() => {
      const mockedMouseDownEvent = {
        type: 'mousedown',
        clientX: 150,
        clientY: 150,
      };
      const iframeComponentElement1 = component1.getJQueryElement('iframe')[0];

      base.offset({
        left: 10,
        top: 10,
      });
      iframe.offset({
        left: 20,
        top: 20,
      });
      iframe.width(200);

      spyOn(ScalingService, 'getScaleFactor').and.returnValue(0.5);
      spyOn(iframeComponentElement1, 'dispatchEvent');

      DragDropService.startDragOrClick(mockedMouseDownEvent, component1);

      const dispatchedEvent = iframeComponentElement1.dispatchEvent.calls.argsFor(0)[0];

      expect(dispatchedEvent.type).toEqual('mousedown');
      expect(dispatchedEvent.bubbles).toEqual(true);
      expect(dispatchedEvent.clientX).toEqual(80);
      expect(dispatchedEvent.clientY).toEqual(260);
      done();
    });
  });

  it('stops dragging when disabled', (done) => {
    loadIframeFixture(() => {
      const mockedMouseDownEvent = {
        type: 'mousedown',
        clientX: 100,
        clientY: 200,
      };

      DragDropService.startDragOrClick(mockedMouseDownEvent, component1);
      expect(DragDropService.isDragging()).toEqual(true);

      DragDropService.disable();
      expect(DragDropService.isDragging()).toEqual(false);

      done();
    });
  });

  it('shows the component properties when a component receives a mouseup event', (done) => {
    loadIframeFixture(() => {
      const iframeComponent1 = component1.getJQueryElement('iframe');

      spyOn(PageStructureService, 'showComponentProperties');

      iframeComponent1.on('mouseup', () => {
        expect(PageStructureService.showComponentProperties).toHaveBeenCalledWith(component1);
        done();
      });
      iframeComponent1.trigger('mouseup');
    });
  });

  it('stops dragging', () => {
    DragDropService.dragging = true;
    DragDropService._stopDrag();
    expect(DragDropService.isDragging()).toEqual(false);
  });
});
