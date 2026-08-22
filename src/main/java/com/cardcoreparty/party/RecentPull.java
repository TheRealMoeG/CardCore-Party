package com.cardcoreparty.party;

public final class RecentPull
{
    private final String playerName;
    private final String cardName;
    private final long timestamp;

    public RecentPull(String playerName, String cardName, long timestamp)
    {
        this.playerName = playerName;
        this.cardName = cardName;
        this.timestamp = timestamp;
    }

    public String getPlayerName()
    {
        return playerName;
    }

    public String getCardName()
    {
        return cardName;
    }

    public long getTimestamp()
    {
        return timestamp;
    }
}
