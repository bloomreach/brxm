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

import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, flushMicrotasks, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { ClientErrorCodes, NavigationTrigger } from '@bloomreach/navapp-communication';

import { AppComponent } from './app.component';
import { AppState } from './services/app-state';
import { ChildApiMethodsService } from './services/child-api-methods.service';
import { CommunicationService } from './services/communication.service';
import { NavigatorService } from './services/navigator.service';

describe('AppComponent', () => {
  let component: AppComponent;
  let fixture: ComponentFixture<AppComponent>;
  let de: DebugElement;

  let communicationServiceMock: jasmine.SpyObj<CommunicationService>;
  let childApiMethodsServiceMock: jasmine.SpyObj<ChildApiMethodsService>;
  let navigatorServiceMock: jasmine.SpyObj<NavigatorService>;
  let stateMock: AppState;

  const childApiMethods = {};

  beforeEach(async () => {
    communicationServiceMock = jasmine.createSpyObj('CommunicationService', {
      connect: Promise.resolve(),
      navigateTo: undefined,
      toggleMask: undefined,
      sendError: undefined,
      notifyAboutUserActivity: undefined,
    });
    (communicationServiceMock as any).parentApiVersion = '1.1.1';

    childApiMethodsServiceMock = jasmine.createSpyObj('ChildApiMethodsService', {
      getMethods: childApiMethods,
    });

    navigatorServiceMock = jasmine.createSpyObj('NavigatorService', {
      getMethods: childApiMethods,
    });

    stateMock = {
      isBrSmMock: true,
      navigatedTo: {
        path: '/path/to',
        pathPrefix: '/some/prefix',
      },
      lastNavigationTriggeredBy: NavigationTrigger.Menu,
      navigateCount: 3,
      buttonClickedCounter: 10,
      userActivityReported: 20,
      overlaid: false,
      generateAnErrorUponLogout: true,
      selectedSiteId: {
        siteId: 30,
        accountId: 50,
      },
    } as any;

    fixture = TestBed.configureTestingModule({
      declarations: [AppComponent],
      providers: [
        { provide: CommunicationService, useValue: communicationServiceMock },
        { provide: ChildApiMethodsService, useValue: childApiMethodsServiceMock },
        { provide: NavigatorService, useValue: navigatorServiceMock },
        { provide: AppState, useValue: stateMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(AppComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    stateMock = TestBed.inject(AppState);

    await component.ngOnInit();

    await fixture.detectChanges();
  });

  it('should initialize the component', () => {
    expect(communicationServiceMock.connect).toHaveBeenCalledWith(childApiMethods);
    expect(component.parentApiVersion).toBe('1.1.1');
  });

  it('should output the current state', () => {
    const stateDe = de.query(By.css('.state'));
    const stateText = stateDe.nativeElement.textContent;

    const locationText = 'Current client app location';
    const locationValue = `${location}`;
    expect(stateText).toContain(`${locationText}${locationValue}`);

    const versionText = 'The parent window uses navapp comm lib version';
    const versionValue = '1.1.1';
    expect(stateText).toContain(`${versionText}${versionValue}`);

    const navigatedToText = 'Last time navigated to';
    const navigatedToValue = 'path: "/path/to", client app appUrl path: "/some/prefix"';
    expect(stateText).toContain(`${navigatedToText}${navigatedToValue}`);

    const lastNavigationTriggerText = 'Last navigation was triggered by';
    const lastNavigationTriggerValue = NavigationTrigger.Menu;
    expect(stateText).toContain(`${lastNavigationTriggerText}${lastNavigationTriggerValue}`);

    const navigationCounterText = 'Navigation counter';
    const navigationCounterValue = 3;
    expect(stateText).toContain(`${navigationCounterText}${navigationCounterValue}`);

    const buttonClickedCounterText = 'Button click counter (and user activity trigger)';
    const buttonClickedCounterValue = 10;
    expect(stateText).toContain(`${buttonClickedCounterText}${buttonClickedCounterValue}`);

    const userActivityReportedText = 'User activity reported counter (from navapp)';
    const userActivityReportedValue = 20;
    expect(stateText).toContain(`${userActivityReportedText}${userActivityReportedValue}`);

    const selectedSiteIdText = 'The selected siteId';
    const selectedSiteIdValue = 30;
    expect(stateText).toContain(`${selectedSiteIdText}${selectedSiteIdValue}`);

    const selectedSiteIdContinuationText = 'and accountId';
    const selectedSiteIdContinuationValue = 50;
    expect(stateText).toContain(`${selectedSiteIdContinuationText}${selectedSiteIdContinuationValue}`);

    const generateAnErrorUponLogoutText = 'Generate an error upon logout';
    const generateAnErrorUponLogoutValue = 'true';
    expect(stateText).toContain(`${generateAnErrorUponLogoutText}${generateAnErrorUponLogoutValue}`);
  });

  describe('actions', () => {
    describe('simple button click', () => {
      beforeEach(() => {
        const buttonDe = de.query(By.css('.simple-button'));
        buttonDe.triggerEventHandler('click', {});
      });

      it('should increment the counter', () => {
        expect(stateMock.buttonClickedCounter).toBe(11);
      });

      it('should notify about user activity', () => {
        expect(communicationServiceMock.notifyAboutUserActivity).toHaveBeenCalled();
      });
    });

    describe('toggle overlay', () => {
      beforeEach(() => {
        const buttonDe = de.query(By.css('.toggle-overlay-button'));
        buttonDe.triggerEventHandler('click', {});
      });

      it('should toggle the mask', () => {
        expect(communicationServiceMock.toggleMask).toHaveBeenCalled();
      });
    });
  });

  describe('navigation', () => {
    beforeEach(() => {
      const buttonDe = de.query(By.css('.navigate-button'));
      const internalPathInputDe = de.query(By.css('.internal-path-input'));
      const breadcrumbNameDe = de.query(By.css('.breadcrumb-name-input'));

      internalPathInputDe.nativeElement.value = '/some/path';
      breadcrumbNameDe.nativeElement.value = 'custom breadcrumb label';

      buttonDe.triggerEventHandler('click', {});
    });

    it('should trigger navigation', () => {
      expect(communicationServiceMock.navigateTo).toHaveBeenCalledWith('/some/path', 'custom breadcrumb label');
    });
  });

  describe('trigger an error', () => {
    describe('showError', () => {
      beforeEach(() => {
        const buttonDe = de.query(By.css('.show-error-button'));
        const errorMessageDe = de.query(By.css('.error-message-input'));

        errorMessageDe.nativeElement.value = 'some error message';

        buttonDe.triggerEventHandler('click', {});
      });

      it('should hand over an error', () => {
        expect(communicationServiceMock.sendError).toHaveBeenCalledWith(ClientErrorCodes.InternalError, 'some error message');
      });
    });

    describe('sendNotAuthorizedError', () => {
      beforeEach(() => {
        const buttonDe = de.query(By.css('.send-not-authenticated-error-button'));
        buttonDe.triggerEventHandler('click', {});
      });

      it('should hand over an not authorized error', () => {
        expect(communicationServiceMock.sendError).toHaveBeenCalledWith(ClientErrorCodes.NotAuthorizedError, 'Not authenticated');
      });
    });

    describe('toggleLogoutErrorState', () => {
      beforeEach(() => {
        const buttonDe = de.query(By.css('.toggle-logout-error-state-button'));
        buttonDe.triggerEventHandler('click', {});
      });

      it('should toggle logout behaviour', () => {
        expect(stateMock.generateAnErrorUponLogout).toBe(false);
      });
    });
  });
});
