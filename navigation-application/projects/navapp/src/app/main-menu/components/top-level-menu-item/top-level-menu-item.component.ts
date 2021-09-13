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

import {
  animate,
  state,
  style,
  transition,
  trigger,
} from '@angular/animations';
import { Component, EventEmitter, HostBinding, HostListener, Input, Output } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: 'brna-top-level-menu-item',
  templateUrl: 'top-level-menu-item.component.html',
  styleUrls: ['top-level-menu-item.component.scss'],
  animations: [
    trigger('collapseToggle', [
      state('true', style({ width: '56px'})),
      state('false', style({ width: '*' })),
      transition('true <=> false', animate('300ms ease')),
    ]),
  ],
})
export class TopLevelMenuItemComponent {
  @Input()
  icon: string;

  @Input()
  caption = '';

  @Input()
  @HostBinding('@collapseToggle')
  collapsed = true;

  @Input()
  highlighted = false;

  @Input()
  pressed = false;

  @Input()
  failed = false;

  @Input()
  @HostBinding('class.small')
  small = false;

  @Output()
  menuItemClick = new EventEmitter<void>();

  constructor(
    private readonly translateService: TranslateService,
  ) {}

  @HostListener('click', ['$event'])
  onHostClickHandler(event: MouseEvent): void {
    event.stopImmediatePropagation();
    this.menuItemClick.emit();
  }

  getTooltip(): string {
    return this.failed ? this.translateService.instant('MENU_ITEM_TOOLTIP_FAILED_ERROR') : '';
  }
}
