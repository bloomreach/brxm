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

import { HttpClientTestingModule } from '@angular/common/http/testing';
import { async, TestBed, waitForAsync } from '@angular/core/testing';
import { Subject } from 'rxjs';
import 'zone.js/dist/zone-patch-rxjs-fake-async';

import { ClientApp } from '../client-app/models/client-app.model';
import { ClientAppService } from '../client-app/services/client-app.service';

import { ConnectionService } from './connection.service';
import { USER_ACTIVITY_DEBOUNCE_TIME } from './user-activity-debounce-time';
import { UserActivityService } from './user-activity.service';

describe('UserActivityService', () => {
  let service: UserActivityService;

  let clientApps: ClientApp[];

  const userActivity$ = new Subject<void>();
  const connectionService = {
    onUserActivity$: userActivity$,
  };

  let clientAppServiceMock = {
    activeApp: undefined,
    apps: [],
  };

  beforeEach(() => {
    clientApps = [
      new ClientApp('http://app1.com', jasmine.createSpyObj('ChildApi1', ['onUserActivity'])),
      new ClientApp('http://app2.com', jasmine.createSpyObj('ChildApi2', ['onUserActivity'])),
      new ClientApp('http://app3.com', jasmine.createSpyObj('ChildApi3', ['onUserActivity'])),
    ];
    clientAppServiceMock.apps = clientApps;

    TestBed.configureTestingModule({
      imports: [
        HttpClientTestingModule,
      ],
      providers: [
        UserActivityService,
        { provide: ConnectionService, useValue: connectionService },
        { provide: ClientAppService, useValue: clientAppServiceMock },
        { provide: USER_ACTIVITY_DEBOUNCE_TIME, useValue: 100 },
      ],
    });

    service = TestBed.inject(UserActivityService);
    clientAppServiceMock = TestBed.inject(ClientAppService);

    service.startPropagation();
  });

  it('should propagate user activity to all apps', () => {
    userActivity$.next();

    expect(clientApps[0].api.onUserActivity).toHaveBeenCalled();
    expect(clientApps[1].api.onUserActivity).toHaveBeenCalled();
    expect(clientApps[2].api.onUserActivity).toHaveBeenCalled();
  });

  describe('after the first propagation has triggered', () => {
    beforeEach(() => {
      clientAppServiceMock.activeApp = clientAppServiceMock.apps[1];

      userActivity$.next();

      (clientApps[0].api.onUserActivity as jasmine.Spy).calls.reset();
      (clientApps[1].api.onUserActivity as jasmine.Spy).calls.reset();
      (clientApps[2].api.onUserActivity as jasmine.Spy).calls.reset();
    });

    it('should not propagate before time delay has passed', () => {
      userActivity$.next();

      expect(clientApps[0].api.onUserActivity).not.toHaveBeenCalled();
      expect(clientApps[1].api.onUserActivity).not.toHaveBeenCalled();
      expect(clientApps[2].api.onUserActivity).not.toHaveBeenCalled();
    });

    it('should propagate next time after the timeout has passed', waitForAsync(() => {
      setTimeout(() => {
        userActivity$.next();

        expect(clientApps[0].api.onUserActivity).toHaveBeenCalled();
        expect(clientApps[1].api.onUserActivity).toHaveBeenCalled();
        expect(clientApps[2].api.onUserActivity).toHaveBeenCalled();
      }, 200);
    }));
  });
});
