package lld.cabooking.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "rider") @Getter @Setter @NoArgsConstructor
public class Rider {
    @Id @Column(name = "rider_id") private String riderId;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String phone;
    @Column(nullable = false, unique = true) private String email;
}
