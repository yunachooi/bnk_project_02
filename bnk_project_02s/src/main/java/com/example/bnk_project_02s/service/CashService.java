package com.example.bnk_project_02s.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bnk_project_02s.repository.CashKrwRepository;

@Service
public class CashService {
	@Autowired
	private CashKrwRepository cashRepository;

}
