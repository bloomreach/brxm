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

import { Component, DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { async, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import * as normalizeWheelEventModule from '../../../helpers/normalize-wheel-event';

import { MenuScrollComponent } from './menu-scroll.component';

@Component({
  template: `
    <brna-menu-scroll>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
    </brna-menu-scroll>
  `,
})
class EnoughSpaceTestComponent {}

@Component({
  template: `
    <brna-menu-scroll [style.height.px]="scrollContainerHeight">
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
    </brna-menu-scroll>
  `,
})
class NotEnoughSpaceTestComponent {
  scrollContainerHeight = 150;
}

describe('MenuScrollComponent', () => {
  describe('if there is enough space', () => {
    let component: EnoughSpaceTestComponent;
    let fixture: ComponentFixture<EnoughSpaceTestComponent>;
    let de: DebugElement;
    let contentDe: DebugElement;

    beforeEach(async(() => {
      fixture = TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
        ],
        declarations: [
          EnoughSpaceTestComponent,
          MenuScrollComponent,
        ],
        schemas: [NO_ERRORS_SCHEMA],
      }).createComponent(EnoughSpaceTestComponent);

      component = fixture.componentInstance;
      de = fixture.debugElement;
      contentDe = de.query(By.css('.content'));

      fixture.detectChanges();
    }));

    it('should be created', () => {
      expect(component).toBeTruthy();
    });

    it('should not show scroll up button', () => {
      const scrollUpButton = de.query(By.css('.arrow.arrow-up'));

      expect(scrollUpButton).toBeNull();
    });

    it('should not show scroll down button', () => {
      const scrollDownButton = de.query(By.css('.arrow.arrow-down'));

      expect(scrollDownButton).toBeNull();
    });

    it('should not scroll the content', () => {
      const event = {
        preventDefault: () => {},
        deltaY: 10,
      };

      contentDe.triggerEventHandler('wheel', event);

      expect(contentDe.styles.transform).toBe('translate3d(0, -0px, 0)');
    });
  });

  describe('if there is not enough space', () => {
    let component: NotEnoughSpaceTestComponent;
    let fixture: ComponentFixture<NotEnoughSpaceTestComponent>;
    let de: DebugElement;
    let contentDe: DebugElement;

    let mouseEvent: jasmine.SpyObj<MouseEvent>;
    let wheelEvent: jasmine.SpyObj<WheelEvent>;

    beforeEach(fakeAsync(() => {
      spyOnProperty(normalizeWheelEventModule, 'normalizeWheelEvent', 'get').and.returnValue((event: WheelEvent) => ({
        x: 0,
        y: event.deltaY,
      }));

      mouseEvent = jasmine.createSpyObj('mouseEvent', [
        'preventDefault',
      ]);

      wheelEvent = jasmine.createSpyObj('wheelEvent',
        [
          'preventDefault',
        ],
        {
          deltaY: 10,
        },
      );

      fixture = TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
        ],
        declarations: [
          NotEnoughSpaceTestComponent,
          MenuScrollComponent,
        ],
        schemas: [NO_ERRORS_SCHEMA],
      }).createComponent(NotEnoughSpaceTestComponent);

      component = fixture.componentInstance;
      de = fixture.debugElement;
      contentDe = de.query(By.css('.content'));

      fixture.detectChanges(); // Call ngAfterViewInit which schedule height calculation
      tick();                  // Perform height calculation
      fixture.detectChanges(); // Reflect changed in DOM

      tick();
    }));

    it('should create', () => {
      expect(component).toBeTruthy();
    });

    it('should not show scroll up button', () => {
      const scrollUpButton = de.query(By.css('.arrow.arrow-up'));

      expect(scrollUpButton).toBeNull();
    });

    it('should show scroll down button', () => {
      const scrollDownButton = de.query(By.css('.arrow.arrow-down'));

      expect(scrollDownButton).not.toBeNull();
    });

    it('should prevent default action if scroll down button is clicked', () => {
      const scrollDownButton = de.query(By.css('.arrow.arrow-down'));

      scrollDownButton.triggerEventHandler('click', mouseEvent);

      expect(mouseEvent.preventDefault).toHaveBeenCalled();
    });

    it('should scroll step down if scroll down button is clicked', fakeAsync(() => {
      const scrollDownButton = de.query(By.css('.arrow.arrow-down'));

      scrollDownButton.triggerEventHandler('click', mouseEvent);

      fixture.detectChanges();

      tick();

      expect(contentDe.styles.transform).toBe('translate3d(0, -86px, 0)');
    }));

    it('should prevent default action if content is scrolled', () => {
      contentDe.triggerEventHandler('wheel', wheelEvent);

      expect(wheelEvent.preventDefault).toHaveBeenCalled();
    });

    it('should not scroll the content up', fakeAsync(() => {
      const event = {
        preventDefault: () => {},
        deltaY: -10,
      };

      contentDe.triggerEventHandler('wheel', event);

      fixture.detectChanges();

      tick();

      expect(contentDe.styles.transform).toBe('translate3d(0, -0px, 0)');
    }));

    it('should scroll the content', fakeAsync(() => {
      contentDe.triggerEventHandler('wheel', wheelEvent);

      fixture.detectChanges();

      tick();

      expect(contentDe.styles.transform).toBe('translate3d(0, -10px, 0)');
    }));

    describe('when content scrolled 10px down', () => {
      beforeEach(async(() => {
        contentDe.triggerEventHandler('wheel', wheelEvent);

        fixture.detectChanges();
      }));

      it('should show scroll up button', () => {
        const scrollUpButton = de.query(By.css('.arrow.arrow-up'));

        expect(scrollUpButton).not.toBeNull();
      });

      it('should show scroll down button', () => {
        const scrollDownButton = de.query(By.css('.arrow.arrow-down'));

        expect(scrollDownButton).not.toBeNull();
      });

      it('should scroll step up if scroll down button is clicked', fakeAsync(() => {
        const scrollDownButton = de.query(By.css('.arrow.arrow-up'));

        scrollDownButton.triggerEventHandler('click', mouseEvent);

        fixture.detectChanges();

        tick();

        expect(contentDe.styles.transform).toBe('translate3d(0, -0px, 0)');
      }));

      it('should scroll step down if scroll down button is clicked', fakeAsync(() => {
        const scrollDownButton = de.query(By.css('.arrow.arrow-down'));

        scrollDownButton.triggerEventHandler('click', mouseEvent);

        fixture.detectChanges();

        tick();

        expect(contentDe.styles.transform).toBe('translate3d(0, -96px, 0)');
      }));

      it('should scroll the content up', fakeAsync(() => {
        const event = {
          preventDefault: () => {},
          deltaY: -2,
        };

        contentDe.triggerEventHandler('wheel', event);

        fixture.detectChanges();

        tick();

        expect(contentDe.styles.transform).toBe('translate3d(0, -8px, 0)');
      }));

      it('should scroll the content down', fakeAsync(() => {
        contentDe.triggerEventHandler('wheel', wheelEvent);

        fixture.detectChanges();

        tick();

        expect(contentDe.styles.transform).toBe('translate3d(0, -20px, 0)');
      }));
    });

    describe('when content scrolled fully down', () => {
      beforeEach(async(() => {
        const event = {
          preventDefault: () => {},
          deltaY: 350,
        };

        contentDe.triggerEventHandler('wheel', event);

        fixture.detectChanges();
      }));

      it('should show scroll up button', () => {
        const scrollUpButton = de.query(By.css('.arrow.arrow-up'));

        expect(scrollUpButton).not.toBeNull();
      });

      it('should not show scroll down button', () => {
        const scrollDownButton = de.query(By.css('.arrow.arrow-down'));

        expect(scrollDownButton).toBeNull();
      });

      it('should scroll step up if scroll down button is clicked', fakeAsync(() => {
        const scrollDownButton = de.query(By.css('.arrow.arrow-up'));

        scrollDownButton.triggerEventHandler('click', mouseEvent);

        fixture.detectChanges();

        tick();

        expect(contentDe.styles.transform).toBe('translate3d(0, -264px, 0)');
      }));

      it('should scroll the content up', fakeAsync(() => {
        const event = {
          preventDefault: () => {},
          deltaY: -10,
        };

        contentDe.triggerEventHandler('wheel', event);

        fixture.detectChanges();

        tick();

        expect(contentDe.styles.transform).toBe('translate3d(0, -340px, 0)');
      }));

      it('should scroll the content down', fakeAsync(() => {
        contentDe.triggerEventHandler('wheel', wheelEvent);

        fixture.detectChanges();

        tick();

        expect(contentDe.styles.transform).toBe('translate3d(0, -350px, 0)');
      }));

      describe('when container\'s height is increased due to window resize', () => {
        beforeEach(fakeAsync(() => {
          component.scrollContainerHeight = 200;

          fixture.detectChanges();
          tick();

          window.dispatchEvent(new Event('resize'));

          fixture.detectChanges();
          tick();
        }));

        it('should scroll content up', () => {
          expect(contentDe.styles.transform).toBe('translate3d(0, -300px, 0)');
        });
      });
    });
  });
});
