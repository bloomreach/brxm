/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import MultiActionDialogCtrl from '../../contentEditor/multiActionDialog/multiActionDialog.controller';
import multiActionDialogTemplate from '../../contentEditor/multiActionDialog/multiActionDialog.html';

const TEMPLATE_PICKER = 'org.hippoecm.hst.core.component.template';

class ComponentEditorService {
  constructor($q, $translate, DialogService, FeedbackService, HstComponentService, PageStructureService) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.HstComponentService = HstComponentService;
    this.PageStructureService = PageStructureService;
  }

  open({ channel, component, container, page }) {
    this.close();

    return this.HstComponentService.getProperties(component.id, component.variant)
      .then(response => this._onLoadSuccess(channel, component, container, page, response.properties))
      .catch(response => this._onLoadFailure(response));
  }

  getPropertyGroups() {
    return this.propertyGroups;
  }

  _onLoadSuccess(channel, component, container, page, properties) {
    this.channel = channel;
    this.component = component;
    this.container = container;
    this.page = page;
    this.properties = this._normalizeProperties(properties);

    console.log('Channel', this.channel);
    console.log('Component', this.component);
    console.log('Component properties', this.properties);
    console.log('Container', this.container);
    console.log('Page', this.page);

    this.propertyGroups = this._groupProperties(this.properties);
  }

  _onLoadFailure(response) {
    this._clearData();
    console.log('TODO: implement ComponentEditorService._onLoadFailure');
    console.log(`Failure for: ${response}`);
  }

  /**
   * Normalize properties data
   * @param {Array} properties
   */
  _normalizeProperties(properties) {
    properties.forEach((property) => {
      if (property.type === 'linkpicker') {
        property.pickerConfig = this._getPickerConfig(property);
      }
    });

    return properties;
  }

  /**
   * Extract config data from the property entity
   * @param {Object} property Component property entity
   */
  _getPickerConfig(property) {
    return {
      linkpicker: {
        configuration: property.pickerConfiguration,
        remembersLastVisited: property.pickerRemembersLastVisited,
        initialPath: property.pickerInitialPath,
        isRelativePath: property.pickerPathIsRelative,
        rootPath: property.pickerRootPath,
        selectableNodeTypes: property.pickerSelectableNodeTypes,
        isPathPicker: true,
      },
    };
  }

  _groupProperties(properties) {
    if (!properties[0]) {
      return [];
    }

    const defaultGroupLabel = this.$translate.instant('DEFAULT_PROPERTY_GROUP_LABEL');
    const groups = new Map();
    properties
      .filter(property => !property.hiddenInChannelManager)
      .map((property) => {
        if (property.value === null && property.defaultValue) {
          property.value = property.defaultValue;
        }
        return property;
      })
      .forEach((property) => {
        if (property.name === TEMPLATE_PICKER) {
          property.groupLabel = TEMPLATE_PICKER;
        }

        const groupLabel = property.groupLabel === ''
          ? defaultGroupLabel
          : property.groupLabel;

        if (groups.has(groupLabel)) {
          groups.get(groupLabel).push(property);
        } else {
          groups.set(groupLabel, [property]);
        }
      });

    return Array.from(groups).map(group => ({
      collapse: group[0] !== null && group[0] !== TEMPLATE_PICKER,
      default: group[0] === defaultGroupLabel,
      fields: group[1],
      label: group[0],
    }));
  }

  confirmDeleteComponent() {
    const translateParams = {
      component: this.component.label,
    };

    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_DELETE_COMPONENT_MESSAGE', translateParams))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  deleteComponent() {
    return this.HstComponentService.deleteComponent(this.container.id, this.component.id)
      .then(() => this.close());
  }

  getComponentName() {
    if (this.component) {
      return this.component.label;
    }
    if (this.error && this.error.messageParams) {
      return this.error.messageParams.displayName;
    }
    return undefined;
  }

  get dirty() {
    return !!this.propertiesDirty;
  }

  set dirty(dirty) {
    this.propertiesDirty = dirty;
  }

  valueChanged() {
    this.PageStructureService.renderComponent(this.component.id, this._propertiesAsFormData());
  }

  save() {
    return this.HstComponentService.setParameters(this.component.id, this.component.variant, this._propertiesAsFormData())
      .then(() => delete this.propertiesDirty);
  }

  _propertiesAsFormData() {
    return this.properties.reduce((formData, property) => {
      formData[property.name] = property.value;
      return formData;
    }, {});
  }

  close() {
    this._clearData();
    delete this.error;
  }

  /**
   * Possible return values:
   * - resolved promise with value 'SAVE' when changes have been saved
   * - resolved promise with value 'DISCARD' when changes have been discarded
   * - rejected promise when user canceled
   */
  confirmSaveOrDiscardChanges() {
    return this._askSaveOrDiscardChanges()
      .then((action) => {
        switch (action) {
          case 'SAVE':
            return this.save()
              .then(() => action); // let caller know that changes have been saved
          case 'DISCARD':
            this.PageStructureService.renderComponent(this.component.id);
            return this.$q.resolve(action);
          default:
            return this.$q.resolve(action); // let caller know that changes have not been saved
        }
      });
  }

  _askSaveOrDiscardChanges() {
    if (!this.propertiesDirty) {
      return this.$q.resolve();
    }

    const message = this.$translate.instant('SAVE_CHANGES_TO_COMPONENT', { componentLabel: this.component.label });
    const title = this.$translate.instant('SAVE_CHANGES_TITLE');

    return this.DialogService.show({
      template: multiActionDialogTemplate,
      controller: MultiActionDialogCtrl,
      controllerAs: '$ctrl',
      locals: {
        title,
        message,
        actions: ['DISCARD', 'SAVE'],
      },
      bindToController: true,
    });
  }

  _clearData() {
    delete this.channel;
    delete this.component;
    delete this.container;
    delete this.page;
    delete this.properties;
    delete this.propertyGroups;
    delete this.propertiesDirty;
  }
}

export default ComponentEditorService;
