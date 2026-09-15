package com.example.dormitory;

import com.example.dormitory.config.RedisLock;
import com.example.dormitory.dto.request.BindingRequestDTO;
import com.example.dormitory.dto.request.RepairCompleteDTO;
import com.example.dormitory.dto.request.RepairOrderCreateDTO;
import com.example.dormitory.dto.response.UnitMatchingDTO;
import com.example.dormitory.entity.LivingUnit;
import com.example.dormitory.entity.MatchingCheckRecord;
import com.example.dormitory.entity.RepairOrder;
import com.example.dormitory.entity.UnitWashbasinBinding;
import com.example.dormitory.entity.Washbasin;
import com.example.dormitory.mapper.LivingUnitMapper;
import com.example.dormitory.mapper.MatchingCheckRecordMapper;
import com.example.dormitory.mapper.RepairOrderMapper;
import com.example.dormitory.mapper.UnitWashbasinBindingMapper;
import com.example.dormitory.mapper.WashbasinMapper;
import com.example.dormitory.service.MatchingService;
import com.example.dormitory.service.RepairOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * 洗漱台送检台账：
 * 1. 开单要选中一台、写清损坏部位和经办值班员；
 * 2. 同一台存在未结送检时不能再开第二张，并带出上一张从什么时候开始还没结；
 * 3. 未结期间该台不计入配套洗漱台，但绑定痕迹不改不删；
 * 4. 已修复后该台重新回到配套，且能再开新送检单；
 * 5. 两人并发给同一台开未结送检，只能留下一张：后交的失败，先写下的待接单仍在；
 * 6. 送检/修复只在分表追加痕迹，历史记录不改不删。
 */
@SpringBootTest
class RepairOrderIsolationIntegrationTest {

    @Autowired
    private RepairOrderService repairOrderService;
    @Autowired
    private MatchingService matchingService;
    @Autowired
    private RepairOrderMapper repairOrderMapper;
    @Autowired
    private WashbasinMapper washbasinMapper;
    @Autowired
    private LivingUnitMapper livingUnitMapper;
    @Autowired
    private UnitWashbasinBindingMapper bindingMapper;
    @Autowired
    private MatchingCheckRecordMapper checkRecordMapper;

    @MockBean
    private RedisLock redisLock;

    private Long buildingId;
    private Long washbasinId;
    private Long unitId;

    @BeforeEach
    void setUp() {
        when(redisLock.tryLock(anyString())).thenReturn(UUID.randomUUID().toString());
        when(redisLock.tryLock(anyString(), anyLong())).thenReturn(UUID.randomUUID().toString());
        org.mockito.Mockito.doNothing().when(redisLock).unlock(anyString(), anyString());

        buildingId = 1L;
        washbasinId = insertWashbasin("W-R-" + System.nanoTime(), 20);
        unitId = insertUnit("U-R-" + System.nanoTime(), 16);
    }

    private Long insertWashbasin(String code, int capacity) {
        Washbasin w = new Washbasin();
        w.setWashbasinCode(code);
        w.setCapacity(capacity);
        w.setBuildingId(buildingId);
        w.setLocation("一层东侧");
        w.setStatus(1);
        washbasinMapper.insert(w);
        return w.getId();
    }

    private Long insertUnit(String code, int residents) {
        LivingUnit u = new LivingUnit();
        u.setUnitCode(code);
        u.setBuildingId(buildingId);
        u.setFloor(1);
        u.setRoomCount(4);
        u.setResidentCount(residents);
        u.setStatus(1);
        livingUnitMapper.insert(u);
        return u.getId();
    }

    private RepairOrderCreateDTO createDto(Long id, String part, String person) {
        RepairOrderCreateDTO dto = new RepairOrderCreateDTO();
        dto.setWashbasinId(id);
        dto.setDamagePart(part);
        dto.setDutyPerson(person);
        dto.setOperator(person);
        return dto;
    }

    @Test
    void openOrder_pendingThenRepaired_completesLifecycle() {
        RepairOrder order = repairOrderService.create(
                createDto(washbasinId, "水龙头漏水", "张值班"));
        assertEquals(RepairOrder.STATUS_PENDING, order.getStatus());
        assertEquals("水龙头漏水", order.getDamagePart());
        assertEquals("张值班", order.getDutyPerson());
        assertEquals(1, order.getOpenFlag());

        RepairOrder open = repairOrderService.getOpenByWashbasin(washbasinId);
        assertNotNull(open);
        assertEquals(order.getId(), open.getId());

        RepairCompleteDTO complete = new RepairCompleteDTO();
        complete.setRepairNote("已更换阀芯");
        complete.setOperator("李值班");
        RepairOrder repaired = repairOrderService.complete(order.getId(), complete);
        assertEquals(RepairOrder.STATUS_REPAIRED, repaired.getStatus());
        assertNotNull(repaired.getRepairedAt());
        assertNull(repaired.getOpenFlag(), "已修复后未结标记必须置 NULL");
        // 损坏部位、经办值班员等原单内容不被修复动作改写
        assertEquals("水龙头漏水", repaired.getDamagePart());
        assertEquals("张值班", repaired.getDutyPerson());
        assertNull(repairOrderService.getOpenByWashbasin(washbasinId));

        // 修复后可再开新单
        RepairOrder second = repairOrderService.create(
                createDto(washbasinId, "下水管堵塞", "王值班"));
        assertEquals(RepairOrder.STATUS_PENDING, second.getStatus());
        assertNotEquals(order.getId(), second.getId());
    }

    @Test
    void secondOpenOrderWhilePending_isRejected_andCarriesSinceTime() {
        RepairOrder first = repairOrderService.create(
                createDto(washbasinId, "水龙头漏水", "张值班"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> repairOrderService.create(createDto(washbasinId, "台面开裂", "另一个值班员")));
        String msg = ex.getMessage();
        assertTrue(msg.contains("未结的送检单"), "必须明确告知已有未结单: " + msg);
        assertTrue(msg.contains(String.valueOf(first.getId())), "必须带出上一张单号: " + msg);
        assertTrue(msg.contains("待接单"), "必须说明还停在待接单: " + msg);

        // 仍然只有一张待接单，且就是先写下的那张
        long countForBasin = repairOrderMapper.selectPendingOrders().stream()
                .filter(o -> o.getWashbasinId().equals(washbasinId)).count();
        assertEquals(1, countForBasin, "同一台未结送检只能有一张");
        assertEquals(first.getId(), repairOrderService.getOpenByWashbasin(washbasinId).getId());
    }

    @Test
    void pendingOrder_removesBasinFromMatchingButKeepsBindingTrace() {
        bind(unitId, washbasinId);
        UnitMatchingDTO before = matchingService.getUnitMatchingInfo(unitId);
        assertEquals(1, before.getWashbasins().size());
        assertEquals(20, before.getTotalCapacity());

        repairOrderService.create(createDto(washbasinId, "水龙头漏水", "张值班"));

        UnitMatchingDTO during = matchingService.getUnitMatchingInfo(unitId);
        // 关键：未结期间不能再算进配套洗漱台（连容量一起剔除），而不是只在前端标灰
        assertEquals(0, during.getWashbasins().size(), "未结送检台不得出现在配套洗漱台中");
        assertEquals(0, during.getTotalCapacity(), "送检台容量不得计入总容纳");
        assertEquals("送检中", during.getMatchingStatus());
        assertEquals(20, during.getRepairingCapacity(), "送检暂缺容量单独列示");

        // 绑定痕迹仍在：出现在送检中名单，单号/部位/值班员/起始时间都在
        assertEquals(1, during.getRepairingWashbasins().size());
        UnitMatchingDTO.WashbasinInfo repairing = during.getRepairingWashbasins().get(0);
        assertEquals(washbasinId, repairing.getId());
        assertEquals(20, repairing.getCapacity().intValue());
        assertTrue(repairing.getRepairing());
        assertEquals("水龙头漏水", repairing.getDamagePart());
        assertEquals("张值班", repairing.getDutyPerson());
        assertNotNull(repairing.getRepairStartedAt(), "必须带出从什么时候开始还没结");

        // 绑定关系本身不改不删
        UnitWashbasinBinding binding = bindingMapper
                .selectByUnitAndWashbasin(unitId, washbasinId);
        assertNotNull(binding);
        assertEquals(1, binding.getStatus(), "送检期间绑定痕迹必须原样保留，不能失效、不能删除");

        // 修复后自动回到配套，无需重新绑定
        repairOrderService.complete(repairing.getRepairOrderId(), new RepairCompleteDTO());
        UnitMatchingDTO after = matchingService.getUnitMatchingInfo(unitId);
        assertEquals(1, after.getWashbasins().size(), "已修复后该台重新计入配套");
        assertEquals(20, after.getTotalCapacity());
        assertEquals(0, after.getRepairingWashbasins().size());
        assertEquals(1, bindingMapper.selectByUnitAndWashbasin(unitId, washbasinId).getStatus(),
                "绑定始终未被改动");
    }

    @Test
    void repairTraces_areAppendOnly() {
        repairOrderService.create(createDto(washbasinId, "水龙头漏水", "张值班"));
        List<MatchingCheckRecord> afterOpen = traces();
        assertEquals(1, afterOpen.size());
        assertEquals("REPAIR_OPEN", afterOpen.get(0).getCheckType());
        assertEquals(washbasinId, afterOpen.get(0).getWashbasinId());
        assertTrue(afterOpen.get(0).getCheckMessage().contains("水龙头漏水"));

        RepairCompleteDTO complete = new RepairCompleteDTO();
        complete.setRepairNote("已更换阀芯");
        repairOrderService.complete(repairOrderService.getOpenByWashbasin(washbasinId).getId(), complete);

        List<MatchingCheckRecord> afterDone = traces();
        assertEquals(2, afterDone.size(), "修复只能再追加一条痕迹");
        assertEquals("REPAIR_DONE", afterDone.get(0).getCheckType(), "最新一条是修复痕迹");
        assertTrue(afterDone.get(0).getCheckMessage().contains("已更换阀芯"));
        // 原送检痕迹原样保留，未被改写
        MatchingCheckRecord openTrace = afterDone.stream()
                .filter(r -> "REPAIR_OPEN".equals(r.getCheckType())).findFirst().orElseThrow();
        assertTrue(openTrace.getCheckMessage().contains("水龙头漏水"));
    }

    @Test
    void concurrentOpenOrders_onlyOneSurvives() throws Exception {
        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failure = new AtomicInteger();
        AtomicReference<Long> winnerId = new AtomicReference<>();

        for (int i = 0; i < threads; i++) {
            final String person = "值班员-" + i;
            pool.submit(() -> {
                try {
                    start.await();
                    RepairOrder order = repairOrderService.create(
                            createDto(washbasinId, "水龙头漏水", person));
                    success.incrementAndGet();
                    winnerId.set(order.getId());
                } catch (RuntimeException e) {
                    failure.incrementAndGet();
                } catch (Throwable t) {
                    failure.incrementAndGet();
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "并发开单应在超时前完成");

        assertEquals(1, success.get(), "只能留下一张未结送检");
        assertEquals(1, failure.get(), "后交的一张必须失败");

        RepairOrder survivor = repairOrderService.getOpenByWashbasin(washbasinId);
        assertNotNull(survivor, "先写下的那张待接单必须还在");
        assertEquals(RepairOrder.STATUS_PENDING, survivor.getStatus());
        assertEquals(winnerId.get(), survivor.getId());
        // 该台送检总数恰好 1，没有第二张任何状态的单
        assertEquals(1, repairOrderMapper.selectAllOrders().stream()
                .filter(o -> o.getWashbasinId().equals(washbasinId)).count());
    }

    private void bind(Long uId, Long wId) {
        BindingRequestDTO dto = new BindingRequestDTO();
        dto.setUnitId(uId);
        dto.setWashbasinId(wId);
        dto.setOperator("tester");
        matchingService.bindWashbasin(dto);
    }

    private List<MatchingCheckRecord> traces() {
        // 测试洗漱台固定在 1 号楼，送检痕迹落分表 matching_check_record_1
        return checkRecordMapper.selectByUnitIdFromTable("matching_check_record_1", 0L).stream()
                .filter(r -> washbasinId.equals(r.getWashbasinId()))
                .toList();
    }
}
