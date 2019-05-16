import { Component, OnInit } from '@angular/core';
import { connectToParent, ParentConnectConfig } from '@bloomreach/navapp-communication';

import { navigationConfiguration } from './mocks';

@Component({
  selector: 'app-root',
  template: `
    <h1>Number of times navigated {{ navigateCount }}</h1>
  `,
})
export class AppComponent implements OnInit {
  navigateCount = 0;

  ngOnInit(): void {
    if (window.parent === window) {
      console.log('Iframe app was not loaded inside iframe');
      return;
    }

    const config: ParentConnectConfig = {
      parentOrigin: 'http://localhost:4200',
      methods: {
        navigate: () => {
          this.navigateCount += 1;
        },
        getNavItems: () => {
          return navigationConfiguration;
        },
      },
    };

    connectToParent(config);
  }
}
