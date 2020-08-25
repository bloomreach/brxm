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
  let ProjectService;
  let RightSidePanelService;
  let PageService;

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
    ]);
    ContentService = jasmine.createSpyObj('ContentService', ['getDocument']);
    RightSidePanelService = jasmine.createSpyObj('RightSidePanelService', [
      'clearContext',
      'setContext',
      'setTitle',
      'startLoading',
      'stopLoading',
    ]);
    PageService = {
      isXPage: false,
      xPageId: undefined,
    };

    angular.mock.module(($provide) => {
      $provide.value('ContentEditor', ContentEditor);
      $provide.value('ContentService', ContentService);
      $provide.value('RightSidePanelService', RightSidePanelService);
      $provide.value('PageService', PageService);
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
    PageService.xPageId = '42';

    editDocument(document);

    expect(RightSidePanelService.clearContext).toHaveBeenCalled();
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(ContentEditor.open).toHaveBeenCalledWith(document.id);
    expect($translate.instant).toHaveBeenCalledWith('PAGE');
    expect(RightSidePanelService.setContext).toHaveBeenCalledWith('PAGE');
    expect(RightSidePanelService.stopLoading).toHaveBeenCalled();
  });

  it('starts editing a document as a part of a xpage', () => {
    const document = {
      id: '43',
    };
    PageService.xPageId = '42';

    editDocument(document);

    expect(RightSidePanelService.clearContext).toHaveBeenCalled();
    expect(RightSidePanelService.startLoading).toHaveBeenCalled();
    expect(ContentEditor.open).toHaveBeenCalledWith(document.id);
    expect($translate.instant).toHaveBeenCalledWith('DOCUMENT');
    expect(RightSidePanelService.setContext).toHaveBeenCalledWith('DOCUMENT');
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
      expect($state.go).toHaveBeenCalledWith('hippo-cm.channel.add-to-project', { documentId: '42' });
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

    expect(ContentEditor.confirmClose).toHaveBeenCalledWith('SAVE_CHANGES_TO_DOCUMENT');
  });

  it('displays dialog with a page related message', () => {
    const document = {
      id: '42',
    };
    PageService.isXPage = true;

    editDocument(document);
    $state.go('hippo-cm');

    expect(ContentEditor.confirmClose).toHaveBeenCalledWith('SAVE_CHANGES_TO_XPAGE');
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
});
