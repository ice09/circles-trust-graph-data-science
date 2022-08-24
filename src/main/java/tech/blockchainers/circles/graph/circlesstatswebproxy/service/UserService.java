package tech.blockchainers.circles.graph.circlesstatswebproxy.service;

import lombok.extern.slf4j.Slf4j;
import org.neo4j.driver.internal.InternalPath;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import tech.blockchainers.circles.graph.circlesstatswebproxy.model.FlatUser;
import tech.blockchainers.circles.graph.circlesstatswebproxy.model.User;
import tech.blockchainers.circles.graph.circlesstatswebproxy.repository.UserRepository;

import java.util.*;

@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final Neo4jClient neo4jClient;

    public UserService(UserRepository userRepository, Neo4jClient neo4jClient) {
        this.userRepository = userRepository;
        this.neo4jClient = neo4jClient;
    }

    public void createProjection() {
        String query = """
                CALL gds.graph.project('circles', 'User', 'TRUSTS')
                """;
        try {
            neo4jClient.query(query).run();
        } catch (InvalidDataAccessResourceUsageException ex) {
            log.error("Projection cannot be created, does it exist already?", ex);
        }
    }


    public List<User> readUserGraph(String name) {
        return userRepository.triadicClosureForUser(name);
    }

    public List<User> calcPathNames(String sender, String receiver) {
        return userRepository.shortestPathNames(sender, receiver);
    }

    public List<User> calcPathAddrs(String sender, String receiver) {
        return userRepository.shortestPathAddr(sender, receiver);
    }

    public List<List<Map<String, String>>> calcAllPathNames(String sender, String receiver) {
        String query = """
            MATCH path = ( (you:User {name:$SENDER})<-[*1..4]-(other:User {name:$RECEIVER}) )
            WHERE all(r IN relationships(path) WHERE (r.amount>0))
            AND size(apoc.coll.toSet(NODES(path))) = size(NODES(path))
            RETURN path
            LIMIT 1000
        """;
        return createUserPathList(query, sender, receiver);
    }

    public List<List<Map<String, String>>> calcAllPathAddrs(String sender, String receiver) {
        String query = """
            MATCH path = ( (you:User {address:$SENDER})<-[*1..4]-(other:User {address:$RECEIVER}) )
            WHERE all(r IN relationships(path) WHERE (r.amount>0))
            AND size(apoc.coll.toSet(NODES(path))) = size(NODES(path))
            RETURN path
            LIMIT 1000
        """;
        return createUserPathList(query, sender, receiver);
    }

    private List<List<Map<String, String>>> createUserPathList(String query, String sender, String receiver) {
        Collection<Map<String, Object>> col =
                neo4jClient
                        .query(query)
                        .bind(sender).to("SENDER")
                        .bind(receiver).to("RECEIVER")
                        .fetch()
                        .all();
        List<List<Map<String, String>>> mappedPaths = new ArrayList<>();
        for (Map<String, Object> entry : col) {
            InternalPath pathEntry = (InternalPath)entry.get("path");
            List<Map<String, String>> onePath = new ArrayList<>();
            for (Node user : pathEntry.nodes()) {
                Map<String, String> userMap = Map.of(user.get("address").asString(), user.get("name").asString());
                onePath.add(userMap);
            }
            mappedPaths.add(onePath);
        }
        return mappedPaths;
    }

    public Collection<Map<String, Object>> readPagerank() {
        String query = """
                CALL gds.pageRank.stream('circles')
                YIELD nodeId, score
                RETURN gds.util.asNode(nodeId).address AS address, gds.util.asNode(nodeId).name AS name, score
                ORDER BY score DESC, name ASC
        """;
        Collection<Map<String, Object>> col =
                neo4jClient
                        .query(query)
                        .fetch()
                        .all();
        return col;
    }

    public Collection<Map<String, Object>> readPagerankStats() {
        String query = """
                CALL gds.pageRank.stats('circles', {
                    maxIterations: 20,
                    dampingFactor: 0.85
                })
                YIELD centralityDistribution
                RETURN centralityDistribution
                """;
        Collection<Map<String, Object>> col =
                neo4jClient
                        .query(query)
                        .fetch()
                        .all();
        return col;
    }

    public Collection<Map<String, Object>> readBetweenness() {
        String query = """
                CALL gds.betweenness.stream('circles') YIELD nodeId, score
                RETURN gds.util.asNode(nodeId).address AS address, gds.util.asNode(nodeId).name AS name, score ORDER BY score DESC, name ASC
                """;
        Collection<Map<String, Object>> col =
                neo4jClient
                        .query(query)
                        .fetch()
                        .all();
        return col;
    }

    public Collection<Map<String, Object>> readBetweennessStats() {
        String query = """
                CALL gds.betweenness.stats('circles')
                YIELD centralityDistribution
                RETURN centralityDistribution                
                """;
        Collection<Map<String, Object>> col =
                neo4jClient
                        .query(query)
                        .fetch()
                        .all();
        return col;
    }

    public Collection<Map<String, Object>> readSimilarityJacc() {
        String query = """
                CALL gds.nodeSimilarity.stream('circles') YIELD node1, node2, similarity
                RETURN gds.util.asNode(node1).address AS user1Addr, gds.util.asNode(node1).name AS user1Name, gds.util.asNode(node2).address AS user2Addr, gds.util.asNode(node2).name AS user2Name, similarity
                ORDER BY similarity DESCENDING, user1Name, user2Name
        """;
        Collection<Map<String, Object>> col =
                neo4jClient
                        .query(query)
                        .fetch()
                        .all();
        return col;
    }

    public Collection<Map<String, Object>> readSimilarityJaccStats() {
        String query = """
                CALL gds.nodeSimilarity.stats('circles')
                YIELD similarityDistribution
                RETURN similarityDistribution
        """;
        Collection<Map<String, Object>> col =
                neo4jClient
                        .query(query)
                        .fetch()
                        .all();
        return col;
    }

    public Collection<FlatUser> readTrustersForUser(String name) {
        Collection<FlatUser> col =
                neo4jClient
                        .query("MATCH (u1:User)<-[:TRUSTS]-(u2:User) WHERE u1.name=$name RETURN u1 as trustee, collect(u2) as trusters")
                        .bind(name).to("name")
                        .fetchAs(FlatUser.class).mappedBy((TypeSystem t, org.neo4j.driver.Record record) -> {
                            List<FlatUser> trusters = record.get("trusters")
                                    .asList(v -> new FlatUser(v.get("address").asString(), v.get("name").asString(), v.get("image_url").asString()));
                            Node node = record.get("trustee").asNode();
                            String addr = node.get("address").asString();
                            String uname = node.get("name").asString();
                            String imageUrl = node.get("image_url").asString();
                            return new FlatUser(addr, uname, imageUrl, trusters);
                        }).all();
        return col;
    }

    public Collection<FlatUser> readTrusteesForUser(String name) {
        Collection<FlatUser> col =
                neo4jClient
                        .query("MATCH (u1:User)-[:TRUSTS]->(u2:User) WHERE u1.name=$name RETURN u1 as truster, collect(u2) as trustees")
                        .bind(name).to("name")
                        .fetchAs(FlatUser.class).mappedBy((TypeSystem t, org.neo4j.driver.Record record) -> {
                            List<FlatUser> trustees = record.get("trustees")
                                    .asList(v -> new FlatUser(v.get("address").asString(), v.get("name").asString(), v.get("image_url").asString()));
                            Node node = record.get("truster").asNode();
                            String addr = node.get("address").asString();
                            String uname = node.get("name").asString();
                            String imageUrl = node.get("image_url").asString();
                            return new FlatUser(addr, uname, imageUrl, trustees);
                        }).all();
        return col;
    }


}
