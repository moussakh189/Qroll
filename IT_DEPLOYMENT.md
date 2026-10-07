# QRoll — IT Deployment Guide

## What is QRoll?
QRoll is a QR-based attendance system for university teachers.  
Each teacher runs it locally on their own PC — no server, no internet required.

---

## Installation (Per Teacher PC)

### Requirements
- Windows 10 or 11 (64-bit)
- No Java installation needed — it is bundled inside the installer

### Steps
1. Copy `QRoll-1.1.exe` to the teacher's PC
2. Double-click `QRoll-1.1.exe` → follow the installer wizard
3. A **QRoll** shortcut will appear on the Desktop and in the Start Menu
4. Done — the teacher can launch QRoll immediately

---

## Where Data Is Stored

| Item | Location |
|------|----------|
| Database | `%APPDATA%\QRoll\qroll.db` (e.g. `C:\Users\<teacher>\AppData\Roaming\QRoll\`) |
| Reports (CSV exports) | Wherever the teacher saves them via the Export button |

> The `%APPDATA%\QRoll\` folder is created automatically on first launch.  
> No admin rights are needed after installation.

---

## Network / Firewall

QRoll starts a local HTTP server on **port 8080** when a session is active.  
Students connect to it via their phones over WiFi to scan the QR code.

**Action required:** Add an inbound Windows Firewall rule for port 8080:

```powershell
# Run as Administrator on the teacher's PC
netsh advfirewall firewall add rule `
  name="QRoll Attendance" `
  dir=in action=allow protocol=TCP localport=8080
```

Or push this rule via Group Policy to all teacher PCs at once.

---

## Uninstall

Standard Windows uninstall:  
**Settings → Apps → QRoll → Uninstall**

The database at `%APPDATA%\QRoll\` is **not deleted** on uninstall (so teachers keep their records).  
To fully remove it: delete `%APPDATA%\QRoll\` manually.

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| App won't open / crashes | Check that no other app is using port 8080 |
| Students can't reach the QR page | Check firewall rule above; teacher PC and student phones must be on the same WiFi |
| "Database error" on launch | Ensure `%APPDATA%\QRoll\` is writeable by the teacher's user account |
| Want to reset all data | Delete `%APPDATA%\QRoll\qroll.db` and restart the app |

---

## Rebuilding the Installer (For Developers)

Run on the developer machine (needs JDK 17+ and IntelliJ Maven):

```powershell
.\build-installer.ps1
```

Output: `installer\QRoll-1.1.exe`
