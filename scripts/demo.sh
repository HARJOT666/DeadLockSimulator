#!/usr/bin/env bash
# End-to-end API demo: register -> create scenario -> simulate (deadlock) -> detect -> compare all
# three recovery strategies on isolated copies -> verified, persisted results.
# Requires: a running backend (default http://localhost:8080), curl, python3.
set -euo pipefail
API="${API:-http://localhost:8080/api}"
TEMPLATE="${1:-cascading-deadlock}"
EMAIL="demo-$(date +%s)@example.com"
J() { python3 -c "import sys,json; d=json.load(sys.stdin); print($1)"; }

echo "== 1. Register $EMAIL"
TOKEN=$(curl -sf -X POST "$API/auth/register" -H 'Content-Type: application/json' \
  -d "{\"name\":\"Demo User\",\"email\":\"$EMAIL\",\"password\":\"demoPass123\"}" | J "d['token']")
AUTH=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')

echo "== 2. Create scenario from template '$TEMPLATE'"
CONFIG=$(curl -sf "$API/scenarios/templates" "${AUTH[@]}" | python3 -c "
import sys,json; t=[x for x in json.load(sys.stdin) if x['key']=='$TEMPLATE'][0]
print(json.dumps({'name':t['name'],'description':t['description'],'config':t['config']}))")
SCENARIO_ID=$(curl -sf -X POST "$API/scenarios" "${AUTH[@]}" -d "$CONFIG" | J "d['id']")
echo "   scenario id = $SCENARIO_ID"

echo "== 3. Run simulation (deterministic request sequence) + WFG/DFS detection"
RUN=$(curl -sf -X POST "$API/simulation/start" "${AUTH[@]}" -d "{\"scenarioId\":$SCENARIO_ID}")
RUN_ID=$(echo "$RUN" | J "d['id']")
echo "$RUN" | J "'   run id = %s, status = %s, deadlocked = %s\n   cycles (DFS witnesses) = %s\n   deadlocked set = %s, blocked = %s\n   |V| = %s, |E| = %s, adjacency entries examined = %s\n   snapshot SHA-256 = %s' % (d['id'], d['status'], d['deadlocked'], [c['processes'] for c in d['detection']['cycles']], d['detection']['deadlockedProcesses'], d['detection']['blockedProcesses'], d['detection']['vertexCount'], d['detection']['edgeCount'], d['detection']['edgesExamined'], d['snapshotFingerprint'])"

echo "== 4. WFG edges of the snapshot"
curl -sf "$API/simulation/$RUN_ID/graph" "${AUTH[@]}" | python3 -c "
import sys,json; g=json.load(sys.stdin)
for e in g['edges']: print('   %s -> %s  [%s]%s' % (e['from'], e['to'], e['resource'], '  (cycle)' if e['inCycle'] else ''))"

echo "== 5. Compare all three strategies (independent deep copies of the same snapshot)"
curl -sf -X POST "$API/recovery/$RUN_ID/compare" "${AUTH[@]}" | python3 -c "
import sys,json; c=json.load(sys.stdin); s=c['summary']
print('   snapshot unchanged after recovery: %s | copies identical before recovery: %s | persisted snapshot intact: %s' % (s['snapshotUnchanged'], s['copiesIdentical'], s['persistedSnapshotIntact']))
for r in sorted(c['results'], key=lambda r: r['rank']):
    m=r['metrics']; v=r['verification']
    print('\n   #%d %-46s status=%s verified=%s (cycles left=%d, invariant violations=%d)' % (r['rank'], r['displayName'], r['status'], r['verified'], len(v['remainingCycles']), len(v['violations'])))
    for rd in r['rounds']:
        for a in rd['actions']:
            print('      round %d: %s' % (rd['round'], a['explanation']))
        print('      round %d: cycles before %s -> after re-detection %s' % (rd['round'], rd['cyclesBefore'], rd['cyclesAfter']))
    print('      workLost=%d terminated=%d rolledBack=%d reclaimed=%d steps=%d starvationRisk=%d  SCORE=%.4f' % (m['workLost'], m['processesTerminated'], m['processesRolledBack'], m['resourcesReclaimed'], m['recoverySteps'], m['starvationRisk'], m['aggregateScore']))
print('\n   Recommendation: ' + s['recommendation']['headline'])
for t in s['recommendation']['tradeoffs']: print('     - ' + t)
print('   Sensitivity: ' + ', '.join('%s -> %s' % (p['name'], p['winner']) for p in s['sensitivity']))"
echo
echo "RUN_ID=$RUN_ID"
