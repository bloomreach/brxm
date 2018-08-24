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
import channelConfig from './channel.config';
import channelService from './channel.service';

import hippoIframeModule from './hippoIframe/hippoIframe';
import maskModule from './mask/mask.module';
import menuModule from './menu/menu.module';
import overlayToggleModule from './overlayToggle/overlayToggle';
import projectToggleModule from './projectToggle/projectToggle.module';
import relevanceModule from './relevance/relevance';
import sidePanelModule from './sidePanels/sidePanel';
import siteMenuEditorModule from './siteMenuEditor/siteMenuEditor.module';
import subpageModule from './subpage/subpage.module';
import viewportToggleModule from './viewportToggle/viewportToggle.module';

const channelModule = angular
  .module('hippo-cm.channel', [
    hippoIframeModule.name,
    maskModule.name,
    menuModule.name,
    overlayToggleModule.name,
    projectToggleModule.name,
    relevanceModule.name,
    sidePanelModule.name,
    siteMenuEditorModule.name,
    subpageModule.name,
    viewportToggleModule.name,
  ])
  .config(channelConfig)
  .component('channel', channelComponent)
  .service('ChannelService', channelService);

export default channelModule;
