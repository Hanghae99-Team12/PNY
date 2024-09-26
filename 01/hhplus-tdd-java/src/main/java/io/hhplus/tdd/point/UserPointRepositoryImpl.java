package io.hhplus.tdd.point;

import io.hhplus.tdd.database.UserPointTable;
import org.springframework.stereotype.Repository;

@Repository
public class UserPointRepositoryImpl implements UserPointRepository {

    private final UserPointTable userPointTable;

    public UserPointRepositoryImpl(UserPointTable userPointTable) {
        this.userPointTable = userPointTable;
    }

    @Override
    public UserPoint selectById(Long id) {
        return userPointTable.selectById(id);
    }

    @Override
    public UserPoint insertOrUpdate(long id, long amount) {
        return userPointTable.insertOrUpdate(id, amount);
    }

}