package io.hhplus.tdd.point;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class PointService {

    private final Lock lock = new ReentrantLock();

    private final UserPointRepository userPointRepository;
    private final PointHistoryRepository pointHistoryRepository;

    public PointService(UserPointRepository userPointRepository, PointHistoryRepository pointHistoryRepository) {
        this.userPointRepository = userPointRepository;
        this.pointHistoryRepository = pointHistoryRepository;
    }

    /*
    공동 제약 조건
	    - 충전/사용해야하는 포인트는 무조건 0보다 커야 한다.
	    - 사용해야 되는 포인트는 1,000단위로 사용된다.
    */

    public UserPoint getUserPoint(long id) {
        /*
	    기능
		    - 특정 사용자 포인트 조회 => 사용자 없으면 새로 생성
		    - 결과 반환
        */
        UserPoint userPoint = userPointRepository.selectById(id);

        // 사용자 없으면 새로 생성
        if (userPoint == null) {
            userPoint = UserPoint.empty(id);
            userPointRepository.insertOrUpdate(userPoint.id(), userPoint.point());
        }

        // 결과 반환
        return userPoint;
    }

    public List<PointHistory> getUserPointHistoryList(long id) {
        /*
	    기능
		    - 특정 사용자 포인트 조회
		    - 해당 사용자 포인트 이력 조회
		    - 결과 반환
        */
        UserPoint userPoint = getUserPoint(id);
        return pointHistoryRepository.selectAllByUserId(userPoint.id());
    }

    public UserPoint useUserPoint(long id, long amount, TransactionType type) {
        /*
	    기능
	        - 특정 사용자 포인트 조회
	        - 입력된 포인트 수는 0이상인 양수
	        - TransactionType 따른 충전/사용 조건 진행
	            - TransactionType.USER 사용시 : 사용자 포인트 + 입력받은 금액 = 양수
	        - 해당 사용자의 포인트 업데이트
	        - 해당 사용자의 포인트 이력 저장
	        - 결과 반환
        */

        // Lock 적용
        lock.lock();

        try {
            UserPoint userPoint = getUserPoint(id);

            if (amount < 0) throw new IllegalArgumentException("포인트가 유효하지 않습니다.");

            long resultPoint = 0;
            if (type.equals(TransactionType.CHARGE)) {
                // 충전시
                resultPoint = userPoint.point() + amount;
            } else if (type.equals(TransactionType.USE)) {
                long amountToUse = (amount / 1000 * 1000);              // 사용시 1,000단위로 사용됨
                if (resultPoint < 0) throw new IllegalArgumentException("사용하고자하는 포인트가 충분하지 않습니다.");
                resultPoint = userPoint.point() - amountToUse;
            } else {
                // 그 외
                throw new IllegalArgumentException("유효하지 않는 종류입니다.");
            }

            userPointRepository.insertOrUpdate(userPoint.id(), resultPoint);                             // UserPoint
            pointHistoryRepository.insert(userPoint.id(), amount, type, System.currentTimeMillis());     // pointHistory

            return userPointRepository.selectById(userPoint.id());
        } finally {
            lock.unlock();
        }
    }

}