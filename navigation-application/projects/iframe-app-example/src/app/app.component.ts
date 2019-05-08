import { Component, OnInit } from '@angular/core';
import { connectToParent } from '@bloomreach/navapp-communication';

@Component({
  selector: 'app-root',
  template: `
    <h1>Number of times navigated {{ navigateCount }}</h1>
  `,
})
export class AppComponent implements OnInit {
  navigateCount = 0;

  ngOnInit(): void {
    connectToParent({
      parentOrigin: 'http://localhost:4200',
      methods: {
        navigate: () => {
          this.navigateCount += 1;
        },
      },
    }).then(() => {
      console.log('connected to parent');
    });
  }
}
