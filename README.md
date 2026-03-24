# item-groups-dumper

Fabric mod that dumps all Minecraft items and their creative tab groupings to JSON.

For [`minecraft-textures`](https://github.com/destruc7i0n/minecraft-textures)

## Output

`generated/`

- **[`items.json`](generated/items.json)** - every item ID in the order they appear in the search tab
- **[`items_grouped.json`](generated/items_grouped.json)** - items organized by creative tab

There is a GitHub Actions workflow that runs the gametest and commits the generated files to the repo.

## Running

```sh
./gradlew runClientGameTest
```
