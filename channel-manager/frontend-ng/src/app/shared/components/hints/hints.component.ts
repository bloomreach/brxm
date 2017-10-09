import { Component, Input, ElementRef, OnInit } from '@angular/core';
import { Hint } from './hints';

@Component({
  selector: 'hints',
  templateUrl: './hints.html'
})
export class HintsComponent implements OnInit {
  @Input() data: Object;

  private el: HTMLElement;
  public hints: Array<Hint> = [];

  constructor(private elementRef: ElementRef) {}

  ngOnInit(): void {
    this.el = this.elementRef.nativeElement;
    const children: NodeListOf<Element> = this.el.querySelectorAll('*');

    for (let i = 0; i < children.length; i++) {
      const child: Element = children[i];

      this.hints.push({
        key: child.getAttribute('hint'),
        text: child.innerHTML,
        classList: child.className ? child.className.split('/\\s+/') : [],
      });

      // Remove original elements from the DOM to prevent an overhead.
      // The hints will be represented as new elements in the template, with *ngIf.
      child.remove();
    }
  }
}
