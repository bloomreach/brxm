import { animate, style, transition, trigger } from '@angular/animations';
import { Component, HostBinding, OnDestroy, OnInit } from '@angular/core';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

import { OverlayService } from '../../../services';

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

  constructor(
    private overlay: OverlayService,
  ) {}

  ngOnInit(): void {
    this.overlay.visible$.pipe(
      takeUntil(this.unsubscribe),
    ).subscribe(visible => this.visible = visible);
  }

  ngOnDestroy(): void {
    this.unsubscribe.next();
    this.unsubscribe.complete();
  }
}
