# FieldKit User Observation Protocol

This protocol guides observers conducting in-person or live observational sessions with practicing repair technicians for **Experiment #001**.

---

## 1. Core Principles of Objective Observation

1. **Observe Reality, Not Intentions**: What technicians *do* matters far more than what they *say*. Watch fingers, hesitation, backtrack taps, and expressions of confusion.
2. **Silence First**: Remain silent while the technician interacts with the screen. Let them explore, misclick, search, and recover on their own before offering any guidance.
3. **No Flattery, No Defense**: Never defend the app's current design. If a technician struggles or hates a feature, record it as valuable friction data.
4. **Zero Leading Questions**: Never ask if they want a specific feature. Ask how they currently solve the problem and what they expected to happen.

---

## 2. Step-by-Step Observation Procedure

### Step 1: Hand Over the APK
- Provide the participant with a device running the verified production build (`app-release.apk`, v1.0.0).

### Step 2: One-Sentence Introduction
- Introduce the app with exactly one sentence:
  > *"FieldKit is an offline tool designed to help repair workshops track their jobs, customer devices, and parts inventory on their phone."*
- Do **NOT** explain how buttons, tabs, or dialogs work.

### Step 3: Prompt Realistic Workshop Tasks
Ask the technician to simulate real workbench operations:
- **Task A (Intake)**: *"Imagine a customer just walked in and handed you a cracked Samsung A52 that needs a screen replacement. Show me how you would record that."*
- **Task B (Parts)**: *"You have a replacement screen in stock that costs you \$25, and you want to bill the customer \$65 total including labor. Record that on this job."*
- **Task C (Progress & Delivery)**: *"You finished the repair two hours later. Show me how you would mark it finished and deliver it."*
- **Task D (History & Search)**: *"Now imagine that same customer comes back next week asking about their warranty. How would you find that job?"*

### Step 4: Silent Observation & Note Taking
- Observe silently. Do not interrupt their flow.
- Record:
  - Exact time taken per task.
  - Buttons tapped and discarded.
  - Pauses longer than 3 seconds (hesitation).
  - Taps on non-clickable text or elements.
  - Back-button navigation loops.

### Step 5: Inquire on Hesitation (Post-Task)
Only after the technician stops or finishes a task, ask neutral probing questions:
- *"I noticed you paused here for a moment?what were you looking for?"*
- *"When you tapped that icon, what did you expect to happen?"*
- *"How do you normally handle this in your daily shop routine?"*

### Step 6: Post-Workflow Value Assessment
Only after all realistic tasks are attempted, explore practical adoption:
- *"Would you use this on your workbench tomorrow instead of your current system? Why or why not?"*
- *"What is the single biggest friction point that would stop you from using this every day?"*

### Step 7: Commercial Inquiries (Only After Value is Established)
Do NOT ask about money until the user has clearly expressed whether the app solves a real problem:
- If they indicate genuine value: *"If this tool solved that problem reliably every day, would you pay for it?"*
- *"Would you expect to buy it once, or pay a monthly subscription?"*
- **Crucial Rule**: Never suggest a price figure before the technician states their number or expectation.

---

## 3. Question Formulations: Leading vs. Objective

| Leading Question (PROHIBITED) | Objective Question (MANDATORY) |
| :--- | :--- |
| ? *"Would you like a customer CRM feature?"* | ? *"How do you currently keep track of returning customers?"* |
| ? *"Isn't it nice that all your data is stored offline?"* | ? *"How do you feel about where your workshop data is stored?"* |
| ? *"Would you pay \$5 a month for this?"* | ? *"If this saved you time every day, what would you expect to pay for it?"* |
| ? *"Did you see the backup button in Settings?"* | ? *"If you got a new phone, how would you expect your data to transfer?"* |
| ? *"Don't you think creating a job is easy?"* | ? *"What part of creating that job felt fast or slow compared to paper?"* |

---

## 4. Strict Anti-Bias Rule

> [!CRITICAL]
> **Anti-Bias Mandate**:
> Do NOT modify the product simply because one user requests a feature.
>
> A feature becomes a candidate for engineering ONLY when:
> 1. Multiple users independently encounter the exact same problem.
> 2. The problem impairs or blocks an essential workshop workflow.
> 3. The current product cannot reasonably handle it with existing features.
> 4. The proposed change delivers a plausible, documented business benefit.
>
> One-off requests must remain cataloged as isolated observations unless independently validated across multiple users.
