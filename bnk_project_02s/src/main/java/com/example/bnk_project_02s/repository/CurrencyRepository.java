package com.example.bnk_project_02s.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.bnk_project_02s.entity.Currency;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {

	Optional<Currency> findByCuname(String cuname);

    // <<< 추가
    Optional<Currency> findByCunameIgnoreCase(String cuname);
}
