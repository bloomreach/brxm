/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, HostBinding, OnInit } from '@angular/core';
import { Observable } from 'rxjs';
import { first } from 'rxjs/operators';

import { QaHelperService } from '../../services';
import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';
import { MenuStateService } from '../services';

@Component({
  selector: 'brna-main-menu',
  templateUrl: 'main-menu.component.html',
  styleUrls: ['main-menu.component.scss'],
})
export class MainMenuComponent implements OnInit {

  get collapsed(): boolean {
    return this.menuStateService.isMenuCollapsed;
  }

  get menu$(): Observable<MenuItem[]> {
    return this.menuStateService.menu$;
  }

  get isDrawerOpen(): boolean {
    return this.menuStateService.isDrawerOpened;
  }

  get drawerMenuItem(): MenuItemContainer {
    return this.menuStateService.drawerMenuItem;
  }

  @HostBinding('class.collapsed')
  get isCollapsed(): boolean {
    return this.collapsed;
  }
  constructor(
    private menuStateService: MenuStateService,
    private qaHelperService: QaHelperService,
  ) { }

  ngOnInit(): void {
    this.menu$.pipe(first()).subscribe(items => this.selectMenuItem(items[0]));
  }

  toggle(): void {
    this.menuStateService.toggle();
  }

  isRippleDisabled(item: MenuItemContainer): boolean {
    return item instanceof MenuItemContainer;
  }

  onMenuItemClick(event: MouseEvent, item: MenuItem): void {
    event.stopImmediatePropagation();
    this.selectMenuItem(item);
  }

  selectMenuItem(item: MenuItem): void {

    if (item instanceof MenuItemLink) {
      this.menuStateService.setActiveItem(item);
      return;
    }

    if (item instanceof MenuItemContainer) {
      this.menuStateService.openDrawer(item);
    }
  }

  isMenuItemActive(item: MenuItem): boolean {
    return this.menuStateService.isMenuItemActive(item);
  }

  getQaClass(item: MenuItem | string): string {
    return this.qaHelperService.getMenuItemClass(item);
  }
}
