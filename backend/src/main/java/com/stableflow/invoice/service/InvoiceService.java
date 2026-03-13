package com.stableflow.invoice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stableflow.invoice.dto.CreateInvoiceRequestDto;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.vo.InvoiceDetailVo;
import com.stableflow.invoice.vo.InvoiceListItemVo;
import com.stableflow.invoice.vo.PaymentInfoVo;
import java.util.List;

public interface InvoiceService extends IService<Invoice> {

    /** Create an invoice and snapshot its payment request / 创建账单并生成支付请求快照 */
    InvoiceDetailVo createInvoice(CreateInvoiceRequestDto request);

    /** List invoices of the current merchant with optional status filtering / 查询当前商家的账单列表，可按状态过滤 */
    List<InvoiceListItemVo> listInvoices(String status);

    /** Return invoice detail owned by the current merchant / 返回当前商家拥有的账单详情 */
    InvoiceDetailVo getInvoiceDetail(Long invoiceId);

    /** Return reusable payment information for an invoice / 返回账单的可复用支付信息 */
    PaymentInfoVo getPaymentInfo(Long invoiceId);
}
