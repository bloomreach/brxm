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

class ProjectService {
  constructor(
    $http,
    $translate,
    $q,
    ConfigService,
    FeedbackService,
    HippoGlobal,
  ) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;

    this.ConfigService = ConfigService;
    this.FeedbackService = FeedbackService;
    this.HippoGlobal = HippoGlobal;

    this.listeners = [];
    this.core = {
      id: 'master',
      name: $translate.instant('CORE'),
    };
  }

  load(mountId, projectId) {
    this.mountId = mountId;

    this._setupProjectSync();

    return this._setupProjects(projectId);
  }

  getActiveProjectId() {
    return this.ConfigService.projectsEnabled ? this.project.id : this.core.id;
  }

  isBranch() {
    return this.getActiveProjectId() !== 'master';
  }

  getCore() {
    return this.core;
  }

  updateSelectedProject(projectId) {
    return this._selectProject(projectId)
      .then(() => this._callListeners(projectId));
  }

  registerListener(cb) {
    this.listeners.push(cb);
  }

  associateWithProject(documentId) {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/${this.project.id}/associate/${documentId}`;
    return this.$http
      .post(url)
      .then(() => {
        this._getAllProjects();
        this.FeedbackService.showNotification('DOCUMENT_ADDED_TO_PROJECT', {
          name: this.project.name,
        });
      });
  }

  showAddToProjectForDocument(documentId) {
    const associatedProject = this._getProjectByDocumentId(documentId);
    return this.project && !associatedProject;
  }

  _getProjectByDocumentId(documentId) {
    // returns undefined if document is not part of any project
    // and therefore is part of core
    return this.allProjects.find(project => project.documents.find(document => document.id === documentId));
  }

  _callListeners(...args) {
    this.listeners.forEach(listener => listener(...args));
  }

  _setupProjects(projectId) {
    return this.$q
      .all([
        this._getProjects(),
        this._getAllProjects(),
      ])
      .then(() => this._selectProject(projectId));
  }

  _selectProject(projectId) {
    this.project = this.allProjects.find(project => project.id === projectId) || this.core;
    return this.project.id === this.core.id ? this._activateCore() : this._activateProject(this.project.id);
  }

  isContentOverlayEnabled() {
    // For now, to prevent all kinds of corner cases, we only enable the content overlay
    // for unapproved projects in review so that you cannot edit documents at all.
    return !this.project || this.project.state === 'UNAPPROVED';
  }

  isComponentsOverlayEnabled() {
    return !this.project
      || this.project.state === 'UNAPPROVED'
      || (this.project.state === 'IN_REVIEW' && this._isActionEnabled('resetChannel'));
    // The action resetChannel puts a channel back into review.
    // It is only enabled if a channel has been rejected and the project is in review.
  }

  isRejectEnabled() {
    return this.project
      && this.project.state === 'IN_REVIEW'
      && this._isActionEnabled('rejectChannel');
  }

  isAcceptEnabled() {
    return this.project
      && this.project.state === 'IN_REVIEW'
      && this._isActionEnabled('approveChannel');
  }

  accept(channelId) {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/${this.project.id}/channel/approve`;

    return this.$http
      .post(url, channelId)
      .then((response) => { this.project = response.data; })
      .catch(() => {
        this.FeedbackService.showError('PROJECT_OUT_OF_SYNC', {});
        this._callListeners(this.project.id);
      });
  }

  reject(channelId, message) {
    const request = {
      method: 'POST',
      url: `${this.ConfigService.getCmsContextPath()}ws/projects/${this.project.id}/channel/reject/${channelId}`,
      headers: {
        'Content-Type': 'text/plain',
      },
      data: message,
    };

    return this.$http(request)
      .then((response) => {
        this.project = response.data;
      })
      .catch(() => {
        this.FeedbackService.showError('PROJECT_OUT_OF_SYNC', {});
        this._callListeners(this.project.id);
      });
  }

  _isActionEnabled(action) {
    const channelInfo = this.project.channels.find(c => c.mountId === this.mountId);
    return channelInfo && channelInfo.actions[action].enabled;
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

  _getAllProjects() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects`;

    return this.$http
      .get(url)
      .then(result => result.data)
      .then((projects) => {
        this.allProjects = projects;
      });
  }

  _setupProjectSync() {
    if (this.HippoGlobal.Projects && this.HippoGlobal.Projects.events) {
      this.projectEvents = this.HippoGlobal.Projects.events;

      this.projectEvents.unsubscribeAll();

      this.projectEvents.subscribe('project-updated', () => this._getProjects());
      this.projectEvents.subscribe('project-status-updated', () => this._callListeners());
      this.projectEvents.subscribe('project-channel-added', () => this._callListeners());
      this.projectEvents.subscribe('project-deleted', (projectId) => {
        this._getProjects().then(() => this.updateSelectedProject(projectId));
      });
      this.projectEvents.subscribe('project-channel-deleted', (projectId) => {
        this._getProjects().then(() => this.updateSelectedProject(projectId));
      });
    }
  }

  _activateProject(projectId) {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/activeProject/${projectId}`;
    return this.$http
      .put(url);
  }

  _activateCore() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/activeProject`;
    return this.$http
      .delete(url);
  }
}

export default ProjectService;
