package com.cardcoreparty;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class CardCorePartyPluginTest
{
    public static void main(String[] args) throws Exception
    {
        ExternalPluginManager.loadBuiltin(CardCorePartyPlugin.class);
        RuneLite.main(args);
    }
}
