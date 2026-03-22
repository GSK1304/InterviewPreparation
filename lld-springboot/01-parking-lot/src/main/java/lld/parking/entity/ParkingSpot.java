package lld.parking.entity;

import jakarta.persistence.*;
import lld.parking.enums.SpotSize;
import lld.parking.enums.SpotStatus;
import lld.parking.enums.VehicleType;
import lombok.*;

@Entity
@Table(name = "parking_spot",
       indexes = @Index(name = "idx_spot_status_size", columnList = "status, size"))
@Getter @Setter @NoArgsConstructor
public class ParkingSpot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "spot_code", nullable = false, unique = true)
    private String spotCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotSize size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpotStatus status = SpotStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "floor_id", nullable = false)
    private ParkingFloor floor;

    public boolean isAvailable()                    { return status == SpotStatus.AVAILABLE; }
    public boolean canFit(VehicleType vehicleType)  { return vehicleType.fitsIn(size); }
}
