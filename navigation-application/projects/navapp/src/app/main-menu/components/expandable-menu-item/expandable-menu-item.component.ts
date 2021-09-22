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
import { Component, HostBinding, Input, OnChanges, QueryList, ViewChildren } from '@angular/core';

import { QaHelperService } from '../../../services/qa-helper.service';
import { MenuItemContainer } from '../../models/menu-item-container.model';
import { MenuItem } from '../../models/menu-item.model';
import { MenuStateService } from '../../services/menu-state.service';
import { ExpandableSubMenuItemComponent } from '../expandable-sub-menu-item/expandable-sub-menu-item.component';

@Component({
  selector: 'brna-expandable-menu-item',
  templateUrl: 'expandable-menu-item.component.html',
  styleUrls: ['expandable-menu-item.component.scss'],
  animations: [
    trigger('menuSlideInOut', [
      state('false', style({ height: '0' })),
      state('true', style({ height: '*' })),
      transition('false <=> true', animate('300ms ease')),
    ]),
  ],
})
export class ExpandableMenuItemComponent implements OnChanges {
  private isChildMenuOpened = false;

  @Input()
  config: MenuItemContainer;

  @Input()
  @HostBinding('class.highlighted')
  highlighted = false;

  @ViewChildren(ExpandableSubMenuItemComponent)
  expandableSubMenuItems: QueryList<ExpandableSubMenuItemComponent>;

  constructor(
    private readonly menuStateService: MenuStateService,
    private readonly qaHelperService: QaHelperService,
  ) {}

  get isOpened(): boolean {
    return this.isChildMenuOpened;
  }

  ngOnChanges(): void {
    if (this.highlighted) {
      this.isChildMenuOpened = true;
    }
  }

  onExpandableSubMenuItemClick(component: ExpandableSubMenuItemComponent): void {
    this.expandableSubMenuItems
      .filter(x => x !== component)
      .forEach(x => x.close());
  }

  toggle(): void {
    this.isChildMenuOpened = !this.isChildMenuOpened;
  }

  close(): void {
    this.isChildMenuOpened = false;

    this.expandableSubMenuItems.forEach(x => x.close());
  }

  isContainer(item: MenuItem): boolean {
    return item instanceof MenuItemContainer;
  }

  isHighlighted(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemHighlighted(item);
  }

  isFailed(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemFailed(item);
  }

  getQaClass(item: MenuItem): string {
    return this.qaHelperService.getMenuItemClass(item);
  }
}
