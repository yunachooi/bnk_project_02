package com.example.bnk_project_02s.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.bnk_project_02s.dto.PushItem;
import com.example.bnk_project_02s.entity.BnkPush;
import com.example.bnk_project_02s.repository.BnkPushRepository;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/app/inbox")
@RequiredArgsConstructor
public class InboxController {
	
	private final BnkPushRepository repo;
	
	@GetMapping
	public List<PushItem> poll(@RequestParam(name="uid") String uid){
		List<BnkPush> rows = repo.findTop20ByUidAndConsumedOrderByIdAsc(uid, false);
		List<PushItem> out = new ArrayList<>();
		for(BnkPush r : rows) {
			PushItem i = new PushItem();
			i.setId(r.getId());
            i.setKind(r.getKind());
            i.setTitle(r.getTitle());
            i.setBody(r.getBody());
            i.setDataJson(r.getDataJson());
            out.add(i);
		}
		return out;
	}
	
	@PostMapping("/ack")
	@Transactional
	public Map<String, Object> ack(@RequestParam(name = "uid") String uid, @RequestBody List<Long> ids){
		int updated = repo.ack(uid, (ids == null) ? List.of() : ids);
		return Map.of("acked", updated);
		
	}

}
