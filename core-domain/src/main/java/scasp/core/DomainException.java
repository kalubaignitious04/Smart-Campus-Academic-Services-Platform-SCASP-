package scasp.core;

/** Business-rule failure with a user-safe message. */
public class DomainException extends RuntimeException {
    public enum Kind { INVALID, NOT_FOUND, FORBIDDEN, CONFLICT }
    private final Kind kind;
    public DomainException(Kind kind, String message) { super(message); this.kind = kind; }
    public Kind kind() { return kind; }
}
