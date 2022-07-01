# Circles UBI Trust Graph Data Science

This repository contains a `docker-compose.yml` with

* The REST API Interface (**Port 9090**)
* Neo4j Graph Database (**Port 8567**)

## Build

* Run `mvn spring-boot:image-build` in root directory

## Run

* `cd docker`
* Run `docker-compose -d`

## Setup

* Install Neo4j Desktop
* Connect to the locally running neo4j at `neo4j://localhost:7867` with `neo4j/neo4j`
* Change password after initial connect tp `neo4j/q`

![](https://i.imgur.com/e9yeiEU.png)

![](https://i.imgur.com/BHau1IY.png)


## Import from CirclesUBI-Land-Export

1. Download & Unzip [Circles-UBI Trustgraph 07/2022](https://drive.google.com/file/d/1UvZLAEGB7SLlJuYaKs2Q8z8q1dxrW7GQ/view?usp=sharing)
2. Save CSV as `export.csv` in neo4j `import` folder
3. Import data form `export.csv`
```
# Initially create the index for user address
CREATE CONSTRAINT ON (u:User) ASSERT u.address IS UNIQUE;

LOAD CSV WITH HEADERS FROM 'file:///export.csv' AS row

MERGE (u1:User {address: row.truster_address})
  SET u1.name = row.truster_name, u1.image_url = row.truster_image_url
MERGE (u2:User {address: row.trustee_address})
  SET u2.name = row.trustee_name, u2.image_url = row.trustee_image_url
MERGE (u1)-[r:TRUSTS]->(u2)
  SET r.blockNumber = toInteger(row.blockNumber), r.amount = toFloat(row.amount);
```
4. Delete **self-trust**
```
MATCH (u1:User)-[t:TRUSTS]->(u2:User)
WHERE (u1.address = u2.address)
DELETE t
```

## Use

* Call http://localhost:9090/swagger-ui.html

## Querying the Trust Graph

### Truster & Trustees

**Get all trusters of user {name}**: `GET /trusters/{name}`

*Cypher* 
```
MATCH (u1:User)<-[:TRUSTS]-(u2:User) 
WHERE u1.name=$name 
RETURN u1 as trustee, collect(u2) as trusters
```

**Get all trustees of user {name}**: `GET /trustees/{name}`

*Cypher*
```
MATCH (u1:User)-[:TRUSTS]->(u2:User) 
WHERE u1.name=$name 
RETURN u1 as truster, collect(u2) as trustees
```

**Get recommendations by triadic closure for user {name}**: `GET /recommendations/{name}`

*Cypher*
```
MATCH (u1:User)<-[t1:TRUSTS]-(u2:User)-[t2:TRUSTS]->(u3:User) 
WHERE u1.name=$NAME AND NOT EXISTS( (u1)-[:TRUSTS]->(u3) ) 
RETURN u3
```
### Trust Path Traversal

**Get shortest path from {sender} to {receiver} as usernames**: `GET /path/names/{sender}/{receiver}`

*Cypher*
```
MATCH path = shortestPath( (you:User {name:$SENDER})-[*]->(other:User {name:$RECEIVER}) )
WHERE all(r IN relationships(path) WHERE (r.amount>0))
RETURN nodes(path)
```

**Get shortest path from {sender} to {receiver} as addresses**: `GET /path/addr/{sender}/{receiver}`

*Cypher*
```
MATCH path = shortestPath( (you:User {address:$SENDER})-[*]->(other:User {address:$RECEIVER}) )
WHERE all(r IN relationships(path) WHERE (r.amount>0))
RETURN nodes(path)
```

### Graph Data Science (GDS)

#### Initial  Setup: Create GDS Projection
`CALL gds.graph.create('circles', 'User', 'TRUSTS')`

**Pagerank**: `GET /pagerank?name=username`

*Cypher*
```
CALL gds.pageRank.stream('circles')
YIELD nodeId, score
RETURN gds.util.asNode(nodeId).address AS address, gds.util.asNode(nodeId).name AS name, score
ORDER BY score DESC, name ASC
```
* see https://neo4j.com/docs/graph-data-science/current/algorithms/page-rank/

**Node Similarity**: `GET /similarity?name=username`

*Cypher*
```
CALL gds.nodeSimilarity.stream('circles') YIELD node1, node2, similarity 
RETURN gds.util.asNode(node1).name AS User1, gds.util.asNode(node2).name AS User2, similarity 
ORDER BY similarity DESCENDING, User1, User2
```

* see https://neo4j.com/docs/graph-data-science/current/algorithms/betweenness-centrality/

**Betweeness**: `GET /betweenness?name=username`

*Cypher*
```
CALL gds.betweenness.stream('circles') YIELD nodeId, score 
RETURN gds.util.asNode(nodeId).name AS name, score ORDER  BY name ASC
```

* see https://neo4j.com/docs/graph-data-science/current/algorithms/node-similarity/

### References

#### Cypher Queries for Recommendations

#### People who trust your Trustees and who are not trusted by you (Triadic Closure)
```
MATCH (u1:User {name: "Martin"})-[:TRUSTS]->(b:User)<-[:TRUSTS]-(o:User)
WHERE NOT EXISTS( (u1)-[:TRUSTS]->(o) ) AND (o.name IS NOT NULL)
RETURN DISTINCT o.name, b.name
LIMIT 25
```

#### People you may know (2nd grade)
```
MATCH (u1:User {name: "Martin"})-[:TRUSTS]->(:User)<-[:TRUSTS]-(o:User)
MATCH (o)-[:TRUSTS]->(u2:User)
WHERE NOT EXISTS( (u1)-[:TRUSTS]->(u2) ) AND (u2.name IS NOT NULL)
RETURN DISTINCT u2.name
LIMIT 25
```

### References

#### Export CSV from neo4j

1. Adjust settings for neo4j DB to relations amount ("Graph Visualization") ~200.000
```
MATCH (truster)-[TRUSTS]->(trustee) WHERE truster.address <> "0x0" AND trustee.address <> "0x0" 
RETURN truster.address AS truster_address,truster.name as truster_name,truster.image_url as truster_image_url,trustee.address as trustee_address,trustee.name AS trustee_name,trustee.image_url as trustee_image_url,TRUSTS.amount AS amount,TRUSTS.blockNumber as blockNumber
```
2. "Export CSV" in table view
3. Replace "null" with ""