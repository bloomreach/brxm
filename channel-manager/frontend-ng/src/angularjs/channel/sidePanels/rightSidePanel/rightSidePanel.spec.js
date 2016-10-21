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
    ContentService = jasmine.createSpyObj('ContentService', ['createDraft', 'getDocumentType']);

    $scope = $rootScope.$new();
    const $element = angular.element('<div></div>');
    $ctrl = $componentController('channelRightSidePanel', {
      $scope,
      $element,
      $timeout,
      ChannelSidePanelService,
      ContentService,
    }, {
      editMode: false,
    });
    $rootScope.$apply();
  });

  it('initializes the channel right side panel service upon instantiation', () => {
    expect(ChannelSidePanelService.initialize).toHaveBeenCalled();
    expect(ChannelSidePanelService.close).toHaveBeenCalled();
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

    $timeout.flush();
    expect($scope.$broadcast).toHaveBeenCalledWith('md-resize-textarea');
  });

  it('recognizes an empty multiple field', () => {
    $ctrl.docType = testDocumentType;
    $ctrl.doc = testDocument;

    expect($ctrl.isEmptyMultiple(stringField)).toBeFalsy();
    expect($ctrl.isEmptyMultiple(multipleStringField)).toBeFalsy();
    expect($ctrl.isEmptyMultiple(emptyMultipleStringField)).toBeTruthy();
  });

  it('can get a field as an array', () => {
    $ctrl.doc = testDocument;

    expect($ctrl.getFieldAsArray('ns:string')).toEqual(['String value']);
    expect($ctrl.getFieldAsArray('ns:multiplestring')).toEqual(['One', 'Two']);
    expect($ctrl.getFieldAsArray('ns:emptymultiplestring')).toEqual([]);
  });

  it('keeps track of the focused field', () => {
    expect($ctrl.isFieldFocused(stringField)).toBe(false);

    $ctrl.onFieldFocus(stringField);
    expect($ctrl.isFieldFocused(stringField)).toBe(true);

    $ctrl.onFieldFocus(multipleStringField);
    expect($ctrl.isFieldFocused(stringField)).toBe(false);
    expect($ctrl.isFieldFocused(multipleStringField)).toBe(true);

    $ctrl.onFieldBlur();
    expect($ctrl.isFieldFocused(multipleStringField)).toBe(false);
  });
});

