/*
 * Copyright 2020-2021 Hippo B.V. (http://www.onehippo.com)
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
import { Component, Input, NgModule } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { BehaviorSubject } from 'rxjs';
import { ContainerItem, TYPE_CONTAINER_ITEM_UNDEFINED } from '@bloomreach/spa-sdk';
import {
  BrContainerItemUndefinedComponent,
} from './br-container-item-undefined/br-container-item-undefined.component';
import { BrNodeContainerItemDirective } from './br-node-container-item.directive';
import { BrNodeDirective } from './br-node.directive';
import { BrPageComponent } from './br-page/br-page.component';

Component({
  selector: 'br-container-item-undefined',
  template: '',
})(BrContainerItemUndefinedComponent);

@Component({
  selector: 'br-container-item-test',
  template: '<a>{{ component.getModels().data }}</a>',
})
class ContainerItemTestComponent {}

@NgModule({
  declarations: [BrContainerItemUndefinedComponent, ContainerItemTestComponent],
  entryComponents: [BrContainerItemUndefinedComponent, ContainerItemTestComponent],
})
class TestModule {}

@Component({ template: '<ng-container [brNodeContainerItem]="containerItem"></ng-container>' })
class TestComponent {
  @Input() containerItem!: ContainerItem;
}

describe('BrNodeContainerItemDirective', () => {
  let containerItem: ContainerItem;
  let node: BrNodeDirective;
  let page: BrPageComponent;
  let component: TestComponent;
  let fixture: ComponentFixture<TestComponent>;

  beforeEach(() => {
    containerItem = {
      getType: jest.fn(() => 'something'),
      getMeta: () => ({
        clear: jest.fn(),
        render: jest.fn(),
      }),
      getModels: jest.fn(() => ({ data: 'something' })),
      off: jest.fn(),
      on: jest.fn(),
    } as unknown as typeof containerItem;
    node = {} as typeof node;
    page = {
      mapping: {},
      state: new BehaviorSubject({ sync: jest.fn() }),
    } as unknown as typeof page;
  });

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ TestComponent, BrNodeContainerItemDirective ],
      imports: [ TestModule ],
      providers: [
        { provide: BrNodeDirective, useFactory: () => node },
        { provide: BrPageComponent, useFactory: () => page },
      ],
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    component.containerItem = containerItem;
    page.mapping.something = ContainerItemTestComponent;

    fixture.detectChanges();
  });

  describe('getMapping', () => {
    it('should render a mapped container item', () => {
      expect(fixture.nativeElement).toMatchSnapshot();
    });

    it('should render undefined container item', () => {
      mocked(containerItem.getType).mockReturnValueOnce('undefined');
      component.containerItem = { ...containerItem };
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
    });

    it('should override undefined container item', () => {
      page.mapping[TYPE_CONTAINER_ITEM_UNDEFINED as any] = ContainerItemTestComponent;
      mocked(containerItem.getType).mockReturnValueOnce('undefined');
      component.containerItem = { ...containerItem };
      fixture.detectChanges();

      expect(fixture.nativeElement).toMatchSnapshot();
    });
  });

  describe('ngOnChanges', () => {
    it('should react on update evants', () => {
      mocked(containerItem.getModels).mockReturnValue({ data: 'updated' });
      const [[, onUpdate]] = mocked(containerItem.on).mock.calls;
      onUpdate({});
      fixture.detectChanges();

      expect(page.state.getValue()?.sync).toBeCalled();
      expect(fixture.nativeElement).toMatchSnapshot();
    });

    it('should unsubscribe from the old container item', () => {
      const [[, onUpdate]] = mocked(containerItem.on).mock.calls;
      component.containerItem = { ...containerItem };
      fixture.detectChanges();

      expect(containerItem.off).toBeCalledWith('update', onUpdate);
    });
  });

  describe('ngOnDestroy', () => {
    it('should stop reacting on update events after destruction', () => {
      const [[, onUpdate]] = mocked(containerItem.on).mock.calls;
      fixture.destroy();

      expect(containerItem.off).toBeCalledWith('update', onUpdate);
    });
  });
});
