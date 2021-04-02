package org.technbolts.keycloak.users;

/**
 * @author <a href="http://twitter.com/aloyer">@aloyer</a>
 */
public class SearchCriteria {
    private String search;
    private String username;
    private String firstname;
    private String lastname;
    private String email;
    private boolean enabled;
    private String groupId;

    public String search() {
        return search;
    }

    public SearchCriteria withSearch(String search) {
        this.search = search;
        return this;
    }

    public String username() {
        return username;
    }

    public SearchCriteria withUsername(String username) {
        this.username = username;
        return this;
    }

    public String firstname() {
        return firstname;
    }

    public SearchCriteria withFirstname(String firstname) {
        this.firstname = firstname;
        return this;
    }

    public String lastname() {
        return lastname;
    }

    public SearchCriteria withLastname(String lastname) {
        this.lastname = lastname;
        return this;
    }

    public String email() {
        return email;
    }

    public SearchCriteria withEmail(String email) {
        this.email = email;
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public SearchCriteria withEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public String groupId() {
        return groupId;
    }

    public SearchCriteria withGroupId(String groupId) {
        this.groupId = groupId;
        return this;
    }
}
