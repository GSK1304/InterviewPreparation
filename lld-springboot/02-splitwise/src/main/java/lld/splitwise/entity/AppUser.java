package lld.splitwise.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "app_user") @Getter @Setter @NoArgsConstructor
public class AppUser {
    @Id @Column(name = "user_id") private String userId;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Column(nullable = false) private String phone;
}
