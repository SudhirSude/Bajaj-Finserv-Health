# Spring Webhook Solver

Author: Sudhir Sude
RegNo: 112215177
Date: 06/10/2025

## What it does

- Calls generateWebhook endpoint on startup.
- Reads webhook URL and accessToken.
- Sends final SQL query to webhook.

## How to build

mvn clean package

## How to run

java -jar target/spring-webhook-solver-0.0.1-SNAPSHOT.jar

## Notes

- Replace application.properties with your name/regNo/email.
- If API not reachable, set GENERATE_WEBHOOK_URL in code to a local mock for testing.

## Files included

- src/, pom.xml, application.properties, README.md, target/*.jar, screenshot_console.png
