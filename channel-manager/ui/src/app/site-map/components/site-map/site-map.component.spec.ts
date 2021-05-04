/*!
 * Copyright 2020 Bloomreach. All rights reserved. (https://www.bloomreach.com/)
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

import { CUSTOM_ELEMENTS_SCHEMA, SimpleChange } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatIconTestingModule } from '@angular/material/icon/testing';
import { MatTreeModule } from '@angular/material/tree';
import { TranslateModule } from '@ngx-translate/core';

import { IframeService } from '../../../channels/services/iframe.service';
import { SiteMapItemMock } from '../../models/site-map-item.model.mock';

import { SiteMapComponent } from './site-map.component';

describe('SiteMapComponent', () => {
  let component: SiteMapComponent;
  let componentEl: HTMLElement;
  let fixture: ComponentFixture<SiteMapComponent>;
  let iframeService: IframeService;

  const scrollIntoViewMock = jest.fn();
  window.HTMLElement.prototype.scrollIntoView = scrollIntoViewMock;

  const mockRenderPathInfo = '/idTest2/idTest3';

  const mockSiteMapTree = [
    new SiteMapItemMock({
      children: [
        new SiteMapItemMock({
          id : 'idTest2',
          name : 'nameTest2',
          pageTitle : null,
          pathInfo : 'idTest2',
          renderPathInfo : '/idTest2',
          children: [
            new SiteMapItemMock({
              id : 'idTest3',
              name : 'nameTest3',
              pageTitle : 'pageTitleTest3',
              pathInfo : 'idTest2/idTest3',
              renderPathInfo : mockRenderPathInfo,
            }),
          ],
        }),
        new SiteMapItemMock({
          id : 'idTest4',
          name : 'nameTest4',
          pageTitle : 'pageTitleTest',
          pathInfo : 'idTest4',
          renderPathInfo : '/idTest4',
        }),
      ],
    }),
  ];

  beforeEach(waitForAsync(() => {
    const iframeServiceMock = {
      load: jest.fn(() => Promise.resolve()),
    };

    TestBed.configureTestingModule({
      declarations: [
        SiteMapComponent,
      ],
      imports: [
        FormsModule,
        MatTreeModule,
        MatIconModule,
        MatIconTestingModule,
        TranslateModule.forRoot(),
      ],
      providers: [
        { provide: IframeService, useValue: iframeServiceMock },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();

    iframeService = TestBed.inject(IframeService);
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SiteMapComponent);
    component = fixture.componentInstance;
    componentEl = fixture.nativeElement;
  });

  describe('no tree items', () => {
    it('should display empty tree', () => {
      component.siteMap = [];
      component.ngOnChanges({
        siteMap: {  } as SimpleChange,
      });

      fixture.detectChanges();

      expect(componentEl.querySelector('mat-tree')).toMatchSnapshot();
    });
  });

  describe('tree behavior', () => {
    beforeEach(() => {
      component.siteMap = mockSiteMapTree;
      component.renderPathInfo = mockRenderPathInfo;
      component.ngOnChanges({
        renderPathInfo: {  } as SimpleChange,
        siteMap: {  } as SimpleChange,
      });

      fixture.detectChanges();
    });

    it('should show the tree when loaded', () => {
      expect(componentEl.querySelector('mat-tree')).toMatchSnapshot();
    });

    it('should indicate selected node', () => {
      component.renderPathInfo = '/idTest4';
      component.ngOnChanges({
        renderPathInfo: {  } as SimpleChange,
      });

      fixture.detectChanges();

      expect(componentEl.querySelector('mat-tree')).toMatchSnapshot();
    });

    it('should scroll selected node into view', () => {
      scrollIntoViewMock.mockClear();

      component.renderPathInfo = '/idTest4';
      component.ngOnChanges({
        renderPathInfo: {  } as SimpleChange,
      });
      fixture.detectChanges();

      expect(scrollIntoViewMock).toHaveBeenCalled();
    });

    it('should load page on node click', () => {
      const rootNode = componentEl.querySelector<HTMLElement>('.qa-tree-node-name-root');

      rootNode?.click();

      expect(iframeService.load).toHaveBeenCalledWith(mockSiteMapTree[0].renderPathInfo);
    });
  });

  describe('search', () => {
    beforeEach(() => {
      component.siteMap = mockSiteMapTree;
      component.renderPathInfo = mockRenderPathInfo;
      component.ngOnChanges({
        renderPathInfo: {} as SimpleChange,
        siteMap: {} as SimpleChange,
      });

      fixture.detectChanges();
    });

    it('should hide not matching nodes', fakeAsync(() => {
      component.search$.next('Test4');
      tick(500);
      fixture.detectChanges();

      expect(componentEl.querySelector<HTMLElement>('.qa-tree-node-name-nameTest2')?.className).toContain('hidden');
      expect(componentEl.querySelector<HTMLElement>('.qa-tree-node-name-nameTest4')?.className).not.toContain('hidden');
    }));

    it('should be case insensitive', fakeAsync(() => {
      component.search$.next('nametest2');
      tick(500);
      fixture.detectChanges();

      expect(componentEl.querySelector<HTMLElement>('.qa-tree-node-name-nameTest2')?.className).not.toContain('hidden');
    }));

    it('should keep parent nodes', fakeAsync(() => {
      component.search$.next('Test3');
      tick(500);
      fixture.detectChanges();

      const parentNode = componentEl.querySelector<HTMLElement>('.qa-tree-node-name-nameTest2');

      expect(parentNode?.className).not.toContain('hidden');
      expect(parentNode?.className).toContain('disabled');
    }));

    it('should keep descendant nodes', fakeAsync(() => {
      component.search$.next('Test2');
      tick(500);
      fixture.detectChanges();

      expect(componentEl.querySelector<HTMLElement>('.qa-tree-node-name-nameTest3')?.className).not.toContain('hidden');
    }));
  });
});
