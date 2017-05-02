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

  constructor($http, ConfigService, PathService, ChannelService, HstService) {
    'ngInject';

    this.$http = $http;
    this.ConfigService = ConfigService;
    this.PathService = PathService;
    this.ChannelService = ChannelService;
    this.HstService = HstService;
  }

  projects() {
    const url = `${this.ConfigService.getCmsContextPath()}${REST_API_PATH}/${this.ChannelService.getChannel().mountId}/channel`;
    return this.$http({ method: 'GET', url, headers: {}, data: {} })
      .then(result => result.data);
  }

  createBranch(project) {
    this.HstService.doPut(null, this.ChannelService.getChannel().mountId, 'createbranch', project.id);
  }

  selectBranch(project) {
    this.HstService.doPut(null, this.ChannelService.getChannel().mountId, 'selectbranch', project.id);
  }

  selectMaster() {
    this.HstService.doPut(null, this.ChannelService.getChannel().mountId, 'selectmaster');
  }

}

export default ProjectService;
