/*
 * Copyright 2015-2018 Hippo B.V. (http://www.onehippo.com)
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
import ngAnimate from 'angular-animate';
import ngDeviceDetector from 'ng-device-detector';
import ngLocalStorage from 'angular-local-storage';
import ngMaterial from 'angular-material';
import ngMessages from 'angular-messages';
import ngTranslate from 'angular-translate';
import 'angular-translate-loader-static-files';
import uiRouter from '@uirouter/angularjs';

import channelModule from './channel/channel';
import directivesModule from './directives/directives.module';
import factoriesModule from './factories/factories.module';
import filtersModule from './filters/filters.module';
import servicesModule from './services/services.module';

import HstConstants from './constants/hst.constants';

import config from './hippo-cm.config';
import run from './hippo-cm.run';

const hippoCmng = angular
  .module('hippo-cm', [
    ngAnimate,
    ngDeviceDetector,
    ngLocalStorage,
    ngMaterial,
    ngMessages,
    ngTranslate,
    uiRouter,
    channelModule.name,
    directivesModule,
    factoriesModule,
    filtersModule,
    servicesModule,
  ])
  .config(config)
  .constant('HstConstants', HstConstants)
  .run(run);

export default hippoCmng;
