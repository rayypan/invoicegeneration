package com.invoice.generation.Service;

import org.springframework.stereotype.Service;

import com.invoice.generation.DTOs.InvoiceDTO;
import com.invoice.generation.DTOs.ItemDTO;

@Service
public class InvoiceService {

    private double applyDiscount(double amount, double discount, String type) {
        if ("PERCENT".equals(type)) {
            return amount - (amount * discount / 100);
        }
        return amount - discount;
    }

    public double calculatePayable(InvoiceDTO invoice) {

        double total = 0;

        for (ItemDTO item : invoice.items) {

            // unit price × quantity
            double itemTotal = item.price * item.quantity;

            // apply item-level discount
            double discountedItemTotal = applyDiscount(
                    itemTotal,
                    item.discount,
                    item.discountType
            );

            total += discountedItemTotal;
        }

        // apply overall discount (backend)
        if (invoice.applyOverallDiscount) {
            total = applyDiscount(
                    total,
                    invoice.overallDiscount,
                    invoice.overallDiscountType
            );
        }

        // apply adjusment discount
        total = applyDiscount(
                total,
                invoice.adjustmentAmount,
                invoice.adjustmentAmountType
        );

        return total;
    }
}
