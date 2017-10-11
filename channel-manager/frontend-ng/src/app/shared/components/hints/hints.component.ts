import { Component, Input, ElementRef, OnInit } from '@angular/core';
import { Hint } from './hints';

@Component({
  selector: 'hippo-hints',
  templateUrl: './hints.html'
})
export class HintsComponent implements OnInit {
  @Input() data: Object;

  private el: HTMLElement;
  public hints: Array<Hint> = [];

  constructor(private elementRef: ElementRef) {}

  ngOnInit() {
    this.el = this.elementRef.nativeElement;
    const children: NodeListOf<Element> = this.el.querySelectorAll('*');

    this.hints = [...children].map((child: Element) => {
      const hint = {
        key: child.getAttribute('hint'),
        content: child.innerHTML,
        classList: child.className ? child.className.split(/\s+/) : [],
      };

      child.remove();
      return hint;
    });
  }
}
