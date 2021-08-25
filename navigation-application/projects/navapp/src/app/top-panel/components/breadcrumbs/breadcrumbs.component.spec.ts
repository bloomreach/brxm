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

import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { Subject } from 'rxjs';

import { NavigationService } from '../../../services/navigation.service';
import { BreadcrumbsService } from '../../services/breadcrumbs.service';

import { BreadcrumbsComponent } from './breadcrumbs.component';

describe('BreadcrumbsComponent', () => {
  let component: BreadcrumbsComponent;
  let fixture: ComponentFixture<BreadcrumbsComponent>;
  let de: DebugElement;

  const breadcrumbsSubject = new Subject<string[]>();

  const breadcrumbsServiceMock = {
    breadcrumbs$: breadcrumbsSubject,
  };

  let navigationServiceMock: jasmine.SpyObj<NavigationService>;

  beforeEach(waitForAsync(() => {
    navigationServiceMock = jasmine.createSpyObj('NavigationService', [
      'navigateToDefaultAppPage',
    ]);

    fixture = TestBed.configureTestingModule({
      declarations: [BreadcrumbsComponent],
      providers: [
        { provide: BreadcrumbsService, useValue: breadcrumbsServiceMock },
        { provide: NavigationService, useValue: navigationServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(BreadcrumbsComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    fixture.detectChanges();
  }));

  it('should create the component', () => {
    expect(component).toBeDefined();
  });

  it('should not display breadcrumbs right after initialization', () => {
    const breadcrumbEls = de.queryAll(By.css('.entry'));

    expect(breadcrumbEls.length).toBe(0);
  });

  describe('when breadcrumbs are emitted', () => {
    beforeEach(waitForAsync(() => {
      breadcrumbsSubject.next([
        'Breadcrumb 1',
        'Breadcrumb 2',
        'Breadcrumb 3',
        'Some suffix',
      ]);

      fixture.detectChanges();
    }));

    it('should display breadcrumbs with a suffix', () => {
      const breadcrumbEls = de.queryAll(By.css('.entry'));

      expect(breadcrumbEls.length).toBe(4);
      expect(breadcrumbEls[0].nativeElement.textContent).toBe('Breadcrumb 1');
      expect(breadcrumbEls[1].nativeElement.textContent).toBe('Breadcrumb 2');
      expect(breadcrumbEls[2].nativeElement.textContent).toBe('Breadcrumb 3');
      expect(breadcrumbEls[3].nativeElement.textContent).toBe('Some suffix');
    });

    it('should display updated breadcrumbs with the same suffix', fakeAsync(() => {
      breadcrumbsSubject.next([
        'Some new breadcrumb 1',
        'Some new breadcrumb 2',
        'Some suffix',
      ]);

      fixture.detectChanges();

      tick();

      const breadcrumbEls = de.queryAll(By.css('.entry'));

      expect(breadcrumbEls.length).toBe(3);
      expect(breadcrumbEls[0].nativeElement.textContent).toBe('Some new breadcrumb 1');
      expect(breadcrumbEls[1].nativeElement.textContent).toBe('Some new breadcrumb 2');
      expect(breadcrumbEls[2].nativeElement.textContent).toBe('Some suffix');
    }));

    it('should display updated breadcrumbs without a suffix', fakeAsync(() => {
      breadcrumbsSubject.next([
        'Some new breadcrumb 1',
        'Some new breadcrumb 2',
        '',
      ]);

      fixture.detectChanges();

      tick();

      const breadcrumbEls = de.queryAll(By.css('.entry'));

      expect(breadcrumbEls.length).toBe(2);
      expect(breadcrumbEls[0].nativeElement.textContent).toBe('Some new breadcrumb 1');
      expect(breadcrumbEls[1].nativeElement.textContent).toBe('Some new breadcrumb 2');
    }));
  });

  describe('last breadcrumb click', () => {
    it('should trigger the navigation if there is a suffix', () => {
      breadcrumbsSubject.next([
        'breadcrumb',
        'Some suffix',
      ]);

      component.onLastBreadcrumbClicked();

      expect(navigationServiceMock.navigateToDefaultAppPage).toHaveBeenCalledWith(NavigationTrigger.Breadcrumbs);
    });

    it('should not trigger the navigation if there is not a suffix', () => {
      breadcrumbsSubject.next([
        'breadcrumb',
        '',
      ]);

      component.onLastBreadcrumbClicked();

      expect(navigationServiceMock.navigateToDefaultAppPage).not.toHaveBeenCalled();
    });
  });
});
