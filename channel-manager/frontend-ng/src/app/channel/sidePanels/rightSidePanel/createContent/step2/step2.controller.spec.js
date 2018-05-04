/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
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

const testDocument = {
  id: 'testId',
  displayName: 'test document',
  info: {
    dirty: false,
    type: {
      id: 'ns:testdocument',
    },
  },
};

describe('Create content step 2 controller', () => {
  let $rootScope;
  let $q;
  let CmsService;
  let ContentEditor;
  let CreateContentService;
  let FeedbackService;
  let Step2Service;

  let $ctrl;

  beforeEach(() => {
    angular.mock.module('hippo-cm.channel.createContent.step2');

    inject((
      $controller,
      _$rootScope_,
      _$q_,
      _CmsService_,
      _ContentEditor_,
      _CreateContentService_,
      _FeedbackService_,
      _Step2Service_,
    ) => {
      $rootScope = _$rootScope_;
      $q = _$q_;
      CmsService = _CmsService_;
      ContentEditor = _ContentEditor_;
      CreateContentService = _CreateContentService_;
      FeedbackService = _FeedbackService_;
      Step2Service = _Step2Service_;

      $ctrl = $controller('step2Ctrl');
    });

    spyOn(CmsService, 'reportUsageStatistic');
    spyOn(ContentEditor, 'getDocument').and.returnValue(testDocument);
    spyOn(ContentEditor, 'getDocumentId').and.returnValue(testDocument.id);
    spyOn(FeedbackService, 'showError');
    spyOn(FeedbackService, 'showNotification');
  });

  describe('$onInit', () => {
    it('set documentIsSaved state to false', () => {
      $ctrl.documentIsSaved = true;
      $ctrl.$onInit();
      expect($ctrl.documentIsSaved).toBe(false);
    });
  });

  it('knows when all mandatory fields are shown', () => {
    spyOn(ContentEditor, 'isEditing');
    spyOn(ContentEditor, 'getDocumentType');

    [true, false].forEach((editing) => {
      [true, false].forEach((canCreateAllRequiredFields) => {
        ContentEditor.isEditing.and.returnValue(editing);
        ContentEditor.getDocumentType.and.returnValue({ canCreateAllRequiredFields });
        expect($ctrl.allMandatoryFieldsShown()).toBe(editing && canCreateAllRequiredFields);
      });
    });
  });

  it('can switch the editor', () => {
    spyOn(CmsService, 'publish');
    spyOn(ContentEditor, 'close');
    spyOn(CreateContentService, 'stop');

    $ctrl.switchEditor();

    expect(CmsService.publish).toHaveBeenCalledWith('open-content', testDocument.id, 'edit');
    expect(ContentEditor.close).toHaveBeenCalled();
    expect(CreateContentService.stop).toHaveBeenCalled();
  });

  it('saves the contentEditor and finishes create-content', () => {
    spyOn(ContentEditor, 'save').and.returnValue($q.resolve());
    spyOn(ContentEditor, 'discardChanges').and.returnValue($q.resolve());
    spyOn(Step2Service, 'saveComponentParameter').and.returnValue($q.resolve());
    spyOn(CreateContentService, 'finish');
    $ctrl.save();

    expect(ContentEditor.save).toHaveBeenCalled();
    $rootScope.$digest();
    expect($ctrl.documentIsSaved).toBe(true);
    expect(ContentEditor.discardChanges).toHaveBeenCalled();
    expect(FeedbackService.showNotification).toHaveBeenCalled();
    expect(CreateContentService.finish).toHaveBeenCalledWith('testId');
    expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CreateContent2Done');
  });

  it('stops create content when close is called', () => {
    spyOn(CreateContentService, 'stop');
    $ctrl.close();
    expect(CreateContentService.stop).toHaveBeenCalled();
  });

  it('returns the document reference of the ContentEditor', () => {
    expect($ctrl.getDocument()).toBe(testDocument);
  });

  it('opens the edit-name-url dialog', () => {
    spyOn(Step2Service, 'openEditNameUrlDialog');
    $ctrl.openEditNameUrlDialog();
    expect(Step2Service.openEditNameUrlDialog).toHaveBeenCalled();
  });

  describe('uiCanExit', () => {
    let deleteDocumentSpy;

    beforeEach(() => {
      spyOn(ContentEditor, 'confirmDiscardChanges').and.callThrough();
      deleteDocumentSpy = spyOn(ContentEditor, 'deleteDocument').and.returnValue($q.resolve());
    });

    it('allows ui-exit without dialog if document is already saved', () => {
      $ctrl.documentIsSaved = true;
      expect($ctrl.uiCanExit()).toBe(true);
      expect(ContentEditor.confirmDiscardChanges).not.toHaveBeenCalled();
    });

    it('allows ui-exit without dialog when switching editor', () => {
      $ctrl.switchingEditor = true;
      expect($ctrl.uiCanExit()).toBe(true);
      expect(ContentEditor.confirmDiscardChanges).not.toHaveBeenCalled();
    });

    it('calls confirmDiscardChanges if document is not yet saved', () => {
      $ctrl.uiCanExit();
      expect(ContentEditor.confirmDiscardChanges).toHaveBeenCalled();
    });

    it('does not delete the draft if confirmDiscardChanges is canceled', (done) => {
      ContentEditor.confirmDiscardChanges.and.returnValue($q.reject());

      $ctrl.uiCanExit().then(
        () => fail('Dialog promise should not be resolved'),
        () => {
          expect(deleteDocumentSpy).not.toHaveBeenCalled();
          done();
        });
      $rootScope.$digest();
    });

    it('deletes the document when confirmDiscardChanges is resolved', (done) => {
      ContentEditor.confirmDiscardChanges.and.returnValue($q.resolve());

      $ctrl.uiCanExit().then(() => {
        expect(deleteDocumentSpy).toHaveBeenCalled();
        done();
      }, () => fail('Dialog should not reject'));
      $rootScope.$digest();
    });
  });
});
