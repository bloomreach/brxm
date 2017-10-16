/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import MultiActionDialogCtrl from '../multiActionDialog/multiActionDialog.controller';
import multiActionDialogTemplate from '../multiActionDialog/multiActionDialog.html';

class editContentController {
  constructor(
    $scope,
    $element,
    $timeout,
    $translate,
    $q,
    SidePanelService,
    ChannelService,
    CmsService,
    ContentService,
    DialogService,
    HippoIframeService,
    FeedbackService,
    localStorageService,
    FieldService,
  ) {
    'ngInject';

    this.$scope = $scope;
    this.$element = $element;
    this.$timeout = $timeout;
    this.$translate = $translate;
    this.$q = $q;

    this.SidePanelService = SidePanelService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ContentService = ContentService;
    this.DialogService = DialogService;
    this.HippoIframeService = HippoIframeService;
    this.FeedbackService = FeedbackService;
    this.localStorageService = localStorageService;
    this.FieldService = FieldService;

    this.defaultTitle = $translate.instant('EDIT_CONTENT');
    this.closeLabel = $translate.instant('CLOSE');
    this.cancelLabel = $translate.instant('CANCEL');
    this.deleteDraftOnClose = true;

    CmsService.subscribe('kill-editor', (documentId) => {
      if (this.documentId === documentId) {
        this.deleteDraftOnClose = false;
        this.close();
      }
    });
  }

  get title() {
    return this.$translate.instant('EDIT_DOCUMENT', this.doc);
  }

  setFullWidth(state) {
    this.isFullWidth = state;
    this.onFullWidth({ state });
  }

  openContentEditor(mode, isPromptUnsavedChanges = true) {
    this.deleteDraftOnClose = false;

    if (!isPromptUnsavedChanges) {
      this._closePanelAndOpenContent(mode);
      return;
    }
    const messageKey = mode === 'view'
      ? 'SAVE_CHANGES_ON_PUBLISH_MESSAGE'
      : 'SAVE_CHANGES_ON_SWITCH_TO_CONTENT_EDITOR_MESSAGE';

    this._dealWithPendingChanges(messageKey, () => {
      if (mode === 'view') {
        this._deleteDraft().finally(() => this._closePanelAndOpenContent(mode));
      } else {
        this._closePanelAndOpenContent(mode);
      }
    });
  }

  _dealWithPendingChanges(messageKey, done) {
    this._confirmSaveOrDiscardChanges(messageKey)
      .then((action) => {
        if (action === 'SAVE') {
          // don't return the result of saveDocument so a failing save does not invoke the 'done' function
          this.saveDocument().then(done);
        } else {
          done(); // discard
        }
      });
  }

  _confirmSaveOrDiscardChanges(messageKey) {
    if (!this.isDocumentDirty()) {
      return this.$q.resolve('DISCARD'); // No pending changes, no dialog, continue normally.
    }

    const message = this.$translate.instant(messageKey, { documentName: this.doc.displayName });
    const title = this.$translate.instant('SAVE_CHANGES_TITLE');

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

  _closePanelAndOpenContent(mode) {
    // The CMS automatically unlocks content that is being viewed, so close the side-panel to reflect that.
    // It will will unlock the document if needed, so don't delete the draft here.
    this.close();

    // mode can be 'view' or 'edit', so the event names can be 'view-content' and 'edit-content'
    this.CmsService.publish('open-content', this.documentId, mode);

    if (mode === 'view') {
      this.CmsService.reportUsageStatistic('CMSChannelsContentPublish');
    } else if (mode === 'edit') {
      this.CmsService.reportUsageStatistic('CMSChannelsContentEditor');
    }
  }

  closeButtonLabel() {
    return 'Cancel';
    // return this.isDocumentDirty() ? this.cancelLabel : this.closeLabel;
  }

  onClose(closeCallback) {
    this.closeHandler = closeCallback;
  }

  onSave(promiseCallback) {
    this.saveHandler = promiseCallback;
  }

  close() {
    this.closeHandler().then(this.SidePanelService.close('right'));
  }

  save() {
    this.saveHandler();
  }

  isSaveDisabled() {
    return true;
  }

  _deleteDraft() {
    if (this.editing) {
      return this.ContentService.deleteDraft(this.documentId);
    }
    return this.$q.resolve();
  }
}

export default editContentController;
