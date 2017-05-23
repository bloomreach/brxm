/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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
  let mockDocumentElement;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$componentController_) => {
      $componentController = _$componentController_;
    });

    jasmine.getFixtures().load('channel/sidePanels/rightSidePanel/resizeHandle/resizeHandle.controller.fixture.html');
    mockHandleElement = $j('#resizeHandle');
    mockSidePanelElement = $j('#sidePanel');
    mockDocumentElement = $j('<div></div>');

    $ctrl = $componentController('resizeHandle', {
      $element: mockHandleElement,
      $document: mockDocumentElement,
    }, {
      element: mockSidePanelElement,
      onResize: () => { },
    });

    $ctrl.maxWidth = 1350;
    spyOn($ctrl, 'onResize');
  });

  it('should initialize the component', () => {
    spyOn($ctrl, '_registerEvents');
    $ctrl.$onInit();
    expect($ctrl._registerEvents).toHaveBeenCalled();
  });

  it('should register events', () => {
    $ctrl._registerEvents(mockSidePanelElement);

    const eMouseDown = new $j.Event('mousedown');
    eMouseDown.pageX = 500;
    eMouseDown.pageY = 100;

    mockHandleElement.trigger(eMouseDown);

    mockSidePanelElement.width('450px');
    expect(mockSidePanelElement.width()).toEqual(450);

    const eMouseMove = new $j.Event('mousemove');
    eMouseMove.pageX = 800;
    eMouseMove.pageY = 100;

    $ctrl.$document.trigger(eMouseMove);

    $ctrl.$document.trigger('mouseup');
    expect(mockSidePanelElement.width() > 440).toEqual(true);
  });

  it('should not allow a too small or too big sidepanel', () => {
    $ctrl._registerEvents(mockSidePanelElement);

    const eMouseDown = new $j.Event('mousedown');
    eMouseDown.pageX = 500;
    eMouseDown.pageY = 100;

    mockHandleElement.trigger(eMouseDown);

    mockSidePanelElement.width('440px');
    expect(mockSidePanelElement.width()).toEqual(440);

    spyOn(mockSidePanelElement, 'css');

    const eMouseMove = new $j.Event('mousemove');
    eMouseMove.pageX = 99999;
    eMouseMove.pageY = 100;

    $ctrl.$document.trigger(eMouseMove);

    $ctrl.$document.trigger('mouseup');
    expect(mockSidePanelElement.css).not.toHaveBeenCalled();
  });
});
