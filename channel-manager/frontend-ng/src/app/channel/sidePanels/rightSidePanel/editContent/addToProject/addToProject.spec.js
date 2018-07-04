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

describe('addToProjectComponent', () => {
  let $componentController;
  let $ctrl;
  let $q;
  let $rootScope;
  let ProjectService;
  let ContentEditor;

  const testDocumentId = 'testDocument';

  const testProject = {
    id: 'test',
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$componentController_,
      _$q_,
      _$rootScope_,
      _ProjectService_,
      _ContentEditor_,
    ) => {
      $componentController = _$componentController_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      ProjectService = _ProjectService_;
      ContentEditor = _ContentEditor_;
    });

    ProjectService.project = testProject;

    spyOn(ProjectService, 'associateWithProject').and.returnValue($q.resolve());
    spyOn(ContentEditor, 'getDocumentId').and.returnValue(testDocumentId);
    spyOn(ContentEditor, 'open');

    $ctrl = $componentController('addToProject');
  });

  describe('getSelectedProject', () => {
    it('returns the selected project', () => {
      expect($ctrl.getSelectedProject()).toEqual(testProject);
    });
  });

  describe('addDocumentToProject', () => {
    it('adds the document to the selected project', () => {
      $ctrl.addDocumentToProject();
      expect(ContentEditor.getDocumentId).toHaveBeenCalled();
      expect(ProjectService.associateWithProject).toHaveBeenCalledWith(testDocumentId);
    });

    it('opens the document in the content editor', () => {
      $ctrl.addDocumentToProject();
      $rootScope.$digest();
      expect(ContentEditor.open).toHaveBeenCalledWith(testDocumentId);
    });

    it('does not open the document in the content editor when the document could not be added to the selected project', () => {
      ProjectService.associateWithProject.and.returnValue($q.reject());
      $ctrl.addDocumentToProject();
      $rootScope.$digest();
      expect(ContentEditor.open).not.toHaveBeenCalled();
    });
  });
});

