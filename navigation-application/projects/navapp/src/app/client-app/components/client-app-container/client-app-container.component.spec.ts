/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Component, DebugElement, Input, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject, Subject } from 'rxjs';

import { ClientApp } from '../../models/client-app.model';
import { ClientAppService } from '../../services/client-app.service';
import { ClientAppComponent } from '../client-app/client-app.component';

import { ClientAppContainerComponent } from './client-app-container.component';

@Component({
  selector: 'brna-client-app',
  template: '',
  providers: [
    {
      provide: ClientAppComponent,
      useExisting: ClientAppMockComponent,
    },
  ],
})
export class ClientAppMockComponent {
  @Input()
  url: string;
}

describe('ClientAppContainerComponent', () => {
  let component: ClientAppContainerComponent;
  let fixture: ComponentFixture<ClientAppContainerComponent>;
  let de: DebugElement;

  let urlsSubject: Subject<string[]>;
  let clientAppServiceMock: jasmine.SpyObj<ClientAppService>;

  beforeEach(waitForAsync(() => {
    urlsSubject = new BehaviorSubject<string[]>(['mytesturl1', 'mytesturl2']);

    clientAppServiceMock = {
      apps: [],
      urls$: urlsSubject,
      activeApp: undefined,
    } as any;

    fixture = TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
      ],
      declarations: [
        ClientAppContainerComponent,
        ClientAppMockComponent,
      ],
      providers: [
        { provide: ClientAppService, useValue: clientAppServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(ClientAppContainerComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    clientAppServiceMock = TestBed.inject(ClientAppService) as jasmine.SpyObj<ClientAppService>;

    fixture.detectChanges();
  }));

  it('should create client app nodes and pass the url as an input', () => {
    const clientApps = de.queryAll(By.directive(ClientAppComponent));

    expect(clientApps.length).toEqual(2);
    expect(clientApps[0].componentInstance.url).toBe('mytesturl1');
    expect(clientApps[1].componentInstance.url).toBe('mytesturl2');
  });

  it('should keep created client app nodes hidden initially', () => {
    const clientApps = de.queryAll(By.directive(ClientAppComponent));

    expect(clientApps[0].classes.hidden).toBeTruthy();
    expect(clientApps[1].classes.hidden).toBeTruthy();
  });

  it('should show the second client app node', () => {
    (clientAppServiceMock as any).activeApp = new ClientApp('mytesturl2', {});

    fixture.detectChanges();

    const clientApps = de.queryAll(By.css('brna-client-app'));

    expect(clientApps[0].classes.hidden).toBeTruthy();
    expect(clientApps[1].classes.hidden).toBeFalsy();
  });

  it('should update client app nodes', () => {
    urlsSubject.next(['mytesturl1', 'mytesturl3', 'mytesturl4']);

    fixture.detectChanges();

    const clientApps = de.queryAll(By.directive(ClientAppComponent));

    expect(clientApps.length).toEqual(3);
    expect(clientApps[0].componentInstance.url).toBe('mytesturl1');
    expect(clientApps[1].componentInstance.url).toBe('mytesturl3');
    expect(clientApps[2].componentInstance.url).toBe('mytesturl4');
  });
});
