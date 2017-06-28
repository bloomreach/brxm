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

class LeftSidePanelToggleCtrl {
  constructor($translate, SidePanelService, ChannelService) {
    'ngInject';

    this.$translate = $translate;
    this.SidePanelService = SidePanelService;
    this.ChannelService = ChannelService;
  }

  $onInit() {
    this.tooltipTranslations = {
      open: this.$translate.instant('TOOLBAR_BUTTON_LEFT_SIDE_PANEL_OPEN'),
      close: this.$translate.instant('TOOLBAR_BUTTON_LEFT_SIDE_PANEL_CLOSE'),
    };
  }

  getTooltipTranslation() {
    return this.isLeftSidePanelOpen() ? this.tooltipTranslations.close : this.tooltipTranslations.open;
  }

  toggleLeftSidePanel() {
    this.SidePanelService.toggle('left');
  }

  isLeftSidePanelOpen() {
    return this.SidePanelService.isOpen('left');
  }
}

export default LeftSidePanelToggleCtrl;
