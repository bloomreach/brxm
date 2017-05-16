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

describe('RightSidePanel', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let $timeout;
  let $translate;
  let SidePanelService;
  let CmsService;
  let ContentService;
  let DialogService;
  let HippoIframeService;
  let FeedbackService;

  let $ctrl;
  let $scope;
  let dialog;

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

    inject((_$componentController_, _$q_, _$rootScope_, _$timeout_, _$translate_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $timeout = _$timeout_;
      $translate = _$translate_;
    });

    SidePanelService = jasmine.createSpyObj('SidePanelService', ['initialize', 'isOpen', 'close']);
    ContentService = jasmine.createSpyObj('ContentService', ['createDraft', 'getDocumentType', 'saveDraft', 'deleteDraft']);
    FeedbackService = jasmine.createSpyObj('FeedbackService', ['showError']);

    CmsService = jasmine.createSpyObj('CmsService', ['closeDocumentWhenValid', 'publish', 'subscribe']);
    DialogService = jasmine.createSpyObj('DialogService', ['confirm', 'show']);
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);

    dialog = jasmine.createSpyObj('dialog', ['textContent', 'ok', 'cancel']);
    dialog.textContent.and.returnValue(dialog);
    dialog.ok.and.returnValue(dialog);
    dialog.cancel.and.returnValue(dialog);
    DialogService.confirm.and.returnValue(dialog);

    spyOn($translate, 'instant').and.callThrough();

    $scope = $rootScope.$new();
    const $element = angular.element('<div></div>');
    $ctrl = $componentController('rightSidePanel', {
      $scope,
      $element,
      $timeout,
      SidePanelService,
      CmsService,
      ContentService,
      DialogService,
      HippoIframeService,
      FeedbackService,
    });
    $ctrl.form = jasmine.createSpyObj('form', ['$setPristine']);
    $rootScope.$apply();
  });

  it('initializes the channel right side panel service upon instantiation', () => {
    expect(SidePanelService.initialize).toHaveBeenCalled();
    expect($ctrl.doc).not.toBeDefined();
    expect($ctrl.docType).not.toBeDefined();
  });

  it('knows when it is locked open', () => {
    SidePanelService.isOpen.and.returnValue(true);
    expect($ctrl.isLockedOpen()).toBe(true);
  });

  it('knows when it is not locked open', () => {
    SidePanelService.isOpen.and.returnValue(false);
    expect($ctrl.isLockedOpen()).toBe(false);
  });

  it('shows the correct close button label', () => {
    $ctrl.closeLabel = 'Close';
    $ctrl.cancelLabel = 'Cancel';
    expect($ctrl.closeButtonLabel()).toBe('Close');

    $ctrl.form.$dirty = true;
    expect($ctrl.closeButtonLabel()).toBe('Cancel');

    $ctrl.form.$dirty = false;
    expect($ctrl.closeButtonLabel()).toBe('Close');

    delete $ctrl.form;
    expect($ctrl.closeButtonLabel()).toBe('Close');
  });

  it('closes the panel', () => {
    SidePanelService.close.and.returnValue($q.resolve());
    $ctrl.close();
    $rootScope.$digest();
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(SidePanelService.close).toHaveBeenCalledWith('right');

    $ctrl.documentId = 'test';
    $ctrl.editing = true;
    $ctrl.close();
    $rootScope.$digest();
    expect(ContentService.deleteDraft).toHaveBeenCalledWith('test');
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect($ctrl.doc).toBeUndefined();
    expect($ctrl.documentId).toBeUndefined();
    expect($ctrl.docType).toBeUndefined();
    expect($ctrl.editing).toBeUndefined();
    expect($ctrl.feedback).toBeUndefined();
    expect($ctrl.disableContentButtons).toBeUndefined();
    expect($ctrl.title).toBe($ctrl.defaultTitle);
    expect($ctrl.form.$setPristine).toHaveBeenCalled();
  });

  it('asks for confirmation when cancelling changes', () => {
    DialogService.show.and.returnValue($q.resolve());
    SidePanelService.close.and.returnValue($q.resolve());
    $ctrl.doc = {
      displayName: 'test',
    };
    $ctrl.documentId = 'test';
    $ctrl.form.$dirty = true;
    $ctrl.editing = true;

    $ctrl.close();
    $rootScope.$digest();

    expect(ContentService.deleteDraft).toHaveBeenCalledWith('test');
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE', {
      documentName: 'test',
    });
    expect($ctrl.editing).toBeFalsy();
  });

  it('asks doesn\'t delete and close if discarding is not confirmed', () => {
    DialogService.show.and.returnValue($q.reject());
    $ctrl.doc = {};
    $ctrl.documentId = 'test';
    $ctrl.form.$dirty = true;
    $ctrl.close();
    $rootScope.$digest();

    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(SidePanelService.close).not.toHaveBeenCalled();
  });

  it('opens a document', () => {
    testDocument.displayName = 'Display Name';
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.resolve(testDocument));
    ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
    spyOn($scope, '$broadcast');

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');

    expect($ctrl.doc).toEqual(testDocument);
    expect($ctrl.docType).toEqual(testDocumentType);
    expect($ctrl.form.$setPristine).toHaveBeenCalled();
    expect($translate.instant).toHaveBeenCalledWith('EDIT_DOCUMENT', testDocument);

    $timeout.flush();
    expect($scope.$broadcast).toHaveBeenCalledWith('md-resize-textarea');
    delete testDocument.displayName;
  });

  it('opens a document with no display name', () => {
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.resolve(testDocument));
    ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
    spyOn($scope, '$broadcast');

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');

    expect($ctrl.doc).toEqual(testDocument);
    expect($ctrl.docType).toEqual(testDocumentType);
    expect($ctrl.form.$setPristine).toHaveBeenCalled();
    expect($translate.instant).not.toHaveBeenCalledWith('EDIT_DOCUMENT', testDocument);

    $timeout.flush();
    expect($scope.$broadcast).toHaveBeenCalledWith('md-resize-textarea');
  });

  it('opens a document without content', () => {
    const emptyDocument = {
      id: 'test',
      displayName: 'Display Name',
      info: {
        type: { id: 'ns:testdocument' },
      },
      fields: { },
    };
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.resolve(emptyDocument));

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');

    expect($ctrl.doc).toBeUndefined();
    expect($ctrl.docType).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('EDIT_DOCUMENT', { displayName: 'Display Name' });
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_NO_EDITABLE_CONTENT_MESSAGE', { });
  });

  it('ignores a non-existing form when opening a document', () => {
    ContentService.createDraft.and.returnValue($q.resolve(testDocument));
    ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
    delete $ctrl.form;

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];

    expect(() => {
      onOpenCallback('test');
      $rootScope.$digest();
    }).not.toThrow();
  });

  it('knows that a document is loading', () => {
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());

    const deferredDraft = $q.defer();
    ContentService.createDraft.and.returnValue(deferredDraft.promise);

    const deferredDocType = $q.defer();
    ContentService.getDocumentType.and.returnValue(deferredDocType.promise);

    $ctrl._loadDocument('test');
    expect($ctrl.loading).toBeTruthy();

    $rootScope.$digest();
    expect($ctrl.loading).toBeTruthy();

    deferredDraft.resolve(testDocument);
    $rootScope.$digest();
    expect($ctrl.loading).toBeTruthy();

    deferredDocType.resolve(testDocumentType);
    $rootScope.$digest();
    expect($ctrl.loading).toBeFalsy();
  });

  describe('with an existing document', () => {
    const newDocument = {
      id: 'newdoc',
      info: {
        type: {
          id: 'ns:newdoctype',
        },
      },
      fields: {
        dummy: 'value',
      },
    };
    const newDocumentType = {
      id: 'ns:newdoctype',
      fields: [
        {
          id: 'dummy',
        },
      ],
    };
    let onOpenCallback;

    beforeEach(() => {
      $ctrl.documentId = 'documentId';
      $ctrl.doc = testDocument;
      $ctrl.docType = testDocumentType;
      $ctrl.editing = true;

      CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
      ContentService.saveDraft.and.returnValue($q.resolve(testDocument));
      ContentService.createDraft.and.returnValue($q.resolve(newDocument));
      ContentService.deleteDraft.and.returnValue($q.resolve());
      ContentService.getDocumentType.and.returnValue($q.resolve(newDocumentType));
      spyOn($scope, '$broadcast');

      onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    });

    function expectNewDocument() {
      expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('newdoc');
      expect(ContentService.createDraft).toHaveBeenCalledWith('newdoc');
      expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:newdoctype');

      expect($ctrl.doc).toEqual(newDocument);
      expect($ctrl.docType).toEqual(newDocumentType);
      expect($ctrl.form.$setPristine).toHaveBeenCalled();

      $timeout.flush();
      expect($scope.$broadcast).toHaveBeenCalledWith('md-resize-textarea');
    }

    it('can save pending changes before opening a new document', () => {
      $ctrl.form.$dirty = true;
      DialogService.show.and.returnValue($q.resolve('SAVE'));

      onOpenCallback('newdoc');
      $rootScope.$digest();

      expect(DialogService.show).toHaveBeenCalled();
      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
      expect(ContentService.deleteDraft).toHaveBeenCalledWith('documentId');
      expectNewDocument();
    });

    it('does not open the new document when saving pending changes in the old document failed', () => {
      $ctrl.form.$dirty = true;
      DialogService.show.and.returnValue($q.resolve('SAVE'));
      ContentService.saveDraft.and.returnValue($q.reject({}));

      onOpenCallback('newdoc');
      $rootScope.$digest();

      expect(DialogService.show).toHaveBeenCalled();
      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
      expect(ContentService.deleteDraft).not.toHaveBeenCalled();
      expect(ContentService.createDraft).not.toHaveBeenCalled();

      expect($ctrl.doc).toEqual(testDocument);
      expect($ctrl.docType).toEqual(testDocumentType);
    });

    it('can discard pending changes to an existing document before opening a new document', () => {
      $ctrl.form.$dirty = true;
      DialogService.show.and.returnValue($q.resolve('DISCARD'));

      onOpenCallback('newdoc');
      $rootScope.$digest();

      expect(DialogService.show).toHaveBeenCalled();
      expect(ContentService.saveDraft).not.toHaveBeenCalled();
      expect(ContentService.deleteDraft).toHaveBeenCalledWith('documentId');
      expectNewDocument();
    });

    it('does not change state when cancelling the reload of a document', () => {
      $ctrl.form.$dirty = true;
      DialogService.show.and.returnValue($q.reject()); // Say Cancel

      onOpenCallback('newdoc');
      $rootScope.$digest();

      expect(DialogService.show).toHaveBeenCalled();
      expect(ContentService.saveDraft).not.toHaveBeenCalled();
      expect(ContentService.deleteDraft).not.toHaveBeenCalled();
      expect(ContentService.createDraft).not.toHaveBeenCalled();
    });

    it('does not save pending changes when there are none', () => {
      $ctrl.form.$dirty = false;

      onOpenCallback('newdoc');
      $rootScope.$digest();

      expect(DialogService.show).not.toHaveBeenCalled();
      expect(ContentService.saveDraft).not.toHaveBeenCalled();
      expect(ContentService.deleteDraft).toHaveBeenCalledWith('documentId');
      expectNewDocument();
    });
  });

  it('fails to open a document with pending invalid changes in the draft', () => {
    CmsService.closeDocumentWhenValid.and.returnValue($q.reject());

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_DRAFT_INVALID_MESSAGE');
  });

  it('fails to open a document owned by another user', () => {
    const response = {
      reason: 'OTHER_HOLDER',
      params: {
        userId: 'jtester',
        userName: 'John Tester',
        displayName: 'Display Name',
      },
    };
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_HELD_BY_OTHER_USER_MESSAGE', { user: 'John Tester' });
    expect($translate.instant).toHaveBeenCalledWith('EDIT_DOCUMENT', response.params);
  });

  it('falls back to the user\'s id if there is no display name', () => {
    const response = {
      reason: 'OTHER_HOLDER',
      params: {
        userId: 'tester',
      },
    };
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_HELD_BY_OTHER_USER_MESSAGE', { user: 'tester' });
    expect($translate.instant).not.toHaveBeenCalledWith('EDIT_DOCUMENT', response.params);
  });

  it('fails to open a document with a publication request', () => {
    const response = {
      reason: 'REQUEST_PENDING',
      params: {
        displayName: 'Display Name',
      },
    };
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_REQUEST_PENDING_MESSAGE', { });
  });

  it('fails to open a document which is not a document', () => {
    const response = {
      reason: 'NOT_A_DOCUMENT',
    };
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($ctrl.disableContentButtons).toBeFalsy();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_NOT_A_DOCUMENT_MESSAGE', { });
  });

  it('fails to open a non-existent document', () => {
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.reject({ status: 404 }));

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($ctrl.disableContentButtons).toBeTruthy();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_NOT_FOUND_MESSAGE', { });
  });

  it('fails to open a document with random data in the response', () => {
    const response = { bla: 'test' };
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_DEFAULT_MESSAGE', { });
  });

  it('fails to open a document with no data in the response', () => {
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.reject({}));

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_DEFAULT_MESSAGE', { });
  });

  it('fails to open a document with an unknown error reason', () => {
    const response = {
      reason: 'unknown',
    };
    CmsService.closeDocumentWhenValid.and.returnValue($q.resolve());
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($translate.instant).not.toHaveBeenCalledWith('FEEDBACK_DEFAULT_MESSAGE', { });
  });

  it('fails to open a document with no type', () => {
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

    const onOpenCallback = SidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(CmsService.closeDocumentWhenValid).toHaveBeenCalledWith('test');
    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).toHaveBeenCalledWith('document:type');
    expect($ctrl.doc).toBeUndefined();
    expect($ctrl.docType).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_DEFAULT_MESSAGE', { });
  });

  it('saves a document', () => {
    const savedDoc = {
      id: '123',
    };
    ContentService.saveDraft.and.returnValue($q.resolve(savedDoc));

    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$digest();

    expect($ctrl.doc).toEqual(savedDoc);
    expect($ctrl.form.$setPristine).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('does not save a document when there are no changes', () => {
    $ctrl.doc = testDocument;

    $ctrl.saveDocument();
    $rootScope.$digest();

    expect(ContentService.saveDraft).not.toHaveBeenCalled();
  });

  it('shows an error when document save fails', () => {
    const response = {
      reason: 'TEST',
    };
    ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_TEST', {});
  });

  it('shows an error when document save fails due to other user now being the holder', () => {
    const response = {
      reason: 'OTHER_HOLDER',
      params: {
        userId: 'tester',
      },
    };
    ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_OTHER_HOLDER', { user: 'tester' });
  });

  it('shows an error when document save fails due to other *named* user now being the holder', () => {
    const response = {
      reason: 'OTHER_HOLDER',
      params: {
        userId: 'tester',
        userName: 'Joe Tester',
      },
    };
    ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_OTHER_HOLDER', { user: 'Joe Tester' });
  });

  describe('when document save fails because of an invalid field', () => {
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

      ContentService.saveDraft.and.returnValue($q.reject({ data: saveResponse }));

      $ctrl.doc = testDocument;
      $ctrl.docType = testDocumentType;
      $ctrl.form.$dirty = true;
    });

    it('shows an error and reloads the document type', () => {
      const reloadedDocumentType = angular.copy(testDocumentType);
      ContentService.getDocumentType.and.returnValue($q.resolve(reloadedDocumentType));

      $ctrl.saveDocument();

      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

      $rootScope.$digest();

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_INVALID_DATA', {});
      expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
      expect($ctrl.docType).toBe(reloadedDocumentType);
    });

    it('shows an error when reloading the document type fails', () => {
      ContentService.getDocumentType.and.returnValue($q.reject({ status: 404 }));

      $ctrl.saveDocument();

      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

      $rootScope.$digest();

      expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_INVALID_DATA', {});
      expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');
      expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_NOT_FOUND_MESSAGE', {});
      expect($ctrl.docType).toBe(testDocumentType);
    });
  });

  it('shows an error when document save fails and there is no data returned', () => {
    ContentService.saveDraft.and.returnValue($q.reject({}));

    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UNABLE_TO_SAVE', {});
  });

  it('directly opens the full content in a certain mode if the form is not dirty', () => {
    $ctrl.documentId = 'test';
    SidePanelService.close.and.returnValue($q.resolve());

    const mode = 'view';
    $ctrl.openFullContent(mode);
    $rootScope.$digest();

    expect(ContentService.saveDraft).not.toHaveBeenCalled();
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'test', mode);
  });

  it('can discard pending changes before opening the full content', () => {
    DialogService.show.and.returnValue($q.resolve('DISCARD'));
    SidePanelService.close.and.returnValue($q.resolve());
    $ctrl.documentId = 'test';
    $ctrl.doc = { displayName: 'Display Name' };
    $ctrl.form.$dirty = true;

    $ctrl.openFullContent('edit');
    $rootScope.$digest();

    expect(DialogService.show).toHaveBeenCalled();
    expect(ContentService.saveDraft).not.toHaveBeenCalled();
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'test', 'edit');
  });

  it('saves pending changes before opening the full content', () => {
    DialogService.show.and.returnValue($q.resolve('SAVE'));
    $ctrl.documentId = 'test';
    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    ContentService.saveDraft.and.returnValue($q.resolve(testDocument));
    SidePanelService.close.and.returnValue($q.resolve());

    $ctrl.openFullContent('edit');
    $rootScope.$digest();

    expect(DialogService.show).toHaveBeenCalled();
    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'test', 'edit');
  });

  it('releases holdership of the document when publishing it', () => {
    DialogService.show.and.returnValue($q.resolve('SAVE'));
    $ctrl.documentId = 'documentId';
    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    $ctrl.editing = true;
    ContentService.saveDraft.and.returnValue($q.resolve(testDocument));
    ContentService.deleteDraft.and.returnValue($q.resolve());
    SidePanelService.close.and.returnValue($q.resolve());

    $ctrl.openFullContent('view');
    $rootScope.$digest();

    expect(DialogService.show).toHaveBeenCalled();
    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    expect(ContentService.deleteDraft).toHaveBeenCalledWith('documentId');
    expect(SidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'documentId', 'view');
  });

  it('does not open the full content if saving changes failed', () => {
    DialogService.show.and.returnValue($q.resolve('SAVE'));
    $ctrl.documentId = 'documentId';
    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    $ctrl.editing = true;
    ContentService.saveDraft.and.returnValue($q.reject({}));

    $ctrl.openFullContent('view');
    $rootScope.$digest();

    expect(DialogService.show).toHaveBeenCalled();
    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(SidePanelService.close).not.toHaveBeenCalled();
    expect(CmsService.publish).not.toHaveBeenCalled();
  });

  it('subscribes to the kill-editor event', () => {
    expect(CmsService.subscribe).toHaveBeenCalled();
    const onKillEditor = CmsService.subscribe.calls.mostRecent().args[1];

    SidePanelService.close.and.returnValue($q.resolve());
    $ctrl.documentId = 'documentId';

    onKillEditor('differentId');
    expect(SidePanelService.close).not.toHaveBeenCalled();

    onKillEditor('documentId');
    expect(SidePanelService.close).toHaveBeenCalled();
  });
});

