package cn.org.alan.exam.mapper;

import cn.org.alan.exam.model.entity.PaymentOrder;
import cn.org.alan.exam.model.vo.payment.PaymentOrderVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {
    Page<PaymentOrderVO> selectPaymentOrderPage(Page<PaymentOrderVO> page,
                                                 @Param("studentId") Integer studentId,
                                                 @Param("keyword") String keyword,
                                                 @Param("status") String status);

    PaymentOrder selectByIdForUpdate(@Param("id") Integer id);

    PaymentOrder selectByEnrollmentIdForUpdate(@Param("enrollmentId") Integer enrollmentId);
}
