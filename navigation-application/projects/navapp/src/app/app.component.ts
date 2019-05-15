import { Component } from '@angular/core';

import { MenuStateService } from './left-menu/services';

@Component({
  selector: 'brna-root',
  templateUrl: './app.component.html',
  styleUrls: ['app.component.scss'],
})
export class AppComponent {
  constructor(private menuStateService: MenuStateService) {}

  get isMenuCollapsed(): boolean {
    return this.menuStateService.isMenuCollapsed;
  }
}
