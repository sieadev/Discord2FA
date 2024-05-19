package dev.siea.common.base;

import net.dv8tion.jda.api.entities.Member;

import java.util.Map;

public interface BaseLinkManager {
    Map<String, Member> getLinking();

    void queLink(Member member, String code);
}
