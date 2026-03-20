package com.stableflow.invoice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.stableflow.invoice.dto.ActivateInvoiceRequestDto;
import com.stableflow.invoice.dto.CreateInvoiceRequestDto;
import com.stableflow.invoice.dto.UpdateInvoiceRequestDto;
import com.stableflow.invoice.entity.Invoice;
import com.stableflow.invoice.enums.ExceptionTagEnum;
import com.stableflow.invoice.vo.InvoiceDetailVo;
import com.stableflow.invoice.vo.InvoiceListItemVo;
import com.stableflow.invoice.vo.PaymentInfoVo;
import com.stableflow.invoice.vo.PaymentStatusVo;
import com.stableflow.invoice.vo.PublicPaymentPageVo;
import com.stableflow.system.api.PageResult;

public interface InvoiceService extends IService<Invoice> {

    /** Create an invoice and snapshot its payment request / 创建账单并生成支付请求快照 */
    InvoiceDetailVo createInvoice(CreateInvoiceRequestDto request);

    /** Activate a draft invoice into pending payment state / 将草稿账单激活为待支付状态 */
    InvoiceDetailVo activateInvoice(ActivateInvoiceRequestDto request);

    /** Update an editable invoice owned by the current merchant / 更新当前商家可编辑的账单 */
    InvoiceDetailVo updateInvoice(UpdateInvoiceRequestDto request);

    /** List invoices of the current merchant with pagination and optional status/exception-tag filtering / 分页查询当前商家的账单列表，可按状态和异常标签过滤 */
    PageResult<InvoiceListItemVo> listInvoices(String status, ExceptionTagEnum exceptionTag, int page, int size);

    /** Return invoice detail owned by the current merchant / 返回当前商家拥有的账单详情 */
    InvoiceDetailVo getInvoiceDetail(Long invoiceId);

    /** Return reusable payment information for an invoice / 返回账单的可复用支付信息 */
    PaymentInfoVo getPaymentInfo(Long invoiceId);

    /** Return the current payment status snapshot of an invoice / 返回账单当前支付状态快照 */
    PaymentStatusVo getPaymentStatus(Long invoiceId);

    /** Return public payment page info by publicId, no auth required / 基于 publicId 返回公共支付页信息，无需登录 */
    PublicPaymentPageVo getPublicPaymentPage(String publicId);

    /** Expire pending invoices that have passed their effective window / 将超过有效期的待支付账单更新为已过期 */
    int expirePendingInvoices();
}
