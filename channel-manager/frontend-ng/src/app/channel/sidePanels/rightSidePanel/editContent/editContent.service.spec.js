/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
  let ConfigService;
  let ContentEditor;
  let ContentService;
  let EditContentService;
  let HippoIframeService;
  let ProjectService;
  let RightSidePanelService;

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    ContentEditor = jasmine.createSpyObj('ContentEditor', [
      'confirmClose',
      'close',
      'confirmSaveOrDiscardChanges',
      'discardChanges',
      'getDocument',
      'getDocumentId',
      'getDocumentDisplayName',
      'getError',
      'kill',
      'open',
      'reload',
    ]);
    ContentService = jasmine.createSpyObj('ContentService', ['getDocument', 'branchDocument']);
    RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', [
      'clearContext',
      'setContext',
      'setTitle',
      'startLoading',
      'stopLoading',
    ]);
    HippoIframeService = jasmine.createSpyObj('HippoIframeService', [
      'reload',
    ]);

    angular.mock.module(($provide) => {
      $provide.value('ContentEditor', ContentEditor);
      $provide.value('ContentService', ContentService);
      $provide.value('HippoIframeService', HippoIframeService);
      $provide.value('RightSidePanelService', RightSidePanelService);
    });

    inject((
      _$q_,
      _$rootScope_,
      _$state_,
      _$translate_,
      _$window_,
      _ConfigService_,
      _EditContentService_,
      _ProjectService_,
    ) => {
      $q = _$q_;
      $rootScope = _$rootScope_;
      $state = _$state_;
      $translate = _$translate_;
      $window = _$window_;
      ConfigService = _ConfigService_;
      EditContentService = _EditContentService_;
      ProjectService = _ProjectService_;
    });

    spyOn($translate, 'instant').and.callThrough();
  });

  function editDocument(document) {
    ContentEditor.open.and.returnValue($q.resolve(document));
    ContentEditor.getDocument.and.returnValue(document);
    ContentEditor.getDocumentId.and.returnValue(document.id);

    EditContentService.startEditing(document.id);
    $rootScope.$digest();
  }

  function editPage(document) {
    ContentEditor.open.and.returnValue($q.resolve(document));
    ContentEditor.getDocument.and.returnValue(document);
    ContentEditor.getDocumentId.and.returnValue(document.id);

    EditContentService.startEditing(document.id, 'hippo-cm.channel.edit-page.content');
    $rootScope.$digest();
  }

  function editLockedDocument(document) {
    ContentEditor.open.and.returnValue($q.resolve(document));
    ContentEditor.getDocument.and.returnValue(undefined);
    ContentEditor.getDocumentDisplayName.and.returnValue(document.displayName);

    EditContentService.startEditing(document.id);
    $rootScope.$digest();
  }

  function editNonProjectDocument(document) {
    const documentNotInProject = {
      id: '42',
      branchId: 'master',
      displayName: 'Non Project Document',
    };
    ConfigService.projectsEnabled = true;
    ProjectService.selectedProject.id = 'q';
    ContentService.getDocument.and.returnValue($q.resolve(documentNotInProject));

    EditContentService.startEditing(document.id);
    $rootScope.$digest();
  }

  function editProjectDocument(document) {
    ConfigService.projectsEnabled = true;
    ProjectService.selectedProject.id = 'q';
    ContentService.getDocument.and.returnValue($q.resolve(document));

    ContentEditor.open.and.returnValue($q.resolve(document));
    ContentEditor.getDocument.and.returnValue(document);
    ContentEditor.getDocumentId.and.returnValue(document.id);

    EditContentService.startEditing(document.id);
    $rootScope.$digest();
  }

  it('starts editing a document', () => {
    const document = {
      id: '42',
    };
    editDocument(document);

    expect(RightSidePanelService.clearContext).toHaveBeenCalled();
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(ContentEditor.open).toHaveBeenCalledWith(document.id);
    expect($translate.instant).toHaveBeenCalledWith('DOCUMENT');
    expect(RightSidePanelService.setContext).toHaveBeenCalledWith('DOCUMENT');
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
  });

  it('starts editing a xpage', () => {
    const document = {
      id: '42',
    };
    editPage(document);

    expect(RightSidePanelService.clearContext).toHaveBeenCalled();
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(ContentEditor.open).toHaveBeenCalledWith(document.id);
    expect($translate.instant).toHaveBeenCalledWith('PAGE');
    expect(RightSidePanelService.setContext).toHaveBeenCalledWith('PAGE');
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
  });

  it('starts editing a locked document', () => {
    const document = {
      id: 42,
      displayName: 'Locked document',
    };
    editLockedDocument(document);

    expect(RightSidePanelService.clearContext).toHaveBeenCalled();
    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('DOCUMENT');
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(ContentEditor.open).toHaveBeenCalledWith(document.id);
    expect($translate.instant).toHaveBeenCalledWith('DOCUMENT');
    expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('Locked document');
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
  });

  describe('editing for projects', () => {
    it('does not start editing a document that is not part of the current project', () => {
      const document = {
        id: '42',
      };
      spyOn($state, 'go');

      editNonProjectDocument(document);

      expect($translate.instant).toHaveBeenCalledWith('DOCUMENT');
      expect(RightSidePanelService.setContext).toHaveBeenCalledWith('DOCUMENT');
      expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('Non Project Document');
      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.add-to-project', {
        documentId: '42',
        nextState: 'hippo-cm.channel.edit-content',
      });
    });

    it('does start editing a document that is part of the current project', () => {
      const document = {
        id: '42',
        branchId: 'q',
      };

      editProjectDocument(document);

      expect(RightSidePanelService.clearContext).toHaveBeenCalled();
      expect(RightSidePanelService.startLoading).toHaveBeenCalled();
      expect(ContentEditor.open).toHaveBeenCalledWith(document.id);
      expect($translate.instant).toHaveBeenCalledWith('DOCUMENT');
      expect(RightSidePanelService.setTitle).toHaveBeenCalledWith('DOCUMENT');
      expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
    });
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

  it('displays dialog with a document related message', () => {
    const document = {
      id: '42',
    };
    editDocument(document);
    $state.go('hippo-cm');

    expect(ContentEditor.confirmClose).toHaveBeenCalledWith(
      'SAVE_CHANGES_TO_DOCUMENT',
      {},
      'SAVE_DOCUMENT_CHANGES_TITLE',
    );
  });

  it('displays dialog with a page related message', () => {
    const document = {
      id: '42',
    };

    editPage(document);
    $state.go('hippo-cm');

    expect(ContentEditor.confirmClose).toHaveBeenCalledWith('SAVE_CHANGES_TO_XPAGE', {}, 'SAVE_XPAGE_CHANGES_TITLE');
  });

  it('confirms closing the open editor when the channel is closed', () => {
    const document = {
      id: '42',
    };
    editDocument(document);
    ContentEditor.confirmClose.and.returnValue($q.resolve());

    $state.go('hippo-cm');
    $rootScope.$digest();

    expect(ContentEditor.confirmClose).toHaveBeenCalled();
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

    expect(ContentEditor.confirmClose).toHaveBeenCalled();
    expect($state.$current.name).toBe('hippo-cm.channel.edit-content');
  });

  describe('isEditing', () => {
    it('returns false if not editing any document', () => {
      expect(EditContentService.isEditing('documentId')).toBe(false);
    });

    it('returns false if not editing the referenced document', () => {
      editDocument({ id: 'documentId' });

      expect(EditContentService.isEditing('anotherDocumentId')).toBe(false);
    });

    it('returns true if editing the referenced document', () => {
      editDocument({ id: 'documentId' });

      expect(EditContentService.isEditing('documentId')).toBe(true);
    });
  });

  describe('reloadEditor', () => {
    it('should reload the editor', () => {
      ContentEditor.getDocumentId.and.returnValue('documentId');
      spyOn($state, 'go');
      EditContentService.reloadEditor();

      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.edit-page.content',
        { documentId: 'documentId', lastModified: jasmine.any(Number) });
    });
  });

  describe('branchAndEditDocument', () => {
    it('should reload the iframe after a successful document branch and edit transition', () => {
      ContentService.branchDocument.and.returnValue($q.resolve());

      EditContentService.branchAndEditDocument('documentId');
      $rootScope.$digest();

      expect(HippoIframeService.reload).toHaveBeenCalled();
    });
  });
});
