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

import { animate, style, transition, trigger } from '@angular/animations';
import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { OverlayService } from '../../../services/overlay.service';

@Component({
  selector: 'brna-overlay',
  template: '',
  styleUrls: ['overlay.component.scss'],
  animations: [
    trigger('fadeInOut', [
      transition(':enter', [
        style({ opacity: '0' }),
        animate('400ms cubic-bezier(.25, .8, .25, 1)', style({ opacity: '1' })),
      ]),
      transition(':leave', [
        animate('400ms cubic-bezier(.25, .8, .25, 1)', style({ opacity: '0' })),
      ]),
    ]),
  ],
})
export class OverlayComponent implements OnInit, OnDestroy {
  @HostBinding('@fadeInOut')
  visible = false;

  private unsubscribe = new Subject();

  constructor(private overlay: OverlayService) {}

  ngOnInit(): void {
    this.overlay.visible$
      .pipe(takeUntil(this.unsubscribe))
      .subscribe(visible => (this.visible = visible));
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }
}
