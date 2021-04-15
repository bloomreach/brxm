/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

class PageCopyCtrl {
  constructor(
    $log,
    $translate,
    ChannelService,
    FeedbackService,
    HippoIframeService,
    SessionService,
    SiteMapItemService,
    SiteMapService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$translate = $translate;

    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.SessionService = SessionService;
    this.SiteMapItemService = SiteMapItemService;
    this.SiteMapService = SiteMapService;
  }

  $onInit() {
    this.channel = undefined;
    this.channels = [];
    this.channelId = this.ChannelService.getId();

    this.errorMap = {
      ITEM_CANNOT_BE_CLONED: 'ERROR_PAGE_COPY_TARGET_EXISTS',
    };
    this.illegalCharacters = '/ :';
    this.illegalCharactersMessage = this.$translate.instant(
      'VALIDATION_ILLEGAL_CHARACTERS',
      {
        characters: this.$translate.instant('VALIDATION_ILLEGAL_CHARACTERS_PATH_INFO_ELEMENT'),
      },
    );

    this.isCrossChannelCopyAvailable = false;
    // The PageMenuService has retrieved the page meta-data when opening the page menu.
    // Now, it is available through the SiteMapItemService.
    this.item = this.SiteMapItemService.get();

    this.lastPathInfoElement = '';
    this.locations = [];
    this.siteMapId = this.ChannelService.getSiteMapId();
    this.subpageTitle = this.$translate.instant('SUBPAGE_PAGE_COPY_TITLE', { pageName: this.item.name });

    if (this.SessionService.isCrossChannelPageCopySupported()) {
      this.channels = this.ChannelService.getPageModifiableChannels();
      if (this.channels && (this.channels.length > 1
        || (this.channels.length === 1 && this.channels[0].id !== this.channelId))) {
        this.channel = this.channels.find(channel => channel.id === this.channelId) || this.channels[0];
        this.isCrossChannelCopyAvailable = true;
      }
    }

    this._loadLocations(this.channel ? this.channel.mountId : undefined);
  }

  copy() {
    const headers = {
      siteMapItemUUId: this.item.id,
      targetName: encodeURIComponent(this.lastPathInfoElement),
    };
    if (this.channel) {
      headers.mountId = this.channel.mountId;
    }
    if (this.location.id) {
      headers.targetSiteMapItemUUID = this.location.id;
    }
    this.SiteMapService.copy(this.siteMapId, headers)
      .then((data) => {
        this._returnToNewUrl(data.renderPathInfo, data.pathInfo);
      })
      .catch(response => this.FeedbackService.showErrorResponse(response, 'ERROR_PAGE_COPY_FAILED', this.errorMap));
  }

  channelChanged() {
    this._loadLocations(this.channel.mountId);
  }

  _returnToNewUrl(renderPathInfo, pathInfo) {
    if (this.channel && this.channel.id !== this.channelId) {
      this.ChannelService.initializeChannel(this.channel.id, this.channel.contextPath, this.channel.hostGroup)
        .then(() => {
          this.HippoIframeService.initializePath(pathInfo);
          this.onDone();
        });
    } else {
      this.HippoIframeService.load(renderPathInfo);
      this.SiteMapService.load(this.siteMapId);
      this.ChannelService.checkChanges()
        .then(() => this.onDone());
    }
  }

  _loadLocations(mountId) {
    this.ChannelService.getNewPageModel(mountId)
      .then((data) => {
        this.locations = data.locations || [];
        this.location = this.locations.find(location => this.item.parentLocation.id === location.id);
        if (!this.location && this.locations.length > 0) {
          [this.location] = this.locations;
        }
      })
      .catch(response => this.FeedbackService.showErrorResponse(response, 'ERROR_PAGE_LOCATIONS_RETRIEVAL_FAILED'));
  }
}

export default PageCopyCtrl;
