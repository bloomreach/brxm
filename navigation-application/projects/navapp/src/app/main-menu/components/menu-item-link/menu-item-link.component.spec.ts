/*
 * Copyright 2019-2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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
import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { NavigationTrigger } from '@bloomreach/navapp-communication';
import { NEVER } from 'rxjs';

import { NavItemMock } from '../../../models/nav-item.mock';
import { NavItem } from '../../../models/nav-item.model';
import { NavigationService } from '../../../services/navigation.service';
import { UrlMapperService } from '../../../services/url-mapper.service';

import { MenuItemLinkComponent } from './menu-item-link.component';

describe('MenuItemLinkComponent', () => {
  let component: MenuItemLinkComponent;
  let fixture: ComponentFixture<MenuItemLinkComponent>;
  let de: DebugElement;

  let navigationServiceMock: jasmine.SpyObj<NavigationService>;
  let mouseEventObj: jasmine.SpyObj<MouseEvent>;

  beforeEach(async(() => {
    navigationServiceMock = jasmine.createSpyObj('NavigationService', [
      'navigateByNavItem',
      'navigateByUrl',
    ]);

    mouseEventObj = jasmine.createSpyObj('MouseEvent', [
      'preventDefault',
    ]);

    const urlMapperServiceMock = {
      mapNavItemToBrowserUrl: () => 'test',
    };

    fixture = TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
      ],
      declarations: [MenuItemLinkComponent],
      providers: [
        { provide: NavigationService, useValue: navigationServiceMock },
        { provide: UrlMapperService, useValue: urlMapperServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(MenuItemLinkComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    fixture.detectChanges();
  }));

  describe('in the initial state', () => {
    it('should not be highlighted', () => {
      expect(de.classes.highlighted).toBeFalsy();
    });

    it('should be disabled', () => {
      expect(de.classes.disabled).toBeTruthy();
    });

    it('should have "qa-disabled" class', () => {
      expect(de.classes['qa-disabled']).toBeTruthy();
    });

    it('should show an empty caption', () => {
      expect(de.nativeElement.textContent.trim()).toBe('');
    });

    describe('if received a click event', () => {
      beforeEach(() => {
        const a = de.query(By.css('a'));

        a.triggerEventHandler('click', mouseEventObj);
      });

      it('should prevent default event handling', () => {
        expect(mouseEventObj.preventDefault).toHaveBeenCalled();
      });

      it('should not navigate to the nav item', () => {
        expect(navigationServiceMock.navigateByNavItem).not.toHaveBeenCalled();
      });
    });
  });

  it('should show a caption', fakeAsync(() => {
    component.caption = 'some caption';

    fixture.detectChanges();

    tick();

    expect(de.nativeElement.textContent).toContain('some caption');
  }));

  describe('when a nav item is set', () => {
    let navItemMock: NavItem;
    let linkEl: DebugElement;

    beforeEach(async(() => {
      linkEl = de.query(By.css('a'));

      navItemMock = new NavItemMock({}, NEVER, false);

      component.navItem = navItemMock;

      fixture.detectChanges();
    }));

    it('should be disabled', () => {
      expect(de.classes.disabled).toBeTruthy();
    });

    describe('if received a click event', () => {
      beforeEach(() => {
        linkEl.triggerEventHandler('click', mouseEventObj);
      });

      it('should prevent default event handling', () => {
        expect(mouseEventObj.preventDefault).toHaveBeenCalled();
      });

      it('should not navigate to the nav item', () => {
        expect(navigationServiceMock.navigateByNavItem).not.toHaveBeenCalled();
      });
    });

    describe('and activated', () => {
      beforeEach(async(() => {
        navItemMock.activate();

        fixture.detectChanges();
      }));

      describe('if received a click event', () => {
        beforeEach(() => {
          linkEl.triggerEventHandler('click', mouseEventObj);
        });

        it('should prevent default event handling', () => {
          expect(mouseEventObj.preventDefault).toHaveBeenCalled();
        });

        it('should navigate to the nav item', () => {
          expect(navigationServiceMock.navigateByUrl).toHaveBeenCalledWith('test', NavigationTrigger.Menu);
        });
      });
    });
  });
});
