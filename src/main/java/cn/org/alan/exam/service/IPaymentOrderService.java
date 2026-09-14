package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.entity.PaymentOrder;
import cn.org.alan.exam.model.vo.payment.PaymentOrderVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface IPaymentOrderService extends IService<PaymentOrder> {
    Result<IPage<PaymentOrderVO>> mine(Integer pageNum, Integer pageSize, String keyword, String status);
    Result<IPage<PaymentOrderVO>> manage(Integer pageNum, Integer pageSize, String keyword, String status);
    Result<String> pay(Integer id);
}
