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
  ) {
    'ngInject';

    this.$http = $http;
    this.$q = $q;

    this.ConfigService = ConfigService;
    this.FeedbackService = FeedbackService;

    this.beforeChangeListeners = new Map();
    this.afterChangeListeners = new Map();
    this.projects = [];
    this.masterId = 'master';

    this.selectedProject = {
      id: this.masterId,
    };
  }

  load(mountId, projectId) {
    this.mountId = mountId;
    return this._setupProjects(projectId);
  }

  getSelectedProjectId() {
    return this.selectedProject.id;
  }

  isBranch() {
    return this.selectedProject.id !== this.masterId;
  }

  isInReview() {
    return this.selectedProject.state && this.selectedProject.state === 'IN_REVIEW';
  }

  updateSelectedProject(projectId) {
    const projectIdIdentical = projectId === this.selectedProject.id;

    return this._callListeners(this.beforeChangeListeners, projectIdIdentical)
      .then(() => this._selectProject(projectId))
      .then(() => this._callListeners(this.afterChangeListeners, projectIdIdentical));
  }

  beforeChange(id, cb) {
    this.beforeChangeListeners.set(id, cb);
  }

  afterChange(id, cb) {
    this.afterChangeListeners.set(id, cb);
  }

  associateWithProject(documentId) {
    const contextPath = this.ConfigService.getCmsContextPath();
    const url = `${contextPath}ws/projects/${this.selectedProject.id}/associate/${documentId}`;
    return this.$http
      .post(url)
      .then(() => {
        this._getProjects();
        this.FeedbackService.showNotification('DOCUMENT_ADDED_TO_PROJECT', {
          name: this.selectedProject.name,
        });
      });
  }

  hasBranchOfProject(channelId) {
    const baseChannelId = channelId.replace(/-preview$/, '');
    const { channels } = this.selectedProject;
    return channels && !!channels.find(c => c.id === baseChannelId);
  }

  _callListeners(listeners, projectIdIdentical) {
    const promises = Array.from(listeners.values())
      .map(listener => listener(projectIdIdentical));

    return this.$q.all(promises);
  }

  _setupProjects(projectId) {
    return this._getProjects()
      .finally(() => this.updateSelectedProject(projectId));
  }

  _selectProject(projectId) {
    this.selectedProject = this.projects.find(project => project.id === projectId);
    return this._setProjectInHST(this.selectedProject.id);
  }

  isEditingAllowed(type) {
    if (type === 'content') {
      return this.selectedProject.state === 'UNAPPROVED';
    }

    return this.selectedProject.state === 'UNAPPROVED'
      || (this.selectedProject.state === 'IN_REVIEW' && this._isActionEnabled('resetChannel'));
  }

  isRejectEnabled() {
    return this.selectedProject.state
      && this.selectedProject.state === 'IN_REVIEW'
      && this._isActionEnabled('rejectChannel');
  }

  isAcceptEnabled() {
    return this.selectedProject.state
      && this.selectedProject.state === 'IN_REVIEW'
      && this._isActionEnabled('approveChannel');
  }

  accept(channelId) {
    const contextPath = this.ConfigService.getCmsContextPath();
    const request = {
      method: 'POST',
      url: `${contextPath}ws/projects/${this.selectedProject.id}/channel/approve/${channelId}`,
    };

    return this.$http(request)
      .then((response) => { this.selectedProject = response.data; })
      .catch(() => {
        this.FeedbackService.showError('PROJECT_OUT_OF_SYNC', {});
        this._callListeners(this.selectedProject.id);
      });
  }

  reject(channelId, message) {
    const contextPath = this.ConfigService.getCmsContextPath();
    const request = {
      method: 'POST',
      url: `${contextPath}ws/projects/${this.selectedProject.id}/channel/reject/${channelId}`,
      headers: {
        'Content-Type': 'text/plain',
      },
      data: message,
    };

    return this.$http(request)
      .then((response) => {
        this.selectedProject = response.data;
      })
      .catch(() => {
        this.FeedbackService.showError('PROJECT_OUT_OF_SYNC', {});
        this._callListeners(this.selectedProject.id);
      });
  }

  _isActionEnabled(action) {
    const { channels } = this.selectedProject;
    const channelInfo = channels && channels.find(c => c.mountId === this.mountId);
    return channelInfo && channelInfo.actions[action].enabled;
  }

  _getProjects() {
    const url = `${this.ConfigService.getCmsContextPath()}ws/projects/${this.mountId}/associated-with-channel`;

    return this.$http
      .get(url)
      .then(result => result.data)
      .then((projects) => {
        this.projects = projects.sort((p1, p2) => {
          if (p1.id === this.masterId) {
            return -1;
          }
          if (p2.id === this.masterId) {
            return 1;
          }
          return p1.name.toLowerCase() <= p2.name.toLowerCase() ? -1 : 1;
        });
      });
  }

  _setProjectInHST(projectId) {
    if (projectId === this.masterId) {
      return this._activateCore();
    }
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
