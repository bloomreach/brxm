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

const WIDGET_TYPES = {
  JcrPath: 'JcrPath',
  ImageSetPath: 'ImageSetPath',
  DropDownList: 'DropDownList',
  CheckBox: 'CheckBox',
  InputBox: 'InputBox',
};

class PropertyFieldCtrl {
  constructor($log, ChannelService, ConfigService, PathService, PickerService) {
    'ngInject';

    this.$log = $log;
    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.PathService = PathService;
    this.PickerService = PickerService;
  }

  $onInit() {
    this.label = this.info.i18nResources[this.field] || this.field;
    this.hint = this.info.i18nResources[`${this.field}.hint`];

    this.definition = this.info.propertyDefinitions[this.field];
    if (!this.definition) {
      this.$log.warn(`Property definition for field '${this.field}' not found. Please check your ChannelInfo class.`);
    }

    this.annotation = this._getFirstFieldAnnotation();
    this.type = this._getType();
    this.qaClass = this._getQaClass();
    this.required = this.definition && this.definition.isRequired;

    this._onLoadDropDownValues();
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
    const pickerConfig = {
      configuration: this.annotation.pickerConfiguration,
      initialPath: this.annotation.pickerInitialPath,
      isRelativePath: this.annotation.isRelative,
      remembersLastVisited: this.annotation.pickerRemembersLastVisited,
      rootPath: this.annotation.pickerRootPath || this.ChannelService.getContentRootPath(),
      selectableNodeTypes: this.annotation.pickerSelectableNodeTypes,
    };

    return this.PickerService.pickPath(pickerConfig, this.value)
      .then(({ path }) => {
        this.getSetPath(path);
      });
  }

  getSetPath(...args) {
    if (args.length) {
      [this.value] = args;
      return this.value;
    }
    return this.PathService.baseName(this.value);
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

  // Replace (subsequent) space and double quote characters with a hyphen.
  // We could do more, but for now this is good enough.
  _getQaClass() {
    return `qa-field-${this.field.replace(/(\s|")+/g, '-')}`;
  }

  _onLoadDropDownValues() {
    this.dropDownValues = [];
    if (this.annotation && this.annotation.value && this.annotation.type === 'DropDownList') {
      this.annotation.value.forEach((value) => {
        const key1 = `${this.field}/${value}`;
        const key2 = `${this.field}#${value}`;
        const key3 = `${this.field}.${value}`;
        const displayName = this.info.i18nResources[key1]
          || this.info.i18nResources[key2]
          || this.info.i18nResources[key3]
          || value;
        this.dropDownValues.push({ value, displayName });
      });
    }
  }
}

export default PropertyFieldCtrl;
