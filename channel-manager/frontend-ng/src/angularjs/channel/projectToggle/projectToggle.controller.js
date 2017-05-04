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
  constructor(
    $scope,
    $translate,
    ChannelService,
    CmsService,
    HippoIframeService,
    OverlayService,
    ProjectService,
    SessionService,
  ) {
    'ngInject';

    this.$translate = $translate;
    this.CmsService = CmsService;
    this.ChannelService = ChannelService;
    this.HippoIframeService = HippoIframeService;
    this.OverlayService = OverlayService;
    this.ProjectService = ProjectService;
    this.SessionService = SessionService;
  }

  $onInit() {
    // In order to have a way to trigger the reloading of the global variants, we tie the reloading
    // to a successful SessionService.initialize call, which happens upon channel switching.
    this.SessionService.registerInitCallback('reloadProjects', () => {
      this._setCurrentBranch();
    });

    this._getProjects();
  }

  $onDestroy() {
    this.SessionService.unregisterInitCallback('reloadProjects');
  }

  _replaceMaster() {
    const channel = this.ChannelService.getChannel();
    const index = this.withBranch.findIndex((project) => {
      if (this.MASTER) {
        return project.id === this.MASTER.id;
      }

      return false;
    });

    if (index !== -1) {
      this.withBranch.splice(index, 1);
    }

    this.MASTER = {
      name: channel.name,
      id: channel.id.replace('-preview', ''),
    };

    this.withBranch.push(this.MASTER);
  }

  _getProjects() {
    this.ProjectService.projects()
      .then((response) => {
        this.withBranch = response.withBranch;
        this.withoutBranch = response.withoutBranch;
      });
  }

  _setCurrentBranch() {
    this.ProjectService.currentBranch()
      .then((branchId) => {
        this._replaceMaster();

        if (branchId) {
          branchId = branchId.replace('-preview', '');
          const currentProject = this.withBranch.find(project => project.id === branchId);
          this.selectedProject = currentProject;
          return;
        }

        this.selectedProject = this.MASTER;
      });
  }

  compareId(p1) {
    return p2 => p1.id === p2.id;
  }

  projectChanged() {
    const p = this.selectedProject;

    if (this.compareId(this.MASTER)(p)) {
      console.log('selected master');
      this.ProjectService.selectMaster();
    } else if (this.withBranch.some(this.compareId(p))) {
      console.log('selected project', p.name);
      this.ProjectService.selectBranch(p);
    } else if (this.withoutBranch.some(this.compareId(p))) {
      console.log('selected withoutBranch');
      this.ProjectService.createBranch(p);
      this.withBranch = this.withBranch.concat(p);
      this.withoutBranch = this.withoutBranch.filter(project => project.id !== p.id);
    }

    this.HippoIframeService.reload();
  }
}

export default ProjectToggleController;
