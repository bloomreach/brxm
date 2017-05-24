/*
 * Copyright 2017 Hippo B.V. (http://www.onehippo.com)
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

class ProjectService {
  constructor(
    $http,
    ConfigService,
    PathService,
    HstService,
  ) {
    'ngInject';

    this.$http = $http;
    this.ConfigService = ConfigService;
    this.PathService = PathService;
    this.HstService = HstService;
  }

  load(channel) {
    this.urlPrefix = `${this.ConfigService.getCmsContextPath()}ws/projects/`;
    this.mountId = channel.mountId;

    this.getProjects()
      .then((projects) => {
        this.getCurrentProject()
          .then((branchId) => {
            this.projects = projects;
            this.selectedProject = this.projects.find(project => project.id === branchId);
          });
      });
  }

  getProjects() {
    const url = `${this.urlPrefix}${this.mountId}/associated-with-channel`;
    return this.$http.get(url).then(result => result.data);
  }

  selectProject(project) {
    if (this.projects.find(p => p.id === project.id)) {
      return this.HstService.doPut(null, this.mountId, 'selectbranch', project.id);
    }

    return this.HstService.doPut(null, this.mountId, 'selectmaster');
  }

  getCurrentProject() {
    return this.HstService
      .doGet(this.mountId, 'currentbranch')
      .then(result => result.data);
  }
}

export default ProjectService;
