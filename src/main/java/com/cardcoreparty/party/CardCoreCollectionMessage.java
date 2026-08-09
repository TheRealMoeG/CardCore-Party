package com.cardcoreparty.party;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.runelite.client.party.messages.PartyMemberMessage;

@Data
@EqualsAndHashCode(callSuper = false)
public class CardCoreCollectionMessage extends PartyMemberMessage
{
    private long revision;
    private String playerName;
    private List<String> ownedNames = new ArrayList<>();
}
