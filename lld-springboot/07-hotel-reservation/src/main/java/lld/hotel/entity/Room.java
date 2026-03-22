package lld.hotel.entity;
import jakarta.persistence.*;
import lld.hotel.enums.BedType;
import lld.hotel.enums.RoomType;
import lombok.*;
@Entity @Table(name = "room") @Getter @Setter @NoArgsConstructor
public class Room {
    @Id @Column(name = "room_number") private String roomNumber;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private RoomType type;
    @Enumerated(EnumType.STRING) @Column(name = "bed_type", nullable = false) private BedType bedType;
    @Column(nullable = false) private Integer capacity;
    @Column(name = "floor_number", nullable = false) private Integer floorNumber;
    @Column(name = "base_rate_paise", nullable = false) private Long baseRatePaise;
}
