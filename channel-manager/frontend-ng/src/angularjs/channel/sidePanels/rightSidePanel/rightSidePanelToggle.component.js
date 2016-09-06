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

export class ChannelRightSidePanelToggleCtrl {
  constructor(ChannelSidePanelService, ChannelService) {
    'ngInject';

    this.ChannelSidePanelService = ChannelSidePanelService;
    this.ChannelService = ChannelService;
  }

  toggleRightSidePanel() {
    this.ChannelSidePanelService.toggle('right');
  }

  isRightSidePanelOpen() {
    return this.ChannelSidePanelService.isOpen('right');
  }
}

const channelRightSidePanelToggleComponentModule = angular
  .module('hippo-cm.channel.rightSidePanelToggleComponentModule', [])
  .component('channelRightSidePanelToggle', {
    bindings: {
      disabled: '=',
    },
    controller: ChannelRightSidePanelToggleCtrl,
    templateUrl: 'channel/sidePanels/rightSidePanel/rightSidePanelToggle.html',
  });

export default channelRightSidePanelToggleComponentModule;
