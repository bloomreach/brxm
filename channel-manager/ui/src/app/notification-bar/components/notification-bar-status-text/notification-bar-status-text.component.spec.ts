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

import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateService } from '@ngx-translate/core';

import { DocumentState } from '../../../models/document-state.enum';
import { XPageStatusInfo } from '../../../models/page-status-info.model';
import { XPageStatus } from '../../../models/xpage-status.enum';
import { MomentPipe } from '../../../shared/pipes/moment.pipe';

import { NotificationBarStatusTextComponent } from './notification-bar-status-text.component';

@Pipe({
  name: 'translate',
})
class TranslateMockPipe implements PipeTransform {
  transform(value: string, params: { pageName: string, status: string | undefined, dateTime: string | null } | undefined): string {
    return `${value}${params?.pageName}${params?.status}${params?.dateTime}`;
  }
}

describe('NotificationBarStatusTextComponent', () => {
  let component: NotificationBarStatusTextComponent;
  let fixture: ComponentFixture<NotificationBarStatusTextComponent>;

  beforeEach(() => {
    const momentPipe = {
      transform: jest.fn(v => v),
    };

    const translateServiceMock = {
      instant: jest.fn(v => v),
    };

    fixture = TestBed.configureTestingModule({
      declarations: [
        NotificationBarStatusTextComponent,
        TranslateMockPipe,
      ],
      providers: [
        { provide: MomentPipe, useValue: momentPipe },
        { provide: TranslateService, useValue: translateServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(NotificationBarStatusTextComponent);

    component = fixture.componentInstance;
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
    ['RejectedRequest', new XPageStatusInfo(
      XPageStatus.RejectedRequest,
      DocumentState.Unpublished,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_REQUEST_REJECTED'],
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
    ['NotPartOfProject', new XPageStatusInfo(
      XPageStatus.NotPartOfProject,
      DocumentState.Live,
      'some page',
      undefined,
      'project name',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_NOT_PART_OF_PROJECT'],
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
    ['ProjectPageRejected', new XPageStatusInfo(
      XPageStatus.ProjectPageRejected,
      DocumentState.Live,
      'some page',
      undefined,
      'project name',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_PAGE_REJECTED'],
    ['ProjectRunning', new XPageStatusInfo(
      XPageStatus.ProjectRunning,
      DocumentState.Live,
      'some page',
      undefined,
      'project name',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PROJECT_IS_RUNNING'],
    ['EditingSharedContainers', new XPageStatusInfo(
      XPageStatus.EditingSharedContainers,
      DocumentState.Live,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_EDITING_SHARED_CONTAINERS'],
    ['PreviousVersion', new XPageStatusInfo(
      XPageStatus.PreviousVersion,
      DocumentState.Live,
      'some page',
    ), 'NOTIFICATION_BAR_XPAGE_LABEL_PREVIOUS_VERSION'],
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
    beforeEach(() => {
      component.statusInfo = statusInfo;

      fixture.detectChanges();
    });

    test(`should provide ${expectedText} label`, () => {
      expect(component.text).toBe(expectedText);
    });

    test(`should render ${expectedText} label`, () => {
      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });
});
