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

class PageMoveCtrl {
  constructor(
    $log,
    $translate,
    ChannelService,
    FeedbackService,
    HippoIframeService,
    SiteMapService,
    SiteMapItemService,
  ) {
    'ngInject';

    this.$log = $log;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.SiteMapService = SiteMapService;
    this.SiteMapItemService = SiteMapItemService;
    this.HippoIframeService = HippoIframeService;
    this.FeedbackService = FeedbackService;
  }

  $onInit() {
    this.errorMap = {
      ITEM_ALREADY_LOCKED: 'ERROR_PAGE_LOCKED_BY',
      ITEM_NOT_FOUND: 'ERROR_PAGE_PARENT_MISSING',
      ITEM_NAME_NOT_UNIQUE: 'ERROR_PAGE_PATH_EXISTS',
      ITEM_EXISTS_OUTSIDE_WORKSPACE: 'ERROR_PAGE_PATH_EXISTS',
      INVALID_PATH_INFO: 'ERROR_PAGE_PATH_INVALID',
    };
    this.illegalCharacters = '/ :';
    this.illegalCharactersMessage = this.$translate.instant('VALIDATION_ILLEGAL_CHARACTERS',
      { characters: this.$translate.instant('VALIDATION_ILLEGAL_CHARACTERS_PATH_INFO_ELEMENT') });

    this.isEditable = this.SiteMapItemService.isEditable();
    // The PageMenuService has retrieved the page meta-data when opening the page menu.
    // Now, it is available through the SiteMapItemService.
    this.item = this.SiteMapItemService.get();
    this.lastPathInfoElement = this.item.name;
    this.locations = [];
    this.siteMapId = this.ChannelService.getSiteMapId();
    this.subpageTitle = this.$translate.instant('SUBPAGE_PAGE_MOVE_TITLE', { pageName: this.item.name });

    this.ChannelService.getNewPageModel()
      .then((data) => {
        this.locations = data.locations || [];
        this.location = this.locations.find(location => this.item.parentLocation.id === location.id);

        // filter out locations that are on the current sitemap location, or downstream of it
        if (this.location) {
          const filteredLocations = [];
          const currentUrl = `${this.location.location}${this.item.name}/`;
          this.locations.forEach((location) => {
            if (!location.location.startsWith(currentUrl)) {
              filteredLocations.push(location);
            }
          });
          this.locations = filteredLocations;
        }
      })
      .catch(response => this.FeedbackService.showErrorResponse(response, 'ERROR_PAGE_MODEL_RETRIEVAL_FAILED'));
  }

  move() {
    const item = {
      id: this.item.id,
      parentId: this.location.id,
      name: this.lastPathInfoElement,
    };

    this.SiteMapItemService.updateItem(item, this.siteMapId)
      .then((data) => {
        this.HippoIframeService.load(data.renderPathInfo);
        this.SiteMapService.load(this.siteMapId);
      })
      .then(() => this.ChannelService.checkChanges())
      .then(() => this.onDone())
      .catch(response => this.FeedbackService.showErrorResponse(response, 'ERROR_PAGE_MOVE_FAILED', this.errorMap));
  }
}

export default PageMoveCtrl;
