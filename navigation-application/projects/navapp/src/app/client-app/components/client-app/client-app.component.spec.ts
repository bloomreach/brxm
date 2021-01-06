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

import { DebugElement, SecurityContext } from '@angular/core';
import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By, DomSanitizer } from '@angular/platform-browser';
import { ChildApi } from '@bloomreach/navapp-communication';

import { Connection } from '../../../models/connection.model';
import { FailedConnection } from '../../../models/failed-connection.model';
import { ConnectionService } from '../../../services/connection.service';
import { ClientAppService } from '../../services/client-app.service';

import { ClientAppComponent } from './client-app.component';

describe('ClientAppComponent', () => {
  let component: ClientAppComponent;
  let fixture: ComponentFixture<ClientAppComponent>;
  let iframeDe: DebugElement;

  let domSanitizerMock: jasmine.SpyObj<DomSanitizer>;
  let connectionServiceMock: jasmine.SpyObj<ConnectionService>;
  let clientAppServiceMock: jasmine.SpyObj<ClientAppService>;

  let resolveIframeConnection: (value: ChildApi) => any;
  let rejectIframeConnection: (reason?: string) => any;

  beforeEach(async(() => {
    domSanitizerMock = jasmine.createSpyObj('DomSanitizer', {
      sanitize: 'sanitized-url',
      bypassSecurityTrustResourceUrl: 'sanitized-url',
    });

    connectionServiceMock = jasmine.createSpyObj('ConnectionService', {
      connectToIframe: new Promise((resolve, reject) => {
        resolveIframeConnection = resolve;
        rejectIframeConnection = reject;
      }),
    });

    clientAppServiceMock = jasmine.createSpyObj('ClientAppService', [
      'addConnection',
    ]);

    fixture = TestBed.configureTestingModule({
      providers: [
        { provide: DomSanitizer, useValue: domSanitizerMock },
        { provide: ConnectionService, useValue: connectionServiceMock },
        { provide: ClientAppService, useValue: clientAppServiceMock },
      ],
      declarations: [ClientAppComponent],
    }).createComponent(ClientAppComponent);

    component = fixture.componentInstance;
    iframeDe = fixture.debugElement.query(By.css('iframe'));

    component.url = 'some-url';

    component.ngOnInit();

    fixture.detectChanges();
  }));

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should sanitize the provided url', () => {
    expect(domSanitizerMock.sanitize).toHaveBeenCalledWith(SecurityContext.URL, 'some-url');
    expect(domSanitizerMock.bypassSecurityTrustResourceUrl).toHaveBeenCalledWith('sanitized-url');
  });

  describe('connection to the iframe', () => {
    it('should be initiated', () => {
      expect(connectionServiceMock.connectToIframe).toHaveBeenCalledWith(iframeDe.nativeElement);
    });

    it('should add a connection if iframe has been successfully connected', fakeAsync(() => {
      const api = {};
      const expected = new Connection(iframeDe.nativeElement.src, api);

      resolveIframeConnection(api);

      tick();

      expect(clientAppServiceMock.addConnection).toHaveBeenCalledWith(expected);
    }));

    it('should add a failed connection if there is an error', fakeAsync(() => {
      const expected = new FailedConnection(iframeDe.nativeElement.src, 'some reason');

      rejectIframeConnection('some reason');

      tick();

      expect(clientAppServiceMock.addConnection).toHaveBeenCalledWith(expected);
    }));
  });

  describe('reloadAndConnect', () => {
    it('should reload the iframe', () => {
      const spy = spyOnProperty(iframeDe.nativeElement, 'src', 'set');

      component.reloadAndConnect();

      expect(spy).toHaveBeenCalledWith('sanitized-url');
    });

    describe('connection to the iframe', () => {
      it('should be initiated', () => {
        expect(connectionServiceMock.connectToIframe).toHaveBeenCalledWith(iframeDe.nativeElement);
      });

      it('should add a connection if iframe has been successfully connected', fakeAsync(() => {
        const api = {};
        const expected = new Connection(iframeDe.nativeElement.src, api);

        resolveIframeConnection(api);

        tick();

        expect(clientAppServiceMock.addConnection).toHaveBeenCalledWith(expected);
      }));

      it('should add a failed connection if there is an error', fakeAsync(() => {
        const expected = new FailedConnection(iframeDe.nativeElement.src, 'some reason');

        rejectIframeConnection('some reason');

        tick();

        expect(clientAppServiceMock.addConnection).toHaveBeenCalledWith(expected);
      }));
    });
  });
});
