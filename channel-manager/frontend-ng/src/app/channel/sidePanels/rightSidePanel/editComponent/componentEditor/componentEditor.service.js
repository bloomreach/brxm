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
const ERROR_MAP = {};

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

  open(componentId) {
    this.close();
    return this._loadComponent(componentId);
  }

  getPropertyGroups() {
    return this.propertyGroups;
  }

  _loadComponent(componentId) {
    this.componentId = componentId;

    return this.HstComponentService.getProperties(componentId, 'hippo-default')
      .then((properties) => {
        this.propertyGroups = this._groupProperties(properties);
        return this.propertyGroups;
      })
      .catch(response => this._onLoadFailure(response));
  }

  _groupProperties(response) {
    const defaultGroupTitle = this.$translate.instant('DEFAULT_PROPERTY_GROUP_TITLE');
    const groups = [];
    if (!response.properties[0]) {
      return groups;
    }
    let currentGroup = {};
    let currentGroupLabel = response.properties[0].groupLabel || defaultGroupTitle;
    let currentGroupFields = [];

    // TODO: do not add empty groups
    // TODO: use default group name if name is blank

    response.properties.forEach((property) => {
      if (property.hiddenInChannelManager) {
        return;
      }
      const thisGroupLabel = property.groupLabel || defaultGroupTitle;
      if (thisGroupLabel !== currentGroupLabel) {
        // store current group
        // TODO: deal with duplicate group names
        currentGroup.label = currentGroupLabel;
        currentGroup.fields = currentGroupFields;
        groups.push(currentGroup);
        // clean up for next group
        currentGroup = {};
        currentGroupFields = [];
        currentGroupLabel = thisGroupLabel;
      } 
      currentGroupFields.push(property);
    });

    // store last group
    currentGroup.label = currentGroupLabel;
    currentGroup.fields = currentGroupFields;
    groups.push(currentGroup);

    return groups;
  }

  _onLoadFailure(response) {
    this._clearData();

    let errorKey;
    let params = null;

    if (this._isErrorInfo(response.data)) {
      const errorInfo = response.data;
      errorKey = errorInfo.reason;
      params = this._extractErrorParams(errorInfo);

      if (errorInfo.params) {
        this.publicationState = errorInfo.params.publicationState;
      }
    } else if (response.status === 404) {
      errorKey = 'NOT_FOUND';
    } else {
      errorKey = 'UNAVAILABLE';
    }

    this.error = ERROR_MAP[errorKey];
    if (params) {
      this.error.messageParams = params;
    }
  }

  isDataDirty() {
    return this.dataDirty;
  }

  markDataDirty() {
    this.dataDirty = true;
  }

  close() {
    delete this.componentId;
    this._clearData();
    delete this.error;
    delete this.killed;
  }

  _clearData() {
    delete this.dataDirty;
  }
}

export default ComponentEditorService;
