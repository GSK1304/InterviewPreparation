package lld.library.service;
import jakarta.transaction.Transactional;
import lld.library.dto.*;
import lld.library.entity.*;
import lld.library.enums.*;
import lld.library.exception.LibraryException;
import lld.library.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class LibraryService {
    private static final Logger log = LoggerFactory.getLogger(LibraryService.class);
    private static final double FINE_PER_DAY_RUPEES = 2.0;

    private final BookRepository       bookRepo;
    private final BookCopyRepository   copyRepo;
    private final MemberRepository     memberRepo;
    private final LoanRepository       loanRepo;
    private final ReservationRepository reservationRepo;

    @Transactional
    public Member registerMember(RegisterMemberRequest req) {
        log.info("[LibraryService] Register member | id={} type={}", req.getMemberId(), req.getType());
        if (memberRepo.existsById(req.getMemberId())) throw new LibraryException("Member already exists: " + req.getMemberId(), HttpStatus.CONFLICT);
        if (memberRepo.existsByEmail(req.getEmail())) throw new LibraryException("Email already registered: " + req.getEmail(), HttpStatus.CONFLICT);
        Member m = new Member(); m.setMemberId(req.getMemberId()); m.setName(req.getName()); m.setEmail(req.getEmail()); m.setType(req.getType());
        memberRepo.save(m);
        log.info("[LibraryService] Member registered | id={} borrowLimit={} loanDays={}", m.getMemberId(), m.getBorrowLimit(), m.getLoanDurationDays());
        return m;
    }

    @Transactional
    public Map<String,Object> borrowBook(BorrowRequest req) {
        log.info("[LibraryService] Borrow request | memberId={} isbn={}", req.getMemberId(), req.getIsbn());
        Member member = getMember(req.getMemberId());
        if (member.hasFine()) throw new LibraryException("Outstanding fine of Rs." + member.getOutstandingFinePaise()/100.0 + ". Please pay before borrowing.", HttpStatus.CONFLICT);
        long activeLoans = loanRepo.countByMemberMemberIdAndStatus(req.getMemberId(), LoanStatus.ACTIVE);
        if (activeLoans >= member.getBorrowLimit()) throw new LibraryException("Borrow limit reached: " + member.getBorrowLimit(), HttpStatus.CONFLICT);
        BookCopy copy = copyRepo.findFirstByBookIsbnAndStatus(req.getIsbn(), CopyStatus.AVAILABLE)
            .orElseThrow(() -> new LibraryException("No available copy for ISBN: " + req.getIsbn(), HttpStatus.NOT_FOUND));
        copyRepo.updateStatus(copy.getBarcode(), CopyStatus.BORROWED);
        Loan loan = new Loan(); loan.setMember(member); loan.setCopy(copy);
        loan.setBorrowDate(LocalDate.now()); loan.setDueDate(LocalDate.now().plusDays(member.getLoanDurationDays()));
        loanRepo.save(loan);
        log.info("[LibraryService] Book borrowed | loanId={} barcode={} dueDate={}", loan.getId(), copy.getBarcode(), loan.getDueDate());
        return Map.of("loanId",loan.getId(),"barcode",copy.getBarcode(),"title",copy.getBook().getTitle(),"dueDate",loan.getDueDate().toString(),"message","Book borrowed successfully");
    }

    @Transactional
    public Map<String,Object> returnBook(ReturnRequest req) {
        log.info("[LibraryService] Return request | barcode={}", req.getBarcode());
        Loan loan = loanRepo.findAll().stream().filter(l -> l.getCopy().getBarcode().equals(req.getBarcode()) && l.getStatus() == LoanStatus.ACTIVE)
            .findFirst().orElseThrow(() -> new LibraryException("No active loan for barcode: " + req.getBarcode(), HttpStatus.NOT_FOUND));
        loan.setReturnDate(LocalDate.now());
        long finePaise = 0;
        if (LocalDate.now().isAfter(loan.getDueDate())) {
            long overdueDays = java.time.temporal.ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
            finePaise = Math.round(overdueDays * FINE_PER_DAY_RUPEES * 100);
            loan.setStatus(LoanStatus.OVERDUE); loan.setFinePaise(finePaise);
            loan.getMember().setOutstandingFinePaise(loan.getMember().getOutstandingFinePaise() + finePaise);
            log.warn("[LibraryService] Overdue return | barcode={} overdueDays={} fine=Rs.{}", req.getBarcode(), overdueDays, finePaise/100.0);
        } else {
            loan.setStatus(LoanStatus.RETURNED);
        }
        copyRepo.updateStatus(req.getBarcode(), CopyStatus.AVAILABLE);
        loanRepo.save(loan);
        // Notify next in reservation queue
        reservationRepo.findFirstByIsbnAndActiveTrueOrderByReservedDateAsc(loan.getCopy().getBook().getIsbn()).ifPresent(res -> {
            log.info("[LibraryService] Notifying reservation | memberId={} isbn={}", res.getMember().getMemberId(), res.getIsbn());
            res.setActive(false); reservationRepo.save(res);
        });
        return Map.of("barcode",req.getBarcode(),"fine",finePaise > 0 ? "Rs."+finePaise/100.0 : "None","status",loan.getStatus().name(),"message","Book returned successfully");
    }

    @Transactional
    public Map<String,Object> payFine(String memberId, PayFineRequest req) {
        Member m = getMember(memberId);
        long payPaise = Math.round(req.getAmountRupees() * 100);
        if (payPaise > m.getOutstandingFinePaise()) throw new LibraryException("Payment Rs."+req.getAmountRupees()+" exceeds outstanding fine Rs."+m.getOutstandingFinePaise()/100.0, HttpStatus.BAD_REQUEST);
        m.setOutstandingFinePaise(m.getOutstandingFinePaise() - payPaise);
        memberRepo.save(m);
        log.info("[LibraryService] Fine paid | memberId={} paid=Rs.{} remaining=Rs.{}", memberId, req.getAmountRupees(), m.getOutstandingFinePaise()/100.0);
        return Map.of("memberId",memberId,"paid","Rs."+req.getAmountRupees(),"outstandingFine","Rs."+m.getOutstandingFinePaise()/100.0);
    }

    public List<Book> searchBooks(String title, String author, String genre) {
        if (title  != null) return bookRepo.findByTitleContainingIgnoreCase(title);
        if (author != null) return bookRepo.findByAuthorContainingIgnoreCase(author);
        if (genre  != null) return bookRepo.findByGenreIgnoreCase(genre);
        return bookRepo.findAll();
    }

    private Member getMember(String id) { return memberRepo.findById(id).orElseThrow(() -> new LibraryException("Member not found: "+id, HttpStatus.NOT_FOUND)); }
}
