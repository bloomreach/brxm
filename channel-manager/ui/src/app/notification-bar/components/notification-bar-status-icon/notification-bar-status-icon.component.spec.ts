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

import { Component, Input } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { XPageStatus } from '../../../models/xpage-status.enum';

import { NotificationBarStatusIconComponent } from './notification-bar-status-icon.component';

@Component({
  // tslint:disable-next-line:component-selector
  selector: 'mat-icon',
  template: '{{ svgIcon }}',
})
class MatIconMockComponent {
  @Input()
  svgIcon!: string;
}

describe('NotificationBarStatusIconComponent', () => {
  let component: NotificationBarStatusIconComponent;
  let fixture: ComponentFixture<NotificationBarStatusIconComponent>;

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      declarations: [
        NotificationBarStatusIconComponent,
        MatIconMockComponent,
      ],
    }).createComponent(NotificationBarStatusIconComponent);

    component = fixture.componentInstance;
  });

  describe.each([
    ['Published', XPageStatus.Published, 'xpage'],
    ['Offline', XPageStatus.Offline, 'minus-circle-outline'],
    ['UnpublishedChanges', XPageStatus.UnpublishedChanges, 'alert-outline'],
    ['PublicationRequest', XPageStatus.PublicationRequest, 'comment-processing-outline'],
    ['RejectedRequest', XPageStatus.RejectedRequest, 'comment-remove-outline'],
    ['TakeOfflineRequest', XPageStatus.TakeOfflineRequest, 'comment-processing-outline'],
    ['ScheduledPublication', XPageStatus.ScheduledPublication, 'calendar-clock'],
    ['ScheduledToTakeOffline', XPageStatus.ScheduledToTakeOffline, 'calendar-clock'],
    ['ScheduledPublicationRequest', XPageStatus.ScheduledPublicationRequest, 'comment-processing-outline'],
    ['ScheduledToTakeOfflineRequest', XPageStatus.ScheduledToTakeOfflineRequest, 'comment-processing-outline'],
    ['NotPartOfProject', XPageStatus.NotPartOfProject, 'xpage'],
    ['ProjectInProgress', XPageStatus.ProjectInProgress, 'minus-circle-outline'],
    ['ProjectInReview', XPageStatus.ProjectInReview, 'comment-processing-outline'],
    ['ProjectPageApproved', XPageStatus.ProjectPageApproved, 'comment-check-outline'],
    ['ProjectPageRejected', XPageStatus.ProjectPageRejected, 'comment-remove-outline'],
    ['ProjectRunning', XPageStatus.ProjectRunning, 'xpage'],
    ['EditingSharedContainers', XPageStatus.EditingSharedContainers, 'alert-circle-outline'],
    ['PreviousVersion', XPageStatus.PreviousVersion, 'alert-circle-outline'],
    ['Locked', XPageStatus.Locked, 'lock-outline'],
  ])('if xpage status is %s', (statusName, statusValue, expectedIcon) => {
    beforeEach(() => {
      component.status = statusValue;

      fixture.detectChanges();
    });

    test(`should have ${expectedIcon} icon`, () => {
      expect(component.icon).toBe(expectedIcon);
    });

    test(`should render ${expectedIcon}`, () => {
      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });
});
