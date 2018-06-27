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

import addToProjectTemplate from './addToProject/addToProject.html';
import editContentMainTemplate from './editContentMain.html';
import editContentToolsTemplate from './editContentTools.html';

function config($stateProvider) {
  'ngInject';

  $stateProvider.state({
    name: 'hippo-cm.channel.add-to-project',
    params: {
      documentId: '',
    },
    views: {
      main: {
        controller: 'addToProjectCtrl',
        controllerAs: '$ctrl',
        template: addToProjectTemplate,
      },
      tools: {
        template: '',
      },
    },
  });

  $stateProvider.state({
    name: 'hippo-cm.channel.edit-content',
    params: {
      documentId: '',
    },
    views: {
      main: {
        controller: 'editContentMainCtrl',
        controllerAs: '$ctrl',
        template: editContentMainTemplate,
      },
      tools: {
        controller: 'editContentToolsCtrl',
        controllerAs: '$ctrl',
        template: editContentToolsTemplate,
      },
    },
  });
}

export default config;
