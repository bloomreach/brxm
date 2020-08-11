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

describe('resizeHandle component', () => {
  let $componentController;
  let $ctrl;
  let $window;

  let mockHandleElement;

  const event = angular.element.Event;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_, _$window_) => {
      $componentController = _$componentController_;
      $window = _$window_;
    });

    jasmine.getFixtures().load('channel/sidePanels/resizeHandle/resizeHandle.controller.fixture.html');
    mockHandleElement = $('#resizeHandle');

    $ctrl = $componentController('resizeHandle', {
      $element: mockHandleElement,
    }, {
      elementWidth: 100,
      onResize: () => { },
    });

    spyOn($ctrl, 'onResize');

    $ctrl.$onInit();
  });

  afterEach(() => {
    $('.resize-handle-mask').remove();
  });

  describe('starting a resize', () => {
    it('should not start a resize if the left button is not being pressed', () => {
      const mouseDownEvent = event('mousedown');
      spyOn(mouseDownEvent, 'preventDefault');

      mockHandleElement.trigger(mouseDownEvent);
      expect(mouseDownEvent.preventDefault).not.toHaveBeenCalled();
    });

    it('initializes maxWidth as a round number', () => {
      const body = $('body');
      const oldBodyWidth = body.width();

      body.width('40px');
      mockHandleElement.trigger(event('mousedown', { which: 1 }));
      expect($ctrl.maxWidth).toBe(20);

      body.width('39px');
      mockHandleElement.trigger(event('mousedown', { which: 1 }));
      expect($ctrl.maxWidth).toBe(19);

      body.width(oldBodyWidth);
    });

    it('creates and shows a transparent mask covering the page on mouse down on the handle', () => {
      mockHandleElement.trigger(event('mousedown', { which: 1 }));

      expect('.resize-handle-mask').toBeInDOM();
      expect('.resize-handle-mask').toBeVisible();
      expect($('.resize-handle-mask').width()).toEqual($('body').width());
      expect($('.resize-handle-mask').height()).toEqual($('body').height());
    });

    it('attaches the mask to the body', () => {
      mockHandleElement.trigger(event('mousedown', { which: 1 }));

      expect($('.resize-handle-mask').parent()[0]).toEqual($('body')[0]);
    });
  });

  describe('resizing', () => {
    it('captures the mouse move events on the mask element', () => {
      mockHandleElement.trigger(event('mousedown', { which: 1, pageX: 100 }));

      const mask = $('.resize-handle-mask');
      mask.trigger(event('mousemove', { buttons: 1, pageX: 200 }));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 200 });
    });

    it('resizes the target element if handle is on the left side', () => {
      $ctrl.handlePosition = 'left';
      $ctrl.$onInit();

      mockHandleElement.trigger(event('mousedown', { which: 1, pageX: 100 }));
      $('.resize-handle-mask').trigger(event('mousemove', { buttons: 1, pageX: 0 }));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 200 });

      $('.resize-handle-mask').trigger(event('mousemove', { buttons: 1, pageX: 50 }));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 150 });
    });

    it('resizes the target element if handle is on the right side', () => {
      $ctrl.handlePosition = 'right';
      $ctrl.$onInit();

      mockHandleElement.trigger(event('mousedown', { which: 1, pageX: 100 }));
      $('.resize-handle-mask').trigger(event('mousemove', { buttons: 1, pageX: 200 }));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 200 });

      $('.resize-handle-mask').trigger(event('mousemove', { buttons: 1, pageX: 150 }));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 150 });
    });

    it('does not resize above the maxWidth', () => {
      mockHandleElement.trigger(event('mousedown', { which: 1, pageX: 100 }));

      const mask = $('.resize-handle-mask');
      mask.trigger(event('mousemove', { buttons: 1, pageX: $ctrl.maxWidth + 10 }));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: $ctrl.maxWidth });
    });
  });

  describe('ending a resize', () => {
    it('should stop a resize if the left button is not being pressed', () => {
      mockHandleElement.trigger(event('mousedown', { which: 1, pageX: 100 }));

      const mask = $('.resize-handle-mask');
      mask.trigger(event('mousemove', { buttons: 0, pageX: 200 }));

      expect('.resize-handle-mask').not.toBeInDOM();
    });

    it('removes the mask on mouse up on the mask', () => {
      mockHandleElement.trigger(event('mousedown', { which: 1, pageX: 100 }));

      const mask = $('.resize-handle-mask');
      mask.trigger('mouseup');

      expect('.resize-handle-mask').not.toBeInDOM();
    });

    it('removes the mouse up and mouse move event listeners from the mask', () => {
      mockHandleElement.trigger(event('mousedown', { which: 1, pageX: 100 }));

      const mask = $('.resize-handle-mask');
      mask.trigger('mouseup');

      const events = $._data(mask[0], 'events');
      expect(events).toBeUndefined();
    });

    it('triggers a window resize event', () => {
      spyOn($window, 'dispatchEvent');
      const resizeEvent = new Event('resize');

      mockHandleElement.trigger(event('mousedown', { which: 1, pageX: 100 }));
      const mask = $('.resize-handle-mask');
      mask.trigger('mouseup');

      const mostRecentEvent = $window.dispatchEvent.calls.mostRecent().args[0];
      expect(mostRecentEvent).toEqual(resizeEvent);
      expect(mostRecentEvent.type).toEqual(resizeEvent.type);
    });
  });

  it('will make the manipulated element bigger when the handle is positioned on the left', () => {
    $ctrl.handlePosition = 'left';
    $ctrl.$onInit();

    expect($ctrl.isInversed).toBe(true);
  });

  it('will make the manipulated element smaller when the handle is not positioned on the left', () => {
    $ctrl.handlePosition = 'notdefaultvalue';
    $ctrl.$onInit();

    expect($ctrl.isInversed).toBe(false);
  });

  it('adds a class named left on the handle when the handle is positioned on the left', () => {
    $ctrl.handlePosition = 'left';
    $ctrl.$onInit();

    expect($ctrl.handle).toHaveClass('left');
  });

  it('adds a class named right on the handle when the handle is not positioned on the left', () => {
    $ctrl.handlePosition = 'notthedefaultvalue';
    $ctrl.$onInit();

    expect($ctrl.handle).toHaveClass('right');
  });
});
