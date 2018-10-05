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
    $translate,
    ChannelService,
    CmsService,
    ComponentEditor,
    EditComponentService,
    FeedbackService,
    HippoIframeService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ComponentEditor = ComponentEditor;
    this.EditComponentService = EditComponentService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
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
        const message = SAVE_ERRORS[error.data.error]
          ? this.$translate.instant(SAVE_ERRORS[error.data.error], error.data.parameterMap)
          : this.$translate.instant(SAVE_ERRORS.GENERAL_ERROR);

        this.FeedbackService.showError(message);
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
            this.EditComponentService.stopEditing();
          })
          .catch((error) => {
            const messageParameters = error.parameterMap;
            messageParameters.component = this.ComponentEditor.getComponentName();
            const message = DELETE_ERRORS[error.error]
              ? this.$translate.instant(DELETE_ERRORS[error.error], messageParameters)
              : this.$translate.instant(DELETE_ERRORS.GENERAL_ERROR, messageParameters);

            this.FeedbackService.showError(message);
            this.HippoIframeService.reload();
          });
      },
      )
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
      return this.ComponentEditor.confirmSaveOrDiscardChanges(this._isFormValid());
    }
    return this.$q.resolve();
  }
}

export default EditComponentMainCtrl;
