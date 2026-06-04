#!/usr/bin/env python3
"""
Raspberry Pi credential manager.
Listens on devices/{PI_ID}/commands for registration messages from the backend.
Saves credentials locally so the doorbell script can use them.

Install dependencies:
    pip3 install paho-mqtt

Usage:
    python3 pi_credential_manager.py --pi-id <pi-id> --broker broker.hivemq.com
"""
import argparse
import json
import os
import paho.mqtt.client as mqtt

CREDENTIALS_FILE = os.path.expanduser("~/.doorbell_credentials.json")
PI_ID = None
BROKER = None

def save_credentials(username, phone, gateway_email):
    """Save credentials to local file."""
    creds = {
        "username": username,
        "phone": phone,
        "gatewayEmail": gateway_email,
        "piId": PI_ID
    }
    try:
        with open(CREDENTIALS_FILE, 'w') as f:
            json.dump(creds, f, indent=2)
        print(f"✓ Credentials saved: {username} ({phone})")
        return True
    except Exception as e:
        print(f"✗ Failed to save credentials: {e}")
        return False

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        commands_topic = f"devices/{PI_ID}/commands"
        client.subscribe(commands_topic, qos=1)
        print(f"✓ Connected and subscribed to {commands_topic}")
    else:
        print(f"✗ Connection failed with code {rc}")

def on_message(client, userdata, msg):
    """Handle registration messages from backend."""
    try:
        payload = json.loads(msg.payload.decode())
        msg_type = payload.get("type")

        if msg_type == "REGISTER_PI":
            username = payload.get("username")
            phone = payload.get("phone")
            gateway_email = payload.get("gatewayEmail", "")

            if username and phone:
                save_credentials(username, phone, gateway_email)
            else:
                print(f"✗ Invalid registration message: missing username or phone")
        else:
            print(f"Received message type: {msg_type}")
    except Exception as e:
        print(f"✗ Error processing message: {e}")

def on_disconnect(client, userdata, rc):
    if rc != 0:
        print(f"✗ Disconnected with code {rc}")

def main():
    global PI_ID, BROKER

    parser = argparse.ArgumentParser()
    parser.add_argument('--pi-id', required=True, help='Pi device ID (e.g. my-pi-doorbell)')
    parser.add_argument('--broker', default='broker.hivemq.com', help='MQTT broker address')
    parser.add_argument('--port', type=int, default=1883, help='MQTT broker port')
    args = parser.parse_args()

    PI_ID = args.pi_id
    BROKER = args.broker

    # Create MQTT client
    client = mqtt.Client(client_id=f"{PI_ID}-cred-manager")
    client.on_connect = on_connect
    client.on_message = on_message
    client.on_disconnect = on_disconnect

    print(f"Connecting to {BROKER}:{args.port}...")
    client.connect(BROKER, args.port, keepalive=60)

    print(f"Listening for registration messages. Press Ctrl+C to stop.")
    try:
        client.loop_forever()
    except KeyboardInterrupt:
        print("\nStopping credential manager...")
        client.disconnect()

if __name__ == '__main__':
    main()
