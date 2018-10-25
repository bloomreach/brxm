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

import { StickyStatesPlugin } from '@uirouter/sticky-states';
import pageInfoMainTemplate from './pageInfoMain.html';

function config($stateProvider, $uiRouterProvider) {
  'ngInject';

  $uiRouterProvider.plugin(StickyStatesPlugin);

  $stateProvider.state({
    name: 'hippo-cm.channel.page-info',
    abstract: true,
    params: {
      pageUrl: {
        type: 'string',
        dynamic: true,
      },
    },
    views: {
      tools: {
        template: '', // no tools
      },
      main: {
        controller: 'pageInfoMainCtrl',
        controllerAs: '$ctrl',
        template: pageInfoMainTemplate,
      },
    },
  });
}

export default config;
