package dev.siea.discord2fa.common.testutil;

import dev.siea.discord2fa.common.config.ConfigAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** In-memory {@link ConfigAdapter} keyed by dotted paths, e.g. {@code database.type}. */
public final class MapConfig implements ConfigAdapter {

    private final Map<String, Object> values = new HashMap<>();

    public MapConfig set(String key, Object value) {
        values.put(key, value);
        return this;
    }

    @Override
    public String getString(String key) {
        Object v = values.get(key);
        return v == null ? null : v.toString();
    }

    @Override
    public int getInt(String key) {
        Object v = values.get(key);
        return v instanceof Number n ? n.intValue() : 0;
    }

    @Override
    public boolean getBoolean(String key) {
        return Boolean.TRUE.equals(values.get(key));
    }

    @Override
    public List<String> getStringList(String key) {
        Object v = values.get(key);
        if (!(v instanceof List<?> list)) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (Object o : list) out.add(o == null ? null : o.toString());
        return out;
    }
}
