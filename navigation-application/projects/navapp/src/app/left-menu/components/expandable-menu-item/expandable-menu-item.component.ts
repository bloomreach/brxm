/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { animate, state, style, transition, trigger } from '@angular/animations';
import { Component, Input } from '@angular/core';

import { MenuItemContainer } from '../../models';

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

  toggle(): void {
    this.opened = !this.opened;
  }
}
