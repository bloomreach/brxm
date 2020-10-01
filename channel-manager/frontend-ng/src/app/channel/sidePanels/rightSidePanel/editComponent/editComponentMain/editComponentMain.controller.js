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

const DELETE_ERRORS = {
  GENERAL_ERROR: 'ERROR_DELETE_COMPONENT',
  ITEM_ALREADY_LOCKED: 'ERROR_DELETE_COMPONENT_ITEM_ALREADY_LOCKED',
};
const SAVE_ERRORS = {
  GENERAL_ERROR: 'ERROR_UPDATE_COMPONENT',
  ITEM_ALREADY_LOCKED: 'ERROR_UPDATE_COMPONENT_ITEM_ALREADY_LOCKED',
};

class EditComponentMainCtrl {
  constructor(
    $log,
    $q,
    $rootScope,
    $scope,
    ChannelService,
    CmsService,
    ComponentEditor,
    ConfigService,
    EditComponentService,
    FeedbackService,
    HippoIframeService,
    ComponentVariantsService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$rootScope = $rootScope;
    this.$scope = $scope;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ComponentEditor = ComponentEditor;
    this.EditComponentService = EditComponentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.ConfigService = ConfigService;
    this.ComponentVariantsService = ComponentVariantsService;

    this._onComponentMoved = this._onComponentMoved.bind(this);
    this._onDocumentSelect = this._onDocumentSelect.bind(this);
    this._onPageChange = this._onPageChange.bind(this);
  }

  $onInit() {
    this._offComponentMoved = this.$rootScope.$on('component:moved', this._onComponentMoved);
    this._offDocumentSelect = this.$rootScope.$on('iframe:document:select', this._onDocumentSelect);
    this._offPageChange = this.$rootScope.$on('page:change', this._onPageChange);
  }

  $onDestroy() {
    this._offComponentMoved();
    this._offDocumentSelect();
    this._offPageChange();
  }

  get variantsVisible() {
    return this.ConfigService.relevancePresent;
  }

  onVariantUpdated(variant) {
    this.ComponentVariantsService.setCurrentVariant(variant);
    this.form.$setDirty();
  }

  onVariantInitiated(variant) {
    this.ComponentVariantsService.setCurrentVariant(variant);
  }

  _onComponentMoved() {
    this.ComponentEditor.updatePreview();
  }

  _onDocumentSelect(event, data) {
    if (this.ComponentEditor.getComponentId() === data.containerItemId) {
      event.preventDefault();
      this.$scope.$broadcast('edit-component:select-document', data.parameterName);
    }
  }

  _onPageChange(event, data) {
    if (!data || !data.initial) {
      return;
    }

    this.ComponentEditor.updatePreview();
  }

  getPropertyGroups() {
    return this.ComponentEditor.getPropertyGroups();
  }

  hasNoProperties() {
    const propertyGroups = this.getPropertyGroups();
    return angular.isArray(propertyGroups) && propertyGroups.length === 0;
  }

  isReadOnly() {
    return this.ComponentEditor.isReadOnly();
  }

  isDeleteDisabled() {
    return this.isReadOnly() || !this.EditComponentService.isReadyForUser();
  }

  discard() {
    this.ComponentEditor.confirmDiscardChanges()
      .then(() => this.ComponentEditor.discardChanges())
      .then(() => this.form.$setPristine());
  }

  save() {
    return this.ComponentEditor.save()
      .then(() => this.form.$setPristine())
      .then(() => this.CmsService.reportUsageStatistic('CMSChannelsSaveComponent'))
      .catch((error) => {
        this.FeedbackService.showError(
          SAVE_ERRORS[error.data.error] || SAVE_ERRORS.GENERAL_ERROR,
          error.data.parameterMap,
        );
        this.HippoIframeService.reload();
        if (error.message && error.message.startsWith('javax.jcr.ItemNotFoundException')) {
          this.EditComponentService.killEditor();
        }
      });
  }

  deleteComponent() {
    return this.ComponentEditor.confirmDeleteComponent()
      .then(() => {
        this.ComponentEditor.deleteComponent()
          .then(() => this.ChannelService.checkChanges())
          .then(() => {
            this.HippoIframeService.reload();
            this.EditComponentService.killEditor();
          })
          .catch((error) => {
            this.FeedbackService.showError(
              DELETE_ERRORS[error.error] || DELETE_ERRORS.GENERAL_ERROR,
              Object.assign(error.parameterMap, { component: this.ComponentEditor.getComponentName() }),
            );
            this.HippoIframeService.reload();
          });
      })
      .catch(() => this.$q.reject()); // user cancelled the delete
  }

  isDiscardAllowed() {
    return this._isFormDirty() && !this.isReadOnly();
  }

  isSaveAllowed() {
    return this._isFormDirty() && this._isFormValid() && !this.isReadOnly();
  }

  _isFormDirty() {
    return this.form && this.form.$dirty;
  }

  _isFormValid() {
    return this.form && this.form.$valid;
  }

  uiCanExit() {
    if (this.ComponentEditor.isKilled() || this.isReadOnly()) {
      return true;
    }
    return this._saveOrDiscardChanges()
      .then(() => this.ComponentEditor.close())
      .catch((e) => {
        if (e) {
          this.$log.error('An error occurred while closing the ComponentEditor ->', e);
        } else {
          // the user has cancelled the confirmation dialog
        }
        return this.$q.reject();
      });
  }

  _saveOrDiscardChanges() {
    if (this._isFormDirty()) {
      return this.ComponentEditor.confirmSaveOrDiscardChanges(this._isFormValid())
        .then((action) => {
          if (action === 'SAVE') {
            return this.save();
          }
          return this.$q.resolve();
        })
        .catch(this.$q.reject);
    }
    return this.$q.resolve();
  }
}

export default EditComponentMainCtrl;
