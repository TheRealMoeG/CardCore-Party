# CardCore Party

A standalone RuneLite Plugin Hub plugin that consumes OSRS TCG's public `PluginMessage` API and shares each party member's distinct owned-card names through RuneLite Party.

## Important: this does not modify or bundle OSRS TCG

CardCore Party has its own package (`com.cardcoreparty`), its own plugin descriptor, and its own party message type. It does **not** import any `com.osrstcg.*` class.

OSRS TCG currently publishes a sibling-plugin API using RuneLite's core `PluginMessage` event:

- namespace: `osrstcg`
- query: `query-owned-names`
- reply: `owned-names`
- push update: `owned-names-changed`
- payload key: `ownedNames`

CardCore Party queries that API locally, then sends its own snapshot to the RuneLite Party.

## v0.1 flow

1. Player has OSRS TCG + CardCore Party installed.
2. CardCore posts `PluginMessage("osrstcg", "query-owned-names")`.
3. OSRS TCG replies with the player's distinct owned card names.
4. CardCore sends those names in `CardCoreCollectionMessage` to the RuneLite Party.
5. Every CardCore member builds a group-wide union and shows member counts/recent unique additions.

## Limitation of OSRS TCG's current public API

The public API exposes **distinct owned card names**, not every raw pack pull. That means:

- a brand-new unique card is detectable;
- duplicate copies are not detectable as a new event;
- normal vs foil is folded together in this API;
- exact pack contents are not exposed to sibling plugins.

To make CardCore react to **every pull**, OSRS TCG would need to expose a second core `PluginMessage` event such as `pull` with card name / foil / duplicate metadata. CardCore can then consume it without owning or modifying the TCG plugin.

## Test

Open as a Gradle project in IntelliJ and run:

```text
./gradlew run
```

Install/enable OSRS TCG in the dev client as well, join the same RuneLite Party on two clients, then click `Sync from OSRS TCG` in CardCore Party.

## Built-in basic party controls

CardCore Party v0.1 also has Create / Join / Leave controls. These use RuneLite's `PartyService`, so the passphrase creates the same underlying RuneLite Party transport used by other party-aware plugins. Every CardCore member should use the same passphrase.
