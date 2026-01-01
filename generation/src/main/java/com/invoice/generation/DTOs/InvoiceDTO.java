package com.invoice.generation;

import java.util.List;

public class InvoiceDTO {

    public String customerName;
    public String customerPhone;
    public String customerAddress;
    public String customerEmail;
    
    public String invoiceStatus;   
    public String invoiceDate;
   
    public String ownerMessage;

    public List<ItemDTO> items;

    public boolean applyOverallDiscount;
    public double overallDiscount;
    public String overallDiscountType;

    public double adjustmentAmount;
    public String adjustmentAmountType;

    

    public String paymentMethod;      
    public String paymentDetails;     
    public String issuedBy; 

    public boolean enableLogging = true; 

}
