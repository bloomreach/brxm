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
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { BehaviorSubject, Subject } from 'rxjs';

import { NavigationService } from '../../../services/navigation.service';
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

  constructor() {
    spyOn(this, 'reloadAndConnect');
  }

  reloadAndConnect(): void {}
}

describe('ClientAppContainerComponent', () => {
  let component: ClientAppContainerComponent;
  let fixture: ComponentFixture<ClientAppContainerComponent>;
  let de: DebugElement;

  let urlsSubject: Subject<string[]>;
  let clientAppServiceMock: jasmine.SpyObj<ClientAppService>;
  let navigationServiceMock: NavigationService;

  beforeEach(waitForAsync(() => {
    urlsSubject = new BehaviorSubject<string[]>(['mytesturl1', 'mytesturl2']);

    clientAppServiceMock = {
      apps: [],
      urls$: urlsSubject,
      activeApp: undefined,
    } as any;

    navigationServiceMock = {
      navigating$: new Subject<boolean>(),
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
        { provide: NavigationService, useValue: navigationServiceMock },
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

  it('should keep created client app nodes hidden', () => {
    const clientApps = de.queryAll(By.directive(ClientAppComponent));

    expect(clientApps[0].classes.hidden).toBeTruthy();
    expect(clientApps[1].classes.hidden).toBeTruthy();
  });

  it('should show the second client app node', fakeAsync(() => {
    (clientAppServiceMock as any).activeApp = new ClientApp('mytesturl2', {});

    fixture.detectChanges();

    tick();

    const clientApps = de.queryAll(By.css('brna-client-app'));

    expect(clientApps[0].classes.hidden).toBeTruthy();
    expect(clientApps[1].classes.hidden).toBeFalsy();
  }));

  it('should update client app nodes', () => {
    urlsSubject.next(['mytesturl1', 'mytesturl3', 'mytesturl4']);

    const clientApps = de.queryAll(By.directive(ClientAppComponent));

    expect(clientApps.length).toEqual(3);
    expect(clientApps[0].componentInstance.url).toBe('mytesturl1');
    expect(clientApps[1].componentInstance.url).toBe('mytesturl3');
    expect(clientApps[2].componentInstance.url).toBe('mytesturl4');
  });

  it('should call reloadAndConnect for client app nodes which are not in clientAppService.apps', () => {
    (clientAppServiceMock as any).apps = [
      new ClientApp('mytesturl1', {}),
    ];

    urlsSubject.next(['mytesturl1', 'mytesturl3', 'mytesturl4']);

    const childComponents = component.clientAppComponents.toArray();

    expect(childComponents[0].url).toBe('mytesturl1');
    expect(childComponents[0].reloadAndConnect).not.toHaveBeenCalled();
    expect(childComponents[1].url).toBe('mytesturl3');
    expect(childComponents[1].reloadAndConnect).toHaveBeenCalled();
    expect(childComponents[2].url).toBe('mytesturl4');
    expect(childComponents[2].reloadAndConnect).toHaveBeenCalled();
  });
});
