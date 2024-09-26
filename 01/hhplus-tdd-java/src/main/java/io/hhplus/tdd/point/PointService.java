package io.hhplus.tdd.point;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PointService {

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointService(UserPointRepository userPointRepository, PointHistoryRepository pointHistoryRepository) {
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    /*
    공동 제약 조건
	    - 충전하고자 하는 금액은 항상 0보다 커야 한다.
		- 사용자의 포인트는 음수면 안된다.
    */

    /**
     * TODO - 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
     */
    public UserPoint getUserPoint(long id) {
        /*
	    기능
		    - 사용자의 포인트를 조회한다. = 사용자가 없으면 실패한다. -> 새로 생성됨
		    - 결과를 반환한다.
        */
        UserPoint userPoint = userPointRepository.selectById(id);
        System.out.println(userPoint.id());
        return userPoint;
    }

    /**
     * TODO - 특정 유저의 포인트 충전/이용 내역을 조회하는 기능을 작성해주세요.
     */
    public List<PointHistory> getUserPointHistoryList(long id) {
        /*
	    기능
		    - 사용자의 포인트를 이력 조회한다. = 사용자가 없으면 실패한다. -> 새로 생성됨
		    - 결과를 반환한다.
        */
        UserPoint userPoint = userPointRepository.selectById(id);
        List<PointHistory> pointHistoryList = pointHistoryRepository.selectAllByUserId(userPoint.id());
        for(PointHistory history : pointHistoryList) {
            System.out.println(history.toString());
        }
        return pointHistoryList;
    }

    /**
     * TODO - 특정 유저의 포인트를 충전/사용하는 기능을 작성해주세요.
     */
    public UserPoint useUserPoint(long id, long amount, TransactionType type) {
        /*
	    기능
		    - 사용자의 포인트를 조회한다. = 사용자가 없으면 실패한다. -> 새로 생성됨
		    - 사용할 포인트는 0보다 커야한다.
		    - 사용자의 포인트와 입력받은 금액을 빼고 남은 금액이 음수면 안된다.
		    - 결과를 저장(사용자 포인트 이력에 결과도 별도로 저장)하고, 그 결과를 반환한다.
        */
        UserPoint userPoint = userPointRepository.selectById(id);
        if (amount < 0) throw new RuntimeException();

        long resultPoint = 0;
        if (type.equals(TransactionType.CHARGE)) {
            // 충전시
            resultPoint = userPoint.point() + amount;
        } else if (type.equals(TransactionType.USE)) {
            // 사용시
            resultPoint = userPoint.point() - amount;
            if (resultPoint < 0) throw new RuntimeException();
        } else {
            // 그 외
            throw new RuntimeException();
        }

        userPointRepository.insertOrUpdate(userPoint.id(), resultPoint);                                            // UserPoint
        pointHistoryRepository.insert(userPoint.id(), amount, TransactionType.USE, System.currentTimeMillis());     // pointHistory

        return userPointRepository.selectById(userPoint.id());
    }

}