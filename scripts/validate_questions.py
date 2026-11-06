#!/usr/bin/env python3
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
questions = json.loads((root / 'app/src/main/assets/questions.json').read_text(encoding='utf8'))
assert questions, 'Question bank must not be empty'
ids = set()
for i, q in enumerate(questions):
    assert isinstance(q['id'], str) and q['id'] not in ids, f'duplicate id: {q["id"]}'
    ids.add(q['id'])
    assert isinstance(q['prompt'], str) and len(q['prompt']) > 20
    assert len(q['choices']) == 5 and len(set(q['choices'])) == 5
    assert 0 <= q['correct'] < 5
    assert len(q['solutions']) >= 1
    assert all(len(s) >= 25 for s in q['solutions'])
    assert q.get('hint')
    assert q.get('source')
print(f'Validated {len(questions)} questions, 5 choices each, solutions and sources present.')
