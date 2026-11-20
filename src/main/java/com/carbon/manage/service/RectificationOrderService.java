package com.carbon.manage.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.carbon.manage.common.BusinessException;
import com.carbon.manage.common.PageResult;
import com.carbon.manage.common.UserContext;
import com.carbon.manage.dto.OrderActionDTO;
import com.carbon.manage.entity.EnergyData;
import com.carbon.manage.entity.RectificationOrder;
import com.carbon.manage.entity.User;
import com.carbon.manage.enums.OrderStatus;
import com.carbon.manage.enums.UserRole;
import com.carbon.manage.mapper.RectificationOrderMapper;
import com.carbon.manage.mapper.UserMapper;

@Service
@RequiredArgsConstructor
public class RectificationOrderService {

    private static final DateTimeFormatter ORDER_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final RectificationOrderMapper orderMapper;
    private final UserMapper userMapper;
    private final EnergyTypeService energyTypeService;

    /**
     * 分页查询：管理员看全部；整改负责人只看派发给自己的工单（待派发单 assignee_id 为空，天然不可见）。
     */
    public PageResult<RectificationOrder> page(int pageNum, int pageSize, String status, Long enterpriseId) {
        UserContext.LoginUser loginUser = UserContext.getRequired();
        Long assigneeId;
        if (UserRole.ADMIN.is(loginUser.role())) {
            assigneeId = null;
        } else if (UserRole.OPERATOR.is(loginUser.role())) {
            assigneeId = loginUser.id();
        } else {
            throw new BusinessException(403, "无权限执行该操作");
        }
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);
        long total = orderMapper.countPage(status, enterpriseId, assigneeId);
        if (total == 0) {
            return PageResult.of(0, safePageNum, safePageSize, List.of());
        }
        int offset = (safePageNum - 1) * safePageSize;
        return PageResult.of(total, safePageNum, safePageSize,
                orderMapper.selectPage(status, enterpriseId, assigneeId, offset, safePageSize));
    }

    public RectificationOrder getById(Long id) {
        UserContext.LoginUser loginUser = UserContext.getRequired();
        RectificationOrder order = loadOrder(id);
        boolean visible = UserRole.ADMIN.is(loginUser.role())
                || (UserRole.OPERATOR.is(loginUser.role())
                && Objects.equals(order.getAssigneeId(), loginUser.id()));
        if (!visible) {
            throw new BusinessException(403, "无权查看该工单");
        }
        return order;
    }

    private RectificationOrder loadOrder(Long id) {
        RectificationOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException(404, "整改工单不存在");
        }
        return order;
    }

    /**
     * AI 诊断成功后自动建单（待派发），由 AiDiagPersistService 在事务中调用。
     */
    @Transactional
    public RectificationOrder createFromDiagnosis(EnergyData data, String suggestion) {
        RectificationOrder exist = orderMapper.selectByEnergyDataId(data.getId());
        if (exist != null) {
            return exist;
        }
        RectificationOrder order = new RectificationOrder();
        order.setOrderNo(generateOrderNo());
        order.setEnterpriseId(data.getEnterpriseId());
        order.setEnergyDataId(data.getId());
        order.setTitle("【碳排超标整改】" + data.getEnterpriseName()
                + " - " + energyTypeService.labelOf(data.getEnergyType()) + "排放超标");
        order.setSuggestion(suggestion);
        order.setStatus(OrderStatus.PENDING_DISPATCH.name());
        order.setDeadline(LocalDateTime.now().plusDays(3));
        orderMapper.insert(order);
        return order;
    }

    /** 管理员派发：待派发 -> 整改中，必须指定启用的整改负责人账号。 */
    @Transactional
    public RectificationOrder dispatch(Long id, OrderActionDTO dto) {
        UserContext.requireRole(UserRole.ADMIN);
        if (dto.getAssigneeId() == null) {
            throw new BusinessException(400, "派单必须指定整改负责人");
        }
        User operator = userMapper.selectById(dto.getAssigneeId());
        if (operator == null
                || !UserRole.OPERATOR.is(operator.getRole())
                || operator.getStatus() == null || operator.getStatus() != 1) {
            throw new BusinessException(400, "请选择有效的整改负责人");
        }
        String assigneeName = StringUtils.hasText(operator.getNickname())
                ? operator.getNickname() : operator.getUsername();
        LocalDateTime deadline = dto.getDeadline() != null
                ? dto.getDeadline() : LocalDateTime.now().plusDays(3);
        return transition(id, OrderStatus.PENDING_DISPATCH, OrderStatus.RECTIFYING,
                operator.getId(), assigneeName, null, null, deadline);
    }

    /** 整改负责人提交：整改中 -> 待审核，只能提交分配给自己的工单。 */
    @Transactional
    public RectificationOrder submit(Long id, OrderActionDTO dto) {
        UserContext.LoginUser loginUser = UserContext.requireRole(UserRole.OPERATOR);
        RectificationOrder order = loadOrder(id);
        if (!Objects.equals(order.getAssigneeId(), loginUser.id())) {
            throw new BusinessException(403, "只能提交分配给自己的工单");
        }
        if (!StringUtils.hasText(dto.getRectifyResult())) {
            throw new BusinessException(400, "提交审核前必须填写整改结果");
        }
        return transition(id, OrderStatus.RECTIFYING, OrderStatus.PENDING_AUDIT,
                null, null, dto.getRectifyResult(), null, null);
    }

    /** 管理员审核通过：待审核 -> 已完成。 */
    @Transactional
    public RectificationOrder auditPass(Long id, OrderActionDTO dto) {
        UserContext.requireRole(UserRole.ADMIN);
        if (!StringUtils.hasText(dto.getAuditRemark())) {
            throw new BusinessException(400, "审核意见不能为空");
        }
        return transition(id, OrderStatus.PENDING_AUDIT, OrderStatus.COMPLETED,
                null, null, null, dto.getAuditRemark(), null);
    }

    /** 管理员审核不通过：待审核 -> 整改中，保留负责人并重置 3 天期限，驳回原因写入审核意见。 */
    @Transactional
    public RectificationOrder auditReject(Long id, OrderActionDTO dto) {
        UserContext.requireRole(UserRole.ADMIN);
        if (!StringUtils.hasText(dto.getAuditRemark())) {
            throw new BusinessException(400, "驳回必须填写驳回原因");
        }
        return transition(id, OrderStatus.PENDING_AUDIT, OrderStatus.RECTIFYING,
                null, null, null, dto.getAuditRemark(), LocalDateTime.now().plusDays(3));
    }

    /**
     * 状态转移核心方法：先锁定期望的起始状态（区分派发与驳回两条进入整改中的边），
     * 再校验状态机合法性，最后按旧状态做 CAS 更新防并发篡改。
     */
    private RectificationOrder transition(Long id, OrderStatus expectedFrom, OrderStatus target,
                                          Long assigneeId, String assignee, String rectifyResult,
                                          String auditRemark, LocalDateTime deadline) {
        RectificationOrder order = loadOrder(id);
        OrderStatus from = OrderStatus.of(order.getStatus());
        if (from != expectedFrom) {
            throw new BusinessException(400,
                    "当前状态为" + from.getLabel() + "，无法执行该操作");
        }
        if (!from.canGoTo(target)) {
            throw new BusinessException(400,
                    "非法状态流转：" + from.getLabel() + " -> " + target.getLabel());
        }
        int updated = orderMapper.changeStatusCas(id, from.name(), target.name(),
                assigneeId, assignee, rectifyResult, auditRemark, deadline);
        if (updated == 0) {
            throw new BusinessException(409, "工单状态已被他人修改，请刷新后重试");
        }
        return loadOrder(id);
    }

    private String generateOrderNo() {
        return "RO" + ORDER_NO_FORMATTER.format(LocalDateTime.now())
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }
}
