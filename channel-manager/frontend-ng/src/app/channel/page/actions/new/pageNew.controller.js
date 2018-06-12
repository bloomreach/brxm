/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

class PageNewCtrl {
  constructor(
    $scope,
    $translate,
    ChannelService,
    FeedbackService,
    HippoIframeService,
    lowercaseFilter,
    SiteMapService,
  ) {
    'ngInject';

    this.$scope = $scope;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.lowercaseFilter = lowercaseFilter;
    this.SiteMapService = SiteMapService;
  }

  $onInit() {
    this.errorMap = {
      ITEM_ALREADY_LOCKED: 'ERROR_PAGE_LOCKED_BY',
      ITEM_NOT_IN_PREVIEW: 'ERROR_PAGE_PARENT_MISSING',
      ITEM_NAME_NOT_UNIQUE: 'ERROR_PAGE_PATH_EXISTS',
      INVALID_PATH_INFO: 'ERROR_PAGE_PATH_INVALID',
    };
    this.illegalCharacters = '/ :';
    this.illegalCharactersMessage = this.$translate.instant('VALIDATION_ILLEGAL_CHARACTERS',
      { characters: this.$translate.instant('VALIDATION_ILLEGAL_CHARACTERS_PATH_INFO_ELEMENT') });
    this.locations = [];
    this.prototypes = [];
    this.siteMapId = this.ChannelService.getSiteMapId();
    this.updateLastPathInfoElementAutomatically = true;

    this.ChannelService.getNewPageModel()
      .then((data) => {
        this.prototypes = data.prototypes;
        this.prototype = (data.prototypes.length > 0) ? data.prototypes[0] : undefined;
        this.locations = data.locations;
        this.location = (data.locations.length > 0) ? data.locations[0] : undefined;
      })
      .catch(response => this.FeedbackService.showErrorResponse(response, 'ERROR_PAGE_MODEL_RETRIEVAL_FAILED'));

    this.$scope.$watch('$ctrl.title', () => {
      if (this.updateLastPathInfoElementAutomatically) {
        this.lastPathInfoElement = this._replaceIllegalCharacters(this.lowercaseFilter(this.title), '-');
      }
    });
  }

  create() {
    const page = {
      pageTitle: this.title,
      name: this.lastPathInfoElement,
      componentConfigurationId: this.prototype.id,
    };
    const parentSiteMapItemId = this.location.id || undefined;

    this.SiteMapService.create(this.siteMapId, parentSiteMapItemId, page)
      .then((data) => {
        this.HippoIframeService.load(data.renderPathInfo);
        this.SiteMapService.load(this.siteMapId);
        this.ChannelService.recordOwnChange();
        this.onDone();
      })
      .catch(response => this.FeedbackService.showErrorResponse(response, 'ERROR_PAGE_CREATION_FAILED', this.errorMap));
  }

  disableAutomaticLastPathInfoElementUpdate() {
    this.updateLastPathInfoElementAutomatically = false;
  }

  _replaceIllegalCharacters(value, replacement) {
    value = value || '';

    angular.forEach(this.illegalCharacters, (character) => {
      while (value.indexOf(character) >= 0) {
        const index = value.indexOf(character);
        value = `${value.substr(0, index)}${replacement}${value.substr(index + 1)}`;
      }
    });
    return value;
  }
}

export default PageNewCtrl;
