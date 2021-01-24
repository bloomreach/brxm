/*
 * Copyright 2016-2021 Hippo B.V. (http://www.onehippo.com)
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

import Sortable from 'sortablejs';

export default class CompoundFieldCtrl {
  constructor($element, $scope, $timeout, FeedbackService, FieldService) {
    'ngInject';

    this.expanded = new WeakSet();
    this.$element = $element;
    this.$scope = $scope;
    this.$timeout = $timeout;
    this.FeedbackService = FeedbackService;
    this.FieldService = FieldService;
  }

  $onInit() {
    // @see https://github.com/SortableJS/angular-legacy-sortablejs/issues/44
    this.sortable = new Sortable(this.$element[0], {
      animation: 300,
      chosenClass: 'field--dragged',
      disabled: true,
      forceFallback: true,
      fallbackClass: 'field--ghost',
      handle: '[ng-sortable-handle]',
      onStart: this.onDrag.bind(this),
      onUpdate: this.onDragging.bind(this),
      onEnd: this.onDrop.bind(this),
    });

    this.$scope.$watch(() => !this.isDraggable(), (disabled) => {
      this.sortable.option('disabled', disabled);
    });
  }

  getFieldName(index) {
    const fieldName = this.name ? `${this.name}/${this.fieldType.id}` : this.fieldType.id;
    return index > 0 ? `${fieldName}[${index + 1}]` : fieldName;
  }

  onFocus() {
    this.$element.triggerHandler('focus');
  }

  onBlur() {
    this.$element.triggerHandler('blur');
  }

  isDraggable() {
    return this.fieldType.multiple && this.fieldType.orderable && this.fieldValues && this.fieldValues.length > 1;
  }

  onDrag({
    clone,
    from,
    item,
    oldIndex,
  }) {
    this._nextNode = from === item.parentNode ? item.nextSibling : clone.nextSibling;
    this.$scope.$apply(() => {
      this.dragging = oldIndex;
      this.$scope.$broadcast('field:drag');
    });
  }

  onDragging({ from, item }) {
    // Move ng-repeat comment node to the right position
    // @see https://github.com/SortableJS/angular-legacy-sortablejs/blob/master/angular-legacy-sortable.js#L136
    if (this._nextNode.nodeType === Node.COMMENT_NODE) {
      from.insertBefore(this._nextNode, item.nextSibling);
    }
  }

  async onDrop({ newIndex, oldIndex }) {
    this.$scope.$apply(() => {
      delete this.dragging;
      this._move(oldIndex, newIndex);
    });

    try {
      await this.FieldService.reorder({ name: this.getFieldName(oldIndex), order: newIndex + 1 });
      this.form.$setDirty();
      this._focus(newIndex);
    } catch (error) {
      this.FeedbackService.showError('ERROR_FIELD_REORDER');
      this._move(newIndex, oldIndex);
      this._focus(oldIndex);
    } finally {
      this.$scope.$broadcast('field:drop');
    }
  }

  async onMove(oldIndex, newIndex) {
    try {
      await this.FieldService.reorder({ name: this.getFieldName(oldIndex), order: newIndex + 1 });
      this._move(oldIndex, newIndex);
      this._focus(newIndex);
      this.form.$setDirty();
    } catch (error) {
      this.FeedbackService.showError('ERROR_FIELD_REORDER');
    }
  }

  _move(oldIndex, newIndex) {
    this.fieldValues.splice(newIndex, 0, this.fieldValues.splice(oldIndex, 1)[0]);
  }

  _focus(index, reset = false) {
    this.$timeout(() => {
      const name = this.getFieldName(index);
      const field = Object.keys(this.form).find(key => key.startsWith(name));

      if (!field) {
        return;
      }

      if (reset) {
        this.form[field].$setUntouched();
      }

      const element = this.form[field].$$element;

      element[0].focus();
      element[0].scrollIntoView();
    });
  }

  async onAdd(index = 0) {
    try {
      const fields = await this.FieldService.add({ name: this.getFieldName(index) });

      if (!this.fieldValues) {
        this.fieldValues = [];
      }

      this.fieldValues.splice(index, 0, { fields });
      this.form.$setDirty();
      this._focus(index, true);
    } catch (error) {
      this.FeedbackService.showError('ERROR_FIELD_ADD');
    }
  }
}
