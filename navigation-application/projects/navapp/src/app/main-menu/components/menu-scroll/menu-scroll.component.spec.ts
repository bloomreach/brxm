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

import { Component, DebugElement, NO_ERRORS_SCHEMA, ViewChild } from '@angular/core';
import { async, ComponentFixture, fakeAsync, flush, TestBed, tick } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

import * as normalizeWheelEventModule from '../../../helpers/normalize-wheel-event';

import { MenuScrollComponent } from './menu-scroll.component';

@Component({
  template: `
    <brna-menu-scroll [height]="scrollContainerHeight">
      <div [style.height.px]="contentFirstElementHeight"></div>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
      <div style="height: 100px"></div>
    </brna-menu-scroll>
  `,
})
class TestComponent {
  @ViewChild(MenuScrollComponent, { static: true })
  menuScroll: MenuScrollComponent;

  scrollContainerHeight = 600;
  contentFirstElementHeight = 100;
}

xdescribe('MenuScrollComponent', () => {
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;
  let de: DebugElement;
  let host: DebugElement;
  let contentDe: DebugElement;

  describe('if there is enough space', () => {
    beforeEach(async(() => {
      fixture = TestBed.configureTestingModule({
        imports: [
          NoopAnimationsModule,
        ],
        declarations: [
          TestComponent,
          MenuScrollComponent,
        ],
        schemas: [NO_ERRORS_SCHEMA],
      }).createComponent(TestComponent);

      component = fixture.componentInstance;
      de = fixture.debugElement;
      host = de.query(By.css('brna-menu-scroll'));
      contentDe = de.query(By.css('.content'));

      component.scrollContainerHeight = 600;

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

      host.triggerEventHandler('wheel', event);

      expect(contentDe.styles.transform).toBe('translateY(-0px)');
    });
  });

  describe('if there is not enough space', () => {
    let mouseEvent: jasmine.SpyObj<MouseEvent>;
    let wheelEvent: jasmine.SpyObj<WheelEvent>;

    beforeEach(fakeAsync(() => {
      spyOnProperty(normalizeWheelEventModule, 'normalizeWheelEvent', 'get').and.returnValue((event: WheelEvent) => ({
        x: 0,
        y: event.deltaY,
      }));

      mouseEvent = jasmine.createSpyObj('mouseEvent', [
        'preventDefault',
        'stopPropagation',
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
          TestComponent,
          MenuScrollComponent,
        ],
        schemas: [NO_ERRORS_SCHEMA],
      }).createComponent(TestComponent);

      component = fixture.componentInstance;
      de = fixture.debugElement;
      host = de.query(By.css('brna-menu-scroll'));
      contentDe = de.query(By.css('.content'));

      component.scrollContainerHeight = 150;

      fixture.detectChanges(); // Call ngAfterViewInit which schedule height calculation
      tick();                  // Perform height calculation
      fixture.detectChanges(); // Reflect changes in DOM
      flush();
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

      expect(contentDe.styles.transform).toBe('translateY(-86px)');
    }));

    it('should prevent default action if content is scrolled', () => {
      host.triggerEventHandler('wheel', wheelEvent);

      expect(wheelEvent.preventDefault).toHaveBeenCalled();
    });

    it('should not scroll the content up', fakeAsync(() => {
      const event = {
        preventDefault: () => {},
        deltaY: -10,
      };

      host.triggerEventHandler('wheel', event);

      fixture.detectChanges();

      flush();

      expect(contentDe.styles.transform).toBe('translateY(-0px)');
    }));

    it('should scroll the content', fakeAsync(() => {
      host.triggerEventHandler('wheel', wheelEvent);

      fixture.detectChanges();

      flush();

      expect(contentDe.styles.transform).toBe('translateY(-10px)');
    }));

    describe('when content scrolled 10px down', () => {
      beforeEach(async(() => {
        host.triggerEventHandler('wheel', wheelEvent);

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

        expect(contentDe.styles.transform).toBe('translateY(-0px)');
      }));

      it('should scroll step down if scroll down button is clicked', fakeAsync(() => {
        const scrollDownButton = de.query(By.css('.arrow.arrow-down'));

        scrollDownButton.triggerEventHandler('click', mouseEvent);

        fixture.detectChanges();

        tick();

        expect(contentDe.styles.transform).toBe('translateY(-96px)');
      }));

      it('should scroll the content up', fakeAsync(() => {
        const event = {
          preventDefault: () => {},
          deltaY: -2,
        };

        host.triggerEventHandler('wheel', event);

        fixture.detectChanges();

        flush();

        expect(contentDe.styles.transform).toBe('translateY(-8px)');
      }));

      it('should scroll the content down', fakeAsync(() => {
        host.triggerEventHandler('wheel', wheelEvent);

        fixture.detectChanges();

        flush();

        expect(contentDe.styles.transform).toBe('translateY(-20px)');
      }));

      describe('when content\'s height is decreased', () => {
        beforeEach(async(() => {
          component.contentFirstElementHeight = 50;

          fixture.detectChanges();
        }));

        describe('and manual content height update is initiated', () => {
          beforeEach(fakeAsync(() => {
            component.menuScroll.updateContentHeight();
            tick(50);

            fixture.detectChanges();
            tick();
          }));

          it('should auto scroll up', () => {
            expect(contentDe.styles.transform).toBe('translateY(-0px)');
          });
        });
      });
    });

    describe('when content scrolled fully down', () => {
      beforeEach(async(() => {
        const event = {
          preventDefault: () => {},
          deltaY: 350,
        };

        host.triggerEventHandler('wheel', event);

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

        expect(contentDe.styles.transform).toBe('translateY(-264px)');
      }));

      it('should scroll the content up', fakeAsync(() => {
        const event = {
          preventDefault: () => {},
          deltaY: -10,
        };

        host.triggerEventHandler('wheel', event);

        fixture.detectChanges();

        flush();

        expect(contentDe.styles.transform).toBe('translateY(-340px)');
      }));

      it('should scroll the content down', fakeAsync(() => {
        host.triggerEventHandler('wheel', wheelEvent);

        fixture.detectChanges();

        flush();

        expect(contentDe.styles.transform).toBe('translateY(-350px)');
      }));

      describe('when container\'s height is increased', () => {
        beforeEach(async(() => {
          component.scrollContainerHeight = 200;

          fixture.detectChanges();
        }));

        it('should scroll content up', () => {
          expect(contentDe.styles.transform).toBe('translateY(-300px)');
        });
      });

      describe('when content\'s height is decreased', () => {
        beforeEach(async(() => {
          component.contentFirstElementHeight = 50;

          fixture.detectChanges();
        }));

        describe('and manual content height update is initiated', () => {
          beforeEach(fakeAsync(() => {
            component.menuScroll.updateContentHeight();
            tick(50);

            fixture.detectChanges();
            tick();
          }));

          it('should auto scroll up', () => {
            expect(contentDe.styles.transform).toBe('translateY(-300px)');
          });
        });
      });
    });
  });
});
