package lld.bookmyshow.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name = "show_event") @Getter @Setter @NoArgsConstructor
public class Show {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "movie_name", nullable = false) private String movieName;
    @Column(nullable = false) private String language;
    @Column(name = "screen_name", nullable = false) private String screenName;
    @Column(name = "show_time", nullable = false) private LocalDateTime showTime;
    @Column(name = "duration_minutes", nullable = false) private Integer durationMinutes;
    @Column(name = "total_seats", nullable = false) private Integer totalSeats;
}
