# FlexPlan - Adaptive Productivity System 🚀

**FlexPlan** is a professional-grade, intelligent Android productivity application designed to bridge the gap between static to-do lists and dynamic schedule management. Built with a premium "Purple Majesty" aesthetic, it utilizes behavioral analysis to help users maintain a realistic and effective daily routine.

---

## 🌟 Key Features

### 1. Adaptive Planning Engine (The "Flex" Logic)
The core of FlexPlan is its ability to learn from user behavior:
- **Behavioral Analysis:** Automatically calculates average delay or early completion patterns based on historical data.
- **Smart Rescheduling:** If a task is completed late, the system offers to "Flex" the remaining schedule, automatically shifting future tasks forward to keep the day organized.
- **Dynamic Corrections:** Detects delays in real-time and proactively manages the impact on subsequent plans.

### 2. Intelligent Scheduling & Conflict Management
- **Overlap Detection:** Prevents double-booking by calculating "Task Windows" based on Start Time + Expected Duration.
- **Proactive Slot Suggestion:** When a time conflict occurs, the "Find Next Free Slot" feature automatically identifies the first available 30-minute gap.
- **Daily Rollover:** Unfinished tasks from previous days automatically "roll over" to today's dashboard, ensuring no task is left behind.

### 3. Pro-Level System Integration
- **Sticky Status Bar:** A persistent notification always shows your very next upcoming task, keeping your focus where it needs to be.
- **Reliable Alarms:** Utilizes Android's `AlarmManager` with `setAlarmClock` logic to ensure task reminders ring even if the app is fully closed or in battery-saver mode.
- **In-Notification Actions:** Silence alarms or "Mark as Done" directly from the notification shade without unlocking your device.

### 4. Comprehensive Analytics & Insights
- **Productivity Score:** Real-time circular progress tracking on the Home Dashboard.
- **Performance Levels:** Classifies users into productivity tiers (e.g., "Highly Proactive", "Consistent", "Needs Focus") based on historical accuracy.
- **Success Tracking:** Full breakdown of total tasks, completion rates, and average delay statistics.

---

## 🎨 Design Philosophy: "Purple Majesty"
FlexPlan features a custom-designed "New-Gen" UI:
- **Palette:** High-contrast theme using **Deep Purple** (#210B2C), **Wisteria Lavender** (#BC96E6), and **Sunglow Yellow** (#FFD166).
- **Layout:** Floating bottom navigation bar with an elevated center Action Button (+).
- **Themed Experience:** Every element, from login fields to delete confirmation popups, follows the brand guidelines.

---

## 🛠️ Technical Stack
- **Language:** Kotlin
- **Database:** SQLite (v4 Schema with full relational integrity)
- **Architecture:** Clean, modular structure (Data, Model, UI, Adapter, Utils)
- **APIs Used:** 
    - `AlarmManager` (Precise Alarms)
    - `BroadcastReceivers` (Background Alerts)
    - `NotificationManager` (Interactive Notifications)
    - `SharedPreferences` (Session Persistence)

---

## 📱 How to Use
1. **Create Account:** Register locally via the secure SQLite auth system.
2. **Set Plans:** Add tasks with specific start times and durations.
3. **Stay Notified:** Receive alarms at task times; mark them complete from the notification bar or dashboard.
4. **Analyze:** Check the Analytics tab to see how your productivity improves over time.

---
*Developed as a sophisticated, high-performance productivity solution for modern Android environments.*
