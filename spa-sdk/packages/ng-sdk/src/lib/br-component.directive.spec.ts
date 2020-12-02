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

import { mocked } from 'ts-jest/utils';
import { Component, Input, TemplateRef, ViewChild } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { Component as SpaComponent, Page, isComponent } from '@bloomreach/spa-sdk';
import { BrComponentContext, BrComponentDirective } from './br-component.directive';
import { BrNodeDirective } from './br-node.directive';
import { BrPageComponent } from './br-page/br-page.component';

jest.mock('@bloomreach/spa-sdk');

@Component({
  template: `
    <ng-template #template let-implicit let-component="component" let-page="page" let-template="template">
      <span>Implicit: {{ implicit.getName() }}</span>
      <span>Component: {{ component.getName() }}</span>
      <span>Page: {{ page.getTitle() }}</span>
      <span>Template: {{ template }}</span>
    </ng-template>
  `,
})
class TemplateComponent {
  @ViewChild('template') template!: TemplateRef<BrComponentContext>;
}

@Component({ template: '<ng-container [brComponent]="component"></ng-container>' })
class TestComponent {
  @Input() component?: SpaComponent | string;
}

describe('BrComponentDirective', () => {
  let component: jest.Mocked<SpaComponent>;
  let page: jest.Mocked<Page>;
  let template: TemplateRef<BrComponentContext>;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(() => {
    jest.resetAllMocks();

    component = {
      getChildren: jest.fn(() => []),
      getComponent: jest.fn(),
      getName: jest.fn(() => 'something'),
    } as unknown as typeof component;
    page = {
      getComponent: jest.fn(() => component),
      getTitle: jest.fn(() => 'Some Page'),
    } as unknown as typeof page;
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ BrComponentDirective, TemplateComponent, TestComponent ],
      providers: [
        { provide: BrPageComponent, useFactory: () => ({ node: template, state: new BehaviorSubject(page) }) },
        { provide: TemplateRef, useValue: 'Some Template' },
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    const templateFixture = TestBed.createComponent(TemplateComponent);
    templateFixture.detectChanges();
    ({ template } = templateFixture.componentInstance);

    fixture = TestBed.createComponent(TestComponent);
  });

  describe('ngOnDestroy', () => {
    it('should clear the container', () => {
      mocked(isComponent).mockReturnValue(true);
      fixture.componentInstance.component = component;
      fixture.detectChanges();
      fixture.destroy();

      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });

  describe('ngOnChanges', () => {
    it('should rerender a component on change', () => {
      mocked(isComponent).mockReturnValue(true);
      fixture.componentInstance.component = component;
      fixture.detectChanges();

      fixture.componentInstance.component = { getName: () => 'updated' } as SpaComponent;
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });

  describe('ngOnInit', () => {
    it('should render nothing if the page component is not ready', () => {
      fixture.debugElement.injector.get(BrPageComponent).state.next(undefined);
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
    });

    it('should render a component', () => {
      mocked(isComponent).mockReturnValue(true);
      fixture.componentInstance.component = component;
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
    });

    it('should render root node children if the component is not set', () => {
      const child1 = { getName: () => 'child1' } as SpaComponent;
      const child2 = { getName: () => 'child2' } as SpaComponent;
      component.getChildren.mockReturnValue([child1, child2]);
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
    });

    it('should render nothing if there is no root node', () => {
      page.getComponent.mockReturnValue(undefined);
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
    });

    it('should render a component using path', () => {
      const child = { getName: () => 'child' } as SpaComponent;
      component.getComponent.mockReturnValue(child);
      fixture.componentInstance.component = 'a/b/c';
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
      expect(component.getComponent).toBeCalledWith('a', 'b', 'c');
    });

    it('should render nothing if no component was found by the specified path', () => {
      fixture.componentInstance.component = 'a/b/c';
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
      expect(component.getComponent).toBeCalledWith('a', 'b', 'c');
    });

    it('should render current node children if the component is not set', () => {
      TestBed.resetTestingModule();
      TestBed.configureTestingModule({
        declarations: [ BrComponentDirective, TemplateComponent, TestComponent ],
        providers: [
          { provide: BrNodeDirective, useValue: { component } },
          { provide: BrPageComponent, useValue: { node: template, state: new BehaviorSubject(page) } },
          { provide: TemplateRef, useValue: 'Some Template' },
        ],
      })
      .compileComponents();
      fixture = TestBed.createComponent(TestComponent);

      const child1 = { getName: () => 'child1' } as SpaComponent;
      const child2 = { getName: () => 'child2' } as SpaComponent;
      component.getChildren.mockReturnValue([child1, child2]);
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });
});
