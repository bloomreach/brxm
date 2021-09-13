/*!
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
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import { QaHelperService } from '../../../services/qa-helper.service';
import { MenuItemContainer } from '../../models/menu-item-container.model';
import { MenuItemLink } from '../../models/menu-item-link.model';
import { MenuStateService } from '../../services/menu-state.service';
import { MenuScrollComponent } from '../menu-scroll/menu-scroll.component';

import { MenuDrawerComponent } from './menu-drawer.component';

describe('MenuDrawerComponent', () => {
  let component: MenuDrawerComponent;
  let fixture: ComponentFixture<MenuDrawerComponent>;
  let de: DebugElement;

  let menuStateServiceMock: jasmine.SpyObj<MenuStateService>;
  let qaHelperServiceMock: jasmine.SpyObj<QaHelperService>;

  let menuScrollComponentMock: jasmine.SpyObj<MenuScrollComponent>;

  beforeEach(waitForAsync(() => {
    menuStateServiceMock = jasmine.createSpyObj('MenuStateService', [
      'closeDrawer',
      'isMenuItemHighlighted',
      'isMenuItemFailed',
    ]);

    qaHelperServiceMock = jasmine.createSpyObj('QaHelperService', [
      'getMenuItemClass',
    ]);

    menuScrollComponentMock = jasmine.createSpyObj('MenuScrollComponent', [
      'updateContentHeight',
    ]);

    fixture = TestBed.configureTestingModule({
      imports: [
        NoopAnimationsModule,
      ],
      declarations: [MenuDrawerComponent],
      providers: [
        { provide: MenuStateService, useValue: menuStateServiceMock },
        { provide: QaHelperService, useValue: qaHelperServiceMock },
      ],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(MenuDrawerComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;

    fixture.debugElement.nativeElement.style.height = '200px';
    component.ngOnInit();

    fixture.detectChanges();
  }));

  it('should determine available height for the scrollable area', () => {
    expect(component.availableHeightForScrollableArea).toBe(200);
  });

  it('click outside should close the drawer', () => {
    component.onClickedOutside();

    expect(menuStateServiceMock.closeDrawer).toHaveBeenCalled();
  });

  it('should close all expandable menu items besides the one was clicked', () => {
    const expandableMenuItems = [
      jasmine.createSpyObj('ExpandableMenuItemComponent1', [
        'close',
      ]),
      jasmine.createSpyObj('ExpandableMenuItemComponent2', [
        'close',
      ]),
      jasmine.createSpyObj('ExpandableMenuItemComponent3', [
        'close',
      ]),
    ];

    component.expandableMenuItems = expandableMenuItems as any;

    component.onExpandableMenuItemClick(expandableMenuItems[1]);

    expect(expandableMenuItems[0].close).toHaveBeenCalled();
    expect(expandableMenuItems[1].close).not.toHaveBeenCalled();
    expect(expandableMenuItems[2].close).toHaveBeenCalled();
  });

  it('should return true for menu item containers', () => {
    const container = new MenuItemContainer('some caption', []);

    const actual = component.isContainer(container);

    expect(actual).toBeTruthy();
  });

  it('should return false for menu item links', () => {
    const link = new MenuItemLink('some-id', 'some caption');

    const actual = component.isContainer(link);

    expect(actual).toBeFalsy();
  });

  it('should initiate scrollable content height refresh', () => {
    component.menuScrollComponent = menuScrollComponentMock;

    component.onMenuItemsWrapperClick();

    expect(menuScrollComponentMock.updateContentHeight).toHaveBeenCalled();
  });

  it('should check for the menu active state', () => {
    menuStateServiceMock.isMenuItemHighlighted.and.returnValue(true);
    const link = new MenuItemLink('some-id', 'some caption');

    const actual = component.isHighlighted(link);

    expect(actual).toBeTruthy();
    expect(menuStateServiceMock.isMenuItemHighlighted).toHaveBeenCalledWith(link);
  });

  it('should check for the menu failed state', () => {
    menuStateServiceMock.isMenuItemFailed.and.returnValue(true);
    const link = new MenuItemLink('some-failed-id', 'some caption');

    const actual = component.isFailed(link);

    expect(actual).toBeTrue();
    expect(menuStateServiceMock.isMenuItemFailed).toHaveBeenCalledWith(link);
  });

  it('should get qa class', () => {
    qaHelperServiceMock.getMenuItemClass.and.returnValue('qa-class');
    const link = new MenuItemLink('some-id', 'some caption');

    const actual = component.getQaClass(link);

    expect(actual).toBe('qa-class');
    expect(qaHelperServiceMock.getMenuItemClass).toHaveBeenCalledWith(link);
  });

  it('should update available height for the scrollable area on window resize', () => {
    fixture.debugElement.nativeElement.style.height = '100px';

    window.dispatchEvent(new Event('resize'));

    expect(component.availableHeightForScrollableArea).toBe(100);
  });
});
