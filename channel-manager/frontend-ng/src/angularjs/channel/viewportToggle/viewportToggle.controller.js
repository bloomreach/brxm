/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

class ViewportToggleCtrl {
  constructor($translate, OverlayService, ChannelService, ViewportService) {
    'ngInject';

    this.$translate = $translate;
    this.OverlayService = OverlayService;
    this.ChannelService = ChannelService;
    this.ViewportService = ViewportService;
  }

  $onInit() {
    this.setViewports();
    this.activate();
  }

  setViewports() {
    const viewportMap = this.ChannelService.getChannel().viewportMap;

    this.viewports = [
      {
        id: 'ANY_DEVICE',
        icon: 'images/any-device.svg',
        width: 0,
      },
      {
        id: 'DESKTOP',
        icon: 'images/desktop.svg',
        width: viewportMap.desktop ? viewportMap.desktop : 1280,
      },
      {
        id: 'TABLET',
        icon: 'images/tablet.svg',
        width: viewportMap.tablet ? viewportMap.tablet : 720,
      },
      {
        id: 'PHONE',
        icon: 'images/phone.svg',
        width: viewportMap.phone ? viewportMap.phone : 320,
      },
    ];
  }

  activate() {
    this.selectedViewport = this.viewports[0];
    this.viewportChanged();
  }

  viewportChanged() {
    this.ViewportService.setWidth(this.selectedViewport.width);
    this.OverlayService.sync();
  }

  getDisplayName(viewport) {
    return this.$translate.instant(`VIEWPORT_${viewport.id}`);
  }
}

export default ViewportToggleCtrl;
