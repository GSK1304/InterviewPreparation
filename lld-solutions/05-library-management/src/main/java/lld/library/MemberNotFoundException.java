package lld.library;
public class MemberNotFoundException extends LibraryException {
    public MemberNotFoundException(String id) { super("Member not found: " + id); }
}
