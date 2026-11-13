package scasp.jdbc;

/** Unchecked wrapper for SQL failures. Details are logged by callers, never shown to users. */
public class StoreException extends RuntimeException {
    public StoreException(Throwable cause) { super("Database error", cause); }
}
