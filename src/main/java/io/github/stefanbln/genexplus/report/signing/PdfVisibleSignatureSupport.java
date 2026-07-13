package io.github.stefanbln.genexplus.report.signing;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationWidget;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceDictionary;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAppearanceStream;
import org.apache.pdfbox.pdmodel.interactive.digitalsignature.PDSignature;
import org.apache.pdfbox.pdmodel.interactive.form.PDAcroForm;
import org.apache.pdfbox.pdmodel.interactive.form.PDField;
import org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Builds a visible signature appearance stream for PDF viewers.
 *
 * <p>Adapted from the Apache PDFBox {@code CreateVisibleSignature2} example (Apache License 2.0).
 * Without a visual appearance, PDFBox creates a zero-size widget that many viewers ignore.
 */
final class PdfVisibleSignatureSupport {

    static final float DEFAULT_WIDTH = 220f;
    static final float DEFAULT_HEIGHT = 72f;
    static final float DEFAULT_MARGIN = 36f;
    static final int LAST_PAGE = -1;

    private static final DateTimeFormatter SIGN_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z").withZone(ZoneId.systemDefault());

    private PdfVisibleSignatureSupport() {}

    static int resolvePageIndex(PDDocument document, int configuredPage) {
        int pageCount = document.getNumberOfPages();
        if (pageCount == 0) {
            throw new IllegalStateException("Cannot sign a PDF with no pages");
        }
        if (configuredPage == LAST_PAGE) {
            return pageCount - 1;
        }
        if (configuredPage < 0 || configuredPage >= pageCount) {
            throw new IllegalArgumentException(
                    "signing.visible.page must be 0.." + (pageCount - 1) + " or -1 for last page");
        }
        return configuredPage;
    }

    static PDRectangle createBottomRightRectangle(PDDocument document, int pageIndex) {
        PDPage page = document.getPage(pageIndex);
        PDRectangle pageRect = page.getCropBox();
        float width = DEFAULT_WIDTH;
        float height = DEFAULT_HEIGHT;
        float margin = DEFAULT_MARGIN;
        float humanX = pageRect.getWidth() - width - margin;
        float humanY = pageRect.getHeight() - height - margin;
        return toPdfRectangle(page, humanX, humanY, width, height);
    }

    static InputStream createVisualSignatureTemplate(
            PDDocument sourceDocument, int pageIndex, PDRectangle rect, PDSignature signature)
            throws IOException {
        try (PDDocument template = new PDDocument()) {
            PDPage templatePage = new PDPage(sourceDocument.getPage(pageIndex).getMediaBox());
            template.addPage(templatePage);

            PDAcroForm acroForm = new PDAcroForm(template);
            template.getDocumentCatalog().setAcroForm(acroForm);
            acroForm.setSignaturesExist(true);
            acroForm.setAppendOnly(true);
            acroForm.getCOSObject().setDirect(true);

            PDSignatureField signatureField = new PDSignatureField(acroForm);
            List<PDAnnotationWidget> widgets = signatureField.getWidgets();
            PDAnnotationWidget widget = widgets.getFirst();
            acroForm.getFields().add(signatureField);
            widget.setRectangle(rect);

            PDStream stream = new PDStream(template);
            PDFormXObject form = new PDFormXObject(stream);
            PDResources resources = new PDResources();
            form.setResources(resources);
            form.setFormType(1);

            PDRectangle bbox = new PDRectangle(rect.getWidth(), rect.getHeight());
            float height = bbox.getHeight();
            Matrix initialScale = null;
            switch (sourceDocument.getPage(pageIndex).getRotation()) {
                case 90 -> {
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(1));
                    initialScale = Matrix.getScaleInstance(
                            bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
                    height = bbox.getWidth();
                }
                case 180 -> form.setMatrix(AffineTransform.getQuadrantRotateInstance(2));
                case 270 -> {
                    form.setMatrix(AffineTransform.getQuadrantRotateInstance(3));
                    initialScale = Matrix.getScaleInstance(
                            bbox.getWidth() / bbox.getHeight(), bbox.getHeight() / bbox.getWidth());
                    height = bbox.getWidth();
                }
                default -> {
                }
            }
            form.setBBox(bbox);

            PDAppearanceDictionary appearance = new PDAppearanceDictionary();
            appearance.getCOSObject().setDirect(true);
            PDAppearanceStream appearanceStream = new PDAppearanceStream(form.getCOSObject());
            appearance.setNormalAppearance(appearanceStream);
            widget.setAppearance(appearance);

            drawAppearance(template, appearanceStream, signature, initialScale, height);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            template.save(output);
            return new ByteArrayInputStream(output.toByteArray());
        }
    }

    private static void drawAppearance(
            PDDocument template,
            PDAppearanceStream appearanceStream,
            PDSignature signature,
            Matrix initialScale,
            float height) throws IOException {
        PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

        try (PDPageContentStream content = new PDPageContentStream(template, appearanceStream)) {
            if (initialScale != null) {
                content.transform(initialScale);
            }

            float boxWidth = appearanceStream.getBBox().getWidth();
            float boxHeight = appearanceStream.getBBox().getHeight();

            content.setNonStrokingColor(new Color(245, 245, 245));
            content.addRect(0, 0, boxWidth, boxHeight);
            content.fill();

            content.setStrokingColor(new Color(60, 60, 60));
            content.setLineWidth(1f);
            content.addRect(0.5f, 0.5f, boxWidth - 1f, boxHeight - 1f);
            content.stroke();

            float fontSize = 8f;
            float leading = fontSize * 1.35f;
            float x = 8f;
            float y = height - leading;

            content.beginText();
            content.setFont(regular, fontSize);
            content.setNonStrokingColor(Color.DARK_GRAY);
            content.newLineAtOffset(x, y);
            content.setLeading(leading);
            content.showText("Digitally signed by");

            content.setFont(bold, fontSize + 1f);
            content.newLine();
            String signer = signature.getName() == null ? "Unknown signer" : signature.getName();
            content.showText(truncate(signer, 34));

            content.setFont(regular, fontSize);
            content.newLine();
            if (signature.getSignDate() != null) {
                content.showText(SIGN_DATE_FORMAT.format(signature.getSignDate().toInstant()));
            }

            String reason = signature.getReason();
            if (reason != null && !reason.isBlank()) {
                content.newLine();
                content.showText(truncate(reason, 40));
            }
            content.endText();
        }
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 1) + "…";
    }

    private static PDRectangle toPdfRectangle(PDPage page, float humanX, float humanY, float width, float height) {
        PDRectangle pageRect = page.getCropBox();
        PDRectangle rect = new PDRectangle();
        switch (page.getRotation()) {
            case 90 -> {
                rect.setLowerLeftY(humanX);
                rect.setUpperRightY(humanX + width);
                rect.setLowerLeftX(humanY);
                rect.setUpperRightX(humanY + height);
            }
            case 180 -> {
                rect.setUpperRightX(pageRect.getWidth() - humanX);
                rect.setLowerLeftX(pageRect.getWidth() - humanX - width);
                rect.setLowerLeftY(humanY);
                rect.setUpperRightY(humanY + height);
            }
            case 270 -> {
                rect.setLowerLeftY(pageRect.getHeight() - humanX - width);
                rect.setUpperRightY(pageRect.getHeight() - humanX);
                rect.setLowerLeftX(pageRect.getWidth() - humanY - height);
                rect.setUpperRightX(pageRect.getWidth() - humanY);
            }
            default -> {
                rect.setLowerLeftX(humanX);
                rect.setUpperRightX(humanX + width);
                rect.setLowerLeftY(pageRect.getHeight() - humanY - height);
                rect.setUpperRightY(pageRect.getHeight() - humanY);
            }
        }
        return rect;
    }

    static void prepareAcroFormForSigning(PDDocument document) {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm(null);
        if (acroForm == null || !acroForm.getNeedAppearances()) {
            return;
        }
        if (acroForm.getFields().isEmpty()) {
            acroForm.getCOSObject().removeItem(COSName.NEED_APPEARANCES);
        }
    }

    /**
     * Controls whether the on-screen stamp is included in print output (PDF {@code /F} print flag).
     */
    static void applyStampPrintPolicy(PDDocument document, boolean printStamp) throws IOException {
        PDAcroForm acroForm = document.getDocumentCatalog().getAcroForm();
        if (acroForm == null) {
            return;
        }
        for (PDField field : acroForm.getFields()) {
            if (!(field instanceof PDSignatureField signatureField)) {
                continue;
            }
            for (PDAnnotationWidget widget : signatureField.getWidgets()) {
                widget.setPrinted(printStamp);
            }
        }
    }
}
