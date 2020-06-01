/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
  constructor($element, $log, $rootScope, SharedSpaceToolbarService) {
    'ngInject';

    this.$element = $element;
    this.$log = $log;
    this.$rootScope = $rootScope;
    this.SharedSpaceToolbarService = SharedSpaceToolbarService;
    this.showBottomToolbar = null;
  }

  $onInit() {
    this.isVisible = this.isVisible === true;
    this.SharedSpaceToolbarService.registerTriggerCallback(this.setToolbarVisible.bind(this));
    this.sharedSpaceElement = this.$element.find('.ckeditor-shared-space');
    this.ckeditorContainer = this.$element.siblings('md-content').first();

    if (this.ckeditorContainer.length === 0) {
      this.$log.warn('SharedspaceToolbar: cannot find md-content sibling element');
    }

    this.sharedSpaceElement.css('display', 'none');
  }

  $onDestroy() {
    this.isVisible = false;
  }

  setToolbarVisible(state, options = {}) {
    this.isVisible = state;
    this.SharedSpaceToolbarService.isToolbarVisible = this.isVisible;
    this.showBottomToolbar = options.hasBottomToolbar || false;
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

    this.ckeditorContainer.animate({ scrollTop: scrollValue }, animateOptions);
  }
}

export default SharedSpaceToolbar;
