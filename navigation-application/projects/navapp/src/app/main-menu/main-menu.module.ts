/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import { ClickOutsideModule } from 'ng-click-outside';

import { SharedModule } from '../shared';

import { ExpandableMenuItemComponent } from './components/expandable-menu-item/expandable-menu-item.component';
import { MainMenuComponent } from './components/main-menu.component';
import { MenuDrawerComponent } from './components/menu-drawer/menu-drawer.component';
import { MenuItemLinkComponent } from './components/menu-item-link/menu-item-link.component';
import { ToolbarDrawerComponent } from './components/toolbar-drawer/toolbar-drawer.component';
import { TopLevelMenuItemComponent } from './components/top-level-menu-item/top-level-menu-item.component';
import { MenuBuilderService, MenuStateService, MenuStructureService } from './services';

@NgModule({
  imports: [
    CommonModule,
    BrowserAnimationsModule,
    SharedModule,
    ClickOutsideModule,
  ],
  declarations: [
    MainMenuComponent,
    TopLevelMenuItemComponent,
    MenuDrawerComponent,
    ExpandableMenuItemComponent,
    MenuItemLinkComponent,
    ToolbarDrawerComponent,
  ],
  providers: [MenuStructureService, MenuBuilderService, MenuStateService],
  exports: [MainMenuComponent],
})
export class MainMenuModule {}
