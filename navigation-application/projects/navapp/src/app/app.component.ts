import { Component, HostBinding, OnInit } from '@angular/core';

import { NavConfigService } from './services';

import { ClientAppService } from './client-app/services';
import { MenuStateService } from './main-menu/services';

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
  ) {}

  ngOnInit(): void {
    this.navConfigService.init();
    this.clientAppService.init();
  }
}
