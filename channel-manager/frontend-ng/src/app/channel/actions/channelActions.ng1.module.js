/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import channelActionsService from './channelActions.service';
import channelSettingsDirective from './settings/settings.directive';
import ChannelSettingsCtrl from './settings/settings.controller';
import propertyField from './settings/propertyField/propertyField.component';
import helpIconDirective from './settings/helpIcon/helpIcon.directive';
import changeManagementDirective from './manageChanges/manageChanges.directive';
import ChangeManagementCtrl from './manageChanges/manageChanges.controller';

const channelActionsModule = angular
  .module('hippo-cm.channel.actions', [
    ngMessages,
  ])
  .controller('ChannelSettingsCtrl', ChannelSettingsCtrl)
  .directive('channelSettings', channelSettingsDirective)
  .component('propertyField', propertyField)
  .directive('helpIcon', helpIconDirective)
  .controller('ChangeManagementCtrl', ChangeManagementCtrl)
  .directive('changeManagement', changeManagementDirective)
  .service('ChannelActionsService', channelActionsService);

export default channelActionsModule;
