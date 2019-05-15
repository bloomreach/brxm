import { animate, style, transition, trigger } from '@angular/animations';
import { Component, ElementRef, HostBinding, HostListener, Input } from '@angular/core';

import { MenuItem, MenuItemContainer } from '../../models';
import { MenuStateService } from '../../services';

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

  constructor(
    private menuStateService: MenuStateService,
    private elRef: ElementRef,
  ) {}

  isContainer(item: MenuItem): boolean {
    return item instanceof MenuItemContainer;
  }

  isActive(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemActive(item);
  }

  @HostListener('document:click', ['$event'])
  private onDocumentClick(event): void {
    if (this.elRef.nativeElement.contains(event.target)) {
      return;
    }

    this.menuStateService.closeDrawer();
  }
}
