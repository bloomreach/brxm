import { ChildPromisedApi } from '@bloomreach/navapp-communication';

export class ClientApp {
  id: string;
  api: ChildPromisedApi;

  constructor(public url: string) {
    this.id = url;
  }
}
