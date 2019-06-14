import { TestBed } from '@angular/core/testing';

import { NavAppSettingsService } from './navapp-settings.service';

describe('NavappSettingService', () => {
  function setup(): {
    navAppSettingsService: NavAppSettingsService;
    userName: string;
  } {
    const userName = 'Frank Zappa';

    (window as any).NavAppSettings = {
      userSettings: {
        userName,
        language: 'en',
        timeZone: 'Europe/Amsterdam',
      },
      appSettings: {
        navConfigResources: [
          {
            resourceType: 'IFRAME',
            url: 'http://localhost:4201',
          },
          {
            resourceType: 'REST',
            url: 'http://localhost:4201/assets/navitems.json',
          },
        ],
      },
    };

    TestBed.configureTestingModule({
      providers: [NavAppSettingsService],
    });

    return {
      navAppSettingsService: TestBed.get(NavAppSettingsService),
      userName,
    };
  }

  it('should take the settings object from the window', () => {
    const { userName, navAppSettingsService } = setup();

    expect(navAppSettingsService.userSettings.userName).toEqual(userName);
  });
});
