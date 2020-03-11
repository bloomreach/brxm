/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
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

import angular from 'angular';
import Penpal from 'penpal';
import ModelModule from '../model/model.module';
import ServicesModule from '../services/services.module';
import config from './iframe.config';
import CommunicationService from './communication.service';
import HstCommentsProcessorService from './page/hst-comments-processor.service';
import PageStructureService from './page/page-structure.service';
import ScrollService from './overlay/scroll.service';

const iframeModule = angular
  .module('hippo-cm-iframe', [ModelModule.name, ServicesModule])
  .config(config)
  .constant('Penpal', Penpal)
  .service('CommunicationService', CommunicationService)
  .service('HstCommentsProcessorService', HstCommentsProcessorService)
  .service('PageStructureService', PageStructureService)
  .service('ScrollService', ScrollService)

  // eslint-disable-next-line no-shadow
  .run(($rootScope, $window, CommunicationService) => {
    'ngInject';

    CommunicationService.connect();

    $rootScope.$on('page:change', () => CommunicationService.emit('page:change'));

    // The `unload` event cannot be used here because `event.source` in the target MessageEvent
    // will be `null`. Penpal checks `event.source`, and in this case, it will reject the request.
    // @see https://github.com/Aaronius/penpal/blob/master/src/connectCallReceiver.js#L34
    $window.addEventListener('beforeunload', () => CommunicationService.emit('unload'));
  });

export default iframeModule.name;
