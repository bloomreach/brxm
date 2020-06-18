/*!
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
import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { QaHelperService } from '../../../services/qa-helper.service';
import { MenuItemContainer } from '../../models/menu-item-container.model';
import { MenuItemLinkMock } from '../../models/menu-item-link.mock';
import { MenuItemLink } from '../../models/menu-item-link.model';
import { MenuStateService } from '../../services/menu-state.service';

import { ExpandableSubMenuItemComponent } from './expandable-sub-menu-item.component';

describe('ExpandableSubMenuItemComponent', () => {
  let component: ExpandableSubMenuItemComponent;
  let fixture: ComponentFixture<ExpandableSubMenuItemComponent>;
  let de: DebugElement;

  let menuStateServiceMock: jasmine.SpyObj<MenuStateService>;
  let qaHelperServiceMock: jasmine.SpyObj<QaHelperService>;

  const configMock = new MenuItemContainer(
    'some caption',
    [
      new MenuItemLinkMock({ id: 'link1' }),
      new MenuItemLinkMock({ id: 'link2' }),
      new MenuItemLinkMock({ id: 'link3' }),
    ],
    'some-icon',
  );

  beforeEach(async(() => {
    menuStateServiceMock = jasmine.createSpyObj('MenuStateService', [
      'isMenuItemHighlighted',
    ]);

    qaHelperServiceMock = jasmine.createSpyObj('QaHelperService', [
      'getMenuItemClass',
    ]);

    fixture = TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
      ],
      declarations: [ExpandableSubMenuItemComponent],
      providers: [
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: QaHelperService, useValue: qaHelperServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(ExpandableSubMenuItemComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    component.config = configMock;

    fixture.detectChanges();
  }));

  it('should show a caption', () => {
    const caption = de.query(By.css('.header'));

    expect(caption).not.toBeNull();
    expect(caption.nativeElement.textContent).toContain('some caption');
  });

  it('should contain a list of links', () => {
    const links = de.queryAll(By.css('.item'));

    expect(links.length).toBe(3);
  });

  it('should be collapsed', () => {
    expect(component.isOpened).toBeFalsy();
  });

  it('should be expanded', fakeAsync(() => {
    component.toggle();

    fixture.detectChanges();

    tick();

    expect(component.isOpened).toBeTruthy();
  }));

  it('should check for the menu active state', () => {
    menuStateServiceMock.isMenuItemHighlighted.and.returnValue(true);
    const link = new MenuItemLink('some-id', 'some caption');

    const actual = component.isHighlighted(link);

    expect(actual).toBeTruthy();
    expect(menuStateServiceMock.isMenuItemHighlighted).toHaveBeenCalledWith(link);
  });

  it('should get qa class', () => {
    qaHelperServiceMock.getMenuItemClass.and.returnValue('qa-class');
    const link = new MenuItemLink('some-id', 'some caption');

    const actual = component.getQaClass(link);

    expect(actual).toBe('qa-class');
    expect(qaHelperServiceMock.getMenuItemClass).toHaveBeenCalledWith(link);
  });

  describe('when it is expanded', () => {
    beforeEach(async(() => {
      component.toggle();

      fixture.detectChanges();
    }));

    it('should be collapsed by toggling', fakeAsync(() => {
      component.toggle();

      fixture.detectChanges();

      tick();

      expect(component.isOpened).toBeFalsy();
    }));

    it('should be collapsed by closing', fakeAsync(() => {
      component.close();

      fixture.detectChanges();

      tick();

      expect(component.isOpened).toBeFalsy();
    }));
  });
});
