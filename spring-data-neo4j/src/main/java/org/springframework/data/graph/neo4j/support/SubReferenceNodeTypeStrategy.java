/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.data.graph.neo4j.support;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.TraversalDescription;
import org.neo4j.helpers.collection.CombiningIterable;
import org.neo4j.helpers.collection.IterableWrapper;
import org.neo4j.kernel.impl.traversal.TraversalDescriptionImpl;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.NodeTypeStrategy;

import java.util.*;

/**
 * A {@link NodeTypeStrategy} that uses a hierarchy of reference nodes to represent the java type of the entity in the
 * graph database. Entity nodes are related to their concrete type via an INSTANCE_OF relationship, the type hierarchy is
 * related to supertypes via SUBCLASS_OF relationships. Each concrete subreference node keeps a count property with the number of
 * instances of this class in the graph.
 *
 * @author Michael Hunger
 * @since 13.09.2010
 */
public class SubReferenceNodeTypeStrategy implements NodeTypeStrategy {
    private final static Log log = LogFactory.getLog(SubReferenceNodeTypeStrategy.class);

    public final static RelationshipType INSTANCE_OF_RELATIONSHIP_TYPE = DynamicRelationshipType.withName("INSTANCE_OF");
    public final static RelationshipType SUBCLASS_OF_RELATIONSHIP_TYPE = DynamicRelationshipType.withName("SUBCLASS_OF");

    public static final String SUBREFERENCE_NODE_COUNTER_KEY = "count";
    public static final String SUBREF_PREFIX = "SUBREF_";
	public static final String SUBREF_CLASS_KEY = "class";

    private final GraphDatabaseContext graphDatabaseContext;

    public SubReferenceNodeTypeStrategy(final GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public static Node getSingleOtherNode(Node node, RelationshipType type,
                                          Direction direction) {
        Relationship rel = node.getSingleRelationship(type, direction);
        return rel == null ? null : rel.getOtherNode(node);
    }

    public static Integer incrementAndGetCounter(Node node, String propertyKey) {
        acquireWriteLock(node);
        int value = (Integer) node.getProperty(propertyKey, 0);
        value++;
        node.setProperty(propertyKey, value);
        return value;
    }

    public static Integer decrementAndGetCounter(Node node, String propertyKey,
                                                 int notLowerThan) {
        int value = (Integer) node.getProperty(propertyKey, 0);
        value--;
        value = value < notLowerThan ? notLowerThan : value;
        node.setProperty(propertyKey, value);
        return value;
    }

    public static void acquireWriteLock(PropertyContainer entity) {
        // TODO At the moment this is the best way of doing it, if you don't want to use
        // the LockManager (and release the lock yourself)
        entity.removeProperty("___dummy_property_for_locking___");
    }

    /**
     * lifecycle method, creates instanceof relationship to type node, creates the type nodes of the inheritance
     * hierarchy if necessary and increments instance counters
     * @param entity
     */
    @Override
    public void postEntityCreation(final NodeBacked entity) {
	    Class<? extends NodeBacked> clazz = entity.getClass();

	    final Node subReference = obtainSubreferenceNode(clazz);
        entity.getUnderlyingState().createRelationshipTo(subReference, INSTANCE_OF_RELATIONSHIP_TYPE);
	    subReference.setProperty(SUBREF_CLASS_KEY, clazz.getName());
	    if (log.isDebugEnabled()) log.debug("Created link to subref node: " + subReference + " with type: " + clazz.getName());

        incrementAndGetCounter(subReference, SUBREFERENCE_NODE_COUNTER_KEY);

	    updateSuperClassSubrefs(clazz, subReference);
    }

    /**
     * removes instanceof relationship and decrements instance counters for type nodes
     * @param entity
     */
    @Override
    public void preEntityRemoval(NodeBacked entity) {
        Class<? extends NodeBacked> clazz = entity.getClass();

        final Node subReference = obtainSubreferenceNode(clazz);
        Node subRefNode = entity.getUnderlyingState();
        Relationship instanceOf = subRefNode.getSingleRelationship(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
        instanceOf.delete();
        if (log.isDebugEnabled()) log.debug("Removed link to subref node: " + subReference + " with type: " + clazz.getName());
        TraversalDescription traversal = new TraversalDescriptionImpl().depthFirst().relationships(SUBCLASS_OF_RELATIONSHIP_TYPE, Direction.OUTGOING);
        for (Node node : traversal.traverse(subReference).nodes()) {
            Integer count = (Integer) node.getProperty(SUBREFERENCE_NODE_COUNTER_KEY);
            Integer newCount = decrementAndGetCounter(node, SUBREFERENCE_NODE_COUNTER_KEY, 0);
            if (log.isDebugEnabled()) log.debug("count on ref " + node + " was " + count + " new " + newCount);
        }
    }

    @Override
    public <T extends NodeBacked> Class<T> confirmType(Node node, Class<T> type) {
        Class<T> nodeType = this.<T>getJavaType(node);
        if (type.isAssignableFrom(nodeType)) return nodeType;
        throw new IllegalArgumentException(String.format("%s does not correspond to the node type %s of node %s",type,nodeType,node));
    }

    private void updateSuperClassSubrefs(Class<?> clazz, Node subReference) {
	    Class<?> superClass = clazz.getSuperclass();
	    if (superClass != null) {
		    Node superClassSubref = obtainSubreferenceNode(superClass);
		    if (getSingleOtherNode(subReference, SUBCLASS_OF_RELATIONSHIP_TYPE, Direction.OUTGOING) == null) {
			    subReference.createRelationshipTo(superClassSubref, SUBCLASS_OF_RELATIONSHIP_TYPE);
		    }
		    superClassSubref.setProperty(SUBREF_CLASS_KEY, superClass.getName());
		    Integer count = incrementAndGetCounter(superClassSubref, SUBREFERENCE_NODE_COUNTER_KEY);
		    if (log.isDebugEnabled()) log.debug("count on ref " + superClassSubref + " for class " + superClass.getSimpleName() + " = " + count);
		    updateSuperClassSubrefs(superClass, superClassSubref);
	    }
	}

	@Override
    public long count(final Class<? extends NodeBacked> entityClass) {
        final Node subrefNode = findSubreferenceNode(entityClass);
        if (subrefNode == null) return 0;
        return (Integer) subrefNode.getProperty(SUBREFERENCE_NODE_COUNTER_KEY, 0);
    }

	@Override
	@SuppressWarnings("unchecked")
	public <T extends NodeBacked> Class<T> getJavaType(Node node) {         
		Node subrefNode = node.getSingleRelationship(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.OUTGOING).getEndNode();
		try {
			Class<T> clazz = (Class<T>) Class.forName((String) subrefNode.getProperty(SUBREF_CLASS_KEY)).asSubclass(NodeBacked.class);
			if (log.isDebugEnabled()) log.debug("Found class " + clazz.getSimpleName() + " for node: " + node);
			return clazz;
		} catch (ClassNotFoundException e) {
			throw new IllegalStateException("Unable to get type for node: " + node, e);
		}
	}

	@Override
    public <T extends NodeBacked> Iterable<T> findAll(final Class<T> clazz) {
        final Node subrefNode = findSubreferenceNode(clazz);
		if (log.isDebugEnabled()) log.debug("Subref: " + subrefNode);
		Iterable<Iterable<T>> relIterables = findEntityIterables(subrefNode);
		return new CombiningIterable<T>(relIterables);
    }

	private <T extends NodeBacked> List<Iterable<T>> findEntityIterables(Node subrefNode) {
        if (subrefNode == null) return Collections.emptyList();
		List<Iterable<T>> result = new LinkedList<Iterable<T>>();
		for (Relationship relationship : subrefNode.getRelationships(SUBCLASS_OF_RELATIONSHIP_TYPE, Direction.INCOMING)) {
			result.addAll((Collection<? extends Iterable<T>>) findEntityIterables(relationship.getStartNode()));
		}
		Iterable<T> t = new IterableWrapper<T, Relationship>(subrefNode.getRelationships(INSTANCE_OF_RELATIONSHIP_TYPE, Direction.INCOMING)) {
            @Override
            protected T underlyingObjectToObject(final Relationship rel) {
                final Node node = rel.getStartNode();
	            T entity = (T) graphDatabaseContext.createEntityFromState(node, getJavaType(node));
	            if (log.isDebugEnabled()) log.debug("Converting node: " + node + " to entity: " + entity);
	            return entity;
            }
        };
		result.add(t);
		return result;
	}


	public Node obtainSubreferenceNode(final Class<?> entityClass) {
        return graphDatabaseContext.getOrCreateSubReferenceNode(subRefRelationshipType(entityClass));
    }

    public Node findSubreferenceNode(final Class<? extends NodeBacked> entityClass) {
        final Relationship subrefRelationship = graphDatabaseContext.getReferenceNode().getSingleRelationship(subRefRelationshipType(entityClass), Direction.OUTGOING);
        return subrefRelationship != null ? subrefRelationship.getEndNode() : null;
    }

    private DynamicRelationshipType subRefRelationshipType(Class<?> clazz) {
        return DynamicRelationshipType.withName(SUBREF_PREFIX + clazz.getName());
    }
}
