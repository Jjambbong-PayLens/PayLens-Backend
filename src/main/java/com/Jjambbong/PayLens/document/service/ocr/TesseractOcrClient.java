package com.Jjambbong.PayLens.document.service.ocr;

import com.Jjambbong.PayLens.document.domain.Document;
import com.Jjambbong.PayLens.document.dto.response.OcrLine;
import com.Jjambbong.PayLens.document.dto.response.OcrResult;
import com.Jjambbong.PayLens.document.service.AnalyzeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TesseractOcrClient implements OcrClient {

    private final AnalyzeService analyzeService;

    @Override
    public OcrEngineType getEngineType() {
        return OcrEngineType.TESSERACT;
    }

    @Override
    public OcrResult extractText(Document document) {
        long start = System.currentTimeMillis();

        Path tempInput = null;
        Path tempOutput = null;
        Path outputTxt = null;

        try {
            byte[] fileBytes = analyzeService.getDocumentBytes(document);

            tempInput = Files.createTempFile("ocr-input-", ".png");
            tempOutput = Files.createTempFile("ocr-output-", "");

            Files.write(tempInput, fileBytes);

            Process process = new ProcessBuilder(
                    "tesseract",
                    tempInput.toAbsolutePath().toString(),
                    tempOutput.toAbsolutePath().toString(),
                    "-l",
                    "kor+eng"
            ).redirectErrorStream(true).start();

            int exitCode = process.waitFor();

            outputTxt = Path.of(tempOutput.toAbsolutePath().toString() + ".txt");
            String text = Files.exists(outputTxt) ? Files.readString(outputTxt) : "";

            long elapsed = System.currentTimeMillis() - start;

            if (exitCode != 0) {
                return new OcrResult(
                        OcrEngineType.TESSERACT,
                        "",
                        0.0,
                        elapsed,
                        List.of(new OcrLine("Tesseract 실행 실패", 0.0))
                );
            }

            return new OcrResult(
                    OcrEngineType.TESSERACT,
                    text,
                    null,
                    elapsed,
                    List.of(new OcrLine(text, null))
            );

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;

            return new OcrResult(
                    OcrEngineType.TESSERACT,
                    "",
                    0.0,
                    elapsed,
                    List.of(new OcrLine("Tesseract 오류: " + e.getMessage(), 0.0))
            );

        } finally {
            deleteQuietly(tempInput);
            deleteQuietly(tempOutput);
            deleteQuietly(outputTxt);
        }
    }

    private void deleteQuietly(Path path) {
        try {
            if (path != null) {
                Files.deleteIfExists(path);
            }
        } catch (Exception ignored) {
        }
    }
}
