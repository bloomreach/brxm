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
    $q,
    $scope,
    $translate,
    CmsService,
    ChannelService,
    ConfigService,
    ComponentEditor,
    ContentEditor,
    EditComponentService,
    EditContentService,
    HippoIframeService,
    ProjectService,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$scope = $scope;
    this.CmsService = CmsService;
    this.ChannelService = ChannelService;
    this.ConfigService = ConfigService;
    this.ComponentEditor = ComponentEditor;
    this.ContentEditor = ContentEditor;
    this.EditComponentService = EditComponentService;
    this.EditContentService = EditContentService;
    this.HippoIframeService = HippoIframeService;
    this.ProjectService = ProjectService;
    this.RightSidePanelService = RightSidePanelService;

    this.closing = false;
  }

  $onInit() {
    this.$scope.$watch('$ctrl.loading', (newValue, oldValue) => {
      if (newValue === oldValue) {
        return;
      }

      if (newValue) {
        this.RightSidePanelService.startLoading();
      } else {
        this.RightSidePanelService.stopLoading();
      }
    });
  }

  discard() {
    console.log('TODO: implement EditComponentMainCtrl.discard');
  }

  save() {
    this.HippoIframeService.reload();
    this.CmsService.reportUsageStatistic('CMSChannelsSaveComponent');
  }

  close() {
    this.closing = true;
    this.EditComponentService.stopEditing();
  }

  // switchEditor() {
  //   this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'edit');
  //   this.ContentEditor.close();
  //   this.EditContentService.stopEditing();
  // }

  deleteComponent() {
    return this.ComponentEditor.confirmDeleteComponent()
      .then(() => {
        this.ComponentEditor.deleteComponent()
          .then(() => {
            this.ChannelService.recordOwnChange();
            this.HippoIframeService.reload();
            this.close();
          })
          .catch((errorResponse) => {
            // delete action failed: show toast message? go to component locked mode?
            // what if someone else deleted the component already: no problem!
            // TODO: see PageStructureService.removeComponentById() for an example to deal with the error response
            console.log(`TODO: implement dealing with the delete component error response: ${errorResponse}`);
          });
      },
      )
      .catch(() => {
        // user cancelled the delete
        this.closing = false;
        return this.$q.reject();
      });
  }

  isSaveAllowed() {
    return false;
  }

  uiCanExit() {
    return this._confirmExit()
      .then(() => this.ContentEditor.discardChanges()
        .catch(() => {
          // ignore errors of discardChanges: if it fails (e.g. because an admin unlocked the document)
          // the editor should still be closed.
        })
        .finally(() => this.ContentEditor.close()),
      )
      .catch(() => {
        // user cancelled the exit
        this.closing = false;
        return this.$q.reject();
      });
  }

  _confirmExit() {
    if (this.closing) {
      return this.ContentEditor.confirmDiscardChanges('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE');
    }
    return this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_ON_BLUR_MESSAGE')
      .then((action) => {
        if (action === 'SAVE') {
          this.HippoIframeService.reload();
        }
      });
  }
}

export default EditComponentMainCtrl;
