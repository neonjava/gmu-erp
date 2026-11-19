package in.neonjava.erp.model;

import jakarta.persistence.*;

/** A bookable campus resource: lab, classroom, seminar hall or piece of equipment. */
@Entity
@Table(name = "campus_resources")
public class CampusResource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** For rooms this matches the room name used in the timetable (e.g. LAB-2). */
    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResourceType type = ResourceType.LAB;

    private String location;

    /** Seats for rooms, number of units for equipment. */
    private int capacity;

    @Column(length = 500)
    private String description;

    private boolean active = true;

    public CampusResource() {}

    public CampusResource(String name, ResourceType type, String location, int capacity, String description) {
        this.name = name;
        this.type = type;
        this.location = location;
        this.capacity = capacity;
        this.description = description;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ResourceType getType() { return type; }
    public void setType(ResourceType type) { this.type = type; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
