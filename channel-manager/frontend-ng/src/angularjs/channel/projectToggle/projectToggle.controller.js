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


class ProjectToggleController {
  constructor($scope, $translate, OverlayService, ChannelService, ProjectService, ConfigService, HippoIframeService, SessionService) {
    'ngInject';

    this.$translate = $translate;
    this.OverlayService = OverlayService;
    this.ProjectService = ProjectService;
    this.available = ProjectService.available;
    this.ChannelService = ChannelService;
    this.projectsEnabled = ConfigService.projectsEnabled;
    this.HippoIframeService = HippoIframeService;
    if (this.projectsEnabled) {
      // In order to have a way to trigger the reloading of the global variants, we tie the reloading
      // to a successful SessionService.initialize call, which happens upon channel switching.
      SessionService.registerInitCallback('reloadProjects', () => this._setCurrentBranch());
      $scope.$on('$destroy', () => SessionService.unregisterInitCallback('reloadProjects'));
      this._setProjectsAndSelectedProject();
    }
  }
  _setProjectsAndSelectedProject() {
    const channel = this.ChannelService.getChannel();
    this.MASTER = { name: channel.name, id: channel.id };
    this.withBranch = [this.MASTER];
    this.ProjectService.projects()
      .then((response) => {
        this.withBranch = this.withBranch.concat(response.withBranch);
        this.withoutBranch = response.withoutBranch;
        this._setCurrentBranch();
      });
  }

  _setCurrentBranch() {
    this.ProjectService.currentBranch()
      .then((branchId) => {
        if (branchId) {
          const currentProject = this.withBranch.find(project => project.id === branchId);
          this.selectedProject = currentProject || this.MASTER;
        } else {
          this.selectedProject = this.MASTER;
        }
      });
  }

  compareId(p1) {
    return p2 => p1.id === p2.id;
  }

  projectChanged() {
    const p = this.selectedProject;
    if (this.compareId(this.MASTER)(p)) {
      this.ProjectService.selectMaster();
    } else
    if (this.withBranch.some(this.compareId(p))) {
      this.ProjectService.selectBranch(p);
    } else
    if (this.withoutBranch.some(this.compareId(p))) {
      this.ProjectService.createBranch(p);
      this.withBranch = this.withBranch.concat(p);
      this.withoutBranch = this.withoutBranch.filter(project => project.id !== p.id);
      this.selectedProject = p;
    }
    this.HippoIframeService.reload();
  }
}

export default ProjectToggleController;
