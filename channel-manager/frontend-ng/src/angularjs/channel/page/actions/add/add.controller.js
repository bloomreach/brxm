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

export class PageAddCtrl {
  constructor($element, $scope, $translate, ChannelService, SiteMapService, HippoIframeService,
              FeedbackService, lowercaseFilter) {
    'ngInject';

    this.ChannelService = ChannelService;
    this.SiteMapService = SiteMapService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;

    this.prototypes = [];
    this.locations = [];
    this.feedbackParent = $element.find('.feedback-parent');
    this.updateLastPathInfoElementAutomatically = true;
    this.siteMapId = ChannelService.getSiteMapId();
    this.illegalCharacters = '/ :';
    this.illegalCharactersMessage = $translate.instant('VALIDATION_ILLEGAL_CHARACTERS',
            { characters: $translate.instant('SUBPAGE_PAGE_ADD_VALIDATION_ILLEGAL_CHARACTERS') });

    $scope.$watch('pageAdd.title', () => {
      if (this.updateLastPathInfoElementAutomatically) {
        this.lastPathInfoElement = this._replaceIllegalCharacters(lowercaseFilter(this.title), '-');
      }
    });

    ChannelService.getNewPageModel()
      .then((data) => {
        this.prototypes = data.prototypes;
        this.prototype = (data.prototypes.length > 0) ? data.prototypes[0] : undefined;
        this.locations = data.locations;
        this.location = (data.locations.length > 0) ? data.locations[0] : undefined;
      })
      .catch(() => {
        this._showError('ERROR_PAGE_MODEL_RETRIEVAL_FAILED');
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
      .catch(() => {
        this._showError('ERROR_PAGE_CREATION_FAILED');
      });
  }

  back() {
    this.onDone();
  }

  disableAutomaticLastPathInfoElementUpdate() {
    this.updateLastPathInfoElementAutomatically = false;
  }

  _showError(key, params) {
    this.FeedbackService.showError(key, params, this.feedbackParent);
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
