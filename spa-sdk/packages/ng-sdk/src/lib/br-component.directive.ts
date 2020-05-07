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

import {
  Directive,
  Input,
  OnChanges,
  OnDestroy,
  OnInit,
  Optional,
  SimpleChanges,
  TemplateRef,
  ViewContainerRef,
} from '@angular/core';
import { isComponent, Component, Page } from '@bloomreach/spa-sdk';
import { BrNodeDirective } from './br-node.directive';
import { BrPageComponent } from './br-page/br-page.component';

/**
 * The template context holding the current brXM Component.
 */
export interface BrComponentContext {
  /**
   * The implicit value of the context is pointing to the current component.
   */
  $implicit: Component;

  /**
   * The current component.
   */
  component: Component;

  /**
   * The current page.
   */
  page: Page;
}

/**
 * The brXM component.
 */
@Directive({ selector: '[brComponent]' })
export class BrComponentDirective implements OnChanges, OnDestroy, OnInit {
  /**
   * The component instance or a path to a component.
   * The path is defined as a slash-separated components name chain
   * relative to the current component (e.g. `main/container`).
   * If it is omitted, all the children will be rendered.
   */
  @Input('brComponent') component?: Component | string;

  constructor(
    private container: ViewContainerRef,
    @Optional() private template?: TemplateRef<BrComponentContext>,
    @Optional() private node?: BrNodeDirective,
    @Optional() private page?: BrPageComponent,
  ) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (!changes.component || changes.component.isFirstChange()) {
      return;
    }

    this.ngOnDestroy();
    this.ngOnInit();
  }

  ngOnDestroy(): void {
    this.container.clear();
  }

  ngOnInit(): void {
    const page = this.page?.state.getValue();

    if (!page) {
      return;
    }

    // tslint:disable: no-non-null-assertion
    this.components?.forEach((component) => this.container.createEmbeddedView(this.page!.node, {
      component,
      page,
      $implicit: component,
      template: this.template,
    }));
    // tslint:enable: no-non-null-assertion
  }

  private get context(): Component | undefined {
    if (this.node) {
      return this.node.component;
    }

    return this.page?.state.getValue()?.getComponent();
  }

  private get components(): Component[] | undefined {
    if (isComponent(this.component)) {
      return [this.component];
    }

    if (!this.component) {
      return this.context?.getChildren();
    }

    const component = this.context?.getComponent(...this.component.split('/'));

    return component && [component];
  }
}
