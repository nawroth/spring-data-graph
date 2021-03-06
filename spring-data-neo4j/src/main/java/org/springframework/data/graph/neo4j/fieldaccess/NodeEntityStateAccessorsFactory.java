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

package org.springframework.data.graph.neo4j.fieldaccess;

import org.neo4j.graphdb.Node;
import org.springframework.data.graph.annotation.NodeEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import static org.springframework.data.graph.neo4j.fieldaccess.PartialNodeEntityStateAccessors.getId;

public class NodeEntityStateAccessorsFactory {

	private GraphDatabaseContext graphDatabaseContext;
	
	private FinderFactory finderFactory;

	private NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory;

	public EntityStateAccessors<NodeBacked,Node> getEntityStateAccessors(final NodeBacked entity) {
        final NodeEntity graphEntityAnnotation = entity.getClass().getAnnotation(NodeEntity.class); // todo cache ??
        boolean autoAttach = graphEntityAnnotation.autoAttach();
        if (graphEntityAnnotation.partial()) {
            PartialNodeEntityStateAccessors<NodeBacked> partialNodeEntityStateAccessors = new PartialNodeEntityStateAccessors<NodeBacked>(null, entity, entity.getClass(), graphDatabaseContext, finderFactory);
            return new DetachableEntityStateAccessors<NodeBacked, Node>(partialNodeEntityStateAccessors, graphDatabaseContext, false) {
                @Override
                protected boolean transactionIsRunning() {
                    return super.transactionIsRunning() && getId(entity, entity.getClass()) != null;
                }
            };
        } else {
            NodeEntityStateAccessors<NodeBacked> nodeEntityStateAccessors = new NodeEntityStateAccessors<NodeBacked>(null, entity, entity.getClass(), graphDatabaseContext, nodeDelegatingFieldAccessorFactory);
            if (autoAttach) {
                return new NestedTransactionEntityStateAccessors<NodeBacked, Node>(nodeEntityStateAccessors,graphDatabaseContext);
            } else {
                return new DetachableEntityStateAccessors<NodeBacked, Node>(nodeEntityStateAccessors, graphDatabaseContext, autoAttach);
            }
        }
    }

	public void setNodeDelegatingFieldAccessorFactory(
			NodeDelegatingFieldAccessorFactory nodeDelegatingFieldAccessorFactory) {
		this.nodeDelegatingFieldAccessorFactory = nodeDelegatingFieldAccessorFactory;
	}
	
	public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
		this.graphDatabaseContext = graphDatabaseContext;
	}

	public void setFinderFactory(FinderFactory finderFactory) {
		this.finderFactory = finderFactory;
	}

}
