package lld.atm.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name = "cash_cassette") @Getter @Setter @NoArgsConstructor
public class CashCassette {
    @Id @Column(name = "denomination") private Integer denomination;
    @Column(name = "note_count", nullable = false) private Integer noteCount;
}
