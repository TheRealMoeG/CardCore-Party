package com.cardcoreparty;

import com.cardcoreparty.party.CardCoreCollectionMessage;
import com.cardcoreparty.ui.CardCorePartyPanel;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.party.PartyMember;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

/**
 * Standalone CardCore companion for OSRS TCG.
 *
 * It does NOT import OSRS TCG classes. OSRS TCG intentionally exposes its owned-card names through
 * RuneLite's core PluginMessage event type so sibling Plugin Hub plugins can consume the data across
 * separate plugin classloaders.
 */
@Slf4j
@PluginDescriptor(
    name = "CardCore Party",
    description = "Shares OSRS TCG owned-card progress across your RuneLite Party",
    tags = {"tcg", "cards", "party", "gim", "cardcore"}
)
public class CardCorePartyPlugin extends Plugin
{
    // Public OSRS TCG PluginMessage API constants. These are strings on purpose: Hub plugins cannot
    // safely import one another's implementation classes.
    private static final String TCG_NAMESPACE = "osrstcg";
    private static final String TCG_QUERY = "query-owned-names";
    private static final String TCG_REPLY = "owned-names";
    private static final String TCG_CHANGED = "owned-names-changed";
    private static final String TCG_KEY_OWNED_NAMES = "ownedNames";

    private static final int RECENT_LIMIT = 12;

    @Inject private net.runelite.client.eventbus.EventBus eventBus;
    @Inject private PartyService partyService;
    @Inject private WSClient wsClient;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ClientThread clientThread;

    private final Set<String> localOwned = new LinkedHashSet<>();
    private final Map<Long, Set<String>> partyOwned = new HashMap<>();
    private final ArrayDeque<String> recent = new ArrayDeque<>();
    private long revision = 0L;

    private CardCorePartyPanel panel;
    private NavigationButton navigationButton;

    @Provides
    CardCorePartyConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(CardCorePartyConfig.class);
    }

    @Override
    protected void startUp()
    {
        wsClient.registerMessage(CardCoreCollectionMessage.class);

        panel = new CardCorePartyPanel();
        panel.setSyncAction(this::queryTcg);
        panel.setPartyActions(this::joinParty, this::createParty, this::leaveParty);
        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/icon.png");
        navigationButton = NavigationButton.builder()
            .tooltip("CardCore Party")
            .icon(icon)
            .priority(6)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navigationButton);

        queryTcg();
        refreshPanel();
        log.info("CardCore Party started");
    }

    @Override
    protected void shutDown()
    {
        wsClient.unregisterMessage(CardCoreCollectionMessage.class);
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
        }
        partyOwned.clear();
        localOwned.clear();
        recent.clear();
        log.info("CardCore Party stopped");
    }


    private void joinParty(String passphrase)
    {
        String key = passphrase == null ? "" : passphrase.trim();
        if (key.isEmpty())
        {
            return;
        }
        partyService.changeParty(key);
        queryTcg();
        refreshPanel();
    }

    private void createParty()
    {
        clientThread.invokeLater(() ->
        {
            String key = partyService.generatePassphrase();
            partyService.changeParty(key);
            SwingUtilities.invokeLater(() -> panel.setPartyKey(key));
            queryTcg();
            refreshPanel();
        });
    }

    private void leaveParty()
    {
        partyService.changeParty(null);
        partyOwned.clear();
        refreshPanel();
    }

    private void queryTcg()
    {
        // Empty payload is sufficient for the OSRS TCG query API.
        eventBus.post(new PluginMessage(TCG_NAMESPACE, TCG_QUERY, Collections.emptyMap()));
        refreshPanel();
    }

    @Subscribe
    public void onPluginMessage(PluginMessage event)
    {
        if (event == null || !TCG_NAMESPACE.equals(event.getNamespace()))
        {
            return;
        }
        if (!TCG_REPLY.equals(event.getName()) && !TCG_CHANGED.equals(event.getName()))
        {
            return;
        }

        Set<String> next = parseOwnedNames(event.getData().get(TCG_KEY_OWNED_NAMES));
        if (next == null)
        {
            return;
        }

        Set<String> added = new HashSet<>(next);
        added.removeAll(localOwned);
        localOwned.clear();
        localOwned.addAll(next);

        if (!added.isEmpty())
        {
            List<String> sorted = new ArrayList<>(added);
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            for (String card : sorted)
            {
                addRecent("You added: " + card);
            }
        }

        publishLocalSnapshot();
        refreshPanel();
    }

    @Subscribe
    public void onCardCoreCollectionMessage(CardCoreCollectionMessage message)
    {
        if (message == null)
        {
            return;
        }
        PartyMember local = partyService.getLocalMember();
        if (local != null && local.getMemberId() == message.getMemberId())
        {
            return;
        }

        Set<String> next = new LinkedHashSet<>();
        if (message.getOwnedNames() != null)
        {
            for (String name : message.getOwnedNames())
            {
                if (name != null && !name.trim().isEmpty())
                {
                    next.add(name.trim());
                }
            }
        }

        Set<String> previous = partyOwned.getOrDefault(message.getMemberId(), Collections.emptySet());
        Set<String> added = new HashSet<>(next);
        added.removeAll(previous);
        partyOwned.put(message.getMemberId(), next);

        if (!added.isEmpty())
        {
            PartyMember member = partyService.getMemberById(message.getMemberId());
            String who = member != null && member.getDisplayName() != null ? member.getDisplayName() : "Party member";
            List<String> sorted = new ArrayList<>(added);
            sorted.sort(String.CASE_INSENSITIVE_ORDER);
            for (String card : sorted)
            {
                addRecent(who + " added: " + card);
            }
        }
        refreshPanel();
    }

    @Subscribe
    public void onUserJoin(UserJoin event)
    {
        publishLocalSnapshot();
        queryTcg();
    }

    @Subscribe
    public void onUserPart(UserPart event)
    {
        partyOwned.remove(event.getMemberId());
        refreshPanel();
    }

    private void publishLocalSnapshot()
    {
        if (!partyService.isInParty())
        {
            return;
        }
        revision++;
        CardCoreCollectionMessage message = new CardCoreCollectionMessage();
        message.setRevision(revision);
        List<String> names = new ArrayList<>(localOwned);
        names.sort(String.CASE_INSENSITIVE_ORDER);
        message.setOwnedNames(names);
        partyService.send(message);
    }

    private Set<String> parseOwnedNames(Object raw)
    {
        if (!(raw instanceof Iterable))
        {
            log.debug("OSRS TCG ownedNames payload missing or not iterable: {}", raw);
            return null;
        }
        Set<String> result = new LinkedHashSet<>();
        for (Object item : (Iterable<?>) raw)
        {
            if (item == null)
            {
                continue;
            }
            String name = item.toString().trim();
            if (!name.isEmpty())
            {
                result.add(name);
            }
        }
        return result;
    }

    private void addRecent(String line)
    {
        recent.addFirst(line);
        while (recent.size() > RECENT_LIMIT)
        {
            recent.removeLast();
        }
    }

    private void refreshPanel()
    {
        if (panel == null)
        {
            return;
        }

        Set<String> union = new HashSet<>(localOwned);
        for (Set<String> cards : partyOwned.values())
        {
            union.addAll(cards);
        }

        List<String> memberLines = new ArrayList<>();
        if (!localOwned.isEmpty())
        {
            memberLines.add("You — " + localOwned.size() + " unique");
        }
        for (Map.Entry<Long, Set<String>> e : partyOwned.entrySet())
        {
            PartyMember member = partyService.getMemberById(e.getKey());
            String who = member != null && member.getDisplayName() != null ? member.getDisplayName() : "Member " + e.getKey();
            memberLines.add(who + " — " + e.getValue().size() + " unique");
        }
        memberLines.sort(String.CASE_INSENSITIVE_ORDER);
        List<String> recentLines = new ArrayList<>(recent);
        String status = partyService.isInParty()
            ? (localOwned.isEmpty() ? "Party connected • waiting for TCG data" : "Party connected • TCG linked")
            : (localOwned.isEmpty() ? "Not in RuneLite Party • waiting for TCG" : "TCG linked • join a RuneLite Party");

        SwingUtilities.invokeLater(() -> panel.update(status, union.size(), memberLines, recentLines));
    }
}
