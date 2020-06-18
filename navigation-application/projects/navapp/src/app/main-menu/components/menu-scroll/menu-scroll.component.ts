/*
 * Copyright 2020 BloomReach. All rights reserved. (https://www.bloomreach.com/)
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

import { animate, state, style, transition, trigger } from '@angular/animations';
import { AfterViewInit, Component, ElementRef, HostBinding, HostListener, Input, ViewChild } from '@angular/core';

import { normalizeWheelEvent } from '../../../helpers/normalize-wheel-event';

enum ScrollButtonAnimationState {
  Animate = 'fast',
  AnimateWithDelay = 'delayed',
}

@Component({
  selector: 'brna-menu-scroll',
  templateUrl: 'menu-scroll.component.html',
  styleUrls: ['menu-scroll.component.scss'],
  animations: [
    trigger('animateScrollUpButton', [
      state('void', style({ opacity: 0, transform: 'translateY(-100%)'})),
      state('fast,delayed', style({ opacity: 1, transform: 'translateY(0)'})),
      transition(':enter', animate('500ms ease-out')),
      transition('fast => void', animate('500ms ease-out')),
      transition('delayed => void', animate('500ms 500ms ease-out')),
    ]),
    trigger('animateScrollDownButton', [
      state('void', style({ opacity: 0, transform: 'translateY(100%)'})),
      state('fast,delayed', style({ opacity: 1, transform: 'translateY(0)'})),
      transition(':enter', animate('500ms ease-out')),
      transition('fast => void', animate('500ms ease-out')),
      transition('delayed => void', animate('500ms 500ms ease-out')),
    ]),
  ],
})
export class MenuScrollComponent implements AfterViewInit {
  @Input()
  set height(value: number) {
    this.animationState = ScrollButtonAnimationState.Animate;

    this.autoScrollOnResize(value - this.availableHeight);

    this.availableHeight = value;
  }

  @ViewChild('content', { static: false })
  readonly content: ElementRef<HTMLElement>;

  @HostBinding('class')
  transitionClass = '';

  @HostBinding('style.height.px')
  availableHeight = 0;

  animationState: ScrollButtonAnimationState;

  private readonly occupiedHeight = 64; // The height occupied by arrow buttons

  private offsetTop = 0;
  private cachedContentHeight = 0;
  private updateContentHeightInterval: any;

  get isScrollableUp(): boolean {
    return this.position > 0;
  }

  get isScrollableDown(): boolean {
    return this.maxPosition > 0 && this.position < this.maxPosition;
  }

  get position(): number {
    return this.offsetTop;
  }

  set position(value: number) {
    this.offsetTop = Math.min(Math.max(0, value), Math.max(0, this.maxPosition));
  }

  private get contentHeight(): number {
    return this.cachedContentHeight;
  }

  private get isScrollable(): boolean {
    return this.isScrollableUp || this.isScrollableDown;
  }

  private get maxPosition(): number {
    return this.contentHeight - this.availableHeight;
  }

  private get step(): number {
    const availableHeight = Math.max(0, this.availableHeight - this.occupiedHeight);

    return Math.min(availableHeight, this.contentHeight - availableHeight);
  }

  ngAfterViewInit(): void {
    setTimeout(() => this.updateContentHeight());
  }

  onScrollUpButtonClick(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

    this.animationState = ScrollButtonAnimationState.AnimateWithDelay;
    this.transitionClass = 'click-transition';
    this.position = this.position - this.step;
  }

  onScrollDownButtonClick(event: MouseEvent): void {
    event.preventDefault();
    event.stopPropagation();

    this.animationState = ScrollButtonAnimationState.AnimateWithDelay;
    this.transitionClass = 'click-transition';
    this.position = this.position + this.step;
  }

  @HostListener('wheel', ['$event'])
  onWheel(event: WheelEvent): void {
    event.preventDefault();

    if (!this.isScrollable) {
      return;
    }

    this.animationState = ScrollButtonAnimationState.Animate;
    const normalized = normalizeWheelEvent(event);
    this.transitionClass = normalized.wheel ? 'wheel-transition' : 'no-transition';
    this.position = this.position + normalized.y;
  }

  updateContentHeight(): void {
    const firstUpdate = this.cachedContentHeight === 0;

    const updateHeight = () => {
      const newHeight = this.content.nativeElement.offsetHeight;
      const addedSpace = this.cachedContentHeight - newHeight;

      this.autoScrollOnResize(addedSpace);

      this.cachedContentHeight = newHeight;

      return addedSpace === 0;
    };

    updateHeight();

    if (firstUpdate) {
      return;
    }

    if (this.updateContentHeightInterval) {
      clearInterval(this.updateContentHeightInterval);
    }

    this.updateContentHeightInterval = setInterval(() => {
      if (!updateHeight()) {
        return;
      }

      clearInterval(this.updateContentHeightInterval);
    }, 50);
  }

  private autoScrollOnResize(addedSpace: number): void {
    if (this.position < 0 || addedSpace < 0) {
      return;
    }

    this.transitionClass = 'resize-transition';
    this.position -= addedSpace;
  }
}
