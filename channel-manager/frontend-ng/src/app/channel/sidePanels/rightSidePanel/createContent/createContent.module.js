/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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

import config from './createContent.config';
import createContentService from './createContent.service';
import step1Component from './step1/step1.component';

const createContentModule = angular
  .module('hippo-cm.channel.rightSidePanel.createContentModule', [])
  .config(config)
  .service('CreateContentService', createContentService)
  .component('createContentStep1', step1Component);

export default createContentModule.name;

