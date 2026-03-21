# GWT_GameRoom

## Local development

1. Start the backend: `mvn spring-boot:run -pl *-server -am`
2. Start the GWT code server: `mvn gwt:codeserver -pl *-client -am`
3. Open http://localhost:4100/

## Deploying to Raspberry Pi

Run `./deploy.sh` to build, upload, and restart the service.

### First-time Pi setup

Run the setup script with your public hostname (a domain you have on Cloudflare):

```bash
./setup-pi.sh gameroom.example.com
```

This will:
- Install and configure nginx
- Install cloudflared, log in to Cloudflare, create the tunnel, and start it as a service
- Write `/opt/gameroom/games.yaml` with the correct `baseUrl`

**Keezenspel config** — create `/opt/keezen/application-override.yaml` on the Pi (done by Keezenspel's `setup-pi.sh`):

```yaml
server:
  servlet:
    context-path: /keezen
```

# Preparing your Raspberry Pi
## 0. prerequisite: have linux installed
## 1. Prepare the Raspberry Pi

Install Java and a few basics on the Pi:
```
sudo apt update
sudo apt install openjdk-25-jdk ufw
```
Check Java:

`
java -version
`
## 2. Create deployment folders on the Pi

Create one folder per app:
```
sudo mkdir -p /opt/keezen
sudo mkdir -p /opt/gameroom
sudo chown -R ubuntu:ubuntu /opt/keezen
sudo chown -R ubuntu:ubuntu /opt/gameroom
```
## 3. Set up SSH key-based deploy from your laptop

Generate a dedicated deploy key on your laptop:

```
ssh-keygen -t ed25519 -f ~/.ssh/pi_deploy_key
```

Copy the public key:

```
cat ~/.ssh/pi_deploy_key.pub
```
**On the Pi:**

```
mkdir -p ~/.ssh
chmod 700 ~/.ssh
nano ~/.ssh/authorized_keys
```
Paste the public key into authorized_keys, then save with:

Ctrl + O

Enter

Ctrl + X

Then run:

```
chmod 600 ~/.ssh/authorized_keys
```

Test from your laptop:

```
ssh -i ~/.ssh/pi_deploy_key ubuntu@<RASPBERRYPI_IP>
```

Add an SSH alias on your laptop in ~/.ssh/config:

```
Host my-pi
    HostName <RASPBERRYPI_IP>
    User ubuntu
    IdentityFile ~/.ssh/pi_deploy_key
```
Then connect with:

```
ssh my-pi
```

## 4. Create a systemd service for each app
game-server.service

On the Pi:

sudo nano /etc/systemd/system/game-server.service

Paste:

[Unit]
Description=Java Game Server
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/game-server
ExecStart=/usr/bin/java -jar /opt/game-server/game-server.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
gameroom.service
sudo nano /etc/systemd/system/gameroom.service

Paste:

[Unit]
Description=Gameroom Server
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/gameroom
ExecStart=/usr/bin/java -jar /opt/gameroom/gameroom.jar
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target

Reload and enable both:

sudo systemctl daemon-reload
sudo systemctl enable --now game-server
sudo systemctl enable --now gameroom

Check them:

sudo systemctl status game-server
sudo systemctl status gameroom
## 5. Make sure the apps listen on the correct ports

Example ports:

game-server → 4200

gameroom → 4100

Check on the Pi:

sudo ss -ltnp | grep -E '4100|4200'

You want to see the apps listening.

Example:

LISTEN 0 100 *:4100 *:* users:(("java",pid=...,fd=...))
LISTEN 0 100 *:4200 *:* users:(("java",pid=...,fd=...))

If Spring Boot is packaging resources inside the JAR, do not use new File(getResource(...).toURI()) for bundled files. Use getResourceAsStream() instead, otherwise it can fail inside a packaged JAR.

## 6. Deploy from your laptop
deploy-game-server.sh
#!/bin/bash
set -e

echo "Building..."
mvn clean package

echo "Uploading..."
scp -i ~/.ssh/pi_deploy_key GWT_Keezenspel-server/target/GWT_Keezenspel.jar my-pi:/home/ubuntu/game-server.jar

echo "Installing..."
ssh -i ~/.ssh/pi_deploy_key my-pi "sudo mkdir -p /opt/game-server && sudo mv /home/ubuntu/game-server.jar /opt/game-server/game-server.jar"

echo "Restarting..."
ssh -i ~/.ssh/pi_deploy_key my-pi "sudo systemctl restart game-server"

echo "Done."
deploy-gameroom.sh
#!/bin/bash
set -e

echo "Building..."
mvn clean package

echo "Uploading..."
scp -i ~/.ssh/pi_deploy_key GWT_Keezenspel-server/target/GWT_Keezenspel.jar my-pi:/home/ubuntu/gameroom.jar

echo "Installing..."
ssh -i ~/.ssh/pi_deploy_key my-pi "sudo mkdir -p /opt/gameroom && sudo mv /home/ubuntu/gameroom.jar /opt/gameroom/gameroom.jar"

echo "Restarting..."
ssh -i ~/.ssh/pi_deploy_key my-pi "sudo systemctl restart gameroom"

echo "Done."

Make scripts executable:

chmod +x deploy-game-server.sh
chmod +x deploy-gameroom.sh

Run them with:

./deploy-game-server.sh
./deploy-gameroom.sh
## 7. Check logs if something fails

For game-server:

journalctl -u game-server -n 100 --no-pager

For gameroom:

journalctl -u gameroom -n 100 --no-pager
## 8. Firewall on the Pi

Allow the required ports:

sudo ufw allow OpenSSH
sudo ufw allow 4100/tcp
sudo ufw allow 4200/tcp
sudo ufw enable
sudo ufw status
## 9. Test inside your own network first

From another device on the same network:

curl http://<PI_IP>:4100
curl http://<PI_IP>:4200

If this fails:

make sure both devices are on the same network

make sure you are not on guest Wi-Fi

verify the Pi IP with:

hostname -I
## 10. Why it may still fail from outside your house

Even if the app works locally, it may still be unreachable from the internet because of CGNAT.

In this setup:

public IP seen on the internet was different from the router WAN IP

that means inbound port forwarding would not work reliably

So instead of fighting the router, the solution was to use a tunnel.

## 11. Expose the app publicly with Cloudflare Tunnel

Install cloudflared manually on the Pi.

For Raspberry Pi ARM64:

wget https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-arm64.deb
sudo dpkg -i cloudflared-linux-arm64.deb
sudo apt-get install -f
cloudflared --version

Start a temporary tunnel for gameroom:

cloudflared tunnel --url http://localhost:4100

Start a temporary tunnel for game-server:

cloudflared tunnel --url http://localhost:4200

This gives a public trycloudflare.com URL that friends can use immediately.

Important:

the URL works

it bypasses CGNAT

it is temporary

it changes when restarted

## 12. Caddy note

caddy was installed, but because there was no domain and the home network was behind CGNAT, the practical public solution was cloudflared.

If you later get a domain or proper public routing, you can place Caddy in front of the apps like this:

game.example.com {
    reverse_proxy 127.0.0.1:4200
}

gameroom.example.com {
    reverse_proxy 127.0.0.1:4100
}
## 13. Useful commands

Check listening ports:

sudo ss -ltnp | grep java

Check services:

sudo systemctl status game-server
sudo systemctl status gameroom

Restart services:

sudo systemctl restart game-server
sudo systemctl restart gameroom

View logs:

journalctl -u game-server -f
journalctl -u gameroom -f
## 14. Summary

Final setup:

local build on laptop with Maven

deploy over SSH using a dedicated key

JARs stored under /opt/game-server and /opt/gameroom

each app managed by its own systemd service

firewall opened for required ports

internal LAN testing first

public access achieved with cloudflared because of CGNAT
