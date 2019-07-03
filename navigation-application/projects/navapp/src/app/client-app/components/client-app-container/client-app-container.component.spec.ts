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

import { NO_ERRORS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';

import { ClientApp } from '../../models/client-app.model';
import { ClientAppService } from '../../services/client-app.service';

import { ClientAppContainerComponent } from './client-app-container.component';

describe('ClientAppContainerComponent', () => {
  let component: ClientAppContainerComponent;
  let fixture: ComponentFixture<ClientAppContainerComponent>;

  const testApp = new ClientApp('mytesturl');
  const testApp2 = new ClientApp('mytesturl2');

  const clientAppService = {
    apps$: of([testApp, testApp2]),
    activeApp$: of(testApp),
  };

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [{ provide: ClientAppService, useValue: clientAppService }],
      declarations: [ClientAppContainerComponent],
      schemas: [NO_ERRORS_SCHEMA],
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ClientAppContainerComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create client app nodes and pass the app as input', () => {
    const el: HTMLElement = fixture.nativeElement;
    const clientApps = el.querySelectorAll('brna-client-app');
    expect(clientApps.length).toEqual(2);
    expect((clientApps[0] as any).app).toBe(testApp);
  });
});
