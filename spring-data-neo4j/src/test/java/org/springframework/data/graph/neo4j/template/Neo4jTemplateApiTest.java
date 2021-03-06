package org.springframework.data.graph.neo4j.template;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.junit.*;
import org.neo4j.graphdb.*;
import org.neo4j.kernel.ImpermanentGraphDatabase;
import org.neo4j.kernel.Traversal;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Iterator;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.*;
import static org.springframework.data.graph.neo4j.template.PropertyMap._;
import static org.springframework.data.graph.neo4j.template.PropertyMap.props;

/**
 * @author mh
 * @since 19.02.11
 */
public class Neo4jTemplateApiTest {
    private static final DynamicRelationshipType KNOWS = DynamicRelationshipType.withName("knows");
    private static final DynamicRelationshipType HAS = DynamicRelationshipType.withName("has");
    private Neo4jTemplate template;
    private static GraphDatabaseService graphDatabase;
    private static PlatformTransactionManager tm;
    private Node referenceNode;
    private Relationship relationship1;
    private Node node1;


    @BeforeClass
    public static void startDb() throws Exception {
        graphDatabase = new ImpermanentGraphDatabase();
//        tm = new JtaTransactionManager(new SpringTransactionManager(graphDatabase));
    }

    @Before
    public void setUp() {
        referenceNode = graphDatabase.getReferenceNode();
        template = new Neo4jTemplate(graphDatabase);
        createData();
    }
    private void createData() {
        Transaction tx = graphDatabase.beginTx();
        try {
            cleanDb(graphDatabase);
            referenceNode.setProperty("name", "node0");
            graphDatabase.index().forNodes("node").add(referenceNode,"name","node0");
            node1 = graphDatabase.createNode();
            node1.setProperty("name", "node1");
            relationship1 = referenceNode.createRelationshipTo(node1,KNOWS);
            relationship1.setProperty("name", "rel1");
            graphDatabase.index().forRelationships("relationship").add(relationship1, "name", "rel1");
            tx.success();
        } finally {
            tx.finish();
        }
    }

    private void cleanDb(GraphDatabaseService graphDatabase) {
        Node refNode = graphDatabase.getReferenceNode();
        for (Node node : graphDatabase.getAllNodes()) {
            for (Relationship rel : node.getRelationships()) {
                rel.delete();
            }
            if (!refNode.equals(node)) {
                node.delete();
            }
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        graphDatabase.shutdown();
    }

    @Test
    public void shouldExecuteCallbackInTransaction() throws Exception {
        Node refNode = template.exec(new GraphCallback<Node>() {
            @Override
            public Node doWithGraph(GraphDatabaseService graph) throws Exception {
                Node referenceNode = graph.getReferenceNode();
                referenceNode.setProperty("test", "testDoInTransaction");
                return referenceNode;
            }
        });
        assertEquals("same reference node",referenceNode,refNode);
        assertTestPropertySet(referenceNode, "testDoInTransaction");
    }

    @Test
    public void shouldRollbackTransactionOnException() {
        try {
            template.exec(new GraphCallback.WithoutResult() {
                @Override
                public void doWithGraphWithoutResult(GraphDatabaseService graph) throws Exception {
                    graph.getReferenceNode().setProperty("test", "shouldRollbackTransactionOnException");
                    throw new RuntimeException("please rollback");
                }
            });
        } catch(RuntimeException re){
            //ignore
        }
        Assert.assertThat((String)graphDatabase.getReferenceNode().getProperty("test","not set"), not("shouldRollbackTransactionOnException"));
    }

    @Test
    @Ignore("until the ImpermanentGraphDatabase cast issue is resolved in neo4j")
    public void shouldRollbackViaStatus() throws Exception {
        new TransactionTemplate(tm).execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                template.exec(new GraphCallback.WithoutResult() {
                    @Override
                    public void doWithGraphWithoutResult(GraphDatabaseService graph) throws Exception {
                        graph.getReferenceNode().setProperty("test", "shouldRollbackTransactionOnException");
                        status.setRollbackOnly();
                    }
                });
            }
        });
        Assert.assertThat((String) graphDatabase.getReferenceNode().getProperty("test","not set"), not("shouldRollbackTransactionOnException"));
    }

    @Test(expected = RuntimeException.class)
    public void shouldNotConvertUserRuntimeExceptionToDataAccessException() {
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabaseService graph) throws Exception {
                throw new RuntimeException();
            }
        });
    }

    @Test(expected = DataAccessException.class)
    public void shouldConvertMissingTransactionExceptionToDataAccessException() {
        Neo4jTemplate template = new Neo4jTemplate(graphDatabase, true);
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabaseService graph) throws Exception {
                graph.createNode();
            }
        });
    }
    @Test(expected = DataAccessException.class)
    public void shouldConvertNotFoundExceptionToDataAccessException() {
        Neo4jTemplate template = new Neo4jTemplate(graphDatabase, true);
        template.exec(new GraphCallback.WithoutResult() {
            @Override
            public void doWithGraphWithoutResult(GraphDatabaseService graph) throws Exception {
                graph.getNodeById(Long.MAX_VALUE);
            }
        });
    }
    @Test(expected = DataAccessException.class)
    public void shouldConvertTemplateNotFoundExceptionToDataAccessException() {
        template.getNode(Long.MAX_VALUE);
    }

    @Test
    public void shouldExecuteCallback() throws Exception {
        Long refNodeId = template.exec(new GraphCallback<Long>() {
            @Override
            public Long doWithGraph(GraphDatabaseService graph) throws Exception {
                return graph.getReferenceNode().getId();
            }
        });
        assertEquals(referenceNode.getId(),(long)refNodeId);
    }

    @Test
    public void testGetReferenceNode() throws Exception {
        assertEquals(referenceNode,template.getReferenceNode());
    }

    @Test
    public void testCreateNode() throws Exception {
        Node node=template.createNode(null);
        assertNotNull("created node",node);
    }

    @Test
    public void testCreateNodeWithProperties() throws Exception {
        Node node=template.createNode(props().set("test", "testCreateNodeWithProperties").toMap());
        assertTestPropertySet(node, "testCreateNodeWithProperties");
    }

    private void assertTestPropertySet(Node node, String testName) {
        assertEquals(testName, node.getProperty("test","not set"));
    }

    @Test
    public void testGetNode() throws Exception {
        Node lookedUpNode = template.getNode(referenceNode.getId());
        assertEquals(referenceNode,lookedUpNode);
    }

    @Test
    public void testGetRelationship() throws Exception {
        Relationship lookedUpRelationship = template.getRelationship(relationship1.getId());
        assertThat(lookedUpRelationship,is(relationship1));

    }

    @Test
    public void testIndexRelationship() throws Exception {
        Relationship lookedUpRelationship = graphDatabase.index().forRelationships("relationship").get("name", "rel1").getSingle();
        assertThat("same relationship from index",lookedUpRelationship,is(relationship1));
    }

    @Test
    public void testIndexNode() throws Exception {
        template.index(null, node1, "name","node1");
        Node lookedUpNode=graphDatabase.index().forNodes("node").get("name","node1").getSingle();
        assertThat("same node from index",lookedUpNode,is(node1));
    }

    @Test
    public void testQueryNodes() throws Exception {
        assertSingleResult("node0", template.query(null, new NodeNameMapper(), new TermQuery(new Term("name", "node0"))));
    }

    @Test
    public void testRetrieveNodes() throws Exception {
        assertSingleResult("node0", template.query(null, new NodeNameMapper(), "name", "node0"));
    }

    @Test
    public void testQueryRelationships() throws Exception {
        assertSingleResult("rel1", template.query("relationship", new RelationshipNameMapper(), new TermQuery(new Term("name", "rel1"))));
    }

    @Test
    public void testRetrieveRelationships() throws Exception {
        assertSingleResult("rel1",template.query("relationship", new RelationshipNameMapper(), "name", "rel1"));
    }

    @Test
    public void testTraverse() throws Exception {
        assertSingleResult("node1",template.traverseGraph(referenceNode, new NodeNameMapper(), Traversal.description().relationships(KNOWS).prune(Traversal.pruneAfterDepth(1)).filter(Traversal.returnAllButStartNode())));
    }

    @Test
    public void shouldGetDirectRelationship() throws Exception {
        assertSingleResult("rel1", template.traverseNext(referenceNode, new RelationshipNameMapper()));
    }
    @Test
    public void shouldGetDirectRelationshipForType() throws Exception {
        assertSingleResult("rel1", template.traverseNext(referenceNode, new RelationshipNameMapper(), KNOWS));
    }
    @Test
    public void shouldGetDirectRelationshipForTypeAndDirection() throws Exception {
        assertSingleResult("rel1", template.traverseNext(referenceNode, new RelationshipNameMapper(), KNOWS, Direction.OUTGOING));
    }

    private <T> void assertSingleResult(T expected, Iterable<T> iterable) {
        Iterator<T> result = iterable.iterator();
        assertEquals(expected, result.next());
        assertEquals(false, result.hasNext());
    }


    @Test
    public void shouldCreateRelationshipWithProperty() throws Exception {
        Relationship relationship = template.createRelationship(referenceNode, node1, HAS, _("name", "rel2"));
        assertNotNull(relationship);
        assertEquals(referenceNode, relationship.getStartNode());
        assertEquals(node1,relationship.getEndNode());
        assertEquals(HAS, relationship.getType());
        assertEquals("rel2",relationship.getProperty("name","not set"));
    }

    private static class RelationshipNameMapper implements PathMapper<String> {
        @Override
        public String mapPath(Path path) {
            return (String) path.lastRelationship().getProperty("name","not set");
        }
    }
    private static class NodeNameMapper implements PathMapper<String> {
        @Override
        public String mapPath(Path path) {
            return (String) path.endNode().getProperty("name","not set");
        }
    }
}
