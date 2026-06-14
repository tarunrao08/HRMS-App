package com.hrms.payroll.util;

import com.hrms.employee.entity.Employee;
import com.hrms.payroll.entity.Payslip;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class PayslipPdfService {

    private static final String[] MONTH_NAMES = {
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    };

    private static final DeviceRgb PRIMARY   = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb HEADER_BG = new DeviceRgb(239, 246, 255);
    private static final DeviceRgb MUTED     = new DeviceRgb(107, 114, 128);
    private static final DeviceRgb LIGHT_BG  = new DeviceRgb(243, 244, 246);

    @Value("${payroll.pdf.base-dir:${user.home}/hrms-payslips}")
    private String basePdfDir;

    /** Generates the PDF to disk and returns the absolute file path stored as pdfUrl. */
    public String generateAndSave(Payslip payslip) throws IOException {
        Employee emp = payslip.getEmployee();
        Path dir = Paths.get(basePdfDir,
                String.valueOf(payslip.getYear()),
                String.format("%02d", payslip.getMonth()));
        Files.createDirectories(dir);

        String fileName = String.format("payslip_%d_%02d_%s.pdf",
                payslip.getYear(), payslip.getMonth(), emp.getEmployeeCode());
        Path out = dir.resolve(fileName);

        try (OutputStream os = new FileOutputStream(out.toFile())) {
            buildPdf(payslip, os);
        }
        log.debug("Generated payslip PDF: {}", out);
        return out.toAbsolutePath().toString();
    }

    // ── PDF layout ─────────────────────────────────────────────────────────────

    private void buildPdf(Payslip payslip, OutputStream out) throws IOException {
        Employee emp = payslip.getEmployee();
        String monthYear = MONTH_NAMES[payslip.getMonth() - 1] + " " + payslip.getYear();

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        Document doc = new Document(pdf, PageSize.A4);
        doc.setMargins(36, 36, 36, 36);

        // ── Header bar ─────────────────────────────────────────────────────────
        Table header = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
        header.addCell(new Cell()
                .add(new Paragraph("HRMS").setFont(bold).setFontSize(20).setFontColor(PRIMARY))
                .add(new Paragraph("Human Resource Management System").setFont(regular).setFontSize(8).setFontColor(MUTED))
                .setBorder(Border.NO_BORDER).setPadding(8));
        header.addCell(new Cell()
                .add(new Paragraph("PAYSLIP").setFont(bold).setFontSize(16).setTextAlignment(TextAlignment.RIGHT))
                .add(new Paragraph(monthYear).setFont(regular).setFontSize(11).setFontColor(MUTED).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setPadding(8));
        doc.add(header);
        doc.add(new Paragraph().setBorderBottom(new SolidBorder(PRIMARY, 2)).setMarginBottom(12));

        // ── Employee info (4-column grid) ──────────────────────────────────────
        Table empInfo = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                .useAllAvailableWidth().setBackgroundColor(HEADER_BG);
        String fullName = emp.getFirstName() + " " + emp.getLastName();
        String designation = emp.getDesignation() != null ? emp.getDesignation().getName() : "—";
        String joining = emp.getJoiningDate() != null ? emp.getJoiningDate().toString() : "—";
        infoCell(empInfo, "Employee Name",  fullName,                          bold, regular);
        infoCell(empInfo, "Employee Code",  emp.getEmployeeCode(),             bold, regular);
        infoCell(empInfo, "Working Days",   String.valueOf(payslip.getWorkingDays()), bold, regular);
        infoCell(empInfo, "Paid Days",      num(payslip.getPaidDays()),        bold, regular);
        infoCell(empInfo, "Designation",    designation,                       bold, regular);
        infoCell(empInfo, "LOP Days",       num(payslip.getLopDays()),         bold, regular);
        infoCell(empInfo, "Pay Period",     monthYear,                         bold, regular);
        infoCell(empInfo, "Joining Date",   joining,                           bold, regular);
        doc.add(empInfo);
        doc.add(new Paragraph().setMarginBottom(12));

        // ── Earnings / Deductions columns ──────────────────────────────────────
        Table twoCol = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();

        // Section headers
        twoCol.addCell(sectionHeader("EARNINGS", bold));
        twoCol.addCell(sectionHeader("DEDUCTIONS", bold));

        // Earnings body
        Table earn = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
        earningRow(earn, "Basic",               payslip.getBasic(),            regular);
        earningRow(earn, "HRA",                 payslip.getHra(),              regular);
        earningRow(earn, "Dearness Allowance",  payslip.getDa(),               regular);
        earningRow(earn, "Conveyance",          payslip.getConveyance(),       regular);
        earningRow(earn, "Medical Allowance",   payslip.getMedicalAllowance(), regular);
        earningRow(earn, "Special Allowance",   payslip.getSpecialAllowance(), regular);
        twoCol.addCell(new Cell().add(earn).setBorder(Border.NO_BORDER).setPadding(0));

        // Deductions body
        Table ded = new Table(UnitValue.createPercentArray(new float[]{60, 40})).useAllAvailableWidth();
        earningRow(ded, "Provident Fund",    payslip.getPfDeduction(),    regular);
        earningRow(ded, "ESI",               payslip.getEsiDeduction(),   regular);
        earningRow(ded, "Professional Tax",  payslip.getProfessionalTax(), regular);
        earningRow(ded, "Tax (TDS)",         payslip.getTds(),            regular);
        twoCol.addCell(new Cell().add(ded).setBorder(Border.NO_BORDER).setPadding(0));
        doc.add(twoCol);

        // ── Subtotals row ──────────────────────────────────────────────────────
        Table subtotals = new Table(UnitValue.createPercentArray(new float[]{50, 50})).useAllAvailableWidth();
        subtotals.addCell(new Cell()
                .add(new Paragraph("Gross Pay: " + inr(payslip.getGrossSalary()))
                        .setFont(bold).setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(LIGHT_BG).setPadding(6)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
        subtotals.addCell(new Cell()
                .add(new Paragraph("Total Deductions: " + inr(payslip.getTotalDeductions()))
                        .setFont(bold).setFontSize(10).setTextAlignment(TextAlignment.RIGHT))
                .setBackgroundColor(LIGHT_BG).setPadding(6)
                .setBorder(new SolidBorder(ColorConstants.LIGHT_GRAY, 0.5f)));
        doc.add(subtotals);

        // ── Net Pay banner ─────────────────────────────────────────────────────
        doc.add(new Paragraph().setMarginBottom(6));
        Table netBanner = new Table(1).useAllAvailableWidth();
        netBanner.addCell(new Cell()
                .add(new Paragraph("NET PAY: " + inr(payslip.getNetSalary()))
                        .setFont(bold).setFontSize(14)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(PRIMARY).setPadding(12).setBorder(Border.NO_BORDER));
        doc.add(netBanner);

        // ── Footer ─────────────────────────────────────────────────────────────
        doc.add(new Paragraph().setMarginBottom(16));
        doc.add(new Paragraph("This is a computer-generated payslip and does not require a signature.")
                .setFont(regular).setFontSize(8).setFontColor(MUTED).setTextAlignment(TextAlignment.CENTER));

        doc.close();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private static void infoCell(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(regular).setFontSize(8).setFontColor(MUTED))
                .add(new Paragraph(value != null ? value : "—").setFont(bold).setFontSize(10))
                .setBorder(Border.NO_BORDER).setPadding(8));
    }

    private static Cell sectionHeader(String title, PdfFont bold) {
        return new Cell()
                .add(new Paragraph(title).setFont(bold).setFontSize(10).setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(PRIMARY).setPadding(6).setBorder(Border.NO_BORDER);
    }

    private static void earningRow(Table table, String name, BigDecimal amount, PdfFont font) {
        table.addCell(new Cell()
                .add(new Paragraph(name).setFont(font).setFontSize(9))
                .setBorder(Border.NO_BORDER).setPaddingTop(3).setPaddingBottom(3).setPaddingLeft(6));
        table.addCell(new Cell()
                .add(new Paragraph(inr(amount)).setFont(font).setFontSize(9).setTextAlignment(TextAlignment.RIGHT))
                .setBorder(Border.NO_BORDER).setPaddingTop(3).setPaddingBottom(3).setPaddingRight(6));
    }

    private static String inr(BigDecimal amount) {
        if (amount == null) return "₹0.00";
        return "₹" + String.format("%,.2f", amount);
    }

    private static String num(BigDecimal val) {
        return val == null ? "0" : val.stripTrailingZeros().toPlainString();
    }
}
