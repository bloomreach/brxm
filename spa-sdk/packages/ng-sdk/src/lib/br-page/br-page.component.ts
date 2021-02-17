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
  AfterContentChecked,
  ChangeDetectorRef,
  ChangeDetectionStrategy,
  Component,
  ContentChild,
  Inject,
  Input,
  NgZone,
  OnChanges,
  OnDestroy,
  Output,
  Optional,
  PLATFORM_ID,
  SimpleChanges,
  TemplateRef,
  Type,
  ViewChild,
} from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { isPlatformBrowser, isPlatformServer } from '@angular/common';
import { makeStateKey, StateKey, TransferState } from '@angular/platform-browser';
import { from, of, BehaviorSubject, Subject } from 'rxjs';
import { filter, map, mapTo, pairwise, pluck, switchMap, take } from 'rxjs/operators';
import { destroy, initialize, isPage, Configuration, Page, PageModel } from '@bloomreach/spa-sdk';
import { BrComponentContext } from '../br-component.directive';
import { BrProps } from '../br-props.model';

interface BrNodeContext extends BrComponentContext {
  template?: TemplateRef<BrComponentContext>;
}

/**
 * The brXM page.
 */
@Component({
  changeDetection: ChangeDetectionStrategy.OnPush,
  selector: 'br-page',
  templateUrl: './br-page.component.html',
})
export class BrPageComponent implements AfterContentChecked, OnChanges, OnDestroy {
  /**
   * The configuration of the SPA SDK.
   * @see https://www.npmjs.com/package/@bloomreach/spa-sdk#configuration
   */
  @Input() configuration!: Omit<Configuration, 'httpClient'>;

  /**
   * The brXM and Angular components mapping.
   */
  @Input() mapping: Record<keyof any, Type<BrProps>> = {};

  /**
   * The pre-initialized page instance or prefetched page model.
   * Mostly this property should be used to transfer state from the server-side to the client-side.
   */
  @Input() page?: Page | PageModel;

  /**
   * The TransferState key is used to transfer the state from the server-side to the client-side.
   * By default, it equals to `brPage`.
   * If `false` is passed then the state transferring feature will be disabled.
   */
  @Input() stateKey: StateKey<PageModel | undefined> | false = makeStateKey('brPage');

  /**
   * The current state of the page component.
   */
  @Output() state = new BehaviorSubject<Page | undefined>(undefined);

  @ViewChild('brNode', { static: true }) node!: TemplateRef<BrNodeContext>;

  @ContentChild(TemplateRef, { static: true }) private template?: TemplateRef<BrComponentContext>;

  private afterContentChecked$ = new Subject();

  constructor(
    private changeDetectorRef: ChangeDetectorRef,
    private httpClient: HttpClient,
    zone: NgZone,
    @Inject(PLATFORM_ID) private platform: any,
    @Optional() private transferState?: TransferState,
  ) {
    this.request = this.request.bind(this);

    this.state.pipe(
      pairwise(),
      pluck(0),
      filter(isPage),
    )
    .subscribe(destroy);

    this.state.pipe(
      filter(isPage),
      switchMap((page) => this.afterContentChecked$.pipe(take(1), mapTo(page))),
    )
    .subscribe((page) => zone.runOutsideAngular(() => page.sync()));

    this.state.pipe(
      filter(() => isPlatformServer(this.platform)),
      filter(isPage),
    )
    .subscribe((page) => this.stateKey && this.transferState?.set(this.stateKey, page.toJSON()));
  }

  get context(): BrNodeContext | undefined {
    const page = this.state.getValue();
    const component = page?.getComponent();

    if (!page || !component) {
      return;
    }

    return {
      component,
      page,
      $implicit: component,
      template: this.template,
    };
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes.configuration || changes.page) {
      this.initialize(changes.page?.currentValue);
    }

    if (changes.stateKey?.previousValue && isPlatformServer(this.platform)) {
      if (changes.stateKey.currentValue && this.transferState?.hasKey(changes.stateKey.previousValue)) {
        this.transferState?.set(
          changes.stateKey.currentValue,
          this.transferState?.get(changes.stateKey.previousValue, undefined),
        );
      }

      this.transferState?.remove(changes.stateKey.previousValue);
    }
  }

  ngAfterContentChecked(): void {
    this.afterContentChecked$.next();
  }

  ngOnDestroy(): void {
    this.state.next(undefined);
    this.state.complete();
    this.afterContentChecked$.complete();
  }

  private initialize(page: Page | PageModel | undefined): void {
    if (this.stateKey && isPlatformBrowser(this.platform) && this.transferState?.hasKey(this.stateKey)) {
      page = page ?? this.transferState?.get(this.stateKey, undefined);
      this.transferState?.remove(this.stateKey);
    }

    const configuration = { httpClient: this.request, ...this.configuration } as Configuration;
    const observable = page
      ? of(initialize(configuration, page))
      : from(initialize(configuration));

    observable.subscribe(state => {
      this.state.next(state);
      this.changeDetectorRef.detectChanges();
    });
  }

  private request(...[{ data: body, headers, method, url }]: Parameters<Configuration['httpClient']>) {
    return this.httpClient.request<PageModel>(
      method,
      url,
      {
        body,
        headers: headers as Record<string, string | string[]>,
        responseType: 'json'
      },
    )
    .pipe(map(data => ({data})))
    .toPromise();
  }
}
