#!/usr/bin/env python3
"""
Raspberry Pi doorbell button script.
Listens for GPIO button press and sends doorbell alert with stored credentials.

Install dependencies:
    pip3 install paho-mqtt RPi.GPIO

Usage:
    python3 pi_doorbell.py --pi-id my-pi-doorbell --broker broker.hivemq.com --gpio 17

The script expects credentials to be saved in ~/.doorbell_credentials.json
(created by pi_credential_manager.py)
"""
import argparse
import json
import os
import time
import paho.mqtt.client as mqtt

try:
    import RPi.GPIO as GPIO
    GPIO_AVAILABLE = True
except ImportError:
    print("⚠ RPi.GPIO not available - running in simulation mode")
    GPIO_AVAILABLE = False

CREDENTIALS_FILE = os.path.expanduser("~/.doorbell_credentials.json")

def load_credentials():
    """Load stored credentials from file."""
    try:
        with open(CREDENTIALS_FILE, 'r') as f:
            creds = json.load(f)
        print(f"✓ Credentials loaded: {creds['username']}")
        return creds
    except FileNotFoundError:
        print(f"✗ Credentials file not found at {CREDENTIALS_FILE}")
        print("  Run pi_credential_manager.py first to register the Pi on the website")
        return None
    except Exception as e:
        print(f"✗ Failed to load credentials: {e}")
        return None

def on_connect(client, userdata, flags, rc):
    if rc == 0:
        print("✓ MQTT connected")
    else:
        print(f"✗ MQTT connection failed with code {rc}")

def send_doorbell_alert(client, pi_id, credentials):
    """Send doorbell alert with credentials."""
    if not credentials:
        print("✗ No credentials available")
        return False

    payload = {
        "type": "DOORBELL_RING",
        "username": credentials.get("username"),
        "phone": credentials.get("phone"),
        "piId": pi_id,
        "message": "Your dog is at the door!"
    }

    topic = f"devices/{pi_id}/events"
    try:
        client.publish(topic, json.dumps(payload), qos=1)
        print(f"✓ Doorbell alert sent for {credentials.get('username')}")
        return True
    except Exception as e:
        print(f"✗ Failed to send alert: {e}")
        return False

def button_pressed_callback(channel, client, pi_id, credentials):
    """Callback when button is pressed."""
    print("\n🔔 Doorbell button pressed!")
    # Debounce: wait 0.5 seconds to avoid multiple triggers
    time.sleep(0.5)
    send_doorbell_alert(client, pi_id, credentials)

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--pi-id', required=True, help='Pi device ID (e.g. my-pi-doorbell)')
    parser.add_argument('--broker', default='broker.hivemq.com', help='MQTT broker address')
    parser.add_argument('--port', type=int, default=1883, help='MQTT broker port')
    parser.add_argument('--gpio', type=int, default=17, help='GPIO pin for doorbell button (default: 17)')
    args = parser.parse_args()

    # Load credentials
    credentials = load_credentials()
    if not credentials:
        return

    # Setup MQTT
    client = mqtt.Client(client_id=f"{args.pi_id}-doorbell")
    client.on_connect = on_connect

    print(f"Connecting to MQTT broker {args.broker}:{args.port}...")
    client.connect(args.broker, args.port, keepalive=60)
    client.loop_start()

    # Setup GPIO if available
    if GPIO_AVAILABLE:
        try:
            GPIO.setmode(GPIO.BCM)
            GPIO.setup(args.gpio, GPIO.IN, pull_up_down=GPIO.PUD_UP)
            print(f"✓ GPIO pin {args.gpio} configured")
            print("Listening for doorbell button press. Press Ctrl+C to stop.")

            # This is a workaround for passing arguments to the callback
            def button_callback(channel):
                button_pressed_callback(channel, client, args.pi_id, credentials)

            GPIO.add_event_detect(args.gpio, GPIO.FALLING, callback=button_callback, bouncetime=500)

            try:
                while True:
                    time.sleep(1)
            except KeyboardInterrupt:
                print("\nStopping doorbell listener...")
                GPIO.cleanup()
                client.loop_stop()
        except Exception as e:
            print(f"✗ GPIO setup failed: {e}")
            print("  Make sure you run this with: sudo python3 pi_doorbell.py")
    else:
        print("\n⚠ GPIO not available. Running in simulation mode.")
        print("Press Enter to simulate a doorbell button press (Ctrl+C to stop):")
        try:
            while True:
                input()
                button_pressed_callback(None, client, args.pi_id, credentials)
        except KeyboardInterrupt:
            print("\nStopping doorbell listener...")
            client.loop_stop()

if __name__ == '__main__':
    main()
