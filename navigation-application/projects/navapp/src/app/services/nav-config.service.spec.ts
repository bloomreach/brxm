import { HttpClient } from '@angular/common/http';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';

import { TestBed } from '@angular/core/testing';

import { NavConfigService } from './nav-config.service';
import { NavAppSettingsService } from './navapp-settings.service';

describe('NavConfigService', () => {
  function setup(): {
    http: HttpClient;
    httpTestingCtrl: HttpTestingController;
    navConfigService: NavConfigService;
    navAppSettings: NavAppSettingsService;
  } {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NavAppSettingsService, NavConfigService],
    });

    return {
      http: TestBed.get(HttpClient),
      httpTestingCtrl: TestBed.get(HttpTestingController),
      navConfigService: TestBed.get(NavConfigService),
      navAppSettings: TestBed.get(NavAppSettingsService),
    };
  }

  it('should exist', () => {
    const { navConfigService } = setup();
    expect(navConfigService).toBeDefined();
  });
});
