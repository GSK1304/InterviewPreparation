package lld.bookmyshow.repository;
import lld.bookmyshow.entity.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Repository
public interface ShowRepository extends JpaRepository<Show, Long> {
    List<Show> findByMovieNameIgnoreCaseAndShowTimeBetweenOrderByShowTime(
        String movie, LocalDateTime from, LocalDateTime to);
}
