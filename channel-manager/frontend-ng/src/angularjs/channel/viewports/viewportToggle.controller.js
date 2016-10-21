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
  constructor($translate, OverlaySyncService) {
    'ngInject';

    this.$translate = $translate;
    this.OverlaySyncService = OverlaySyncService;

    this.viewPorts = [
      {
        id: 'DESKTOP',
        icon: '/cms/angular/hippo-cm/images/desktop.svg',
        width: 0,
      },
      {
        id: 'TABLET',
        icon: '/cms/angular/hippo-cm/images/tablet.svg',
        width: 720,
      },
      {
        id: 'PHONE',
        icon: '/cms/angular/hippo-cm/images/phone.svg',
        width: 320,
      },
    ];

    this.activate();
  }

  activate() {
    this.selectedViewPort = this.viewPorts[0];
    this.viewPortChanged();
  }

  getDisplayName(viewport) {
    return this.$translate.instant(`VIEWPORT_${viewport.id}`);
  }

  viewPortChanged() {
    this.OverlaySyncService.setViewPortWidth(this.selectedViewPort.width);
    this.OverlaySyncService.syncIframe();
  }
}

export default ViewportToggleCtrl;
