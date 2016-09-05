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

const CHANNEL_LEFT_SIDE_PANEL_ID = 'channel-left-side-panel'; // must match with directive mark-up

export class ChannelLeftSidePanelService {
  constructor($mdSidenav, ScalingService) {
    'ngInject';

    this.$mdSidenav = $mdSidenav;
    this.ScalingService = ScalingService;
  }

  initialize(leftSidePanelJQueryElement) {
    this.leftSidePanelJQueryElement = leftSidePanelJQueryElement;
  }

  toggle() {
    this.$mdSidenav(CHANNEL_LEFT_SIDE_PANEL_ID).toggle();
    this.ScalingService.setPushWidth(this.isOpen() ? this.leftSidePanelJQueryElement.width() : 0);
  }

  isOpen() {
    return this.leftSidePanelJQueryElement && this.$mdSidenav(CHANNEL_LEFT_SIDE_PANEL_ID).isOpen();
  }

  close() {
    if (this.isOpen()) {
      this.$mdSidenav(CHANNEL_LEFT_SIDE_PANEL_ID).close();
      this.ScalingService.setPushWidth(0);
    }
  }
}
