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

import ngMessages from 'angular-messages';

import channelActionsDirective from './channelActions.directive';
import ChannelActionsCtrl from './channelActions.controller';
import channelSettingsDirective from './settings/settings.directive';
import ChannelSettingsCtrl from './settings/settings.controller';
import propertyField from './settings/property/property.component';
import helpIconDirective from './settings/helpIcon/helpIcon.directive';

const channelActionsModule = angular
  .module('hippo-cm.channel.actions', [
    ngMessages,
  ])
  .controller('ChannelActionsCtrl', ChannelActionsCtrl)
  .directive('channelActions', channelActionsDirective)
  .controller('ChannelSettingsCtrl', ChannelSettingsCtrl)
  .directive('channelSettings', channelSettingsDirective)
  .component('propertyField', propertyField)
  .directive('helpIcon', helpIconDirective);

export default channelActionsModule;
