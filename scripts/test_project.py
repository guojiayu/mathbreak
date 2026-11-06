#!/usr/bin/env python3
"""Offline structural tests. Does NOT replace an Android compiler or device tests."""
import re
import subprocess
from pathlib import Path
from xml.etree import ElementTree as ET

root = Path(__file__).resolve().parents[1]
manifest = ET.parse(root / 'app/src/main/AndroidManifest.xml').getroot()
ns='{http://schemas.android.com/apk/res/android}'
assert manifest.find(f".//uses-permission[@{ns}name='android.permission.SYSTEM_ALERT_WINDOW']") is not None
assert manifest.find(f".//uses-permission[@{ns}name='android.permission.RECEIVE_BOOT_COMPLETED']") is not None
receivers = [x.attrib.get(ns+'name') for x in manifest.findall('.//receiver')]
assert '.QuizAlarmReceiver' in receivers and '.BootReceiver' in receivers
assert any(x.attrib.get(ns+'name') == '.QuizOverlayService' for x in manifest.findall('.//service'))
services = manifest.findall('.//service')
assert any(x.attrib.get(ns+'name') == '.MediaListenerService' for x in services)
java = root / 'app/src/main/java/org/mathbreak/tv'
assert len(list(java.glob('*.java'))) >= 7
main = (java/'MainActivity.java').read_text()
overlay = (java/'QuizOverlayService.java').read_text()
assert 'QuizScheduler.afterCorrect(this)' in overlay
assert 'PlaybackController.resume(this)' in overlay
assert 'windows.addView(root, params)' in overlay
assert 'TYPE_APPLICATION_OVERLAY' in overlay
assert 'WRAP_CONTENT' in overlay
assert 'dispatchKeyEvent(KeyEvent event)' in overlay
assert 'QuizOverlayService.launch(this)' in main
assert 'quizPausedMedia' not in main
assert 'showBreakMenu()' in main
assert 'beginQuizMode(15)' in main and 'beginQuizMode(30)' in main and 'beginQuizMode(45)' in main
assert 'singleTask' in (root/'app/src/main/AndroidManifest.xml').read_text()
receiver = (java/'QuizAlarmReceiver.java').read_text()
assert 'QuizOverlayService.launch(context)' in receiver
assert 'context.startActivity(activity)' not in receiver
assert 'QuizOverlayService' in (root/'app/src/main/AndroidManifest.xml').read_text()
scheduler = (java/'QuizScheduler.java').read_text()
assert 'setAndAllowWhileIdle' in scheduler
assert 'AlarmManager.RTC_WAKEUP' in scheduler
assert 'QuizState.KEY_DUE, false' in scheduler
subprocess.run(['python', str(root/'scripts/validate_questions.py')], check=True)
print('Manifest, source wiring, permissions and scheduling structure: OK')
print('NOTE: Android SDK compilation and device execution were not tested by this script.')
