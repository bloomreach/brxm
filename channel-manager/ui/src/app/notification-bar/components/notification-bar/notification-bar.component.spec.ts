/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { BehaviorSubject, Subject } from 'rxjs';

import { DocumentState } from '../../../models/document-state.enum';
import { XPageStatusInfo } from '../../../models/page-status-info.model';
import { XPageStatus } from '../../../models/xpage-status.enum';
import { PageService } from '../../../services/page.service';

import { NotificationBarComponent } from './notification-bar.component';

describe('NotificationBarComponent', () => {
  let component: NotificationBarComponent;
  let fixture: ComponentFixture<NotificationBarComponent>;

  let pageStatusInfo$: Subject<XPageStatusInfo | undefined>;

  beforeEach(() => {
    pageStatusInfo$ = new BehaviorSubject<XPageStatusInfo | undefined>(new XPageStatusInfo(
      XPageStatus.Published,
      DocumentState.Live,
      'some xpage document',
    ));

    const pageServiceMock = {
      pageStatusInfo$,
    };

    fixture = TestBed.configureTestingModule({
      declarations: [NotificationBarComponent],
      providers: [
        { provide: PageService, useValue: pageServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(NotificationBarComponent);

    component = fixture.componentInstance;
  });

  it('should show the component', () => {
    fixture.detectChanges();

    expect(fixture.nativeElement).toMatchSnapshot();
  });

  describe.each([
    ['RejectedRequest', new XPageStatusInfo(
      XPageStatus.RejectedRequest,
      DocumentState.Unpublished,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_REQUEST_REJECTED'],
    ['ProjectPageRejected', new XPageStatusInfo(
      XPageStatus.ProjectPageRejected,
      DocumentState.Live,
      'some page',
      undefined,
      'project name',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_PAGE_REJECTED'],
    ['EditingSharedContainers', new XPageStatusInfo(
      XPageStatus.EditingSharedContainers,
      DocumentState.Live,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_EDITING_SHARED_CONTAINERS'],
    ['Locked', new XPageStatusInfo(
      XPageStatus.Locked,
      DocumentState.Live,
      'some page',
      undefined,
      undefined,
      undefined,
      'username',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_LOCKED_BY_USER'],
  ])('if xpage status is %s', (statusName, statusInfo, expectedText) => {
    beforeEach(waitForAsync(() => {
      pageStatusInfo$.next(statusInfo);

      fixture.detectChanges();
    }));

    /*test(`should use danger color as background`, () => {
      expect(component.danger).toBeTruthy();
    });*/
  });

  describe.each([
    ['Published', new XPageStatusInfo(
      XPageStatus.Published,
      DocumentState.Live,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_LIVE'],
    ['Offline', new XPageStatusInfo(
      XPageStatus.Offline,
      DocumentState.New,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_OFFLINE'],
    ['UnpublishedChanges', new XPageStatusInfo(
      XPageStatus.UnpublishedChanges,
      DocumentState.Changed,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_LIVE_UNPUBLISHED_CHANGES'],
    ['PublicationRequest', new XPageStatusInfo(
      XPageStatus.PublicationRequest,
      DocumentState.Unpublished,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PUBLICATION_REQUESTED'],
    ['TakeOfflineRequest', new XPageStatusInfo(
      XPageStatus.TakeOfflineRequest,
      DocumentState.Live,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_TAKE_OFFLINE_REQUESTED'],
    ['ScheduledPublication', new XPageStatusInfo(
      XPageStatus.ScheduledPublication,
      DocumentState.Unpublished,
      'some page',
      1596811323,
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_PUBLICATION'],
    ['ScheduledToTakeOffline', new XPageStatusInfo(
      XPageStatus.ScheduledToTakeOffline,
      DocumentState.Live,
      'some page',
      1596811323,
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_TO_TAKE_OFFLINE'],
    ['ScheduledPublicationRequest', new XPageStatusInfo(
      XPageStatus.ScheduledPublicationRequest,
      DocumentState.Unpublished,
      'some page',
      1596811323,
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_PUBLICATION_REQUESTED'],
    ['ScheduledToTakeOfflineRequest', new XPageStatusInfo(
      XPageStatus.ScheduledToTakeOfflineRequest,
      DocumentState.Live,
      'some page',
      1596811323,
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_TO_TAKE_OFFLINE_REQUESTED'],
    ['ProjectInProgress', new XPageStatusInfo(
      XPageStatus.ProjectInProgress,
      DocumentState.Live,
      'some page',
      undefined,
      'project name',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IN_PROGRESS'],
    ['ProjectInReview', new XPageStatusInfo(
      XPageStatus.ProjectInReview,
      DocumentState.Live,
      'some page',
      undefined,
      'project name',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IN_REVIEW'],
    ['ProjectPageApproved', new XPageStatusInfo(
      XPageStatus.ProjectPageApproved,
      DocumentState.Live,
      'some page',
      undefined,
      'project name',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_PAGE_APPROVED'],
    ['ProjectRunning', new XPageStatusInfo(
      XPageStatus.ProjectRunning,
      DocumentState.Live,
      'some page',
      undefined,
      'project name',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IS_RUNNING'],
    ['PreviousVersion', new XPageStatusInfo(
      XPageStatus.PreviousVersion,
      DocumentState.Live,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PREVIOUS_VERSION'],
  ])('if xpage status is %s', (statusName, statusInfo, expectedText) => {
    beforeEach(() => {
      pageStatusInfo$.next(statusInfo);
    });

    /*test(`should not use danger color as background`, () => {
      expect(component.danger).toBeFalsy();
    });*/
  });
});
