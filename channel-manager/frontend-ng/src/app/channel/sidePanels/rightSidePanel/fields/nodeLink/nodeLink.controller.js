/*
 * Copyright 2018-2019 Hippo B.V. (http://www.onehippo.com)
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

export default class NodeLinkController {
  constructor($element, $scope, $timeout, PickerService) {
    'ngInject';

    this.$element = $element;
    this.$scope = $scope;
    this.$timeout = $timeout;
    this.PickerService = PickerService;
  }

  $onInit() {
    if (this.index === 0) {
      this.$scope.$on('primitive-field:focus', ($event, focusEvent) => this.onFocusFromParent(focusEvent));
    }

    // material doesn't do that in our case because the input field is read-only
    // @see https://github.com/angular/material/blob/master/src/components/input/input.js#L397
    this.$scope.$watch(
      () => this.ngModel.$invalid && this.ngModel.$touched,
      isInvalid => this.mdInputContainer && this.mdInputContainer.setInvalid(isInvalid),
    );
  }

  onFocusFromParent(focusEvent) {
    // Don't let the click event bubble through the label as it can trigger an
    // unexpected click on the input element
    focusEvent.preventDefault();

    if (this.ngModel.$modelValue === '') {
      this.open();
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
  onBlur($event) {
    this._blurPromise = this.$timeout(() => {
      if (this._pickerPromise) {
        return;
      }

      if (this.mdInputContainer) {
        this.mdInputContainer.setFocused(false);
      }

      this.hasFocus = false;
      this.$element.triggerHandler($event || 'blur');
    }, 10);
  }

  onFocus($event) {
    if (this.mdInputContainer) {
      this.mdInputContainer.setFocused(true);
    }

    if (this._blurPromise) {
      this.$timeout.cancel(this._blurPromise);
    }

    this.hasFocus = true;
    this.$element.triggerHandler($event || 'focus');
  }

  open() {
    this.onFocus();
    this._pickerPromise = this._pickerPromise || (async () => {
      try {
        const data = await this._showPicker();
        await this._onPick(data);
      } catch (e) {
        this._focusSelectButton();
      } finally {
        delete this._pickerPromise;
      }
    })();

    return this._pickerPromise;
  }

  async _showPicker() {
    const { uuid: value, displayName } = await this.PickerService.pickLink(
      this.config.linkpicker,
      { uuid: this.ngModel.$modelValue },
    );

    return { value, displayName };
  }

  _onPick({ value, displayName }) {
    if (this.isPicked) {
      this._focusSelectButton();
    }
    this.isPicked = true;
    this.displayName = displayName;
    this.ngModel.$setViewValue(value);
  }

  clear() {
    this.isPicked = false;
    this.displayName = '';
    this.ngModel.$setTouched();
    this.ngModel.$setViewValue('');
    this._focusSelectButton();
  }
}
