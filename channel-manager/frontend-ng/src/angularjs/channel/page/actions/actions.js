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

import { pageActionsDirective } from './pageActions.directive';
import { PageActionsCtrl } from './pageActions.controller';
import { pageCreateDirective } from './create/create.directive.js';
import { PageCreateCtrl } from './create/create.controller.js';
import { pageEditDirective } from './edit/edit.directive';
import { PageEditCtrl } from './edit/edit.controller';
import { pageMoveDirective } from './move/move.directive';
import { PageMoveCtrl } from './move/move.controller';
import { pageCopyDirective } from './copy/copy.directive';
import { PageCopyCtrl } from './copy/copy.controller';

export const channelPageActionsModule = angular
  .module('hippo-cm.channel.page.actions', ['ngMessages', 'focus-if'])
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
