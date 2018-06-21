/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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

import channelSubpageModule from './subpage/subpage';
import channelPageModule from './page/page';
import channelPageActionsModule from './page/actions/actions';
import channelActionsModule from './actions/actions';
import channelMenuModule from './menu/menu';
import channelSidePanelModule from './sidePanels/sidePanel';
import channelHippoIframeModule from './hippoIframe/hippoIframe';
import siteMenuEditorModule from './siteMenuEditor/siteMenuEditor.module';
import maskModule from './mask/mask.module';
import channelRelevanceModule from './relevance/relevance';
import projectToggleModule from './projectToggle/projectToggle.module';
import viewportsModule from './viewportToggle/viewportToggle.module';
import overlayToggleModule from './overlayToggle/overlayToggle';
import config from './channel.config';
import ChannelComponent from './channel.component';
import ChannelService from './channel.service';

const channelModule = angular
  .module('hippo-cm.channel', [
    channelSubpageModule.name,
    channelPageModule.name,
    channelPageActionsModule.name,
    channelActionsModule.name,
    channelMenuModule.name,
    channelSidePanelModule.name,
    channelHippoIframeModule.name,
    siteMenuEditorModule.name,
    channelRelevanceModule.name,
    projectToggleModule.name,
    viewportsModule.name,
    overlayToggleModule.name,
    maskModule.name,
  ])
  .config(config)
  .component('channel', ChannelComponent)
  .service('ChannelService', ChannelService);

export default channelModule;
