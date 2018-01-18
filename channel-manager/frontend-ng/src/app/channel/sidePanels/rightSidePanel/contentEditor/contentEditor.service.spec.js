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
  let CmsService;
  let ContentEditor;
  let ContentService;
  let DialogService;
  let FeedbackService;
  let FieldService;

  const stringField = {
    id: 'ns:string',
    type: 'STRING',
  };
  const multipleStringField = {
    id: 'ns:multiplestring',
    type: 'STRING',
    multiple: true,
  };
  const emptyMultipleStringField = {
    id: 'ns:emptymultiplestring',
    type: 'STRING',
    multiple: true,
  };
  const testDocumentType = {
    id: 'ns:testdocument',
    fields: [
      stringField,
      multipleStringField,
      emptyMultipleStringField,
    ],
  };
  const testDocument = {
    id: 'test',
    info: {
      type: {
        id: 'ns:testdocument',
      },
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

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ContentService = jasmine.createSpyObj('ContentService', ['createDraft', 'getDocumentType', 'saveDraft', 'deleteDraft']);
    DialogService = jasmine.createSpyObj('DialogService', ['confirm', 'show']);
    FeedbackService = jasmine.createSpyObj('FeedbackService', ['showError']);
    FieldService = jasmine.createSpyObj('FieldService', ['setDocumentId']);

    angular.mock.module(($provide) => {
      $provide.value('ContentService', ContentService);
      $provide.value('DialogService', DialogService);
      $provide.value('FeedbackService', FeedbackService);
      $provide.value('FieldService', FieldService);
    });

    inject((_$q_, _$rootScope_, _CmsService_, _ContentEditor_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      CmsService = _CmsService_;
      ContentEditor = _ContentEditor_;
    });

    spyOn(CmsService, 'closeDocumentWhenValid');
    spyOn(CmsService, 'reportUsageStatistic');
  });

  it('is cleared initially', () => {
    expect(ContentEditor.getDocument()).toBeUndefined();
    expect(ContentEditor.getDocumentId()).toBeUndefined();
    expect(ContentEditor.getDocumentType()).toBeUndefined();
    expect(ContentEditor.getError()).toBeUndefined();
    expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    expect(ContentEditor.isEditing()).toBeFalsy();
  });

  //
  // it('asks for confirmation when cancelling changes', () => {
  //   DialogService.show.and.returnValue($q.resolve());
  //
  //   const deferClose = $q.defer();
  //   SidePanelService.close.and.returnValue(deferClose.promise);
  //
  //   ContentEditor.getDocument() = {
  //     displayName: 'test',
  //   };
  //   ContentEditor.documentId = 'test';
  //   ContentEditor.form.$dirty = true;
  //   ContentEditor.editing = true;
  //
  //   ContentEditor.close();
  //   $rootScope.$digest();
  //
  //   expect(ContentEditor.deleteDraftOnClose).toBe(false);
  //   expect(ContentService.deleteDraft).toHaveBeenCalledWith('test');
  //   expect(SidePanelService.close).toHaveBeenCalledWith('right');
  //
  //   deferClose.resolve();
  //   $rootScope.$digest();
  //
  //   expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE', {
  //     documentName: 'test',
  //   });
  //   expect(ContentEditor.editing).toBeFalsy();
  //   expect(ContentEditor.deleteDraftOnClose).toBe(true);
  // });
  //
  // it('asks doesn\'t delete and close if discarding is not confirmed', () => {
  //   DialogService.show.and.returnValue($q.reject());
  //   ContentEditor.getDocument() = {};
  //   ContentEditor.documentId = 'test';
  //   ContentEditor.form.$dirty = true;
  //   ContentEditor.close();
  //   $rootScope.$digest();
  //
  //   expect(ContentService.deleteDraft).not.toHaveBeenCalled();
  //   expect(SidePanelService.close).not.toHaveBeenCalled();
  // });
  //

  it('opens a document', () => {
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.resolve(testDocument));
    ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));

    ContentEditor.open('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');

    expect(ContentEditor.getDocument()).toEqual(testDocument);
    expect(ContentEditor.getDocumentType()).toEqual(testDocumentType);
    expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    expect(ContentEditor.isEditing()).toBe(true);
    expect(ContentEditor.getError()).toBeUndefined();
  });

  describe('sets an error when it', () => {
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
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.resolve(emptyDocument));

      ContentEditor.open(emptyDocument.id);
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
      expectError({
        titleKey: 'FEEDBACK_NOT_EDITABLE_HERE_TITLE',
        messageKey: 'FEEDBACK_NO_EDITABLE_CONTENT_MESSAGE',
        messageParams: {
          displayName: 'Display Name',
        },
        linkToContentEditor: true,
      });
    });

    it('opens a document with pending invalid changes in the draft', () => {
      CmsService.closeDocumentWhenValid.and.returnValue($q.reject());

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).not.toHaveBeenCalled();
      expect(ContentEditor.getDocument()).toBeUndefined();
      expect(ContentEditor.getError()).toEqual({
        titleKey: 'FEEDBACK_DRAFT_INVALID_TITLE',
        messageKey: 'FEEDBACK_DRAFT_INVALID_MESSAGE',
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
        },
      };
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.reject({ data: response }));

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
      expect(ContentService.getDocumentType).not.toHaveBeenCalled();
      expect(ContentEditor.getDocument()).toBeUndefined();
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
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.reject({ data: response }));

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
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
        },
      };
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.reject({ data: response }));

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
      expect(ContentService.getDocumentType).not.toHaveBeenCalled();
      expect(ContentEditor.getDocument()).toBeUndefined();
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
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.reject({ data: response }));

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
      expect(ContentService.getDocumentType).not.toHaveBeenCalled();
      expect(ContentEditor.getDocument()).toBeUndefined();
      expect(ContentEditor.getError()).toEqual({
        titleKey: 'FEEDBACK_NOT_A_DOCUMENT_TITLE',
        messageKey: 'FEEDBACK_NOT_A_DOCUMENT_MESSAGE',
        linkToContentEditor: true,
      });
    });

    it('opens a non-existing document', () => {
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.reject({ status: 404 }));

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
      expectError({
        titleKey: 'FEEDBACK_NOT_FOUND_TITLE',
        messageKey: 'FEEDBACK_NOT_FOUND_MESSAGE',
        disableContentButtons: true,
      });
    });

    it('opens a document with random data in the response', () => {
      const response = { bla: 'test' };
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.reject({ data: response }));

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
      expectDefaultError();
    });

    it('opens a document with no data in the response', () => {
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.reject({}));

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
      expectDefaultError();
    });

    it('opens a document with an unknown error reason', () => {
      const response = {
        reason: 'unknown',
      };
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.reject({ data: response }));

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
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
      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.createDraft.and.returnValue($q.resolve(doc));
      ContentService.getDocumentType.and.returnValue($q.reject({}));

      ContentEditor.open('test');
      $rootScope.$digest();

      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
      expect(ContentService.createDraft).toHaveBeenCalledWith('test');
      expect(ContentService.getDocumentType).toHaveBeenCalledWith('document:type');
      expectDefaultError();
    });
  });

  it('marks a document dirty', () => {
    expect(ContentEditor.isDocumentDirty()).toBeFalsy();
    ContentEditor.markDocumentDirty();
    expect(ContentEditor.isDocumentDirty()).toBe(true);
  });

  it('saves a dirty document', () => {
    const savedDoc = {
      id: '123',
    };
    ContentService.saveDraft.and.returnValue($q.resolve(savedDoc));

    ContentEditor.document = testDocument;
    ContentEditor.markDocumentDirty();
    ContentEditor.save();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$digest();

    expect(ContentEditor.getDocument()).toEqual(savedDoc);
    expect(ContentEditor.isDocumentDirty()).toBeFalsy();
  });

  it('does not save a pristine document', () => {
    ContentEditor.document = testDocument;

    ContentEditor.save();
    $rootScope.$digest();

    expect(ContentService.saveDraft).not.toHaveBeenCalled();
  });

  describe('shows error feedback when saving a document', () => {
    it('fails', () => {
      const response = {
        reason: 'TEST',
      };
      ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

      ContentEditor.document = testDocument;
      ContentEditor.markDocumentDirty();
      ContentEditor.save();

      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

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
      ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

      ContentEditor.document = testDocument;
      ContentEditor.markDocumentDirty();
      ContentEditor.save();

      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

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
      ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

      ContentEditor.document = testDocument;
      ContentEditor.markDocumentDirty();
      ContentEditor.save();

      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

      $rootScope.$digest();

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_OTHER_HOLDER', { user: 'Joe Tester' });
    });

    describe('when document save fails because of an invalid field', () => {
    //   beforeEach(() => {
    //     const saveResponse = angular.copy(testDocument);
    //     saveResponse.fields['ns:string'] = [
    //       {
    //         value: '',
    //         errorInfo: {
    //           code: 'REQUIRED_FIELD_EMPTY',
    //         },
    //       },
    //     ];
    //
    //     ContentService.saveDraft.and.returnValue($q.reject({ data: saveResponse }));
    //
    //     ContentEditor.getDocument() = testDocument;
    //     ContentEditor.getDocumentType() = testDocumentType;
    //     ContentEditor.form.$dirty = true;
    //   });
    //
    //   it('shows an error and reloads the document type', () => {
    //     const reloadedDocumentType = angular.copy(testDocumentType);
    //     ContentService.getDocumentType.and.returnValue($q.resolve(reloadedDocumentType));
    //
    //     ContentEditor.saveDocument();
    //
    //     expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    //
    //     $rootScope.$digest();
    //
    //     expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_INVALID_DATA', {});
    //     expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
    //     expect(ContentEditor.getDocumentType()).toBe(reloadedDocumentType);
    //   });
    //
    //   it('shows an error when reloading the document type fails', () => {
    //     ContentService.getDocumentType.and.returnValue($q.reject({ status: 404 }));
    //
    //     ContentEditor.saveDocument();
    //
    //     expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    //
    //     $rootScope.$digest();
    //
    //     expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_INVALID_DATA', {});
    //     expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
    //     expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_NOT_FOUND_MESSAGE', {});
    //     expect(ContentEditor.getDocumentType()).toBe(testDocumentType);
    //   });
    // });
    //
    // it('shows an error when document save fails and there is no data returned', () => {
    //   ContentService.saveDraft.and.returnValue($q.reject({}));
    //
    //   ContentEditor.getDocument() = testDocument;
    //   ContentEditor.form.$dirty = true;
    //   ContentEditor.saveDocument();
    //
    //   expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    //
    //   $rootScope.$digest();
    //
    //   expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UNABLE_TO_SAVE', {});
    // });
    //
    // it('directly opens the content editor in a certain mode if the form is not dirty', () => {
    //   ContentEditor.documentId = 'test';
    //   SidePanelService.close.and.returnValue($q.resolve());
    //
    //   const mode = 'view';
    //   ContentEditor.openContentEditor(mode);
    //
    //   expect(ContentEditor.deleteDraftOnClose).toBe(false);
    //
    //   $rootScope.$digest();
    //
    //   expect(ContentService.saveDraft).not.toHaveBeenCalled();
    //   expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    //   expect(SidePanelService.close).toHaveBeenCalledWith('right');
    //   expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'test', mode);
    //   expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsContentPublish');
    });
  });
  //
  // it('can discard pending changes before opening the content editor', () => {
  //   DialogService.show.and.returnValue($q.resolve('DISCARD'));
  //   SidePanelService.close.and.callFake(() => {
  //     sidePanelHandlers.onClose();
  //     return $q.resolve();
  //   });
  //   ContentEditor.documentId = 'test';
  //   ContentEditor.getDocument() = { displayName: 'Display Name' };
  //   ContentEditor.form.$dirty = true;
  //
  //   ContentEditor.openContentEditor('edit');
  //
  //   expect(ContentEditor.deleteDraftOnClose).toBe(false);
  //
  //   $rootScope.$digest();
  //
  //   expect(DialogService.show).toHaveBeenCalled();
  //   expect(DialogService.show.calls.count()).toEqual(1);
  //   expect(ContentService.saveDraft).not.toHaveBeenCalled();
  //   expect(ContentService.deleteDraft).not.toHaveBeenCalled();
  //   expect(SidePanelService.close).toHaveBeenCalledWith('right');
  //   expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'test', 'edit');
  //   expect(CmsService.reportUsageStatistic).toHaveBeenCalledWith('CMSChannelsContentEditor');
  // });
  //
  // it('saves pending changes before opening the content editor', () => {
  //   DialogService.show.and.returnValue($q.resolve('SAVE'));
  //   ContentEditor.documentId = 'test';
  //   ContentEditor.getDocument() = testDocument;
  //   ContentEditor.form.$dirty = true;
  //   ContentService.saveDraft.and.returnValue($q.resolve(testDocument));
  //   SidePanelService.close.and.returnValue($q.resolve());
  //
  //   ContentEditor.openContentEditor('edit');
  //
  //   expect(ContentEditor.deleteDraftOnClose).toBe(false);
  //
  //   $rootScope.$digest();
  //
  //   expect(DialogService.show).toHaveBeenCalled();
  //   expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
  //   expect(ContentService.deleteDraft).not.toHaveBeenCalled();
  //   expect(SidePanelService.close).toHaveBeenCalledWith('right');
  //   expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'test', 'edit');
  //   expect(CmsService.reportUsageStatistic.calls.allArgs()).toEqual([
  //     ['CMSChannelsSaveDocument'],
  //     ['CMSChannelsContentEditor'],
  //   ]);
  // });
  //
  // it('releases holdership of the document when publishing it', () => {
  //   DialogService.show.and.returnValue($q.resolve('SAVE'));
  //   ContentEditor.documentId = 'documentId';
  //   ContentEditor.getDocument() = testDocument;
  //   ContentEditor.form.$dirty = true;
  //   ContentEditor.editing = true;
  //   ContentService.saveDraft.and.returnValue($q.resolve(testDocument));
  //   ContentService.deleteDraft.and.returnValue($q.resolve());
  //   SidePanelService.close.and.returnValue($q.resolve());
  //
  //   ContentEditor.openContentEditor('view');
  //   $rootScope.$digest();
  //
  //   expect(DialogService.show).toHaveBeenCalled();
  //   expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
  //   expect(ContentService.deleteDraft).toHaveBeenCalledWith('documentId');
  //   expect(SidePanelService.close).toHaveBeenCalledWith('right');
  //   expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'documentId', 'view');
  // });
  //
  // it('does not open the content editor if saving changes failed', () => {
  //   DialogService.show.and.returnValue($q.resolve('SAVE'));
  //   ContentEditor.documentId = 'documentId';
  //   ContentEditor.getDocument() = testDocument;
  //   ContentEditor.form.$dirty = true;
  //   ContentEditor.editing = true;
  //   ContentService.saveDraft.and.returnValue($q.reject({}));
  //
  //   ContentEditor.openContentEditor('view');
  //   $rootScope.$digest();
  //
  //   expect(DialogService.show).toHaveBeenCalled();
  //   expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
  //   expect(ContentService.deleteDraft).not.toHaveBeenCalled();
  //   expect(SidePanelService.close).not.toHaveBeenCalled();
  //   expect(CmsService.publish).not.toHaveBeenCalled();
  // });
  //
  // it('subscribes to the kill-editor event', () => {
  //   expect(CmsService.subscribe).toHaveBeenCalled();
  //   const onKillEditor = CmsService.subscribe.calls.mostRecent().args[1];
  //
  //   SidePanelService.close.and.returnValue($q.resolve());
  //   ContentEditor.documentId = 'documentId';
  //
  //   onKillEditor('differentId');
  //   expect(ContentEditor.deleteDraftOnClose).toBe(true);
  //   expect(SidePanelService.close).not.toHaveBeenCalled();
  //
  //   onKillEditor('documentId');
  //   expect(ContentEditor.deleteDraftOnClose).toBe(false);
  //   expect(SidePanelService.close).toHaveBeenCalled();
  // });
});

