package dev.siea.discord2fa.discord;

import dev.siea.discord2fa.Discord2FA;
import dev.siea.discord2fa.database.AccountUtil;
import dev.siea.discord2fa.database.models.Account;
import dev.siea.discord2fa.manager.LinkManager;
import dev.siea.discord2fa.manager.VerifyManager;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.security.SecureRandom;
import java.util.Objects;

public class DiscordUtils extends ListenerAdapter {
    private static final ShardManager shardManager = Discord2FA.getDiscordBot().getShardManager();
    private static final TextChannel channel = shardManager.getTextChannelById(Objects.requireNonNull(Discord2FA.getPlugin().getConfig().getString("discord.channel")));
    private static final String codeMessage = Discord2FA.getPlugin().getConfig().getString("messages.codeMessage");
    public DiscordUtils() {
    }

    public static void init() {
        Plugin plugin = Discord2FA.getPlugin();
        String title = plugin.getConfig().getString("Messages.link.title");
        String text = plugin.getConfig().getString("Messages.link.text");
        String footer = plugin.getConfig().getString("Messages.link.footer");
        String button = plugin.getConfig().getString("Messages.link.linkButton");
        sendVerifyMessage(title, text, footer, button);
    }

    private static void sendVerifyMessage(String title, String text, String footer, String button) {
        if (title == null) {
            title = "Verify";
        }
        if (text == null) {
            text = "Click the button below to verify your account!";
        }
        if (footer == null) {
            footer = "Discord2FA";
        }
        if (button == null) {
            button = "Verify";
        }
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(text);
        embedBuilder.setFooter(footer);
        assert channel != null;
        channel.sendMessageEmbeds(embedBuilder.build()).addActionRow(
                Button.success("verify", button))
                .queue();
        ;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("verify")) {
            event.deferEdit().queue();
            Objects.requireNonNull(event.getMember()).getUser().openPrivateChannel().queue(privateChannel -> {
                if (AccountUtil.isLinkedByDiscord(event.getMember())) {
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

    public static void sendVerify(Account account){
        String title = Discord2FA.getPlugin().getConfig().getString("Messages.verify.title");
        String text = Discord2FA.getPlugin().getConfig().getString("Messages.verify.text");
        String footer = Discord2FA.getPlugin().getConfig().getString("Messages.verify.footer");
        String button1 = Discord2FA.getPlugin().getConfig().getString("Messages.verify.button1");
        String button2 = Discord2FA.getPlugin().getConfig().getString("Messages.verify.button2");
        Member member = (Member) account.getUser();
        assert member != null;
        member.getUser().openPrivateChannel().queue(privateChannel -> {
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(title);
            embedBuilder.setDescription(text);
            embedBuilder.setFooter(footer);
            privateChannel.sendMessageEmbeds(embedBuilder.build()).addActionRow(
                    Button.success("verify", button1),
                    Button.danger("deny", button2))
                    .queue();
            ;
        });
    }


}
