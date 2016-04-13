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

import { channelPageModule } from './page/page';
import { channelSidenavModule } from './sidenav/sidenav';
import { channelHippoIframeModule } from './hippoIframe/hippoIframe';
import { channelMaskModule } from './mask/mask';
import { channelRelevanceModule } from './relevance/relevance';
import { config } from './channel.config';
import { ChannelCtrl } from './channel.controller';
import { ChannelService } from './channel.service';
import { run } from './channel.run';
import { CatalogComponentDirective } from './catalog.component.directive';

export const channelModule = angular
  .module('hippo-cm.channel', [
    'hippo-cm-api',
    channelPageModule.name,
    channelSidenavModule.name,
    channelHippoIframeModule.name,
    channelMaskModule.name,
    channelRelevanceModule.name,
  ])
  .config(config)
  .controller('ChannelCtrl', ChannelCtrl)
  .service('ChannelService', ChannelService)
  .directive('catalogComponent', CatalogComponentDirective)
  .run(run);
