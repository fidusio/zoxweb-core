package org.zoxweb.server.api;

import org.zoxweb.shared.accounting.FinancialTransaction;
import org.zoxweb.shared.api.APIServiceProvider;
import org.zoxweb.shared.util.AppID;

public interface APIPaymentProcessor<P, S>
        extends APIServiceProvider<P, S>, AppID<String> {

    /**
     * Create transaction.
     * @param financialTransaction
     * @return
     */
    FinancialTransaction createTransaction(FinancialTransaction financialTransaction);

    /**
     * Lookup transaction.
     * @param financialTransaction
     * @return
     */
    FinancialTransaction lookupTransaction(FinancialTransaction financialTransaction);

    /**
     * Update transaction.
     * @param financialTransaction
     * @return
     */
    FinancialTransaction updateTransaction(FinancialTransaction financialTransaction);

    /**
     * Cancel transaction.
     * @param financialTransaction
     * @return
     */
    FinancialTransaction cancelTransaction(FinancialTransaction financialTransaction);

    /**
     * Capture transaction (e.g. authorize $100.00 at beginning of transaction
     * then deduct total amount and release remaining amount).
     * @param financialTransaction
     * @return
     */
    FinancialTransaction captureTransaction(FinancialTransaction financialTransaction);

    /**
     * Refund transaction (after cancel transaction is no longer permitted).
     * @param financialTransaction
     * @return
     */
    FinancialTransaction refundTransaction(FinancialTransaction financialTransaction);

}