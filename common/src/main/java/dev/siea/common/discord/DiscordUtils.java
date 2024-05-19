package dev.siea.common.discord;

import dev.siea.common.Common;
import dev.siea.common.base.BaseLinkManager;
import dev.siea.common.base.BaseVerifyManager;
import dev.siea.common.messages.Messages;
import dev.siea.common.storage.CommonStorageManager;
import dev.siea.common.storage.models.Account;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.sharding.ShardManager;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.security.SecureRandom;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DiscordUtils extends ListenerAdapter {
    private TextChannel channel;
    private Role role;
    private final BaseLinkManager lm;
    private Messages messages;
    private final BaseVerifyManager vm;
    private final CommonStorageManager sm;

    public DiscordUtils(Common common) {
        String title = messages.get("link.title");
        String text = messages.get("link.text");
        String footer = messages.get("link.footer");
        String button = messages.get("link.linkButton");
        ShardManager shardManager = common.getShardManager();
        this.lm = common.getLinkManager();
        this.messages = common.getMessages();
        this.vm = common.getVerifyManager();
        this.sm = common.getStorageManager();

        try{
            channel = shardManager.getTextChannelById(common.getConfigString("discord.channel"));
        } catch (Exception e) {
            channel = null;
        }
        if (channel == null) {
            common.disable("Disabling due being unable to locate channel");
            return;
        }

        sendLinkMessage(title, text, footer, button);
        try{role = shardManager.getRoleById(common.getConfigString("discord.role")); } catch (Exception ignore){}
    }

    public void giveRole(String userId) {
        if (role == null || channel == null){
            return;
        }
        Member member = channel.getGuild().getMemberById(userId);
        if (member == null){
            return;
        }
        channel.getGuild().addRoleToMember(member, role).queue();
    }


    public void giveRole(String userId, boolean action) {
        if (action){
            giveRole(userId);
        }
        else{
            if (role == null || channel == null){
                return;
            }
            Member member = channel.getGuild().getMemberById(userId);
            if (member == null){
                return;
            }
            channel.getGuild().removeRoleFromMember(member, role).queue();
        }
    }


    private void sendLinkMessage(final String title,final String text,final String footer,final String button) {
        assert channel != null;
        CompletableFuture.runAsync(() -> {
            channel.purgeMessages(channel.getHistory().retrievePast(100).complete());
            EmbedBuilder embedBuilder = generateEmbed(title, text, footer);

            if (button == null) {
                channel.sendMessageEmbeds(embedBuilder.build()).addActionRow(
                                Button.success("link", "Link"))
                        .queue();
            }
            else{
                channel.sendMessageEmbeds(embedBuilder.build()).addActionRow(
                                Button.success("link", button))
                        .queue();
            }
        });
    }

    private @NotNull EmbedBuilder generateEmbed(String title, String text, String footer) {
        EmbedBuilder embedBuilder = new EmbedBuilder();

        if (title == null) {
            embedBuilder.setTitle("Link your account!");
        }
        else{
            embedBuilder.setTitle(title);
        }

        if (text == null) {
            embedBuilder.setDescription("Click the button below to Link your account!");
        }
        else{
            embedBuilder.setDescription(text);
        }

        if (footer == null) {
            embedBuilder.setFooter("Discord2FA");
        }
        else{
            embedBuilder.setFooter(footer);
        }

        embedBuilder.setColor(Color.GREEN);
        return embedBuilder;
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        if (event.getComponentId().equals("link")) {
            event.deferEdit().queue();
            if (lm.getLinking().containsValue(event.getMember())) {
                event.getUser().openPrivateChannel().queue(privateChannel -> privateChannel.sendMessage(messages.get("alreadyLinking")).queue());
                return;
            }
            Objects.requireNonNull(event.getMember()).getUser().openPrivateChannel().queue(privateChannel -> {
                if (sm.isLinkedByDiscord(event.getMember())) {
                    privateChannel.sendMessage("You are already linked!").queue();
                    return;
                }
                String code = generateRandomCode();
                String message = messages.get("codeMessage").replace("%code%", code);
                privateChannel.sendMessage(message).queue();
                lm.queLink(event.getMember(), code);
            });
        }
        if (event.getComponentId().equals("accept")) {
            Account account = sm.findAccountByDiscordID(event.getUser().getId());
            if (account == null) {
                event.getUser().openPrivateChannel().queue(privateChannel -> event.reply("Unable to find Account Information...").queue());
                return;
            }
            event.reply(messages.get("acceptMessage")).queue();
            vm.verifying(account.getMinecraftUUID(), true);

        }
        if (event.getComponentId().equals("deny")) {
            Account account = sm.findAccountByDiscordID(event.getUser().getId());
            if (account == null) {
                event.getUser().openPrivateChannel().queue(privateChannel -> event.reply("Unable to find Account Information...").queue());
                return;
            }
            vm.verifying(account.getMinecraftUUID(), false);
            event.reply(messages.get("denyMessage")).queue();
        }
    }

    private String generateRandomCode() {
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

    public void sendVerify(Account account, String ip){
        String title = messages.get("verify.title").replace("%ip%", ip);
        String text = messages.get("verify.text").replace("%ip%", ip);
        String footer = messages.get("verify.footer").replace("%ip%", ip);
        String button1 = messages.get("verify.VerifyButton").replace("%ip%", ip);
        String button2 = messages.get("verify.DenyButton").replace("%ip%", ip);


        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle(title);
        embedBuilder.setDescription(text);
        embedBuilder.setFooter(footer);

        account.getUser().thenAccept(user -> {
            if (user != null) {
                user.openPrivateChannel().queue(privateChannel -> privateChannel.sendMessageEmbeds(embedBuilder.build()).addActionRow(
                                Button.success("accept", button1).withEmoji(Emoji.fromUnicode("✅")),
                                Button.danger("deny", button2).withEmoji((Emoji.fromUnicode("❌"))))
                        .queue());
            } else {
                System.out.println("User is null");
                System.out.println("ID: \"" + account.getDiscordID() + "\"");
            }
        });
    }
}