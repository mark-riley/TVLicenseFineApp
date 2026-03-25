package com.example;

import java.awt.Color;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/")
public class TVLicenseFineController {

    private final TVLicenseFineRepository repo;
    private final TVLicenseTransactionRepository transactionRepo;
    private final TVLicenseStatusRepository statusRepo;
    private final MessageSource messageSource;

    public TVLicenseFineController(TVLicenseFineRepository repo,
                                   TVLicenseTransactionRepository transactionRepo,
                                   TVLicenseStatusRepository statusRepo,
                                   MessageSource messageSource) {
        this.repo = repo;
        this.transactionRepo = transactionRepo;
        this.statusRepo = statusRepo;
        this.messageSource = messageSource;
        Stripe.apiKey = "sk_test_51TEVGrCt5TY5B9EcCm2HAPmwsQGYOGmlhtARd1lWEUMQr1fbYhVuGQAcfnkb5bJVzZryP6SWAPiplwh0egdncnj400TKnGwUe6";
    }

    private String getMessage(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
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
                redirectAttributes.addFlashAttribute("toast", getMessage("error.paid"));
                return "redirect:/";
            }
            BigDecimal totalPaid = transactionRepo.findByFine(fine).stream()
                    .map(TVLicenseTransaction::getAmount_paid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = fine.getAmountValue().subtract(totalPaid);

            model.addAttribute("fine", fine);
            model.addAttribute("totalAmount", fine.getAmount());
            model.addAttribute("outstanding", outstanding);
            return "fines/make";
        }
        model.addAttribute("error", getMessage("error.notfound"));
        return "fines/find";
    }

    @PostMapping("/submit-payment")
    public String submitPayment(@RequestParam Long fineId, @RequestParam BigDecimal amountPaid,
                                HttpServletRequest request, RedirectAttributes redirectAttributes) throws Exception {
        TVLicenseFine fine = repo.findById(fineId).orElseThrow();
        BigDecimal totalPaidSoFar = transactionRepo.findByFine(fine).stream()
                .map(TVLicenseTransaction::getAmount_paid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal outstanding = fine.getAmountValue().subtract(totalPaidSoFar);

        if (amountPaid.compareTo(BigDecimal.ZERO) <= 0 || amountPaid.compareTo(outstanding) > 0) {
            redirectAttributes.addFlashAttribute("error", getMessage("error.invalid"));
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
        Optional<TVLicenseTransaction> existingTx = transactionRepo.findByProcessorToken(sessionId);

        if (existingTx.isPresent()) {
            TVLicenseTransaction tx = existingTx.get();
            TVLicenseFine fine = tx.getFine();
            BigDecimal totalPaid = transactionRepo.findByFine(fine).stream()
                    .map(TVLicenseTransaction::getAmount_paid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            model.addAttribute("fine", fine);
            model.addAttribute("amountPaid", tx.getAmount_paid());
            model.addAttribute("remaining", fine.getAmountValue().subtract(totalPaid));
            model.addAttribute("transactionId", tx.getClient_transaction_id());
            model.addAttribute("createdAt", tx.getCreated_at().format(DateTimeFormatter.ofPattern("dd-MMMM-yyyy HH:mm:ss")));
            return "fines/confirmation";
        }

        Session session = Session.retrieve(sessionId);
        if ("paid".equals(session.getPaymentStatus())) {
            TVLicenseFine fine = repo.findById(fineId).orElseThrow();
            BigDecimal amountPaid = new BigDecimal(session.getAmountTotal()).divide(new BigDecimal(100));
            String clientTransactionId = fine.getReference() + LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddHHmmss"));

            TVLicenseTransaction transaction = new TVLicenseTransaction(fine, amountPaid, "Stripe", sessionId, clientTransactionId, LocalDateTime.now());
            transactionRepo.save(transaction);

            BigDecimal totalPaid = transactionRepo.findByFine(fine).stream()
                    .map(TVLicenseTransaction::getAmount_paid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal remaining = fine.getAmountValue().subtract(totalPaid);

            fine.setStatus(statusRepo.findById(remaining.compareTo(BigDecimal.ZERO) <= 0 ? 3L : 2L).get());
            repo.save(fine);

            model.addAttribute("fine", fine);
            model.addAttribute("amountPaid", amountPaid);
            model.addAttribute("remaining", remaining);
            model.addAttribute("transactionId", clientTransactionId);
            model.addAttribute("createdAt", transaction.getCreated_at().format(DateTimeFormatter.ofPattern("dd-MMMM-yyyy HH:mm:ss")));
            return "fines/confirmation";
        }
        return "redirect:/";
    }

    @GetMapping("/direct-pay/{reference}/{postcode}")
    public String directPay(@PathVariable String reference, @PathVariable String postcode,
                            Model model, RedirectAttributes redirectAttributes) {
        List<TVLicenseFine> fines = repo.findByReferenceAndPostcode(reference, postcode);

        if (!fines.isEmpty()) {
            TVLicenseFine fine = fines.get(0);
            if (fine.getStatus().getStatus_id() == 3L) {
                redirectAttributes.addFlashAttribute("toast", getMessage("error.paid"));
                return "redirect:/";
            }

            BigDecimal totalPaid = transactionRepo.findByFine(fine).stream()
                    .map(TVLicenseTransaction::getAmount_paid)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal outstanding = fine.getAmountValue().subtract(totalPaid);

            model.addAttribute("fine", fine);
            model.addAttribute("totalAmount", fine.getAmount());
            model.addAttribute("outstanding", outstanding);

            return "fines/make"; 
        }

        redirectAttributes.addFlashAttribute("error", getMessage("error.notfound"));
        return "redirect:/";
    }

    @GetMapping("/download-receipt")
    public void downloadReceipt(@RequestParam String transactionId, HttpServletResponse response) throws Exception {
        // PDF generation logic remains exactly the same as your original code
        TVLicenseTransaction tx = transactionRepo.findByClientTransactionId(transactionId).orElse(null);
        if (tx == null) return;

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=GOVUK_Receipt_" + transactionId + ".pdf");

        TVLicenseFine fine = tx.getFine();
        BigDecimal totalPaid = transactionRepo.findByFine(fine).stream()
                .map(TVLicenseTransaction::getAmount_paid)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        PdfPTable headerTable = new PdfPTable(1);
        headerTable.setWidthPercentage(100);
        PdfPCell crownCell = new PdfPCell(new Phrase("GOV.UK", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE)));
        crownCell.setBackgroundColor(Color.BLACK);
        crownCell.setPadding(20);
        crownCell.setBorder(Rectangle.NO_BORDER);
        headerTable.addCell(crownCell);
        document.add(headerTable);

        document.add(new Paragraph("\n"));

        PdfPTable successPanel = new PdfPTable(1);
        successPanel.setWidthPercentage(100);
        PdfPCell panelCell = new PdfPCell();
        panelCell.setBackgroundColor(new Color(0, 112, 60));
        panelCell.setPadding(20);
        panelCell.setBorder(Rectangle.NO_BORDER);
        panelCell.addElement(new Paragraph("Payment receipt", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, Color.WHITE)));
        panelCell.addElement(new Paragraph("Transaction reference: " + transactionId, FontFactory.getFont(FontFactory.HELVETICA, 11, Color.WHITE)));
        successPanel.addCell(panelCell);
        document.add(successPanel);

        document.add(new Paragraph("\n"));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        String[][] data = {
                {"Fine reference", fine.getReference()},
                {"Full name", fine.getFull_name().toUpperCase()},
                {"Payment date", tx.getCreated_at().format(DateTimeFormatter.ofPattern("d MMMM yyyy"))},
                {"Amount paid", "£" + tx.getAmount_paid().setScale(2, BigDecimal.ROUND_HALF_UP)},
                {"Balance remaining", "£" + fine.getAmountValue().subtract(totalPaid).setScale(2, BigDecimal.ROUND_HALF_UP)}
        };

        for (String[] row : data) {
            table.addCell(new Phrase(row[0], FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11)));
            table.addCell(new Phrase(row[1], FontFactory.getFont(FontFactory.HELVETICA, 11)));
        }
        document.add(table);
        document.close();
    }
}