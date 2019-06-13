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

import { animate, style, transition, trigger } from '@angular/animations';
import { Component, HostBinding, Input } from '@angular/core';

import { UserSettings } from '../../../models/dto';
import { CommunicationsService } from '../../../services';

@Component({
  selector: 'brna-user-toolbar-drawer',
  templateUrl: './user-toolbar-drawer.component.html',
  styleUrls: ['./user-toolbar-drawer.component.scss'],
  animations: [
    trigger('slideInOut', [
      transition(':enter', [
        style({ transform: 'translateX(-100%)' }),
        animate('300ms ease-in-out', style({ transform: 'translateX(0%)' })),
      ]),
      transition(':leave', [
        animate('300ms ease-in-out', style({ transform: 'translateX(-100%)' })),
      ]),
    ]),
  ],
})
export class UserToolbarDrawerComponent {
  @Input()
  config: UserSettings;

  @HostBinding('@slideInOut')
  animate = true;

  constructor(
    private communicationService: CommunicationsService,
  ) { }

  get userName(): string {
    return this.config.userName;
  }

  get email(): string {
    return this.config.email || '';
  }

  get loginUrl(): string {
    return window.location.href;
  }

  logout(): void {
    this.communicationService
      .logout()
      .subscribe(results => {
        results
          .filter(e => e instanceof Error)
          .forEach(e => console.error(e));
        window.location.reload();
      });
  }

}
