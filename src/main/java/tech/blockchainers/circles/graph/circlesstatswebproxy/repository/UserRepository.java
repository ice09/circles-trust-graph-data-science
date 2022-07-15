package tech.blockchainers.circles.graph.circlesstatswebproxy.repository;

import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tech.blockchainers.circles.graph.circlesstatswebproxy.model.User;

import java.util.List;

@Repository
public interface UserRepository extends Neo4jRepository<User, Long> {

    @Query("""
    MATCH (u1:User)<-[t1:TRUSTS]-(u2:User)-[t2:TRUSTS]->(u3:User) 
    WHERE u1.name=$NAME AND NOT EXISTS( (u1)-[:TRUSTS]->(u3) ) 
    AND (u2.name IS NOT NULL) AND NOT (u2.name STARTS WITH '0x') AND NOT (u3.name STARTS WITH '0x') AND (u3.name IS NOT NULL) 
    RETURN u3
    """)
    List<User> triadicClosureForUser(@Param("NAME") String name);

    @Query("""
    MATCH path = shortestPath( (you:User {name:$SENDER})-[*]->(other:User {name:$RECEIVER}) )
    WHERE all(r IN relationships(path) WHERE (r.amount>0))
    RETURN nodes(path)
    """)
    List<User> shortestPathNames(@Param("SENDER") String senderName, @Param("RECEIVER") String receiverName);

    @Query("""
    MATCH path = shortestPath( (you:User {address:$SENDER})-[*]->(other:User {address:$RECEIVER}) )
    WHERE all(r IN relationships(path) WHERE (r.amount>0))
    RETURN nodes(path)
    """)
    List<User> shortestPathAddr(@Param("SENDER") String senderAddr, @Param("RECEIVER") String receiverAddr);

}
