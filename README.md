# Washere

A powerful and flexible Minecraft server core plugin that enhances server management and player experience.

## 🚀 Features

### PvP System
- Individual PvP toggling
- Configurable cooldown system
- Real-time action bar notifications
- Protected PvP zones

### Player Settings
- ⚙️ Customizable player preferences
- 💾 Persistent data storage
- ⚡ Asynchronous data handling
- 📋 Default settings management

### Core Features
- 🌍 Multiple server type support
- 📊 Dynamic scoreboard system
- 👥 Player visibility controls
- 🕒 Personal time settings
- 💬 Enhanced messaging system
- 📍 TPA functionality

## 📦 Installation

1. Download `WashereCore.jar`
2. Place in server's `plugins` folder
3. Restart server
4. Edit `config.yml` as needed

## ⚙️ Configuration

```yaml
# =======================================
#           GENERAL SETTINGS
# =======================================
server-type: none  # Options: survival, lobby (ffa coming soon)

# =======================================
#           STORAGE SETTINGS
# =======================================
storage:
  type: "yaml"  # Options: yaml, mysql

mysql:
  host: "127.0.0.1"
  port: 3306
  database: "washeresettings"
  username: "root"
  password: "password"
```
## 🛠️ Commands

### Player Commands
- `/settings` - Access settings menu
- `/pvp` - Toggle PvP state
- `/tpa <player>` - Send teleport request

### Admin Commands
- `/wreload` - Reload configuration
- `/jail | /unjail | /setjail` Jail system.

## 🔒 Permissions
washere.admin:
description: Admin access
default: op
washere.jail
washere.npc
fly:
  description: toggle fly
  usage: /fly
  permission: washere.vip

## 🔧 Dependencies
- Server: Paper/Spigot 1.21+
- Java: 21+
- Memory: 512MB minimum
- LuckPerms
- PlaceholderAPI

## 🤝 Contributing
1. Fork repository
2. Create feature branch
3. Commit changes
4. Push to branch
5. Open pull request

## 📝 License
MIT License - see LICENSE file

## 👥 Support
- Issues: GitHub Issue Tracker
- Discord: khqledsyr
- 
## ⭐ Credits
Created and maintained by WashereMC
