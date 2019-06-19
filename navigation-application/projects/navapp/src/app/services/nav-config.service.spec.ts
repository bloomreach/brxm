/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { HttpClient } from '@angular/common/http';
import {
  HttpClientTestingModule,
  HttpTestingController,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { GlobalSettingsService } from './global-settings.service';
import { NavConfigService } from './nav-config.service';

describe('NavConfigService', () => {
  function setup(): {
    http: HttpClient;
    httpTestingCtrl: HttpTestingController;
    navConfigService: NavConfigService;
    navAppSettings: GlobalSettingsService;
  } {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [GlobalSettingsService, NavConfigService],
    });

    return {
      http: TestBed.get(HttpClient),
      httpTestingCtrl: TestBed.get(HttpTestingController),
      navConfigService: TestBed.get(NavConfigService),
      navAppSettings: TestBed.get(GlobalSettingsService),
    };
  }

  it('should exist', () => {
    const { navConfigService } = setup();
    expect(navConfigService).toBeDefined();
  });
});
