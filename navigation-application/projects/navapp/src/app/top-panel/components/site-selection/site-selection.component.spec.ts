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

import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatTreeModule } from '@angular/material/tree';
import { Site } from '@bloomreach/navapp-communication';
import { PerfectScrollbarModule } from 'ngx-perfect-scrollbar';
import { Subject } from 'rxjs';

import { SiteService } from '../../../services/site.service';
import { RightSidePanelService } from '../../services/right-side-panel.service';

import { SiteSelectionComponent } from './site-selection.component';

describe('SiteSelectionComponent', () => {
  let component: SiteSelectionComponent;
  let fixture: ComponentFixture<SiteSelectionComponent>;

  const selectSiteSubject = new Subject<Site>();
  const mockSites: Site[] = [
    {
      siteId: -1,
      accountId: 1,
      name: 'www.company.com',
      isNavappEnabled: true,
      subGroups: [
        {
          siteId: 1,
          accountId: 1,
          isNavappEnabled: true,
          name: 'UK & Germany',
          subGroups: [
            {
              siteId: 2,
              accountId: 1,
              isNavappEnabled: true,
              name: 'Office UK',
            },
            {
              siteId: 3,
              accountId: 1,
              isNavappEnabled: true,
              name: 'Office DE',
            },
          ],
        },
      ],
    },
    {
      siteId: -1,
      accountId: 2,
      isNavappEnabled: true,
      name:
        'An example company that has a very long name and a subgroup with many items',
      subGroups: [
        {
          siteId: 12,
          accountId: 2,
          isNavappEnabled: true,
          name: 'Sub company 001',
        },
        {
          siteId: 13,
          accountId: 2,
          isNavappEnabled: true,
          name: 'Sub company 002',
        },
      ],
    },
  ];

  const siteServiceMock = jasmine.createSpyObj('SiteService', {
    updateSelectedSite: Promise.resolve(),
    setSelectedSite: undefined,
  });
  siteServiceMock.selectedSite$ = selectSiteSubject;
  siteServiceMock.sites = mockSites;

  const rightSidePanelServiceMock = jasmine.createSpyObj('RightSidePanelService', [
    'close',
  ]);

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [
        FormsModule,
        MatTreeModule,
        MatIconModule,
        PerfectScrollbarModule,
      ],
      declarations: [SiteSelectionComponent],
      providers: [
        { provide: SiteService, useValue: siteServiceMock },
        { provide: RightSidePanelService, useValue: rightSidePanelServiceMock },
      ],
    }).createComponent(SiteSelectionComponent);

    component = fixture.componentInstance;
  });

  it('should be crated', () => {
    expect(component).toBeDefined();
  });

  it('should call updateSelectedSite when site is selected in the tree view', () => {
    const expected = { accountId: 1, siteId: -1, isNavappEnabled: true, name: 'www.company.com' };

    const node = {
      accountId: 1,
      siteId: -1,
      isNavappEnabled: true,
      expandable: true,
      name: 'www.company.com',
      level: 0,
    };

    component.onNodeClicked(node);

    expect(siteServiceMock.updateSelectedSite).toHaveBeenCalledWith(expected);
  });
});
