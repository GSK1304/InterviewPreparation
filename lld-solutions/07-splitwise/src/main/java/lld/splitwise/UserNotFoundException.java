package lld.splitwise;
public class UserNotFoundException extends SplitwiseException { public UserNotFoundException(String id) { super("User not found: " + id); } }
