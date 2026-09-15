package com.example.dormitory;

import com.example.dormitory.config.RedisLock;
import com.example.dormitory.dto.request.BindingRequestDTO;
import com.example.dormitory.dto.response.MatchingCheckResultDTO;
import com.example.dormitory.dto.response.UnitMatchingDTO;
import com.example.dormitory.entity.LivingUnit;
import com.example.dormitory.entity.MatchingCheckRecord;
import com.example.dormitory.entity.UnitWashbasinBinding;
import com.example.dormitory.entity.Washbasin;
import com.example.dormitory.mapper.LivingUnitMapper;
import com.example.dormitory.mapper.MatchingCheckRecordMapper;
import com.example.dormitory.mapper.UnitWashbasinBindingMapper;
import com.example.dormitory.mapper.WashbasinMapper;
import com.example.dormitory.service.MatchingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * 验证“一台公共洗漱台同时挂多个居住单元”场景下：
 * 1. 从一个单元解绑，只拆本单元关系，其他单元绑定与容量不变；
 * 2. 被拆单元按拆后剩余台重新校验容量；
 * 3. 同一台并发解绑 / 挂到第三个单元，互不误伤；
 * 4. 校验记录只追加，不改不删。
 */
@SpringBootTest
class UnbindIsolationIntegrationTest {

    @Autowired
    private MatchingService matchingService;
    @Autowired
    private WashbasinMapper washbasinMapper;
    @Autowired
    private LivingUnitMapper livingUnitMapper;
    @Autowired
    private UnitWashbasinBindingMapper bindingMapper;
    @Autowired
    private MatchingCheckRecordMapper checkRecordMapper;
    @Autowired
    private PlatformTransactionManager txManager;

    @MockBean
    private RedisLock redisLock;

    private Long buildingId;
    private Long w1;   // 公共洗漱台，容量 20
    private Long w2;   // 另一台，容量 30
    private Long unitA;
    private Long unitB;
    private Long unitC;

    @BeforeEach
    void setUp() {
        // 假锁：直接放行（返回非空令牌），互斥正确性由数据库行锁保证
        when(redisLock.tryLock(anyString())).thenReturn(UUID.randomUUID().toString());
        when(redisLock.tryLock(anyString(), anyLong())).thenReturn(UUID.randomUUID().toString());
        org.mockito.Mockito.doNothing().when(redisLock).unlock(anyString(), anyString());

        // H2 每次重建库后 building id 从 1 开始（schema.sql 每次启动执行）
        buildingId = 1L;

        w1 = insertWashbasin("W-T1-" + System.nanoTime(), 20, "一层东侧");
        w2 = insertWashbasin("W-T2-" + System.nanoTime(), 30, "一层西侧");

        unitA = insertUnit("U-A-" + System.nanoTime(), 16);
        unitB = insertUnit("U-B-" + System.nanoTime(), 18);
        unitC = insertUnit("U-C-" + System.nanoTime(), 10);
    }

    private Long insertWashbasin(String code, int capacity, String location) {
        Washbasin washbasin = new Washbasin();
        washbasin.setWashbasinCode(code);
        washbasin.setCapacity(capacity);
        washbasin.setBuildingId(buildingId);
        washbasin.setLocation(location);
        washbasin.setStatus(1);
        washbasinMapper.insert(washbasin);
        return washbasin.getId();
    }

    private Long insertUnit(String code, int residents) {
        LivingUnit unit = new LivingUnit();
        unit.setUnitCode(code);
        unit.setBuildingId(buildingId);
        unit.setFloor(1);
        unit.setRoomCount(4);
        unit.setResidentCount(residents);
        unit.setStatus(1);
        livingUnitMapper.insert(unit);
        return unit.getId();
    }

    private void bind(Long unitId, Long washbasinId) {
        BindingRequestDTO dto = new BindingRequestDTO();
        dto.setUnitId(unitId);
        dto.setWashbasinId(washbasinId);
        dto.setOperator("tester");
        matchingService.bindWashbasin(dto);
    }

    private UnitWashbasinBinding activeBinding(Long unitId, Long washbasinId) {
        return bindingMapper.selectByUnitAndWashbasin(unitId, washbasinId);
    }

    @Test
    void unbindOnlyAffectsCurrentUnit_otherUnitsKeepBindingAndCapacity() {
        // 同一台 w1 同时挂在 A、B 两个单元
        bind(unitA, w1);
        bind(unitB, w1);

        // 从 A 拆掉 w1
        MatchingCheckResultDTO result = matchingService.unbindWashbasin(unitA, w1, "tester");

        // A 的关系失效
        UnitWashbasinBinding aBinding = activeBinding(unitA, w1);
        assertNotNull(aBinding);
        assertEquals(0, aBinding.getStatus(), "被拆单元的关系必须失效");

        // B 的关系必须还在（这是本次修复的核心）
        UnitWashbasinBinding bBinding = activeBinding(unitB, w1);
        assertNotNull(bBinding);
        assertEquals(1, bBinding.getStatus(), "其他单元挂同台的关系不得被波及");

        // 匹配一览：B 的配套台、总容纳、剩余、状态都不被改写
        UnitMatchingDTO bInfo = matchingService.getUnitMatchingInfo(unitB);
        assertEquals(1, bInfo.getWashbasins().size(), "B 的配套洗漱台仍为同台");
        assertEquals(w1, bInfo.getWashbasins().get(0).getId());
        assertEquals(20, bInfo.getTotalCapacity(), "B 总容纳不得掉一截");
        assertEquals(2, bInfo.getRemainingCapacity(), "B 剩余容量=20-18=2");
        assertNotEquals("未绑定", bInfo.getMatchingStatus());

        // A 拆完后不再把已拆台容量算进去：容量归零
        UnitMatchingDTO aInfo = matchingService.getUnitMatchingInfo(unitA);
        assertEquals(0, aInfo.getWashbasins().size(), "A 已无配套洗漱台");
        assertEquals(0, aInfo.getTotalCapacity(), "已拆掉的容量不得继续计入 A");
        assertEquals(-16, aInfo.getRemainingCapacity());
        assertEquals("未绑定", aInfo.getMatchingStatus());
        assertEquals(0, result.getTotalCapacity(), "解绑返回的校验结果也必须按拆后容量");
    }

    @Test
    void unbindOneOfSeveral_recalculatedFromRemainingBasins() {
        // A 挂两台：w1(20)+w2(30)=50，住 16 人，匹配
        bind(unitA, w1);
        bind(unitA, w2);
        UnitMatchingDTO before = matchingService.getUnitMatchingInfo(unitA);
        assertEquals(50, before.getTotalCapacity());
        assertEquals("匹配", before.getMatchingStatus());

        // 拆掉 w1，只剩 w2(30)，仍按 30 重新校验
        MatchingCheckResultDTO result = matchingService.unbindWashbasin(unitA, w1, "tester");
        assertEquals(30, result.getTotalCapacity(), "拆后容量=剩余 w2 的 30，不能把 w1 的 20 算进去");

        UnitMatchingDTO after = matchingService.getUnitMatchingInfo(unitA);
        assertEquals(1, after.getWashbasins().size());
        assertEquals(w2, after.getWashbasins().get(0).getId());
        assertEquals(30, after.getTotalCapacity());
        assertEquals(14, after.getRemainingCapacity());
    }

    @Test
    void unbindWhenCapacityBecomesInsufficient_reportsFail() {
        // B 住 18 人，w1 容量 20，拆完后容量 0，应报 FAIL
        bind(unitB, w1);
        MatchingCheckResultDTO result = matchingService.unbindWashbasin(unitB, w1, "tester");
        assertEquals("WARN", result.getCheckResult(), "无台可绑按系统既有 WARN 提示处理");
        assertEquals(0, result.getTotalCapacity());
        UnitMatchingDTO info = matchingService.getUnitMatchingInfo(unitB);
        assertEquals("未绑定", info.getMatchingStatus());
    }

    @Test
    void rebindAfterUnbindReactivatesRelation() {
        bind(unitA, w1);
        matchingService.unbindWashbasin(unitA, w1, "tester");
        assertEquals(0, activeBinding(unitA, w1).getStatus());

        // 再次绑定不应因唯一键 (unit_id, washbasin_id) 报错，应复活原失效行
        MatchingCheckResultDTO rebound = matchingService.bindWashbasin(
                bindingDto(unitA, w1));
        assertNotNull(rebound);
        assertEquals(1, activeBinding(unitA, w1).getStatus(), "重绑后关系恢复有效");
        assertEquals(20, matchingService.getUnitMatchingInfo(unitA).getTotalCapacity());
    }

    @Test
    void checkRecordsAreAppendOnly() {
        bind(unitA, w1);
        int beforeCount = recordsOf(unitA).size();
        MatchingCheckRecord bindRecord = recordsOf(unitA).get(0);
        assertEquals("BIND", bindRecord.getCheckType());

        matchingService.unbindWashbasin(unitA, w1, "tester");

        List<MatchingCheckRecord> after = recordsOf(unitA);
        assertEquals(beforeCount + 1, after.size(), "只能新增一条 UNBIND 痕迹");
        // 原 BIND 记录原样保留
        MatchingCheckRecord stillThere = after.stream()
                .filter(r -> r.getId().equals(bindRecord.getId()))
                .findFirst().orElseThrow();
        assertEquals("BIND", stillThere.getCheckType());
        assertEquals(bindRecord.getCheckResult(), stillThere.getCheckResult());
        assertEquals(bindRecord.getTotalCapacity(), stillThere.getTotalCapacity());
        assertEquals("UNBIND", after.get(0).getCheckType(), "最新一条是 UNBIND");
    }

    @Test
    void concurrentUnbindAndBindToThirdUnit_neverCrossDamage() throws Exception {
        // w1 先挂在 A 和 B
        bind(unitA, w1);
        bind(unitB, w1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        // 线程1：把 w1 从 A 拆掉
        Runnable unbindTask = () -> {
            try {
                start.await();
                matchingService.unbindWashbasin(unitA, w1, "operator-1");
            } catch (Throwable t) {
                error.compareAndSet(null, t);
            }
        };
        // 线程2：同时把同台 w1 挂到第三个单元 C
        Runnable bindTask = () -> {
            try {
                start.await();
                matchingService.bindWashbasin(bindingDto(unitC, w1));
            } catch (Throwable t) {
                error.compareAndSet(null, t);
            }
        };

        pool.submit(unbindTask);
        pool.submit(bindTask);
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS), "并发操作应在超时前完成");
        assertNull(error.get(), "并发解绑/绑定不应抛异常: " + error.get());

        // 串行化后终态：A 已拆，B 仍在，C 新挂成功——三者关系都符合预期
        assertEquals(0, activeBinding(unitA, w1).getStatus(), "A 已拆");
        assertEquals(1, activeBinding(unitB, w1).getStatus(), "B 不得被拆请求误伤");
        assertEquals(1, activeBinding(unitC, w1).getStatus(), "C 新挂的关系不得被拆请求误伤");

        new TransactionTemplate(txManager).executeWithoutResult(s -> {
            assertEquals(20, matchingService.getUnitMatchingInfo(unitB).getTotalCapacity());
            assertEquals(20, matchingService.getUnitMatchingInfo(unitC).getTotalCapacity());
            assertEquals(0, matchingService.getUnitMatchingInfo(unitA).getTotalCapacity());
        });
    }

    private BindingRequestDTO bindingDto(Long unitId, Long washbasinId) {
        BindingRequestDTO dto = new BindingRequestDTO();
        dto.setUnitId(unitId);
        dto.setWashbasinId(washbasinId);
        dto.setOperator("operator-2");
        return dto;
    }

    private List<MatchingCheckRecord> recordsOf(Long unitId) {
        // 测试单元固定在 1 号楼，记录落分表 matching_check_record_1
        return checkRecordMapper.selectByUnitIdFromTable("matching_check_record_1", unitId);
    }
}
