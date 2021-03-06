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

import org.neo4j.graphdb.NotInTransactionException;
import org.neo4j.graphdb.Relationship;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.graph.core.RelationshipBacked;
import org.springframework.data.graph.neo4j.finder.FinderFactory;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Michael Hunger
 * @since 21.09.2010
 */
public class RelationshipEntityStateAccessors<ENTITY extends RelationshipBacked> extends DefaultEntityStateAccessors<ENTITY, Relationship> {

    private final GraphDatabaseContext graphDatabaseContext;
    
    private final FinderFactory finderFactory;

    public RelationshipEntityStateAccessors(final Relationship underlyingState, final ENTITY entity, final Class<? extends ENTITY> type, final GraphDatabaseContext graphDatabaseContext, final FinderFactory finderFactory) {
        super(underlyingState, entity, type, new DelegatingFieldAccessorFactory(graphDatabaseContext, finderFactory) {
            @Override
            protected Collection<FieldAccessorListenerFactory<?>> createListenerFactories() {
                return Arrays.<FieldAccessorListenerFactory<?>>asList(
                        new IndexingNodePropertyFieldAccessorListenerFactory(
                		graphDatabaseContext,
                		new PropertyFieldAccessorFactory(),
                		new ConvertingNodePropertyFieldAccessorFactory(graphDatabaseContext.getConversionService())
                ));
            }

            @Override
            protected Collection<? extends FieldAccessorFactory<?>> createAccessorFactories() {
                return Arrays.<FieldAccessorFactory<?>>asList(
                        new TransientFieldAccessorFactory(),
                        new RelationshipNodeFieldAccessorFactory(graphDatabaseContext),
                        new PropertyFieldAccessorFactory(),
                        new ConvertingNodePropertyFieldAccessorFactory(graphDatabaseContext.getConversionService())
                );
            }
        });
        this.graphDatabaseContext = graphDatabaseContext;
        this.finderFactory = finderFactory;
    }

    @Override
    public void createAndAssignState() {
        if (entity.getUnderlyingState()!=null) return;
        try {
            final Object id = getIdFromEntity();
            if (id instanceof Number) {
                final Relationship relationship = graphDatabaseContext.getRelationshipById(((Number) id).longValue());
                setUnderlyingState(relationship);
                if (log.isInfoEnabled())
                    log.info("Entity reattached " + entity.getClass() + "; used Relationship [" + entity.getUnderlyingState() + "];");
                return;
            }

            final Relationship relationship = null; // TODO graphDatabaseContext.create();
            setUnderlyingState(relationship);
            if (log.isInfoEnabled()) log.info("User-defined constructor called on class " + entity.getClass() + "; created Relationship [" + getUnderlyingState() + "]; Updating metamodel");
        } catch (NotInTransactionException e) {
            throw new InvalidDataAccessResourceUsageException("Not in a Neo4j transaction.", e);
        }
    }

    @Override
    public ENTITY attach(boolean isOnCreate) {
        createAndAssignState();
        return entity;
    }

}
