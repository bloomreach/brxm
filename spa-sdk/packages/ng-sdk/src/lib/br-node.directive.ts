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

import { Directive, Input, TemplateRef } from '@angular/core';
import { Component, Page } from '@bloomreach/spa-sdk';

/**
 * The template context holding the current brXM Component.
 */
export interface BrNodeContext {
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

@Directive({
  selector: '[brNode]',
})
export class BrNodeDirective {
  @Input('brNode') component!: Component;
  @Input('brNodeTemplate') template?: TemplateRef<BrNodeContext>;
}
