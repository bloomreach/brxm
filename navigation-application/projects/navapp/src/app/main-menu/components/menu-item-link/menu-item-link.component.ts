/*
 * Copyright 2019-2021 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Component, HostBinding, Input, OnInit } from '@angular/core';
import { NavigationTrigger } from '@bloomreach/navapp-communication';

import { NavItem } from '../../../models/nav-item.model';
import { NavigationService } from '../../../services/navigation.service';
import { UrlMapperService } from '../../../services/url-mapper.service';

@Component({
  selector: 'brna-menu-item-link',
  templateUrl: 'menu-item-link.component.html',
  styleUrls: ['menu-item-link.component.scss'],
})
export class MenuItemLinkComponent implements OnInit {
  @Input()
  caption: string;

  @Input()
  navItem: NavItem;

  @Input()
  @HostBinding('class.highlighted')
  @HostBinding('class.qa-highlighted')
  highlighted = false;

  navItemUrl: string;

  constructor(
    private readonly navigationService: NavigationService,
    private readonly urlMapperService: UrlMapperService,
  ) { }

  ngOnInit(): void {
    this.navItemUrl = this.urlMapperService.mapNavItemToBrowserUrl(this.navItem);
  }

  @HostBinding('class.disabled')
  @HostBinding('class.qa-disabled')
  get disabled(): boolean {
    return !this.navItem || !this.navItem.active;
  }

  onClick(e: MouseEvent): void {
    e.preventDefault();

    if (this.disabled || !this.navItem) {
      return;
    }

    this.navigationService.navigateByUrl(this.navItemUrl, NavigationTrigger.Menu);
  }
}
