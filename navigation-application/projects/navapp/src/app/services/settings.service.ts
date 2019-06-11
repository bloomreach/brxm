import { Injectable } from '@angular/core';

import { AppSettings, NavAppSettings, UserSettings } from '../models';

@Injectable({
  providedIn: 'root',
})
export class SettingsService implements NavAppSettings {
  userSettings: UserSettings;
  appSettings: AppSettings;

  constructor() {
    const settings = (window as any).NavAppSettings;
    Object.assign(this, settings);
  }
}
