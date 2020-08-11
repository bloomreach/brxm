/*
 * Copyright 2016-2018 Hippo B.V. (http://www.onehippo.com)
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

import ChangeManagementComponent from './manageChanges/manageChanges.component';
import ChannelSettingsComponent from './settings/settings.component';
import HintIconComponent from './settings/hintIcon/hintIcon.component';
import PropertyFieldComponent from './settings/propertyField/propertyField.component';
import ChangeManagementCtrl from './manageChanges/manageChanges.controller';
import ChannelSettingsCtrl from './settings/settings.controller';
import RejectPromptCtrl from './rejectPrompt/reject-prompt.controller';
import channelMenuService from './channelMenu.service';

const channelMenuModule = angular
  .module('hippo-cm.channel.menu.channel', [
    ngMessages,
  ])
  .component('changeManagement', ChangeManagementComponent)
  .component('channelSettings', ChannelSettingsComponent)
  .component('hintIcon', HintIconComponent)
  .component('propertyField', PropertyFieldComponent)
  .controller('ChangeManagementCtrl', ChangeManagementCtrl)
  .controller('ChannelSettingsCtrl', ChannelSettingsCtrl)
  .controller('RejectPromptCtrl', RejectPromptCtrl)
  .service('ChannelMenuService', channelMenuService);

export default channelMenuModule;
