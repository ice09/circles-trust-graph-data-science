package tech.blockchainers.circles.graph.circlesstatswebproxy.model;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.voodoodyne.jackson.jsog.JSOGGenerator;
import org.springframework.data.neo4j.core.schema.*;

import java.util.List;

@JsonIdentityInfo(generator = JSOGGenerator.class)
@Node
public class User {

    @Id
    @GeneratedValue
    Long id;

    private String address;

    private String name;
    @Property("image_url")
    private String imageUrl;

    @Relationship(type = "TRUSTS", direction = Relationship.Direction.OUTGOING)
    private List<User> trustees;

    public User() {}

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

    public void setTrustees(List<User> trustees) {
        this.trustees = trustees;
    }

    public List<User> getTrustees() {
        return trustees;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
