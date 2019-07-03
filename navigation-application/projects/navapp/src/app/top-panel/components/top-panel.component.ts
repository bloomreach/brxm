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

import { Component, OnDestroy, OnInit } from '@angular/core';
import { Observable, Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { ClientAppService } from '../../client-app/services';
import { Site } from '../../models/dto';
import { CommunicationsService, NavConfigService } from '../../services';
import { SiteSelectionSidePanelService } from '../services';

@Component({
  selector: 'brna-top-panel',
  templateUrl: 'top-panel.component.html',
  styleUrls: ['top-panel.component.scss'],
})
export class TopPanelComponent implements OnInit, OnDestroy {
  private site: Site;
  private unsubscribe = new Subject();

  constructor(
    private navConfigService: NavConfigService,
    private siteSelectionPanelService: SiteSelectionSidePanelService,
    private clientAppService: ClientAppService,
    private communicationsService: CommunicationsService,
  ) {}

  get selectedSite(): Site {
    return this.site;
  }

  set selectedSite(site: Site) {
    this.communicationsService.updateSite(site.id).then(() => {
      this.site = site;
      this.siteSelectionPanelService.close();
    });
  }

  get sites$(): Observable<Site[]> {
    return this.navConfigService.sites$;
  }

  get isSidePanelOpened(): boolean {
    return this.siteSelectionPanelService.isOpened;
  }

  get isSiteSelectionEnabled(): boolean {
    return this.clientAppService.doesActiveAppSupportSites;
  }

  ngOnInit(): void {
    this.navConfigService.selectedSite$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(selectedSite => this.site = selectedSite);
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }

  onSiteSelectorClicked(): void {
    this.siteSelectionPanelService.toggle();
  }

  onBackdropClicked(): void {
    this.siteSelectionPanelService.close();
  }
}
