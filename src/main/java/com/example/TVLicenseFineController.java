package com.example;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/")
public class TVLicenseFineController {

    private final TVLicenseFineRepository repo;
    private final TVLicenseTransactionRepository transactionRepo;
    private final TVLicenseStatusRepository statusRepo;

    public TVLicenseFineController(TVLicenseFineRepository repo,
                                   TVLicenseTransactionRepository transactionRepo,
                                   TVLicenseStatusRepository statusRepo) {
        this.repo = repo;
        this.transactionRepo = transactionRepo;
        this.statusRepo = statusRepo;
        // Note: In production, move this to an environment variable
        Stripe.apiKey = "sk_test_51TEVGrCt5TY5B9EcCm2HAPmwsQGYOGmlhtARd1lWEUMQr1fbYhVuGQAcfnkb5bJVzZryP6SWAPiplwh0egdncnj400TKnGwUe6";
    }

    @GetMapping
    public String home() {
        return "fines/find";
    }

    @PostMapping("/find-fine")
    public String processSearch(@RequestParam String reference, @RequestParam String postcode,
                                Model model, RedirectAttributes redirectAttributes) {
        List<TVLicenseFine> fines = repo.findByReferenceAndPostcode(reference, postcode);
        if (!fines.isEmpty()) {
            TVLicenseFine fine = fines.get(0);

            if (fine.getStatus().getStatus_id() == 3L) {
                redirectAttributes.addFlashAttribute("toast", "This fine has already been paid in full.");
                return "redirect:/";
            }

            BigDecimal totalPaid = transactionRepo.findByFine(fine).stream()
                    .map(TVLicenseTransaction::getAmount_paid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = new BigDecimal(fine.getAmount()).subtract(totalPaid);

            model.addAttribute("fine", fine);
            model.addAttribute("totalAmount", fine.getAmount());
            model.addAttribute("outstanding", outstanding);
            return "fines/make";
        }
        model.addAttribute("error", "No records found with the provided details.");
        return "fines/find";
    }

    @PostMapping("/submit-payment")
    public String submitPayment(@RequestParam Long fineId, @RequestParam BigDecimal amountPaid,
                                HttpServletRequest request, RedirectAttributes redirectAttributes) throws Exception {

        TVLicenseFine fine = repo.findById(fineId).orElseThrow();
        BigDecimal totalPaidSoFar = transactionRepo.findByFine(fine).stream()
                .map(TVLicenseTransaction::getAmount_paid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = new BigDecimal(fine.getAmount()).subtract(totalPaidSoFar);

        if (amountPaid.compareTo(BigDecimal.ZERO) <= 0 || amountPaid.compareTo(outstanding) > 0) {
            redirectAttributes.addFlashAttribute("error", "Invalid amount. You cannot pay more than the outstanding balance.");
            return "redirect:/";
        }

        long amountInPence = amountPaid.multiply(new BigDecimal("100")).longValue();
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(baseUrl + "/confirmation?session_id={CHECKOUT_SESSION_ID}&fineId=" + fineId)
                .setCancelUrl(baseUrl + "/")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("gbp")
                                .setUnitAmount(amountInPence)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Fine Payment - Ref: " + fine.getReference()).build())
                                .build())
                        .build())
                .build();

        Session session = Session.create(params);
        return "redirect:" + session.getUrl();
    }

    @GetMapping("/confirmation")
    public String confirmation(@RequestParam("session_id") String sessionId, @RequestParam("fineId") Long fineId, Model model) throws Exception {
        Session session = Session.retrieve(sessionId);

        if ("paid".equals(session.getPaymentStatus())) {
            TVLicenseFine fine = repo.findById(fineId).orElseThrow();
            BigDecimal amountPaid = new BigDecimal(session.getAmountTotal()).divide(new BigDecimal(100));

            // Generate the client-facing ID: Fine Reference + ddHHmmss (8 digits)
            String timestampAppend = LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddHHmmss"));
            String clientTransactionId = fine.getReference() + timestampAppend;

            // Save transaction with both the Stripe token and the client ID
            TVLicenseTransaction transaction = new TVLicenseTransaction(fine, amountPaid, "Stripe", sessionId, clientTransactionId, LocalDateTime.now());
            transactionRepo.save(transaction);

            BigDecimal totalPaid = transactionRepo.findByFine(fine).stream()
                    .map(TVLicenseTransaction::getAmount_paid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = new BigDecimal(fine.getAmount()).subtract(totalPaid);

            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                fine.setStatus(statusRepo.findById(3L).get());
            } else {
                fine.setStatus(statusRepo.findById(2L).get());
            }
            repo.save(fine);

            model.addAttribute("fine", fine);
            model.addAttribute("amountPaid", amountPaid);
            model.addAttribute("remaining", remaining);
            model.addAttribute("transactionId", clientTransactionId);

            return "fines/confirmation";
        }
        return "redirect:/";
    }

    @GetMapping("/download-receipt")
    public void downloadReceipt(@RequestParam String transactionId, HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=GOVUK_Receipt_" + transactionId + ".pdf");

        // Search using the new DB query for the client_transaction_id
        TVLicenseTransaction tx = transactionRepo.findByClientTransactionId(transactionId).orElse(null);

        if (tx == null) return;

        TVLicenseFine fine = tx.getFine();
        BigDecimal totalPaid = transactionRepo.findByFine(fine).stream()
                .map(TVLicenseTransaction::getAmount_paid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = new BigDecimal(fine.getAmount()).subtract(totalPaid);

        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        // --- GOV.UK STYLE COLORS ---
        Color govGreen = new Color(0, 112, 60);
        Color borderGrey = new Color(177, 180, 182);

        // 1. BLACK GOV.UK HEADER
        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        PdfPCell crownCell = new PdfPCell(new Phrase("GOV.UK", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE)));
        crownCell.setBackgroundColor(Color.BLACK);
        crownCell.setPadding(8);
        crownCell.setPaddingTop(20);
        crownCell.setPaddingBottom(20);
        crownCell.setPaddingLeft(25);
        crownCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(crownCell);
        document.add(headerTable);

        document.add(new Paragraph("\n"));

        // 2. GREEN SUCCESS BANNER
        PdfPTable successPanel = new PdfPTable(1);
        successPanel.setWidthPercentage(100);
        PdfPCell panelCell = new PdfPCell();
        panelCell.setBackgroundColor(govGreen);
        panelCell.setPadding(20);
        panelCell.setPaddingTop(8);
        panelCell.setBorder(Rectangle.NO_BORDER);

        panelCell.addElement(new Paragraph("Payment successful", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.WHITE)));
        Paragraph refPara = new Paragraph("Transaction reference: " + transactionId, FontFactory.getFont(FontFactory.HELVETICA, 11, Color.WHITE));
        refPara.setSpacingBefore(8);
        panelCell.addElement(refPara);

        successPanel.addCell(panelCell);
        document.add(successPanel);

        document.add(new Paragraph("\n"));

        // 3. PAYMENT SUMMARY
        document.add(new Paragraph("Payment summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        document.add(new Paragraph("\n"));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1.2f, 2.8f});

        String[][] data = {
                {"Fine reference", fine.getReference()},
                {"Full name", fine.getFull_name().toUpperCase()},
                {"Payment date", tx.getCreated_at().format(DateTimeFormatter.ofPattern("d MMMM yyyy"))},
                {"Amount paid", "£" + tx.getAmount_paid().setScale(2, BigDecimal.ROUND_HALF_UP).toString()},
                {"Balance remaining", "£" + outstanding.setScale(2, BigDecimal.ROUND_HALF_UP).toString()}
        };

        for (String[] row : data) {
            PdfPCell keyCell = new PdfPCell(new Phrase(row[0], FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            PdfPCell valCell = new PdfPCell(new Phrase(row[1], FontFactory.getFont(FontFactory.HELVETICA, 11)));

            keyCell.setBorder(Rectangle.BOTTOM);
            valCell.setBorder(Rectangle.BOTTOM);
            keyCell.setBorderColor(borderGrey);
            valCell.setBorderColor(borderGrey);
            keyCell.setPadding(12);
            valCell.setPadding(12);

            table.addCell(keyCell);
            table.addCell(valCell);
        }
        document.add(table);

        // 4. FOOTER / NEXT STEPS
        document.add(new Paragraph("\n\n"));

        document.close();
    }
}