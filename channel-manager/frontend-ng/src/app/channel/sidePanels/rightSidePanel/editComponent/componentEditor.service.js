/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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

import MultiActionDialogCtrl from '../contentEditor/multiActionDialog/multiActionDialog.controller';
import multiActionDialogTemplate from '../contentEditor/multiActionDialog/multiActionDialog.html';

const TEMPLATE_PICKER = 'org.hippoecm.hst.core.component.template';

const isEmpty = str => str === undefined || str === null || str === '';

class ComponentEditorService {
  constructor(
    $q,
    $rootScope,
    $translate,
    ChannelService,
    CmsService,
    CommunicationService,
    ComponentVariantsService,
    ConfigService,
    ContainerService,
    DialogService,
    FeedbackService,
    HippoIframeService,
    HstComponentService,
    PageStructureService,
    TargetingService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.CommunicationService = CommunicationService;
    this.ComponentVariantsService = ComponentVariantsService;
    this.ConfigService = ConfigService;
    this.ContainerService = ContainerService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.HstComponentService = HstComponentService;
    this.PageStructureService = PageStructureService;
    this.TargetingService = TargetingService;

    this.killed = false;

    this._onPageChange = this._onPageChange.bind(this);
  }

  open(componentId, variantId) {
    const channel = this.ChannelService.getChannel();
    const page = this.PageStructureService.getPage();
    const component = page.getComponentById(componentId);

    this.close();
    this._offPageChange = this.$rootScope.$on('page:change', this._onPageChange);

    return this.load(channel, page, component, variantId);
  }

  reopen() {
    return this.load(this.channel, this.page, this.component);
  }

  load(channel, page, component, variantId) {
    this.request = this.HstComponentService.getProperties(component.getId(), variantId);
    this.request
      .then(response => this._onLoadSuccess(channel, component, page, response.properties))
      .then(() => this.updatePreview())
      .catch(() => this._onLoadFailure())
      .finally(() => { delete this.request; });

    return this.request;
  }

  getPropertyGroups() {
    return this.propertyGroups;
  }

  isReadOnly() {
    return this.component && this.component.getContainer().isDisabled();
  }

  openComponentPage() {
    if (!this.page) {
      return;
    }

    const pagePath = this.page.getMeta().getPathInfo();

    if (!this.ChannelService.matchesChannel(this.channel.id)) {
      this.ChannelService.initializeChannel(this.channel.id, this.channel.contextPath, this.channel.hostGroup)
        .then(() => this.HippoIframeService.initializePath(pagePath));
    } else {
      this.HippoIframeService.load(pagePath);
    }
  }

  getComponent() {
    return this.component;
  }

  isForeignPage() {
    const currentPage = this.PageStructureService.getPage();
    if (!currentPage || !this.page || !this.component) {
      return false;
    }

    return currentPage.getMeta().getPageId() !== this.page.getMeta().getPageId()
      && !currentPage.getComponentById(this.component.id);
  }

  _onLoadSuccess(channel, component, page, properties) {
    this.channel = channel;
    this.component = component;
    this.page = page;
    this.properties = this._normalizeProperties(properties);
    this.propertyGroups = this._groupProperties(this.properties);

    this.CommunicationService.selectComponent(component.getId());
    this.CmsService.reportUsageStatistic('CompConfigSidePanelOpened');
  }

  _onLoadFailure() {
    this.FeedbackService.showError('ERROR_UPDATE_COMPONENT');
    this.HippoIframeService.reload();
    return this.$q.reject();
  }

  _onPageChange(event, data) {
    if (!this.component) {
      return;
    }

    const page = this.PageStructureService.getPage();
    const component = page && page.getComponentById(this.component.getId());
    if (!component) {
      return;
    }

    if (data && data.initial) {
      this.CommunicationService.selectComponent(component.getId());
    }

    const currentContainer = this.component.getContainer();
    const changedContainer = component.getContainer();
    const isLockApplied = currentContainer.isDisabled() !== changedContainer.isDisabled();

    this.page = page;
    this.component = component;

    if (isLockApplied) {
      this.reopen();
    }
  }

  /**
   * Normalize properties data
   * @param {Array} properties
   */
  _normalizeProperties(properties) {
    if (!properties) {
      return [];
    }

    properties.forEach((property) => {
      const {
        defaultValue,
        displayValue,
        type,
        value,
      } = property;

      if (type === 'linkpicker') {
        property.pickerConfig = this._getPickerConfig(property);
        if (!isEmpty(defaultValue)) {
          property.defaultDisplayValue = value === defaultValue
            ? displayValue
            : defaultValue.substring(defaultValue.lastIndexOf('/') + 1);
        }
      }

      if (type === 'checkbox') {
        if (!isEmpty(value)) {
          property.value = this._booleanAsOnOff(value);
        }
        property.defaultValue = this._booleanAsOnOff(defaultValue);
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

  _booleanAsOnOff(boolean) {
    return ['true', '1', 'on'].includes(String(boolean).toLowerCase())
      ? 'on'
      : 'off';
  }

  _groupProperties(properties) {
    if (!properties[0]) {
      return [];
    }

    const defaultGroupLabel = this.$translate.instant('DEFAULT_PROPERTY_GROUP_LABEL');
    const groups = new Map();
    properties
      .filter(property => !property.hiddenInChannelManager)
      .forEach((property) => {
        this.setDefaultIfValueIsEmpty(property);

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

  setDefaultIfValueIsEmpty(property) {
    if (!property) {
      return;
    }

    const {
      defaultValue,
      defaultDisplayValue,
      displayValue,
      type,
      value,
    } = property;

    if (isEmpty(value) && !isEmpty(defaultValue)) {
      property.value = defaultValue;
    }

    if (type === 'linkpicker' && isEmpty(displayValue) && !isEmpty(defaultDisplayValue)) {
      property.displayValue = defaultDisplayValue;
    }
  }

  confirmDeleteComponent() {
    const translateParams = {
      component: this.component.getLabel(),
    };

    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_DELETE_COMPONENT_MESSAGE', translateParams))
      .ok(this.$translate.instant('DELETE'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  deleteComponent() {
    const container = this.component.getContainer();
    return this.HstComponentService.deleteComponent(container.getId(), this.component.getId())
      .then(() => this.close())
      .then(() => this.CmsService.reportUsageStatistic('CompConfigSidePanelDeleteComp'));
  }

  getComponentId() {
    return this.component && this.component.getId();
  }

  getComponentName() {
    if (this.component) {
      return this.component.getLabel();
    }
    if (this.error && this.error.messageParams) {
      return this.error.messageParams.displayName;
    }
    return undefined;
  }

  async updatePreview() {
    const page = this.PageStructureService.getPage();
    const component = page && page.getComponentById(this.component.getId());

    if (!component) {
      return;
    }

    await this.ContainerService.renderComponent(component, this.propertiesAsFormData());
  }

  async save() {
    let response;
    const componentId = this.component.getId();
    const formData = this.propertiesAsFormData();

    if (this.ConfigService.relevancePresent) {
      const variant = this.ComponentVariantsService.getCurrentVariant();
      const { persona, characteristics } = this.ComponentVariantsService.extractExpressions(variant);
      const variantId = variant.id;
      response = await this.TargetingService.updateVariant(
        componentId,
        formData,
        variantId,
        persona,
        characteristics,
      );
    } else {
      const variantId = this.component.getRenderVariant();
      response = await this.HstComponentService.setParameters(
        componentId,
        variantId,
        formData,
      );
    }

    this.CmsService.reportUsageStatistic('CompConfigSidePanelSave');
    return response;
  }

  propertiesAsFormData() {
    return this.properties.reduce((formData, { name, type, value }) => {
      if (type === 'datefield') {
        // cut off the time and time zone information from the value that the datefield returns
        formData[name] = value.substring(0, 10);
      } else {
        formData[name] = value;
      }
      return formData;
    }, {});
  }

  confirmDiscardChanges() {
    const translateParams = {
      component: this.component.getLabel(),
    };

    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant('CONFIRM_DISCARD_CHANGES_TO_COMPONENT', translateParams))
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  async discardChanges() {
    await this.reopen();

    if (!this.isForeignPage()) {
      await this.HippoIframeService.reload();
    }

    this.$rootScope.$emit('component:reset-current-variant');
  }

  close() {
    if (this._offPageChange) {
      this._offPageChange();
      delete this._offPageChange;
    }

    if (this.request) {
      this.request.cancel();
    }

    this._clearData();
    this.CommunicationService.selectComponent(null);
    delete this.error;
  }

  isKilled() {
    return this.killed;
  }

  kill() {
    this.killed = true;
  }

  /**
   * @param isValid whether the changes are valid
   *
   * Possible return values:
   * - resolved promise with value 'SAVE' when changes have been saved
   * - resolved promise with value 'DISCARD' when changes have been discarded
   * - rejected promise when user canceled
   */
  confirmSaveOrDiscardChanges(isValid) {
    return this._askSaveOrDiscardChanges()
      .then((action) => {
        switch (action) {
          case 'SAVE':
            if (isValid) {
              return this.$q.resolve(action); // let caller know that changes should saved
            }
            return this._alertFieldErrors()
              .then(() => this.$q.reject());
          case 'DISCARD':
            return this.discardChanges()
              .then(this.$q.resolve(action));
          default:
            return this.$q.resolve(action); // let caller know that changes should not be saved
        }
      });
  }

  _askSaveOrDiscardChanges() {
    const message = this.$translate.instant('SAVE_CHANGES_TO_COMPONENT', { componentLabel: this.component.getLabel() });
    const title = this.$translate.instant('SAVE_COMPONENT_CHANGES_TITLE');

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

  _alertFieldErrors() {
    const params = { componentLabel: this.component.getLabel() };
    const message = this.$translate.instant('FEEDBACK_CANNOT_SAVE_COMPONENT_WITH_INVALID_FIELD_VALUES', params);
    const ok = this.$translate.instant('OK');
    const alert = this.DialogService.alert()
      .textContent(message)
      .ok(ok);

    return this.DialogService.show(alert);
  }

  _clearData() {
    delete this.channel;
    delete this.component;
    delete this.killed;
    delete this.page;
    delete this.properties;
    delete this.propertyGroups;
  }
}

export default ComponentEditorService;
