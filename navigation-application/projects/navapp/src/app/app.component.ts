import { Component, HostBinding, OnInit } from '@angular/core';

import { NavConfigService } from './services';

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
    private menuStateService: MenuStateService,
    private navConfigService: NavConfigService,
  ) {}

  get isMenuCollapsed(): boolean {
    return this.menuStateService.isMenuCollapsed;
  }

  ngOnInit(): void {
    this.navConfigService.init();
  }
}
