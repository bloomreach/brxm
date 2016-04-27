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

import { channelActionsDirective } from './channelActions.directive';
import { ChannelActionsCtrl } from './channelActions.controller';
import { channelEditDirective } from './edit/edit.directive';
import { ChannelEditCtrl } from './edit/edit.controller';

export const channelActionsModule = angular
  .module('hippo-cm.channel.actions', ['ngMessages'])
  .controller('ChannelActionsCtrl', ChannelActionsCtrl)
  .directive('channelActions', channelActionsDirective)
  .controller('ChannelEditCtrl', ChannelEditCtrl)
  .directive('channelEdit', channelEditDirective);
