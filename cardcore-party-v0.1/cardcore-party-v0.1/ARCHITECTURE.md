# Architecture

```text
OSRS TCG (member A)                     OSRS TCG (member B)
       |                                       |
       | core PluginMessage                    | core PluginMessage
       v                                       v
CardCore Party A                         CardCore Party B
       |                                       |
       +---------- RuneLite Party -------------+
                    |
                    v
             shared union/view
```

## Why PluginMessage instead of importing OSRS TCG

RuneLite Plugin Hub plugins are loaded in separate classloaders. Direct references to classes in another Hub plugin are not a supported integration mechanism. OSRS TCG's own source explicitly documents its `OwnedCardNamesApiService` as a read-only sibling-plugin API and says sibling plugins should copy the string constants rather than import the class.

This is exactly what CardCore Party v0.1 does.
