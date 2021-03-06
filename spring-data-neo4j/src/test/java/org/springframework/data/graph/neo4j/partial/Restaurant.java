package org.springframework.data.graph.neo4j.partial;

import org.springframework.data.annotation.Indexed;
import org.springframework.data.graph.annotation.GraphProperty;
import org.springframework.data.graph.annotation.NodeEntity;

import javax.persistence.*;

/**
 * @author Michael Hunger
 * @since 27.09.2010
 */
@Entity
@NodeEntity(partial = true)
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.TABLE, generator = "id_gen")
    @TableGenerator(name = "id_gen", table = "SEQUENCE", pkColumnName = "SEQ_NAME", valueColumnName = "SEQ_COUNT", pkColumnValue = "SEQ_GEN", allocationSize = 1)
    long id;
    String name;
    String zipCode;

    @GraphProperty
    @Indexed
    @Transient
    Cuisine cuisine;

}
