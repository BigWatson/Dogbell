#!/usr/bin/env python3
"""
Simple Raspberry Pi script that publishes a SEND_SMS_REQUEST to the MQTT broker.

Install dependencies:
    pip install paho-mqtt

Usage:
    python pi_send_sms.py --broker broker.example.com --device-id my-device-123 --to +1555... --body "Your code is 123456"

This script only publishes the request. The backend should subscribe to devices/+/events and forward the message to the SMS provider.
"""
import argparse
import json
import time
import uuid
import paho.mqtt.client as mqtt

def main():
    p = argparse.ArgumentParser()
    p.add_argument('--broker', required=True)
    p.add_argument('--port', type=int, default=1883)
    p.add_argument('--device-id', required=True)
    p.add_argument('--to', required=True)
    p.add_argument('--body', required=True)
    args = p.parse_args()

    client = mqtt.Client(client_id=args.device_id)
    client.connect(args.broker, args.port)

    payload = {
        'type': 'SEND_SMS_REQUEST',
        'requestId': str(uuid.uuid4()),
        'to': args.to,
        'body': args.body,
        # Optionally provide an smtp gateway fallback address
        # 'smtpGateway': '1234567890@txt.att.net'
    }

    topic = f'devices/{args.device_id}/events'
    client.publish(topic, json.dumps(payload), qos=1)
    # allow time for network flush
    time.sleep(1)
    client.disconnect()

if __name__ == '__main__':
    main()
