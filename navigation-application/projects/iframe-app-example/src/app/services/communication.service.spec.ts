/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { fakeAsync, flushMicrotasks, TestBed } from '@angular/core/testing';
import * as navCommLib from '@bloomreach/navapp-communication';

import { AppState } from './app-state';
import { CommunicationService } from './communication.service';

describe('CommunicationService', () => {
  let service: CommunicationService;
  let stateMock: AppState;
  let parentApiMock: jasmine.SpyObj<navCommLib.ParentApi>;

  beforeEach(async () => {
    stateMock = {
      navigatedTo: undefined,
      overlaid: false,
    } as any;

    parentApiMock = jasmine.createSpyObj('ParentApi', {
      getConfig: Promise.resolve({
        apiVersion: '1.1.1',
      }),
      navigate: undefined,
      updateNavLocation: undefined,
      showMask: Promise.resolve(),
      hideMask: Promise.resolve(),
      onError: undefined,
      onUserActivity: undefined,
    });

    spyOnProperty(navCommLib, 'connectToParent', 'get').and.returnValue(() => Promise.resolve(parentApiMock));

    TestBed.configureTestingModule({
      providers: [
        CommunicationService,
        { provide: AppState, useValue: stateMock },
      ],
    });

    service = TestBed.get(CommunicationService);
    stateMock = TestBed.get(AppState);

    return service.connect({});
  });

  it('should return parent api version', () => {
    expect(service.parentApiVersion).toBe('1.1.1');
  });

  describe('navigateTo()', () => {
    it('should call parentApi.navigate', () => {
      service.navigateTo('/some/path', 'breadcrumb label');

      expect(parentApiMock.navigate).toHaveBeenCalledWith({ path: '/some/path', breadcrumbLabel: 'breadcrumb label' });
    });

    it('should call parentApi.updateNavLocation if it is internal navigation', () => {
      stateMock.navigatedTo = { path: '/some/path' };

      service.navigateTo('/some/path/to', 'some label');

      expect(parentApiMock.updateNavLocation).toHaveBeenCalledWith({ path: '/some/path/to', breadcrumbLabel: 'some label' });
    });
  });

  describe('toggleMask()', () => {
    it('should call parentApi.showMask', () => {
      service.toggleMask();

      expect(parentApiMock.showMask).toHaveBeenCalled();
    });

    it('should set state.overlaid = true', fakeAsync(() => {
      service.toggleMask();

      flushMicrotasks();

      expect(stateMock.overlaid).toBeTruthy();
    }));

    it('should call parentApi.hideMask', () => {
      stateMock.overlaid = true;

      service.toggleMask();

      expect(parentApiMock.hideMask).toHaveBeenCalled();
    });

    it('should set state.overlaid = false', fakeAsync(() => {
      stateMock.overlaid = true;

      service.toggleMask();

      flushMicrotasks();

      expect(stateMock.overlaid).toBeFalsy();
    }));
  });

  describe('sendError()', () => {
    it('should send an error', () => {
      const expected = {
        errorCode: 500,
        message: 'Some message',
      };

      service.sendError(expected.errorCode, expected.message);

      expect(parentApiMock.onError).toHaveBeenCalledWith(expected);
    });
  });

  describe('notifyAboutUserActivity()', () => {
    it('should send user activity event', () => {
      service.notifyAboutUserActivity();

      expect(parentApiMock.onUserActivity).toHaveBeenCalled();
    });
  });
});
