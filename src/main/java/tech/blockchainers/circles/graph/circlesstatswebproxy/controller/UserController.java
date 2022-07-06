package tech.blockchainers.circles.graph.circlesstatswebproxy.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import tech.blockchainers.circles.graph.circlesstatswebproxy.model.FlatUser;
import tech.blockchainers.circles.graph.circlesstatswebproxy.model.User;
import tech.blockchainers.circles.graph.circlesstatswebproxy.service.UserService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@RestController
public class UserController {

    private final UserService userService;
    private Collection<Map<String, Object>> cachedBet;
    private Collection<Map<String, Object>> cachedSim;
    private Collection<Map<String, Object>> cachedPagerank;
    private Collection<Map<String, Object>> cachedBetStats;
    private Collection<Map<String, Object>> cachedSimStats;
    private Collection<Map<String, Object>> cachedPagerankStats;

    public UserController(UserService userService) {
        this.userService = userService;
        this.cachedSim = new ArrayList<>();
        this.cachedBet = new ArrayList<>();
        this.cachedPagerank = new ArrayList<>();
        this.cachedSimStats = new ArrayList<>();
        this.cachedBetStats = new ArrayList<>();
        this.cachedPagerankStats = new ArrayList<>();
    }

    @GetMapping("/recommendations/{name}")
    public List<User> trustGraphForUser(@RequestParam("name") String name) {
        return userService.readUserGraph(name);
    }

    @GetMapping("/path/names/{sender}/{receiver}")
    public List<User> pathForNames(@PathVariable("sender") String sender, @PathVariable("receiver") String receiver) {
        return userService.calcPathNames(sender, receiver);
    }

    @GetMapping("/path/addr/{sender}/{receiver}")
    public List<User> pathForAddr(@PathVariable("sender") String sender, @PathVariable("receiver") String receiver) {
        return userService.calcPathAddr(sender, receiver);
    }

    @GetMapping("/trusters/{name}")
    public Collection<FlatUser> trustersForUser(@RequestParam("name") String name) {
        return userService.readTrustersForUser(name);
    }

    @GetMapping("/trustees/{name}")
    public Collection<FlatUser> trusteesForUser(@RequestParam("name") String name) {
        return userService.readTrusteesForUser(name);
    }

    @GetMapping("/similarity/reset")
    public ResponseEntity<Void> resetSimilarity() {
        cachedSim.clear();
        cachedSimStats.clear();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/similarity/stats")
    public Collection<Map<String, Object>> statsSimilarity() {
        if (cachedSimStats.isEmpty()) {
            cachedSimStats = userService.readSimilarityJaccStats();
        }
        return cachedSimStats;
    }

    @GetMapping("/similarity")
    public Collection<Map<String, Object>> similarity(@RequestParam(value = "name", required = false) String name) {
        if (cachedSim.isEmpty()) {
            cachedSim = userService.readSimilarityJacc();
        }
        if (StringUtils.hasText(name)) {
            List<Map<String, Object>> filteredForName = new ArrayList<>();
            for (Map<String, Object> map : cachedSim) {
                if ((map.get("user1Name") != null) && map.get("user1Name").toString().equals(name) || ((map.get("user2Name") != null) && map.get("user2Name").toString().equals(name))) {
                    filteredForName.add(map);
                }
            }
            return filteredForName;
        } else {
            return cachedSim;
        }
    }

    @GetMapping("/pagerank/stats")
    public Collection<Map<String, Object>> pagerankStats() {
        if (cachedPagerankStats.isEmpty()) {
            cachedPagerankStats = userService.readPagerankStats();
        }
        return cachedPagerankStats;
    }

    @GetMapping("/pagerank/reset")
    public ResponseEntity<Void> resetPagerank() {
        cachedPagerank.clear();
        cachedPagerankStats.clear();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pagerank")
    public Collection<Map<String, Object>> readPagerank(@RequestParam(value = "name",required = false) String name) {
        if (cachedPagerank.isEmpty()) {
            cachedPagerank = userService.readPagerank();
        }
        if (StringUtils.hasText(name)) {
            List<Map<String, Object>> filteredForName = new ArrayList<>();
            for (Map<String, Object> map : cachedPagerank) {
                if (map.containsKey("name") && map.get("name")!= null && map.get("name").toString().equals(name)) {
                    filteredForName.add(map);
                }
            }
            return filteredForName;
        } else {
            return cachedPagerank;
        }
    }

    @GetMapping("/betweenness/reset")
    public ResponseEntity<Void> resetBetweenness() {
        cachedBet.clear();
        cachedBetStats.clear();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/betweenness/stats")
    public Collection<Map<String, Object>> statsBetweenness() {
        if (cachedBetStats.isEmpty()) {
            cachedBetStats = userService.readBetweennessStats();
        }
        return cachedBetStats;
    }

    @GetMapping("/betweenness")
    public Collection<Map<String, Object>> readBetweenness(@RequestParam(value = "name",required = false) String name) {
        if (cachedBet.isEmpty()) {
            cachedBet = userService.readBetweenness();
        }
        if (StringUtils.hasText(name)) {
            List<Map<String, Object>> filteredForName = new ArrayList<>();
            for (Map<String, Object> map : cachedBet) {
                if ((map.get("name") != null) && map.get("name").toString().equals(name)) {
                    filteredForName.add(map);
                }
            }
            return filteredForName;
        } else {
            return cachedBet;
        }
    }

}
