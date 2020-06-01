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

import { Component } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';

import { ClientAppService } from '../../client-app/services/client-app.service';
import { SiteService } from '../../services/site.service';
import { RightSidePanelService } from '../services/right-side-panel.service';

@Component({
  selector: 'brna-top-panel',
  templateUrl: 'top-panel.component.html',
  styleUrls: ['top-panel.component.scss'],
})
export class TopPanelComponent {

  constructor(
    private readonly siteService: SiteService,
    private readonly clientAppService: ClientAppService,
    private readonly rightSidePanelService: RightSidePanelService,
  ) { }

  get selectedSiteName$(): Observable<string> {
    return this.siteService.selectedSite$.pipe(
      map(x => x ? x.name : ''),
    );
  }

  get isSiteSelectionEnabled(): boolean {
    return this.clientAppService.doesActiveAppSupportSites;
  }

  onSiteSelectorClicked(): void {
    this.rightSidePanelService.open();
  }
}
