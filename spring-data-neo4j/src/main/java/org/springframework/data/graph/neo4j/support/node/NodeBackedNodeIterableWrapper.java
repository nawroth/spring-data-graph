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

package org.springframework.data.graph.neo4j.support.node;

import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.traversal.Traverser;
import org.neo4j.helpers.collection.IterableWrapper;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

/**
 * Simple wrapper to create an Iterable from a traverser, tied to a NodeBacked entity. Creates NodeEntities on the fly
 * while iterating the Iterator from the traverersal result.
 * @author Michael Hunger
 * @since 14.09.2010
 */
public class NodeBackedNodeIterableWrapper<T extends NodeBacked> extends IterableWrapper<T, Node> {
    private final Class<T> targetType;
    private final GraphDatabaseContext graphDatabaseContext;

    public NodeBackedNodeIterableWrapper(Traverser traverser, Class<T> targetType, final GraphDatabaseContext graphDatabaseContext) {
        super(traverser.nodes());
        this.targetType = targetType;
        this.graphDatabaseContext = graphDatabaseContext;
    }

    @Override
    protected T underlyingObjectToObject(Node node) {
        return graphDatabaseContext.createEntityFromState(node, targetType);
    }
}
