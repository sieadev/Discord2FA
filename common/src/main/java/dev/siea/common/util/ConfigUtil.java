package dev.siea.common.util;
import dev.siea.common.Common;
import org.simpleyaml.configuration.file.FileConfiguration;
import org.simpleyaml.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigUtil {
    private final File file;
    private final FileConfiguration config;

    public ConfigUtil(Path configDir, String configFileName) {
        this(configDir + "/" + configFileName);
    }

    public ConfigUtil(String path) {
        path = path.replace("discord2fa", "Discord2FA");
        this.file = new File(path);
        try {
            if (!this.file.exists()) {
                InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(this.file.getName());
                if (resourceStream != null) {
                    Files.copy(resourceStream, this.file.toPath());
                } else {
                    this.file.createNewFile();
                }
            }
            this.config = YamlConfiguration.loadConfiguration(this.file);
        } catch (IOException e) {
            Common.getInstance().log("Failed to load config file: " + file.getAbsolutePath());
            throw new RuntimeException(e);
        }
    }


    public boolean save() {
        try {
            this.config.save(this.file);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }

    public File getFile() {
        return this.file;
    }

    public FileConfiguration getConfig() {
        return this.config;
    }
}