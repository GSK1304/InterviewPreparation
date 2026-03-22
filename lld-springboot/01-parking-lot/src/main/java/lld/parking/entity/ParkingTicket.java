package lld.parking.entity;

import jakarta.persistence.*;
import lld.parking.enums.VehicleType;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "parking_ticket")
@Getter @Setter @NoArgsConstructor
public class ParkingTicket {

    @Id
    @Column(name = "ticket_id")
    private String ticketId;

    @Column(name = "license_plate", nullable = false)
    private String licensePlate;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false)
    private VehicleType vehicleType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "spot_id", nullable = false)
    private ParkingSpot spot;

    @Column(name = "entry_time", nullable = false)
    private Instant entryTime;

    @Column(name = "exit_time")
    private Instant exitTime;

    @Column(name = "fee_paise")
    private Long feePaise;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}
