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

class ChoiceFieldCtrl {
  constructor($element, $scope, $timeout, FeedbackService, FieldService) {
    'ngInject';

    this.expanded = new Set();
    this.$element = $element;
    this.$scope = $scope;
    this.$timeout = $timeout;
    this.FeedbackService = FeedbackService;
    this.FieldService = FieldService;
  }

  $onInit() {
    this.$scope.$watch(() => !!this.fieldValues, () => this.$timeout(() => {
      if (this.sortable) {
        this.sortable.destroy();
        delete this.sortable;
      }

      if (this.fieldValues) {
        // @see https://github.com/SortableJS/angular-legacy-sortablejs/issues/44
        this.sortable = new Sortable(this.$element.find('[ng-sortable-area]')[0], {
          animation: 300,
          chosenClass: 'field--dragged',
          disabled: !this.isDraggable(),
          forceFallback: true,
          fallbackClass: 'field--ghost',
          handle: '[ng-sortable-handle]',
          onStart: this.onDrag.bind(this),
          onUpdate: this.onDragging.bind(this),
          onEnd: this.onDrop.bind(this),
        });
      }
    }));

    this.$scope.$watch(() => !this.isDraggable(), (disabled) => {
      if (this.sortable) {
        this.sortable.option('disabled', disabled);
      }
    });
  }

  getFieldName(index) {
    return index > 0 ? `${this.name}[${index + 1}]` : `${this.name}`;
  }

  onFocus() {
    this.hasFocus = true;
    this.$element.triggerHandler('focus');
  }

  onBlur() {
    delete this.hasFocus;
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
      this.$scope.$broadcast('field:drag', this);
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
      this.$scope.$broadcast('field:drop', this);
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

  _focus(index) {
    this.$timeout(() => {
      const { id } = this.fieldType.choices[this.fieldValues[index].chosenId];
      const name = `${this.getFieldName(index)}/${id}`;
      const field = Object.keys(this.form).find(key => key.startsWith(name));

      if (!field) {
        return;
      }

      const element = this.form[field].$$element;

      element[0].focus();
      this.$timeout(() => element[0].scrollIntoView(), 500);
    });
  }
}

export default ChoiceFieldCtrl;
