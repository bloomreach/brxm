/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

const WIDGET_TYPES = {
  JcrPath: 'JcrPath',
  ImageSetPath: 'ImageSetPath',
  DropDownList: 'DropDownList',
  CheckBox: 'CheckBox',
  InputBox: 'InputBox',
};

export class ChannelPropertyCtrl {
  constructor($log, $scope, CmsService, ConfigService, PathService) {
    'ngInject';

    this.$log = $log;
    this.$scope = $scope;
    this.CmsService = CmsService;
    this.PathService = PathService;

    this.label = this.data.i18nResources[this.field] || this.field;
    this.type = this._getType();
    this.qaClass = this._getQaClass();
    this.readOnly = this.data.lockedBy && this.data.lockedBy !== ConfigService.cmsUser;

    if (this.type === 'JcrPath') {
      this.CmsService.subscribe('picked', this._onPicked, this);
      this.$scope.$on('$destroy', () => this.CmsService.unsubscribe('picked', this._onPicked, this));
    }
  }

  getDropDownListValues() {
    const fieldAnnotation = this._getFirstFieldAnnotation();
    if (!fieldAnnotation || fieldAnnotation.type !== 'DropDownList') {
      this.$log.debug(`Field '${this.field}' is not a dropdown.`);
      return [];
    }
    return fieldAnnotation.value;
  }

  showPicker() {
    const annotation = this._getFirstFieldAnnotation();
    this.CmsService.publish('show-picker', this.field, this.value, {
      configuration: annotation.pickerConfiguration,
      initialPath: annotation.pickerInitialPath,
      isRelativePath: annotation.isRelative,
      remembersLastVisited: annotation.pickerRemembersLastVisited,
      rootPath: annotation.pickerRootPath,
      selectableNodeTypes: annotation.pickerSelectableNodeTypes,
    });
  }

  _onPicked(field, path) {
    if (this.field === field) {
      this.getSetPath(path);
      this.$scope.$digest();
    }
  }

  getSetPath(...args) {
    return args.length ? (this.value = args[0]) : this.PathService.baseName(this.value);
  }

  _getType() {
    const fieldAnnotation = this._getFirstFieldAnnotation();
    if (fieldAnnotation) {
      const widgetType = WIDGET_TYPES[fieldAnnotation.type];
      if (widgetType) {
        return widgetType;
      }
    }

    const propertyDefinition = this._getPropertyDefinition();
    if (propertyDefinition && propertyDefinition.valueType === 'BOOLEAN') {
      return WIDGET_TYPES.CheckBox;
    }

    // default widget
    return WIDGET_TYPES.InputBox;
  }

  _getFirstFieldAnnotation() {
    const propertyDefinition = this._getPropertyDefinition();
    if (!propertyDefinition) {
      this.$log.warn(`Property definition for field '${this.field}' not found. Please check your ChannelInfo class.`);
      return undefined;
    }

    const fieldAnnotations = propertyDefinition.annotations;
    if (!fieldAnnotations || fieldAnnotations.length === 0) {
      return undefined;
    }
    if (fieldAnnotations.length > 1) {
      this.$log.warn(`Field '${this.field}' contains too many annotations. Please check your ChannelInfo class.`);
    }
    return fieldAnnotations[0];
  }

  _getPropertyDefinition() {
    return this.data.propertyDefinitions[this.field];
  }

  // Replace (subsequent) space and double quote characters with a hyphen. We could do more, but for now this is good enough
  _getQaClass() {
    return `qa-field-${this.field.replace(/(\s|")+/g, '-')}`;
  }
}
