/*!
 * Copyright 2019-2023 Bloomreach
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
import { ComponentFixture, fakeAsync, TestBed, tick, waitForAsync } from '@angular/core/testing';
import { MatTooltipModule } from '@angular/material/tooltip';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { TranslateModule } from '@ngx-translate/core';
import { first } from 'rxjs/operators';

import { TopLevelMenuItemComponent } from './top-level-menu-item.component';

describe('TopLevelMenuItemComponent', () => {
  let component: TopLevelMenuItemComponent;
  let fixture: ComponentFixture<TopLevelMenuItemComponent>;
  let de: DebugElement;
  let wrapper: DebugElement;

  beforeEach(waitForAsync(() => {
    fixture = TestBed.configureTestingModule({
      imports: [
        MatTooltipModule,
        NoopAnimationsModule,
        TranslateModule.forRoot(),
      ],
      declarations: [TopLevelMenuItemComponent],
      schemas: [NO_ERRORS_SCHEMA],
    }).createComponent(TopLevelMenuItemComponent);

    component = fixture.componentInstance;
    de = fixture.debugElement;
    wrapper = de.query(By.css('.menu-item'));

    fixture.detectChanges();
  }));

  it('should contain the menu item wrapper', () => {
    expect(wrapper).toBeDefined();
  });

  describe('in the initial state', () => {
    it('should be collapsed', () => {
      expect(wrapper.classes.collapsed).toBeTruthy();
    });

    it('should not be highlighted', () => {
      expect(wrapper.classes.highlighted).toBeFalsy();
    });

    it('should not be pressed', () => {
      expect(wrapper.classes.pressed).toBeFalsy();
    });

    it('should not be failed', () => {
      expect(wrapper.classes.failed).toBeFalsy();
    });

    it('should show an empty caption', () => {
      const caption = wrapper.query(By.css('.caption'));

      expect(caption.nativeElement.textContent.trim()).toBe('');
    });
  });

  it('should show a caption', fakeAsync(() => {
    component.caption = 'some caption';

    fixture.detectChanges();

    tick();

    expect(de.nativeElement.textContent).toContain('some caption');
  }));

  it('should switch into the pressed state', fakeAsync(() => {
    component.pressed = true;

    fixture.detectChanges();

    tick();

    expect(wrapper.classes.pressed).toBeTruthy();
  }));

  it('should switch into the highlighted state', fakeAsync(() => {
    component.highlighted = true;

    fixture.detectChanges();

    tick();

    expect(wrapper.classes.highlighted).toBeTruthy();
  }));

  it('should switch into the expanded state', fakeAsync(() => {
    component.collapsed = false;

    fixture.detectChanges();

    tick();

    expect(wrapper.classes.collapsed).toBeFalsy();
  }));

  it('should switch into the failed state', fakeAsync(() => {
    component.failed = true;

    fixture.detectChanges();

    tick();

    expect(wrapper.classes.failed).toBeTrue();
  }));

  describe('failed tooltip', () => {
    it('should contain tooltip if failed', fakeAsync(() => {
      component.failed = true;

      fixture.detectChanges();

      expect(de.nativeElement.innerHTML).toContain('MENU_ITEM_TOOLTIP_FAILED_ERROR');
    }));

    it('should not contain tooltip if not failed', fakeAsync(() => {
      component.failed = false;

      fixture.detectChanges();

      expect(de.nativeElement.innerHTML).not.toContain('MENU_ITEM_TOOLTIP_FAILED_ERROR');
    }));
  });

  describe('click on the host element', () => {
    let emitted: boolean;
    let event: jasmine.SpyObj<MouseEvent>;

    beforeEach(() => {
      emitted = false;

      component.menuItemClick.pipe(
        first(),
      ).subscribe(() => emitted = true);

      event = jasmine.createSpyObj('MouseEvent', [
        'stopImmediatePropagation',
      ]);

      de.triggerEventHandler('click', event);
    });

    it('should stop immediate propagation', () => {
      expect(event.stopImmediatePropagation).toHaveBeenCalled();
    });

    it('should emit menuItemClick output', () => {
      expect(emitted).toBe(true);
    });
  });
});
