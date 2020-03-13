/*
 * Copyright 2016-2020 Hippo B.V. (http://www.onehippo.com)
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

import hippoIframeComponent from './hippoIframe.component';
import CommunicationService from './spa/communication.service';
import ContainerService from './container/container.service';
import HippoIframeService from './hippoIframe.service';
import PageModule from './page/page.module';
import RpcService from './spa/rpc.service';
import ScrollService from './scrolling/scroll.service';
import SpaService from './spa/spa.service';
import ViewportService from './viewport/viewport.service';

const channelHippoIframeModule = angular
  .module('hippo-cm.channel.hippoIframe', [
    PageModule.name,
  ])
  .component('hippoIframe', hippoIframeComponent)
  .service('CommunicationService', CommunicationService)
  .service('ContainerService', ContainerService)
  .service('HippoIframeService', HippoIframeService)
  .service('RpcService', RpcService)
  .service('ScrollService', ScrollService)
  .service('SpaService', SpaService)
  .service('ViewportService', ViewportService);

export default channelHippoIframeModule;
