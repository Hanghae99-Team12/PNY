package io.hhplus.tdd.point;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
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
    void checkUser() {
        // given
        Long id = 12L;

        // 해당 id 조회시 없으면 null 반환
        given(userPointRepository.selectById(id)).willReturn(null);

        // 새로운 사용자 생성 시
        UserPoint newUser = UserPoint.empty(id);
        given(userPointRepository.insertOrUpdate(id, 0L)).willReturn(newUser);

        // when
        UserPoint result = pointService.getUserPoint(id);

        // then
        assertNotNull(result);
        assertEquals(0L, result.point());
        assertEquals(id, result.id());
    }

    // 실패02 - 포인트가 음수나 0일 경우
    @Test
    void checkAmount() {
        // given
        Long id = 12L;
        Long negativeAmount = -1L;
        TransactionType type = TransactionType.CHARGE;

        UserPoint existingUser = new UserPoint(id, 100L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(existingUser);

        // then
        assertThrows(IllegalArgumentException.class, () -> {
            // when
            pointService.useUserPoint(id, negativeAmount, type);
        }, "포인트가 유효하지 않습니다.");
    }

    // 공통 제약 조건 01 - 충전/사용해야하는 포인트는 무조건 0보다 커야 한다.
    @Test
    void checkPointValue() {
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

    // 기능01 - 특정 사용자 포인트 조회
    @Test
    void checkUserPoint() {
        // given
        Long id = 12L;

        // 특정 사용자 정보 객체 반환
        UserPoint existingUser = new UserPoint(id, 100L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(existingUser);

        // when
        UserPoint result = pointService.getUserPoint(id);

        // then
        assertNotNull(result);
        assertEquals(100L, result.point());
        assertEquals(id, result.id());
    }

    // 기능02 - 특정 사용자 포인트 이력 조회
    @Test
    void checkUserPointHistory() {
        // given
        Long id = 12L;

        UserPoint existingUser = new UserPoint(id, 100L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(existingUser);

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
    void checkChargePoint() {
        // given
        Long id = 12L;
        Long amount = 50L;
        TransactionType type = TransactionType.CHARGE;

        UserPoint existingUser = new UserPoint(id, 100L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(existingUser);

        // when
        pointService.useUserPoint(id, amount, type);

        // then
        verify(userPointRepository).insertOrUpdate(eq(id), eq(150L));
        verify(pointHistoryRepository).insert(eq(id), eq(amount), eq(type), anyLong());
    }

    // 기능04 - 포인트 사용
    @Test
    void checkUsePoint() {
        // given
        Long id = 12L;
        Long amount = 30L;
        TransactionType type = TransactionType.USE;

        UserPoint existingUser = new UserPoint(id, 100L, System.currentTimeMillis());
        given(userPointRepository.selectById(id)).willReturn(existingUser);

        // when
        pointService.useUserPoint(id, amount, type);

        // then
        verify(userPointRepository).insertOrUpdate(id, 70L);
        verify(pointHistoryRepository).insert(eq(id), eq(amount), eq(type), anyLong());
    }

}