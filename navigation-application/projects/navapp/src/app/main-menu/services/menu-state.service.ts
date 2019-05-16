import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';

import { MenuBuilderService } from './menu-builder.service';

@Injectable()
export class MenuStateService implements OnDestroy {
  // tslint:disable-next-line:variable-name
  private readonly _menu$: Observable<MenuItem[]>;
  private currentMenu: MenuItem[];
  private breadcrumbs: MenuItem[] = [];
  private collapsed = true;
  // tslint:disable-next-line:variable-name
  private _drawerMenuItem: MenuItemContainer;
  private unsubscribe = new Subject();

  constructor(
    private menuBuilderService: MenuBuilderService,
  ) {
    this._menu$ = this.menuBuilderService.buildMenu();
    this._menu$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(menu => {
      if (this.currentMenu && this.currentMenu.length) {
        throw new Error(
          'Menu has changed. Rebuild breadcrumbs functionality must be implemented to prevent menu incorrect behavior issues.'
        );
      }

      this.currentMenu = menu;
    });
  }

  get menu$(): Observable<MenuItem[]> {
    return this._menu$;
  }

  get isMenuCollapsed(): boolean {
    return this.collapsed;
  }

  get isDrawerOpened(): boolean {
    return !!this._drawerMenuItem;
  }

  get drawerMenuItem(): MenuItemContainer {
    return this._drawerMenuItem;
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  setActiveItem(item: MenuItemLink): void {
    this.closeDrawer();
    this.breadcrumbs = this.buildBreadcrumbs(this.currentMenu, item);
  }

  isMenuItemActive(item: MenuItem): boolean {
    return this.breadcrumbs.some(breadcrumbItem => breadcrumbItem === item);
  }

  toggle(): void {
    this.collapsed = !this.collapsed;
  }

  openDrawer(item: MenuItemContainer): void {
    this._drawerMenuItem = item;
  }

  closeDrawer(): void {
    this._drawerMenuItem = undefined;
  }

  private buildBreadcrumbs(menu: MenuItem[], activeItem: MenuItemLink): MenuItem[] {
    return menu.reduce((breadcrumbs, item) => {
      if (item instanceof MenuItemContainer) {
        const subBreadcrumbs = this.buildBreadcrumbs(item.children, activeItem);

        if (subBreadcrumbs.length > 0) {
          subBreadcrumbs.unshift(item);
          breadcrumbs = breadcrumbs.concat(subBreadcrumbs);
        }
      }

      if (item === activeItem) {
        breadcrumbs.push(item);
      }

      return breadcrumbs;
    }, []);
  }
}
