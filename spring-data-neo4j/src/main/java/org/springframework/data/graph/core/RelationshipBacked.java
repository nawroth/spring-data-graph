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

import org.neo4j.graphdb.Relationship;

/**
 * concrete interface introduced onto Relationship entities by the {@link org.springframework.data.graph.neo4j.support.relationship.Neo4jRelationshipBacking}
 * aspect, encapsulates a neo4j relationship as backing state
 */
public interface RelationshipBacked extends GraphBacked<Relationship>{
	
}
