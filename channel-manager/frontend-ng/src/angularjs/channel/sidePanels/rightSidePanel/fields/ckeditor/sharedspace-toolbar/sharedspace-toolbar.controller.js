/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

class SharedSpaceToolbar {
  constructor($rootScope, $element, SharedSpaceToolbarService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$element = $element;

    this.isVisible = this.isVisible || false;
    this.SharedSpaceToolbarService = SharedSpaceToolbarService;
    this.showBottomToolbar = null;
  }

  $onInit() {
    this.SharedSpaceToolbarService.registerTriggerCallback(this.setToolbarVisible.bind(this));
    this.sharedSpaceElement = this.$element.find('.ckeditor-shared-space');
    this.rightSidePanelContent = $('#rightSidePanel-content');

    this.sharedSpaceElement.css('display', 'none');
  }

  $onDestroy() {
    this.isVisible = false;
  }

  setToolbarVisible(state, options = {}) {
    this.isVisible = state;
    this.SharedSpaceToolbarService.isToolbarVisible = this.isVisible;
    this.showBottomToolbar = options.hasBottomToolbar || false;
    this.$rootScope.$apply();
    this._fixScrollingPosition(state);
  }

  _fixScrollingPosition(state) {
    if (state === true) {
      this.sharedSpaceElement.css('display', 'block');
      this.sharedSpaceElement.css('top', `-${this.sharedSpaceElement.height()}px`);
    }

    const toolbarHeight = this.$element.find('.ckeditor-shared-space-top').height();
    const scrollValue = state === true ? `+=${toolbarHeight}` : `-=${toolbarHeight}`;
    const animateOptions = { duration: 200, ease: 'linear', queue: false };

    this.$element.animate({ maxHeight: state === true ? toolbarHeight : 0 }, animateOptions);
    this.sharedSpaceElement.animate({ top: state === true ? 0 : `-${toolbarHeight}px` }, animateOptions);

    this.rightSidePanelContent.animate({ scrollTop: scrollValue }, animateOptions);
  }
}

export default SharedSpaceToolbar;
