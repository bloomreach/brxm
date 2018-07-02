/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

class nodeLinkController {
  constructor($element, $scope, $timeout, CmsService) {
    'ngInject';

    this.$element = $element;
    this.$scope = $scope;
    this.$timeout = $timeout;
    this.CmsService = CmsService;
  }

  $onInit() {
    if (this.index === 0) {
      this.$scope.$on('primitive-field:focus', () => this.onFocusFromParent());
    }
  }

  onFocusFromParent() {
    if (this.ngModel.$modelValue === '') {
      this.openLinkPicker();
    } else {
      this._focusClearButton();
    }
  }

  _focusClearButton() {
    this.$element.find('.hippo-node-link-clear').focus();
  }

  _focusSelectButton() {
    this.$element.find('.hippo-node-link-select').focus();
  }

  // set "hasFocus" to false after a short timeout to prevent the bottom-border styling
  // of the link picker to flicker while tabbing; it *can* trigger a blur event, followed by
  // a immediate focus event, in which case the blue bottom border will be removed and added
  // again, resulting in annoying flickering of the UI.
  blur($event) {
    this.blurPromise = this.$timeout(() => {
      this.hasFocus = false;
    }, 10);
    this.onBlur($event);
  }

  focus($event) {
    if (this.blurPromise) {
      this.$timeout.cancel(this.blurPromise);
    }
    this.hasFocus = true;
    this.onFocus($event);
  }

  openLinkPicker() {
    const uuid = this.ngModel.$modelValue;
    this.CmsService.publish('show-link-picker', this.config.linkpicker, { uuid },
      link => this._onLinkPicked(link),
      () => this._focusSelectButton(),
    );
  }

  _onLinkPicked(link) {
    this.$scope.$apply(() => {
      if (this.linkPicked) {
        this._focusSelectButton();
      }
      this.linkPicked = true;
      this.displayName = link.displayName;
      this.ngModel.$setViewValue(link.uuid);
    });
  }

  clear() {
    this.linkPicked = false;
    this.displayName = '';
    this.ngModel.$setViewValue('');
    this._focusSelectButton();
  }
}

export default nodeLinkController;
