package com.cardcoreparty;

import com.cardcoreparty.party.CardCoreCollectionMessage;
import com.cardcoreparty.ui.CardCorePartyPanel;
import com.google.inject.Provides;
import java.awt.image.BufferedImage;
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
import net.runelite.api.Client;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.PluginMessage;
import net.runelite.client.party.PartyMember;
import net.runelite.client.party.PartyService;
import net.runelite.client.party.WSClient;
import net.runelite.client.party.events.UserJoin;
import net.runelite.client.party.events.UserPart;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
    name = "CardCore Party",
    description = "Share OSRS TCG card unlocks with friends",
    tags = {"tcg", "cards", "party", "gim", "cardcore"}
)
public class CardCorePartyPlugin extends Plugin
{
    private static final String TCG_NAMESPACE = "osrstcg";
    private static final String TCG_QUERY = "query-owned-names";
    private static final String TCG_REPLY = "owned-names";
    private static final String TCG_CHANGED = "owned-names-changed";
    private static final String TCG_KEY_OWNED_NAMES = "ownedNames";

    @Inject private net.runelite.client.eventbus.EventBus eventBus;
    @Inject private PartyService partyService;
    @Inject private WSClient wsClient;
    @Inject private ClientToolbar clientToolbar;
    @Inject private ClientThread clientThread;
    @Inject private Client client;

    private final Set<String> localOwned = new LinkedHashSet<>();
    private final Map<Long, Set<String>> partyOwned = new HashMap<>();
    private final Map<Long, String> partyNames = new HashMap<>();

    private long revision;
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
        panel.setActions(this::createParty, this::joinParty, this::leaveParty, this::queryTcg);

        BufferedImage icon = ImageUtil.loadImageResource(getClass(), "/cardcore.png");
        navigationButton = NavigationButton.builder()
            .tooltip("CardCore Party")
            .icon(icon)
            .priority(6)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navigationButton);

        queryTcg();
        refreshPanel();
    }

    @Override
    protected void shutDown()
    {
        wsClient.unregisterMessage(CardCoreCollectionMessage.class);
        if (navigationButton != null)
        {
            clientToolbar.removeNavigation(navigationButton);
        }
        localOwned.clear();
        partyOwned.clear();
        partyNames.clear();
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

    private void joinParty(String passphrase)
    {
        String key = passphrase == null ? "" : passphrase.trim();
        if (key.isEmpty())
        {
            return;
        }

        partyService.changeParty(key);
        partyOwned.clear();
        partyNames.clear();
        queryTcg();
        refreshPanel();
    }

    private void leaveParty()
    {
        partyService.changeParty(null);
        partyOwned.clear();
        partyNames.clear();
        SwingUtilities.invokeLater(() -> panel.setPartyKey(""));
        refreshPanel();
    }

    private void queryTcg()
    {
        eventBus.post(new PluginMessage(TCG_NAMESPACE, TCG_QUERY, Collections.emptyMap()));
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

        localOwned.clear();
        localOwned.addAll(next);
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

        partyOwned.put(message.getMemberId(), next);

        String explicit = cleanName(message.getPlayerName());
        if (explicit != null)
        {
            partyNames.put(message.getMemberId(), explicit);
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
        partyNames.remove(event.getMemberId());
        refreshPanel();
    }

    private void publishLocalSnapshot()
    {
        if (!partyService.isInParty())
        {
            return;
        }

        CardCoreCollectionMessage message = new CardCoreCollectionMessage();
        message.setRevision(++revision);
        message.setPlayerName(localPlayerName());

        List<String> names = new ArrayList<>(localOwned);
        names.sort(String.CASE_INSENSITIVE_ORDER);
        message.setOwnedNames(names);

        partyService.send(message);
    }

    private Set<String> parseOwnedNames(Object raw)
    {
        if (!(raw instanceof Iterable))
        {
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

        List<String> sharedCards = new ArrayList<>(union);
        sharedCards.sort(String.CASE_INSENSITIVE_ORDER);

        List<String> memberLines = new ArrayList<>();
        if (!localOwned.isEmpty())
        {
            memberLines.add(localPlayerName() + " — " + localOwned.size());
        }

        for (Map.Entry<Long, Set<String>> entry : partyOwned.entrySet())
        {
            String name = partyNames.get(entry.getKey());
            if (name == null)
            {
                PartyMember member = partyService.getMemberById(entry.getKey());
                name = member == null ? null : cleanName(member.getDisplayName());
            }
            if (name == null)
            {
                name = "CardCore member";
            }

            memberLines.add(name + " — " + entry.getValue().size());
        }

        memberLines.sort(String.CASE_INSENSITIVE_ORDER);

        boolean tcgLinked = !localOwned.isEmpty();
        boolean inParty = partyService.isInParty();

        SwingUtilities.invokeLater(() ->
            panel.update(tcgLinked ? "linked" : "waiting", inParty, union.size(), memberLines, sharedCards));
    }

    private String localPlayerName()
    {
        if (client.getLocalPlayer() != null)
        {
            String name = cleanName(client.getLocalPlayer().getName());
            if (name != null)
            {
                return name;
            }
        }

        PartyMember local = partyService.getLocalMember();
        if (local != null)
        {
            String name = cleanName(local.getDisplayName());
            if (name != null)
            {
                return name;
            }
        }

        return "You";
    }

    private static String cleanName(String name)
    {
        if (name == null)
        {
            return null;
        }

        String trimmed = name.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
