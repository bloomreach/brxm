import { Injectable, OnDestroy } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { MenuItem, MenuItemContainer, MenuItemLink } from '../models';

import { MenuBuilderService } from './menu-builder.service';

@Injectable()
export class MenuStateService implements OnDestroy {
  private breadcrumbs: MenuItem[] = [];
  private menu$: Observable<MenuItem[]>;
  private currentMenu: MenuItem[];
  private unsubscribe = new Subject();

  constructor(
    private menuBuilderService: MenuBuilderService,
  ) {
    this.menu$ = this.menuBuilderService.buildMenu();
    this.menu$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(menu => {
      this.currentMenu = menu;
      // TODO: rebuild the breadcumbs if menu has been updated afte initialization
    });
  }

  get menu(): MenuItem[] {
    return this.currentMenu;
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  setActiveItem(item: MenuItemLink): void {
    this.breadcrumbs = this.buildBreadcrumbs(this.currentMenu, item);
  }

  isMenuItemActive(item: MenuItem): boolean {
    return this.breadcrumbs.some(breadcrumbItem => breadcrumbItem === item);
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
