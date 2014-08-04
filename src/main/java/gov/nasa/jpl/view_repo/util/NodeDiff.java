package gov.nasa.jpl.view_repo.util;

import gov.nasa.jpl.mbee.util.Pair;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;

/**
 * NodeDiff provides access to differences between two nodes in alfresco, at
 * least, in terms of its properties. All String keys in Sets and Maps are in
 * the short format, such as cm:name. The values are the raw Serializable values
 * returned by getProperties(). The second node is treated as the result of a
 * change to the first.
 */
public class NodeDiff {
    protected static boolean computeDiffOnConstruction = false;
    protected boolean lazy = true;
    public NodeRef node1, node2;
//    public Set<String> removedAspects = null;
//    public Set<String> addedAspects = null;
    public Map<String, Object> removedProperties = null;
    public Map<String, Object> addedProperties = null;
    public Map<String, Pair< Object, Object > > updatedProperties = null;
    public Map<String, Pair< Object, Object > > propertyChanges = null;
    private ServiceRegistry services = null;

    public NodeDiff( NodeRef node1, NodeRef node2 ) {
        this( node1, node2, null );
    }

    public NodeDiff( NodeRef node1, NodeRef node2, Boolean lazy ) {
        if ( lazy != null ) this.lazy = lazy;
        this.node1 = node1;
        this.node2 = node2;
        if ( computeDiffOnConstruction ) diffProperties();
    }

    /**
     * Compute property changes and save them in propertyChanges.
     */
    protected void diffProperties() {
        NodeService nodeService = getServices().getNodeService();
        Map< QName, Serializable > properties1 = nodeService.getProperties( node1 );
        Map< QName, Serializable > properties2 = nodeService.getProperties( node2 );
        Set< QName > keys = new TreeSet< QName >( properties1.keySet() );
        keys.addAll( properties2.keySet() );
        propertyChanges = new TreeMap< String, Pair<Object,Object> >();
        if ( !lazy ) {
            addedProperties = new TreeMap< String, Object >();
            removedProperties = new TreeMap< String, Object >();
            updatedProperties = new TreeMap< String, Pair<Object,Object> >();
        }
        for ( QName qName : keys ) {
            Serializable val1 = properties1.get( qName );
            Serializable val2 = properties2.get( qName );
            
            if ( val1 == null && val2 == null ) continue;
            String propName = qName.toPrefixString();
            if ( !lazy ) {
                if ( val1 == null ) {
                    addedProperties.put( propName, val2 );
                } else if ( val2 == null ) {
                    removedProperties.put( propName, val1 );
                } else {
                    updatedProperties.put( propName, new Pair< Object, Object >( val1, val2 ) );
                }
            }
            propertyChanges.put( propName, new Pair< Object, Object >( val1, val2 ) );
        }
    }

    private ServiceRegistry getServices() {
        if ( services == null ) {
            services = NodeUtil.getServices();
        }
        return services;
    }

    public NodeRef getNode1() {
        return node1;
    }
    public NodeRef getNode2() {
        return node2;
    }
    
//  private void diffAspects() {
//  // TODO Auto-generated method stub
//}

//    public Set< String > getRemovedAspects() {
//        if ( removedAspects == null ) {
//            diffAspects();
//        }
//        return removedAspects;
//    }
//
//    public Set< String > getAddedAspects() {
//        if ( addedAspects == null ) {
//            diffAspects();
//        }
//        return addedAspects;
//    }
    
    public Map< String, Object > getRemovedProperties() {
        if ( removedProperties == null ) {
            removedProperties = new TreeMap< String, Object >();
            for ( Map.Entry< String, Pair< Object, Object > > e : getPropertyChanges().entrySet() ) {
                if ( e.getValue().second == null ) {
                    removedProperties.put( e.getKey(), e.getValue().first );
                }
            }
//            diffProperties();
        }
        return removedProperties;
    }

    public Map< String, Object > getAddedProperties() {
        if ( addedProperties == null ) {
            addedProperties = new TreeMap< String, Object >();
            for ( Map.Entry< String, Pair< Object, Object > > e : getPropertyChanges().entrySet() ) {
                if ( e.getValue().first == null ) {
                    addedProperties.put( e.getKey(), e.getValue().second );
                }
            }
//            diffProperties();
        }
        return addedProperties;
    }

    public Map< String, Pair< Object, Object >> getUpdatedProperties() {
        if ( updatedProperties == null ) {
            updatedProperties = new TreeMap< String, Pair<Object,Object> >( getPropertyChanges() );
            for ( String k : getAddedProperties().keySet() ) {
                updatedProperties.remove( k );
            }
            for ( String k : getRemovedProperties().keySet() ) {
                updatedProperties.remove( k );
            }
//            diffProperties();
        }
        return updatedProperties;
    }

    public Map< String, Pair< Object, Object >> getPropertyChanges() {
        if ( propertyChanges == null ) {
            diffProperties();
        }
        return propertyChanges;
    }
}
