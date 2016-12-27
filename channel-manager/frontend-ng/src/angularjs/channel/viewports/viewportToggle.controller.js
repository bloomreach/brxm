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

class ViewportToggleCtrl {
  constructor($translate, ScalingService, ViewportService) {
    'ngInject';

    this.$translate = $translate;
    this.ScalingService = ScalingService;
    this.ViewportService = ViewportService;

    this.viewports = [
      {
        id: 'DESKTOP',
        icon: 'images/desktop.svg',
        width: 0,
      },
      {
        id: 'TABLET',
        icon: 'images/tablet.svg',
        width: 720,
      },
      {
        id: 'PHONE',
        icon: 'images/phone.svg',
        width: 320,
      },
    ];

    this.selectedViewport = this.viewports[0];
  }

  getDisplayName(viewport) {
    return this.$translate.instant(`VIEWPORT_${viewport.id}`);
  }

  viewportChanged() {
    this.ViewportService.setWidth(this.selectedViewport.width);
    this.ScalingService.sync();
  }
}

export default ViewportToggleCtrl;
