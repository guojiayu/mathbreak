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
services = manifest.findall('.//service')
assert any(x.attrib.get(ns+'name') == '.MediaListenerService' for x in services)
java = root / 'app/src/main/java/org/mathbreak/tv'
assert len(list(java.glob('*.java'))) >= 7
main = (java/'MainActivity.java').read_text()
assert 'QuizScheduler.afterCorrect(this)' in main
assert 'PlaybackController.resume(this)' in main
assert 'showBreakMenu()' in main
assert 'beginQuizMode(15)' in main and 'beginQuizMode(30)' in main and 'beginQuizMode(45)' in main
assert 'quizPausedMedia' in main
scheduler = (java/'QuizScheduler.java').read_text()
assert 'setAndAllowWhileIdle' in scheduler
assert 'AlarmManager.RTC_WAKEUP' in scheduler
assert 'QuizState.KEY_DUE, false' in scheduler
subprocess.run(['python', str(root/'scripts/validate_questions.py')], check=True)
print('Manifest, source wiring, permissions and scheduling structure: OK')
print('NOTE: Android SDK compilation and device execution were not tested by this script.')
