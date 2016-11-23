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
  let ChannelSidePanelService;
  let CmsService;
  let ContentService;
  let HippoIframeService;
  let FeedbackService;

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

    inject((_$componentController_, _$q_, _$rootScope_, _$timeout_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $timeout = _$timeout_;
    });

    ChannelSidePanelService = jasmine.createSpyObj('ChannelSidePanelService', ['initialize', 'isOpen', 'close']);
    ContentService = jasmine.createSpyObj('ContentService', ['createDraft', 'getDocumentType', 'saveDraft', 'deleteDraft']);
    FeedbackService = jasmine.createSpyObj('FeedbackService', ['showErrorResponse']);

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

  it('closes the panel', () => {
    ChannelSidePanelService.close.and.returnValue($q.resolve());
    $ctrl.close();
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');

    $ctrl.doc = testDocument;
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

  it('shows a toast when document save fails', () => {
    const response = {
      reason: 'TEST',
    };
    ContentService.saveDraft.and.returnValue($q.reject({ data: response }));

    $ctrl.doc = testDocument;
    $ctrl.form.$pristine = false;
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$apply();

    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(undefined, 'ERROR_TEST', undefined, $ctrl.$element);
  });

  it('shows a toast when document save fails and there is no data returned', () => {
    ContentService.saveDraft.and.returnValue($q.reject());

    $ctrl.doc = testDocument;
    $ctrl.form.$pristine = false;
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$apply();

    expect(FeedbackService.showErrorResponse).toHaveBeenCalledWith(undefined, 'ERROR_UNABLE_TO_SAVE', undefined, $ctrl.$element);
  });

  it('views the full content by saving changes, closing the panel and publishing a view-content event', () => {
    $ctrl.doc = testDocument;
    ContentService.saveDraft.and.returnValue($q.resolve(testDocument));
    ChannelSidePanelService.close.and.returnValue($q.resolve());

    $ctrl.viewFullContent();
    $rootScope.$digest();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
    expect(ContentService.deleteDraft).not.toHaveBeenCalled();
    expect(CmsService.publish).toHaveBeenCalledWith('view-content', testDocument.id);
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
    $ctrl.doc = testDocument;
    $ctrl.editFullContent();
    expect(CmsService.publish).toHaveBeenCalledWith('edit-content', testDocument.id);
  });
});

