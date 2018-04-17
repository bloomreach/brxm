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

describe('EditContentToolsCtrl', () => {
  let $q;
  let $rootScope;
  let CmsService;
  let ContentEditor;
  let EditContentService;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject(($controller, _$q_, _$rootScope_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;

      CmsService = jasmine.createSpyObj('CmsService', ['publish', 'reportUsageStatistic']);
      ContentEditor = jasmine.createSpyObj('ContentEditor', [
        'close', 'confirmSaveOrDiscardChanges', 'deleteDraft', 'getDocument', 'getDocumentId', 'getError', 'isEditing',
      ]);
      EditContentService = jasmine.createSpyObj('EditContentService', ['stopEditing']);

      const $scope = $rootScope.$new();
      $ctrl = $controller('editContentToolsCtrl', {
        $scope,
        CmsService,
        ContentEditor,
        EditContentService,
      });
    });
  });

  it('disables buttons when the content editor error says so', () => {
    ContentEditor.getError.and.returnValue({
      disableContentButtons: true,
    });
    expect($ctrl.isDisabled()).toBeTruthy();

    ContentEditor.getError.and.returnValue({
      disableContentButtons: false,
    });
    expect($ctrl.isDisabled()).toBeFalsy();

    ContentEditor.getError.and.returnValue(undefined);
    expect($ctrl.isDisabled()).toBeFalsy();
  });

  it('knows when the content editor is editing', () => {
    ContentEditor.isEditing.and.returnValue(true);
    expect($ctrl.isEditing()).toBe(true);

    ContentEditor.isEditing.and.returnValue(false);
    expect($ctrl.isEditing()).toBe(false);
  });

  describe('opens the content editor in view mode and', () => {
    const documentId = '42';

    beforeEach(() => {
      ContentEditor.getDocumentId.and.returnValue(documentId);

      $ctrl.openContentEditor('view');
      expect(EditContentService.stopEditing).toHaveBeenCalled();
    });

    function expectSuccess() {
      expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalledWith('SAVE_CHANGES_ON_PUBLISH_MESSAGE');
      expect(ContentEditor.deleteDraft).toHaveBeenCalled();
      expect(CmsService.publish).toHaveBeenCalledWith('open-content', documentId, 'view');
      expect(ContentEditor.close).toHaveBeenCalled();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsContentPublish');
    }

    it('succeeds', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
      ContentEditor.deleteDraft.and.returnValue($q.resolve());

      $ctrl.uiCanExit().then(() => {
        expectSuccess();
        done();
      });
      $rootScope.$digest();
    });

    it('fails because save/discard changes is canceled', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());

      $ctrl.uiCanExit().catch(() => {
        expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalledWith('SAVE_CHANGES_ON_PUBLISH_MESSAGE');
        done();
      });
      $rootScope.$digest();
    });

    it('fails because the draft cannot be deleted', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
      ContentEditor.deleteDraft.and.returnValue($q.reject());

      $ctrl.uiCanExit().catch(() => {
        expect(ContentEditor.confirmSaveOrDiscardChanges).toHaveBeenCalledWith('SAVE_CHANGES_ON_PUBLISH_MESSAGE');
        expect(ContentEditor.deleteDraft).toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('succeeds after a previous attempt failed because save/discard changes was canceled', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.reject());
      $ctrl.uiCanExit().catch(() => {
        $ctrl.openContentEditor('view');
        ContentEditor.confirmSaveOrDiscardChanges.and.returnValue($q.resolve());
        $ctrl.uiCanExit().then(() => {
          expectSuccess();
          done();
        });
      });
      $rootScope.$digest();
    });
  });

  it('opens the content editor in edit mode', () => {
    const documentId = '42';
    ContentEditor.getDocumentId.and.returnValue(documentId);

    $ctrl.openContentEditor('edit');
    expect(EditContentService.stopEditing).toHaveBeenCalled();

    const uiRouterExitState = $ctrl.uiCanExit();

    expect(uiRouterExitState).toBe(true);
    expect(CmsService.publish).toHaveBeenCalledWith('open-content', documentId, 'edit');
    expect(ContentEditor.close).toHaveBeenCalled();
    expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsContentEditor');
  });

  it('always allows exiting the ui-router state when no button has been clicked', () => {
    expect($ctrl.uiCanExit()).toBe(true);
  });

  describe('the publication icon name', () => {
    it('is based on the publication state of the document', () => {
      ContentEditor.isEditing.and.returnValue(true);

      ContentEditor.getDocument.and.returnValue({ info: { publicationState: 'new' } });
      expect($ctrl.getPublicationIconName()).toBe('mdi-minus-circle-outline');

      ContentEditor.getDocument.and.returnValue({ info: { publicationState: 'live' } });
      expect($ctrl.getPublicationIconName()).toBe('mdi-check-circle-outline');

      ContentEditor.getDocument.and.returnValue({ info: { publicationState: 'changed' } });
      expect($ctrl.getPublicationIconName()).toBe('mdi-alert-outline');

      ContentEditor.getDocument.and.returnValue({ info: { publicationState: 'unknown' } });
      expect($ctrl.getPublicationIconName()).toBe('mdi-file-outline');
    });
    it('is empty when there is no document', () => {
      ContentEditor.getDocument.and.returnValue(null);
      expect($ctrl.getPublicationIconName()).toBe('');
    });
  });

  describe('the publication icon tooltip', () => {
    it('is based on the publication state of the document', () => {
      ContentEditor.isEditing.and.returnValue(true);

      ContentEditor.getDocument.and.returnValue({ info: { publicationState: 'new' } });
      expect($ctrl.getPublicationIconTooltip()).toBe('DOCUMENT_NEW_TOOLTIP');

      ContentEditor.getDocument.and.returnValue({ info: { publicationState: 'live' } });
      expect($ctrl.getPublicationIconTooltip()).toBe('DOCUMENT_LIVE_TOOLTIP');

      ContentEditor.getDocument.and.returnValue({ info: { publicationState: 'changed' } });
      expect($ctrl.getPublicationIconTooltip()).toBe('DOCUMENT_CHANGED_TOOLTIP');

      ContentEditor.getDocument.and.returnValue({ info: { publicationState: 'unknown' } });
      expect($ctrl.getPublicationIconTooltip()).toBe('');
    });
    it('is empty when there is no document', () => {
      ContentEditor.getDocument.and.returnValue(null);
      expect($ctrl.getPublicationIconTooltip()).toBe('');
    });
  });
});
