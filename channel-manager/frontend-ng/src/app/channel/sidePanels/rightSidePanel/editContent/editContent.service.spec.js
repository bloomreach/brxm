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

describe('EditContentService', () => {
  let $q;
  let $rootScope;
  let $state;
  let $translate;
  let $window;
  let ContentEditor;
  let EditContentService;
  let RightSidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ContentEditor = jasmine.createSpyObj('ContentEditor', [
      'confirmClose', 'getDocument', 'getDocumentId', 'getError', 'kill', 'open',
    ]);
    RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', ['startLoading', 'stopLoading', 'setTitle']);

    angular.mock.module(($provide) => {
      $provide.value('ContentEditor', ContentEditor);
      $provide.value('RightSidePanelService', RightSidePanelService);
    });

    inject((_$q_, _$rootScope_, _$state_, _$translate_, _$window_, _EditContentService_) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $translate = _$translate_;
      $window = _$window_;
      EditContentService = _EditContentService_;
    });

    spyOn($translate, 'instant').and.callThrough();
  });

  function editDocument(document) {
    ContentEditor.open.and.returnValue($q.resolve());
    ContentEditor.getDocument.and.returnValue(document);
    ContentEditor.getDocumentId.and.returnValue(document.id);

    EditContentService.startEditing(document.id);
    $rootScope.$digest();
  }

  function editLockedDocument(documentId, error) {
    ContentEditor.open.and.returnValue($q.resolve());
    ContentEditor.getDocument.and.returnValue(undefined);
    ContentEditor.getError.and.returnValue(error);

    EditContentService.startEditing(documentId);
    $rootScope.$digest();
  }

  it('starts editing a document', () => {
    const document = {
      id: '42',
    };
    editDocument(document);

    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('EDIT_CONTENT');
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(ContentEditor.open).toHaveBeenCalledWith(document.id);
    expect($translate.instant).toHaveBeenCalledWith('EDIT_DOCUMENT', document);
    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('EDIT_DOCUMENT');
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
  });

  it('starts editing a locked document', () => {
    const documentId = '42';
    const error = {
      messageParams: {
        displayName: 'Locked document',
      },
    };
    editLockedDocument(documentId, error);

    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('EDIT_CONTENT');
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(ContentEditor.open).toHaveBeenCalledWith(documentId);
    expect($translate.instant).toHaveBeenCalledWith('EDIT_DOCUMENT', error.messageParams);
    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('EDIT_DOCUMENT');
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
  });

  it('stops editing', () => {
    spyOn($state, 'go');
    EditContentService.stopEditing();
    expect($state.go).toHaveBeenCalledWith('hippo-cm.channel');
  });

  describe('other editor opened', () => {
    beforeEach(() => {
      const document = {
        id: '42',
      };
      editDocument(document);
      spyOn($state, 'go').and.callThrough();
    });

    it('kills an open editor for the same document', () => {
      $window.CMS_TO_APP.publish('kill-editor', '42');

      expect(ContentEditor.kill).toHaveBeenCalled();
      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel');
    });

    it('does not kill an open editor for another document', () => {
      $window.CMS_TO_APP.publish('kill-editor', '1');

      expect(ContentEditor.kill).not.toHaveBeenCalled();
      expect($state.go).not.toHaveBeenCalled();
    });

    it('does not kill an open editor when another state is active', () => {
      EditContentService.stopEditing();
      $rootScope.$digest();

      $window.CMS_TO_APP.publish('kill-editor', '42');

      expect(ContentEditor.kill).not.toHaveBeenCalled();
    });
  });

  it('confirms closing the open editor when the channel is closed', () => {
    const document = {
      id: '42',
    };
    editDocument(document);
    ContentEditor.confirmClose.and.returnValue($q.resolve());

    $state.go('hippo-cm');
    $rootScope.$digest();

    expect(ContentEditor.confirmClose).toHaveBeenCalledWith('SAVE_CHANGES_ON_CLOSE_CHANNEL');
    expect($state.$current.name).toBe('hippo-cm');
  });

  it('does not close the editor when the channel is closed but confirming save or discard changes is canceled', () => {
    const document = {
      id: '42',
    };
    editDocument(document);
    ContentEditor.confirmClose.and.returnValue($q.reject());

    $state.go('hippo-cm');
    $rootScope.$digest();

    expect(ContentEditor.confirmClose).toHaveBeenCalledWith('SAVE_CHANGES_ON_CLOSE_CHANNEL');
    expect($state.$current.name).toBe('hippo-cm.channel.edit-content');
  });
});
