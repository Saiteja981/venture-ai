package com.ventureai.backend.service.reports;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/**
 * Minimal PDF builder for report exports.
 *
 * Produces A4 portrait documents with a title, optional subtitle, a
 * two-column key/value table, and a footer line.
 */
public class PdfBuilder {

    private static final Font TITLE_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, new Color(15, 23, 42));
    private static final Font SUBTITLE_FONT =
            FontFactory.getFont(FontFactory.HELVETICA, 11, new Color(100, 116, 139));
    private static final Font HEADING_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, new Color(30, 41, 59));
    private static final Font LABEL_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, new Color(71, 85, 105));
    private static final Font VALUE_FONT =
            FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(15, 23, 42));
    private static final Font FOOTER_FONT =
            FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8, new Color(148, 163, 184));

    private final Document doc;
    private final ByteArrayOutputStream out;

    public PdfBuilder(String title, String subtitle) throws DocumentException {
        this.out = new ByteArrayOutputStream();
        this.doc = new Document(PageSize.A4, 48, 48, 54, 54);
        PdfWriter.getInstance(doc, out);
        doc.open();

        Paragraph titleP = new Paragraph(title, TITLE_FONT);
        titleP.setSpacingAfter(4);
        doc.add(titleP);

        if (subtitle != null && !subtitle.isBlank()) {
            Paragraph subP = new Paragraph(subtitle, SUBTITLE_FONT);
            subP.setSpacingAfter(18);
            doc.add(subP);
        }
    }

    public PdfBuilder heading(String text) throws DocumentException {
        Paragraph p = new Paragraph(text, HEADING_FONT);
        p.setSpacingBefore(14);
        p.setSpacingAfter(6);
        doc.add(p);
        return this;
    }

    public PdfBuilder keyValue(String label, Object value) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{1.5f, 4.5f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(2);

        PdfPCell labelCell = new PdfPCell(new Phrase(label, LABEL_FONT));
        labelCell.setBorder(0);
        labelCell.setPaddingBottom(4);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(
                value == null ? "—" : value.toString(), VALUE_FONT));
        valueCell.setBorder(0);
        valueCell.setPaddingBottom(4);
        table.addCell(valueCell);

        doc.add(table);
        return this;
    }

    public PdfBuilder separator() throws DocumentException {
        Paragraph p = new Paragraph(" ", FOOTER_FONT);
        p.setSpacingAfter(8);
        doc.add(p);
        return this;
    }

    public PdfBuilder note(String text) throws DocumentException {
        Paragraph p = new Paragraph(text, FOOTER_FONT);
        p.setSpacingBefore(14);
        p.setAlignment(Element.ALIGN_LEFT);
        doc.add(p);
        return this;
    }

    public PdfBuilder table(String[] headers, java.util.List<Object[]> rows)
            throws DocumentException {
        if (headers == null || headers.length == 0) return this;
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(10);

        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, LABEL_FONT));
            c.setBackgroundColor(new Color(241, 245, 249));
            c.setPadding(6);
            c.setBorder(0);
            table.addCell(c);
        }
        for (Object[] row : rows) {
            for (Object cell : row) {
                PdfPCell c = new PdfPCell(new Phrase(
                        cell == null ? "—" : cell.toString(), VALUE_FONT));
                c.setPadding(6);
                c.setBorder(0);
                table.addCell(c);
            }
        }
        doc.add(table);
        return this;
    }

    public byte[] close() {
        if (doc.isOpen()) doc.close();
        return out.toByteArray();
    }
}