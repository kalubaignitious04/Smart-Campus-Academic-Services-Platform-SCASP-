package scasp.web;

/** Settings come from -D system properties or environment variables, with safe local defaults. */
public final class AppConfig {
    private AppConfig() {}
    public static String get(String key, String def) {
        String v = System.getProperty(key);
        if (v == null) v = System.getenv(key.replace('.', '_').toUpperCase());
        return v == null ? def : v;
    }
    public static int getInt(String key, int def) { return Integer.parseInt(get(key, String.valueOf(def))); }
}
