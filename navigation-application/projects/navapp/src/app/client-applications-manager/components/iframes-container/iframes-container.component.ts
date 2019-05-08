/*
 * (C) Copyright 2019 Bloomreach. All rights reserved. (https://www.bloomreach.com)
 */

import { Component, ElementRef, OnDestroy, OnInit, Renderer2, RendererFactory2 } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { ClientApplicationsManagerService } from '../../services';

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
    private clientAppsManager: ClientApplicationsManagerService,
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
