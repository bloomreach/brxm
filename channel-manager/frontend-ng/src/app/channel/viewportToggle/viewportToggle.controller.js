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
    this.setViewports();
    this.activate();
  }

  setViewports() {
    const viewportMap = this.ChannelService.getChannel().viewportMap;
    const viewportWidths = Object.assign({}, DEFAULT_VIEWPORT_WIDTHS, viewportMap);

    this.viewports = [
      {
        id: 'ANY_DEVICE',
        icon: 'any-device',
        width: 0,
      },
      {
        id: 'DESKTOP',
        icon: 'desktop',
        width: viewportWidths.desktop,
      },
      {
        id: 'TABLET',
        icon: 'tablet',
        width: viewportWidths.tablet,
      },
      {
        id: 'PHONE',
        icon: 'phone',
        width: viewportWidths.phone,
      },
    ];
  }

  activate() {
    this.selectedViewport = this.viewports[0];
    this.viewportChanged();
  }

  viewportChanged() {
    this.ViewportService.setWidth(this.selectedViewport.width);
  }

  getDisplayName(viewport) {
    return this.$translate.instant(`VIEWPORT_${viewport.id}`);
  }
}

export default ViewportToggleCtrl;
