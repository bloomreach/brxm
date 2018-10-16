/*
 * Copyright 2017-2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

class ProjectToggleController {
  constructor(
    CmsService,
    ProjectService,
  ) {
    'ngInject';

    this.CmsService = CmsService;
    this.ProjectService = ProjectService;
  }

  $onInit() {
    this.getProjects();
  }

  getProjects() {
    this.projects = this.ProjectService.projects;
  }

  get selectedProject() {
    return this.ProjectService.selectedProject;
  }

  set selectedProject(selectedProject) {
    this.ProjectService.updateSelectedProject(selectedProject.id)
      .then(() => this.CmsService.reportUsageStatistic('CMSChannelsProjectSwitch'));
  }
}

export default ProjectToggleController;
