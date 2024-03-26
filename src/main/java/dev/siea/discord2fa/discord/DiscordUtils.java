package dev.siea.discord2fa.discord;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.storage.StorageManager;
import dev.siea.discord2fa.storage.database.Database;
import dev.siea.discord2fa.storage.models.Account;
import dev.siea.discord2fa.manager.LinkManager;
import dev.siea.discord2fa.manager.VerifyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.awt.*;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.Objects;

public class DiscordUtils extends ListenerAdapter {
    private static final ShardManager shardManager = DiscordBot.getShardManager();
    private static final TextChannel channel = shardManager.getTextChannelById(Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("discord.channel")));
    private static final String codeMessage = Discord2FA.getPlugin().getConfig().getString("messages.codeMessage");
    private static final String alreadyLinking = Discord2FA.getPlugin().getConfig().getString("messages.alreadyLinking");
    private static final String acceptMessage = Discord2FA.getPlugin().getConfig().getString("messages.acceptMessage");
    private static final String denyMessage = Discord2FA.getPlugin().getConfig().getString("messages.denyMessage");

    public DiscordUtils() {
    }

    public static void init() {
        Plugin plugin = Discord2FA.getPlugin();
        String title = plugin.getConfig().getString("messages.link.title");
        String text = plugin.getConfig().getString("messages.link.text");
        String footer = plugin.getConfig().getString("messages.link.footer");
        String button = plugin.getConfig().getString("messages.link.linkButton");
        sendLinkMessage(title, text, footer, button);
    }

    private static void sendLinkMessage(String title, String text, String footer, String button) {
        assert channel != null;
        channel.purgeMessages(channel.getHistory().retrievePast(100).complete());
        if (title == null) {
            title = "Link your account!";
        }
        if (text == null) {
            text = "Click the button below to Link your account!";
        }
        if (footer == null) {
            footer = "Discord2FA";
        }
        if (button == null) {
            button = "Link";
        }
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(text);
        embedBuilder.setFooter(footer);
        embedBuilder.setColor(Color.GREEN);
        channel.sendMessageEmbeds(embedBuilder.build()).addActionRow(
                Button.success("link", button))
                .queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("link")) {
            event.deferEdit().queue();
            if (LinkManager.getLinking().containsValue(event.getMember())) {
                event.getUser().openPrivateChannel().queue(privateChannel -> {
                    assert alreadyLinking != null;
                    privateChannel.sendMessage(alreadyLinking).queue();
                });
                return;
            }
            Objects.requireNonNull(event.getMember()).getUser().openPrivateChannel().queue(privateChannel -> {
                if (StorageManager.isLinkedByDiscord(event.getMember())) {
                    privateChannel.sendMessage("You are already linked!").queue();
                    return;
                }
                String code = generateRandomCode();
                assert codeMessage != null;
                String message = codeMessage.replace("%code%", code);
                privateChannel.sendMessage(message).queue();
                LinkManager.queLink(event.getMember(), code);
            });
        }
        if (event.getComponentId().equals("accept")) {
            try {
                Account account = Database.findAccountByDiscordID(event.getUser().getId());
                if (account == null) {
                    event.getUser().openPrivateChannel().queue(privateChannel -> event.reply("Unable to find Account Information...").queue());
                    return;
                }
                assert acceptMessage != null;
                event.reply(acceptMessage).queue();
                VerifyManager.verifying((Player) account.getPlayer(), true);

            } catch (SQLException e) {
                event.reply("Unable to find Account Information...").queue();
                throw new RuntimeException(e);
            }
        }
        if (event.getComponentId().equals("deny")) {
            try {
                Account account = Database.findAccountByDiscordID(event.getUser().getId());
                if (account == null) {
                    event.getUser().openPrivateChannel().queue(privateChannel -> event.reply("Unable to find Account Information...").queue());
                    return;
                }
                VerifyManager.verifying((Player) account.getPlayer(), false);
            } catch (SQLException e) {
                event.reply("Unable to find Account Information...").queue();
                throw new RuntimeException(e);
            }
            assert denyMessage != null;
            event.reply(denyMessage).queue();
        }
    }

    private static String generateRandomCode() {
        String CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        SecureRandom random = new SecureRandom();
        StringBuilder code = new StringBuilder(10);

        for (int i = 0; i < 10; i++) {
            int randomIndex = random.nextInt(CHARACTERS.length());
            char randomChar = CHARACTERS.charAt(randomIndex);
            code.append(randomChar);
        }

        return code.toString();
    }

    public static void sendVerify(Account account, String ip){
        String title = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.verify.title")).replace("%ip%", ip);
        String text = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.verify.text")).replace("%ip%", ip);
        String footer = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.verify.footer")).replace("%ip%", ip);
        String button1 = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.verify.VerifyButton")).replace("%ip%", ip);
        String button2 = Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("messages.verify.DenyButton")).replace("%ip%", ip);


        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(text);
        embedBuilder.setFooter(footer);

        User user = account.getUser();

        if (user != null) {
            user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(embedBuilder.build()).addActionRow(
                            Button.success("accept", button1).withEmoji(Emoji.fromUnicode("✅")),
                            Button.danger("deny", button2).withEmoji((Emoji.fromUnicode("❌"))))
                    .queue());
        }

        else {
            System.out.println("User is null");
            System.out.println("ID: \"" + account.getDiscordID() + "\"");
        }
    }


}
