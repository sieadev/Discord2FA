package dev.siea.discord2fa.gameserver.adapter;

import dev.siea.discord2fa.common.config.ConfigAdapter;

import java.util.Collections;
import java.util.List;

public class GameServerConfigAdapter implements ConfigAdapter {
    @Override
    public String getString(String key) {
        return "";
    }

    @Override
    public int getInt(String key) {
        return 0;
    }

    @Override
    public boolean getBoolean(String key) {
        return false;
    }

    @Override
    public List<String> getStringList(String key) {
        return Collections.emptyList();
    }
}
