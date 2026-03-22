package lld.library.entity;
import jakarta.persistence.*;
import lld.library.enums.MemberType;
import lombok.*;
@Entity @Table(name = "member") @Getter @Setter @NoArgsConstructor
public class Member {
    @Id @Column(name = "member_id") private String memberId;
    @Column(nullable = false) private String name;
    @Column(nullable = false, unique = true) private String email;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private MemberType type;
    @Column(name = "outstanding_fine_paise", nullable = false) private Long outstandingFinePaise = 0L;
    public int getBorrowLimit()     { return switch (type) { case FACULTY -> 10; case STUDENT -> 3; case PUBLIC -> 2; }; }
    public int getLoanDurationDays(){ return switch (type) { case FACULTY -> 30; case STUDENT -> 14; case PUBLIC -> 7; }; }
    public boolean hasFine()        { return outstandingFinePaise > 0; }
}
