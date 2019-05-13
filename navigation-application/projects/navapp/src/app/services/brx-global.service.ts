import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root',
})
export class BrxGlobalService implements BrxGlobal {
  userSettings: {
    userName: '';
    language: '';
    timeZone: '';
  };

  appSettings: {
    navConfigResources: NavConfigResource[];
  };

  constructor() {
    const BRX: BrxGlobal = (window as any).BRX;
    Object.assign(this, BRX);
  }
}
