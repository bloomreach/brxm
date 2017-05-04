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
    const url = `${this.ConfigService.getCmsContextPath()}${REST_API_PATH}/${this._mountId}/channel`;
    return this.$http({ method: 'GET', url, headers: {}, data: {} })
      .then((result) => {
        this.currentBranch()
          .then((branchId) => {
            this.withBranch = [this._master];
            this.withBranch = this.withBranch.concat(result.data.withBranch);
            this.withoutBranch = result.data.withoutBranch;
            this.selectedProject = this._master;
            if (branchId) {
              this.selectedProject = this.withBranch.find(project => this._compareIgnorePreview(project.id, branchId));
            }
          });
      });
  }

  compareId(p1) {
    return p2 => this._compareIgnorePreview(p1.id, p2.id);
  }

  _compareIgnorePreview(id1, id2) {
    return this._stripPreview(id1) === this._stripPreview(id2);
  }

  _stripPreview(id) {
    return id.replace('-preview', '');
  }

  projectChanged(selectedProject) {
    if (this.compareId(this._master)(selectedProject)) {
      this.selectMaster();
    } else if (this.withBranch.some(this.compareId(selectedProject))) {
      this.selectBranch(selectedProject);
    } else if (this.withoutBranch.some(this.compareId(selectedProject))) {
      this.createBranch(selectedProject);
      this.withBranch = this.withBranch.concat(selectedProject);
      this.withoutBranch = this.withoutBranch.filter(project => project.id !== selectedProject.id);
    }
  }

  currentBranch() {
    return this.HstService.doGet(this._mountId, 'currentbranch')
      .then(result => result.data);
  }

  createBranch(project) {
    this.HstService.doPut(null, this._mountId, 'createbranch', project.id);
  }

  selectBranch(project) {
    this.HstService.doPut(null, this._mountId, 'selectbranch', project.id);
  }

  selectMaster() {
    this.HstService.doPut(null, this._mountId, 'selectmaster');
  }
}

export default ProjectService;
