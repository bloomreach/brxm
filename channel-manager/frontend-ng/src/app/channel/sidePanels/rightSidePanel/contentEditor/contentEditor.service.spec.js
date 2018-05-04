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

describe('ContentEditorService', () => {
  let $q;
  let $rootScope;
  let $translate;
  let CmsService;
  let ContentEditor;
  let ContentService;
  let DialogService;
  let FeedbackService;
  let FieldService;
  let WorkflowService;

  let stringField;
  let multipleStringField;
  let emptyMultipleStringField;
  let testDocumentType;
  let testDocument;

  function expectDocumentLoaded() {
    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');

    expect(ContentEditor.getDocument()).toEqual(testDocument);
    expect(ContentEditor.getDocumentType()).toEqual(testDocumentType);
    expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    expect(ContentEditor.isPublishAllowed()).toBeFalsy();
    expect(ContentEditor.isEditing()).toBe(true);
    expect(ContentEditor.getPublicationState()).toBe('live');
    expect(ContentEditor.getError()).toBeUndefined();
  }

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    stringField = {
      id: 'ns:string',
      type: 'STRING',
    };
    multipleStringField = {
      id: 'ns:multiplestring',
      type: 'STRING',
      multiple: true,
    };
    emptyMultipleStringField = {
      id: 'ns:emptymultiplestring',
      type: 'STRING',
      multiple: true,
    };
    testDocumentType = {
      id: 'ns:testdocument',
      fields: [
        stringField,
        multipleStringField,
        emptyMultipleStringField,
      ],
    };
    testDocument = {
      id: 'test',
      info: {
        type: {
          id: 'ns:testdocument',
        },
        publicationState: 'live',
      },
      fields: {
        'ns:string': [
          {
            value: 'String value',
          },
        ],
        'ns:multiplestring': [
          {
            value: 'One',
          },
          {
            value: 'Two',
          },
        ],
        'ns:emptymultiplestring': [],
      },
    };

    ContentService = jasmine.createSpyObj('ContentService', ['getEditableDocument', 'getDocumentType', 'saveDocument', 'discardChanges', 'deleteDocument']);
    FeedbackService = jasmine.createSpyObj('FeedbackService', ['showError', 'showNotification']);
    FieldService = jasmine.createSpyObj('FieldService', ['setDocumentId']);
    WorkflowService = jasmine.createSpyObj('WorkflowService', ['createWorkflowAction']);

    angular.mock.module(($provide) => {
      $provide.value('ContentService', ContentService);
      $provide.value('FeedbackService', FeedbackService);
      $provide.value('FieldService', FieldService);
      $provide.value('WorkflowService', WorkflowService);
    });

    inject((_$q_, _$rootScope_, _$translate_, _CmsService_, _ContentEditor_, _DialogService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $translate = _$translate_;
      CmsService = _CmsService_;
      ContentEditor = _ContentEditor_;
      DialogService = _DialogService_;
    });

    spyOn(CmsService, 'closeDocumentWhenValid');
    spyOn(CmsService, 'reportUsageStatistic');

    spyOn(DialogService, 'show');
  });

  describe('opens a document', () => {
    beforeEach(() => {
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.getEditableDocument.and.returnValue($q.resolve(testDocument));
      ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
    });

    it('does not report unsupported fields when there are none', () => {
      ContentEditor.open('test');
      $rootScope.$digest();

      expectDocumentLoaded();
      expect(CmsService.reportUsageStatistic).not.toHaveBeenCalled();
    });

    it('reports all field types in a document that are not yet supported by the content editor', () => {
      testDocumentType.unsupportedFieldTypes = ['Date', 'selection:selection'];

      ContentEditor.open('test');
      $rootScope.$digest();

      expectDocumentLoaded();
      expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('VisualEditingUnsupportedFields', {
        unsupportedFieldTypes: 'Date,selection:selection',
      });
    });

    it('closes the previous document', () => {
      ContentEditor.open('test');
      $rootScope.$digest();

      testDocument.id = 'test2';
      ContentEditor.open('test2');

      expect(ContentEditor.getDocument()).toBeUndefined();
      expect(ContentEditor.getPublicationState()).toBeUndefined();
      $rootScope.$digest();
    });

    it('and allows publication when it can be published', () => {
      testDocument.info.canPublish = true;

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(ContentEditor.isPublishAllowed()).toBe(true);
    });

    it('and allows publication when request publication is enabled', () => {
      testDocument.info.canRequestPublication = true;

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(ContentEditor.isPublishAllowed()).toBe(true);
    });

    it('and does not allow publication when it cannot be published and no request for publication can be filed', () => {
      testDocument.info.canPublish = false;
      testDocument.info.canRequestPublication = false;

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(ContentEditor.isPublishAllowed()).toBe(false);
    });

    it('and does not allow publication when no publication info is available', () => {
      ContentEditor.open('test');
      $rootScope.$digest();

      expect(ContentEditor.isPublishAllowed()).toBeFalsy();
    });

    describe('and sets an error when it', () => {
      function expectError(error) {
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getDocumentType()).toBeUndefined();
        expect(ContentEditor.isDocumentDirty()).toBeFalsy();
        expect(ContentEditor.isEditing()).toBe(false);
        expect(ContentEditor.getError()).toEqual(error);
      }

      function expectDefaultError() {
        expectError({
          titleKey: 'FEEDBACK_DEFAULT_TITLE',
          messageKey: 'FEEDBACK_DEFAULT_MESSAGE',
          linkToContentEditor: true,
        });
      }

      it('opens a document without content', () => {
        const emptyDocument = {
          id: 'test',
          displayName: 'Display Name',
          info: {
            type: { id: 'ns:testdocument' },
          },
          fields: {},
        };
        ContentService.getEditableDocument.and.returnValue($q.resolve(emptyDocument));

        ContentEditor.open(emptyDocument.id);
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expectError({
          titleKey: 'FEEDBACK_NOT_EDITABLE_HERE_TITLE',
          messageKey: 'FEEDBACK_NO_EDITABLE_CONTENT_MESSAGE',
          messageParams: {
            displayName: 'Display Name',
          },
          linkToContentEditor: true,
        });
      });

      it('opens a document with pending invalid changes in the editable document', () => {
        CmsService.closeDocumentWhenValid.and.returnValue($q.reject());

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_DOCUMENT_INVALID_TITLE',
          messageKey: 'FEEDBACK_DOCUMENT_INVALID_MESSAGE',
          linkToContentEditor: true,
        });
      });

      it('opens a document owned by another user', () => {
        const response = {
          reason: 'OTHER_HOLDER',
          params: {
            displayName: 'Display Name',
            userId: 'jtester',
            userName: 'John Tester',
            publicationState: 'changed',
          },
        };
        ContentService.getEditableDocument.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getPublicationState()).toBe('changed');
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
          messageKey: 'FEEDBACK_HELD_BY_OTHER_USER_MESSAGE',
          messageParams: {
            displayName: 'Display Name',
            user: 'John Tester',
          },
        });
      });

      it('opens a document owned by another user and falls back to the user\'s id if there is no display name', () => {
        const response = {
          reason: 'OTHER_HOLDER',
          params: {
            userId: 'tester',
          },
        };
        ContentService.getEditableDocument.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
          messageKey: 'FEEDBACK_HELD_BY_OTHER_USER_MESSAGE',
          messageParams: {
            user: 'tester',
          },
        });
      });

      it('opens a document with a publication request', () => {
        const response = {
          reason: 'REQUEST_PENDING',
          params: {
            displayName: 'Display Name',
            publicationState: 'new',
          },
        };
        ContentService.getEditableDocument.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getPublicationState()).toBe('new');
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
          messageKey: 'FEEDBACK_REQUEST_PENDING_MESSAGE',
          messageParams: {
            displayName: 'Display Name',
          },
        });
      });

      it('opens a document which is not a document', () => {
        const response = {
          reason: 'NOT_A_DOCUMENT',
        };
        ContentService.getEditableDocument.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).not.toHaveBeenCalled();
        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getPublicationState()).toBeUndefined();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_NOT_A_DOCUMENT_TITLE',
          messageKey: 'FEEDBACK_NOT_A_DOCUMENT_MESSAGE',
          linkToContentEditor: true,
        });
      });

      it('opens a non-existing document', () => {
        ContentService.getEditableDocument.and.returnValue($q.reject({ status: 404 }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expect(ContentEditor.getPublicationState()).toBeUndefined();
        expectError({
          titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
          messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
          disableContentButtons: true,
        });
      });

      it('opens a document with random data in the response', () => {
        const response = { bla: 'test' };
        ContentService.getEditableDocument.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expectDefaultError();
      });

      it('opens a document with no data in the response', () => {
        ContentService.getEditableDocument.and.returnValue($q.reject({}));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expectDefaultError();
      });

      it('opens a document with an unknown error reason', () => {
        const response = {
          reason: 'unknown',
        };
        ContentService.getEditableDocument.and.returnValue($q.reject({ data: response }));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expectError(undefined);
      });

      it('opens a document without a type', () => {
        const doc = {
          info: {
            type: {
              id: 'document:type',
            },
          },
          fields: {
            bla: 1,
          },
          displayName: 'Document Display Name',
        };
        ContentService.getEditableDocument.and.returnValue($q.resolve(doc));
        ContentService.getDocumentType.and.returnValue($q.reject({}));

        ContentEditor.open('test');
        $rootScope.$digest();

        expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expect(ContentService.getDocumentType).toHaveBeenCalledWith('document:type');
        expectDefaultError();
      });
    });
  });

  it('marks a document dirty', () => {
    expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    ContentEditor.markDocumentDirty();
    expect(ContentEditor.isDocumentDirty()).toBe(true);
  });

  describe('save', () => {
    it('happens with a dirty document', () => {
      const savedDoc = {
        id: '123',
      };
      ContentService.saveDocument.and.returnValue($q.resolve(savedDoc));

      ContentEditor.document = testDocument;
      ContentEditor.markDocumentDirty();
      ContentEditor.save();

      expect(ContentService.saveDocument).toHaveBeenCalledWith(testDocument);

      $rootScope.$digest();

      expect(ContentEditor.getDocument()).toEqual(savedDoc);
      expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    });

    it('does not happen with a pristine document', () => {
      ContentEditor.document = testDocument;

      ContentEditor.save();
      $rootScope.$digest();

      expect(ContentService.saveDocument).not.toHaveBeenCalled();
    });

    describe('shows error feedback when it', () => {
      it('fails', () => {
        const response = {
          reason: 'TEST',
        };
        ContentService.saveDocument.and.returnValue($q.reject({ data: response }));

        ContentEditor.document = testDocument;
        ContentEditor.markDocumentDirty();
        ContentEditor.save();

        expect(ContentService.saveDocument).toHaveBeenCalledWith(testDocument);

        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_TEST');
      });

      it('fails because another user is now the holder', () => {
        const response = {
          reason: 'OTHER_HOLDER',
          params: {
            userId: 'tester',
          },
        };
        ContentService.saveDocument.and.returnValue($q.reject({ data: response }));

        ContentEditor.document = testDocument;
        ContentEditor.markDocumentDirty();
        ContentEditor.save();

        expect(ContentService.saveDocument).toHaveBeenCalledWith(testDocument);

        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_OTHER_HOLDER', { user: 'tester' });
      });

      it('fails because another *named* user is now the holder', () => {
        const response = {
          reason: 'OTHER_HOLDER',
          params: {
            userId: 'tester',
            userName: 'Joe Tester',
          },
        };
        ContentService.saveDocument.and.returnValue($q.reject({ data: response }));

        ContentEditor.document = testDocument;
        ContentEditor.markDocumentDirty();
        ContentEditor.save();

        expect(ContentService.saveDocument).toHaveBeenCalledWith(testDocument);

        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_OTHER_HOLDER', { user: 'Joe Tester' });
      });

      describe('fails because of an invalid field', () => {
        beforeEach(() => {
          const saveResponse = angular.copy(testDocument);
          saveResponse.fields['ns:string'] = [
            {
              value: '',
              errorInfo: {
                code: 'REQUIRED_FIELD_EMPTY',
              },
            },
          ];

          ContentService.saveDocument.and.returnValue($q.reject({ data: saveResponse }));

          ContentEditor.document = testDocument;
          ContentEditor.documentType = testDocumentType;
          ContentEditor.markDocumentDirty();
        });

        it('reloads the document type', () => {
          const reloadedDocumentType = angular.copy(testDocumentType);
          ContentService.getDocumentType.and.returnValue($q.resolve(reloadedDocumentType));

          ContentEditor.save();

          expect(ContentService.saveDocument).toHaveBeenCalledWith(testDocument);

          $rootScope.$digest();

          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_INVALID_DATA');
          expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
          expect(ContentEditor.getDocumentType()).toBe(reloadedDocumentType);
        });

        it('shows an error when reloading the document type fails', () => {
          ContentService.getDocumentType.and.returnValue($q.reject({ status: 404 }));

          ContentEditor.save();

          expect(ContentService.saveDocument).toHaveBeenCalledWith(testDocument);

          $rootScope.$digest();

          expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_INVALID_DATA');
          expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
          expect(ContentEditor.getDocumentType()).toBe(testDocumentType);
          expect(ContentEditor.getError()).toEqual({
            titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
            messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
            disableContentButtons: true,
          });
        });
      });

      it('fails because there is no data returned', () => {
        ContentService.saveDocument.and.returnValue($q.reject({}));

        ContentEditor.document = testDocument;
        ContentEditor.markDocumentDirty();
        ContentEditor.save();

        expect(ContentService.saveDocument).toHaveBeenCalledWith(testDocument);

        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UNABLE_TO_SAVE');
      });
    });
  });

  describe('confirm discard changes', () => {
    const showPromise = {};

    beforeEach(() => {
      ContentEditor.document = {
        displayName: 'Test',
      };
      spyOn($translate, 'instant');
      spyOn(DialogService, 'confirm').and.callThrough();
      DialogService.show.and.returnValue(showPromise);
    });

    it('shows a dialog', () => {
      ContentEditor.markDocumentDirty();

      const result = ContentEditor.confirmDiscardChanges('MESSAGE_KEY');

      expect(DialogService.confirm).toHaveBeenCalled();
      expect($translate.instant).toHaveBeenCalledWith('MESSAGE_KEY', {
        documentName: 'Test',
      });
      expect(DialogService.show).toHaveBeenCalled();
      expect(result).toBe(showPromise);
    });

    it('shows a dialog with a title', () => {
      ContentEditor.markDocumentDirty();

      const result = ContentEditor.confirmDiscardChanges('MESSAGE_KEY', 'TITLE_KEY');

      expect(DialogService.confirm).toHaveBeenCalled();
      expect($translate.instant).toHaveBeenCalledWith('MESSAGE_KEY', {
        documentName: 'Test',
      });
      expect($translate.instant).toHaveBeenCalledWith('TITLE_KEY', {
        documentName: 'Test',
      });
      expect(DialogService.show).toHaveBeenCalled();
      expect(result).toBe(showPromise);
    });

    it('does not show a dialog when the document has not changed', (done) => {
      ContentEditor.confirmDiscardChanges().then(() => {
        expect(DialogService.show).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not show a dialog when the editor is killed', (done) => {
      ContentEditor.markDocumentDirty();
      ContentEditor.kill();
      ContentEditor.confirmDiscardChanges().then(() => {
        expect(DialogService.show).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });
  });

  describe('confirm save or discard changes', () => {
    beforeEach(() => {
      testDocument.displayName = 'Test';
      ContentEditor.document = testDocument;
      spyOn($translate, 'instant');
    });

    it('shows a dialog and saves changes', (done) => {
      ContentEditor.markDocumentDirty();
      DialogService.show.and.returnValue($q.resolve('SAVE'));
      ContentService.saveDocument.and.returnValue($q.resolve(testDocument));

      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').then((action) => {
        expect(action).toBe('SAVE');
        expect($translate.instant).toHaveBeenCalledWith('TEST_MESSAGE_KEY', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('SAVE_CHANGES_TITLE');
        expect(DialogService.show).toHaveBeenCalled();
        expect(ContentService.saveDocument).toHaveBeenCalledWith(testDocument);
        done();
      });
      $rootScope.$digest();
    });

    it('shows a dialog and discards changes', (done) => {
      ContentEditor.markDocumentDirty();
      DialogService.show.and.returnValue($q.resolve('DISCARD'));

      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').then((action) => {
        expect(action).toBe('DISCARD');
        expect($translate.instant).toHaveBeenCalledWith('TEST_MESSAGE_KEY', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('SAVE_CHANGES_TITLE');
        expect(DialogService.show).toHaveBeenCalled();
        expect(ContentService.saveDocument).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('shows a dialog and does nothing', (done) => {
      ContentEditor.markDocumentDirty();
      DialogService.show.and.returnValue($q.reject());

      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').catch(() => {
        expect($translate.instant).toHaveBeenCalledWith('TEST_MESSAGE_KEY', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('SAVE_CHANGES_TITLE');
        expect(DialogService.show).toHaveBeenCalled();
        expect(ContentService.saveDocument).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not show a dialog when the document has not changed', (done) => {
      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').then(() => {
        expect(DialogService.show).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not show a dialog when the editor is killed', (done) => {
      ContentEditor.markDocumentDirty();
      ContentEditor.kill();
      ContentEditor.confirmSaveOrDiscardChanges('TEST_MESSAGE_KEY').then(() => {
        expect(DialogService.show).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });
  });

  describe('discard document', () => {
    it('happens when a document is edited and the editor is not killed', () => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentService.discardChanges.and.returnValue($q.resolve());

      ContentEditor.discardChanges();
      $rootScope.$digest();

      expect(ContentService.discardChanges).toHaveBeenCalledWith(testDocument.id);
    });

    it('does not happens when no document is being edited', (done) => {
      ContentEditor.discardChanges().then(() => {
        expect(ContentService.discardChanges).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not happens when the editor is killed', (done) => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;
      ContentEditor.kill();

      ContentEditor.discardChanges().then(() => {
        expect(ContentService.discardChanges).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });
  });

  describe('delete document', () => {
    it('happens when a document is edited and the editor is not killed', () => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentService.deleteDocument.and.returnValue($q.resolve());

      ContentEditor.deleteDocument();
      $rootScope.$digest();

      expect(ContentService.deleteDocument).toHaveBeenCalledWith(testDocument.id);
    });

    it('does not happens when no document is being edited', (done) => {
      ContentEditor.deleteDocument().then(() => {
        expect(ContentService.deleteDocument).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('does not happens when the editor is killed', (done) => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;
      ContentEditor.kill();

      ContentEditor.deleteDocument().then(() => {
        expect(ContentService.deleteDocument).not.toHaveBeenCalled();
        done();
      });
      $rootScope.$digest();
    });

    it('shows an error when deleting the document fails', () => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentService.deleteDocument.and.returnValue($q.reject({
        data: {
          reason: 'NOT_ALLOWED',
          params: {
            foo: 1,
          },
        },
      }));

      ContentEditor.deleteDocument();
      $rootScope.$digest();

      expect(ContentService.deleteDocument).toHaveBeenCalledWith(testDocument.id);
      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_NOT_ALLOWED', { foo: 1 });
    });
  });

  function expectClear() {
    expect(ContentEditor.getDocument()).toBeUndefined();
    expect(ContentEditor.getDocumentId()).toBeUndefined();
    expect(ContentEditor.getDocumentType()).toBeUndefined();
    expect(ContentEditor.getError()).toBeUndefined();
    expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    expect(ContentEditor.isEditing()).toBeFalsy();
  }

  it('is cleared initially', () => {
    expectClear();
  });

  describe('close', () => {
    it('clears an opened document', () => {
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.getEditableDocument.and.returnValue($q.resolve(testDocument));
      ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
      ContentEditor.open('test');

      ContentEditor.close();

      expectClear();
    });

    it('clears an error', () => {
      ContentEditor.error = {
        titleKey: 'FEEDBACK_DEFAULT_TITLE',
      };

      ContentEditor.close();

      expectClear();
    });

    it('resets the kill state', () => {
      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentEditor.kill();

      ContentEditor.discardChanges();
      $rootScope.$digest();

      expect(ContentService.discardChanges).not.toHaveBeenCalled();

      ContentEditor.close();

      ContentEditor.document = testDocument;
      ContentEditor.documentType = testDocumentType;

      ContentEditor.discardChanges();
      $rootScope.$digest();

      expect(ContentService.discardChanges).toHaveBeenCalled();
    });
  });

  describe('confirmPublication', () => {
    const showPromise = {};

    beforeEach(() => {
      ContentEditor.document = {
        displayName: 'Test',
      };
      spyOn($translate, 'instant');
      spyOn(DialogService, 'confirm').and.callThrough();
      DialogService.show.and.returnValue(showPromise);
    });

    describe('when a user can publish', () => {
      beforeEach(() => {
        ContentEditor.canPublish = true;
      });

      it('shows a "publish" confirmation dialog', () => {
        const publishDialog = ContentEditor.confirmPublication();

        expect(DialogService.confirm).toHaveBeenCalled();
        expect($translate.instant).toHaveBeenCalledWith('CONFIRM_PUBLISH_DOCUMENT', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('PUBLISH');
        expect(DialogService.show).toHaveBeenCalled();
        expect(publishDialog).toBe(showPromise);
      });

      it('shows a "save and publish" confirmation dialog', () => {
        ContentEditor.markDocumentDirty();
        const saveAndPublishDialog = ContentEditor.confirmPublication();

        expect(DialogService.confirm).toHaveBeenCalled();
        expect($translate.instant).toHaveBeenCalledWith('CONFIRM_PUBLISH_DIRTY_DOCUMENT', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('SAVE_AND_PUBLISH');
        expect(DialogService.show).toHaveBeenCalled();
        expect(saveAndPublishDialog).toBe(showPromise);
      });
    });

    describe('when a user can request publication', () => {
      beforeEach(() => {
        ContentEditor.canRequestPublication = true;
      });

      it('shows a "request publication" confirmation dialog', () => {
        const requestPublicationDialog = ContentEditor.confirmPublication();

        expect(DialogService.confirm).toHaveBeenCalled();
        expect($translate.instant).toHaveBeenCalledWith('CONFIRM_REQUEST_PUBLICATION_OF_DOCUMENT', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('REQUEST_PUBLICATION');
        expect(DialogService.show).toHaveBeenCalled();
        expect(requestPublicationDialog).toBe(showPromise);
      });

      it('shows a "save and request publication" confirmation dialog', () => {
        ContentEditor.markDocumentDirty();
        const saveAndPublishDialog = ContentEditor.confirmPublication();

        expect(DialogService.confirm).toHaveBeenCalled();
        expect($translate.instant).toHaveBeenCalledWith('CONFIRM_REQUEST_PUBLICATION_OF_DIRTY_DOCUMENT', {
          documentName: 'Test',
        });
        expect($translate.instant).toHaveBeenCalledWith('SAVE_AND_REQUEST_PUBLICATION');
        expect(DialogService.show).toHaveBeenCalled();
        expect(saveAndPublishDialog).toBe(showPromise);
      });
    });
  });

  describe('publish', () => {
    const errorObject = {
      data: {
        reason: 'error-reason',
        params: 'error-params',
      },
    };
    const newDoc = { id: 'new-doc' };

    beforeEach(() => {
      ContentEditor.documentId = 'test';
      ContentEditor.document = {
        displayName: 'Test',
      };
      ContentService.discardChanges.and.returnValue($q.resolve());
      WorkflowService.createWorkflowAction.and.returnValue($q.resolve());
    });

    describe('when a user can publish', () => {
      beforeEach(() => {
        ContentEditor.canPublish = true;
        ContentService.getEditableDocument.and.returnValue($q.resolve(newDoc));
      });

      it('discards the document changes', () => {
        ContentEditor.publish();

        expect(ContentService.discardChanges).toHaveBeenCalledWith('test');
      });

      it('does not execute workflow action if discard changes fails', () => {
        ContentService.discardChanges.and.returnValue($q.reject());
        ContentEditor.publish();
        $rootScope.$digest();

        expect(WorkflowService.createWorkflowAction).not.toHaveBeenCalled();
      });

      it('executes the publish document workflow action', () => {
        ContentEditor.publish();
        $rootScope.$digest();

        expect(WorkflowService.createWorkflowAction).toHaveBeenCalledWith('test', 'publish');
      });

      it('notifies the user of a successful publication action', () => {
        ContentEditor.publish();
        $rootScope.$digest();

        expect(FeedbackService.showNotification).toHaveBeenCalledWith('NOTIFICATION_DOCUMENT_PUBLISHED', { documentName: 'Test' });
      });

      it('displays an error if publication fails', () => {
        WorkflowService.createWorkflowAction.and.returnValue($q.reject(errorObject));

        ContentEditor.publish();
        $rootScope.$digest();

        expect(FeedbackService.showNotification).not.toHaveBeenCalled();
        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_PUBLISH_DOCUMENT_FAILED', { documentName: 'Test' });
      });

      it('gets an editable document again after publication succeeds', () => {
        ContentEditor.publish();
        $rootScope.$digest();

        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expect(ContentEditor.document).toBe(newDoc);
      });

      it('gets an editable document again if publication fails', () => {
        WorkflowService.createWorkflowAction.and.returnValue($q.reject(errorObject));

        ContentEditor.publish();
        $rootScope.$digest();

        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expect(ContentEditor.document).toBe(newDoc);
      });

      it('sets an error if getting an editable document fails', () => {
        ContentService.getEditableDocument.and.returnValue($q.reject(errorObject));

        ContentEditor.publish();
        $rootScope.$digest();

        expect(ContentEditor.getDocument()).toBeUndefined();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_DOCUMENT_INVALID_TITLE',
          messageKey: 'FEEDBACK_DOCUMENT_INVALID_MESSAGE',
          linkToContentEditor: true,
        });
      });

      it('resolves when publication is successful', (done) => {
        ContentEditor.publish().then((done));
        $rootScope.$digest();
      });

      it('rejects when publication fails', (done) => {
        WorkflowService.createWorkflowAction.and.returnValue($q.reject(errorObject));
        ContentEditor.publish().catch(done);
        $rootScope.$digest();
      });

      it('rejects when discardChanges fails', (done) => {
        ContentService.discardChanges.and.returnValue($q.reject(errorObject));
        ContentEditor.publish().catch(done);
        $rootScope.$digest();
      });

      it('resolves when getting an editable document fails', (done) => {
        ContentService.getEditableDocument.and.returnValue($q.reject(errorObject));
        ContentEditor.publish().then(done);
        $rootScope.$digest();
      });
    });

    describe('when a user can request publication', () => {
      let expectedDocumentError;

      beforeEach(() => {
        expectedDocumentError = {
          data: {
            reason: 'REQUEST_PENDING',
          },
        };

        ContentEditor.canRequestPublication = true;
        ContentService.getEditableDocument.and.returnValue($q.reject(expectedDocumentError));
      });

      it('discards the document', () => {
        ContentEditor.publish();

        expect(ContentService.discardChanges).toHaveBeenCalledWith('test');
      });

      it('does not execute workflow action if discard document fails', () => {
        ContentService.discardChanges.and.returnValue($q.reject());
        ContentEditor.publish();
        $rootScope.$digest();

        expect(WorkflowService.createWorkflowAction).not.toHaveBeenCalled();
      });

      it('executes the requestPublication document workflow action', () => {
        ContentEditor.publish();
        $rootScope.$digest();

        expect(WorkflowService.createWorkflowAction).toHaveBeenCalledWith('test', 'requestPublication');
      });

      it('notifies the user of a successful publication request', () => {
        ContentEditor.publish();
        $rootScope.$digest();

        expect(FeedbackService.showNotification).toHaveBeenCalledWith('NOTIFICATION_PUBLICATION_REQUESTED', { documentName: 'Test' });
      });

      it('displays an error if publication request fails', () => {
        WorkflowService.createWorkflowAction.and.returnValue($q.reject(errorObject));

        ContentEditor.publish();
        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_REQUEST_PUBLICATION_FAILED', { documentName: 'Test' });
      });

      it('fails to get an editable document after publication request succeeds', () => {
        ContentEditor.publish();
        $rootScope.$digest();

        expect(ContentService.getEditableDocument).toHaveBeenCalledWith('test');
        expect(ContentEditor.document).toBeUndefined();
        expect(FeedbackService.showError).not.toHaveBeenCalled();
        expect(ContentEditor.getError()).toEqual({
          titleKey: 'FEEDBACK_NOT_EDITABLE_TITLE',
          messageKey: 'FEEDBACK_REQUEST_PENDING_MESSAGE',
          messageParams: {
            displayName: 'Display Name',
          },
        });
      });

      it('resolves when publication request is successful', (done) => {
        ContentEditor.publish().then((done));
        $rootScope.$digest();
      });

      it('rejects when publication request fails', (done) => {
        WorkflowService.createWorkflowAction.and.returnValue($q.reject(errorObject));
        ContentEditor.publish().catch(done);
        $rootScope.$digest();
      });

      it('rejects when discardChanges fails', (done) => {
        ContentService.discardChanges.and.returnValue($q.reject(errorObject));
        ContentEditor.publish().catch(done);
        $rootScope.$digest();
      });

      it('resolves when getting an editable document fails', (done) => {
        ContentEditor.publish().then(done);
        $rootScope.$digest();
      });
    });

    describe('when a user can cancel a request for publication', () => {
      let expectedDocumentError;

      beforeEach(() => {
        expectedDocumentError = {
          data: {
            reason: 'CANCELABLE_PUBLICATION_REQUEST_PENDING',
            params: {
              displayName: 'Test',
            },
          },
        };

        CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
        ContentService.getEditableDocument.and.returnValue($q.reject(expectedDocumentError));
        ContentEditor.open('test');
        $rootScope.$digest();
      });

      it('executes a cancelRequest workflow call', () => {
        ContentEditor.cancelRequestPublication();

        expect(WorkflowService.createWorkflowAction).toHaveBeenCalledWith('test', 'cancelRequest');
      });

      it('shows an error and rejects if cancelRequest workflow call fails', (done) => {
        WorkflowService.createWorkflowAction.and.returnValue($q.reject());
        ContentEditor.cancelRequestPublication().catch(done);
        $rootScope.$digest();

        expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_CANCEL_REQUEST_PUBLICATION_FAILED', { documentName: 'Test' });
      });

      it('(re)loads the document and document type after a successful workflow call', () => {
        WorkflowService.createWorkflowAction.and.returnValue($q.resolve());

        CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
        ContentService.getEditableDocument.and.returnValue($q.resolve(testDocument));
        ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));

        ContentEditor.cancelRequestPublication();
        $rootScope.$digest();

        expectDocumentLoaded();
      });
    });
  });
});
