package lld.library.controller;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lld.library.dto.*;
import lld.library.entity.*;
import lld.library.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/v1/library") @RequiredArgsConstructor
@Tag(name = "Library Management", description = "Book borrowing with member types, borrow limits, and fine calculation")
public class LibraryController {
    private static final Logger log = LoggerFactory.getLogger(LibraryController.class);
    private final LibraryService service;

    @PostMapping("/members")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a member", description = "STUDENT: 3 books/14days, FACULTY: 10 books/30days, PUBLIC: 2 books/7days")
    public Member registerMember(@Valid @RequestBody RegisterMemberRequest req) { return service.registerMember(req); }

    @PostMapping("/borrow")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Borrow a book")
    public Map<String,Object> borrowBook(@Valid @RequestBody BorrowRequest req) {
        log.info("[LibraryController] POST /borrow | memberId={} isbn={}", req.getMemberId(), req.getIsbn());
        return service.borrowBook(req);
    }

    @PostMapping("/return")
    @Operation(summary = "Return a book", description = "Calculates fine if overdue (Rs.2/day). Notifies reservation queue.")
    public Map<String,Object> returnBook(@Valid @RequestBody ReturnRequest req) {
        log.info("[LibraryController] POST /return | barcode={}", req.getBarcode());
        return service.returnBook(req);
    }

    @PostMapping("/members/{memberId}/pay-fine")
    @Operation(summary = "Pay outstanding fine")
    public Map<String,Object> payFine(@PathVariable String memberId, @Valid @RequestBody PayFineRequest req) { return service.payFine(memberId, req); }

    @GetMapping("/books/search")
    @Operation(summary = "Search books by title, author or genre")
    public List<Book> searchBooks(@RequestParam(required=false) String title, @RequestParam(required=false) String author, @RequestParam(required=false) String genre) {
        return service.searchBooks(title, author, genre);
    }
}
