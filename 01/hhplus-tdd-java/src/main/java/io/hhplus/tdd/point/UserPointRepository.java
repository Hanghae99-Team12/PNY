package io.hhplus.tdd.point;

public interface UserPointRepository {

    UserPoint selectById(Long id);
    UserPoint insertOrUpdate(long id, long amount);

}