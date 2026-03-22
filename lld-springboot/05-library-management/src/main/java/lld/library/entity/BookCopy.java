package lld.library.entity;
import jakarta.persistence.*;
import lld.library.enums.CopyStatus;
import lombok.*;
@Entity @Table(name = "book_copy") @Getter @Setter @NoArgsConstructor
public class BookCopy {
    @Id @Column(name = "barcode") private String barcode;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "isbn", nullable = false) private Book book;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private CopyStatus status = CopyStatus.AVAILABLE;
}
