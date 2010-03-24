package org.hippoecm.repository.jackrabbit;

import javax.jcr.NamespaceException;

import org.apache.jackrabbit.spi.Name;
import org.apache.jackrabbit.spi.Path;
import org.apache.jackrabbit.spi.commons.conversion.CachingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.IllegalNameException;
import org.apache.jackrabbit.spi.commons.conversion.MalformedPathException;
import org.apache.jackrabbit.spi.commons.conversion.NameParser;
import org.apache.jackrabbit.spi.commons.conversion.NamePathResolver;
import org.apache.jackrabbit.spi.commons.conversion.NameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingNameResolver;
import org.apache.jackrabbit.spi.commons.conversion.ParsingPathResolver;
import org.apache.jackrabbit.spi.commons.conversion.PathResolver;
import org.apache.jackrabbit.spi.commons.name.NameFactoryImpl;
import org.apache.jackrabbit.spi.commons.name.PathFactoryImpl;
import org.apache.jackrabbit.spi.commons.namespace.NamespaceResolver;

public class HippoNamePathResolver implements NamePathResolver {

    private final NameResolver nResolver;

    private final PathResolver pResolver;

    public HippoNamePathResolver(NamespaceResolver nsResolver, boolean enableCaching) {
        NameResolver nr = new ParsingNameResolver(NameFactoryImpl.getInstance(), nsResolver);
        PathResolver pr = new ParsingPathResolver(PathFactoryImpl.getInstance(), nr);
        if (enableCaching) {
            this.nResolver = new CachingNameResolver(nr);
            this.pResolver = new HippoCachingPathResolver(pr, nr);
        } else {
            this.nResolver = nr;
            this.pResolver = pr;
        }
    }

    public Name getQName(String name) throws IllegalNameException, NamespaceException {
        return nResolver.getQName(name);
    }

    public String getJCRName(Name name) throws NamespaceException {
        return nResolver.getJCRName(name);
    }

    public Path getQPath(String path) throws MalformedPathException, IllegalNameException, NamespaceException {
        return pResolver.getQPath(path);
    }

    public String getJCRPath(Path path) throws NamespaceException {
        return pResolver.getJCRPath(path);
    }

    private class NameResolverImpl implements NameResolver {
        NamespaceResolver resolver;

        NameResolverImpl(NamespaceResolver nsResolver) {
            this.resolver = nsResolver;
        }

        public Name getQName(String name) throws IllegalNameException, NamespaceException {
            return NameParser.parse(name, resolver, NameFactoryImpl.getInstance());
        }

        public String getJCRName(Name name) throws NamespaceException {
            String uri = name.getNamespaceURI();
            if (resolver.getPrefix(uri).length() == 0) {
                return name.getLocalName();
            } else {
                return resolver.getPrefix(uri) + ":" + name.getLocalName();
            }
        }
    }
}
