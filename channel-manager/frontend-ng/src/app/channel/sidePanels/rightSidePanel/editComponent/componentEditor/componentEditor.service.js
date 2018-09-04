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

class ComponentEditorService {
  constructor($q, $translate, CmsService, DialogService, FeedbackService, HstComponentService) {
    'ngInject';

    this.$q = $q;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.DialogService = DialogService;
    this.FeedbackService = FeedbackService;
    this.HstComponentService = HstComponentService;
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
    this.properties = properties;

    console.log('Channel', this.channel);
    console.log('Component', this.component);
    console.log('Component properties', this.properties);
    console.log('Container', this.container);
    console.log('Page', this.page);

    this.propertyGroups = this._groupProperties(this.properties);
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
        if (property.name === 'org.hippoecm.hst.core.component.template') {
          property.groupLabel = 'org.hippoecm.hst.core.component.template';
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
      collapse: group[0] !== null && group[0] !== 'org.hippoecm.hst.core.component.template',
      fields: group[1],
      label: group[0],
    }));
  }

  _onLoadFailure(response) {
    this._clearData();
    console.log('TODO: implement ComponentEditorService._onLoadFailure');
    console.log(`Failure for: ${response}`);
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

  isDataDirty() {
    return this.dataDirty;
  }

  markDataDirty() {
    this.dataDirty = true;
  }

  close() {
    this._clearData();
    delete this.error;
  }

  _clearData() {
    delete this.channel;
    delete this.component;
    delete this.container;
    delete this.page;
    delete this.properties;
    delete this.propertyGroups;
    delete this.dataDirty;
  }
}

export default ComponentEditorService;
