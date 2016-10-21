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

import RenderingService from './rendering.service';
import ExtJsHandlerService from './extJsHandler.service';
import PageMetaDataService from './pageMetaData.service';
import PageStructureService from './pageStructure.service';
import run from './page.run';

const channelPageModule = angular
  .module('hippo-cm.channel.page', [])
  .service('RenderingService', RenderingService)
  .service('ExtJsHandlerService', ExtJsHandlerService)
  .service('PageMetaDataService', PageMetaDataService)
  .service('PageStructureService', PageStructureService)
  .run(run);

export default channelPageModule;
