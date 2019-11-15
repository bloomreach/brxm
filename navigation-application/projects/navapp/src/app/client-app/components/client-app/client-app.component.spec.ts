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

import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ConnectionService } from '../../../services/connection.service';
import { ClientAppService } from '../../services/client-app.service';

import { ClientAppComponent } from './client-app.component';

describe('ClientAppComponent', () => {
  let component: ClientAppComponent;
  let fixture: ComponentFixture<ClientAppComponent>;

  const mockConnection = {
    api: {},
  };

  const clientAppServiceMock = jasmine.createSpyObj(['addConnection']);

  const connectionService = jasmine.createSpyObj('ConnectionService', {
    connectToIframe: Promise.resolve(mockConnection),
  });

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: ConnectionService, useValue: connectionService },
      ],
      declarations: [ClientAppComponent],
    });
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ClientAppComponent);
    component = fixture.componentInstance;
    component.url = '';
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should sanitize the provided url', () => {
    spyOn(console, 'warn');
    component.url = 'Javascript:alert(\'xss\')';
    fixture.detectChanges();
    component.ngOnInit();
    // tslint:disable-next-line: no-console
    expect(console.warn).toHaveBeenCalledWith(
      'WARNING: sanitizing unsafe URL value Javascript:alert(\'xss\') (see http://g.co/ng/security#xss)',
    );
  });
});
