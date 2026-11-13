package scasp.web;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotFoundException;
import java.lang.reflect.Method;

/**
 * Lets JSP expressions read Java records (${user.role}) and no-argument getters such as seatsLeft().
 * Tomcat 10.1's expression language only knows JavaBean getX() methods, and our domain model uses records.
 */
public class RecordELResolver extends ELResolver {
    @Override public Object getValue(ELContext ctx, Object base, Object property) {
        if (base == null || !(property instanceof String name) || !base.getClass().isRecord() && !hasPlain(base, name)) return null;
        try {
            Method m = base.getClass().getMethod(name);
            if (m.getParameterCount() != 0) return null;
            m.setAccessible(true);
            Object v = m.invoke(base);
            ctx.setPropertyResolved(base, property);
            return v;
        } catch (NoSuchMethodException e) {
            if (base.getClass().isRecord()) throw new PropertyNotFoundException("No property " + name + " on " + base.getClass().getSimpleName());
            return null;
        } catch (ReflectiveOperationException e) {
            throw new jakarta.el.ELException(e);
        }
    }
    private static boolean hasPlain(Object base, String name) { return false; }   // only records are resolved here
    @Override public Class<?> getType(ELContext ctx, Object base, Object property) { return null; }
    @Override public void setValue(ELContext ctx, Object base, Object property, Object value) { }
    @Override public boolean isReadOnly(ELContext ctx, Object base, Object property) { return true; }
    @Override public Class<?> getCommonPropertyType(ELContext ctx, Object base) { return Object.class; }
}
