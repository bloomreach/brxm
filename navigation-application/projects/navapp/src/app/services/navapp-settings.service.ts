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
    const settings = (window as any).NavAppSettings;
    Object.assign(this, settings);
  }
}
