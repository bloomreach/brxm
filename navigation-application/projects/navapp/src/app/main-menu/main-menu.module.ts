/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ExpandableMenuItemComponent } from './components/expandable-menu-item/expandable-menu-item.component';
import { MainMenuComponent } from './components/main-menu.component';
import { MenuItemLinkComponent } from './components/menu-item-link/menu-item-link.component';
import { MenuItemComponent } from './components/menu-item/menu-item.component';
import { TopLevelMenuItemComponent } from './components/top-level-menu-item/top-level-menu-item.component';
import { MenuBuilderService, MenuStructureService } from './services';

@NgModule({
  imports: [CommonModule],
  declarations: [
    MainMenuComponent,
    MenuItemComponent,
    TopLevelMenuItemComponent,
    ExpandableMenuItemComponent,
    MenuItemLinkComponent,
  ],
  providers: [MenuStructureService, MenuBuilderService],
  exports: [MainMenuComponent],
})
export class MainMenuModule {}
