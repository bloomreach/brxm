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

const REST_API_PATH = 'ws/projects';

class ProjectService {

  constructor($http, ConfigService, PathService, HstService) {
    'ngInject';

    this.$http = $http;
    this.ConfigService = ConfigService;
    this.PathService = PathService;
    this.HstService = HstService;
  }

  load(channel) {
    this._mountId = channel.mountId;
    this._master = {
      name: this._masterName(channel),
      id: channel.id,
    };
    this.projects();
  }

  _masterName(channel) {
    return `${channel.name} Core`;
  }

  projects() {
    return this._getProjects(this._mountId)
      .then((result) => {
        this.currentBranch()
          .then((branchId) => {
            this.withBranch = [this._master];
            this.withBranch = this.withBranch.concat(result.withBranch);
            this.selectedProject = this._master;
            let disabled = false;
            if (branchId) {
              disabled = true;
              this.selectedProject = this.withBranch.find(project => project.id === branchId);
            }
            this.withoutBranch = result.withoutBranch;
            this.withoutBranch.forEach((p) => { Object.assign(p, { disabled: disabled || p.state !== 'UNAPPROVED' }); });
          });
      });
  }

  _getProjects(mountId) {
    const url = `${this.ConfigService.getCmsContextPath()}${REST_API_PATH}/${mountId}/channel`;
    return this.$http({ method: 'GET', url, headers: {}, data: {} }).then(result => result.data);
  }

  compareId(p1) {
    return p2 => p1.id === p2.id;
  }

  projectChanged(selectedProject) {
    if (this.compareId(this._master)(selectedProject)) {
      return this.selectMaster();
    }
    if (this.withoutBranch.some(this.compareId(selectedProject))) {
      return this.createBranch(selectedProject);
    }

    return this.selectBranch(selectedProject);
  }

  currentBranch() {
    return this.HstService.doGet(this._mountId, 'currentbranch')
      .then(result => result.data);
  }

  createBranch(project) {
    return this.HstService.doPut(null, this._mountId, 'createbranch', project.id);
  }

  selectBranch(project) {
    return this.HstService.doPut(null, this._mountId, 'selectbranch', project.id);
  }

  selectMaster() {
    return this.HstService.doPut(null, this._mountId, 'selectmaster');
  }
}

export default ProjectService;
