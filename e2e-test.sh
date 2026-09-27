#!/bin/bash
# 端到端业务流验证：上报→应急→排查→待定→认领→工单→维修→验收→结案→赔付→分摊→统计
BASE=http://localhost:8080/api
PASS=0; FAIL=0

login() {
  curl -s $BASE/auth/login -X POST -H "Content-Type: application/json" \
    -d "{\"username\":\"$1\",\"password\":\"$2\"}" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['token'])"
}

check() { # $1=描述 $2=实际 $3=期望包含
  if echo "$2" | grep -q "$3"; then echo "  ✓ $1"; PASS=$((PASS+1)); else echo "  ✗ $1 -> $2"; FAIL=$((FAIL+1)); fi
}

echo "== 1. 市民上报 =="
CITIZEN=$(login citizen1 123456)
ADMIN=$(login admin admin123)
# 记录基线，保证脚本可重复执行
BASE_OV=$(curl -s $BASE/stats/overview -H "X-Token: $ADMIN")
BASE_TOTAL=$(echo "$BASE_OV" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['totalEvents'])")
BASE_COMP=$(echo "$BASE_OV" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['totalCompensation'])")
BASE_DISPUTE=$(curl -s $BASE/stats/disputed-units -H "X-Token: $ADMIN" | python3 -c "import sys,json;print([u['disputeCount'] for u in json.load(sys.stdin)['data'] if u['unitName']=='燃气公司'][0])")
echo "  基线: 事件=$BASE_TOTAL 赔付=$BASE_COMP 燃气争议=$BASE_DISPUTE"
EVENT=$(curl -s $BASE/events -X POST -H "X-Token: $CITIZEN" -H "Content-Type: application/json" -d '{
  "location":"跃进路与翟营大街交口西北角","roadName":"跃进路","roadType":"次干道",
  "lng":114.52,"lat":38.05,"problemType":"缺失","dangerLevel":"高",
  "damageCaused":true,"damageDesc":"电动车夜间坠入，骑手轻伤"}')
echo "$EVENT" | head -c 200; echo ""
EID=$(echo "$EVENT" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
check "事件创建 id=$EID" "$EVENT" '"status":"REPORTED"'

echo "== 2. 街道应急处置线 =="
STREET=$(login street1 123456)
R=$(curl -s $BASE/events/$EID/dispatch -X POST -H "X-Token: $STREET")
check "受理派发" "$R" '"dispatchedAt"'
R=$(curl -s $BASE/events/$EID/secure -X POST -H "X-Token: $STREET" -H "Content-Type: application/json" -d '{
  "measureTypes":"警示锥,临时盖板,夜间反光标识","cost":350,"note":"夜间加派反光标识"}')
check "临时围护完成" "$R" '"measureTypes"'
D=$(curl -s $BASE/events/$EID -H "X-Token: $STREET")
check "应急状态=SECURED" "$D" '"emergencyStatus":"SECURED"'
check "费用记为责任待定" "$D" '"bearerStatus":"PENDING"'

echo "== 3. 权属排查：先待定（街道先行处置） =="
R=$(curl -s $BASE/events/$EID/investigations -X POST -H "X-Token: $STREET" -H "Content-Type: application/json" -d '{
  "coverMarking":"井盖无铸字","pipelineInfo":"管线资料缺失","constructionHistory":"2024年多家单位施工",
  "merchantInfo":"商户称曾为燃气施工","urbanRecords":"台账无记录","candidateUnitId":3,"conclusion":"UNDETERMINED","note":"暂无法确认"}')
check "排查结论=责任待定" "$R" '"conclusion":"UNDETERMINED"'
D=$(curl -s $BASE/events/$EID -H "X-Token: $STREET")
check "事件状态=OWNER_UNDETERMINED" "$D" '"status":"OWNER_UNDETERMINED"'

echo "== 4. 权属单位认领 =="
GAS=$(login gas 123456)
LIST=$(curl -s $BASE/events -H "X-Token: $GAS")
check "待定事件对权属单位可见" "$LIST" '"eventNo"'
R=$(curl -s $BASE/events/$EID/claim -X POST -H "X-Token: $GAS")
check "燃气公司认领" "$R" '"ownershipStatus":"CONFIRMED"'

echo "== 5. 派发维修工单 =="
TEAMS=$(curl -s $BASE/meta/repair-teams -H "X-Token: $GAS")
TEAM1=$(echo "$TEAMS" | python3 -c "import sys,json;print(json.load(sys.stdin)['data'][0]['id'])")
R=$(curl -s $BASE/events/$EID/work-orders -X POST -H "X-Token: $GAS" -H "Content-Type: application/json" -d "{\"repairTeamId\":$TEAM1,\"note\":\"尽快处理\"}")
check "工单创建" "$R" '"orderNo"'
WOID=$(echo "$R" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")

echo "== 6. 维修队处理 =="
REPAIR=$(login repair1 123456)
R=$(curl -s $BASE/work-orders/$WOID/accept -X POST -H "X-Token: $REPAIR")
check "维修队接收" "$R" '"status":"ACCEPTED"'
R=$(curl -s $BASE/work-orders/$WOID/report -X POST -H "X-Token: $REPAIR" -H "Content-Type: application/json" -d '{
  "arrivedAt":"2026-09-27 10:30","chamberCondition":"井室完好，井圈轻微破损","coverSpec":"Φ700重型球墨铸铁井盖",
  "roadClosure":true,"professionalUnits":"燃气","repairDesc":"更换井盖，加固井圈","repairCost":2800}')
check "维修信息登记(封路+燃气协同)" "$R" '"roadClosure":true'
R=$(curl -s $BASE/work-orders/$WOID/finish -X POST -H "X-Token: $REPAIR")
check "维修完工" "$R" '"status":"FINISHED"'

echo "== 7. 城管巡查验收 =="
INSP=$(login inspector1 123456)
R=$(curl -s $BASE/work-orders/$WOID/acceptance -X POST -H "X-Token: $INSP" -H "Content-Type: application/json" -d '{
  "photoOk":true,"locationOk":true,"flatnessOk":true,"rollingTestOk":true,"result":"PASS","comment":"碾压测试无异响"}')
check "验收通过" "$R" '"result":"PASS"'
D=$(curl -s $BASE/events/$EID -H "X-Token: $INSP")
check "事件状态=ACCEPTED" "$D" '"status":"ACCEPTED"'
check "维修费用已入账" "$D" '"维修施工"'

echo "== 8. 结案 =="
R=$(curl -s $BASE/events/$EID/close -X POST -H "X-Token: $ADMIN")
check "结案" "$R" '"status":"CLOSED"'

echo "== 9. 赔付专员 =="
COMP=$(login comp1 123456)
R=$(curl -s $BASE/events/$EID/compensations -X POST -H "X-Token: $COMP" -H "Content-Type: application/json" -d '{
  "claimantName":"王女士","claimantType":"人员受伤","claimAmount":5000,"accidentProof":"急诊病历、现场照片、报警记录"}')
check "赔付立案" "$R" '"caseNo"'
CID=$(echo "$R" | python3 -c "import sys,json;print(json.load(sys.stdin)['data']['id'])")
EV=$(curl -s $BASE/compensations/$CID -H "X-Token: $COMP")
check "证据链含围护时效" "$EV" '围护到场时效'
check "证据链含权属确认耗时" "$EV" '权属确认耗时'
R=$(curl -s $BASE/compensations/$CID/decide -X POST -H "X-Token: $COMP" -H "Content-Type: application/json" -d '{
  "responsibility":"权属单位","decidedAmount":3600,"responsibilityNote":"围护及时但权属单位巡检缺失，承担主要责任"}')
check "赔付判定" "$R" '"status":"DECIDED"'
R=$(curl -s $BASE/compensations/$CID/pay -X POST -H "X-Token: $COMP")
check "赔付完成" "$R" '"status":"PAID"'

echo "== 10. 费用复核分摊 =="
COSTS=$(curl -s "$BASE/costs?eventId=$EID" -H "X-Token: $ADMIN")
COSTID=$(echo "$COSTS" | python3 -c "import sys,json;print([c['id'] for c in json.load(sys.stdin)['data'] if c['bearerStatus']=='PENDING'][0])")
R=$(curl -s $BASE/costs/$COSTID/allocate -X POST -H "X-Token: $ADMIN" -H "Content-Type: application/json" -d '{
  "bearerType":"OWNER","bearerUnitId":3,"note":"复核确认为燃气公司井，应急费用由其承担"}')
check "应急费用分摊给燃气公司" "$R" '"bearerStatus":"ALLOCATED"'

echo "== 11. 统计沉淀 =="
EXPECT_TOTAL=$((BASE_TOTAL+1))
EXPECT_COMP=$(python3 -c "print(round(float('$BASE_COMP')+3600,2))")
EXPECT_DISPUTE=$((BASE_DISPUTE+1))
OV=$(curl -s $BASE/stats/overview -H "X-Token: $ADMIN")
check "统计-总事件数=$EXPECT_TOTAL" "$OV" "\"totalEvents\":$EXPECT_TOTAL"
check "统计-赔付总额=$EXPECT_COMP" "$OV" "\"totalCompensation\":$EXPECT_COMP"
HR=$(curl -s $BASE/stats/hot-roads -H "X-Token: $ADMIN")
check "高发路段含跃进路" "$HR" '跃进路'
DU=$(curl -s $BASE/stats/disputed-units -H "X-Token: $ADMIN")
check "争议单位燃气公司=$EXPECT_DISPUTE" "$DU" "\"unitName\":\"燃气公司\",\"unitType\":\"燃气\",\"disputeCount\":$EXPECT_DISPUTE"
CP=$(curl -s $BASE/stats/compensations -H "X-Token: $ADMIN")
check "赔付按单位汇总" "$CP" '燃气公司'

echo ""
echo "======== 结果: PASS=$PASS FAIL=$FAIL ========"
