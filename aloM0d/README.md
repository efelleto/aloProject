# aloM0d

aloM0d is a client-side Minecraft Fabric mod written in Kotlin.

The project goal is to build a small utility-client style mod with a modular architecture, while keeping dependencies to a minimum. The event bus should be implemented inside this codebase instead of adding a framework dependency.

## Development

- Minecraft: `26.1.2`
- Fabric Loader: `0.19.2`
- Fabric API: `0.146.1+26.1.2`
- Kotlin: `2.3.20`

Build locally with:

```sh
./gradlew build
```

## Architecture References

Meteor Client is used as an architecture and module-implementation reference when a practical implementation pattern is needed:

- https://github.com/MeteorDevelopment/meteor-client

This project is not affiliated with Meteor Development. Meteor Client is licensed under GPL-3.0, and this project is licensed under GPL-3.0 to keep compliance straightforward if implementation details are adapted from Meteor Client in the future.

## License

This project is licensed under the GNU General Public License v3.0. See `LICENSE`.
