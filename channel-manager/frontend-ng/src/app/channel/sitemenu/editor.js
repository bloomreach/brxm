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

import menuEditorDirective from './editor.directive';
import selectAllOnFocusDirective from './selectAllOnFocus.directive';
import MenuEditorCtrl from './editor.controller';
import uiTreeModule from './tree/tree';
import pickerModule from './picker/picker';

const channelSitemenuModule = angular
  .module('hippo-cm.channel.sitemenu', [
    uiTreeModule.name,
    pickerModule.name,
    'focus-if',
  ])
  .directive('menuEditor', menuEditorDirective)
  .directive('selectAllOnFocus', selectAllOnFocusDirective)
  .controller('MenuEditorCtrl', MenuEditorCtrl);

export default channelSitemenuModule;
