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

import { DatePipe } from '@angular/common';
import { NO_ERRORS_SCHEMA, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { mocked } from 'ts-jest/utils';

import { DocumentState } from '../../../models/document-state.enum';
import { XPageState } from '../../../models/xpage-state.model';
import { XPageStatus } from '../../../models/xpage-status.enum';
import { PageService } from '../../../pages/services/page.service';

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

  let pageServiceMock: PageService;

  beforeEach(() => {
    pageServiceMock = {
      getXPageStatus: jest.fn(),
    } as unknown as typeof pageServiceMock;

    const datePipe = {
      transform: jest.fn(v => v),
    };

    fixture = TestBed.configureTestingModule({
      declarations: [
        NotificationBarStatusTextComponent,
        TranslateMockPipe,
      ],
      providers: [
        { provide: PageService, useValue: pageServiceMock },
        { provide: DatePipe, useValue: datePipe },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(NotificationBarStatusTextComponent);

    component = fixture.componentInstance;
  });

  it('should compute XPage status', () => {
    const mockState = {} as XPageState;

    component.state = mockState;

    expect(pageServiceMock.getXPageStatus).toHaveBeenCalledWith(mockState);
  });

  describe.each([
    ['Published', XPageStatus.Published, 'NOTIFICATION_BAR_XPAGE_LABEL_LIVE'],
    ['Offline', XPageStatus.Offline, 'NOTIFICATION_BAR_XPAGE_LABEL_OFFLINE'],
    ['UnpublishedChanges', XPageStatus.UnpublishedChanges, 'NOTIFICATION_BAR_XPAGE_LABEL_LIVE_UNPUBLISHED_CHANGES'],
    ['PublicationRequest', XPageStatus.PublicationRequest, 'NOTIFICATION_BAR_XPAGE_LABEL_PUBLICATION_REQUESTED'],
    ['RejectedRequest', XPageStatus.RejectedRequest, 'NOTIFICATION_BAR_XPAGE_LABEL_REQUEST_REJECTED'],
    ['TakeOfflineRequest', XPageStatus.TakeOfflineRequest, 'NOTIFICATION_BAR_XPAGE_LABEL_TAKE_OFFLINE_REQUESTED'],
    ['ScheduledPublication', XPageStatus.ScheduledPublication, 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_PUBLICATION'],
    ['ScheduledToTakeOffline', XPageStatus.ScheduledToTakeOffline, 'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_TO_TAKE_OFFLINE'],
    [
      'ScheduledPublicationRequest',
      XPageStatus.ScheduledPublicationRequest,
      'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_PUBLICATION_REQUESTED',
      DocumentState.New,
    ],
    [
      'ScheduledToTakeOfflineRequest',
      XPageStatus.ScheduledToTakeOfflineRequest,
      'NOTIFICATION_BAR_XPAGE_LABEL_SCHEDULED_TO_TAKE_OFFLINE_REQUESTED',
      DocumentState.Unpublished,
    ],
  ])('if xpage status is %s', (statusName, statusValue, expectedText, documentState?) => {
    beforeEach(() => {
      mocked(pageServiceMock.getXPageStatus).mockReturnValue(statusValue);

      component.state = {
        name: 'page name',
        state: documentState || DocumentState.Live,
        scheduledRequest: {
          scheduledDate: 1596811323,
        },
      } as XPageState;

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
