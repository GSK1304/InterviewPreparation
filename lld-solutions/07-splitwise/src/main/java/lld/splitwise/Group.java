package lld.splitwise;
import java.util.*;

public class Group {
    private static int counter = 100;
    private final String      groupId;
    private final String      name;
    private final Set<String> memberIds = new LinkedHashSet<>();

    public Group(String name, String creatorId) {
        this.groupId = "GRP-" + counter++;
        this.name    = Objects.requireNonNull(name);
        if (name.isBlank()) throw new IllegalArgumentException("Group name required");
        memberIds.add(Objects.requireNonNull(creatorId));
    }

    public void addMember(String userId)    { memberIds.add(Objects.requireNonNull(userId)); }
    public void removeMember(String userId) { memberIds.remove(userId); }
    public boolean hasMember(String userId) { return memberIds.contains(userId); }
    public String      getGroupId()  { return groupId; }
    public String      getName()     { return name; }
    public Set<String> getMemberIds(){ return Collections.unmodifiableSet(memberIds); }
}
