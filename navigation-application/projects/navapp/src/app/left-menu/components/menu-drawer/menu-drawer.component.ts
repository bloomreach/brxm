import { animate, style, transition, trigger } from '@angular/animations';
import { Component, HostBinding, Input } from '@angular/core';

import { MenuItem, MenuItemContainer } from '../../models';

@Component({
  selector: 'brna-menu-drawer',
  templateUrl: 'menu-drawer.component.html',
  styleUrls: ['menu-drawer.component.scss'],
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
export class MenuDrawerComponent {
  @HostBinding('@slideInOut')
  animate = true;

  @Input()
  config: MenuItemContainer;

  isContainer(item: MenuItem): boolean {
    return item instanceof MenuItemContainer;
  }
}
