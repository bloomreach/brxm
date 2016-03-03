/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 */

/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 */

function makePlaceholder(text) {
  return '<div class="richtextsnippet-component"><div class="container"><div class="row"><div><h2>['
    + text + ']</h2></div></div></div></div>';
}

export class ComponentsService {
  constructor() {
    this.components = [
      {
        name: 'Banner',
        icon: 'site/images/components/banner.png',
        placeholder: makePlaceholder('Banner Component'),
      },
      {
        name: 'Banner Collection',
        icon: 'site/images/components/banner-collection.png',
        placeholder: makePlaceholder('Banner Collection Component'),
      },
      {
        name: 'Carousel',
        icon: 'site/images/components/carousel.png',
        placeholder: makePlaceholder('Carousel Component'),
      },
      {
        name: 'Events',
        icon: 'site/images/components/events.png',
        placeholder: makePlaceholder('Events Component'),
      },
      {
        name: 'Map',
        icon: 'site/images/components/map.png',
        placeholder: makePlaceholder('Map Component'),
      },
      {
        name: 'Rich Text Snippet',
        icon: 'site/images/components/richtextsnippet.png',
        placeholder: makePlaceholder('Rich Text Snippet Component'),
      },
    ];
  }

  setCatalogElement(component, element) {
    component.element = element;
  }

  getComponentByElement(element) {
    return this.components.find((component) => component.element === element);
  }

}
