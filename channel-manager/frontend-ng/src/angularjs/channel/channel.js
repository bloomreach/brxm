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

import { ChannelCtrl } from './channel.controller';
import { ChannelService } from './channel.service';
import { channelHippoIframeModule } from './hippoIframe/hippoIframe';

function config ($stateProvider) {
  $stateProvider.state('hippo-cm.channel', {
    url: '/channel/:channelId/',
    controller: 'ChannelCtrl as channelCtrl',
    templateUrl: 'channel/channel.html'
  });
}

function run ($state, CmsService, ChannelService) {

  function showChannel(channel) {
    $state.go('hippo-cm.channel', {channelId: channel.id}, {reload:true});
  }

  CmsService.subscribe('load-channel', (channel) => {
    ChannelService.load(channel).then(showChannel); // TODO: handle error.
  });

  // Handle reloading of iframe by BrowserSync during development
  CmsService.publish('reload-channel');
}

export const channelModule = angular
  .module('hippo-cm.channel', [
    'hippo-cm-api',
    channelHippoIframeModule.name,
  ])
  .config(config)
  .controller('ChannelCtrl', ChannelCtrl)
  .service('ChannelService', ChannelService)
  .run(run);


