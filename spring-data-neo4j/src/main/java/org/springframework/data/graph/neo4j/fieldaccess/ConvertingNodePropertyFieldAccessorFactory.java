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

import org.neo4j.graphdb.PropertyContainer;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.convert.ConversionService;
import org.springframework.data.graph.core.GraphBacked;
import org.springframework.data.graph.core.NodeBacked;
import org.springframework.data.graph.core.RelationshipBacked;

import java.lang.reflect.Field;

/**
 * @author Michael Hunger
 * @since 12.09.2010
 */
@Configurable
public class ConvertingNodePropertyFieldAccessorFactory implements FieldAccessorFactory<GraphBacked<PropertyContainer>> {

	ConversionService conversionService;
	
    public ConvertingNodePropertyFieldAccessorFactory(ConversionService conversionService) {
		super();
		this.conversionService = conversionService;
	}

    
	@Override
    public boolean accept(final Field field) {
        return isSerializableField(field) && isDeserializableField(field);
    }

    @Override
    public FieldAccessor<GraphBacked<PropertyContainer>> forField(final Field field) {
        return new ConvertingNodePropertyFieldAccessor(field, conversionService);
    }

    private boolean isSerializableField(final Field field) {
        return isSimpleValueField(field) && conversionService.canConvert(field.getType(), String.class);
    }

    private boolean isDeserializableField(final Field field) {
        return isSimpleValueField(field) && conversionService.canConvert(String.class, field.getType());
    }

    private boolean isSimpleValueField(final Field field) {
        final Class<?> type = field.getType();
        if (Iterable.class.isAssignableFrom(type) || NodeBacked.class.isAssignableFrom(type) || RelationshipBacked.class.isAssignableFrom(type))
            return false;
        return true;
    }

    public static class ConvertingNodePropertyFieldAccessor extends PropertyFieldAccessorFactory.PropertyFieldAccessor {
        private final ConversionService conversionService;

        public ConvertingNodePropertyFieldAccessor(final Field field, final ConversionService conversionService) {
            super(field);
            this.conversionService = conversionService;
        }

        @Override
        public Object setValue(final GraphBacked<PropertyContainer> graphBacked, final Object newVal) {
            super.setValue(graphBacked, serializePropertyValue(newVal));
            return newVal;
        }

        @Override
        public Object doGetValue(final GraphBacked<PropertyContainer> graphBacked) {
            return deserializePropertyValue(super.doGetValue(graphBacked));
        }

        private Object serializePropertyValue(final Object newVal) {
            return conversionService.convert(newVal, String.class);
        }

        private Object deserializePropertyValue(final Object value) {
            return conversionService.convert(value, field.getType());
        }

    }
}
