package lld.hotel.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "guest") @Getter @Setter @NoArgsConstructor
public class Guest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String phone;
    @Column(name = "id_proof", nullable = false) private String idProof;
}
