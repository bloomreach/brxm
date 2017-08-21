/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

class ConfigService {

  constructor($window, CmsService) {
    'ngInject';

    this.$window = $window;

    this.locale = 'en';
    this.rootUuid = 'cafebabe-cafe-babe-cafe-babecafebabe';
    this.contextPaths = ['/site'];

    Object.assign(this, CmsService.getConfig());

    this.contextPath = this.contextPaths[0];
  }

  // TODO: the current context path is a property
  // of the current channel, and therefore belongs into the ChannelService.
  // Keeping this state here is a temporary work-around,
  // because the HstService has no access to the ChannelService.
  setContextPathForChannel(contextPath) {
    this.contextPath = contextPath;
  }

  getCmsContextPath() {
    return this.$window.parent ? this.$window.parent.location.pathname : '/cms/';
  }
}

export default ConfigService;
