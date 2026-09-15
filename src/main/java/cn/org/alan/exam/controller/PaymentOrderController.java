package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.vo.payment.AlipayPagePayVO;
import cn.org.alan.exam.model.vo.payment.AlipayPaymentQueryVO;
import cn.org.alan.exam.model.vo.payment.PaymentCapabilitiesVO;
import cn.org.alan.exam.model.vo.payment.PaymentOrderVO;
import cn.org.alan.exam.service.IPaymentOrderService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
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

    @ApiOperation("查询可用支付方式")
    @GetMapping("/capabilities")
    @PreAuthorize("hasAuthority('role_student')")
    public Result<PaymentCapabilitiesVO> capabilities() {
        return paymentOrderService.capabilities();
    }

    @ApiOperation("学员本地模拟缴费")
    @PutMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('role_student')")
    public Result<String> pay(@PathVariable Integer id) {
        return paymentOrderService.pay(id);
    }

    @ApiOperation("创建支付宝沙箱电脑网站支付")
    @PostMapping("/{id}/alipay/page-pay")
    @PreAuthorize("hasAuthority('role_student')")
    public Result<AlipayPagePayVO> createAlipayPagePay(@PathVariable Integer id) {
        return paymentOrderService.createAlipayPagePay(id);
    }

    @ApiOperation("主动查询支付宝沙箱支付结果")
    @PostMapping("/alipay/query/{orderNo}")
    @PreAuthorize("hasAuthority('role_student')")
    public Result<AlipayPaymentQueryVO> queryAlipayPayment(@PathVariable String orderNo) {
        return paymentOrderService.queryAlipayPayment(orderNo);
    }

    @ApiOperation("支付宝沙箱异步通知")
    @PostMapping(value = "/alipay/notify", produces = MediaType.TEXT_PLAIN_VALUE)
    public String alipayNotify(HttpServletRequest request) {
        try {
            return paymentOrderService.handleAlipayNotification(parameters(request)) ? "success" : "failure";
        } catch (RuntimeException exception) {
            log.warn("支付宝沙箱异步通知处理失败: {}", exception.getMessage());
            return "failure";
        }
    }

    @ApiOperation("支付宝沙箱同步返回")
    @GetMapping("/alipay/return")
    public void alipayReturn(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect(paymentOrderService.handleAlipayReturn(parameters(request)));
    }

    private Map<String, String> parameters(HttpServletRequest request) {
        Map<String, String> result = new LinkedHashMap<>();
        request.getParameterMap().forEach((key, values) -> {
            if (values != null && values.length > 0) {
                result.put(key, String.join(",", values));
            }
        });
        return result;
    }
}
