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

package org.springframework.data.graph.core;

import org.neo4j.graphdb.Node;

/**
 * Interface introduced to objects annotated with &#64;NodeEntity by the {@link org.springframework.data.graph.neo4j.support.node.Neo4jNodeBacking} aspect.
 * annotation, to hold underlying Neo4j Node state.
 * @author Rod Johnson
 */
public interface NodeBacked extends GraphBacked<Node> {

	// Relationship relateTo(NodeBacked nb, RelationshipType type);

}
