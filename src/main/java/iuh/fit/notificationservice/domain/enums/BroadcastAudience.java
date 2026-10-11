package iuh.fit.notificationservice.domain.enums;

import java.util.Set;

public enum BroadcastAudience {
    ALL(Set.of("PLAYER", "VENUE_OWNER")),
    PLAYER(Set.of("PLAYER")),
    VENUE_OWNER(Set.of("VENUE_OWNER"));

    private final Set<String> roles;

    BroadcastAudience(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> roles() {
        return roles;
    }
}
