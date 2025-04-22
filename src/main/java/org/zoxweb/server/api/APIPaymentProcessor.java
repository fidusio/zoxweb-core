package org.zoxweb.server.api;

import org.zoxweb.shared.accounting.FinancialTransactionDAO;
import org.zoxweb.shared.api.APIServiceProvider;
import org.zoxweb.shared.util.AppID;

public interface APIPaymentProcessor<P, S>
        extends APIServiceProvider<P, S>, AppID<String> {

    /**
     * Create transaction.
     * @param financialTransactionDAO
     * @return
     */
    FinancialTransactionDAO createTransaction(FinancialTransactionDAO financialTransactionDAO);

    /**
     * Lookup transaction.
     * @param financialTransactionDAO
     * @return
     */
    FinancialTransactionDAO lookupTransaction(FinancialTransactionDAO financialTransactionDAO);

    /**
     * Update transaction.
     * @param financialTransactionDAO
     * @return
     */
    FinancialTransactionDAO updateTransaction(FinancialTransactionDAO financialTransactionDAO);

    /**
     * Cancel transaction.
     * @param financialTransactionDAO
     * @return
     */
    FinancialTransactionDAO cancelTransaction(FinancialTransactionDAO financialTransactionDAO);

    /**
     * Capture transaction (e.g. authorize $100.00 at beginning of transaction
     * then deduct total amount and release remaining amount).
     * @param financialTransactionDAO
     * @return
     */
    FinancialTransactionDAO captureTransaction(FinancialTransactionDAO financialTransactionDAO);

    /**
     * Refund transaction (after cancel transaction is no longer permitted).
     * @param financialTransactionDAO
     * @return
     */
    FinancialTransactionDAO refundTransaction(FinancialTransactionDAO financialTransactionDAO);

}