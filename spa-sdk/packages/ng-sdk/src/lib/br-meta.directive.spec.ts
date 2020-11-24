/*
 * Copyright 2020 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { Component, Directive, Input } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { MetaCollection } from '@bloomreach/spa-sdk';
import { BrMetaDirective } from './br-meta.directive';

@Directive({ selector: '[brMeta]' })
class BrMetaTestDirective extends BrMetaDirective {
  @Input('brMeta') protected meta!: MetaCollection;
}

class TestComponent {
  @Input() meta!: MetaCollection;
}

describe('BrMetaDirective', () => {
  let clear: jest.Mocked<ReturnType<MetaCollection['render']>>;
  let meta: jest.Mocked<MetaCollection>;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(() => {
    clear = jest.fn();
    meta = { render: jest.fn(() => clear) } as unknown as typeof meta;
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ BrMetaTestDirective, TestComponent ],
    })
    .compileComponents();

    fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.meta = meta;
    fixture.detectChanges();
  }));

  describe('ngOnChanges', () => {
    describe('when there is no template', () => {
      beforeAll(() => {
        Component({ template: '<a [brMeta]="meta"></a>' })(TestComponent);
      });

      it('should surround the container with meta', () => {
        expect(meta.render).toBeCalledWith(
          fixture.nativeElement.querySelector('a'),
          fixture.nativeElement.querySelector('a'),
        );
      });

      it('should clear the previously rendered meta', () => {
        fixture.componentInstance.meta = { ...meta };
        fixture.detectChanges();

        expect(clear).toBeCalled();
      });

      it('should not rerender if the meta was not changed', () => {
        fixture.detectChanges();

        expect(clear).not.toBeCalled();
      });
    });

    describe('when there is a template', () => {
      beforeAll(() => {
        Component({
          template: `
            <ng-template [brMeta]="meta">
              <span></span>
              <span></span>
            </ng-template>
          `,
        })(TestComponent);
      });

      it('should surround the template with meta', () => {
        expect(fixture.nativeElement).toMatchSnapshot();
      });

      it('should render the template', () => {
        expect(meta.render).toBeCalledWith(
          fixture.nativeElement.querySelectorAll('span')[0],
          fixture.nativeElement.querySelectorAll('span')[1],
        );
      });
    });
  });

  describe('ngOnDestroy', () => {
    beforeAll(() => {
      Component({
        template: `
          <ng-template [brMeta]="meta">
            <a></a>
          </ng-template>
        `,
      })(TestComponent);
    });

    beforeEach(() => {
      fixture.destroy();
    });

    it('should clear the container', () => {
      expect(fixture.nativeElement).toMatchSnapshot();
    });

    it('should clear the meta', () => {
      expect(clear).toBeCalled();
    });
  });
});
