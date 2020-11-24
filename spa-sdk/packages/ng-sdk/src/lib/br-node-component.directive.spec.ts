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

import { Component, ElementRef, Input, NgModule, TemplateRef, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { Component as SpaComponent, MetaCollection, Page } from '@bloomreach/spa-sdk';
import { BrComponentContext } from './br-component.directive';
import { BrNodeComponentDirective } from './br-node-component.directive';
import { BrNodeDirective } from './br-node.directive';
import { BrPageComponent } from './br-page/br-page.component';
import { BehaviorSubject } from 'rxjs';

@Component({
  selector: 'br-component-test',
  template: `
    <span>Component: {{ component.getName() }}</span>
    <span>Page: {{ page.getTitle() }}</span>
  `,
})
class ComponentTestComponent {
  @Input() component!: SpaComponent;
  @Input() page!: Page;
}

@NgModule({
  declarations: [ComponentTestComponent],
  entryComponents: [ComponentTestComponent],
})
class TestModule {}

@Component({
  template: `
    <ng-template #template let-implicit let-component="component" let-page="page">
      <span class="head">Implicit: {{ implicit.getName() }}</span>
      <span>Component: {{ component.getName() }}</span>
      <span class="tail">Page: {{ page.getTitle() }}</span>
    </ng-template>
  `,
})
class TemplateComponent {
  @ViewChild('template') template!: TemplateRef<BrComponentContext>;
}

@Component({ template: '<ng-container #container [brNodeComponent]="component"></ng-container>' })
class TestComponent {
  @Input() component!: SpaComponent;
  @ViewChild('container') container!: ElementRef<Comment>;
}

describe('BrNodeComponentDirective', () => {
  let clear: jest.Mocked<ReturnType<MetaCollection['render']>>;
  let component: jest.Mocked<SpaComponent>;
  let meta: jest.Mocked<MetaCollection>;
  let node: BrNodeDirective;
  let page: BrPageComponent;
  let template: TemplateRef<BrComponentContext>;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(() => {
    clear = jest.fn();
    meta = { render: jest.fn(() => clear) } as unknown as typeof meta;
    component = {
      getChildren: jest.fn(() => []),
      getMeta: jest.fn(() => meta),
      getName: jest.fn(() => 'something'),
    } as unknown as typeof component;
    node = {} as BrNodeDirective;
    page = {
      mapping: {},
      state: new BehaviorSubject({ getTitle: () => 'Some Page' }),
    } as unknown as typeof page;
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ BrNodeComponentDirective, TemplateComponent, TestComponent ],
      imports: [ TestModule ],
      providers: [
        { provide: BrNodeDirective, useFactory: () => node },
        { provide: BrPageComponent, useFactory: () => page },
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    const templateFixture = TestBed.createComponent(TemplateComponent);
    templateFixture.detectChanges();
    ({ template } = templateFixture.componentInstance);

    fixture = TestBed.createComponent(TestComponent);
    fixture.componentInstance.component = component;
  });

  describe('ngOnChanges', () => {
    it('should render a node template', () => {
      node.template = template;
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
      expect(meta.render).toBeCalledWith(
        fixture.nativeElement.querySelector('.head'),
        fixture.nativeElement.querySelector('.tail'),
      );
    });

    it('should render children when there is no mapping', () => {
      const child1 = { getName: () => 'child1' } as SpaComponent;
      const child2 = { getName: () => 'child2' } as SpaComponent;
      component.getChildren.mockReturnValue([child1, child2]);
      page.node = template;
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
      expect(meta.render).toBeCalledWith(
        fixture.nativeElement.querySelectorAll('.head')[0],
        fixture.nativeElement.querySelectorAll('.tail')[1],
      );
    });

    it('should surround the container with meta-data when there is no children to render', () => {
      fixture.detectChanges();

      expect(meta.render).toBeCalledWith(
        fixture.componentInstance.container.nativeElement,
        fixture.componentInstance.container.nativeElement,
      );
    });

    it('should render a mapped component', () => {
      page.mapping.something = ComponentTestComponent;
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
      expect(meta.render).toBeCalledWith(
        fixture.nativeElement.querySelector('br-component-test'),
        fixture.nativeElement.querySelector('br-component-test'),
      );
    });
  });

  describe('ngOnDestroy', () => {
    it('should clear the container', () => {
      node.template = template;
      fixture.detectChanges();
      fixture.destroy();

      expect(fixture.nativeElement).toMatchSnapshot();
      expect(clear).toBeCalled();
    });
  });
});
