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

package org.springframework.data.graph.neo4j.support.relationship;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.FieldSignature;
import org.neo4j.graphdb.Relationship;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.annotation.RelationshipEntity;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.fieldaccess.*;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;
import org.springframework.persistence.support.AbstractTypeAnnotatingMixinFields;

import java.lang.reflect.Field;

import static org.springframework.data.graph.neo4j.fieldaccess.DoReturn.unwrap;

/**
 * Aspect for handling relationship entity creation and field access (read & write)
 * puts the underlying state into and delegates field access to an {@link EntityStateAccessors} instance,
 * created by a configured {@link RelationshipEntityStateAccessorsFactory}
 */
public aspect Neo4jRelationshipBacking {
	
    protected final Log log = LogFactory.getLog(getClass());

    declare parents : (@RelationshipEntity *) implements RelationshipBacked;
    declare @type: RelationshipBacked+: @Configurable;


    protected pointcut entityFieldGet(RelationshipBacked entity) :
            get(* RelationshipBacked+.*) &&
            this(entity) &&
            !get(* RelationshipBacked.*);


    protected pointcut entityFieldSet(RelationshipBacked entity, Object newVal) :
            set(* RelationshipBacked+.*) &&
            this(entity) &&
            args(newVal) &&
            !set(* RelationshipBacked.*);

	private GraphDatabaseContext graphDatabaseContext;
    private RelationshipEntityStateAccessorsFactory entityStateAccessorsFactory;


    public void setGraphDatabaseContext(GraphDatabaseContext graphDatabaseContext) {
        this.graphDatabaseContext = graphDatabaseContext;
    }

    public void setRelationshipEntityStateAccessorsFactory(RelationshipEntityStateAccessorsFactory entityStateAccessorsFactory) {
        this.entityStateAccessorsFactory = entityStateAccessorsFactory;
    }

    /**
     * field for {@link EntityStateAccessors} that takes care of all entity operations
     */
    private EntityStateAccessors<RelationshipBacked,Relationship> RelationshipBacked.stateAccessors;

    /**
     * creates a new {@link EntityStateAccessors} instance with the relationship parameter or updates an existing one
     * @param r
     */
	public void RelationshipBacked.setUnderlyingState(Relationship r) {
        if (this.stateAccessors == null) {
            this.stateAccessors = Neo4jRelationshipBacking.aspectOf().entityStateAccessorsFactory.getEntityStateAccessors(this);
        }
        this.stateAccessors.setUnderlyingState(r);
	}
	
	public Relationship RelationshipBacked.getUnderlyingState() {
		return this.stateAccessors.getUnderlyingState();
	}

	public boolean RelationshipBacked.hasUnderlyingRelationship() {
		return this.stateAccessors.hasUnderlyingState();
	}

    /**
     * @return relationship id if there is an underlying relationship
     */
	public Long RelationshipBacked.getId() {
        if (!hasUnderlyingRelationship()) return null;
		return getUnderlyingState().getId();
	}


    /**
     * @param obj
     * @return result of equality check of the underlying relationship
     */
	public final boolean RelationshipBacked.equals(Object obj) {
		if (obj instanceof RelationshipBacked) {
			return this.getUnderlyingState().equals(((RelationshipBacked) obj).getUnderlyingState());
		}
		return false;
	}

    /**
     * @return hashCode of the underlying relationship
     */
	public final int RelationshipBacked.hashCode() {
		return getUnderlyingState().hashCode();
	}

	public void RelationshipBacked.remove() {
	     Neo4jRelationshipBacking.aspectOf().graphDatabaseContext.removeRelationshipEntity(this);
	}

    public <R extends RelationshipBacked> R  RelationshipBacked.projectTo(Class<R> targetType) {
        return (R)Neo4jRelationshipBacking.aspectOf().graphDatabaseContext.projectTo(this, targetType);
    }

    Object around(RelationshipBacked entity): entityFieldGet(entity) {
        Object result=entity.stateAccessors.getValue(field(thisJoinPoint));
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity);
    }

    Object around(RelationshipBacked entity, Object newVal) : entityFieldSet(entity, newVal) {
        Object result=entity.stateAccessors.setValue(field(thisJoinPoint),newVal);
        if (result instanceof DoReturn) return unwrap(result);
        return proceed(entity,result);
	}


    Field field(JoinPoint joinPoint) {
        FieldSignature fieldSignature = (FieldSignature)joinPoint.getSignature();
        return fieldSignature.getField();
    }
}
