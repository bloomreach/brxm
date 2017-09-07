/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import channelSubpageModule from './subpage/subpage.ng1.module';
import channelPageModule from './page/page.ng1.module';
import channelPageActionsModule from './page/actions/pageActions.ng1.module';
import channelActionsModule from './actions/channelActions.ng1.module';
import channelMenuModule from './menu/menu.ng1.module';
import channelSidePanelModule from './sidePanels/sidePanel.ng1.module';
import channelHippoIframeModule from './hippoIframe/hippoIframe.ng1.module';
import channelSitemenuModule from './sitemenu/editor.ng1.module';
import maskModule from './mask/mask.ng1.module';
import channelRelevanceModule from './relevance/relevance.ng1.module';
import projectToggleModule from './projectToggle/projectToggle.ng1.module';
import viewportsModule from './viewportToggle/viewportToggle.ng1.module';
import overlayToggleModule from './overlayToggle/overlayToggle';
import config from './channel.config';
import ChannelCtrl from './channel.controller';
import ChannelService from './channel.service';
import run from './channel.run';

const channelModule = angular
  .module('hippo-cm.channel', [
    channelSubpageModule.name,
    channelPageModule.name,
    channelPageActionsModule.name,
    channelActionsModule.name,
    channelMenuModule.name,
    channelSidePanelModule.name,
    channelHippoIframeModule.name,
    channelSitemenuModule.name,
    channelRelevanceModule.name,
    projectToggleModule.name,
    viewportsModule.name,
    overlayToggleModule.name,
    maskModule.name,
  ])
  .config(config)
  .controller('ChannelCtrl', ChannelCtrl)
  .service('ChannelService', ChannelService)
  .run(run);

export default channelModule;
