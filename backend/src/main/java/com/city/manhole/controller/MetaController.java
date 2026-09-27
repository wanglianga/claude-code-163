package com.city.manhole.controller;

import com.city.manhole.common.ApiResponse;
import com.city.manhole.common.AuthContext;
import com.city.manhole.common.BizException;
import com.city.manhole.common.Passwords;
import com.city.manhole.entity.OwnerUnit;
import com.city.manhole.entity.User;
import com.city.manhole.enums.*;
import com.city.manhole.repository.OwnerUnitRepository;
import com.city.manhole.repository.UserRepository;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/meta")
@RequiredArgsConstructor
public class MetaController {
    private final OwnerUnitRepository unitRepo;
    private final UserRepository userRepo;

    /** 下拉枚举字典 */
    @GetMapping("/enums")
    public ApiResponse<?> enums() {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("roles", labels(Role.values()));
        r.put("eventStatus", labels(EventStatus.values()));
        r.put("emergencyStatus", labels(EmergencyStatus.values()));
        r.put("ownershipStatus", labels(OwnershipStatus.values()));
        r.put("workOrderStatus", labels(WorkOrderStatus.values()));
        r.put("compensationStatus", labels(CompensationStatus.values()));
        r.put("costBearerStatus", labels(CostBearerStatus.values()));
        r.put("problemTypes", List.of("缺失", "松动", "异响", "塌陷"));
        r.put("roadTypes", List.of("主干道", "次干道", "支路", "人行道", "小区道路"));
        r.put("dangerLevels", List.of("高", "中", "低"));
        r.put("measureTypes", List.of("警示锥", "临时盖板", "夜间反光标识"));
        r.put("professionalUnits", List.of("燃气", "电力", "通信", "供水", "排水"));
        r.put("claimantTypes", List.of("人员受伤", "车辆受损", "财产受损"));
        r.put("responsibilities", List.of("权属单位", "街道应急不力", "多方分摊", "不予赔付"));
        return ApiResponse.ok(r);
    }

    private List<Map<String, String>> labels(Enum<?>[] values) {
        return Arrays.stream(values).map(v -> {
            try {
                return Map.of("value", v.name(), "label", (String) v.getClass().getMethod("getLabel").invoke(v));
            } catch (Exception e) {
                return Map.of("value", v.name(), "label", v.name());
            }
        }).collect(Collectors.toList());
    }

    @GetMapping("/units")
    public ApiResponse<?> units() {
        AuthContext.require();
        return ApiResponse.ok(unitRepo.findAll());
    }

    @PostMapping("/units")
    public ApiResponse<?> createUnit(@RequestBody OwnerUnit unit) {
        AuthContext.requireRole(Role.ADMIN);
        unit.setId(null);
        return ApiResponse.ok(unitRepo.save(unit));
    }

    @GetMapping("/repair-teams")
    public ApiResponse<?> repairTeams() {
        AuthContext.require();
        List<Map<String, Object>> teams = userRepo.findByRole(Role.REPAIR_TEAM).stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", u.getId());
            m.put("name", u.getOrgName() != null ? u.getOrgName() : u.getName());
            return m;
        }).collect(Collectors.toList());
        return ApiResponse.ok(teams);
    }

    @GetMapping("/users")
    public ApiResponse<?> users() {
        AuthContext.requireRole(Role.ADMIN);
        List<User> all = userRepo.findAll();
        all.forEach(u -> u.setPassword(null));
        return ApiResponse.ok(all);
    }

    public record CreateUserReq(@NotBlank String username, @NotBlank String password,
                                @NotBlank String name, @NotBlank String role,
                                String orgName, Long unitId, String phone) {}

    @PostMapping("/users")
    public ApiResponse<?> createUser(@RequestBody CreateUserReq req) {
        AuthContext.requireRole(Role.ADMIN);
        if (userRepo.findByUsername(req.username()).isPresent()) throw new BizException("用户名已存在");
        User u = new User();
        u.setUsername(req.username());
        u.setPassword(Passwords.sha256(req.password()));
        u.setName(req.name());
        u.setRole(Role.valueOf(req.role()));
        u.setOrgName(req.orgName());
        u.setUnitId(req.unitId());
        u.setPhone(req.phone());
        userRepo.save(u);
        u.setPassword(null);
        return ApiResponse.ok(u);
    }
}
