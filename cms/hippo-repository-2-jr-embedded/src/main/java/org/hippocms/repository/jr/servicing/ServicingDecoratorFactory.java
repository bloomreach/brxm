package org.hippocms.repository.jr.servicing;

import java.util.WeakHashMap;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.ValueFactory;
import javax.jcr.ItemVisitor;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.QueryManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.lock.Lock;

public class ServicingDecoratorFactory
  implements DecoratorFactory
{
    protected WeakHashMap<Repository,RepositoryDecorator> repositoryDecorators;
    protected WeakHashMap<Session,SessionDecorator> sessionDecorators;
    protected WeakHashMap<Workspace,ServicingWorkspaceImpl> workspaceDecorators;
    //protected WeakHashMap<Node,ServicingNodeImpl> nodeDecorators;
    //protected WeakHashMap<Property,PropertyDecorator> propertyDecorators;
    //protected WeakHashMap<Lock,LockDecorator> lockDecorators;
    //protected WeakHashMap<Version,VersionDecorator> versionDecorators;
    //protected WeakHashMap<VersionHistory,VersionHistoryDecorator> versionHistoryDecorators;
    //protected WeakHashMap<Item,ItemDecorator> itemDecorators;
    //protected WeakHashMap<QueryManager,QueryManagerDecorator> queryManagerDecorators;
    //protected WeakHashMap<Query,QueryDecorator> queryDecorators;
    //protected WeakHashMap<QueryResult,QueryResultDecorator> queryResultDecorators;
    //protected WeakHashMap<ValueFactory,ValueFactoryDecorator> valueFactoryDecorators;
    //protected WeakHashMap<ItemVisitor,ItemVisitorDecorator> itemVisitorDecorators;

    public ServicingDecoratorFactory() {
        repositoryDecorators = new WeakHashMap<Repository,RepositoryDecorator>();
        sessionDecorators = new WeakHashMap<Session,SessionDecorator>();
        workspaceDecorators = new WeakHashMap<Workspace,ServicingWorkspaceImpl>();
        //nodeDecorators = new WeakHashMap<Node,ServicingNodeImpl>();
        //propertyDecorators = new WeakHashMap<Property,PropertyDecorator>();
        //lockDecorators = new WeakHashMap<Lock,LockDecorator>();
        //versionDecorators = new WeakHashMap<Version,VersionDecorator>();
        //versionHistoryDecorators = new WeakHashMap<VersionHistory,VersionHistoryDecorator>();
        //itemDecorators = new WeakHashMap<Item,ItemDecorator>();
        //queryManagerDecorators = new WeakHashMap<QueryManager,QueryManagerDecorator>();
        //queryDecorators = new WeakHashMap<Query,QueryDecorator>();
        //queryResultDecorators = new WeakHashMap<QueryResult,QueryResultDecorator>();
        //valueFactoryDecorators = new WeakHashMap<ValueFactory,ValueFactoryDecorator>();
        //itemVisitorDecorators = new WeakHashMap<ItemVisitor,ItemVisitorDecorator>();
    }

    public Repository getRepositoryDecorator(Repository repository) {
        if(!repositoryDecorators.containsKey(repository)) {
            RepositoryDecorator wrapper = new RepositoryDecorator(this, repository);
            repositoryDecorators.put(repository, wrapper);
            return wrapper;
        } else
            return repositoryDecorators.get(repository);
    }
    public Session getSessionDecorator(Repository repository, Session session) {
        if(!sessionDecorators.containsKey(session)) {
            SessionDecorator wrapper = new SessionDecorator(this, repository, session);
            sessionDecorators.put(session, wrapper);
            return wrapper;
        } else
            return sessionDecorators.get(session);
    }
    public Workspace getWorkspaceDecorator(Session session, Workspace workspace) {
        if(!workspaceDecorators.containsKey(workspace)) {
            ServicingWorkspaceImpl wrapper = new ServicingWorkspaceImpl(this, session, workspace);
            workspaceDecorators.put(workspace, wrapper);
            return wrapper;
        } else
            return workspaceDecorators.get(workspace);
    }
    public Node getNodeDecorator(Session session, Node node) {
        if (node instanceof Version) {
            return getVersionDecorator(session, (Version) node);
        } else if (node instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) node);
        } else {
            return new ServicingNodeImpl(this, session, node);
            /*
            if(!nodeDecorators.containsKey(node)) {
                ServicingNodeImpl wrapper = new ServicingNodeImpl(this, session, node);
                nodeDecorators.put(node, wrapper);
                return wrapper;
            } else
                return nodeDecorators.get(node);
            */
        }
    }
    public Property getPropertyDecorator(Session session, Property property) {
        return new PropertyDecorator(this, session, property);
        /*
        if(!propertyDecorators.containsKey(property)) {
            PropertyDecorator wrapper = new PropertyDecorator(this, session, property);
            propertyDecorators.put(property, wrapper);
            return wrapper;
        } else
            return propertyDecorators.get(property);
        */
    }
    public Lock getLockDecorator(Session session, Lock lock) {
        return new LockDecorator(this, session, lock);
        /*
        if(!lockDecorators.containsKey(lock)) {
            LockDecorator wrapper = new LockDecorator(this, session, lock);
            lockDecorators.put(lock, wrapper);
            return wrapper;
        } else
            return lockDecorators.get(lock);
        */
    }
    public Version getVersionDecorator(Session session, Version version) {
        return new VersionDecorator(this, session, version);
        /*
        if(!versionDecorators.containsKey(version)) {
            VersionDecorator wrapper = new VersionDecorator(this, session, version);
            versionDecorators.put(version, wrapper);
            return wrapper;
        } else
            return versionDecorators.get(version);
        */
    }
    public VersionHistory getVersionHistoryDecorator(Session session,
                                                     VersionHistory versionHistory) {
        return new VersionHistoryDecorator(this, session, versionHistory);
        /*
        if(!versionHistoryDecorators.containsKey(versionHistory)) {
            VersionHistoryDecorator wrapper = new VersionHistoryDecorator(this, session, versionHistory);
            versionHistoryDecorators.put(versionHistory, wrapper);
            return wrapper;
        } else
            return versionHistoryDecorators.get(versionHistory);
        */
    }
    public Item getItemDecorator(Session session, Item item) {
        if (item instanceof Version) {
            return getVersionDecorator(session, (Version) item);
        } else if (item instanceof VersionHistory) {
            return getVersionHistoryDecorator(session, (VersionHistory) item);
        } else if (item instanceof Node) {
            return getNodeDecorator(session, (Node) item);
        } else if (item instanceof Property) {
            return getPropertyDecorator(session, (Property) item);
        } else {
            return new ItemDecorator(this, session, item);
            /*
            if(!itemDecorators.containsKey(item)) {
                ItemDecorator wrapper = new ItemDecorator(this, session, item);
                itemDecorators.put(item, wrapper);
                return wrapper;
            } else
                return itemDecorators.get(item);
            */
        }
    }
    public QueryManager getQueryManagerDecorator(Session session,
                                                 QueryManager queryManager) {
        return new QueryManagerDecorator(this, session, queryManager);
        /*
        if(!queryManagerDecorators.containsKey(queryManager)) {
            QueryManagerDecorator wrapper = new QueryManagerDecorator(this, session, queryManager);
            queryManagerDecorators.put(queryManager, wrapper);
            return wrapper;
        } else
            return queryManagerDecorators.get(queryManager);
        */
    }
    public Query getQueryDecorator(Session session, Query query) {
        return new QueryDecorator(this, session, query);
        /*
        if(!queryDecorators.containsKey(query)) {
            QueryDecorator wrapper = new QueryDecorator(this, session, query);
            queryDecorators.put(query, wrapper);
            return wrapper;
        } else
            return queryDecorators.get(query);
        */
    }
    public QueryResult getQueryResultDecorator(Session session,
                                               QueryResult result) {
        return new QueryResultDecorator(this, session, result);
        /*
        if(!queryResultDecorators.containsKey(result)) {
            QueryResultDecorator wrapper = new QueryResultDecorator(this, session, result);
            queryResultDecorators.put(result, wrapper);
            return wrapper;
        } else
            return queryResultDecorators.get(result);
        */
    }
    public ValueFactory getValueFactoryDecorator(Session session,
                                                 ValueFactory valueFactory) {
        return new ValueFactoryDecorator(this, session, valueFactory);
        /*
        if(!valueFactoryDecorators.containsKey(valueFactory)) {
            ValueFactoryDecorator wrapper = new ValueFactoryDecorator(this, session, valueFactory);
            valueFactoryDecorators.put(valueFactory, wrapper);
            return wrapper;
        } else
            return valueFactoryDecorators.get(valueFactory);
        */
    }
    public ItemVisitor getItemVisitorDecorator(Session session,
                                               ItemVisitor visitor) {
        return new ItemVisitorDecorator(this, session, visitor);
        /*
        if(!itemVisitorDecorators.containsKey(visitor)) {
            ItemVisitorDecorator wrapper = new ItemVisitorDecorator(this, session, visitor);
            itemVisitorDecorators.put(visitor, wrapper);
            return wrapper;
        } else
            return itemVisitorDecorators.get(visitor);
        */
    }
}
