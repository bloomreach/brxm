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

import hippoIframe from './hippoIframe.directive';
import HippoIframeCtrl from './hippoIframe.controller';
import RenderingService from './rendering/rendering.service';
import DragDropService from './dragDrop/dragDrop.service';
import HippoIframeService from './hippoIframe.service';
import HstCommentsProcessorService from './processing/hstCommentsProcessor.service';
import LinkProcessorService from './processing/linkProcessor.service';
import OverlayService from './overlay/overlay.service';
import ScrollService from './scrolling/scroll.service';
import SpaService from './spa/spa.service';
import ViewportService from './viewport/viewport.service';

const channelHippoIframeModule = angular
  .module('hippo-cm.channel.hippoIframe', [])
  .controller('hippoIframeCtrl', HippoIframeCtrl)
  .directive('hippoIframe', hippoIframe)
  .service('DragDropService', DragDropService)
  .service('HippoIframeService', HippoIframeService)
  .service('HstCommentsProcessorService', HstCommentsProcessorService)
  .service('LinkProcessorService', LinkProcessorService)
  .service('OverlayService', OverlayService)
  .service('RenderingService', RenderingService)
  .service('ScrollService', ScrollService)
  .service('SpaService', SpaService)
  .service('ViewportService', ViewportService);

export default channelHippoIframeModule;
