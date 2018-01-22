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

function config($stateProvider) {
  'ngInject';

  $stateProvider.state({
    name: 'hippo-cm.channel.create-content-step-1',
    url: 'create-content-step-1',
    params: {
      config: {},
    },
    views: {
      tools: {
        template: '', // no tools
      },
      main: 'step1Component',
    },
  });

  $stateProvider.state({
    name: 'hippo-cm.channel.create-content-step-2',
    url: 'create-content-step-2',
    params: {
      document: {},
      step1: {},
    },
    views: {
      tools: {
        template: '', // no tools
      },
      main: 'step2Component',
    },
  });
}

export default config;
