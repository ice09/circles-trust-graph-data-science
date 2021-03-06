package tech.blockchainers.circles.graph.circlesstatswebproxy.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.voodoodyne.jackson.jsog.JSOGGenerator;
import org.springframework.data.neo4j.core.schema.*;

import java.util.List;

@Node
@JsonIdentityInfo(generator = JSOGGenerator.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FlatUser {

    @Id
    @GeneratedValue
    @JsonIgnore
    private Long id;

    private String address;

    private String name;

    private String imageUrl;

    @Relationship(type = "TRUSTS", direction = Relationship.Direction.OUTGOING)
    private List<FlatUser> trustees;
    @Relationship(type = "TRUSTS", direction = Relationship.Direction.INCOMING)
    private List<FlatUser> trusters;

    public FlatUser(String address, List<FlatUser> trusters) {
        this.address = address;
        this.trusters = trusters;
    }
    public FlatUser(String address, String name, List<FlatUser> trusters) {
        this.address = address;
        this.name = name;
        this.trusters = trusters;
    }
    public FlatUser(String address, String name) {
        this.address = address;
        this.name = name;
    }
    public FlatUser(String address, String name, String imageUrl, List<FlatUser> trusters) {
        this.address = address;
        this.name = name;
        this.imageUrl = ((imageUrl == null) || ("null".equals(imageUrl))) ? null : imageUrl;
        this.trusters = trusters;
    }
    public FlatUser(String address, String name, String imageUrl) {
        this.address = address;
        this.name = name;
        this.imageUrl = ((imageUrl == null) || ("null".equals(imageUrl))) ? null : imageUrl;
    }

    public FlatUser(String name) {
        this.name = name;
    }

    public FlatUser() {}

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setTrustees(List<FlatUser> trustees) {
        this.trustees = trustees;
    }

    public List<FlatUser> getTrustees() {
        return trustees;
    }

    public void setTrusters(List<FlatUser> trusters) {
        this.trusters = trusters;
    }

    public List<FlatUser> getTrusters() {
        return trusters;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
