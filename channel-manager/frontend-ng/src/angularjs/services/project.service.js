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
    ConfigService,
    HstService,
    HippoGlobal,
  ) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;

    this.ConfigService = ConfigService;
    this.HstService = HstService;
    this.HippoGlobal = HippoGlobal;
  }

  load(mountId, projectId) {
    this.updateListeners = [];
    this.selectListeners = [];
    this.mountId = mountId;
    this.projectId = projectId;

    this._setupProjectSync();

    return this._setupProjects();
  }

  getBaseChannelId(channelId) {
    const channel = this.channels.find(ch => ch.id === channelId);
    return channel && channel.branchOf ? channel.branchOf : channelId;
  }

  getCurrentProject(mountId = this.mountId) {
    return this.HstService
      .doGet(mountId, 'currentbranch')
      .then(result => result.data);
  }

  updateSelectedProject(projectId) {
    const selectedProject = this.projects.find(project => project.id === projectId);
    const selectionPromise = selectedProject ? this._selectProject(projectId) : this._selectCore();

    return selectionPromise.then(() => {
      this._callListeners(this.selectListeners, projectId);
    });
  }

  registerSelectListener(cb) {
    this.selectListeners.push(cb);
  }

  registerUpdateListener(cb) {
    this.updateListeners.push(cb);
  }

  _callListeners(listeners, ...args) {
    listeners.forEach(listener => listener(...args));
  }

  _setupProjects() {
    return this.$q
      .all([
        this._getProjects(),
        this._getChannels(),
      ])
      .then(() => {
        const selectedProject = this.projects.find(project => project.id === this.projectId);
        this.selectedProject = selectedProject;
        return this.selectedProject ? this._selectProject(this.selectedProject.id) : this._selectCore();
      });
  }

  _getProjects() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/${this.mountId}/associated-with-channel`;

    return this.$http
      .get(url)
      .then(result => result.data)
      .then((projects) => {
        this.projects = projects;
      });
  }

  _getChannels() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/channels/`;

    return this.$http
      .get(url)
      .then(response => response.data)
      .then((channels) => {
        this.channels = channels;
        return channels;
      });
  }

  _setupProjectSync() {
    if (this.HippoGlobal.Projects && this.HippoGlobal.Projects.events) {
      this.projectEvents = this.HippoGlobal.Projects.events;

      this.projectEvents.unsubscribeAll();

      this.projectEvents.subscribe('project-updated', () => this._getProjects());
      this.projectEvents.subscribe('project-status-updated', () => this._callListeners(this.updateListeners));
      this.projectEvents.subscribe('project-channel-added', () => this._callListeners(this.updateListeners));
      this.projectEvents.subscribe('project-deleted', (projectId) => {
        this._getProjects().then(() => this.updateSelectedProject(projectId));
      });
      this.projectEvents.subscribe('project-channel-deleted', (projectId) => {
        this._getProjects().then(() => this.updateSelectedProject(projectId));
      });
    }
  }

  _selectProject(projectId) {
    return this.HstService
      .doPut(null, this.mountId, 'selectbranch', projectId);
  }

  _selectCore() {
    return this.HstService
      .doPut(null, this.mountId, 'selectmaster');
  }
}

export default ProjectService;
