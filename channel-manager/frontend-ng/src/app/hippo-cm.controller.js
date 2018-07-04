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
    ConfigService,
    HippoIframeService,
  ) {
    'ngInject';

    this.$rootScope = $rootScope;
    this.$state = $state;
    this.BrowserService = BrowserService;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.ConfigService = ConfigService;
    this.HippoIframeService = HippoIframeService;
  }

  $onInit() {
    // don't log state transition errors
    this.$state.defaultErrorHandler(angular.noop);

    // add ie11 class for ie11 specific hacks
    if (this.BrowserService.isIE()) {
      $('body').addClass('ie11');
    }

    this.CmsService.subscribe('load-channel', (channelId, initialPath, branchId) => {
      this.$rootScope.$apply(() => this._loadChannel(channelId, initialPath, branchId));
    });

    // Reload current channel
    this.CmsService.subscribe('channel-changed-in-extjs', () => {
      this.$rootScope.$apply(() => this.ChannelService.reload());
    });

    if (this.ConfigService.isDevMode()) {
      this.CmsService.subscribe('load-channel', (channelId, initialPath, branchId) => {
        this.$rootScope.$apply(() => this._storeAppState(channelId, initialPath, branchId));
      });
      this._restoreAppState();
    }
  }

  _loadChannel(channelId, initialPath, branchId) {
    if (!this.ChannelService.matchesChannel(channelId)) {
      this._initializeChannel(channelId, initialPath, branchId);
    } else {
      this.HippoIframeService.initializePath(initialPath);
    }
  }

  _initializeChannel(channelId, initialPath, branchId) {
    this.ChannelService.initializeChannel(channelId, branchId)
      .then(() => this.$state.go('hippo-cm.channel'))
      .then(() => this.HippoIframeService.initializePath(initialPath));
  }

  _storeAppState(channelId, initialPath, branchId) {
    sessionStorage.channelId = channelId;
    sessionStorage.channelPath = initialPath;
    sessionStorage.channelBranch = branchId;
  }

  _restoreAppState() {
    if (sessionStorage.channelId) {
      this._initializeChannel(
        sessionStorage.channelId,
        sessionStorage.channelPath,
        sessionStorage.channelBranch,
      );
    }
  }
}

export default HippoCmCtrl;
