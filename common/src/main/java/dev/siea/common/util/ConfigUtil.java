package dev.siea.common.util;
import dev.siea.common.Common;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.loader.ConfigurationLoader;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigUtil {
    private CommentedConfigurationNode node;
    private Path configPath;

    public ConfigUtil(Path dir, String path) {
        try {
            configPath = dir.resolve(path);
            // Load the configuration
            ConfigurationLoader<CommentedConfigurationNode> loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();
            if (Files.exists(configPath)) {
                this.node = loader.load();
            } else {
                // If the file doesn't exist, create a new empty configuration
                this.node = loader.createNode();
                saveConfig(loader);
            }

            // If the file exists but is empty, save the default configuration
            if (this.node.empty()) {
                saveConfig(loader);
            }
        } catch (IOException e) {
            Common.getInstance().log("An exception occurred while loading " + e.getMessage());
        }
    }

    private void saveConfig(ConfigurationLoader<CommentedConfigurationNode> loader) {
        try {
            loader.save(this.node);
        } catch (IOException e) {
            Common.getInstance().log("An exception occurred while loading " + e.getMessage());
        }
    }

    private void copyFromResources(Path dir, String path, Class<?> anchorClass) throws IOException {
        try (InputStream is = anchorClass.getResourceAsStream("/" + path)) {
            if (is != null) {
                Files.createDirectories(dir);
                Files.copy(is, dir.resolve(path));
            }
        }
    }

    public CommentedConfigurationNode getNode() {
        return node;
    }

    public void save() {
        try {
            ConfigurationLoader<CommentedConfigurationNode> loader = YamlConfigurationLoader.builder()
                    .path(configPath)
                    .build();
            loader.save(node);
        } catch (IOException e) {
            Common.getInstance().log("An exception occurred while saving " + e.getMessage());
        }
    }
}