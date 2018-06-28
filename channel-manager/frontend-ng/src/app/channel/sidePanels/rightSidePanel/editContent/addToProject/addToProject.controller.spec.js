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
  let $controller;
  let $ctrl;
  let $q;
  let $rootScope;
  let $uiRouterGlobals;
  let ProjectService;
  let EditContentService;

  const testDocumentId = 'testDocument';

  const testProject = {
    id: 'test',
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$controller_,
      _$q_,
      _$rootScope_,
      _$uiRouterGlobals_,
      _ProjectService_,
      _EditContentService_,
    ) => {
      $controller = _$controller_;
      $q = _$q_;
      $rootScope = _$rootScope_;
      $uiRouterGlobals = _$uiRouterGlobals_;
      ProjectService = _ProjectService_;
      EditContentService = _EditContentService_;
    });

    ProjectService.selectedProject = testProject;
    $uiRouterGlobals.params = { documentId: testDocumentId };

    $ctrl = $controller('addToProjectCtrl');

  });

  describe('getSelectedProject', () => {
    it('returns the selected project', () => {
      expect($ctrl.getSelectedProject()).toEqual(testProject);
    });
  });

  describe('addDocumentToProject', () => {
    it('adds the document to the selected project', () => {
      spyOn(EditContentService, 'editDocument');
      $ctrl.addDocumentToProject();
      expect(EditContentService.editDocument).toHaveBeenCalledWith(testDocumentId);
    });

    it('closes the content service', () => {
      spyOn(EditContentService, 'stopEditing');
      $ctrl.close();
      expect(EditContentService.stopEditing).toHaveBeenCalled();
    });
  });
});

