package io.hhplus.tdd.point;

public class PointUtils {

    // 아이디 유효성 확인
    public static UserPoint validateUserId(long id) {
        if (id <= 0) {
            return new UserPoint(0, 0, 0);
        }
        return null;
    }

}