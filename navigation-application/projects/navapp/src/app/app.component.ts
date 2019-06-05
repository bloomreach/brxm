import { Component, HostBinding, OnInit } from '@angular/core';
import { Observable } from 'rxjs';

import { ClientAppService } from './client-app/services';
import { NavConfigService, OverlayService } from './services';

@Component({
  selector: 'brna-root',
  templateUrl: './app.component.html',
  styleUrls: ['app.component.scss'],
})
export class AppComponent implements OnInit {
  @HostBinding('class.mat-typography')
  typography = true;

  constructor(
    private navConfigService: NavConfigService,
    private clientAppService: ClientAppService,
    private overlayService: OverlayService,
  ) {}

  get isOverlayVisible$(): Observable<boolean> {
    return this.overlayService.visible$;
  }

  ngOnInit(): void {
    this.navConfigService.init();
    this.clientAppService.init();
  }
}
