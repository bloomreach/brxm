/*!
 * Copyright 2021 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { InjectionToken } from '@angular/core';
import { ClientError, NavLocation, ParentConfig, UserSettings } from '@bloomreach/navapp-communication';

export interface Ng1NavappService {
  subscribe: (api: string, callback: () => Promise<any>) => void;
  unsubscribe: (api: string, callback: () => Promise<any>) => void;
  connect: () => Promise<void>;
  getConfig(): Promise<ParentConfig>;
  getUserSettings(): Promise<UserSettings>;
  updateNavLocation(location: NavLocation): Promise<void>;
  navigate(location: NavLocation): Promise<void>;
  showMask(): Promise<void>;
  hideMask(): Promise<void>;
  showBusyIndicator(): Promise<void>;
  hideBusyIndicator(): Promise<void>;
  onUserActivity(): Promise<void>;
  onSessionExpired(): Promise<void>;
  onError(error: ClientError): Promise<void>;
}

export const NG1_NAVAPP_SERVICE = new InjectionToken<Ng1NavappService>('NG1_NAVAPP_SERVICE');
