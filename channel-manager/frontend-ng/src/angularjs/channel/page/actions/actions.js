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
import focusIf from 'ng-focus-if';

import pageActionsDirective from './pageActions.directive';
import PageActionsCtrl from './pageActions.controller';
import pageCreateDirective from './new/new.directive';
import PageCreateCtrl from './new/new.controller';
import pageEditDirective from './properties/properties.directive';
import PageEditCtrl from './properties/properties.controller';
import pageMoveDirective from './move/move.directive';
import PageMoveCtrl from './move/move.controller';
import pageCopyDirective from './copy/copy.directive';
import PageCopyCtrl from './copy/copy.controller';

const channelPageActionsModule = angular
  .module('hippo-cm.channel.page.actions', [
    ngMessages,
    focusIf,
  ])
  .controller('PageActionsCtrl', PageActionsCtrl)
  .directive('pageActions', pageActionsDirective)
  .controller('PageCreateCtrl', PageCreateCtrl)
  .directive('pageCreate', pageCreateDirective)
  .controller('PageEditCtrl', PageEditCtrl)
  .directive('pageEdit', pageEditDirective)
  .controller('PageMoveCtrl', PageMoveCtrl)
  .directive('pageMove', pageMoveDirective)
  .controller('PageCopyCtrl', PageCopyCtrl)
  .directive('pageCopy', pageCopyDirective);

export default channelPageActionsModule;
