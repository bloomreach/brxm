/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MaterialModule } from '../shared/material/material.module';

import { ExpandableMenuItemComponent } from './components/expandable-menu-item/expandable-menu-item.component';
import { MainMenuComponent } from './components/main-menu.component';
import { MenuDrawerComponent } from './components/menu-drawer/menu-drawer.component';
import { MenuItemLinkComponent } from './components/menu-item-link/menu-item-link.component';
import { TopLevelMenuItemComponent } from './components/top-level-menu-item/top-level-menu-item.component';
import { MenuBuilderService, MenuStateService, MenuStructureService } from './services';

@NgModule({
  imports: [CommonModule, BrowserAnimationsModule, MaterialModule],
  declarations: [
    MainMenuComponent,
    TopLevelMenuItemComponent,
    MenuDrawerComponent,
    ExpandableMenuItemComponent,
    MenuItemLinkComponent,
  ],
  providers: [MenuStructureService, MenuBuilderService, MenuStateService],
  exports: [MainMenuComponent],
})
export class MainMenuModule {}
