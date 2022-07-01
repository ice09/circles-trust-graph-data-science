package tech.blockchainers.circles.graph.circlesstatswebproxy.service;

import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.TypeSystem;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import tech.blockchainers.circles.graph.circlesstatswebproxy.model.FlatUser;
import tech.blockchainers.circles.graph.circlesstatswebproxy.model.User;
import tech.blockchainers.circles.graph.circlesstatswebproxy.repository.UserRepository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Neo4jClient neo4jClient;

    public UserService(UserRepository userRepository, Neo4jClient neo4jClient) {
        this.userRepository = userRepository;
        this.neo4jClient = neo4jClient;
    }

    public List<User> readUserGraph(String name) {
        return userRepository.triadicClosureForUser(name);
    }

    public List<User> calcPathNames(String sender, String receiver) {
        return userRepository.shortestPathNames(sender, receiver);
    }

    public List<User> calcPathAddr(String sender, String receiver) {
        return userRepository.shortestPathAddr(sender, receiver);
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

    public Collection<Map<String, Object>> readPagerankNames() {
        String query = """
                CALL gds.pageRank.stream('circles')
                YIELD nodeId, score
                WHERE gds.util.asNode(nodeId).name IS NOT NULL AND NOT gds.util.asNode(nodeId).name STARTS WITH '0x'
                RETURN gds.util.asNode(nodeId).name AS name, score
                ORDER BY score DESC, name ASC
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

    public Collection<Map<String, Object>> readBetweennessNames() {
        String query = """
                CALL gds.betweenness.stream('circles') YIELD nodeId, score
                WHERE gds.util.asNode(nodeId).name IS NOT NULL AND NOT gds.util.asNode(nodeId).name STARTS WITH '0x'
                RETURN gds.util.asNode(nodeId).name AS name, score ORDER BY score DESC, name ASC
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
                WHERE gds.util.asNode(node1).name IS NOT NULL AND NOT gds.util.asNode(node1).name STARTS WITH '0x' AND gds.util.asNode(node2).name IS NOT NULL AND NOT gds.util.asNode(node2).name STARTS WITH '0x'
                RETURN gds.util.asNode(node1).name AS User1, gds.util.asNode(node2).name AS User2, similarity
                ORDER BY similarity DESCENDING, User1, User2
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
                                    .asList(v -> new FlatUser(v.get("address").asString(), v.get("name").asString()));
                            Node node = record.get("trustee").asNode();
                            String addr = node.get("address").asString();
                            String uname = node.get("name").asString();
                            String imageUrl = node.get("imageUrl").asString();
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
                            List<FlatUser> trusters = record.get("trustees")
                                    .asList(v -> new FlatUser(v.get("address").asString(), v.get("name").asString()));
                            Node node = record.get("truster").asNode();
                            String addr = node.get("address").asString();
                            String uname = node.get("name").asString();
                            String imageUrl = node.get("imageUrl").asString();
                            return new FlatUser(addr, uname, imageUrl, trusters);
                        }).all();
        return col;
    }


}
