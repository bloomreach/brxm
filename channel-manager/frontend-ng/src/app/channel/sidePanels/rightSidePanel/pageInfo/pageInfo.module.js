/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import config from './pageInfo.config';
import iframeExtensionComponent from './iframeExtension.component';
import pageInfoMainCtrl from './pageInfoMain.controller';
import PageInfoService from './pageInfo.service';

const pageInfoModule = angular
  .module('hippo-cm.channel.pageInfo', [])
  .config(config)
  .service('PageInfoService', PageInfoService)
  .controller('pageInfoMainCtrl', pageInfoMainCtrl)
  .component('iframeExtension', iframeExtensionComponent);

export default pageInfoModule.name;
