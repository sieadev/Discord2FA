package dev.siea.common.util;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class ConfigUtil {
    private final File file;
    private YamlDocument config;

    public ConfigUtil(Path dir, String path) {
        try {
            config = YamlDocument.create(new File(dir.toFile(), path),
                    Objects.requireNonNull(getClass().getResourceAsStream("/" + path)),
                    GeneralSettings.DEFAULT,
                    LoaderSettings.builder().setAutoUpdate(true).build(),
                    DumperSettings.DEFAULT,
                    UpdaterSettings.builder().setVersioning(new BasicVersioning("file-version"))
                            .setOptionSorting(UpdaterSettings.OptionSorting.SORT_BY_DEFAULTS).build());

            config.update();
            config.save();
        } catch (IOException ignore) {
        }
        this.file = new File(path);
    }

    public void save() {
        try {
            this.config.save(this.file);
        } catch (Exception ignore) {
        }
    }

    public YamlDocument getConfig() {
        return this.config;
    }
}