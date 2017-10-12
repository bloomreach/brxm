/*
 * Copyright 2015-2017 Hippo B.V. (http://www.onehippo.com)
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

import { Component, Input, ElementRef, OnInit } from '@angular/core';
import { Hint } from './hints';

@Component({
  selector: 'hippo-hints',
  templateUrl: './hints.html'
})
export class HintsComponent implements OnInit {
  @Input() data: Object;

  private el: HTMLElement;
  public hints: Array<Hint> = [];

  constructor(private elementRef: ElementRef) {}

  ngOnInit() {
    this.el = this.elementRef.nativeElement;
    const children: NodeListOf<Element> = this.el.querySelectorAll('*');

    this.hints = [...children].map((child: Element) => {
      const hint = {
        key: child.getAttribute('hint'),
        content: child.innerHTML,
        classList: child.className ? child.className.split(/\s+/) : [],
      };

      child.remove();
      return hint;
    });
  }
}
