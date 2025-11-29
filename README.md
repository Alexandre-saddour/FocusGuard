# FocusGuard: App Blocker — Your Cognitive Friction Bodyguard 🧠

## 🎯 Project Goal

**FocusGuard** is a modern digital well-being utility designed to break the cycle of “zombie scrolling” and unconscious app launching.

Instead of traditional, frustrating app blockers, FocusGuard implements **cognitive friction** — a required, non-trivial step — before allowing access to distracting applications (Social Media, Games, etc.).

The goal is to provide a brief, conscious **Pause** that allows the user to re-evaluate their intent, turning an automatic reflex into a deliberate choice.

---

## 💡 How It Works: The Pause Mechanism

FocusGuard uses an **Android AccessibilityService** to monitor the foreground application state.

1. **Selection:** The user marks distracting apps (e.g., Instagram, TikTok) in the settings.  
2. **Interception:** When a blocked app is launched, FocusGuard immediately intercepts the process.  
3. **The Challenge:** A full-screen overlay, the *Pause Activity*, appears. To dismiss it and proceed, the user must precisely type a customizable phrase (e.g., “I am conscious of this choice”).  
4. **Launch:** Only upon successful, intentional typing is the original application allowed to resume.

---

## ⚙️ Important Note on Permissions

Because the core functionality relies on monitoring app launches, after installation you must manually enable the FocusGuard Accessibility Service in your device’s settings:

## 📜 License

See the `LICENSE` file for details.
