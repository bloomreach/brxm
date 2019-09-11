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
import {
  Component,
  EventEmitter,
  HostBinding,
  Input,
  Output,
} from '@angular/core';

import { UserSettings } from '../../../models/dto/user-settings.dto';
import { CommunicationsService } from '../../../services/communications.service';

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

  @Input()
  userDrawerOpen: boolean;

  @Output()
  userDrawerOpenChange = new EventEmitter<boolean>();

  @HostBinding('@slideInOut')
  animate = true;

  constructor(private communicationService: CommunicationsService) {}

  get userName(): string {
    return this.config.userName;
  }

  get email(): string {
    return this.config.email || '';
  }

  logout(event: Event): void {
    event.preventDefault();
    this.communicationService.logout().then(() => window.location.reload());
  }

  onClickedOutside(): void {
    this.userDrawerOpenChange.emit(false);
  }
}
