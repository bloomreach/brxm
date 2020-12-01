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

describe('addToProjectComponent', () => {
  let $componentController;
  let $ctrl;
  let $uiRouterGlobals;
  let ProjectService;
  let EditContentService;

  const testDocumentId = 'testDocument';
  const testState = 'teststate';

  const testProject = {
    id: 'test',
  };

  beforeEach(() => {
    angular.mock.module('hippo-cm');

    inject((
      _$componentController_,
      _$uiRouterGlobals_,
      _ProjectService_,
      _EditContentService_,
    ) => {
      $componentController = _$componentController_;
      $uiRouterGlobals = _$uiRouterGlobals_;
      ProjectService = _ProjectService_;
      EditContentService = _EditContentService_;
    });

    ProjectService.selectedProject = testProject;

    spyOn(ProjectService, 'beforeChange');

    $uiRouterGlobals.params = { documentId: testDocumentId, nextState: testState };

    $ctrl = $componentController('addToProject');
    $ctrl.$onInit();
  });

  describe('life cycle method', () => {
    it('should register a beforeChange callback', () => {
      expect(ProjectService.beforeChange).toHaveBeenCalled();
    });

    it('should remove the beforeChange callback on component destruction', () => {
      expect(ProjectService.beforeChangeListeners.size).toEqual(1);

      $ctrl.$onDestroy();

      expect(ProjectService.beforeChangeListeners.get('addToProject')).toEqual(undefined);
    });
  });

  describe('getSelectedProject', () => {
    it('returns the selected project', () => {
      expect($ctrl.getSelectedProject()).toEqual(testProject);
    });
  });

  describe('addDocumentToProject', () => {
    it('adds the document to the selected project', () => {
      spyOn(EditContentService, 'branchAndEditDocument');
      $ctrl.addDocumentToProject();
      expect(EditContentService.branchAndEditDocument).toHaveBeenCalledWith(testDocumentId, testState);
    });

    it('closes the content service', () => {
      spyOn(EditContentService, 'stopEditing');
      $ctrl.close();
      expect(EditContentService.stopEditing).toHaveBeenCalled();
    });
  });
});
