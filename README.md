# Discord2FA

**Two-factor authentication for Minecraft via Discord.**
Lets players link their Minecraft account to Discord enabling two-factor authentication through Discord. Supports both game servers (Paper, Spigot) and proxies (BungeeCord, Velocity).

---

## How it works

1. **Linking**
   - Player joins your Discord server and clicks the “Link” button in the configured channel.
   - They receive a one-time code in DMs.
   - In-game they run `/link <code>` to complete the link.

2. **Verification**
   - If you use “remember sign-in location,” the plugin tracks IP and client version.
   - When a linked player joins from a new location, they get a Discord DM with “Verify” / “Deny.”
   - Until they verify, they are restricted by `allowedCommands` and `allowedActions`.

3. **Proxy**
   - On BungeeCord/Velocity, the same link/verify flow runs on the proxy; you can optionally send unverified players to a specific backend and move them after verification.

---

## Supported platforms

| Platform       | Module       | Notes                                                  |
|----------------|--------------|--------------------------------------------------------|
| **Paper**      | `paper`      | 1.16.5+                                                |
| **Spigot**     | `spigot`     | 1.20.5+                                                |
| **BungeeCord** | `bungeecord` | Proxy; optional verification/post-verification servers |
| **Velocity**   | `velocity`   | Proxy; same options as BungeeCord                      |

Use the JAR that matches your platform (e.g. `discord2fa-paper-2.0.0.jar` for Paper).

---

## Installation

1. **Download** the right JAR from [Releases](https://github.com/sieadev/Discord2FA/releases) or [Modrinth](https://modrinth.com/plugin/discord2fa).
2. **Game servers (Paper/Spigot):** Put the JAR in `plugins/`.
3. **Proxies (BungeeCord/Velocity):** Put the JAR in the proxy’s `plugins/` folder.
4. Start the server once to generate `config.yml` and the `lang/` folder.
5. Edit `config.yml`: set **database** (or leave SQLite default) and **Discord bot** (token, guild ID, channel ID).
6. Restart the server.

The Discord bot will only start when `discord.token`, `discord.guild`, and `discord.channel` are set. Until then, the plugin will run but log that the bot is not configured.

---

## Configuration

Config lives in the plugin folder as `config.yml`. Key sections:

### Database

- **type:** `sqlite` (default), `mysql`, or `postgresql`
- **url:** Leave empty for SQLite to use `discord2fa.db` in the plugin folder. For MySQL/PostgreSQL, set the JDBC URL.
- **Paper/Spigot:** MySQL/PostgreSQL drivers are loaded automatically via `plugin.yml` libraries.
- **BungeeCord/Velocity:** For MySQL or PostgreSQL, add the matching JDBC driver JAR to the proxy’s classpath (e.g. plugin folder or `lib/`).

### Discord bot

- **token** — Bot token from the [Discord Developer Portal](https://discord.com/developers/applications).
- **guild** — Discord server (guild) ID.
- **channel** — Channel ID where the “Link your account” message and button appear.

The bot creates one link message in that channel and reuses it after restarts.

### Settings

- **language** — Language code for messages (e.g. `en`, `de`). Files in `lang/` can be edited.
- **allowedCommands** — Commands players can run before verifying (e.g. `/link`).
- **allowedActions** — What unverified players can do: `CHAT`, `MOVE`, `BREAK`, `PLACE`, etc.
- **forceLink** — If `true`, every player must link before playing.
- **rememberSignInLocation** — If `true`, players are only asked to verify when they join from a new IP/version; known locations are trusted for 30 days.

### Proxy (BungeeCord / Velocity only)

- **server.verification** — Optional backend server name where unverified players are sent.
- **server.post-verification** — Optional server to send players to after they verify.

**Important:** The proxy only sees connection, chat, and commands. It does **not** see in-game events (block break/place, movement, etc.). To keep unverified players from breaking or building on your verification server, secure that server with a protection plugin (e.g. [WorldGuard](https://dev.bukkit.org/projects/worldguard) or similar) so the verification server is a restricted area—no build/destroy, or a small safe lobby—instead of relying on Discord2FA on every backend.

---

## Commands

| Command                  | Description                                | Permission          |
|--------------------------|--------------------------------------------|---------------------|
| `/link <code>`           | Link account using the code from Discord.  | `discord2fa.link`   |
| `/unlink`                | Unlink Discord (only if already verified). | `discord2fa.unlink` |
| `/discord2fa` or `/d2fa` | Admin info and status.                     | `discord2fa.admin`  |

### Admin: `/discord2fa` (alias `/d2fa`)

- **`/discord2fa version`** — Shows current plugin version and whether a newer release or an experimental build is running.
- **`/discord2fa status`** — Shows whether the database and Discord bot are running, with short reasons if something failed (e.g. bot not configured).

---

## Permissions

Discord2FA uses **grant-by-default** permissions for link/unlink and **op-only** for admin:

| Permission          | Default  | Description                          |
|---------------------|----------|--------------------------------------|
| `discord2fa.link`   | **true** | Use `/link`.                         |
| `discord2fa.unlink` | **true** | Use `/unlink`.                       |
| `discord2fa.admin`  | op       | Use `/discord2fa` (version, status). |

**Grant by default vs revoke by default**

- **`discord2fa.link`** and **`discord2fa.unlink`** are **granted by default** (Paper/Spigot: `default: true`). Everyone can use `/link` and `/unlink` unless you explicitly revoke the permission (e.g. in LuckPerms: `lp user <player> permission set discord2fa.link false`).
- **`discord2fa.admin`** is **op by default**; you grant it (or give op) to allow use of the admin command.

On **BungeeCord** and **Velocity**, the same permission nodes are checked in code. Configure your permission plugin so that the default group grants `discord2fa.link` and `discord2fa.unlink` (or use a wildcard); revoke them for specific users/groups to deny link/unlink.

All other behavior is controlled by config (e.g. who must link, which commands are allowed before verification).

---

## Development

### Building

**Requirements:** Java 16+, Maven 3.6+

```bash
git clone https://github.com/sieadev/Discord2FA.git
cd Discord2FA
mvn package -DskipTests
```

Output JARs (with version in the name, e.g. `2.0.0`):

- `paper/target/discord2fa-paper-2.0.0.jar`
- `spigot/target/discord2fa-spigot-2.0.0.jar`
- `bungeecord/target/discord2fa-bungeecord-2.0.0.jar`
- `velocity/target/discord2fa-velocity-2.0.0.jar`

Version is set in the root `pom.xml` via `<revision>` and is used for all modules.

### Project structure

```
Discord2FA/
├── common/          # Shared logic: database, Discord bot, config, i18n
├── gameserver/      # Game-server core (used by Paper & Spigot)
├── proxyserver/     # Proxy core (used by BungeeCord & Velocity)
├── paper/           # Paper plugin
├── spigot/          # Spigot plugin
├── bungeecord/      # BungeeCord plugin
├── velocity/        # Velocity plugin
└── pom.xml          # Parent POM (revision, dependency management, shade config)
```

- **common** — Database (HikariCP, SQLite/MySQL/PostgreSQL), Discord (Javacord), config adapters, messages.
- **gameserver / proxyserver** — Extend `BaseServer` and add platform-agnostic player handling; platforms wrap these and register commands/listeners.

### Contributing

1. Fork the repo and create a branch.
2. Make changes; keep formatting and style consistent with the existing code.
3. Run `mvn compile` (and tests if you add or change any).
4. Open a pull request with a short description of the change.

### License

MIT. See [LICENSE](LICENSE) in the repository.

---

**Author:** [sieadev](https://github.com/sieadev) · Mail · [Contact@siea.dev](mailto:Contact@siea.dev)
