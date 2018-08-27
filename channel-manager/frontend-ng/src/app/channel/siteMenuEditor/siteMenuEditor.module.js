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

import getByPropertyFilter from './tree/getByProperty.filter';
import pickerModule from './picker/picker.module';
import scrollToIfDirective from './scrollToIf.directive';
import selectAllOnFocusDirective from './selectAllOnFocus.directive';
import siteMenuEditorComponent from './siteMenuEditor.component';
import stopPropagationDirective from './stopPropagation.directive';
import uiTreeModule from './tree/tree';

const siteMenuEditorModule = angular
  .module('hippo-cm.channel.siteMenuEditor', [
    'focus-if',
    pickerModule.name,
    uiTreeModule.name,
  ])
  .component('siteMenuEditor', siteMenuEditorComponent)
  .directive('scrollToIf', scrollToIfDirective)
  .directive('selectAllOnFocus', selectAllOnFocusDirective)
  .directive('stopPropagation', stopPropagationDirective)
  .filter('getByProperty', getByPropertyFilter);

export default siteMenuEditorModule;
