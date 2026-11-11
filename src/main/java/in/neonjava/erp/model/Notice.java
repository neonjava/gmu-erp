package in.neonjava.erp.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String body;

    private LocalDate postedOn = LocalDate.now();

    public Notice() {}

    public Notice(String title, String body) {
        this.title = title;
        this.body = body;
    }

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public LocalDate getPostedOn() { return postedOn; }
    public void setPostedOn(LocalDate postedOn) { this.postedOn = postedOn; }
}
