import { Component, OnInit } from '@angular/core';
import { connectToParent, NavLocation, ParentConnectConfig } from '@bloomreach/navapp-communication';

import { navigationConfiguration } from './mocks';

@Component({
  selector: 'app-root',
  template: `
    <h1>Number of times navigated {{ navigateCount }}</h1>
    <h2>It was navigated to "{{ navigatedTo }}"</h2>
    <h3>The button was clicked {{ buttonClicked }} times.</h3>
    <button (click)="onButtonClicked()">Button</button>
  `,
})
export class AppComponent implements OnInit {
  navigateCount = 0;
  navigatedTo: string;
  buttonClicked = 0;

  ngOnInit(): void {
    if (window.parent === window) {
      console.log('Iframe app was not loaded inside iframe');
      return;
    }

    const config: ParentConnectConfig = {
      parentOrigin: 'http://localhost:4200',
      methods: {
        navigate: (location: NavLocation) => {
          this.navigateCount += 1;
          this.navigatedTo = location.path;
        },
        getNavItems: () => {
          return navigationConfiguration;
        },
      },
    };

    connectToParent(config);
  }

  onButtonClicked(): void {
    this.buttonClicked++;
  }
}
