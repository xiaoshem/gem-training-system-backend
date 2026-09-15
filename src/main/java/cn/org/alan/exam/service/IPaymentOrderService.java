package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.PaymentOrder;
import cn.org.alan.exam.model.vo.payment.AlipayPagePayVO;
import cn.org.alan.exam.model.vo.payment.AlipayPaymentQueryVO;
import cn.org.alan.exam.model.vo.payment.PaymentCapabilitiesVO;
import cn.org.alan.exam.model.vo.payment.PaymentOrderVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Map;

public interface IPaymentOrderService extends IService<PaymentOrder> {
    Result<IPage<PaymentOrderVO>> mine(Integer pageNum, Integer pageSize, String keyword, String status);
    Result<IPage<PaymentOrderVO>> manage(Integer pageNum, Integer pageSize, String keyword, String status);
    Result<String> pay(Integer id);
    Result<PaymentCapabilitiesVO> capabilities();
    Result<AlipayPagePayVO> createAlipayPagePay(Integer id);
    Result<AlipayPaymentQueryVO> queryAlipayPayment(String orderNo);
    boolean handleAlipayNotification(Map<String, String> parameters);
    String handleAlipayReturn(Map<String, String> parameters);
}
