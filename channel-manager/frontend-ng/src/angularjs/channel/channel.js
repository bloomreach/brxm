/*
 * Copyright 2015-2016 Hippo B.V. (http://www.onehippo.com)
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
import channelChangesModule from './changes/changes';
import channelPageModule from './page/page';
import channelPageActionsModule from './page/actions/actions';
import channelActionsModule from './actions/actions';
import channelSidePanelModule from './sidePanels/sidePanel';
import channelHippoIframeModule from './hippoIframe/hippoIframe';
import channelMenuModule from './menu/editor';
import maskModule from './mask/mask.module';
import channelRelevanceModule from './relevance/relevance';
import channelViewportsModule from './viewports/viewports';
import config from './channel.config';
import ChannelCtrl from './channel.controller';
import ChannelService from './channel.service';
import run from './channel.run';

const channelModule = angular
  .module('hippo-cm.channel', [
    channelSubpageModule.name,
    channelChangesModule.name,
    channelPageModule.name,
    channelPageActionsModule.name,
    channelActionsModule.name,
    channelSidePanelModule.name,
    channelHippoIframeModule.name,
    channelMenuModule.name,
    channelRelevanceModule.name,
    channelViewportsModule.name,
    maskModule.name,
  ])
  .config(config)
  .controller('ChannelCtrl', ChannelCtrl)
  .service('ChannelService', ChannelService)
  .run(run);

export default channelModule;
