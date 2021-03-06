package org.springframework.data.graph.neo4j;

import org.neo4j.graphdb.Node;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.data.graph.neo4j.support.GraphDatabaseContext;

/**
 * EXAMPLE OF CODE THAT SHOULD BE GENERATED BY ROO BESIDES EACH GRAPHENTITY CLASS 
 * 
 * Note: Combines X_Roo_Entity with X_Roo_Finder, as
 * we need only a single aspect for entities.
 * @author rodjohnson
 *
 */
privileged aspect Person_Graph_Entity {
	
	// TODO should be a better way of getting this? Could at least pull out gdsholder class
	private static GraphDatabaseContext graphDatabaseContext() {
		return new Person_Graph_Entity.GdsHolder().graphDatabaseContext;
	}
	
	@Configurable
	public static class GdsHolder {
		@Autowired
		public GraphDatabaseContext graphDatabaseContext;
	}
	
	/**
	 * Add constructor that takes node.
	 * @param node
	 */
	public Person.new(Node node) {
		setUnderlyingState(node);
	}

//	public static long Person.countPeople() {
//        return new SubReferenceNodeTypeStrategy(graphDatabaseContext()).count(Person.class);
//	}
//
//	public static Iterable<Person> Person.findAllPeople() {
//        final SubReferenceNodeTypeStrategy strategy = new SubReferenceNodeTypeStrategy(graphDatabaseContext());
//        return strategy.findAll(Person.class);
//	}
//
//	public static Person Person.findPerson(Long id) {
//		Node personNode = Person_Graph_Entity.graphDatabaseContext().getNodeById(id);
//        return new Person(personNode);
//	}
	

	// Pluggable query executors/resolvers, discussed with PL
//	public static Person.findFooBars(int a, int b) {
//		return executeQuery("foobar", a, b);
//		// First look for String, then for method
//		// QueryInterceptionResolver
//	}

//	public static List<Person> Person.findPersonEntries(int firstResult,
//			int maxResults) {
//		throw new UnsupportedOperationException();
//	}
}
