package club.polarite.normalizer.config;

import club.polarite.normalizer.util.VersionTool;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;

import net.minecraft.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Config screen
 * Localizations in resources/assets/normalizer/lang/*.json
 */
public class NormalizerConfigScreen {
    public static Screen create(Screen parent) {
        NormalizerConfig config = ConfigManager.getConfig();
        String mcVersion = SharedConstants.getCurrentVersion().name(); // .getName() in 1.21.4-5, automatically gonna be changed at build-time by gradle 👍️
        String installedVersion = VersionTool.getInstalledVersion();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.normalizer.title"))
                .setSavingRunnable(ConfigManager::saveConfig);

        ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.normalizer.category.general"));
        ConfigCategory servers = builder.getOrCreateCategory(Component.translatable("config.normalizer.category.servers"));
        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.normalizer.option.restoreSneakingHitbox"),
                        config.restoreSneakingHitbox
                )
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.normalizer.tooltip.restoreSneakingHitbox"))
                .setSaveConsumer(value -> config.restoreSneakingHitbox = value)
                .build()
        );

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.normalizer.option.disableSwimming"),
                        config.disableSwimming
                )
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.normalizer.tooltip.disableSwimming"))
                .setSaveConsumer(value -> config.disableSwimming = value)
                .build()
        );

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.normalizer.option.disableCrawling"),
                        config.disableCrawling
                )
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.normalizer.tooltip.disableCrawling"))
                .setSaveConsumer(value -> config.disableCrawling = value)
                .build()
        );

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.normalizer.option.disableBedBounce"),
                        config.disableBedBounce
                )
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.normalizer.tooltip.disableBedBounce"))
                .setSaveConsumer(value -> config.disableBedBounce = value)
                .build()
        );

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.normalizer.option.fixSneakDesync"),
                        config.fixSneakDesync
                )
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.normalizer.tooltip.fixSneakDesync"))
                .setSaveConsumer(value -> config.fixSneakDesync = value)
                .build()
        );

        general.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.normalizer.option.restoreLegacyBuckets"),
                        config.restoreLegacyBuckets
                )
                .setDefaultValue(false)
                .setTooltip(Component.translatable("config.normalizer.tooltip.restoreLegacyBuckets"))
                .setSaveConsumer(value -> config.restoreLegacyBuckets = value)
                .requireRestart()
                .build()
        );

        if (!Objects.equals(mcVersion, "1.21.4")) {
            general.addEntry(entryBuilder
                    .startBooleanToggle(
                            Component.translatable("config.normalizer.option.restoreSprintCancel"),
                            config.restoreSprintCancel
                    )
                    .setDefaultValue(true)
                    .setTooltip(Component.translatable("config.normalizer.tooltip.restoreSprintCancel"))
                    .setSaveConsumer(value -> config.restoreSprintCancel = value)
                    .build()
            );
        }

        servers.addEntry(entryBuilder
                .startStrList(
                        Component.translatable("config.normalizer.option.serverWhitelist"),
                        config.serverWhitelist
                )
                .setDefaultValue(Arrays.asList("*.hypixel.net"))
                .setTooltip(Component.translatable("config.normalizer.tooltip.serverWhitelist"))
                .setSaveConsumer(list -> config.serverWhitelist = list)
                .build()
        );

        servers.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.normalizer.option.multiplayerOnly"),
                        config.multiplayerOnly
                )
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.normalizer.tooltip.multiplayerOnly"))
                .setSaveConsumer(value -> config.multiplayerOnly = value)
                .build()
        );

        servers.addEntry(entryBuilder
                .startBooleanToggle(
                        Component.translatable("config.normalizer.option.fixSneakDesyncWarning"),
                        config.fixSneakDesyncWarning
                )
                .setDefaultValue(true)
                .setTooltip(Component.translatable("config.normalizer.tooltip.fixSneakDesyncWarning"))
                .setSaveConsumer(value -> config.fixSneakDesyncWarning = value)
                .build()
        );

        if (!ConfigManager.updateMessageClosed) {
            CompletableFuture
                    .supplyAsync(VersionTool::fetchLatestVersion)
                    .thenAccept(latestRemoteTag -> {
                        if (!latestRemoteTag.isEmpty() && !installedVersion.equals(latestRemoteTag)) {
                            Minecraft.getInstance().execute(() -> {
                                Minecraft.getInstance().setScreen(new NewUpdateScreen(builder.build(), installedVersion, latestRemoteTag));
                            });
                        }
                    });
        }

        return builder.build();
    }

    /**
     * The screen that shows up when a newer version of the mod is available
     */
    @Environment(EnvType.CLIENT)
    private static class NewUpdateScreen extends Screen {
        private final Screen parent;
        private final String installed;
        private final String latest;

        private static final int buttonWidth = 200;
        private static final int buttonHeight = 20;
        private static final int spacing = 5;

        private NewUpdateScreen(Screen parent, String installed, String latest) {
            super(Component.translatable("config.normalizer.update.text"));
            this.parent = parent;
            this.installed = installed;
            this.latest = latest;
        }

        @Override
        protected void init() {
            super.init();

            record Row(Component text, @Nullable Button.OnPress action) {
            }

            List<Row> rows = List.of(
                    new Row(
                            Component.translatable("config.normalizer.update.title"),
                            null
                    ),
                    new Row(
                            Component.translatable("config.normalizer.update.text"),
                            null
                    ),
                    new Row(
                            Component.literal("§b" + installed + " §7→ §b" + latest),
                            null
                    ),
                    new Row(
                            Component.translatable("config.normalizer.update.getLatestButton"),
                            btn -> Util.getPlatform().openUri("https://modrinth.com/mod/normalizer/version/" + latest)
                    ),
                    new Row(
                            Component.literal("OK"),
                            btn -> Minecraft.getInstance().setScreen(parent)
                    )
            );

            int totalHeight = rows.stream()
                    .mapToInt(row -> row.action == null ? font.lineHeight : buttonHeight)
                    .sum()
                    + spacing * (rows.size() - 1);

            int centerX = width / 2;
            int currentY = (height - totalHeight) / 2;

            for (Row row : rows) {
                if (row.action == null) {
                    int textWidth = font.width(row.text.getString());
                    addRenderableWidget(new MultiLineTextWidget(
                            centerX - textWidth / 2,
                            currentY,
                            row.text,
                            font
                    ));
                    currentY += font.lineHeight + spacing;
                } else {
                    addRenderableWidget(Button.builder(row.text, row.action)
                            .bounds(
                                    centerX - buttonWidth / 2,
                                    currentY,
                                    buttonWidth,
                                    buttonHeight
                            )
                            .build()
                    );
                    currentY += buttonHeight + spacing;
                }
            }

            ConfigManager.updateMessageClosed = true;
        }

        @Override
        public void onClose() {
            Minecraft.getInstance().setScreen(parent);
        }
    }
}