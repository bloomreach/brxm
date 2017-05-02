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
  constructor($translate, OverlayService, ChannelService, ProjectService, ConfigService, HippoIframeService) {
    'ngInject';

    this.$translate = $translate;
    this.OverlayService = OverlayService;
    this.ProjectService = ProjectService;
    this.available = ProjectService.available;
    const channel = ChannelService.getChannel();
    this.MASTER = { name: channel.name, id: channel.id };
    this.withBranch = [this.MASTER];
    this.selectedProject = this.MASTER;
    this.projectsEnabled = ConfigService.projectsEnabled;
    this.HippoIframeService = HippoIframeService;
    if (this.projectsEnabled) {
      this._setProjects();
    }
  }
  _setProjects() {
    this.ProjectService.projects()
      .then((response) => {
        this.withBranch = this.withBranch.concat(response.withBranch);
        this.withoutBranch = response.withoutBranch;
      });
  }
  projectChanged() {
    if (this.selectedProject === this.MASTER) {
      this.ProjectService.selectMaster();
      return;
    }
    if (this.withBranch.indexOf(this.selectedProject) === -1) {
      this.ProjectService.selectBranch(this.selectedProject);
    } else {
      this.ProjectService.createBranch(this.selectedProject);
    }
    this.HippoIframeService.reload();
  }
}

export default ProjectToggleController;
