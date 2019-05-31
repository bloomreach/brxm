/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Injectable } from '@angular/core';
import { ChildPromisedApi } from '@bloomreach/navapp-communication';

@Injectable({
  providedIn: 'root',
})
export class ConnectionService {
  private connections: Map<string, Promise<ChildPromisedApi>> = new Map();

  addConnection(
    appURL: string,
    appConnection: Promise<ChildPromisedApi>,
  ): void {
    this.connections.set(appURL, appConnection);
  }

  getConnection(id: string): Promise<ChildPromisedApi> {
    if (!this.connections.has(id)) {
      throw new Error(`There is no connection to an ifrane with id = ${id}`);
    }

    return this.connections.get(id);
  }
}
