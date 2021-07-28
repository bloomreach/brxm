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
          fallbackTolerance: 10,
        });
      }
    }));

    this.$scope.$watch(() => !this.isDraggable(), (disabled) => {
      if (this.sortable) {
        this.sortable.option('disabled', disabled);
      }
    });
  }

  $onChanges({ fieldType }) {
    if (fieldType) {
      this.choices = Object.entries(this.fieldType.choices).map(([id, { displayName }]) => ({ id, displayName }));
    }
  }

  getFieldName(index) {
    return `${this.name}${index > 0 ? `[${index + 1}]` : ''}`;
  }

  getPrimitiveFieldName(index) {
    return `${this.getFieldName(index)}/${this.fieldType.choices[this.fieldValues[index].chosenId].id}`;
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

  isEmpty() {
    return !this.fieldValues || !this.fieldValues.length;
  }

  isRemovable() {
    // we can always remove an optional value
    if (this.fieldType.optional) {
      return true;
    }

    // if the user can only choose 1 type and the minimum amount of fields is reached, it makes no sense to remove
    if (Object.keys(this.fieldType.choices).length === 1 && this._getNumberOfValues() === this.fieldType.minValues) {
      return false;
    }

    return this._getNumberOfValues() > 0;
  }

  isAddable() {
    return this.fieldType.multiple || (this.fieldType.optional && !this.fieldValues.length);
  }

  hasMaxValues() {
    return this.fieldType.hasMaxValues;
  }

  hasReachedMaxValues() {
    return this._getNumberOfValues() === this.fieldType.maxValues;
  }

  getMaxValuesStatus() {
    return {
      current: this._getNumberOfValues(),
      max: this.fieldType.maxValues,
    };
  }

  _getNumberOfValues() {
    return this.fieldValues ? this.fieldValues.length : 0;
  }

  onDrag({ clone, from, item, oldIndex, }) {
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
      this._focus(newIndex, false, this._isCKEditorField(newIndex));
    } catch (error) {
      this.FeedbackService.showError('ERROR_FIELD_REORDER');
      this._move(newIndex, oldIndex);
      this._focus(oldIndex, false, this._isCKEditorField(oldIndex));
    } finally {
      this.$scope.$broadcast('field:drop', this);
    }
  }

  async onMove(oldIndex, newIndex) {
    try {
      await this.FieldService.reorder({ name: this.getFieldName(oldIndex), order: newIndex + 1 });
      this._move(oldIndex, newIndex);
      this.form.$setDirty();
      this._focus(newIndex, false, this._isCKEditorField(newIndex));
    } catch (error) {
      this.FeedbackService.showError('ERROR_FIELD_REORDER');
    }
  }

  _move(oldIndex, newIndex) {
    this.fieldValues.splice(newIndex, 0, this.fieldValues.splice(oldIndex, 1)[0]);
  }

  _focus(index, reset = false, isCKEditor = false) {
    this.$timeout(() => {
      const name = this.getFieldName(index);
      const fields = Object.keys(this.form);
      let field;

      /*
       * This check is needed because "this.form" not always has the same order as the fields in the page.
       * So, if the field selected happens to be in the first index, the name will match the first key that
       * starts with that string, which might be one with a higher index since the ordering isn't reliable.
       *
       * Example:
       *
       * const name = hst:employee/hst:interview;
       *
       * this.form = {
       *     hst:employee/hst:interview[3]/hst:name: { ... },
       *     hst:employee/hst:interview/hst:surname: { ... },
       *     hst:employee/hst:interview[2]/hst:address: { ... },
       * };
       *
       * hst:employee/hst:interview[3]/hst:name will be matched, which is the wrong field selected.
       */
      if (name.includes("[")) {
        field = fields.find(key => key.startsWith(name));
      } else {
        field = fields.find(key => key.startsWith(name) && !key.includes("["));
      }

      if (!field) {
        return;
      }

      if (reset) {
        this.form[field].$setUntouched();
      }

      const element = this.form[field].$$element;

      if (isCKEditor) {
        this.$timeout(() => {
          const cke = element[0].nextSibling.querySelector('.cke_editable');
          this._focusAndScrollIntoView(cke);
        });
      } else {
        this._focusAndScrollIntoView(element[0]);
      }
    });
  }

  _focusAndScrollIntoView(element) {
    element.focus();
    this.$timeout(() => element.scrollIntoView({ behavior: 'smooth', block: 'center' }), 500);
  }

  _focusAddButton() {
    this.$timeout(() => this.$element.find('.field__title-buttons button').focus());
  }

  _isCKEditorField(index) {
    const { chosenId } = this.fieldValues[index];
    return this.fieldType.choices[chosenId].type === 'HTML';
  }

  async onAdd(chosenId, index = 0) {
    try {
      const fields = await this.FieldService.add({ name: `${this.getFieldName(index)}/${chosenId}` });
      const chosenType = this.fieldType.choices[chosenId].type;
      const chosenValue = chosenType === 'COMPOUND' ? { fields } : fields[chosenId][0];

      if (!this.fieldValues) {
        this.fieldValues = [];
      }

      this.fieldValues.splice(index, 0, { chosenId, chosenValue });
      this.form.$setDirty();
      this._focus(index, true, chosenType === 'HTML');
    } catch (error) {
      this.FeedbackService.showError('ERROR_FIELD_ADD');
    }
  }

  async onRemove(index) {
    try {
      await this.FieldService.remove({ name: this.getFieldName(index) });
      this.fieldValues.splice(index, 1);
      this.form.$setDirty();

      if (this.fieldValues.length) {
        const prevIndex = Math.max(index - 1, 0);
        this._focus(prevIndex, false, this._isCKEditorField(prevIndex));
      } else {
        this._focusAddButton();
      }
    } catch (error) {
      this.FeedbackService.showError('ERROR_FIELD_REMOVE');
    }
  }
}

export default ChoiceFieldCtrl;
