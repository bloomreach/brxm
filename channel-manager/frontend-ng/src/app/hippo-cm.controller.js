/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

class HippoCmCtrl {
  constructor(
    $rootScope,
    $state,
    BrowserService,
    ChannelService,
    CmsService,
    HippoIframeService,
  ) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$state = $state;
    this.BrowserService = BrowserService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.HippoIframeService = HippoIframeService;
  }

  $onInit() {
    // don't log state transition errors
    this.$state.defaultErrorHandler(angular.noop);

    // add ie11 class for ie11 specific hacks
    if (this.BrowserService.isIE()) {
      $('body').addClass('ie11');
    }

    this.CmsService.subscribe('load-channel', (channel, initialPath, projectId) => {
      if (!this.ChannelService.matchesChannel(channel, projectId)) {
        this.ChannelService.initializeChannel(channel, initialPath, projectId);
      } else if (angular.isString(initialPath) // a null path means: reuse the current render path
        && this.HippoIframeService.getCurrentRenderPathInfo() !== initialPath) {
        this.$rootScope.$apply(() => { // change from outside, so the trigger digest loop to let AngularJs pick it up
          this.HippoIframeService.load(initialPath);
        });
      } else {
        this.HippoIframeService.reload();
      }
    });

    // Reload current channel
    this.CmsService.subscribe('channel-changed-in-extjs', () => {
      this.$rootScope.$apply(() => this.ChannelService.reload());
    });

    // Handle reloading of iframe by Webpack during development
    this.CmsService.publish('reload-channel');
  }
}

export default HippoCmCtrl;
