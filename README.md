# CardCore Party

Simple shared-card mode for OSRS TCG.

## What it does

1. CardCore reads each player's distinct owned cards from OSRS TCG.
2. Friends create/join the same CardCore party.
3. CardCore combines everyone's owned cards into one shared unlock list.
4. If any party member owns a card, that card appears in the shared collection for everyone.

There is no rules engine, slider, points system, backend, or extra game mode.

## Current transport

The party code uses RuneLite PartyService underneath because it is already available to RuneLite
plugins and requires no separate server. CardCore exposes its own Create / Join / Leave controls.

## OSRS TCG limitation

The public OSRS TCG sibling-plugin API currently exposes distinct owned card names. Duplicate pulls
and foil state are not included in this shared collection.


## v1.1 sidebar

The sidebar now uses a custom dark purple CardCore theme with:
- gradient background
- rounded bordered sections
- TCG/party status indicators
- large shared-unlock count
- compact party controls
- styled member rows
- searchable shared collection
- no horizontal scrollbar
