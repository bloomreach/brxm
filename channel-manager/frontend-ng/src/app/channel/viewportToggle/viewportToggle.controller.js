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

const DEFAULT_VIEWPORT_WIDTHS = {
  desktop: 1280,
  tablet: 720,
  phone: 320,
};

class ViewportToggleCtrl {
  constructor($translate, ChannelService, ViewportService) {
    'ngInject';

    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.ViewportService = ViewportService;
  }

  $onInit() {
    const viewportMap = this.ChannelService.getChannel().viewportMap;
    const widths = Object.assign({}, DEFAULT_VIEWPORT_WIDTHS, viewportMap);

    this.values = [
      {
        id: 'ANY_DEVICE',
        icon: 'any-device',
        width: 0,
      },
      {
        id: 'DESKTOP',
        icon: 'desktop',
        width: widths.desktop,
      },
      {
        id: 'TABLET',
        icon: 'tablet',
        width: widths.tablet,
      },
      {
        id: 'PHONE',
        icon: 'phone',
        width: widths.phone,
      },
    ];

    if (!this.values.some(item => item.id === this.value)) {
      this.value = this.values[0].id;
    }

    this._updateViewport();
  }

  onChange() {
    this._updateViewport();
    if (this.ngModel) {
      this.ngModel.$setViewValue(this.value);
    }
  }

  _updateViewport() {
    const { width } = this.values.find(item => item.id === this.value);

    this.ViewportService.setWidth(width);
  }

  getDisplayName(viewport) {
    return this.$translate.instant(`VIEWPORT_${viewport.id}`);
  }
}

export default ViewportToggleCtrl;
