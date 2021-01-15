/*
 * Copyright 2016-2019 Hippo B.V. (http://www.onehippo.com)
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

class PrimitiveFieldCtrl {
  constructor($rootScope, $scope, FieldService, SharedSpaceToolbarService) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$scope = $scope;
    this.FieldService = FieldService;
    this.SharedSpaceToolbarService = SharedSpaceToolbarService;

    this.ngSortable = {
      chosenClass: 'field--dragged',
      forceFallback: true,
      fallbackClass: 'field--ghost',
      handle: '[ng-sortable-handle]',
      onStart: this.onDrag.bind(this),
      onEnd: this.onDrop.bind(this),
    };
  }

  $onChanges(changes) {
    if (changes.fieldValues) {
      this._onFieldValuesChanged(changes.fieldValues.currentValue);
    }
  }

  _onFieldValuesChanged(fieldValues) {
    delete this.firstServerError;

    fieldValues.forEach((value, index) => {
      const field = this.form[this.getFieldName(index)];
      if (!field) {
        return;
      }

      const isValid = !value.errorInfo || !value.errorInfo.message;
      field.$setValidity('server', isValid);
      if (!isValid) {
        field.$setTouched();
      }

      if (!isValid && !this.firstServerError) {
        this.firstServerError = value.errorInfo.message;
      }
    });
  }

  getFieldName(index) {
    const fieldName = this.name ? `${this.name}/${this.fieldType.id}` : this.fieldType.id;
    return index > 0 ? `${fieldName}[${index + 1}]` : fieldName;
  }

  getFieldError() {
    let combinedError = null;
    this.fieldValues.forEach((value, index) => {
      const fieldName = this.getFieldName(index);
      const field = this.form[fieldName];
      if (field) {
        combinedError = Object.assign(combinedError || {}, field.$error);
      }
    });
    return combinedError;
  }

  isValid() {
    return !this.fieldValues.some((fieldValue, index) => {
      const fieldName = this.getFieldName(index);
      const field = this.form[fieldName];
      return field && field.$invalid && field.$touched;
    });
  }

  onLabelClick($event) {
    this.$scope.$broadcast('primitive-field:focus', $event);
  }

  focusPrimitive($event = null) {
    this.hasFocus = true;
    this.onFieldFocus();

    this.oldValues = angular.copy(this.fieldValues);
    this.FieldService.unsetFocusedInput();

    if ($event) {
      $event.target = angular.element($event.target);
      this.FieldService.setFocusedInput($event.target, $event.customFocus);
    }
  }

  blurPrimitive($event = null) {
    if ($event) {
      $event.target = angular.element($event.relatedTarget);

      if (this.FieldService.shouldPreserveFocus($event.target)) {
        this.FieldService.triggerInputFocus();
        return;
      }
    }
    delete this.hasFocus;
    this.onFieldBlur();
    this._saveField();
  }

  valueChanged() {
    this._saveField({ throttle: true });
  }

  _saveField(options) {
    if (!angular.equals(
      this.FieldService.cleanValues(this.oldValues),
      this.FieldService.cleanValues(this.fieldValues),
    )) {
      this.FieldService.save({
        ...options,
        name: this.getFieldName(),
        values: this.fieldValues,
      }).then(this._afterSaveField.bind(this));
    }
  }

  _afterSaveField(validatedValues) {
    let errorsChanged = false;

    validatedValues.forEach((validatedValue, index) => {
      const currentValue = this.fieldValues[index];
      const field = this.form[this.getFieldName(index)];

      if (field) {
        field.$setTouched();
      }

      if (validatedValue.errorInfo && !currentValue.errorInfo) {
        currentValue.errorInfo = validatedValue.errorInfo;
        errorsChanged = true;
      }

      if (!validatedValue.errorInfo && currentValue.errorInfo) {
        delete currentValue.errorInfo;
        errorsChanged = true;
      }
    });

    if (errorsChanged) {
      this._onFieldValuesChanged(this.fieldValues);
    }
  }

  isDraggable() {
    return this.fieldType.multiple && this.fieldType.orderable && this.fieldValues.length > 1;
  }

  isRemovable() {
    return this.fieldType.multiple && (!this.fieldType.required || this.fieldValues.length > 1);
  }

  // eslint-disable-next-line consistent-return
  async onMove(oldIndex, newIndex) {
    const [value] = this.fieldValues.splice(oldIndex, 1);
    this.fieldValues.splice(newIndex, 0, value);
    await this._saveField();
    this.form.$setDirty();
    this.form[this.getFieldName(newIndex)].$$element[0].focus();
  }

  onDrag({ oldIndex }) {
    this.dragging = oldIndex;
  }

  async onDrop({ newIndex }) {
    delete this.dragging;
    await this._saveField();
    this.form.$setDirty();
    this.form[this.getFieldName(newIndex)].$$element[0].focus();
  }

  async onRemove(index) {
    this.fieldValues.splice(index, 1);
    await this._saveField();
    this.form.$setDirty();

    if (this.fieldValues.length) {
      this.form[this.getFieldName(Math.max(index - 1, 0))].$$element[0].focus();
    }
  }
}

export default PrimitiveFieldCtrl;
