package gr.taxpulse.user.entity;

/**
 * Application roles (RBAC).
 * <ul>
 *   <li>{@code ADMIN} - office owner: manages users and can delete data.</li>
 *   <li>{@code ACCOUNTANT} - full read/write on clients, obligations and documents.</li>
 *   <li>{@code ASSISTANT} - read access plus status updates on assigned work.</li>
 *   <li>{@code CLIENT} - a client of the office, limited to the client portal and its own data.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    ACCOUNTANT,
    ASSISTANT,
    CLIENT;

    /** Office staff, as opposed to portal users. */
    public boolean isStaff() {
        return this != CLIENT;
    }

    /** Spring Security authority name, e.g. {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
