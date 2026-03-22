package lld.library;
import java.util.Objects;
public class BookCopy {
    private final String     barcode;
    private final ISBN       isbn;
    private BookStatus       status;

    public BookCopy(String barcode, ISBN isbn) {
        this.barcode = Objects.requireNonNull(barcode);
        this.isbn    = Objects.requireNonNull(isbn);
        this.status  = BookStatus.AVAILABLE;
        if (barcode.isBlank()) throw new IllegalArgumentException("Barcode cannot be blank");
    }

    public String     getBarcode() { return barcode; }
    public ISBN       getIsbn()    { return isbn; }
    public BookStatus getStatus()  { return status; }
    public void       setStatus(BookStatus s) { this.status = Objects.requireNonNull(s); }
    public boolean    isAvailable(){ return status == BookStatus.AVAILABLE; }
}
