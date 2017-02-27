/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
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
  constructor($log, $translate, ChannelService, SessionService, SiteMapService, SiteMapItemService, HippoIframeService,
    FeedbackService) {
    'ngInject';

    this.$log = $log;
    this.ChannelService = ChannelService;
    this.SiteMapService = SiteMapService;
    this.HippoIframeService = HippoIframeService;
    this.FeedbackService = FeedbackService;

    this.locations = [];
    this.channels = [];
    this.siteMapId = ChannelService.getSiteMapId();
    this.channelId = ChannelService.getId();
    this.illegalCharacters = '/ :';
    this.illegalCharactersMessage = $translate.instant('VALIDATION_ILLEGAL_CHARACTERS',
      { characters: $translate.instant('VALIDATION_ILLEGAL_CHARACTERS_PATH_INFO_ELEMENT') });
    this.errorMap = {
      ITEM_CANNOT_BE_CLONED: 'ERROR_PAGE_COPY_TARGET_EXISTS',
    };

    // The PageActionsCtrl has retrieved the page meta-data when opening the page menu.
    // Now, it is available through the SiteMapItemService.
    this.item = SiteMapItemService.get();
    this.lastPathInfoElement = '';
    this.subpageTitle = $translate.instant('SUBPAGE_PAGE_COPY_TITLE', { pageName: this.item.name });

    if (SessionService.isCrossChannelPageCopySupported()) {
      this.channels = ChannelService.getPageModifiableChannels();
      if (this.channels && (this.channels.length > 1 ||
        (this.channels.length === 1 && this.channels[0].id !== this.channelId))) {
        this.channel = this.channels.find(channel => channel.id === this.channelId) || this.channels[0];
        this.isCrossChannelCopyAvailable = true;
      }
    }

    this._loadLocations(this.channel ? this.channel.mountId : undefined);
  }

  copy() {
    const headers = {
      siteMapItemUUId: this.item.id,
      targetName: this.lastPathInfoElement,
    };
    if (this.channel) {
      headers.mountId = this.channel.mountId;
    }
    if (this.location.id) {
      headers.targetSiteMapItemUUID = this.location.id;
    }
    this.SiteMapService.copy(this.siteMapId, headers)
      .then((data) => {
        this._returnToNewUrl(data.renderPathInfo);
      })
      .catch(response => this.FeedbackService.showErrorResponseOnSubpage(response, 'ERROR_PAGE_COPY_FAILED', this.errorMap));
  }

  _returnToNewUrl(renderPathInfo) {
    if (this.channel && this.channel.id !== this.channelId) {
      this.ChannelService.switchToChannel(this.channel.contextPath, this.channel.id)
        .then(() => {
          this.HippoIframeService.load(renderPathInfo);
          this.onDone();
        })
        .catch(() => {
          this.onDone();
          // this error message will show on the main page
          this.FeedbackService.showError('ERROR_CHANNEL_SWITCH_FAILED');
        });
    } else {
      this.HippoIframeService.load(renderPathInfo);
      this.SiteMapService.load(this.siteMapId);
      this.ChannelService.recordOwnChange();
      this.onDone();
    }
  }

  channelChanged() {
    this._loadLocations(this.channel.mountId);
  }

  _loadLocations(mountId) {
    this.ChannelService.getNewPageModel(mountId)
      .then((data) => {
        this.locations = data.locations || [];
        this.location = this.locations.find(location => this.item.parentLocation.id === location.id);
        if (!this.location && this.locations.length > 0) {
          this.location = this.locations[0];
        }
      })
      .catch(response => this.FeedbackService.showErrorResponseOnSubpage(response, 'ERROR_PAGE_LOCATIONS_RETRIEVAL_FAILED'));
  }
}

export default PageCopyCtrl;
