/*
 * Copyright 2016 Hippo B.V. (http://www.onehippo.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { ComponentElement } from './element/componentElement';
import { ContainerElement } from './element/containerElement';
import { EmbeddedLink } from './element/embeddedLink';

const EMBEDDED_LINK_MARKUP = '<a class="hst-cmseditlink"></a>';

export class PageStructureService {

  constructor($log,
              $q,
              ChannelService,
              CmsService,
              FeedbackService,
              HippoIframeService,
              hstCommentsProcessorService,
              HstConstants,
              HstService,
              MaskService,
              OverlaySyncService,
              PageMetaDataService,
              RenderingService) {
    'ngInject';

    // Injected
    this.$log = $log;
    this.$q = $q;
    this.ChannelService = ChannelService;
    this.CmsService = CmsService;
    this.FeedbackService = FeedbackService;
    this.HippoIframeService = HippoIframeService;
    this.hstCommentsProcessorService = hstCommentsProcessorService;
    this.HST = HstConstants;
    this.HstService = HstService;
    this.MaskService = MaskService;
    this.OverlaySyncService = OverlaySyncService;
    this.PageMetaDataService = PageMetaDataService;
    this.RenderingService = RenderingService;

    this.clearParsedElements();
  }

  clearParsedElements() {
    this.containers = [];
    this.contentLinks = [];
    this.editMenuLinks = [];
    this.headContributions = [];
    this.PageMetaDataService.clear();
  }

  registerParsedElement(commentDomElement, metaData) {
    switch (metaData[this.HST.TYPE]) {
      case this.HST.TYPE_CONTAINER:
        this._registerContainer(commentDomElement, metaData);
        break;
      case this.HST.TYPE_COMPONENT:
        this._registerComponent(commentDomElement, metaData);
        break;
      case this.HST.TYPE_PAGE:
        this._registerPageMetaData(metaData);
        break;
      case this.HST.TYPE_PROCESSED_HEAD_CONTRIBUTIONS:
      case this.HST.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS:
        this._registerHeadContributions(metaData);
        break;
      default:
        this._registerEmbeddedLink(commentDomElement, metaData);
        break;
    }
  }

  _registerContainer(commentDomElement, metaData) {
    const container = new ContainerElement(commentDomElement, metaData, this.hstCommentsProcessorService);
    this.containers.push(container);
  }

  _registerComponent(commentDomElement, metaData) {
    if (this.containers.length === 0) {
      this.$log.warn('Unable to register component outside of a container context.');
      return;
    }

    const container = this.containers[this.containers.length - 1];
    try {
      const component = new ComponentElement(commentDomElement, metaData, container, this.hstCommentsProcessorService);
      container.addComponent(component);
    } catch (exception) {
      this.$log.debug(exception, metaData);
    }
  }

  _registerPageMetaData(metaData) {
    delete metaData[this.HST.TYPE];
    this.PageMetaDataService.add(metaData);
  }

  _registerHeadContributions(metaData) {
    this.headContributions = this.headContributions.concat(metaData[this.HST.HEAD_ELEMENTS]);
  }

  _registerEmbeddedLink(commentDomElement, metaData) {
    if (metaData[this.HST.TYPE] === this.HST.TYPE_CONTENT_LINK) {
      this.contentLinks.push(new EmbeddedLink(commentDomElement, metaData));
    }

    if (metaData[this.HST.TYPE] === this.HST.TYPE_EDIT_MENU_LINK) {
      this.editMenuLinks.push(new EmbeddedLink(commentDomElement, metaData));
    }
  }

  // Attaching the embedded links to the page structure (by means of the 'enclosingElement') is only
  // done as a final step of processing an entire page or markup fragment, because it requires an
  // up-to-date and complete page structure (containers, components).
  attachEmbeddedLinks() {
    this.contentLinks.forEach((link) => this._attachEmbeddedLink(link));
    this.editMenuLinks.forEach((link) => this._attachEmbeddedLink(link));
  }

  _attachEmbeddedLink(link) {
    let enclosingElement = link.getEnclosingElement();

    if (enclosingElement === undefined) {
      // link is not yet attached, determine enclosing element.
      const commentDomElement = link.getStartComment()[0];
      this.getContainers().some((container) => {
        container.getComponents().some((component) => {
          if (component.containsDomElement(commentDomElement)) {
            enclosingElement = component;
          }
          return enclosingElement;
        });
        if (!enclosingElement && container.containsDomElement(commentDomElement)) {
          enclosingElement = container;
        }
        return enclosingElement;
      });
      if (enclosingElement === undefined) {
        enclosingElement = null; // marks that the *page* is the enclosing element
      }
      link.setEnclosingElement(enclosingElement);

      // insert transparent placeholder into page
      const linkElement = $(EMBEDDED_LINK_MARKUP);
      link.getStartComment().after(linkElement);
      link.setBoxElement(linkElement);
    }
  }

  hasContentLinks() {
    return this.contentLinks.length > 0;
  }

  getContentLinks() {
    return this.contentLinks;
  }

  hasEditMenuLinks() {
    return this.editMenuLinks.length > 0;
  }

  getEditMenuLinks() {
    return this.editMenuLinks;
  }

  getComponentById(id) {
    let component = null;
    this.containers.some((container) => {
      component = container.getComponent(id);
      return component;
    });
    return component;
  }

  hasContainer(container) {
    return this.containers.indexOf(container) !== -1;
  }

  getContainers() {
    return this.containers;
  }

  /**
   * Remove the component identified by given Id
   * @param componentId
   * @return a promise with the object { oldContainer, newContainer }
   */
  removeComponentById(componentId) {
    const component = this.getComponentById(componentId);

    if (component) {
      const oldContainer = component.getContainer();
      return this.HstService.removeHstComponent(oldContainer.getId(), componentId)
        .then(() => {
          this._onAfterRemoveComponent();
          return this.renderContainer(oldContainer).then((newContainer) => { // eslint-disable-line arrow-body-style
            return { oldContainer, newContainer };
          });
        },
        (errorResponse) => {
          const errorKey = errorResponse.error === 'ITEM_ALREADY_LOCKED' ? 'ERROR_DELETE_COMPONENT_ITEM_ALREADY_LOCKED' : 'ERROR_DELETE_COMPONENT';
          const params = errorResponse.parameterMap;
          params.component = component.getLabel();
          return this._showFeedbackAndReload(errorKey, params);
        });
    }
    this.$log.debug(`Could not remove component with ID '${componentId}' because it does not exist in the page structure.`);
    return this.$q.reject();
  }

  _onAfterRemoveComponent() {
    this.ChannelService.recordOwnChange();
  }

  getContainerByIframeElement(containerIFrameElement) {
    return this.containers.find((container) => container.getBoxElement().is(containerIFrameElement));
  }

  showComponentProperties(componentElement) {
    if (!componentElement) {
      this.$log.warn('Problem opening the component properties dialog: no component provided.');
      return;
    }

    this.CmsService.publish('show-component-properties', {
      component: {
        id: componentElement.getId(),
        label: componentElement.getLabel(),
        lastModified: componentElement.getLastModified(),
      },
      container: {
        isDisabled: componentElement.container.isDisabled(),
        isInherited: componentElement.container.isInherited(),
      },
      page: this.PageMetaDataService.get(),
    });
    this.MaskService.add();
  }

  printParsedElements() {
    this.containers.forEach((container, index) => {
      this.$log.debug(`Container ${index}`, container);
      container.items.forEach((component, itemIndex) => {
        this.$log.debug(`  Component ${itemIndex}`, component);
      });
    });
  }

  renderComponent(componentId, propertiesMap) {
    const component = this.getComponentById(componentId);
    if (component) {
      this.RenderingService.fetchComponentMarkup(component, propertiesMap).then((response) => {
        const updatedComponent = this._updateComponent(componentId, response.data);
        if ($.isEmptyObject(propertiesMap) && this.containsNewHeadContributions(updatedComponent)) {
          this.$log.info(`Updated '${updatedComponent.getLabel()}' component needs additional head contributions, reloading page`);
          this.HippoIframeService.reload();
        } else {
          this.OverlaySyncService.syncIframe();
        }
      });
      // TODO handle error
      // show error message that component rendering failed.
      // can we use the toast for this? the component properties dialog is open at this moment...
    } else {
      this.$log.warn(`Cannot render unknown component '${componentId}'`);
    }
  }

  /**
   * Update the component with the new markup
   */
  _updateComponent(componentId, newMarkup) {
    const oldComponent = this.getComponentById(componentId);
    const container = oldComponent.getContainer();

    this._removeEmbeddedLinksInComponent(oldComponent);
    const jQueryNodeCollection = oldComponent.replaceDOM(newMarkup);
    const newComponent = this._createComponent(jQueryNodeCollection, container);

    if (newComponent) {
      container.replaceComponent(oldComponent, newComponent);
    } else {
      container.removeComponent(oldComponent);
    }
    this.attachEmbeddedLinks();

    return newComponent;
  }

  /**
   * Lets the back-end re-render a container.
   * @param container
   * @returns {*} a promise with the new container object
   * @private
   */
  renderContainer(container) {
    return this.RenderingService.fetchContainerMarkup(container)
      .then((markup) => this._updateContainer(container, markup));
    // TODO: handle error
    // try reloading the entire page?
  }

  _updateContainer(oldContainer, newMarkup) {
    this._removeEmbeddedLinksInContainer(oldContainer);

    // consider following three actions to be an atomic operation
    const newContainer = this._replaceContainer(oldContainer, this._createContainer(oldContainer.replaceDOM(newMarkup)));

    this.attachEmbeddedLinks();
    return newContainer;
  }

  containsNewHeadContributions(pageStructureElement) {
    if (!pageStructureElement) {
      return false;
    }
    const elementHeadContributions = pageStructureElement.getHeadContributions();
    return elementHeadContributions.some((contribution) => !this.headContributions.includes(contribution));
  }

  _removeEmbeddedLinksInContainer(container) {
    container.getComponents().forEach((component) => this._removeEmbeddedLinksInComponent(component));

    this.contentLinks = this._getLinksNotEnclosedInElement(this.contentLinks, container);
    this.editMenuLinks = this._getLinksNotEnclosedInElement(this.editMenuLinks, container);
  }

  _removeEmbeddedLinksInComponent(component) {
    this.contentLinks = this._getLinksNotEnclosedInElement(this.contentLinks, component);
    this.editMenuLinks = this._getLinksNotEnclosedInElement(this.editMenuLinks, component);
  }

  _getLinksNotEnclosedInElement(links, element) {
    const remainingContentLinks = [];
    links.forEach((link) => {
      if (link.getEnclosingElement() !== element) {
        remainingContentLinks.push(link);
      }
    });
    return remainingContentLinks;
  }

  addComponentToContainer(catalogComponent, container) {
    return this.HstService.addHstComponent(catalogComponent, container.getId())
      .then(
        (newComponentJson) => {
          this.ChannelService.recordOwnChange();
          // TODO: handle error when rendering container failed
          return this.renderContainer(container).then(() => this.getComponentById(newComponentJson.id));
        },
        (errorResponse) => {
          const errorKey = errorResponse.error === 'ITEM_ALREADY_LOCKED' ? 'ERROR_ADD_COMPONENT_ITEM_ALREADY_LOCKED' : 'ERROR_ADD_COMPONENT';
          const params = errorResponse.parameterMap;
          params.component = catalogComponent.name;
          return this._showFeedbackAndReload(errorKey, params);
        });
  }

  getContainerByOverlayElement(overlayElement) {
    return this.containers.find((container) => {
      const containerOverlay = container.getOverlayElement();
      return containerOverlay && (containerOverlay[0] === overlayElement);
    });
  }

  _replaceContainer(oldContainer, newContainer) {
    const index = this.containers.indexOf(oldContainer);
    if (index === -1) {
      this.$log.warn('Cannot find container', oldContainer);
      return null;
    }
    if (newContainer) {
      this.containers[index] = newContainer;
    } else {
      this.containers.splice(index, 1);
    }
    return newContainer;
  }

  /**
   * Create a new container with meta-data from the given markup value
   * @param markup
   * @returns {*}
   * @private
   */
  _createContainer(jQueryNodeCollection) {
    let container = null;

    this.hstCommentsProcessorService.processFragment(jQueryNodeCollection, (commentDomElement, metaData) => {
      try {
        switch (metaData[this.HST.TYPE]) {
          case this.HST.TYPE_CONTAINER:
            if (!container) {
              container = new ContainerElement(commentDomElement, metaData, this.hstCommentsProcessorService);
            } else {
              this.$log.warn('More than one container in the DOM Element!');
              return;
            }
            break;

          case this.HST.TYPE_COMPONENT:
            if (!container) {
              this.$log.warn('Unable to register component outside of a container context.');
              return;
            }

            try {
              container.addComponent(new ComponentElement(commentDomElement, metaData,
                container, this.hstCommentsProcessorService));
            } catch (exception) {
              this.$log.debug(exception, metaData);
            }
            break;

          case this.HST.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS:
            if (container) {
              const unprocessedElements = metaData[this.HST.HEAD_ELEMENTS];
              container.setHeadContributions(unprocessedElements);
            }
            break;

          default:
            this._registerEmbeddedLink(commentDomElement, metaData);
            break;
        }
      } catch (exception) {
        this.$log.debug(exception, metaData);
      }
    });

    if (!container) {
      this.$log.error('Failed to create a new container');
    }

    return container;
  }

  /**
   * Create a new component with meta-data from the given markup value
   */
  _createComponent(jQueryNodeCollection, container) {
    let component = null;

    this.hstCommentsProcessorService.processFragment(jQueryNodeCollection, (commentDomElement, metaData) => {
      switch (metaData[this.HST.TYPE]) {
        case this.HST.TYPE_COMPONENT:
          try {
            component = new ComponentElement(commentDomElement, metaData, container, this.hstCommentsProcessorService);
          } catch (exception) {
            this.$log.debug(exception, metaData);
          }
          break;

        case this.HST.TYPE_UNPROCESSED_HEAD_CONTRIBUTIONS:
          if (component) {
            const unprocessedElements = metaData[this.HST.HEAD_ELEMENTS];
            component.setHeadContributions(unprocessedElements);
          }
          break;

        default:
          this._registerEmbeddedLink(commentDomElement, metaData);
          break;
      }
    });

    if (!component) {
      this.$log.error('Failed to create a new component');
    }

    return component;
  }

  _showFeedbackAndReload(errorKey, params) {
    this.FeedbackService.showError(errorKey, params);
    return this.HippoIframeService.reload().then(() => this.$q.reject());
  }

  reloadChannel(errorResponse) {
    let errorKey;
    switch (errorResponse.error) {
      case 'ITEM_ALREADY_LOCKED':
        errorKey = 'ERROR_UPDATE_COMPONENT_ITEM_ALREADY_LOCKED';
        break;
      case 'ITEM_NOT_FOUND':
        errorKey = 'ERROR_COMPONENT_DELETED';
        break;
      default:
        errorKey = 'ERROR_UPDATE_COMPONENT';
    }

    this._showFeedbackAndReload(errorKey, errorResponse.parameterMap);
  }
}
