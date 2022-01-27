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

import { debounce } from 'throttle-debounce';

class EditContentMainCtrl {
  constructor(
    $interval,
    $log,
    $q,
    $scope,
    $rootScope,
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

    this.$interval = this.$interval;
    this.$log = $log;
    this.$q = $q;
    this.$scope = $scope;
    this.$rootScope = $rootScope;
    this.$translate = $translate;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.ContentEditor = ContentEditor;
    this.DialogService = DialogService;
    this.EditContentService = EditContentService;
    this.HippoIframeService = HippoIframeService;
    this.ProjectService = ProjectService;
    this.RightSidePanelService = RightSidePanelService;

    this._onFieldChange = this._onFieldChange.bind(this);
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

    this._offFieldChange = this.$rootScope.$on('field:change', this._onFieldChange);

    this._reloadPage = debounce(250, true, () => {
      this.HippoIframeService.reload();
    });

    // save draft if editor is dirty every 30 seconds
    this._saveDraftIfDirtyPromise = this.$interval(() => {
      this.ContentEditor.saveDraftIfDirty();
    }, 30 * 1000);

  }

  $onDestroy() {
    this._offFieldChange();
    this._reloadPage.cancel();
  }

  _onFieldChange() {
    this._reloadPage();
  }

  notAllFieldsShown() {
    return this.ContentEditor.isEditing() && !this.ContentEditor.getDocumentType().allFieldsIncluded;
  }

  getDocumentErrorMessages() {
    return this.ContentEditor.getDocumentErrorMessages();
  }

  save() {
    const stopLoading = this.startLoading();

    return this.ContentEditor.save()
      .then(() => {
        this.form.$setPristine();
        this.HippoIframeService.reload();
        this.CmsService.reportUsageStatistic('CMSChannelsSaveDocument');
      })
      .catch(() => this._focusFirstInvalidField())
      .finally(stopLoading);
  }

  _focusFirstInvalidField() {
    // stop previous watch if it still exists
    if (this.stopServerErrorWatch) {
      this.stopServerErrorWatch();
    }
    // create new watch for server errors, and focus the first field with such an error
    this.stopServerErrorWatch = this.$scope.$watch('$ctrl.form.$error.server', (error) => {
      if (error && error.length > 0) {
        error[0].$$element.focus();
        this.stopServerErrorWatch();
        delete this.stopServerErrorWatch;
      }
    });

    return this.$q.reject();
  }

  startLoading() {
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
    let messageKey;

    if (this.isRetainable()) {
      messageKey = this.EditContentService.isEditingXPage()
        ? 'CONFIRM_DISCARD_XPAGE_UNSAVED_RETAINABLE_DRAFT_CHANGES_MESSAGE'
        : 'CONFIRM_DISCARD_DOCUMENT_UNSAVED_RETAINABLE_DRAFT_CHANGES_MESSAGE';
    } else {
      messageKey = this.EditContentService.isEditingXPage()
        ? 'CONFIRM_DISCARD_XPAGE_UNSAVED_CHANGES_MESSAGE'
        : 'CONFIRM_DISCARD_DOCUMENT_UNSAVED_CHANGES_MESSAGE';
    }

    return this._confirmDiscardChanges(messageKey)
      .then(() => {
        this.form.$setPristine();
        this.ContentEditor.discardChanges()
          .then(() => this.EditContentService._loadDocument(this.ContentEditor.getDocumentId()))
          .then(() => this.HippoIframeService.reload())
      });
  }

  publish() {
    this.CmsService.reportUsageStatistic('VisualEditingPublishButton');

    const stopLoading = this.startLoading();
    return this._doPublish().finally(stopLoading);
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
    return !this.EditContentService.isEditingXPage()
      && this.ContentEditor.isPublishAllowed()
      && !this.isDocumentDirty();
  }

  isRetainable() {
    return this.ContentEditor.isRetainable();
  }

  isSaveAllowed() {
    return this.isEditing() && (this.isDocumentDirty() || this.isRetainable()) && this.form.$valid;
  }

  isKeepDraftShown() {
    return this.ContentEditor.isKeepDraftAllowed();
  }

  keepDraft() {
    this.ContentEditor.keepDraft().then(
      () => this.ContentEditor.open(this.ContentEditor.getDocumentId()),
    );
  }

  isKeepDraftEnabled() {
    return this.isRetainable();
  }

  switchEditor() {
    this.CmsService.publish('open-content', this.ContentEditor.getDocumentId(), 'edit');
    this.ContentEditor.close();
    this.EditContentService.stopEditing();
  }

  async uiCanExit() {
    if (this.isRetainable()) {
      this.ContentEditor.close();
      return;
    }

    const isEditingXPage = this.EditContentService.isEditingXPage();
    const titleKey = isEditingXPage ? 'SAVE_XPAGE_CHANGES_TITLE' : 'SAVE_DOCUMENT_CHANGES_TITLE';
    const messageKey = isEditingXPage ? 'SAVE_CHANGES_TO_XPAGE' : 'SAVE_CHANGES_TO_DOCUMENT';

    await this.ContentEditor.confirmSaveOrDiscardChanges(messageKey, {}, titleKey);

    try {
      await this.ContentEditor.discardChanges();
    } catch (e) {
      this.$log.error('Failed to discard changes while exiting the component', e);
      // ignore errors of discardChanges: if it fails (e.g. because an admin unlocked the document)
      // the editor should still be closed.
    } finally {
      this.HippoIframeService.reload();
      this.ContentEditor.close();
    }
  }

}

export default EditContentMainCtrl;
