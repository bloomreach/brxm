import { Injectable } from '@angular/core';

import { GlobalSettings } from '../models';

@Injectable({
  providedIn: 'root',
})
export class NavAppSettingsService extends GlobalSettings {
  constructor() {
    super();

    const settings = (window as any).NavAppSettings;
    Object.assign(this, settings);
  }
}
