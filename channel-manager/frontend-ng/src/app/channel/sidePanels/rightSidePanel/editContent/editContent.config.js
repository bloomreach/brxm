/*
 * Copyright 2018-2020 Hippo B.V. (http://www.onehippo.com)
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
    name: 'hippo-cm.channel.add-to-project',
    params: {
      documentId: '',
      nextState: '',
    },
    views: {
      main: {
        component: 'addToProject',
      },
      tools: {
        template: '',
      },
      icon: {
        template: '', // no icon
      },
    },
  });

  $stateProvider.state({
    name: 'hippo-cm.channel.edit-page-unavailable',
    params: {
      title: '',
    },
    views: {
      main: {
        component: 'editPageUnavailable',
      },
      tools: {
        template: '',
      },
      icon: {
        template: '', // no icon
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
        component: 'editContentMain',
      },
      tools: {
        component: 'editContentTools',
      },
      icon: {
        component: 'editContentIcon',
      },
    },
  });

  $stateProvider.state({
    name: 'hippo-cm.channel.edit-page',
    abstract: true,
    params: {
      documentId: '',
      lastModified: '',
    },
    views: {
      main: {
        component: 'contentTabs',
      },
      tools: {
        component: 'editContentTools',
      },
      icon: {
        component: 'editContentIcon',
      },
    },
  }).state({
    name: 'hippo-cm.channel.edit-page.content',
    component: 'editContentMain',
  }).state({
    name: 'hippo-cm.channel.edit-page.versions',
    template: '<em-versions-info></em-versions-info>',
  });
}

export default config;
