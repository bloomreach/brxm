/*
 * Copyright 2021 Hippo B.V. (http://www.onehippo.com)
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

export default class CompoundFieldValueCtrl {
  constructor($element, $scope) {
    'ngInject';

    this.children = new Set();
    this.$element = $element;
    this.$scope = $scope;

    this.onDrag = this.onDrag.bind(this);
    this.onDrop = this.onDrop.bind(this);
  }

  $onInit() {
    if (this.parent) {
      this.parent.children.add(this);
    }

    this.$scope.$on('field:drag', this.onDrag);
    this.$scope.$on('field:drop', this.onDrop);
    this.$scope.$watch(() => this.hasFocus, value => value && this.collapse.isCollapsed && this.collapse.open());
  }

  $onChanges({ fieldValue }) {
    if (fieldValue) {
      this.setError(!!fieldValue.currentValue.errorInfo);
    }
  }

  $onDestroy() {
    if (this.parent) {
      this.parent.children.delete(this);
    }
  }

  setError(value) {
    this.hasError = value;

    if (this.hasError) {
      this.collapse.open();
      this.$element.triggerHandler('focus');
    }

    if (this.parent) {
      this.parent.setError([...this.parent.children]
        .reduce((result, child) => result || child.hasError, false));
    }
  }

  onDrag() {
    if (!this.collapse.isCollapsed) {
      this.context.expanded.add(this.fieldValue);
    }

    this.collapse.collapse();
  }

  onDrop() {
    if (this.context.expanded.has(this.fieldValue)) {
      this.collapse.open();
      this.context.expanded.delete(this.fieldValue);
    }
  }
}
