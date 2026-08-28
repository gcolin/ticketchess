---
name: publish-to-integration
description: >-
  Builds the Ticket Chess fat JAR, publishes it to the integration server via
  SCP, and runs reset.sh to restart the app. Use when the user asks to publish,
  deploy, or push to integration, or mentions deploying ticket-chess to the Pi
  test environment.
---

# Publish to Integration

Deploy `target/ticket-chess-1.0.0-fat.jar` to the integration server:

```
pi@<hôte-integration>:/mnt/nvme/ticketchess/test
```

## Workflow

1. Run from the repository root (`e:\repo\ticketchess` or project root).
2. Ensure the fat JAR exists; build if missing:
   ```bash
   mvn package -P pack -DskipTests
   ```
3. Publish with SCP:
   ```bash
   scp ./target/ticket-chess-1.0.0-fat.jar pi@<hôte-integration>:/mnt/nvme/ticketchess/test
   ```
4. Restart the integration app:
   ```bash
   ssh pi@<hôte-integration> "/mnt/nvme/ticketchess/test/reset.sh"
   ```

On Windows, prefer the script (handles path checks and clearer errors):

```powershell
.\.cursor\skills\publish-to-integration\scripts\publish.ps1
```

Skip rebuild when the JAR is already fresh:

```powershell
.\.cursor\skills\publish-to-integration\scripts\publish.ps1 -SkipBuild
```

## Verification

After a successful SCP and reset:

- Confirm exit code `0` and no SSH/SCP errors for both steps.
- Optionally verify remote file size matches local:
  ```bash
  ssh pi@<hôte-integration> "ls -lh /mnt/nvme/ticketchess/test/ticket-chess-1.0.0-fat.jar"
  ```

## Troubleshooting

| Issue | Action |
|-------|--------|
| JAR missing | Run `mvn package -P pack -DskipTests` |
| `scp` not found (Windows) | Install OpenSSH Client (Settings → Apps → Optional features) |
| Host key / auth failure | Ensure SSH key or password access to `pi@<hôte-integration>` works (`ssh pi@<hôte-integration>`) |
| Network unreachable | Confirm machine is on the same LAN as `<hôte-integration>` |

Do not change the remote path or host unless the user explicitly requests it.
