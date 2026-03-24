# item-groups-dumper

Fabric mod that dumps all Minecraft items and their creative tab groupings to JSON.

## Output

`generated/`

- **`items.json`** — every item ID in the order they appear in the search tab
- **`items_grouped.json`** — items organized by creative tab

Both files are committed to this repo and uploaded as a workflow artifact on each run.

## Running

```sh
./gradlew runClientGameTest
```
