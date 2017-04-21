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

const MASTER = { name: 'Default', id: 'master' };

class ProjectToggleController {
  constructor($translate, OverlayService, ChannelService, ProjectService) {
    'ngInject';

    this.$translate = $translate;
    this.OverlayService = OverlayService;
    this.ChannelService = ChannelService;
    this.ProjectService = ProjectService;
    this.available = ProjectService.available;
    this.withBranch = [MASTER];
    this.selectedProject = MASTER;
    this._setProjects();
  }
  $onInit() {
    this._activate();
  }

  _setProjects() {
    this.ProjectService.projects(this.ChannelService.getChannel().id)
      .then((response) => {
        this.withBranch = this.withBranch.concat(response.withBranch);
        this.withoutBranch = response.withoutBranch;
      });
  }

  _activate() {
    this._projectChanged();
  }

  _projectChanged() {
    if (this.withBranch.indexOf(this.selectedProject) >= 0) {
      this.ProjectService.doSelectBranch(this.selectedProject);
    } else {
      this.ProjectService.doCreateBranch(this.selectedProject);
    }
  }

}

export default ProjectToggleController;
