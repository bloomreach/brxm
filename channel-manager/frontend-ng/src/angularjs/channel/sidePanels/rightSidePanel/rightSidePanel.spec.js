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
  'use strict';

  let $componentController;
  let $q;
  let $rootScope;
  let $timeout;
  let ChannelSidePanelService;
  let ContentService;

  let $ctrl;
  let $scope;

  const testDocument = {
    id: 'test',
    info: {
      type: {
        id: 'ns:testdocument',
      },
    },
  };
  const testDocumentType = {
    id: 'ns:testdocument',
  };

  beforeEach(() => {
    module('hippo-cm');

    inject((_$componentController_, _$q_, _$rootScope_, _$timeout_) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $timeout = _$timeout_;
    });

    ChannelSidePanelService = jasmine.createSpyObj('ChannelSidePanelService', ['initialize', 'close']);
    ContentService = jasmine.createSpyObj('ContentService', ['getDocument', 'getDocumentType']);

    ContentService.getDocument.and.returnValue($q.resolve(testDocument));
    ContentService.getDocumentType.and.returnValue($q.resolve(testDocumentType));

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
    expect($ctrl.doc).toEqual(testDocument);
    expect($ctrl.docType).toEqual(testDocumentType);
  });

  it('closes the panel', () => {
    $ctrl.close();
    expect(ChannelSidePanelService.close).toHaveBeenCalledWith('right');
  });

  it('sets the initial size of textareas when opened', () => {
    spyOn($scope, '$broadcast');
    $ctrl.onOpen();
    $timeout.flush();
    expect($scope.$broadcast).toHaveBeenCalledWith('md-resize-textarea');
  });
});

