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

class EditContentMainCtrl {
  constructor(
    $q,
    $scope,
    $translate,
    CmsService,
    ConfigService,
    ContentEditor,
    DialogService,
    EditContentService,
    HippoIframeService,
    ProjectService,
    RightSidePanelService,
  ) {
    'ngInject';

    this.$q = $q;
    this.$scope = $scope;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.ContentEditor = ContentEditor;
    this.DialogService = DialogService;
    this.EditContentService = EditContentService;
    this.HippoIframeService = HippoIframeService;
    this.ProjectService = ProjectService;
    this.RightSidePanelService = RightSidePanelService;
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

  notAllFieldsShown() {
    return this.ContentEditor.isEditing() && !this.ContentEditor.getDocumentType().allFieldsIncluded;
  }

  save() {
    return this.ContentEditor.save()
      .then(() => {
        this.form.$setPristine();
        this.HippoIframeService.reload();
        this.CmsService.reportUsageStatistic('CMSChannelsSaveDocument');
      })
      .finally(this.switchLoading());
  }

  switchLoading() {
    this.loading = true;

    return () => { this.loading = false; };
  }

  _confirmDiscardChanges(messageKey) {
    if (this.ContentEditor.isPristine()) {
      return this.$q.resolve();
    }

    const translateParams = { documentName: this.ContentEditor.getDocumentDisplayName() };
    const confirm = this.DialogService.confirm()
      .textContent(this.$translate.instant(messageKey, translateParams))
      .ok(this.$translate.instant('DISCARD'))
      .cancel(this.$translate.instant('CANCEL'));

    return this.DialogService.show(confirm);
  }

  discard() {
    return this._confirmDiscardChanges('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE')
      .then(() => {
        this.form.$setPristine();
        this.ContentEditor.discardChanges()
          .then(this.EditContentService._loadDocument(this.ContentEditor.getDocumentId()));
      });
  }

  publish() {
    this.CmsService.reportUsageStatistic('VisualEditingPublishButton');
    return this.ContentEditor.confirmPublication()
      .then(() => this._doPublish()
        .finally(this.switchLoading()));
  }

  _doPublish() {
    return this.ContentEditor.isDocumentDirty()
      ? this.save().then(() => this.ContentEditor.publish())
      : this.ContentEditor.publish();
  }

  isEditing() {
    return this.ContentEditor.isEditing();
  }

  isDocumentDirty() {
    return this.ContentEditor.isDocumentDirty();
  }

  isPublishAllowed() {
    return this.ContentEditor.isPublishAllowed() && !this.isDocumentDirty();
  }

  isSaveAllowed() {
    return this.isEditing() && this.isDocumentDirty() && this.form.$valid;
  }

  switchEditor() {
    this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'edit');
    this.ContentEditor.close();
    this.EditContentService.stopEditing();
  }

  uiCanExit() {
    return this._confirmExit()
      .then(() => this.ContentEditor.discardChanges()
        .catch(() => {
          // ignore errors of discardChanges: if it fails (e.g. because an admin unlocked the document)
          // the editor should still be closed.
        })
        .finally(() => this.ContentEditor.close()));
  }

  _confirmExit() {
    return this.ContentEditor.confirmSaveOrDiscardChanges('SAVE_CHANGES_TO_DOCUMENT')
      .then((action) => {
        if (action === 'SAVE') {
          this.HippoIframeService.reload();
        }
      });
  }
}

export default EditContentMainCtrl;
