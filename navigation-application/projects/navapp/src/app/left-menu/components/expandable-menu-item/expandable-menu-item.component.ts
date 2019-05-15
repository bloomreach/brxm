/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { animate, state, style, transition, trigger } from '@angular/animations';
import { Component, HostBinding, Input } from '@angular/core';

import { MenuItem, MenuItemContainer } from '../../models';
import { MenuStateService } from '../../services';

@Component({
  selector: 'brna-expandable-menu-item',
  templateUrl: 'expandable-menu-item.component.html',
  styleUrls: ['expandable-menu-item.component.scss'],
  animations: [
    trigger('slideInOut', [
      state('true', style({
        marginTop: '10px',
      })),
      transition(':enter', [
        style({ height: '0', marginTop: '0' }),
        animate('300ms ease', style({ height: '*', marginTop: '10px' })),
      ]),
      transition(':leave', [
        animate('300ms ease', style({ height: '0', marginTop: '0' })),
      ]),
    ]),
  ],
})
export class ExpandableMenuItemComponent {
  opened = false;

  @Input()
  config: MenuItemContainer;

  @Input()
  @HostBinding('class.active')
  active = false;

  constructor(
    private menuStateService: MenuStateService,
  ) {}

  toggle(): void {
    this.opened = !this.opened;
  }

  isActive(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemActive(item);
  }
}
