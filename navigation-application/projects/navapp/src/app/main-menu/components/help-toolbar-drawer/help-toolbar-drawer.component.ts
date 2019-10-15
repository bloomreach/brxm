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
export class HelpToolbarDrawerComponent {
  @HostBinding('@slideInOut')
  animate = true;

  @Input()
  helpDrawerOpen: boolean;

  @Output()
  helpDrawerOpenChange = new EventEmitter<boolean>();

  onClickedOutside(): void {
    this.helpDrawerOpenChange.emit(false);
  }

  onLinkClicked(): void {
    this.onClickedOutside();
  }
}
