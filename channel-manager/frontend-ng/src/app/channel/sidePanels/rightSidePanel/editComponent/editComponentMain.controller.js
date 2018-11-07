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
    $scope,
    ChannelService,
    CmsService,
    ComponentEditor,
    ContainerService,
    EditComponentService,
    FeedbackService,
    HippoIframeService,
    OverlayService,
    RenderingService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$scope = $scope;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ComponentEditor = ComponentEditor;
    this.ContainerService = ContainerService;
    this.EditComponentService = EditComponentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.OverlayService = OverlayService;
    this.RenderingService = RenderingService;
  }

  $onInit() {
    this._overrideSelectDocumentHandler();
    this._offComponentMoved = this.ContainerService.onComponentMoved(() => this.ComponentEditor.updatePreview());
    this._offOverlayCreated = this.RenderingService.onOverlayCreated(() => this.ComponentEditor.updatePreview());
  }

  $onDestroy() {
    this._restoreSelectDocumentHandler();
    this._offComponentMoved();
    this._offOverlayCreated();
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
          .then(() => {
            this.ChannelService.recordOwnChange();
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

  _overrideSelectDocumentHandler() {
    this.defaultSelectDocumentHandler = this.OverlayService.onSelectDocument(this._onSelectDocument.bind(this));
  }

  _restoreSelectDocumentHandler() {
    this.OverlayService.onSelectDocument(this.defaultSelectDocumentHandler);
  }

  _onSelectDocument(component, parameterName, parameterValue, pickerConfig, parameterBasePath) {
    if (this.ComponentEditor.getComponentId() === component.getId()) {
      this.$scope.$broadcast('edit-component:select-document', parameterName);
    } else {
      this.defaultSelectDocumentHandler(component, parameterName, parameterValue, pickerConfig, parameterBasePath);
    }
  }
}

export default EditComponentMainCtrl;
