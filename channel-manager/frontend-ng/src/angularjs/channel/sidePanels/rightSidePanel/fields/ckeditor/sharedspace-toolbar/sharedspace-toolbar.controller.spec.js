/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

describe('Sharedspace toolbar component controller', () => {
  let $rootScope;
  let $componentController;
  let $ctrl;
  let SharedSpaceToolbarService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((_$rootScope_,
            _$componentController_,
            _SharedSpaceToolbarService_) => {
      $rootScope = _$rootScope_;
      $componentController = _$componentController_;
      SharedSpaceToolbarService = _SharedSpaceToolbarService_;
    });

    $ctrl = $componentController('sharedspaceToolbar', {
      $rootScope,
      $element: angular.element('<div></div>'),
      SharedSpaceToolbarService,
    });
  });

  it('should initialize component', () => {
    spyOn(SharedSpaceToolbarService, 'registerTriggerCallback');
    $ctrl.$onInit();
    expect(SharedSpaceToolbarService.registerTriggerCallback).toHaveBeenCalled();
    expect($ctrl.sharedSpaceElement).toBeDefined();
    expect($ctrl.rightSidePanelContent).toBeDefined();
  });

  it('should destroy component', () => {
    $ctrl.isVisible = true;
    $ctrl.$onDestroy();
    expect($ctrl.isVisible).toEqual(false);
  });

  it('should set toolbar visibility state', () => {
    $ctrl.$onInit();

    $ctrl.setToolbarVisible(true, {
      hasBottomToolbar: false,
    });

    expect($ctrl.isVisible).toEqual(true);
    expect($ctrl.showBottomToolbar).toEqual(false);
    expect(SharedSpaceToolbarService.isToolbarVisible).toEqual(true);
  });

  describe('fix scrolling position', () => {
    const animateOptions = jasmine.any(Object);
    const mockHeight = 110;

    beforeEach(() => {
      $ctrl.$onInit();
      $ctrl.$element.append($('<div class="ckeditor-shared-space-top"></div>').height(mockHeight));

      spyOn($ctrl.$element, 'animate');
      spyOn($ctrl.sharedSpaceElement, 'animate');
      spyOn($ctrl.rightSidePanelContent, 'animate');
    });

    it('should fix scrolling position when state is true', () => {
      $ctrl._fixScrollingPosition(true);
      expect($ctrl.$element.animate).toHaveBeenCalledWith({ maxHeight: mockHeight }, animateOptions);
      expect($ctrl.sharedSpaceElement.animate).toHaveBeenCalledWith({ top: 0 }, animateOptions);
      expect($ctrl.rightSidePanelContent.animate).toHaveBeenCalledWith({ scrollTop: `+=${mockHeight}` }, animateOptions);
    });

    it('should fix scrolling position when state is false', () => {
      $ctrl._fixScrollingPosition(false);
      expect($ctrl.$element.animate).toHaveBeenCalledWith({ maxHeight: 0 }, animateOptions);
      expect($ctrl.sharedSpaceElement.animate).toHaveBeenCalledWith({ top: `-${mockHeight}px` }, animateOptions);
      expect($ctrl.rightSidePanelContent.animate).toHaveBeenCalledWith({ scrollTop: `-=${mockHeight}` }, animateOptions);
    });
  });
});
