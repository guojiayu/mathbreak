# MathBreak TV

## V0.2.0 — small TV overlay and Math Mode Back navigation

In timed Quiz Mode, the timer receiver starts `QuizOverlayService`, which creates a **focusable, non-fullscreen `TYPE_APPLICATION_OVERLAY`** window instead of bringing a second Activity above the video. The video application's Activity remains in its prior task/lifecycle state. If notification access is granted, the service requests a MediaSession pause after the overlay is shown, and a resume when it disappears. Players may ignore those commands.

A correct answer dismisses exactly **one** question and restarts the selected timer. An incorrect answer retains the same question. Math Mode remains an Activity with unlimited practice. `singleTask` launcher behavior prevents older MathBreak Activity instances from accumulating; pressing Back in Math Mode no longer reveals a stack of previous problem Activities.

Android permissions: grant **Display over other apps** for the Quiz overlay. The short-lived overlay foreground service declares Android 14 `specialUse` type. Some Android/TV firmware may restrict background foreground-service starts; the pending quiz is retained and a fallback notification remains. This ordinary APK cannot force third-party players to pause or block system Home/navigation.

### V0.2.0 device validation

- Solve several Math Mode problems; press Back and confirm previously solved questions do not reappear.
- Start 15/30/45-minute Quiz Mode; while playing a video, use debug **TEST: Trigger one quiz now**.
- Verify a centered window smaller than the screen appears without the video Activity's `onPause`/`onResume` lifecycle.
- Wrong answer: same problem remains. Back: quiz stays visible. Correct answer: overlay disappears, MediaSession play is requested only if MathBreak paused it, and a new interval is scheduled.
- Test permission denial, Doze/delayed alarms, screen sleep, force-stop, reboot, and non-cooperating video players.


An **Android TV remote-first learning app** that alternates between continuous math practice and timed, one-question system-wide quizzes. Built as an ordinary sideloadable APK. No integrated video player, no AndroidX dependencies, no native code.

> **Status:** implementation source and CI build definition. This is an MVP to test on real Android TVs. System-wide Activity launches and control of *other* video applications are **best effort** and **cannot be guaranteed** on all TVs by an ordinary APK.

## Product behavior (V4)

| State | Behavior |
|---|---|
| First launch | Permission primer → Math Mode |
| Math Mode | Unlimited problems; wrong choices stay in same problem; correct choice unlocks Next, written solutions, and Quiz Mode |
| Back before correct answer | Explanation that math must be solved; cannot dismiss the app's question with Back |
| Back after correct answer | Choose Quiz Mode interval: 15 / 30 / 45 minutes or remain in Math Mode |
| Quiz Mode break | Activity finishes and previous task is revealed; one-shot timer runs in background |
| Timer expires | Marks one quiz pending; starts a small interactive overlay above the TV app; posts fallback notification |
| Quiz wrong answer | Same problem stays, optional hint after 2 wrong attempts |
| Quiz correct answer | Save result, schedule **one new** interval from answer time, request playback resume, finish Activity |
| Device reboot | Boot receiver restores the next due alarm; pending quiz remains pending |

**One quiz per interval:** Correct answer never opens another problem in Quiz Mode. Continuous practice happens only in Math Mode.

## Building

Needs Android SDK Platform 35, JDK 17, Gradle 8.9, and access to Google Maven for Android Gradle Plugin 8.7.3.

```bash
# Open the project folder in Android Studio, or build via CLI:
gradle --no-daemon :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**GitHub Actions:** `.github/workflows/android.yml` installs the required toolchain, runs `gradle :app:assembleDebug`, and publishes `mathbreak-tv-debug-apk` as a workflow artifact. This is a sideloadable **debug-signed APK**, not a Play Store release.

**ARMv7:** The app uses only Java/Dex, XML, JSON, and PNG; it contains **no native `.so` files**. Therefore the **same APK is compatible with 32-bit `armeabi-v7a` Android TV devices** as well as ARM64/x86 Android TV devices. It is not an ARMv7-exclusive ABI split. `minSdk` is 23; `targetSdk` is 34.

## Required TV configuration

1. Install the APK and launch **MathBreak TV** from the Android TV launcher.
2. At the welcome screen, select **Set up device permissions**.
3. Grant **Display over other apps** (`SYSTEM_ALERT_WINDOW`) to improve automatic quiz launches over other apps. Some TV OEMs omit this settings screen, deny overlays, or restrict background Activity launches even with permission.
4. Optional: grant **Notification Access** (used only to query active media sessions for best-effort pause and resume). This is not a guarantee of video control, and some video players do not publish a usable MediaSession.
5. On Android 13+ optionally allow **Notifications** as a fallback if a background quiz Activity launch fails. Some Android TV notification UIs do not surface normal app notifications prominently.
6. Start Math Mode, answer the first question correctly, then choose **Switch to Quiz Mode** and an interval.

**Limitations:** No ordinary APK can reliably block the Home button, Stop/Force-stop, permission removal, switching to another app, HDMI inputs, or DRM/non-cooperating TV players. The Back button can be gated *within the MathBreak Activity* only. There is no silent guaranteed pausing of arbitrary video apps. Timers are **inexact, one-shot `AlarmManager.setAndAllowWhileIdle()` alarms**; actual appearance may be delayed by Doze or OEM policy. They are elapsed wall-clock breaks, not measured video playback time.

## Demo and debug testing

The built-in `app/src/main/assets/questions.json` contains **25 original AMC-style demonstration problems** with A–E choices, answer key, hint and two written solutions per problem. They are **not official AMC 8 contest questions**.

On a debug build, while **Quiz Mode is waiting**, open MathBreak from the launcher and choose **TEST: Trigger one quiz now** to avoid waiting the whole interval. Answering correctly should immediately dismiss the quiz and schedule the next interval.

### Quick manual test matrix

| Test | Expected result |
|---|---|
| Open app first time | Welcome + settings primer |
| Answer incorrectly | Wrong choice disabled; same question remains |
| Press Back before solving | Locked reminder; problem remains |
| Solve correctly in Math Mode | Next / solutions / switch mode available |
| Open two solutions | Both explain the same answer |
| Set 15-minute Quiz Mode | App exits; next alarm recorded |
| Trigger test quiz from waiting screen | Exactly one problem shown |
| Wrong answer in Quiz Mode | Same question remains |
| Correct answer in Quiz Mode | Overlay closes, timer restarts, playback resume requested if previously paused |
| Reboot during timed break | Next alarm restored |
| Reopen app after forced process death | Current mode/quiz due state persists in SharedPreferences |
| No overlay permission | Fallback notification; quiz still appears if launched manually |
| Incompatible/no MediaSession | Quiz remains usable; playback control may fail |

For device logs use:

```bash
adb logcat -s ActivityTaskManager AlarmManager NotificationService AndroidRuntime
adb shell dumpsys alarm | grep -i mathbreak
adb shell dumpsys package org.mathbreak.tv
```

## Adding licensed AMC 8 content

The AoPS AMC 8 problem archive is the intended reference: https://artofproblemsolving.com/wiki/index.php?title=AMC_8_Problems_and_Solutions

**Do not assume that archive access permits repackaging third-party contest questions, user-written solutions or videos.** Obtain rights/permissions, then replace `app/src/main/assets/questions.json` with a licensed question pack. The on-device app **does not scrape AoPS**. Field structure:

```json
{
  "id": "YOUR-LICENSED-ID",
  "prompt": "The problem as readable text",
  "choices": ["choice A", "choice B", "choice C", "choice D", "choice E"],
  "correct": 2,
  "hint": "A helpful hint",
  "solutions": ["Method 1", "Method 2"],
  "videoUrl": "https://your-licensed-video-link",
  "source": "Contest / author / license"
}
```

`correct` is zero-based (A=0, B=1, C=2, D=3, E=4). `videoUrl` may be empty; if present, the video explanation opens an external installed video/browser app **in Math Mode after the problem is solved**. Quiz Mode immediately resumes TV after the correct answer. The MVP renders question text and does not yet provide embedded LaTeX/geometry diagrams, a content scraper, or authenticated video catalogs.

Validate content before building:

```bash
python3 scripts/validate_questions.py
python3 scripts/test_project.py
```

## Architecture

- `MainActivity`: leanback launcher; native D-pad layout; state machine for Math/Quiz/waiting, solution review, and permissions.
- `QuizState`: persistent mode, question index, answer count, interval and due flag.
- `QuizScheduler`: one-shot alarms and next interval from quiz success time.
- `QuizAlarmReceiver`: mark quiz due, post fallback notification, try showing Activity (subject to OS restrictions).
- `BootReceiver`: restores scheduled quiz after reboot/app replacement.
- `PlaybackController`: optionally pauses/resumes an active MediaSession matching the originally paused package.
- `MediaListenerService`: user-granted access gate for active media sessions.
- `QuestionBank` / `Question`: local JSON question pack.

## GitHub upload instructions

The GitHub repo should be called `mathbreak-tv` (or another chosen name). If you create an empty repository at https://github.com/new, upload the project with:

```bash
unzip mathbreak-tv-source.zip
cd mathbreak-tv
git init -b main
git add .
git commit -m 'Initial MathBreak TV Android MVP'
git remote add origin https://github.com/YOUR_USERNAME/mathbreak-tv.git
git push -u origin main
```

Alternatively, after authenticating GitHub CLI locally, run `bash scripts/publish_github.sh mathbreak-tv --private` from the project directory. It creates a new private GitHub repository and pushes source; change `--private` to `--public` if desired.

Run the **Android APK (ARMv7 compatible)** workflow under Actions, then download the `mathbreak-tv-debug-apk` artifact. If using a newly created repository, GitHub may ask you to enable Actions first.

## License

Code and original demonstration questions are under MIT (see `LICENSE`). AMC/AoPS material is **not** included or licensed by this repository.
