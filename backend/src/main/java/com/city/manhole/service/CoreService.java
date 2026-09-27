package com.city.manhole.service;

import com.city.manhole.common.BizException;
import com.city.manhole.entity.*;
import com.city.manhole.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class CoreService {
    private final ManholeEventRepository eventRepo;
    private final EventLogRepository logRepo;
    private final CostRecordRepository costRepo;

    private static final AtomicLong SEQ = new AtomicLong(System.currentTimeMillis() % 100000);

    public ManholeEvent event(Long id) {
        return eventRepo.findById(id).orElseThrow(() -> new BizException("事件不存在: " + id));
    }

    public void log(Long eventId, User actor, String action, String detail) {
        logRepo.save(new EventLog(eventId, actor == null ? null : actor.getId(),
                actor == null ? "系统" : actor.getName(), action, detail));
    }

    public String nextNo(String prefix) {
        return prefix + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "-" + String.format("%05d", SEQ.incrementAndGet() % 100000);
    }

    public CostRecord addCost(Long eventId, String costType, java.math.BigDecimal amount,
                              String refType, Long refId, String note) {
        CostRecord c = new CostRecord();
        c.setEventId(eventId);
        c.setCostType(costType);
        c.setAmount(amount == null ? java.math.BigDecimal.ZERO : amount);
        c.setRefType(refType);
        c.setRefId(refId);
        c.setNote(note);
        return costRepo.save(c);
    }

    public CostRecord saveCost(CostRecord c) {
        return costRepo.save(c);
    }
}
