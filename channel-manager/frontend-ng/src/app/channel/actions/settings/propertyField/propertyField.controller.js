/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

class PropertyFieldCtrl {
  constructor($log, $scope, ChannelService, CmsService, ConfigService, PathService) {
    'ngInject';

    this.$log = $log;
    this.$scope = $scope;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.PathService = PathService;
  }

  $onInit() {
    this.label = this.info.i18nResources[this.field] || this.field;
    this.help = this.info.i18nResources[`${this.field}.help`];

    this.definition = this.info.propertyDefinitions[this.field];
    if (!this.definition) {
      this.$log.warn(`Property definition for field '${this.field}' not found. Please check your ChannelInfo class.`);
    }

    this.annotation = this._getFirstFieldAnnotation();
    this.type = this._getType();
    this.qaClass = this._getQaClass();
    this.required = this.definition && this.definition.isRequired;

    if (this._isPickerField()) {
      this.CmsService.subscribe('path-picked', this._onPathPicked, this);
    }
  }

  $onDestroy() {
    if (this._isPickerField()) {
      this.CmsService.unsubscribe('path-picked', this._onPathPicked, this);
    }
  }

  getDropDownListValues() {
    if (!this.annotation || this.annotation.type !== 'DropDownList') {
      this.$log.debug(`Field '${this.field}' is not a dropdown.`);
      return [];
    }
    return this.annotation.value;
  }

  _isPickerField() {
    return this.type === 'ImageSetPath' || this.type === 'JcrPath';
  }

  getImageVariantPath() {
    const imageName = this.PathService.baseName(this.value);
    const cmsContextPath = this.ConfigService.cmsLocation.pathname;
    const variantName = this.annotation.previewVariant || 'hippogallery:thumbnail';
    const binaryPath = this.PathService.concatPaths(cmsContextPath, 'binaries', this.value, imageName, variantName);
    const cmsProtocol = this.ConfigService.cmsLocation.protocol;
    const cmsHost = this.ConfigService.cmsLocation.host;

    return `${cmsProtocol}//${cmsHost}${binaryPath}`;
  }

  showPathPicker() {
    this.CmsService.publish('show-path-picker', this.field, this.value, {
      configuration: this.annotation.pickerConfiguration,
      initialPath: this.annotation.pickerInitialPath,
      isRelativePath: this.annotation.isRelative,
      remembersLastVisited: this.annotation.pickerRemembersLastVisited,
      rootPath: this.annotation.pickerRootPath || this.ChannelService.getContentRootPath(),
      selectableNodeTypes: this.annotation.pickerSelectableNodeTypes,
    });
  }

  _onPathPicked(field, path) {
    if (this.field === field) {
      this.getSetPath(path);
      this.$scope.$digest();
    }
  }

  getSetPath(...args) {
    const path = args.length ? (this.value = args[0]) : this.PathService.baseName(this.value);
    return path;
  }

  _getType() {
    if (this.annotation) {
      const widgetType = WIDGET_TYPES[this.annotation.type];
      if (widgetType) {
        return widgetType;
      }
    }

    if (this.definition && this.definition.valueType === 'BOOLEAN') {
      return WIDGET_TYPES.CheckBox;
    }

    // default widget
    return WIDGET_TYPES.InputBox;
  }

  _getFirstFieldAnnotation() {
    const fieldAnnotations = this.definition && this.definition.annotations;
    if (!fieldAnnotations || fieldAnnotations.length === 0) {
      return undefined;
    }
    if (fieldAnnotations.length > 1) {
      this.$log.warn(`Field '${this.field}' contains too many annotations. Please check your ChannelInfo class.`);
    }
    return fieldAnnotations[0];
  }

  // Replace (subsequent) space and double quote characters with a hyphen. We could do more, but for now this is good enough
  _getQaClass() {
    return `qa-field-${this.field.replace(/(\s|")+/g, '-')}`;
  }
}

export default PropertyFieldCtrl;
