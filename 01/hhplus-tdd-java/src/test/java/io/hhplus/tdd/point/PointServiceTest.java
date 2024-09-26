package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

public class PointServiceTest {

    @Mock
    private PointHistoryRepositoryImpl pointHistoryRepository;

    @Mock
    private UserPointRepositoryImpl userPointRepository;

    @InjectMocks
    private PointService pointService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // 실패01 - 사용자 없는 경우
    @Test
    void checkUser() throws Exception {
        // given
        Long id = 12L;

        // 해당 id 조회시 없으면 null 반환
        given(userPointRepository.selectById(id)).willReturn(null);

        // 새로운 사용자 생성 시
        UserPoint user = UserPoint.empty(id);
        given(userPointRepository.insertOrUpdate(id, 0L)).willReturn(user);

        // when
        UserPoint result = pointService.getUserPoint(id);

        // then
        assertNotNull(result);
        assertEquals(0L, result.point());
        assertEquals(id, result.id());
    }

    // 실패02 - 포인트가 음수나 0일 경우
    @Test
    void checkAmount() throws Exception {
        // given
        Long id = 12L;
        Long amount = -1L;
        TransactionType type = TransactionType.CHARGE;

        UserPoint existingUser = new UserPoint(id, 100L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(existingUser);

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            pointService.useUserPoint(id, amount, type);
        }, "포인트가 유효하지 않습니다.");
    }

    // 공통 제약 조건 01 - 충전/사용해야하는 포인트는 무조건 0보다 커야 한다.
    @Test
    void checkPointValue() throws Exception {
        // given
        Long id = 12L;
        Long amount = -1L;

        given(userPointRepository.selectById(id)).willReturn(new UserPoint(id, 10L, System.currentTimeMillis()));

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            pointService.useUserPoint(id, amount, TransactionType.CHARGE);
        });
    }

    // 공통 제약 조건 02 - 사용해야하는 포인트는 1,000단위로 사용
    @Test
    void checkUsePointValue() throws Exception {
        // given
        long originPoint = 21234L;
        long amount = 1234L;

        // when
        long resultPoint = originPoint - (amount / 1000L * 1000L);

        // then
        long expectedPoint = 20234L;
        assertEquals(expectedPoint, resultPoint, "포인트가 1000 단위로 내림되어 차감됩니다.");
    }

    // 기능01 - 특정 사용자 포인트 조회
    @Test
    void checkUserPoint() throws Exception {
        // given
        Long id = 12L;

        // 특정 사용자 정보 객체 반환
        UserPoint user = new UserPoint(id, 100L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(user);

        // when
        UserPoint result = pointService.getUserPoint(id);

        // then
        assertNotNull(result);
        assertEquals(100L, result.point());
        assertEquals(id, result.id());
    }

    // 기능02 - 특정 사용자 포인트 이력 조회
    @Test
    void checkUserPointHistory() throws Exception {
        // given
        Long id = 12L;

        UserPoint user = new UserPoint(id, 100L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(user);

        // 가짜 이력 데이터
        List<PointHistory> historyList = List.of(
                new PointHistory(1L, id, 50L, TransactionType.CHARGE, System.currentTimeMillis()),
                new PointHistory(2L, id, 30L, TransactionType.USE, System.currentTimeMillis())
        );
        given(pointHistoryRepository.selectAllByUserId(id)).willReturn(historyList);

        // when
        List<PointHistory> result = pointService.getUserPointHistoryList(id);

        // then
        assertNotNull(result);
        assertEquals(2, result.size());

        // then - 첫 번째 이력
        assertEquals(50L, result.get(0).amount());
        assertEquals(TransactionType.CHARGE, result.get(0).type());

        // then - 두 번째 이력
        assertEquals(30L, result.get(1).amount());
        assertEquals(TransactionType.USE, result.get(1).type());
    }

    // 기능03 - 포인트 충전
    @Test
    void checkChargePoint() throws Exception {
        // given
        Long id = 12L;
        Long amount = 50L;
        TransactionType type = TransactionType.CHARGE;

        UserPoint user = new UserPoint(id, 100L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(user);

        // when
        pointService.useUserPoint(id, amount, type);

        // then
        verify(userPointRepository).insertOrUpdate(eq(id), eq(150L));
        verify(pointHistoryRepository).insert(eq(id), eq(amount), eq(type), anyLong());
    }

    // 기능04 - 포인트 사용
    @Test
    void checkUsePoint() throws Exception {
        // given
        Long id = 12L;
        Long amount = 1050L;
        TransactionType type = TransactionType.USE;

        UserPoint user = new UserPoint(id, 2000L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(user);

        // when
        pointService.useUserPoint(id, amount, type);

        // then
        verify(userPointRepository).insertOrUpdate(id, 1000L);
        verify(pointHistoryRepository).insert(eq(id), eq(amount), eq(type), anyLong());
    }

    // 동시성 제어
    @Test
    public void checkConcurrency() throws Exception {
        // given
        long id = 1L;
        long originPoint = 10000L;

        UserPoint user = new UserPoint(id, originPoint, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(user);

        ExecutorService executorService = Executors.newFixedThreadPool(10);

        // 10번 포인트 사용
        List<Future<UserPoint>> futures = new ArrayList<>();
        long amount = 1000L;  // 한 번에 사용할 포인트

        for (int i = 0; i < 10; i++) {
            Future<UserPoint> future = executorService.submit(() -> pointService.useUserPoint(id, amount, TransactionType.USE));
            futures.add(future);
        }

        // 모든 작업이 완료될 때까지 대기
        for (Future<UserPoint> future : futures) {
            future.get();
        }

        // when
        UserPoint userPoint = userPointRepository.selectById(id);

        // then
        long expectedRemainingPoints = 0L;
        assertEquals(expectedRemainingPoints, userPoint.point(), "최종 포인트는 0이어야 합니다.");

        // ExecutorService 종료
        executorService.shutdown();
    }

}