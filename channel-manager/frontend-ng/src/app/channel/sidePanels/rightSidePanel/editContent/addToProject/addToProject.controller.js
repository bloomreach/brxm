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

class AddToProjectController {
  constructor(
    $uiRouterGlobals,
    CmsService,
    EditContentService,
    ProjectService,
  ) {
    'ngInject';

    this.$uiRouterGlobals = $uiRouterGlobals;
    this.CmsService = CmsService;
    this.EditContentService = EditContentService;
    this.ProjectService = ProjectService;
  }

  $onInit() {
    this.ProjectService.beforeChange('addToProject', (projectIdIdentical) => {
      if (!projectIdIdentical) {
        this.close();
      }
    });
  }

  $onDestroy() {
    this.ProjectService.beforeChangeListeners.delete('addToProject');
  }

  getSelectedProject() {
    return this.ProjectService.selectedProject;
  }

  addDocumentToProject() {
    this.CmsService.reportUsageStatistic('AddToProjectVisualEditor');
    const { documentId, nextState } = this.$uiRouterGlobals.params;
    this.EditContentService.branchAndEditDocument(documentId, nextState);
  }

  close() {
    this.EditContentService.stopEditing();
  }
}

export default AddToProjectController;
