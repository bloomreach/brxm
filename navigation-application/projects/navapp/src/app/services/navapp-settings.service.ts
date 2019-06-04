import { Injectable } from '@angular/core';

import { NavAppSettings, NavConfigResource } from '../models';

import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class NavAppSettingsService implements NavAppSettings {
  userSettings: {
    userName: '';
    language: '';
    timeZone: '';
  };

  appSettings: {
    navConfigResources: NavConfigResource[];
  };

  constructor() {
    let settings: NavAppSettings;

    if (environment.production) {
      settings = (window as any).NavAppSettings;
    } else {
      settings = environment.NavAppSettings;
    }
    Object.assign(this, settings);
  }
}
