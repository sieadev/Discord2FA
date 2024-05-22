package dev.siea.common.util;

import dev.dejvokep.boostedyaml.YamlDocument;
import dev.dejvokep.boostedyaml.dvs.versioning.BasicVersioning;
import dev.dejvokep.boostedyaml.settings.dumper.DumperSettings;
import dev.dejvokep.boostedyaml.settings.general.GeneralSettings;
import dev.dejvokep.boostedyaml.settings.loader.LoaderSettings;
import dev.dejvokep.boostedyaml.settings.updater.UpdaterSettings;
import dev.siea.common.Common;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;

public class ConfigUtil {
    private final File file;
    private YamlDocument config;

    public ConfigUtil(Path dir, String path) {
        System.out.println("Test");
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
        } catch (Exception ignore) {
            Common.getInstance().log("Failed to load config file: " + path);
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