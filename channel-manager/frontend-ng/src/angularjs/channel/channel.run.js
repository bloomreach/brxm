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

export function run($state, CmsService, ChannelService) {
  'ngInject';

  function showChannel(channel) {
    $state.go('hippo-cm.channel', { channelId: channel.id }, { reload: true });
  }

  CmsService.subscribe('load-channel', (channel) => {
    ChannelService.load(channel).then(showChannel); // TODO: handle error.
    // If this goes wrong, the CM won't work. display a dialog explaining so,
    // and on confirmation, switch back to the channel overview.
  });

  // Handle reloading of iframe by BrowserSync during development
  CmsService.publish('reload-channel');
}
