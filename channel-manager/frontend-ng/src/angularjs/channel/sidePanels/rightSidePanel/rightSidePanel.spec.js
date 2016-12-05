/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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

describe('ChannelRightSidePanel', () => {
  let $componentController;
  let $q;
  let $rootScope;
  let $timeout;
  let $translate;
  let ChannelSidePanelService;
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
      editing: {
        state: 'AVAILABLE',
      },
    },
    fields: {
      'ns:string': 'String value',
      'ns:multiplestring': ['One', 'Two'],
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

    ChannelSidePanelService = jasmine.createSpyObj('ChannelSidePanelService', ['initialize', 'isOpen', 'close']);
    ContentService = jasmine.createSpyObj('ContentService', ['createDraft', 'getDocumentType', 'saveDraft', 'deleteDraft']);
    FeedbackService = jasmine.createSpyObj('FeedbackService', ['showError']);

    CmsService = jasmine.createSpyObj('CmsService', ['publish']);
    DialogService = jasmine.createSpyObj('DialogService', ['confirm', 'show']);
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);

    dialog = jasmine.createSpyObj('dialog', ['textContent', 'ok', 'cancel']);
    dialog.textContent.and.returnValue(dialog);
    dialog.ok.and.returnValue(dialog);
    dialog.cancel.and.returnValue(dialog);
    DialogService.confirm.and.returnValue(dialog);

    $scope = $rootScope.$new();
    const $element = angular.element('<div></div>');
    $ctrl = $componentController('channelRightSidePanel', {
      $scope,
      $element,
      $timeout,
      ChannelSidePanelService,
      CmsService,
      ContentService,
      DialogService,
      HippoIframeService,
      FeedbackService,
    }, {
      editMode: false,
    });
    $ctrl.form = jasmine.createSpyObj('form', ['$setPristine']);
    $rootScope.$apply();
  });

  it('initializes the channel right side panel service upon instantiation', () => {
    expect(ChannelSidePanelService.initialize).toHaveBeenCalled();
    expect($ctrl.doc).not.toBeDefined();
    expect($ctrl.docType).not.toBeDefined();
  });

  it('knows when it is locked open', () => {
    ChannelSidePanelService.isOpen.and.returnValue(true);
    expect($ctrl.isLockedOpen()).toBe(true);
  });

  it('knows when it is not locked open', () => {
    ChannelSidePanelService.isOpen.and.returnValue(false);
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
    ChannelSidePanelService.close.and.returnValue($q.resolve());
    $ctrl.close();
    $rootScope.$digest();
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');

    $ctrl.documentId = 'test';
    $ctrl.editing = true;
    $ctrl.close();
    $rootScope.$digest();
    expect(ContentService.deleteDraft).toHaveBeenCalledWith('test');
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
  });

  it('asks for confirmation when cancelling changes', () => {
    spyOn($translate, 'instant');

    DialogService.show.and.returnValue($q.resolve());
    ChannelSidePanelService.close.and.returnValue($q.resolve());
    $ctrl.doc = {
      displayName: 'test',
    };
    $ctrl.documentId = 'test';
    $ctrl.form.$dirty = true;
    $ctrl.editing = true;

    $ctrl.close();
    $rootScope.$digest();

    expect(ContentService.deleteDraft).toHaveBeenCalledWith('test');
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
    expect($translate.instant).toHaveBeenCalledWith('CONFIRM_DISCARD_UNSAVED_CHANGES_MESSAGE', {
      documentName: 'test',
    });
  });

  it('asks doesn\'t delete and close if discarding is not confirmed', () => {
    DialogService.show.and.returnValue($q.reject());
    $ctrl.doc = {};
    $ctrl.documentId = 'test';
    $ctrl.form.$dirty = true;
    $ctrl.close();
    $rootScope.$digest();

    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(ChannelSidePanelService.close).not.toHaveBeenCalled();
  });

  it('opens a document', () => {
    ContentService.createDraft.and.returnValue($q.resolve(testDocument));
    ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
    spyOn($scope, '$broadcast');

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');

    expect($ctrl.doc).toEqual(testDocument);
    expect($ctrl.docType).toEqual(testDocumentType);
    expect($ctrl.form.$setPristine).toHaveBeenCalled();

    $timeout.flush();
    expect($scope.$broadcast).toHaveBeenCalledWith('md-resize-textarea');
  });

  it('knows that a document is loading', () => {
    ContentService.createDraft.and.returnValue($q.resolve(testDocument));
    ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));

    $ctrl._loadDocument('test');

    expect($ctrl.loading).toBeTruthy();

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
        editing: {
          state: 'AVAILABLE',
        },
      },
    };
    const newDocumentType = {
      id: 'ns:newdoctype',
    };
    let onOpenCallback;

    beforeEach(() => {
      $ctrl.doc = testDocument;
      $ctrl.docType = testDocumentType;

      ContentService.saveDraft.and.returnValue($q.resolve(testDocument));
      ContentService.createDraft.and.returnValue($q.resolve(newDocument));
      ContentService.getDocumentType.and.returnValue($q.resolve(newDocumentType));
      spyOn($scope, '$broadcast');

      onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    });

    function expectNewDocument() {
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
      DialogService.show.and.returnValue($q.resolve()); // Say 'Save'

      onOpenCallback('newdoc');
      $rootScope.$digest();

      expect(DialogService.show).toHaveBeenCalledWith(dialog);
      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
      expectNewDocument();
    });

    it('does not open the new document when saving pending changes in the old document failed', () => {
      $ctrl.form.$dirty = true;
      DialogService.show.and.returnValue($q.resolve()); // Say 'Save'
      ContentService.saveDraft.and.returnValue($q.reject({}));

      onOpenCallback('newdoc');
      $rootScope.$digest();

      expect(DialogService.show).toHaveBeenCalledWith(dialog);
      expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
      expect(ContentService.createDraft).not.toHaveBeenCalled();

      expect($ctrl.doc).toEqual(testDocument);
      expect($ctrl.docType).toEqual(testDocumentType);
    });

    it('can discard pending changes to an existing document before opening a new document', () => {
      $ctrl.form.$dirty = true;
      DialogService.show.and.returnValue($q.reject()); // Say 'Discard'

      onOpenCallback('newdoc');
      $rootScope.$digest();

      expect(DialogService.show).toHaveBeenCalledWith(dialog);
      expect(ContentService.saveDraft).not.toHaveBeenCalled();
      expectNewDocument();
    });

    it('does not save pending changes when there are none', () => {
      $ctrl.form.$dirty = false;

      onOpenCallback('newdoc');
      $rootScope.$digest();

      expect(DialogService.show).not.toHaveBeenCalled();
      expect(ContentService.saveDraft).not.toHaveBeenCalled();
      expectNewDocument();
    });
  });

  it('fails to open a document owned by another user', () => {
    const response = {
      id: 'test-id',
      info: {
        editing: {
          state: 'UNAVAILABLE_HELD_BY_OTHER_USER',
          holder: {
            displayName: 'John Tester',
          },
        },
      },
      displayName: 'Document Display Name',
    };
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBe(response);
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_HELD_BY_OTHER_USER_MESSAGE', { user: 'John Tester' });
    expect($translate.instant).toHaveBeenCalledWith('EDIT_DOCUMENT', response);
  });

  it('falls back to the user\'s id if there is no display name', () => {
    const response = {
      id: 'test-id',
      info: {
        editing: {
          state: 'UNAVAILABLE_HELD_BY_OTHER_USER',
          holder: {
            id: 'tester',
          },
        },
      },
    };
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBe(response);
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_HELD_BY_OTHER_USER_MESSAGE', { user: 'tester' });
    expect($translate.instant).not.toHaveBeenCalledWith('EDIT_DOCUMENT', response);
  });

  it('fails to open a document with a publication request', () => {
    const response = {
      id: 'test-id',
      info: {
        editing: {
          state: 'UNAVAILABLE_REQUEST_PENDING',
        },
      },
      displayName: 'Document Display Name',
    };
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBe(response);
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_REQUEST_PENDING_MESSAGE', { });
  });

  it('fails to open a document which is not a document', () => {
    const response = {
      reason: 'NOT_A_DOCUMENT',
    };
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($ctrl.disableContentButtons).toBeFalsy();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_NOT_A_DOCUMENT_MESSAGE', { });
  });

  it('fails to open a non-existent document', () => {
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.reject({ status: 404 }));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($ctrl.disableContentButtons).toBeTruthy();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_NOT_FOUND_MESSAGE', { });
  });

  it('fails to open a document with random data in the response', () => {
    const response = { bla: 'test' };
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_DEFAULT_MESSAGE', { });
  });

  it('fails to open a document with no data in the response', () => {
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.reject({}));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($translate.instant).toHaveBeenCalledWith('FEEDBACK_DEFAULT_MESSAGE', { });
  });

  it('fails to open a document with no type', () => {
    const response = {
      info: {
        editing: {
          state: 'AVAILABLE',
        },
        type: {
          id: 'document:type',
        },
      },
      displayName: 'Document Display Name',
    };
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.resolve(response));
    ContentService.getDocumentType.and.returnValue($q.reject({}));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');
    $rootScope.$digest();

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

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_TEST', {}, $ctrl.$element);
  });

  it('shows an error when document save fails and there is no data returned', () => {
    ContentService.saveDraft.and.returnValue($q.reject({}));

    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$digest();

    expect(FeedbackService.showError).toHaveBeenCalledWith('ERROR_UNABLE_TO_SAVE', {}, $ctrl.$element);
  });

  it('directly opens the full content in a certain mode if the form is not dirty', () => {
    $ctrl.documentId = 'test';
    ChannelSidePanelService.close.and.returnValue($q.resolve());

    const mode = 'view';
    $ctrl.openFullContent(mode);
    $rootScope.$digest();

    expect(ContentService.saveDraft).not.toHaveBeenCalled();
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'test', mode);
  });

  it('can discard pending changes before opening the full content', () => {
    DialogService.show.and.returnValue($q.reject()); // Say 'Discard'
    ChannelSidePanelService.close.and.returnValue($q.resolve());
    $ctrl.documentId = 'test';
    $ctrl.doc = { displayName: 'Display Name' };
    $ctrl.form.$dirty = true;

    $ctrl.openFullContent('edit');
    $rootScope.$digest();

    expect(DialogService.show).toHaveBeenCalledWith(dialog);
    expect(ContentService.saveDraft).not.toHaveBeenCalled();
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'test', 'edit');
  });

  it('saves pending changes before opening the full content', () => {
    DialogService.show.and.returnValue($q.resolve()); // Say 'Save'
    $ctrl.documentId = 'test';
    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    ContentService.saveDraft.and.returnValue($q.resolve(testDocument));
    ChannelSidePanelService.close.and.returnValue($q.resolve());

    $ctrl.openFullContent('edit');
    $rootScope.$digest();

    expect(DialogService.show).toHaveBeenCalledWith(dialog);
    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
    expect(CmsService.publish).toHaveBeenCalledWith('open-content', 'test', 'edit');
  });

  it('does not open the full content if saving changes failed', () => {
    DialogService.show.and.returnValue($q.resolve()); // Say 'Save'
    $ctrl.doc = testDocument;
    $ctrl.form.$dirty = true;
    ContentService.saveDraft.and.returnValue($q.reject({}));

    $ctrl.openFullContent('view');
    $rootScope.$digest();

    expect(DialogService.show).toHaveBeenCalledWith(dialog);
    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(ChannelSidePanelService.close).not.toHaveBeenCalled();
    expect(CmsService.publish).not.toHaveBeenCalled();
  });
});

