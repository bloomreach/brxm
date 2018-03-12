/*
 * Copyright 2018 Hippo B.V. (http://www.onehippo.com)
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
import 'angular';
import 'angular-mocks';

const MODULE = 'hippo-cm';

function renderDiagram(widthInPixels, renderCallback) {
  dump('@startuml');
  dump(`scale ${widthInPixels} width`);
  renderCallback();
  dump('@enduml');
}

function isInternalModule(name) {
  return name.startsWith(MODULE);
}

function isExternalModule(name) {
  return !isInternalModule(name);
}

function shorten(moduleName) {
  return moduleName.substring(moduleName.lastIndexOf('.') + 1);
}

function sanitize(moduleName) {
  let result = isInternalModule(moduleName) ? shorten(moduleName) : moduleName;
  result = result.replace('-', '_');
  result = result.replace('$', 'dollar_')
  return result;
}

function moduleId(moduleName) {
  return `${sanitize(moduleName)}_module`;
}

const renderedModules = new Set();

function renderModule(name) {
  if (!renderedModules.has(name)) {
    dump(`rectangle ${sanitize(name)} as ${moduleId(name)}`);
    renderedModules.add(name);
  }
}

function renderModuleAsContainer(name, displayName, renderCallback) {
  dump(`rectangle "${displayName}" as ${moduleId(name)} {`);
  renderCallback();
  dump(`}`);
}

function renderTogether(renderCallback) {
  dump('together {');
  renderCallback();
  dump(`}`);
}

function isInternalInjectable(name) {
  const externalPrefixes = [
    '$',
    'BROWSERS',
    'deviceDetector',
    'locals',
    'lowercaseFilter',
  ];
  return !externalPrefixes.some((prefix) => name.startsWith(prefix));
}

const renderedInjectables = new Set();

function renderInjectable(name) {
  if (!renderedInjectables.has(name)) {
    dump(`rectangle ${sanitize(name)}`);
    renderedInjectables.add(name);
  }
}

const moduleArrowDescriptions = {
  'channel > hippoIframe': 'Renders site in',
};

function renderModuleArrow(from, to, arrow = '-->') {
  const fromId = moduleId(from);
  const toId = moduleId(to);

  const arrowDescription = moduleArrowDescriptions[fromId + ' > ' + toId];

  const suffix = arrowDescription ? `: ${arrowDescription}` : '';
  dump(`${fromId} ${arrow} ${toId} ${suffix}`);
}

function renderInjectableArrow(from, to, internal, arrow = '-->') {
  if (!internal) {
    arrow = '~~>';
  }

  dump(`${sanitize(from)} ${arrow} ${sanitize(to)}`);
}

function renderInternalInjectableArrow(from, to, internalInModule, arrow) {
  if (isInternalInjectable(to)) {
    renderInjectableArrow(from, to, internalInModule, arrow);
  }
}

function walkModuleGraph(moduleName, nodeCallback, edgeCallback, walkedNodes = new Set()) {
  const module = angular.module(moduleName);

  module.requires.forEach((dependencyName) => {
    if (!walkedNodes.has(dependencyName)) {
      nodeCallback(dependencyName);
      walkedNodes.add(dependencyName);
    }

    if (edgeCallback(module.name, dependencyName)) {
      walkModuleGraph(dependencyName, nodeCallback, edgeCallback, walkedNodes);
    }
  })
}

function walkInternalModuleGraph(moduleName, internalModuleCallback, internalModuleArrowCallback) {
  walkModuleGraph(moduleName,
    (module) => {
      if (isInternalModule(module)/* && module !== moduleName*/) {
        internalModuleCallback(module);
      }
    },
    (from, to) => {
      if (isExternalModule(to)) {
        return false;
      }
      if (from !== moduleName) {
        internalModuleArrowCallback(from, to);
      }
      return true;
    }
  );

}

function renderExternalModules(moduleName) {
  walkModuleGraph(moduleName,
    (node) => {
      if (isExternalModule(node)) {
        renderModule(node);
      }
    },
    (from, to) => isInternalModule(to),
  );
}

xit('generates a graph of all internal modules', () => {
  renderDiagram(1000, () => {
    const moduleName = MODULE;

    renderModuleAsContainer(moduleName, "Channel Editor", () => {
      walkInternalModuleGraph(moduleName, renderModule, renderModuleArrow);
    });
  });
});


function walkInjectableGraph(moduleName, nodeCallback, edgeCallback) {
  const module = angular.module(moduleName);

  const moduleInjectables = module._invokeQueue.map((provider) => provider[2][0]);

  moduleInjectables.forEach((injectable) => {
    nodeCallback(injectable);
  });

  module._invokeQueue.forEach((provider) => {
    const injectable = provider[2][0];
    const dependencies = provider[2][1].$inject || [];
    dependencies.forEach((dependency) => {
      const internalInModule = moduleInjectables.some((injectable) => injectable === dependency);
      edgeCallback(injectable, dependency, internalInModule);
    });
  });
}

xit('generates a graph of all internal injectables of hippoIframe', () => {
  renderDiagram(1500, () => {
    const moduleName = 'hippo-cm.channel.hippoIframe';

    renderModuleAsContainer(moduleName, shorten(moduleName), () => {
      walkInjectableGraph(moduleName, renderInjectable,
        (from, to, internal) => {
          if (!internal && isInternalInjectable(to)) {
            renderInjectable(to);
          }
        }
      );
      walkInjectableGraph(moduleName,
        angular.noop,
        (from, to, internal) => {
          //if (isInternalInjectable(to)) {
          if (internal) {
            renderInjectableArrow(from, to);
          }
        },
      );
    });
  });
});

xit('generates a graph of all internal modules and their injectables, without any arrows', () => {
  renderDiagram(3000, () => {
    const moduleName = MODULE;

    // render all boxes
    renderModuleAsContainer(moduleName, "Channel Editor", () => {
      walkInjectableGraph(moduleName, renderInjectable, angular.noop);

      walkInternalModuleGraph(moduleName,
        (internalModule) => {
          renderModuleAsContainer(internalModule, shorten(internalModule), () => {
            walkInjectableGraph(internalModule, renderInjectable, angular.noop);
          });
        },
        angular.noop, // no arrows between modules
      )
    });
  });
});

xit('generates a graph of all internal modules and their injectables with arrows between modules', () => {
  renderDiagram(3000, () => {
    const moduleName = MODULE;

    // render all boxes
    renderModuleAsContainer(moduleName, "Channel Editor", () => {
      walkInternalModuleGraph(moduleName,
        (internalModule) => {
          renderModuleAsContainer(internalModule, shorten(internalModule), () => {
            walkInjectableGraph(internalModule,
              (injectable) => {
                renderInjectable(injectable);
              },
              angular.noop, // don't render arrows between injectables yet
            );
          });
        },
        renderModuleArrow,
      )
    });
  });
});

xit('generates a graph of all internal modules and their injectables with arrows between injectables', () => {
  renderDiagram(5000, () => {
    const moduleName = MODULE;

    // render all boxes
    renderModuleAsContainer(moduleName, "Channel Editor", () => {
      walkInjectableGraph(moduleName, renderInjectable, angular.noop);

      walkInternalModuleGraph(moduleName,
        (internalModule) => {
          renderModuleAsContainer(internalModule, shorten(internalModule), () => {
            walkInjectableGraph(internalModule, renderInjectable, angular.noop);
          });
        },
        angular.noop, // no arrows between modules
      )
    });

    // render all arrows
    walkInjectableGraph(moduleName, angular.noop,
      (from, to, internal) => {
        renderInternalInjectableArrow(from, to, internal, '-up->');
      }
    );

    walkInternalModuleGraph(moduleName,
      (internalModule) => {
        walkInjectableGraph(internalModule, angular.noop, renderInternalInjectableArrow);
      },
      angular.noop, // no arrows between modules
    )
  });
});

xit('generates a graph of all internal injectables with arrows between injectables', () => {
  renderDiagram(1000, () => {
    const moduleName = 'hippo-cm.channel.page';

    // render all boxes
    renderModuleAsContainer(moduleName, shorten(moduleName), () => {
      walkInjectableGraph(moduleName, renderInjectable, angular.noop);

      walkInternalModuleGraph(moduleName,
        (internalModule) => {
          walkInjectableGraph(internalModule, renderInjectable, angular.noop);
        },
        angular.noop, // no arrows between modules
      )
    });

    // render all arrows
    walkInjectableGraph(moduleName, angular.noop, renderInternalInjectableArrow);

    walkInternalModuleGraph(moduleName,
      (internalModule) => {
        walkInjectableGraph(internalModule, angular.noop, renderInternalInjectableArrow);
      },
      angular.noop, // no arrows between modules
    )
  });
});

xit('generates a graph of all internal injectables grouped by module with arrows between injectables', () => {
  renderDiagram(5000, () => {
    const moduleName = 'hippo-cm.channel.hippoIframe';

    // render all boxes
    renderModuleAsContainer(moduleName, "Channel Editor", () => {
      walkInjectableGraph(moduleName, renderInjectable, angular.noop);

      walkInternalModuleGraph(moduleName,
        (internalModule) => {
          renderTogether(() => {
            walkInjectableGraph(internalModule, renderInjectable, angular.noop);
          });
        },
        angular.noop, // no arrows between modules
      )
    });

    // render all arrows
    walkInjectableGraph(moduleName, angular.noop, renderInternalInjectableArrow);

    walkInternalModuleGraph(moduleName,
      (internalModule) => {
        walkInjectableGraph(internalModule, angular.noop, renderInternalInjectableArrow);
      },
      angular.noop, // no arrows between modules
    )
  });
});

