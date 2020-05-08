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

const DEFAULT_VIEWPORTS = [
  {
    id: 'any_device',
    icon: 'any-device',
    width: 0,
  },
  {
    id: 'desktop',
    icon: 'desktop',
    width: 1280,
  },
  {
    id: 'tablet',
    icon: 'tablet',
    width: 720,
  },
  {
    id: 'phone',
    icon: 'phone',
    width: 320,
  },
];

class ViewportToggleCtrl {
  constructor($translate, ChannelService, ViewportService) {
    'ngInject';

    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.ViewportService = ViewportService;
  }

  $onInit() {
    const { defaultDevice, viewportMap } = this.ChannelService.getChannel();

    this.values = DEFAULT_VIEWPORTS
      .map((viewport) => {
        const width = viewportMap[viewport.id] || viewport.width;
        return { ...viewport, width };
      });

    const selectedViewport = this.value || defaultDevice.toLowerCase();
    this.value = this.values.some(item => item.id === selectedViewport) ? selectedViewport : this.values[0].id;

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
    return this.$translate.instant(`VIEWPORT_${viewport.id.toUpperCase()}`);
  }
}

export default ViewportToggleCtrl;
