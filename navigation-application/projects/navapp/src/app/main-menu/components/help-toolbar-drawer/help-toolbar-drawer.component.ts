/*
 * Copyright 2019-2023 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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
import { Component, EventEmitter, HostBinding, Inject, Input, OnInit, Output } from '@angular/core';

import { AppSettings } from '../../../models/dto/app-settings.dto';
import { NavAppHelpLink } from '../../../models/dto/nav-app-help-link.dto';
import { APP_SETTINGS } from '../../../services/app-settings';

@Component({
  selector: 'brna-help-toolbar-drawer',
  templateUrl: './help-toolbar-drawer.component.html',
  styleUrls: ['./help-toolbar-drawer.component.scss'],
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
export class HelpToolbarDrawerComponent implements OnInit {
  @HostBinding('@slideInOut')
  animate = true;

  @Input()
  helpDrawerOpen: boolean;

  @Output()
  helpDrawerOpenChange = new EventEmitter<boolean>();

  links: NavAppHelpLink[];

  constructor(@Inject(APP_SETTINGS) private readonly appSettings: AppSettings) {}

  ngOnInit(): void {
    this.links = this.appSettings.helpLinks;
  }

  onClickedOutside(): void {
    this.helpDrawerOpenChange.emit(false);
  }

  onLinkClicked(): void {
    this.onClickedOutside();
  }
}
