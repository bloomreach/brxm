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

import channelComponent from './channel.component';
import channelHippoIframeModule from './hippoIframe/hippoIframe';
import channelRelevanceModule from './relevance/relevance';
import channelService from './channel.service';
import channelSidePanelModule from './sidePanels/sidePanel';
import channelSubpageModule from './subpage/subpage';
import config from './channel.config';
import maskModule from './mask/mask.module';
import menuModule from './menu/menu.module';
import overlayToggleModule from './overlayToggle/overlayToggle';

import projectToggleModule from './projectToggle/projectToggle.module';
import siteMenuEditorModule from './siteMenuEditor/siteMenuEditor.module';
import viewportsModule from './viewportToggle/viewportToggle.module';

const channelModule = angular
  .module('hippo-cm.channel', [
    channelHippoIframeModule.name,
    channelRelevanceModule.name,
    channelSidePanelModule.name,
    channelSubpageModule.name,
    overlayToggleModule.name,
    maskModule.name,
    menuModule.name,
    projectToggleModule.name,
    siteMenuEditorModule.name,
    viewportsModule.name,
  ])
  .config(config)
  .component('channel', channelComponent)
  .service('ChannelService', channelService);

export default channelModule;
