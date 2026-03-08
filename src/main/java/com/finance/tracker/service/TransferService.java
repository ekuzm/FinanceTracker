package com.finance.tracker.service;

import com.finance.tracker.dto.request.TransferRequest;
import com.finance.tracker.dto.response.TransferResponse;

import java.util.List;
import java.util.UUID;

public interface TransferService {

    TransferResponse getTransferById(UUID id);

    List<TransferResponse> getAllTransfers();

    TransferResponse createTransferTx(TransferRequest request, boolean failAfterDebit);

    TransferResponse createTransferNoTx(TransferRequest request, boolean failAfterDebit);

    TransferResponse updateTransfer(UUID id, TransferRequest request);

    void deleteTransfer(UUID id);
}
