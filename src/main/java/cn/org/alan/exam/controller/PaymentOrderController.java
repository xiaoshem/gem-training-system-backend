package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.vo.payment.PaymentOrderVO;
import cn.org.alan.exam.service.IPaymentOrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@Api(tags = "培训缴费订单")
@RestController
@RequestMapping("/api/payment-orders")
public class PaymentOrderController {
    @Resource
    private IPaymentOrderService paymentOrderService;

    @ApiOperation("学员分页查询缴费订单")
    @GetMapping("/mine/paging")
    @PreAuthorize("hasAuthority('role_student')")
    public Result<IPage<PaymentOrderVO>> mine(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return paymentOrderService.mine(pageNum, pageSize, keyword, status);
    }

    @ApiOperation("管理员分页查询缴费订单")
    @GetMapping("/manage/paging")
    @PreAuthorize("hasAuthority('role_admin')")
    public Result<IPage<PaymentOrderVO>> manage(
            @RequestParam(value = "pageNum", defaultValue = "1") Integer pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") Integer pageSize,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status) {
        return paymentOrderService.manage(pageNum, pageSize, keyword, status);
    }

    @ApiOperation("学员模拟缴费")
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('role_student')")
    public Result<String> pay(@PathVariable Integer id) {
        return paymentOrderService.pay(id);
    }
}
