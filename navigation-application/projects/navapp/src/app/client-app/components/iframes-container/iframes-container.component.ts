/*
 * Copyright 2019 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { Component, ElementRef, OnDestroy, OnInit, Renderer2, RendererFactory2 } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { ClientAppService } from '../../services';

@Component({
  selector: 'brna-iframes-container',
  template: '',
  styleUrls: ['iframes-container.component.scss'],
})
export class IframesContainerComponent implements OnInit, OnDestroy {
  private renderer: Renderer2;
  private unsubscribe = new Subject();

  constructor(
    private elRef: ElementRef,
    private clientAppsManager: ClientAppService,
    private rendererFactory: RendererFactory2,
  ) {}

  ngOnInit(): void {
    this.renderer = this.rendererFactory.createRenderer(undefined, undefined);

    this.clientAppsManager.applicationCreated$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(app => {
      this.renderer.appendChild(this.elRef.nativeElement, app.iframeEl);
    });
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }
}
