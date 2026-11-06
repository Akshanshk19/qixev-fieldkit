# Qixev FieldKit v1.0.0 Release Notes

**Qixev FieldKit** is an offline, utility-first workshop and field inventory tool designed for independent technicians, field workers, and workshop managers.

---

## Core Capabilities

### 1. 100% Offline & Local-First
- **Zero Cloud Dependencies**: All data is stored locally in an embedded SQLite database on your device.
- **Zero Telemetry or Tracking**: No accounts, no sign-ins, no analytics, no ads, and no external network connections.
- **Immediate Startup**: Works anywhere, anytime, with or without internet connectivity.

### 2. Work Units & Project Tracking
- **Flexible Work Unit Creation**: Track repair jobs, field tasks, builds, or maintenance tickets with custom titles, descriptions, and optional customer information.
- **Status Lifecycle**: Manage work progression through clear stages: *Received*, *In Progress*, *Waiting for Parts*, *Completed*, *Delivered*, and *Cancelled*.
- **Protection Against Accidental Edits**: Delivered and Cancelled tickets are locked against unintended modifications.

### 3. Inventory Management
- **Parts & Supplies Tracking**: Maintain parts catalog with SKU, name, category, in-stock quantity, minimum alert thresholds, purchase cost, and selling price.
- **Low Stock Alerts**: Visual indicators flag items running at or below safety stock levels.
- **Referential Protection**: Built-in referential integrity prevents accidental deletion of parts that are actively referenced by work history.

### 4. Discrete Inventory Usage
- **Granular Allocation**: Record exactly which parts were used, how many, when, where (location/bench/rack), and for what specific purpose.
- **Multiple Allocations per Item**: Log repeated uses of the same inventory item within a single work unit as independent, timestamped usage events.
- **Safe Restitution**: Removing a single usage record immediately returns only that specific quantity back to inventory stock.
- **Cancellation Restitution**: Cancelling a work unit automatically returns all allocated parts back to inventory.

### 5. Cost Tracking & Summary
- **Financial Visibility**: Track estimated costs, final billable amounts, and automatic real-time parts cost totals calculated from unit selling prices at the time of use.

### 6. Data Portability & Safety
- **Full JSON Backup & Restore**: Export full database snapshots using Android's native document picker. Restore data with complete integrity verification and transactional safety.
- **Inventory CSV Export**: Export your inventory catalog to spreadsheet-compatible CSV format for reporting and external stock takes.

---

## Intentionally Excluded

To maintain privacy, performance, and operational simplicity, the following are intentionally NOT included in Qixev FieldKit:
- **No Cloud Synchronization**: Data is never sent to remote servers.
- **No User Accounts or Passwords**: The app belongs to the device.
- **No In-App Purchases or Subscriptions**: No paywalls or feature locks.
- **No AI / Chatbots**: Deterministic, utility-first workflows only.
- **No Network Permissions**: The app does not request or possess the Android `INTERNET` permission.

---

## System Requirements
- **Operating System**: Android 8.0 (API level 26) or higher.
- **Architecture**: Supports all standard Android architectures (arm64-v8a, armeabi-v7a, x86_64).
- **Storage**: ~2 MB app install footprint.
