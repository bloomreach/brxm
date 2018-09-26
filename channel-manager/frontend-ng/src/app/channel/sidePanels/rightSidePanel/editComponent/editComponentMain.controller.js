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

class EditComponentMainCtrl {
  constructor(
    $log,
    $q,
    ChannelService,
    CmsService,
    ComponentEditor,
    EditComponentService,
    HippoIframeService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$q = $q;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ComponentEditor = ComponentEditor;
    this.EditComponentService = EditComponentService;
    this.HippoIframeService = HippoIframeService;
  }

  getPropertyGroups() {
    return this.ComponentEditor.getPropertyGroups();
  }

  hasProperties() {
    return this.ComponentEditor.getPropertyGroups().length > 0;
  }

  discard() {
    this.ComponentEditor.confirmDiscardChanges()
      .then(() => this.ComponentEditor.discardChanges());
  }

  save() {
    this.ComponentEditor.save()
      .then(() => this.form.$setPristine());

    this.CmsService.reportUsageStatistic('CMSChannelsSaveComponent');
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
          .catch((errorResponse) => {
            // delete action failed: show toast message? go to component locked mode?
            // what if someone else deleted the component already: no problem!
            // TODO: see PageStructureService.removeComponentById() for an example to deal with the error response
            console.log(`TODO: implement dealing with the delete component error response: ${errorResponse}`);
          });
      },
      )
      .catch(() => this.$q.reject()); // user cancelled the delete
  }

  isSaveAllowed() {
    return this._isFormDirty() && this._isFormValid();
  }

  _isFormDirty() {
    return this.form && this.form.$dirty;
  }

  _isFormValid() {
    return this.form && this.form.$valid;
  }

  uiCanExit() {
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
      return this.ComponentEditor.confirmSaveOrDiscardChanges();
    }
    return this.$q.resolve();
  }
}

export default EditComponentMainCtrl;
