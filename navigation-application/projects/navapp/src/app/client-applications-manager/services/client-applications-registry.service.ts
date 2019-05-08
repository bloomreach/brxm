/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
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
