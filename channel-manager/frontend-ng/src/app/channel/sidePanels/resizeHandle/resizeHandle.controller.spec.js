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

describe('resizeHandle component', () => {
  let $componentController;
  let $ctrl;

  let mockHandleElement;
  let mockSidePanelElement;

  function event(name, pageX) {
    const e = new $.Event(name);
    e.pageX = pageX;
    return e;
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_) => {
      $componentController = _$componentController_;
    });

    jasmine.getFixtures().load('channel/sidePanels/resizeHandle/resizeHandle.controller.fixture.html');
    mockHandleElement = $('#resizeHandle');
    mockSidePanelElement = $('#sidePanel');

    $ctrl = $componentController('resizeHandle', {
      $element: mockHandleElement,
    }, {
      element: mockSidePanelElement,
      minWidth: 100,
      onResize: () => { },
    });

    spyOn($ctrl, 'onResize');

    $ctrl.$onInit();
  });

  afterEach(() => {
    $('.resize-handle-mask').remove();
  });

  it('initializes minWidth and maxWidth', () => {
    expect($ctrl.minWidth).toBe(100);
    expect($ctrl.maxWidth).toBe($('body').width() / 2);
  });

  describe('starting a resize', () => {
    it('creates and shows a transparent mask covering the page on mouse down on the handle', () => {
      mockHandleElement.trigger('mousedown');

      expect('.resize-handle-mask').toBeInDOM();
      expect('.resize-handle-mask').toBeVisible();
      expect($('.resize-handle-mask').width()).toEqual($('body').width());
      expect($('.resize-handle-mask').height()).toEqual($('body').height());
    });

    it('attaches the mask to the body', () => {
      mockHandleElement.trigger('mousedown');

      expect($('.resize-handle-mask').parent()[0]).toEqual($('body')[0]);
    });
  });

  describe('resizing', () => {
    beforeEach(() => {
      mockSidePanelElement.width('100px');
    });

    it('captures the mouse move events on the mask element', () => {
      mockHandleElement.trigger(event('mousedown', 100));

      const mask = $('.resize-handle-mask');
      mask.trigger(event('mousemove', 200));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 200 });
    });

    it('resizes the target element if handle is on the left side', () => {
      $ctrl.handlePosition = 'left';
      $ctrl.$onInit();

      mockHandleElement.trigger(event('mousedown', $ctrl.maxWidth - 100));
      $('.resize-handle-mask').trigger(event('mousemove', $ctrl.maxWidth - 200));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 200 });
      expect(mockSidePanelElement).toHaveCss({ width: '200px' });

      $('.resize-handle-mask').trigger(event('mousemove', $ctrl.maxWidth - 150));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 150 });
      expect(mockSidePanelElement).toHaveCss({ width: '150px' });
    });

    it('resizes the target element if handle is on the right side', () => {
      $ctrl.handlePosition = 'right';
      $ctrl.$onInit();

      mockHandleElement.trigger(event('mousedown', 100));
      $('.resize-handle-mask').trigger(event('mousemove', 200));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 200 });
      expect(mockSidePanelElement).toHaveCss({ width: '200px' });

      $('.resize-handle-mask').trigger(event('mousemove', 150));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 150 });
      expect(mockSidePanelElement).toHaveCss({ width: '150px' });
    });

    it('does not resize below the minWidth', () => {
      mockHandleElement.trigger(event('mousedown', 100));

      const mask = $('.resize-handle-mask');
      mask.trigger(event('mousemove', 0));

      expect($ctrl.onResize).not.toHaveBeenCalled();

      mockSidePanelElement.width('150px');
      mask.trigger(event('mousemove', 0));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: 100 });
    });

    it('does not resize above the maxWidth', () => {
      mockHandleElement.trigger(event('mousedown', 100));

      const mask = $('.resize-handle-mask');
      mask.trigger(event('mousemove', $ctrl.maxWidth + 10));

      expect($ctrl.onResize).toHaveBeenCalledWith({ newWidth: $ctrl.maxWidth });

      $ctrl.onResize.calls.reset();
      mask.trigger(event('mousemove', $ctrl.maxWidth + 10));

      expect($ctrl.onResize).not.toHaveBeenCalled();
    });
  });

  describe('ending a resize', () => {
    it('removes the mask on mouse up on the mask', () => {
      mockHandleElement.trigger('mousedown');

      const mask = $('.resize-handle-mask');
      mask.trigger('mouseup');

      expect('.resize-handle-mask').not.toBeInDOM();
    });

    it('removes the mouse up and mouse move event listeners from the mask', () => {
      mockHandleElement.trigger('mousedown');

      const mask = $('.resize-handle-mask');
      mask.trigger('mouseup');

      const events = $._data(mask[0], 'events');
      expect(events).toBeUndefined();
    });
  });

  it('will make the manipulated element bigger when the handle is positioned on the left', () => {
    $ctrl.handlePosition = 'left';
    $ctrl.$onInit();

    expect($ctrl.add).toBe(true);
  });

  it('will make the manipulated element smaller when the handle is not positioned on the left', () => {
    $ctrl.handlePosition = 'notdefaultvalue';
    $ctrl.$onInit();

    expect($ctrl.add).toBe(false);
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
