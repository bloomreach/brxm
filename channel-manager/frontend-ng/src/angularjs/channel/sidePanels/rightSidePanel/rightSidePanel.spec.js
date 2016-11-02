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
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', ['reload']);

    $scope = $rootScope.$new();
    const $element = angular.element('<div></div>');
    $ctrl = $componentController('channelRightSidePanel', {
      $scope,
      $element,
      $timeout,
      ChannelSidePanelService,
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
    expect($ctrl.doc).toBe(null);
    expect($ctrl.docType).toBe(null);
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
    expect($ctrl.doc).toBe(null);
    expect($ctrl.docType).toBe(null);

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
    $ctrl.saveDocument();

    expect(ContentService.saveDraft).toHaveBeenCalledWith(testDocument);

    $rootScope.$apply();

    expect($ctrl.doc).toEqual(savedDoc);
    expect($ctrl.form.$setPristine).toHaveBeenCalled();
    expect(HippoIframeService.reload).toHaveBeenCalled();
  });
});

