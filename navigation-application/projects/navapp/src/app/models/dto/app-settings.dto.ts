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

import { NgxLoggerLevel } from 'ngx-logger';

import { ConfigResource } from './config-resource.dto';

/**
 * AppSettings is provided in the NavAppSettings to configure settings for the navapp
 */
export interface AppSettings {
  /**
   * The initial path that the navapp should navigate to after loading.
   */
  initialPath?: string;

  /**
   * The path that the <base href=""> is set to. Usually a URL like the webpack dev server or a CDN or other location.
   * The navapp scripts are loaded from this path.
   */
  basePath: string;

  /**
   * The path the navapp should navigate to when the user logs out (or is logged out programmatically)
   */
  loginPath?: string;

  /**
   * The path where the resources of the navapp are located. This points to the location of the assets folder of the navapp.
   * The assets folder normally contains files such as svg's.
   */
  navAppResourceLocation: string;

  /**
   * An array of [[ConfigResource]] to fetch nav items and site items from.
   */
  navConfigResources: ConfigResource[];

  /**
   * An array of [[ConfigResource]] to perform logins.
   */
  loginResources: ConfigResource[];

  /**
   * An array of [[ConfigResource]] to perform logouts.
   */
  logoutResources: ConfigResource[];

  /**
   * The time in milliseconds after which a timeout will occur when trying to connect to an iframe @{link {ConfigResource}}.
   */
  iframesConnectionTimeout: number;

  /**
   * Level of logging to be done by the NgxLogger
   */
  logLevel: keyof typeof NgxLoggerLevel;

  /**
   * Enable or disable usage statistics tracking.
   */
  usageStatisticsEnabled: boolean;
}
