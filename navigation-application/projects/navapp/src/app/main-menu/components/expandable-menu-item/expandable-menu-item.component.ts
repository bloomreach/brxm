/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { animate, style, transition, trigger } from '@angular/animations';
import { Component, HostBinding, Input, OnChanges, SimpleChanges } from '@angular/core';

import { MenuItem, MenuItemContainer, MenuItemLink } from '../../models';
import { MenuStateService } from '../../services';
import { QaHelperService } from '../../../services';

@Component({
  selector: 'brna-expandable-menu-item',
  templateUrl: 'expandable-menu-item.component.html',
  styleUrls: ['expandable-menu-item.component.scss'],
  animations: [
    trigger('slideInOut', [
      transition(':enter', [
        style({ height: '0' }),
        animate('300ms ease', style({ height: '*' })),
      ]),
      transition(':leave', [
        animate('300ms ease', style({ height: '0' })),
      ]),
    ]),
  ],
})
export class ExpandableMenuItemComponent implements OnChanges {
  private isChildMenuOpened = false;

  @Input()
  config: MenuItemContainer;

  @Input()
  @HostBinding('class.active')
  active = false;

  constructor(
    private menuStateService: MenuStateService,
    private qaHelperService: QaHelperService,
  ) {}

  get isOpened(): boolean {
    return this.isChildMenuOpened;
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (this.active) {
      this.isChildMenuOpened = true;
    }
  }

  toggle(): void {
    this.isChildMenuOpened = !this.isChildMenuOpened;
  }

  isChildMenuItemActive(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemActive(item);
  }
  getQaClass(item: MenuItemLink): string {
    return this.qaHelperService.getMenuItemClass(item);
  }
}
