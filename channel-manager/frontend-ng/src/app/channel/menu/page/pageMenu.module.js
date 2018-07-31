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

import focusIf from 'ng-focus-if';
import ngMessages from 'angular-messages';

import illegalCharactersDirective from './illegalCharacters.directive';
import pageCopyComponent from './copy/pageCopy.component';
import PageMenuService from './pageMenu.service';
import pageMoveComponent from './move/pageMove.component';
import pageNewComponent from './new/pageNew.component';
import pagePropertiesComponent from './properties/pageProperties.component';

const pageMenuModule = angular
  .module('hippo-cm.channel.menu.page', [
    ngMessages,
    focusIf,
  ])
  .component('pageCopy', pageCopyComponent)
  .component('pageMove', pageMoveComponent)
  .component('pageNew', pageNewComponent)
  .component('pageProperties', pagePropertiesComponent)
  .directive('illegalCharacters', illegalCharactersDirective)
  .service('PageMenuService', PageMenuService);

export default pageMenuModule;
