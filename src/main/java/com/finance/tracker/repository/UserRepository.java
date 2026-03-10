package com.finance.tracker.repository;

import com.finance.tracker.domain.AccountType;
import com.finance.tracker.domain.User;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    @Query(
            value = """
                    select distinct u
                    from User u
                    join u.accounts a
                    join u.budgets b
                    where a.type = :accountType
                      and b.limitAmount between :minBudgetLimit and :maxBudgetLimit
                    order by u.id
                    """)
    List<User> findUsersByAccountTypeWithJpql(
            AccountType accountType, BigDecimal minBudgetLimit, BigDecimal maxBudgetLimit);

    @Query(
            value = """
                    select distinct u.*
                    from users u
                    join accounts a on a.user_id = u.id
                    join budgets b on b.user_id = u.id
                    where cast(a.type as text) = :accountType
                      and b.limit_amount between :minBudgetLimit and :maxBudgetLimit
                    order by u.id
                    """,
            nativeQuery = true)
    List<User> findUsersByAccountTypeWithNative(
            String accountType, BigDecimal minBudgetLimit, BigDecimal maxBudgetLimit);
}
