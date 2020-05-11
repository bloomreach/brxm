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
    icon: 'mdi-cellphone-link',
    width: 0,
  },
  {
    id: 'desktop',
    icon: 'mdi-monitor',
    width: 1280,
  },
  {
    id: 'tablet',
    icon: 'mdi-tablet',
    width: 720,
  },
  {
    id: 'phone',
    icon: 'mdi-cellphone',
    width: 320,
  },
];

const DEFAULT_VIEWPORT_ICON = 'mdi-devices';

class ViewportToggleCtrl {
  constructor($log, $translate, ChannelService, ViewportService) {
    'ngInject';

    this.$log = $log;
    this.$translate = $translate;
    this.ChannelService = ChannelService;
    this.ViewportService = ViewportService;
  }

  $onInit() {
    const { defaultDevice, devices, viewportMap } = this.ChannelService.getChannel();

    this.values = DEFAULT_VIEWPORTS
      .map((viewport) => {
        const label = this._getViewportLabel(viewport.id);
        const width = viewportMap[viewport.id] || viewport.width;
        return { ...viewport, label, width };
      });

    this._parseJSON(devices)
      .forEach((customViewport) => {
        const viewport = this.values.find(vp => vp.id === customViewport.id.toLowerCase());
        if (viewport) {
          Object.assign(viewport, customViewport);
          return;
        }

        customViewport.label = this._getViewportLabel(customViewport.id);
        customViewport.id = customViewport.id.toLowerCase();
        if (!customViewport.icon) {
          customViewport.icon = DEFAULT_VIEWPORT_ICON;
        }
        this.values.push(customViewport);
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

  _parseJSON(values) {
    return values
      .map((value) => {
        if (!value || !value.trim) {
          return null;
        }

        value = value.trim();
        if (!value.startsWith('{') || !value.endsWith('}')) {
          return null;
        }

        try {
          return JSON.parse(value);
        } catch (e) {
          this.$log.error('Failed to parse viewport JSON blob', value, e);
          return null;
        }
      })
      .filter(Boolean);
  }

  _updateViewport() {
    const { width } = this.values.find(item => item.id === this.value);

    this.ViewportService.setWidth(width);
  }

  /**
   * If the translated value is the same as the lookup key, we assume there is no translation
   * and use the id instead.
   */
  _getViewportLabel(id) {
    const key = `VIEWPORT_${id.toUpperCase()}`;
    const translation = this.$translate.instant(key);
    return translation !== key ? translation : id;
  }
}

export default ViewportToggleCtrl;
