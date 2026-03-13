package dev.siea.discord2fa.velocity.adapter;

import dev.siea.discord2fa.common.config.ConfigAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** ConfigAdapter backed by a Map (e.g. from YAML). Supports dotted keys like "server.verification". */
public final class VelocityConfigAdapter implements ConfigAdapter {

    @SuppressWarnings("unchecked")
    private static Object get(Map<String, Object> map, String key) {
        if (map == null || key == null || key.isEmpty()) return null;
        int dot = key.indexOf('.');
        if (dot < 0) return map.get(key);
        String head = key.substring(0, dot);
        String tail = key.substring(dot + 1);
        Object val = map.get(head);
        if (val instanceof Map) return get((Map<String, Object>) val, tail);
        return null;
    }

    private final Map<String, Object> root;

    public VelocityConfigAdapter(Map<String, Object> root) {
        this.root = root != null ? root : Collections.emptyMap();
    }

    @Override
    public String getString(String key) {
        Object o = get(root, key);
        return o != null ? o.toString() : null;
    }

    @Override
    public int getInt(String key) {
        Object o = get(root, key);
        if (o == null) return 0;
        if (o instanceof Number) return ((Number) o).intValue();
        try { return Integer.parseInt(o.toString()); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public boolean getBoolean(String key) {
        Object o = get(root, key);
        if (o == null) return false;
        if (o instanceof Boolean) return (Boolean) o;
        return Boolean.parseBoolean(o.toString());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getStringList(String key) {
        Object o = get(root, key);
        if (o == null) return Collections.emptyList();
        if (o instanceof List) {
            List<String> out = new ArrayList<>();
            for (Object item : (List<?>) o) if (item != null) out.add(item.toString());
            return out;
        }
        return Collections.emptyList();
    }
}
