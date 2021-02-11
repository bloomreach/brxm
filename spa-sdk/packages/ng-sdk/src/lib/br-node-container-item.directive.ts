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

import {
  ChangeDetectorRef,
  ComponentFactoryResolver,
  Directive,
  Injector,
  OnChanges,
  OnDestroy,
  SimpleChanges,
  Type,
  ViewContainerRef,
} from '@angular/core';
import { ContainerItem, TYPE_CONTAINER_ITEM_UNDEFINED } from '@bloomreach/spa-sdk';
import {
  BrContainerItemUndefinedComponent,
} from './br-container-item-undefined/br-container-item-undefined.component';
import { BrNodeComponentDirective } from './br-node-component.directive';
import { BrNodeDirective } from './br-node.directive';
import { BrPageComponent } from './br-page/br-page.component';
import { BrProps } from './br-props.model';

@Directive({
  selector: '[brNodeContainerItem]',
  inputs: [ 'component:brNodeContainerItem' ], // tslint:disable-line: no-inputs-metadata-property
})
export class BrNodeContainerItemDirective
extends BrNodeComponentDirective<ContainerItem>
implements OnChanges, OnDestroy {
  constructor(
    container: ViewContainerRef,
    injector: Injector,
    componentFactoryResolver: ComponentFactoryResolver,
    node: BrNodeDirective,
    page: BrPageComponent,
    private changeDetectorRef: ChangeDetectorRef,
  ) {
    super(container, injector, componentFactoryResolver, node, page);

    this.onUpdate = this.onUpdate.bind(this);
  }

  ngOnChanges(changes: SimpleChanges): void {
    changes.component?.previousValue?.off('update', this.onUpdate);

    super.ngOnChanges(changes);

    this.component.on('update', this.onUpdate);
  }

  ngOnDestroy(): void {
    this.component.off('update', this.onUpdate);
  }

  private onUpdate(): void {
    this.changeDetectorRef.markForCheck();
    this.page.state.getValue()?.sync();
  }

  protected getMapping(): Type<BrProps> | undefined {
    const type = this.component.getType();

    if (type && type in this.page.mapping) {
      return this.page.mapping[type];
    }

    return this.page.mapping[TYPE_CONTAINER_ITEM_UNDEFINED as any] ?? BrContainerItemUndefinedComponent;
  }
}
