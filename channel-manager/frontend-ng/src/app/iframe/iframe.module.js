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

import ngTranslate from 'angular-translate';
import Penpal from 'penpal';
import ModelModule from '../model/model.module';
import ServicesModule from '../services/services.module';
import config from './iframe.config';
import translateLoader from './translate-loader.factory';
import CommunicationService from './communication.service';
import DragDropService from './overlay/drag-drop.service';
import HstCommentsProcessorService from './page/hst-comments-processor.service';
import LinkProcessorService from './overlay/link-processor.service';
import OverlayService from './overlay/overlay.service';
import PageStructureService from './page/page-structure.service';
import ScrollService from './overlay/scroll.service';
import SvgService from './overlay/svg.service';

const iframeModule = angular
  .module('hippo-cm-iframe', [ngTranslate, ModelModule.name, ServicesModule])
  .config(config)
  .constant('Penpal', Penpal)
  .factory('translateLoader', translateLoader)
  .service('CommunicationService', CommunicationService)
  .service('DragDropService', DragDropService)
  .service('HstCommentsProcessorService', HstCommentsProcessorService)
  .service('LinkProcessorService', LinkProcessorService)
  .service('OverlayService', OverlayService)
  .service('PageStructureService', PageStructureService)
  .service('ScrollService', ScrollService)
  .service('SvgService', SvgService)

  // eslint-disable-next-line no-shadow
  .run(($translate, $window, CommunicationService, DragDropService, LinkProcessorService, OverlayService) => {
    'ngInject';

    if (!$window.top || $window === $window.top) {
      return;
    }

    CommunicationService.connect()
      .then(() => CommunicationService.getLocale())
      .then(locale => $translate.use(locale))
      .then(() => DragDropService.initialize())
      .then(() => LinkProcessorService.initialize())
      .then(() => OverlayService.initialize())
      .then(() => CommunicationService.ready());

    // The `unload` event cannot be used here because `event.source` in the target MessageEvent
    // will be `null`. Penpal checks `event.source`, and in this case, it will reject the request.
    // @see https://github.com/Aaronius/penpal/blob/master/src/connectCallReceiver.js#L34
    $window.addEventListener('beforeunload', () => CommunicationService.emit('unload'));
  });

export default iframeModule.name;
