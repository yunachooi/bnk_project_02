package com.example.bnk_project_02s.repository;

import com.example.bnk_project_02s.entity.Currency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CurrencyRepository extends JpaRepository<Currency, String> {

    Optional<Currency> findByCuno(String cuno);

    List<Currency> findByCunoIn(Collection<String> cunoList);
    
    Optional<Currency> findByCuname(String cuname);
}
