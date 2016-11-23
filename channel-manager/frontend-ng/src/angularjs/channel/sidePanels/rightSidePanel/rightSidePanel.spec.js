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
  let HippoIframeService;

  let $ctrl;
  let $scope;

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
    CmsService = jasmine.createSpyObj('CmsService', ['publish']);
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);

    $scope = $rootScope.$new();
    const $element = angular.element('<div></div>');
    $ctrl = $componentController('channelRightSidePanel', {
      $scope,
      $element,
      $timeout,
      ChannelSidePanelService,
      CmsService,
      ContentService,
      HippoIframeService,
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

  it('closes the panel', () => {
    ChannelSidePanelService.close.and.returnValue($q.resolve());
    $ctrl.close();
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');

    $ctrl.documentId = 'test';
    $ctrl.editing = true;
    $ctrl.close();
    expect(ContentService.deleteDraft).toHaveBeenCalledWith('test');
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
  });

  it('opens a document', () => {
    ContentService.createDraft.and.returnValue($q.resolve(testDocument));
    ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));
    spyOn($scope, '$broadcast');

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    expect($ctrl.doc).not.toBeDefined();
    expect($ctrl.docType).not.toBeDefined();

    $rootScope.$apply();

    expect(ContentService.getDocumentType).toHaveBeenCalledWith('ns:testdocument');

    expect($ctrl.doc).toEqual(testDocument);
    expect($ctrl.docType).toEqual(testDocumentType);
    expect($ctrl.form.$setPristine).toHaveBeenCalled();

    $timeout.flush();
    expect($scope.$broadcast).toHaveBeenCalledWith('md-resize-textarea');
  });

  it('fails to open a document owned by another user', () => {
    const response = {
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

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    $rootScope.$digest();

    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBe(response);
    expect($ctrl.state).toBe('UNAVAILABLE_HELD_BY_OTHER_USER');
    expect($translate.instant).toHaveBeenCalledWith('UNAVAILABLE_HELD_BY_OTHER_USER', { user: 'John Tester' });
    expect($translate.instant).toHaveBeenCalledWith('EDIT_DOCUMENT', response);
  });

  it('falls back to the user\'s id if there is no display name', () => {
    const response = {
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

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    $rootScope.$digest();

    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBe(response);
    expect($ctrl.state).toBe('UNAVAILABLE_HELD_BY_OTHER_USER');
    expect($translate.instant).toHaveBeenCalledWith('UNAVAILABLE_HELD_BY_OTHER_USER', { user: 'tester' });
    expect($translate.instant).not.toHaveBeenCalledWith('EDIT_DOCUMENT', response);
  });

  it('fails to open a document with a publication request', () => {
    const response = {
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

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    $rootScope.$digest();

    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBe(response);
    expect($ctrl.state).toBe('UNAVAILABLE_REQUEST_PENDING');
    expect($translate.instant).toHaveBeenCalledWith('UNAVAILABLE_REQUEST_PENDING', { });
  });

  it('fails to open a document with random data in the response', () => {
    const response = { bla: 'test' };
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.reject({ data: response }));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    $rootScope.$digest();

    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBe(response);
    expect($ctrl.state).toBe('UNAVAILABLE_CONTENT');
    expect($translate.instant).toHaveBeenCalledWith('UNAVAILABLE_CONTENT', { });
  });

  it('fails to open a document with no data in the response', () => {
    spyOn($translate, 'instant');
    ContentService.createDraft.and.returnValue($q.reject({}));

    const onOpenCallback = ChannelSidePanelService.initialize.calls.mostRecent().args[2];
    onOpenCallback('test');

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    $rootScope.$digest();

    expect(ContentService.getDocumentType).not.toHaveBeenCalled();
    expect($ctrl.doc).toBeUndefined();
    expect($ctrl.state).toBe('UNAVAILABLE_CONTENT');
    expect($translate.instant).toHaveBeenCalledWith('UNAVAILABLE_CONTENT', { });
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

    expect(ContentService.createDraft).toHaveBeenCalledWith('test');
    $rootScope.$digest();

    expect(ContentService.getDocumentType).toHaveBeenCalledWith('document:type');
    expect($ctrl.doc).toBeUndefined();
    expect($ctrl.docType).toBeUndefined();
    expect($ctrl.state).toBe('UNAVAILABLE_CONTENT');
    expect($translate.instant).toHaveBeenCalledWith('UNAVAILABLE_CONTENT', { });
  });

  it('saves a document', () => {
    const savedDoc = {
      id: '123',
    };
    ContentService.saveDraft.and.returnValue($q.resolve(savedDoc));

    $ctrl.doc = testDocument;
    $ctrl.form.$pristine = false;
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$apply();

    expect($ctrl.doc).toEqual(savedDoc);
    expect($ctrl.form.$setPristine).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });

  it('does not save a document when there are no changes', () => {
    $ctrl.doc = testDocument;
    $ctrl.form.$pristine = true;

    $ctrl.saveDocument();
    $rootScope.$apply();

    expect(ContentService.saveDraft).not.toHaveBeenCalled();
  });

  it('views the full content by saving changes, closing the panel and publishing a view-content event', () => {
    $ctrl.documentId = 'test';
    $ctrl.doc = testDocument;
    ContentService.saveDraft.and.returnValue($q.resolve(testDocument));
    ChannelSidePanelService.close.and.returnValue($q.resolve());

    $ctrl.viewFullContent();
    $rootScope.$digest();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(CmsService.publish).toHaveBeenCalledWith('view-content', 'test');
  });

  it('does not view the full content if saving changes failed', () => {
    $ctrl.doc = testDocument;
    ContentService.saveDraft.and.returnValue($q.reject());

    $ctrl.viewFullContent();
    $rootScope.$digest();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(CmsService.publish).not.toHaveBeenCalled();
  });

  it('edits the full content by publishing an edit-content event', () => {
    $ctrl.documentId = 'test';
    $ctrl.editFullContent();
    expect(CmsService.publish).toHaveBeenCalledWith('edit-content', 'test');
  });
});

