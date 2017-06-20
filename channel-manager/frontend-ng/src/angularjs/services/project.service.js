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
    $q,
    $window,
    ConfigService,
    PathService,
    HstService,
    HippoGlobal,
  ) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;
    this.$window = $window;

    this.ConfigService = ConfigService;
    this.PathService = PathService;
    this.HstService = HstService;
    this.HippoGlobal = HippoGlobal;

    this.listeners = [];
  }

  load(channel, branchId = '') {
    this.urlPrefix = `${this.ConfigService.getCmsContextPath()}ws/projects/`;
    this.mountId = channel.mountId;
    this.branchId = branchId;

    if (this.HippoGlobal.Projects && this.HippoGlobal.Projects.events) {
      this.events = this.HippoGlobal.Projects.events;

      this.events.unsubscribeAll();

      this.events.subscribe('projects-changed', () => {
        this._getProjects();
      });
    }

    return this._setupProjects();
  }

  registerChangeListener(cb) {
    this.listeners.push(cb);
  }

  updateSelectedProject(projectId) {
    this.listeners.forEach(listener => listener());
    const selectedProject = this.projects.find(project => project.id === projectId);
    const selectionPromise = selectedProject ? this._selectProject(projectId) : this._selectCore();

    return selectionPromise.then(() => {
      this.selectedProject = selectedProject;
    });
  }

  _setupProjects() {
    return this
      ._getProjects()
      .then(() => this.branchId || this._getCurrentProject())
      .then(projectId => this.updateSelectedProject(projectId));
  }

  _getProjects() {
    const url = `${this.urlPrefix}${this.mountId}/associated-with-channel`;
    return this.$http
      .get(url)
      .then(result => result.data)
      .then((projects) => {
        this.projects = projects;
      });
  }

  _selectProject(projectId) {
    return this.HstService
      .doPut(null, this.mountId, 'selectbranch', projectId);
  }

  _selectCore() {
    return this.HstService
      .doPut(null, this.mountId, 'selectmaster');
  }

  _getCurrentProject() {
    return this.HstService
      .doGet(this.mountId, 'currentbranch')
      .then(result => result.data);
  }

}

export default ProjectService;
