package gr.taxpulse.user.entity;

/**
 * Application roles (RBAC).
 * <ul>
 *   <li>{@code ADMIN} - office owner: manages users and can delete data.</li>
 *   <li>{@code ACCOUNTANT} - full read/write on clients, obligations and documents.</li>
 *   <li>{@code ASSISTANT} - read access plus status updates on assigned work.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    ACCOUNTANT,
    ASSISTANT;

    /** Spring Security authority name, e.g. {@code ROLE_ADMIN}. */
    public String authority() {
        return "ROLE_" + name();
    }
}
