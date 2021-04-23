/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
  Inject,
  Input,
  Output,
} from '@angular/core';

import { UserSettings } from '../../../models/dto/user-settings.dto';
import { AuthService } from '../../../services/auth.service';
import { USER_SETTINGS } from '../../../services/user-settings';

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
  userDrawerOpen: boolean;

  @Output()
  userDrawerOpenChange = new EventEmitter<boolean>();

  @HostBinding('@slideInOut')
  animate = true;

  constructor(
    private readonly authService: AuthService,
    @Inject(USER_SETTINGS) private readonly userSettings: UserSettings,
  ) {}

  get userName(): string {
    return this.userSettings.userName;
  }

  get email(): string {
    return this.userSettings.email || '';
  }

  logout(event: Event): void {
    event.preventDefault();

    this.closeDrawer();

    this.authService.activeLogout();
  }

  onClickedOutside(): void {
    this.closeDrawer();
  }

  private closeDrawer(): void {
    this.userDrawerOpenChange.emit(false);
  }
}
