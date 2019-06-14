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

import { CdkTreeModule } from '@angular/cdk/tree';
import { DebugElement, SimpleChange } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { PerfectScrollbarModule } from 'ngx-perfect-scrollbar';

import { Site } from '../../../models/dto';

import { SiteSelectionSidePanelComponent } from './site-selection-side-panel.component';

describe('SiteSelectionSidePanelComponent', () => {
  let component: SiteSelectionSidePanelComponent;
  let fixture: ComponentFixture<SiteSelectionSidePanelComponent>;
  let de: DebugElement;

  const mockSites: Site[] = [
    {
      id: 1,
      name: 'www.company.com',
      subGroups: [
        {
          id: 2,
          name: 'UK & Germany',
          subGroups: [
            {
              id: 3,
              name: 'Office UK',
            },
            {
              id: 4,
              name: 'Office DE',
            },
          ],
        },
      ],
    },
    {
      id: 5,
      name: 'An example company that has a very long name and a subgroup with many items',
      subGroups: [
        {
          id: 6,
          name: 'Sub company 001',
        },
        {
          id: 7,
          name: 'Sub company 002',
        },
      ],
    },
  ];

  beforeEach(() => {
    fixture = TestBed.configureTestingModule({
      imports: [
        PerfectScrollbarModule,
        FormsModule,
        CdkTreeModule,
        MatIconModule,
        NoopAnimationsModule,
      ],
      declarations: [
        SiteSelectionSidePanelComponent,
      ],
    }).createComponent(SiteSelectionSidePanelComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;
  });

  beforeEach(() => {
    component.sites = mockSites;
    component.ngOnChanges({ sites: new SimpleChange(undefined, mockSites, true) });
    fixture.detectChanges();
  });

  it('should init data source', fakeAsync(() => {
    const expected = [
      { id: 1, expandable: true, name: 'www.company.com', level: 0 },
      { id: 2, expandable: true, name: 'UK & Germany', level: 1 },
      { id: 3, expandable: false, name: 'Office UK', level: 2 },
      { id: 4, expandable: false, name: 'Office DE', level: 2 },
      { id: 5, expandable: true, name: 'An example company that has a very long name and a subgroup with many items', level: 0 },
      { id: 6, expandable: false, name: 'Sub company 001', level: 1 },
      { id: 7, expandable: false, name: 'Sub company 002', level: 1 },
    ];
    let actual: any;

    component.dataSource.connect().subscribe(data => actual = data);

    tick();

    expect(actual).toEqual(actual);
  }));

  it('should show filter out sites', fakeAsync(() => {
    const expected = [
      { id: 5, expandable: true, name: 'An example company that has a very long name and a subgroup with many items', level: 0 },
      { id: 6, expandable: false, name: 'Sub company 001', level: 1 },
      { id: 7, expandable: false, name: 'Sub company 002', level: 1 },
    ];
    let actual: any;

    component.searchText = 'Sub company';
    component.ngOnChanges({ searchText: new SimpleChange(undefined, component.searchText, true) });
    fixture.detectChanges();

    component.dataSource.connect().subscribe(data => actual = data);

    tick();

    expect(actual).toEqual(expected);
  }));

  describe('the active node', () => {
    beforeEach(() => {
      component.selectedSite = { id: 4, name: 'Office DE' };
      component.ngOnChanges({ selectedSite: new SimpleChange(undefined, component.selectedSite, true) });
      fixture.detectChanges();
    });

    it('should be marked by "isExpanded: true" in the data', fakeAsync(() => {
      const expected = [
        { id: 1, expandable: true, name: 'www.company.com', level: 0, isExpanded: true },
        { id: 2, expandable: true, name: 'UK & Germany', level: 1, isExpanded: true },
        { id: 3, expandable: false, name: 'Office UK', level: 2 },
        { id: 4, expandable: false, name: 'Office DE', level: 2, isExpanded: true },
        { id: 5, expandable: true, name: 'An example company that has a very long name and a subgroup with many items', level: 0 },
        { id: 6, expandable: false, name: 'Sub company 001', level: 1 },
        { id: 7, expandable: false, name: 'Sub company 002', level: 1 },
      ];
      let actual: any;

      component.dataSource.connect().subscribe(data => actual = data);

      tick();

      expect(actual).toEqual(expected);
    }));

    it('should be marked with ".active" class in the DOM', () => {
      const expected = 'Office DE';
      let actual: string;

      const activeLeafNode = de.query(By.css('.active'));

      expect(activeLeafNode).toBeDefined();

      actual = activeLeafNode.nativeElement.textContent.trim();

      expect(actual).toBe(expected);
    });
  });
});
