# Rare Fish Finder

Client-side Fabric mod that makes rare tropical fish easy to spot. Vanilla
spawns the same 22 common varieties most of the time, everything else is rare
and this mod makes those glow and shows their pattern and colors above them.

## Usage

- Rare fish glow. R cycles off / rare only / all tropical fish, and an
  optional filter glows only what you have not collected yet
- Name tags show pattern and colors above rare fish (N to toggle)
- Bucket tooltips show the actual fish plus Rare, Solid and NEW badges, and
  bucket icons carry two color squares so a chest of fish reads at a glance
- Fish collection screen (B): tracks collected variants and catch counts
  per server. Hovering buckets in chests fills in what you already own
- First catch of a variant pops a toast with the live fish and its
  collection number; finishing a pattern page, all commons, all solids or
  the whole dex celebrates properly
- A Fish Collection tab in the vanilla advancements screen, drawn as a real
  advancement tree with your poster fish as icons. Clicking a pattern node
  opens the collection grid
- Everything is in Mod Menu > Rare Fish Finder

Plays nice with others, nothing required:

- Fancy Toasts: catches and milestones announce in its styling, still with
  the live fish as the icon (toggleable)
- Better Advancements: the collection tab stays available
- ClientSort / Mouse Wheelie: two extra sort orders group tropical fish
  buckets by pattern or by base color
- Xaero's Minimap: two options, both off by default, hide common fish from
  the maps or hide everything except tropical fish

## Requirements

Minecraft 26.1+, Fabric API and Cloth Config. Mod Menu recommended.

## Translations

- Danish, English: Rasmus
- French: [MrCookie112](https://github.com/MrCookie112)

Translations are very welcome. Copy
`src/main/resources/assets/rarefishfinder/lang/en_us.json`, translate the
values, and open a PR or attach the file to an issue.
