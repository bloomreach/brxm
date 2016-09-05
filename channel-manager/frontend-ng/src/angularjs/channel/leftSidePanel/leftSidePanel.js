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

import { ChannelLeftSidePanelService } from './leftSidePanel.service.js';
import { ChannelLeftSidePanelToggleCtrl } from './toggle.controller';
import { channelLeftSidePanelToggleDirective } from './toggle.directive';
import { ChannelLeftSidePanelCtrl } from './leftSidePanel.controller.js';
import { channelLeftSidePanelDirective } from './leftSidePanel.directive.js';

export const channelLeftSidePanelModule = angular
  .module('hippo-cm.channel.leftSidePanel', [])
  .service('ChannelLeftSidePanelService', ChannelLeftSidePanelService)
  .controller('ChannelLeftSidePanelToggleCtrl', ChannelLeftSidePanelToggleCtrl)
  .directive('channelLeftSidePanelToggle', channelLeftSidePanelToggleDirective)
  .controller('ChannelLeftSidePanelCtrl', ChannelLeftSidePanelCtrl)
  .directive('channelLeftSidePanel', channelLeftSidePanelDirective);
