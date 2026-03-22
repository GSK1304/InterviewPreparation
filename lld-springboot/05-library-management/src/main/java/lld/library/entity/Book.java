package lld.library.entity;
import jakarta.persistence.*;
import lombok.*;
import java.util.*;
@Entity @Table(name = "book") @Getter @Setter @NoArgsConstructor
public class Book {
    @Id @Column(name = "isbn") private String isbn;
    @Column(nullable = false) private String title;
    @Column(nullable = false) private String author;
    @Column(nullable = false) private String genre;
    @Column(name = "published_year") private Integer publishedYear;
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BookCopy> copies = new ArrayList<>();
}
