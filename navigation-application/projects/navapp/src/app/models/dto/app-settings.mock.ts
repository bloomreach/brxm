/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { AppSettings } from './app-settings.dto';
import { ConfigResource } from './config-resource.dto';
import { ConfigResourceMock } from './config-resource.mock';

export class AppSettingsMock implements AppSettings {
  basePath = '/base/path';
  initialPath = '/initial/path';
  navAppResourceLocation = '/testLocation';
  navConfigResources = [
    new ConfigResourceMock({
      resourceType: 'IFRAME',
      url: '/testIFRAMEurl',
    }) as ConfigResource,
    new ConfigResourceMock({
      resourceType: 'REST',
      url: '/testRESTurl',
    }) as ConfigResource,
    new ConfigResourceMock({
      resourceType: 'INTERNAL_REST',
      url: '/internalRESTurl',
    }) as ConfigResource,
  ];
  iframesConnectionTimeout = 30000;
  logLevel = 'DEBUG' as any;
  usageStatisticsEnabled = true;

  loginResources = [
    new ConfigResourceMock({
      resourceType: 'IFRAME',
      url: '/testLoginResource1',
    }) as ConfigResource,
    new ConfigResourceMock({
      resourceType: 'IFRAME',
      url: '/testLoginResource2',
    }) as ConfigResource,
  ];

  logoutResources = [
    new ConfigResourceMock({
      resourceType: 'IFRAME',
      url: '/testLogoutResource1',
    }) as ConfigResource,
    new ConfigResourceMock({
      resourceType: 'IFRAME',
      url: '/testLogoutResource2',
    }) as ConfigResource,
  ];

  constructor(initObject = {}) {
    Object.keys(initObject).forEach(key => {
      this[key] = initObject[key];
    });
  }
}
