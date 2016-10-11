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

const BINARIES_PATH = 'binaries';
const DEFAULT_IMAGE_VARIANT = 'hippogallery:thumbnail';

class ChannelPropertyCtrl {
  constructor($log, $scope, ChannelService, CmsService, ConfigService, PathService) {
    'ngInject';

    this.$log = $log;
    this.$scope = $scope;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.PathService = PathService;

    this.label = this.data.i18nResources[this.field] || this.field;
    this.help = this.data.i18nResources[`${this.field}.help`];

    this.definition = this.data.propertyDefinitions[this.field];
    if (!this.definition) {
      $log.warn(`Property definition for field '${this.field}' not found. Please check your ChannelInfo class.`);
    }

    this.annotation = this._getFirstFieldAnnotation();
    this.type = this._getType();
    this.qaClass = this._getQaClass();
    this.readOnly = this.data.lockedBy && this.data.lockedBy !== ConfigService.cmsUser;
    this.required = this.definition && this.definition.isRequired;

    if (this._isPickerField()) {
      this.CmsService.subscribe('picked', this._onPicked, this);
      this.$scope.$on('$destroy', () => this.CmsService.unsubscribe('picked', this._onPicked, this));
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
    const cmsProtocol = this.ConfigService.cmsLocation.protocol;
    const cmsHost = this.ConfigService.cmsLocation.host;

    const imageName = this.PathService.baseName(this.value);
    const variantName = this.annotation.previewVariant || DEFAULT_IMAGE_VARIANT;
    const cmsContextPath = this.ConfigService.cmsLocation.pathname;
    const binaryPath = this.PathService.concatPaths(cmsContextPath, BINARIES_PATH, this.value, imageName, variantName);

    return `${cmsProtocol}//${cmsHost}${binaryPath}`;
  }

  showPicker() {
    this.CmsService.publish('show-picker', this.field, this.value, {
      configuration: this.annotation.pickerConfiguration,
      initialPath: this.annotation.pickerInitialPath,
      isRelativePath: this.annotation.isRelative,
      remembersLastVisited: this.annotation.pickerRemembersLastVisited,
      rootPath: this.annotation.pickerRootPath || this.ChannelService.getContentRootPath(),
      selectableNodeTypes: this.annotation.pickerSelectableNodeTypes,
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

export default ChannelPropertyCtrl;
