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

import uiTree from 'angular-ui-tree';
import hippoTreeDirective from './tree.directive';
import itemsRendererDirective from './itemsRenderer/itemsRenderer.directive';
import hippoTreeTemplateDirective from './tree.template.directive';
import HippoTreeCtrl from './tree.controller';

const uiTreeModule = angular
  .module('hippo-cm.ui.tree', [
    uiTree,
  ])
  .directive('hippoTree', hippoTreeDirective)
  .directive('itemsRenderer', itemsRendererDirective)
  .directive('hippoTreeTemplate', hippoTreeTemplateDirective)
  .controller('HippoTreeCtrl', HippoTreeCtrl);

export default uiTreeModule;
