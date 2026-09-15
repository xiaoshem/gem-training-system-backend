package cn.org.alan.exam.model.vo.payment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AlipayPagePayVO {
    private String orderNo;
    private String formHtml;
}
