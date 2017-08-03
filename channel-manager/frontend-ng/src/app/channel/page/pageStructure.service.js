/*
 * Copyright 2016-2017 Hippo B.V. (http://www.onehippo.com)
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

import ComponentElement from './element/componentElement';
import ContainerElement from './element/containerElement';
import ContentLink from './element/contentLink';
import MenuLink from './element/menuLink';

class PageStructureService {

  constructor(
    $log,
    $q,
    ChannelService,
    CmsService,
    FeedbackService,
    HippoIframeService,
    hstCommentsProcessorService,
    HstConstants,
    HstService,
    MaskService,
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
    this.PageMetaDataService = PageMetaDataService;
    this.RenderingService = RenderingService;

    this.changeListeners = [];
    this.CmsService.subscribe('hide-component-properties', () => this.MaskService.unmask());
    this.clearParsedElements();
  }

  clearParsedElements() {
    this.containers = [];
    this.embeddedLinks = [];
    this.headContributions = [];
    this.PageMetaDataService.clear();
    this._notifyChangeListeners();
  }

  registerParsedElement(commentDomElement, metaData) {
    const type = metaData[this.HST.TYPE];
    switch (type) {
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
      case this.HST.TYPE_CONTENT_LINK:
        this._registerContentLink(commentDomElement, metaData);
        break;
      case this.HST.TYPE_EDIT_MENU_LINK:
        this._registerMenuLink(commentDomElement, metaData);
        break;
      default:
        this.$log.warn(`Ignoring unknown page structure element '${type}'`);
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

  _registerContentLink(commentDomElement, metaData) {
    this.embeddedLinks.push(new ContentLink(commentDomElement, metaData));
  }

  _registerMenuLink(commentDomElement, metaData) {
    this.embeddedLinks.push(new MenuLink(commentDomElement, metaData));
  }

  registerChangeListener(callback) {
    this.changeListeners.push(callback);
  }

  _notifyChangeListeners() {
    this.changeListeners.forEach((callback) => {
      callback();
    });
  }

  // Attaching the embedded links to the page structure (by means of the 'enclosingElement') is only
  // done as a final step of processing an entire page or markup fragment, because it requires an
  // up-to-date and complete page structure (containers, components).
  attachEmbeddedLinks() {
    this.embeddedLinks.forEach(link => this._attachEmbeddedLink(link));
    this._notifyChangeListeners();
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
      link.prepareBoxElement();
    }
  }

  getEmbeddedLinks() {
    return this.embeddedLinks;
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
            this._notifyChangeListeners();
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

  getComponentByOverlayElement(componentOverlayElement) {
    let component;
    this.containers.some((container) => {
      component = container.getComponents().find(c => c.getOverlayElement().is(componentOverlayElement));
      return !!component;
    });
    return component;
  }

  getContainerByIframeElement(containerIFrameElement) {
    return this.containers.find(container => container.getBoxElement().is(containerIFrameElement));
  }

  showComponentProperties(componentElement) {
    if (!componentElement) {
      this.$log.warn('Problem opening the component properties dialog: no component provided.');
      return;
    }

    this.MaskService.mask();

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
    let component = this.getComponentById(componentId);
    if (component) {
      this.RenderingService.fetchComponentMarkup(component, propertiesMap).then((response) => {
        // re-fetch component because a parallel renderComponent call may have updated the component's markup
        component = this.getComponentById(componentId);

        const newMarkup = response.data;
        const updatedComponent = this._updateComponent(component, newMarkup);

        if ($.isEmptyObject(propertiesMap) && this.containsNewHeadContributions(updatedComponent)) {
          this.$log.info(`Updated '${updatedComponent.getLabel()}' component needs additional head contributions, reloading page`);
          this.HippoIframeService.reload();
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
  _updateComponent(oldComponent, newMarkup) {
    const $newMarkup = $(newMarkup);
    const container = oldComponent.getContainer();

    this._removeEmbeddedLinksInComponent(oldComponent);

    oldComponent.replaceDOM($newMarkup, () => {
      this._notifyChangeListeners();
    });

    const newComponent = this._createComponent($newMarkup, container);

    if (newComponent) {
      container.replaceComponent(oldComponent, newComponent);
      // reuse the overlay element to reduce DOM manipulation and improve performance
      newComponent.setOverlayElement(oldComponent.getOverlayElement());
    } else {
      container.removeComponent(oldComponent);
    }
    this.attachEmbeddedLinks();

    return newComponent;
  }

  renderContainer(container) {
    return this.RenderingService.fetchContainerMarkup(container)
      .then(markup => this._updateContainer(container, markup));
  }

  _updateContainer(oldContainer, newMarkup) {
    this._removeEmbeddedLinksInContainer(oldContainer);
    const $newMarkup = $(newMarkup);

    oldContainer.replaceDOM($newMarkup, () => {
      this._notifyChangeListeners();
    });

    const container = this._createContainer($newMarkup);
    const newContainer = this._replaceContainer(oldContainer, container);

    this.attachEmbeddedLinks();
    return newContainer;
  }

  containsNewHeadContributions(pageStructureElement) {
    if (!pageStructureElement) {
      return false;
    }
    const elementHeadContributions = pageStructureElement.getHeadContributions();
    return elementHeadContributions.some(contribution => !this.headContributions.includes(contribution));
  }

  _removeEmbeddedLinksInContainer(container) {
    container.getComponents().forEach(component => this._removeEmbeddedLinksInComponent(component));

    this.embeddedLinks = this._getLinksNotEnclosedInElement(this.embeddedLinks, container);
  }

  _removeEmbeddedLinksInComponent(component) {
    this.embeddedLinks = this._getLinksNotEnclosedInElement(this.embeddedLinks, component);
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
      // reuse the overlay element to reduce DOM manipulation and improve performance
      newContainer.setOverlayElement(oldContainer.getOverlayElement());
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
      const type = metaData[this.HST.TYPE];
      try {
        switch (type) {
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

          case this.HST.TYPE_CONTENT_LINK:
            this._registerContentLink(commentDomElement, metaData);
            break;

          case this.HST.TYPE_EDIT_MENU_LINK:
            this._registerMenuLink(commentDomElement, metaData);
            break;

          default:
            this.$log.warn(`Ignoring unknown page structure element '${type}'`);
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
      const type = metaData[this.HST.TYPE];
      switch (type) {
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

        case this.HST.TYPE_CONTENT_LINK:
          this._registerContentLink(commentDomElement, metaData);
          break;

        case this.HST.TYPE_EDIT_MENU_LINK:
          this._registerMenuLink(commentDomElement, metaData);
          break;

        default:
          this.$log.warn(`Ignoring unknown page structure element '${type}'`);
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

export default PageStructureService;
