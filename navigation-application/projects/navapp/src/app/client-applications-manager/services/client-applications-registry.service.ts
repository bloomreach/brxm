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

import { Injectable } from '@angular/core';

import { ClientApplicationHandler } from '../models';

@Injectable()
export class ClientApplicationsRegistryService {
  private iframes = new Map<string, ClientApplicationHandler>();

  set(id: string, handler: ClientApplicationHandler): void {
    this.iframes.set(id, handler);
  }

  has(id: string): boolean {
    return this.iframes.has(id);
  }

  get(id: string): ClientApplicationHandler {
    return this.iframes.get(id);
  }

  getAll(): ClientApplicationHandler[] {
    return Array.from(this.iframes.values());
  }
}
