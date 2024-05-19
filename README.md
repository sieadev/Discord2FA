
![Discord2FA](https://github.com/sieadev/Discord2FA/assets/69807609/400a5aed-78cd-4c39-a591-cd1d8842a8ff)
![subtitle](https://github.com/sieadev/Discord2FA/assets/69807609/f46dfa3d-a2ab-47f1-84e5-def0d005346f)


### Ever felt the need to enable 2FA on your Server? No? Well, I will tell you 2 reasons why it makes sense to do so:

#### 1. Security:
With Token-Grabbing becoming a big problem in the Minecraft community
this plugin makes sure to keep your player's accounts safe.
#### 2. Verification:
Implementing a verification process on your server offers many advantages,
such as minimizing the presence of alt-accounts.

![Features-27-3-2024 (1)](https://github.com/sieadev/Discord2FA/assets/69807609/ae8c39c8-d82c-4c22-b914-468964dc82d4)

![features](https://github.com/sieadev/Discord2FA/assets/69807609/1e5318a6-d922-4ce7-a167-1f463082f5e3)

## Config
```
# ----------------------------- Lang -----------------------------
# Choose from EN,DE,FR,IT,PL,RO,RS,TR and UA
language: en

# ----------------------------- Data -----------------------------
# MYSQL or FILE
dev.siea.discord2fa.storage: "FILE"
fileAsFallback: true #This will use files if the database is not accessible

database:
  ip: ""
  name: ""

  user: ""
  password: ""

# -------------------------- DiscordBot --------------------------
dev.siea.discord2fa.discord:
    token: ""
    guild: ""
    channel: ""

# --------------------------- Settings ---------------------------
allowedCommands: ["/link"] # Commands allowed before the Player is verified
force-link: false # Force Users to Link their accounts
rememberIPAddresses: true # Remember IP addresses; only request verification on new ones
```

## Additional Info

#### Author : [@sieadev](https://www.github.com/sieadev)

#### Version : 1.5.2

#### Contact
- Discord [@sieadev](dsc.gg/siea)
- E-Mail contact@siea.dev
- Telegram [@sieadev](t.me/sieadev)

Project Link: https://github.com/sieadev/Discord2FA


### Fun fact
Md approved this plugin!

![MD_Approved](https://dev.siea.discord2fa.storage.ko-fi.com/cdn/useruploads/display/cf23a2d8-9690-4742-ad1a-b56627b46cd6_hhgpkrp.png)


