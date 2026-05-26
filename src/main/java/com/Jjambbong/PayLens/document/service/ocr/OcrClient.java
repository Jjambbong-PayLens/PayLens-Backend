package com.Jjambbong.PayLens.document.service.ocr;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.dto.response.OcrResult;

public interface OcrClient {

    OcrEngineType getEngineType();

    OcrResult extractText(Document document);
}
